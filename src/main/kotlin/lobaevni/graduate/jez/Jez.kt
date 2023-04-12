package lobaevni.graduate.jez

import lobaevni.graduate.Utils.cartesianProduct
import lobaevni.graduate.jez.JezHeuristics.assume
import lobaevni.graduate.jez.JezHeuristics.checkSideContradictions
import lobaevni.graduate.jez.JezHeuristics.getSideConstants
import lobaevni.graduate.jez.JezHeuristics.tryShorten
import lobaevni.graduate.jez.action.JezReplaceConstantsAction
import lobaevni.graduate.jez.action.JezReplaceVariablesAction

/**
 * Tries to find minimal solution of this [JezEquation].
 */
fun JezEquation.tryFindMinimalSolution(
    allowRevert: Boolean,
    storeHistory: Boolean,
    storeEquations: Boolean,
    maxIterationsCount: Int,
    dot: Boolean,
    dotHTMLLabels: Boolean,
    dotMaxStatementsCount: Int,
): JezResult {
    val state = JezState(
        equation = this,
        storeHistory = storeHistory || allowRevert,
        storeEquations = storeEquations,
        dot = dot,
        dotHTMLLabels = dotHTMLLabels,
        dotMaxStatementsCount = dotMaxStatementsCount
    ).apply {
        val variables = equation.getUsedVariables()
        for (variable in variables) {
            sigmaLeft[variable] = mutableListOf()
            sigmaRight[variable] = mutableListOf()
            negativeSigmaLeft[variable] = mutableSetOf()
            negativeSigmaRight[variable] = mutableSetOf()
        }
        history?.init(equation)
    }
    return state.tryFindMinimalSolution(allowRevert, maxIterationsCount)
}

/**
 * Tries to find minimal solution of [this.equation].
 */
internal fun JezState.tryFindMinimalSolution(
    allowRevert: Boolean,
    maxIterationsCount: Int,
): JezResult {
    var iteration = 0
    while (
        !equation.checkEmptySolution() &&
        iteration++ < maxIterationsCount
    ) {
        if (!tryShorten() || equation.checkSideContradictions()) {
            if (!allowRevert || !revertUntilNoSolution()) break
        }

        val currentEquation = equation

        val compressions = listOf(
            { blockCompNCr() },
            { blockCompCr() },
            { pairCompNCr() },
            { pairCompCr(equation == currentEquation) }
        )
        for (compression in compressions) {
            compression()
            if (!tryShorten() || equation.checkEmptySolution() || equation.checkSideContradictions()) break
        }
        if (!tryShorten()) continue
        if (equation.checkEmptySolution()) break
        if (equation.checkSideContradictions()) continue

        if (equation == currentEquation && (!allowRevert || !revertUntilNoSolution())) break
    }

    val sigma: JezSigma = equation.getUsedVariables().associateWith { variable ->
        (sigmaLeft[variable]!! + sigmaRight[variable]!!).toJezSourceConstants()
    }

    val solutionState = if (equation.checkEmptySolution()) {
        JezResult.SolutionState.Found
    } else {
        if (iteration > maxIterationsCount) {
            JezResult.SolutionState.NoSolution.NotEnoughIterations
        } else {
            JezResult.SolutionState.NoSolution.Absolutely
        }
    }

    if (solutionState is JezResult.SolutionState.Found) {
        apply(JezReplaceVariablesAction(
            replaces = equation.getUsedVariables().map { variable ->
                Pair(listOf(variable), listOf())
            },
        ))
    }

    return JezResult(
        sigma = sigma,
        solutionState = solutionState,
        historyDotGraph = history?.dotRootGraph,
    )
}

/**
 * TODO
 */
internal fun JezState.blockCompCr(): JezState {
    val suggestedBlockComps = mutableMapOf<JezVariable, MutableList<JezConstant>>() //duplicates allowed, because order is more important than space complexity
    for (equationPart in listOf(equation.u, equation.v)) {
        equationPart.forEachIndexed { index, element ->
            if (element !is JezVariable) return@forEachIndexed

            val previous = equationPart.elementAtOrNull(index - 1)
            val next = equationPart.elementAtOrNull(index + 1)
            listOfNotNull(previous, next)
                .filterIsInstance<JezGeneratedConstant>()
                .filter { it.isBlock }
                .forEach { constant ->
                    suggestedBlockComps.getOrPut(element) { mutableListOf() }.add(constant.source.first())
                }
        }
    }

    val connectedBlocks = mutableMapOf<JezConstant, MutableSet<JezConstant>>()
    for (equationPart in listOf(equation.u, equation.v)) {
        equationPart.forEachIndexed { index, element ->
            if ((element as? JezGeneratedConstant)?.isBlock != true) return@forEachIndexed

            val previous = equationPart.elementAtOrNull(index - 1)
            val next = equationPart.elementAtOrNull(index + 1)
            listOfNotNull(previous, next)
                .filterIsInstance<JezGeneratedConstant>()
                .filter { it.isBlock }
                .forEach { constant ->
                    connectedBlocks.getOrPut(element.value.first()) { mutableSetOf() }.add(constant.value.first())
                    connectedBlocks.getOrPut(constant.value.first()) { mutableSetOf() }.add(element.value.first())
                }
        }
    }
    for (suggestedBlockComp in suggestedBlockComps) {
        for (constant in suggestedBlockComp.value.toList()) { //need a copy, because we modify it same time
            suggestedBlockComp.value.addAll(connectedBlocks.getOrDefault(constant, null) ?: listOf())
        }
    }

    suggestedBlockComps.forEach { (variable, constants) ->
        constants.any { constant ->
            if (negativeSigmaLeft[variable]!!.contains(constant) &&
                negativeSigmaRight[variable]!!.contains(constant)) return@any false

            val leftBlock = if (!negativeSigmaLeft[variable]!!.contains(constant)) {
                listOf(generateConstantBlock(constant))
            } else listOf()
            val rightBlock = if (!negativeSigmaRight[variable]!!.contains(constant)) {
                listOf(generateConstantBlock(constant))
            } else listOf()
            if (!apply(JezReplaceVariablesAction(
                    variable,
                    leftPart = leftBlock,
                    rightPart = rightBlock,
                    subNegativeSigmaLeft = mapOf(variable to negativeSigmaLeft[variable]!!),
                    subNegativeSigmaRight = mapOf(variable to negativeSigmaRight[variable]!!),
                ))) return@any false

            nonEmptyVariables.add(variable)
            return@any true
        }
    }
    return this
}

/**
 * Compression for non-crossing blocks.
 */
internal fun JezState.blockCompNCr(): JezState {
    val blocks: MutableSet<List<JezConstant>> = mutableSetOf()
    for (equationPart in listOf(equation.u, equation.v)) {
        data class Acc(
            val element: JezElement?,
            val count: Int,
        )

        (equationPart + null).map { element ->
            Acc(element, 1)
        }.reduce { lastAcc, currentAcc ->
            if (lastAcc.element == currentAcc.element) {
                Acc(currentAcc.element, lastAcc.count + 1)
            } else {
                if (lastAcc.element is JezConstant && lastAcc.count > 1) {
                    val block = List(lastAcc.count) { lastAcc.element }
                    blocks.add(block)
                }
                currentAcc
            }
        }
    }

    apply(JezReplaceConstantsAction(blocks.map { block ->
        Pair(block, listOf(getOrGenerateConstant(block)))
    }))
    return this
}

/**
 * Compression for non-crossing pairs.
 */
internal fun JezState.pairCompNCr(): JezState {
    val constants = equation.getSideConstants()
    val pairs = cartesianProduct(constants.first, constants.second)
    apply(JezReplaceConstantsAction(pairs.map { pair ->
        Pair(pair.toList(), listOf(getOrGenerateConstant(pair.toList())))
    }))
    return this
}

/**
 * Compression for a crossing pairs.
 * @param necComp a trick whether should we necessarily perform compression for a random possibly crossing pair if
 * there really is no way to compress the equation at the algorithm iteration.
 */
internal fun JezState.pairCompCr(necComp: Boolean): JezState {
    /**
     * Tries to assume which pair we really may count as crossing and then applies appropriate action, if one has been
     * found.
     */
    fun tryAssumeAndApply(sideConstants: Set<JezConstant>, left: Boolean): Boolean {
        val assumption = equation.assume(sideConstants, left) ?: return false
        val action = if (left) {
            JezReplaceVariablesAction(
                variable = assumption.first,
                leftPart = listOf(assumption.second),
                rightPart = listOf(),
                subNegativeSigmaLeft = mapOf(assumption.first to negativeSigmaLeft[assumption.first]!!),
                subNonEmptyVariables = if (assumption.first in nonEmptyVariables) listOf(assumption.first) else listOf(),
            )
        } else {
            JezReplaceVariablesAction(
                variable = assumption.first,
                leftPart = listOf(),
                rightPart = listOf(assumption.second),
                subNegativeSigmaRight = mapOf(assumption.first to negativeSigmaRight[assumption.first]!!),
                subNonEmptyVariables = if (assumption.first in nonEmptyVariables) listOf(assumption.first) else listOf(),
            )
        }
        return apply(action)
    }

    val sideConstantsNCr = equation.getSideConstants()
    for (left in listOf(true, false)) {
        tryAssumeAndApply(sideConstantsNCr.second, left)
        if (!tryShorten() || equation.checkEmptySolution() || equation.checkSideContradictions()) return this
    }

    if (!necComp) return this

    for (left in listOf(true, false)) {
        for (variable in equation.getUsedVariables()) {
            for (constant in (equation.getUsedSourceConstants() + equation.getUsedGeneratedConstants())) {
                val action = if (left) {
                    JezReplaceVariablesAction(
                        variable,
                        leftPart = listOf(constant),
                        rightPart = listOf(),
                        subNegativeSigmaLeft = mapOf(variable to negativeSigmaLeft[variable]!!),
                        subNonEmptyVariables = if (variable in nonEmptyVariables) listOf(variable) else listOf(),
                    )
                } else {
                    JezReplaceVariablesAction(
                        variable,
                        leftPart = listOf(),
                        rightPart = listOf(constant),
                        subNegativeSigmaRight = mapOf(variable to negativeSigmaRight[variable]!!),
                        subNonEmptyVariables = if (variable in nonEmptyVariables) listOf(variable) else listOf(),
                    )
                }
                if (apply(action)) return this
            }
        }
    }
    return this
}

/**
 * Reverts actions (via addressing to a history) until it is able to move another way (until there can be no solution
 * yet).
 * @return true, if there was found node, through which we can try to find a solution, false otherwise.
 */
internal fun JezState.revertUntilNoSolution(): Boolean {
    while (true) {
        val action = history?.currentGraphNode?.action ?: return false
        if (!revert(action)) return false

        when (action) {
            is JezReplaceVariablesAction -> return true
            else -> {}
        }
    }
}

/**
 * @return whether an empty solution is suitable for this [JezEquation].
 */
fun JezEquation.checkEmptySolution(): Boolean {
    val u = u.filterIsInstance<JezConstant>()
    val v = v.filterIsInstance<JezConstant>()

    /*val a = mk.ndarray(mk[mk[2, 3, -7, -11]])
    val b = mk.ndarray(mk[8])
    val result = mk.linalg.solve(a, b)
    print(result)*/

    return u == v
}

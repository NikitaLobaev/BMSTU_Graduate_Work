package lobaevni.graduate.jez

import lobaevni.graduate.Utils.cartesianProduct
import lobaevni.graduate.jez.JezHeuristics.getSideConstants
import lobaevni.graduate.jez.JezHeuristics.tryAssumeAndApply
import lobaevni.graduate.jez.action.JezCropAction
import lobaevni.graduate.jez.action.JezDropVariablesAction
import lobaevni.graduate.jez.action.JezReplaceConstantsAction
import lobaevni.graduate.jez.action.JezReplaceVariablesAction
import lobaevni.graduate.jez.data.*
import kotlin.math.E
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Tries to find minimal solution of this [JezEquation].
 */
fun JezEquation.tryFindMinimalSolution(
    allowRevert: Boolean,
    allowBlockCompCr: Boolean,
    storeHistory: Boolean,
    storeEquations: Boolean,
    heurExtNegRest: Boolean,
    maxIterationsCount: Long?,
    dot: Boolean,
    dotHTMLLabels: Boolean,
    dotMaxStatementsCount: Int,
): JezResult = JezState(
    equation = this,
    allowRevert = allowRevert,
    allowBlockCompCr = allowBlockCompCr,
    storeHistory = storeHistory || allowRevert,
    storeEquations = storeEquations,
    heurExtNegRest = heurExtNegRest,
    dot = dot,
    dotHTMLLabels = dotHTMLLabels,
    dotMaxStatementsCount = dotMaxStatementsCount
).tryFindMinimalSolution(maxIterationsCount)

/**
 * Tries to find minimal solution of [this.equation].
 */
internal fun JezState.tryFindMinimalSolution(
    maxIterationsCount: Long?,
): JezResult {
    assert(maxIterationsCount == null || maxIterationsCount >= 0)

    val variables = equation.getUsedVariables()

    var iteration: Long = 0
    val maxSolutionLength: Long = (E.pow(E.pow(equation.u.size + equation.v.size))).roundToLong() //TODO
    mainLoop@ while (maxIterationsCount == null || iteration++ < maxIterationsCount) {
        try {
            tryShorten()
        } catch (e: JezEquationNotConvergesException) {
            break@mainLoop
        }
        if (checkTrivialContradictions() || checkEmptySolution()) break@mainLoop

        val currentEquation = equation

        val compressions = listOfNotNull(
            { blockCompNCr() },
            if (allowBlockCompCr) { { blockCompCr() } } else null,
            { pairCompNCr() },
            { pairCompCr(equation == currentEquation) }
        )
        try {
            compressionLoop@ for (compression in compressions) {
                compression()

                tryShorten()
                if (checkEmptySolution()) break@mainLoop
                if (checkTrivialContradictions() || !checkEquationLength(maxSolutionLength)) {
                    throw JezEquationNotConvergesException()
                }
            }
        } catch (e: JezEquationNotConvergesException) {
            if (!allowRevert || !revertUntilNoSolution()) break@mainLoop
            continue@mainLoop
        }

        if (equation == currentEquation) {
            if (!allowRevert || !revertUntilNoSolution(skips = 1)) break@mainLoop
        }
    }

    val sigma: JezSigma = variables.associateWith { variable ->
        (sigmaLeft[variable]!! + sigmaRight[variable]!!).toJezSourceConstants()
    }

    val solutionState = if (checkEmptySolution()) {
        JezResult.SolutionState.Found
    } else {
        if (maxIterationsCount != null && iteration > maxIterationsCount) {
            JezResult.SolutionState.NotFound.NotEnoughIterations
        } else {
            //TODO: check for possible reverts, and if allowRevert is false, then its not Absolutely
            JezResult.SolutionState.NotFound.NoSolution
        }
    }

    if (solutionState is JezResult.SolutionState.Found) {
        val curUsedVariables = equation.getUsedVariables()
        assert(curUsedVariables.isEmpty() || apply(JezDropVariablesAction(
            replaces = curUsedVariables.map { variable ->
                Pair(listOf(variable), listOf())
            },
        )))
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
            if (negativeSigmaLeft?.get(variable)?.contains(constant) == true &&
                negativeSigmaRight?.get(variable)?.contains(constant) == true) return@any false

            val leftBlock = if (negativeSigmaLeft?.get(variable)?.contains(constant) != true) {
                listOf(generateConstantBlock(constant))
            } else listOf()
            val rightBlock = if (negativeSigmaRight?.get(variable)?.contains(constant) != true) {
                listOf(generateConstantBlock(constant))
            } else listOf()
            if (!apply(JezReplaceVariablesAction(
                    variable,
                    leftPart = leftBlock,
                    rightPart = rightBlock,
                    oldNegativeSigmaLeft = negativeSigmaLeft?.toJezNegativeSigma()?.filterKeys { it == variable },
                    oldNegativeSigmaRight = negativeSigmaRight?.toJezNegativeSigma()?.filterKeys { it == variable },
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
        Pair(block, listOf(getOrPutGeneratedConstant(block)))
    }))
    return this
}

/**
 * Compression for non-crossing pairs.
 */
internal fun JezState.pairCompNCr(): JezState {
    val sideConstants = getSideConstants()
    val pairs = cartesianProduct(sideConstants.first, sideConstants.second)
    //TODO: maybe filter existing pairs from cartesian product
    apply(JezReplaceConstantsAction(pairs.map { pair ->
        Pair(pair.toList(), listOf(getOrPutGeneratedConstant(pair.toList())))
    }))
    return this
}

/**
 * Compression for a crossing pairs.
 * @param necComp heuristic whether should we necessarily perform compression for a random possibly crossing pair if
 * there really is no way to compress the equation at the algorithm iteration.
 */
internal fun JezState.pairCompCr(necComp: Boolean): JezState {
    if (tryAssumeAndApply() || !necComp) return this

    for (left in listOf(true, false)) {
        for (variable in equation.getUsedVariables()) {
            for (constant in (equation.getUsedSourceConstants() + equation.getUsedGeneratedConstants())) {
                if (apply(JezReplaceVariablesAction(
                        variable,
                        leftPart = if (left) listOf(constant) else listOf(),
                        rightPart = if (left) listOf() else listOf(constant),
                        oldNegativeSigmaLeft = negativeSigmaLeft?.toJezNegativeSigma()?.filterKeys { it == variable },
                        oldNegativeSigmaRight = negativeSigmaRight?.toJezNegativeSigma()?.filterKeys { it == variable },
                ))) return this
            }
        }
    }
    return this
}

/**
 * Shortening of the [JezEquation]. Cuts similar starts and ends of the left and right side of the
 * [JezEquation].
 * @return TODO
 */
internal fun JezState.tryShorten(): Boolean {
    //TODO: maybe collapse adjacent parametrized blocks
    val leftIndex = equation.u.zip(equation.v).indexOfFirst { (uElement, vElement) ->
        uElement != vElement
    }.takeIf { it != -1 } ?: minOf(equation.u.size, equation.v.size)
    val rightIndex = equation.u.reversed().zip(equation.v.reversed()).indexOfFirst { (uElement, vElement) ->
        uElement != vElement
    }.takeIf { it != -1 } ?: minOf(equation.u.size, equation.v.size)

    if (leftIndex + rightIndex == 0) return false

    if (!apply(JezCropAction(
            equation = equation,
            leftSize = leftIndex,
            rightSize = rightIndex,
        ))) throw JezEquationNotConvergesException()

    return true
}

/**
 * Checking trivial and side contradictions in the [JezEquation] at left and at right sides of both parts of it.
 * @return true, if contradiction was found, false otherwise.
 */
internal fun JezState.checkTrivialContradictions(): Boolean {
    if ((equation.u.isEmpty() && equation.v.find { it is JezConstant } != null) ||
        (equation.v.isEmpty() && equation.u.find { it is JezConstant } != null)) return true

    fun JezElement.retrieveConstant(): JezConstant? {
        return when (this) {
            is JezGeneratedConstantBlock -> constant
            is JezConstant -> this
            else -> null
        }
    }

    return (equation.u.firstOrNull()?.retrieveConstant() is JezConstant &&
            equation.v.firstOrNull()?.retrieveConstant() is JezConstant &&
            equation.u.first().retrieveConstant() != equation.v.first().retrieveConstant()) ||
            (equation.u.lastOrNull()?.retrieveConstant() is JezConstant &&
                    equation.v.lastOrNull()?.retrieveConstant() is JezConstant &&
                    equation.u.last().retrieveConstant() != equation.v.last().retrieveConstant())
}

/**
 * Reverts actions (via addressing to a history) until it is able to move another way (until there can be no solution
 * yet).
 * @return true, if there was found node, through which we can try to find a solution, false otherwise.
 */
internal fun JezState.revertUntilNoSolution(skips: Int = 0): Boolean {
    assert(skips >= 0)
    var skipsRemaining = skips
    while (skipsRemaining >= 0) {
        val action = history?.currentGraphNode?.action ?: return false
        if (!revert(action)) return false

        when (action) {
            is JezReplaceVariablesAction,
            is JezDropVariablesAction -> skipsRemaining--
            else -> {}
        }
    }
    return true
}

/**
 * @return whether an empty solution is suitable for this [JezEquation].
 */
internal fun JezState.checkEmptySolution(): Boolean {
    if (!allowBlockCompCr) {
        return equation.u.filterIsInstance<JezConstant>() == equation.v.filterIsInstance<JezConstant>()
    }

    data class JezEquationEntry(
        val source: List<JezSourceConstant> = listOf(),
        val entries: MutableList<JezConstant> = mutableListOf(),
    )

    /**
     * TODO
     */
    fun List<JezConstant>.groupBySource(): List<JezEquationEntry> {
        return map { constant ->
            mutableListOf(JezEquationEntry(constant.source, mutableListOf(constant)))
        }.reduce { last, current ->
            if (last.last().source == current.first().source) {
                last.last().entries.addAll(current.first().entries)
            } else {
                last.addAll(current)
            }
            return@reduce last
        }
    }

    //val d: D2Array<Int> = mk.ndarray(mk[mk[2, 3, -1], mk[1, -2, 1], mk[1, 0, 2]])

    val u = equation.u.filterIsInstance<JezConstant>().groupBySource()
    val v = equation.v.filterIsInstance<JezConstant>().groupBySource()
    val uIterator = u.iterator()
    val vIterator = v.iterator()
    uLoop@ while (uIterator.hasNext()) {
        val uEntry = uIterator.next()
        vLoop@ while (vIterator.hasNext()) {
            val vEntry = vIterator.next()
            if (vEntry.source != uEntry.source) {
                //TODO
                continue@vLoop
            }

            //TODO
            continue@uLoop
            break@vLoop
        }
        return false
    }

    /*val a: D2Array<Long> = mk.ndarray(mk[mk[2, 3, -1], mk[1, -2, 1], mk[1, 0, 2]])
    val b: D1Array<Long> = mk.ndarray(mk[9, 3, 2])
    val rrr = mk.linalg.solve(a, b)
    print(rrr)*/

    return u == v
}

/**
 * TODO
 */
internal fun JezState.checkEquationLength(maxSolutionLength: Long): Boolean {
    /**
     * TODO
     */
    fun JezEquationPart.getLength(): Long = filterIsInstance<JezConstant>().sumOf { it.source.size.toLong() }

    val currentLength = equation.u.getLength() + equation.v.getLength()
    return currentLength <= maxSolutionLength
}

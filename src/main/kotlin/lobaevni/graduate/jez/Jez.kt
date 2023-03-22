package lobaevni.graduate.jez

import io.github.rchowell.dotlin.DotRootGraph
import lobaevni.graduate.jez.JezHeuristics.assume
import lobaevni.graduate.jez.JezHeuristics.findSideContradictions
import lobaevni.graduate.jez.JezHeuristics.getSideConstants
import lobaevni.graduate.jez.JezHeuristics.tryShorten
import lobaevni.graduate.jez.action.ConstantsRepAction
import lobaevni.graduate.jez.action.VariablesDropAction
import lobaevni.graduate.jez.action.VariableRepAction

data class JezResult(
    val sigma: JezSigma,
    val isSolved: Boolean,
    val historyDotGraph: DotRootGraph?,
)

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
    )
    return state.tryFindMinimalSolution(allowRevert, maxIterationsCount)
}

/**
 * Tries to find minimal solution of [this.equation].
 */
internal fun JezState.tryFindMinimalSolution(
    allowRevert: Boolean,
    maxIterationsCount: Int,
): JezResult {
    sigmaLeft.clear()
    sigmaRight.clear()
    val variables = equation.getUsedVariables()
    for (variable in variables) {
        sigmaLeft[variable] = mutableListOf()
        sigmaRight[variable] = mutableListOf()
    }

    history?.init(equation)

    trySolveTrivial()

    var iteration = 0
    while (
        (equation.u.size > 1 || equation.v.size > 1) &&
        !equation.checkEmptySolution() &&
        iteration++ < maxIterationsCount
    ) {
        if (equation.findSideContradictions() && (!allowRevert || !revertUntilNoSolution())) break

        val currentEquation = equation

        blockCompNCr()
        trySolveTrivial()
        if (equation.checkEmptySolution()) break
        if (equation.findSideContradictions()) continue

        pairCompNCr()
        trySolveTrivial()
        if (equation.checkEmptySolution()) break
        if (equation.findSideContradictions()) continue

        pairCompCr()
        trySolveTrivial()
        if (equation.checkEmptySolution()) break
        if (equation.findSideContradictions()) continue

        if (equation == currentEquation && (!allowRevert || !revertUntilNoSolution())) break
    }

    val isSolved = equation.checkEmptySolution()
    val sigma: JezSigma = variables.associateWith { variable ->
        sigmaLeft[variable]!! + sigmaRight[variable]!!
    }

    if (isSolved) {
        apply(VariablesDropAction(equation.getUsedVariables()))
    }

    return JezResult(
        sigma = sigma,
        isSolved = isSolved,
        historyDotGraph = history?.dotRootGraph,
    )
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

    for (block in blocks) {
        apply(ConstantsRepAction(block, getOrGenerateConstant(block)))
    }
    return this
}

/**
 * Compression for a crossing pairs.
 */
internal fun JezState.pairCompCr(): JezState {
    val sideConstantsNCr = equation.getSideConstants()
    val usedConstants = equation.getUsedConstants()
    val leftConstantsCr = sideConstantsNCr.first.toList() + usedConstants.filterNot { constant ->
        sideConstantsNCr.first.contains(constant)
    }
    val rightConstantsCr = sideConstantsNCr.second.toList() + usedConstants.filterNot { constant ->
        sideConstantsNCr.second.contains(constant)
    }

    for (side in listOf(true, false)) { //true value for left side constants of the equation, false value for right
        val sideConstants = if (side) leftConstantsCr else rightConstantsCr
        val assumption = equation.assume(sideConstants, side)
        val variables = listOfNotNull(assumption?.first) + equation.getUsedVariables().filterNot { variable ->
            variable == assumption?.first
        }
        val constants = listOfNotNull(assumption?.second) + sideConstants.filterNot { constant ->
            constant == assumption?.second
        }
        for (variable in variables) {
            val curUsedConstants = equation.getUsedConstants().toSet()
            constants.firstOrNull { assumedConstant ->
                if (!curUsedConstants.contains(assumedConstant)) {
                    return@firstOrNull false
                }

                val action = VariableRepAction(
                    variable = variable,
                    leftRepPart = if (side) listOf(assumedConstant) else listOf(),
                    rightRepPart = if (side) listOf() else listOf(assumedConstant),
                )
                if (!apply(action)) {
                    return@firstOrNull false
                }

                if (equation.findSideContradictions()) {
                    assert(revert())
                    return@firstOrNull false
                }

                return@firstOrNull true
            }?.let {
                trySolveTrivial()
                if (equation.checkEmptySolution()) return this
            }
        }
    }
    return this
}

/**
 * Compression for non-crossing pairs.
 */
internal fun JezState.pairCompNCr(): JezState {
    val constants = equation.getSideConstants()
    for (a in constants.first) {
        for (b in constants.second) {
            if (a == b) continue //we don't compress blocks here

            val pair = listOf(a, b)
            if (!apply(ConstantsRepAction(pair, getOrGenerateConstant(pair)))) continue

            trySolveTrivial()

            if (equation.findSideContradictions() || equation.checkEmptySolution()) return this
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
        val revertedAction = history?.currentGraphNode?.action
        if (!revert()) return false

        when (revertedAction) {
            is VariableRepAction -> return true
            else -> {}
        }
    }
}

/**
 * Tries to find trivial solutions in current [JezEquation].
 * @return modified [JezEquation], if trivial solutions were successfully found, otherwise source [JezEquation] (but
 * shortened).
 */
internal fun JezState.trySolveTrivial() {
    tryShorten()

    if (equation.checkEmptySolution() || equation.u.size != 1 || equation.v.size != 1) return

    val uFirst = equation.u.first()
    val vFirst = equation.v.first()
    if (equation.u.size == 1 && uFirst is JezVariable && vFirst is JezConstant) {
        apply(VariableRepAction(uFirst, listOf(vFirst), listOf()))
    } else if (equation.v.size == 1 && vFirst is JezVariable && uFirst is JezConstant) {
        apply(VariableRepAction(vFirst, listOf(uFirst), listOf()))
    }
}

/**
 * @return whether an empty solution is suitable for this [JezEquation].
 */
fun JezEquation.checkEmptySolution(): Boolean =
    u.filterIsInstance<JezConstant>() == v.filterIsInstance<JezConstant>()

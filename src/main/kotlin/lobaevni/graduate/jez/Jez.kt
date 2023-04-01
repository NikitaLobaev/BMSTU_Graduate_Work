package lobaevni.graduate.jez

import lobaevni.graduate.jez.JezHeuristics.assume
import lobaevni.graduate.jez.JezHeuristics.checkSideContradictions
import lobaevni.graduate.jez.JezHeuristics.getSideConstants
import lobaevni.graduate.jez.JezHeuristics.tryShorten
import lobaevni.graduate.jez.action.ConstantsRepAction
import lobaevni.graduate.jez.action.VariablesDropAction
import lobaevni.graduate.jez.action.VariableRepAction

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

    var iteration = 0
    while (
        !equation.checkEmptySolution() &&
        iteration++ < maxIterationsCount
    ) {
        if (!tryShorten() || equation.checkSideContradictions()) {
            if (!allowRevert || !revertUntilNoSolution()) break
        }

        val currentEquation = equation

        blockCompNCr()
        if (!tryShorten()) continue
        if (equation.checkEmptySolution()) break
        if (equation.checkSideContradictions()) continue

        pairCompNCr()
        if (!tryShorten()) continue
        if (equation.checkEmptySolution()) break
        if (equation.checkSideContradictions()) continue

        pairCompCr(equation == currentEquation)
        if (!tryShorten()) continue
        if (equation.checkEmptySolution()) break
        if (equation.checkSideContradictions()) continue

        if (equation == currentEquation && (!allowRevert || !revertUntilNoSolution())) break
    }

    val sigma: JezSigma = variables.associateWith { variable ->
        sigmaLeft[variable]!! + sigmaRight[variable]!!
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
        apply(VariablesDropAction(equation.getUsedVariables()))
    }

    return JezResult(
        sigma = sigma,
        solutionState = solutionState,
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
 * Compression for non-crossing pairs.
 */
internal fun JezState.pairCompNCr(): JezState {
    val constants = equation.getSideConstants()
    for (a in constants.first) {
        for (b in constants.second) {
            if (a == b) continue //we don't compress blocks here

            val pair = listOf(a, b)
            if (!apply(ConstantsRepAction(pair, getOrGenerateConstant(pair)))) continue

            if (!tryShorten() || equation.checkSideContradictions() || equation.checkEmptySolution()) return this
        }
    }
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
        val action = VariableRepAction(
            variable = assumption.first,
            leftRepPart = if (left) listOf(assumption.second) else listOf(),
            rightRepPart = if (left) listOf() else listOf(assumption.second),
        )
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
            for (constant in equation.getUsedConstants()) {
                val action = VariableRepAction(
                    variable = variable,
                    leftRepPart = if (left) listOf(constant) else listOf(),
                    rightRepPart = if (left) listOf() else listOf(constant),
                )
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
        val revertedAction = history?.currentGraphNode?.action
        if (!revert()) return false

        when (revertedAction) {
            is VariableRepAction -> return true
            else -> {}
        }
    }
}

/**
 * @return whether an empty solution is suitable for this [JezEquation].
 */
fun JezEquation.checkEmptySolution(): Boolean = u.filterIsInstance<JezConstant>() == v.filterIsInstance<JezConstant>()

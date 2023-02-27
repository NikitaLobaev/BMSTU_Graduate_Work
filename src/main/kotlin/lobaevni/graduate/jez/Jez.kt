package lobaevni.graduate.jez

import io.github.rchowell.dotlin.DotRootGraph
import lobaevni.graduate.jez.JezHeuristics.findSideContradictions
import lobaevni.graduate.jez.JezHeuristics.getSideLetters
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
    allowRevert: Boolean = false,
    storeHistory: Boolean = allowRevert,
    dot: Boolean = false,
    dotHTMLLabels: Boolean = false,
    maxIterationsCount: Int = (u.size + v.size) * 2, //TODO: this value might be too small for some cases, increase it
): JezResult {
    val state = JezState(
        equation = this,
        storeHistory = storeHistory || allowRevert,
        dot = dot,
        dotHTMLLabels = dotHTMLLabels,
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
        val currentEquation = equation

        if (equation.findSideContradictions() && (!allowRevert || !revertUntilNoSolution())) break

        blockComp().trySolveTrivial()

        if (equation.checkEmptySolution()) break
        if (equation.findSideContradictions()) continue

        pairComp().trySolveTrivial()

        if (equation == currentEquation && (!allowRevert || !revertUntilNoSolution())) break
    }

    val isSolved = equation.checkEmptySolution()
    val sigma: JezSigma = variables.associateWith { variable ->
        sigmaLeft[variable]!! + sigmaRight[variable]!!
    }

    if (isSolved) {
        apply(VariablesDropAction(this))
    }

    return JezResult(
        sigma = sigma,
        isSolved = isSolved,
        historyDotGraph = history?.dotRootGraph,
    )
}

/**
 * Compressing blocks of letters.
 */
internal fun JezState.blockComp(): JezState {
    val blocks: MutableSet<List<JezConstant>> = mutableSetOf()
    for (equationPart in listOf(equation.u, equation.v)) {
        data class Acc(
            val element: JezElement,
            val count: Int,
        )

        (equationPart + stubGeneratedConstant).map { element ->
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
        apply(ConstantsRepAction(this, block))
    }
    return this
}

/**
 * Turning crossing pairs into non-crossing ones and compressing them.
 */
internal fun JezState.pairComp(): JezState {
    var constants = equation.getSideLetters()
    val usedConstants = equation.getUsedConstants().toMutableSet()
    usedConstants.removeAll(constants.first)
    val constantsLeft = constants.first + usedConstants.toList()
    usedConstants.addAll(constants.first)
    usedConstants.removeAll(constants.second)
    val constantsRight = constants.second + usedConstants.toList()
    pop(constantsLeft, constantsRight)

    constants = equation.getSideLetters()
    for (a in constants.first) {
        for (b in constants.second) {
            if (a == b) continue //we don't compress blocks here

            apply(ConstantsRepAction(this, listOf(a, b)))

            trySolveTrivial()

            if (equation.findSideContradictions() || equation.checkEmptySolution()) return this
        }
    }
    return this
}

/**
 * Compression for a crossing pairs.
 * @param constantsLeft constants, that might be on the left side of the crossing pair being replaced.
 * @param constantsRight similarly, constants, that might be on the right side.
 */
internal fun JezState.pop(
    constantsLeft: Collection<JezConstant>,
    constantsRight: Collection<JezConstant>,
): JezState {
    /**
     * Pops the specified [constant] from the [variable].
     * @param left if true, then pop [constant] from the left side of the [variable], otherwise from the right side.
     */
    fun JezEquationPart.popPart(
        variable: JezVariable,
        constant: JezConstant,
        left: Boolean,
    ): JezEquationPart {
        return map { element ->
            if (element == variable) {
                if (left) {
                    listOf(constant, variable)
                } else {
                    listOf(variable, constant)
                }
            } else {
                listOf(element)
            }
        }.flatten()
    }

    /**
     * Tries to assume which variable and constant could we use for popping first via checking prefixes (or postfixes,
     * according to [left] respectively).
     */
    fun JezEquation.assume(
        allowedConstants: Collection<JezConstant>,
        left: Boolean,
    ): Pair<JezVariable, JezConstant>? {
        val uElement: JezElement?
        val vElement: JezElement?
        if (left) {
            uElement = u.firstOrNull()
            vElement = v.firstOrNull()
        } else {
            uElement = u.lastOrNull()
            vElement = v.lastOrNull()
        }
        if (uElement is JezVariable && vElement is JezConstant && allowedConstants.contains(vElement)) {
            return Pair(uElement, vElement)
        } else if (vElement is JezVariable && uElement is JezConstant && allowedConstants.contains(uElement)) {
            return Pair(vElement, uElement)
        }
        return null
    }

    for (side in listOf(true, false)) { //true value for left side constants of the equation, false value for right
        val allowedConstants = if (side) constantsLeft else constantsRight
        val assumption = equation.assume(allowedConstants, side)
        val variables = listOfNotNull(assumption?.first) + equation.getUsedVariables().filterNot { variable ->
            variable == assumption?.first
        }
        val constants = listOfNotNull(assumption?.second) + allowedConstants.filterNot { constant ->
            constant == assumption?.second
        }
        for (variable in variables) {
            val usedConstants = equation.getUsedConstants().toSet()
            constants.filter { constant ->
                usedConstants.contains(constant)
            }.filterNot { assumedConstant ->
                history?.currentGraphNode?.childNodes?.find { childNode ->
                    childNode.action is VariableRepAction &&
                            ((side && childNode.action.leftRepPart == listOf(assumedConstant)) ||
                                    (!side && childNode.action.rightRepPart == listOf(assumedConstant)))
                } != null
            }.firstOrNull()?.let { assumedConstant ->
                val newPossibleEquation = equation.copy(
                    u = equation.u.popPart(variable, assumedConstant, side),
                    v = equation.v.popPart(variable, assumedConstant, side),
                )
                val action = VariableRepAction(
                    state = this,
                    variable = variable,
                    leftRepPart = if (side) listOf(assumedConstant) else listOf(),
                    rightRepPart = if (side) listOf() else listOf(assumedConstant),
                )
                if (!newPossibleEquation.findSideContradictions()) {
                    apply(action)

                    trySolveTrivial()

                    if (equation.findSideContradictions() || equation.checkEmptySolution()) return this else {}
                } else {
                    history?.putApplication(equation, action, newPossibleEquation, true)
                }
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
        apply(VariableRepAction(this, uFirst, listOf(vFirst), listOf()))
    } else if (equation.v.size == 1 && vFirst is JezVariable && uFirst is JezConstant) {
        apply(VariableRepAction(this, vFirst, listOf(uFirst), listOf()))
    }
}

/**
 * @return whether an empty solution is suitable for this [JezEquation].
 */
fun JezEquation.checkEmptySolution(): Boolean =
    u.filterIsInstance<JezConstant>() == v.filterIsInstance<JezConstant>()

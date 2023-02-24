package lobaevni.graduate.jez

import io.github.rchowell.dotlin.DotRootGraph
import lobaevni.graduate.jez.JezHeuristics.findSideContradictions
import lobaevni.graduate.jez.JezHeuristics.getSideLetters
import lobaevni.graduate.jez.JezHeuristics.tryShorten
import lobaevni.graduate.jez.action.ConstantsRepAction
import lobaevni.graduate.jez.action.VariableRepAction
import java.util.*

data class JezResult(
    val sigma: JezSigma,
    val isSolved: Boolean,
    val historyDotGraph: DotRootGraph?,
)

private val stubGeneratedConstant = JezGeneratedConstant(listOf())

/**
 * Tries to find minimal solution of this [JezEquation].
 */
fun JezEquation.tryFindMinimalSolution(
    storeHistory: Boolean = false,
    dot: Boolean = false,
    dotHTMLLabels: Boolean = false,
    maxIterationsCount: Int = (u.size + v.size) * 2, //TODO: this value might be too small for some cases, increase it
): JezResult {
    val state = JezState(
        equation = this,
        storeHistory = storeHistory,
        dot = dot,
        dotHTMLLabels = dotHTMLLabels,
    )
    return state.tryFindMinimalSolution(maxIterationsCount)
}

/**
 * Tries to find minimal solution of [this.equation].
 */
internal fun JezState.tryFindMinimalSolution(
    maxIterationsCount: Int,
): JezResult {
    sigma.clear()
    for (variable in equation.getUsedVariables()) {
        sigma[variable] = LinkedList()
    }

    history?.put(newEquation = equation)

    trySolveTrivial()

    var iteration = 0
    while (
        (equation.u.size > 1 || equation.v.size > 1) &&
        !equation.findSideContradictions() &&
        !equation.checkEmptySolution() &&
        iteration < maxIterationsCount
    ) {
        blockComp().trySolveTrivial()

        if (equation.findSideContradictions() || equation.checkEmptySolution()) break

        val constants = equation.getSideLetters()
        pairComp(constants.first, constants.second).trySolveTrivial()
        iteration++
    }

    val isSolved = equation.checkEmptySolution()

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
 * Turning crossing pairs from cartesian product [lettersLeft]*[lettersRight] into non-crossing ones and compressing
 * them.
 * @param lettersLeft constants, that might be on the left side of the crossing pair being replaced.
 * @param lettersRight similarly, constants, that might be on the right side.
 */
internal fun JezState.pairComp(
    lettersLeft: Collection<JezConstant>,
    lettersRight: Collection<JezConstant>,
): JezState {
    pop(lettersLeft, lettersRight)

    for (a in lettersLeft) {
        for (b in lettersRight) {
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
     * Tries to assume which constant could we use for popping via checking prefixes (or postfixes, according to [left]
     * respectively).
     */
    fun JezEquation.assumeConstant(
        variable: JezVariable,
        constants: Collection<JezConstant>,
        left: Boolean,
    ): JezConstant? {
        val uConstant: JezElement?
        val vConstant: JezElement?
        if (left) {
            uConstant = u.firstOrNull()
            vConstant = v.firstOrNull()
        } else {
            uConstant = u.lastOrNull()
            vConstant = v.lastOrNull()
        }
        if (uConstant == variable && vConstant is JezConstant && constants.contains(vConstant)) {
            return vConstant
        } else if (vConstant == variable && uConstant is JezConstant && constants.contains(uConstant)) {
            return uConstant
        }
        return constants.firstOrNull()
    }

    for (variable in equation.getUsedVariables()) {
        for (side in listOf(true, false)) { //true value for left side of the equation, false value for right
            val constant = equation.assumeConstant(
                variable = variable,
                constants = if (side) constantsLeft else constantsRight,
                left = side,
            )
            constant?.let {
                val newPossibleEquation = equation.copy(
                    u = equation.u.popPart(variable, constant, side),
                    v = equation.v.popPart(variable, constant, side),
                )
                val action = VariableRepAction(
                    state = this,
                    variable = variable,
                    leftRepPart = if (side) listOf(constant) else listOf(),
                    rightRepPart = if (side) listOf() else listOf(constant),
                )
                if (!newPossibleEquation.findSideContradictions()) {
                    apply(action)

                    trySolveTrivial()

                    if (equation.findSideContradictions() || equation.checkEmptySolution()) return this else {}
                } else {
                    history?.put(equation, action, newPossibleEquation, true)
                }
            }
        }
    }
    return this
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

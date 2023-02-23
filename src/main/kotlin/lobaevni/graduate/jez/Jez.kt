package lobaevni.graduate.jez

import io.github.rchowell.dotlin.DotRootGraph
import lobaevni.graduate.jez.JezHeuristics.findSideContradictions
import lobaevni.graduate.jez.JezHeuristics.getSideLetters
import lobaevni.graduate.jez.JezHeuristics.tryShorten
import lobaevni.graduate.jez.action.ConstantsRepAction
import lobaevni.graduate.jez.action.VariableRepAction

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
    storeHistory: Boolean,
    dotShortenLabels: Boolean,
    maxIterationsCount: Int = (u.size + v.size) * 2, //TODO: this value might be too small for some cases, increase it
): JezResult {
    val state = JezState(
        equation = this,
        storeHistory = storeHistory,
        dotShortenLabels = dotShortenLabels,
    )
    return state.tryFindMinimalSolution(maxIterationsCount)
}

/**
 * Tries to find minimal solution of [this.equation].
 */
internal fun JezState.tryFindMinimalSolution(
    maxIterationsCount: Int,
): JezResult {
    for (variable in equation.getUsedVariables()) {
        sigmaLeft.getOrPut(variable) { emptyList() }
        sigmaRight.getOrPut(variable) { emptyList() }
    }

    history?.putApplied(newEquation = equation,)

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

        val letters = equation.getSideLetters()
        pairComp(letters.first, letters.second).trySolveTrivial()
        iteration++
    }

    val isSolved = equation.checkEmptySolution()

    val sigma = sigmaLeft
    for (entry in sigmaRight) {
        sigma[entry.key] = sigma[entry.key]!! + entry.value.reversed()
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
 * Turning crossing pairs from cartesian product [lettersLeft]*[lettersRight] into non-crossing ones and compressing
 * them.
 * @param lettersLeft constants, that might be on the left side of the crossing pair being replaced.
 * @param lettersRight similarly, constants, that might be on the right side.
 */
internal fun JezState.pairComp(
    lettersLeft: Set<JezConstant>,
    lettersRight: Set<JezConstant>,
): JezState {
    pop(lettersLeft, lettersRight)

    for (a in lettersLeft) {
        for (b in lettersRight) {
            if (a == b) continue //we don't compress blocks here

            apply(ConstantsRepAction(this, listOf(a, b)))

            //history.addEquation(equation, "($a, $b) --> $gc")

            trySolveTrivial()

            if (equation.findSideContradictions() || equation.checkEmptySolution()) return this
        }
    }
    return this
}

/**
 * Compression for a crossing pairs.
 * @param lettersLeft constants, that might be on the left side of the crossing pair being replaced.
 * @param lettersRight similarly, constants, that might be on the right side.
 */
internal fun JezState.pop(
    lettersLeft: Set<JezConstant>,
    lettersRight: Set<JezConstant>,
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
     * Tries to assume which constant could we use for popping via checking prefixes (or postfixes, according to
     * [left] respectively).
     */
    fun JezEquation.assumeLetter(
        variable: JezVariable,
        letters: Set<JezConstant>,
        left: Boolean,
    ): JezConstant? {
        val uLetter: JezElement?
        val vLetter: JezElement?
        if (left) {
            uLetter = u.firstOrNull()
            vLetter = v.firstOrNull()
        } else {
            uLetter = u.lastOrNull()
            vLetter = v.lastOrNull()
        }
        if (uLetter == variable && vLetter is JezConstant && letters.contains(vLetter)) {
            return vLetter
        } else if (vLetter == variable && uLetter is JezConstant && letters.contains(uLetter)) {
            return uLetter
        }
        return letters.firstOrNull()
    }

    for (variable in equation.getUsedVariables()) {
        val firstLetter = equation.assumeLetter(variable, lettersRight, true)
        firstLetter?.let {
            val newPossibleEquation = equation.copy(
                u = equation.u.popPart(variable, firstLetter, true),
                v = equation.v.popPart(variable, firstLetter, true),
            )
            if (!newPossibleEquation.findSideContradictions()) {
                apply(VariableRepAction(this, variable, leftRepPart = listOf(firstLetter)))

                trySolveTrivial()

                if (equation.findSideContradictions() || equation.checkEmptySolution()) return this
            } else {
                //TODO: history.addEquation(newPossibleEquation, comment, false)
            }
        }

        val lastLetter = equation.assumeLetter(variable, lettersLeft, false)
        lastLetter?.let {
            val newPossibleEquation = equation.copy(
                u = equation.u.popPart(variable, lastLetter, false),
                v = equation.v.popPart(variable, lastLetter, false),
            )
            if (!newPossibleEquation.findSideContradictions()) {
                apply(VariableRepAction(this, variable, rightRepPart = listOf(lastLetter)))

                trySolveTrivial()

                if (equation.findSideContradictions() || equation.checkEmptySolution()) return this
            } else {
                //TODO: history.addEquation(newPossibleEquation, comment, false)
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

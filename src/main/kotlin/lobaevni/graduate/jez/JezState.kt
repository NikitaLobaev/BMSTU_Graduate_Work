package lobaevni.graduate.jez

import io.github.rchowell.dotlin.DotRootGraph
import lobaevni.graduate.jez.JezHeuristics.findSideContradictions
import lobaevni.graduate.jez.JezHeuristics.getSideLetters
import lobaevni.graduate.jez.JezHeuristics.tryShorten

data class JezResult(
    val sigma: JezSigma,
    val history: DotRootGraph,
    val isSolved: Boolean,
)

internal class JezState(
    var equation: JezEquation,
) {

    val sigmaLeft: JezSigma = mutableMapOf()
    val sigmaRight: JezSigma = mutableMapOf()
    val replaces: JezReplaces = mutableMapOf()
    val history: JezHistory = JezHistory()

    /**
     * Tries to find minimal solution of [equation].
     */
    internal fun tryFindMinimalSolution(
        maxIterationsCount: Int,
    ): JezResult {
        for (variable in equation.getUsedVariables()) {
            sigmaLeft.getOrPut(variable) { emptyList() }
            sigmaRight.getOrPut(variable) { emptyList() }
        }
        history.addEquation(equation)

        trySolveTrivial()

        var iteration = 0
        while ((equation.u.size > 1 || equation.v.size > 1) &&
            !equation.findSideContradictions() &&
            !equation.checkEmptySolution() &&
            iteration < maxIterationsCount) {

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
            sigma,
            history.graph,
            isSolved = isSolved,
        )
    }

    /**
     * Compressing blocks of letters.
     */
    internal fun blockComp(): JezState {
        for (constant in equation.getUsedConstants()) {
            equation = equation.copy(
                u = equation.u.blockCompNCr(this, constant),
                v = equation.v.blockCompNCr(this, constant),
            )

            history.addEquation(equation, "all blocks of $constant")

            trySolveTrivial()

            if (equation.findSideContradictions() || equation.checkEmptySolution()) break
        }
        return this
    }

    /**
     * Turning crossing pairs from cartesian product [lettersLeft]*[lettersRight] into non-crossing ones and compressing
     * them.
     * @param lettersLeft constants, that might be on the left side of the crossing pair being replaced.
     * @param lettersRight similarly, constants, that might be on the right side.
     */
    internal fun pairComp(
        lettersLeft: List<JezConstant>,
        lettersRight: List<JezConstant>,
    ): JezState {
        pop(lettersLeft, lettersRight)

        for (a in lettersLeft) {
            for (b in lettersRight) {
                if (a == b) continue //we don't compress blocks here

                val sourcePart = a.source + b.source
                val gc = addConstantsReplacement(sourcePart)
                equation = equation.copy(
                    u = equation.u.pairCompNCr(a, b, gc),
                    v = equation.v.pairCompNCr(a, b, gc),
                )

                history.addEquation(equation, "($a, $b) --> $gc")

                trySolveTrivial()

                if (equation.findSideContradictions() || equation.checkEmptySolution()) return this
            }
        }
        return this
    }

    /**
     * Pairs compression for a crossing pairs.
     * @param lettersLeft constants, that might be on the left side of the crossing pair being replaced.
     * @param lettersRight similarly, constants, that might be on the right side.
     */
    internal fun pop(
        lettersLeft: List<JezConstant>,
        lettersRight: List<JezConstant>,
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
            letters: List<JezConstant>,
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
                val comment = "$variable --> ($firstLetter, $variable)"
                if (!newPossibleEquation.findSideContradictions()) {
                    addVariablesReplacement(variable, listOf(firstLetter), true)
                    equation = newPossibleEquation

                    history.addEquation(equation, comment)

                    trySolveTrivial()

                    if (equation.findSideContradictions() || equation.checkEmptySolution()) return this
                } else {
                    history.addEquation(newPossibleEquation, comment, false)
                }
            }

            val lastLetter = equation.assumeLetter(variable, lettersLeft, false)
            lastLetter?.let {
                val newPossibleEquation = equation.copy(
                    u = equation.u.popPart(variable, lastLetter, false),
                    v = equation.v.popPart(variable, lastLetter, false),
                )
                val comment = "$variable --> ($variable, $lastLetter)"
                if (!newPossibleEquation.findSideContradictions()) {
                    addVariablesReplacement(variable, listOf(lastLetter), false)
                    equation = newPossibleEquation

                    history.addEquation(equation, comment)

                    trySolveTrivial()

                    if (equation.findSideContradictions() || equation.checkEmptySolution()) return this
                } else {
                    history.addEquation(newPossibleEquation, comment, false)
                }
            }
        }
        return this
    }

    /**
     * Pair compression for a non-crossing pair of [a] and [b] via replacing it occurrences to [gc].
     */
    internal fun JezEquationPart.pairCompNCr(
        a: JezConstant,
        b: JezConstant,
        gc: JezGeneratedConstant,
    ): JezEquationPart {
        assert(a != b) { "Couldn't replace block instead of a pair." }
        var result: JezEquationPart = zipWithNext().map { (element1, element2) ->
            if (element1 == a && element2 == b) {
                gc
            } else {
                element1
            }
        }.toMutableList()
        if (result.lastOrNull() != gc) {
            result = result + last()
        }

        return result
    }

    /**
     * Block compression for a letter [a] with no crossing block.
     */
    internal fun JezEquationPart.blockCompNCr( //TODO: should it be JezState.blockCompNCr instead?
        state: JezState,
        a: JezConstant,
    ): JezEquationPart {
        data class Acc(
            val total: List<JezElement>,
            val element: JezElement,
            val count: Int,
        )

        val newEquationPart = this
            .map { element ->
                Acc(listOf(), element, 1)
            }
            .reduce { lastAcc, currentAcc ->
                if (lastAcc.element == currentAcc.element) {
                    Acc(lastAcc.total, currentAcc.element, lastAcc.count + 1)
                } else {
                    if (lastAcc.element == a && lastAcc.count > 1) {
                        val repeatingPart = List(lastAcc.count) { a }
                        val newVariable = state.addConstantsReplacement(repeatingPart)
                        Acc((lastAcc.total + newVariable).toMutableList(), currentAcc.element, 1)
                    } else {
                        val repeatingPart = List(lastAcc.count) { lastAcc.element }
                        Acc((lastAcc.total + repeatingPart).toMutableList(), currentAcc.element, 1)
                    }
                }
            }.let { acc ->
                Acc(acc.total + acc.element, acc.element, 0)
            }.total
        return newEquationPart
    }

    /**
     * Tries to find trivial solutions in current [JezEquation].
     * @return modified [JezEquation], if trivial solutions were successfully found, otherwise source [JezEquation] (but
     * shortened).
     */
    internal fun trySolveTrivial() {
        tryShorten()

        if (equation.checkEmptySolution() || equation.u.size != 1 || equation.v.size != 1) return

        val uFirst = equation.u.first()
        val vFirst = equation.v.first()
        if (equation.u.size == 1 && uFirst is JezVariable) {
            val value = equation.v.filterIsInstance<JezConstant>().toJezSourceConstants()
            addVariablesReplacement(uFirst, value, true)
            equation = JezEquation(
                u = equation.u.replaceVariable(uFirst, value),
                v = equation.v.replaceVariable(uFirst, value),
            )
        } else if (equation.v.size == 1 && vFirst is JezVariable) {
            val value = equation.u.filterIsInstance<JezConstant>().toJezSourceConstants()
            addVariablesReplacement(vFirst, value, true)
            equation = JezEquation(
                u = equation.u.replaceVariable(vFirst, value),
                v = equation.v.replaceVariable(vFirst, value),
            )
        }
    }

    /**
     * Adds new replacement of [repPart] to the [JezReplaces]. Note that it doesn't replace [repPart] in the source
     * equation.
     * @return an existing generated constant for specified [repPart] or newly generated constant for that [repPart].
     */
    internal fun addConstantsReplacement(
        repPart: List<JezConstant>,
    ): JezGeneratedConstant {
        val repSourcePart = repPart.toJezSourceConstants()
        return replaces.getOrPut(repSourcePart) { JezGeneratedConstant(repPart, replaces.size) }
    }

    /**
     * Adds new replacement of [repPart] to the [variable] in sigma of this [JezState]. Note that it doesn't replace
     * [repPart] in the source equation.
     * @param left if true, then add replacement from the left side of the [variable], otherwise from the right side.
     */
    internal fun addVariablesReplacement(
        variable: JezVariable,
        repPart: List<JezConstant>,
        left: Boolean,
    ) {
        val sigma = if (left) sigmaLeft else sigmaRight
        sigma[variable] = sigma[variable]!! + repPart.toJezSourceConstants()
    }

    /**
     * Reveals source constants values and returns list of these [JezSourceConstant].
     */
    internal fun List<JezConstant>.toJezSourceConstants(): List<JezSourceConstant> {
        return map { constant ->
            constant.source
        }.flatten()
    }

}
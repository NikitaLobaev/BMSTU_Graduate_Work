package lobaevni.graduate.jez

import lobaevni.graduate.jez.action.CropAction

object JezHeuristics {

    /**
     * Heuristic of shortening of the [JezEquation]. Cuts similar starts and ends of the left and right side of the
     * [JezEquation].
     */
    internal fun JezState.tryShorten(): Boolean {
        val leftIndex = equation.u.zip(equation.v).indexOfFirst { (uElement, vElement) ->
            uElement != vElement
        }.takeIf { it != -1 } ?: minOf(equation.u.size, equation.v.size)

        val rightIndex = equation.u.reversed().zip(equation.v.reversed()).indexOfFirst { (uElement, vElement) ->
            uElement != vElement
        }.takeIf { it != -1 } ?: minOf(equation.u.size, equation.v.size)

        apply(CropAction(
            state = this,
            leftPart = equation.u.subList(0, leftIndex),
            rightPart = equation.v.subList(equation.v.size - rightIndex, equation.v.size),
        ))

        return leftIndex > 0 || rightIndex > 0
    }

    /**
     * Heuristic of finding contradictions in the [JezEquation] at left and at right sides of both parts of it.
     * @return true, if contradiction was found, false otherwise.
     */
    internal fun JezEquation.findSideContradictions(): Boolean {
        return (u.firstOrNull() is JezConstant && v.firstOrNull() is JezConstant && u.first() != v.first()) ||
                (u.lastOrNull() is JezConstant && v.lastOrNull() is JezConstant && u.last() != v.last()) ||
                (u.isEmpty() && v.find { it is JezConstant } != null) ||
                (v.isEmpty() && u.find { it is JezConstant } != null)
    }

    /**
     * Heuristic to determine what pairs of constants we might count as non-crossing.
     * @return pair of left and right constants lists respectively.
     */
    internal fun JezEquation.getSideLetters(): Pair<Set<JezConstant>, Set<JezConstant>> {
        fun JezEquationPart.findExcludedLetters(): Pair<Set<JezConstant>, Set<JezConstant>> {
            val lettersLeftExcluded = mutableSetOf<JezConstant>()
            val lettersRightExcluded = mutableSetOf<JezConstant>()
            forEachIndexed { index, element ->
                if (element !is JezConstant) {
                    return@forEachIndexed
                }

                if (index > 0 && elementAt(index - 1) is JezVariable) {
                    lettersRightExcluded += element
                }
                if (index + 1 < size && elementAt(index + 1) is JezVariable) {
                    lettersLeftExcluded += element
                }
            }
            return Pair(lettersLeftExcluded, lettersRightExcluded)
        }

        val letters = getUsedConstants()
        val uExcludedLetters = u.findExcludedLetters()
        val vExcludedLetters = v.findExcludedLetters()
        val leftLetters = letters.toMutableSet().apply {
            removeAll(uExcludedLetters.first)
            removeAll(vExcludedLetters.first)
        }
        val rightLetters = letters.toMutableSet().apply {
            removeAll(uExcludedLetters.second)
            removeAll(vExcludedLetters.second)
        }

        return Pair(leftLetters, rightLetters)
    }

}

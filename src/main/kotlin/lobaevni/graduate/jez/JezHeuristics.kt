package lobaevni.graduate.jez

private const val SHORTEN_HEUR_NAME = "shorten"

object JezHeuristics {

    /**
     * Heuristic of shortening of the [JezEquation]. Cuts similar starts and ends of the left and right side of the
     * [JezEquation].
     */
    internal fun JezState.tryShorten(): Boolean {
        val leftIndex = equation.u.zip(equation.v).indexOfFirst { (uElement, vElement) ->
            uElement != vElement
        }.takeIf { it != -1 } ?: minOf(equation.u.size, equation.v.size)
        equation = JezEquation(
            u = equation.u.drop(leftIndex),
            v = equation.v.drop(leftIndex),
        )

        val rightIndex = equation.u.reversed().zip(equation.v.reversed()).indexOfFirst { (uElement, vElement) ->
            uElement != vElement
        }.takeIf { it != -1 } ?: minOf(equation.u.size, equation.v.size)
        equation = JezEquation(
            u = equation.u.dropLast(rightIndex),
            v = equation.v.dropLast(rightIndex),
        )

        return if (leftIndex > 0 || rightIndex > 0) {
            history.addEquation(equation, SHORTEN_HEUR_NAME)
            true
        } else {
            false
        }
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
    internal fun JezEquation.getSideLetters(): Pair<List<JezConstant>, List<JezConstant>> { //TODO: shouldn't we return pair of sets instead of pair of lists?
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

        return Pair(leftLetters.toMutableList(), rightLetters.toMutableList())
    }

}

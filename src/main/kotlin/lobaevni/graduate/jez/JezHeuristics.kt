package lobaevni.graduate.jez

import lobaevni.graduate.jez.action.CropAction

object JezHeuristics {

    /**
     * Heuristic of shortening of the [JezEquation]. Cuts similar starts and ends of the left and right side of the
     * [JezEquation].
     * @return TODO
     */
    internal fun JezState.tryShorten(): Boolean {
        val leftIndex = equation.u.zip(equation.v).indexOfFirst { (uElement, vElement) ->
            uElement != vElement
        }.takeIf { it != -1 } ?: minOf(equation.u.size, equation.v.size)
        val rightIndex = equation.u.reversed().zip(equation.v.reversed()).indexOfFirst { (uElement, vElement) ->
            uElement != vElement
        }.takeIf { it != -1 } ?: minOf(equation.u.size, equation.v.size)
        return leftIndex + rightIndex == 0 || apply(CropAction(
            equation = equation,
            leftSize = leftIndex,
            rightSize = rightIndex,
        ))
    }

    /**
     * Heuristic of checking contradictions in the [JezEquation] at left and at right sides of both parts of it.
     * @return true, if contradiction was found, false otherwise.
     */
    internal fun JezEquation.checkSideContradictions(): Boolean {
        return (u.firstOrNull() is JezConstant && v.firstOrNull() is JezConstant && u.first() != v.first()) ||
                (u.lastOrNull() is JezConstant && v.lastOrNull() is JezConstant && u.last() != v.last()) ||
                (u.isEmpty() && v.find { it is JezConstant } != null) ||
                (v.isEmpty() && u.find { it is JezConstant } != null)
    }

    /**
     * Heuristic to determine what pairs of constants we might count as non-crossing.
     * @return pair of left and right constants lists respectively.
     */
    internal fun JezEquation.getSideConstants(): Pair<Set<JezConstant>, Set<JezConstant>> {
        fun JezEquationPart.findExcludedConstants(): Pair<Set<JezConstant>, Set<JezConstant>> {
            val constantsLeftExcluded = mutableSetOf<JezConstant>()
            val constantsRightExcluded = mutableSetOf<JezConstant>()
            forEachIndexed { index, element ->
                if (element !is JezConstant) return@forEachIndexed

                if (elementAtOrNull(index - 1) is JezVariable) {
                    constantsRightExcluded += element
                }
                if (elementAtOrNull(index + 1) is JezVariable) {
                    constantsLeftExcluded += element
                }
            }
            return Pair(constantsLeftExcluded, constantsRightExcluded)
        }

        val usedConstants = getUsedConstants()
        val uExcludedConstants = u.findExcludedConstants()
        val vExcludedConstants = v.findExcludedConstants()
        val leftConstants = usedConstants.toMutableSet().apply {
            removeAll(uExcludedConstants.first)
            removeAll(vExcludedConstants.first)
        }
        val rightConstants = usedConstants.toMutableSet().apply {
            removeAll(uExcludedConstants.second)
            removeAll(vExcludedConstants.second)
        }

        return Pair(leftConstants, rightConstants)
    }

    /**
     * Heuristic of assuming variable and constant that we could use for popping first via checking prefixes
     * (or postfixes, according to [left] respectively).
     */
    internal fun JezEquation.assume(
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

}

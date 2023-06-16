package lobaevni.graduate.jez

import lobaevni.graduate.jez.action.JezDropParametersAndVariablesAction
import lobaevni.graduate.jez.action.JezReplaceVariablesAction
import lobaevni.graduate.jez.data.*
import lobaevni.graduate.jez.data.JezConstant
import lobaevni.graduate.jez.data.JezGeneratedConstantBlock
import lobaevni.graduate.jez.data.JezState
import lobaevni.graduate.jez.data.JezVariable

object JezHeuristics {

    /**
     * Heuristic to determine what pairs of constants we might count as non-crossing.
     * @return pair of left and right constants lists respectively.
     */
    internal fun JezState.getSideConstants(): Pair<Set<JezConstant>, Set<JezConstant>> {
        fun JezEquationPart.findExcludedConstants(): Pair<Set<JezConstant>, Set<JezConstant>> {
            val constantsLeftExcluded = mutableSetOf<JezConstant>()
            val constantsRightExcluded = mutableSetOf<JezConstant>()
            forEachIndexed { index, element ->
                if (element !is JezConstant || element is JezGeneratedConstantBlock) return@forEachIndexed

                if (elementAtOrNull(index - 1) is JezVariable) {
                    constantsRightExcluded += element
                }
                if (elementAtOrNull(index + 1) is JezVariable) {
                    constantsLeftExcluded += element
                }
            }
            return Pair(constantsLeftExcluded, constantsRightExcluded)
        }

        val usedConstants = equation.getUsedSourceConstants() + equation.getUsedGeneratedConstants()
        val uExcludedConstants = equation.u.findExcludedConstants()
        val vExcludedConstants = equation.v.findExcludedConstants()
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
     * Heuristic of assuming variable and constant that we could use for popping first via checking prefixes and
     * suffixes.
     * @return true, if heuristic worked and some [JezReplaceVariablesAction] was successfully applied, false otherwise.
     */
    internal fun JezState.tryAssumeAndApply(): Boolean {
        val pairs = listOf( //Pair((y, A), left=true)
            Pair(Pair(equation.u.firstOrNull(), equation.v.firstOrNull()), true),
            Pair(Pair(equation.u.lastOrNull(), equation.v.lastOrNull()), false),
        )
            .map { pair ->
                if (pair.first.first !is JezVariable) {
                    Pair(Pair(pair.first.second, pair.first.first), pair.second)
                } else pair
            }
            .mapNotNull { pair ->
                Pair(Pair(
                    pair.first.first as? JezVariable ?: return@mapNotNull null,
                    pair.first.second as? JezConstant ?: return@mapNotNull null,
                ), pair.second)
            }
        val pair = pairs.firstOrNull() ?: return false
        val variable = pair.first.first
        if (apply(JezReplaceVariablesAction(
                variable,
                leftPart = if (pair.second) listOf(pair.first.second) else listOf(),
                rightPart = if (pair.second) listOf() else listOf(pair.first.second),
                oldNegativeSigmaLeft = negativeSigmaLeft?.toJezNegativeSigma()?.filterKeys { it == variable },
                oldNegativeSigmaRight = negativeSigmaRight?.toJezNegativeSigma()?.filterKeys { it == variable },
            ))) return true
        if (apply(JezDropParametersAndVariablesAction(
                replaces = mapOf(listOf(variable) to listOf()),
                indexes = mapOf(variable to Pair(
                    equation.u.getElementIndexes(variable),
                    equation.v.getElementIndexes(variable),
                )),
            ))) return true
        throw JezEquationNotConvergesException()
    }

}

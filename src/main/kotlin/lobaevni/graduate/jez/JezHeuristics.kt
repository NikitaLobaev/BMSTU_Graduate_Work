package lobaevni.graduate.jez

import lobaevni.graduate.jez.action.JezDropVariablesAction
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
     * @return TODO
     */
    internal fun JezState.tryAssumeAndApply(): Boolean {
        val pairs = listOf( //((y, A), left=true)
            Pair(Pair(equation.u.firstOrNull(), equation.v.firstOrNull()), true),
            Pair(Pair(equation.u.lastOrNull(), equation.v.lastOrNull()), false),
        ).map { pair ->
            if (pair.first.first !is JezVariable) {
                Pair(Pair(pair.first.second, pair.first.first), pair.second)
            } else pair
        }.mapNotNull { pair ->
            Pair(Pair(
                pair.first.first as? JezVariable ?: return@mapNotNull null,
                pair.first.second as? JezConstant ?: return@mapNotNull null,
            ), pair.second)
        }
        return pairs.any { pair ->
            val variable = pair.first.first
            if (!(apply(JezReplaceVariablesAction(
                    variable,
                    leftPart = if (pair.second) listOf(pair.first.second) else listOf(),
                    rightPart = if (pair.second) listOf() else listOf(pair.first.second),
                    oldNegativeSigmaLeft = negativeSigmaLeft?.toJezNegativeSigma()?.filterKeys { it == variable },
                    oldNegativeSigmaRight = negativeSigmaRight?.toJezNegativeSigma()?.filterKeys { it == variable },
                )) || apply(JezDropVariablesAction(
                    replaces = listOf(Pair(listOf(variable), listOf())),
                    indexes = mapOf(variable to Pair(
                        equation.u.withIndex().filter { it.value == variable }.map { it.index }.toSet(),
                        equation.v.withIndex().filter { it.value == variable }.map { it.index }.toSet(),
                    )),
                )))) {
                throw JezContradictionException()
            }
            return@any true
        }
    }

}

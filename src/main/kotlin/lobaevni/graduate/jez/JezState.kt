package lobaevni.graduate.jez

import lobaevni.graduate.jez.action.JezAction
import lobaevni.graduate.jez.history.JezHistory

internal class JezState(
    var equation: JezEquation,
    storeHistory: Boolean,
    storeEquations: Boolean,
    dot: Boolean,
    dotHTMLLabels: Boolean,
    dotMaxStatementsCount: Int,
) {

    val sigmaLeft: JezMutableSigma = mutableMapOf()
    val sigmaRight: JezMutableSigma = mutableMapOf()

    val replaces: JezReplaces = mutableMapOf()
    private var blocksCount: Int = 0

    val history: JezHistory? = if (storeHistory) {
        JezHistory(storeEquations, dot, dotHTMLLabels, dotMaxStatementsCount)
    } else null

    /**
     * @see JezAction.applyAction
     */
    fun apply(action: JezAction): Boolean {
        return action.applyAction(this)
    }

    /**
     * @see JezAction.revertAction
     */
    fun revert(): Boolean {
        return history?.currentGraphNode?.action?.revertAction(this) ?: false
    }

    fun getOrGenerateConstant(repPart: List<JezConstant>): JezGeneratedConstant {
        return replaces.getOrPut(repPart.toJezSourceConstants()) {
            val constant = JezGeneratedConstant(repPart, replaces.size - blocksCount)
            if (constant.isBlock) {
                blocksCount++
            }
            return constant
        }
    }

}

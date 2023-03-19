package lobaevni.graduate.jez

import lobaevni.graduate.jez.action.JezAction
import lobaevni.graduate.jez.history.JezHistory

internal class JezState(
    var equation: JezEquation,
    storeHistory: Boolean,
    dot: Boolean,
    dotHTMLLabels: Boolean,
) {

    val sigmaLeft: JezMutableSigma = mutableMapOf()
    val sigmaRight: JezMutableSigma = mutableMapOf()

    val replaces: JezReplaces = mutableMapOf()
    private var blocksCount: Int = 0

    val history: JezHistory? = if (storeHistory) JezHistory(dot, dotHTMLLabels) else null

    /**
     * @see JezAction.applyAction
     */
    fun apply(action: JezAction): Boolean {
        return action.applyAction()
    }

    /**
     * @see JezAction.revertAction
     */
    fun revert(): Boolean {
        return history?.currentGraphNode?.action?.revertAction() ?: false
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

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
    val history: JezHistory? = if (storeHistory) JezHistory(dot, dotHTMLLabels) else null

    /**
     * TODO @return @see...
     */
    fun apply(action: JezAction): Boolean {
        return action.applyAction()
    }

    /**
     * TODO @return @see...
     */
    fun revert(): Boolean {
        return history?.currentGraphNode?.action?.revertAction() ?: false
    }

    fun getOrGenerateConstant(repPart: List<JezConstant>): JezGeneratedConstant {
        return replaces.getOrPut(repPart.toJezSourceConstants()) { JezGeneratedConstant(repPart, replaces.size) }
    }

}

package lobaevni.graduate.jez

import lobaevni.graduate.jez.action.JezAction
import lobaevni.graduate.jez.history.JezHistory

internal class JezState(
    var equation: JezEquation,
    storeHistory: Boolean,
    dot: Boolean,
    dotHTMLLabels: Boolean,
) {

    val sigma: JezSigma = mutableMapOf()
    val replaces: JezReplaces = mutableMapOf()
    val history: JezHistory? = if (storeHistory) JezHistory(dot, dotHTMLLabels) else null

    fun apply(action: JezAction) {
        action.applyAction()
    }

    fun revert(action: JezAction) {
        action.revertAction()
    }

    fun getOrGenerateConstant(repPart: List<JezConstant>): JezGeneratedConstant {
        return replaces.getOrPut(repPart.toJezSourceConstants()) { JezGeneratedConstant(repPart, replaces.size) }
    }

}

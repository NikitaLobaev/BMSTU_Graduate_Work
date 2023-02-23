package lobaevni.graduate.jez

import lobaevni.graduate.jez.action.JezAction
import lobaevni.graduate.jez.history.JezHistory

internal class JezState(
    var equation: JezEquation,
    storeHistory: Boolean,
    dotShortenLabels: Boolean,
) {

    val sigmaLeft: JezSigma = mutableMapOf() //TODO: it's enough just one sigma with mutable bidirectional linked list
    val sigmaRight: JezSigma = mutableMapOf()
    val replaces: JezReplaces = mutableMapOf()
    val history: JezHistory? = if (storeHistory) JezHistory(dotShortenLabels) else null

    fun apply(action: JezAction) {
        action.applyAction()
    }

    //TODO: fun revert(action: JezAction)...

    fun getOrGenerateConstant(repPart: List<JezConstant>): JezGeneratedConstant {
        return replaces.getOrPut(repPart.toJezSourceConstants()) { JezGeneratedConstant(repPart, replaces.size) }
    }

}

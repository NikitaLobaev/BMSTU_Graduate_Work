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

    private val replaces: JezReplaces = mutableMapOf()
    private var replacedBlocksCount: Int = 0

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

    /**
     * @return [JezGeneratedConstant] for [repPart] from [replaces] or generates new constant, if it is not present.
     */
    fun getOrGenerateConstant(repPart: List<JezConstant>): JezGeneratedConstant {
        return replaces.getOrElse(repPart.toJezSourceConstants()) {
            val constant = JezGeneratedConstant(repPart, replaces.size - replacedBlocksCount)
            if (constant.isBlock) {
                replacedBlocksCount++
            }
            return constant
        }
    }

    /**
     * @return whether new [constant] was put successfully.
     */
    fun putGeneratedConstant(constant: JezGeneratedConstant): Boolean {
        if (constant.source in replaces) return false

        replaces[constant.source] = constant
        return true
    }

    /**
     * @return whether existing [constant] was removed successfully.
     */
    fun removeGeneratedConstant(constant: JezGeneratedConstant): Boolean {
        if (constant.source !in replaces) return false

        if (replaces[constant.source]!!.isBlock) {
            replacedBlocksCount--
        }
        return replaces.remove(constant.source) != null
    }

}

package lobaevni.graduate.jez

import lobaevni.graduate.jez.action.JezAction
import lobaevni.graduate.jez.history.JezHistory

class JezContradictionException : Exception()

internal class JezState(
    var equation: JezEquation,
    val allowRevert: Boolean,
    val allowBlockCompCr: Boolean,
    storeHistory: Boolean,
    storeEquations: Boolean,
    val heurExtNegRest: Boolean,
    dot: Boolean,
    dotHTMLLabels: Boolean,
    dotMaxStatementsCount: Int,
) {

    val sigmaLeft: JezMutableSigma = mutableMapOf()
    val sigmaRight: JezMutableSigma = mutableMapOf()

    val negativeSigmaLeft: JezMutableNegativeSigma? = if (heurExtNegRest) mutableMapOf() else null
    val negativeSigmaRight: JezMutableNegativeSigma? = if (heurExtNegRest) mutableMapOf() else null
    val nonEmptyVariables: MutableSet<JezVariable> = mutableSetOf()

    private val replaces: JezReplaces = mutableMapOf()
    private var generatedBlocksCount: Int = 0
    private var generatedBlocksPowersCount: Int = 0

    val history: JezHistory? = if (storeHistory) {
        JezHistory(storeEquations, dot, dotHTMLLabels, dotMaxStatementsCount)
    } else null

    init {
        val variables = equation.getUsedVariables()
        for (variable in variables) {
            sigmaLeft[variable] = mutableListOf()
            sigmaRight[variable] = mutableListOf()
            negativeSigmaLeft?.put(variable, mutableSetOf())
            negativeSigmaRight?.put(variable, mutableSetOf())
        }
        history?.init(equation)
    }

    /**
     * @see JezAction.applyAction
     */
    fun apply(action: JezAction): Boolean {
        if (history?.currentGraphNode?.childNodes?.containsKey(action) == true) return false
        return action.applyAction(this)
    }

    /**
     * @see JezAction.revertAction
     */
    fun revert(action: JezAction): Boolean {
        if (history?.currentGraphNode?.action != action) return false
        return action.revertAction(this)
    }

    /**
     * @return TODO
     */
    fun getOrPutGeneratedConstant(repPart: List<JezConstant>): JezGeneratedConstant {
        return replaces.getOrElse(repPart.toJezSourceConstants()) {
            val newConstant = JezGeneratedConstant(repPart, replaces.size - generatedBlocksCount)
            replaces[newConstant.source] = newConstant
            if (newConstant.isBlock) {
                generatedBlocksCount++
            }
            return newConstant
        }
    }

    /**
     * @return whether existing [constant] was removed successfully.
     */
    fun removeGeneratedConstant(constant: JezGeneratedConstant): Boolean {
        if (constant.source !in replaces) return false

        if (constant.isBlock) {
            generatedBlocksCount--
        }
        return replaces.remove(constant.source) != null
    }

    /**
     * TODO
     */
    fun generateConstantBlock(constant: JezConstant): JezGeneratedConstantBlock {
        return JezGeneratedConstantBlock(constant, generatedBlocksPowersCount++)
    }

}

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
    dot: Boolean,
    dotHTMLLabels: Boolean,
    dotMaxStatementsCount: Int,
) {

    val sigmaLeft: JezMutableSigma = mutableMapOf()
    val sigmaRight: JezMutableSigma = mutableMapOf()

    val negativeSigmaLeft: JezMutableNegativeSigma = mutableMapOf()
    val negativeSigmaRight: JezMutableNegativeSigma = mutableMapOf()
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
            negativeSigmaLeft[variable] = mutableSetOf()
            negativeSigmaRight[variable] = mutableSetOf()
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
     * @return [JezGeneratedConstant] for [repPart] from [replaces] or generates new constant, if it is not present.
     */
    fun getOrGenerateConstant(repPart: List<JezConstant>): JezGeneratedConstant {
        return replaces.getOrElse(repPart.toJezSourceConstants()) {
            JezGeneratedConstant(repPart, replaces.size - generatedBlocksCount)
        }
    }

    /**
     * @return whether new [constant] was put successfully.
     */
    fun putGeneratedConstant(constant: JezGeneratedConstant): Boolean {
        if (constant.source in replaces) return false

        replaces[constant.source] = constant
        if (constant.isBlock) {
            generatedBlocksCount++
        }
        return true
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

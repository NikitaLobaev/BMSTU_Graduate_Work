package lobaevni.graduate.jez.data

import lobaevni.graduate.jez.action.JezAction
import lobaevni.graduate.jez.checkEmptySolution
import lobaevni.graduate.jez.history.JezHistory

class JezEquationNotConvergesException : Exception()

internal class JezState(
    var equation: JezEquation,
    val allowRevert: Boolean,
    val allowBlockCompCr: Boolean,
    storeHistory: Boolean,
    storeEquations: Boolean,
    val heurExtNegRest: Boolean,
    val fullTraversal: Boolean,
    dot: Boolean,
    dotHTMLLabels: Boolean,
    dotMaxStatementsCount: Int?,
) {

    val sigmaLeft: JezMutableSigma = mutableMapOf()
    val sigmaRight: JezMutableSigma = mutableMapOf()

    val sigma: JezSigma
        get() = (sigmaLeft.keys + sigmaRight.keys)
            .associateWith { variable ->
                (sigmaLeft[variable]!! + sigmaRight[variable]!!).toJezSourceConstants()
            }

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
        assert(dotMaxStatementsCount == null || dotMaxStatementsCount >= 0)

        val variables = equation.getUsedVariables()
        variables.forEach { variable ->
            sigmaLeft[variable] = mutableListOf()
            sigmaRight[variable] = mutableListOf()
            negativeSigmaLeft?.put(variable, mutableSetOf())
            negativeSigmaRight?.put(variable, mutableSetOf())
        }
        history?.init(equation, converges = checkEmptySolution())
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
     * Retrieves existing [JezGeneratedConstant] for specified [repPart] or creates new.
     * @return [JezGeneratedConstant] for specified [repPart].
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
        if (constant.isBlock) generatedBlocksCount--
        return replaces.remove(constant.source) != null
    }

    /**
     * @return new [JezGeneratedConstantBlock] for specified [constant].
     */
    fun generateConstantBlock(constant: JezConstant): JezGeneratedConstantBlock =
        JezGeneratedConstantBlock(constant, generatedBlocksPowersCount++)

}

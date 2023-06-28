package lobaevni.graduate.jez.action

import lobaevni.graduate.Utils.prefixFunction
import lobaevni.graduate.Utils.replace
import lobaevni.graduate.jez.data.*

/**
 * Action of recompression.
 */
internal sealed class JezAction {

    /**
     * Tries to apply this [JezAction] to current [state].
     * @return true, if action was successfully applied, false otherwise.
     */
    internal abstract fun applyAction(state: JezState): Boolean

    /**
     * Tries to revert this [JezAction] in current [state].
     * @return false, if action was successfully reverted, false otherwise.
     */
    internal abstract fun revertAction(state: JezState): Boolean

    open fun toHTMLString(): String = toString()

    /**
     * Replaces all occurrences of [repPart] to [newPart] in current [JezEquation].
     * @return new [JezEquation] with corresponding replaces.
     */
    protected fun JezEquation.replace(
        repPart: List<JezElement>,
        newPart: List<JezElement>,
    ): JezEquation {
        if (repPart.isEmpty()) return this

        val repPartArray = ArrayList(repPart)
        val prefixFunctionValues = repPart.prefixFunction()
        return JezEquation(
            u = u.replace(repPartArray, newPart, prefixFunctionValues),
            v = v.replace(repPartArray, newPart, prefixFunctionValues),
        )
    }

}

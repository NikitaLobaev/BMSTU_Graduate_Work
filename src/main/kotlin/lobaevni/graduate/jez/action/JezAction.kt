package lobaevni.graduate.jez.action

import lobaevni.graduate.Utils.prefixFunction
import lobaevni.graduate.jez.*

/**
 * Action of recompression for [JezEquation].
 */
internal sealed class JezAction {

    /**
     * Tries to apply [JezAction] to [JezState] and adds corresponding record to
     * [lobaevni.graduate.jez.history.JezHistory].
     * @return true, if action was successfully applied, false otherwise.
     */
    internal abstract fun applyAction(state: JezState): Boolean

    /**
     * Tries to revert [JezAction] in [JezState] and adds corresponding record to
     * [lobaevni.graduate.jez.history.JezHistory].
     * @return false, if action was successfully reverted, false otherwise.
     */
    internal abstract fun revertAction(state: JezState): Boolean

    abstract override fun toString(): String //TODO: somewhy it doesn't forces to implement this method by inheritors, fix it

    open fun toHTMLString(): String {
        return toString()
    }

    /**
     * Replaces all occurrences of [repPart] to [newPart] in current [JezEquation].
     * @return new [JezEquation] with corresponding replaces.
     */
    protected fun JezEquation.replace(
        repPart: List<JezElement>,
        newPart: List<JezElement>,
    ): JezEquation {
        if (repPart.isEmpty()) return this

        val p = repPart.prefixFunction()
        return JezEquation(
            u = u.replace(repPart, newPart, p),
            v = v.replace(repPart, newPart, p),
        )
    }

    /**
     * @return [JezEquationPart] with specified [newPart] replacement of all [repPart] occurrences.
     */
    private fun JezEquationPart.replace( //TODO: try to rewrite this function in functional style
        repPart: List<JezElement>,
        newPart: List<JezElement>,
        p: Collection<Int>? = null,
    ): JezEquationPart {
        var k = 0
        var l = 0
        var result: MutableList<JezElement> = mutableListOf()
        while (k < size) {
            if (elementAt(k) == repPart[l]) {
                l++
                if (l == repPart.size) {
                    result = (result.dropLast(repPart.size - 1) + newPart).toMutableList() //TODO: create extension?
                    l = 0
                } else {
                    result += elementAt(k)
                }
                k++
            } else if (l == 0) {
                result += elementAt(k)
                k++
            } else {
                l = p?.elementAt(l - 1) ?: 0
            }
        }
        return result
    }

}

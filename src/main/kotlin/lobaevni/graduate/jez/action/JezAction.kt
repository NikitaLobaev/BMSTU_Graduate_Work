package lobaevni.graduate.jez.action

import lobaevni.graduate.Utils.prefixFunction
import lobaevni.graduate.jez.data.JezElement
import lobaevni.graduate.jez.data.JezEquation
import lobaevni.graduate.jez.data.JezEquationPart
import lobaevni.graduate.jez.data.JezState

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

    /**
     * @return [JezEquationPart] with specified [newPart] replacement of all [repPart] occurrences.
     */
    private fun JezEquationPart.replace(
        repPart: Collection<JezElement>,
        newPart: Collection<JezElement>,
        prefixFunctionValues: Collection<Int>? = null,
    ): JezEquationPart {
        if (isEmpty() || repPart.isEmpty() || repPart == newPart) return this

        data class Acc(
            val element: JezElement? = null,
            val buffer: MutableList<JezElement> = mutableListOf(),
            val result: MutableList<JezElement> = mutableListOf(),
        )

        val result = (listOf(Acc(null)) + map { element ->
            Acc(element)
        }).reduce { lastAcc, curAcc ->
            lastAcc.buffer.add(curAcc.element!!)

            val repPartElement = repPart.elementAt(lastAcc.buffer.size - 1)
            while (lastAcc.buffer.isNotEmpty() && lastAcc.buffer.last() != repPartElement) {
                val dropCount = lastAcc.buffer.size -
                        (prefixFunctionValues?.elementAt(lastAcc.buffer.size - 1) ?: 0)
                for (i in 0 until dropCount) {
                    lastAcc.result.add(lastAcc.buffer.removeFirst())
                }
            }

            if (lastAcc.buffer.size == repPart.size) {
                lastAcc.buffer.clear()
                lastAcc.result.addAll(newPart)
            }
            lastAcc
        }.also { acc ->
            acc.result.addAll(acc.buffer)
        }.result
        return result
    }

}

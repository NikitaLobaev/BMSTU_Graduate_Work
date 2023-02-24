package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.*

internal data class VariableRepAction(
    val state: JezState,
    val variable: JezVariable,
    val leftRepPart: List<JezConstant> = listOf(),
    val rightRepPart: List<JezConstant> = listOf(),
) : JezAction() {

    override fun applyAction(): Boolean {
        return action(true)
    }

    override fun revertAction(): Boolean {
        return action(false)
    }

    override fun toString(): String {
        return "$variable -> ${leftRepPart.convertToString()}$variable${rightRepPart.convertToString()}"
    }

    override fun toHTMLString(): String {
        return "${variable.toHTMLString()} &rarr; ${leftRepPart.convertToHTMLString()}${variable.toHTMLString()}${rightRepPart.convertToHTMLString()}"
    }

    /**
     * Applies or reverts current [JezAction]
     * @param apply true to apply current [JezAction], false to revert.
     */
    private fun action(apply: Boolean): Boolean {
        if (leftRepPart.isEmpty() && rightRepPart.isEmpty()) return false

        val repPart = leftRepPart + variable + rightRepPart
        val oldEquation = state.equation
        state.equation = if (apply) {
            oldEquation.replace(listOf(variable), repPart)
        } else {
            oldEquation.replace(repPart, listOf(variable))
        }

        val leftRepSourcePart = leftRepPart.toJezSourceConstants()
        val rightRepSourcePart = rightRepPart.toJezSourceConstants()
        if (apply) {
            leftRepSourcePart.reversed().forEach { constant ->
                state.sigma[variable]!!.addFirst(constant)
            }
            rightRepSourcePart.forEach { constant ->
                state.sigma[variable]!!.addLast(constant)
            }
        } else {
            for (i in leftRepSourcePart.indices) {
                state.sigma[variable]!!.removeFirst()
            }
            for (i in rightRepSourcePart.indices) {
                state.sigma[variable]!!.removeLast()
            }
        }

        state.history?.put(
            oldEquation = oldEquation,
            action = this,
            newEquation = state.equation,
            reversion = !apply,
        )
        return true
    }

}

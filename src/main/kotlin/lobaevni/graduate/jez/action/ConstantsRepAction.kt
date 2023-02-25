package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.*

internal data class ConstantsRepAction(
    val state: JezState,
    val repPart: List<JezConstant>,
    val constant: JezGeneratedConstant = state.getOrGenerateConstant(repPart),
) : JezAction() {

    override fun applyAction(): Boolean {
        return action(true)
    }

    override fun revertAction(): Boolean {
        return action(false)
    }

    override fun toString(): String {
        return "${repPart.convertToString()} -> $constant"
    }

    override fun toHTMLString(): String {
        return "${repPart.convertToHTMLString()} &rarr; ${constant.toHTMLString()}"
    }

    /**
     * Applies or reverts current [JezAction]
     * @param apply true to apply current [JezAction], false to revert.
     */
    private fun action(apply: Boolean): Boolean {
        if (repPart.isEmpty()) return false

        val oldEquation = state.equation
        state.equation = if (apply) {
            oldEquation.replace(repPart, listOf(constant))
        } else {
            oldEquation.replace(listOf(constant), repPart)
        }

        if (apply) {
            state.replaces[repPart.toJezSourceConstants()] = constant
        } else {
            state.replaces.remove(repPart.toJezSourceConstants())
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

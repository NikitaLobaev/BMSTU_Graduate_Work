package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.*

internal data class ConstantsRepAction(
    val repPart: List<JezConstant>,
    val constant: JezGeneratedConstant,
) : JezAction() {

    override fun applyAction(state: JezState): Boolean {
        if (repPart.isEmpty()) return false
        if (state.history?.currentGraphNode?.childNodes?.containsKey(this) == true) return false

        val oldEquation = state.equation
        val newEquation = oldEquation.replace(repPart, listOf(constant))

        if (oldEquation == newEquation) return false
        if (state.history?.graphNodes?.containsKey(newEquation) == true) {
            state.history.putApplication(
                oldEquation = oldEquation,
                action = this,
                newEquation = newEquation,
                ignored = true,
            )
            return false
        }

        if (!state.putGeneratedConstant(constant)) return false

        state.equation = newEquation

        state.history?.putApplication(
            oldEquation = oldEquation,
            action = this,
            newEquation = state.equation,
        )
        return true
    }

    override fun revertAction(state: JezState): Boolean {
        if (repPart.isEmpty()) return false

        val oldEquation = state.equation
        val newEquation = oldEquation.replace(listOf(constant), repPart)
        if (oldEquation == newEquation) return false

        if (!state.removeGeneratedConstant(constant)) return false

        state.equation = newEquation

        state.history?.putReversion(
            oldEquation = oldEquation,
            newEquation = state.equation,
        )
        return true
    }

    override fun toString(): String {
        return "${repPart.convertToString()} -> $constant"
    }

    override fun toHTMLString(): String {
        return "${repPart.convertToHTMLString()} &rarr; ${constant.toHTMLString()}"
    }

}

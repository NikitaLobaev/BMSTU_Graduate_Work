package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.*

internal data class ConstantsRepAction(
    val state: JezState,
    val repPart: List<JezConstant>,
    val constant: JezGeneratedConstant = state.getOrGenerateConstant(repPart),
) : JezAction() {

    override fun applyAction(): Boolean {
        if (repPart.isEmpty()) return false

        val oldEquation = state.equation
        state.equation = oldEquation.replace(repPart, listOf(constant))

        state.replaces[repPart.toJezSourceConstants()] = constant

        state.history?.putApplication(
            oldEquation = oldEquation,
            action = this,
            newEquation = state.equation,
        )
        return true
    }

    override fun revertAction(): Boolean {
        if (repPart.isEmpty()) return false

        val oldEquation = state.equation
        state.equation = oldEquation.replace(listOf(constant), repPart)

        state.replaces.remove(repPart.toJezSourceConstants())

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

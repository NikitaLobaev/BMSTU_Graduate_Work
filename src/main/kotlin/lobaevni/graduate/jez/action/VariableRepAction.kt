package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.*

internal data class VariableRepAction(
    val state: JezState,
    val variable: JezVariable,
    val leftRepPart: List<JezConstant> = listOf(),
    val rightRepPart: List<JezConstant> = listOf(),
) : JezAction() {

    override fun applyAction(): Boolean {
        if (leftRepPart.isEmpty() && rightRepPart.isEmpty()) return false

        val repPart = leftRepPart + variable + rightRepPart
        val oldEquation = state.equation
        state.equation = oldEquation.replace(listOf(variable), repPart)

        val leftRepSourcePart = leftRepPart.toJezSourceConstants()
        val rightRepSourcePart = rightRepPart.toJezSourceConstants()
        val sigmaLeftValue = state.sigmaLeft[variable]!!
        val sigmaRightValue = state.sigmaRight[variable]!!
        sigmaLeftValue += leftRepSourcePart
        sigmaRightValue += rightRepSourcePart

        state.history?.putApplication(
            oldEquation = oldEquation,
            action = this,
            newEquation = state.equation,
        )
        return true
    }

    override fun revertAction(): Boolean {
        if (leftRepPart.isEmpty() && rightRepPart.isEmpty()) return false

        val repPart = leftRepPart + variable + rightRepPart
        val oldEquation = state.equation
        state.equation = oldEquation.replace(repPart, listOf(variable))

        val leftRepSourcePart = leftRepPart.toJezSourceConstants()
        val rightRepSourcePart = rightRepPart.toJezSourceConstants()
        val sigmaLeftValue = state.sigmaLeft[variable]!!
        val sigmaRightValue = state.sigmaRight[variable]!!
        for (i in leftRepSourcePart.indices) {
            sigmaLeftValue.removeLast()
        }
        for (i in rightRepSourcePart.indices) {
            sigmaRightValue.removeLast()
        }

        state.history?.putReversion(
            oldEquation = oldEquation,
            newEquation = state.equation,
        )
        return true
    }

    override fun toString(): String {
        return "$variable -> ${leftRepPart.convertToString()}$variable${rightRepPart.convertToString()}"
    }

    override fun toHTMLString(): String {
        return "${variable.toHTMLString()} &rarr; ${leftRepPart.convertToHTMLString(false)}${variable.toHTMLString()}${rightRepPart.convertToHTMLString(false)}"
    }

}

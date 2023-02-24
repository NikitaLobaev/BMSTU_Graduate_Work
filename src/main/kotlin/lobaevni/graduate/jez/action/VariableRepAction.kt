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
        state.equation = JezEquation(
            u = state.equation.u.replaceVariable(variable, repPart),
            v = state.equation.v.replaceVariable(variable, repPart),
        )

        state.sigmaLeft[variable] = state.sigmaLeft[variable]!! + leftRepPart.toJezSourceConstants()
        state.sigmaRight[variable] = state.sigmaRight[variable]!! + rightRepPart.toJezSourceConstants()

        state.history?.putApplication(
            oldEquation = oldEquation,
            action = this,
            newEquation = state.equation,
        )
        return true
    }

    override fun revertAction(): Boolean {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return "$variable -> ${leftRepPart.convertToString()}$variable${rightRepPart.convertToString()}"
    }

    override fun toHTMLString(): String {
        return "${variable.toHTMLString()} &rarr; ${leftRepPart.convertToHTMLString()}${variable.toHTMLString()}${rightRepPart.convertToHTMLString()}"
    }

    /**
     * @return [JezEquationPart] with specified [repPart] replacement of [variable].
     */
    private fun JezEquationPart.replaceVariable(
        variable: JezVariable,
        repPart: List<JezElement>,
    ): JezEquationPart {
        return map { element ->
            if (element == variable) {
                repPart
            } else {
                listOf(element)
            }
        }.flatten()
    }

}

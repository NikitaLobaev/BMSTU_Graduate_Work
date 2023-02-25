package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.*
import lobaevni.graduate.jez.JezState

internal data class VariablesDropAction(
    val state: JezState,
) : JezAction() {

    private val variables = state.equation.getUsedVariables()

    override fun applyAction(): Boolean {
        if (variables.isEmpty()) return false

        val oldEquation = state.equation
        state.equation = JezEquation(
            u = oldEquation.u.filterIsInstance<JezConstant>(),
            v = oldEquation.v.filterIsInstance<JezConstant>(),
        )

        state.history?.putApplication(
            oldEquation,
            this,
            state.equation,
        )
        return true
    }

    override fun revertAction(): Boolean {
        throw NotImplementedError()
    }

    override fun toString(): String {
        return "empty substitution"
    }

    override fun toHTMLString(): String {
        return "&epsilon;-subst"
    }

}

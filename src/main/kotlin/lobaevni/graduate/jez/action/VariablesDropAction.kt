package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.*
import lobaevni.graduate.jez.JezState

internal data class VariablesDropAction(
    val variables: Set<JezVariable>,
) : JezAction() {

    override fun applyAction(state: JezState): Boolean {
        if (variables.isEmpty()) return false

        val oldEquation = state.equation
        val newEquation = JezEquation(
            u = oldEquation.u.filterNot { variables.contains(it) },
            v = oldEquation.v.filterNot { variables.contains(it) },
        )
        if (oldEquation == newEquation) return false

        state.equation = newEquation

        state.history?.putApplication(
            oldEquation,
            this,
            state.equation,
        )
        return true
    }

    override fun revertAction(state: JezState): Boolean {
        throw NotImplementedError()
    }

    override fun toString(): String {
        return "empty substitution"
    }

    override fun toHTMLString(): String {
        return "&epsilon;-subst"
    }

}

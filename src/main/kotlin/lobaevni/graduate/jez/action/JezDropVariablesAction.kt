package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.data.*
import lobaevni.graduate.jez.data.JezState
import lobaevni.graduate.jez.data.JezVariable

internal data class JezDropVariablesAction(
    override val replaces: Collection<Pair<List<JezVariable>, List<JezElement>>>,
    private val indexes: Map<JezVariable, Pair<Set<Int>, Set<Int>>>? = null,
) : JezReplaceAction() {

    init {
        assert(replaces.all { it.second.isEmpty() })
    }

    override fun applyAction(state: JezState): Boolean {
        /**
         * TODO
         */
        fun JezEquationPart.validateIndexes(variable: JezVariable, variableIndexes: Set<Int>): Boolean =
            withIndex().all {
                if (it.value == variable) {
                    variableIndexes.contains(it.index)
                } else {
                    !variableIndexes.contains(it.index)
                }
            }

        if (replaces.any { it.first.any { variable -> state.nonEmptyVariables.contains(variable) } } ||
            indexes?.any { !state.equation.u.validateIndexes(it.key, it.value.first) } == true ||
            indexes?.any { !state.equation.v.validateIndexes(it.key, it.value.second) } == true) return false

        return super.applyAction(state)
    }

    //TODO: duplicate of code
    override fun revertAction(state: JezState): Boolean {
        /**
         * TODO
         */
        fun JezEquationPart.insertVariable(variable: JezVariable, indexes: Set<Int>): JezEquationPart =
            toMutableList().apply {
                indexes.forEach { index ->
                    add(index, variable) //TODO: optimize
                }
            }

        if (indexes == null) return false

        val oldEquation = state.equation
        var newEquation = oldEquation
        for ((variable, indexes) in indexes) {
            newEquation = JezEquation(
                u = newEquation.u.insertVariable(variable, indexes.first),
                v = newEquation.v.insertVariable(variable, indexes.second),
            )
        }

        if (oldEquation == newEquation) return false

        state.equation = newEquation

        state.history?.putReversion(
            oldEquation = oldEquation,
            newEquation = state.equation,
        )
        return true
    }

}

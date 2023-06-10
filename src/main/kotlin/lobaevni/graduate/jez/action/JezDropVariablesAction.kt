package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.data.*

internal data class JezDropVariablesAction(
    override val replaces: Collection<Pair<List<JezVariable>, List<JezElement>>>,
    private val indexes: Map<JezVariable, Pair<Set<Int>, Set<Int>>>,
) : JezReplaceAction() {

    constructor(
        variables: List<JezVariable>, //list, because we require deterministic order for revert to become possible
        indexes: Map<JezVariable, Pair<Set<Int>, Set<Int>>>,
    ) : this(
        replaces = variables.map { variable ->
            Pair(listOf(variable), listOf())
        },
        indexes,
    )

    init {
        assert(replaces.all { it.second.isEmpty() })
    }

    override fun applyAction(state: JezState): Boolean {
        /**
         * @return true, if [variableIndexes] exactly represents all indexes of [variable] in this [JezEquationPart],
         * false otherwise.
         */
        fun JezEquationPart.validateIndexes(variable: JezVariable, variableIndexes: Set<Int>): Boolean = this
            .withIndex()
            .all {
                if (it.value == variable) {
                    variableIndexes.contains(it.index)
                } else {
                    !variableIndexes.contains(it.index)
                }
            }

        if (replaces.any { it.first.any { variable -> state.nonEmptyVariables.contains(variable) } } ||
            indexes.any { !state.equation.u.validateIndexes(it.key, it.value.first) } ||
            indexes.any { !state.equation.v.validateIndexes(it.key, it.value.second) }) return false

        return super.applyAction(state)
    }

    override fun revertAction(state: JezState): Boolean {
        fun JezEquationPart.insertVariables(variables: Map<JezVariable, Set<Int>>): JezEquationPart {
            val equationPartArray = Array<JezElement?>(size + variables.values.sumOf { indexes -> indexes.size }) {
                null
            }
            variables.forEach { entry ->
                entry.value.forEach { index ->
                    equationPartArray[index] = entry.key
                }
            }

            val iterator = iterator()
            val result = equationPartArray
                .mapNotNull { element ->
                    if (element != null) return@mapNotNull element
                    assert(iterator.hasNext())
                    return@mapNotNull iterator.next()
                }
            assert(!iterator.hasNext())
            return result.toList()
        }

        val oldEquation = state.equation
        val newEquation = JezEquation(
            u = oldEquation.u.insertVariables(indexes.mapValues { entry -> entry.value.first }),
            v = oldEquation.v.insertVariables(indexes.mapValues { entry -> entry.value.second }),
        )

        if (oldEquation == newEquation) return false

        state.equation = newEquation

        state.history?.putReversion(
            oldEquation = oldEquation,
            newEquation = state.equation,
        )
        return true
    }

}

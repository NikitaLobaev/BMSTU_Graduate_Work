package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.data.*

internal data class JezDropParametersAndVariablesAction(
    override val replaces: Collection<Pair<List<JezElement>, List<JezElement>>>,
    private val indexes: Map<JezElement, Pair<Set<Int>, Set<Int>>>,
) : JezReplaceAction() {

    constructor(
        elements: Set<JezElement>, //TODO: если сломается, заменить на List
        indexes: Map<JezElement, Pair<Set<Int>, Set<Int>>>,
    ) : this(
        replaces = elements.map { variable ->
            Pair(listOf(variable), listOf())
        },
        indexes,
    )

    init {
        //assert(replaces.all { it.second.isEmpty() }) //TODO
    }

    override fun applyAction(state: JezState): Boolean {
        /**
         * @return true, if [elementIndexes] exactly represents all indexes of [element] in this [JezEquationPart],
         * false otherwise.
         */
        fun JezEquationPart.validateIndexes(element: JezElement, elementIndexes: Set<Int>): Boolean = this
            .withIndex()
            .all {
                if (it.value == element) {
                    elementIndexes.contains(it.index)
                } else {
                    !elementIndexes.contains(it.index)
                }
            }

        if (indexes.any { !state.equation.u.validateIndexes(it.key, it.value.first) } ||
            indexes.any { !state.equation.v.validateIndexes(it.key, it.value.second) }) return false

        return super.applyAction(state)
    }

    override fun revertAction(state: JezState): Boolean {
        fun JezEquationPart.insertElements(elements: Map<JezElement, Set<Int>>): JezEquationPart {
            val equationPartArray = Array<JezElement?>(size + elements.values.sumOf { indexes -> indexes.size }) {
                null
            }
            elements.forEach { entry ->
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
            u = oldEquation.u.insertElements(indexes.mapValues { entry -> entry.value.first }),
            v = oldEquation.v.insertElements(indexes.mapValues { entry -> entry.value.second }),
        )

        if (oldEquation == newEquation) return false

        state.equation = newEquation

        state.history?.putReversion(
            oldEquation = oldEquation,
            newEquation = state.equation,
        )
        return true
    }

    override fun toString(): String = super.toString()

    override fun toHTMLString(): String {
        val generatedConstantBlocksStr = replaces
            .filter { replace ->
                replace.first.first() is JezGeneratedConstantBlock && replace.second.isNotEmpty()
            }
            .joinToString(", ") { (from, to) ->
                "i<sub>${(from.first() as JezGeneratedConstantBlock).powerIndex}</sub> &rarr; ${(to.first() as JezGeneratedConstant).number}"
            }
        val variablesStr = replaces
            .filter { replace ->
                replace.first.first() is JezVariable
            }
            .joinToString(", ") { (from, _) ->
                from.convertToHTMLString()
            } + " &rarr; &epsilon;"
        return listOf(generatedConstantBlocksStr, variablesStr)
            .filter { str ->
                str.isNotEmpty()
            }
            .joinToString(", ")
    }

}

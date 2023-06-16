package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.data.*

internal data class JezDropParametersAndVariablesAction(
    override val replaces: Map<List<JezElement>, List<JezElement>>,
    private val indexes: Map<JezElement, Pair<Set<Int>, Set<Int>>>,
) : JezReplaceAction() {

    constructor(
        elements: Set<JezElement>,
        indexes: Map<JezElement, Pair<Set<Int>, Set<Int>>>,
    ) : this(
        replaces = elements.associate { variable ->
            Pair(listOf(variable), listOf())
        },
        indexes,
    )

    init { //TODO
        //assert(replaces.all { it.second.isEmpty() })
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
        fun JezEquationPart.insertElements(
            elements: Map<JezElement, Set<Int>>,
        ): JezEquationPart {
            val equationPartArray = Array<JezElement?>(
                size = size + elements.values.sumOf { indexes -> indexes.size },
            ) { null }
            val skipIndexes: MutableSet<Int> = mutableSetOf()
            elements.forEach { entry ->
                entry.value.forEach { index ->
                    equationPartArray[index] = entry.key
                    if (replaces[listOf(entry.key)]!!.isNotEmpty()) {
                        skipIndexes.add(index)
                    }
                }
            }

            val iterator = iterator()
            val result = equationPartArray
                .dropLast(skipIndexes.size)
                .mapIndexedNotNull { index, element ->
                    if (element != null) {
                        if (skipIndexes.contains(index)) {
                            iterator.next()
                        }
                        return@mapIndexedNotNull element
                    }
                    assert(iterator.hasNext())
                    return@mapIndexedNotNull iterator.next()
                }
            assert(!iterator.hasNext())
            return result
        }

        val oldEquation = state.equation
        val newEquation = JezEquation(
            u = oldEquation.u.insertElements(
                elements = indexes.mapValues { entry -> entry.value.first },
            ),
            v = oldEquation.v.insertElements(
                elements = indexes.mapValues { entry -> entry.value.second },
            ),
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
                replace.key.first() is JezGeneratedConstantBlock && replace.value.isNotEmpty()
            }
            .entries
            .joinToString(", ") { (from, to) ->
                "i<sub>${(from.first() as JezGeneratedConstantBlock).powerIndex}</sub> &rarr; ${(to.first() as JezGeneratedConstant).number}"
            }
        val variablesStr = replaces
            .filter { replace ->
                replace.key.first() is JezVariable
            }
            .entries
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

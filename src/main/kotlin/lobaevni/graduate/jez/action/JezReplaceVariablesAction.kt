package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.*

internal data class JezReplaceVariablesAction(
    override val replaces: Collection<Pair<List<JezVariable>, List<JezElement>>>,
    val subNegativeSigmaLeft: JezNegativeSigma = mapOf(),
    val subNegativeSigmaRight: JezNegativeSigma = mapOf(),
    val subNonEmptyVariables: List<JezVariable> = listOf(),
) : JezReplaceAction() {

    constructor(
        variable: JezVariable,
        leftPart: List<JezConstant> = listOf(),
        rightPart: List<JezConstant> = listOf(),
        subNegativeSigmaLeft: JezNegativeSigma = mapOf(),
        subNegativeSigmaRight: JezNegativeSigma = mapOf(),
        subNonEmptyVariables: List<JezVariable> = listOf(),
    ) : this(
        replaces = listOf(Pair(listOf(variable), leftPart + listOf(variable) + rightPart)),
        subNegativeSigmaLeft,
        subNegativeSigmaRight,
        subNonEmptyVariables,
    )

    init {
        assert(replaces.all { (from, to) ->
            val variables = to.filterIsInstance<JezVariable>()
            from.size == 1 && variables.size <= 1 && variables.all { it == from.first() }
        })
    }

    override fun applyAction(state: JezState): Boolean {
        if (replaces.any { (from, to) ->
                val first = (to.firstOrNull() as? JezConstant)?.source?.first()
                val last = (to.lastOrNull() as? JezConstant)?.source?.last()
                (first != null && state.negativeSigmaLeft[from.first()]!!.contains(first)) ||
                        (last != null && state.negativeSigmaRight[from.last()]!!.contains(last))
        }) return false

        if (!super.applyAction(state)) return false

        replaces.forEach { (from, to) ->
            val variable = from.first()
            val (leftReplacedPart, rightReplacedPart) = getReplacedParts(variable, to)
            state.sigmaLeft[variable]!!.addAll(leftReplacedPart)
            state.sigmaRight[variable]!!.addAll(rightReplacedPart)

            leftReplacedPart.firstOrNull()?.let { constant ->
                state.negativeSigmaLeft[variable]!!.clear()
                state.negativeSigmaLeft[variable]!!.add(constant.source.first())
            }
            rightReplacedPart.lastOrNull()?.let { constant ->
                state.negativeSigmaRight[variable]!!.clear()
                state.negativeSigmaLeft[variable]!!.add(constant.source.last())
            }
            if (leftReplacedPart.isNotEmpty() || rightReplacedPart.isNotEmpty()) {
                state.nonEmptyVariables.remove(variable)
            }
        }
        return true
    }

    override fun revertAction(state: JezState): Boolean {
        if (!super.revertAction(state)) return false

        replaces.forEach { (from, to) ->
            val variable = from.first()
            val (leftReplacedPart, rightReplacedPart) = getReplacedParts(variable, to)
            leftReplacedPart.indices.forEach { _ -> state.sigmaLeft[variable]!!.removeLast() }
            rightReplacedPart.indices.forEach { _ -> state.sigmaRight[variable]!!.removeLast() }

            state.negativeSigmaLeft[variable]!!.apply {
                clear()
                subNegativeSigmaLeft[variable]?.let { sourceConstants ->
                    addAll(sourceConstants)
                }
                leftReplacedPart.firstOrNull()?.source?.first()?.let { sourceConstant ->
                    add(sourceConstant)
                }
            }
            state.negativeSigmaRight[variable]!!.apply {
                clear()
                subNegativeSigmaRight[variable]?.let { sourceConstants ->
                    addAll(sourceConstants)
                }
                rightReplacedPart.lastOrNull()?.source?.last()?.let { sourceConstant ->
                    add(sourceConstant)
                }
            }
            state.nonEmptyVariables.remove(variable)
        }

        state.nonEmptyVariables.addAll(subNonEmptyVariables)

        return true
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) || (other as? JezReplaceVariablesAction)?.replaces == replaces
    }

    override fun hashCode(): Int {
        return replaces.hashCode()
    }

    private fun getReplacedParts(
        variable: JezVariable,
        replacedPart: JezEquationPart,
    ): Pair<List<JezConstant>, List<JezConstant>> {
        val variableIndex = replacedPart.indexOf(variable).takeIf { it != -1 } ?: replacedPart.size
        val leftReplacedPart = replacedPart.subList(0, variableIndex)
        val rightReplacedPart = replacedPart.subList(minOf(replacedPart.size, variableIndex + 1), replacedPart.size)
        return Pair(leftReplacedPart.map { it as JezConstant }, rightReplacedPart.map { it as JezConstant })
    }

}

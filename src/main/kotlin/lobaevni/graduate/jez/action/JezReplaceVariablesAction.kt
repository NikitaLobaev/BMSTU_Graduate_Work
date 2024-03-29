package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.data.*

internal data class JezReplaceVariablesAction(
    override val replaces: Map<List<JezVariable>, List<JezElement>>,
    val oldNegativeSigmaLeft: JezNegativeSigma? = null,
    val oldNegativeSigmaRight: JezNegativeSigma? = null,
) : JezReplaceAction() {

    constructor(
        variable: JezVariable,
        leftPart: List<JezConstant> = listOf(),
        rightPart: List<JezConstant> = listOf(),
        oldNegativeSigmaLeft: JezNegativeSigma? = null,
        oldNegativeSigmaRight: JezNegativeSigma? = null,
    ) : this(
        replaces = mapOf(listOf(variable) to leftPart + listOf(variable) + rightPart),
        oldNegativeSigmaLeft,
        oldNegativeSigmaRight,
    )

    init {
        assert(replaces.all { (from, to) ->
            val variables = to.filterIsInstance<JezVariable>()
            from.size == 1 && variables.size == 1 && variables.first() == from.first()
        })
    }

    override fun applyAction(state: JezState): Boolean {
        if (replaces.any { (from, to) ->
                val first = (to.firstOrNull() as? JezConstant)?.source?.first()
                val last = (to.lastOrNull() as? JezConstant)?.source?.last()
                (first != null && state.negativeSigmaLeft?.get(from.first())?.contains(first) == true) ||
                        (last != null && state.negativeSigmaRight?.get(from.last())?.contains(last) == true)
        }) return false

        if (!super.applyAction(state)) return false

        replaces.forEach { (from, to) ->
            val variable = from.first()
            val (leftReplacedPart, rightReplacedPart) = getReplacedParts(variable, to)
            state.sigmaLeft[variable]!!.addAll(leftReplacedPart)
            state.sigmaRight[variable]!!.addAll(rightReplacedPart)

            state.negativeSigmaLeft?.get(variable)?.apply {
                leftReplacedPart.firstOrNull()?.source?.firstOrNull()?.let { constant ->
                    clear()
                    add(constant)
                }
            }
            state.negativeSigmaRight?.get(variable)?.apply {
                rightReplacedPart.lastOrNull()?.source?.lastOrNull()?.let { constant ->
                    clear()
                    add(constant)
                }
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

            state.negativeSigmaLeft?.get(variable)?.apply {
                if (leftReplacedPart.isNotEmpty()) {
                    clear()
                    oldNegativeSigmaLeft?.get(variable)?.let { oldNegativeSigma ->
                        addAll(oldNegativeSigma)
                    }
                    leftReplacedPart.firstOrNull()?.source?.firstOrNull()?.let { constant ->
                        add(constant)
                    }
                }
            }
            state.negativeSigmaRight?.get(variable)?.apply {
                if (rightReplacedPart.isNotEmpty()) {
                    clear()
                    oldNegativeSigmaRight?.get(variable)?.let { oldNegativeSigma ->
                        addAll(oldNegativeSigma)
                    }
                    rightReplacedPart.lastOrNull()?.source?.lastOrNull()?.let { constant ->
                        add(constant)
                    }
                }
            }
        }

        return true
    }

    override fun toString(): String = super.toString()

    /**
     * @return pair with left and right parts of [variable] value in this [JezReplaceVariablesAction]].
     */
    private fun getReplacedParts(
        variable: JezVariable,
        replacedPart: JezEquationPart,
    ): Pair<List<JezConstant>, List<JezConstant>> {
        val variableIndex = replacedPart.indexOf(variable).takeIf { it != -1 } ?: replacedPart.size
        val leftReplacedPart = replacedPart.subList(0, variableIndex)
        val rightReplacedPart = replacedPart.subList(minOf(replacedPart.size, variableIndex + 1), replacedPart.size)
        assert(leftReplacedPart.size + rightReplacedPart.size + 1 == replacedPart.size)
        return Pair(leftReplacedPart.map { it as JezConstant }, rightReplacedPart.map { it as JezConstant })
    }

}

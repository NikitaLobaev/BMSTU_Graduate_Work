package lobaevni.graduate.jez

data class JezEquation(
    val u: JezEquationPart,
    val v: JezEquationPart,
) {

    /**
     * Tries to find minimal solution of this [JezEquation].
     */
    fun tryFindMinimalSolution(
        maxIterationsCount: Int = (u.size + v.size) * 2, //TODO: this value might be too small for some cases, increase it
    ): JezResult {
        val state = JezState(this)
        return state.tryFindMinimalSolution(maxIterationsCount)
    }

    /**
     * @return all used in this [JezEquation] constants.
     */
    fun getUsedConstants(): List<JezConstant> =
        (u + v).toSet().filterIsInstance<JezConstant>()

    /**
     * @return all used in this [JezEquation] source constants.
     */
    fun getUsedSourceConstants(): List<JezSourceConstant> =
        (u + v).toSet().filterIsInstance<JezSourceConstant>()

    /**
     * @return all used in this [JezEquation] generated constants.
     */
    fun getUsedGeneratedConstants(): List<JezGeneratedConstant> =
        (u + v).toSet().filterIsInstance<JezGeneratedConstant>()

    /**
     * @return all used in this [JezEquation] variables.
     */
    fun getUsedVariables(): List<JezVariable> =
        (u + v).toSet().filterIsInstance<JezVariable>()

    /**
     * @return whether an empty solution is suitable for this [JezEquation].
     */
    fun checkEmptySolution(): Boolean =
        u.filterIsInstance<JezConstant>() == v.filterIsInstance<JezConstant>()

    override fun toString(): String = u.convertToString() + " = " + v.convertToString()

    fun toHTMLString(): String = u.convertToHTMLString() + " = " + v.convertToHTMLString()

}

typealias JezEquationPart = List<JezElement>

/**
 * @return [JezEquationPart] with specified [repPart] replacement of [variable].
 */
internal fun JezEquationPart.replaceVariable(
    variable: JezVariable,
    repPart: List<JezConstant>,
): JezEquationPart {
    return map { element ->
        if (element == variable) {
            repPart
        } else {
            listOf(element)
        }
    }.flatten()
}

private fun JezEquationPart.convertToString(): String {
    return this.map {
        "$it "
    }.joinToString("") {
        it
    }.trim()
}

private fun JezEquationPart.convertToHTMLString(): String {
    data class Acc(
        val total: String,
        val element: JezElement,
        val count: Int,
    )

    return this
        .map { element ->
            Acc("", element, 1)
        }
        .reduce { lastAcc, currentAcc ->
            if (lastAcc.element == currentAcc.element) {
                Acc(lastAcc.total, currentAcc.element, lastAcc.count + 1)
            } else {
                if (lastAcc.count > 1) {
                    Acc(lastAcc.total + "<sup>${lastAcc.count}</sup>", currentAcc.element, 1)
                } else {
                    Acc(lastAcc.total + lastAcc.element, currentAcc.element, 1)
                }
            }
        }.let { acc ->
            Acc(acc.total + acc.element.toString(), acc.element, 0)
        }.total
}

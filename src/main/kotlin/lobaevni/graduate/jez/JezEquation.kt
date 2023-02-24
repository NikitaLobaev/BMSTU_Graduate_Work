package lobaevni.graduate.jez

/**
 * @property u left equation part.
 * @property v right equation part.
 */
data class JezEquation(
    val u: JezEquationPart,
    val v: JezEquationPart,
) {

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

    override fun toString(): String = u.convertToString() + " = " + v.convertToString()

    fun toHTMLString(): String = u.convertToHTMLString() + " = " + v.convertToHTMLString()

}

typealias JezEquationPart = List<JezElement>

internal fun JezEquationPart.convertToString(): String {
    return this.map {
        "$it "
    }.joinToString("") {
        it
    }.trim()
}

internal fun JezEquationPart.convertToHTMLString(
    epsilonIfEmpty: Boolean = true,
): String {
    data class Acc(
        val total: String,
        val element: JezElement,
        val count: Int,
    )

    return this
        .map { element ->
            Acc("", element, 1)
        }
        .reduceOrNull { lastAcc, currentAcc ->
            if (lastAcc.element == currentAcc.element) {
                Acc(lastAcc.total, currentAcc.element, lastAcc.count + 1)
            } else {
                val elementStr = lastAcc.element.toHTMLString()
                if (lastAcc.count > 1) {
                    Acc(lastAcc.total + "$elementStr<sup>${lastAcc.count}</sup>", currentAcc.element, 1)
                } else {
                    Acc(lastAcc.total + elementStr, currentAcc.element, 1)
                }
            }
        }?.let { acc ->
            val elementStr = acc.element.toHTMLString()
            Acc(acc.total + elementStr, acc.element, 0)
        }?.total ?: if (epsilonIfEmpty) "&epsilon;" else ""
}

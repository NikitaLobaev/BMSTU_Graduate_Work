package lobaevni.graduate.jez.data

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
    fun getUsedConstants(): Set<JezConstant> =
        (u + v).filterIsInstance<JezConstant>().toSet()

    /**
     * @return all used in this [JezEquation] source constants.
     */
    fun getUsedSourceConstants(): Set<JezSourceConstant> =
        (u + v).filterIsInstance<JezSourceConstant>().toSet()

    /**
     * @return all used in this [JezEquation] generated constants.
     */
    fun getUsedGeneratedConstants(): Set<JezGeneratedConstant> =
        (u + v).filterIsInstance<JezGeneratedConstant>().toSet()

    /**
     * @return all used in this [JezEquation] generated blocks of constants.
     */
    fun getUsedGeneratedConstantBlocks(): Set<JezGeneratedConstantBlock> =
        (u + v).filterIsInstance<JezGeneratedConstantBlock>().toSet()

    /**
     * @return all used in this [JezEquation] variables.
     */
    fun getUsedVariables(): Set<JezVariable> =
        (u + v).filterIsInstance<JezVariable>().toSet()

    override fun toString(): String = u.convertToString() + " = " + v.convertToString()

    fun toHTMLString(): String = u.convertToHTMLString() + " = " + v.convertToHTMLString()

}

typealias JezEquationPart = List<JezElement>

/**
 * @return indexes of all occurrences of specified [variable] in this [JezEquationPart].
 */
fun JezEquationPart.getVariableIndexes(variable: JezVariable): Set<Int> = this
    .withIndex()
    .filter { it.value == variable }
    .map { it.index }
    .toSet()

internal fun JezEquationPart.convertToString(): String = this
    .map {
        "$it "
    }
    .joinToString("") {
        it
    }
    .trim()

internal fun JezEquationPart.convertToHTMLString(
    epsilonIfEmpty: Boolean = true,
): String {
    data class Acc(
        val total: String,
        val element: JezElement?,
        val count: Int,
    )

    return (this + null)
        .map { element ->
            Acc("", element, 1)
        }
        .reduce { lastAcc, currentAcc ->
            if (lastAcc.element == currentAcc.element) {
                Acc(lastAcc.total, currentAcc.element, lastAcc.count + 1)
            } else {
                val elementStr = lastAcc.element!!.toHTMLString()
                if (lastAcc.count > 1) {
                    Acc(lastAcc.total + "$elementStr<sup>${lastAcc.count}</sup>", currentAcc.element, 1)
                } else {
                    Acc(lastAcc.total + elementStr, currentAcc.element, 1)
                }
            }
        }
        .total
        .takeIf { it.isNotEmpty() } ?: if (epsilonIfEmpty) "&epsilon;" else ""
}

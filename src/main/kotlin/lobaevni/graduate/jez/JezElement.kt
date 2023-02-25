package lobaevni.graduate.jez

typealias JezSigma = Map<JezVariable, List<JezSourceConstant>>
internal typealias JezMutableSigma = MutableMap<JezVariable, MutableList<JezSourceConstant>>
internal typealias JezReplaces = MutableMap<List<JezSourceConstant>, JezGeneratedConstant>
internal typealias JezConstant = JezElement.Constant
internal typealias JezSourceConstant = JezElement.Constant.Source
internal typealias JezGeneratedConstant = JezElement.Constant.Generated
internal typealias JezVariable = JezElement.Variable

internal val stubGeneratedConstant = JezGeneratedConstant(listOf())

sealed class JezElement {

    abstract class Constant : JezElement() {

        abstract val source: List<JezSourceConstant>

        data class Source(
            val value: Any,
        ) : Constant() {

            override val source = listOf(this)

            override fun toString(): String = "CONST($value)"

            override fun toHTMLString(): String = "$value"

        }

        data class Generated(
            val value: List<JezConstant>,
            val number: Int = (Math.random() * Int.MAX_VALUE).toInt(),
        ) : Constant() {

            override val source = value.map { it.source }.flatten()

            override fun toString(): String = "GENCONST($number)"

            override fun toHTMLString(): String = "&zeta;<sub>$number</sub>"

        }

    }

    data class Variable(
        val name: Any,
    ) : JezElement() {

        override fun toString(): String = "VAR($name)"

        override fun toHTMLString(): String = "$name"

    }

    //TODO: open fun toString()? same problem with JezAction.toString()

    open fun toHTMLString(): String {
        return toString()
    }

}

/**
 * Reveals source constants values and returns list of these [JezSourceConstant].
 */
internal fun List<JezConstant>.toJezSourceConstants(): List<JezSourceConstant> {
    return map { constant ->
        constant.source
    }.flatten()
}

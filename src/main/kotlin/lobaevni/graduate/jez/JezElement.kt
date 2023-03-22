package lobaevni.graduate.jez

typealias JezSigma = Map<JezVariable, List<JezSourceConstant>>
internal typealias JezMutableSigma = MutableMap<JezVariable, MutableList<JezSourceConstant>>
internal typealias JezReplaces = MutableMap<List<JezSourceConstant>, JezGeneratedConstant>
internal typealias JezConstant = JezElement.Constant
internal typealias JezSourceConstant = JezElement.Constant.Source
internal typealias JezGeneratedConstant = JezElement.Constant.Generated
internal typealias JezVariable = JezElement.Variable

sealed class JezElement {

    abstract class Constant : JezElement() {

        abstract val source: List<JezSourceConstant>

        data class Source(
            val value: Any,
        ) : Constant() {

            override val source = listOf(this)

            init {
                assert(source.isNotEmpty())
            }

            override fun toString(): String = "CONST($value)"

            override fun toHTMLString(): String = "$value"

        }

        data class Generated(
            val value: List<JezConstant>,
            private val preferredNumber: Int = (Math.random() * Int.MAX_VALUE).toInt(),
        ) : Constant() {

            val isBlock: Boolean = value.all { it == value.first() }

            val number: Int = if (isBlock) {
                val constant = value.firstOrNull()
                if (constant is JezGeneratedConstant && constant.isBlock) {
                    constant.number * value.size
                } else {
                    value.size
                }
            } else {
                preferredNumber
            }

            override val source = value.map { it.source }.flatten()

            init {
                assert(source.isNotEmpty())
            }

            override fun toString(): String {
                return if (isBlock) {
                    "BLOCK(${value.firstOrNull()}, ${value.size})"
                } else {
                    "GENCONST($number)"
                }
            }

            override fun toHTMLString(): String {
                return if (isBlock) {
                    "${value.first().toHTMLString()}<sub>$number</sub>"
                } else {
                    "&xi;<sub>$number</sub>"
                }
            }

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

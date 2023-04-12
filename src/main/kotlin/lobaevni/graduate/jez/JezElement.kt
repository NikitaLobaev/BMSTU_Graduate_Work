package lobaevni.graduate.jez

typealias JezSigma = Map<JezVariable, List<JezSourceConstant>>
internal typealias JezMutableSigma = MutableMap<JezVariable, MutableList<JezConstant>>
internal typealias JezNegativeSigma = Map<JezVariable, Set<JezSourceConstant>>
internal typealias JezMutableNegativeSigma = MutableMap<JezVariable, MutableSet<JezSourceConstant>>
internal typealias JezReplaces = MutableMap<List<JezSourceConstant>, JezGeneratedConstant>
internal typealias JezConstant = JezElement.Constant
internal typealias JezSourceConstant = JezElement.Constant.Source
internal typealias JezGeneratedConstant = JezElement.Constant.Generated
internal typealias JezGeneratedConstantBlock = JezElement.Constant.GeneratedBlock
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
                    var constant = value.first()
                    while ((constant as? JezGeneratedConstant)?.isBlock == true) {
                        constant = constant.value.first()
                    }
                    if (constant is JezGeneratedConstant) {
                        "(${constant.toHTMLString()})<sub>$number</sub>"
                    } else {
                        "${constant.toHTMLString()}<sub>$number</sub>"
                    }
                } else {
                    "&xi;<sub>$number</sub>"
                }
            }

        }

        data class GeneratedBlock(
            val constant: JezConstant,
            val powerIndexes: List<Int>,
            val additionalPower: Int = 0,
        ) : Constant() {

            constructor(
                constant: JezConstant,
                powerIndexes: Int,
                additionalPower: Int = 0,
            ) : this(constant, listOf(powerIndexes), additionalPower)

            override val source = constant.source

            override fun toString(): String {
                val powerIndexesStr = powerIndexes.joinToString(" + ") { "i_$it" } +
                        if (additionalPower > 0) " + $additionalPower" else ""
                return "BLOCK($constant, $powerIndexesStr)"
            }

            override fun toHTMLString(): String {
                val powerIndexesStr = powerIndexes.joinToString(" + ") { "i<sub>$it</sub>" } +
                        if (additionalPower > 0) " + $additionalPower" else ""
                return "${constant.toHTMLString()}<sup>$powerIndexesStr&nbsp;</sup>"
            }

        }

    }

    data class Variable(
        val name: Any,
    ) : JezElement() {

        override fun toString(): String = "VAR($name)"

        override fun toHTMLString(): String = "$name"

    }

    open fun toHTMLString(): String {
        return toString()
    }

}

/**
 * Reveals source constants values and returns list of these [JezSourceConstant].
 */
internal fun Collection<JezConstant>.toJezSourceConstants(): List<JezSourceConstant> {
    return map { constant ->
        constant.source
    }.flatten()
}

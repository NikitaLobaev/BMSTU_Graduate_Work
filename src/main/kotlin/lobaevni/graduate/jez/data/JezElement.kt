package lobaevni.graduate.jez.data

typealias JezSigma = Map<JezVariable, List<JezSourceConstant>>
internal typealias JezMutableSigma = MutableMap<JezVariable, MutableList<JezConstant>>
internal typealias JezNegativeSigma = Map<JezVariable, Set<JezConstant>>
internal typealias JezMutableNegativeSigma = MutableMap<JezVariable, MutableSet<JezConstant>>
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

            val number: Int = if (isBlock) { //TODO: оптимизировать. мы не можем так много времени тратить (на рекурсию) лишь на строковое представление блока
                val constant = value.firstOrNull()
                if ((constant as? JezGeneratedConstant)?.isBlock == true) {
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

            override fun toString(): String =
                if (isBlock) {
                    "BLOCK(${value.firstOrNull()}, ${value.size})"
                } else {
                    "GENCONST($number)"
                }

            override fun toHTMLString(): String =
                if (isBlock) {
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

        data class GeneratedBlock(
            val constant: JezConstant,
            val powerIndex: Int,
        ) : Constant() {

            override val source = constant.source

            override fun equals(other: Any?): Boolean = constant == (other as? JezGeneratedConstantBlock)?.constant

            override fun hashCode(): Int = constant.hashCode()

            override fun toString(): String = "BLOCK($constant, i_$powerIndex)"

            override fun toHTMLString(): String = "${constant.toHTMLString()}<sup>i<sub>$powerIndex</sub>&nbsp;</sup>"

        }

    }

    data class Variable(
        val name: Any,
    ) : JezElement() {

        override fun toString(): String = "VAR($name)"

        override fun toHTMLString(): String = "$name"

    }

    open fun toHTMLString(): String = toString()

}

/**
 * Reveals and returns source constants values.
 */
internal fun Collection<JezConstant>.toJezSourceConstants(): List<JezSourceConstant> = this
    .map { constant ->
        constant.source
    }
    .flatten()

/**
 * Transforms [JezMutableNegativeSigma] to [JezNegativeSigma].
 */
internal fun JezMutableNegativeSigma.toJezNegativeSigma(): JezNegativeSigma = this
    .toMap()
    .mapValues { entry ->
        entry.value.toSet()
    }

/**
 * @return length of this [JezSigma] (sum of lengths of each variable).
 */
internal fun JezSigma.getLength() = this
    .values
    .sumOf { constants ->
        constants.size
    }

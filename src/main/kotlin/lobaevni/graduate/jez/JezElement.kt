package lobaevni.graduate.jez

typealias JezSigma = MutableMap<JezVariable, List<JezSourceConstant>>
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

            override fun toString(): String = "CONST($value)"

        }

        data class Generated(
            val value: List<JezConstant>,
            val number: Int = (Math.random() * Int.MAX_VALUE).toInt(),
        ) : Constant() {

            override val source = value.map { it.source }.flatten()

            override fun toString(): String = "GENCONST($number)"

        }

    }

    data class Variable(
        val name: Any,
    ) : JezElement() {

        override fun toString(): String = "VAR($name)"

    }

}

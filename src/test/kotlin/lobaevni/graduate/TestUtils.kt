package lobaevni.graduate

import lobaevni.graduate.jez.data.*
import lobaevni.graduate.jez.data.JezSourceConstant
import lobaevni.graduate.jez.data.JezVariable

object TestUtils {

    /**
     * Parses [JezEquation] in kind of "[u] = [v]".
     */
    fun parseEquation(u: String, v: String): JezEquation {
        return JezEquation(
            u = parseEquationPart(u),
            v = parseEquationPart(v),
        )
    }

    /**
     * Parses part (left or right) of the [JezEquation].
     */
    private fun parseEquationPart(u: String): JezEquationPart {
        return u.map {
            if (it.isUpperCase()) { //constant
                JezSourceConstant(it)
            } else { //variable
                JezVariable(it)
            }
        }.toMutableList()
    }

    /**
     * Converts sigma to a human-readable map of strings of source constants of the [JezEquation].
     */
    fun JezSigma.toStringMap(): Map<String, String> {
        return this
            .mapKeys {
                it.key.name.toString()
            }
            .mapValues {
                it.value.map { sourceConstants ->
                    sourceConstants.value.toString()
                }.joinToString("") { sourceConstant ->
                    sourceConstant
                }
            }
    }

}

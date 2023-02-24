package lobaevni.graduate.jez.action

import lobaevni.graduate.Utils.prefixFunction
import lobaevni.graduate.jez.*

internal data class ConstantsRepAction(
    val state: JezState,
    val repPart: List<JezConstant>,
    val constant: JezGeneratedConstant = state.getOrGenerateConstant(repPart),
) : JezAction() {

    override fun applyAction(): Boolean {
        if (repPart.isEmpty()) return false

        val p = repPart.prefixFunction()
        val oldEquation = state.equation
        state.equation = JezEquation(
            u = state.equation.u.replace(p),
            v = state.equation.v.replace(p),
        )

        state.replaces[repPart.toJezSourceConstants()] = constant

        state.history?.putApplication(
            oldEquation = oldEquation,
            action = this,
            newEquation = state.equation,
        )
        return true
    }

    override fun revertAction(): Boolean {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return "${repPart.convertToHTMLString()} -> $constant"
    }

    override fun toHTMLString(): String {
        return "${repPart.convertToHTMLString()} &rarr; ${constant.toHTMLString()}"
    }

    /**
     * Replaces all subparts [p] with [constant] in [JezEquationPart].
     */
    private fun JezEquationPart.replace(p: Collection<Int>): JezEquationPart { //TODO: try to rewrite this function in functional style
        var k = 0
        var l = 0
        var result: MutableList<JezElement> = mutableListOf()
        while (k < size) {
            if (elementAt(k) == repPart[l]) {
                l++
                if (l == repPart.size) {
                    result = (result.dropLast(repPart.size - 1) + constant).toMutableList() //TODO: create extension?
                    l = 0
                } else {
                    result += elementAt(k)
                }
                k++
            } else if (l == 0) {
                result += elementAt(k)
                k++
            } else {
                l = p.elementAt(l - 1)
            }
        }
        return result
    }

}

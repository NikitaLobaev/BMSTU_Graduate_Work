package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.checkTrivialEmptySolution
import lobaevni.graduate.jez.data.*

internal abstract class JezReplaceAction : JezAction() {

    abstract val replaces: Map<out JezEquationPart, JezEquationPart>

    override fun applyAction(state: JezState): Boolean {
        if (replaces.isEmpty()) return false

        val oldEquation = state.equation
        var newEquation = oldEquation
        replaces.forEach { (from, to) ->
            newEquation = newEquation.replace(from, to)
        }

        if (oldEquation == newEquation) return false
        if (state.history?.graphNodes?.containsKey(newEquation) == true) {
            state.history.putApplication(
                oldEquation = oldEquation,
                action = this,
                newEquation = newEquation,
                ignored = true,
            )
            return false
        }

        state.equation = newEquation

        state.history?.putApplication(
            oldEquation = oldEquation,
            action = this,
            newEquation = state.equation,
            converges = state.checkTrivialEmptySolution(),
        )
        return true
    }

    override fun revertAction(state: JezState): Boolean {
        if (replaces.isEmpty() || replaces.any { (_, to) -> to.isEmpty() }) return false

        val oldEquation = state.equation
        var newEquation = oldEquation
        replaces.forEach { (from, to) ->
            newEquation = newEquation.replace(to, from)
        }

        if (oldEquation == newEquation) return false

        state.equation = newEquation

        state.history?.putReversion(
            oldEquation = oldEquation,
            newEquation = state.equation,
        )
        return true
    }

    override fun equals(other: Any?): Boolean =
        super.equals(other) || (other as? JezReplaceAction)?.replaces == replaces

    override fun hashCode(): Int = replaces.hashCode()

    override fun toString(): String = replaces
        .entries
        .joinToString(", ") { (from, to) ->
            "${from.convertToString()} -> ${to.convertToString()}"
        }

    override fun toHTMLString(): String = replaces
        .entries
        .joinToString(", ") { (from, to) ->
            "${from.convertToHTMLString()} &rarr; ${to.convertToHTMLString()}"
        }

}

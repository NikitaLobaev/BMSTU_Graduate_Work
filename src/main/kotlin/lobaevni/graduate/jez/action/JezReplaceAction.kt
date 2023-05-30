package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.JezEquationPart
import lobaevni.graduate.jez.JezState
import lobaevni.graduate.jez.convertToHTMLString
import lobaevni.graduate.jez.convertToString

internal abstract class JezReplaceAction : JezAction() {

    abstract val replaces: Collection<Pair<JezEquationPart, JezEquationPart>>

    init {
        //assert(replaces.isNotEmpty()) //TODO
    }

    override fun applyAction(state: JezState): Boolean {
        if (replaces.isEmpty()) return false

        val oldEquation = state.equation
        var newEquation = oldEquation
        for ((from, to) in replaces) {
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
        )
        return true
    }

    override fun revertAction(state: JezState): Boolean {
        if (replaces.isEmpty() || replaces.any { (_, to) -> to.isEmpty() }) return false

        val oldEquation = state.equation
        var newEquation = oldEquation
        for ((from, to) in replaces) {
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

    override fun equals(other: Any?): Boolean {
        return super.equals(other) || (other as? JezReplaceAction)?.replaces == replaces
    }

    override fun hashCode(): Int {
        return replaces.hashCode()
    }

    override fun toString(): String {
        return replaces.joinToString(", ") { (from, to) ->
            "${from.convertToString()} -> ${to.convertToString()}"
        }
    }

    override fun toHTMLString(): String {
        return replaces.joinToString(", ") { (from, to) ->
            "${from.convertToHTMLString()} &rarr; ${to.convertToHTMLString()}"
        }
    }

}

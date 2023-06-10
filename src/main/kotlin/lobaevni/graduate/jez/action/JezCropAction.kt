package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.checkEmptySolution
import lobaevni.graduate.jez.data.JezElement
import lobaevni.graduate.jez.data.JezEquation
import lobaevni.graduate.jez.data.JezState

internal data class JezCropAction(
    val leftPart: List<JezElement>,
    val rightPart: List<JezElement>,
) : JezAction() {

    constructor(
        equation: JezEquation,
        leftSize: Int,
        rightSize: Int,
    ) : this(
        leftPart = equation.v.subList(0, leftSize),
        rightPart = equation.v.subList(equation.v.size - rightSize, equation.v.size),
    )

    init {
        assert(leftPart.size + rightPart.size > 0)
    }

    override fun applyAction(state: JezState): Boolean {
        val cropSize = leftPart.size + rightPart.size
        if (cropSize > state.equation.u.size || cropSize > state.equation.v.size ||
            state.equation.u.subList(0, leftPart.size) != leftPart ||
            state.equation.v.subList(0, leftPart.size) != leftPart ||
            state.equation.u.subList(state.equation.u.size - rightPart.size, state.equation.u.size) != rightPart ||
            state.equation.v.subList(state.equation.v.size - rightPart.size, state.equation.v.size) != rightPart) {
            return false
        }

        val oldEquation = state.equation
        val newEquation = JezEquation(
            u = oldEquation.u.drop(leftPart.size).dropLast(rightPart.size),
            v = oldEquation.v.drop(leftPart.size).dropLast(rightPart.size),
        )

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
            converges = state.checkEmptySolution(),
        )
        return true
    }

    override fun revertAction(state: JezState): Boolean {
        val oldEquation = state.equation
        state.equation = JezEquation(
            u = leftPart + oldEquation.u + rightPart,
            v = leftPart + oldEquation.v + rightPart,
        )

        state.history?.putReversion(
            oldEquation = oldEquation,
            newEquation = state.equation,
        )
        return true
    }

    override fun toString(): String {
        return StringBuilder("crop").apply {
            if (leftPart.isNotEmpty()) append(" ${leftPart.size} left")
            if (rightPart.isNotEmpty()) append(" ${rightPart.size} right")
        }.toString()
    }

}

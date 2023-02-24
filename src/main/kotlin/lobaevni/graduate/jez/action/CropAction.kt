package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.JezElement
import lobaevni.graduate.jez.JezEquation
import lobaevni.graduate.jez.JezState

internal data class CropAction(
    val state: JezState,
    val leftPart: List<JezElement>,
    val rightPart: List<JezElement>,
) : JezAction() {

    constructor(
        state: JezState,
        leftSize: Int,
        rightSize: Int,
    ) : this(state, state.equation.v.subList(0, leftSize),
        state.equation.v.subList(state.equation.v.size - rightSize, state.equation.v.size))

    override fun applyAction(): Boolean {
        val cropSize = leftPart.size + rightPart.size
        if (cropSize == 0 || cropSize > state.equation.u.size || cropSize > state.equation.v.size ||
            state.equation.u.subList(0, leftPart.size) != leftPart ||
            state.equation.v.subList(0, leftPart.size) != leftPart ||
            state.equation.u.subList(state.equation.u.size - rightPart.size, state.equation.u.size) != rightPart ||
            state.equation.v.subList(state.equation.v.size - rightPart.size, state.equation.v.size) != rightPart) {
            return false
        }

        val oldEquation = state.equation
        state.equation = JezEquation(
            u = oldEquation.u.drop(leftPart.size).dropLast(rightPart.size),
            v = oldEquation.v.drop(leftPart.size).dropLast(rightPart.size),
        )

        state.history?.put(
            oldEquation = oldEquation,
            action = this,
            newEquation = state.equation,
        )
        return true
    }

    override fun revertAction(): Boolean {
        val cropSize = leftPart.size + rightPart.size
        if (cropSize == 0) {
            return false
        }

        val oldEquation = state.equation
        state.equation = JezEquation(
            u = leftPart + oldEquation.u + rightPart,
            v = leftPart + oldEquation.v + rightPart,
        )

        state.history?.put(
            oldEquation = oldEquation,
            action = this,
            newEquation = state.equation,
            reversion = true,
        )
        return true
    }

    override fun toString(): String {
        val sb = StringBuilder("crop")
        if (leftPart.isNotEmpty()) {
            sb.append(" ${leftPart.size} left")
        }
        if (rightPart.isNotEmpty()) {
            sb.append(" ${rightPart.size} right")
        }
        return sb.toString()
    }

}

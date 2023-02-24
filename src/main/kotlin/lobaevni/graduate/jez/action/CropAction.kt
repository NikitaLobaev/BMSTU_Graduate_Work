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
            u = state.equation.u.drop(leftPart.size).dropLast(rightPart.size),
            v = state.equation.v.drop(leftPart.size).dropLast(rightPart.size),
        )

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

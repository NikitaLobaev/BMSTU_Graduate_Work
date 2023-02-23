package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.*

/**
 * Action of recompression for [JezEquation].
 */
internal sealed class JezAction {

    /**
     * Tries to apply [JezAction] to [JezState] and adds corresponding record to
     * [lobaevni.graduate.jez.history.JezHistory].
     * @return true, if action was successfully applied, false otherwise.
     */
    internal abstract fun applyAction(): Boolean

    /**
     * Tries to revert [JezAction] in [JezState] and adds corresponding record to
     * [lobaevni.graduate.jez.history.JezHistory].
     * @return false, if action was successfully reverted, false otherwise.
     */
    internal abstract fun revertAction(): Boolean

    abstract override fun toString(): String //TODO: somewhy it doesn't forces to implement this method by inheritors, fix it

    open fun toHTMLString(): String {
        return toString()
    }

}

package lobaevni.graduate.jez.history

import lobaevni.graduate.jez.action.JezAction

internal data class JezHistoryGraphNode(
    internal val action: JezAction? = null,
    internal val parentNode: JezHistoryGraphNode? = null,
) {

    internal val childNodes: MutableMap<JezAction, JezHistoryGraphNode> = mutableMapOf()

}

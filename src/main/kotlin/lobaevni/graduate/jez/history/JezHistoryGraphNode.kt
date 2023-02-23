package lobaevni.graduate.jez.history

import lobaevni.graduate.jez.action.JezAction

internal data class JezHistoryGraphNode(
    val action: JezAction? = null,
    val parentNode: JezHistoryGraphNode? = null,
    val childNodes: MutableList<JezHistoryGraphNode> = mutableListOf(),
    //TODO: isVisited
)

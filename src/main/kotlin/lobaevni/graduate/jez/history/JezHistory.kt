package lobaevni.graduate.jez.history

import io.github.rchowell.dotlin.DotNodeShape
import io.github.rchowell.dotlin.DotRootGraph
import io.github.rchowell.dotlin.digraph
import lobaevni.graduate.jez.action.JezAction
import lobaevni.graduate.jez.JezEquation

private const val DOT_GRAPH_NAME = "recompression"
private const val DOT_NODE_IGNORED_COLOR = "#F1948A"

internal class JezHistory(
    dot: Boolean = false,
    private val dotShortenLabels: Boolean = false,
) {

    private val rootGraphNode: JezHistoryGraphNode = JezHistoryGraphNode()
    private var currentGraphNode: JezHistoryGraphNode = rootGraphNode

    val dotRootGraph: DotRootGraph? = if (dot) {
        digraph(DOT_GRAPH_NAME) {
            node {
                shape = DotNodeShape.BOX
            }
        }
    } else null

    /**
     * Puts record to a history.
     * @param reversion true, if this action was reverted, false if this action was applied.
     * @param ignored false, if [newEquation] was honestly processed in main algorithm, true otherwise.
     */
    fun put(
        oldEquation: JezEquation? = null,
        action: JezAction? = null,
        newEquation: JezEquation,
        reversion: Boolean = false, //TODO
        ignored: Boolean = false,
    ) {
        val newGraphNode = JezHistoryGraphNode(
            action = action,
            parentNode = currentGraphNode,
        )
        currentGraphNode.childNodes.add(newGraphNode)
        if (!ignored) {
            currentGraphNode = newGraphNode
        }

        dotRootGraph?.apply {
            val newEquationStrBr = "\"$newEquation\""
            +newEquationStrBr + {
                label = if (dotShortenLabels) newEquation.toHTMLString().formatHTMLLabel() else newEquation.toString()
                if (ignored) color = DOT_NODE_IGNORED_COLOR
            }
            oldEquation?.let {
                val oldEquationStrBr = "\"$oldEquation\""
                action?.let {
                    oldEquationStrBr - newEquationStrBr + {
                        label = if (dotShortenLabels) action.toHTMLString().formatHTMLLabel() else action.toString()
                        if (ignored) color = DOT_NODE_IGNORED_COLOR
                    }
                }
            }
        }
    }

    //TODO: @Experimental? @RequiresOptIn?
    private fun String.formatHTMLLabel(): String {
        return "\" label=<$this> hacklabel=\""
    }

}

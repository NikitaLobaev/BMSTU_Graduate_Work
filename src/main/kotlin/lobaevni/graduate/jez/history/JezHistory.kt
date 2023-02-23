package lobaevni.graduate.jez.history

import io.github.rchowell.dotlin.DotNodeShape
import io.github.rchowell.dotlin.DotRootGraph
import io.github.rchowell.dotlin.digraph
import lobaevni.graduate.jez.action.JezAction
import lobaevni.graduate.jez.JezEquation

private const val DOT_GRAPH_NAME = "recompression"

internal class JezHistory(
    private val dotShortenLabels: Boolean,
) {

    private val rootGraphNode: JezHistoryGraphNode = JezHistoryGraphNode()
    private var currentGraphNode: JezHistoryGraphNode = rootGraphNode

    val dotRootGraph: DotRootGraph = digraph(DOT_GRAPH_NAME) {
        node {
            shape = DotNodeShape.BOX
        }
    }

    fun putApplied(
        oldEquation: JezEquation? = null,
        action: JezAction? = null,
        newEquation: JezEquation,
    ) {
        val newGraphNode = JezHistoryGraphNode(
            action = action,
            parentNode = currentGraphNode,
        )
        currentGraphNode.childNodes.add(newGraphNode)
        currentGraphNode = newGraphNode

        dotRootGraph.apply {
            val newEquationStr = "\"$newEquation\""
            +newEquationStr + {
                label = if (dotShortenLabels) newEquation.toHTMLString().formatHTMLLabel() else newEquation.toString()
            }
            oldEquation?.let {
                val oldEquationStr = "\"$oldEquation\""
                +oldEquationStr + {
                    label = if (dotShortenLabels) oldEquation.toHTMLString().formatHTMLLabel() else oldEquation.toString()
                }
                action?.let {
                    oldEquationStr - newEquationStr + {
                        label = if (dotShortenLabels) action.toHTMLString().formatHTMLLabel() else action.toString()
                    }
                }
            }
        }
    }

    fun removeApplied() {
        //TODO
    }

    private fun String.formatHTMLLabel(): String {
        return "\" label=<$this> hacklabel=\""
    }

}

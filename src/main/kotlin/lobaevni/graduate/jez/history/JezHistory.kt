package lobaevni.graduate.jez.history

import io.github.rchowell.dotlin.DotNodeShape
import io.github.rchowell.dotlin.DotRootGraph
import io.github.rchowell.dotlin.digraph
import lobaevni.graduate.jez.action.JezAction
import lobaevni.graduate.jez.data.JezEquation

private const val DOT_GRAPH_NAME = "recompression"
private const val DOT_GRAPH_ORDERING = "out"
private const val DOT_ROOT_NODE_STYLE = "bold"
private const val DOT_NODE_DEFAULT_COLOR = "black"
private const val DOT_NODE_REVERTED_COLOR = "red"
private const val DOT_EDGE_IGNORED_COLOR = "gray"

internal class JezHistory(
    storeEquations: Boolean,
    dot: Boolean,
    private val dotHTMLLabels: Boolean,
    private val dotMaxStatementsCount: Int,
) {

    private val rootGraphNode: JezHistoryGraphNode = JezHistoryGraphNode()
    var currentGraphNode: JezHistoryGraphNode = rootGraphNode
    private set

    val graphNodes: MutableMap<JezEquation, JezHistoryGraphNode>? = if (storeEquations) {
        mutableMapOf()
    } else null

    val dotRootGraph: DotRootGraph? = if (dot) {
        digraph(name = DOT_GRAPH_NAME, strict = true) {
            ordering = DOT_GRAPH_ORDERING
            node {
                color = DOT_NODE_DEFAULT_COLOR
                shape = DotNodeShape.BOX
            }
        }
    } else null

    /**
     * Puts first record with source [equation] to a history.
     */
    @OptIn(DotExperimentalHTMLLabel::class)
    fun init(
        equation: JezEquation,
    ) {
        graphNodes?.put(equation, rootGraphNode)
        dotRootGraph?.apply {
            val equationStrBr = "\"$equation\""
            +equationStrBr + {
                label = if (dotHTMLLabels) equation.toHTMLString().formatHTMLLabel() else equation.toString()
                style = DOT_ROOT_NODE_STYLE
            }
        }
    }

    /**
     * Puts record of applied [action] to a history.
     * @param ignored false, if [newEquation] was honestly processed in main algorithm iteration, true otherwise.
     */
    @OptIn(DotExperimentalHTMLLabel::class)
    fun putApplication(
        oldEquation: JezEquation,
        action: JezAction,
        newEquation: JezEquation,
        ignored: Boolean = false,
    ) {
        assert(!ignored || !currentGraphNode.childNodes.contains(action))

        val newGraphNode = JezHistoryGraphNode(
            action = action,
            parentNode = currentGraphNode,
        )
        currentGraphNode.childNodes[action] = newGraphNode
        graphNodes?.let { graphNodes ->
            assert(ignored || !graphNodes.containsKey(newEquation))
            graphNodes.put(newEquation, newGraphNode)
        }
        if (!ignored) {
            currentGraphNode = newGraphNode
        }

        dotRootGraph?.apply {
            if (stmts.size >= dotMaxStatementsCount) return@apply

            val newEquationStrBr = "\"$newEquation\""
            val oldEquationStrBr = "\"$oldEquation\""
            if (!ignored) {
                +newEquationStrBr + {
                    label = if (dotHTMLLabels) newEquation.toHTMLString().formatHTMLLabel() else newEquation.toString()
                }
            }
            oldEquationStrBr - newEquationStrBr + {
                label = if (dotHTMLLabels) action.toHTMLString().formatHTMLLabel() else action.toString()
                if (ignored) color = DOT_EDGE_IGNORED_COLOR
            }
        }
    }

    /**
     * Reverts action of current node and puts corresponding record to a history.
     */
    fun putReversion(
        oldEquation: JezEquation,
        newEquation: JezEquation,
    ) {
        currentGraphNode.childNodes.clear()
        currentGraphNode = currentGraphNode.parentNode ?: return

        dotRootGraph?.apply {
            if (stmts.size >= dotMaxStatementsCount) return@apply

            val oldEquationStrBr = "\"$oldEquation\""
            val newEquationStrBr = "\"$newEquation\""
            +oldEquationStrBr + {
                color = DOT_NODE_REVERTED_COLOR
            }
            oldEquationStrBr - newEquationStrBr + {
                color = DOT_NODE_REVERTED_COLOR
            }
        }
    }

    @DotExperimentalHTMLLabel
    private fun String.formatHTMLLabel(): String {
        return "\" label=<&nbsp;$this&nbsp;> hacklabel=\""
    }

}

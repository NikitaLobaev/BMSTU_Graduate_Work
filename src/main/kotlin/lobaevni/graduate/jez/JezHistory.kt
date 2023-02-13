package lobaevni.graduate.jez

import io.github.rchowell.dotlin.DotRootGraph
import io.github.rchowell.dotlin.digraph

private const val GRAPH_NAME = "recompression"
private const val MAX_NODES_COUNT: Long = 300

internal data class JezHistory(
    val graph: DotRootGraph = digraph(GRAPH_NAME) {},
    private var lastEquationStr: String? = null,
    private var nodesCount: Long = 0, //TODO: does DotRootGraph really doesn't count vertices?
) {

    fun addEquation(equation: JezEquation, comment: String = "", updateLast: Boolean = true) {
        if (nodesCount == MAX_NODES_COUNT) return
        nodesCount++

        val equationStr = "\"$equation\""
        if (equationStr == lastEquationStr) return

        graph.apply {
            +equationStr + {
                if (!updateLast) {
                    color = "red"
                }
            }
            lastEquationStr?.let { lastEquationStr ->
                lastEquationStr - equationStr + {
                    label = comment
                }
            }
            if (updateLast) {
                lastEquationStr = equationStr
            }
        }
    }

}

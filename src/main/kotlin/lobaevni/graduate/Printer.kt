package lobaevni.graduate

import io.github.rchowell.dotlin.DotRootGraph
import lobaevni.graduate.jez.data.JezResult
import lobaevni.graduate.jez.data.JezSourceConstant
import lobaevni.graduate.jez.data.JezVariable
import java.io.File

/**
 * Prints solution (substitution) of equation.
 */
internal fun printSolution(
    result: JezResult,
    variables: List<JezVariable>,
) {
    assert(result.solutionState is JezResult.SolutionState.Found)

    fun printVariable(variable: JezVariable, value: List<JezSourceConstant>) {
        println("$variable = $value")
    }

    result.sigma.forEach { mapEntry ->
        printVariable(mapEntry.key, mapEntry.value)
    }
    variables.forEach { variable ->
        if (!result.sigma.containsKey(variable)) {
            printVariable(variable, listOf())
        }
    }
}

/**
 * Writes DOT-representation of stages history of an algorithm to DOT file and to graphic (DOT-representation) file.
 */
internal fun writeDOT(
    historyDotGraph: DotRootGraph,
    filename: String,
) {
    println()
    print("Writing DOT-representation...")
    val graphStr = historyDotGraph.dot()

    val graphDOTFile = File("$filename.dot")
    graphDOTFile.createNewFile()
    graphDOTFile.printWriter().use { printWriter ->
        printWriter.println(graphStr)
        printWriter.flush()
        printWriter.close()
    }

    val resultPNGFile = File("$filename.png")
    "dot -Tpng ${graphDOTFile.path} -o ${resultPNGFile.path}".runCommand()

    println(" OK")
}

/**
 * Creates operating system processes to run specified command.
 */
private fun String.runCommand() {
    ProcessBuilder(*split(" ").toTypedArray())
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
}

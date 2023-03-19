package lobaevni.graduate

import io.github.rchowell.dotlin.DotRootGraph
import lobaevni.graduate.jez.JezResult
import lobaevni.graduate.jez.JezSourceConstant
import lobaevni.graduate.jez.JezVariable
import java.io.File

/**
 * Prints result (substitution) of equation, if it was successfully solved, otherwise does nothing.
 */
internal fun printResult(
    result: JezResult,
    variables: List<JezVariable>,
) {
    fun printVariable(variable: JezVariable, value: List<JezSourceConstant>) {
        println("$variable = $value")
    }

    if (!result.isSolved) return

    for (mapEntry in result.sigma) {
        printVariable(mapEntry.key, mapEntry.value)
    }
    for (variable in variables) {
        if (!result.sigma.containsKey(variable)) {
            printVariable(variable, listOf())
        }
    }
}

/**
 * Writes DOT-representation of stages history of an algorithm to dot file and to graphic file.
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

    println(" SUCCESS")
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

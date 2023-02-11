package lobaevni.graduate

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import lobaevni.graduate.jez.*
import lobaevni.graduate.jez.Jez.wordEqSat
import java.io.File

private const val PROGRAM_NAME = "jez"

private val letterRegex = "^[A-Z][A-Z0-9]*$".toRegex()
private val variableRegex = "^[a-z][a-z0-9]*$".toRegex()

fun main(args: Array<String>) {
    val parser = ArgParser(PROGRAM_NAME)
    val dotFilename by parser.option(
        type = ArgType.String,
        fullName = "dot",
        description = "Output DOT-representation filename (without extension)",
    )
    parser.parse(args)

    val letters: JezSourceConstants
    val variables: JezVariables
    val equation: JezEquation
    try {
        letters = readln().parseLetters()
        variables = readln().parseVariables()
        equation = readln().parseEquation(letters, variables)
    } catch (e: Exception) {
        println("Usage:")
        println("{A, B, ...}")
        println("{x, y, ...}")
        println("T1 = T2")
        return
    }

    val result = try {
        equation.wordEqSat()
    } catch (e: Exception) {
        println("Unfortunately, exception was thrown while solving the equation.")
        e.printStackTrace()
        return
    }

    try {
        if (result.isSolved) {
            printResult(result)
        } else {
            println("Unfortunately, solution wasn't found.")
        }

        dotFilename?.let {
            writeDOT(result, it)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return
    }
}

private fun String.parseLetters(): JezSourceConstants {
    val letters = parseSequence().map { JezElement.Constant.Source(it) }
    assert(letters.find { !(it.value as String).matches(letterRegex) } == null)
    return letters
}

private fun String.parseVariables(): JezVariables {
    val variables = parseSequence().map { JezElement.Variable(it) }
    assert(variables.find { !(it.name as String).matches(variableRegex) } == null)
    return variables
}

private fun String.parseEquation(letters: JezSourceConstants, variables: JezVariables): JezEquation {
    val eqStr = split("=").toMutableList()
    assert(eqStr.size == 2)

    val eqParts: MutableList<JezEquationPart> = mutableListOf(mutableListOf(), mutableListOf())
    eqStr.forEachIndexed { eqPartIdx, sourceEqPartStr ->
        var eqPartStr = sourceEqPartStr.trim()
        assert(eqPartStr.isNotEmpty())

        do {
            val element: JezElement? = if (eqPartStr[0].isUpperCase()) { // letter
                letters.find { eqPartStr.startsWith(it.value as String) }
            } else { // variable
                variables.find { eqPartStr.startsWith(it.name as String) }
            }
            assert(element != null) { """Couldn't parse element "$element"""" }
            eqParts[eqPartIdx] += listOf(element!!)

            val length: Int = when (element) {
                is JezElement.Constant.Source -> (element.value as String).length
                is JezElement.Variable -> (element.name as String).length
                else -> 0
            }
            assert(length > 0) { """Empty length of element "$element"""" }
            eqPartStr = eqPartStr.substring(length)
        } while (eqPartStr.isNotEmpty())
    }

    return JezEquation(eqParts[0], eqParts[1])
}

private fun printResult(result: JezResult) {
    for (mapEntry in result.sigma) {
        println("${mapEntry.key} = ${mapEntry.value}")
    }
}

private fun String.parseSequence(): List<String> {
    return trim('{', '}')
        .split(",")
        .map { it.trim() }
}

private fun writeDOT(result: JezResult, filename: String) {
    println()
    print("Writing DOT-representation...")
    val graphStr = result.history.dot()

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

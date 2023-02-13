package lobaevni.graduate

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import lobaevni.graduate.jez.*
import java.io.File

private val letterRegex = "^[A-Z][A-Z0-9]*$".toRegex()
private val variableRegex = "^[a-z][a-z0-9]*$".toRegex()

fun main(args: Array<String>) {
    val parser = ArgParser("jez")
    val dotFilename by parser.option(
        description = "Output DOT-representation filename (without extension)", //TODO: move all of options to constants
        fullName = "dot",
        type = ArgType.String,
    )
    val dotShortenLabels by parser.option(
        description = "Shorten labels in DOT-representation",
        fullName = "dot-shorten-labels",
        shortName = "sl",
        type = ArgType.Boolean,
    ).default(false)
    val dotClipExtraVertices by parser.option(
        description = "Remove extra vertices from DOT-representation",
        fullName = "dot-clip-extra-vertices",
        shortName = "cev",
        type = ArgType.Boolean,
    ).default(false)
    parser.parse(args)

    val letters: List<JezSourceConstant>
    val variables: List<JezVariable>
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
        equation.tryFindMinimalSolution()
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
            writeDOT(
                result = result,
                filename = it,
                shortenLabels = dotShortenLabels,
                clipExtraVertices = dotClipExtraVertices,
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return
    }
}

private fun String.parseLetters(): List<JezSourceConstant> {
    val letters = parseSequence().map { JezSourceConstant(it) }
    assert(letters.find { !(it.value as String).matches(letterRegex) } == null)
    return letters
}

private fun String.parseVariables(): List<JezVariable> {
    val variables = parseSequence().map { JezVariable(it) }
    assert(variables.find { !(it.name as String).matches(variableRegex) } == null)
    return variables
}

private fun String.parseEquation(letters: List<JezSourceConstant>, variables: List<JezVariable>): JezEquation {
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
                is JezSourceConstant -> (element.value as String).length
                is JezVariable -> (element.name as String).length
                else -> 0
            }
            assert(length > 0) { """Empty length of element "$element"""" }
            eqPartStr = eqPartStr.substring(length)
        } while (eqPartStr.isNotEmpty())
    }

    return JezEquation(eqParts[0], eqParts[1])
}

/**
 * Prints result (substitution) of equation, if it was successfully solved, otherwise does nothing.
 */
private fun printResult(
    result: JezResult,
) {
    if (!result.isSolved) return

    for (mapEntry in result.sigma) {
        println("${mapEntry.key} = ${mapEntry.value}")
    }
}

/**
 * Writes DOT-representation of stages history of an algorithm to dot file and to graphic file.
 */
private fun writeDOT(
    result: JezResult,
    filename: String,
    shortenLabels: Boolean, //TODO: implement this unused flags
    clipExtraVertices: Boolean,
) {
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

private fun String.parseSequence(): List<String> {
    return trim('{', '}')
        .split(",")
        .map { it.trim() }
}

package lobaevni.graduate

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import lobaevni.graduate.jez.*

private const val OPTION_DOT_FILENAME_DESCRIPTION = "Output DOT-representation filename (without extension)"
private const val OPTION_DOT_SHORTEN_LABELS_DESCRIPTION = "Shorten labels in DOT-representation"
private const val OPTION_DOT_SHORTEN_PATHS_DESCRIPTION = "Shorten paths in DOT-representation"

private const val USAGE_MESSAGE = """
Usage:
{A, B, ...}
{x, y, ...}
U = V
"""
private const val NOT_SOLVED_MESSAGE = "Unfortunately, solution wasn't found."
private const val ERROR_MESSAGE = "Unfortunately, exception was thrown while solving the equation."

fun main(args: Array<String>) {
    val parser = ArgParser("jez")
    val dotFilename by parser.option(
        description = OPTION_DOT_FILENAME_DESCRIPTION,
        fullName = "dot",
        type = ArgType.String,
    )
    val dotShortenLabels by parser.option(
        description = OPTION_DOT_SHORTEN_LABELS_DESCRIPTION,
        fullName = "dot-shorten-labels",
        shortName = "sl",
        type = ArgType.Boolean,
    ).default(false)
    val dotShortenPaths by parser.option(
        description = OPTION_DOT_SHORTEN_PATHS_DESCRIPTION,
        fullName = "dot-shorten-paths",
        shortName = "sp",
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
        println(USAGE_MESSAGE)
        return
    }

    val result = try {
        equation.tryFindMinimalSolution(
            storeHistory = dotFilename != null,
            dotShortenLabels = dotShortenLabels,
        )
    } catch (e: Exception) {
        println(ERROR_MESSAGE)
        e.printStackTrace()
        return
    }

    try {
        if (result.isSolved) {
            printResult(result)
        } else {
            println(NOT_SOLVED_MESSAGE)
        }

        dotFilename?.let { df ->
            result.historyDotGraph?.let {
                writeDOT(
                    historyDotGraph = result.historyDotGraph,
                    filename = df,
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return
    }
}

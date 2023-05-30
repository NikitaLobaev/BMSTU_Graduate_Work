package lobaevni.graduate

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import lobaevni.graduate.jez.*

private const val PROGRAM_NAME = "jez"

private const val OPTION_ALLOW_REVERT_DESCRIPTION =
    "Allow reverting of recompression actions until no solution found"
private const val OPTION_DISALLOW_CYCLES_DESCRIPTION =
    "Try preventing of cycles by storing the equations themselves in the history"
private const val OPTION_ALLOW_BLOCK_COMP_CR = "Allow compression of crossing blocks with parametrized blocks"
private const val OPTION_HEURISTIC_EXTENDED_NEGATIVE_RESTRICTIONS =
    "Use heuristic of extended negative sigma restrictions"
private const val OPTION_MAX_ITERATIONS_COUNT_DESCRIPTION = "Max iterations count"
private const val OPTION_DOT_FILENAME_DESCRIPTION = "Output DOT-representation filename (without extension)"
private const val OPTION_DOT_SHORTEN_LABELS_DESCRIPTION = "Shorten labels in output DOT-representation"
private const val OPTION_DOT_MAX_STATEMENTS_COUNT_DESCRIPTION = "Max statements count in output DOT-representation"

private const val USAGE_MESSAGE = """
Usage:
{A, B, ...}
{x, y, ...}
U = V
"""
private const val SOLUTION_STATE_MESSAGE = "Solution state: "
private const val NOT_SOLVED_MESSAGE = "Unfortunately, solution wasn't found."
private const val ERROR_MESSAGE = "Unfortunately, exception was thrown while solving the equation."

fun main(args: Array<String>) {
    val parser = ArgParser(PROGRAM_NAME)
    val allowRevert by parser.option(
        description = OPTION_ALLOW_REVERT_DESCRIPTION,
        fullName = "allow-revert",
        type = ArgType.Boolean,
    ).default(false)
    val disallowCycles by parser.option(
        description = OPTION_DISALLOW_CYCLES_DESCRIPTION,
        fullName = "disallow-cycles",
        type = ArgType.Boolean,
    ).default(false)
    val allowBlockCompCr by parser.option(
        description = OPTION_ALLOW_BLOCK_COMP_CR,
        fullName = "allow-block-comp-cr",
        type = ArgType.Boolean,
    ).default(false)
    val heurExtNegRest by parser.option(
        description = OPTION_HEURISTIC_EXTENDED_NEGATIVE_RESTRICTIONS,
        fullName = "heur-ext-neg-rest",
        type = ArgType.Boolean,
    ).default(false)
    val maxIterationsCount by parser.option(
        description = OPTION_MAX_ITERATIONS_COUNT_DESCRIPTION,
        fullName = "max-iters-count",
        type = ArgType.Int,
    ).default(Int.MAX_VALUE)
    val dotFilename by parser.option(
        description = OPTION_DOT_FILENAME_DESCRIPTION,
        fullName = "dot-filename",
        type = ArgType.String,
    )
    val dotHTMLLabels by parser.option(
        description = OPTION_DOT_SHORTEN_LABELS_DESCRIPTION,
        fullName = "dot-html-labels",
        type = ArgType.Boolean,
    ).default(false)
    val dotMaxStatementsCount by parser.option(
        description = OPTION_DOT_MAX_STATEMENTS_COUNT_DESCRIPTION,
        fullName = "dot-max-stmts-count",
        type = ArgType.Int,
    ).default(Int.MAX_VALUE)
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
            allowRevert = allowRevert,
            allowBlockCompCr = allowBlockCompCr,
            storeHistory = allowRevert || disallowCycles || dotFilename != null,
            storeEquations = disallowCycles,
            heurExtNegRest = heurExtNegRest,
            maxIterationsCount = maxIterationsCount,
            dot = dotFilename != null,
            dotHTMLLabels = dotHTMLLabels,
            dotMaxStatementsCount = dotMaxStatementsCount,
        )
    } catch (e: Exception) {
        println(ERROR_MESSAGE)
        e.printStackTrace()
        return
    }

    try {
        if (result.solutionState is JezResult.SolutionState.Found) {
            printSolution(result, variables)
        } else {
            println(NOT_SOLVED_MESSAGE)
        }
        println("$SOLUTION_STATE_MESSAGE${result.solutionState.javaClass.simpleName}")

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

package lobaevni.graduate.jez

import lobaevni.graduate.Utils.cartesianProduct
import lobaevni.graduate.Utils.tryFindMinSolutionOfSLDE
import lobaevni.graduate.jez.JezHeuristics.getSideConstants
import lobaevni.graduate.jez.JezHeuristics.tryAssumeAndApply
import lobaevni.graduate.jez.action.*
import lobaevni.graduate.jez.data.*
import lobaevni.graduate.logger
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.set
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.collections.first
import kotlin.math.*

/**
 * Tries to find minimal solution of this [JezEquation]. The entry point to the main algorithm.
 */
fun JezEquation.tryFindMinimalSolution(
    allowRevert: Boolean,
    allowBlockCompCr: Boolean,
    storeHistory: Boolean,
    storeEquations: Boolean,
    heurExtNegRest: Boolean,
    fullTraversal: Boolean,
    maxIterationsCount: BigInteger?,
    dot: Boolean,
    dotHTMLLabels: Boolean,
    dotMaxStatementsCount: Int?,
): JezResult = JezState(
    equation = this,
    allowRevert = allowRevert,
    allowBlockCompCr = allowBlockCompCr,
    storeHistory = storeHistory || allowRevert,
    storeEquations = storeEquations,
    heurExtNegRest = heurExtNegRest,
    fullTraversal = fullTraversal,
    dot = dot,
    dotHTMLLabels = dotHTMLLabels,
    dotMaxStatementsCount = dotMaxStatementsCount,
).tryFindMinimalSolution(maxIterationsCount)

/**
 * Tries to find minimal solution of [this.equation]. The main algorithm.
 */
internal fun JezState.tryFindMinimalSolution(
    maxIterationsCount: BigInteger?,
): JezResult {
    assert(maxIterationsCount == null || maxIterationsCount > BigInteger.ZERO)

    var iteration = BigInteger.ZERO
    val maxSolutionLength: BigInteger = BigInteger.ONE +
        BigDecimal(E.pow(E)).pow(equation.u.size + equation.v.size).toBigInteger() //inclusive
    val maxBlockLength: BigInteger =
        BigDecimal(4).pow(equation.u.size + equation.v.size).toBigInteger() //non-inclusive
    var bestSigma: JezSigma? = null
    mainLoop@ while (true) {
        logger.debug("main loop iteration #{}", iteration)

        if (checkTrivialContradictions()) break@mainLoop
        if (checkEmptySolution()) {
            bestSigma = compareSolutions(bestSigma)
            break@mainLoop
        }

        if (iteration++ == maxIterationsCount) break@mainLoop

        try {
            tryShorten()
        } catch (e: JezEquationNotConvergesException) {
            break@mainLoop
        }

        val currentEquation = equation

        val compressions = listOfNotNull(
            { blockCompNCr(maxBlockLength) },
            if (allowBlockCompCr) { { blockCompCr() } } else null,
            { pairCompNCr() },
            { pairCompCr(equation == currentEquation) }
        )
        try {
            compressionLoop@ for (compression in compressions) {
                compression()

                if (checkEmptySolution()) {
                    val usedVariables = equation.getUsedVariables()
                    bestSigma = compareSolutions(bestSigma)

                    if (!fullTraversal) break@mainLoop
                    if (revertUntilNoSolution(skips = if (usedVariables.isNotEmpty()) 1 else 0)) {
                        continue@mainLoop
                    } else break@mainLoop
                }
                if (!checkEquationLength(maxSolutionLength)) throw JezEquationNotConvergesException()
                tryShorten()
                if (checkTrivialContradictions()) throw JezEquationNotConvergesException()
            }
        } catch (e: JezEquationNotConvergesException) {
            if (revertUntilNoSolution()) {
                continue@mainLoop
            } else break@mainLoop
        }

        if (equation == currentEquation && !revertUntilNoSolution()) break@mainLoop
    }

    val solutionState = if (bestSigma != null) { //solution was found
        if (fullTraversal && iteration <= maxIterationsCount) {
            JezResult.SolutionState.Found.Minimal
        } else {
            JezResult.SolutionState.Found.Arbitrary
        }
    } else { //solution wasn't found
        if (maxIterationsCount != null && iteration > maxIterationsCount) {
            JezResult.SolutionState.NotFound.NotEnoughIterations
        } else if (history == null) {
            JezResult.SolutionState.NotFound.HistoryNotStored
        } else if (!allowRevert) {
            JezResult.SolutionState.NotFound.RevertNotAllowed
        } else if (heurExtNegRest) {
            JezResult.SolutionState.NotFound.HeurExtNegRestDamage
        } else {
            JezResult.SolutionState.NotFound.NoSolution
        }
    }
    return JezResult(
        sigma = bestSigma ?: sigma,
        solutionState = solutionState,
        iterationsCount = iteration,
        historyDotGraph = history?.dotRootGraph,
    )
}

/**
 * Compression for non-crossing blocks.
 * @param maxBlockLength maximum possible length of any block in current equation.
 */
internal fun JezState.blockCompNCr(maxBlockLength: BigInteger) {
    val blocks: MutableSet<List<JezConstant>> = mutableSetOf()
    listOf(equation.u, equation.v).forEach { equationPart ->
        data class Acc(
            val element: JezElement?,
            val count: Int,
        )

        (equationPart + null)
            .map { element ->
                Acc(element, 1)
            }
            .reduce { lastAcc, currentAcc ->
                if (lastAcc.element == currentAcc.element) {
                    Acc(currentAcc.element, lastAcc.count + 1)
                } else {
                    if (lastAcc.element is JezConstant && lastAcc.count > 1) {
                        val curBlockLength = BigInteger.valueOf(lastAcc.element.source.size.toLong() * lastAcc.count)
                        if (curBlockLength > maxBlockLength) {
                            throw JezEquationNotConvergesException()
                        }

                        val block = List(lastAcc.count) { lastAcc.element }
                        blocks.add(block)
                    }
                    currentAcc
                }
            }
    }

    apply(JezReplaceConstantsAction(blocks.associateWith { block ->
        listOf(getOrPutGeneratedConstant(block))
    }))
}

/**
 * Compression for crossing blocks.
 */
internal fun JezState.blockCompCr() {
    //duplicates allowed, because order is more important than space complexity
    val suggestedBlockComps = mutableMapOf<JezVariable, MutableList<JezConstant>>()
    listOf(equation.u, equation.v).forEach { equationPart ->
        equationPart.forEachIndexed { index, element ->
            if (element !is JezVariable) return@forEachIndexed

            val previous = equationPart.elementAtOrNull(index - 1)
            val next = equationPart.elementAtOrNull(index + 1)
            listOfNotNull(previous, next)
                .filterIsInstance<JezGeneratedConstant>()
                .filter { it.isBlock }
                .forEach { constant ->
                    suggestedBlockComps.getOrPut(element) { mutableListOf() }.add(constant.source.first())
                }
        }
    }

    val connectedBlocks = mutableMapOf<JezConstant, MutableSet<JezConstant>>()
    listOf(equation.u, equation.v).forEach { equationPart ->
        equationPart.forEachIndexed { index, element ->
            if ((element as? JezGeneratedConstant)?.isBlock != true) return@forEachIndexed

            val previous = equationPart.elementAtOrNull(index - 1)
            val next = equationPart.elementAtOrNull(index + 1)
            listOfNotNull(previous, next)
                .filterIsInstance<JezGeneratedConstant>()
                .filter { it.isBlock }
                .forEach { constant ->
                    connectedBlocks.getOrPut(element.value.first()) { mutableSetOf() }.add(constant.value.first())
                    connectedBlocks.getOrPut(constant.value.first()) { mutableSetOf() }.add(element.value.first())
                }
        }
    }
    suggestedBlockComps.forEach { suggestedBlockComp ->
        suggestedBlockComp.value.toList().forEach { constant -> //need a copy, because we modify it same time
            suggestedBlockComp.value.addAll(connectedBlocks.getOrDefault(constant, null) ?: listOf())
        }
    }

    suggestedBlockComps.forEach { (variable, constants) ->
        constants.any { constant ->
            if (negativeSigmaLeft?.get(variable)?.contains(constant) == true &&
                negativeSigmaRight?.get(variable)?.contains(constant) == true) return@any false

            val leftBlock = if (negativeSigmaLeft?.get(variable)?.contains(constant) != true) {
                listOf(generateConstantBlock(constant))
            } else listOf()
            val rightBlock = if (negativeSigmaRight?.get(variable)?.contains(constant) != true) {
                listOf(generateConstantBlock(constant))
            } else listOf()
            return@any apply(JezReplaceVariablesAction(
                variable,
                leftPart = leftBlock,
                rightPart = rightBlock,
                oldNegativeSigmaLeft = negativeSigmaLeft?.toJezNegativeSigma()?.filterKeys { it == variable },
                oldNegativeSigmaRight = negativeSigmaRight?.toJezNegativeSigma()?.filterKeys { it == variable },
            ))
        }
    }
}

/**
 * Compression for non-crossing pairs.
 */
internal fun JezState.pairCompNCr() {
    val sideConstants = getSideConstants()
    val pairsMap = cartesianProduct(sideConstants.first, sideConstants.second).associateWith { false }.toMutableMap()
    listOf(equation.u, equation.v).forEach { equationPart -> //filtering existing pairs in equation
        equationPart.zipWithNext().forEach pair@ { pair ->
            if (pair.first !is JezConstant || pair.second !is JezConstant || !pairsMap.containsKey(pair)) return@pair
            pairsMap[Pair(pair.first as JezConstant, pair.second as JezConstant)] = true
        }
    }

    apply(JezReplaceConstantsAction(
        replaces = pairsMap
            .filterValues { it }
            .keys
            .associate { pair ->
                Pair(pair.toList(), listOf(getOrPutGeneratedConstant(pair.toList())))
            }
    ))
}

/**
 * Compression for a crossing pairs.
 * @param necComp heuristic whether should we necessarily perform compression for a random possibly crossing pair if
 * there really is no way to compress the equation at the algorithm iteration.
 */
internal fun JezState.pairCompCr(necComp: Boolean) {
    if (tryAssumeAndApply() || !necComp) return

    listOf(true, false).forEach { left ->
        equation.getUsedVariables().forEach { variable ->
            (equation.getUsedSourceConstants() + equation.getUsedGeneratedConstants()).forEach { constant ->
                if (apply(JezReplaceVariablesAction(
                        variable,
                        leftPart = if (left) listOf(constant) else listOf(),
                        rightPart = if (left) listOf() else listOf(constant),
                        oldNegativeSigmaLeft = negativeSigmaLeft?.toJezNegativeSigma()?.filterKeys { it == variable },
                        oldNegativeSigmaRight = negativeSigmaRight?.toJezNegativeSigma()?.filterKeys { it == variable },
                ))) return
            }
        }
    }
}

/**
 * @return best sigma (with minimal source sum length) of [lastSigma] and current [JezState.sigma].
 */
internal fun JezState.compareSolutions(lastSigma: JezSigma?): JezSigma =
    if (lastSigma == null || sigmaLeft.getSourceLength() + sigmaRight.getSourceLength() < lastSigma.getSourceLength()) {
        sigma
    } else lastSigma

/**
 * Shortening of the [JezEquation]. Cuts similar prefixes and suffixes of the both [JezEquation] parts (guaranteed
 * without loss to the solution).
 * @return true, if [JezCropAction] was successfully applied to the equation, false otherwise.
 */
internal fun JezState.tryShorten(): Boolean {
    val leftIndex = equation.u.zip(equation.v).indexOfFirst { (uElement, vElement) ->
        uElement != vElement
    }.takeIf { it != -1 } ?: minOf(equation.u.size, equation.v.size)
    val rightIndex = equation.u.reversed().zip(equation.v.reversed()).indexOfFirst { (uElement, vElement) ->
        uElement != vElement
    }.takeIf { it != -1 } ?: minOf(equation.u.size, equation.v.size)

    if (leftIndex + rightIndex == 0) return false

    if (!apply(JezCropAction(
            equation = equation,
            leftSize = leftIndex,
            rightSize = rightIndex,
        ))) throw JezEquationNotConvergesException()

    return true
}

/**
 * Checking side and some trivial contradictions in current [JezEquation] in prefixes and in suffixes of both parts.
 * @return true, if contradiction was found, false otherwise.
 */
internal fun JezState.checkTrivialContradictions(): Boolean {
    if ((equation.u.isEmpty() && equation.v.find { it is JezConstant } != null) ||
        (equation.v.isEmpty() && equation.u.find { it is JezConstant } != null)) return true

    fun JezElement.retrieveConstant(): JezConstant? =
        when (this) {
            is JezGeneratedConstantBlock -> constant
            is JezConstant -> this
            else -> null
        }

    return (equation.u.firstOrNull()?.retrieveConstant() is JezConstant &&
            equation.v.firstOrNull()?.retrieveConstant() is JezConstant &&
            equation.u.first().retrieveConstant() != equation.v.first().retrieveConstant()) ||
            (equation.u.lastOrNull()?.retrieveConstant() is JezConstant &&
                    equation.v.lastOrNull()?.retrieveConstant() is JezConstant &&
                    equation.u.last().retrieveConstant() != equation.v.last().retrieveConstant())
}

/**
 * Reverts actions (via addressing to a history) until it is able to move another way (until there can be no solution
 * yet).
 * @return true, if there was successfully found node (and if we currently moved to it), through which we can try to
 * find a solution, false otherwise.
 */
internal fun JezState.revertUntilNoSolution(skips: Int = 0): Boolean {
    assert(skips >= 0)
    if (!allowRevert) return false

    var skipsRemaining = skips
    while (skipsRemaining >= 0) {
        val action = history?.currentGraphNode?.action ?: return false
        if (!revert(action)) return false

        if (action.isFlawed()) skipsRemaining--
    }
    return true
}

/**
 * @return whether an empty solution is suitable for current [JezEquation].
 */
internal fun JezState.checkEmptySolution(): Boolean =
    if (allowBlockCompCr) {
        checkParametrizedEmptySolution()
    } else checkTrivialEmptySolution()

/**
 * @see [checkEmptySolution].
 */
internal fun JezState.checkTrivialEmptySolution(): Boolean {
    val result =
        equation.u.filterIsInstance<JezConstant>().toJezSourceConstants() ==
                equation.v.filterIsInstance<JezConstant>().toJezSourceConstants()
    if (result) {
        val curUsedVariables = equation.getUsedVariables()
        apply(JezDropParametersAndVariablesAction(
            elements = curUsedVariables,
            indexes = curUsedVariables.associateWith { variable ->
                Pair(
                    equation.u.getElementIndexes(variable),
                    equation.v.getElementIndexes(variable),
                )
            },
        ))
    }
    return result
}

/**
 * Checks empty solution for satisfiability and, if it success, applies actions of removing variables and indexes of
 * parametrized blocks.
 * @return whether an empty solution is suitable for current [JezEquation].
 */
internal fun JezState.checkParametrizedEmptySolution(): Boolean {
    data class JezEquationEntry(
        val source: List<JezSourceConstant> = listOf(),
        val entries: MutableList<JezConstant> = mutableListOf(),
    )

    /**
     * Groups each constant together with similar neighbours (like flatten).
     */
    fun List<JezConstant>.groupBySource(): List<JezEquationEntry> = this
        .map { constant ->
            val source = when (constant) {
                is JezGeneratedConstant -> constant.value.first().source
                else -> constant.source
            }
            mutableListOf(JezEquationEntry(source, mutableListOf(constant)))
        }
        .reduceOrNull { last, current ->
            if (last.last().source == current.first().source) {
                last.last().entries.addAll(current.first().entries)
            } else {
                last.addAll(current)
            }
            last
        } ?: listOf()

    val variablesIndexes: MutableMap<Int, Pair<Int, JezGeneratedConstantBlock>> = mutableMapOf() //power index -> matrix index
    val usedGeneratedConstantBlocks = equation.getUsedGeneratedConstantBlocks()
    usedGeneratedConstantBlocks.forEach { block ->
        variablesIndexes.getOrPut(block.powerIndex) { Pair(variablesIndexes.size, block) }
    }

    if (variablesIndexes.isEmpty()) return checkTrivialEmptySolution()

    var matrixA: D2Array<Long> = mk.zeros(0, variablesIndexes.size)
    var vectorB: D1Array<Long> = mk.zeros(0)

    val u = equation.u.filterIsInstance<JezConstant>().groupBySource()
    val v = equation.v.filterIsInstance<JezConstant>().groupBySource()
    val uIterator = u.iterator()
    val vIterator = v.iterator()
    uLoop@ while (uIterator.hasNext()) {
        val uEntry = uIterator.next()
        var updated = false
        vLoop@ while (vIterator.hasNext()) {
            val vEntry = vIterator.next()
            if (vEntry.source != uEntry.source) {
                vEntry.entries.forEach { constant ->
                    when (constant) {
                        is JezGeneratedConstantBlock -> {
                            val matrixARow: D2Array<Long> = mk.zeros(1, variablesIndexes.size)
                            matrixARow[0, variablesIndexes[constant.powerIndex]!!.first] = 1
                            vectorB = vectorB.append(0)
                            matrixA = matrixA.append(matrixARow).reshape(vectorB.size, variablesIndexes.size)
                        }
                        else -> return false
                    }
                }
                continue@vLoop
            }

            val matrixARow: D1Array<Long> = mk.zeros(variablesIndexes.size)
            vectorB = vectorB.append(0)
            listOf(Pair(uEntry.entries, 1L), Pair(vEntry.entries, -1L)).forEach { pair ->
                pair.first.forEach pairForEach@ { constant ->
                    when (constant) {
                        is JezGeneratedConstantBlock -> {
                            matrixARow[variablesIndexes[constant.powerIndex]!!.first] += pair.second
                            return@pairForEach
                        }
                        is JezGeneratedConstant -> {
                            if (constant.isBlock) {
                                vectorB[vectorB.size - 1] += -pair.second * constant.value.size
                                return@pairForEach
                            }
                        }
                    }
                    vectorB[vectorB.size - 1] += -pair.second
                }
            }
            matrixA = matrixA.append(matrixARow).reshape(vectorB.size, variablesIndexes.size)
            updated = true
            break@vLoop
        }
        if (!updated) return false
    }
    vLoop@ while (vIterator.hasNext()) {
        val vEntry = vIterator.next()
        vEntry.entries.forEach { constant ->
            when (constant) {
                is JezGeneratedConstantBlock -> {
                    val matrixARow: D2Array<Long> = mk.zeros(1, variablesIndexes.size)
                    matrixARow[0, variablesIndexes[constant.powerIndex]!!.first] = 1
                    vectorB = vectorB.append(0)
                    matrixA = matrixA.append(matrixARow).reshape(vectorB.size, variablesIndexes.size)
                }
                else -> return false
            }
        }
    }

    logger.debug("trying to solve SLDE with matrix A={} and vector B={}",
        matrixA.toString().replace("\n", " "), vectorB)
    //TODO: we can try add a priori information and only after Gauss-Jordan "half-elimination" perform a complete search
    val result: Array<Long>? = tryFindMinSolutionOfSLDE(matrixA, vectorB)
    logger.debug("SLDE solution is {}", result)

    if (result == null) return false

    val curUsedVariables = equation.getUsedVariables()
    val variablesReplaces: List<Pair<List<JezElement>, List<JezElement>>> = curUsedVariables.map { variable ->
        Pair(listOf(variable), listOf())
    }
    val blocksReplaces: List<Pair<List<JezElement>, List<JezElement>>> = variablesIndexes.map { entry ->
        if (result[entry.value.first] > 0) {
            //TODO: we should clear memory after revert properly
            val generatedConstant =
                getOrPutGeneratedConstant(List(result[entry.value.first].toInt()) { entry.value.second.constant })
            Pair(listOf(entry.value.second), listOf(generatedConstant))
        } else {
            Pair(listOf(entry.value.second), listOf())
        }
    }
    apply(JezDropParametersAndVariablesAction(
        replaces = (variablesReplaces + blocksReplaces).toMap(),
        indexes = (curUsedVariables + usedGeneratedConstantBlocks)
            .associateWith { variable ->
                Pair(
                    equation.u.getElementIndexes(variable),
                    equation.v.getElementIndexes(variable),
                )
            },
    ))

    lastParameters?.apply {
        clear()
        putAll(variablesIndexes.map { entry ->
            Pair(entry.key, result[entry.value.first])
        })
    }

    return true
}

/**
 * Validates current [JezEquation] length.
 */
internal fun JezState.checkEquationLength(maxSolutionLength: BigInteger): Boolean {
    val curLength = sigmaLeft.getSourceLength() + sigmaRight.getSourceLength()
    logger.debug("current sigma length is {} (max length is {})", curLength, maxSolutionLength)
    return curLength <= maxSolutionLength
}

/**
 * @return true, if this [JezAction] may be flawed, false if exactly not.
 */
internal fun JezAction.isFlawed(): Boolean =
    when (this) {
        is JezReplaceVariablesAction,
        is JezDropParametersAndVariablesAction -> true
        is JezReplaceConstantsAction ->
            replaces.any { entry ->
                entry.value.any { generatedConstant ->
                    generatedConstant.isBlock
                }
            }
        else -> false
    }

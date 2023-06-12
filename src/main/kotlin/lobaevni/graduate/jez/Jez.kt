package lobaevni.graduate.jez

import lobaevni.graduate.Utils.cartesianProduct
import lobaevni.graduate.jez.JezHeuristics.getSideConstants
import lobaevni.graduate.jez.JezHeuristics.tryAssumeAndApply
import lobaevni.graduate.jez.action.*
import lobaevni.graduate.jez.data.*
import java.math.BigInteger
import kotlin.math.E
import kotlin.math.pow
import kotlin.math.roundToLong

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
    val maxSolutionLength: Long = (E.pow(E.pow(equation.u.size + equation.v.size))).roundToLong() //TODO: ok?
    val maxBlockLength: Long = 4L.toDouble().pow(equation.u.size + equation.v.size).roundToLong() //TODO: validate input equation too
    var bestSigma: JezSigma? = null
    mainLoop@ while (true) {
        if (checkTrivialContradictions()) break@mainLoop
        if (checkEmptySolution()) {
            bestSigma = processSolution(bestSigma)
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
                    bestSigma = processSolution(bestSigma)

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
        JezResult.SolutionState.Found.Minimal //TODO: не забыть проверять на минимальное!!!
    } else { //solution wasn't found
        if (maxIterationsCount != null && iteration > maxIterationsCount) {
            JezResult.SolutionState.NotFound.NotEnoughIterations
        } else if (history == null) {
            JezResult.SolutionState.NotFound.HistoryNotStored
        } else if (!allowRevert && checkRevertIsPossible()) {
            //TODO: даже если мы разрешаем откаты и он возможен (мог быть ущербный), он может быть единственный
            // и быть случаем, когда уже рассмотрели x->Ax и x->e и других вариантов гарантированно нет и тогда
            // можем утверждать что решения точно не существует... как обрабатывать этот кейс?
            JezResult.SolutionState.NotFound.RevertNotAllowed
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
internal fun JezState.blockCompNCr(maxBlockLength: Long): JezState {
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
                        if (lastAcc.element.source.size.toLong() * lastAcc.count >= maxBlockLength) {
                            throw JezEquationNotConvergesException()
                        }

                        val block = List(lastAcc.count) { lastAcc.element }
                        blocks.add(block)
                    }
                    currentAcc
                }
            }
    }

    apply(JezReplaceConstantsAction(blocks.map { block ->
        Pair(block, listOf(getOrPutGeneratedConstant(block)))
    }))
    return this
}

/**
 * Compression for crossing blocks.
 */
internal fun JezState.blockCompCr(): JezState {
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
            if (!apply(JezReplaceVariablesAction(
                    variable,
                    leftPart = leftBlock,
                    rightPart = rightBlock,
                    oldNegativeSigmaLeft = negativeSigmaLeft?.toJezNegativeSigma()?.filterKeys { it == variable },
                    oldNegativeSigmaRight = negativeSigmaRight?.toJezNegativeSigma()?.filterKeys { it == variable },
                ))) return@any false

            nonEmptyVariables.add(variable)
            return@any true
        }
    }
    return this
}

/**
 * Compression for non-crossing pairs.
 */
internal fun JezState.pairCompNCr(): JezState {
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
            .map { pair ->
                Pair(pair.toList(), listOf(getOrPutGeneratedConstant(pair.toList())))
            }
    ))
    return this
}

/**
 * Compression for a crossing pairs.
 * @param necComp heuristic whether should we necessarily perform compression for a random possibly crossing pair if
 * there really is no way to compress the equation at the algorithm iteration.
 */
internal fun JezState.pairCompCr(necComp: Boolean): JezState {
    if (tryAssumeAndApply() || !necComp) return this

    listOf(true, false).forEach { left ->
        equation.getUsedVariables().forEach { variable ->
            (equation.getUsedSourceConstants() + equation.getUsedGeneratedConstants()).forEach { constant ->
                if (apply(JezReplaceVariablesAction(
                        variable,
                        leftPart = if (left) listOf(constant) else listOf(),
                        rightPart = if (left) listOf() else listOf(constant),
                        oldNegativeSigmaLeft = negativeSigmaLeft?.toJezNegativeSigma()?.filterKeys { it == variable },
                        oldNegativeSigmaRight = negativeSigmaRight?.toJezNegativeSigma()?.filterKeys { it == variable },
                ))) return this
            }
        }
    }
    return this
}

/**
 * Processes currently found solution.
 * @return best sigma (with minimal length) of [lastSigma] and current.
 */
internal fun JezState.processSolution(lastSigma: JezSigma?): JezSigma {
    val curUsedVariables = equation.getUsedVariables()
    if (curUsedVariables.isNotEmpty()) {
        assert(apply(JezDropVariablesAction(
            variables = curUsedVariables.toList(),
            indexes = curUsedVariables.associateWith { variable ->
                Pair(
                    equation.u.getVariableIndexes(variable),
                    equation.v.getVariableIndexes(variable),
                )
            },
        )))
    }

    val curSigma = sigma
    return if (lastSigma == null || curSigma.getLength() < lastSigma.getLength()) {
        curSigma
    } else lastSigma
}

/**
 * Shortening of the [JezEquation]. Cuts similar prefixes and suffixes of the both [JezEquation] parts (guaranteed
 * without loss to the solution).
 * @return true, if [JezCropAction] was successfully applied to the equation, false otherwise.
 */
internal fun JezState.tryShorten(): Boolean {
    //TODO: maybe collapse adjacent parametrized blocks
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
internal fun JezState.checkEmptySolution(): Boolean {
    if (!allowBlockCompCr)
        return equation.u.filterIsInstance<JezConstant>() == equation.v.filterIsInstance<JezConstant>()

    data class JezEquationEntry(
        val source: List<JezSourceConstant> = listOf(),
        val entries: MutableList<JezConstant> = mutableListOf(),
    )

    /**
     * TODO
     */
    fun List<JezConstant>.groupBySource(): List<JezEquationEntry> = this
        .map { constant ->
            mutableListOf(JezEquationEntry(constant.source, mutableListOf(constant)))
        }
        .reduce { last, current ->
            if (last.last().source == current.first().source) {
                last.last().entries.addAll(current.first().entries)
            } else {
                last.addAll(current)
            }
            return@reduce last
        }

    //val d: D2Array<Int> = mk.ndarray(mk[mk[2, 3, -1], mk[1, -2, 1], mk[1, 0, 2]])

    val u = equation.u.filterIsInstance<JezConstant>().groupBySource()
    val v = equation.v.filterIsInstance<JezConstant>().groupBySource()
    val uIterator = u.iterator()
    val vIterator = v.iterator()
    uLoop@ while (uIterator.hasNext()) {
        val uEntry = uIterator.next()
        vLoop@ while (vIterator.hasNext()) {
            val vEntry = vIterator.next()
            if (vEntry.source != uEntry.source) {
                //TODO
                continue@vLoop
            }

            //TODO
            continue@uLoop
            break@vLoop
        }
        return false
    }

    /*val a: D2Array<Long> = mk.ndarray(mk[mk[2, 3, -1], mk[1, -2, 1], mk[1, 0, 2]])
    val b: D1Array<Long> = mk.ndarray(mk[9, 3, 2])
    val rrr = mk.linalg.solve(a, b)
    print(rrr)*/

    return u == v
}

/**
 * Validates current [JezEquation] length.
 */
internal fun JezState.checkEquationLength(maxSolutionLength: Long): Boolean { //TODO: мы crop'ами теряем информацию же...
    fun JezEquationPart.getLength(): Long = filterIsInstance<JezConstant>().sumOf { it.source.size.toLong() }

    val currentLength = equation.u.getLength() + equation.v.getLength()
    return currentLength <= maxSolutionLength
}

/**
 * @return true, if revert is currently possible (there are exists parent node in history, which we can revert and try
 * to find solution in another branch), false otherwise.
 */
internal fun JezState.checkRevertIsPossible(): Boolean {
    var curNode = history?.currentGraphNode
    while (curNode?.action != null && !curNode.action!!.isFlawed()) {
        curNode = curNode.parentNode
    }
    return curNode != null
}

/**
 * @return true, if this [JezAction] may be flawed, false if exactly not.
 */
internal fun JezAction.isFlawed(): Boolean =
    when (this) {
        is JezReplaceVariablesAction,
        is JezDropVariablesAction -> true
        is JezReplaceConstantsAction ->
            replaces.any { pair ->
                pair.second.any { generatedConstant ->
                    generatedConstant.isBlock
                }
            }
        else -> false
    }

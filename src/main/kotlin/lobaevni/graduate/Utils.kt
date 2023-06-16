package lobaevni.graduate

import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.set
import org.jetbrains.kotlinx.multik.ndarray.operations.mapIndexed
import org.jetbrains.kotlinx.multik.ndarray.operations.minBy
import org.jetbrains.kotlinx.multik.ndarray.operations.sum
import org.jetbrains.kotlinx.multik.ndarray.operations.times
import kotlin.math.abs
import kotlin.math.max

private const val SUBSTITUTION_CHECK_LOG_INTERVAL_MS: Long = 1000

object Utils {

    /**
     * Knuth–Morris–Pratt algorithm.
     * @return [List] with specified [newPart] replacement of all [repPart] occurrences.
     */
    fun <T> List<T>.replace(
        repPart: Collection<T>,
        newPart: Collection<T>,
        prefixFunctionValues: Collection<Int>? = null,
    ): List<T> {
        if (isEmpty() || repPart.isEmpty() || repPart == newPart) return this

        data class Acc(
            val element: T? = null,
            val buffer: MutableList<T> = mutableListOf(),
            val result: MutableList<T> = mutableListOf(),
        )

        val result = (listOf(Acc(null)) + map { element ->
            Acc(element)
        }).reduce { lastAcc, curAcc ->
            lastAcc.buffer.add(curAcc.element!!)

            while (lastAcc.buffer.isNotEmpty() &&
                lastAcc.buffer.last() != repPart.elementAt(lastAcc.buffer.size - 1)) {
                val dropCount = if (prefixFunctionValues != null) {
                    val pfValue = prefixFunctionValues.elementAt(lastAcc.buffer.size - 1)
                    if (pfValue != 0) {
                        lastAcc.buffer.size - pfValue
                    } else 1
                } else 1
                for (i in 0 until dropCount) {
                    lastAcc.result.add(lastAcc.buffer.removeFirst())
                }
            }

            if (lastAcc.buffer.size == repPart.size) {
                lastAcc.buffer.clear()
                lastAcc.result.addAll(newPart)
            }
            lastAcc
        }.also { acc ->
            acc.result.addAll(acc.buffer)
        }.result
        return result
    }

    /**
     * Computes prefix function for [Collection].
     * @return [ArrayList] of values of computed prefix function.
     */
    fun <T: Any> Collection<T>.prefixFunction(): ArrayList<Int> {
        data class Acc(
            val elements: ArrayList<T>,
            val pfValues: ArrayList<Int>,
        )

        return this
            .map { element ->
                Acc(arrayListOf(element), arrayListOf(0))
            }
            .reduceIndexedOrNull { i, lastAcc, curAcc ->
                lastAcc.elements.add(curAcc.elements.first())

                var j = lastAcc.pfValues.last()
                while (j > 0 && lastAcc.elements[i] != lastAcc.elements[j]) {
                    j = lastAcc.pfValues[j - 1]
                }
                if (lastAcc.elements[i] == lastAcc.elements[j]) {
                    j++
                }

                lastAcc.pfValues.add(j)

                lastAcc
            }
            ?.pfValues ?: arrayListOf()
    }

    /**
     * Tries to find minimal solution of specified SLDE (AX=B) via direct search.
     * @return minimal substitution of specified SLDE, if found, null otherwise.
     */
    fun tryFindMinSolutionOfSLDE(sourceMatrixA: D2Array<Long>, sourceVectorB: D1Array<Long>): Array<Long>? {
        if (sourceMatrixA.size == 0 || sourceMatrixA[0].size != sourceVectorB.size) return null

        val matrixA = sourceMatrixA.copy()
        val vectorB = sourceVectorB.copy()
        var lastSubstitutionCheckTimeMs = -SUBSTITUTION_CHECK_LOG_INTERVAL_MS

        /**
         * Check satisfiability of constructed SLDE with specified substitution.
         */
        fun checkSatisfiability(substitution: Array<Long>): Boolean {
            if (System.currentTimeMillis() - lastSubstitutionCheckTimeMs >= SUBSTITUTION_CHECK_LOG_INTERVAL_MS) {
                logger.debug("checking SLDE substitution {}", substitution.toList())
                lastSubstitutionCheckTimeMs = System.currentTimeMillis()
            }
            for (rowIndex in 0 until vectorB.size) {
                val sum = matrixA[rowIndex]
                    .mapIndexed { index, value ->
                        value * substitution.elementAt(index)
                    }
                    .sum()
                if (sum != vectorB[rowIndex]) return false
            }
            return true
        }

        var maxValue: Long = 0
        for (rowIndex in 0 until vectorB.size) {
            if (vectorB[rowIndex] < 0) {
                vectorB[rowIndex] *= -1L
                matrixA[rowIndex] = matrixA[rowIndex].times(-1)
            }

            val minDiv = matrixA[rowIndex]
                .minBy { value ->
                    abs(value)
                }
                ?.takeIf { minValue -> //find minimal value, but it may be at least one
                    minValue > 0
                } ?: 1
            maxValue = max(maxValue, vectorB[rowIndex] / minDiv) //round down
        }

        val currentCombination = Array<Long>(matrixA[0].size) { 0 }
        while (true) {
            if (checkSatisfiability(currentCombination)) return currentCombination

            val index = currentCombination.indexOfLast { it != maxValue }
            if (index < 0) break

            currentCombination[index]++
            for (i in index + 1 until currentCombination.size) {
                currentCombination[i] = 0
            }
        }
        return null
    }

    /**
     * @return cartesian product of two collections.
     */
    fun <T, U> cartesianProduct(c1: Collection<T>, c2: Collection<U>): List<Pair<T, U>> =
        c1.flatMap { lhsElem -> c2.map { rhsElem -> lhsElem to rhsElem } }

}

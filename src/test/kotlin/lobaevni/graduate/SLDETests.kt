package lobaevni.graduate

import lobaevni.graduate.Utils.tryFindMinSolutionOfSLDE
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private const val FAILED_MSG_WRONG_SOLUTION = "Wrong solution (substitution vector X)"

class SLDETests {

    @Nested
    inner class OneVariable {

        @Test
        fun test1() {
            val sourceMatrixA: D2Array<Long> = mk.ndarray(mk[mk[1]])
            val sourceVectorB: D1Array<Long> = mk.ndarray(mk[3])
            val expectedVectorX: D1Array<Long> = mk.ndarray(mk[3])
            test(sourceMatrixA, sourceVectorB, expectedVectorX)
        }

        @Test
        fun test2() {
            val sourceMatrixA: D2Array<Long> = mk.ndarray(mk[mk[1], mk[-5]])
            val sourceVectorB: D1Array<Long> = mk.ndarray(mk[2, -10])
            val expectedVectorX: D1Array<Long> = mk.ndarray(mk[2])
            test(sourceMatrixA, sourceVectorB, expectedVectorX)
        }

        @Test
        fun testNoSolution1() {
            val sourceMatrixA: D2Array<Long> = mk.ndarray(mk[mk[-2]])
            val sourceVectorB: D1Array<Long> = mk.ndarray(mk[4])
            test(sourceMatrixA, sourceVectorB, null)
        }

        @Test
        fun testNoSolution2() {
            val sourceMatrixA: D2Array<Long> = mk.ndarray(mk[mk[5]])
            val sourceVectorB: D1Array<Long> = mk.ndarray(mk[-5])
            test(sourceMatrixA, sourceVectorB, null)
        }

    }

    @Nested
    inner class MTOneVariables {

        @Test
        fun test1() {
            val sourceMatrixA: D2Array<Long> = mk.ndarray(mk[mk[2, 3], mk[0, 17]])
            val sourceVectorB: D1Array<Long> = mk.ndarray(mk[5, 17])
            val expectedVectorX: D1Array<Long> = mk.ndarray(mk[1, 1])
            test(sourceMatrixA, sourceVectorB, expectedVectorX)
        }

        @Test
        fun test2() {
            val sourceMatrixA: D2Array<Long> = mk.ndarray(mk[mk[-2, -3], mk[0, 17]])
            val sourceVectorB: D1Array<Long> = mk.ndarray(mk[-5, 17])
            val expectedVectorX: D1Array<Long> = mk.ndarray(mk[1, 1])
            test(sourceMatrixA, sourceVectorB, expectedVectorX)
        }

        @Test
        fun test3() {
            val sourceMatrixA: D2Array<Long> = mk.ndarray(mk[mk[2, 0, 3, 10], mk[3, 0, 0, -6]])
            val sourceVectorB: D1Array<Long> = mk.ndarray(mk[17, 0])
            val expectedVectorX: D1Array<Long> = mk.ndarray(mk[2, 0, 1, 1])
            test(sourceMatrixA, sourceVectorB, expectedVectorX)
        }

        @Test
        fun testNoSolution1() {
            val sourceMatrixA: D2Array<Long> = mk.ndarray(mk[mk[5, 2]])
            val sourceVectorB: D1Array<Long> = mk.ndarray(mk[3])
            test(sourceMatrixA, sourceVectorB, null)
        }

        @Test
        fun testNoSolution2() {
            val sourceMatrixA: D2Array<Long> = mk.ndarray(mk[mk[2, 3], mk[0, 1]])
            val sourceVectorB: D1Array<Long> = mk.ndarray(mk[5, 2])
            test(sourceMatrixA, sourceVectorB, null)
        }

    }

    @Nested
    inner class CornerCases {

        @Test
        fun test1() {
            val sourceMatrixA: D2Array<Long> = mk.zeros(0, 0)
            val sourceVectorB: D1Array<Long> = mk.zeros(0)
            val expectedVectorX: D1Array<Long> = mk.zeros(0)
            test(sourceMatrixA, sourceVectorB, expectedVectorX)
        }

        @Test
        fun test2() {
            val sourceMatrixA: D2Array<Long> = mk.zeros(3, 2)
            val sourceVectorB: D1Array<Long> = mk.zeros(2)
            test(sourceMatrixA, sourceVectorB, null)
        }

        @Test
        fun test3() {
            val sourceMatrixA: D2Array<Long> = mk.zeros(3, 2)
            val sourceVectorB: D1Array<Long> = mk.zeros(4)
            test(sourceMatrixA, sourceVectorB, null)
        }

    }

    private fun test(sourceMatrixA: D2Array<Long>, sourceVectorB: D1Array<Long>, expectedVectorX: D1Array<Long>?) {
        assertEquals(expectedVectorX, tryFindMinSolutionOfSLDE(sourceMatrixA, sourceVectorB),
            FAILED_MSG_WRONG_SOLUTION)
    }

}

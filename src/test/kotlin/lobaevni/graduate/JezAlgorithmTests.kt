package lobaevni.graduate

import lobaevni.graduate.TestUtils.parseEquation
import lobaevni.graduate.TestUtils.toStringMap
import lobaevni.graduate.jez.data.JezResult
import lobaevni.graduate.jez.tryFindMinimalSolution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigInteger

private const val FAILED_MSG_WRONG_SOLUTION_STATE = "Wrong solution state"
private const val FAILED_MSG_WRONG_SIGMA = "Wrong answer"

class JezAlgorithmTests {

    @Nested
    inner class FullTraversal {

        private val maxIterationsCount = BigInteger.valueOf(500)

        @Test
        fun test1() {
            test(
                equation = "x = y",
                expectedSigma = mapOf(
                    "x" to "",
                    "y" to "",
                ),
                expectedState = JezResult.SolutionState.Found.Minimal,
            )
        }

        @Test
        fun test2() {
            test(
                equation = "A = A",
                expectedSigma = mapOf(),
                expectedState = JezResult.SolutionState.Found.Minimal,
            )
        }

        @Test
        fun test3() {
            test(
                equation = "x = A",
                expectedSigma = mapOf(
                    "x" to "A",
                ),
                expectedState = JezResult.SolutionState.Found.Minimal,
            )
        }

        @Test
        fun test4() {
            test(
                equation = "Ax = AB",
                expectedSigma = mapOf(
                    "x" to "B",
                ),
                expectedState = JezResult.SolutionState.Found.Minimal,
            )
        }

        @Test
        fun test5() {
            test(
                equation = "ABxy = xBAy",
                expectedSigma = mapOf(
                    "x" to "A",
                    "y" to "",
                ),
                expectedState = JezResult.SolutionState.Found.Minimal,
            )
        }

        @Test
        fun test6() {
            test(
                equation = "x = AAA",
                expectedSigma = mapOf(
                    "x" to "AAA",
                ),
                expectedState = JezResult.SolutionState.Found.Minimal,
            )
        }

        @Test
        fun test7() {
            test(
                equation = "AAAABBB = x",
                expectedSigma = mapOf(
                    "x" to "AAAABBB",
                ),
                expectedState = JezResult.SolutionState.Found.Minimal,
            )
        }

        @Test
        fun test8() {
            test(
                equation = "AAAAAx = yCBBB",
                expectedSigma = mapOf(
                    "x" to "CBBB",
                    "y" to "AAAAA",
                ),
                expectedState = JezResult.SolutionState.Found.Minimal,
            )
        }

        @Test
        fun test9() {
            test(
                equation = "zxz = AByAB",
                expectedSigma = mapOf(
                    "x" to "",
                    "y" to "",
                    "z" to "AB",
                ),
                expectedState = JezResult.SolutionState.Found.Minimal,
            )
        }

        @Test
        fun test10() {
            test(
                equation = "xxzyyyy = AAAAAAA",
                expectedSigma = mapOf(
                    "x" to "A",
                    "y" to "A",
                    "z" to "A",
                ),
                expectedState = JezResult.SolutionState.Found.Minimal,
            )
        }

        @Test
        fun testNoSolution1() {
            test(
                equation = "A = B",
                expectedState = JezResult.SolutionState.NotFound.NoSolution,
            )
        }

        @Test
        fun testNoSolution2() {
            test(
                equation = "x = Ax",
                expectedState = JezResult.SolutionState.NotFound.NoSolution,
            )
        }

        private fun test(
            equation: String,
            expectedSigma: Map<String, String>? = null,
            expectedState: JezResult.SolutionState,
        ) {
            val sourceEquation = parseEquation(equation)
            val result = sourceEquation.tryFindMinimalSolution(
                allowRevert = true,
                allowBlockCompCr = false,
                storeHistory = true,
                storeEquations = true,
                heurExtNegRest = false,
                fullTraversal = true,
                maxIterationsCount = maxIterationsCount,
                dot = false,
                dotHTMLLabels = false,
                dotMaxStatementsCount = null,
            )
            assertEquals(expectedState, result.solutionState, FAILED_MSG_WRONG_SOLUTION_STATE)

            if (expectedSigma != null) {
                val actualSigma = result.sigma.toStringMap()
                assertEquals(expectedSigma, actualSigma, FAILED_MSG_WRONG_SIGMA)
            }
        }

    }

}

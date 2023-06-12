package lobaevni.graduate

import lobaevni.graduate.TestUtils.parseEquation
import lobaevni.graduate.TestUtils.toStringMap
import lobaevni.graduate.jez.data.JezEquation
import lobaevni.graduate.jez.data.JezResult
import lobaevni.graduate.jez.tryFindMinimalSolution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigInteger

private const val FAILED_MSG_WRONG_SOLUTION_STATE = "Wrong solution state"
private const val FAILED_MSG_WRONG_SIGMA = "Wrong answer"
private val MAX_ITERATIONS_COUNT = BigInteger.valueOf(50)

class JezEquationTests {

    @Test
    fun test1() {
        val sourceEquation = parseEquation("x", "y")
        val expectedSigma = mapOf(
            "x" to "",
            "y" to "",
        )
        val result = sourceEquation.tryFindMinimalSolutionDefault()
        assertTrue(result.solutionState is JezResult.SolutionState.Found.Minimal, FAILED_MSG_WRONG_SOLUTION_STATE)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma, FAILED_MSG_WRONG_SIGMA)
    }

    @Test
    fun test2() {
        val sourceEquation = parseEquation("A", "A")
        val expectedSigma = emptyMap<String, String>()
        val result = sourceEquation.tryFindMinimalSolutionDefault()
        assertTrue(result.solutionState is JezResult.SolutionState.Found.Minimal, FAILED_MSG_WRONG_SOLUTION_STATE)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma, FAILED_MSG_WRONG_SIGMA)
    }

    @Test
    fun test3() {
        val sourceEquation = parseEquation("x", "A")
        val expectedSigma = mapOf(
            "x" to "A",
        )
        val result = sourceEquation.tryFindMinimalSolutionDefault()
        assertTrue(result.solutionState is JezResult.SolutionState.Found.Minimal, FAILED_MSG_WRONG_SOLUTION_STATE)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma, FAILED_MSG_WRONG_SIGMA)
    }

    @Test
    fun test4() {
        val sourceEquation = parseEquation("Ax", "AB")
        val expectedSigma = mapOf(
            "x" to "B",
        )
        val result = sourceEquation.tryFindMinimalSolutionDefault()
        assertTrue(result.solutionState is JezResult.SolutionState.Found.Minimal, FAILED_MSG_WRONG_SOLUTION_STATE)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma, FAILED_MSG_WRONG_SIGMA)
    }

    @Test
    fun test5() {
        val sourceEquation = parseEquation("ABxy", "xBAy")
        val expectedSigma = mapOf(
            "x" to "A",
            "y" to "",
        )
        val result = sourceEquation.tryFindMinimalSolutionDefault()
        assertTrue(result.solutionState is JezResult.SolutionState.Found.Minimal, FAILED_MSG_WRONG_SOLUTION_STATE)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma, FAILED_MSG_WRONG_SIGMA)
    }

    @Test
    fun test6() {
        val sourceEquation = parseEquation("x", "AAAA")
        val expectedSigma = mapOf(
            "x" to "AAAA",
        )
        val result = sourceEquation.tryFindMinimalSolutionDefault()
        assertTrue(result.solutionState is JezResult.SolutionState.Found.Minimal, FAILED_MSG_WRONG_SOLUTION_STATE)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma, FAILED_MSG_WRONG_SIGMA)
    }

    @Test
    fun test7() {
        val sourceEquation = parseEquation("AAAAAx", "yCBBB")
        val expectedSigma = mapOf(
            "x" to "CBBB",
            "y" to "AAAAA",
        )
        val result = sourceEquation.tryFindMinimalSolutionDefault()
        assertTrue(result.solutionState is JezResult.SolutionState.Found.Minimal, FAILED_MSG_WRONG_SOLUTION_STATE)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma, FAILED_MSG_WRONG_SIGMA)
    }

    @Test
    fun test8() {
        val sourceEquation = parseEquation("x", "AB")
        val expectedSigma = mapOf(
            "x" to "AB",
        )
        val result = sourceEquation.tryFindMinimalSolutionDefault()
        assertTrue(result.solutionState is JezResult.SolutionState.Found.Minimal, FAILED_MSG_WRONG_SOLUTION_STATE)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma, FAILED_MSG_WRONG_SIGMA)
    }

    @Test
    fun testNoSolution1() {
        val sourceEquation = parseEquation("A", "B")
        val result = sourceEquation.tryFindMinimalSolutionDefault()
        assertTrue(result.solutionState is JezResult.SolutionState.NotFound.NoSolution,
            FAILED_MSG_WRONG_SOLUTION_STATE)
    }

    @Test
    fun testNoSolution2() {
        val sourceEquation = parseEquation("x", "Ax")
        val result = sourceEquation.tryFindMinimalSolutionDefault()
        assertTrue(result.solutionState is JezResult.SolutionState.NotFound.NoSolution,
            FAILED_MSG_WRONG_SOLUTION_STATE)
    }

    private fun JezEquation.tryFindMinimalSolutionDefault() = tryFindMinimalSolution(
        allowRevert = true,
        allowBlockCompCr = false,
        storeHistory = true,
        storeEquations = true,
        heurExtNegRest = false,
        fullTraversal = true,
        maxIterationsCount = MAX_ITERATIONS_COUNT,
        dot = false,
        dotHTMLLabels = false,
        dotMaxStatementsCount = null,
    )

}

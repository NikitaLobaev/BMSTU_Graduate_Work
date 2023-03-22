package lobaevni.graduate

import lobaevni.graduate.TestUtils.parseEquation
import lobaevni.graduate.TestUtils.toStringMap
import lobaevni.graduate.jez.JezEquation
import lobaevni.graduate.jez.tryFindMinimalSolution
import kotlin.test.*

private const val FAILED_MSG_IS_NOT_SOLVED = "The equation had to be solved"
private const val FAILED_MSG_WRONG_SIGMA = "Wrong answer"
private const val FAILED_MSG_IS_SOLVED = "The equation shouldn't have been solved"

private const val MAX_ITERATIONS_COUNT = 15

class JezEquationTests {

    @Test
    fun test1() {
        val sourceEquation = parseEquation("x", "y")
        val expectedSigma = mapOf(
            "x" to "",
            "y" to "",
        )
        val result = sourceEquation.tryFindMinimalSolution()
        assertTrue(result.isSolved, FAILED_MSG_IS_NOT_SOLVED)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma, FAILED_MSG_WRONG_SIGMA)
    }

    @Test
    fun test2() {
        val sourceEquation = parseEquation("A", "A")
        val expectedSigma = emptyMap<String, String>()
        val result = sourceEquation.tryFindMinimalSolution()
        assertTrue(result.isSolved, FAILED_MSG_IS_NOT_SOLVED)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma, FAILED_MSG_WRONG_SIGMA)
    }

    @Test
    fun test3() {
        val sourceEquation = parseEquation("x", "A")
        val expectedSigma = mapOf(
            "x" to "A",
        )
        val result = sourceEquation.tryFindMinimalSolution()
        assertTrue(result.isSolved, FAILED_MSG_IS_NOT_SOLVED)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma, FAILED_MSG_WRONG_SIGMA)
    }

    @Test
    fun test4() {
        val sourceEquation = parseEquation("Ax", "AB")
        val expectedSigma = mapOf(
            "x" to "B",
        )
        val result = sourceEquation.tryFindMinimalSolution()
        assertTrue(result.isSolved, FAILED_MSG_IS_NOT_SOLVED)
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
        val result = sourceEquation.tryFindMinimalSolution()
        assertTrue(result.isSolved, FAILED_MSG_IS_NOT_SOLVED)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma, FAILED_MSG_WRONG_SIGMA)
    }

    @Test
    fun test6() {
        val sourceEquation = parseEquation("x", "AAAA")
        val expectedSigma = mapOf(
            "x" to "AAAA",
        )
        val result = sourceEquation.tryFindMinimalSolution()
        assertTrue(result.isSolved, FAILED_MSG_IS_NOT_SOLVED)
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
        val result = sourceEquation.tryFindMinimalSolution()
        assertTrue(result.isSolved, FAILED_MSG_IS_NOT_SOLVED)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma, FAILED_MSG_WRONG_SIGMA)
    }

    @Test
    fun test8() {
        val sourceEquation = parseEquation("x", "AB")
        val expectedSigma = mapOf(
            "x" to "AB",
        )
        val result = sourceEquation.tryFindMinimalSolution()
        assertTrue(result.isSolved, FAILED_MSG_IS_NOT_SOLVED)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma, FAILED_MSG_WRONG_SIGMA)
    }

    @Test
    fun testNoSolution1() {
        val sourceEquation = parseEquation("A", "B")
        val result = sourceEquation.tryFindMinimalSolution()
        assertFalse(result.isSolved, FAILED_MSG_IS_SOLVED)
    }

    @Test
    fun testNoSolution2() {
        val sourceEquation = parseEquation("x", "Ax")
        val result = sourceEquation.tryFindMinimalSolution()
        assertFalse(result.isSolved, FAILED_MSG_IS_SOLVED)
    }

    private fun JezEquation.tryFindMinimalSolution() = tryFindMinimalSolution(
        allowRevert = true,
        storeHistory = true,
        storeEquations = true,
        maxIterationsCount = MAX_ITERATIONS_COUNT,
        dot = false,
        dotHTMLLabels = false,
        dotMaxStatementsCount = Int.MAX_VALUE,
    )

}

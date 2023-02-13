package lobaevni.graduate

import lobaevni.graduate.Utils.parseEquation
import lobaevni.graduate.Utils.toStringMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JezEquationTests {

    @Test
    fun test1() {
        val sourceEquation = parseEquation("x", "y")
        val expectedSigma = mapOf(
            "x" to "",
            "y" to "",
        )
        val result = sourceEquation.tryFindMinimalSolution()
        assertTrue(result.isSolved)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma)
    }

    @Test
    fun test2() {
        val sourceEquation = parseEquation("A", "A")
        val expectedSigma = emptyMap<String, String>()
        val result = sourceEquation.tryFindMinimalSolution()
        assertTrue(result.isSolved)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma)
    }

    @Test
    fun test3() {
        val sourceEquation = parseEquation("x", "A")
        val expectedSigma = mapOf(
            "x" to "A",
        )
        val result = sourceEquation.tryFindMinimalSolution()
        assertTrue(result.isSolved)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma)
    }

    @Test
    fun test4() {
        val sourceEquation = parseEquation("Ax", "AB")
        val expectedSigma = mapOf(
            "x" to "B",
        )
        val result = sourceEquation.tryFindMinimalSolution()
        assertTrue(result.isSolved)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma)
    }

    @Test
    fun test5() {
        val sourceEquation = parseEquation("ABxy", "xBAy")
        val expectedSigma = mapOf(
            "x" to "A",
            "y" to "",
        )
        val result = sourceEquation.tryFindMinimalSolution()
        assertTrue(result.isSolved)
        val actualSigma = result.sigma.toStringMap()
        assertEquals(expectedSigma, actualSigma)
    }

    @Test
    fun testNoSolution1() {
        val sourceEquation = parseEquation("A", "B")
        val result = sourceEquation.tryFindMinimalSolution()
        assertFalse(result.isSolved)
    }

    @Test
    fun testNoSolution2() {
        val sourceEquation = parseEquation("x", "Ax")
        val result = sourceEquation.tryFindMinimalSolution()
        assertFalse(result.isSolved)
    }

}

package lobaevni.graduate

import kotlin.test.Test
import lobaevni.graduate.Utils.prefixFunction
import kotlin.test.assertEquals

private const val FAILED_MSG_WRONG_PREFIX_FUNC = "The prefix function computed wrong values"

class PrefixFunctionTests {

    @Test
    fun test1() {
        val collection = "".chunked(1)
        val actualPValues = collection.prefixFunction()
        val expectedPValues = listOf<Int>()
        assertEquals(expectedPValues, actualPValues, FAILED_MSG_WRONG_PREFIX_FUNC)
    }

    @Test
    fun test2() {
        val collection = "A".chunked(1)
        val actualPValues = collection.prefixFunction()
        val expectedPValues = listOf<Int>(0)
        assertEquals(expectedPValues, actualPValues, FAILED_MSG_WRONG_PREFIX_FUNC)
    }

    @Test
    fun test3() {
        val collection = "ABC".chunked(1)
        val actualPValues = collection.prefixFunction()
        val expectedPValues = listOf(0, 0, 0)
        assertEquals(expectedPValues, actualPValues, FAILED_MSG_WRONG_PREFIX_FUNC)
    }

    @Test
    fun test4() {
        val collection = "AABCA".chunked(1)
        val actualPValues = collection.prefixFunction()
        val expectedPValues = listOf(0, 1, 0, 0, 1)
        assertEquals(expectedPValues, actualPValues, FAILED_MSG_WRONG_PREFIX_FUNC)
    }

    @Test
    fun test5() {
        val collection = "ABRACADABRA".chunked(1)
        val actualPValues = collection.prefixFunction()
        val expectedPValues = listOf(0, 0, 0, 1, 0, 1, 0, 1, 2, 3, 4)
        assertEquals(expectedPValues, actualPValues, FAILED_MSG_WRONG_PREFIX_FUNC)
    }

    @Test
    fun test6() {
        val collection = "ABCABCAABCABD".chunked(1)
        val actualPValues = collection.prefixFunction()
        val expectedPValues = listOf(0, 0, 0, 1, 2, 3, 4, 1, 2, 3, 4, 5, 0)
        assertEquals(expectedPValues, actualPValues, FAILED_MSG_WRONG_PREFIX_FUNC)
    }

}

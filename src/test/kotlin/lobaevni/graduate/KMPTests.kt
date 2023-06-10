package lobaevni.graduate

import lobaevni.graduate.Utils.prefixFunction
import lobaevni.graduate.Utils.replace
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

private const val FAILED_MSG_WRONG_REPLACE_RESULT = "The KMP algorithm did wrong result"

class KMPTests {

    @Nested
    inner class WithPrefixFunction {

        @Test
        fun test1() {
            test(source = "ABCABCD", from = "A", to = "D")
        }

        @Test
        fun test2() {
            test(source = "ABCABCD", from = "B", to = "D")
        }

        @Test
        fun test3() {
            test(source = "ABCABCD", from = "D", to = "E")
        }

        @Test
        fun test4() {
            test(source = "ABCABCD", from = "A", to = "DBD")
        }

        @Test
        fun test5() {
            test(source = "ABCABCD", from = "B", to = "")
        }

        @Test
        fun test6() {
            test(source = "ABCABCD", from = "D", to = "E")
        }

        @Test
        fun test7() {
            test(source = "ABCABCD", from = "ABC", to = "E")
        }

        @Test
        fun test8() {
            test("ABCABCD", from = "BC", to = "B")
        }

        @Test
        fun test9() {
            test(source = "ABCABCD", from = "ABC", to = "BCA")
        }

        @Test
        fun test10() {
            test(source = "ABAABC", from = "A", to = "AD")
        }

        @Test
        fun test11() {
            test(source = "ABAAB", from = "AB", to = "B")
        }

        @Test
        fun test12() {
            test(source = "ABAABC", from = "AB", to = "B")
        }

        @Test
        fun test13() {
            test(source = "ABCABCABCDABC", from = "ABC", to = "ABCD")
        }

        @Test
        fun test14() {
            test(source = "AAABA", from = "A", to = "B")
        }

        @Test
        fun test15() {
            test(source = "", from = "", to = "")
        }

        @Test
        fun test16() {
            test(source = "", from = "AB", to = "BC")
        }

        @Test
        fun test17() {
            test(source = "AAABA", from = "A", to = "A")
        }

        @Test
        fun test18() {
            test(source = "ABCCBCBCC", from = "CC", to = "c")
        }

        private fun test(source: String, from: String, to: String) = test(source, from, to, withPrefixFunction = true)

    }

    @Nested
    inner class WithoutPrefixFunction {

        @Test
        fun test1() {
            test(source = "ABCABCD", from = "A", to = "D")
        }

        @Test
        fun test2() {
            test(source = "ABCABCD", from = "B", to = "D")
        }

        @Test
        fun test3() {
            test(source = "ABCABCD", from = "D", to = "E")
        }

        @Test
        fun test4() {
            test(source = "ABCABCD", from = "A", to = "DBD")
        }

        @Test
        fun test5() {
            test(source = "ABCABCD", from = "B", to = "")
        }

        @Test
        fun test6() {
            test(source = "ABCABCD", from = "D", to = "E")
        }

        @Test
        fun test7() {
            test(source = "ABCABCD", from = "ABC", to = "E")
        }

        @Test
        fun test8() {
            test("ABCABCD", from = "BC", to = "B")
        }

        @Test
        fun test9() {
            test(source = "ABCABCD", from = "ABC", to = "BCA")
        }

        @Test
        fun test10() {
            test(source = "ABAABC", from = "A", to = "AD")
        }

        @Test
        fun test11() {
            test(source = "ABAAB", from = "AB", to = "B")
        }

        @Test
        fun test12() {
            test(source = "ABAABC", from = "AB", to = "B")
        }

        @Test
        fun test13() {
            test(source = "ABCABCABCDABC", from = "ABC", to = "ABCD")
        }

        @Test
        fun test14() {
            test(source = "AAABA", from = "A", to = "B")
        }

        @Test
        fun test15() {
            test(source = "", from = "", to = "")
        }

        @Test
        fun test16() {
            test(source = "", from = "AB", to = "BC")
        }

        @Test
        fun test17() {
            test(source = "AAABA", from = "A", to = "A")
        }

        private fun test(source: String, from: String, to: String) = test(source, from, to, withPrefixFunction = false)

    }

    private fun test(source: String, from: String, to: String, withPrefixFunction: Boolean) {
        val actualResult = source.kmp(from, to, withPrefixFunction)
        val expectedResult = source.replace(from, to)
        assertEquals(expectedResult, actualResult, FAILED_MSG_WRONG_REPLACE_RESULT)
    }

    private fun String.kmp(from: String, to: String, withPrefixFunction: Boolean): String {
        val repPart = from.chunked(1)
        return this
            .chunked(1)
            .replace(
                repPart = repPart,
                newPart = to.chunked(1),
                prefixFunctionValues = if (withPrefixFunction) repPart.prefixFunction() else null,
            )
            .joinToString("")
    }

}

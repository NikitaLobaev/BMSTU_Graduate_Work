package lobaevni.graduate

import lobaevni.graduate.Utils.prefixFunction
import lobaevni.graduate.Utils.replace
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private const val FAILED_MSG_WRONG_REPLACE_RESULT = "The KMP algorithm did wrong result"

class KMPTests {

    @Nested
    inner class WithPrefixFunction {

        @Nested
        inner class EmptyInput {

            @Test
            fun test1() {
                test(source = "", from = "", to = "", expected = "")
            }

            @Test
            fun test2() {
                test(source = "", from = "AB", to = "BC")
            }

            @Test
            fun test3() {
                test(source = "", from = "", to = "A", expected = "")
            }

        }

        @Nested
        inner class ZeroToSomething {

            @Test
            fun test1() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "", to = "A", expected = "ABCABCDAABBBCDDDDABCD")
            }

            @Test
            fun test2() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "", to = "AB", expected = "ABCABCDAABBBCDDDDABCD")
            }

        }

        @Nested
        inner class OneToZero {

            @Test
            fun test1() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "A", to = "")
            }

            @Test
            fun test2() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "B", to = "")
            }

            @Test
            fun test3() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "D", to = "")
            }

            @Test
            fun test4() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "E", to = "")
            }

        }

        @Nested
        inner class OneToOne {

            @Test
            fun test1() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "A", to = "E")
            }

            @Test
            fun test2() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "A", to = "A")
            }

            @Test
            fun test3() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "B", to = "E")
            }

            @Test
            fun test4() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "B", to = "B")
            }

            @Test
            fun test5() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "D", to = "E")
            }

            @Test
            fun test6() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "D", to = "D")
            }

            @Test
            fun test7() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "E", to = "F")
            }

            @Test
            fun test8() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "E", to = "E")
            }

        }

        @Nested
        inner class OneToMTOne {

            @Test
            fun test1() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "A", to = "ABA")
            }

            @Test
            fun test2() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "A", to = "BAB")
            }

            @Test
            fun test3() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "B", to = "ABA")
            }

            @Test
            fun test4() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "B", to = "BAB")
            }

            @Test
            fun test5() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "D", to = "DCD")
            }

            @Test
            fun test6() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "D", to = "CDC")
            }

            @Test
            fun test7() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "E", to = "FF")
            }

        }

        @Nested
        inner class MTOneToZero {

            @Test
            fun test1() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "")
            }

            @Test
            fun test2() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "")
            }

            @Test
            fun test3() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "")
            }

            @Test
            fun test4() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "EF", to = "GH")
            }

        }

        @Nested
        inner class MTOneToOne {

            @Test
            fun test1() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "A")
            }

            @Test
            fun test2() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "B")
            }

            @Test
            fun test3() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "E")
            }

            @Test
            fun test4() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "B")
            }

            @Test
            fun test5() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "C")
            }

            @Test
            fun test6() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "E")
            }

            @Test
            fun test7() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "C")
            }

            @Test
            fun test8() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "D")
            }

            @Test
            fun test9() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "E")
            }

        }

        @Nested
        inner class MTOneToMTOne {

            @Test
            fun test1() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "ABC")
            }

            @Test
            fun test2() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "ABE")
            }

            @Test
            fun test3() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "BC")
            }

            @Test
            fun test4() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "BE")
            }

            @Test
            fun test5() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "EB")
            }

            @Test
            fun test6() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "BA")
            }

            @Test
            fun test7() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "BCD")
            }

            @Test
            fun test8() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "BCE")
            }

            @Test
            fun test9() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "CD")
            }

            @Test
            fun test10() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "CE")
            }

            @Test
            fun test11() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "AB")
            }

            @Test
            fun test12() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "EB")
            }

            @Test
            fun test13() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "CB")
            }

            @Test
            fun test14() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "CDA")
            }

            @Test
            fun test15() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "CDE")
            }

            @Test
            fun test16() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "DA")
            }

            @Test
            fun test17() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "DE")
            }

            @Test
            fun test18() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "ED")
            }

            @Test
            fun test19() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "DC")
            }

        }

        private fun test(source: String, from: String, to: String, expected: String? = null) =
            test(source, from, to, withPrefixFunction = true, expected)

    }

    @Nested
    inner class WithoutPrefixFunction {

        @Nested
        inner class EmptyInput {

            @Test
            fun test1() {
                test(source = "", from = "", to = "", expected = "")
            }

            @Test
            fun test2() {
                test(source = "", from = "AB", to = "BC")
            }

            @Test
            fun test3() {
                test(source = "", from = "", to = "A", expected = "")
            }

        }

        @Nested
        inner class ZeroToSomething {

            @Test
            fun test1() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "", to = "", expected = "ABCABCDAABBBCDDDDABCD")
            }

            @Test
            fun test2() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "", to = "A", expected = "ABCABCDAABBBCDDDDABCD")
            }

            @Test
            fun test3() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "", to = "AB", expected = "ABCABCDAABBBCDDDDABCD")
            }

        }

        @Nested
        inner class OneToZero {

            @Test
            fun test1() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "A", to = "")
            }

            @Test
            fun test2() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "B", to = "")
            }

            @Test
            fun test3() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "D", to = "")
            }

            @Test
            fun test4() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "E", to = "")
            }

        }

        @Nested
        inner class OneToOne {

            @Test
            fun test1() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "A", to = "E")
            }

            @Test
            fun test2() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "A", to = "A")
            }

            @Test
            fun test3() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "B", to = "E")
            }

            @Test
            fun test4() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "B", to = "B")
            }

            @Test
            fun test5() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "D", to = "E")
            }

            @Test
            fun test6() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "D", to = "D")
            }

            @Test
            fun test7() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "E", to = "F")
            }

            @Test
            fun test8() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "E", to = "E")
            }

        }

        @Nested
        inner class OneToMTOne {

            @Test
            fun test1() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "A", to = "ABA")
            }

            @Test
            fun test2() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "A", to = "BAB")
            }

            @Test
            fun test3() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "B", to = "ABA")
            }

            @Test
            fun test4() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "B", to = "BAB")
            }

            @Test
            fun test5() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "D", to = "DCD")
            }

            @Test
            fun test6() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "D", to = "CDC")
            }

            @Test
            fun test7() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "E", to = "FF")
            }

        }

        @Nested
        inner class MTOneToZero {

            @Test
            fun test1() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "")
            }

            @Test
            fun test2() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "")
            }

            @Test
            fun test3() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "")
            }

            @Test
            fun test4() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "EF", to = "GH")
            }

        }

        @Nested
        inner class MTOneToOne {

            @Test
            fun test1() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "A")
            }

            @Test
            fun test2() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "B")
            }

            @Test
            fun test3() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "E")
            }

            @Test
            fun test4() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "B")
            }

            @Test
            fun test5() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "C")
            }

            @Test
            fun test6() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "E")
            }

            @Test
            fun test7() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "C")
            }

            @Test
            fun test8() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "D")
            }

            @Test
            fun test9() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "E")
            }

        }

        @Nested
        inner class MTOneToMTOne {

            @Test
            fun test1() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "ABC")
            }

            @Test
            fun test2() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "ABE")
            }

            @Test
            fun test3() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "BC")
            }

            @Test
            fun test4() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "BE")
            }

            @Test
            fun test5() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "EB")
            }

            @Test
            fun test6() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "AB", to = "BA")
            }

            @Test
            fun test7() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "BCD")
            }

            @Test
            fun test8() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "BCE")
            }

            @Test
            fun test9() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "CD")
            }

            @Test
            fun test10() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "CE")
            }

            @Test
            fun test11() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "AB")
            }

            @Test
            fun test12() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "EB")
            }

            @Test
            fun test13() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "BC", to = "CB")
            }

            @Test
            fun test14() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "CDA")
            }

            @Test
            fun test15() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "CDE")
            }

            @Test
            fun test16() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "DA")
            }

            @Test
            fun test17() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "DE")
            }

            @Test
            fun test18() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "ED")
            }

            @Test
            fun test19() {
                test(source = "ABCABCDAABBBCDDDDABCD", from = "CD", to = "DC")
            }

        }

        private fun test(source: String, from: String, to: String, expected: String? = null) =
            test(source, from, to, withPrefixFunction = false, expected)

    }

    private fun test(source: String, from: String, to: String, withPrefixFunction: Boolean, expected: String? = null) {
        val actualResult = source.kmp(from, to, withPrefixFunction)
        val expectedResult = expected ?: source.replace(from, to)
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

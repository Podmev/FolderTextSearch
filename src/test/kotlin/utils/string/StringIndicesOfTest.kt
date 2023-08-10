package utils.string

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import utils.indicesOf
import java.util.stream.Stream

/*Checking String extension indicesOf* */
class StringIndicesOfTest {

    @ParameterizedTest(name = "string:\"{0}\", token:\"{1}\", expected positions:{2}")
    @MethodSource("notIgnoreCaseProvider")
    fun notIgnoreCaseTest(string: String, token: String, expectedPositions: List<Int>) {
        Assertions.assertEquals(
            /* expected = * */ expectedPositions,
            /* actual = * */ string.indicesOf(token = token, ignoreCase = false).toList()
        )
    }

    @ParameterizedTest(name = "string:\"{0}\", token:\"{1}\", expected positions:{2}")
    @MethodSource("ignoreCaseProvider")
    fun ignoreCaseTest(string: String, token: String, expectedPositions: List<Int>) {
        Assertions.assertEquals(
            /* expected = * */ expectedPositions,
            /* actual = * */ string.indicesOf(token = token, ignoreCase = true).toList()
        )
    }

    companion object {

        /*list input and expected output for case when not ignoring case of symbols
        * [string, token, expected positions]
        *  * */
        @JvmStatic
        fun notIgnoreCaseProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("", "", emptyList<Int>()),
                Arguments.of("abc", "", emptyList<Int>()),
                Arguments.of("", "abc", emptyList<Int>()),
                Arguments.of("abc", "abc", listOf(0)),
                Arguments.of("ababab", "ab", listOf(0, 2, 4)),
                Arguments.of("aaaaa", "aa", listOf(0, 1, 2, 3)),
                //case sensitive
                Arguments.of("ababab", "AB", emptyList<Int>()),
                Arguments.of("abABab", "AB", listOf(2)),
                Arguments.of("abABab", "ab", listOf(0, 4)),
            )
        }

        /*list input and expected output for case when not ignoring case of symbols
        * [string, token, expected positions]
        *  * */
        @JvmStatic
        fun ignoreCaseProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("", "", emptyList<Int>()),
                Arguments.of("abc", "", emptyList<Int>()),
                Arguments.of("", "abc", emptyList<Int>()),
                Arguments.of("abc", "abc", listOf(0)),
                Arguments.of("ababab", "ab", listOf(0, 2, 4)),
                Arguments.of("aaaaa", "aa", listOf(0, 1, 2, 3)),
                //case sensitive
                Arguments.of("ababab", "AB", listOf(0, 2, 4)),
                Arguments.of("abABab", "AB", listOf(0, 2, 4)),
                Arguments.of("abABab", "ab", listOf(0, 2, 4)),
            )
        }
    }
}
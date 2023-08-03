package common

import api.TokenMatch
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertAll

/*Detailed assert for list fo tokenMatches with no exact order of files in search result.
* If error happens, shows exact mismatch.
* */
fun assertEqualsTokenMatches(expectedTokenMatches: List<TokenMatch>, actualTokenMatches: List<TokenMatch>) {
    val expectedTokenMatchesSet = expectedTokenMatches.toSet()
    val actualTokenMatchesSet = actualTokenMatches.toSet()

    val notFoundChecks = buildList<() -> Unit> {
        for (notFoundExpectedTokenMatch in expectedTokenMatchesSet.minus(actualTokenMatchesSet)) {
            add { -> Assertions.fail("Not found some expected TokenMatch in actual: $notFoundExpectedTokenMatch") }
        }
    }
    val extraFoundChecks = buildList<() -> Unit> {
        for (extraTokenMatchInActual in actualTokenMatchesSet.minus(expectedTokenMatchesSet)) {
            add { -> Assertions.fail("Found extra TokenMatch in actual: $extraTokenMatchInActual") }
        }
    }
    assertAll(
        "compare search results",
        { ->
            Assertions.assertEquals(
                expectedTokenMatchesSet.size,
                actualTokenMatchesSet.size,
                "Compare number of found tokens and expected"
            )
        },
        *(notFoundChecks + extraFoundChecks).toTypedArray()
    )
}
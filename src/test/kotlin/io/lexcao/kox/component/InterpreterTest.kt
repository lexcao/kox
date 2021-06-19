package io.lexcao.kox.component

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InterpreterTest {

    private val interpreter = Interpreter()

    @Test
    fun shouldSuccess() {
        val given = "1 * 3 - 2;"

        val actual = interpreter.interpret(
            Parser(Scanner(given).scanTokens()).parse()
        )

        assertEquals(1.0, actual)
    }

    @MethodSource
    @ParameterizedTest(name = "{0}")
    fun interpret(given: String, expect: Any) {
        val actual = interpreter.interpret(
            Parser(Scanner(given).scanTokens()).parse()
        )

        assertEquals(expect, actual)
    }

    private fun interpret() = listOf(
        arguments("1 + 1;", 2.0),
        arguments("1 - 1;", 0.0),
        arguments("2 * 2;", 4.0),
        arguments("9 / 3;", 3.0),
        arguments("1 < 2;", true),
        arguments("2 <= 2;", true),
        arguments("2 > 1;", true),
        arguments("2 >= 2;", true),
        arguments("!true;", false),
        arguments("!false;", true),
        arguments("!(1 < 2);", false),
        arguments("\"hello\" + \" world\";", "hello world"),
        arguments("\"answer is: \" + 99;", "answer is: 99.0"),
    )
}

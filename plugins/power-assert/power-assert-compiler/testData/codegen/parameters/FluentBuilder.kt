import kotlinx.powerassert.Explain
import kotlinx.powerassert.PowerAssert
import kotlinx.powerassert.CallDiagram
import kotlinx.powerassert.toDefaultMessage

@PowerAssert.Ignore
class AssertBuilder(val actual: Any?, val actualDiagram: CallDiagram)

@PowerAssert
fun assertThat(actual: Any?): AssertBuilder {
    return AssertBuilder(actual, PowerAssert.diagram ?: error("no power-assert: assertThat"))
}

@PowerAssert
fun AssertBuilder.isEqualTo(expected: Any?) {
    if (actual != expected) {
        val expectedDiagram = PowerAssert.diagram ?: error("no power-assert: isEqualTo")
        throw AssertionError(buildString {
            appendLine()
            appendLine("Expected:")
            append(expectedDiagram.toDefaultMessage())
            appendLine()
            appendLine("Actual:")
            append(actualDiagram.toDefaultMessage())
        })
    }
}

fun box(): String {
    return test1()
}

fun test1() = expectThrowableMessage {
    @Explain val hello = "Hello"
    @Explain val world = "World".substring(1, 4)

    @Explain
    val expected =
        hello.length
    @Explain val actual = world.length

    assertThat(actual).isEqualTo(expected)
}

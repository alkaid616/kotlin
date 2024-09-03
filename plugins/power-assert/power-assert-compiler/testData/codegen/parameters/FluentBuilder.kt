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
    @PowerAssert val hello = "Hello"
    @PowerAssert val world = "World".substring(1, 4)

    @PowerAssert
    val expected =
        hello.length
    @PowerAssert val actual = world.length

    assertThat(actual).isEqualTo(expected)
}

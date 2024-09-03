// DUMP_KT_IR

import kotlinx.powerassert.PowerAssert
import kotlinx.powerassert.CallDiagram
import kotlinx.powerassert.toDefaultMessage

@PowerAssert
fun assertEquals(expected: Any?, actual: Any?, @PowerAssert.Ignore message: String? = null) {
    if (actual != expected) {
        val diagram = PowerAssert.diagram ?: error("no power-assert")
        throw AssertionError("\n" + diagram.toDefaultMessage())
    }
}

fun box(): String {
    return test1() +
            test2() +
            test3()
}

fun test1() = expectThrowableMessage {
    assertEquals("Hello".length, "World".substring(1, 4).length)
}

fun test2() = expectThrowableMessage {
    assertEquals("Hello".length, "World".substring(1, 4).length, message = "Values are not equal!")
}

fun test3() = expectThrowableMessage {
    val message = "Values are not equal!"
    assertEquals("Hello".length, "World".substring(1, 4).length, message)
}

// DUMP_KT_IR

import kotlinx.powerassert.PowerAssert

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
    assert(expected == actual)
}

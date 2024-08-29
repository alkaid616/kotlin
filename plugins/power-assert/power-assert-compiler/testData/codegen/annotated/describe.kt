// DUMP_KT_IR

import kotlinx.powerassert.PowerAssert
import kotlinx.powerassert.toDefaultMessage

fun box(): String {
    val reallyLongList = listOf("a", "b")
    return describe(reallyLongList.reversed() == emptyList<String>()) ?: "FAIL"
}

@PowerAssert
fun describe(value: Any): String? {
    return PowerAssert.diagram?.toDefaultMessage()
}

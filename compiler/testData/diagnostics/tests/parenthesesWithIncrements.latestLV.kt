// ISSUE: KT-70507
// WITH_STDLIB
// LATEST_LV_DIFFERENCE

object O {
    operator fun inc() = this

    operator fun get(i: Int) = this
    operator fun set(i: Int, o: O) {}
}

fun main() {
    var b = O
    <!PARENTHESIZED_LHS_ERROR!>(b)<!>++

    <!PARENTHESIZED_LHS_ERROR!>(O[1])<!>++
}


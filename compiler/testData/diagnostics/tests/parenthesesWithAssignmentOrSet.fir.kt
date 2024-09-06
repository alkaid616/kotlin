// ISSUE: KT-70507
// DIAGNOSTICS: -VARIABLE_WITH_REDUNDANT_INITIALIZER
// WITH_STDLIB
// LATEST_LV_DIFFERENCE

class A {
    operator fun plus(x: String): A = this
}

fun foo(a: Array<A>) {
    a[0] = a[0] + ""
    a[0] += ""
    <!PARENTHESIZED_LHS_WARNING!>(a[0])<!> += ""
    <!PARENTHESIZED_LHS_WARNING!>(a[0])<!> = a[0]
}

fun bar() {
    var x = ""

    <!PARENTHESIZED_LHS_WARNING!>(x)<!> = ""
    <!PARENTHESIZED_LHS_WARNING!>(x)<!> += ""
}

fun baz() {
    (mutableListOf("")) += ""
}

fun bak() {
    val it = mutableListOf(mutableListOf(10))
    (it[0]) += 20
}

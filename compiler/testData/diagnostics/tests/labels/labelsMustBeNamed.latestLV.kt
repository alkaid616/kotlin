// LATEST_LV_DIFFERENCE

fun foo(a: Any?): Int {
    <!SYNTAX!>@<!>{ ->
        return<!SYNTAX!>@<!>
    }

    <!SYNTAX!>@<!> while(a == null) {
        if (true) {
            break<!SYNTAX!>@<!>
        }
        else {
            continue<!SYNTAX!>@<!>
        }
    }

    var b = 1

    <!PARENTHESIZED_LHS_ERROR!>(<!SYNTAX!>@<!> b)<!> = 2

    return<!SYNTAX!>@<!> 1
}

open class A {
    fun foo() {}
}

class B : A() {
    fun bar() {
        this<!SYNTAX!>@<!>.foo()
        super<!SYNTAX!>@<!>.foo()
    }
}

fun bar(f: () -> Unit) = f
fun test() {
    bar <!SYNTAX!>@<!>{}
}

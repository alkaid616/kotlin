// WITH_STDLIB

fun stringF(): String = ""

fun Any.consume(): Unit = Unit

fun stringLambda(l: () -> String) {
    <!RETURN_VALUE_NOT_USED!>l()<!> // unused
}

fun unitLambda(l: () -> Unit) {
    l()
}

fun stringLambdaReturns(l: () -> String): String {
    return l()
}

fun main() {
    stringLambda {
        stringF() // used
    }
    unitLambda {
        stringF() // TODO: unused because of coercion, not supported for now
    }
    <!RETURN_VALUE_NOT_USED!>stringLambdaReturns {
        stringF()
    }<!> // stringF() is used, stringLambdaReturns is unused
}

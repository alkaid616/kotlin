// K2 test crashes with "IrSimpleFunctionSymbolImpl is already bound"

// MODULE: m1-common
// FILE: common.kt

fun foo() {}
class Foo

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

<!CONFLICTING_OVERLOADS!>fun foo()<!> {}
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>

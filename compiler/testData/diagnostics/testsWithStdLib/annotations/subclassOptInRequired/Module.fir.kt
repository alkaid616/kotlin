// MODULE: a
package a

@RequiresOptIn
annotation class Boom

@SubclassOptInRequired(Boom::class)
open class B {}

// MODULE: b(a)
package b
import a.B

class C : <!OPT_IN_USAGE_ERROR!>B<!>()

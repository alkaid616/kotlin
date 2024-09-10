// MODULE: a
package a

@RequiresOptIn
annotation class Boom

@SubclassOptInRequired(Boom::class)
open class B {}

// MODULE: b(a)
package b
import a.B

class C : B()
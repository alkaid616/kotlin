// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: ClassMappings
// FILE: classes.kt
import kotlin.native.internal.reflect.objCNameOrNull

class FinalClass {
    class NestedFinalClass
}
open class OpenClass
private class PrivateClass : OpenClass()

fun isAnyClassNameNull(): Boolean = Any::class.objCNameOrNull == null
fun getAnyClassName(): String = Any::class.objCNameOrNull!!

fun isFinalClassNameNull(): Boolean = FinalClass::class.objCNameOrNull == null
fun getFinalClassName(): String = FinalClass::class.objCNameOrNull!!

fun isNestedFinalClassNameNull(): Boolean = FinalClass.NestedFinalClass::class.objCNameOrNull == null
fun getNestedFinalClassName(): String = FinalClass.NestedFinalClass::class.objCNameOrNull!!

fun isOpenClassNameNull(): Boolean = OpenClass::class.objCNameOrNull == null
fun getOpenClassName(): String = OpenClass::class.objCNameOrNull!!

fun isPrivateClassNameNull(): Boolean = PrivateClass::class.objCNameOrNull == null
fun getPrivateClassName(): String = PrivateClass::class.objCNameOrNull!!

// FILE: classes_in_namespace.kt
package namespace

import kotlin.native.internal.reflect.objCNameOrNull

class NamespacedFinalClass

fun isNamespacedFinalClassNameNull(): Boolean = NamespacedFinalClass::class.objCNameOrNull == null
fun getNamespacedFinalClassName(): String = NamespacedFinalClass::class.objCNameOrNull!!
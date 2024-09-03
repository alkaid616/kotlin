/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class PowerAssert {
    @Target(AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.RUNTIME)
    @MustBeDocumented
    public annotation class Ignore

    public companion object {
        @Suppress("RedundantNullableReturnType")
        public val diagram: CallDiagram?
            get() = throw NotImplementedError("Intrinsic property! Make sure the Power-Assert compiler-plugin is applied to your build.")
    }

}

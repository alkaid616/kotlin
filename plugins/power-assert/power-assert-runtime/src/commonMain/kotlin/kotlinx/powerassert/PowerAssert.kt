/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

public annotation class PowerAssert {
    companion object {
        @Suppress("RedundantNullableReturnType")
        public val diagram: CallDiagram?
            get() = error("Power-Assert compiler-plugin must be applied when project is compiled.")
    }
}

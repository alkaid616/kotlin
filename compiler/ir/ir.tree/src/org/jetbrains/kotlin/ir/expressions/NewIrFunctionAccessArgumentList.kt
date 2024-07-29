/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

abstract class NewIrFunctionAccessArgumentList : AbstractMutableList<IrExpression?>() {
    @Deprecated("Use [] syntax", level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("this[index]"))
    operator fun invoke(index: Int): IrExpression? = error("Not implemented")
}
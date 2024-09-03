/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.name.FqName

val POWER_ASSERT_FUNCTION by IrDeclarationOriginImpl.Synthetic

val ExplainAnnotation = FqName("kotlinx.powerassert.Explain")
val PowerAssertAnnotation = FqName("kotlinx.powerassert.PowerAssert")
val PowerAssertIgnoreAnnotation = FqName("kotlinx.powerassert.PowerAssert.Ignore")
val PowerAssertGetDiagram = FqName("kotlinx.powerassert.PowerAssert.Companion.<get-diagram>")

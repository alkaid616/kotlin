/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers

import org.jetbrains.kotlin.backend.common.checkers.IrInlineDeclarationChecker.InlineFunctionInfo
import org.jetbrains.kotlin.backend.common.diagnostics.SerializationErrors
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.irError
import org.jetbrains.kotlin.ir.util.isConsideredAsPrivateForInlining
import org.jetbrains.kotlin.ir.visitors.IrTypeVisitor

/**
 * Reports an IR-level diagnostic whenever a private type is used within an `inline` function with broader visibility.
 */
class IrInlineDeclarationChecker(
    private val diagnosticReporter: IrDiagnosticReporter,
) : IrTypeVisitor<Unit, InlineFunctionInfo?>() {

    private var currentFile: IrFile? = null

    data class InlineFunctionInfo(
        val function: IrFunction,
    )

    override fun visitTypeRecursively(container: IrElement, type: IrType, data: InlineFunctionInfo?) {
        if (data == null) return
        val klass = type.classifierOrNull?.takeIf { it.isBound }?.owner as? IrClass ?: return
        // TODO: Test private types declared inside the inline function body!!!
        if (DescriptorVisibilities.isPrivate(klass.visibility)) {
            val file = currentFile ?: irError("Cannot show a diagnostic without a file") {
                withIrEntry("element", container)
                withIrEntry("inline function", data.function)
            }
            diagnosticReporter.at(container, file).report(
                SerializationErrors.IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION,
                data.function,
                klass,
            )
        }
    }

    override fun visitElement(element: IrElement, data: InlineFunctionInfo?) {
        element.acceptChildren(this, data)
    }

    override fun visitFile(declaration: IrFile, data: InlineFunctionInfo?) {
        currentFile = declaration
        declaration.acceptChildren(this, null)
    }

    override fun visitFunction(declaration: IrFunction, data: InlineFunctionInfo?) {
        val inlineFunctionInfo = if (declaration.isInline && !declaration.isConsideredAsPrivateForInlining()) {
            InlineFunctionInfo(declaration)
        } else {
            null
        }
        declaration.acceptChildren(this, inlineFunctionInfo)
    }
}
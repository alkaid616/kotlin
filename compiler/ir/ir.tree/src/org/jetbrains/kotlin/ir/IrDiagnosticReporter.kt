/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticReporterWithContext.DiagnosticContextImpl
import org.jetbrains.kotlin.diagnostics.chooseFactory
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.isPropertyAccessor
import org.jetbrains.kotlin.ir.util.isSetter

interface IrDiagnosticReporter {
    val languageVersionSettings: LanguageVersionSettings
    fun at(irDeclaration: IrDeclaration): DiagnosticContextImpl
    fun at(irElement: IrElement, containingIrFile: IrFile): DiagnosticContextImpl
    fun at(irElement: IrElement, containingIrDeclaration: IrDeclaration): DiagnosticContextImpl
    fun at(sourceElement: AbstractKtSourceElement?, irElement: IrElement, containingFile: IrFile): DiagnosticContextImpl
}

object IrDiagnosticRenderers {
    val SYMBOL_OWNER_DECLARATION_FQ_NAME = Renderer<IrSymbol> {
        (it.owner as? IrDeclarationWithName)?.fqNameWhenAvailable?.asString() ?: "unknown name"
    }
    val DECLARATION_NAME = Renderer<IrDeclarationWithName> { it.name.asString() }

    /**
     * Inspired by [org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.SYMBOL_KIND].
     */
    val DECLARATION_KIND = Renderer<IrDeclaration> { declaration ->
        when (declaration) {
            is IrSimpleFunction -> when {
                declaration.isPropertyAccessor -> "property accessor"
                else -> "function"
            }
            is IrConstructor -> "constructor"
            is IrProperty -> "property"
            else -> "declaration"
        }
    }
}

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.fir.backend.utils.defaultTypeWithoutArguments
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotationWithEqualFqName
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.powerassert.builder.diagram.CallDiagramFactory

class PowerAssertFunctionTransformer(
    private val context: IrPluginContext,
) : DeclarationTransformer {

    val transformed = mutableMapOf<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>()

    private val callDiagramType by lazy {
        context.referenceClass(CallDiagramFactory.classIdCallDiagram)!!.defaultTypeWithoutArguments
    }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction) {
            if (declaration.hasAnnotation(PowerAssertAnnotation)) {
                return lower(declaration)
            }
        }

        return null
    }

    private fun lower(irFunction: IrSimpleFunction): List<IrSimpleFunction> {
        val newIrFunction = irFunction.deepCopyWithSymbols(irFunction.parent).apply {
            origin = POWER_ASSERT_FUNCTION
            name = Name.identifier(irFunction.name.identifier + "\$powerassert")
            annotations = annotations.filter { !it.isAnnotationWithEqualFqName(PowerAssertAnnotation) }
            addValueParameter {
                name = Name.identifier("\$diagram") // TODO what if there's another property with this name?
                type = callDiagramType
            }
        }

        // Transform the generated function to use the `$diagram` parameter instead of PowerAssert.diagram.
        val diagramParameter = newIrFunction.valueParameters.last()
        newIrFunction.transformChildrenVoid(DiagramDispatchTransformer(diagramParameter, context))

        // Transform the original function to use `null` instead of PowerAssert.diagram.
        // This keeps the code from throwing an error when PowerAssert.diagram.
        // This in turn helps make sure the compiler-plugin is applied to functions which use `@PowerAssert`.
        irFunction.transformChildrenVoid(DiagramDispatchTransformer(diagram = null, context))

        transformed[irFunction.symbol] = newIrFunction.symbol

        return listOf(irFunction, newIrFunction)
    }

    private class DiagramDispatchTransformer(
        private val diagram: IrValueParameter?,
        private val context: IrPluginContext,
    ) : IrElementTransformerVoidWithContext() {
        override fun visitExpression(expression: IrExpression): IrExpression {
            return when {
                isPowerAssertDiagram(expression) -> when (diagram) {
                    null -> IrConstImpl.constNull(expression.startOffset, expression.endOffset, context.irBuiltIns.anyType.makeNullable())
                    else -> IrGetValueImpl(expression.startOffset, expression.endOffset, diagram.type, diagram.symbol)
                }
                else -> super.visitExpression(expression)
            }
        }

        private fun isPowerAssertDiagram(expression: IrExpression): Boolean =
            (expression as? IrCall)?.symbol?.owner?.kotlinFqName == PowerAssertGetDiagram
    }
}

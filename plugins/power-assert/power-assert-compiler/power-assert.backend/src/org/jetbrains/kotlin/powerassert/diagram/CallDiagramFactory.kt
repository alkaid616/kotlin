/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.diagram

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.backend.utils.defaultTypeWithoutArguments
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class CallDiagramFactory(
    private val irPluginContext: IrPluginContext,
) {
    companion object {
        private val packageFqName = FqName("kotlinx.powerassert")
        val classIdCallDiagram = ClassId(packageFqName, Name.identifier("CallDiagram"))
        val classIdValueParameter = classIdCallDiagram.createNestedClassId(Name.identifier("ValueParameter"))
        val classIdReceiver = classIdCallDiagram.createNestedClassId(Name.identifier("Receiver"))
        val classIdDisplayable = classIdCallDiagram.createNestedClassId(Name.identifier("Displayable"))
        val classIdExpression = classIdCallDiagram.createNestedClassId(Name.identifier("Expression"))
        val classIdEqualityExpression = classIdCallDiagram.createNestedClassId(Name.identifier("EqualityExpression"))
    }

    private val displayableType by lazy {
        irPluginContext.referenceClass(classIdDisplayable)!!.defaultTypeWithoutArguments
    }

    private val pairType by lazy {
        val classId = ClassId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("Pair"))
        val pairSymbol = irPluginContext.referenceClass(classId)!!
        val valueParameterSymbol = irPluginContext.referenceClass(classIdValueParameter)!!
        pairSymbol.typeWith(irPluginContext.irBuiltIns.stringType, valueParameterSymbol.defaultTypeWithoutArguments)
    }

    private val pairConstructorSymbol by lazy {
        val classId = ClassId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("Pair"))
        irPluginContext.referenceConstructors(classId).single()
    }

    private val mapOfSymbol by lazy {
        val callableId = CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier("mapOf"))
        irPluginContext.referenceFunctions(callableId).single { it.owner.valueParameters.firstOrNull()?.isVararg == true }
    }

    private val listOfSymbol by lazy {
        val callableId = CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier("listOf"))
        irPluginContext.referenceFunctions(callableId).single { it.owner.valueParameters.firstOrNull()?.isVararg == true }
    }

    private fun IrBuilderWithScope.irPair(first: IrExpression, second: IrExpression): IrExpression {
        return irCall(pairConstructorSymbol).apply {
            putValueArgument(0, first)
            putValueArgument(1, second)
        }
    }

    private fun IrBuilderWithScope.irMapOf(map: Map<String, IrExpression>): IrExpression {
        return irCall(mapOfSymbol).apply {
            val entries = map.entries.map { (key, value) -> irPair(irString(key), value) }
            putValueArgument(0, irVararg(elementType = pairType, values = entries))
        }
    }

    private fun IrBuilderWithScope.irListOf(list: List<IrExpression>): IrExpression {
        return irCall(listOfSymbol).apply {
            putValueArgument(0, irVararg(elementType = displayableType, values = list))
        }
    }

    fun IrBuilderWithScope.irCallDiagram(
        source: IrExpression,
        dispatchReceiver: IrExpression?,
        extensionReceiver: IrExpression?,
        valueParameters: Map<String, IrExpression>,
    ): IrConstructorCall {
        val constructor = irPluginContext.referenceConstructors(classIdCallDiagram)
            .singleOrNull { it.owner.isPrimary }
            ?: error("No primary constructor found for 'kotlinx.powerassert.CallDiagram'")
        return irCall(constructor).apply {
            putValueArgument(0, source)
            putValueArgument(1, dispatchReceiver)
            putValueArgument(2, extensionReceiver)
            putValueArgument(3, irMapOf(valueParameters))
        }
    }

    fun IrBuilderWithScope.irValueParameter(expressions: List<IrExpression>): IrConstructorCall {
        val constructor = irPluginContext.referenceConstructors(classIdValueParameter)
            .singleOrNull { it.owner.isPrimary }
            ?: error("No primary constructor found for 'kotlinx.powerassert.CallDiagram.ValueParameter'")
        return irCall(constructor).apply {
            putValueArgument(0, irListOf(expressions))
        }
    }

    fun IrBuilderWithScope.irReceiver(implicit: IrExpression, expressions: List<IrExpression>): IrConstructorCall {
        val constructor =
            irPluginContext.referenceConstructors(classIdReceiver)
                .singleOrNull { it.owner.isPrimary }
                ?: error("No primary constructor found for 'kotlinx.powerassert.CallDiagram.Receiver'")
        return irCall(constructor).apply {
            putValueArgument(0, implicit)
            putValueArgument(1, irListOf(expressions))
        }
    }

    fun IrBuilderWithScope.irExpression(
        startOffset: Int,
        endOffset: Int,
        displayOffset: Int,
        value: IrExpression,
    ): IrConstructorCall {
        val constructor = irPluginContext.referenceConstructors(classIdExpression)
            .singleOrNull { it.owner.isPrimary }
            ?: error("No primary constructor found for 'kotlinx.powerassert.CallDiagram.Expression'")
        return irCall(constructor).apply {
            putValueArgument(0, irInt(startOffset))
            putValueArgument(1, irInt(endOffset))
            putValueArgument(2, irInt(displayOffset))
            putValueArgument(3, value)
        }
    }

    fun IrBuilderWithScope.irEqualityExpression(
        startOffset: Int,
        endOffset: Int,
        displayOffset: Int,
        value: IrExpression,
        lhs: IrExpression,
        rhs: IrExpression,
    ): IrConstructorCall {
        val constructor = irPluginContext.referenceConstructors(classIdEqualityExpression)
            .singleOrNull { it.owner.isPrimary }
            ?: error("No primary constructor found for 'kotlinx.powerassert.CallDiagram.EqualityExpression'")
        return irCall(constructor).apply {
            putValueArgument(0, irInt(startOffset))
            putValueArgument(1, irInt(endOffset))
            putValueArgument(2, irInt(displayOffset))
            putValueArgument(3, value)
            putValueArgument(4, lhs)
            putValueArgument(5, rhs)
        }
    }

    fun IrBuilderWithScope.irDefaultMessage(callDiagram: IrExpression): IrCall {
        TODO("Not yet implemented")
    }
}

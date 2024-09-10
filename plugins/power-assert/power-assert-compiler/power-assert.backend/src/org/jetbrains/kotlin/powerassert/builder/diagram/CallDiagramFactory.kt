/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.builder.diagram

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.powerassert.PowerAssertBuiltIns

class CallDiagramFactory(
    private val builtIns: PowerAssertBuiltIns,
) {
    private fun IrBuilderWithScope.irPair(first: IrExpression, second: IrExpression): IrExpression {
        return irCall(builtIns.pairConstructor).apply {
            putValueArgument(0, first)
            putValueArgument(1, second)
        }
    }

    private fun IrBuilderWithScope.irMapOf(map: Map<String, IrExpression>): IrExpression {
        return irCall(builtIns.mapOfFunction).apply {
            val entries = map.entries.map { (key, value) -> irPair(irString(key), value) }
            val elementType = builtIns.pairType(builtIns.stringType, builtIns.valueParameterType)
            putValueArgument(0, irVararg(elementType = elementType, values = entries))
        }
    }

    private fun IrBuilderWithScope.irListOf(list: List<IrExpression>): IrExpression {
        return irCall(builtIns.listOfFunction).apply {
            putValueArgument(0, irVararg(elementType = builtIns.expressionType, values = list))
        }
    }

    fun IrBuilderWithScope.irVariableDiagram(
        source: IrExpression,
        assignment: IrExpression,
    ): IrConstructorCall {
        return irCall(builtIns.variableExplanationConstructor).apply {
            putValueArgument(0, source)
            putValueArgument(1, assignment)
        }
    }

    fun IrBuilderWithScope.irAssignment(expressions: List<IrExpression>): IrConstructorCall {
        return irCall(builtIns.initializerConstructor).apply {
            putValueArgument(0, irListOf(expressions))
        }
    }

    fun IrBuilderWithScope.irCallDiagram(
        source: IrExpression,
        dispatchReceiver: IrExpression?,
        extensionReceiver: IrExpression?,
        valueParameters: Map<String, IrExpression>,
    ): IrConstructorCall {
        return irCall(builtIns.callExplanationConstructor).apply {
            putValueArgument(0, source)
            putValueArgument(1, dispatchReceiver)
            putValueArgument(2, extensionReceiver)
            putValueArgument(3, irMapOf(valueParameters))
        }
    }

    fun IrBuilderWithScope.irValueParameter(expressions: List<IrExpression>): IrConstructorCall {
        return irCall(builtIns.valueParameterConstructor).apply {
            putValueArgument(0, irListOf(expressions))
        }
    }

    fun IrBuilderWithScope.irReceiver(expressions: List<IrExpression>): IrConstructorCall {
        return irCall(builtIns.receiverConstructor).apply {
            putValueArgument(0, irListOf(expressions))
        }
    }

    fun IrBuilderWithScope.irExpression(
        startOffset: Int,
        endOffset: Int,
        displayOffset: Int,
        value: IrExpression,
    ): IrConstructorCall {
        return irCall(builtIns.expressionConstructor).apply {
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
        return irCall(builtIns.equalityExpressionConstructor).apply {
            putValueArgument(0, irInt(startOffset))
            putValueArgument(1, irInt(endOffset))
            putValueArgument(2, irInt(displayOffset))
            putValueArgument(3, value)
            putValueArgument(4, lhs)
            putValueArgument(5, rhs)
        }
    }

    fun IrBuilderWithScope.irVariableAccessExpression(
        startOffset: Int,
        endOffset: Int,
        displayOffset: Int,
        value: IrExpression,
        diagram: IrExpression,
    ): IrConstructorCall {
        return irCall(builtIns.variableAccessExpressionConstructor).apply {
            putValueArgument(0, irInt(startOffset))
            putValueArgument(1, irInt(endOffset))
            putValueArgument(2, irInt(displayOffset))
            putValueArgument(3, value)
            putValueArgument(4, diagram)
        }
    }

    fun IrBuilderWithScope.irDefaultMessage(callDiagram: IrExpression): IrCall {
        return irCall(builtIns.toDefaultMessageFunction).apply {
            extensionReceiver = callDiagram
        }
    }
}

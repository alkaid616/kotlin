/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.builder.diagram

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.powerassert.diagram.IrTemporaryVariable
import org.jetbrains.kotlin.powerassert.diagram.SourceFile
import org.jetbrains.kotlin.powerassert.diagram.irDiagramString

class StringDiagramBuilder(
    private val sourceFile: SourceFile,
    private val originalCall: IrCall,
    private val function: IrSimpleFunction,
    private val messageArgument: IrExpression?,
) : DiagramBuilder {
    override fun build(
        builder: IrBuilderWithScope,
        dispatchReceiver: List<IrTemporaryVariable>?,
        extensionReceiver: List<IrTemporaryVariable>?,
        valueParameters: Map<String, List<IrTemporaryVariable>>,
    ): IrExpression {
        return builder.irDiagramString(
            sourceFile,
            messageArgument?.let { builder.buildMessagePrefix(it, function.valueParameters.last()) },
            originalCall,
            dispatchReceiver.orEmpty() + extensionReceiver.orEmpty() + valueParameters.values.flatten()
        )
    }

    private fun IrBuilderWithScope.buildMessagePrefix(
        messageArgument: IrExpression?,
        messageParameter: IrValueParameter,
    ): IrExpression? {
        return when (messageArgument) {
            null -> null
            is IrConst -> messageArgument
            is IrStringConcatenation -> messageArgument
            is IrGetValue -> {
                if (messageArgument.type == context.irBuiltIns.stringType) {
                    return messageArgument
                } else {
                    val invoke = messageParameter.type.classOrNull!!.functions
                        .filter { !it.owner.isFakeOverride } // TODO best way to find single access method?
                        .single()
                    irCall(invoke).apply { dispatchReceiver = messageArgument }
                }
            }
            // Kotlin Lambda or SAMs conversion lambda
            is IrFunctionExpression, is IrTypeOperatorCall -> {
                val invoke = messageParameter.type.classOrNull!!.functions
                    .filter { !it.owner.isFakeOverride } // TODO best way to find single access method?
                    .single()
                irCall(invoke).apply { dispatchReceiver = messageArgument }
            }
            else -> null
        }
    }
}

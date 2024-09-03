/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.builder.diagram

import org.jetbrains.kotlin.ir.BuiltInOperatorNames
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.powerassert.ExplainAnnotation
import org.jetbrains.kotlin.powerassert.diagram.IrTemporaryVariable
import org.jetbrains.kotlin.powerassert.diagram.SourceFile

class CallDiagramDiagramBuilder(
    private val factory: CallDiagramFactory,
    private val sourceFile: SourceFile,
    private val originalCall: IrCall,
    private val variableDiagrams: Map<IrVariable, IrVariable>,
) : DiagramBuilder {
    override fun build(
        builder: IrBuilderWithScope,
        dispatchReceiver: List<IrTemporaryVariable>?,
        extensionReceiver: List<IrTemporaryVariable>?,
        valueParameters: Map<String, List<IrTemporaryVariable>>,
    ): IrExpression {
        return builder.irCallDiagram(
            factory,
            sourceFile,
            originalCall,
            dispatchReceiver,
            extensionReceiver,
            valueParameters,
            variableDiagrams,
        )
    }
}

private fun IrBuilderWithScope.irCallDiagram(
    factory: CallDiagramFactory,
    sourceFile: SourceFile,
    call: IrCall,
    dispatchReceiver: List<IrTemporaryVariable>?,
    extensionReceiver: List<IrTemporaryVariable>?,
    valueParameters: Map<String, List<IrTemporaryVariable>>,
    variableDiagrams: Map<IrVariable, IrVariable>,
): IrExpression {
    val callInfo = sourceFile.getSourceRangeInfo(call)

    // Get call source string starting at the very beginning of the first line.
    // This is so multiline calls all start from the same column offset.
    val startOffset = callInfo.startOffset - callInfo.startColumnNumber
    val indentedSource = sourceFile.getText(startOffset, callInfo.endOffset)
        .clearSourcePrefix(callInfo.startColumnNumber)
    val rows = indentedSource.split("\n")

    // TODO should we even do this trimIndent()? make it a diagram creation problem?
    val lineIndent = rows.minOf { line ->
        // Find index of first non-whitespace character.
        val indent = line.indexOfFirst { !it.isWhitespace() }
        if (indent == -1) Int.MAX_VALUE else indent
    }

    fun IrBuilderWithScope.irExpression(it: IrTemporaryVariable): IrConstructorCall {
        val value = it.toValueDisplay(callInfo)
        val previousRows = rows.subList(0, value.row)
        val rowPadding = previousRows.sumOf { it.length + 1 } // TODO cache?
        val offsetAdjust = lineIndent + previousRows.sumOf { minOf(it.length, lineIndent) } // TODO cache?
        return with(factory) {
            val initializer = it.temporary.initializer

            if (it.temporary.hasAnnotation(ExplainAnnotation) && it.temporary in variableDiagrams) {
                irVariableAccessExpression(
                    startOffset = it.sourceRangeInfo.startOffset - startOffset - offsetAdjust,
                    endOffset = it.sourceRangeInfo.endOffset - startOffset - offsetAdjust,
                    displayOffset = rowPadding + value.indent - offsetAdjust,
                    value = irGet(value.value),
                    diagram = irGet(variableDiagrams.getValue(it.temporary)),
                )
            } else if (
                initializer is IrCall &&
                initializer.symbol.owner.name.asString() == BuiltInOperatorNames.EQEQ &&
                initializer.origin == IrStatementOrigin.EQEQ
            ) {
                val lhs = initializer.getValueArgument(0)!!
                val rhs = initializer.getValueArgument(1)!!
                irEqualityExpression(
                    startOffset = it.sourceRangeInfo.startOffset - startOffset - offsetAdjust,
                    endOffset = it.sourceRangeInfo.endOffset - startOffset - offsetAdjust,
                    displayOffset = rowPadding + value.indent - offsetAdjust,
                    value = irGet(value.value),
                    lhs = lhs.deepCopyWithSymbols(),
                    rhs = rhs.deepCopyWithSymbols(),
                )
            } else {
                irExpression(
                    startOffset = it.sourceRangeInfo.startOffset - startOffset - offsetAdjust,
                    endOffset = it.sourceRangeInfo.endOffset - startOffset - offsetAdjust,
                    displayOffset = rowPadding + value.indent - offsetAdjust,
                    value = irGet(value.value),
                )
            }
        }
    }

    val source = rows.joinToString("\n") { it.substring(startIndex = minOf(it.length, lineIndent)) }
    return with(factory) {
        irCallDiagram(
            source = irString(source),
            dispatchReceiver = dispatchReceiver?.let { variables ->
                irReceiver(expressions = dispatchReceiver.map { irExpression(it) })
            },
            extensionReceiver = extensionReceiver?.let { variables ->
                irReceiver(expressions = extensionReceiver.map { irExpression(it) })
            },
            valueParameters = valueParameters.mapValues { (_, variables) ->
                irValueParameter(expressions = variables.map { irExpression(it) })
            },
        )
    }
}

fun String.clearSourcePrefix(offset: Int): String = buildString {
    for ((i, c) in this@clearSourcePrefix.withIndex()) {
        when {
            i >= offset -> {
                // Append the remaining characters and exit.
                append(this@clearSourcePrefix.substring(i))
                break
            }
            c == '\t' -> append('\t') // Preserve tabs.
            else -> append(' ') // Replace all other characters with spaces.
        }
    }
}

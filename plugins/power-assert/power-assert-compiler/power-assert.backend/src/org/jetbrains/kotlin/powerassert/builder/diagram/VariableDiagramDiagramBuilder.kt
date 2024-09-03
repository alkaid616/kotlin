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

fun IrBuilderWithScope.irVariableDiagram(
    factory: CallDiagramFactory,
    sourceFile: SourceFile,
    variable: IrVariable,
    variables: List<IrTemporaryVariable>,
    variableDiagrams: Map<IrVariable, IrVariable>,
): IrExpression {
    val variableInfo = sourceFile.getSourceRangeInfo(
        // TODO K1 and K2 have different offsets for the variable...
        //  K2 doesn't include the annotations and val/var keyword
        minOf(
            variable.startOffset,
            variable.annotations.minOfOrNull { it.startOffset } ?: variable.startOffset
        ),
        variable.initializer!!.endOffset,
    )

    // Get call source string starting at the very beginning of the first line.
    // This is so multiline calls all start from the same column offset.
    val startOffset = variableInfo.startOffset - variableInfo.startColumnNumber
    val indentedSource = sourceFile.getText(startOffset, variableInfo.endOffset)
        .clearSourcePrefix(variableInfo.startColumnNumber)
    val rows = indentedSource.split("\n")

    // TODO should we even do this trimIndent()? make it a diagram creation problem?
    val lineIndent = rows.minOf { line ->
        // Find index of first non-whitespace character.
        val indent = line.indexOfFirst { !it.isWhitespace() }
        if (indent == -1) Int.MAX_VALUE else indent
    }

    fun IrBuilderWithScope.irExpression(it: IrTemporaryVariable): IrConstructorCall {
        val value = it.toValueDisplay(variableInfo)
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
        irVariableDiagram(
            source = irString(source),
            assignment = irAssignment(expressions = variables.map { irExpression(it) }),
        )
    }
}

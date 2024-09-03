/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

public class CallDiagram(
    public val source: String,
    public val dispatchReceiver: Receiver?,
    public val extensionReceiver: Receiver?,
    public val valueParameters: Map<String, ValueParameter>,
) {
    public abstract class Parameter internal constructor() {
        public abstract val expressions: List<Expression>
    }

    public class ValueParameter(
        override val expressions: List<Expression>,
    ) : Parameter()

    public class Receiver(
        override val expressions: List<Expression>,
    ) : Parameter()
}

public fun CallDiagram.toDefaultMessage(
    render: Expression.() -> String = Expression::render,
): String {
    val callExpressions = buildList {
        if (dispatchReceiver != null) addAll(dispatchReceiver.expressions)
        if (extensionReceiver != null) addAll(extensionReceiver.expressions)
        for (valueParameter in valueParameters.values) {
            addAll(valueParameter.expressions)
        }
    }

    return buildString {
        appendDiagram(source, callExpressions, render)

        val variableDiagrams = callExpressions
            .filterIsInstance<VariableAccessExpression>()
            .mapNotNull { it.diagram }
            .toMutableList()

        var index = 0
        while (index < variableDiagrams.size) {
            appendLine()
            appendLine(if (index == 0) "With:" else "And:")

            val diagram = variableDiagrams[index++]
            val assignmentExpressions = diagram.assignment.expressions
            appendDiagram(diagram.source, assignmentExpressions, render)
            assignmentExpressions
                .filterIsInstance<VariableAccessExpression>()
                .mapNotNull { it.diagram }
                .let(variableDiagrams::addAll)
        }
    }
}

private fun StringBuilder.appendDiagram(
    source: String,
    expressions: List<Expression>,
    render: Expression.() -> String,
) {
    val rows = source.split("\n")

    class DiagramValue(
        val row: Int,
        val indent: Int,
        val result: String,
    ) {
        fun overlaps(other: DiagramValue): Boolean {
            return result.length >= (other.indent - indent)
        }
    }

    fun Expression.toDiagramValue(): DiagramValue {
        val prefix = source.substring(startIndex = 0, endIndex = displayOffset)
        val row = prefix.count { it == '\n' }
        val rowOffset = if (row == 0) 0 else (prefix.substringBeforeLast("\n").length + 1)
        return DiagramValue(
            row = row,
            indent = displayOffset - rowOffset,
            result = render(),
        )
    }

    val valuesByRow = expressions.sortedBy { it.displayOffset }
        .map { it.toDiagramValue() }
        .groupBy { it.row }

    run {
        var separationNeeded = false
        for ((rowIndex, rowSource) in rows.withIndex()) {
            // Add an extra blank line if needed between values and source code.
            if (separationNeeded && rowSource.isNotBlank()) appendLine()
            separationNeeded = false

            appendLine(rowSource)

            val rowValues = valuesByRow[rowIndex] ?: continue
            separationNeeded = true

            run {
                // Print the first row of displayable indicators.
                var currentIndent = 0
                for (indent in rowValues.map { it.indent }) {
                    repeat(indent - currentIndent) {
                        if (rowSource[currentIndent + it] == '\t') append('\t')
                        else append(' ')
                    }
                    append('|')
                    currentIndent = indent + 1
                }
                appendLine()
            }

            // If a display value other than the last value covers a tab, it cannot be displayed with this row.
            // Precalculate all values that cover a tab so they can be excluded as needed.
            val valuesCoveringTab = mutableSetOf<DiagramValue>()
            for (value in rowValues) {
                if ('\t' in rowSource.substring(value.indent, minOf(rowSource.length, value.indent + value.result.length))) {
                    valuesCoveringTab.add(value)
                }
            }

            val remaining = rowValues.toMutableList()
            while (remaining.isNotEmpty()) {
                // Figure out which displays will fit on this row.
                val displayRow = remaining.windowed(2, partialWindows = true)
                    .filter { it.size == 1 || it[0] !in valuesCoveringTab && !it[0].overlaps(it[1]) }
                    .map { it[0] }
                    .toSet()

                var currentIndent = 0
                for (diagramValue in remaining) {
                    repeat(diagramValue.indent - currentIndent) {
                        if (rowSource[currentIndent + it] == '\t') append('\t')
                        else append(' ')
                    }
                    val result = if (diagramValue in displayRow) diagramValue.result else "|"
                    append(result)
                    currentIndent = diagramValue.indent + result.length
                }
                appendLine()

                remaining -= displayRow
            }
        }
    }
}

public fun Expression.render(): String {
    if (this is EqualityExpression && value == false) {
        return "Expected <$lhs>, actual <$rhs>."
    }

    return value.toString()
}

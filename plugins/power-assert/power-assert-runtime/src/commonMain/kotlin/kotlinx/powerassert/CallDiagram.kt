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
        public abstract val expressions: List<Displayable>
    }

    public class ValueParameter(
        override val expressions: List<Displayable>,
    ) : Parameter()

    public class Receiver(
        public val implicit: Boolean,
        override val expressions: List<Displayable>,
    ) : Parameter()

    public abstract class Displayable internal constructor() {
        public abstract val startOffset: Int
        public abstract val endOffset: Int
        public abstract val displayOffset: Int
        public abstract val value: Any?
    }

    public class Expression(
        override val startOffset: Int,
        override val endOffset: Int,
        override val displayOffset: Int,
        override val value: Any?,
    ) : Displayable()

    public class EqualityExpression(
        override val startOffset: Int,
        override val endOffset: Int,
        override val displayOffset: Int,
        override val value: Any?,
        public val lhs: Any?,
        public val rhs: Any?,
    ) : Displayable()
}

public fun CallDiagram.toDefaultMessage(
    render: CallDiagram.Displayable.() -> String = CallDiagram.Displayable::render,
): String {
    val rows = source.split("\n")
    val expressions = buildList {
        if (dispatchReceiver != null && !dispatchReceiver.implicit) addAll(dispatchReceiver.expressions)
        if (extensionReceiver != null && !extensionReceiver.implicit) addAll(extensionReceiver.expressions)
        for (valueParameter in valueParameters.values) {
            addAll(valueParameter.expressions)
        }
    }

    class DiagramValue(
        val row: Int,
        val indent: Int,
        val result: String,
    ) {
        fun overlaps(other: DiagramValue): Boolean {
            return result.length >= (other.indent - indent)
        }
    }

    fun CallDiagram.Displayable.toDiagramValue(): DiagramValue {
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

    return buildString {
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
            // Precalculate all values which cover a tab so they can be excluded as needed.
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

public fun CallDiagram.Displayable.render(): String {
    if (this is CallDiagram.EqualityExpression && value == false) {
        return "Expected <$lhs>, actual <$rhs>."
    }

    return value.toString()
}

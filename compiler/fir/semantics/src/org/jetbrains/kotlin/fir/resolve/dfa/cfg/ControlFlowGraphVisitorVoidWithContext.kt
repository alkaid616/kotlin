/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

abstract class ControlFlowGraphVisitorVoidWithContext(private val root: ControlFlowGraph) : ControlFlowGraphVisitorVoid() {
    private val subGraphs = mutableListOf<ControlFlowGraph>()

    override fun visitSubGraph(node: CFGNodeWithSubgraphs<*>, graph: ControlFlowGraph): ControlFlowGraphVisitorVoid? {
        subGraphs.add(graph)
        return this
    }

    override fun visitSubGraphEnd() {
        subGraphs.removeLast()
    }

    protected fun currentNonInPlaceGraph(): ControlFlowGraph =
        subGraphs.lastOrNull { !it.isInPlace } ?: root
}

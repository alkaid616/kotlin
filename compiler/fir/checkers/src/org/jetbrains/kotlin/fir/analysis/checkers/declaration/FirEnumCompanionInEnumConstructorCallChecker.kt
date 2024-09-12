/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.isEnumEntry
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

object FirEnumCompanionInEnumConstructorCallChecker : FirClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val enumClass = when (declaration.classKind) {
            ClassKind.ENUM_CLASS -> declaration as FirRegularClass
            ClassKind.ENUM_ENTRY -> context.containingDeclarations.lastIsInstanceOrNull()
            else -> null
        } ?: return
        val companionOfEnum = enumClass.companionObjectSymbol ?: return
        val graph = declaration.controlFlowGraphReference?.controlFlowGraph ?: return
        analyzeGraph(graph, companionOfEnum, context, reporter)
        if (declaration.classKind.isEnumEntry) {
            val constructor = declaration.declarations.firstIsInstanceOrNull<FirPrimaryConstructor>()
            val constructorGraph = constructor?.controlFlowGraphReference?.controlFlowGraph
            if (constructorGraph != null) {
                analyzeGraph(constructorGraph, companionOfEnum, context, reporter)
            }
        }
    }

    private fun analyzeGraph(
        graph: ControlFlowGraph,
        companionSymbol: FirRegularClassSymbol,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) = graph.traverse(object : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {}
        override fun visitSubGraph(node: CFGNodeWithSubgraphs<*>, graph: ControlFlowGraph) = this.takeIf { graph.isInPlace }
        override fun visitQualifiedAccessNode(node: QualifiedAccessNode) = visitQualifiedAccess(node.fir)
        override fun visitFunctionCallExitNode(node: FunctionCallExitNode) = visitQualifiedAccess(node.fir)

        private fun visitQualifiedAccess(qualifiedAccess: FirQualifiedAccessExpression) {
            val matchingReceiver = qualifiedAccess.allReceiverExpressions
                .firstOrNull { it.unwrapSmartcastExpression().getClassSymbol(context.session) == companionSymbol } ?: return
            val source = matchingReceiver.source ?: qualifiedAccess.source
            reporter.reportOn(source, FirErrors.UNINITIALIZED_ENUM_COMPANION, companionSymbol, context)
        }
    })

    private fun FirExpression.getClassSymbol(session: FirSession): FirRegularClassSymbol? {
        return when (this) {
            is FirResolvedQualifier -> {
                this.resolvedType.toRegularClassSymbol(session)
            }
            else -> (this.toReference(session) as? FirThisReference)?.boundSymbol
        } as? FirRegularClassSymbol
    }
}

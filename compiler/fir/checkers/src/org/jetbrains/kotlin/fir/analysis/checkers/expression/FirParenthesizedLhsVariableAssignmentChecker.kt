/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.psi.psiUtil.hasOutermostParenthesizedAsAssignmentLhs

object FirParenthesizedLhsVariableAssignmentChecker : FirVariableAssignmentChecker(MppCheckerKind.Platform) {
    override fun check(expression: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        val isLhsParenthesized = expression.lValue.source.hasOutermostParenthesizedAsAssignmentLhs()

        // For:
        // - `(x) = ""` where `x: String`
        // - `(x) += ""` where `x: String`
        // - `(getInt()) += 343`
        if (isLhsParenthesized && !expression.source.isDesugaredIncrementOrDecrement) {
            reporter.reportOn(expression.lValue.source, FirErrors.PARENTHESIZED_LHS, context)
        }
    }

    private val KtSourceElement?.isDesugaredIncrementOrDecrement: Boolean
        get() = this?.kind in DESUGARED_INCREMENTS_AND_DECREMENTS

    private val DESUGARED_INCREMENTS_AND_DECREMENTS = setOf(
        KtFakeSourceElementKind.DesugaredPrefixInc,
        KtFakeSourceElementKind.DesugaredPrefixDec,
        KtFakeSourceElementKind.DesugaredPostfixInc,
        KtFakeSourceElementKind.DesugaredPostfixDec,
    )
}

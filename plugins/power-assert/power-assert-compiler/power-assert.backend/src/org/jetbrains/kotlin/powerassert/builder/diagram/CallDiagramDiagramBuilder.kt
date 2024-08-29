/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.builder.diagram

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.powerassert.diagram.CallDiagramFactory
import org.jetbrains.kotlin.powerassert.diagram.IrTemporaryVariable
import org.jetbrains.kotlin.powerassert.diagram.SourceFile
import org.jetbrains.kotlin.powerassert.diagram.irCallDiagram

class CallDiagramDiagramBuilder(
    private val factory: CallDiagramFactory,
    private val sourceFile: SourceFile,
    private val originalCall: IrCall,
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
        )
    }
}

/*
 * Copyright 2023-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Copyright (C) 2020-2023 Brian Norman
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.parentClassId
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.powerassert.builder.call.CallBuilder
import org.jetbrains.kotlin.powerassert.builder.call.LambdaCallBuilder
import org.jetbrains.kotlin.powerassert.builder.call.SamConversionLambdaCallBuilder
import org.jetbrains.kotlin.powerassert.builder.call.SimpleCallBuilder
import org.jetbrains.kotlin.powerassert.builder.diagram.CallDiagramDiagramBuilder
import org.jetbrains.kotlin.powerassert.builder.diagram.DiagramBuilder
import org.jetbrains.kotlin.powerassert.builder.diagram.StringDiagramBuilder
import org.jetbrains.kotlin.powerassert.diagram.*

class PowerAssertCallTransformer(
    private val sourceFile: SourceFile,
    private val context: IrPluginContext,
    private val messageCollector: MessageCollector,
    private val functions: Set<FqName>,
    private val transformed: Map<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>,
) : IrElementTransformerVoidWithContext() {
    private val irTypeSystemContext = IrTypeSystemContextImpl(context.irBuiltIns)

    override fun visitCall(expression: IrCall): IrExpression {
        val function = expression.symbol.owner
        val fqName = function.kotlinFqName
        return when {
            function.valueParameters.isEmpty() -> super.visitCall(expression)
            function.hasAnnotation(PowerAssertAnnotation) -> buildForAnnotated(expression, function)
            fqName in functions -> buildForOverride(expression, function)
            else -> super.visitCall(expression)
        }
    }

    private fun buildForAnnotated(
        originalCall: IrCall,
        function: IrSimpleFunction,
    ): IrExpression {
        val synthetic = transformed[function.symbol]
            ?: context.referenceFunctions(function.callableId).singleOrNull { it.isSyntheticFor(function) }
        if (synthetic == null) {
            messageCollector.warn(
                expression = originalCall,
                message = "Called function '$function' was not compiled with the power-assert compiler-plugin.",
            )
            return super.visitCall(originalCall)
        }

        val callBuilder = SimpleCallBuilder(synthetic, originalCall)
        val diagramBuilder = CallDiagramDiagramBuilder(CallDiagramFactory(context), sourceFile, originalCall)
        return buildPowerAssertCall(originalCall, function, callBuilder, diagramBuilder)
    }

    private fun buildForOverride(originalCall: IrCall, function: IrSimpleFunction): IrExpression {
        // Find a valid delegate function or do not translate
        // TODO better way to determine which delegate to actually use
        val callBuilders = findCallBuilders(function, originalCall)
        val callBuilder = callBuilders.maxByOrNull { it.function.valueParameters.size }
        if (callBuilder == null) {
            val fqName = function.kotlinFqName
            val valueTypesTruncated = function.valueParameters.subList(0, function.valueParameters.size - 1)
                .joinToString("") { it.type.render() + ", " }
            val valueTypesAll = function.valueParameters.joinToString("") { it.type.render() + ", " }
            messageCollector.warn(
                expression = originalCall,
                message = """
              |Unable to find overload of function $fqName for power-assert transformation callable as:
              | - $fqName(${valueTypesTruncated}String)
              | - $fqName($valueTypesTruncated() -> String)
              | - $fqName(${valueTypesAll}String)
              | - $fqName($valueTypesAll() -> String)
            """.trimMargin(),
            )
            return super.visitCall(originalCall)
        }

        val messageArgument = when (callBuilder.function.valueParameters.size) {
            function.valueParameters.size -> originalCall.getValueArgument(originalCall.valueArgumentsCount - 1)
            else -> null
        }
        val diagramBuilder = StringDiagramBuilder(CallDiagramFactory(context), sourceFile, originalCall, function, messageArgument)

        return buildPowerAssertCall(originalCall, function, callBuilder, diagramBuilder)
    }

    private fun buildPowerAssertCall(
        originalCall: IrCall,
        function: IrSimpleFunction,
        callBuilder: CallBuilder,
        diagramBuilder: DiagramBuilder,
    ): IrExpression {
        val dispatchRoot = when {
            originalCall.symbol.owner.isInfix -> originalCall.dispatchReceiver?.let { argument ->
                function.dispatchReceiverParameter?.let { parameter ->
                    buildTree(parameter, argument)
                }
            }
            else -> null
        }
        val extensionRoot = when {
            originalCall.symbol.owner.isInfix -> originalCall.extensionReceiver?.let { argument ->
                function.extensionReceiverParameter?.let { parameter ->
                    buildTree(parameter, argument)
                }
            }
            else -> null
        }
        val argumentRoots = when (callBuilder.function.valueParameters.size) {
            function.valueParameters.size -> {
                (0 until originalCall.valueArgumentsCount - 1)
                    .map { buildTree(function.valueParameters[it], originalCall.getValueArgument(it)) }
            }
            else -> {
                (0 until originalCall.valueArgumentsCount)
                    .map { buildTree(function.valueParameters[it], originalCall.getValueArgument(it)) }
            }
        }

        // If all roots are null, there are no transformable parameters
        if (dispatchRoot?.child == null && extensionRoot?.child == null && argumentRoots.all { it.child == null }) {
            messageCollector.info(originalCall, "Expression is constant and will not be power-assert transformed")
            return super.visitCall(originalCall)
        }

        val symbol = currentScope!!.scope.scopeOwnerSymbol
        val builder = DeclarationIrBuilder(context, symbol, originalCall.startOffset, originalCall.endOffset)
        return builder.diagram(
            originalCall = originalCall,
            callBuilder = callBuilder,
            diagramBuilder = diagramBuilder,
            dispatchRoot = dispatchRoot,
            extensionRoot = extensionRoot,
            argumentRoots = argumentRoots,
        )
    }

    private fun DeclarationIrBuilder.diagram(
        originalCall: IrCall,
        callBuilder: CallBuilder,
        diagramBuilder: DiagramBuilder,
        dispatchRoot: RootNode? = null,
        extensionRoot: RootNode? = null,
        argumentRoots: List<RootNode>,
    ): IrExpression {
        return buildReceiverDiagram(sourceFile, dispatchRoot?.child) { dispatch, dispatchVariables ->
            buildReceiverDiagram(sourceFile, extensionRoot?.child) { extension, extensionVariables ->

                fun recursive(
                    index: Int,
                    arguments: List<IrExpression?>,
                    argumentVariables: Map<String, List<IrTemporaryVariable>>,
                ): IrExpression {
                    if (index >= argumentRoots.size) {
                        val diagram = diagramBuilder.build(this, dispatchVariables, extensionVariables, argumentVariables)
                        return callBuilder.buildCall(this, dispatch, extension, arguments, diagram)
                    } else {
                        val root = argumentRoots[index]
                        val child = root.child
                        val name = root.parameter.name.toString()
                        if (child == null) {
                            val newArguments = arguments + originalCall.getValueArgument(index)
                            val newArgumentVariables = argumentVariables + (name to emptyList())
                            return recursive(index + 1, newArguments, newArgumentVariables)
                        } else {
                            return buildDiagramNesting(sourceFile, child) { argument, newVariables ->
                                val newArguments = arguments + argument
                                val newArgumentVariables = argumentVariables + (name to newVariables)
                                recursive(index + 1, newArguments, newArgumentVariables)
                            }
                        }
                    }
                }

                recursive(0, emptyList(), emptyMap())
            }
        }
    }

    private fun findCallBuilders(function: IrFunction, original: IrCall): List<CallBuilder> {
        val values = function.valueParameters
        if (values.isEmpty()) return emptyList()

        // Java static functions require searching by class
        val parentClassFunctions = (
                function.parentClassId
                    ?.let { context.referenceClass(it) }
                    ?.functions ?: emptySequence()
                )
            .filter { it.owner.kotlinFqName == function.kotlinFqName }
            .toList()
        val possible = (context.referenceFunctions(function.callableId) + parentClassFunctions)
            .distinct()

        return possible.mapNotNull { overload ->
            // Dispatch receivers must always match exactly
            if (function.dispatchReceiverParameter?.type != overload.owner.dispatchReceiverParameter?.type) {
                return@mapNotNull null
            }

            // Extension receiver may only be assignable
            if (!function.extensionReceiverParameter?.type.isAssignableTo(overload.owner.extensionReceiverParameter?.type)) {
                return@mapNotNull null
            }

            val parameters = overload.owner.valueParameters
            if (parameters.size !in values.size..values.size + 1) return@mapNotNull null
            if (!parameters.zip(values).all { (param, value) -> value.type.isAssignableTo(param.type) }) {
                return@mapNotNull null
            }

            val messageType = parameters.last().type
            return@mapNotNull when {
                isStringSupertype(messageType) -> SimpleCallBuilder(overload, original)
                isStringFunction(messageType) -> LambdaCallBuilder(overload, original, messageType)
                isStringJavaSupplierFunction(messageType) -> SamConversionLambdaCallBuilder(overload, original, messageType)
                else -> null
            }
        }
    }

    private fun IrSimpleFunctionSymbol.isSyntheticFor(function: IrSimpleFunction): Boolean {
        val owner = owner
        if (function.dispatchReceiverParameter?.type != owner.dispatchReceiverParameter?.type) return false
        if (function.extensionReceiverParameter?.type != owner.extensionReceiverParameter?.type) return false

        if (function.valueParameters.size != owner.valueParameters.size - 1) return false
        for (index in function.valueParameters.indices) {
            if (function.valueParameters[index].type != owner.valueParameters[index].type) return false
        }

        return owner.valueParameters.last().type.classFqName == CallDiagramFactory.classIdCallDiagram.asSingleFqName()
    }

    private fun isStringFunction(type: IrType): Boolean =
        type.isFunctionOrKFunction() && type is IrSimpleType && (type.arguments.size == 1 && isStringSupertype(type.arguments.first()))

    private fun isStringJavaSupplierFunction(type: IrType): Boolean {
        val javaSupplier = context.referenceClass(ClassId.topLevel(FqName("java.util.function.Supplier")))
        return javaSupplier != null && type.isSubtypeOfClass(javaSupplier) &&
                type is IrSimpleType && (type.arguments.size == 1 && isStringSupertype(type.arguments.first()))
    }

    private fun isStringSupertype(argument: IrTypeArgument): Boolean =
        argument is IrTypeProjection && isStringSupertype(argument.type)

    private fun isStringSupertype(type: IrType): Boolean =
        context.irBuiltIns.stringType.isSubtypeOf(type, irTypeSystemContext)

    private fun IrType?.isAssignableTo(type: IrType?): Boolean {
        if (this != null && type != null) {
            if (isSubtypeOf(type, irTypeSystemContext)) return true
            val superTypes = (type.classifierOrNull as? IrTypeParameterSymbol)?.owner?.superTypes
            return superTypes != null && superTypes.all { isSubtypeOf(it, irTypeSystemContext) }
        } else {
            return this == null && type == null
        }
    }

    private fun MessageCollector.info(expression: IrElement, message: String) {
        report(expression, CompilerMessageSeverity.INFO, message)
    }

    private fun MessageCollector.warn(expression: IrElement, message: String) {
        report(expression, CompilerMessageSeverity.WARNING, message)
    }

    private fun MessageCollector.report(expression: IrElement, severity: CompilerMessageSeverity, message: String) {
        report(severity, message, sourceFile.getCompilerMessageLocation(expression))
    }
}

val IrFunction.callableId: CallableId
    get() {
        val parentClass = parent as? IrClass
        val classId = parentClass?.classId
        return if (classId != null && !parentClass.isFileClass) {
            CallableId(classId, name)
        } else {
            CallableId(parent.kotlinFqName, name)
        }
    }

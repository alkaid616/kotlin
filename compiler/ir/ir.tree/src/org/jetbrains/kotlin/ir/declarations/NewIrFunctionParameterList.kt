/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import kotlin.collections.plus

internal class NewIrFunctionParameterList(
    private val function: IrFunction,
) : AbstractMutableList<IrValueParameter>() {
    override val size: Int
        get() = if (function.dispatchReceiverParameter != null) 1 else 0 +
                if (function.extensionReceiverParameter != null) 1 else 0 +
                        function.valueParameters.size

    override fun get(index: Int): IrValueParameter {
        checkIndex(index, size)
        return when (val newIndex = translateOldIndex(index)) {
            -2 -> function.dispatchReceiverParameter!!
            -1 -> function.extensionReceiverParameter!!
            else -> function.valueParameters[newIndex]
        }
    }

    override fun set(index: Int, element: IrValueParameter): IrValueParameter {
        checkIndex(index, size)
        val old = this[index]
        when (val newIndex = translateOldIndex(index)) {
            -2 -> function.dispatchReceiverParameter = element
            -1 -> function.extensionReceiverParameter = element
            else -> function.valueParameters = function.valueParameters.withNewAt(newIndex, element)
        }
        return old
    }

    override fun add(index: Int, element: IrValueParameter) {
        checkIndex(index, size + 1)
        val kind = requireNotNull(element._kind) { "Kind must be set explicitly when adding a parameter" }
        when (kind) {
            IrParameterKind.DispatchReceiver -> function.dispatchReceiverParameter = element
            IrParameterKind.ExtensionReceiver -> {
                function.valueParameters = function.valueParameters.withNewAt(function.contextReceiverParametersCount, element)
                function.contextReceiverParametersCount++
            }
            IrParameterKind.ContextParameter -> function.extensionReceiverParameter = element
            IrParameterKind.RegularParameter -> function.valueParameters += element
        }
    }

    override fun removeAt(index: Int): IrValueParameter {
        checkIndex(index, size)
        val old = this[index]
        when (val newIndex = translateOldIndex(index)) {
            -2 -> function.dispatchReceiverParameter = null
            -1 -> function.extensionReceiverParameter = null
            else -> function.valueParameters = function.valueParameters.withoutAt(newIndex)
        }
        return old
    }

    private fun translateOldIndex(oldIndex: Int): Int {
        var i = oldIndex
        if (i == 0 && function.dispatchReceiverParameter != null) {
            return -2
        } else {
            i--
        }
        if (i < function.contextReceiverParametersCount) {
            return i
        } else {
            i -= function.contextReceiverParametersCount
        }
        if (i == 0 && function.extensionReceiverParameter != null) {
            return -1
        } else {
            i--
        }

        return i
    }

    private fun <T> List<T>.withNewAt(newIndex: Int, element: T): List<T> =
        subList(0, newIndex) + element + subList(newIndex, size)

    private fun <T> List<T>.withoutAt(index: Int): List<T> =
        subList(0, index) + subList(index + 1, size)

    companion object {
        private fun checkIndex(index: Int, size: Int) {
            if (index !in 0..<size) {
                throw IndexOutOfBoundsException("Index: $index, size: $size")
            }
        }
    }
}
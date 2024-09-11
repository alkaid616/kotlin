/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.mangler

import org.jetbrains.kotlin.sir.SirClass
import org.jetbrains.kotlin.sir.SirDeclarationParent
import org.jetbrains.kotlin.sir.SirEnum
import org.jetbrains.kotlin.sir.SirErrorType
import org.jetbrains.kotlin.sir.SirExistentialType
import org.jetbrains.kotlin.sir.SirExtension
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirNamed
import org.jetbrains.kotlin.sir.SirNamedDeclaration
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirStruct
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirTypealias
import org.jetbrains.kotlin.sir.SirUnsupportedType
import org.jetbrains.kotlin.sir.SirVariable
import org.jetbrains.kotlin.sir.util.SirSwiftModule

// See https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst for details

/**
 * Identifier in Swift.
 *
 * `name` must conform to `[_a-zA-Z][_$a-zA-Z0-9]*`.
 */
// TODO(KT-71023): support unicode in identifiers
@JvmInline
private value class Identifier(val name: String)

private val SirNamed.identifier: Identifier
    get() = Identifier(name)

/**
 * `name-length name`
 *
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#identifiers).
 *
 * This never produces substitutions.
 */
private val Identifier.mangledName: String
    get() = "${name.length}$name"

/**
 * `identifier`
 *
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#declaration-contexts)
 */
public val SirModule.mangledName: String
    get() = when (this) {
        is SirSwiftModule -> "s"
        else -> identifier.mangledName
    }

/**
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#types) for `context`
 */
public val SirDeclarationParent.mangledName: String
    get() = when (this) {
        is SirModule -> mangledName
        is SirClass -> mangledName
        is SirStruct -> mangledName
        is SirEnum -> mangledName
        is SirExtension -> mangledName
        is SirVariable -> TODO()
    }

/**
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#entities) for `entity`
 */
public val SirNamedDeclaration.mangledName: String
    get() = when (this) {
        is SirClass -> mangledName
        is SirEnum -> mangledName
        is SirStruct -> mangledName
        is SirTypealias -> TODO()
    }

/**
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#types)
 */
public val SirType.mangledName: String
    get() = when (this) {
        is SirNominalType -> typeDeclaration.mangledName
        is SirExistentialType -> TODO()
        is SirErrorType -> error("SirErrorType is not represented in Swift")
        is SirUnsupportedType -> error("SirUnsupportedType is not represented in Swift")
    }

/**
 * `entity module 'E'`
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#types) for `extension mangling`
 */
public val SirExtension.mangledName: String
    get() {
        require(parent is SirModule)
        return "${extendedType.mangledName}${parent.mangledName}E"
    }

/**
 * `context decl-name 'C'`
 *
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#types) for `nominal class type`
 *
 * Assumes that `decl-name` is always just an `identifier`.
 */
public val SirClass.mangledName: String
    get() = "${parent.mangledName}${identifier.mangledName}C"

/**
 * `context decl-name 'V'`
 *
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#types) for `nominal struct type`
 *
 * Assumes that `decl-name` is always just an `identifier`.
 */
public val SirStruct.mangledName: String
    get() = "${parent.mangledName}${identifier.mangledName}V"

/**
 * `context decl-name 'O'`
 *
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#types) for `nominal enum type`
 *
 * Assumes that `decl-name` is always just an `identifier`.
 */
public val SirEnum.mangledName: String
    get() = "${parent.mangledName}${identifier.mangledName}O"
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.classic.ClassicBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrBackendInputsFromK1AndK2
import org.jetbrains.kotlin.test.frontend.K1AndK2OutputArtifact
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact

object FrontendKinds {
    object ClassicFrontend : FrontendKind<ClassicFrontendOutputArtifact>("ClassicFrontend")
    object FIR : FrontendKind<FirOutputArtifact>("FIR")
    object ClassicAndFIR : FrontendKind<K1AndK2OutputArtifact>("ClassicAndFIR")

    fun fromString(string: String): FrontendKind<*>? {
        return when (string) {
            "ClassicFrontend" -> ClassicFrontend
            "FIR" -> FIR
            "ClassicAndFIR" -> ClassicAndFIR
            else -> null
        }
    }
}

val FrontendKind<*>.isFir: Boolean
    get() = this == FrontendKinds.FIR

object BackendKinds {
    object ClassicBackend : BackendKind<ClassicBackendInput>("ClassicBackend")

    /**
     * The artifact kind representing IR generated by the frontend.
     */
    object IrBackend : BackendKind<IrBackendInput>("IrBackend") {
        override val afterDeserializer: BackendKind<IrBackendInput>
            get() = DeserializedIrBackend
    }

    /**
     * The artifact kind representing two IRs, generated by K1 and K2 frontends respectively
     */
    object IrBackendForK1AndK2 : BackendKind<IrBackendInputsFromK1AndK2>("TwoIrBackends")

    /**
     * The artifact kind representing IR deserialized from a klib.
     */
    object DeserializedIrBackend : BackendKind<IrBackendInput>("DeserializedIrBackend") {
        override val afterFrontend: BackendKind<IrBackendInput>
            get() = IrBackend
    }

    fun fromString(string: String): BackendKind<*>? {
        return when (string) {
            "ClassicBackend" -> ClassicBackend
            "IrBackend" -> IrBackend
            "DeserializedIrBackend" -> DeserializedIrBackend
            else -> null
        }
    }

    fun fromTargetBackend(targetBackend: TargetBackend?): BackendKind<*> =
        when {
            targetBackend == null -> BackendKind.NoBackend
            targetBackend == TargetBackend.JS_IR -> DeserializedIrBackend
            targetBackend.isIR -> IrBackend
            else -> ClassicBackend
        }
}

object ArtifactKinds {
    object Jvm : BinaryKind<BinaryArtifacts.Jvm>("JVM")
    object JvmFromK1AndK2 : BinaryKind<BinaryArtifacts.JvmFromK1AndK2>("JvmFromK1AndK2")
    object Js : BinaryKind<BinaryArtifacts.Js>("JS")
    object Native : BinaryKind<BinaryArtifacts.Native>("Native")
    object Wasm : BinaryKind<BinaryArtifacts.Wasm>("Wasm")
    object KLib : BinaryKind<BinaryArtifacts.KLib>("KLib")

    fun fromString(string: String): BinaryKind<*>? {
        return when (string) {
            "Jvm" -> Jvm
            "Js" -> Js
            "Native" -> Native
            "Wasm" -> Wasm
            "KLib" -> KLib
            "JvmFromK1AndK2" -> JvmFromK1AndK2
            else -> null
        }
    }
}

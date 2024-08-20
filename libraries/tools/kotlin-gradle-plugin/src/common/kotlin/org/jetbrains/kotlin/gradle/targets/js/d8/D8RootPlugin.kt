/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.d8

// To be compatible with previous KGP version, we need to keep D8RootPlugin as deprecated.
// uncomment after bootstrap
//@Deprecated("This type is deprecated. Use D8Plugin instead.", ReplaceWith("D8Plugin"))
typealias D8RootPlugin = D8Plugin
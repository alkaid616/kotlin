/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.concurrent

import java.util.concurrent.atomic.*

@Suppress("UNCHECKED_CAST")
public fun AtomicInt.asJavaAtomic(): AtomicInteger = this as AtomicInteger

@Suppress("UNCHECKED_CAST")
public fun AtomicInteger.asKotlinAtomic(): AtomicInt = this as AtomicInt

@Suppress("UNCHECKED_CAST")
public fun AtomicLong.asJavaAtomic(): java.util.concurrent.atomic.AtomicLong = this as java.util.concurrent.atomic.AtomicLong

@Suppress("UNCHECKED_CAST")
public fun java.util.concurrent.atomic.AtomicLong.asKotlinAtomic(): AtomicLong = this as AtomicLong

@Suppress("UNCHECKED_CAST")
public fun <T> AtomicBoolean.asJavaAtomic(): java.util.concurrent.atomic.AtomicBoolean = this as java.util.concurrent.atomic.AtomicBoolean

@Suppress("UNCHECKED_CAST")
public fun <T> java.util.concurrent.atomic.AtomicBoolean.asKotlinAtomic(): AtomicBoolean = this as AtomicBoolean

@Suppress("UNCHECKED_CAST")
public fun <T> AtomicReference<T>.asJavaAtomic(): java.util.concurrent.atomic.AtomicReference<T> = this as java.util.concurrent.atomic.AtomicReference<T>

@Suppress("UNCHECKED_CAST")
public fun <T> java.util.concurrent.atomic.AtomicReference<T>.asKotlinAtomic(): AtomicReference<T> = this as AtomicReference<T>
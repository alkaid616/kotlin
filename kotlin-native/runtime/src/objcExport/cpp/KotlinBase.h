/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <inttypes.h>
#import <Foundation/Foundation.h>

struct ObjHeader;

@interface KotlinBase : NSObject <NSCopying>

+ (instancetype)createRetainedWrapper:(struct ObjHeader *)obj;

// Initialize this class with kotlin.native.ref.ExternalRCRef
// Does not retain `ref` itself.
// `mode` is one of the following:
// * 0: `ref` must not already point to another `KotlinBase` instance, `self` type is
//   considered precise. `self` will remain the same after initialization completes.
//   This mode can be used from Swift subclass `init()`.
// * 1: `ref` may already point to another `KotlinBase` instance, and `self` type may
//   be imprecise. In this mode `self` may be replaced during initialization with
//   a more appropriate instance, older `self` will be automatically destroyed.
//   This mode can be used in Swift return value transformation.
// * 2: `ref` may already point to another `KotlinBase` instance, but `self` is precise,
//   This mode is used internally.
- (instancetype)initWithExternalRCRef:(uintptr_t)ref
                                 mode:(int)mode NS_REFINED_FOR_SWIFT;

// Return kotlin.native.ref.ExternalRCRef stored in this class
- (uintptr_t)externalRCRef NS_REFINED_FOR_SWIFT;

@end

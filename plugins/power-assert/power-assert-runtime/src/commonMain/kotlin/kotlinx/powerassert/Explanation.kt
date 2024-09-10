/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

// TODO include startOffset? row/column? how does that impact offsets in expressions?
public abstract class Explanation internal constructor() {
    public abstract val source: String
    public abstract val expressions: List<Expression>
}

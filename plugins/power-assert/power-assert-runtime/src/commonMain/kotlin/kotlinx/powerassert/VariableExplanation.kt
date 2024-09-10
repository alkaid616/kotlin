/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

public class VariableExplanation(
    public override val source: String,
    public val initializer: Initializer,
) : Explanation() {
    override val expressions: List<Expression>
        get() = initializer.expressions

    public class Initializer(
        public val expressions: List<Expression>,
    )
}

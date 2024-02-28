/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.generators.tree.*

sealed class Field : AbstractField<Field>() {
    open var withReplace: Boolean = false

    open var needsSeparateTransform: Boolean = false
    var parentHasSeparateTransform: Boolean = true
    open var needTransformInOtherChildren: Boolean = false

    open val isMutableOrEmptyList: Boolean
        get() = false

    open var isMutableInInterface: Boolean = false
    open val fromDelegate: Boolean get() = false

    open var useNullableForReplace: Boolean = false

    var withBindThis = true

    override var origin: Field = this

    override var withGetter: Boolean = false
    override var defaultValueInImplementation: String? = null
    override var defaultValueInBuilder: String? = null

    override var customSetter: String? = null

    abstract override var isVolatile: Boolean

    abstract override var isFinal: Boolean

    abstract override var isParameter: Boolean

    abstract override var isMutable: Boolean

    override fun replaceType(newType: TypeRefWithNullability): Field = copy()

    override fun copy(): Field = internalCopy().also {
        updateFieldsInCopy(it)
    }

    override fun updateFieldsInCopy(copy: Field) {
        super.updateFieldsInCopy(copy)
        copy.origin = origin
        copy.needsSeparateTransform = needsSeparateTransform
        copy.needTransformInOtherChildren = needTransformInOtherChildren
        copy.useNullableForReplace = useNullableForReplace
        copy.customInitializationCall = customInitializationCall
        copy.parentHasSeparateTransform = parentHasSeparateTransform
    }

    protected abstract fun internalCopy(): Field

    override fun updatePropertiesFromOverriddenField(parentField: Field, haveSameClass: Boolean) {
        needsSeparateTransform = needsSeparateTransform || parentField.needsSeparateTransform
        needTransformInOtherChildren = needTransformInOtherChildren || parentField.needTransformInOtherChildren
        withReplace = withReplace || parentField.withReplace
        parentHasSeparateTransform = parentField.needsSeparateTransform
        if (parentField.nullable != nullable && haveSameClass) {
            useNullableForReplace = true
        }
    }
}

class SimpleField(
    override val name: String,
    override val typeRef: TypeRefWithNullability,
    override val isChild: Boolean,
    override var isMutable: Boolean,
    override var withReplace: Boolean,
    override var isVolatile: Boolean = false,
    override var isFinal: Boolean = false,
    override var isLateinit: Boolean = false,
    override var isParameter: Boolean = false,
) : Field() {

    override fun internalCopy(): Field {
        return SimpleField(
            name = name,
            typeRef = typeRef,
            isChild = isChild,
            isMutable = isMutable,
            withReplace = withReplace,
            isVolatile = isVolatile,
            isFinal = isFinal,
            isLateinit = isLateinit,
            isParameter = isParameter,
        ).apply {
            withBindThis = this@SimpleField.withBindThis
        }
    }

    override fun replaceType(newType: TypeRefWithNullability) = SimpleField(
        name = name,
        typeRef = newType,
        isChild = isChild,
        isMutable = isMutable,
        withReplace = withReplace,
        isVolatile = isVolatile,
        isFinal = isFinal,
        isLateinit = isLateinit,
        isParameter = isParameter
    ).also {
        it.withBindThis = withBindThis
        updateFieldsInCopy(it)
    }
}
// ----------- Field list -----------

class FieldList(
    override val name: String,
    override val baseType: TypeRef,
    override var withReplace: Boolean,
    override val isChild: Boolean,
    useMutableOrEmpty: Boolean = false,
) : Field(), ListField {
    override var defaultValueInImplementation: String? = null

    override val typeRef: ClassRef<PositionTypeParameterRef>
        get() = super.typeRef

    override val listType: ClassRef<PositionTypeParameterRef>
        get() = StandardTypes.list

    override var isVolatile: Boolean = false
    override var isFinal: Boolean = false
    override var isMutable: Boolean = true
    override val isMutableOrEmptyList: Boolean = useMutableOrEmpty
    override var isLateinit: Boolean = false
    override var isParameter: Boolean = false

    override fun internalCopy(): Field {
        return FieldList(
            name,
            baseType,
            withReplace,
            isChild,
            isMutableOrEmptyList
        )
    }
}

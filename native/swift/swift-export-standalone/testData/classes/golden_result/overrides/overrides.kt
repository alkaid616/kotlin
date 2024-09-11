import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("overrides_BAR_bar")
public fun overrides_BAR_bar(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as overrides.BAR
    __self.bar()
}

@ExportedBridge("overrides_BAR_init_allocate")
public fun overrides_BAR_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<overrides.BAR>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("overrides_BAR_init_initialize__TypesOfArguments__uintptr_t__")
public fun overrides_BAR_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, overrides.BAR())
}

@ExportedBridge("overrides_Baz_baz")
public fun overrides_Baz_baz(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as overrides.Baz
    __self.baz()
}

@ExportedBridge("overrides_Baz_init_allocate")
public fun overrides_Baz_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<overrides.Baz>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("overrides_Baz_init_initialize__TypesOfArguments__uintptr_t__")
public fun overrides_Baz_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, overrides.Baz())
}

@ExportedBridge("overrides_FOO_foo")
public fun overrides_FOO_foo(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as overrides.FOO
    __self.foo()
}

@ExportedBridge("overrides_FOO_init_allocate")
public fun overrides_FOO_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<overrides.FOO>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("overrides_FOO_init_initialize__TypesOfArguments__uintptr_t__")
public fun overrides_FOO_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, overrides.FOO())
}


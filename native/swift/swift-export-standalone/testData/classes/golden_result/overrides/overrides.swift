@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_overrides
import KotlinRuntime

public extension ExportedKotlinPackages.overrides {
    open class BAR : ExportedKotlinPackages.overrides.FOO {
        public override init() {
            let __kt = overrides_BAR_init_allocate()
            super.init(__externalRCRef: __kt)
            overrides_BAR_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public func bar() -> Swift.Void {
            return overrides_BAR_bar(self.__externalRCRef())
        }
    }
    public final class Baz : ExportedKotlinPackages.overrides.BAR {
        public override init() {
            let __kt = overrides_Baz_init_allocate()
            super.init(__externalRCRef: __kt)
            overrides_Baz_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public func baz() -> Swift.Void {
            return overrides_Baz_baz(self.__externalRCRef())
        }
    }
    open class FOO : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = overrides_FOO_init_allocate()
            super.init(__externalRCRef: __kt)
            overrides_FOO_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public func foo() -> Swift.Void {
            return overrides_FOO_foo(self.__externalRCRef())
        }
    }
}

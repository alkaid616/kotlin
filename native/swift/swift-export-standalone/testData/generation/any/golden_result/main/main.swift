import KotlinRuntime
@_implementationOnly import KotlinBridges_main

public final class MyObject : KotlinRuntime.KotlinBase {
    public static var shared: main.MyObject {
        get {
            return main.MyObject(__externalRCRef: __root___MyObject_get(), mode: 1)
        }
    }
    private override init() {
        fatalError()
    }
    public override init(
        __externalRCRef: Swift.UInt,
        mode: Swift.Int32
    ) {
        super.init(__externalRCRef: __externalRCRef, mode: mode)
    }
}
public func getMainObject() -> KotlinRuntime.KotlinBase {
    return KotlinRuntime.KotlinBase(__externalRCRef: __root___getMainObject(), mode: 1)
}
public func isMainObject(
    obj: KotlinRuntime.KotlinBase
) -> Swift.Bool {
    return __root___isMainObject__TypesOfArguments__KotlinRuntime_KotlinBase__(obj.__externalRCRef())
}

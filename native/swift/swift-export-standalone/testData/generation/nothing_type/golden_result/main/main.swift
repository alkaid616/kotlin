@_implementationOnly import KotlinBridges_main
import KotlinRuntime

public typealias Foo = Swift.Never
public typealias OptionalNothing = Swift.Never
public final class Bar : KotlinRuntime.KotlinBase {
    public var p: Swift.Never {
        get {
            return Bar_p_get(self.__externalRCRef())
        }
    }
    public init(
        p: Swift.Never
    ) {
        fatalError()
    }
    public override init(
        __externalRCRef: Swift.UInt,
        mode: Swift.Int32
    ) {
        super.init(__externalRCRef: __externalRCRef, mode: mode)
    }
}
public var value: Swift.Never {
    get {
        return __root___value_get()
    }
}
public var variable: Swift.Never {
    get {
        return __root___variable_get()
    }
    set {
        fatalError()
    }
}
public func meaningOfLife() -> Swift.Never {
    return __root___meaningOfLife()
}
public func meaningOfLife(
    p: Swift.Never
) -> Swift.Never {
    fatalError()
}

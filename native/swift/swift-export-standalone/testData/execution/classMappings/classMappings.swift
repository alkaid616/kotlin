import ClassMappings
import KotlinRuntime
import ObjectiveC

func testType<T: KotlinBase>(_ name: String, _ type: T.Type) throws {
    try assertSame(
        actual: objc_getClass(name) as! T.Type,
        expected: type
    )
}

func testAnyClass() throws {
    try assertFalse(isAnyClassNameNull())
    try testType(getAnyClassName(), KotlinBase.self)
}

func testFinalClass() throws {
    try assertFalse(isFinalClassNameNull())
    try testType(getFinalClassName(), FinalClass.self)
}

func testNestedFinalClass() throws {
    try assertFalse(isNestedFinalClassNameNull())
    try testType(getNestedFinalClassName(), FinalClass.NestedFinalClass.self)
}

func testNamespacedFinalClass() throws {
    try assertFalse(namespace.isNamespacedFinalClassNameNull())
    try testType(namespace.getNamespacedFinalClassName(), namespace.NamespacedFinalClass.self)
}

func testOpenClass() throws {
    try assertFalse(isOpenClassNameNull())
    try testType(getOpenClassName(), OpenClass.self)
}

func testPrivateClass() throws {
    try assertTrue(isPrivateClassNameNull())
}

class ClassMappingsTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testAnyClass", method: withAutorelease(testAnyClass)),
            TestCase(name: "testFinalClass", method: withAutorelease(testFinalClass)),
            TestCase(name: "testNestedFinalClass", method: withAutorelease(testNestedFinalClass)),
            TestCase(name: "testNamespacedFinalClass", method: withAutorelease(testNamespacedFinalClass)),
            TestCase(name: "testOpenClass", method: withAutorelease(testOpenClass)),
            TestCase(name: "testPrivateClass", method: withAutorelease(testPrivateClass)),
        ]
    }
}
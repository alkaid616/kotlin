/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.cinterop.*

import platform.Foundation.*
import platform.UniformTypeIdentifiers.UTTypeSourceCode
import platform.XCTest.*
import platform.objc.*

import kotlin.native.internal.test.GeneratedSuites
import kotlin.native.internal.test.TestCase
import kotlin.native.internal.test.TestSuite

@ExportObjCClass(name = "Kotlin/Native::Test")
class TestCaseRunner(
    invocation: NSInvocation,
    private val testName: String,
    private val testCase: TestCase
) : XCTestCase(invocation) {
    // Sets XCTest to continue running after failure to match Kotlin Test
    override fun continueAfterFailure(): Boolean = true

    private val ignored = testCase.ignored || testCase.suite.ignored

    @ObjCAction
    fun run() {
        if (ignored) {
            // It is not possible to use XCTSkip() due to KT-43719 and not implemented exception propagation
            //  just skip it for now as no one catches the _XCTSkipFailureException
            //  023-01-02 20:06:56.016 xctest[76004:10364894] *** Terminating app due to uncaught exception '_XCTSkipFailureException', reason: 'Test skipped'

            // TODO: Probably it's better to inherit from the proxy class with run method
            //  that will use XCTSkip or this handler if the return value != 0
            //  But this can affect createRunMethod as it will be virtual (need to check that)
//            _XCTSkipHandler(testName, 0U, "Test $testName is ignored")
            return
        }
        try {
            testCase.doRun()
        } catch (throwable: Throwable) {
            val type = when (throwable) {
                is AssertionError -> XCTIssueTypeAssertionFailure
                else -> XCTIssueTypeUncaughtException
            }
            val stackTrace = throwable.getStackTrace()
            val failedStackLine = stackTrace.first {
                // try to filter out kotlin.Exceptions and kotlin.test.Assertion inits
                !it.contains("kfun:kotlin.")
            }
            // Find path and line number to create source location
            val matchResult = Regex("^\\d+ +.* \\((.*):(\\d+):.*\\)$").find(failedStackLine)
            val sourceLocation = if (matchResult != null) {
                val (file, line) = matchResult.destructured
                XCTSourceCodeLocation(file, line.toLong())
            } else {
                XCTSourceCodeLocation()
            }

            @Suppress("CAST_NEVER_SUCCEEDS")
            val stackAsPayload = (stackTrace.joinToString("\n") as? NSString)
                ?.dataUsingEncoding(NSUTF8StringEncoding)
            val stackTraceAttachment = XCTAttachment.attachmentWithUniformTypeIdentifier(
                identifier = UTTypeSourceCode.identifier,
                name = "Kotlin stacktrace (full)",
                payload = stackAsPayload,
                userInfo = null
            )

            val issue = XCTIssue(
                type = type,
                compactDescription = "$throwable in $testName",
                detailedDescription = "Caught exception $throwable in $testName (caused by ${throwable.cause})",
                sourceCodeContext = XCTSourceCodeContext(
                    callStackAddresses = throwable.getStackTraceAddresses(),
                    location = sourceLocation
                ),
                associatedError = null,
                attachments = listOf(stackTraceAttachment)
            )
            testRun?.recordIssue(issue) ?: error("TestRun for the test $testName not found")
        }
    }

    override fun setUp() {
        super.setUp()
        if (!ignored) testCase.doBefore()
    }

    override fun tearDown() {
        if (!ignored) testCase.doAfter()
        super.tearDown()
    }

    override fun description(): String = buildString {
        append(testName)
        if (ignored) append("(ignored)")
    }

    // TODO: file an issue for null in this::class.simpleName
    //  "${this::class.simpleName}::$testName" leads to "null::$testName"
    override fun name() = testName

    companion object : XCTestCaseMeta(){
        /**
         * Used if the test suite is generated as a default one from methods extracted by the XCTest from the
         * runner that extends XCTestCase and is exported to ObjC.
         */
        override fun testCaseWithInvocation(invocation: NSInvocation?): XCTestCase {
            error(
                """
                This should not happen by default.
                Got invocation: ${invocation?.description}
                with selector @sel(${NSStringFromSelector(invocation?.selector)})
                """.trimIndent()
            )
        }

        // region: Dynamic run methods creation

        /**
         * TODO: describe what happens here
         */

        private fun createRunMethod(selector: SEL) {
            // Note: must be disposed off with imp_removeBlock
            val result = class_addMethod(
                cls = this.`class`(),
                name = selector,
                imp = imp_implementationWithBlock(this::runner),
                types = "v@:" // See ObjC' type encodings: v (returns void), @ (id self), : (SEL _cmd)
            )
            check(result) {
                "Internal error: was unable to add method with selector $selector"
            }
        }

        private fun dispose(selector: SEL) {
            val imp = class_getMethodImplementation(
                cls = this.`class`(),
                name = selector
            )
            val result = imp_removeBlock(imp)
            check(result) {
                "Internal error: was unable to remove block for $selector"
            }
        }

        // TODO: need to clean up those methods. When/where should this be invoked?
        private fun disposeRunMethods() {
            createTestMethodsNames().forEach {
                val selector = NSSelectorFromString(it)
                dispose(selector)
            }
        }

        @Suppress("UNUSED_PARAMETER")
        private fun runner(runner: TestCaseRunner, cmd: SEL) {
            runner.run()
        }
        //endregion

        @OptIn(ExperimentalStdlibApi::class)
        private fun createTestMethodsNames(): List<String> =
            GeneratedSuites.suites.flatMap { testSuite ->
                testSuite.testCases.values.map { "$testSuite.${it.name}" }
            }

        /**
         * Create Test invocations for each test method to make them resolvable by the XCTest's machinery
         * @see NSInvocation
         */
        override fun testInvocations(): List<NSInvocation> = createTestMethodsNames().map {
            val selector = NSSelectorFromString(it)
            createRunMethod(selector)
            this.instanceMethodSignatureForSelector(selector)?.let { signature ->
                val invocation = NSInvocation.invocationWithMethodSignature(signature)
                invocation.setSelector(selector)
                invocation
            } ?: error("Not able to create NSInvocation for method $it")
        }
    }
}

internal typealias SEL = COpaquePointer?

class TestSuiteRunner(private val testSuite: TestSuite) : XCTestSuite(testSuite.name) {
    private val ignoredSuite: Boolean
        get() = testSuite.ignored || testSuite.testCases.all { it.value.ignored }

    override fun setUp() {
        super.setUp()
        if (!ignoredSuite) testSuite.doBeforeClass()
    }

    override fun tearDown() {
        if (!ignoredSuite) testSuite.doAfterClass()
        super.tearDown()
    }
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.native.internal.ExportForCppRuntime("Konan_create_testSuite")
fun defaultTestSuiteFactory(): XCTestSuite {
    XCTestObservationCenter.sharedTestObservationCenter.addTestObserver(XCSimpleTestListener())
    val nativeTestSuite = XCTestSuite.testSuiteWithName("Kotlin/Native test suite")

    println(":::: Query bundle principal classes ::::")
    println("* Main bundle is: ${NSBundle.mainBundle}")
    NSBundle.allBundles.forEach {
        println("* Bundle: $it with principal = ${(it as? NSBundle)?.principalClass()}")
        println((it as? NSBundle)?.infoDictionary?.get("TestKeyToBundle") ?: "Dictionary is null")
    }

    println(":::: Create test suites ::::")
    createTestSuites().forEach {
        println("* Suite '${it.name}' with tests: " + it.tests().joinToString(", ", "[", "]"))
        nativeTestSuite.addTest(it)
    }

    println(":::: Tests created (self-check) ::::")
    @Suppress("UNCHECKED_CAST")
    (nativeTestSuite.tests as List<XCTest>).forEach {
        println("* Suite '${it.name}' with ${it.testCaseCount} test cases was created successfully")
    }
    return nativeTestSuite
}

@OptIn(ExperimentalStdlibApi::class)
internal fun createTestSuites(): List<XCTestSuite> {
    val testInvocations = TestCaseRunner.testInvocations()
    return GeneratedSuites.suites.map {
        val suite = TestSuiteRunner(it)
        it.testCases.values.map { testCase ->
            testInvocations.filter { nsInvocation ->
                NSStringFromSelector(nsInvocation.selector) == "${it.name}.${testCase.name}"
            }.map { inv ->
                TestCaseRunner(
                    invocation = inv, testName = "${it.name}.${testCase.name}", testCase = testCase
                )
            }.single()
        }.forEach { t ->
            suite.addTest(t)
        }
        suite
    }
}

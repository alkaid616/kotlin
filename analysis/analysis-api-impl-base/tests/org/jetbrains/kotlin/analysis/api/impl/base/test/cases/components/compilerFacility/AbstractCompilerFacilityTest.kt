/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility

import com.intellij.openapi.extensions.LoadingOrder
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.parentClassId
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.BytecodeListingTextCollectingVisitor
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.plugin.services.PluginRuntimeAnnotationsProvider
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.io.File
import kotlin.reflect.jvm.jvmName

abstract class AbstractFirPluginPrototypeMultiModuleCompilerFacilityTest : AbstractCompilerFacilityTest() {
    override fun extraCustomRuntimeClasspathProviders(): Array<Constructor<RuntimeClasspathProvider>> =
        arrayOf(::PluginRuntimeAnnotationsProvider)
}

abstract class AbstractCompilerFacilityTest : AbstractAnalysisApiBasedTest() {
    private companion object {
        private val ALLOWED_ERRORS = listOf(
            FirErrors.INVISIBLE_REFERENCE,
            FirErrors.INVISIBLE_SETTER,
            FirErrors.DEPRECATION_ERROR,
            FirErrors.DIVISION_BY_ZERO,
            FirErrors.OPT_IN_USAGE_ERROR,
            FirErrors.OPT_IN_OVERRIDE_ERROR,
            FirErrors.UNSAFE_CALL,
            FirErrors.UNSAFE_IMPLICIT_INVOKE_CALL,
            FirErrors.UNSAFE_INFIX_CALL,
            FirErrors.UNSAFE_OPERATOR_CALL,
            FirErrors.ITERATOR_ON_NULLABLE,
            FirErrors.UNEXPECTED_SAFE_CALL,
            FirErrors.DSL_SCOPE_VIOLATION,
        ).map { it.name }
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val testFile = mainModule.testModule.files.single { it.name == mainFile.name }

        val annotationToCheckCalls = mainModule.testModule.directives[Directives.CHECK_CALLS_WITH_ANNOTATION].singleOrNull()
        val irCollector = CollectingIrGenerationExtension(annotationToCheckCalls)

        val compilerConfiguration = CompilerConfiguration().apply {
            put(CommonConfigurationKeys.MODULE_NAME, mainModule.testModule.name)
            put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, mainModule.testModule.languageVersionSettings)
            put(JVMConfigurationKeys.IR, true)

            testFile.directives[Directives.CODE_FRAGMENT_CLASS_NAME].singleOrNull()
                ?.let { put(KaCompilerFacility.CODE_FRAGMENT_CLASS_NAME, it) }

            testFile.directives[Directives.CODE_FRAGMENT_METHOD_NAME].singleOrNull()
                ?.let { put(KaCompilerFacility.CODE_FRAGMENT_METHOD_NAME, it) }
        }

        val target = KaCompilerTarget.Jvm(ClassBuilderFactories.TEST)
        val allowedErrorFilter: (KaDiagnostic) -> Boolean = { it.factoryName in ALLOWED_ERRORS }

        var prebuiltDependencies: KaPrebuiltDependencies? = null
        mainModule.testModule.directives[Directives.PROVIDE_PREBUILT_DEPENDENCIES].singleOrNull()?.let { dependencyClassNames ->
            val classNameToByteArray = compileDependencyFilesContainingInline(
                mainFile, mainModule.ktModule as KaSourceModule, compilerConfiguration, target, allowedErrorFilter
            )
            assert(dependencyClassNames.split(',') == classNameToByteArray.keys.toList())
            prebuiltDependencies = KaPrebuiltDependencies(classNameToByteArray)
        }

        val project = mainFile.project
        project.extensionArea.getExtensionPoint(IrGenerationExtension.extensionPointName)
            .registerExtension(irCollector, LoadingOrder.LAST, project)

        analyze(mainFile) {
            val result = compile(mainFile, compilerConfiguration, target, allowedErrorFilter, prebuiltDependencies)

            val actualText = when (result) {
                is KaCompilationResult.Failure -> result.errors.joinToString("\n") { dumpDiagnostic(it) }
                is KaCompilationResult.Success -> dumpClassFiles(result.output)
            }

            testServices.assertions.assertEqualsToTestDataFileSibling(actualText)

            if (result is KaCompilationResult.Success) {
                testServices.assertions.assertEqualsToTestDataFileSibling(irCollector.result, extension = ".ir.txt")
            }

            if (annotationToCheckCalls != null) {
                testServices.assertions.assertEqualsToTestDataFileSibling(
                    irCollector.functionsWithAnnotationToCheckCalls.joinToString("\n"), extension = ".check_calls.txt"
                )
            }
        }
    }

    open fun extraCustomRuntimeClasspathProviders(): Array<Constructor<RuntimeClasspathProvider>> = emptyArray()

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(Directives)
            useConfigurators(::CompilerFacilityEnvironmentConfigurator)
            defaultDirectives {
                +ConfigurationDirectives.WITH_STDLIB
                +JvmEnvironmentConfigurationDirectives.FULL_JDK
            }
            useCustomRuntimeClasspathProviders(*extraCustomRuntimeClasspathProviders())
        }
    }

    private fun collectFilesContainingDependencyInlineFunc(file: KtFile, mainModule: KaSourceModule): Set<KtFile> {
        val filesContainingInlineFunction = mutableSetOf<KtFile>()
        file.accept(object : KtTreeVisitorVoid() {
            override fun visitExpression(expression: KtExpression) {
                super.visitExpression(expression)
                analyze(expression) {
                    val symbol = expression.resolveToCall()?.singleFunctionCallOrNull()?.symbol ?: return@analyze
                    val containingModule = symbol.containingModule as? KaSourceModule ?: return@analyze
                    if (containingModule != mainModule) {
                        val ktFileContainingSymbol = symbol.containingFile?.psi?.containingFile as? KtFile ?: return@analyze
                        filesContainingInlineFunction.add(ktFileContainingSymbol)
                    }
                }
            }
        })
        return filesContainingInlineFunction
    }

    /**
     * This function collects inline functions declared in source library modules of [mainFile] and
     * compiler them to prepare the class path to [ByteArray] map. Its implementation is almost the
     * same as `CompilationPeerCollectingVisitor`, but we can run this function out of
     * [KaCompilerFacility.compile], so we can test whether the given [KaPrebuiltDependencies] to
     * [KaCompilerFacility.compile] correctly handles the inline function dependencies or not.
     */
    private fun compileDependencyFilesContainingInline(
        mainFile: KtFile, mainModule: KaSourceModule,
        configuration: CompilerConfiguration,
        target: KaCompilerTarget,
        allowedErrorFilter: (KaDiagnostic) -> Boolean,
    ): Map<String, ByteArray> {
        val dependencyFilesContainingInline = collectFilesContainingDependencyInlineFunc(mainFile, mainModule)
        val classNameToByteArray = mutableMapOf<String, ByteArray>()
        dependencyFilesContainingInline.forEach { dependencyFile ->
            val className = dependencyFile.javaFileFacadeFqName.toString().replace(".", "/").let {
                if (it.endsWith("Kt")) it.substringBeforeLast("Kt")
                else it
            }
            val compileResult = analyze(dependencyFile) {
                compile(dependencyFile, configuration, target, allowedErrorFilter)
            }
            val artifact = compileResult as? KaCompilationResult.Success ?: return@forEach
            val compiledFile = artifact.output.firstOrNull { it.path == "$className.class" } ?: return@forEach
            classNameToByteArray[className] = compiledFile.content
        }
        return classNameToByteArray
    }

    private fun dumpDiagnostic(diagnostic: KaDiagnostic): String {
        val textRanges = when (diagnostic) {
            is KaDiagnosticWithPsi<*> -> {
                diagnostic.textRanges.singleOrNull()?.toString()
                    ?: diagnostic.textRanges.joinToString(prefix = "[", postfix = "]")
            }
            else -> null
        }

        return buildString {
            if (textRanges != null) {
                append(textRanges)
                append(" ")
            }
            append(diagnostic.factoryName)
            append(" ")
            append(diagnostic.defaultMessage)
        }
    }

    private fun dumpClassFiles(outputFiles: List<KaCompiledFile>): String {
        val classes = outputFiles
            .filter { it.path.endsWith(".class", ignoreCase = true) }
            .also { check(it.isNotEmpty()) }
            .sortedBy { it.path }
            .map { outputFile ->
                val classReader = ClassReader(outputFile.content)
                ClassNode(Opcodes.API_VERSION).also { classReader.accept(it, ClassReader.SKIP_CODE) }
            }

        val allClasses = classes.associateBy { Type.getObjectType(it.name) }

        return classes.joinToString("\n\n") { node ->
            val visitor = BytecodeListingTextCollectingVisitor(
                BytecodeListingTextCollectingVisitor.Filter.EMPTY,
                allClasses,
                withSignatures = false,
                withAnnotations = false,
                sortDeclarations = true
            )

            node.accept(visitor)
            visitor.text
        }
    }

    object Directives : SimpleDirectivesContainer() {
        val CODE_FRAGMENT_CLASS_NAME by stringDirective(
            "Short name of a code fragment class",
            applicability = DirectiveApplicability.File
        )

        val CODE_FRAGMENT_METHOD_NAME by stringDirective(
            "Name of a code fragment facade method",
            applicability = DirectiveApplicability.File
        )

        val ATTACH_DUPLICATE_STDLIB by directive(
            "Attach the 'stdlib-jvm-minimal-for-test' library to simulate duplicate stdlib dependency"
        )

        val CHECK_CALLS_WITH_ANNOTATION by stringDirective(
            "Check whether all functions of calls and getters of properties with a given annotation are listed in *.check_calls.txt or not"
        )

        val PROVIDE_PREBUILT_DEPENDENCIES by stringDirective(
            "Class names of prebuilt dependencies to be compiled and kept in ${KaPrebuiltDependencies::class.jvmName}"
        )
    }
}

private class CompilerFacilityEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (module.directives.contains(AbstractCompilerFacilityTest.Directives.ATTACH_DUPLICATE_STDLIB)) {
            configuration.add(CLIConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(ForTestCompileRuntime.minimalRuntimeJarForTests()))
        }
    }
}

internal fun createCodeFragment(ktFile: KtFile, module: TestModule, testServices: TestServices): KtCodeFragment? {
    val ioFile = module.files.single { it.name == ktFile.name }.originalFile
    val ioFragmentFile = File(ioFile.parent, "${ioFile.nameWithoutExtension}.fragment.${ioFile.extension}")

    if (!ioFragmentFile.exists()) {
        return null
    }

    val contextElement = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtElement>(ktFile)

    val fragmentText = ioFragmentFile.readText()
    val isBlockFragment = fragmentText.any { it == '\n' }

    val project = ktFile.project
    val factory = KtPsiFactory(project, markGenerated = false)

    return when {
        isBlockFragment -> factory.createBlockCodeFragment(fragmentText, contextElement)
        else -> factory.createExpressionCodeFragment(fragmentText, contextElement)
    }
}

private class CollectingIrGenerationExtension(private val annotationToCheckCalls: String?) : IrGenerationExtension {
    var result: String = ""

    val functionsWithAnnotationToCheckCalls: MutableSet<String> = mutableSetOf()

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val dumpOptions = DumpIrTreeOptions(
            normalizeNames = true,
            stableOrder = true,
            printModuleName = false,
            printFilePath = false
        )

        result = moduleFragment.dump(dumpOptions)

        annotationToCheckCalls?.let { annotationFqName ->
            moduleFragment.accept(
                CheckCallsWithAnnotationVisitor(annotationFqName) { functionsWithAnnotationToCheckCalls.add(it.name.asString()) }, null
            )
        }
    }

    /**
     * This class recursively visits all calls of functions and getters, and if the function or the getter used for a call has
     * an annotation whose FqName is [annotationFqName], it runs [handleFunctionWithAnnotation] for the function or the getter.
     */
    private class CheckCallsWithAnnotationVisitor(
        private val annotationFqName: String,
        private val handleFunctionWithAnnotation: (declaration: IrDeclarationWithName) -> Unit,
    ) : IrElementVisitorVoid {
        val annotationClassId by lazy {
            val annotationFqNameUnsafe = FqNameUnsafe(annotationFqName)
            ClassId(FqName(annotationFqNameUnsafe.parent()), FqName(annotationFqNameUnsafe.shortName().asString()), false)
        }

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitCall(expression: IrCall) {
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            val function = expression.symbol.owner
            if (function.containsAnnotationToCheckCalls()) {
                handleFunctionWithAnnotation(function)
            }
        }

        override fun visitFieldAccess(expression: IrFieldAccessExpression) {
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            val field = expression.symbol.owner
            if (field.containsAnnotationToCheckCalls()) {
                handleFunctionWithAnnotation(field)
            }
        }

        private fun IrAnnotationContainer.containsAnnotationToCheckCalls() =
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            annotations.any { it.symbol.owner.parentClassId == annotationClassId }
    }
}
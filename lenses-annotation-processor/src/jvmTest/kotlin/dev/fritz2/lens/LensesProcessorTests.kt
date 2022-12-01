package dev.fritz2.lens

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.test.Test

class LensesProcessorTests {

    @ExperimentalPathApi
    private fun compileSource(vararg source: SourceFile) = KotlinCompilation().apply {
        sources = source.toList()
        symbolProcessorProviders = listOf(LensesProcessorProvider())
        workingDir = createTempDirectory("fritz2-tests").toFile()
        inheritClassPath = true
        verbose = false
    }.compile()

    // workaround copied by https://github.com/tschuchortdev/kotlin-compile-testing/issues/129#issuecomment-804390310
    internal val KotlinCompilation.Result.workingDir: File
        get() =
            outputDirectory.parentFile!!

    // workaround inspired by https://github.com/tschuchortdev/kotlin-compile-testing/issues/129#issuecomment-804390310
    val KotlinCompilation.Result.kspGeneratedSources: List<File>
        get() {
            val kspWorkingDir = workingDir.resolve("ksp")
            val kspGeneratedDir = kspWorkingDir.resolve("sources")
            val kotlinGeneratedDir = kspGeneratedDir.resolve("kotlin")
            val javaGeneratedDir = kspGeneratedDir.resolve("java")
            return kotlinGeneratedDir.walkTopDown().toList() +
                    javaGeneratedDir.walkTopDown()
        }

    @ExperimentalPathApi
    @Test
    fun `validate lenses generation works`() {
        val kotlinSource = SourceFile.kotlin(
            "dataClassesForLensesTests.kt", """
                package dev.fritz2.lenstest

                import dev.fritz2.core.Lenses

                class MyType
                class MyGenericType<T>

                @Lenses
                data class Foo(
                    val bar: Int,
                    val foo: String,
                    val fooBar: MyType,
                    val baz: MyGenericType<Int>
                ) {
                    companion object
                }
            """
        )

        val compilationResult = compileSource(kotlinSource)

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
            softly.assertThat(
                compilationResult.kspGeneratedSources.find { it.name == "FooLenses.kt" }
            ).usingCharset(StandardCharsets.UTF_8).hasContent(
                """
                |// GENERATED by fritz2 - NEVER CHANGE CONTENT MANUALLY!
                |package dev.fritz2.lenstest
                |
                |import dev.fritz2.core.Lens
                |import dev.fritz2.core.lensOf
                |import kotlin.Int
                |import kotlin.String
                |
                |public fun Foo.Companion.bar(): Lens<Foo, Int> = lensOf(
                |    "bar",
                |    { it.bar },
                |    { p, v -> p.copy(bar = v)}
                |  )
                |
                |public fun Foo.Companion.foo(): Lens<Foo, String> = lensOf(
                |    "foo",
                |    { it.foo },
                |    { p, v -> p.copy(foo = v)}
                |  )
                |
                |public fun Foo.Companion.fooBar(): Lens<Foo, MyType> = lensOf(
                |    "fooBar",
                |    { it.fooBar },
                |    { p, v -> p.copy(fooBar = v)}
                |  )
                |
                |public fun Foo.Companion.baz(): Lens<Foo, MyGenericType<Int>> = lensOf(
                |    "baz",
                |    { it.baz },
                |    { p, v -> p.copy(baz = v)}
                |  )
                """.trimMargin()
            )
        }
    }

    @ExperimentalPathApi
    @Test
    fun `lenses can handle multiple classes`() {
        val kotlinSource = SourceFile.kotlin(
            "dataClassesForLensesTests.kt", """
                package dev.fritz2.lenstest

                import dev.fritz2.core.Lenses

                // lenses will appear in `FooLenses.kt`
                @Lenses
                data class Foo(val bar: Int) {
                    companion object
                }

                // lenses will appear in `BarLenses.kt`
                @Lenses
                data class Bar(val bar: Int) {
                    companion object
                }
            """
        )

        val compilationResult = compileSource(kotlinSource)

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
            softly.assertThat(
                compilationResult.kspGeneratedSources.find { it.name == "FooLenses.kt" }
            ).usingCharset(StandardCharsets.UTF_8).hasContent(
                """
                |// GENERATED by fritz2 - NEVER CHANGE CONTENT MANUALLY!
                |package dev.fritz2.lenstest
                |
                |import dev.fritz2.core.Lens
                |import dev.fritz2.core.lensOf
                |import kotlin.Int
                |
                |public fun Foo.Companion.bar(): Lens<Foo, Int> = lensOf(
                |    "bar",
                |    { it.bar },
                |    { p, v -> p.copy(bar = v)}
                |  )
                """.trimMargin()
            )
            softly.assertThat(
                compilationResult.kspGeneratedSources.find { it.name == "BarLenses.kt" }
            ).usingCharset(StandardCharsets.UTF_8).hasContent(
                """
                |// GENERATED by fritz2 - NEVER CHANGE CONTENT MANUALLY!
                |package dev.fritz2.lenstest
                |
                |import dev.fritz2.core.Lens
                |import dev.fritz2.core.lensOf
                |import kotlin.Int
                |
                |public fun Bar.Companion.bar(): Lens<Bar, Int> = lensOf(
                |    "bar",
                |    { it.bar },
                |    { p, v -> p.copy(bar = v)}
                |  )
                """.trimMargin()
            )
        }
    }

    @ExperimentalPathApi
    @Test
    fun `lenses can cope with named companion objects`() {
        val kotlinSource = SourceFile.kotlin(
            "dataClassesForLensesTests.kt", """
                package dev.fritz2.lenstest

                import dev.fritz2.core.Lenses

                @Lenses
                data class Foo(val bar: Int) {
                    companion object MySpecialCompanion
                }
            """
        )

        val compilationResult = compileSource(kotlinSource)

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
            softly.assertThat(
                compilationResult.kspGeneratedSources.find { it.name == "FooLenses.kt" }
            ).usingCharset(StandardCharsets.UTF_8).hasContent(
                """
                |// GENERATED by fritz2 - NEVER CHANGE CONTENT MANUALLY!
                |package dev.fritz2.lenstest
                |
                |import dev.fritz2.core.Lens
                |import dev.fritz2.core.lensOf
                |import kotlin.Int
                |
                |public fun Foo.MySpecialCompanion.bar(): Lens<Foo, Int> = lensOf(
                |    "bar",
                |    { it.bar },
                |    { p, v -> p.copy(bar = v)}
                |  )
                """.trimMargin()
            )
        }
    }


    @ExperimentalPathApi
    @Test
    fun `lenses with no public property value in ctor will not generate anything`() {
        val kotlinSource = SourceFile.kotlin(
            "dataClassesForLensesTests.kt", """
                package dev.fritz2.lenstest

                import dev.fritz2.core.Lenses

                @Lenses
                data class Foo(private val foo: Int, param: String) { // no public property defined in ctor!
                    companion object
                    val someNoneCtorProp: Int = foo + 1
                }
            """
        )

        val compilationResult = compileSource(kotlinSource)

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
            softly.assertThat(compilationResult.kspGeneratedSources.find { it.name == "FooLenses.kt" }).isNull()
            softly.assertThat(compilationResult.messages).contains("can not create any lenses though")
        }
    }

    @ExperimentalPathApi
    @ParameterizedTest
    @MethodSource("getFalseAnnotatedEntities")
    fun `lenses will throw error if not applied to data class`(kotlinSource: SourceFile) {
        val compilationResult = compileSource(kotlinSource)

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
            softly.assertThat(compilationResult.messages).contains("Foo is not a data class!")
        }
    }


    @ExperimentalPathApi
    @Test
    fun `lenses will throw error if companion object is missing`() {
        val kotlinSource = SourceFile.kotlin(
            "dataClassesForLensesTests.kt", """
                package dev.fritz2.lenstest

                import dev.fritz2.core.Lenses

                @Lenses
                data class Foo(val bar: Int)
                // no companion declared 
            """
        )

        val compilationResult = compileSource(kotlinSource)

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
            softly.assertThat(compilationResult.messages)
                .contains("The companion object for data class Foo is missing!")
        }
    }

    @ExperimentalPathApi
    @ParameterizedTest
    @MethodSource("getNameBlockingDataClasses")
    fun `lenses will throw error if lens fun's name is already in use`(kotlinSource: SourceFile) {
        val compilationResult = compileSource(kotlinSource)

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
            softly.assertThat(compilationResult.messages)
                .contains("The companion object of Foo already defines the following functions / properties")
        }
    }

    @ExperimentalPathApi
    @Test
    fun `lenses ignore none ctor or private ctor properties`() {
        val kotlinSource = SourceFile.kotlin(
            "dataClassesForLensesTests.kt", """
                package dev.fritz2.lenstest

                import dev.fritz2.core.Lenses

                @Lenses
                data class Foo(val bar: Int, private val ignoredProp: Int) {
                //                           ^^^^^^^
                //                           private field -> no lens possible!
                    companion object
                    val ignored = bar + 1 // must not appear in lens!
                }
            """
        )

        val compilationResult = compileSource(kotlinSource)

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
            softly.assertThat(
                compilationResult.kspGeneratedSources.find { it.name == "FooLenses.kt" }
            ).usingCharset(StandardCharsets.UTF_8).hasContent(
                """
                |// GENERATED by fritz2 - NEVER CHANGE CONTENT MANUALLY!
                |package dev.fritz2.lenstest
                |
                |import dev.fritz2.core.Lens
                |import dev.fritz2.core.lensOf
                |import kotlin.Int
                |
                |public fun Foo.Companion.bar(): Lens<Foo, Int> = lensOf(
                |    "bar",
                |    { it.bar },
                |    { p, v -> p.copy(bar = v)}
                |  )
                """.trimMargin()
            )
        }
    }

    @ExperimentalPathApi
    @Test
    fun `lenses can handle generic data classes`() {
        val kotlinSource = SourceFile.kotlin(
            "dataClassesForLensesTests.kt", """
                package dev.fritz2.lenstest

                import dev.fritz2.core.Lenses

                @Lenses
                data class Foo<T>(val bar: T) {
                    companion object
                }

                @Lenses
                data class Bar<T, E>(val foo: T, val fooBar: E) {
                    companion object
                }
            """
        )

        val compilationResult = compileSource(kotlinSource)

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
            softly.assertThat(
                compilationResult.kspGeneratedSources.find { it.name == "FooLenses.kt" }
            ).usingCharset(StandardCharsets.UTF_8).hasContent(
                """
                |// GENERATED by fritz2 - NEVER CHANGE CONTENT MANUALLY!
                |package dev.fritz2.lenstest
                |
                |import dev.fritz2.core.Lens
                |import dev.fritz2.core.lensOf
                |
                |public fun <T> Foo.Companion.bar(): Lens<Foo<T>, T> = lensOf(
                |    "bar",
                |    { it.bar },
                |    { p, v -> p.copy(bar = v)}
                |  )
                """.trimMargin()
            )
            softly.assertThat(
                compilationResult.kspGeneratedSources.find { it.name == "BarLenses.kt" }
            ).usingCharset(StandardCharsets.UTF_8).hasContent(
                """
                |// GENERATED by fritz2 - NEVER CHANGE CONTENT MANUALLY!
                |package dev.fritz2.lenstest
                |
                |import dev.fritz2.core.Lens
                |import dev.fritz2.core.lensOf
                |
                |public fun <T, E> Bar.Companion.foo(): Lens<Bar<T, E>, T> = lensOf(
                |    "foo",
                |    { it.foo },
                |    { p, v -> p.copy(foo = v)}
                |  )
                |
                |public fun <T, E> Bar.Companion.fooBar(): Lens<Bar<T, E>, E> = lensOf(
                |    "fooBar",
                |    { it.fooBar },
                |    { p, v -> p.copy(fooBar = v)}
                |  )
                """.trimMargin()
            )
        }
    }

    /**
     * See use case: https://github.com/jwstegemann/fritz2/issues/480
     */
    @ExperimentalPathApi
    @Test
    fun `lenses with nullable generic property works`() {
        val kotlinSource = SourceFile.kotlin(
            "dataClassesForLensesTests.kt", """
                package dev.fritz2.lenstest

                import dev.fritz2.core.Lenses

                @Lenses
                data class Data<T> (val item : T? = null) {
                    companion object
                }
            """
        )

        val compilationResult = compileSource(kotlinSource)

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
            softly.assertThat(
                compilationResult.kspGeneratedSources.find { it.name == "DataLenses.kt" }
            ).usingCharset(StandardCharsets.UTF_8).hasContent(
                """
                |// GENERATED by fritz2 - NEVER CHANGE CONTENT MANUALLY!
                |package dev.fritz2.lenstest
                |
                |import dev.fritz2.core.Lens
                |import dev.fritz2.core.lensOf
                |
                |public fun <T> Data.Companion.item(): Lens<Data<T>, T?> = lensOf(
                |    "item",
                |    { it.item },
                |    { p, v -> p.copy(item = v)}
                |  )
                """.trimMargin()
            )
        }
    }


    companion object {
        @JvmStatic
        fun getFalseAnnotatedEntities(): List<Arguments> {
            val resultForSimpleClass = Arguments.of(
                SourceFile.kotlin(
                    "SimpleClass.kt", """
                package dev.fritz2.lenstest

                import dev.fritz2.core.Lenses

                @Lenses
                class Foo
            """
                )
            )

            val resultForInterface = Arguments.of(
                SourceFile.kotlin(
                    "Interface.kt", """
                package dev.fritz2.lenstest

                import dev.fritz2.core.Lenses

                @Lenses
                interface Foo
            """
                )
            )

            val resultForObject = Arguments.of(
                SourceFile.kotlin(
                    "Object.kt", """
                package dev.fritz2.lenstest

                import dev.fritz2.core.Lenses

                @Lenses
                object Foo
            """
                )
            )

            return listOf(resultForSimpleClass, resultForInterface, resultForObject)
        }

        @JvmStatic
        fun getNameBlockingDataClasses() = listOf(
            SourceFile.kotlin(
                "dataClassesForLensesTests.kt", """
                package dev.fritz2.lenstest

                import dev.fritz2.core.Lenses

                @Lenses
                data class Foo(val bar: Int, val foo: Int) {
                    companion object {
                        val bar = 42 // block name for lens creation!
                        // foo() is available though
                    }
                }
            """
            ),
            SourceFile.kotlin(
                "dataClassesForLensesTests.kt", """
                package dev.fritz2.lenstest

                import dev.fritz2.core.Lenses

                @Lenses
                data class Foo(val bar: Int, val foo: Int) {
                    companion object {
                        fun bar() = 42 // block name for lens creation!
                        // foo() is available though
                    }
                }
            """
            ),
        )

    }
}
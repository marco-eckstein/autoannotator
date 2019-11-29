package com.marcoeckstein.autoannotator.core

import com.marcoeckstein.autoannotator.api.AnnotationInfo
import com.marcoeckstein.autoannotator.api.AutoAnnotatorIgnored
import com.marcoeckstein.autoannotator.api.ClassFilter
import com.marcoeckstein.autoannotator.api.ClassOptions
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import javassist.ClassPool
import org.junit.BeforeClass
import org.junit.Test
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.validation.constraints.Pattern

class KBasicPackageAnnotatorTests {

    @Entity
    internal class Annotated(
        val nonNullString: String,

        val nullableString: String?,

        @field:Pattern(regexp = "originalRegexp")
        val nullableStringWithExplicitPattern: String?,

        val zonedDateTime: ZonedDateTime,

        @AutoAnnotatorIgnored // Applies to field
        val ignoredString: String
    )

    @AutoAnnotatorIgnored
    @Entity
    internal class Ignored(val nonNullString: String)

    companion object {

        private lateinit var annotated: Class<*>
        private lateinit var ignored: Class<*>

        @JvmStatic
        @BeforeClass
        fun setUp() {
            val className = KBasicPackageAnnotatorTests::class.java.name
            val packageName = KBasicPackageAnnotatorTests::class.java.getPackage().name
            val classPool = ClassPool.getDefault()
            val annotatedCtClass = classPool.get("$className\$Annotated")
            val ignoredCtClass = classPool.get("$className\$Ignored")
            PackageAnnotator(
                allClasses = setOf(annotatedCtClass, ignoredCtClass),
                classFilter = ClassFilter(packageName),
                classOptions = ClassOptions(
                    annotationsByFieldType = mapOf(
                        ZonedDateTime::class.java.name to setOf(
                            AnnotationInfo(
                                clazz = Column::class,
                                members = mapOf(Column::columnDefinition to "timestamp with time zone")
                            )
                        )
                    )
                )
            ).annotate()
            annotated = annotatedCtClass.toClass()
            ignored = ignoredCtClass.toClass()
        }
    }

    @Test
    fun `annotates non-null string with NotNull and NotBlank`() {
        with(annotated.getDeclaredField("nonNullString")) {
            annotations shouldHaveSize 2
            getAnnotation(javax.validation.constraints.NotNull::class.java) shouldNotBe null
            getAnnotation(javax.validation.constraints.NotBlank::class.java) shouldNotBe null
        }
    }

    @Test
    fun `annotates nullable string with a not-blank constraint`() {
        with(annotated.getDeclaredField("nullableString")) {
            annotations shouldHaveSize 1
            getAnnotation(Pattern::class.java).also {
                it shouldNotBe null
                it.regexp shouldBe Regex("""(?s).*\S.*""").pattern
            }
        }
    }

    @Test
    fun `does not overwrite explicit annotation members`() {
        with(annotated.getDeclaredField("nullableStringWithExplicitPattern")) {
            annotations shouldHaveSize 1
            getAnnotation(Pattern::class.java).also {
                it shouldNotBe null
                it.regexp shouldBe Regex("originalRegexp").pattern
            }
        }
    }

    @Test
    fun `annotates types with given annotations`() {
        with(annotated.getDeclaredField("zonedDateTime")) {
            getAnnotation(Column::class.java).also {
                it shouldNotBe null
                it.columnDefinition shouldBe "timestamp with time zone"
            }
        }
    }

    @Test
    fun `can ignore fields`() {
        with(annotated.getDeclaredField("ignoredString")) {
            annotations shouldHaveSize 1
            getAnnotation(AutoAnnotatorIgnored::class.java) shouldNotBe null
        }
    }

    @Test
    fun `can ignore classes`() {
        ignored.getDeclaredField("nonNullString").annotations shouldHaveSize 0
    }
}

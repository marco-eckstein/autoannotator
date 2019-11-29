package com.marcoeckstein.autoannotator.core

import com.marcoeckstein.autoannotator.api.ClassFilter
import com.marcoeckstein.autoannotator.api.ClassOptions
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import javassist.ClassPool
import org.junit.BeforeClass
import org.junit.Test
import javax.persistence.Column
import javax.persistence.Entity
import javax.validation.constraints.Size

class KAdvancedPackageAnnotatorTests {

    @Entity
    internal class Annotated(
        val nonNullString: String,

        val nullableString: String?,

        @field:Size(max = 1)
        val limitedString: String,

        @field:[Size(max = 1) Column(length = 42)]
        val limitedStringWithDifferentColumnLength: String,

        @field:[Size(max = 1) Column(name = "originalName")]
        val limitedStringWithColumnName: String
    )

    companion object {

        private lateinit var annotated: Class<*>

        @JvmStatic
        @BeforeClass
        fun setUp() {
            val className = KAdvancedPackageAnnotatorTests::class.java.name
            val packageName = KAdvancedPackageAnnotatorTests::class.java.getPackage().name
            val classPool = ClassPool.getDefault()
            val annotatedCtClass = classPool.get("$className\$Annotated")
            PackageAnnotator(
                allClasses = setOf(annotatedCtClass),
                classFilter = ClassFilter(packageName),
                classOptions = ClassOptions(
                    validationAnnotations = ClassOptions.ValidationAnnotations(
                        inferNotNullConstraint = false,
                        inferStringsNotBlankConstraints = false
                    ),
                    jpaAnnotations = ClassOptions.JpaAnnotations(
                        inferColumnNullable = true,
                        inferColumnLength = true
                    )
                )
            ).annotate()
            annotated = annotatedCtClass.toClass()
        }
    }

    @Test
    fun `annotates non-null string with Column(nullable=false)`() {
        with(annotated.getDeclaredField("nonNullString")) {
            annotations shouldHaveSize 1
            getAnnotation(Column::class.java).also {
                it shouldNotBe null
                it.nullable shouldBe false
            }
        }
    }

    @Test
    fun `annotates nullable string with nothing`() {
        with(annotated.getDeclaredField("nullableString")) {
            annotations shouldHaveSize 0
        }
    }

    @Test
    fun `annotates limited string with Column(length=maxSize)`() {
        with(annotated.getDeclaredField("limitedString")) {
            getAnnotation(Column::class.java).also {
                it shouldNotBe null
                it.length shouldBe getAnnotation(Size::class.java).max
            }
        }
    }

    @Test
    fun `does not overwrite explicit Column length`() {
        with(annotated.getDeclaredField("limitedStringWithDifferentColumnLength")) {
            getAnnotation(Column::class.java).also {
                it shouldNotBe null
                it.length shouldNotBe getAnnotation(Size::class.java).max
            }
        }
    }

    @Test
    fun `does not overwrite explicit Column name`() {
        with(annotated.getDeclaredField("limitedStringWithColumnName")) {
            getAnnotation(Column::class.java).also {
                it shouldNotBe null
                it.name shouldBe "originalName"
            }
        }
    }

    @Test
    fun `works with more complex case`() {
        with(annotated.getDeclaredField("limitedStringWithColumnName")) {
            annotations shouldHaveSize 2
            getAnnotation(Size::class.java) shouldNotBe null
            getAnnotation(Column::class.java).also {
                it shouldNotBe null
                it.nullable shouldBe false
                it.length shouldBe getAnnotation(Size::class.java).max
                it.name shouldBe "originalName"
            }
        }
    }
}

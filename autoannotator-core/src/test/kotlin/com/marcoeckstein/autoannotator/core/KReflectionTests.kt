package com.marcoeckstein.autoannotator.core

import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldNotBe
import javassist.ClassPool
import org.junit.Test
import java.lang.annotation.RetentionPolicy

/**
 * Reflection demo for better understanding of the actual tests
 */
class KReflectionTests {

    internal class MyClass(val nonNullString: String)

    /**
     * The Kotlin compiler adds [org.jetbrains.annotations.NotNull] to non-null fields.
     * Java reflection does not find this annotation due to its [RetentionPolicy], but Javassist does.
     */
    @Test
    fun `jetbrains' @NotNull is not visible for normal reflection but for Javassist`() {
        MyClass::class.java.getDeclaredField("nonNullString").also {
            it.annotations shouldHaveSize 0
        }
        ClassPool.getDefault().get(MyClass::class.java.name).getField("nonNullString").also {
            it.annotations shouldHaveSize 1
            it.getAnnotation(org.jetbrains.annotations.NotNull::class.java) shouldNotBe null
        }
    }
}

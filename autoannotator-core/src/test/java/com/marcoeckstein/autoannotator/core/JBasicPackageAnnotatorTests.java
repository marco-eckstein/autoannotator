package com.marcoeckstein.autoannotator.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.marcoeckstein.autoannotator.api.*;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.Pattern;
import java.lang.reflect.Field;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A subset of the tests implemented in Kotlin.
 * <p>
 * They demonstrate basic usage in Java. Please see the Kotlin tests for more behavior.
 */
@SuppressWarnings("unused")
public class JBasicPackageAnnotatorTests {

    @Entity
    static class Annotated {
        @javax.annotation.Nonnull
        String nonNullString = "";

        String nullableString;

        @Pattern(regexp = "originalRegexp")
        String nullableStringWithExplicitPattern;

        ZonedDateTime zonedDateTime;

        @AutoAnnotatorIgnored
        String ignoredString;
    }

    @AutoAnnotated(fieldsAreNonnullByDefault = true)
    static class Annotated2 {
        String nonNullString = "";

        @javax.annotation.Nullable
        String nullableString;
    }

    @AutoAnnotatorIgnored
    @Entity
    static class Ignored {
        @javax.annotation.Nonnull
        String nonNullString = "";
    }

    private static Class<?> annotated;
    private static Class<?> annotated2;
    private static Class<?> ignored;

    @BeforeClass
    public static void setUp() throws NotFoundException, CannotCompileException {
        String className = JBasicPackageAnnotatorTests.class.getName();
        String packageName = JBasicPackageAnnotatorTests.class.getPackage().getName();
        ClassPool classPool = ClassPool.getDefault();
        CtClass annotatedCtClass = classPool.get(className + "$Annotated");
        CtClass annotated2CtClass = classPool.get(className + "$Annotated2");
        CtClass ignoredCtClass = classPool.get(className + "$Ignored");
        new PackageAnnotator(
            ImmutableSet.of(annotatedCtClass, annotated2CtClass, ignoredCtClass),
            new ClassFilter(packageName),
            new ClassOptions(
                ImmutableMap.of(
                    ZonedDateTime.class.getName(),
                    ImmutableSet.of(
                        new AnnotationInfo(
                            javax.persistence.Column.class, // clazz
                            ImmutableMap.of("columnDefinition", "timestamp with time zone") // members
                        )
                    )
                )
            )
        ).annotate();
        annotated = annotatedCtClass.toClass();
        annotated2 = annotated2CtClass.toClass();
        ignored = ignoredCtClass.toClass();
    }

    @Test
    public void annotates_non_null_string_with_NotNull_and_NotBlank() throws NoSuchFieldException {
        for (Class<?> clazz : ImmutableList.of(annotated, annotated2)) {
            Field field = clazz.getDeclaredField("nonNullString");
            assertThat(field.getAnnotations()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(field.getAnnotation(javax.validation.constraints.NotNull.class)).isNotNull();
            assertThat(field.getAnnotation(javax.validation.constraints.NotBlank.class)).isNotNull();
        }
    }

    @Test
    public void annotates_nullable_string_with_non_blank_constraint() throws NoSuchFieldException {
        for (Class<?> clazz : ImmutableList.of(annotated, annotated2)) {
            Field field = clazz.getDeclaredField("nullableString");
            assertThat(field.getAnnotations()).hasSizeGreaterThanOrEqualTo(1);
            Pattern pattern = field.getAnnotation(Pattern.class);
            assertThat(pattern).isNotNull();
            assertThat(pattern.regexp()).isEqualTo("(?s).*\\S.*");
        }
    }

    @Test
    public void does_not_overwrite_explicit_annotation_members() throws NoSuchFieldException {
        Field field = annotated.getDeclaredField("nullableStringWithExplicitPattern");
        assertThat(field.getAnnotations()).hasSizeGreaterThanOrEqualTo(1);
        Pattern pattern = field.getAnnotation(Pattern.class);
        assertThat(pattern).isNotNull();
        assertThat(pattern.regexp()).isEqualTo("originalRegexp");
    }

    @Test
    public void annotates_types_with_given_annotations() throws NoSuchFieldException {
        Field field = annotated.getDeclaredField("zonedDateTime");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.columnDefinition()).isEqualTo("timestamp with time zone");
    }

    @Test
    public void can_ignore_fields() throws NoSuchFieldException {
        Field field = annotated.getDeclaredField("ignoredString");
        assertThat(field.getAnnotations()).hasSize(1);
        assertThat(field.getAnnotation(AutoAnnotatorIgnored.class)).isNotNull();
    }

    @Test
    public void can_ignore_classes() throws NoSuchFieldException {
        Field field = ignored.getDeclaredField("nonNullString");
        assertThat(field.getAnnotations()).hasSize(1);
        assertThat(field.getAnnotations()[0].annotationType()).isEqualTo(javax.annotation.Nonnull.class);
    }
}

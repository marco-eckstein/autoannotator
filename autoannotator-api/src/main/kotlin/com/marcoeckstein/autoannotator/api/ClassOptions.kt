package com.marcoeckstein.autoannotator.api

import com.marcoeckstein.autoannotator.api.ClassOptions.Companion.DefaultNonNullAnnotations
import com.marcoeckstein.autoannotator.api.ClassOptions.Companion.DefaultNullableAnnotations
import com.marcoeckstein.autoannotator.api.ClassOptions.JpaAnnotations
import com.marcoeckstein.autoannotator.api.ClassOptions.JpaAnnotations.Companion
    .DefaultAnnotationsThatPrecludeColumnAnnotation
import com.marcoeckstein.autoannotator.api.ClassOptions.JpaAnnotations.Companion
    .DefaultJpaClassAnnotations
import com.marcoeckstein.autoannotator.api.ClassOptions.ValidationAnnotations

/**
 * Options for annotating an individual class/interface.
 */
data class ClassOptions @JvmOverloads constructor(
    /**
     * Arbitrary annotations that will be added to fields, depending on the type of the field.
     *
     * A key is the qualified name of a JVM type, e.g. [java.lang.String] or `int`.
     *
     * Default: empty map
     *
     * ### Kotlin users:
     * Since AutoAnnotator manipulates bytecode, you must use the JVM equivalents of Kotlin types.
     * E.g., use `int` or [java.lang.Integer], not [kotlin.Int].
     */
    val annotationsByFieldType: Map<String, Set<AnnotationInfo>> = mapOf(),
    /**
     * Options regarding `javax.validation.constraints.*` annotations.
     *
     * Default: [ValidationAnnotations]`()` (all enabled)
     */
    val validationAnnotations: ValidationAnnotations = ValidationAnnotations(),
    /**
     * Options regarding `javax.persistence.*` annotations.
     *
     * Default: [JpaAnnotations]`(false, false)` (all disabled; suitable for Hibernate with
     * `hibernate.validator.apply_to_ddl` at its default `true`)
     */
    val jpaAnnotations: JpaAnnotations = JpaAnnotations(
        inferColumnNullable = false,
        inferColumnLength = false
    ),
    /**
     * Fields annotated with any of these annotation qualified names will be ignored.
     *
     * Default: set with [AutoAnnotatorIgnored]'s qualified name
     */
    val ignoredFieldAnnotations: Set<String> = setOf(AutoAnnotatorIgnored::class.java.name),
    /**
     * Annotation qualified names that signal non-nullability.
     *
     * Default: [DefaultNonNullAnnotations]
     */
    val nonNullAnnotations: Set<String> = DefaultNonNullAnnotations,

    /**
     * Annotation qualified names that signal nullability.
     *
     * Default: [DefaultNullableAnnotations]
     */
    val nullableAnnotations: Set<String> = DefaultNullableAnnotations
) {

    /**
     * Options regarding `javax.validation.constraints.*` annotations.
     */
    data class ValidationAnnotations @JvmOverloads constructor(
        /**
         * Infer `@`[javax.validation.constraints.NotNull] from other nullability annotations.
         *
         * Default: `true`
         *
         * ### Kotlin users
         * The compiler inserts `@`[org.jetbrains.annotations.NotNull] for non-null types,
         * so the source code does not have to be annotated at all.
         */
        val inferNotNullConstraint: Boolean = true,
        /**
         * For strings, infer `@`[javax.validation.constraints.NotBlank] or [nullOrNotBlankAnnotation],
         * respectively.
         *
         * Default: `true`
         */
        val inferStringsNotBlankConstraints: Boolean = true,
        /**
         * The annotation to use for nullable but not blank strings.
         *
         * See [stackoverflow](https://stackoverflow.com/questions/31132477/).
         *
         * Default: `@`[javax.validation.constraints.Pattern]`(regexp = "(?s).*\\S.*",
         * message = "must be null or not blank")`
         */
        val nullOrNotBlankAnnotation: AnnotationInfo = AnnotationInfo(
            "javax.validation.constraints.Pattern",
            members = mapOf(
                "regexp" to "(?s).*\\S.*",
                "message" to "must be null or not blank"
            )
        )
    )

    /**
     * Options regarding `javax.persistence.*` annotations.
     *
     * If you use Hibernate with `hibernate.validator.apply_to_ddl` at its default `true`, you probably want
     * to set all `infer*` options to `false`.
     *
     * If your JPA provider does not use validation constraints to infer database constraints, you may want
     * to set some `infer*` options  to `true`. Note that this may lead to problems when mapping a class
     * hierarchy with the table-per-hierarchy strategy. If so, you might need to explicitly add
     * `@`[javax.persistence.Column]`(nullable = true)` or `@`[AutoAnnotatorIgnored] to the affected fields.
     */
    data class JpaAnnotations @JvmOverloads constructor(
        /**
         * For JPA classes, infer `@`[javax.persistence.Column]`(nullable = false)` from
         * nullability annotations.
         *
         * Default: `true`
         *
         * ### Kotlin users
         * The compiler inserts `@`[org.jetbrains.annotations.NotNull] for non-null types,
         * so the source code does not have to be annotated at all.
         */
        val inferColumnNullable: Boolean = true,
        /**
         * For JPA classes, infer `@`[javax.persistence.Column]`(length = n)` from
         * `@`[javax.validation.constraints.Size]`(max = n)`
         *
         * Default: `true`
         */
        val inferColumnLength: Boolean = true,
        /**
         * Annotation qualified names that determine which classes are treated as JPA classes.
         *
         * Default: [DefaultJpaClassAnnotations]
         */
        val jpaClassAnnotations: Set<String> = DefaultJpaClassAnnotations,
        /**
         * Default: [DefaultAnnotationsThatPrecludeColumnAnnotation]
         */
        val annotationsThatPrecludeColumnAnnotation: Set<String> =
            DefaultAnnotationsThatPrecludeColumnAnnotation
    ) {

        companion object {

            /**
             * - [javax.persistence.Entity]
             * - [javax.persistence.Embeddable]
             * - [javax.persistence.MappedSuperclass]
             */
            @JvmField
            val DefaultJpaClassAnnotations = setOf(
                "javax.persistence.Entity",
                "javax.persistence.Embeddable",
                "javax.persistence.MappedSuperclass"
            )

            /**
             * - [javax.persistence.ManyToOne]
             */
            @JvmField
            val DefaultAnnotationsThatPrecludeColumnAnnotation = setOf(
                "javax.persistence.ManyToOne"
            )
        }
    }

    companion object {
        /**
         * - `org.jetbrains.annotations.NotNull`
         * - `javax.annotation.NonNull`
         * - `edu.umd.cs.findbugs.annotations.NonNull`
         * - `android.support.annotation.NonNull`
         * - `androidx.annotation.NonNull`
         * - `androidx.annotation.RecentlyNonNull`
         * - `org.checkerframework.checker.nullness.qual.NonNull`
         * - `org.checkerframework.checker.nullness.compatqual.NonNullDecl`
         * - `org.checkerframework.checker.nullness.compatqual.NonNullType`
         * - `com.android.annotations.NonNull`
         * - `javax.validation.constraints.NotNull`
         * - `javax.validation.constraints.NotNull.List`
         */
        @Suppress("SpellCheckingInspection")
        @JvmField
        val DefaultNonNullAnnotations = setOf(
            // From IntelliJ as of 2019-10-30 (search for "configure annotations"):
            "org.jetbrains.annotations.NotNull",
            "javax.annotation.Nonnull",
            "edu.umd.cs.findbugs.annotations.NonNull",
            "android.support.annotation.NonNull",
            "androidx.annotation.NonNull",
            "androidx.annotation.RecentlyNonNull",
            "org.checkerframework.checker.nullness.qual.NonNull",
            "org.checkerframework.checker.nullness.compatqual.NonNullDecl",
            "org.checkerframework.checker.nullness.compatqual.NonNullType",
            "com.android.annotations.NonNull",
            // Additional:
            "javax.validation.constraints.NotNull",
            "javax.validation.constraints.NotNull.List"
        )

        /**
         * - org.jetbrains.annotations.Nullable
         * - javax.annotation.Nullable
         * - javax.annotation.CheckForNull
         * - edu.umd.cs.findbugs.annotations.Nullable
         * - android.support.annotation.Nullable
         * - androidx.annotation.Nullable
         * - androidx.annotation.RecentlyNullable
         * - org.checkerframework.checker.nullness.qual.Nullable
         * - org.checkerframework.checker.nullness.compatqual.NullableDecl
         * - org.checkerframework.checker.nullness.compatqual.NullableType
         * - com.android.annotations.Nullable
         */
        @Suppress("SpellCheckingInspection")
        @JvmField
        val DefaultNullableAnnotations = setOf(
            // From IntelliJ as of 2019-10-30 (search for "configure annotations"):
            "org.jetbrains.annotations.Nullable",
            "javax.annotation.Nullable",
            "javax.annotation.CheckForNull",
            "edu.umd.cs.findbugs.annotations.Nullable",
            "android.support.annotation.Nullable",
            "androidx.annotation.Nullable",
            "androidx.annotation.RecentlyNullable",
            "org.checkerframework.checker.nullness.qual.Nullable",
            "org.checkerframework.checker.nullness.compatqual.NullableDecl",
            "org.checkerframework.checker.nullness.compatqual.NullableType",
            "com.android.annotations.Nullable"
        )
    }
}

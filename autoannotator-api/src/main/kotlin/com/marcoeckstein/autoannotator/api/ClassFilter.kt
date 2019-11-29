package com.marcoeckstein.autoannotator.api

/**
 * A [ClassFilter] determines which classes/interfaces get auto-annotated.
 */
data class ClassFilter @JvmOverloads constructor(
    /**
     * The prefix of the packages that contain the classes/interfaces to annotate.
     *
     * Default: "" (empty, i.e. annotate all classes)
     */
    val packagePrefix: String = "",
    /**
     * The annotations that mark the classes/interfaces to annotate.
     *
     * Default:
     *   - [javax.persistence.Entity]
     *   - [javax.persistence.Embeddable]
     *   - [javax.persistence.MappedSuperclass]
     *   - [com.marcoeckstein.autoannotator.api.AutoAnnotated]
     */
    val markerAnnotations: Set<String> =
        ClassOptions.JpaAnnotations.DefaultJpaClassAnnotations + AutoAnnotated::class.qualifiedName!!,
    /**
     * Classes annotated with any of these annotation will be ignored.
     *
     * Default:  [com.marcoeckstein.autoannotator.api.AutoAnnotatorIgnored]
     */
    val ignoredAnnotations: Set<String> = setOf(AutoAnnotatorIgnored::class.qualifiedName!!)
)

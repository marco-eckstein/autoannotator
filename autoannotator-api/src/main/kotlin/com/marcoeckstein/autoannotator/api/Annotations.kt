package com.marcoeckstein.autoannotator.api

/**
 * This annotation is used to explicitly exclude classes and fields from being auto-annotated,
 * even if they would otherwise be included according to the configuration.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
annotation class AutoAnnotatorIgnored

/**
 * This annotation is used to explicitly include classes in being auto-annotated.
 * To be discovered, these classes must still reside in packages that are configured to be auto-annotated.
 *
 * @param fieldsAreNonnullByDefault Treat all fields as if they were annotated with a non-null annotation.
 *                                  Default: `false`
 */
@Target(AnnotationTarget.CLASS)
annotation class AutoAnnotated(val fieldsAreNonnullByDefault: Boolean = false)

/**
 * Annotation for the single static parameterless method in one of your classes that returns an
 * instance of [AutoAnnotatorConfig].
 */
@Target(AnnotationTarget.FUNCTION)
annotation class AutoAnnotatorConfigSource

package com.marcoeckstein.autoannotator.api

/**
 * The class that holds the AutoAnnotator configuration.
 *
 * In your classes, there must be a single static parameterless method that returns an instance of this class
 * and is annotated with `@`[AutoAnnotatorConfigSource].
 */
data class AutoAnnotatorConfig(
    /**
     * Default: [ClassFilter]`()`
     */
    val classFilter: ClassFilter = ClassFilter(),
    /**
     * Default: [ClassOptions]`()`
     */
    val classOptions: ClassOptions = ClassOptions()
)

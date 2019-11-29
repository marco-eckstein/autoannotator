package com.marcoeckstein.autoannotator.core

import com.marcoeckstein.autoannotator.api.AutoAnnotatorConfigSource
import java.io.File

sealed class AutoAnnotatorException(message: String) : RuntimeException(message)

class InconsistentNullabilityException(
    className: String,
    fieldName: String,
    offendingAnnotationNames: Iterable<String>
) : AutoAnnotatorException(
    "Field $className.$fieldName is annotated with an annotation that signals non-nullability and " +
        "and an annotation that signals nullability: $offendingAnnotationNames"
)

class NoClassFilesException(outDirectories: Iterable<File>) : AutoAnnotatorException(
    "Found no class files in these directories: $outDirectories"
)

class MisconfigurationException(configSourceMethodCount: Int) : AutoAnnotatorException(
    "There must be a single static zero-argument method annotated with " +
        "@${AutoAnnotatorConfigSource::class.qualifiedName}, but found $configSourceMethodCount."
)

class ConfigurationChangeException(oldConfig: String, newConfig: String) : AutoAnnotatorException(
    """
        The configuration has changed. You must rebuild (i.e. clean and build) your project.
        Old: $oldConfig
        New: $newConfig
    """.trimIndent()
)

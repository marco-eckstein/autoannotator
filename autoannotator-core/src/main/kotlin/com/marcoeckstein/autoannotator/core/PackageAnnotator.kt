package com.marcoeckstein.autoannotator.core

import com.marcoeckstein.autoannotator.api.ClassFilter
import com.marcoeckstein.autoannotator.api.ClassOptions
import javassist.CtClass
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PackageAnnotator(
    private val allClasses: Iterable<CtClass>,
    private val classFilter: ClassFilter,
    private val classOptions: ClassOptions
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun annotate() {
        logger.info(
            "Annotate classes in packages with prefix '${classFilter.packagePrefix}' that are annotated " +
                "with any of ${classFilter.markerAnnotations}."
        )
        val changeCount = allClasses
            .filter {
                it.packageName.startsWith(classFilter.packagePrefix) &&
                    classFilter.markerAnnotations.any { markerAnnotation -> it.hasAnnotation(markerAnnotation) }
            }
            .filter {
                !it.mustBeIgnored.also { ignored -> if (ignored) logger.debug("Ignore class ${it.name}") }
            }
            .count { ClassAnnotator(it, classOptions).annotate() }
        logger.info("Annotated $changeCount class" + (if (changeCount == 1) "." else "es."))
    }

    private val CtClass.mustBeIgnored get() = classFilter.ignoredAnnotations.any { hasAnnotation(it) }
}

// In an intermediate stage of development, it was necessary to order classes before manipulating them,
// otherwise there would be a problem with attempted reloads. This ordering seems to be obsolete now,
// but the code is kept in case we might need it again. It uses the JGraphT library.
//
//    fun Iterable<CtClass>.sortedTopologicallyByInheritanceDepth(): List<CtClass> =
//        TopologicalOrderIterator(
//            DirectedAcyclicGraph<CtClass, DefaultEdge>(DefaultEdge::class.java).also { dag ->
//                forEach { type -> dag.addVertex(type) }
//                forEach { type ->
//                    type.supertypes
//                        .filter { it in this }
//                        .forEach { dag.addEdge(it, type) }
//                }
//            }
//        )
//            .asSequence()
//            .toList()

package com.marcoeckstein.autoannotator.api

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * A representation of an annotation
 */
data class AnnotationInfo @JvmOverloads constructor(
    val clazzName: String,
    val members: Map<String, Any> = mapOf()
) {

    @JvmOverloads
    constructor(
        /**
         * The annotation class.
         */
        clazz: Class<out Any>,
        /**
         * The members (aka elements) of the annotation.
         *
         * The types of the values must be legal annotation member types, e.g. String, int, long.
         *
         * Default: empty map
         */
        members: Map<String, Any> = mapOf()
    ) :
        this(clazz.name, members)

    constructor(
        /**
         * The annotation class.
         */
        clazz: KClass<out Any>,
        /**
         * The members (aka elements) of the annotation.
         *
         * The types of the values must be legal annotation member types, e.g. String, int, long.
         *
         * Default: empty map
         */
        members: Map<out KProperty1<*, *>, Any> = mapOf()
    ) :
        this(requireNotNull(clazz.qualifiedName), members.mapKeys { it.key.name })

    // Overriding toString() because toString() of an array yields something like: [Ljava.lang.String;@38082d64
    override fun toString() =
        AnnotationInfo::class.simpleName!! + "(" +
            "clazzName=$clazzName, " +
            "members=" + members.mapValues { (_, value) -> if (value is Array<*>) value.toList() else value } +
            ")"
}

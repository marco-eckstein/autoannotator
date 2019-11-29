package com.marcoeckstein.autoannotator.core

import com.marcoeckstein.autoannotator.api.AnnotationInfo
import com.marcoeckstein.autoannotator.api.AutoAnnotated
import com.marcoeckstein.autoannotator.api.ClassOptions
import com.marcoeckstein.ext.javassist.addFieldAnnotationIfMissing
import com.marcoeckstein.ext.javassist.addFieldAnnotationMemberIfMissing
import com.marcoeckstein.ext.javassist.annotationsStronglyTyped
import javassist.CtClass
import javassist.CtField
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.IntegerMemberValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ClassAnnotator(
    private val clazz: CtClass,
    private val classOptions: ClassOptions
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * @return Changed
     */
    fun annotate(): Boolean {
        logger.debug("Annotate class ${clazz.name}")
        return clazz.declaredFields.count { annotate(it) } > 0
    }

    /**
     * @return Changed
     */
    private fun annotate(field: CtField): Boolean {
        if (field.mustBeIgnored) {
            logger.debug("Ignore field ${clazz.name}.${field.name}")
            return false
        }
        logger.debug("Annotate field ${clazz.name}.${field.name}")
        val annotationsBefore = field.annotationsStronglyTyped.toString()
        addTypeAnnotations(field)
        if (classOptions.validationAnnotations.inferNotNullConstraint)
            inferNotNullConstraint(field)
        if (classOptions.validationAnnotations.inferStringsNotBlankConstraints)
            inferStringsNotBlankConstraints(field)
        if (clazz.isJpaClass && !field.isTransient) {
            if (classOptions.jpaAnnotations.inferColumnNullable) inferColumnNullable(field)
            if (classOptions.jpaAnnotations.inferColumnLength) inferColumnLength(field)
        }
        val annotationsAfter = field.annotationsStronglyTyped.toString()
        return if (annotationsBefore == annotationsAfter) {
            logger.debug("Nothing changed")
            false
        } else {
            logger.info("Changed field ${clazz.name}.${field.name}:")
            logger.info("Before: $annotationsBefore")
            logger.info("After:  $annotationsAfter")
            true
        }
    }

    private fun addTypeAnnotations(field: CtField) {
        classOptions.annotationsByFieldType[field.type.name]?.forEach { annotation ->
            clazz.addFieldAnnotationIfMissing(field, annotation)
        }
    }

    private fun inferNotNullConstraint(field: CtField) {
        if (field.isNonNull) {
            clazz.addFieldAnnotationIfMissing(field, "javax.validation.constraints.NotNull")
        }
    }

    private fun inferStringsNotBlankConstraints(field: CtField) {
        if (field.type.name == "java.lang.String") {
            if (field.isNonNull) {
                clazz.addFieldAnnotationIfMissing(field, "javax.validation.constraints.NotBlank")
            } else {
                clazz.addFieldAnnotationIfMissing(
                    field,
                    classOptions.validationAnnotations.nullOrNotBlankAnnotation
                )
            }
        }
    }

    private fun inferColumnNullable(field: CtField) {
        if (field.isNonNull && field.isColumnAnnotationAllowed) {
            clazz.addFieldAnnotationIfMissing(field, "javax.persistence.Column")
            clazz.addFieldAnnotationMemberIfMissing(field, "javax.persistence.Column", "nullable", false)
        }
    }

    private fun inferColumnLength(field: CtField) {
        val maxSize = field.getMaxSize()
        if (maxSize != null && field.isColumnAnnotationAllowed) {
            clazz.addFieldAnnotationIfMissing(field, "javax.persistence.Column")
            clazz.addFieldAnnotationMemberIfMissing(field, "javax.persistence.Column", "length", maxSize)
        }

//    Idea sketch for an additional feature:
//    private fun inferEntityId() {
//        if (clazz.isEntity && !clazz.hasId) {
//            val idFields = options.jpaAnnotations.idFieldNames
//            val candidates = clazz.declaredFields.filter { it.name in idFields }
//            val id = candidates.singleOrNull()
//                ?: throw CannotInferIdException(clazz.name, idFields, candidates.map { it.name }.toSet())
//            clazz.addFieldAnnotationIfMissing(id,"javax.persistence.Id")
//        }
//    }
    }

    private val CtClass.isJpaClass
        get() = classOptions.jpaAnnotations.jpaClassAnnotations.any { hasAnnotation(it) }

    private fun CtClass.addFieldAnnotationIfMissing(
        field: CtField,
        annotation: AnnotationInfo
    ) {
        addFieldAnnotationIfMissing(field, annotation.clazzName)
        annotation.members.forEach { m ->
            clazz.addFieldAnnotationMemberIfMissing(field, annotation.clazzName, m.key, m.value)
        }
    }

    private val CtField.mustBeIgnored get() = classOptions.ignoredFieldAnnotations.any { hasAnnotation(it) }

    private val CtField.isColumnAnnotationAllowed
        get() = annotationsStronglyTyped.none {
            it.typeName in classOptions.jpaAnnotations.annotationsThatPrecludeColumnAnnotation
        }

    private val CtField.isNonNull: Boolean
        get() {
            val autoAnnotated: AutoAnnotated? = clazz.getAnnotation(
                AutoAnnotated::class.java
            ) as? AutoAnnotated
            val nonNullByDefault = autoAnnotated?.fieldsAreNonnullByDefault ?: false
            val explicitlyNonNull = annotationsStronglyTyped.any { it.isNonNull }
            val explicitlyNullable = annotationsStronglyTyped.any { it.isNullable }
            if (explicitlyNonNull && explicitlyNullable)
                throw InconsistentNullabilityException(
                    clazz.name,
                    name,
                    annotationsStronglyTyped.filter { it.isNonNull || it.isNullable }.map { it.typeName }
                )
            return explicitlyNonNull || (nonNullByDefault && !explicitlyNullable)
        }

    private val Annotation.isNonNull get() = typeName in classOptions.nonNullAnnotations

    private val Annotation.isNullable get() = typeName in classOptions.nullableAnnotations

    private companion object {

        fun CtField.getMaxSize(): Int? =
            annotationsStronglyTyped.singleOrNull { it.isSize }?.let {
                (it.getMemberValue("max") as? IntegerMemberValue)?.value
            }

        val CtField.isTransient get() = annotationsStronglyTyped.any { it.isTransient }

        val Annotation.isTransient get() = typeName == "javax.persistence.Transient"

        val Annotation.isSize get() = typeName == "javax.validation.constraints.Size"
    }
}

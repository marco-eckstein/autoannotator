package com.marcoeckstein.ext.javassist

import javassist.CtClass
import javassist.CtField
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.Descriptor
import javassist.bytecode.FieldInfo
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.ArrayMemberValue
import javassist.bytecode.annotation.BooleanMemberValue
import javassist.bytecode.annotation.CharMemberValue
import javassist.bytecode.annotation.ClassMemberValue
import javassist.bytecode.annotation.DoubleMemberValue
import javassist.bytecode.annotation.EnumMemberValue
import javassist.bytecode.annotation.FloatMemberValue
import javassist.bytecode.annotation.IntegerMemberValue
import javassist.bytecode.annotation.LongMemberValue
import javassist.bytecode.annotation.MemberValue
import javassist.bytecode.annotation.StringMemberValue
import kotlin.reflect.KClass

/**
 * Adds an annotation.
 *
 * If there is an annotation with the same type, it is removed before the new annotation is added.
 */
internal fun CtClass.addFieldAnnotation(
    fieldName: String,
    annotationQualifiedName: String,
    runtimeVisible: Boolean = true
) {
    val fieldInfo = getField(fieldName).fieldInfo
    val attributeName =
        if (runtimeVisible) AnnotationsAttribute.visibleTag else AnnotationsAttribute.invisibleTag
    val attribute: AnnotationsAttribute =
        fieldInfo.getAttribute(attributeName)?.asInstanceOfOrNull()
            ?: AnnotationsAttribute(classFile.constPool, attributeName).also { fieldInfo.addAttribute(it) }
    attribute.addAnnotation(
        Annotation(annotationQualifiedName, classFile.constPool)
    )
}

internal fun CtClass.addFieldAnnotationIfMissing(field: CtField, annotationName: String) {
    if (!field.hasAnnotation(annotationName))
        addFieldAnnotation(field.fieldInfo.name, annotationName)
}

internal fun CtClass.addFieldAnnotationMemberIfMissing(
    field: CtField,
    annotationName: String,
    memberName: String,
    memberValue: Any
) {
    field.addAnnotationMemberIfMissing(annotationName, memberName, toMemberValue(memberValue))
}

private fun CtClass.toMemberValue(v: Any): MemberValue {
    val cp = classFile.constPool
    return when (v) {
        is Boolean -> BooleanMemberValue(v, cp)
        is Long -> LongMemberValue(cp.addLongInfo(v), cp)
        is Int -> IntegerMemberValue(cp.addIntegerInfo(v), cp)
        is Short -> IntegerMemberValue(cp.addIntegerInfo(v.toInt()), cp)
        is Byte -> IntegerMemberValue(cp.addIntegerInfo(v.toInt()), cp)
        is Double -> DoubleMemberValue(cp.addDoubleInfo(v), cp)
        is Float -> FloatMemberValue(cp.addFloatInfo(v), cp)
        is String -> StringMemberValue(cp.addUtf8Info(v), cp)
        is Char -> CharMemberValue(v, cp)
        is Enum<*> ->
            EnumMemberValue(cp.addUtf8Info(Descriptor.of(v.javaClass.name)), cp.addUtf8Info(v.name), cp)
        is Class<*> -> ClassMemberValue(v.name, cp)
        is Array<*> -> ArrayMemberValue(cp).apply {
            value = v.map { toMemberValue(it!!) }.toTypedArray()
        }
        // Checking for an annotation seems to be unnecessary, since they cannot be instantiated.
        else -> throw IllegalArgumentException(
            "Type ${v.javaClass.name} is not allowed in an annotation member."
        )
    }
}

internal val CtClass.supertypes: Set<CtClass>
    get() = (interfaces.toSet() + superclass).filterNotNull().toSet()

private fun CtField.addAnnotationMemberIfMissing(
    annotationName: String,
    memberName: String,
    memberValue: MemberValue
) {
    val annotationsAttribute: AnnotationsAttribute =
        requireNotNull(fieldInfo.getAnnotationsAttribute(annotationName)) {
            "Annotation $annotationName does not exist."
        }
    val annotation: Annotation = annotationsAttribute.getAnnotation(annotationName)!!
    if (annotation.getMemberValue(memberName) == null) {
        annotation.addMemberValue(memberName, memberValue)
        annotationsAttribute.addAnnotation(annotation) // replace
    }
}

internal val CtField.annotationsStronglyTyped: List<Annotation>
    get() = listOfNotNull(
        fieldInfo.runtimeInvisibleAnnotationsAttribute,
        fieldInfo.runtimeVisibleAnnotationsAttribute
    )
        .flatMap { it.annotations.toList() }

internal val FieldInfo.runtimeVisibleAnnotationsAttribute: AnnotationsAttribute?
    get() = getAttribute(AnnotationsAttribute.visibleTag)?.asInstanceOfOrNull()

internal val FieldInfo.runtimeInvisibleAnnotationsAttribute: AnnotationsAttribute?
    get() = getAttribute(AnnotationsAttribute.invisibleTag)?.asInstanceOfOrNull()

internal fun FieldInfo.getAnnotationsAttribute(annotationName: String): AnnotationsAttribute? =
    setOf<AnnotationsAttribute?>(
        getAttribute(AnnotationsAttribute.visibleTag).asInstanceOfOrNull(),
        getAttribute(AnnotationsAttribute.invisibleTag).asInstanceOfOrNull()
    )
        .filter { it?.getAnnotation(annotationName) != null }
        .apply { assert(count() <= 1) }
        .singleOrNull()

private infix fun Any?.isInstanceOf(clazz: KClass<*>): Boolean = clazz.isInstance(this)

@Suppress("UNCHECKED_CAST")
private infix fun <T : Any> Any?.asInstanceOfOrNull(clazz: KClass<T>): T? =
    if (this isInstanceOf clazz) this as T else null

private inline fun <reified T : Any> Any?.asInstanceOfOrNull(): T? =
    this asInstanceOfOrNull T::class

package com.marcoeckstein.autoannotator.core

import com.marcoeckstein.autoannotator.api.AutoAnnotatorConfig
import com.marcoeckstein.autoannotator.api.AutoAnnotatorConfigSource
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.bytecode.AccessFlag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream

class AutoAnnotator(
    private val outputDirectoriesToAnnotate: Collection<File>,
    private val autoAnnotatorClassesRootDirectory: File
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val classPool: ClassPool = ClassPool.getDefault()

    fun annotate() {
        logger.info("Scanning output directories for class files: $outputDirectoriesToAnnotate")
        val allClassesWithOutDirs = outputDirectoriesToAnnotate.flatMap { outDir ->
            outDir.walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .map { file -> classPool.makeClass(FileInputStream(file)) to outDir }
                .toList()
        }.toMap()
        if (allClassesWithOutDirs.isEmpty())
            throw NoClassFilesException(outputDirectoriesToAnnotate)
        val allClasses = allClassesWithOutDirs.keys
        allClassesWithOutDirs.values.distinct().forEach { outDir ->
            val classesInOutDir = allClassesWithOutDirs.filter { it.value == outDir }
            logger.info("Found ${classesInOutDir.size} class files in ${outDir.absolutePath}.")
        }
        val config = getConfig(allClasses)
        checkAndRemember(config)
        PackageAnnotator(allClasses, config.classFilter, config.classOptions).annotate()
        allClassesWithOutDirs.forEach { (clazz, rootDir) -> clazz.writeFile(rootDir.absolutePath) }
    }

    private fun getConfig(allClasses: Iterable<CtClass>): AutoAnnotatorConfig {
        val configMethods = allClasses.flatMap { it.methods.toList() }.filter { m -> m.isConfigSource }
        if (configMethods.size != 1) throw MisconfigurationException(configMethods.size)
        val configMethod = configMethods.single()
        val configClass = allClasses.single { it.methods.any { m -> m.isConfigSource } }
        logger.info("Getting config from ${configClass.name}.${configMethod.name}()")
        return Thread.currentThread().contextClassLoader.loadClass(configClass.name)
            .getMethod(configMethod.name)
            .invoke(null) as AutoAnnotatorConfig
    }

    private fun checkAndRemember(config: AutoAnnotatorConfig) {
        val rootDir = autoAnnotatorClassesRootDirectory
        val metaPackage = "com.marcoeckstein.autoannotator.meta"
        val file = File(rootDir, metaPackage.replace('.', '/') + "/Meta.class")
        val expectedIndex = 7
        if (file.exists()) {
            val clazz = classPool.makeClass(FileInputStream(file))
            val configAsString = clazz.classFile.constPool.getUtf8Info(expectedIndex)
            if (configAsString != config.toString())
                throw ConfigurationChangeException(configAsString, config.toString())
        } else {
            val clazz = classPool.makeClass("$metaPackage.Meta")
            val index = clazz.classFile.constPool.addUtf8Info(config.toString())
            if (index != expectedIndex) throw AssertionError("Index is $index.")
            clazz.writeFile(rootDir.absolutePath)
        }
    }

    private companion object {
        val CtMethod.isConfigSource
            get() = hasAnnotation(AutoAnnotatorConfigSource::class.qualifiedName!!) &&
                (this.modifiers and AccessFlag.STATIC != 0)
    }
}

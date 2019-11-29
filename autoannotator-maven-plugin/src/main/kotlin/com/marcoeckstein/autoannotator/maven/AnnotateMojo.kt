package com.marcoeckstein.autoannotator.maven

import com.marcoeckstein.autoannotator.core.AutoAnnotator
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import java.io.File
import java.net.URLClassLoader

@Mojo(
    name = "annotate",
    defaultPhase = LifecyclePhase.PROCESS_CLASSES,
    requiresDependencyResolution = ResolutionScope.TEST
)
class AnnotateMojo : AbstractMojo() {

    /**
     * Annotate test classes?
     */
    @Parameter(
        property = "autoannotator.annotateTestClasses",
        required = true,
        defaultValue = "false"
    )
    private var annotateTestClasses: Boolean = false

    /**
     * An output directory of your project where the plugin may output new class files to.
     *
     * An output directory is a directory where your build process deletes class files during a clean or
     * a rebuild.
     */
    @Parameter(
        property = "autoannotator.outputDirectory",
        required = true,
        defaultValue = "\${project.build.testOutputDirectory}"
    )
    private lateinit var outputDirectory: File

    @Parameter(
        required = true,
        defaultValue = "\${project}",
        readonly = true
    )
    private lateinit var project: MavenProject

    @Suppress("UNCHECKED_CAST")
    private val classpathElements: List<String>
        get() = (if (annotateTestClasses) project.testClasspathElements else project.compileClasspathElements)
            as List<String>

    private val outputDirectoriesToAnnotate: List<File>
        get() = mutableListOf(project.build.outputDirectory)
            .also { if (annotateTestClasses) it + project.build.testOutputDirectory }
            .map(::File)

    override fun execute() {
        try {
            log.info("parameter annotateTestClasses: $annotateTestClasses")
            log.info("parameter outputDirectory: $outputDirectory")
            log.info("output directories to annotate: $outputDirectoriesToAnnotate")
            log.info("classpath elements: $classpathElements")
            setContextClassLoader()
            AutoAnnotator(outputDirectoriesToAnnotate, outputDirectory).annotate()
        } catch (e: Exception) {
            log.error(e)
            throw e
        }
    }

    private fun setContextClassLoader() {
        Thread.currentThread().contextClassLoader = URLClassLoader(
            classpathElements.map { File(it).toURI().toURL() }.toTypedArray(),
            Thread.currentThread().contextClassLoader
        )
    }
}

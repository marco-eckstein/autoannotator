@file:JvmName("Main")

package com.marcoeckstein.autoannotator.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.marcoeckstein.autoannotator.core.AutoAnnotator
import java.io.File

fun main(args: Array<String>) {
    MainCommand.main(args)
}

private object MainCommand : CliktCommand() {

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    val outputDirectoriesToAnnotate: String
        by option(
            help = "The output directories of your project with the classes that should be annotated. " +
                "Separated by the system's path separator (typically ';' on Windows and ':' on Linux/Unix.)"
        )
            .default(
                "target/classes",
                defaultForHelp = "the subdirectory 'target/classes' inside the current directory"
            )
    val autoAnnotatorOutputDirectory: String
        by option(
            help = "An output directory of your project where the plugin may output new class files to. " +
                "An output directory is a directory where your build process deletes class files during " +
                "a clean or a rebuild."
        )
            .default(
                "target/test-classes",
                defaultForHelp = "the subdirectory 'target/test-classes' inside the current directory"
            )

    override fun run() {
        setLoggerProperties()
        AutoAnnotator(
            outputDirectoriesToAnnotate.split(File.pathSeparator).map(::File),
            File(autoAnnotatorOutputDirectory)
        ).annotate()
    }
}

private fun setLoggerProperties() {
    mapOf(
        "org.slf4j.simpleLogger.logFile" to "System.out",
        "org.slf4j.simpleLogger.showDateTime" to "true",
        "org.slf4j.simpleLogger.dateTimeFormat" to "yyyy-MM-dd HH:mm:ss.SSS"
    ).forEach {
        if (System.getProperty(it.key) == null)
            System.setProperty(it.key, it.value)
    }
}

package me.tylerbwong.gradle.metalava

import com.android.build.gradle.LibraryExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.findPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File

internal sealed class Module {

    open val bootClasspath: Collection<File> = emptyList()
    abstract val compileClasspath: Collection<File>

    class Android(private val extension: LibraryExtension) : Module() {
        override val bootClasspath: Collection<File>
            get() = extension.bootClasspath
        override val compileClasspath: Collection<File>
            get() = extension.libraryVariants.find {
                it.name.contains("debug", ignoreCase = true)
            }?.getCompileClasspath(null)?.filter { it.exists() }?.files ?: emptyList()
    }

    class Multiplatform(private val extension: KotlinMultiplatformExtension) : Module() {
        override val compileClasspath: Collection<File>
            get() = extension.targets
                .flatMap { it.compilations }
                .filter { it.defaultSourceSetName.contains("main", ignoreCase = true) }
                .flatMap { it.compileDependencyFiles }
    }

    class Java(private val convention: JavaPluginConvention) : Module() {
        override val compileClasspath: Collection<File>
            get() = convention.sourceSets
                .filter { it.name.contains("main", ignoreCase = true) }
                .map { it.compileClasspath.files }
                .flatten()
    }

    companion object {
        internal val Project.module: Module
            get() {
                val libraryExtension = extensions.findByType<LibraryExtension>()
                val multiplatformExtension = extensions.findByType<KotlinMultiplatformExtension>()
                val javaPluginConvention = convention.findPlugin<JavaPluginConvention>()
                return when {
                    libraryExtension != null -> Android(libraryExtension)
                    multiplatformExtension != null -> Multiplatform(multiplatformExtension)
                    javaPluginConvention != null -> Java(javaPluginConvention)
                    else -> throw GradleException("This module is currently not supported by the Metalava plugin")
                }
            }
    }
}

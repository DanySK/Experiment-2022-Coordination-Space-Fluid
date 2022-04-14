import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.awt.GraphicsEnvironment
import java.io.ByteArrayOutputStream

plugins {
    application
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.multiJvmTesting)
    alias(libs.plugins.taskTree)
}

repositories {
    mavenCentral()
}
/*
 * Only required if you plan to use Protelis, remove otherwise
 */
sourceSets {
    main {
        resources {
            srcDir("src/main/protelis")
        }
    }
}

val usesJvm: Int = File(File(projectDir, "util"), "Dockerfile")
    .readText()
    .let {
        Regex("FROM\\s+openjdk:(\\d+)\\s+$").find(it)?.groups?.get(1)?.value
            ?: throw IllegalStateException("Cannot read information on the JVM to use.")
    }
    .toInt()

multiJvm {
    jvmVersionForCompilation.set(usesJvm)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.bundles.alchemist.protelis)
    if (!GraphicsEnvironment.isHeadless()) {
        implementation("it.unibo.alchemist:alchemist-swingui:${libs.versions.alchemist.get()}")
    }
}

// Heap size estimation for batches
val isCI = System.getenv("CI") == "true"
val maxHeap: Long? by project
val heap: Long = maxHeap ?: if (System.getProperty("os.name").toLowerCase().contains("linux")) {
    ByteArrayOutputStream().use { output ->
        exec {
            executable = "sh"
            args = listOf("-c", "cat /proc/meminfo | grep MemAvailable | grep -o '[0-9]*'")
            standardOutput = output
        }
        output.toString().trim().toLong() / 1024
    }.also { println("Detected ${it}MB RAM available.") }
} else {
    // Guess 16GB RAM of which 2 used by the OS
    14 * 1024L
}
val taskSizeFromProject: Int? by project
val taskSize = (taskSizeFromProject ?: 8000)
    .takeUnless { isCI } ?: 512 // CI Servers run a simplified version of the simulation
val threadCount = maxOf(1, minOf(Runtime.getRuntime().availableProcessors(), heap.toInt() / taskSize))

val alchemistGroup = "Run Alchemist"
/*
 * This task is used to run all experiments in sequence
 */
val runAllGraphic by tasks.register<DefaultTask>("runAllGraphic") {
    group = alchemistGroup
    description = "Launches all simulations with the graphic subsystem enabled"
}
val runAllBatch by tasks.register<DefaultTask>("runAllBatch") {
    group = alchemistGroup
    description = "Launches all experiments"
}

fun Boolean.whenTrue(body: Boolean.() -> Unit) = this.also { if (this) { body() } }
val jvmCrashes: List<File> = projectDir.listFiles()
    ?.filter { file -> file.name.matches(Regex("^hs_err_pid\\d+.log$")) }
    ?: emptyList()
println("Previous JVM crashes have been detected. Scanning them for known bugs")
val disableOpenGL = jvmCrashes.any { file ->
    file.bufferedReader().lineSequence().any { line ->
        line.contains("libnvidia-glcore").whenTrue {
            val url = "https://github.com/adoptium/adoptium-support/issues/489"
            println(
                """
                WARNING: a well-known bug with Linux and nVidia drivers occured in the past.
                The full log is in ${file.name}.
                Information on the bug and its solution is available at: $url
                As a workaround, OpenGL acceleration will be disabled until the aforementioned crash report is deleted.
                This could negatively impact performance.
                """.trimIndent()
            )
        }
    }
}

/*
 * Scan the folder with the simulation files, and create a task for each one of them.
 */
File(rootProject.rootDir.path + "/src/main/yaml").listFiles()
    ?.filter { it.extension == "yml" }
    ?.sortedBy { it.nameWithoutExtension }
    ?.forEach {
        fun basetask(name: String, additionalConfiguration: JavaExec.() -> Unit = {}) = tasks.register<JavaExec>(name) {
            group = alchemistGroup
            description = "Launches graphic simulation ${it.nameWithoutExtension}"
            mainClass.set("it.unibo.alchemist.Alchemist")
            classpath = sourceSets["main"].runtimeClasspath
            args("-y", it.absolutePath)
            if (isCI) {
                args("-hl", "-t", "2")
            } else {
                args("-g", "effects/${it.nameWithoutExtension}.json")
            }
            javaLauncher.set(
                javaToolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(usesJvm))
                }
            )
            if (disableOpenGL) {
                jvmArgs("-Dsun.java2d.opengl=false")
            }
            this.additionalConfiguration()
        }
        val capitalizedName = it.nameWithoutExtension.capitalize()
        val graphic by basetask("run${capitalizedName}Graphic") {
            if (isCI) maxHeapSize = "4500m"
        }
        runAllGraphic.dependsOn(graphic)
        val batch by basetask("run${capitalizedName}Batch") {
            description = "Launches batch experiments for $capitalizedName"
            maxHeapSize = "${(heap * 0.85).toInt()}m"
            File("data").mkdirs()
            args(
                "-e", "data/${it.nameWithoutExtension}",
                "-b",
                "-var", "seed", "deployment",
                "-p", threadCount,
                "-i", 1
            )
        }
        runAllBatch.dependsOn(batch)
    }

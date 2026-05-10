import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask

plugins {
    id("java")
    id("idea")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.grammarkit")
}

val genPath: String by project
val junitVersion: String by project
val platformVersion: String by project
val platformType: String by project
val platformPlugins: String by project
val flexPath: String by project
val genLexerPath: String by project
val bnfPath: String by project
val genParserClassPath: String by project
val genPsiPath: String by project
val kotlinVersion: String by project
val JSONataVersion: String by project
val pluginSinceBuild: String by project
val pluginUntilBuild: String by project
val plantumlVersion: String by project
val elkVersion: String by project
val jflexVersion: String by project
val grammarKitVersion: String by project


repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
}

dependencies {
    intellijPlatform {
        intellijIdea("2026.1.1")
        val pluginsList: List<String> = platformPlugins.split(',').map { it.trim() }.filter { it.isNotEmpty() };
        bundledPlugins( pluginsList )
        jetbrainsRuntime()
    }
    implementation("org.jetbrains.intellij.deps.jflex:jflex:$jflexVersion")
    // implementation("org.jetbrains.grammarkit:${grammarKitVersion}")

    implementation("com.ibm.jsonata4java:JSONata4Java:$JSONataVersion")
    implementation("net.sourceforge.plantuml:plantuml:$plantumlVersion")
    implementation("org.eclipse.elk:org.eclipse.elk.alg.layered:$elkVersion")
    implementation("org.eclipse.elk:org.eclipse.elk.core:$elkVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    // implementation("org.javassist:javassist:3.29.2-GA")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild.set(pluginSinceBuild)
            untilBuild.set(pluginUntilBuild)
        }
        changeNotes.set(provider {
            file("src/main/resources/html/change-notes.html").readText()
        })
    }

}

sourceSets {
    main {
        java { srcDir(genPath) }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        options.encoding = "UTF-8"
        dependsOn("genJSONataParser")
    }

    withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        dependsOn("genJSONataParser")
    }

    // Твои таски генерации
    register<GenerateLexerTask>("genJSONataLexer") {
        sourceFile.set(project.file(flexPath))
        targetOutputDir.set(project.file(genLexerPath))
        purgeOldFiles.set(true)
    }

    register<GenerateParserTask>("genJSONataParser") {
        dependsOn("genJSONataLexer")
        sourceFile.set(project.file(bnfPath))
        targetRootOutputDir.set(project.file(genPath))
        pathToParser.set(genParserClassPath)
        pathToPsiRoot.set(genPsiPath)
        purgeOldFiles.set(true)

        //val currentClasspath = this.classpath
        //val ideaLibs = project.configurations.getByName("intellijPlatformDependency")
        //this.setClasspath(ideaLibs + currentClasspath)

        val ideaLibs = configurations.intellijPlatformClasspath.get()
        this.setClasspath(ideaLibs + this.classpath)
    }
}

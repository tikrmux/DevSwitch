import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import java.net.URL

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.intellij.platform)
    alias(libs.plugins.compose.plugin)
    alias(libs.plugins.compose.jetbrains)
    id("maven-publish")
}

group = "com.tinkrmux.devswitch"
version = "1.0.3"

repositories {
    mavenCentral()
    mavenLocal()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://packages.jetbrains.team/maven/p/kpm/public/")  // Jewel repository

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation(libs.ddmlib)
    
    // Jewel IDE LaF Bridge for platform 243
    // Exclude kotlinx.coroutines - provided by the IDE
    implementation("org.jetbrains.jewel:jewel-ide-laf-bridge-243:0.27.0") {
        exclude(group = "org.jetbrains.kotlinx")
    }
    
    // Coroutines - compile only (IDE provides at runtime)
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    
    // Skiko natives - required for Compose rendering
    implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:0.8.18")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-x64:0.8.18")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:0.8.18")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.8.18")

    intellijPlatform {
        intellijIdeaCommunity("2024.3.2")  // Platform 243
        
        pluginVerifier()
        zipSigner()
    }
}

// Exclude kotlinx.coroutines from runtime - IDE provides it
configurations.runtimeClasspath {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"  // Platform 243+ required for bundled Jewel
            untilBuild = provider { null } // No upper bound
        }
    }

    pluginVerification {
        ides {
            // IntelliJ IDEA Community (243+)
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.3.2")
            
            // IntelliJ IDEA Ultimate (243+)
            ide(IntelliJPlatformType.IntellijIdeaUltimate, "2024.3.2")
            
            // Android Studio Meerkat (243+)
            ide(IntelliJPlatformType.AndroidStudio, "2024.3.1.13")
        }
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

kotlin {
    jvmToolchain(17)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}

tasks.withType<DokkaTask>().configureEach {
    moduleName.set(project.name)
    moduleVersion.set(project.version.toString())
    outputDirectory.set(layout.buildDirectory.dir("dokka/$name"))
    failOnWarning.set(false)
    suppressObviousFunctions.set(true)
    suppressInheritedMembers.set(false)
    offlineMode.set(false)

    dokkaSourceSets {
        configureEach {
            suppress.set(false)
            displayName.set(name)
            documentedVisibilities.set(setOf(DokkaConfiguration.Visibility.PUBLIC))
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(true)
            jdkVersion.set(17)
            languageVersion.set("17")
            apiVersion.set("17")
            noStdlibLink.set(false)
            noJdkLink.set(false)
            noAndroidSdkLink.set(false)
            includes.from(project.files(), "packages.md", "extra.md")
            platform.set(org.jetbrains.dokka.Platform.DEFAULT)
            sourceRoots.from(file("src"))
            classpath.from(project.files(), file("libs/dependency.jar"))
            samples.from(project.files(), "samples/Basic.kt", "samples/Advanced.kt")

            sourceLink {
                localDirectory.set(projectDir.resolve("src"))
                remoteUrl.set(URL("https://github.com/tinkrmux/DevSwitch/tree/master/src"))
                remoteLineSuffix.set("#L")
            }

            externalDocumentationLink {
                url.set(URL("https://kotlinlang.org/api/core/kotlin-stdlib/"))
                packageListUrl.set(
                    rootProject.projectDir.resolve("stdlib.package.list").toURI().toURL()
                )
            }

            perPackageOption {
                matchingRegex.set(".*api.*")
                suppress.set(false)
                skipDeprecated.set(false)
                reportUndocumented.set(false)
                documentedVisibilities.set(
                    setOf(
                        DokkaConfiguration.Visibility.PUBLIC,
                        DokkaConfiguration.Visibility.PRIVATE,
                        DokkaConfiguration.Visibility.PROTECTED,
                        DokkaConfiguration.Visibility.INTERNAL,
                        DokkaConfiguration.Visibility.PACKAGE
                    )
                )
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = group.toString()
            artifactId = "devswitch"
            version = version.toString()

            pom {
                name.set("DevSwitch")
                description.set("IntelliJ plugin to quickly change Android developer settings")
            }
        }
    }

    repositories {
        maven {
            name = "local"
            url = uri(rootProject.layout.buildDirectory.dir("repo"))
        }
        mavenLocal()
    }
}

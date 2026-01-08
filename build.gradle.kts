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
version = "1.0.2"

repositories {
    mavenCentral()
    mavenLocal()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation(libs.ddmlib)
    
    // Use common compose desktop without OS-specific natives
    // The IDE runtime provides the necessary Skia libraries
    implementation(compose.desktop.common)
    implementation(compose.material)
    implementation(compose.ui)
    implementation(compose.foundation)
    implementation(compose.runtime)

    intellijPlatform {
        intellijIdeaCommunity("2024.1.7")
        
        pluginVerifier()
        zipSigner()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "241"
            untilBuild = provider { null } // No upper bound
        }
    }

    pluginVerification {
        ides {
            // IntelliJ IDEA Community
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.3.2")
            
            // IntelliJ IDEA Ultimate
            ide(IntelliJPlatformType.IntellijIdeaUltimate, "2024.3.2")
            
            // Android Studio
            ide(IntelliJPlatformType.AndroidStudio, "2024.2.1.12")  // Ladybug
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

import org.apache.commons.lang3.SystemUtils
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.Properties

plugins {
    idea
    java
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val baseGroup: String by project
val mcVersion: String by project
val version: String by project
val mixinGroup = "$baseGroup.mixin"
val modid: String by project

// Load local credentials (api.url, api.key)
val localProps = Properties().also { props ->
    val f = file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
}
val apiUrl: String = localProps.getProperty("api.url", "")
val apiKey: String = localProps.getProperty("api.key", "")

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

loom {
    log4jConfigs.from(file("log4j2.xml"))
    launchConfigs {
        "client" {
            property("mixin.debug", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
    }
    runConfigs {
        "client" {
            if (SystemUtils.IS_OS_MAC_OSX) {
                vmArgs.remove("-XstartOnFirstThread")
            }
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        mixinConfig("mixins.$modid.json")
    }
    mixin {
        defaultRefmapName.set("mixins.$modid.refmap.json")
    }
}

sourceSets.main {
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")

    compileOnly(files("F:/CoffeeClient/replaymod/versions/1.8.9/build/libs/ReplayMod-v1_8-2.6.14.jar"))

    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(org.gradle.jvm.tasks.Jar::class) {
    archiveBaseName.set(modid)
    manifest.attributes.run {
        this["MixinConfigs"] = "mixins.$modid.json"
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mcversion", mcVersion)
    inputs.property("modid", modid)
    inputs.property("basePackage", baseGroup)
    inputs.property("api_url", apiUrl)
    inputs.property("api_key", apiKey)

    filesMatching("mixins.$modid.json") {
        expand(mapOf("basePackage" to baseGroup, "modid" to modid))
    }

    filesMatching("mcmod.info") {
        expand(mapOf("version" to project.version))
    }

    filesMatching("api.properties") {
        expand(mapOf("api_url" to apiUrl, "api_key" to apiKey))
    }
}

tasks.jar {
    archiveClassifier.set("without-deps")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
}

tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    archiveClassifier.set("non-obfuscated-with-deps")
    configurations = listOf(shadowImpl)
    doLast {
        configurations.forEach {
            println("Copying dependencies into mod: ${it.files}")
        }
    }

    relocate("$baseGroup.mixin", "com.replaymod.replay.mixin.$modid")

    fun relocate(name: String) = relocate(name, "$baseGroup.deps.$name")
}

val fixMixinConfigs by tasks.registering {
    dependsOn(tasks.shadowJar)
    val shadowOut = tasks.shadowJar.get().archiveFile
    inputs.file(shadowOut)

    doLast {
        val jar = shadowOut.get().asFile
        val relocatedPkg = "com.replaymod.replay.mixin.$modid"
        val configName = "mixins.$modid.json"

        val uri = URI.create("jar:" + jar.toURI())
        val fs = FileSystems.newFileSystem(uri, emptyMap<String, Any>())
        fs.use { zipFs ->
            val entry = zipFs.getPath(configName)
            if (Files.exists(entry)) {
                val bytes = Files.readAllBytes(entry)
                val text = String(bytes, Charsets.UTF_8)
                val updated = text.replace(
                    "\"package\": \"$baseGroup.mixin\"",
                    "\"package\": \"$relocatedPkg\""
                )
                if (updated != text) {
                    Files.write(entry, updated.toByteArray(Charsets.UTF_8))
                    println("fixMixinConfigs: updated package -> $relocatedPkg")
                } else {
                    println("fixMixinConfigs: no match found in $configName (already correct?)")
                }
            } else {
                println("fixMixinConfigs: $configName not found in JAR")
            }
        }
    }
}

tasks.named("remapJar") { enabled = false }

val renameToCoffeeJar by tasks.registering(Copy::class) {
    dependsOn(fixMixinConfigs)
    from(tasks.shadowJar.get().archiveFile)
    into(layout.buildDirectory.dir("libs"))
    rename { "${modid}-${version}.coffeeclient.jar" }
}

tasks.assemble.get().dependsOn(renameToCoffeeJar)

tasks.register<Copy>("idep") {
    dependsOn(renameToCoffeeJar)
    from(layout.buildDirectory.dir("libs")) {
        include("*.coffeeclient.jar")
    }
    into(file("F:\\CoffeeClient\\coffeemods"))
    doFirst {
        file("F:\\CoffeeClient\\coffeemods").mkdirs()
    }
    doLast {
        println("Deployed ${modid}-${version}.coffeeclient.jar -> F:\\CoffeeClient\\coffeemods\\")
    }
}

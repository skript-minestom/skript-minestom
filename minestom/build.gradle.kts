plugins {
	id("com.gradleup.shadow") version "9.3.0"
	application
	java
	`maven-publish`
}

group = "com.github.hapily04"
version = "1.0.0-alpha.15"

repositories {
    mavenCentral()
	maven("https://maven.conceptmc.com/releases") // luckperms
	maven("https://repo.hypera.dev/snapshots/") // spark (& was luckperms)
	maven("https://repo.lucko.me/")
	maven("https://maven.hapily.me/releases")
	maven("https://maven.hapily.me/snapshots")
	maven("https://oss.sonatype.org/content/repositories/snapshots/")
	maven("https://repo.kenzie.mx/releases")
	maven("https://jitpack.io")
}

dependencies {
	implementation("net.minestom:minestom-sm:2026.08.08-26.2")
	implementation("net.kyori:adventure-text-minimessage:5.2.0")
	implementation("dev.hollowcube:polar:1.16.0")
	implementation("it.unimi.dsi:fastutil:8.5.18") // fix polar error
	implementation("com.conceptmc:luckperms-minestom:5.5-SNAPSHOT")
	implementation("com.h2database:h2:2.2.224") // fix luckperms cause it's lowkey being stupid
	implementation("dev.lu15:spark-minestom:1.10-SNAPSHOT")
	implementation("org.spongepowered:configurate-hocon:3.7.2") // configuration using hocon
	implementation("org.jline:jline:3.28.0") // part of terminal implementation
	compileOnly("org.jetbrains:annotations:26.0.2")
	implementation("org.eclipse.jdt:org.eclipse.jdt.annotation:2.2.700")
	implementation("ch.qos.logback:logback-classic:1.5.32")
	implementation("com.google.code.gson:gson:2.11.0")
	implementation("mx.kenzie:mirror:5.0.3")
	implementation(project(":common"))
	implementation("org.apache.commons:commons-lang3:3.20.0") // fix skript dependency
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(25)
}

val generateSources by tasks.registering(Copy::class) {
	from("src/main/java-templates")
	into(layout.buildDirectory.dir("generated/sources/java"))

	inputs.property("version", project.version) // fix: version wasn't rewriting unless file is gone

	expand("version" to project.version)
	rename { it.removeSuffix(".peb") }
}

sourceSets.main {
	java.srcDir(layout.buildDirectory.dir("generated/sources/java"))
}

tasks.compileJava {
	dependsOn(generateSources)
}

application {
	mainClass = "com.github.hapily04.skriptminestom.SkriptMinestom"
}

publishing {
	publications {
		create<MavenPublication>("minestomPublish") {
			groupId = project.group.toString()
			artifactId = "skript-minestom"
			version = project.version.toString()

			artifact(tasks.shadowJar) {
				classifier = "" // no -all
			}
		}
	}

	repositories {
		maven {
			url = uri("https://maven.hapily.me/snapshots")
			credentials {
				username = providers.gradleProperty("repoHapilyUsername").orNull
				password = providers.gradleProperty("repoHapilyPassword").orNull
			}
		}
	}
}
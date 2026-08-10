plugins {
	`maven-publish`
}

group = "com.github.hapily04.skriptminestom"
version = "1.0.0-alpha.20"

repositories {
	mavenCentral()
}

subprojects {
	apply(plugin = "java")
	apply(plugin = "maven-publish")

	publishing {
		publications {
			create<MavenPublication>("maven") {
				groupId = "com.github.hapily04.skriptminestom"
				artifactId = project.name
				version = rootProject.version.toString()

				from(components["java"])
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
}

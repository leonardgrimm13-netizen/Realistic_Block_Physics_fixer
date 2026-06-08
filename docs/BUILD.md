# Build

## Requirements

- JDK 17 available for compilation.
- Gradle 8.x. The project provides `./gradlew` configured for Gradle 8.8.
- Network access to Maven Central, Forge Maven, Mojang metadata, and Gradle distributions for a first clean checkout.

ForgeGradle 6.x is not compatible with Gradle 9. If a global Gradle 9 is installed, use this repository's `./gradlew` entrypoint instead.

## Command

```bash
./gradlew clean build
```

Output:

```text
build/libs/Realistic_Block_Physics_fixer-<version>.jar
```

## Repository notes

`settings.gradle` intentionally does not enforce `RepositoriesMode.FAIL_ON_PROJECT_REPOS`, because ForgeGradle injects repositories during setup. `build.gradle` declares Forge Maven and Maven Central explicitly.

## Environment troubleshooting

- If Gradle cannot find Java 17, set `JAVA_HOME` to a JDK 17 installation.
- If a proxy blocks `services.gradle.org`, Mojang, or Forge Maven, the first build cannot download Gradle/Forge/Minecraft artifacts.
- If Gradle 9 is used accidentally, switch to `./gradlew` or install/use Gradle 8.x.

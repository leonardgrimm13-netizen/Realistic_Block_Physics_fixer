# Build-Anleitung

## Wichtig

ForgeGradle 6.x unterstützt Gradle 9 noch nicht. Wenn dein System `gradle 9.x` nutzt, bricht der Build mit ungefähr dieser Meldung ab:

```text
Found Gradle version Gradle 9.x. Versions Gradle 9.0 and newer are not supported yet.
```

Dieses Projekt nutzt deshalb bewusst Gradle 8.8.

## Empfohlener Build auf Linux mit SDKMAN

```bash
cd Realistic_Block_Physics_fixer
./build_gradle8.sh
```

Mit ausführlichem Log:

```bash
./build_gradle8_info.sh
```

Die fertige Mod-JAR liegt danach hier:

```text
build/libs/Realistic_Block_Physics_fixer-1.0.0.jar
```

## Alternative

Wenn du Gradle 8.8 bereits global aktivieren willst:

```bash
sdk install gradle 8.8
sdk use gradle 8.8
gradle --no-daemon clean build
```

Danach kannst du wieder zu deiner normalen Gradle-Version zurück:

```bash
sdk use gradle 9.5.1
```

## Java-Version

Minecraft Forge 1.20.1-Modding nutzt Java 17 als Target. Es ist aber okay, den Build mit JDK 21 laufen zu lassen, solange Gradle 8.8 verwendet wird. Das Projekt setzt im `build.gradle` das Java-Toolchain-Target auf 17.

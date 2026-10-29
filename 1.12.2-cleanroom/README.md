# UniGUI Cleanroom 1.12.2

This is an isolated CleanroomGradle build because CleanroomGradle requires a newer Gradle and JDK than the main Loom/Stonecutter build.

## Requirements

- JDK 25 to run this build.
- JDK 21 to build the shared `core` project with the root Gradle wrapper.
- Foojay toolchain resolution downloads JDK 8 for the Cleanroom reobfuscation task.

## Build and run

From the repository root, build the shared core first:

```powershell
./gradlew.bat :core:jar
```

Then switch the Gradle JVM to JDK 25 and run this module's wrapper from this directory:

```powershell
./gradlew.bat build
./gradlew.bat runClient
```

The produced mod jar includes the core jar, including its bundled fastutil dependency.

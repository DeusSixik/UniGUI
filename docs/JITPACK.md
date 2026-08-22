# JitPack build

This project is configured for JitPack through the root `jitpack.yml`.

## Build command

JitPack starts the build on JDK 21:

```yaml
jdk:
  - openjdk21
```

The project itself uses Gradle toolchains because different Minecraft profiles require different Java versions:

| Minecraft | Java toolchain |
| --- | --- |
| `1.20.1` | Java 17 |
| `1.21.1` | Java 21 |

`settings.gradle` applies `org.gradle.toolchains.foojay-resolver-convention`, so Gradle can download a missing Java 17 toolchain on JitPack even though the main build JVM is Java 21.

JitPack publishes all Gradle Maven publications to the local Maven repository with:

```bash
./gradlew -Dorg.gradle.java.home=$JAVA_HOME publishToMavenLocal -x test --no-daemon
```

The Gradle wrapper executable bit is restored with:

```bash
chmod +x gradlew
```

## Published modules

Artifact ids are derived from `archives_name`, module name and Minecraft version:

```groovy
artifactId = "${archives_name}-$project.name-${minecraft_version}"
```

For version `1.1.0`, the expected artifacts are:

| Minecraft | Loader/module | Artifact id |
| --- | --- | --- |
| `1.20.1` | common | `unigui-common-1.20.1` |
| `1.20.1` | Fabric | `unigui-fabric-1.20.1` |
| `1.20.1` | Forge | `unigui-forge-1.20.1` |
| `1.21.1` | common | `unigui-common-1.21.1` |
| `1.21.1` | Fabric | `unigui-fabric-1.21.1` |
| `1.21.1` | Forge | `unigui-forge-1.21.1` |
| `1.21.1` | NeoForge | `unigui-neoforge-1.21.1` |

## Consumer example

Replace `<USER>`, `<REPO>` and `<TAG_OR_COMMIT>` with the GitHub repository owner, repository name and a JitPack-supported version.

```groovy
repositories {
    maven { url = uri("https://jitpack.io") }
    // Required when consuming Fabric artifacts or any dependency that resolves Fabric Loader.
    maven { url = uri("https://maven.fabricmc.net/") }
}
```

Minecraft `1.20.1`:

```groovy
dependencies {
    implementation "com.github.<USER>.<REPO>:unigui-common-1.20.1:<TAG_OR_COMMIT>"
    modImplementation "com.github.<USER>.<REPO>:unigui-fabric-1.20.1:<TAG_OR_COMMIT>"
    // or
    modImplementation "com.github.<USER>.<REPO>:unigui-forge-1.20.1:<TAG_OR_COMMIT>"
}
```

Minecraft `1.21.1`:

```groovy
dependencies {
    implementation "com.github.<USER>.<REPO>:unigui-common-1.21.1:<TAG_OR_COMMIT>"
    modImplementation "com.github.<USER>.<REPO>:unigui-fabric-1.21.1:<TAG_OR_COMMIT>"
    // or
    modImplementation "com.github.<USER>.<REPO>:unigui-forge-1.21.1:<TAG_OR_COMMIT>"
    // or
    modImplementation "com.github.<USER>.<REPO>:unigui-neoforge-1.21.1:<TAG_OR_COMMIT>"
}
```

For mod loader projects, prefer the platform module matching the target loader and use the loader-specific dependency configuration (`modImplementation`, `modApi`, etc.) used by that project. If you only consume `unigui-common-*`, the common artifact should not bring Fabric Loader transitively; Fabric Maven is still needed for Fabric platform artifacts.

## Versioning rule

Minecraft version is part of the artifact id. Library/JitPack version is the dependency version:

```text
com.github.<USER>.<REPO> : unigui-fabric-1.21.1 : 1.1.0
        group id          artifact + MC version   Git tag / JitPack version
```

# JitPack build

This project is configured for JitPack through the root jitpack.yml.

## Build command

JitPack uses Java 17 and publishes all Gradle Maven publications to the local Maven repository:

    ./gradlew clean publishToMavenLocal -x test --no-daemon

The Gradle wrapper executable bit is restored in JitPack with:

    chmod +x gradlew

## Published modules

The Gradle publication artifact ids are derived from archives_name and the module name:

- unigui-common
- unigui-fabric
- unigui-forge

## Consumer example

Replace <USER>, <REPO> and <TAG_OR_COMMIT> with the GitHub repository owner, repository name and a JitPack-supported version.

    repositories {
        maven { url = uri("https://jitpack.io") }
    }

    dependencies {
        implementation "com.github.<USER>.<REPO>:unigui-common:<TAG_OR_COMMIT>"
        modImplementation "com.github.<USER>.<REPO>:unigui-fabric:<TAG_OR_COMMIT>"
        modImplementation "com.github.<USER>.<REPO>:unigui-forge:<TAG_OR_COMMIT>"
    }

For mod loader projects, prefer the platform module matching the target loader and use the loader-specific dependency configuration (modImplementation, modApi, etc.) used by that project.
#!/bin/sh
# Helper for JitPack builds (called from jitpack.yml).
#
# Some JitPack build images deliver a truncated or missing
# gradle/wrapper/gradle-wrapper.jar in the checkout, which fails with:
#   Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain
# This script verifies the wrapper jar and falls back to a plain Gradle
# distribution download (version pinned in gradle-wrapper.properties).

GRADLE_VERSION=8.12.1
GRADLE_CMD=./gradlew

# The JitPack "latest" image occasionally fails to provision the JDK via
# SDKMAN ("Download has failed, aborting!"), leaving JAVA_HOME pointing at a
# missing directory. Ensure a working JAVA_HOME ourselves in that case.
ensure_java() {
    if [ -x "$JAVA_HOME/bin/java" ]; then
        echo "JAVA_HOME OK: $JAVA_HOME"
        return 0
    fi
    echo "WARNING: JAVA_HOME is invalid ($JAVA_HOME) - installing Temurin 21 ourselves"
    if command -v java >/dev/null 2>&1 && java -version 2>&1 | grep -q 'version "21'; then
        JAVA_HOME=$(dirname "$(dirname "$(command -v java)")")
        export JAVA_HOME
        echo "Using system JDK 21 at $JAVA_HOME"
        return 0
    fi
    curl -fsSL -o /tmp/temurin21.tar.gz \
        "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
    mkdir -p /tmp/jdk21
    tar -xzf /tmp/temurin21.tar.gz -C /tmp/jdk21 --strip-components=1
    JAVA_HOME=/tmp/jdk21
    export JAVA_HOME
    "$JAVA_HOME/bin/java" -version
    echo "Installed fallback JDK 21 at $JAVA_HOME"
}

wrapper_ok() {
    if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
        echo "CHECK: gradle/wrapper/gradle-wrapper.jar is MISSING from the checkout"
        return 1
    fi
    if ! "$JAVA_HOME/bin/jar" -tf gradle/wrapper/gradle-wrapper.jar 2>/dev/null | grep -q GradleWrapperMain; then
        echo "CHECK: gradle/wrapper/gradle-wrapper.jar is CORRUPT (no GradleWrapperMain inside)"
        return 1
    fi
    echo "CHECK: gradle-wrapper.jar OK"
    return 0
}

case "$1" in
    verify)
        ensure_java
        if wrapper_ok; then
            exit 0
        fi
        echo "WARNING: will use the direct Gradle fallback instead of the wrapper"
        exit 0
        ;;
    build)
        ensure_java
        if ! wrapper_ok; then
            echo "FALLBACK: downloading Gradle $GRADLE_VERSION directly"
            curl -fsSL -o /tmp/gradle.zip "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
            unzip -qo /tmp/gradle.zip -d /tmp
            GRADLE_CMD="sh /tmp/gradle-$GRADLE_VERSION/bin/gradle"
        fi
        # Memory limits: the cold JitPack build runs many tasks in parallel and
        # the Architectury Transformer needs more than the 2G heap from
        # gradle.properties (:1.20.1:common:transformProductionForge died with
        # "Java heap space"). Cap workers and raise the heap for the Gradle
        # daemon (org.gradle.jvmargs) and every forked JVM (_JAVA_OPTIONS).
        echo "JitPack build memory tuning: heap=3G, max-workers=2"
        export _JAVA_OPTIONS="-Xmx3G"
        $GRADLE_CMD -Dorg.gradle.java.home="$JAVA_HOME" \
                    -Dorg.gradle.jvmargs=-Xmx3G \
                    --max-workers=2 \
                    publishToMavenLocal -x test --no-daemon
        ;;
    *)
        echo "usage: $0 verify|build" >&2
        exit 2
        ;;
esac

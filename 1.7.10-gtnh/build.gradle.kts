
plugins {
    id("com.gtnewhorizons.gtnhconvention")
    id("me.champeau.jmh") version "0.7.2"
}

sourceSets.main {
    java.srcDir("../.minecraft-legacy-backend-shared/src/main/java")
    java.srcDir("../.minecraft-scene-gl/src/main/java")
}

dependencies {
    add("jmh", "org.openjdk.jmh:jmh-core:1.37")
    add("jmhAnnotationProcessor", "org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

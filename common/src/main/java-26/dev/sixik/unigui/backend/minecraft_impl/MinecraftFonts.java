package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.resource.UniResource;
import dev.sixik.unigui.api.resource.UniResources;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory and default-face helpers for Minecraft resource-pack fonts.
 */
public final class MinecraftFonts {
    private static final Map<Identifier, MinecraftFontFace> FACES = new ConcurrentHashMap<>();

    private MinecraftFonts() {
    }

    public static MinecraftFontFace face(Identifier location) {
        return FACES.computeIfAbsent(location, MinecraftFontFace::new);
    }

    public static MinecraftFontFace face(String location) {
        Identifier parsed = Identifier.tryParse(location);
        if (parsed == null) throw new IllegalArgumentException("Invalid Minecraft font location: " + location);
        return face(parsed);
    }

    public static MinecraftFontFace face(UniResource resource) {
        return FACES.computeIfAbsent(MinecraftResourceCompat.toMinecraft(resource), MinecraftFontFace::new);
    }

    public static MinecraftFontFace faceId(String location) {
        return face(UniResources.parse(location));
    }

    public static MinecraftFontFace defaultFace() {
        return face(Identifier.tryBuild("minecraft", "default"));
    }

    public static MinecraftFontFace uniformFace() {
        return face(Identifier.tryBuild("minecraft", "uniform"));
    }

    public static MinecraftFontFace altFace() {
        return face(Identifier.tryBuild("minecraft", "alt"));
    }

    public static FontFace useVanillaAsDefault() {
        MinecraftFontFace face = defaultFace();
        Fonts.global().defaultFace(face);
        return face;
    }
}

package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.Fonts;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Factory and default-face helpers for Minecraft resource-pack fonts. */
public final class MinecraftFonts {
    private static final Map<ResourceLocation, MinecraftFontFace> FACES = new ConcurrentHashMap<>();

    private MinecraftFonts() {
    }

    public static MinecraftFontFace face(ResourceLocation location) {
        return FACES.computeIfAbsent(location, MinecraftFontFace::new);
    }

    public static MinecraftFontFace face(String location) {
        ResourceLocation parsed = ResourceLocation.tryParse(location);
        if (parsed == null) throw new IllegalArgumentException("Invalid Minecraft font location: " + location);
        return face(parsed);
    }

    public static MinecraftFontFace defaultFace() {
        return face(ResourceLocation.tryBuild("minecraft", "default"));
    }

    public static MinecraftFontFace uniformFace() {
        return face(ResourceLocation.tryBuild("minecraft", "uniform"));
    }

    public static MinecraftFontFace altFace() {
        return face(ResourceLocation.tryBuild("minecraft", "alt"));
    }

    public static FontFace useVanillaAsDefault() {
        MinecraftFontFace face = defaultFace();
        Fonts.global().defaultFace(face);
        return face;
    }
}

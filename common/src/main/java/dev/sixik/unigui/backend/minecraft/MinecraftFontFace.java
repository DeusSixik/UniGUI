package dev.sixik.unigui.backend.minecraft;

import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** FontFace backed by a Minecraft FontSet and its resource-pack glyph providers. */
public final class MinecraftFontFace implements FontFace {
    private static final float VANILLA_BASE_SIZE = 9.0f;

    private final ResourceLocation location;
    private final String id;
    private final Map<SizeKey, FontMetrics> metricsCache = new ConcurrentHashMap<>();
    private final Map<AdvanceKey, Float> advanceCache = new ConcurrentHashMap<>();

    public MinecraftFontFace(ResourceLocation location) {
        this.location = Objects.requireNonNull(location, "location");
        this.id = "minecraft-font:" + location;
    }

    public MinecraftFontFace(String location) {
        this(ResourceLocation.tryParse(Objects.requireNonNull(location, "location")));
    }

    public ResourceLocation location() {
        return location;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public FontMetrics metrics(float pixelSize) {
        float size = normalizeSize(pixelSize);
        return metricsCache.computeIfAbsent(new SizeKey(Float.floatToIntBits(size)), ignored -> {
            Font font = minecraftFont();
            float lineHeight = font == null ? size : Math.max(1.0f, font.lineHeight) * size / VANILLA_BASE_SIZE;
            float ascent = lineHeight * (8.0f / 9.0f);
            return new FontMetrics(ascent, Math.max(0.0f, lineHeight - ascent), 0.0f, lineHeight);
        });
    }

    @Override
    public float advance(int codePoint, float pixelSize) {
        float size = normalizeSize(pixelSize);
        return advanceCache.computeIfAbsent(
                new AdvanceKey(codePoint, Float.floatToIntBits(size)),
                ignored -> {
                    Font font = minecraftFont();
                    if (font == null) return size * 0.5f;
                    String value = new String(Character.toChars(codePoint));
                    Component component = Component.literal(value)
                            .withStyle(style -> style.withFont(location));
                    return Math.max(0.0f, font.width(component) * size / VANILLA_BASE_SIZE);
                });
    }

    private static float normalizeSize(float pixelSize) {
        return Float.isFinite(pixelSize) ? Math.max(1.0f, pixelSize) : VANILLA_BASE_SIZE;
    }

    private static Font minecraftFont() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft == null ? null : minecraft.font;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private record SizeKey(int pixelSizeBits) {
    }

    private record AdvanceKey(int codePoint, int pixelSizeBits) {
    }
}

package dev.sixik.unigui.backend.minecraft;

import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import dev.sixik.unigui.api.text.TextRun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** FontFace backed by a Minecraft FontSet and its resource-pack glyph providers. */
public final class MinecraftFontFace implements FontFace {
    private static final float VANILLA_BASE_SIZE = TextRun.DEFAULT_PIXEL_SIZE;

    private final ResourceLocation location;
    private final String id;
    private final Map<Integer, Float> unitAdvanceCache = new ConcurrentHashMap<>();
    private volatile UnitMetrics unitMetrics;

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
        UnitMetrics metrics = unitMetrics();
        return new FontMetrics(
                metrics.ascent * size,
                metrics.descent * size,
                0.0f,
                metrics.lineHeight * size);
    }

    @Override
    public float advance(int codePoint, float pixelSize) {
        float size = normalizeSize(pixelSize);
        return unitAdvanceCache.computeIfAbsent(codePoint, ignored -> {
            Font font = minecraftFont();
            if (font == null) return 0.5f;
            String value = new String(Character.toChars(codePoint));
            Component component = Component.literal(value)
                    .withStyle(style -> style.withFont(location));
            return Math.max(0.0f, font.width(component)) / VANILLA_BASE_SIZE;
        }) * size;
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

    private UnitMetrics unitMetrics() {
        UnitMetrics cached = unitMetrics;
        if (cached != null) return cached;
        Font font = minecraftFont();
        float lineHeight = font == null ? 1.0f : Math.max(1.0f, font.lineHeight) / VANILLA_BASE_SIZE;
        float ascent = lineHeight * (8.0f / 9.0f);
        UnitMetrics computed = new UnitMetrics(ascent, Math.max(0.0f, lineHeight - ascent), lineHeight);
        unitMetrics = computed;
        return computed;
    }

    private record UnitMetrics(float ascent, float descent, float lineHeight) {
    }
}

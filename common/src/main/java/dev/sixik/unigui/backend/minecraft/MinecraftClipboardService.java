package dev.sixik.unigui.backend.minecraft;

import dev.sixik.unigui.api.input.ClipboardService;
import net.minecraft.client.Minecraft;

import java.util.Objects;

public final class MinecraftClipboardService implements ClipboardService {
    private final Minecraft minecraft;

    public MinecraftClipboardService() {
        this(Minecraft.getInstance());
    }

    public MinecraftClipboardService(Minecraft minecraft) {
        this.minecraft = Objects.requireNonNull(minecraft, "minecraft");
    }

    @Override
    public String getText() {
        return minecraft.keyboardHandler.getClipboard();
    }

    @Override
    public void setText(String text) {
        minecraft.keyboardHandler.setClipboard(text == null ? "" : text);
    }
}

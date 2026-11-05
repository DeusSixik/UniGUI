package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.input.ClipboardService;
import net.minecraft.client.gui.GuiScreen;

/**
 * System clipboard through the vanilla {@link GuiScreen} helpers; shared by screens, layers and the runtime backend.
 */
public final class CleanroomClipboardService implements ClipboardService {
    @Override
    public String getText() {
        String text = GuiScreen.getClipboardString();
        return text == null ? "" : text;
    }

    @Override
    public void setText(String text) {
        GuiScreen.setClipboardString(text == null ? "" : text);
    }
}

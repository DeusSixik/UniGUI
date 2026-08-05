package dev.sixik.unigui.utils.impl.wrapper;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VanillaScreenWrapper extends Screen {

    private static final Component EMPTY = Component.empty();

    public VanillaScreenWrapper() {
        super(EMPTY);
    }
}

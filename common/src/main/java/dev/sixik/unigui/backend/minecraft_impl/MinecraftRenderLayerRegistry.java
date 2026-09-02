package dev.sixik.unigui.backend.minecraft_impl;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;

/** Внутренний registry со snapshot без аллокаций на render-проходе. */
final class MinecraftRenderLayerRegistry<C> {
    private final ObjectArrayList<MinecraftRenderLayerRegistration<C>> entries = new ObjectArrayList<>();
    private volatile Object[] snapshot = new Object[0];

    MinecraftRenderLayerRegistration<C> add(MinecraftWidgetRenderLayer layer,
                                            Predicate<? super C> visibility,
                                            int priority) {
        MinecraftRenderLayerRegistration<C> registration = new MinecraftRenderLayerRegistration<>(
                this, Objects.requireNonNull(layer, "layer"),
                Objects.requireNonNull(visibility, "visibility"), priority);
        synchronized (entries) {
            entries.add(registration);
            rebuildSnapshot();
        }
        return registration;
    }

    void remove(MinecraftRenderLayerRegistration<C> registration) {
        synchronized (entries) {
            if (entries.remove(registration)) rebuildSnapshot();
        }
    }

    void orderChanged() {
        synchronized (entries) {
            rebuildSnapshot();
        }
    }

    void render(C context, GuiGraphics graphics, int width, int height,
                int mouseX, int mouseY, float partialTick) {
        Object[] current = snapshot;
        for (int i = 0; i < current.length; i++) {
            @SuppressWarnings("unchecked")
            MinecraftRenderLayerRegistration<C> registration =
                    (MinecraftRenderLayerRegistration<C>) current[i];
            if (registration.shouldRender(context)) {
                registration.layer().render(graphics, width, height, mouseX, mouseY, partialTick);
            }
        }
    }

    boolean hasEntries() {
        return snapshot.length != 0;
    }

    boolean mouseMoved(C context, double mouseX, double mouseY) {
        Object[] current = snapshot;
        for (int i = current.length - 1; i >= 0; i--) {
            @SuppressWarnings("unchecked")
            MinecraftRenderLayerRegistration<C> registration =
                    (MinecraftRenderLayerRegistration<C>) current[i];
            if (registration.shouldRender(context)) {
                registration.layer().mouseMoved(mouseX, mouseY);
            }
        }
        return false;
    }

    boolean mouseClicked(C context, double mouseX, double mouseY, int button) {
        Object[] current = snapshot;
        for (int i = current.length - 1; i >= 0; i--) {
            @SuppressWarnings("unchecked")
            MinecraftRenderLayerRegistration<C> registration =
                    (MinecraftRenderLayerRegistration<C>) current[i];
            if (registration.shouldRender(context)
                    && registration.layer().mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    boolean mouseReleased(C context, double mouseX, double mouseY, int button) {
        Object[] current = snapshot;
        for (int i = current.length - 1; i >= 0; i--) {
            @SuppressWarnings("unchecked")
            MinecraftRenderLayerRegistration<C> registration =
                    (MinecraftRenderLayerRegistration<C>) current[i];
            if (registration.shouldRender(context)
                    && registration.layer().mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    boolean mouseDragged(C context, double mouseX, double mouseY, int button,
                         double dragX, double dragY) {
        Object[] current = snapshot;
        for (int i = current.length - 1; i >= 0; i--) {
            @SuppressWarnings("unchecked")
            MinecraftRenderLayerRegistration<C> registration =
                    (MinecraftRenderLayerRegistration<C>) current[i];
            if (registration.shouldRender(context)
                    && registration.layer().mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        return false;
    }

    boolean mouseScrolled(C context, double mouseX, double mouseY, double delta) {
        Object[] current = snapshot;
        for (int i = current.length - 1; i >= 0; i--) {
            @SuppressWarnings("unchecked")
            MinecraftRenderLayerRegistration<C> registration =
                    (MinecraftRenderLayerRegistration<C>) current[i];
            if (registration.shouldRender(context)
                    && registration.layer().mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return false;
    }

    boolean keyPressed(C context, int keyCode, int scanCode, int modifiers) {
        Object[] current = snapshot;
        for (int i = current.length - 1; i >= 0; i--) {
            @SuppressWarnings("unchecked")
            MinecraftRenderLayerRegistration<C> registration =
                    (MinecraftRenderLayerRegistration<C>) current[i];
            if (registration.shouldRender(context)
                    && registration.layer().keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    boolean keyReleased(C context, int keyCode, int scanCode, int modifiers) {
        Object[] current = snapshot;
        for (int i = current.length - 1; i >= 0; i--) {
            @SuppressWarnings("unchecked")
            MinecraftRenderLayerRegistration<C> registration =
                    (MinecraftRenderLayerRegistration<C>) current[i];
            if (registration.shouldRender(context)
                    && registration.layer().keyReleased(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    boolean charTyped(C context, char codePoint, int modifiers) {
        Object[] current = snapshot;
        for (int i = current.length - 1; i >= 0; i--) {
            @SuppressWarnings("unchecked")
            MinecraftRenderLayerRegistration<C> registration =
                    (MinecraftRenderLayerRegistration<C>) current[i];
            if (registration.shouldRender(context)
                    && registration.layer().charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return false;
    }

    void clear() {
        Object[] current;
        synchronized (entries) {
            current = snapshot;
            entries.clear();
            snapshot = new Object[0];
        }
        for (int i = 0; i < current.length; i++) {
            @SuppressWarnings("unchecked")
            MinecraftRenderLayerRegistration<C> registration =
                    (MinecraftRenderLayerRegistration<C>) current[i];
            registration.close();
        }
    }

    private void rebuildSnapshot() {
        entries.sort(Comparator.comparingInt(MinecraftRenderLayerRegistration::priority));
        snapshot = entries.toArray();
    }
}

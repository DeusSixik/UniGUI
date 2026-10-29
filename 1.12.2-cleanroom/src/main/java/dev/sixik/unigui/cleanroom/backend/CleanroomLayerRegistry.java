package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.backend.UniGuiBackend;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

final class CleanroomLayerRegistry<C> {
    private final List<Entry<C>> entries = new ArrayList<>();
    private volatile Entry<?>[] snapshot = new Entry<?>[0];

    synchronized UniGuiBackend.LayerHandle add(LayerRenderer renderer, Predicate<Object> visibility, int priority) {
        Entry<C> entry = new Entry<>(renderer, visibility, priority);
        entries.add(entry);
        rebuildSnapshot();
        return new Handle(entry);
    }

    void render(C context, int width, int height, int mouseX, int mouseY, float partialTick) {
        Entry<?>[] current = snapshot;
        for (Entry<?> entry : current) {
            if (entry.visible(context)) entry.renderer.render(width, height, mouseX, mouseY, partialTick);
        }
    }

    boolean mouseMoved(C context, double mouseX, double mouseY) {
        Entry<?>[] current = snapshot;
        for (int i = current.length - 1; i >= 0; i--) {
            Entry<?> entry = current[i];
            if (entry.visible(context)) entry.renderer.mouseMoved(mouseX, mouseY);
        }
        return false;
    }

    boolean mouseClicked(C context, double mouseX, double mouseY, int button) {
        Entry<?>[] current = snapshot;
        for (int i = current.length - 1; i >= 0; i--) {
            Entry<?> entry = current[i];
            if (entry.visible(context) && entry.renderer.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    boolean mouseReleased(C context, double mouseX, double mouseY, int button) {
        Entry<?>[] current = snapshot;
        for (int i = current.length - 1; i >= 0; i--) {
            Entry<?> entry = current[i];
            if (entry.visible(context) && entry.renderer.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    boolean mouseScrolled(C context, double mouseX, double mouseY, double delta) {
        Entry<?>[] current = snapshot;
        for (int i = current.length - 1; i >= 0; i--) {
            Entry<?> entry = current[i];
            if (entry.visible(context) && entry.renderer.mouseScrolled(mouseX, mouseY, delta)) return true;
        }
        return false;
    }

    boolean keyTyped(C context, char character, int keyCode, int modifiers) {
        Entry<?>[] current = snapshot;
        for (int i = current.length - 1; i >= 0; i--) {
            Entry<?> entry = current[i];
            if (entry.visible(context) && entry.renderer.keyTyped(character, keyCode, modifiers)) return true;
        }
        return false;
    }

    boolean keyReleased(C context, int keyCode, int modifiers) {
        Entry<?>[] current = snapshot;
        for (int i = current.length - 1; i >= 0; i--) {
            Entry<?> entry = current[i];
            if (entry.visible(context) && entry.renderer.keyReleased(keyCode, modifiers)) return true;
        }
        return false;
    }

    synchronized void clear() {
        Entry<?>[] current = snapshot;
        entries.clear();
        snapshot = new Entry<?>[0];
        for (Entry<?> entry : current) entry.close();
    }

    private synchronized void rebuildSnapshot() {
        entries.removeIf(Entry::closed);
        entries.sort(Comparator.comparingInt(Entry::priority));
        snapshot = entries.toArray(Entry<?>[]::new);
    }

    interface LayerRenderer {
        void render(int width, int height, int mouseX, int mouseY, float partialTick);

        default void mouseMoved(double mouseX, double mouseY) {
        }

        default boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        default boolean mouseReleased(double mouseX, double mouseY, int button) {
            return false;
        }

        default boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            return false;
        }

        default boolean keyTyped(char character, int keyCode, int modifiers) {
            return false;
        }

        default boolean keyReleased(int keyCode, int modifiers) {
            return false;
        }

        void close();
    }

    private static final class Entry<C> {
        private final LayerRenderer renderer;
        private Predicate<Object> visibility;
        private int priority;
        private boolean enabled = true;
        private boolean closed;

        private Entry(LayerRenderer renderer, Predicate<Object> visibility, int priority) {
            this.renderer = Objects.requireNonNull(renderer, "renderer");
            this.visibility = Objects.requireNonNull(visibility, "visibility");
            this.priority = priority;
        }

        private boolean visible(Object context) {
            return !closed && enabled && visibility.test(context);
        }

        private int priority() {
            return priority;
        }

        private boolean closed() {
            return closed;
        }

        private void close() {
            if (closed) return;
            closed = true;
            renderer.close();
        }
    }

    private final class Handle implements UniGuiBackend.LayerHandle {
        private final Entry<C> entry;

        private Handle(Entry<C> entry) {
            this.entry = entry;
        }

        @Override
        public boolean enabled() {
            return !entry.closed && entry.enabled;
        }

        @Override
        public void enabled(boolean enabled) {
            synchronized (CleanroomLayerRegistry.this) {
                if (entry.closed) return;
                entry.enabled = enabled;
            }
        }

        @Override
        public int priority() {
            return entry.priority;
        }

        @Override
        public void priority(int priority) {
            synchronized (CleanroomLayerRegistry.this) {
                if (entry.closed || entry.priority == priority) return;
                entry.priority = priority;
                rebuildSnapshot();
            }
        }

        @Override
        public void visibleWhen(Predicate<Object> visibility) {
            synchronized (CleanroomLayerRegistry.this) {
                if (entry.closed) return;
                entry.visibility = Objects.requireNonNull(visibility, "visibility");
            }
        }

        @Override
        public boolean closed() {
            return entry.closed;
        }

        @Override
        public void close() {
            synchronized (CleanroomLayerRegistry.this) {
                if (entry.closed) return;
                entry.close();
                entries.remove(entry);
                rebuildSnapshot();
            }
        }
    }
}

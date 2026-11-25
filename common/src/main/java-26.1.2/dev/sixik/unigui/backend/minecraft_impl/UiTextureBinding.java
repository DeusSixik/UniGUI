package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import java.util.IdentityHashMap;

/**
 * Разрешённая привязка {@link TextureHandle} к текстуре Minecraft 26.1.
 *
 * <p>Поддерживаемые native-хэндлы: цветовая текстура {@link MinecraftRenderTarget}, {@link Identifier} (текстура
 * из {@code TextureManager}), {@link AbstractTexture} и {@link GpuTextureView}. Числовые GL-имена текстур,
 * которые принимали 1.20/1.21, в 26.1 не поддерживаются: текстуры там создаются через {@code GpuDevice}.</p>
 *
 * @param view               вид текстуры
 * @param sampler            сэмплер с учётом {@link TextureOptions}
 * @param flipY              текстура хранится снизу вверх (цель отрисовки) и V нужно отразить
 * @param premultipliedAlpha текстура хранит premultiplied alpha
 * @param options            опции, по которым построен сэмплер
 * @param source             исходный native-объект (для проверки актуальности)
 */
record UiTextureBinding(GpuTextureView view, GpuSampler sampler, boolean flipY, boolean premultipliedAlpha,
                        TextureOptions options, Object source) {

    /**
     * Разрешает хэндл.
     *
     * @param texture хэндл
     * @return привязка или {@code null}, если текстура недоступна
     */
    static UiTextureBinding resolve(TextureHandle texture) {
        if (texture == null) return null;
        TextureOptions options = texture.options() == null ? TextureOptions.defaults() : texture.options();
        Object nativeHandle = texture.nativeHandle();
        if (nativeHandle instanceof MinecraftRenderTarget.ColorTextureHandle colorTexture) {
            if (colorTexture.isDisposed()) return null;
            GpuTextureView view = colorTexture.view();
            if (view == null) return null;
            return new UiTextureBinding(view, MinecraftTextureSamplerState.sampler(options, colorTexture.defaultSampler()),
                    colorTexture.flipY(), true, options, view);
        }
        if (nativeHandle instanceof GpuTextureView view) {
            if (view.isClosed()) return null;
            return new UiTextureBinding(view, MinecraftTextureSamplerState.sampler(options), false,
                    options.premultipliedAlpha(), options, view);
        }
        AbstractTexture abstractTexture;
        if (nativeHandle instanceof AbstractTexture direct) {
            abstractTexture = direct;
        } else {
            Identifier location = nativeHandle instanceof Identifier identifier
                    ? identifier
                    : Identifier.tryParse(texture.id());
            if (location == null) return null;
            try {
                abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(location);
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        try {
            GpuTextureView view = abstractTexture.getTextureView();
            return new UiTextureBinding(view, MinecraftTextureSamplerState.sampler(options, abstractTexture.getSampler()),
                    false, options.premultipliedAlpha(), options, view);
        } catch (IllegalStateException notLoaded) {
            return null;
        }
    }

    /**
     * Проверяет, что привязка всё ещё описывает тот же хэндл (текстура не пересоздана, опции не менялись).
     */
    boolean matches(TextureHandle texture) {
        if (view.isClosed()) return false;
        TextureOptions current = texture.options() == null ? TextureOptions.defaults() : texture.options();
        if (current.packed() != options.packed()) return false;
        Object nativeHandle = texture.nativeHandle();
        if (nativeHandle instanceof MinecraftRenderTarget.ColorTextureHandle colorTexture) {
            return !colorTexture.isDisposed() && colorTexture.view() == source;
        }
        if (nativeHandle instanceof GpuTextureView currentView) return currentView == source;
        return true;
    }

    /**
     * Ограниченный LRU-кэш привязок с identity-ключами по хэндлу.
     */
    static final class Cache {
        private final IdentityHashMap<TextureHandle, Entry> entries;
        private final int capacity;
        private Entry first;
        private Entry last;

        Cache(int capacity) {
            this.capacity = Math.max(1, capacity);
            this.entries = new IdentityHashMap<>(this.capacity * 2);
        }

        UiTextureBinding resolve(TextureHandle texture) {
            if (texture == null) return null;
            Entry entry = entries.get(texture);
            if (entry != null) {
                if (entry.binding != null && entry.binding.matches(texture)) {
                    moveToFront(entry);
                    return entry.binding;
                }
                remove(entry);
            }

            UiTextureBinding binding = UiTextureBinding.resolve(texture);
            if (binding == null) return null;
            put(texture, binding);
            return binding;
        }

        void clear() {
            entries.clear();
            first = null;
            last = null;
        }

        private void put(TextureHandle texture, UiTextureBinding binding) {
            Entry entry = entries.get(texture);
            if (entry == null) {
                if (entries.size() >= capacity) {
                    entry = last;
                    entries.remove(entry.texture);
                    unlink(entry);
                } else {
                    entry = new Entry();
                }
                entries.put(texture, entry);
            }
            entry.texture = texture;
            entry.binding = binding;
            linkFirst(entry);
        }

        private void moveToFront(Entry entry) {
            if (entry == first) return;
            unlink(entry);
            linkFirst(entry);
        }

        private void remove(Entry entry) {
            entries.remove(entry.texture);
            unlink(entry);
            entry.texture = null;
            entry.binding = null;
        }

        private void linkFirst(Entry entry) {
            entry.previous = null;
            entry.next = first;
            if (first != null) first.previous = entry;
            else last = entry;
            first = entry;
        }

        private void unlink(Entry entry) {
            if (entry.previous == null) first = entry.next;
            else entry.previous.next = entry.next;
            if (entry.next == null) last = entry.previous;
            else entry.next.previous = entry.previous;
            entry.previous = null;
            entry.next = null;
        }

        private static final class Entry {
            private TextureHandle texture;
            private UiTextureBinding binding;
            private Entry previous;
            private Entry next;
        }
    }
}

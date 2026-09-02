package dev.sixik.unigui.backend.minecraft_impl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;

import java.util.Optional;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Контекст текущего загрузочного overlay Minecraft.
 *
 * <p>Экземпляр создаётся автоматически из оригинального конструктора
 * {@link LoadingOverlay}. Контекст предназначен для чтения состояния загрузки
 * пользовательским UniGUI-слоем. Сам жизненный цикл остаётся у Minecraft:
 * vanilla самостоятельно вызывает callback после проверки {@link ReloadInstance}
 * и обрабатывает ошибки через {@code Optional<Throwable>}.</p>
 */
@Environment(EnvType.CLIENT)
public final class LoadingScreenContext {
    private final LoadingOverlay overlay;
    private final Minecraft minecraft;
    private final ReloadInstance reloadInstance;
    private final Consumer<Optional<Throwable>> completionConsumer;
    private final boolean fadeIn;

    LoadingScreenContext(LoadingOverlay overlay, Minecraft minecraft,
                        ReloadInstance reloadInstance,
                        Consumer<Optional<Throwable>> completionConsumer,
                        boolean fadeIn) {
        this.overlay = Objects.requireNonNull(overlay, "overlay");
        this.minecraft = Objects.requireNonNull(minecraft, "minecraft");
        this.reloadInstance = Objects.requireNonNull(reloadInstance, "reloadInstance");
        this.completionConsumer = Objects.requireNonNull(completionConsumer, "completionConsumer");
        this.fadeIn = fadeIn;
    }

    /** @return оригинальный LoadingOverlay, для которого создан контекст */
    public LoadingOverlay overlay() {
        return overlay;
    }

    /** @return экземпляр Minecraft из конструктора LoadingOverlay */
    public Minecraft minecraft() {
        return minecraft;
    }

    /** @return оригинальный ReloadInstance из конструктора LoadingOverlay */
    public ReloadInstance reloadInstance() {
        return reloadInstance;
    }

    /**
     * Возвращает текущий прогресс resource reload.
     *
     * @return значение в диапазоне {@code 0.0f..1.0f}
     */
    public float progress() {
        return clamp(reloadInstance.getActualProgress());
    }

    /** @return {@code true}, если reload завершён */
    public boolean done() {
        return reloadInstance.isDone();
    }

    /**
     * Возвращает исходный флаг плавного появления из конструктора LoadingOverlay.
     *
     * @return {@code true}, если vanilla overlay использует fade-in
     */
    public boolean fadeIn() {
        return fadeIn;
    }

    /**
     * Возвращает оригинальный callback завершения загрузки.
     *
     * <p>Callback сохранён для advanced-интеграций, которым требуется передать
     * его дальше при создании собственного vanilla-compatible overlay. При обычном
     * использовании UniGUI вызывать его не нужно: Minecraft вызывает callback
     * автоматически ровно в рамках своего loading lifecycle.</p>
     *
     * @return оригинальный callback Minecraft
     */
    public Consumer<Optional<Throwable>> completionConsumer() {
        return completionConsumer;
    }

    private static float clamp(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}

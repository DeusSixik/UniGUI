package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.widget.Widget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Точка регистрации UniGUI-слоёв для экрана загрузки Minecraft.
 *
 * <p>Загрузочный экран Minecraft является {@code LoadingOverlay}, а не обычным
 * {@code Screen}. UniGUI-слой подключается после vanilla-рендера этого overlay,
 * поэтому стандартная загрузка ресурсов, прогресс и завершение reload продолжают
 * работать без вмешательства. Если корневой виджет занимает весь viewport, он может
 * полностью заменить визуальную часть стандартного экрана.</p>
 *
 * <pre>{@code
 * Box root = new Box();
 * root.layout(style -> style.sizePercent(100.0f, 100.0f));
 * LoadingScreenRender.register(root);
 * }</pre>
 *
 * <p>Регистрация создаётся один раз и живёт до вызова {@link MinecraftRenderLayerRegistration#close()}.
 * Backend и render-ресурсы создаются лениво на первом render-проходе, когда Minecraft уже
 * подготовил графический контекст.</p>
 */
@Environment(EnvType.CLIENT)
public final class LoadingScreenRender {
    private static final MinecraftRenderLayerRegistry<Minecraft> REGISTRY =
            new MinecraftRenderLayerRegistry<>();
    private static volatile LoadingScreenContext activeContext;

    private LoadingScreenRender() {
    }

    /** Регистрирует слой с контекстом UniGUI по умолчанию. */
    public static MinecraftRenderLayerRegistration<Minecraft> register(Widget root) {
        return register(new MinecraftWidgetRenderLayer(root));
    }

    /** Регистрирует слой с указанным контекстом UniGUI. */
    public static MinecraftRenderLayerRegistration<Minecraft> register(Widget root, UIContext context) {
        return register(new MinecraftWidgetRenderLayer(root, context));
    }

    /** Регистрирует готовый render-layer загрузочного экрана. */
    public static MinecraftRenderLayerRegistration<Minecraft> register(MinecraftWidgetRenderLayer layer) {
        return REGISTRY.add(layer, client -> true, 0);
    }

    /** Удаляет все зарегистрированные загрузочные слои и освобождает их ресурсы. */
    public static void clear() {
        REGISTRY.clear();
    }

    /**
     * Возвращает контекст активного LoadingOverlay.
     *
     * @return контекст во время загрузки или {@code null}, если загрузочный overlay не активен
     */
    public static LoadingScreenContext context() {
        LoadingScreenContext context = activeContext;
        if (context == null) return null;

        Minecraft client = Minecraft.getInstance();
        return client.getOverlay() == context.overlay() ? context : null;
    }

    /** @return {@code true}, если сейчас активен LoadingOverlay */
    public static boolean isLoading() {
        return context() != null;
    }

    /**
     * Возвращает {@code true}, если vanilla пока не должен снимать указанный overlay.
     *
     * <p>Метод используется version-specific mixin'ом: Minecraft удаляет
     * {@link LoadingOverlay} через две секунды после начала своего fade-out. Если
     * пользовательский слой делает более длинную анимацию, это снятие временно
     * откладывается до вызова {@link #releaseOverlay()}.</p>
     */
    public static boolean shouldKeepOverlay(LoadingOverlay overlay) {
        LoadingScreenContext context = activeContext;
        return overlay != null
                && context != null
                && context.overlay() == overlay
                && REGISTRY.hasEntries();
    }

    /**
     * Разрешает завершить loading overlay и снимает его с Minecraft.
     *
     * <p>Вызывать метод следует после завершения собственной анимации loading-слоя.
     * Если overlay уже был снят vanilla, метод безопасно завершает только внутренний
     * контекст.</p>
     */
    public static void releaseOverlay() {
        LoadingScreenContext context = activeContext;
        if (context == null) return;

        activeContext = null;
        Minecraft client = context.minecraft();
        if (client.getOverlay() == context.overlay()) {
            client.setOverlay(null);
        }
    }

    /** Внутренняя точка вызова из version-specific LoadingOverlay hook. */
    public static void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (graphics == null) return;

        Minecraft client = Minecraft.getInstance();
        if (client.getWindow() == null) return;
        if (!(client.getOverlay() instanceof LoadingOverlay)) return;

        REGISTRY.render(
                client,
                graphics,
                client.getWindow().getGuiScaledWidth(),
                client.getWindow().getGuiScaledHeight(),
                mouseX,
                mouseY,
                partialTick
        );
    }

    /**
     * Сохраняет оригинальные аргументы конструктора LoadingOverlay для пользовательского UI.
     *
     * <p>Метод нужен version-specific mixin'ам и не предназначен для прямого вызова
     * из кода мода. Пользователь получает созданный контекст через {@link #context()}.</p>
     */
    public static void onLoadingOverlayCreated(LoadingOverlay overlay, Minecraft minecraft,
                                               ReloadInstance reloadInstance,
                                               Consumer<Optional<Throwable>> completionConsumer,
                                               boolean fadeIn) {
        activeContext = new LoadingScreenContext(
                overlay,
                minecraft,
                reloadInstance,
                completionConsumer,
                fadeIn
        );
    }
}

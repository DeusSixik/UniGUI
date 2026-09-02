package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.posteffect.UiPostEffects;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.HudRender;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftRenderLayerRegistration;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetRenderLayer;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.backend.minecraft_impl.ScreenOverlayRender;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.display.Label;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Полный пример подключения UniGUI к двум внешним точкам рендера Minecraft.
 *
 * <p>Класс специально оставлен в {@code TestMod}, чтобы его можно было открыть и
 * использовать как шаблон при интеграции библиотеки в мод. В рабочем моде обычно
 * достаточно перенести аналогичную регистрацию в собственный client entrypoint.</p>
 *
 * <h2>HUD и Screen Overlay</h2>
 * <ul>
 *     <li><b>HUD</b> вызывается из рендера игрового HUD. Он подходит для индикаторов,
 *     прицела, статуса задания и других элементов, которые показываются во время игры.</li>
 *     <li><b>Screen Overlay</b> вызывается после рендера текущего Minecraft Screen.
 *     Он подходит для собственных кнопок, подсказок или панелей поверх чужого интерфейса,
 *     например JEI.</li>
 * </ul>
 *
 * <h2>Жизненный цикл</h2>
 * <p>{@link #register()} вызывается один раз при запуске клиентской части. Регистрация
 * не пересоздаётся каждый кадр: слой и его UI-дерево живут до вызова {@link #close()}.
 * Во время рендера библиотека сама выполняет layout, animation tick, построение
 * draw list, render cache и PostEffect.</p>
 *
 * <h2>Минимальный шаблон</h2>
 * <pre>{@code
 * Box root = new Box();
 * MinecraftRenderLayerRegistration<Screen> registration =
 *         ScreenOverlayRender.register(root);
 * registration.visibleWhen(screen -> screen instanceof InventoryScreen);
 *
 * // Когда UI больше не нужен:
 * registration.close();
 * }</pre>
 *
 * @see HudRender
 * @see ScreenOverlayRender
 * @see MinecraftWidgetRenderLayer
 * @see MinecraftRenderLayerRegistration
 */
public final class OverlayRenderDemo {
    /*
     * Регистрации сохраняются в полях, чтобы:
     * 1) не создавать новый слой при каждом вызове register();
     * 2) иметь возможность корректно освободить UI-ресурсы через close().
     */
    private static MinecraftRenderLayerRegistration<Minecraft> hudRegistration;
    private static MinecraftRenderLayerRegistration<Screen> screenOverlayRegistration;

    private OverlayRenderDemo() {
    }

    /**
     * Регистрирует оба примера.
     *
     * <p>Метод безопасно вызвать несколько раз: после первой успешной регистрации
     * новые render-layer не создаются.</p>
     */
    public static void register() {
        if (isRegistered()) {
            return;
        }

        registerHud();
        registerScreenOverlay();
    }

    /**
     * Регистрирует HUD-слой.
     *
     * <p>Тип контекста регистрации здесь {@code Minecraft}, поэтому predicate получает
     * текущий клиент. HUD вызывается из vanilla HUD-прохода и поэтому остаётся видимым
     * под открытым Minecraft Screen, например под чатом или инвентарём. Predicate можно
     * заменить на любое условие, если конкретному HUD нужно скрываться в отдельных состояниях.</p>
     */
    private static void registerHud() {
        Box hudRoot = transparentRoot(createHudBadge());
        MinecraftWidgetRenderLayer hudLayer = new MinecraftWidgetRenderLayer(hudRoot);

        hudLayer.postEffect(UiPostEffects.CHROMATIC_ABERRATION);

        // HUD вызывается из vanilla HUD-прохода до Minecraft Screen.
        // Поэтому он продолжает жить под чатом, инвентарём и другими экранами.
        hudRegistration = HudRender.register(hudLayer);
    }

    /**
     * Регистрирует слой поверх чужих Minecraft Screen.
     *
     * <p>Тип контекста здесь {@code Screen}, поэтому predicate получает экран, поверх
     * которого выполняется рендер. Важно исключить {@link MinecraftWidgetScreen}, если
     * overlay не должен появляться поверх экранов, которые уже принадлежат UniGUI.</p>
     *
     * <p>Overlay также получает события мыши и клавиатуры. Если виджет обработал событие,
     * оно не передаётся дальше в чужой Minecraft Screen.</p>
     */
    private static void registerScreenOverlay() {
        Box overlayRoot = transparentRoot(createScreenOverlayBadge());
        MinecraftWidgetRenderLayer overlayLayer = new MinecraftWidgetRenderLayer(overlayRoot);

        screenOverlayRegistration = ScreenOverlayRender.register(
                overlayLayer,
                screen -> !(screen instanceof MinecraftWidgetScreen),
                100
        );
    }

    /**
     * Показывает, существуют ли ещё активные регистрации.
     *
     * <p>Проверка {@link MinecraftRenderLayerRegistration#closed()} нужна на случай,
     * если конкретную регистрацию закрыли напрямую, а затем снова вызвали register().</p>
     */
    private static boolean isRegistered() {
        return (hudRegistration != null && !hudRegistration.closed())
                || (screenOverlayRegistration != null && !screenOverlayRegistration.closed());
    }

    /**
     * Удаляет демонстрационные слои и освобождает их UI/render-ресурсы.
     *
     * <p>В реальном моде этот метод нужно вызывать при отключении функциональности,
     * смене режима или другой точке, где UI больше не должен существовать.</p>
     */
    public static void close() {
        if (hudRegistration != null) {
            hudRegistration.close();
            hudRegistration = null;
        }
        if (screenOverlayRegistration != null) {
            screenOverlayRegistration.close();
            screenOverlayRegistration = null;
        }
    }

    /**
     * Создаёт корневой контейнер на весь logical GUI viewport.
     *
     * <p>Корневой контейнер прозрачный, поэтому он не рисует фон поверх Minecraft.
     * Его единственная задача здесь - дать дочернему виджету обычное UniGUI-дерево
     * и область раскладки. Размер viewport библиотека передаёт автоматически.</p>
     */
    private static Box transparentRoot(Widget child) {
        Box root = new Box();
        root.themeEnabled(false);
        root.backgroundVisible(false);
        root.borderVisible(false);
        root.addChild(child);
        return root;
    }

    /**
     * Создаёт пример виджета, который рисуется в игровом HUD.
     */
    private static Box createHudBadge() {
        Box badge = createBadge("UNIGUI HUD", 112.0f);
        badge.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .right(8.0f)
                .bottom(8.0f)
                .size(112.0f, 24.0f)
                .flexGrow(0.0f)
                .flexShrink(0.0f));

        // Анимация выполняется существующим animation engine слоя.
        badge.animateOpacity(
                0.55f,
                TransitionSpec.of(0.9f, AnimationEasing.EASE_IN_OUT).loop().yoyo()
        );
        return badge;
    }

    /**
     * Создаёт пример виджета, который рисуется поверх чужого Screen.
     */
    private static Box createScreenOverlayBadge() {
        Box badge = createBadge("SCREEN OVERLAY", 132.0f);
        badge.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .left(8.0f)
                .top(8.0f)
                .size(132.0f, 24.0f)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        return badge;
    }

    /**
     * Общая визуальная часть двух примеров.
     *
     * <p>Это обычные UniGUI-виджеты. Разница между HUD и overlay находится только
     * в выборе точки регистрации, а не в способе создания или рендера компонентов.</p>
     */
    private static Box createBadge(String text, float width) {
        Box badge = new Box();
        badge.themeEnabled(false);
        badge.backgroundVisible(true);
        badge.borderVisible(true);
        badge.radius(4.0f);
        badge.background().set(0.025f, 0.035f, 0.050f, 0.94f);
        badge.borderColor().set(0.20f, 0.78f, 0.92f, 0.90f);

        Label label = new Label(text);
        label.color().set(0.72f, 0.94f, 1.0f, 1.0f);
        label.layout(style -> style
                .size(width - 12.0f, 16.0f)
                .margin(6.0f, 4.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        badge.addChild(label);
        return badge;
    }
}

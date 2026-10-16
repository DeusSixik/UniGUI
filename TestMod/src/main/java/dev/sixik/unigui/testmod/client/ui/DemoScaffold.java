package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.Slider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Общий скаффолд демо-экранов TestMod: палитра, открытие экрана и layout-оболочка.
 *
 * <p>Все демо делят один каркас: полноэкранный viewport с фоном, тело
 * {@code HBox} (контент + боковая панель 260px) и панель с заголовком,
 * статусом, хинтом и кнопками Recenter/Close. Скаффолд убирает копипасту
 * {@code open()/root()/label()/button()/slider()} из каждого экрана.</p>
 */
public final class DemoScaffold {
    /** Фон демо-viewport'а. */
    public static final MutableColor BG = MutableColor.rgba(0.014f, 0.018f, 0.028f, 1.0f);
    /** Фон боковой панели. */
    public static final MutableColor PANEL = MutableColor.rgba(0.050f, 0.064f, 0.088f, 0.96f);
    /** Цвет рамки боковой панели. */
    public static final MutableColor BORDER = MutableColor.rgba(0.220f, 0.400f, 0.580f, 0.88f);
    /** Основной цвет текста демо. */
    public static final MutableColor TEXT = MutableColor.rgba(0.820f, 0.900f, 0.960f, 1.0f);
    /** Приглушённый цвет текста хинтов. */
    public static final MutableColor MUTED = MutableColor.rgba(0.500f, 0.600f, 0.700f, 1.0f);

    private DemoScaffold() {
    }

    /**
     * Открывает демо-экран с заголовком и построителем контента.
     *
     * <p>Создаёт UI-контекст со scale-провайдером и профайлером, строит root
     * через скаффолд и показывает {@link MinecraftWidgetScreen}. Закрытие
     * возвращает предыдущий экран.</p>
     *
     * @param title заголовок окна Minecraft
     * @param panelTitle заголовок боковой панели
     * @param hintText текст хинта под статусом; {@code null} прячет хинт
     * @param build наполнение контента и панели; получает собранный {@link DemoShell}
     */
    public static void open(String title, String panelTitle, String hintText, Consumer<DemoShell> build) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen previous = minecraft.screen;

        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        context.scaleProvider(new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 6.0f)
                .userScale(2.0f));
        context.debugFlags(DebugFlags.PROFILER_OVERLAY);

        Runnable[] close = new Runnable[1];
        DemoShell shell = new DemoShell(panelTitle, hintText, () -> close[0].run());
        if (build != null) {
            build.accept(shell);
        }
        Widget root = shell.root();
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(
                Component.literal(title == null ? "Demo" : title), root, context) {
            @Override
            public void onClose() {
                Minecraft.getInstance().setScreen(previous);
            }
        };
        close[0] = screen::onClose;
        screen.useContextScale().scaleWithMinecraftGui(false);
        screen.useSdfDefaultFont();
        screen.renderPolicy(UiRenderPolicy.vsync());
        minecraft.setScreen(screen);
    }

    /**
     * Создаёт label с цветом текста.
     *
     * @param text текст
     * @param color цвет или {@code null} для дефолтного
     * @return новый label
     */
    public static Label label(String text, MutableColor color) {
        Label label = new Label(text);
        if (color != null) {
            label.color(color);
        }
        return label;
    }

    /**
     * Создаёт кнопку с click-обработчиком.
     *
     * @param text текст кнопки
     * @param action действие по клику или {@code null}
     * @return новая кнопка
     */
    public static Button button(String text, Runnable action) {
        Button button = new Button(text);
        if (action != null) {
            button.onClick(event -> action.run());
        }
        return button;
    }

    /**
     * Создаёт слайдер с диапазоном и стартовым значением.
     *
     * @param min минимум диапазона
     * @param max максимум диапазона
     * @param value стартовое значение
     * @return новый слайдер на всю ширину панели
     */
    public static Slider slider(float min, float max, float value) {
        Slider slider = new Slider();
        slider.range(min, max);
        slider.value(value);
        slider.layout(style -> style.widthPercent(100.0f).height(24.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        return slider;
    }

    /**
     * Создаёт чекбокс с подписью.
     *
     * @param text подпись
     * @param checked стартовое состояние
     * @return новый чекбокс
     */
    public static Checkbox checkbox(String text, boolean checked) {
        Checkbox checkbox = new Checkbox(text);
        checkbox.checked(checked);
        return checkbox;
    }

    /**
     * Растягивает виджет на ширину панели с фиксированной высотой.
     *
     * @param widget виджет боковой панели
     * @param height высота в UI-пикселях
     * @param <T> тип виджета
     * @return тот же виджет для fluent-цепочек
     */
    public static <T extends WidgetBase> T fullWidth(T widget, float height) {
        widget.layout(style -> style.widthPercent(100.0f).height(height)
                .flexGrow(0.0f).flexShrink(0.0f));
        return widget;
    }

    /**
     * Собранная оболочка демо: контент, панель, статус и закрытие.
     */
    public static final class DemoShell {
        private final VBox side = new VBox();
        private final Label stats;
        private final Label hint;
        private final Runnable closeAction;
        private Widget content;
        private Runnable recenterAction;
        private boolean rootBuilt;
        private Widget builtRoot;

        private DemoShell(String panelTitle, String hintText, Runnable closeAction) {
            this.closeAction = closeAction;
            side.spacing(8.0f);
            side.layout(style -> style.width(240.0f).heightPercent(100.0f).padding(10.0f)
                    .gap(8.0f).flexGrow(0.0f).flexShrink(0.0f));
            Label title = label(panelTitle == null ? "DEMO" : panelTitle, TEXT);
            fullWidth(title, 22.0f);
            side.addChild(title);
            stats = label("", TEXT);
            fullWidth(stats, 20.0f);
            side.addChild(stats);
            if (hintText != null) {
                hint = label(hintText, MUTED);
                fullWidth(hint, 34.0f);
                side.addChild(hint);
            } else {
                hint = null;
            }
        }

        /**
         * Задаёт контент-виджет слева от панели.
         *
         * @param content canvas или другой контент; {@code null} очищает
         * @return эта оболочка
         */
        public DemoShell content(Widget content) {
            this.content = content;
            if (content instanceof WidgetBase base) {
                base.layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));
            }
            return this;
        }

        /**
         * Возвращает статус-строку панели.
         *
         * @return label статуса для обновления текста
         */
        public Label stats() {
            return stats;
        }

        /**
         * Возвращает хинт панели.
         *
         * @return label хинта или {@code null}, если хинт не задан
         */
        public Label hint() {
            return hint;
        }

        /**
         * Возвращает VBox боковой панели для добавления контролов.
         *
         * @return контейнер панели (заголовок/статус/хинт уже добавлены)
         */
        public VBox side() {
            return side;
        }

        /**
         * Добавляет виджет на боковую панель.
         *
         * @param widget контрол панели
         * @return эта оболочка
         */
        public DemoShell add(Widget widget) {
            side.addChild(widget);
            return this;
        }

        /**
         * Задаёт действие кнопки Recenter.
         *
         * @param recenterAction действие или {@code null}, чтобы скрыть кнопку
         * @return эта оболочка
         */
        public DemoShell onRecenter(Runnable recenterAction) {
            this.recenterAction = recenterAction;
            return this;
        }

        /**
         * Возвращает действие закрытия экрана.
         *
         * @return runnable закрытия
         */
        public Runnable closeAction() {
            return closeAction;
        }

        private Widget root() {
            if (rootBuilt) return builtRoot;
            StackPanel viewport = new StackPanel();
            viewport.layout(style -> style.sizePercent(100.0f, 100.0f).overflow(Overflow.HIDDEN));

            Box background = new Box();
            background.themeEnabled(false);
            background.backgroundVisible(true);
            background.borderVisible(false);
            background.background().set(BG);
            background.layout(style -> style.sizePercent(100.0f, 100.0f)
                    .align(Alignment.STRETCH, Alignment.STRETCH));
            viewport.addChild(background);

            if (recenterAction != null) {
                Button recenter = button("Recenter", recenterAction);
                fullWidth(recenter, 28.0f);
                side.addChild(recenter);
            }
            Button close = button("Close", closeAction);
            fullWidth(close, 28.0f);
            side.addChild(close);

            Box sidePanel = new Box();
            sidePanel.themeEnabled(false);
            sidePanel.backgroundVisible(true);
            sidePanel.borderVisible(true);
            sidePanel.background().set(PANEL);
            sidePanel.borderColor().set(BORDER);
            sidePanel.layout(style -> style.width(260.0f).heightPercent(100.0f)
                    .flexGrow(0.0f).flexShrink(0.0f));
            sidePanel.addChild(side);

            HBox body = new HBox();
            body.spacing(10.0f);
            body.layout(style -> style.sizePercent(96.0f, 90.0f)
                    .align(Alignment.CENTER, Alignment.CENTER).gap(10.0f));
            if (content != null) {
                body.addChild(content);
            }
            body.addChild(sidePanel);
            viewport.addChild(body);
            builtRoot = new OverlayLayer(viewport);
            rootBuilt = true;
            return builtRoot;
        }
    }
}

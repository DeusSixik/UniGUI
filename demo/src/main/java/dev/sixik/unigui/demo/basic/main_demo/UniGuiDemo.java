package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.ClipboardService;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.style.*;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.utils.consumers.basic.FloatConsumer;
import dev.sixik.unigui.api.utils.reactive.FloatSignal;
import dev.sixik.unigui.api.utils.reactive.SignalSubscription;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.input.MemoryClipboardService;
import dev.sixik.unigui.impl.style.DefaultStylePack;
import dev.sixik.unigui.widgets.containers.*;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.NotificationView;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.Toast;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.navigation.NavigationView;
import dev.sixik.unigui.widgets.navigation.TreeView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UniGuiDemo {

    private static final FloatSignal SCALE = new FloatSignal(1f);

    private UniGuiDemo() {
    }

    public static UniScreen openGui() {
        return openGui(new MemoryClipboardService());
    }

    public static UniScreen openGui(ClipboardService service) {
        final UnityLikeUIScaleProvider provider = new UnityLikeUIScaleProvider();
        return openGui(new DefaultUIContext(service).scaleProvider(provider), provider::userScale);
    }

    public static UniScreen openGui(UIContext context, FloatConsumer onScaleChange) {
        UniScreen screen = createScreen(context, onScaleChange);
        screen.openGui();
        return screen;
    }

    public static UniScreen createScreen() {
        final UnityLikeUIScaleProvider scale = new UnityLikeUIScaleProvider();
        UIContext context = new DefaultUIContext(new MemoryClipboardService())
                .scaleProvider(new UnityLikeUIScaleProvider());
        return createScreen(context, scale::userScale);
    }

    public static UniScreen createScreen(UIContext context, FloatConsumer onScaleChange) {
        GalleryTemplate gallery = new GalleryTemplate();
        OverlayLayer root = host(gallery);
        UniScreen screen = new UniScreen("demo", root, context);
        final SignalSubscription sub = SCALE.subscribe(onScaleChange::accept);
        screen.onClose(sub::close);
        screen.defaultFont(Fonts.defaultFace());
        gallery.exitAction(screen::closeGui);
        screen.scaleWithGameGui(false);

        if (context instanceof DefaultUIContext defaultUIContext) {
            defaultUIContext.theme(PACK, root);
        }

        return screen;
    }

    private static final MutableColor CARD_TEXT = rgb(25, 42, 56);
    private static final MutableColor CARD_TEXT_DISABLED = rgb(140, 152, 163);
    private static final MutableColor CARD_ACCENT = rgb(30, 166, 222);
    private static final MutableColor CARD_CONTROL_BORDER = rgb(120, 134, 146);
    private static final MutableColor CARD_ROW_HOVER = rgb(225, 234, 242);
    private static final MutableColor CARD_ROW_SELECTED = rgb(206, 226, 243);

    /**
     * Объявлен после цветов {@code CARD_*}: статические поля инициализируются по порядку, и иначе правила
     * пакета получили бы {@code null} вместо цветов.
     */
    public static final StylePack PACK = demoStylePack();

    /**
     * Пакет стилей демо: галерея кладёт Checkbox и RadioButton на светлую карточку, поэтому им нужен
     * тёмный текст во всех состояниях (дерево красит {@link #cardTree}). Одного обычного состояния мало: тема
     * задаёт цвет текста отдельно для hover, checked и disabled, и без них текст остался бы белым.
     */
    private static StylePack demoStylePack() {
        StylePack pack = DefaultStylePack.copy().id("demo");
        pack.fallback(blueAccentColors(pack.fallback()));
        for (StyleDefinition definition : List.copyOf(pack.styles().values())) {
            pack.put(definition.style(blueAccentColors(definition.style())));
        }
        return pack
                .put(style("demo.checkbox", StyleIds.Widget.CHECKBOX, darkText(new MutableStyle())
                        .put(StyleKeys.CHECKBOX_BACKGROUND_COLOR, rgb(255, 255, 255))
                        .put(StyleKeys.CHECKBOX_BACKGROUND_COLOR, WidgetState.CHECKED, CARD_ACCENT)
                        .put(StyleKeys.BORDER_COLOR, CARD_CONTROL_BORDER)
                        .put(StyleKeys.BORDER_COLOR, WidgetState.HOVERED, CARD_ACCENT)
                        .put(StyleKeys.BORDER_COLOR, WidgetState.CHECKED, CARD_ACCENT)
                        .put(StyleKeys.CHECKBOX_MARK_SHAPE, CheckboxMarkShape.CHECK)
                        .put(StyleKeys.CHECKBOX_MARK_COLOR, rgb(255, 255, 255))))
                .put(style("demo.radio", StyleIds.Widget.RADIO_BUTTON, darkText(new MutableStyle())
                        .put(StyleKeys.BORDER_COLOR, CARD_CONTROL_BORDER)
                        .put(StyleKeys.BORDER_COLOR, WidgetState.HOVERED, CARD_ACCENT)
                        .put(StyleKeys.BORDER_COLOR, WidgetState.CHECKED, CARD_ACCENT)
                        .put(StyleKeys.ACCENT_COLOR, WidgetState.CHECKED, CARD_ACCENT)))
                .put(style("demo.switch", StyleIds.Widget.TOGGLE_SWITCH, darkText(new MutableStyle())));
    }

    private static MutableStyle blueAccentColors(Style source) {
        MutableStyle result = new MutableStyle();
        for (var stateEntry : source.values().entrySet()) {
            for (var valueEntry : stateEntry.getValue().entrySet()) {
                Object value = valueEntry.getValue();
                if (value instanceof ColorView color) {
                    value = replaceDefaultAccent(color);
                }
                putStyleValue(result, valueEntry.getKey(), stateEntry.getKey(), value);
            }
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void putStyleValue(MutableStyle style, StyleKey<?> key, WidgetState state, Object value) {
        style.put((StyleKey) key, state, value);
    }

    private static ColorView replaceDefaultAccent(ColorView color) {
        if (near(color, 201, 138, 75)) return rgb(59, 130, 246);
        if (near(color, 224, 166, 104)) return rgb(96, 165, 250);
        if (near(color, 107, 74, 44)) return rgb(37, 99, 235);
        if (near(color, 240, 189, 122)) return rgb(147, 197, 253);
        if (near(color, 32, 18, 6)) return rgb(255, 255, 255);
        if (near(color, 95, 174, 122)) return rgb(59, 130, 246);
        if (near(color, 60, 104, 76)) return rgb(37, 99, 235);
        return color;
    }

    private static boolean near(ColorView color, int red, int green, int blue) {
        return Math.abs(color.r() - red / 255.0f) < 0.01f
                && Math.abs(color.g() - green / 255.0f) < 0.01f
                && Math.abs(color.b() - blue / 255.0f) < 0.01f;
    }

    /**
     * Красит дерево под светлую карточку галереи.
     *
     * <p>Через {@link StylePack} это не сделать: {@code TreeView} наследует {@code LinearBox}, который выключает
     * глобальную тему, поэтому глобальные правила на дерево не действуют, а локальный стиль действует всегда.</p>
     *
     * @param tree дерево на светлой карточке
     * @return это же дерево
     */
    public static TreeView cardTree(TreeView tree) {
        tree.style(style -> style
                .put(StyleKeys.TREE_TEXT_COLOR, rgb(45, 62, 78))
                .put(StyleKeys.TREE_SELECTED_TEXT_COLOR, rgb(11, 60, 100))
                .put(StyleKeys.TREE_DISABLED_TEXT_COLOR, CARD_TEXT_DISABLED)
                .put(StyleKeys.TREE_HOVER_COLOR, CARD_ROW_HOVER)
                .put(StyleKeys.TREE_SELECTED_COLOR, CARD_ROW_SELECTED)
                .put(StyleKeys.TREE_DISCLOSURE_COLOR, rgb(96, 112, 126))
                .put(StyleKeys.TREE_ICON_COLOR, rgb(96, 112, 126)));
        return tree;
    }

    /**
     * Тёмный текст для обычного, hover, checked и disabled состояний.
     */
    private static MutableStyle darkText(MutableStyle style) {
        return style
                .put(StyleKeys.TEXT_COLOR, CARD_TEXT)
                .put(StyleKeys.TEXT_COLOR, WidgetState.HOVERED, CARD_TEXT)
                .put(StyleKeys.TEXT_COLOR, WidgetState.CHECKED, CARD_TEXT)
                .put(StyleKeys.TEXT_COLOR, WidgetState.DISABLED, CARD_TEXT_DISABLED);
    }

    private static StyleDefinition style(String id, String target, MutableStyle style) {
        return StyleDefinition.of(id, style).target(target);
    }

    private static MutableColor rgb(int red, int green, int blue) {
        return MutableColor.rgba255(red, green, blue, 255);
    }

    public static Widget createRoot() {
        return host(new GalleryTemplate());
    }

    /**
     * Кладёт галерею в {@link OverlayLayer} и добавляет в него стек уведомлений, чтобы {@link Toast} было где показать.
     */
    private static OverlayLayer host(GalleryTemplate gallery) {
        return new OverlayLayer(gallery).addOverlay(gallery.notifications);
    }

    /**
     * Слайдер масштаба интерфейса: применяет значение только когда кнопку мыши отпустили.
     *
     * <p>Слайдер сам лежит в масштабируемом интерфейсе. Если менять масштаб на каждом движении мыши, ползунок
     * уезжает из-под курсора, значение пересчитывается от нового положения, масштаб меняется снова, и ползунок
     * «убегает» в крайнее положение, а мышь перестаёт им управлять. Поэтому во время перетаскивания меняется
     * только положение ползунка, а масштаб применяется один раз при отпускании.</p>
     */
    private static final class ScaleSlider extends Slider {
        @Override
        public void handle(Event event) {
            boolean wasDragging = dragging();
            super.handle(event);
            if (wasDragging && !dragging() && event instanceof PointerReleasedEvent) {
                SCALE.set(value());
            }
        }
    }

    protected static final class GalleryTemplate extends SplitPanel {
        protected static final MutableColor SIDEBAR = color(19, 30, 42);
        private static final MutableColor SIDEBAR_HEADER = color(25, 39, 53);
        protected static final MutableColor NAV_GROUP = color(39, 54, 69);
        protected static final MutableColor NAV_SELECTED = color(20, 119, 178);
        private static final MutableColor WORKSPACE = color(22, 70, 108);
        private static final MutableColor WORKSPACE_DEEP = color(19, 61, 96);
        protected static final MutableColor CARD = color(247, 249, 251);
        protected static final MutableColor CARD_BORDER = color(216, 224, 231);
        protected static final MutableColor TEXT_DARK = color(25, 42, 56);
        protected static final MutableColor TEXT_MUTED = color(183, 210, 228);
        protected static final MutableColor EXIT = color(199, 63, 43);
        protected static final MutableColor CODE_BACKGROUND = color(10, 12, 15);
        protected static final MutableColor CODE_BORDER = color(46, 54, 63);
        protected static final MutableColor CODE_TEXT = color(214, 226, 236);
        protected static final MutableColor CODE_HINT = color(112, 130, 146);
        protected static final MutableColor CODE_SCROLL_THUMB = color(78, 92, 106);
        protected static final MutableColor CODE_SCROLL_THUMB_HOVER = color(112, 130, 146);

        private final List<DemoCategory> categories = List.of(
                new BasicInputCategory(),
                new TextCategory(),
                new LayoutCategory(),
                new CanvasCategory(),
                new GraphCategory(),
                new BlueprintCategory(),
                new NavigationWidgetsCategory(),
                new DataCategory(),
                new MediaCategory(),
                new FeedbackCategory(),
                new StatusCategory(),
                new DemoAppsCategory());

        protected final NavigationView navigation = new NavigationView();
        protected final NotificationView notifications = new NotificationView()
                .placement(NotificationView.Placement.BOTTOM_RIGHT)
                .duration(1.8f)
                .maxVisible(3);
        protected final VBox article = new VBox();
        protected final List<Slider> accentTargets = new ArrayList<>();
        protected final Label breadcrumb = new Label("BASIC INPUT");
        protected final HBox accentSwatches = new HBox();
        protected final MutableColor accent = color(30, 166, 222);
        protected String selectedId = "slider";
        protected Runnable exitAction = () -> {
        };

        private GalleryTemplate() {
            super();
            orientation(Orientation.HORIZONTAL);
            splitRatio(0.18f);
            minFirstSize(176.0f);
            minSecondSize(360.0f);
            splitterThickness(3.0f);
            layout(style -> style.expand());

            first(createSidebar());
            second(createWorkspace());
            refreshArticle();
        }

        /**
         * Копирует текст в буфер обмена контекста и показывает уведомление в углу экрана.
         *
         * @param text    копируемый текст
         * @param message текст уведомления
         */
        protected void copyToClipboard(String text, String message) {
            UIContext context = uiContext();
            boolean copied = context != null && context.clipboard() != null;
            if (copied) context.clipboard().setText(text);
            notifications.addToast(new Toast(copied ? message : "Clipboard is unavailable")
                    .duration(1.8f)
                    .preferredSize(190.0f, 34.0f));
        }

        private void exitAction(Runnable action) {
            exitAction = action == null ? () -> {
            } : action;
        }

        private Widget createSidebar() {
            VBox sidebar = new VBox();
            sidebar.themeEnabled(false);
            sidebar.backgroundVisible(true);
            sidebar.background().set(SIDEBAR);
            sidebar.borderVisible(false);
            sidebar.spacing(0.0f);
            sidebar.layout(style -> style.expand());

            VBox brand = new VBox();
            brand.themeEnabled(false);
            brand.spacing(2.0f);
            brand.backgroundVisible(true);
            brand.background().set(SIDEBAR_HEADER);
            brand.layout(style -> style.widthPercent(100.0f).padding(18.0f, 16.0f).flexGrow(0.0f).flexShrink(0.0f));
            Label brandTitle = label("Demo", color(246, 250, 253));
            brandTitle.font(null, 16.0f);
            brand.addChild(brandTitle);
            brand.addChild(label("UNIGUI  /  COMPONENT LAB", color(142, 169, 189)));
            sidebar.addChild(brand);

            navigation.spacing(0.0f);
            navigation.layout(style -> style.widthPercent(100.0f));
            navigation.categoryColor(NAV_GROUP);
            navigation.style(style -> style.put(StyleKeys.TREE_SELECTED_COLOR, NAV_SELECTED));
            buildNavigation();

            ScrollView navScroll = new ScrollView(navigation);
            navScroll.overlayScrollBars(false);
            navScroll.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f));
            sidebar.addChild(navScroll);

            Box footer = new Box();
            footer.themeEnabled(false);
            footer.backgroundVisible(true);
            footer.background().set(SIDEBAR_HEADER);
            footer.layout(style -> style.widthPercent(100.0f).padding(12.0f, 14.0f).flexGrow(0.0f).flexShrink(0.0f));
            footer.addChild(label("UniGUI  ·  Interactive controls", color(164, 187, 203)));
            sidebar.addChild(footer);
            return sidebar;
        }

        /**
         * Передаёт построение групп, пунктов и их иконок соответствующим категориям демо.
         */
        private void buildNavigation() {
            for (DemoCategory category : categories) {
                category.init(this);
                category.addNavigation(navigation);
            }
            navigation.selectItem(selectedId);
            navigation.onItemSelected(item -> {
                for (DemoCategory category : categories) {
                    for (DemoCategory.Control control : category.controls()) {
                        if (control.id().equals(item.id())) {
                            select(category, control);
                            return;
                        }
                    }
                }
            });
        }

        private Widget createWorkspace() {
            VBox workspace = new VBox();
            workspace.themeEnabled(false);
            workspace.backgroundVisible(true);
            workspace.background().set(WORKSPACE);
            workspace.borderVisible(false);
            workspace.spacing(0.0f);
            workspace.layout(style -> style.expand());

            HBox toolbar = new HBox();
            toolbar.themeEnabled(false);
            toolbar.spacing(14.0f);
            toolbar.backgroundVisible(true);
            toolbar.background().set(WORKSPACE_DEEP);
            toolbar.layout(style -> style.widthPercent(100.0f).height(54.0f).padding(16.0f, 8.0f)
                    .alignItems(Align.CENTER).flexGrow(0.0f).flexShrink(0.0f));
            breadcrumb.color(color(156, 196, 221));
            toolbar.addChild(breadcrumb);
            Box spacer = new Box();
            spacer.themeEnabled(false);
            spacer.backgroundVisible(false);
            spacer.borderVisible(false);
            spacer.layout(style -> style.flexGrow(1.0f).height(1.0f));
            toolbar.addChild(spacer);

            Label accentLabel = label("Scale", color(154, 192, 218));
            toolbar.addChild(accentLabel);
            accentSwatches.spacing(6.0f);
            accentSwatches.layout(style -> style.alignItems(Align.CENTER));

            final Slider scaleSlider = new ScaleSlider();
            scaleSlider.range(1f, 3f).value(SCALE.get());
            scaleSlider.onValueChanged(event -> {
                // Во время перетаскивания масштаб не трогаем (см. ScaleSlider), клавиатура применяется сразу.
                if (!scaleSlider.dragging()) SCALE.set(event.newValue());
            });

            accentSwatches.addChild(scaleSlider);
            toolbar.addChild(accentSwatches);

            Button exit = new Button("Exit");
            styleButton(exit, EXIT, color(255, 255, 255));
            exit.layout(style -> style.width(72.0f).height(34.0f).flexGrow(0.0f).flexShrink(0.0f));
            exit.onClick(event -> exitAction.run());

            toolbar.addChild(exit);
            workspace.addChild(toolbar);

            article.spacing(18.0f);
            article.backgroundVisible(false);
            article.borderVisible(false);
            article.layout(style -> style.widthPercent(100.0f).padding(26.0f, 24.0f).flexGrow(0.0f));
            ScrollView articleViewport = new ScrollView(article);
            articleViewport.overlayScrollBars(false);
            articleViewport.scrollStep(28.0f);
            articleViewport.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f));
            workspace.addChild(articleViewport);
            return workspace;
        }

        private void addAccentSwatch(String name, ColorView color) {
            Button swatch = new Button(name);
            styleButton(swatch, color, color(255, 255, 255));
            swatch.layout(style -> style.width(54.0f).height(26.0f).flexGrow(0.0f).flexShrink(0.0f));
            swatch.onClick(event -> {
                accent.set(color);
                for (Slider slider : accentTargets) {
                    slider.fillColor().set(accent);
                }
            });
            accentSwatches.addChild(swatch);
        }

        private void select(DemoCategory category, DemoCategory.Control control) {
            selectedId = control.id();
            breadcrumb.text(category.title().toUpperCase());
            refreshArticle(category, control);
        }

        private void refreshArticle() {
            DemoCategory category = categories.get(0);
            DemoCategory.Control control = category.controls().get(0);
            search:
            for (DemoCategory candidate : categories) {
                for (DemoCategory.Control item : candidate.controls()) {
                    if (item.id().equals(selectedId)) {
                        category = candidate;
                        control = item;
                        break search;
                    }
                }
            }
            refreshArticle(category, control);
        }

        private void refreshArticle(DemoCategory category, DemoCategory.Control control) {
            article.clearChildren();
            accentTargets.clear();
            Label title = label(control.title(), color(255, 255, 255));
            title.font(null, 24.0f);
            article.addChild(title);
            Label summary = label(control.description(), color(207, 225, 239));
            summary.font(null, 12.0f);
            summary.layout(style -> style.widthPercent(100.0f));
            article.addChild(summary);

            category.addDemo(control.id(), article);
        }

        protected static Label valueLabel(float value) {
            Label label = label(format(value), TEXT_DARK);
            label.layout(style -> style.alignItems(Align.CENTER));
            return label;
        }

        protected static Box expandSpacer() {
            Box spacer = new Box();
            spacer.themeEnabled(false);
            spacer.backgroundVisible(false);
            spacer.borderVisible(false);
            spacer.layout(style -> style.flexGrow(1.0f).height(1.0f));
            return spacer;
        }

        protected static Label label(String text, ColorView color) {
            Label label = new Label(text);
            label.color(color);
            return label;
        }

        protected static void styleButton(Button button, ColorView background, ColorView foreground) {
            button.themeEnabled(false);
            button.backgroundVisible(true);
            button.background().set(background);
            button.borderVisible(false);
            button.radius(4.0f);
            button.textColor(foreground);
            button.textPadding(8.0f, 4.0f);
        }

        protected static MutableColor color(int red, int green, int blue) {
            return MutableColor.rgba255(red, green, blue, 255);
        }

        protected static String format(float value) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
    }
}

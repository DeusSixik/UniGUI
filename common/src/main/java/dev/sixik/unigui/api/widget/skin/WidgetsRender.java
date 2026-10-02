package dev.sixik.unigui.api.widget.skin;

import dev.sixik.unigui.api.widget.render.WidgetRenderRegistry;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.api.render.plan.StyleRenderPlanRegistry;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.widgets.render.BorderRenderPlans;
import dev.sixik.unigui.widgets.render.BorderState;
import dev.sixik.unigui.widgets.render.BoxRenderPlans;
import dev.sixik.unigui.widgets.render.BoxState;
import dev.sixik.unigui.widgets.render.ButtonRenderPlans;
import dev.sixik.unigui.widgets.render.ButtonState;
import dev.sixik.unigui.widgets.render.ProgressBarRenderPlans;
import dev.sixik.unigui.widgets.render.ProgressBarState;
import dev.sixik.unigui.widgets.render.ScrollBarRenderPlans;
import dev.sixik.unigui.widgets.render.ScrollBarState;
import dev.sixik.unigui.widgets.render.SeparatorRenderPlans;
import dev.sixik.unigui.widgets.render.SeparatorState;
import dev.sixik.unigui.widgets.render.ShapeRenderPlans;
import dev.sixik.unigui.widgets.render.ShapeState;
import dev.sixik.unigui.widgets.render.SliderRenderPlans;
import dev.sixik.unigui.widgets.render.SliderState;
import dev.sixik.unigui.widgets.render.TextInputRenderPlans;
import dev.sixik.unigui.widgets.render.TextInputState;
import dev.sixik.unigui.widgets.render.TextureWidgetRenderPlans;
import dev.sixik.unigui.widgets.render.TextureWidgetState;

/**
 * Фасад доступа к текущему набору процедурных renderer'ов виджетов.
 *
 * <p>{@code WidgetsRender} хранит активную реализацию {@link WidgetsRenderImpl} и всегда
 * возвращает безопасный renderer: если custom implementation отдаёт {@code null}, facade
 * автоматически использует {@link DefaultWidgetsRenderImpl}. Это позволяет модам и темам
 * переопределять только нужные renderers, не копируя весь набор дефолтов.</p>
 *
 * <p>Facade также регистрирует дефолтные Java-renderer id в {@link WidgetRenderRegistry}
 * и декларативные RenderPlan builders в {@link StyleRenderPlanRegistry}. Поэтому StylePack
 * может ссылаться на стандартные id, а виджеты получают единый fallback path.</p>
 *
 * @see WidgetsRenderImpl
 * @see DefaultWidgetsRenderImpl
 * @see WidgetRenderRegistry
 */
public final class WidgetsRender {
    private static volatile WidgetsRenderImpl impl = DefaultWidgetsRenderImpl.INSTANCE;

    static {
        registerDefaults(WidgetRenderRegistry.global());
    }

    private WidgetsRender() {
    }

    /**
     * Возвращает активную реализацию renderer-набора.
     *
     * @return custom implementation или {@link DefaultWidgetsRenderImpl#INSTANCE}
     */
    public static WidgetsRenderImpl current() {
        return impl;
    }

    /**
     * Устанавливает активную реализацию renderer-набора.
     *
     * <p>{@code null} возвращает систему к дефолтной реализации. После смены implementation
     * facade повторно регистрирует стандартные renderer id, чтобы registry ссылался на актуальные
     * renderer instances.</p>
     *
     * @param customImpl частичная или полная реализация renderer-набора
     */
    public static void use(WidgetsRenderImpl customImpl) {
        impl = customImpl == null ? DefaultWidgetsRenderImpl.INSTANCE : customImpl;
        registerDefaults(WidgetRenderRegistry.global());
    }


    /**
     * Регистрирует стандартные renderer id в глобальном {@link WidgetRenderRegistry}.
     */
    public static void registerDefaults() {
        registerDefaults(WidgetRenderRegistry.global());
    }

    /**
     * Регистрирует стандартные renderer id в указанном registry.
     *
     * @param registry registry, куда нужно записать renderer id; {@code null} игнорируется
     */
    public static void registerDefaults(WidgetRenderRegistry registry) {
        if (registry == null) return;
        registry.register("unigui:loading/default", loadingDefault());
        registry.register("unigui:loading/spinner", loadingSpinner());
        registry.register("unigui:loading/dots", loadingDots());
        registry.register("unigui:loading/bar", loadingBar());
        registry.register("unigui:progress-bar/default", progressBar());
        registry.register("unigui:slider/default", slider());
        registry.register("unigui:sparkline/default", sparkline());
        registry.register("unigui:chart/default", chart());
        registry.register("unigui:graph-view/default", graphView());
        registry.register("unigui:node-graph/default", nodeGraph());
        registry.register("unigui:color-picker/default", colorPicker());
        registry.register("unigui:date-picker/default", datePicker());
        registry.register("unigui:scroll-bar/default", scrollBar());
        registry.register("unigui:button/default", button());
        registry.register("unigui:toggle-button/default", toggleButton());
        registry.register("unigui:toggle-switch/default", toggleSwitch());
        registry.register("unigui:checkbox/default", checkbox());
        registry.register("unigui:radio-button/default", radioButton());
        registry.register("unigui:tool-button/default", toolButton());
        registry.register("unigui:hold-button/default", holdButton());
        registry.register("unigui:text-input/default", textInput());
        registry.register("unigui:text-field/default", textField());
        registry.register("unigui:search-field/default", searchField());
        registry.register("unigui:password-field/default", passwordField());
        registry.register("unigui:number-field/default", numberField());
        registry.register("unigui:text-area/default", textArea());
        registry.register("unigui:shape/default", shape());
        registry.register("unigui:separator/default", separator());
        registry.register("unigui:border/default", border());
        registry.register("unigui:tooltip/default", tooltip());
        registry.register("unigui:texture-widget/default", textureWidget());
        registry.register("unigui:image-view/default", imageView());
        registry.register("unigui:path/default", path());
        registry.register("unigui:cached-subtree/default", cachedSubtree());
        registry.register("unigui:box/default", box());
        registry.register("unigui:window/default", window());
        registry.register("unigui:modal-scrim/default", modalScrim());
        registry.register("unigui:docking-root/default", dockingRoot());
        registry.register("unigui:dock-pane/default", dockPane());
        registry.register("unigui:dock-split-handle/default", dockSplitHandle());
        registry.register("unigui:dock-drop-preview/default", dockDropPreview());
        registry.register("unigui:splitter/default", splitter());
        registry.register("unigui:text-widget/default", textWidget());
        registry.register("unigui:virtual-list-view/default", virtualListView());
        registry.register("unigui:tree-view/default", treeView());
        registry.register("unigui:virtual-table-view/default", virtualTableView());
        registerDefaultRenderPlans(StyleRenderPlanRegistry.global());
    }

    /**
     * Регистрирует стандартные StylePack RenderPlan builders в указанном registry.
     *
     * <p>Эти builders позволяют декларативным стилям строить draw-команды для базовых виджетов
     * без Java renderer override. Регистрация безопасна для повторного вызова: одинаковые widget ids
     * перезаписываются актуальными builders.</p>
     *
     * @param registry registry render-plan builders; {@code null} игнорируется
     */
    public static void registerDefaultRenderPlans(StyleRenderPlanRegistry registry) {
        if (registry == null) return;
        registry.register(StyleIds.Widget.BORDER, BorderState.class, BorderRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.BOX, BoxState.class, BoxRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.BUTTON, ButtonState.class, ButtonRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.TOGGLE_BUTTON, ButtonState.class, ButtonRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.TOGGLE_SWITCH, ButtonState.class, ButtonRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.CHECKBOX, ButtonState.class, ButtonRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.RADIO_BUTTON, ButtonState.class, ButtonRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.PROGRESS_BAR, ProgressBarState.class, ProgressBarRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.SCROLL_BAR, ScrollBarState.class, ScrollBarRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.SEPARATOR, SeparatorState.class, SeparatorRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.SHAPE, ShapeState.class, ShapeRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.SLIDER, SliderState.class, SliderRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.TEXT_INPUT, TextInputState.class, TextInputRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.TEXT_FIELD, TextInputState.class, TextInputRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.PASSWORD_FIELD, TextInputState.class, TextInputRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.NUMBER_FIELD, TextInputState.class, TextInputRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.SEARCH_FIELD, TextInputState.class, TextInputRenderPlans::searchStyledPlan);
        registry.register(StyleIds.Widget.TEXTURE_WIDGET, TextureWidgetState.class, TextureWidgetRenderPlans::styledPlan);
        registry.register(StyleIds.Widget.IMAGE_VIEW, TextureWidgetState.class, TextureWidgetRenderPlans::styledPlan);
    }

    /**
     * Возвращает renderer для loading indicator по умолчанию.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender loadingDefault() {
        WidgetRender renderer = impl.loadingDefault();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.loadingDefault() : renderer;
    }

    /**
     * Возвращает renderer для spinner loading indicator.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender loadingSpinner() {
        WidgetRender renderer = impl.loadingSpinner();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.loadingSpinner() : renderer;
    }

    /**
     * Возвращает renderer для dots loading indicator.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender loadingDots() {
        WidgetRender renderer = impl.loadingDots();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.loadingDots() : renderer;
    }

    /**
     * Возвращает renderer для bar loading indicator.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender loadingBar() {
        WidgetRender renderer = impl.loadingBar();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.loadingBar() : renderer;
    }

    /**
     * Возвращает renderer для progress bar.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender progressBar() {
        WidgetRender renderer = impl.progressBar();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.progressBar() : renderer;
    }

    /**
     * Возвращает renderer для slider.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender slider() {
        WidgetRender renderer = impl.slider();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.slider() : renderer;
    }

    /**
     * Возвращает renderer для sparkline.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender sparkline() {
        WidgetRender renderer = impl.sparkline();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.sparkline() : renderer;
    }

    /**
     * Возвращает renderer для chart.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender chart() {
        WidgetRender renderer = impl.chart();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.chart() : renderer;
    }

    /**
     * Возвращает renderer для graph view.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender graphView() {
        WidgetRender renderer = impl.graphView();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.graphView() : renderer;
    }

    /**
     * Возвращает renderer для node graph.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender nodeGraph() {
        WidgetRender renderer = impl.nodeGraph();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.nodeGraph() : renderer;
    }

    /**
     * Возвращает renderer для color picker.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender colorPicker() {
        WidgetRender renderer = impl.colorPicker();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.colorPicker() : renderer;
    }

    /**
     * Возвращает renderer для date picker.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender datePicker() {
        WidgetRender renderer = impl.datePicker();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.datePicker() : renderer;
    }

    /**
     * Возвращает renderer для scroll bar.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender scrollBar() {
        WidgetRender renderer = impl.scrollBar();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.scrollBar() : renderer;
    }

    /**
     * Возвращает renderer для обычной button.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender button() {
        WidgetRender renderer = impl.button();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.button() : renderer;
    }

    /**
     * Возвращает renderer для toggle button.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender toggleButton() {
        WidgetRender renderer = impl.toggleButton();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.toggleButton() : renderer;
    }

    /**
     * Возвращает renderer для toggle switch.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender toggleSwitch() {
        WidgetRender renderer = impl.toggleSwitch();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.toggleSwitch() : renderer;
    }

    /**
     * Возвращает renderer для checkbox.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender checkbox() {
        WidgetRender renderer = impl.checkbox();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.checkbox() : renderer;
    }

    /**
     * Возвращает renderer для radio button.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender radioButton() {
        WidgetRender renderer = impl.radioButton();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.radioButton() : renderer;
    }

    /**
     * Возвращает renderer для toolbar button.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender toolButton() {
        WidgetRender renderer = impl.toolButton();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.toolButton() : renderer;
    }

    /**
     * Возвращает renderer для hold button.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender holdButton() {
        WidgetRender renderer = impl.holdButton();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.holdButton() : renderer;
    }

    /**
     * Возвращает renderer для базового text input.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender textInput() {
        WidgetRender renderer = impl.textInput();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.textInput() : renderer;
    }

    /**
     * Возвращает renderer для text field.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender textField() {
        WidgetRender renderer = impl.textField();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.textField() : renderer;
    }

    /**
     * Возвращает renderer для search field.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender searchField() {
        WidgetRender renderer = impl.searchField();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.searchField() : renderer;
    }

    /**
     * Возвращает renderer для password field.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender passwordField() {
        WidgetRender renderer = impl.passwordField();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.passwordField() : renderer;
    }

    /**
     * Возвращает renderer для number field.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender numberField() {
        WidgetRender renderer = impl.numberField();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.numberField() : renderer;
    }

    /**
     * Возвращает renderer для text area.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender textArea() {
        WidgetRender renderer = impl.textArea();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.textArea() : renderer;
    }

    /**
     * Возвращает renderer для shape widget.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender shape() {
        WidgetRender renderer = impl.shape();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.shape() : renderer;
    }

    /**
     * Возвращает renderer для separator.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender separator() {
        WidgetRender renderer = impl.separator();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.separator() : renderer;
    }

    /**
     * Возвращает renderer для border.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender border() {
        WidgetRender renderer = impl.border();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.border() : renderer;
    }

    /**
     * Возвращает renderer для tooltip.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender tooltip() {
        WidgetRender renderer = impl.tooltip();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.tooltip() : renderer;
    }

    /**
     * Возвращает renderer для texture widget.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender textureWidget() {
        WidgetRender renderer = impl.textureWidget();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.textureWidget() : renderer;
    }

    /**
     * Возвращает renderer для image view.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender imageView() {
        WidgetRender renderer = impl.imageView();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.imageView() : renderer;
    }

    /**
     * Возвращает renderer для path widget.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender path() {
        WidgetRender renderer = impl.path();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.path() : renderer;
    }

    /**
     * Возвращает renderer для cached subtree.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender cachedSubtree() {
        WidgetRender renderer = impl.cachedSubtree();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.cachedSubtree() : renderer;
    }

    /**
     * Возвращает renderer для box/container.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender box() {
        WidgetRender renderer = impl.box();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.box() : renderer;
    }

    /**
     * Возвращает renderer для window.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender window() {
        WidgetRender renderer = impl.window();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.window() : renderer;
    }

    /**
     * Возвращает renderer для modal scrim.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender modalScrim() {
        WidgetRender renderer = impl.modalScrim();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.modalScrim() : renderer;
    }

    /**
     * Возвращает renderer для docking root.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender dockingRoot() {
        WidgetRender renderer = impl.dockingRoot();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.dockingRoot() : renderer;
    }

    /**
     * Возвращает renderer для dock pane.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender dockPane() {
        WidgetRender renderer = impl.dockPane();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.dockPane() : renderer;
    }

    /**
     * Возвращает renderer для dock split handle.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender dockSplitHandle() {
        WidgetRender renderer = impl.dockSplitHandle();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.dockSplitHandle() : renderer;
    }

    /**
     * Возвращает renderer для dock drop preview.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender dockDropPreview() {
        WidgetRender renderer = impl.dockDropPreview();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.dockDropPreview() : renderer;
    }

    /**
     * Возвращает renderer для splitter.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender splitter() {
        WidgetRender renderer = impl.splitter();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.splitter() : renderer;
    }

    /**
     * Возвращает renderer для text widget.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender textWidget() {
        WidgetRender renderer = impl.textWidget();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.textWidget() : renderer;
    }

    /**
     * Возвращает renderer для virtual list view.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender virtualListView() {
        WidgetRender renderer = impl.virtualListView();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.virtualListView() : renderer;
    }

    /**
     * Возвращает renderer для tree view.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender treeView() {
        WidgetRender renderer = impl.treeView();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.treeView() : renderer;
    }

    /**
     * Возвращает renderer для virtual table view.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WidgetRender virtualTableView() {
        WidgetRender renderer = impl.virtualTableView();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.virtualTableView() : renderer;
    }
}

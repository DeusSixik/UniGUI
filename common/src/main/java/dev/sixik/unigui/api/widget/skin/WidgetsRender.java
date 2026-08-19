package dev.sixik.unigui.api.widget.skin;

import dev.sixik.unigui.api.widget.render.WidgetRendererRegistry;
import dev.sixik.unigui.api.render.plan.StyleRenderPlanRegistry;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.widgets.render.LoadingIndicatorRenderer;
import dev.sixik.unigui.widgets.render.ChartRenderer;
import dev.sixik.unigui.widgets.render.ColorPickerRenderer;
import dev.sixik.unigui.widgets.render.DatePickerRenderer;
import dev.sixik.unigui.widgets.render.DockDropPreviewRenderer;
import dev.sixik.unigui.widgets.render.DockPaneRenderer;
import dev.sixik.unigui.widgets.render.DockSplitHandleRenderer;
import dev.sixik.unigui.widgets.render.DockingRootRenderer;
import dev.sixik.unigui.widgets.render.GraphViewRenderer;
import dev.sixik.unigui.widgets.render.NodeGraphRenderer;
import dev.sixik.unigui.widgets.render.ModalScrimRenderer;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ProgressBarRenderer;
import dev.sixik.unigui.widgets.render.ScrollBarRenderer;
import dev.sixik.unigui.widgets.render.SliderRenderer;
import dev.sixik.unigui.widgets.render.SparklineRenderer;
import dev.sixik.unigui.widgets.render.TextAreaRenderer;
import dev.sixik.unigui.widgets.render.TextInputRenderer;
import dev.sixik.unigui.widgets.render.ShapeRenderer;
import dev.sixik.unigui.widgets.render.SeparatorRenderer;
import dev.sixik.unigui.widgets.render.BorderRenderer;
import dev.sixik.unigui.widgets.render.TooltipRenderer;
import dev.sixik.unigui.widgets.render.TextureWidgetRenderer;
import dev.sixik.unigui.widgets.render.PathRenderer;
import dev.sixik.unigui.widgets.render.CachedSubtreeRenderer;
import dev.sixik.unigui.widgets.render.BoxRenderer;
import dev.sixik.unigui.widgets.render.WindowRenderer;
import dev.sixik.unigui.widgets.render.SplitterRenderer;
import dev.sixik.unigui.widgets.render.TextWidgetRenderer;
import dev.sixik.unigui.widgets.render.VirtualListViewRenderer;
import dev.sixik.unigui.widgets.render.TreeViewRenderer;
import dev.sixik.unigui.widgets.render.VirtualTableViewRenderer;
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
 * <p>Facade также регистрирует дефолтные Java-renderer id в {@link WidgetRendererRegistry}
 * и декларативные RenderPlan builders в {@link StyleRenderPlanRegistry}. Поэтому StylePack
 * может ссылаться на стандартные id, а виджеты получают единый fallback path.</p>
 *
 * @see WidgetsRenderImpl
 * @see DefaultWidgetsRenderImpl
 * @see WidgetRendererRegistry
 */
public final class WidgetsRender {
    private static volatile WidgetsRenderImpl impl = DefaultWidgetsRenderImpl.INSTANCE;

    static {
        registerDefaults(WidgetRendererRegistry.global());
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
        registerDefaults(WidgetRendererRegistry.global());
    }


    /**
     * Регистрирует стандартные renderer id в глобальном {@link WidgetRendererRegistry}.
     */
    public static void registerDefaults() {
        registerDefaults(WidgetRendererRegistry.global());
    }

    /**
     * Регистрирует стандартные renderer id в указанном registry.
     *
     * @param registry registry, куда нужно записать renderer id; {@code null} игнорируется
     */
    public static void registerDefaults(WidgetRendererRegistry registry) {
        if (registry == null) return;
        registry.register("unigui:loading/default", LoadingIndicatorRenderer.class, loadingDefault());
        registry.register("unigui:loading/spinner", LoadingIndicatorRenderer.class, loadingSpinner());
        registry.register("unigui:loading/dots", LoadingIndicatorRenderer.class, loadingDots());
        registry.register("unigui:loading/bar", LoadingIndicatorRenderer.class, loadingBar());
        registry.register("unigui:progress-bar/default", ProgressBarRenderer.class, progressBar());
        registry.register("unigui:slider/default", SliderRenderer.class, slider());
        registry.register("unigui:sparkline/default", SparklineRenderer.class, sparkline());
        registry.register("unigui:chart/default", ChartRenderer.class, chart());
        registry.register("unigui:graph-view/default", GraphViewRenderer.class, graphView());
        registry.register("unigui:node-graph/default", NodeGraphRenderer.class, nodeGraph());
        registry.register("unigui:color-picker/default", ColorPickerRenderer.class, colorPicker());
        registry.register("unigui:date-picker/default", DatePickerRenderer.class, datePicker());
        registry.register("unigui:scroll-bar/default", ScrollBarRenderer.class, scrollBar());
        registry.register("unigui:button/default", ButtonRenderer.class, button());
        registry.register("unigui:toggle-button/default", ButtonRenderer.class, toggleButton());
        registry.register("unigui:toggle-switch/default", ButtonRenderer.class, toggleSwitch());
        registry.register("unigui:checkbox/default", ButtonRenderer.class, checkbox());
        registry.register("unigui:radio-button/default", ButtonRenderer.class, radioButton());
        registry.register("unigui:text-input/default", TextInputRenderer.class, textInput());
        registry.register("unigui:text-field/default", TextInputRenderer.class, textField());
        registry.register("unigui:search-field/default", TextInputRenderer.class, searchField());
        registry.register("unigui:password-field/default", TextInputRenderer.class, passwordField());
        registry.register("unigui:number-field/default", TextInputRenderer.class, numberField());
        registry.register("unigui:text-area/default", TextAreaRenderer.class, textArea());
        registry.register("unigui:shape/default", ShapeRenderer.class, shape());
        registry.register("unigui:separator/default", SeparatorRenderer.class, separator());
        registry.register("unigui:border/default", BorderRenderer.class, border());
        registry.register("unigui:tooltip/default", TooltipRenderer.class, tooltip());
        registry.register("unigui:texture-widget/default", TextureWidgetRenderer.class, textureWidget());
        registry.register("unigui:image-view/default", TextureWidgetRenderer.class, imageView());
        registry.register("unigui:path/default", PathRenderer.class, path());
        registry.register("unigui:cached-subtree/default", CachedSubtreeRenderer.class, cachedSubtree());
        registry.register("unigui:box/default", BoxRenderer.class, box());
        registry.register("unigui:window/default", WindowRenderer.class, window());
        registry.register("unigui:modal-scrim/default", ModalScrimRenderer.class, modalScrim());
        registry.register("unigui:docking-root/default", DockingRootRenderer.class, dockingRoot());
        registry.register("unigui:dock-pane/default", DockPaneRenderer.class, dockPane());
        registry.register("unigui:dock-split-handle/default", DockSplitHandleRenderer.class, dockSplitHandle());
        registry.register("unigui:dock-drop-preview/default", DockDropPreviewRenderer.class, dockDropPreview());
        registry.register("unigui:splitter/default", SplitterRenderer.class, splitter());
        registry.register("unigui:text-widget/default", TextWidgetRenderer.class, textWidget());
        registry.register("unigui:virtual-list-view/default", VirtualListViewRenderer.class, virtualListView());
        registry.register("unigui:tree-view/default", TreeViewRenderer.class, treeView());
        registry.register("unigui:virtual-table-view/default", VirtualTableViewRenderer.class, virtualTableView());
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
    public static LoadingIndicatorRenderer loadingDefault() {
        LoadingIndicatorRenderer renderer = impl.loadingDefault();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.loadingDefault() : renderer;
    }

    /**
     * Возвращает renderer для spinner loading indicator.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static LoadingIndicatorRenderer loadingSpinner() {
        LoadingIndicatorRenderer renderer = impl.loadingSpinner();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.loadingSpinner() : renderer;
    }

    /**
     * Возвращает renderer для dots loading indicator.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static LoadingIndicatorRenderer loadingDots() {
        LoadingIndicatorRenderer renderer = impl.loadingDots();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.loadingDots() : renderer;
    }

    /**
     * Возвращает renderer для bar loading indicator.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static LoadingIndicatorRenderer loadingBar() {
        LoadingIndicatorRenderer renderer = impl.loadingBar();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.loadingBar() : renderer;
    }

    /**
     * Возвращает renderer для progress bar.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static ProgressBarRenderer progressBar() {
        ProgressBarRenderer renderer = impl.progressBar();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.progressBar() : renderer;
    }

    /**
     * Возвращает renderer для slider.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static SliderRenderer slider() {
        SliderRenderer renderer = impl.slider();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.slider() : renderer;
    }

    /**
     * Возвращает renderer для sparkline.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static SparklineRenderer sparkline() {
        SparklineRenderer renderer = impl.sparkline();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.sparkline() : renderer;
    }

    /**
     * Возвращает renderer для chart.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static ChartRenderer chart() {
        ChartRenderer renderer = impl.chart();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.chart() : renderer;
    }

    /**
     * Возвращает renderer для graph view.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static GraphViewRenderer graphView() {
        GraphViewRenderer renderer = impl.graphView();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.graphView() : renderer;
    }

    /**
     * Возвращает renderer для node graph.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static NodeGraphRenderer nodeGraph() {
        NodeGraphRenderer renderer = impl.nodeGraph();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.nodeGraph() : renderer;
    }

    /**
     * Возвращает renderer для color picker.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static ColorPickerRenderer colorPicker() {
        ColorPickerRenderer renderer = impl.colorPicker();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.colorPicker() : renderer;
    }

    /**
     * Возвращает renderer для date picker.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static DatePickerRenderer datePicker() {
        DatePickerRenderer renderer = impl.datePicker();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.datePicker() : renderer;
    }

    /**
     * Возвращает renderer для scroll bar.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static ScrollBarRenderer scrollBar() {
        ScrollBarRenderer renderer = impl.scrollBar();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.scrollBar() : renderer;
    }

    /**
     * Возвращает renderer для обычной button.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static ButtonRenderer button() {
        ButtonRenderer renderer = impl.button();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.button() : renderer;
    }

    /**
     * Возвращает renderer для toggle button.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static ButtonRenderer toggleButton() {
        ButtonRenderer renderer = impl.toggleButton();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.toggleButton() : renderer;
    }

    /**
     * Возвращает renderer для toggle switch.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static ButtonRenderer toggleSwitch() {
        ButtonRenderer renderer = impl.toggleSwitch();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.toggleSwitch() : renderer;
    }

    /**
     * Возвращает renderer для checkbox.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static ButtonRenderer checkbox() {
        ButtonRenderer renderer = impl.checkbox();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.checkbox() : renderer;
    }

    /**
     * Возвращает renderer для radio button.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static ButtonRenderer radioButton() {
        ButtonRenderer renderer = impl.radioButton();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.radioButton() : renderer;
    }

    /**
     * Возвращает renderer для базового text input.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static TextInputRenderer textInput() {
        TextInputRenderer renderer = impl.textInput();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.textInput() : renderer;
    }

    /**
     * Возвращает renderer для text field.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static TextInputRenderer textField() {
        TextInputRenderer renderer = impl.textField();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.textField() : renderer;
    }

    /**
     * Возвращает renderer для search field.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static TextInputRenderer searchField() {
        TextInputRenderer renderer = impl.searchField();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.searchField() : renderer;
    }

    /**
     * Возвращает renderer для password field.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static TextInputRenderer passwordField() {
        TextInputRenderer renderer = impl.passwordField();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.passwordField() : renderer;
    }

    /**
     * Возвращает renderer для number field.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static TextInputRenderer numberField() {
        TextInputRenderer renderer = impl.numberField();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.numberField() : renderer;
    }

    /**
     * Возвращает renderer для text area.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static TextAreaRenderer textArea() {
        TextAreaRenderer renderer = impl.textArea();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.textArea() : renderer;
    }

    /**
     * Возвращает renderer для shape widget.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static ShapeRenderer shape() {
        ShapeRenderer renderer = impl.shape();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.shape() : renderer;
    }

    /**
     * Возвращает renderer для separator.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static SeparatorRenderer separator() {
        SeparatorRenderer renderer = impl.separator();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.separator() : renderer;
    }

    /**
     * Возвращает renderer для border.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static BorderRenderer border() {
        BorderRenderer renderer = impl.border();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.border() : renderer;
    }

    /**
     * Возвращает renderer для tooltip.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static TooltipRenderer tooltip() {
        TooltipRenderer renderer = impl.tooltip();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.tooltip() : renderer;
    }

    /**
     * Возвращает renderer для texture widget.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static TextureWidgetRenderer textureWidget() {
        TextureWidgetRenderer renderer = impl.textureWidget();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.textureWidget() : renderer;
    }

    /**
     * Возвращает renderer для image view.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static TextureWidgetRenderer imageView() {
        TextureWidgetRenderer renderer = impl.imageView();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.imageView() : renderer;
    }

    /**
     * Возвращает renderer для path widget.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static PathRenderer path() {
        PathRenderer renderer = impl.path();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.path() : renderer;
    }

    /**
     * Возвращает renderer для cached subtree.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static CachedSubtreeRenderer cachedSubtree() {
        CachedSubtreeRenderer renderer = impl.cachedSubtree();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.cachedSubtree() : renderer;
    }

    /**
     * Возвращает renderer для box/container.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static BoxRenderer box() {
        BoxRenderer renderer = impl.box();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.box() : renderer;
    }

    /**
     * Возвращает renderer для window.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static WindowRenderer window() {
        WindowRenderer renderer = impl.window();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.window() : renderer;
    }

    /**
     * Возвращает renderer для modal scrim.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static ModalScrimRenderer modalScrim() {
        ModalScrimRenderer renderer = impl.modalScrim();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.modalScrim() : renderer;
    }

    /**
     * Возвращает renderer для docking root.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static DockingRootRenderer dockingRoot() {
        DockingRootRenderer renderer = impl.dockingRoot();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.dockingRoot() : renderer;
    }

    /**
     * Возвращает renderer для dock pane.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static DockPaneRenderer dockPane() {
        DockPaneRenderer renderer = impl.dockPane();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.dockPane() : renderer;
    }

    /**
     * Возвращает renderer для dock split handle.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static DockSplitHandleRenderer dockSplitHandle() {
        DockSplitHandleRenderer renderer = impl.dockSplitHandle();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.dockSplitHandle() : renderer;
    }

    /**
     * Возвращает renderer для dock drop preview.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static DockDropPreviewRenderer dockDropPreview() {
        DockDropPreviewRenderer renderer = impl.dockDropPreview();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.dockDropPreview() : renderer;
    }

    /**
     * Возвращает renderer для splitter.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static SplitterRenderer splitter() {
        SplitterRenderer renderer = impl.splitter();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.splitter() : renderer;
    }

    /**
     * Возвращает renderer для text widget.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static TextWidgetRenderer textWidget() {
        TextWidgetRenderer renderer = impl.textWidget();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.textWidget() : renderer;
    }

    /**
     * Возвращает renderer для virtual list view.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static VirtualListViewRenderer virtualListView() {
        VirtualListViewRenderer renderer = impl.virtualListView();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.virtualListView() : renderer;
    }

    /**
     * Возвращает renderer для tree view.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static TreeViewRenderer treeView() {
        TreeViewRenderer renderer = impl.treeView();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.treeView() : renderer;
    }

    /**
     * Возвращает renderer для virtual table view.
     *
     * @return активный renderer с fallback на дефолтную реализацию
     */
    public static VirtualTableViewRenderer virtualTableView() {
        VirtualTableViewRenderer renderer = impl.virtualTableView();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.virtualTableView() : renderer;
    }
}

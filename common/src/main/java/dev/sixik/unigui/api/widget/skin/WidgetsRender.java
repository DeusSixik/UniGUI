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

public final class WidgetsRender {
    private static volatile WidgetsRenderImpl impl = DefaultWidgetsRenderImpl.INSTANCE;

    static {
        registerDefaults(WidgetRendererRegistry.global());
    }

    private WidgetsRender() {
    }

    public static WidgetsRenderImpl current() {
        return impl;
    }

    public static void use(WidgetsRenderImpl customImpl) {
        impl = customImpl == null ? DefaultWidgetsRenderImpl.INSTANCE : customImpl;
        registerDefaults(WidgetRendererRegistry.global());
    }


    public static void registerDefaults() {
        registerDefaults(WidgetRendererRegistry.global());
    }

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

    public static LoadingIndicatorRenderer loadingDefault() {
        LoadingIndicatorRenderer renderer = impl.loadingDefault();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.loadingDefault() : renderer;
    }

    public static LoadingIndicatorRenderer loadingSpinner() {
        LoadingIndicatorRenderer renderer = impl.loadingSpinner();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.loadingSpinner() : renderer;
    }

    public static LoadingIndicatorRenderer loadingDots() {
        LoadingIndicatorRenderer renderer = impl.loadingDots();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.loadingDots() : renderer;
    }

    public static LoadingIndicatorRenderer loadingBar() {
        LoadingIndicatorRenderer renderer = impl.loadingBar();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.loadingBar() : renderer;
    }

    public static ProgressBarRenderer progressBar() {
        ProgressBarRenderer renderer = impl.progressBar();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.progressBar() : renderer;
    }

    public static SliderRenderer slider() {
        SliderRenderer renderer = impl.slider();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.slider() : renderer;
    }

    public static SparklineRenderer sparkline() {
        SparklineRenderer renderer = impl.sparkline();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.sparkline() : renderer;
    }

    public static ChartRenderer chart() {
        ChartRenderer renderer = impl.chart();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.chart() : renderer;
    }

    public static GraphViewRenderer graphView() {
        GraphViewRenderer renderer = impl.graphView();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.graphView() : renderer;
    }

    public static NodeGraphRenderer nodeGraph() {
        NodeGraphRenderer renderer = impl.nodeGraph();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.nodeGraph() : renderer;
    }

    public static ColorPickerRenderer colorPicker() {
        ColorPickerRenderer renderer = impl.colorPicker();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.colorPicker() : renderer;
    }

    public static DatePickerRenderer datePicker() {
        DatePickerRenderer renderer = impl.datePicker();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.datePicker() : renderer;
    }

    public static ScrollBarRenderer scrollBar() {
        ScrollBarRenderer renderer = impl.scrollBar();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.scrollBar() : renderer;
    }

    public static ButtonRenderer button() {
        ButtonRenderer renderer = impl.button();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.button() : renderer;
    }

    public static ButtonRenderer toggleButton() {
        ButtonRenderer renderer = impl.toggleButton();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.toggleButton() : renderer;
    }

    public static ButtonRenderer toggleSwitch() {
        ButtonRenderer renderer = impl.toggleSwitch();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.toggleSwitch() : renderer;
    }

    public static ButtonRenderer checkbox() {
        ButtonRenderer renderer = impl.checkbox();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.checkbox() : renderer;
    }

    public static ButtonRenderer radioButton() {
        ButtonRenderer renderer = impl.radioButton();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.radioButton() : renderer;
    }

    public static TextInputRenderer textInput() {
        TextInputRenderer renderer = impl.textInput();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.textInput() : renderer;
    }

    public static TextInputRenderer textField() {
        TextInputRenderer renderer = impl.textField();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.textField() : renderer;
    }

    public static TextInputRenderer searchField() {
        TextInputRenderer renderer = impl.searchField();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.searchField() : renderer;
    }

    public static TextInputRenderer passwordField() {
        TextInputRenderer renderer = impl.passwordField();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.passwordField() : renderer;
    }

    public static TextInputRenderer numberField() {
        TextInputRenderer renderer = impl.numberField();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.numberField() : renderer;
    }

    public static TextAreaRenderer textArea() {
        TextAreaRenderer renderer = impl.textArea();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.textArea() : renderer;
    }

    public static ShapeRenderer shape() {
        ShapeRenderer renderer = impl.shape();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.shape() : renderer;
    }

    public static SeparatorRenderer separator() {
        SeparatorRenderer renderer = impl.separator();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.separator() : renderer;
    }

    public static BorderRenderer border() {
        BorderRenderer renderer = impl.border();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.border() : renderer;
    }

    public static TooltipRenderer tooltip() {
        TooltipRenderer renderer = impl.tooltip();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.tooltip() : renderer;
    }

    public static TextureWidgetRenderer textureWidget() {
        TextureWidgetRenderer renderer = impl.textureWidget();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.textureWidget() : renderer;
    }

    public static TextureWidgetRenderer imageView() {
        TextureWidgetRenderer renderer = impl.imageView();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.imageView() : renderer;
    }

    public static PathRenderer path() {
        PathRenderer renderer = impl.path();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.path() : renderer;
    }

    public static CachedSubtreeRenderer cachedSubtree() {
        CachedSubtreeRenderer renderer = impl.cachedSubtree();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.cachedSubtree() : renderer;
    }

    public static BoxRenderer box() {
        BoxRenderer renderer = impl.box();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.box() : renderer;
    }

    public static WindowRenderer window() {
        WindowRenderer renderer = impl.window();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.window() : renderer;
    }

    public static ModalScrimRenderer modalScrim() {
        ModalScrimRenderer renderer = impl.modalScrim();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.modalScrim() : renderer;
    }

    public static DockingRootRenderer dockingRoot() {
        DockingRootRenderer renderer = impl.dockingRoot();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.dockingRoot() : renderer;
    }

    public static DockPaneRenderer dockPane() {
        DockPaneRenderer renderer = impl.dockPane();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.dockPane() : renderer;
    }

    public static DockSplitHandleRenderer dockSplitHandle() {
        DockSplitHandleRenderer renderer = impl.dockSplitHandle();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.dockSplitHandle() : renderer;
    }

    public static DockDropPreviewRenderer dockDropPreview() {
        DockDropPreviewRenderer renderer = impl.dockDropPreview();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.dockDropPreview() : renderer;
    }

    public static SplitterRenderer splitter() {
        SplitterRenderer renderer = impl.splitter();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.splitter() : renderer;
    }

    public static TextWidgetRenderer textWidget() {
        TextWidgetRenderer renderer = impl.textWidget();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.textWidget() : renderer;
    }

    public static VirtualListViewRenderer virtualListView() {
        VirtualListViewRenderer renderer = impl.virtualListView();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.virtualListView() : renderer;
    }

    public static TreeViewRenderer treeView() {
        TreeViewRenderer renderer = impl.treeView();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.treeView() : renderer;
    }

    public static VirtualTableViewRenderer virtualTableView() {
        VirtualTableViewRenderer renderer = impl.virtualTableView();
        return renderer == null ? DefaultWidgetsRenderImpl.INSTANCE.virtualTableView() : renderer;
    }
}

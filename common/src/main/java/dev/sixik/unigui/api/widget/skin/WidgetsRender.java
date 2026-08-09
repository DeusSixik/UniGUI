package dev.sixik.unigui.api.widget.skin;

import dev.sixik.unigui.widgets.render.LoadingIndicatorRenderer;
import dev.sixik.unigui.widgets.render.ChartRenderer;
import dev.sixik.unigui.widgets.render.ColorPickerRenderer;
import dev.sixik.unigui.widgets.render.DatePickerRenderer;
import dev.sixik.unigui.widgets.render.DockDropPreviewRenderer;
import dev.sixik.unigui.widgets.render.DockPaneRenderer;
import dev.sixik.unigui.widgets.render.DockSplitHandleRenderer;
import dev.sixik.unigui.widgets.render.DockingRootRenderer;
import dev.sixik.unigui.widgets.render.GraphViewRenderer;
import dev.sixik.unigui.widgets.render.ModalScrimRenderer;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ProgressBarRenderer;
import dev.sixik.unigui.widgets.render.ScrollBarRenderer;
import dev.sixik.unigui.widgets.render.SliderRenderer;
import dev.sixik.unigui.widgets.render.SparklineRenderer;
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

public final class WidgetsRender {
    private static volatile WidgetsRenderImpl impl = DefaultWidgetsRenderImpl.INSTANCE;

    private WidgetsRender() {
    }

    public static WidgetsRenderImpl current() {
        return impl;
    }

    public static void use(WidgetsRenderImpl customImpl) {
        impl = customImpl == null ? DefaultWidgetsRenderImpl.INSTANCE : customImpl;
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

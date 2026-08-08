package dev.sixik.unigui.api.widget.skin;

import dev.sixik.unigui.widgets.render.LoadingIndicatorRenderer;
import dev.sixik.unigui.widgets.render.LoadingIndicatorRenderers;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ButtonRenderers;
import dev.sixik.unigui.widgets.render.ProgressBarRenderer;
import dev.sixik.unigui.widgets.render.ProgressBarRenderers;
import dev.sixik.unigui.widgets.render.ScrollBarRenderer;
import dev.sixik.unigui.widgets.render.ScrollBarRenderers;
import dev.sixik.unigui.widgets.render.SliderRenderer;
import dev.sixik.unigui.widgets.render.SliderRenderers;
import dev.sixik.unigui.widgets.render.TextInputRenderer;
import dev.sixik.unigui.widgets.render.TextInputRenderers;
import dev.sixik.unigui.widgets.render.ShapeRenderer;
import dev.sixik.unigui.widgets.render.ShapeRenderers;
import dev.sixik.unigui.widgets.render.SeparatorRenderer;
import dev.sixik.unigui.widgets.render.SeparatorRenderers;
import dev.sixik.unigui.widgets.render.BorderRenderer;
import dev.sixik.unigui.widgets.render.BorderRenderers;
import dev.sixik.unigui.widgets.render.TooltipRenderer;
import dev.sixik.unigui.widgets.render.TooltipRenderers;
import dev.sixik.unigui.widgets.render.TextureWidgetRenderer;
import dev.sixik.unigui.widgets.render.TextureWidgetRenderers;
import dev.sixik.unigui.widgets.render.PathRenderer;
import dev.sixik.unigui.widgets.render.PathRenderers;
import dev.sixik.unigui.widgets.render.CachedSubtreeRenderer;
import dev.sixik.unigui.widgets.render.CachedSubtreeRenderers;
import dev.sixik.unigui.widgets.render.BoxRenderer;
import dev.sixik.unigui.widgets.render.BoxRenderers;
import dev.sixik.unigui.widgets.render.WindowRenderer;
import dev.sixik.unigui.widgets.render.WindowRenderers;
import dev.sixik.unigui.widgets.render.SplitterRenderer;
import dev.sixik.unigui.widgets.render.SplitterRenderers;
import dev.sixik.unigui.widgets.render.TextWidgetRenderer;
import dev.sixik.unigui.widgets.render.TextWidgetRenderers;
import dev.sixik.unigui.widgets.render.VirtualListViewRenderer;
import dev.sixik.unigui.widgets.render.VirtualListViewRenderers;
import dev.sixik.unigui.widgets.render.TreeViewRenderer;
import dev.sixik.unigui.widgets.render.TreeViewRenderers;
import dev.sixik.unigui.widgets.render.VirtualTableViewRenderer;
import dev.sixik.unigui.widgets.render.VirtualTableViewRenderers;

public final class DefaultWidgetsRenderImpl implements WidgetsRenderImpl {
    public static final DefaultWidgetsRenderImpl INSTANCE = new DefaultWidgetsRenderImpl();

    private DefaultWidgetsRenderImpl() {
    }

    @Override
    public LoadingIndicatorRenderer loadingDefault() {
        return loadingSpinner();
    }

    @Override
    public LoadingIndicatorRenderer loadingSpinner() {
        return LoadingIndicatorRenderers.SPINNER;
    }

    @Override
    public LoadingIndicatorRenderer loadingDots() {
        return LoadingIndicatorRenderers.DOTS;
    }

    @Override
    public LoadingIndicatorRenderer loadingBar() {
        return LoadingIndicatorRenderers.BAR;
    }

    @Override
    public ProgressBarRenderer progressBar() {
        return ProgressBarRenderers.DEFAULT;
    }

    @Override
    public SliderRenderer slider() {
        return SliderRenderers.DEFAULT;
    }

    @Override
    public ScrollBarRenderer scrollBar() {
        return ScrollBarRenderers.DEFAULT;
    }

    @Override
    public ButtonRenderer button() {
        return ButtonRenderers.DEFAULT;
    }

    @Override
    public ButtonRenderer toggleButton() {
        return ButtonRenderers.DEFAULT;
    }

    @Override
    public ButtonRenderer checkbox() {
        return ButtonRenderers.CHECKBOX;
    }

    @Override
    public ButtonRenderer radioButton() {
        return ButtonRenderers.RADIO_BUTTON;
    }

    @Override
    public TextInputRenderer textInput() {
        return TextInputRenderers.DEFAULT;
    }

    @Override
    public TextInputRenderer textField() {
        return TextInputRenderers.DEFAULT;
    }

    @Override
    public TextInputRenderer searchField() {
        return TextInputRenderers.SEARCH_FIELD;
    }

    @Override
    public TextInputRenderer passwordField() {
        return TextInputRenderers.DEFAULT;
    }

    @Override
    public TextInputRenderer numberField() {
        return TextInputRenderers.DEFAULT;
    }

    @Override
    public ShapeRenderer shape() {
        return ShapeRenderers.DEFAULT;
    }

    @Override
    public SeparatorRenderer separator() {
        return SeparatorRenderers.DEFAULT;
    }

    @Override
    public BorderRenderer border() {
        return BorderRenderers.DEFAULT;
    }

    @Override
    public TooltipRenderer tooltip() {
        return TooltipRenderers.DEFAULT;
    }

    @Override
    public TextureWidgetRenderer textureWidget() {
        return TextureWidgetRenderers.DEFAULT;
    }

    @Override
    public TextureWidgetRenderer imageView() {
        return TextureWidgetRenderers.DEFAULT;
    }

    @Override
    public PathRenderer path() {
        return PathRenderers.DEFAULT;
    }

    @Override
    public CachedSubtreeRenderer cachedSubtree() {
        return CachedSubtreeRenderers.DEFAULT;
    }

    @Override
    public BoxRenderer box() {
        return BoxRenderers.DEFAULT;
    }

    @Override
    public WindowRenderer window() {
        return WindowRenderers.DEFAULT;
    }

    @Override
    public SplitterRenderer splitter() {
        return SplitterRenderers.DEFAULT;
    }

    @Override
    public TextWidgetRenderer textWidget() {
        return TextWidgetRenderers.DEFAULT;
    }

    @Override
    public VirtualListViewRenderer virtualListView() {
        return VirtualListViewRenderers.DEFAULT;
    }

    @Override
    public TreeViewRenderer treeView() {
        return TreeViewRenderers.DEFAULT;
    }

    @Override
    public VirtualTableViewRenderer virtualTableView() {
        return VirtualTableViewRenderers.DEFAULT;
    }
}

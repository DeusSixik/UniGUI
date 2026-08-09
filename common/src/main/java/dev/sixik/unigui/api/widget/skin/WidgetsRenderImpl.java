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
import dev.sixik.unigui.widgets.render.ProgressBarRenderer;
import dev.sixik.unigui.widgets.render.ScrollBarRenderer;
import dev.sixik.unigui.widgets.render.SliderRenderer;
import dev.sixik.unigui.widgets.render.SparklineRenderer;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
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

public interface WidgetsRenderImpl {
    default LoadingIndicatorRenderer loadingDefault() {
        return null;
    }

    default LoadingIndicatorRenderer loadingSpinner() {
        return null;
    }

    default LoadingIndicatorRenderer loadingDots() {
        return null;
    }

    default LoadingIndicatorRenderer loadingBar() {
        return null;
    }

    default ProgressBarRenderer progressBar() {
        return null;
    }

    default SliderRenderer slider() {
        return null;
    }

    default SparklineRenderer sparkline() {
        return null;
    }

    default ChartRenderer chart() {
        return null;
    }

    default GraphViewRenderer graphView() {
        return null;
    }

    default ColorPickerRenderer colorPicker() {
        return null;
    }

    default DatePickerRenderer datePicker() {
        return null;
    }

    default ScrollBarRenderer scrollBar() {
        return null;
    }

    default ButtonRenderer button() {
        return null;
    }

    default ButtonRenderer toggleButton() {
        return null;
    }

    default ButtonRenderer checkbox() {
        return null;
    }

    default ButtonRenderer radioButton() {
        return null;
    }

    default TextInputRenderer textInput() {
        return null;
    }

    default TextInputRenderer textField() {
        return null;
    }

    default TextInputRenderer searchField() {
        return null;
    }

    default TextInputRenderer passwordField() {
        return null;
    }

    default TextInputRenderer numberField() {
        return null;
    }

    default ShapeRenderer shape() {
        return null;
    }

    default SeparatorRenderer separator() {
        return null;
    }

    default BorderRenderer border() {
        return null;
    }

    default TooltipRenderer tooltip() {
        return null;
    }

    default TextureWidgetRenderer textureWidget() {
        return null;
    }

    default TextureWidgetRenderer imageView() {
        return null;
    }

    default PathRenderer path() {
        return null;
    }

    default CachedSubtreeRenderer cachedSubtree() {
        return null;
    }

    default BoxRenderer box() {
        return null;
    }

    default WindowRenderer window() {
        return null;
    }

    default ModalScrimRenderer modalScrim() {
        return null;
    }

    default DockingRootRenderer dockingRoot() {
        return null;
    }

    default DockPaneRenderer dockPane() {
        return null;
    }

    default DockSplitHandleRenderer dockSplitHandle() {
        return null;
    }

    default DockDropPreviewRenderer dockDropPreview() {
        return null;
    }

    default SplitterRenderer splitter() {
        return null;
    }

    default TextWidgetRenderer textWidget() {
        return null;
    }

    default VirtualListViewRenderer virtualListView() {
        return null;
    }

    default TreeViewRenderer treeView() {
        return null;
    }

    default VirtualTableViewRenderer virtualTableView() {
        return null;
    }
}

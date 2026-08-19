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
import dev.sixik.unigui.widgets.render.NodeGraphRenderer;
import dev.sixik.unigui.widgets.render.ModalScrimRenderer;
import dev.sixik.unigui.widgets.render.ProgressBarRenderer;
import dev.sixik.unigui.widgets.render.ScrollBarRenderer;
import dev.sixik.unigui.widgets.render.SliderRenderer;
import dev.sixik.unigui.widgets.render.SparklineRenderer;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
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

/**
 * Частичная реализация процедурных renderer'ов для набора виджетов.
 *
 * <p>Мод или приложение может реализовать только те методы, которые хочет заменить.
 * Возврат {@code null} означает: использовать renderer из {@link DefaultWidgetsRenderImpl}.
 * Публичный код обычно обращается не к этому интерфейсу напрямую, а через {@link WidgetsRender},
 * потому что facade гарантирует fallback и регистрацию стандартных renderer id.</p>
 *
 * @see WidgetsRender#use(WidgetsRenderImpl)
 */
public interface WidgetsRenderImpl {
    /**
     * Возвращает override renderer для loading indicator по умолчанию.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default LoadingIndicatorRenderer loadingDefault() {
        return null;
    }

    /**
     * Возвращает override renderer для spinner loading indicator.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default LoadingIndicatorRenderer loadingSpinner() {
        return null;
    }

    /**
     * Возвращает override renderer для dots loading indicator.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default LoadingIndicatorRenderer loadingDots() {
        return null;
    }

    /**
     * Возвращает override renderer для bar loading indicator.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default LoadingIndicatorRenderer loadingBar() {
        return null;
    }

    /**
     * Возвращает override renderer для progress bar.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default ProgressBarRenderer progressBar() {
        return null;
    }

    /**
     * Возвращает override renderer для slider.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default SliderRenderer slider() {
        return null;
    }

    /**
     * Возвращает override renderer для sparkline.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default SparklineRenderer sparkline() {
        return null;
    }

    /**
     * Возвращает override renderer для chart.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default ChartRenderer chart() {
        return null;
    }

    /**
     * Возвращает override renderer для graph view.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default GraphViewRenderer graphView() {
        return null;
    }

    /**
     * Возвращает override renderer для node graph.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default NodeGraphRenderer nodeGraph() {
        return null;
    }

    /**
     * Возвращает override renderer для color picker.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default ColorPickerRenderer colorPicker() {
        return null;
    }

    /**
     * Возвращает override renderer для date picker.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default DatePickerRenderer datePicker() {
        return null;
    }

    /**
     * Возвращает override renderer для scroll bar.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default ScrollBarRenderer scrollBar() {
        return null;
    }

    /**
     * Возвращает override renderer для обычной button.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default ButtonRenderer button() {
        return null;
    }

    /**
     * Возвращает override renderer для toggle button.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default ButtonRenderer toggleButton() {
        return null;
    }

    /**
     * Возвращает override renderer для toggle switch.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default ButtonRenderer toggleSwitch() {
        return null;
    }

    /**
     * Возвращает override renderer для checkbox.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default ButtonRenderer checkbox() {
        return null;
    }

    /**
     * Возвращает override renderer для radio button.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default ButtonRenderer radioButton() {
        return null;
    }

    /**
     * Возвращает override renderer для базового text input.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default TextInputRenderer textInput() {
        return null;
    }

    /**
     * Возвращает override renderer для text field.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default TextInputRenderer textField() {
        return null;
    }

    /**
     * Возвращает override renderer для search field.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default TextInputRenderer searchField() {
        return null;
    }

    /**
     * Возвращает override renderer для password field.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default TextInputRenderer passwordField() {
        return null;
    }

    /**
     * Возвращает override renderer для number field.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default TextInputRenderer numberField() {
        return null;
    }

    /**
     * Возвращает override renderer для text area.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default TextAreaRenderer textArea() {
        return null;
    }

    /**
     * Возвращает override renderer для shape widget.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default ShapeRenderer shape() {
        return null;
    }

    /**
     * Возвращает override renderer для separator.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default SeparatorRenderer separator() {
        return null;
    }

    /**
     * Возвращает override renderer для border.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default BorderRenderer border() {
        return null;
    }

    /**
     * Возвращает override renderer для tooltip.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default TooltipRenderer tooltip() {
        return null;
    }

    /**
     * Возвращает override renderer для texture widget.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default TextureWidgetRenderer textureWidget() {
        return null;
    }

    /**
     * Возвращает override renderer для image view.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default TextureWidgetRenderer imageView() {
        return null;
    }

    /**
     * Возвращает override renderer для path widget.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default PathRenderer path() {
        return null;
    }

    /**
     * Возвращает override renderer для cached subtree.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default CachedSubtreeRenderer cachedSubtree() {
        return null;
    }

    /**
     * Возвращает override renderer для box/container.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default BoxRenderer box() {
        return null;
    }

    /**
     * Возвращает override renderer для window.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WindowRenderer window() {
        return null;
    }

    /**
     * Возвращает override renderer для modal scrim.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default ModalScrimRenderer modalScrim() {
        return null;
    }

    /**
     * Возвращает override renderer для docking root.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default DockingRootRenderer dockingRoot() {
        return null;
    }

    /**
     * Возвращает override renderer для dock pane.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default DockPaneRenderer dockPane() {
        return null;
    }

    /**
     * Возвращает override renderer для dock split handle.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default DockSplitHandleRenderer dockSplitHandle() {
        return null;
    }

    /**
     * Возвращает override renderer для dock drop preview.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default DockDropPreviewRenderer dockDropPreview() {
        return null;
    }

    /**
     * Возвращает override renderer для splitter.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default SplitterRenderer splitter() {
        return null;
    }

    /**
     * Возвращает override renderer для text widget.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default TextWidgetRenderer textWidget() {
        return null;
    }

    /**
     * Возвращает override renderer для virtual list view.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default VirtualListViewRenderer virtualListView() {
        return null;
    }

    /**
     * Возвращает override renderer для tree view.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default TreeViewRenderer treeView() {
        return null;
    }

    /**
     * Возвращает override renderer для virtual table view.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default VirtualTableViewRenderer virtualTableView() {
        return null;
    }
}

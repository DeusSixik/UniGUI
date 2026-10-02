package dev.sixik.unigui.api.widget.skin;

import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.render.LoadingIndicatorRenderers;
import dev.sixik.unigui.widgets.render.ChartRenderers;
import dev.sixik.unigui.widgets.render.ColorPickerRenderers;
import dev.sixik.unigui.widgets.render.DatePickerRenderers;
import dev.sixik.unigui.widgets.render.DockDropPreviewRenderers;
import dev.sixik.unigui.widgets.render.DockPaneRenderers;
import dev.sixik.unigui.widgets.render.DockSplitHandleRenderers;
import dev.sixik.unigui.widgets.render.DockingRootRenderers;
import dev.sixik.unigui.widgets.render.GraphViewRenderers;
import dev.sixik.unigui.widgets.render.NodeGraphRenderers;
import dev.sixik.unigui.widgets.render.ModalScrimRenderers;
import dev.sixik.unigui.widgets.render.ButtonRenderers;
import dev.sixik.unigui.widgets.render.CheckboxRenderers;
import dev.sixik.unigui.widgets.render.HoldButtonRenderers;
import dev.sixik.unigui.widgets.render.RadioButtonRenderers;
import dev.sixik.unigui.widgets.render.ToggleButtonRenderers;
import dev.sixik.unigui.widgets.render.ToggleSwitchRenderers;
import dev.sixik.unigui.widgets.render.ToolButtonRenderers;
import dev.sixik.unigui.widgets.render.ProgressBarRenderers;
import dev.sixik.unigui.widgets.render.ScrollBarRenderers;
import dev.sixik.unigui.widgets.render.SliderRenderers;
import dev.sixik.unigui.widgets.render.SparklineRenderers;
import dev.sixik.unigui.widgets.render.TextAreaRenderers;
import dev.sixik.unigui.widgets.render.TextInputRenderers;
import dev.sixik.unigui.widgets.render.ShapeRenderers;
import dev.sixik.unigui.widgets.render.SeparatorRenderers;
import dev.sixik.unigui.widgets.render.BorderRenderers;
import dev.sixik.unigui.widgets.render.TooltipRenderers;
import dev.sixik.unigui.widgets.render.TextureWidgetRenderers;
import dev.sixik.unigui.widgets.render.PathRenderers;
import dev.sixik.unigui.widgets.render.CachedSubtreeRenderers;
import dev.sixik.unigui.widgets.render.BoxRenderers;
import dev.sixik.unigui.widgets.render.WindowRenderers;
import dev.sixik.unigui.widgets.render.SplitterRenderers;
import dev.sixik.unigui.widgets.render.TextWidgetRenderers;
import dev.sixik.unigui.widgets.render.VirtualListViewRenderers;
import dev.sixik.unigui.widgets.render.TreeViewRenderers;
import dev.sixik.unigui.widgets.render.VirtualTableViewRenderers;

/**
 * Дефолтная реализация renderer-набора UniGUI.
 *
 * <p>Класс связывает публичный facade {@link WidgetsRender} с конкретными renderer-константами
 * из {@code dev.sixik.unigui.widgets.render}. Его методы не создают новый visual state,
 * а возвращают уже готовые stateless renderer instances.</p>
 */
public final class DefaultWidgetsRenderImpl implements WidgetsRenderImpl {
    /** Единственный дефолтный instance renderer-набора. */
    public static final DefaultWidgetsRenderImpl INSTANCE = new DefaultWidgetsRenderImpl();

    /** Закрытый конструктор: используется singleton {@link #INSTANCE}. */
    private DefaultWidgetsRenderImpl() {
    }

    @Override
    public WidgetRender loadingDefault() {
        return loadingSpinner();
    }

    @Override
    public WidgetRender loadingSpinner() {
        return LoadingIndicatorRenderers.SPINNER;
    }

    @Override
    public WidgetRender loadingDots() {
        return LoadingIndicatorRenderers.DOTS;
    }

    @Override
    public WidgetRender loadingBar() {
        return LoadingIndicatorRenderers.BAR;
    }

    @Override
    public WidgetRender progressBar() {
        return ProgressBarRenderers.DEFAULT;
    }

    @Override
    public WidgetRender slider() {
        return SliderRenderers.DEFAULT;
    }

    @Override
    public WidgetRender sparkline() {
        return SparklineRenderers.DEFAULT;
    }

    @Override
    public WidgetRender chart() {
        return ChartRenderers.DEFAULT;
    }

    @Override
    public WidgetRender graphView() {
        return GraphViewRenderers.DEFAULT;
    }

    @Override
    public WidgetRender nodeGraph() {
        return NodeGraphRenderers.DEFAULT;
    }

    @Override
    public WidgetRender colorPicker() {
        return ColorPickerRenderers.DEFAULT;
    }

    @Override
    public WidgetRender datePicker() {
        return DatePickerRenderers.DEFAULT;
    }

    @Override
    public WidgetRender scrollBar() {
        return ScrollBarRenderers.DEFAULT;
    }

    @Override
    public WidgetRender button() {
        return ButtonRenderers.DEFAULT;
    }

    @Override
    public WidgetRender toggleButton() {
        return ToggleButtonRenderers.DEFAULT;
    }

    @Override
    public WidgetRender toggleSwitch() {
        return ToggleSwitchRenderers.DEFAULT;
    }

    @Override
    public WidgetRender checkbox() {
        return CheckboxRenderers.DEFAULT;
    }

    @Override
    public WidgetRender radioButton() {
        return RadioButtonRenderers.DEFAULT;
    }

    @Override
    public WidgetRender toolButton() {
        return ToolButtonRenderers.DEFAULT;
    }

    @Override
    public WidgetRender holdButton() {
        return HoldButtonRenderers.DEFAULT;
    }

    @Override
    public WidgetRender textInput() {
        return TextInputRenderers.DEFAULT;
    }

    @Override
    public WidgetRender textField() {
        return TextInputRenderers.DEFAULT;
    }

    @Override
    public WidgetRender searchField() {
        return TextInputRenderers.SEARCH_FIELD;
    }

    @Override
    public WidgetRender passwordField() {
        return TextInputRenderers.DEFAULT;
    }

    @Override
    public WidgetRender numberField() {
        return TextInputRenderers.DEFAULT;
    }

    @Override
    public WidgetRender textArea() {
        return TextAreaRenderers.DEFAULT;
    }

    @Override
    public WidgetRender shape() {
        return ShapeRenderers.DEFAULT;
    }

    @Override
    public WidgetRender separator() {
        return SeparatorRenderers.DEFAULT;
    }

    @Override
    public WidgetRender border() {
        return BorderRenderers.DEFAULT;
    }

    @Override
    public WidgetRender tooltip() {
        return TooltipRenderers.DEFAULT;
    }

    @Override
    public WidgetRender textureWidget() {
        return TextureWidgetRenderers.DEFAULT;
    }

    @Override
    public WidgetRender imageView() {
        return TextureWidgetRenderers.DEFAULT;
    }

    @Override
    public WidgetRender path() {
        return PathRenderers.DEFAULT;
    }

    @Override
    public WidgetRender cachedSubtree() {
        return CachedSubtreeRenderers.DEFAULT;
    }

    @Override
    public WidgetRender box() {
        return BoxRenderers.DEFAULT;
    }

    @Override
    public WidgetRender window() {
        return WindowRenderers.DEFAULT;
    }

    @Override
    public WidgetRender modalScrim() {
        return ModalScrimRenderers.DEFAULT;
    }

    @Override
    public WidgetRender dockingRoot() {
        return DockingRootRenderers.DEFAULT;
    }

    @Override
    public WidgetRender dockPane() {
        return DockPaneRenderers.DEFAULT;
    }

    @Override
    public WidgetRender dockSplitHandle() {
        return DockSplitHandleRenderers.DEFAULT;
    }

    @Override
    public WidgetRender dockDropPreview() {
        return DockDropPreviewRenderers.DEFAULT;
    }

    @Override
    public WidgetRender splitter() {
        return SplitterRenderers.DEFAULT;
    }

    @Override
    public WidgetRender textWidget() {
        return TextWidgetRenderers.DEFAULT;
    }

    @Override
    public WidgetRender virtualListView() {
        return VirtualListViewRenderers.DEFAULT;
    }

    @Override
    public WidgetRender treeView() {
        return TreeViewRenderers.DEFAULT;
    }

    @Override
    public WidgetRender virtualTableView() {
        return VirtualTableViewRenderers.DEFAULT;
    }
}

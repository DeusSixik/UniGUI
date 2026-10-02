package dev.sixik.unigui.api.widget.skin;

import dev.sixik.unigui.api.widget.render.WidgetRender;

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
    default WidgetRender loadingDefault() {
        return null;
    }

    /**
     * Возвращает override renderer для spinner loading indicator.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender loadingSpinner() {
        return null;
    }

    /**
     * Возвращает override renderer для dots loading indicator.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender loadingDots() {
        return null;
    }

    /**
     * Возвращает override renderer для bar loading indicator.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender loadingBar() {
        return null;
    }

    /**
     * Возвращает override renderer для progress bar.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender progressBar() {
        return null;
    }

    /**
     * Возвращает override renderer для slider.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender slider() {
        return null;
    }

    /**
     * Возвращает override renderer для sparkline.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender sparkline() {
        return null;
    }

    /**
     * Возвращает override renderer для chart.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender chart() {
        return null;
    }

    /**
     * Возвращает override renderer для graph view.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender graphView() {
        return null;
    }

    /**
     * Возвращает override renderer для node graph.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender nodeGraph() {
        return null;
    }

    /**
     * Возвращает override renderer для color picker.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender colorPicker() {
        return null;
    }

    /**
     * Возвращает override renderer для date picker.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender datePicker() {
        return null;
    }

    /**
     * Возвращает override renderer для scroll bar.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender scrollBar() {
        return null;
    }

    /**
     * Возвращает override renderer для обычной button.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender button() {
        return null;
    }

    /**
     * Возвращает override renderer для toggle button.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender toggleButton() {
        return null;
    }

    /**
     * Возвращает override renderer для toggle switch.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender toggleSwitch() {
        return null;
    }

    /**
     * Возвращает override renderer для checkbox.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender checkbox() {
        return null;
    }

    /**
     * Возвращает override renderer для radio button.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender radioButton() {
        return null;
    }

    /**
     * Возвращает override renderer для toolbar button.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender toolButton() {
        return null;
    }

    /**
     * Возвращает override renderer для hold button.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender holdButton() {
        return null;
    }

    /**
     * Возвращает override renderer для базового text input.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender textInput() {
        return null;
    }

    /**
     * Возвращает override renderer для text field.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender textField() {
        return null;
    }

    /**
     * Возвращает override renderer для search field.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender searchField() {
        return null;
    }

    /**
     * Возвращает override renderer для password field.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender passwordField() {
        return null;
    }

    /**
     * Возвращает override renderer для number field.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender numberField() {
        return null;
    }

    /**
     * Возвращает override renderer для text area.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender textArea() {
        return null;
    }

    /**
     * Возвращает override renderer для shape widget.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender shape() {
        return null;
    }

    /**
     * Возвращает override renderer для separator.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender separator() {
        return null;
    }

    /**
     * Возвращает override renderer для border.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender border() {
        return null;
    }

    /**
     * Возвращает override renderer для tooltip.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender tooltip() {
        return null;
    }

    /**
     * Возвращает override renderer для texture widget.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender textureWidget() {
        return null;
    }

    /**
     * Возвращает override renderer для image view.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender imageView() {
        return null;
    }

    /**
     * Возвращает override renderer для path widget.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender path() {
        return null;
    }

    /**
     * Возвращает override renderer для cached subtree.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender cachedSubtree() {
        return null;
    }

    /**
     * Возвращает override renderer для box/container.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender box() {
        return null;
    }

    /**
     * Возвращает override renderer для window.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender window() {
        return null;
    }

    /**
     * Возвращает override renderer для modal scrim.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender modalScrim() {
        return null;
    }

    /**
     * Возвращает override renderer для docking root.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender dockingRoot() {
        return null;
    }

    /**
     * Возвращает override renderer для dock pane.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender dockPane() {
        return null;
    }

    /**
     * Возвращает override renderer для dock split handle.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender dockSplitHandle() {
        return null;
    }

    /**
     * Возвращает override renderer для dock drop preview.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender dockDropPreview() {
        return null;
    }

    /**
     * Возвращает override renderer для splitter.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender splitter() {
        return null;
    }

    /**
     * Возвращает override renderer для text widget.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender textWidget() {
        return null;
    }

    /**
     * Возвращает override renderer для virtual list view.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender virtualListView() {
        return null;
    }

    /**
     * Возвращает override renderer для tree view.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender treeView() {
        return null;
    }

    /**
     * Возвращает override renderer для virtual table view.
     *
     * @return renderer или {@code null}, чтобы оставить дефолтный renderer
     */
    default WidgetRender virtualTableView() {
        return null;
    }
}

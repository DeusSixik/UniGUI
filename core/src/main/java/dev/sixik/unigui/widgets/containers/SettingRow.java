package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.display.Label;

/**
 * Удобная строка настройки: подпись слева и control/parameter widget справа.
 *
 * <p>{@code SettingRow} специализирует {@link PanelRowWidget}: слева всегда
 * живёт {@link Label}, а справа — один управляемый виджет. Label автоматически
 * получает focus target control'а, поэтому клики/навигация по подписи могут
 * переводить фокус на соответствующий control.</p>
 *
 * <p>Если на любой стороне нужно несколько произвольных виджетов, используй
 * {@link PanelRowWidget} напрямую.</p>
 *
 * <pre>{@code
 * SettingRow volume = new SettingRow("Volume", new Slider())
 *         .controlWidth(96.0f)
 *         .rowHeight(24.0f);
 * }</pre>
 */
@XmlWidgetName("SettingRow")
public class SettingRow extends PanelRowWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.SETTING_ROW;

    /**
     * Дефолтная высота строки настроек.
     */
    public static final float DEFAULT_ROW_HEIGHT = PanelRowWidget.DEFAULT_ROW_HEIGHT;
    /**
     * Дефолтное расстояние между подписью и control'ом.
     */
    public static final float DEFAULT_GAP = PanelRowWidget.DEFAULT_GAP;

    private final Label label = new Label();
    private Widget control;

    /**
     * Создаёт пустую строку настройки с готовым label слева.
     */
    public SettingRow() {
        left(label);
    }

    /**
     * Создаёт строку настройки с plain-text подписью и control'ом.
     *
     * @param labelText текст подписи
     * @param control виджет управления справа
     */
    public SettingRow(String labelText, Widget control) {
        this();
        labelText(labelText);
        control(control);
    }

    /**
     * Создаёт строку настройки с rich-text подписью и control'ом.
     *
     * @param labelText rich-text подпись
     * @param control виджет управления справа
     */
    public SettingRow(RichText labelText, Widget control) {
        this();
        labelText(labelText);
        control(control);
    }

    /**
     * Возвращает label, который расположен слева.
     *
     * @return live label строки
     */
    public Label label() {
        return label;
    }

    /**
     * Возвращает plain-text подпись label'а.
     *
     * @return текущий текст подписи
     */
    public String labelText() {
        return label.text();
    }

    /**
     * Задаёт plain-text подпись label'а.
     *
     * @param labelText новый текст подписи
     * @return эта строка для fluent-настройки
     */
    @XmlAttribute(value = "labelText", category = "Content", defaultValue = "", description = "Plain text shown in the setting label.")
    public SettingRow labelText(String labelText) {
        label.text(labelText);
        return this;
    }

    /**
     * Возвращает rich-text подпись label'а.
     *
     * @return текущий rich text
     */
    public RichText labelRichText() {
        return label.richText();
    }

    /**
     * Задаёт rich-text подпись label'а.
     *
     * @param labelText новый rich text
     * @return эта строка для fluent-настройки
     */
    public SettingRow labelText(RichText labelText) {
        label.richText(labelText);
        return this;
    }

    /**
     * Возвращает control, расположенный справа.
     *
     * @return текущий control или {@code null}
     */
    public Widget control() {
        return control;
    }

    /**
     * Заменяет control справа и обновляет focus target label'а.
     *
     * @param control новый control или {@code null}
     * @return эта строка для fluent-настройки
     */
    public SettingRow control(Widget control) {
        if (this.control == control) return this;
        this.control = control;
        label.focusTarget(control);
        right(control);
        return this;
    }

    @Override
    @XmlAttribute(value = "rowHeight", category = "Layout", defaultValue = "24", description = "Preferred row height or auto.")
    public SettingRow rowHeight(float rowHeight) {
        super.rowHeight(rowHeight);
        return this;
    }

    @Override
    @XmlAttribute(value = "gap", category = "Layout", defaultValue = "8", description = "Gap between label and control groups.")
    public SettingRow gap(float gap) {
        super.gap(gap);
        return this;
    }

    /**
     * Возвращает предпочитаемую ширину control-группы.
     *
     * @return ширина control'а или {@link LayoutConstraints#AUTO}
     */
    public float controlWidth() {
        return rightWidth();
    }

    /**
     * Задаёт предпочитаемую ширину control-группы.
     *
     * @param controlWidth ширина control'а или {@link LayoutConstraints#AUTO}
     * @return эта строка для fluent-настройки
     */
    @XmlAttribute(value = "controlWidth", category = "Layout", defaultValue = "auto", description = "Preferred width of the right-side control group.")
    public SettingRow controlWidth(float controlWidth) {
        rightWidth(controlWidth);
        return this;
    }

    /**
     * Alias для {@link #controlWidth(float)}.
     *
     * @param width ширина правого parameter/control блока
     * @return эта строка для fluent-настройки
     */
    public SettingRow parameterWidth(float width) {
        return controlWidth(width);
    }

    @Override
    @XmlAttribute(value = "rightWidth", category = "Layout", defaultValue = "auto", description = "Preferred width of the right widget group.")
    public SettingRow rightWidth(float rightWidth) {
        super.rightWidth(rightWidth);
        return this;
    }

    @Override
    @XmlAttribute(value = "leftWidth", category = "Layout", defaultValue = "auto", description = "Preferred width of the label group.")
    public SettingRow leftWidth(float leftWidth) {
        super.leftWidth(leftWidth);
        return this;
    }

    @Override
    @XmlAttribute(value = "leftGap", category = "Layout", defaultValue = "8", description = "Gap between widgets in the label group.")
    public SettingRow leftGap(float leftGap) {
        super.leftGap(leftGap);
        return this;
    }

    @Override
    @XmlAttribute(value = "rightGap", category = "Layout", defaultValue = "8", description = "Gap between widgets in the control group.")
    public SettingRow rightGap(float rightGap) {
        super.rightGap(rightGap);
        return this;
    }

    /**
     * Возвращает ширину control'а к auto-sizing.
     *
     * @return эта строка для fluent-настройки
     */
    public SettingRow autoControlWidth() {
        return controlWidth(LayoutConstraints.AUTO);
    }
}

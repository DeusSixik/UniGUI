package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.TextBlock;

/**
 * Компактный виджет для отображения XML-диагностик в редакторе и hot-reload preview.
 *
 * <p>Панель является обычным UniGUI widget-ом и не привязана к конкретному editor shell.
 * Ей можно передать готовую {@link XmlWidgetDiagnosticsModel} или статус hot-reload preview,
 * после чего она сама перестроит строки и подсветит состояние рамкой.</p>
 */
public final class XmlWidgetDiagnosticsPanel extends Box {
    private final VBox content = new VBox();
    private final Label title = new Label("XML Diagnostics");
    private final Label summary = new Label();
    private final VBox entries = new VBox();
    private XmlWidgetDiagnosticsModel model = XmlWidgetDiagnosticsModel.empty();
    private int entryLimit = 6;

    /**
     * Создаёт diagnostics panel с дефолтным dark styling.
     */
    public XmlWidgetDiagnosticsPanel() {
        themeEnabled(false);
        backgroundVisible(true);
        borderVisible(true);
        background().set(0.030f, 0.036f, 0.048f, 0.94f);
        borderColor().set(0.30f, 0.36f, 0.46f, 0.82f);
        borderWidth(1.0f);
        radius(4.0f);

        content.spacing(4.0f);
        content.layout(style -> style.margin(8.0f).flexGrow(1.0f).flexShrink(1.0f));

        title.color().set(0.88f, 0.93f, 1.0f, 1.0f);
        title.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        summary.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        entries.spacing(3.0f);
        entries.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));

        content.addChild(title);
        content.addChild(summary);
        content.addChild(entries);
        addChild(content);
        applyQueuedMutations();
        content.applyQueuedMutations();
        model(XmlWidgetDiagnosticsModel.empty());
    }

    /**
     * Возвращает текущую модель диагностик.
     *
     * @return diagnostics model
     */
    public XmlWidgetDiagnosticsModel model() {
        return model;
    }

    /**
     * Задаёт модель диагностик и перестраивает строки панели.
     *
     * @param model новая модель; {@code null} заменяется empty model
     * @return эта панель для fluent-настройки
     */
    public XmlWidgetDiagnosticsPanel model(XmlWidgetDiagnosticsModel model) {
        this.model = model == null ? XmlWidgetDiagnosticsModel.empty() : model;
        rebuildEntries();
        return this;
    }

    /**
     * Задаёт состояние панели из hot-reload status.
     *
     * @param status статус preview или {@code null}
     * @return эта панель для fluent-настройки
     */
    public XmlWidgetDiagnosticsPanel status(XmlWidgetHotReloadPreview.Status status) {
        return model(XmlWidgetDiagnosticsModel.from(status));
    }

    /**
     * Возвращает максимальное количество отображаемых строк.
     *
     * @return лимит visible entries
     */
    public int entryLimit() {
        return entryLimit;
    }

    /**
     * Задаёт максимальное количество отображаемых строк.
     *
     * <p>Если diagnostics больше лимита, последняя строка покажет количество скрытых сообщений.</p>
     *
     * @param entryLimit новый лимит; значения меньше 1 нормализуются в 1
     * @return эта панель для fluent-настройки
     */
    public XmlWidgetDiagnosticsPanel entryLimit(int entryLimit) {
        this.entryLimit = Math.max(1, entryLimit);
        rebuildEntries();
        return this;
    }

    /**
     * Возвращает label заголовка для дополнительной настройки theme/style.
     *
     * @return title label
     */
    public Label titleLabel() {
        return title;
    }

    /**
     * Возвращает label со сводкой diagnostics.
     *
     * @return summary label
     */
    public Label summaryLabel() {
        return summary;
    }

    /**
     * Возвращает контейнер строк diagnostics.
     *
     * @return VBox, в который панель добавляет message rows
     */
    public VBox entriesHost() {
        return entries;
    }

    private void rebuildEntries() {
        summary.text(model.summary());
        if (model.hasErrors()) {
            summary.color().set(1.0f, 0.43f, 0.43f, 1.0f);
            borderColor().set(0.84f, 0.24f, 0.28f, 0.90f);
        } else {
            summary.color().set(0.44f, 0.86f, 0.58f, 1.0f);
            borderColor().set(0.24f, 0.55f, 0.36f, 0.82f);
        }

        entries.clearChildren();
        if (model.isEmpty()) {
            entries.addChild(messageRow("OK", "No diagnostics.", false));
        } else {
            int visible = Math.min(entryLimit, model.entries().size());
            for (int i = 0; i < visible; i++) {
                XmlWidgetDiagnosticsModel.Entry entry = model.entries().get(i);
                String location = entry.locationLabel();
                String text = location.isEmpty() ? entry.message() : location + " - " + entry.message();
                entries.addChild(messageRow(entry.severity().name(), text, entry.severity() == XmlWidgetDiagnosticsModel.Severity.ERROR));
            }
            int hidden = model.entries().size() - visible;
            if (hidden > 0) {
                entries.addChild(messageRow("+", hidden + " more diagnostics...", true));
            }
        }
        entries.applyQueuedMutations();
        invalidate(dev.sixik.unigui.api.core.InvalidationFlags.LAYOUT | dev.sixik.unigui.api.core.InvalidationFlags.VISUAL);
    }

    private static HBox messageRow(String badge, String message, boolean error) {
        HBox row = new HBox();
        row.spacing(6.0f);
        row.layout(style -> style.size(LayoutConstraints.AUTO, 20.0f).flexGrow(0).flexShrink(0.0f));

        Label badgeLabel = new Label(badge);
        badgeLabel.color().set(error ? 1.0f : 0.44f, error ? 0.43f : 0.86f, error ? 0.43f : 0.58f, 1.0f);
        badgeLabel.layout(style -> style.size(44.0f, 18.0f).flexGrow(0).flexShrink(0.0f));

        TextBlock text = new TextBlock(message);
        text.wrap(false);
        text.overflowMode(TextOverflowMode.CLIP);
        text.color().set(error ? 1.0f : 0.75f, error ? 0.73f : 0.92f, error ? 0.73f : 0.78f, 1.0f);
        text.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(1.0f).flexShrink(1.0f));

        row.addChild(badgeLabel);
        row.addChild(text);
        row.applyQueuedMutations();
        return row;
    }
}

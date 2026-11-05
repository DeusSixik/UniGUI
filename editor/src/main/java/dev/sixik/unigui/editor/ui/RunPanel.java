package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.editor.style.ToolButton;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.component.Contract;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.preview.ComponentPreview;
import dev.sixik.unigui.xaml.schema.LoadContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Запуск компонента: примерные значения параметров для предпросмотра, отправка входящих событий и журнал исходящих.
 *
 * <p>Отправка и журнал работают только с копией, запущенной по Play: предпросмотр остаётся чертежом. Идентификаторы
 * виджетов {@code sample.Имя}, {@code send.Имя}, {@code send.Событие.Аргумент} стабильны и нужны тестам.</p>
 */
final class RunPanel extends SessionPanel {
    /** Идентификатор панели в каркасе. */
    static final String ID = "run";

    private final ComponentView componentView;
    private final VBox samples = column();
    private final VBox sends = column();
    private final VBox log = new VBox();
    private final Label problem = new Label("");
    private ComponentDefinition shown;
    private boolean syncing;

    RunPanel(ComponentView componentView) {
        this.componentView = componentView;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String title() {
        return "Run";
    }

    @Override
    public dev.sixik.unigui.api.render.UiIcon icon() {
        return EditorIcons.PLAY;
    }

    @Override
    public PanelSlot slot() {
        return PanelSlot.BOTTOM;
    }

    private static VBox column() {
        VBox column = new VBox();
        column.spacing(3.0f);
        column.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
        return column;
    }

    @Override
    protected Widget build(EditorContext editorContext) {
        HBox columns = new HBox();
        columns.spacing(16.0f);
        columns.layout(style -> style.widthPercent(100.0f).padding(8.0f, 6.0f, 8.0f, 8.0f));
        VBox logColumn = column();
        logColumn.addChild(title("Event log"));
        log.spacing(1.0f);
        log.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));
        logColumn.addChild(log);
        problem.color(EditorStyles.ERROR_RED);
        logColumn.addChild(problem);
        logColumn.applyQueuedMutations();
        columns.addChild(samples);
        columns.addChild(sends);
        columns.addChild(logColumn);
        columns.applyQueuedMutations();
        ScrollView scroll = new ScrollView(columns);
        EditorStyles.styleScroll(scroll);
        scroll.layout(style -> style.sizePercent(100.0f, 100.0f));
        componentView.subscribe(this::changed);
        return scroll;
    }

    @Override
    protected void bound(EditorSession next) {
        rebuild();
    }

    /** Поля пересобираются только с новым определением: иначе запись журнала сбрасывала бы набранное значение. */
    private void changed() {
        ComponentDefinition next = componentView.preview().flatMap(ComponentPreview::definition).orElse(null);
        if (next != shown) rebuild();
        else refreshLog();
    }

    private void rebuild() {
        Optional<ComponentPreview> preview = componentView.preview();
        shown = preview.flatMap(ComponentPreview::definition).orElse(null);
        samples.clearChildren();
        sends.clearChildren();
        samples.addChild(title("Preview values"));
        sends.addChild(title("Send events"));
        syncing = true;
        try {
            if (shown == null) {
                samples.addChild(EditorStyles.caption("The component does not build yet"));
            } else {
                sampleRows(preview.get());
                sendRows();
            }
        } finally {
            syncing = false;
        }
        samples.applyQueuedMutations();
        sends.applyQueuedMutations();
        EditorStyles.apply(samples);
        EditorStyles.apply(sends);
        refreshLog();
    }

    private void sampleRows(ComponentPreview preview) {
        for (Contract.Param param : shown.contract().params()) {
            if (!param.type().textual()) continue;
            Object current = preview.host().orElseThrow().instance().get(param.name());
            TextField sample = new TextField(current == null ? "" : String.valueOf(current));
            sample.id("sample." + param.name());
            sample.onTextChanged(event -> {
                if (!syncing) setSample(preview, param, sample.text());
            });
            samples.addChild(row(param.name(), sample));
        }
    }

    /** События уходят только в запущенную копию: без Play кнопки недоступны, а предпросмотр не меняется. */
    private void sendRows() {
        List<Contract.Event> incoming = shown.contract().events().stream()
                .filter(event -> event.direction() == Contract.Direction.IN).toList();
        if (incoming.isEmpty()) {
            sends.addChild(EditorStyles.caption("The component has no incoming events"));
            return;
        }
        sends.addChild(EditorStyles.caption("Available in Play mode"));
        for (Contract.Event event : incoming) {
            Map<String, TextField> values = new LinkedHashMap<>();
            for (Contract.Arg arg : event.args()) {
                if (!arg.type().textual()) continue;
                TextField value = new TextField("");
                value.id("send." + event.name() + "." + arg.name());
                value.placeholder(arg.type().name());
                values.put(arg.name(), value);
                sends.addChild(row(arg.name(), value));
            }
            ToolButton send = new ToolButton(EditorIcons.PLAY, "Send " + event.name(), ToolButton.Variant.SOLID);
            send.id("send." + event.name());
            send.layout(style -> style.widthPercent(100.0f));
            send.availableWhen(() -> componentView.running().isPresent());
            send.onClick(click -> send(event, values));
            sends.addChild(send);
        }
    }

    private void send(Contract.Event event, Map<String, TextField> values) {
        ComponentPreview running = componentView.running().orElse(null);
        if (running == null) return;
        try {
            Map<String, Object> arguments = new LinkedHashMap<>();
            for (Contract.Arg arg : event.args()) {
                TextField value = values.get(arg.name());
                if (value != null && !value.text().isEmpty()) {
                    arguments.put(arg.name(), arg.type().parse(value.text(), LoadContext.DEFAULT));
                }
            }
            running.send(event.name(), Args.of(arguments));
            problem.text("");
        } catch (RuntimeException failure) {
            problem.text(failure.getMessage());
        }
    }

    private void setSample(ComponentPreview preview, Contract.Param param, String text) {
        try {
            preview.sample(param.name(), param.type().parse(text, LoadContext.DEFAULT));
            problem.text("");
        } catch (RuntimeException failure) {
            problem.text("Sample " + param.name() + ": " + failure.getMessage());
        }
    }

    private void refreshLog() {
        log.clearChildren();
        ComponentPreview running = componentView.running().orElse(null);
        List<String> lines = running == null ? List.of() : running.events();
        if (lines.isEmpty()) log.addChild(EditorStyles.caption(running == null ? "Press Play to see events" : "No events yet"));
        for (String line : lines) log.addChild(new Label(line));
        log.applyQueuedMutations();
    }

    private static HBox row(String name, TextField field) {
        HBox row = new HBox();
        row.spacing(4.0f);
        row.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));
        Label label = new Label(name);
        label.color(EditorStyles.TEXT_LABEL);
        label.layout(style -> style.width(60.0f).height(EditorStyles.ROW_HEIGHT).flexShrink(0.0f));
        field.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
        row.addChild(label);
        row.addChild(field);
        row.applyQueuedMutations();
        return row;
    }

    private static Label title(String text) {
        Label label = new Label(text);
        label.color(EditorStyles.TEXT_BOLD);
        return label;
    }
}

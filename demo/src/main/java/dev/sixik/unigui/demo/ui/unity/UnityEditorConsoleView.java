package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;

import java.util.ArrayList;
import java.util.List;

/**
 * Панель Console в стиле редактора Unity:
 * кнопки управления (Clear, Collapse, Clear on Play, Error Pause),
 * счетчики сообщений (ℹ Info, ⚠ Warning, ⛔ Error),
 * список записей журнала с возможностью выбора
 * и область просмотра подробностей/стектрейса выбранной ошибки.
 */
public class UnityEditorConsoleView extends LinearBox {
    public enum LogType {
        INFO,
        WARNING,
        ERROR
    }

    public record LogEntry(LogType type, String message, String stackTrace, String time) {}

    private final List<LogEntry> entries = new ArrayList<>();
    private final VBox logListContainer = new VBox();
    private final Label detailMessage = new Label("");
    private final Label detailStack = new Label("");

    private final Button infoCountBtn = new Button("ℹ 3");
    private final Button warnCountBtn = new Button("⚠ 1");
    private final Button errorCountBtn = new Button("⛔ 0");

    private int selectedIndex = 0;
    private boolean clearOnPlay = true;

    public UnityEditorConsoleView() {
        super(Orientation.VERTICAL);
        spacing(0.0f);
        layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));

        // 1. Панель инструментов Console
        HBox toolbar = new HBox();
        toolbar.spacing(4.0f);
        toolbar.layout(style -> style.widthPercent(100.0f).height(22.0f)
                .padding(4.0f, 0.0f, 4.0f, 0.0f).alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        Button clearBtn = createToolBtn("Clear");
        clearBtn.onClick(e -> clear());
        toolbar.addChild(clearBtn);

        toolbar.addChild(createToolBtn("Collapse"));

        Button copBtn = createToolBtn("Clear on Play");
        copBtn.onClick(e -> {
            clearOnPlay = !clearOnPlay;
            copBtn.background().set(clearOnPlay ? UnityStyles.BG_BUTTON_ACTIVE : UnityStyles.BG_INPUT);
        });
        toolbar.addChild(copBtn);

        toolbar.addChild(createToolBtn("Error Pause"));

        Box spacer = new Box();
        spacer.themeEnabled(false);
        spacer.layout(style -> style.flexGrow(1.0f).height(1.0f));
        toolbar.addChild(spacer);

        // Счетчики
        UnityStyles.styleButton(infoCountBtn);
        infoCountBtn.layout(style -> style.height(18.0f).padding(5.0f, 0.0f));
        toolbar.addChild(infoCountBtn);

        UnityStyles.styleButton(warnCountBtn);
        warnCountBtn.layout(style -> style.height(18.0f).padding(5.0f, 0.0f));
        toolbar.addChild(warnCountBtn);

        UnityStyles.styleButton(errorCountBtn);
        errorCountBtn.layout(style -> style.height(18.0f).padding(5.0f, 0.0f));
        toolbar.addChild(errorCountBtn);

        addChild(toolbar);
        addChild(UnityStyles.separatorLine());

        // 2. Список логов в ScrollView
        logListContainer.spacing(1.0f);
        logListContainer.layout(style -> style.widthPercent(100.0f));
        ScrollView scroll = new ScrollView(logListContainer);
        scroll.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));
        addChild(scroll);

        addChild(UnityStyles.separatorLine());

        // 3. Область подробностей выбранного лога внизу
        VBox detailBox = new VBox();
        detailBox.themeEnabled(false);
        detailBox.backgroundVisible(true);
        detailBox.background().set(UnityStyles.BG_WINDOW);
        detailBox.spacing(2.0f);
        detailBox.layout(style -> style.widthPercent(100.0f).height(50.0f)
                .padding(6.0f, 4.0f, 6.0f, 4.0f).flexGrow(0.0f).flexShrink(0.0f));

        detailMessage.color(UnityStyles.TEXT_PRIMARY);
        detailMessage.layout(style -> style.widthPercent(100.0f).height(18.0f));
        detailBox.addChild(detailMessage);

        detailStack.color(UnityStyles.TEXT_MUTED);
        detailStack.layout(style -> style.widthPercent(100.0f).height(18.0f));
        detailBox.addChild(detailStack);

        addChild(detailBox);

        // Заполнить начальные записи (как в редакторе Unity)
        addLog(LogType.WARNING,
                "Deferred and MultiSampleAntiAliasing is not supported. This camera will render without MSAA buffers.",
                "UnityEngine.Camera:Render()\nUnityEditor.Handles:DrawCamera(Rect, Camera)",
                "22:15:02");
        addLog(LogType.INFO,
                "[Scene] Loaded scene 'SampleScene' in 0.042 seconds.",
                "UnityEngine.SceneManagement.SceneManager:LoadScene(String)",
                "22:15:00");
        addLog(LogType.INFO,
                "[UniGUI] Docking system initialized with 6 view panes.",
                "dev.sixik.unigui.widgets.docking.DockingRoot:init()",
                "22:15:00");
        addLog(LogType.INFO,
                "[Physics] 3D Physics engine synchronized with 60 Hz tickrate.",
                "UnityEngine.Physics:Simulate()",
                "22:14:59");

        selectEntry(0);
    }

    public void addLog(LogType type, String message, String stackTrace, String time) {
        entries.add(new LogEntry(type, message, stackTrace, time));
        rebuildRows();
        updateCounters();
    }

    public void onPlayModeChanged(boolean playing) {
        if (playing) {
            if (clearOnPlay) {
                clear();
            }
            addLog(LogType.INFO, "[PlayMode] Starting play-mode session...", "UnityEditor.EditorApplication:Play()", "now");
        } else {
            addLog(LogType.INFO, "[PlayMode] Play-mode session ended.", "UnityEditor.EditorApplication:Stop()", "now");
        }
    }

    public void clear() {
        entries.clear();
        logListContainer.clearChildren();
        detailMessage.text("");
        detailStack.text("");
        updateCounters();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void updateCounters() {
        int info = 0, warn = 0, error = 0;
        for (LogEntry e : entries) {
            switch (e.type) {
                case INFO -> info++;
                case WARNING -> warn++;
                case ERROR -> error++;
            }
        }
        infoCountBtn.text("ℹ " + info);
        warnCountBtn.text("⚠ " + warn);
        errorCountBtn.text("⛔ " + error);
    }

    private void rebuildRows() {
        logListContainer.clearChildren();
        for (int i = 0; i < entries.size(); i++) {
            final int idx = i;
            LogEntry entry = entries.get(i);

            HBox row = new HBox();
            row.themeEnabled(false);
            row.borderVisible(false);
            row.spacing(6.0f);
            row.layout(style -> style.widthPercent(100.0f).height(20.0f)
                    .padding(6.0f, 0.0f, 6.0f, 0.0f).alignItems(Align.CENTER));

            boolean selected = (idx == selectedIndex);
            row.backgroundVisible(selected);
            if (selected) {
                row.background().set(UnityStyles.SELECTION_BLUE);
            }

            dev.sixik.unigui.api.widget.Widget iconWidget = switch (entry.type) {
                case WARNING -> UnityIcons.warningIcon(12.0f);
                case ERROR -> {
                    Label l = new Label("⛔");
                    l.layout(style -> style.size(12.0f, 12.0f));
                    yield l;
                }
                case INFO -> {
                    Label l = new Label("ℹ");
                    l.color(UnityStyles.TEXT_MUTED);
                    l.layout(style -> style.size(12.0f, 12.0f));
                    yield l;
                }
            };
            row.addChild(iconWidget);

            Label msgLabel = new Label(entry.message);
            msgLabel.color(selected ? UnityStyles.TEXT_BOLD : UnityStyles.TEXT_PRIMARY);
            msgLabel.layout(style -> style.flexGrow(1.0f).height(18.0f));
            row.addChild(msgLabel);

            Label timeLabel = new Label(entry.time);
            timeLabel.color(UnityStyles.TEXT_MUTED);
            timeLabel.layout(style -> style.height(18.0f).padding(4.0f, 0.0f));
            row.addChild(timeLabel);

            row.on(PointerPressedEvent.TYPE, e -> {
                if (e.button() == PointerButton.PRIMARY) {
                    selectEntry(idx);
                    e.cancel();
                }
            });

            logListContainer.addChild(row);
        }
    }

    private void selectEntry(int index) {
        if (index < 0 || index >= entries.size()) return;
        this.selectedIndex = index;
        LogEntry entry = entries.get(index);
        detailMessage.text(entry.message);
        detailStack.text(entry.stackTrace);
        rebuildRows();
    }

    private static Button createToolBtn(String label) {
        Button btn = new Button(label);
        UnityStyles.styleButton(btn);
        btn.focusRingVisible(false);
        btn.focusable(false);
        btn.layout(style -> style.height(18.0f).padding(5.0f, 0.0f));
        return btn;
    }
}

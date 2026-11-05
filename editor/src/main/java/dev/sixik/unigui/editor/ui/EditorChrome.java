package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.editor.style.ToolButton;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.xaml.UiDiagnostic;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.EditorSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Общие части окна редакторов: знак в строке меню, действия с документом, переключатель раскладок и строка состояния.
 */
final class EditorChrome {
    private EditorChrome() {
    }

    /** @return квадратный знак редактора в начале строки меню */
    static Widget logo(EditorShell shell) {
        CanvasWidget logo = new CanvasWidget();
        logo.layout(style -> style.size(16.0f, 16.0f).flexShrink(0.0f));
        logo.onDraw(draw -> {
            var b = logo.layoutBounds();
            draw.addRectFilled(b.x(), b.y(), b.width(), b.height(), 3.0f, EditorStyles.TAB_ACCENT);
            draw.addRectFilled(b.x() + 4.0f, b.y() + 4.0f, 3.0f, 8.0f, 0.5f, EditorStyles.TEXT_BOLD);
            draw.addRectFilled(b.x() + 9.0f, b.y() + 4.0f, 3.0f, 5.0f, 0.5f, EditorStyles.TEXT_BOLD);
        });
        return logo;
    }

    /** @return сохранение и история документа */
    static Widget documentActions(EditorShell shell) {
        HBox group = group(1.0f);
        group.addChild(shell.commandButton("file.save", EditorIcons.SAVE));
        group.addChild(divider());
        group.addChild(shell.commandButton("edit.undo", EditorIcons.UNDO));
        group.addChild(shell.commandButton("edit.redo", EditorIcons.REDO));
        group.applyQueuedMutations();
        return group;
    }

    /** @return сегменты по числу раскладок; выбранная подсвечена */
    static Widget layoutSwitch(EditorShell shell) {
        HBox group = group(2.0f);
        List<ToolButton> buttons = new ArrayList<>();
        for (var layout : shell.layouts()) {
            boolean code = layout.name().equals(UiEditor.CODE_LAYOUT);
            ToolButton button = new ToolButton(code ? EditorIcons.CODE : EditorIcons.VIEWPORT, layout.name(),
                    ToolButton.Variant.GHOST);
            button.id("toolbar.layout." + layout.name());
            button.active(layout.name().equals(shell.currentLayout()));
            button.onClick(click -> {
                shell.applyLayout(layout.name());
                for (ToolButton other : buttons) other.active(other == button);
            });
            buttons.add(button);
            group.addChild(button);
        }
        group.applyQueuedMutations();
        return group;
    }

    /** @return путь к выбранному узлу */
    static Widget selectionStatus(EditorShell shell) {
        Label path = EditorStyles.caption("");
        path.id("status.selection");
        Follower.follow(shell.context(), () -> {
            EditorSession session = UiEditor.sessionOf(shell.context());
            path.text(session == null ? "No document" : session.selected().map(node -> pathOf(session, node))
                    .orElse("Nothing selected"));
        });
        return path;
    }

    /** @return число проблем и состояние сохранения */
    static Widget documentStatus(EditorShell shell) {
        HBox group = group(12.0f);
        StatusBadge problems = new StatusBadge();
        Label state = EditorStyles.caption("");
        state.id("status.document");
        group.addChild(problems.widget());
        group.addChild(state);
        group.applyQueuedMutations();
        Follower.follow(shell.context(), () -> {
            EditorSession session = UiEditor.sessionOf(shell.context());
            List<UiDiagnostic> found = session == null ? List.of() : session.allDiagnostics();
            problems.show(found.size());
            boolean dirty = shell.context().document().map(document -> document.dirty()).orElse(false);
            state.text(session == null ? "" : dirty ? "Modified" : "Saved");
        });
        return group;
    }

    private static String pathOf(EditorSession session, UiNode target) {
        List<String> names = new ArrayList<>();
        collect(session.document().root(), target, names);
        return String.join("  ›  ", names);
    }

    private static boolean collect(UiNode node, UiNode target, List<String> names) {
        names.add(node.attributeText("id").filter(id -> !id.isBlank()).orElse(node.type().local()));
        if (node == target) return true;
        for (UiNode child : node.children()) if (collect(child, target, names)) return true;
        names.remove(names.size() - 1);
        return false;
    }

    private static HBox group(float spacing) {
        HBox group = new HBox();
        group.spacing(spacing);
        group.layout(style -> style.alignItems(Align.CENTER).flexShrink(0.0f));
        return group;
    }

    /** @return тонкая вертикальная черта между группами кнопок */
    static Box divider() {
        Box line = new Box();
        line.themeEnabled(false);
        line.backgroundVisible(true);
        line.background().set(EditorStyles.BORDER_SUBTLE);
        line.borderVisible(false);
        line.layout(style -> style.size(1.0f, 12.0f).margin(4.0f, 0.0f).flexShrink(0.0f));
        return line;
    }

    /** Значок с числом проблем: зелёная галочка, пока их нет. */
    private static final class StatusBadge {
        private final HBox row = group(4.0f);
        private final CanvasWidget mark = new CanvasWidget();
        private final Label text = EditorStyles.caption("");
        private int count;

        StatusBadge() {
            mark.layout(style -> style.size(10.0f, 10.0f).flexShrink(0.0f));
            mark.onDraw(draw -> {
                var b = mark.layoutBounds();
                ColorView tint = count == 0 ? EditorStyles.PLAY_GREEN : EditorStyles.ERROR_RED;
                (count == 0 ? EditorIcons.CHECK : EditorIcons.ERROR).draw(EditorStyles.scope(draw, mark), b.x(), b.y(),
                        b.width(), tint);
            });
            text.id("status.problems");
            row.addChild(mark);
            row.addChild(text);
            row.applyQueuedMutations();
        }

        Widget widget() {
            return row;
        }

        void show(int value) {
            count = value;
            text.text(value == 0 ? "No problems" : value + (value == 1 ? " problem" : " problems"));
            mark.redraw();
        }
    }

    /** Вызывает обновление при смене документа и при любом изменении его сессии. */
    private static final class Follower {
        private Follower() {
        }

        static void follow(EditorContext context, Runnable refresh) {
            dev.sixik.unigui.api.event.EventSubscription[] current = new dev.sixik.unigui.api.event.EventSubscription[1];
            context.events().on(EditorContext.DocumentChanged.class, event -> {
                if (current[0] != null) current[0].unsubscribe();
                current[0] = null;
                EditorSession session = UiEditor.sessionOf(context);
                if (session != null) current[0] = session.onChanged(change -> refresh.run());
                refresh.run();
            });
            refresh.run();
        }
    }
}

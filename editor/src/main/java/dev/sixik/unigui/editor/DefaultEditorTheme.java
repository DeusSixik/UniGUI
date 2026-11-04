package dev.sixik.unigui.editor;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.docking.DockTheme;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.interaction.Button;

/**
 * Нейтральная тёмная тема каркаса. Использует тему докинга {@link DockTheme#editor()} и обычные стили виджетов.
 */
final class DefaultEditorTheme implements EditorTheme {
    private static final MutableColor WORKSPACE = MutableColor.rgba255(32, 32, 36, 255);
    private static final MutableColor SEPARATOR = MutableColor.rgba255(18, 18, 20, 255);
    private static final MutableColor TEXT = MutableColor.rgba255(224, 224, 228, 255);
    private static final MutableColor TEXT_MUTED = MutableColor.rgba255(150, 150, 158, 255);
    private static final MutableColor MENU_BACKGROUND = MutableColor.rgba255(36, 36, 40, 255);
    private static final MutableColor MENU_BORDER = MutableColor.rgba255(60, 60, 66, 255);

    @Override
    public MutableColor workspaceBackground() {
        return WORKSPACE;
    }

    @Override
    public void styleDock(DockingRoot dock) {
        dock.theme(DockTheme.editor());
        dock.tearOffOnDragOut(true);
    }

    @Override
    public Widget separator() {
        Box line = new Box();
        line.themeEnabled(false);
        line.backgroundVisible(true);
        line.background().set(SEPARATOR);
        line.borderVisible(false);
        line.layout(style -> style.widthPercent(100.0f).height(1.0f).flexGrow(0.0f).flexShrink(0.0f));
        return line;
    }

    @Override
    public Button menuButton(String text) {
        Button button = new Button(text);
        button.themeEnabled(false);
        button.boxVisualEnabled(false);
        button.focusRingVisible(false);
        button.focusable(false);
        button.textColor().set(TEXT);
        button.layout(style -> style.height(20.0f).padding(6.0f, 0.0f));
        return button;
    }

    @Override
    public Widget title(String text) {
        Label label = new Label(text);
        label.color(TEXT_MUTED);
        label.layout(style -> style.height(20.0f).padding(16.0f, 0.0f));
        return label;
    }

    @Override
    public float menuBarHeight() {
        return 22.0f;
    }

    @Override
    public float toolbarHeight() {
        return 28.0f;
    }

    @Override
    public float menuWidth() {
        return 160.0f;
    }

    @Override
    public void styleMenu(ContextMenu menu) {
        menu.themeEnabled(false);
        menu.backgroundVisible(true);
        menu.background().set(MENU_BACKGROUND);
        menu.borderVisible(true);
        menu.borderColor().set(MENU_BORDER);
        menu.borderWidth(1.0f);
        menu.radius(3.0f);
        menu.padding(EdgeInsets.all(3.0f));
    }

    @Override
    public void addMenuItem(ContextMenu menu, String text, String shortcut, boolean enabled, float width,
                            Runnable action) {
        String label = shortcut == null || shortcut.isEmpty() ? text : text + "    " + shortcut;
        Button item = new Button(label);
        item.themeEnabled(false);
        item.enabled(enabled);
        item.focusRingVisible(false);
        item.textColor().set(enabled ? TEXT : TEXT_MUTED);
        if (action != null) item.onClick(event -> action.run());
        menu.item(item);
        item.layout(style -> style.size(width, 19.0f).alignItems(Align.CENTER).flexGrow(0.0f).flexShrink(0.0f));
    }

    @Override
    public void addMenuSeparator(ContextMenu menu, float width) {
        menu.separator();
    }
}

package dev.sixik.unigui.editor;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.docking.DockColor;
import dev.sixik.unigui.widgets.docking.DockTheme;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.interaction.Button;

/**
 * Тема каркаса редакторов по умолчанию. Весь вид берётся из {@link EditorStyles}.
 */
final class DefaultEditorTheme implements EditorTheme {
    private static final float TAB_HEIGHT = 22.0f;
    private static final float SPLIT = 4.0f;

    @Override
    public MutableColor workspaceBackground() {
        return EditorStyles.BG_WORKSPACE;
    }

    @Override
    public void styleDock(DockingRoot dock) {
        DockTheme theme = DockTheme.editor();
        theme.color(DockColor.STRIP_BACKGROUND).set(EditorStyles.BG_TAB_INACTIVE);
        theme.color(DockColor.STRIP_BORDER).set(EditorStyles.BG_TAB_INACTIVE);
        theme.color(DockColor.TAB_BACKGROUND).set(EditorStyles.BG_TAB_INACTIVE);
        theme.color(DockColor.TAB_HOVER).set(EditorStyles.BG_PANEL);
        theme.color(DockColor.TAB_PRESSED).set(EditorStyles.BG_TAB_ACTIVE);
        theme.color(DockColor.TAB_SEPARATOR).set(EditorStyles.BG_TAB_INACTIVE);
        theme.color(DockColor.TAB_SELECTED).set(EditorStyles.BG_TAB_ACTIVE);
        theme.color(DockColor.TAB_SELECTED_INACTIVE).set(EditorStyles.BG_TAB_ACTIVE);
        theme.color(DockColor.TAB_ACCENT).set(EditorStyles.TAB_ACCENT);
        theme.color(DockColor.TAB_ACCENT_INACTIVE).set(EditorStyles.BORDER_BUTTON);
        theme.color(DockColor.TAB_TEXT).set(EditorStyles.TEXT_PRIMARY);
        theme.color(DockColor.TAB_TEXT_MUTED).set(EditorStyles.TEXT_MUTED);
        theme.color(DockColor.CONTENT_BACKGROUND).set(EditorStyles.BG_WINDOW);
        theme.color(DockColor.CLOSE_GLYPH).set(EditorStyles.TEXT_MUTED);
        theme.color(DockColor.CLOSE_GLYPH_HOVER).set(EditorStyles.TEXT_BOLD);
        theme.color(DockColor.CLOSE_HOVER_BACKGROUND).set(EditorStyles.BG_BUTTON_HOVER);
        // Панели лежат островами: разделитель того же цвета, что и фон под ними, поэтому читается как зазор.
        theme.color(DockColor.PANE_BORDER).set(EditorStyles.BG_WORKSPACE);
        theme.color(DockColor.SPLIT).set(EditorStyles.BG_WORKSPACE);
        theme.color(DockColor.SPLIT_HOVER).set(EditorStyles.TAB_ACCENT);
        theme.color(DockColor.SPLIT_PRESSED).set(EditorStyles.TAB_ACCENT);
        theme.splitStyle(DockingRoot.SplitHandleStyle.SEAM);
        theme.splitThickness(SPLIT);
        theme.tabHeight(TAB_HEIGHT);
        theme.iconSize(11.0f);
        theme.closeSize(11.0f);
        theme.tabPaddingX(7.0f);
        theme.tabRadius(0.0f);
        theme.accentThickness(1.5f);

        dock.theme(theme);
        dock.tabHeight(TAB_HEIGHT);
        dock.splitHandleThickness(SPLIT);
        dock.splitHandleStyle(DockingRoot.SplitHandleStyle.SEAM);
        dock.tearOffOnDragOut(true);
    }

    @Override
    public Widget separator() {
        return EditorStyles.separatorLine();
    }

    @Override
    public Button menuButton(String text) {
        Button button = new Button(text);
        button.themeEnabled(false);
        button.boxVisualEnabled(false);
        button.focusRingVisible(false);
        button.focusable(false);
        button.textColor().set(EditorStyles.TEXT_PRIMARY);
        button.textPaddingX(7.0f);
        button.hoverBackground().set(EditorStyles.BG_HEADER);
        button.layout(style -> style.height(20.0f));
        return button;
    }

    @Override
    public Widget title(String text) {
        Label title = new Label(text);
        title.color(EditorStyles.TEXT_MUTED);
        title.layout(style -> style.height(20.0f).padding(16.0f, 0.0f));
        return title;
    }

    @Override
    public float menuBarHeight() {
        return 26.0f;
    }

    @Override
    public float toolbarHeight() {
        return 28.0f;
    }

    @Override
    public float menuWidth() {
        return 170.0f;
    }

    @Override
    public void styleMenu(ContextMenu menu) {
        EditorStyles.styleContextMenu(menu);
    }

    @Override
    public void addMenuItem(ContextMenu menu, String text, String shortcut, boolean enabled, float width,
                            Runnable action) {
        EditorStyles.addContextMenuItem(menu, text, shortcut == null || shortcut.isEmpty() ? null : shortcut,
                enabled, width, action);
    }

    @Override
    public void addMenuSeparator(ContextMenu menu, float width) {
        EditorStyles.addContextMenuSeparator(menu, width);
    }
}

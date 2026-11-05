package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorTheme;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.docking.DockColor;
import dev.sixik.unigui.widgets.docking.DockTheme;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.interaction.Button;

/**
 * Тема каркаса редакторов в стиле Unity: цвета, вкладки, меню. Весь вид берётся из {@link UnityStyles}, поэтому
 * сам каркас остаётся нейтральным.
 */
public final class UnityEditorTheme implements EditorTheme {
    @Override
    public MutableColor workspaceBackground() {
        return UnityStyles.BG_WORKSPACE;
    }

    @Override
    public void styleDock(DockingRoot dock) {
        DockTheme theme = DockTheme.editor();
        theme.color(DockColor.STRIP_BACKGROUND).set(UnityStyles.BG_WORKSPACE);
        theme.color(DockColor.STRIP_BORDER).set(UnityStyles.BORDER_DEFAULT);
        theme.color(DockColor.TAB_BACKGROUND).set(UnityStyles.BG_TAB_INACTIVE);
        theme.color(DockColor.TAB_HOVER).set(UnityStyles.BG_TAB_ACTIVE);
        theme.color(DockColor.TAB_SELECTED).set(UnityStyles.BG_TAB_ACTIVE);
        theme.color(DockColor.TAB_SELECTED_INACTIVE).set(UnityStyles.BG_TAB_INACTIVE);
        theme.color(DockColor.TAB_ACCENT).set(UnityStyles.TAB_ACCENT);
        theme.color(DockColor.TAB_ACCENT_INACTIVE).set(UnityStyles.TAB_ACCENT);
        theme.color(DockColor.TAB_TEXT).set(UnityStyles.TEXT_PRIMARY);
        theme.color(DockColor.TAB_TEXT_MUTED).set(UnityStyles.TEXT_MUTED);
        theme.color(DockColor.CONTENT_BACKGROUND).set(UnityStyles.BG_WINDOW);
        theme.color(DockColor.PANE_BORDER).set(UnityStyles.BORDER_DEFAULT);
        theme.color(DockColor.SPLIT).set(UnityStyles.BORDER_DEFAULT);
        theme.color(DockColor.SPLIT_HOVER).set(UnityStyles.SELECTION_BLUE);
        theme.color(DockColor.SPLIT_PRESSED).set(UnityStyles.SELECTION_BLUE);
        theme.splitStyle(DockingRoot.SplitHandleStyle.SEAM);
        theme.splitThickness(3.0f);
        theme.tabHeight(22.0f);

        dock.theme(theme);
        dock.tabHeight(22.0f);
        dock.splitHandleThickness(3.0f);
        dock.splitHandleStyle(DockingRoot.SplitHandleStyle.SEAM);
        dock.tearOffOnDragOut(true);
    }

    @Override
    public Widget separator() {
        return UnityStyles.separatorLine();
    }

    @Override
    public Button menuButton(String text) {
        Button button = new Button(text);
        button.themeEnabled(false);
        button.boxVisualEnabled(false);
        button.focusRingVisible(false);
        button.focusable(false);
        button.textColor().set(UnityStyles.TEXT_PRIMARY);
        button.layout(style -> style.height(20.0f).padding(5.0f, 0.0f));
        return button;
    }

    @Override
    public Widget title(String text) {
        Label title = new Label(text);
        title.color(UnityStyles.TEXT_MUTED);
        title.layout(style -> style.height(20.0f).padding(16.0f, 0.0f));
        return title;
    }

    @Override
    public float menuBarHeight() {
        return 22.0f;
    }

    @Override
    public float toolbarHeight() {
        return 30.0f;
    }

    @Override
    public float menuWidth() {
        return 145.0f;
    }

    @Override
    public void styleMenu(ContextMenu menu) {
        UnityStyles.styleContextMenu(menu);
    }

    @Override
    public void addMenuItem(ContextMenu menu, String text, String shortcut, boolean enabled, float width,
                            Runnable action) {
        UnityStyles.addContextMenuItem(menu, text, shortcut == null || shortcut.isEmpty() ? null : shortcut,
                enabled, width, action);
    }

    @Override
    public void addMenuSeparator(ContextMenu menu, float width) {
        UnityStyles.addContextMenuSeparator(menu, width);
    }
}

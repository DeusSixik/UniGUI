package dev.sixik.unigui.editor;

import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.ToolButton;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;

import java.util.List;
import java.util.function.Supplier;

/**
 * Контекстные меню панелей: действия над строкой (очистить, удалить, дублировать) вместо кнопки на каждой строке.
 *
 * <p>Меню открывается правой кнопкой по строке или кнопкой «⋯» и оформляется темой редактора. Слой оверлея и тема
 * берутся из контекста, который их предоставляет {@link EditorShell}.</p>
 */
public final class ContextMenus {
    private ContextMenus() {
    }

    /**
     * Пункт меню.
     *
     * @param title   подпись; {@code null} означает разделитель
     * @param enabled доступен ли пункт
     * @param action  действие
     */
    public record Entry(String title, boolean enabled, Runnable action) {
        /**
         * @param title  подпись
         * @param action действие
         * @return доступный пункт
         */
        public static Entry of(String title, Runnable action) {
            return new Entry(title, true, action);
        }

        /**
         * @param title   подпись
         * @param enabled доступность
         * @param action  действие
         * @return пункт
         */
        public static Entry of(String title, boolean enabled, Runnable action) {
            return new Entry(title, enabled, action);
        }

        /** @return разделитель */
        public static Entry separator() {
            return new Entry(null, false, null);
        }
    }

    /**
     * Строит меню по теме редактора.
     *
     * @param context контекст редактора
     * @param entries пункты
     * @return меню, не добавленное ни в какой слой
     */
    public static ContextMenu build(EditorContext context, List<Entry> entries) {
        EditorTheme theme = context.require(EditorTheme.class);
        ContextMenu menu = new ContextMenu();
        theme.styleMenu(menu);
        float width = theme.menuWidth();
        for (Entry entry : entries) {
            if (entry.title() == null) {
                theme.addMenuSeparator(menu, width);
            } else {
                theme.addMenuItem(menu, entry.title(), "", entry.enabled(), width, () -> {
                    menu.close();
                    if (entry.action() != null) entry.action().run();
                });
            }
        }
        return menu;
    }

    /**
     * Открывает меню в точке.
     *
     * @param context контекст редактора
     * @param x       X в координатах окна
     * @param y       Y в координатах окна
     * @param entries пункты; пустой список ничего не открывает
     */
    public static void open(EditorContext context, float x, float y, List<Entry> entries) {
        if (entries.isEmpty()) return;
        OverlayLayer overlay = context.require(OverlayLayer.class);
        ContextMenu menu = build(context, entries);
        overlay.addOverlay(menu);
        overlay.applyQueuedMutations();
        menu.openAt(x, y);
    }

    /**
     * Открывает меню по правой кнопке на виджете. Пункты вычисляются при каждом открытии.
     *
     * @param target  виджет, по которому щёлкают
     * @param context контекст редактора
     * @param entries источник пунктов
     */
    public static void onSecondaryClick(Widget target, EditorContext context, Supplier<List<Entry>> entries) {
        target.on(PointerPressedEvent.TYPE, event -> {
            if (event.button() != PointerButton.SECONDARY) return;
            open(context, event.rootX(), event.rootY(), entries.get());
            event.cancel();
        });
    }

    /**
     * Кнопка «⋯», открывающая меню под собой.
     *
     * @param context контекст редактора
     * @param entries источник пунктов
     * @return кнопка
     */
    public static Button button(EditorContext context, Supplier<List<Entry>> entries) {
        ToolButton more = new ToolButton(EditorIcons.MORE, "More");
        more.onClick(click -> {
            RectView bounds = more.layoutBounds();
            open(context, bounds.x(), bounds.y() + bounds.height() + 2.0f, entries.get());
        });
        return more;
    }
}

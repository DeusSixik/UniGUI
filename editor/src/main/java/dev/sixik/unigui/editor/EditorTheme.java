package dev.sixik.unigui.editor;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.interaction.Button;

/**
 * Внешний вид каркаса: цвета фона и вкладок, строка меню, выпадающие меню.
 *
 * <p>Тема нужна, чтобы вид «как в Unity» или «как в Unreal» был отдельной деталью, а не копией кода: каркас строит
 * структуру и просит у темы виджеты и стили. {@link #dark()} даёт нейтральную тёмную тему.</p>
 */
public interface EditorTheme {
    /** @return цвет фона под всем редактором */
    MutableColor workspaceBackground();

    /**
     * Оформляет область докинга: тему вкладок и разделителей, высоту вкладок, отрыв панелей в окна.
     *
     * @param dock область докинга редактора
     */
    void styleDock(DockingRoot dock);

    /** @return тонкий разделитель между строкой меню, панелью инструментов и рабочей областью */
    Widget separator();

    /**
     * @param text надпись
     * @return кнопка меню верхнего уровня
     */
    Button menuButton(String text);

    /**
     * @param text надпись
     * @return виджет с заголовком редактора в строке меню
     */
    Widget title(String text);

    /** @return высота строки меню */
    float menuBarHeight();

    /** @return высота панели инструментов */
    float toolbarHeight();

    /** @return ширина выпадающего меню */
    float menuWidth();

    /**
     * Оформляет только что созданное выпадающее меню.
     *
     * @param menu меню
     */
    void styleMenu(ContextMenu menu);

    /**
     * Добавляет пункт в выпадающее меню.
     *
     * @param menu     меню
     * @param text     заголовок
     * @param shortcut подпись сочетания клавиш или пустая строка
     * @param enabled  доступность пункта
     * @param width    ширина пункта
     * @param action   действие
     */
    void addMenuItem(ContextMenu menu, String text, String shortcut, boolean enabled, float width, Runnable action);

    /**
     * Добавляет разделитель в выпадающее меню.
     *
     * @param menu  меню
     * @param width ширина
     */
    void addMenuSeparator(ContextMenu menu, float width);

    /** @return нейтральная тёмная тема */
    static EditorTheme dark() {
        return new DefaultEditorTheme();
    }
}

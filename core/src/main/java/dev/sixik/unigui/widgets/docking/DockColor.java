package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Роль цвета в {@link DockTheme}.
 *
 * <p>Painter запрашивает цвета по роли, а не по литералу: смена темы или переопределение одной роли
 * ({@code theme.color(DockColor.TAB_ACCENT, ...)}) перекрашивает вкладки, заголовки окон и зоны дропа
 * одновременно.</p>
 */
@AiDoc(summary = "Semantic color roles used throughout DockTheme to style tabs, split bars, floating window chrome, and drop preview zones."
)
@WikiPage(
        title = "DockColor",
        slug = "dock-color",
        category = "<key:docking.category>",
        summary = "<key:dock.color.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:dock.color.section.overview.body>",
        order = 10
)
@WikiLink(
        label = "DockTheme",
        target = "page:dock-theme",
        description = "<key:dock.theme.summary>"
)
public enum DockColor {
    /** Фон полосы вкладок (и заголовка окна). */
    STRIP_BACKGROUND,
    /** Линия под полосой вкладок. */
    STRIP_BORDER,
    /** Фон невыбранной вкладки. */
    TAB_BACKGROUND,
    /** Фон вкладки под курсором. */
    TAB_HOVER,
    /** Фон выбранной вкладки активного листа; совпадает с фоном содержимого, чтобы вкладка «росла» из панели. */
    TAB_SELECTED,
    /** Фон выбранной вкладки листа, не имеющего фокуса. */
    TAB_SELECTED_INACTIVE,
    /** Фон вкладки в момент нажатия. */
    TAB_PRESSED,
    /** Тонкий разделитель между соседними невыбранными вкладками. */
    TAB_SEPARATOR,
    /** Цвет текста выбранной вкладки. */
    TAB_TEXT,
    /** Цвет текста невыбранной вкладки. */
    TAB_TEXT_MUTED,
    /** Акцентная линия сверху выбранной вкладки активного листа. */
    TAB_ACCENT,
    /** Акцентная линия сверху выбранной вкладки листа без фокуса. */
    TAB_ACCENT_INACTIVE,
    /** Точка «есть несохранённые изменения». */
    TAB_DIRTY,
    /** Контур вкладки, которую сейчас перетаскивают. */
    TAB_DRAG,
    /** Крестик закрытия в покое. */
    CLOSE_GLYPH,
    /** Крестик закрытия под курсором. */
    CLOSE_GLYPH_HOVER,
    /** Подложка кнопки закрытия под курсором. */
    CLOSE_HOVER_BACKGROUND,
    /** Фон области содержимого листа. */
    CONTENT_BACKGROUND,
    /** Рамка листа. */
    PANE_BORDER,
    /** Разделитель панелей в покое. */
    SPLIT,
    /** Разделитель панелей под курсором. */
    SPLIT_HOVER,
    /** Разделитель панелей во время перетаскивания. */
    SPLIT_PRESSED,
    /** Фон меню переполнения вкладок. */
    OVERFLOW_BACKGROUND,
    /** Рамка меню переполнения. */
    OVERFLOW_BORDER,
    /** Подсветка выбранной строки меню переполнения. */
    OVERFLOW_SELECTED,
    /** Подсветка строки меню переполнения под курсором. */
    OVERFLOW_HOVER,
    /** Текст строк меню переполнения. */
    OVERFLOW_TEXT,
    /** Глиф кнопки переполнения. */
    OVERFLOW_GLYPH,
    /** Заливка превью места дропа. */
    DROP_FILL,
    /** Контур превью места дропа. */
    DROP_STROKE,
    /** Заливка ячейки креста зон. */
    ZONE_FILL,
    /** Заливка ячейки креста зон под курсором. */
    ZONE_FILL_ACTIVE,
    /** Контур ячейки креста зон. */
    ZONE_STROKE,
    /** Пиктограмма внутри ячейки креста зон. */
    ZONE_GLYPH,
    /** Текст-подсказка пустого root. */
    EMPTY_TEXT,
    /** Рамка плавающего окна. */
    WINDOW_BORDER,
    /** Рамка активного плавающего окна. */
    WINDOW_BORDER_ACTIVE,
    /** Цвет тени плавающего окна (альфа — сила у границы окна). */
    WINDOW_SHADOW,
    /** Ручка изменения размера окна в покое. */
    GRIP,
    /** Ручка изменения размера окна под курсором. */
    GRIP_HOVER,
    /** Ручка изменения размера окна во время растягивания. */
    GRIP_ACTIVE,
    /** Заливка «призрачного» окна, пока его тащат компактным. */
    GHOST_FILL
}

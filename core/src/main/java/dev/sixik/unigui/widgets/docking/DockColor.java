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
@AiDoc(
        description = "Semantic color roles used throughout DockTheme to style tabs, split bars, floating window chrome, and drop preview zones."
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
    @AiDoc(description = "Background of the tab strip and window title bar.")
    STRIP_BACKGROUND,
    /** Линия под полосой вкладок. */
    @AiDoc(description = "Border line under the tab strip.")
    STRIP_BORDER,
    /** Фон невыбранной вкладки. */
    @AiDoc(description = "Background of unselected tabs.")
    TAB_BACKGROUND,
    /** Фон вкладки под курсором. */
    @AiDoc(description = "Background of tabs when hovered by the pointer.")
    TAB_HOVER,
    /** Фон выбранной вкладки активного листа; совпадает с фоном содержимого, чтобы вкладка «росла» из панели. */
    @AiDoc(description = "Background of the selected tab in the active leaf node.")
    TAB_SELECTED,
    /** Фон выбранной вкладки листа, не имеющего фокуса. */
    @AiDoc(description = "Background of the selected tab in an inactive leaf node.")
    TAB_SELECTED_INACTIVE,
    /** Фон вкладки в момент нажатия. */
    @AiDoc(description = "Background of tabs during mouse press.")
    TAB_PRESSED,
    /** Тонкий разделитель между соседними невыбранными вкладками. */
    @AiDoc(description = "Subtle separator between adjacent unselected tabs.")
    TAB_SEPARATOR,
    /** Цвет текста выбранной вкладки. */
    @AiDoc(description = "Foreground text color for selected tabs.")
    TAB_TEXT,
    /** Цвет текста невыбранной вкладки. */
    @AiDoc(description = "Muted text color for unselected tabs.")
    TAB_TEXT_MUTED,
    /** Акцентная линия сверху выбранной вкладки активного листа. */
    @AiDoc(description = "Accent highlight stripe above the selected tab in the active leaf.")
    TAB_ACCENT,
    /** Акцентная линия сверху выбранной вкладки листа без фокуса. */
    @AiDoc(description = "Muted accent stripe for selected tabs in inactive leaves.")
    TAB_ACCENT_INACTIVE,
    /** Точка «есть несохранённые изменения». */
    @AiDoc(description = "Color of the unsaved changes indicator dot on dirty tabs.")
    TAB_DIRTY,
    /** Контур вкладки, которую сейчас перетаскивают. */
    @AiDoc(description = "Outline stroke for a tab currently being dragged.")
    TAB_DRAG,
    /** Крестик закрытия в покое. */
    @AiDoc(description = "Glyph color for the tab close button in idle state.")
    CLOSE_GLYPH,
    /** Крестик закрытия под курсором. */
    @AiDoc(description = "Glyph color for the tab close button under mouse hover.")
    CLOSE_GLYPH_HOVER,
    /** Подложка кнопки закрытия под курсором. */
    @AiDoc(description = "Background highlight behind the close button when hovered.")
    CLOSE_HOVER_BACKGROUND,
    /** Фон области содержимого листа. */
    @AiDoc(description = "Background fill of the pane content area.")
    CONTENT_BACKGROUND,
    /** Рамка листа. */
    @AiDoc(description = "Border stroke surrounding dock pane leaves.")
    PANE_BORDER,
    /** Разделитель панелей в покое. */
    @AiDoc(description = "Color of split dividers in idle state.")
    SPLIT,
    /** Разделитель панелей под курсором. */
    @AiDoc(description = "Color of split dividers under mouse hover.")
    SPLIT_HOVER,
    /** Разделитель панелей во время перетаскивания. */
    @AiDoc(description = "Color of split dividers while actively dragging.")
    SPLIT_PRESSED,
    /** Фон меню переполнения вкладок. */
    @AiDoc(description = "Background fill for the tab overflow dropdown menu.")
    OVERFLOW_BACKGROUND,
    /** Рамка меню переполнения. */
    @AiDoc(description = "Border stroke for the tab overflow menu.")
    OVERFLOW_BORDER,
    /** Подсветка выбранной строки меню переполнения. */
    @AiDoc(description = "Highlight color for the selected row in overflow menu.")
    OVERFLOW_SELECTED,
    /** Подсветка строки меню переполнения под курсором. */
    @AiDoc(description = "Highlight color for rows under hover in overflow menu.")
    OVERFLOW_HOVER,
    /** Текст строк меню переполнения. */
    @AiDoc(description = "Text color for items inside the overflow menu.")
    OVERFLOW_TEXT,
    /** Глиф кнопки переполнения. */
    @AiDoc(description = "Chevron glyph color for the overflow button.")
    OVERFLOW_GLYPH,
    /** Заливка превью места дропа. */
    @AiDoc(description = "Fill color of the docking drop preview zone.")
    DROP_FILL,
    /** Контур превью места дропа. */
    @AiDoc(description = "Stroke color of the docking drop preview zone.")
    DROP_STROKE,
    /** Заливка ячейки креста зон. */
    @AiDoc(description = "Fill color for directional drop cross cells.")
    ZONE_FILL,
    /** Заливка ячейки креста зон под курсором. */
    @AiDoc(description = "Fill color for hovered drop cross cells.")
    ZONE_FILL_ACTIVE,
    /** Контур ячейки креста зон. */
    @AiDoc(description = "Stroke color for drop cross cells.")
    ZONE_STROKE,
    /** Пиктограмма внутри ячейки креста зон. */
    @AiDoc(description = "Glyph/icon color inside drop cross cells.")
    ZONE_GLYPH,
    /** Текст-подсказка пустого root. */
    @AiDoc(description = "Placeholder text color displayed when DockingRoot is empty.")
    EMPTY_TEXT,
    /** Рамка плавающего окна. */
    @AiDoc(description = "Border stroke for floating docking windows.")
    WINDOW_BORDER,
    /** Рамка активного плавающего окна. */
    @AiDoc(description = "Border stroke for active floating docking windows.")
    WINDOW_BORDER_ACTIVE,
    /** Цвет тени плавающего окна (альфа — сила у границы окна). */
    @AiDoc(description = "Drop shadow color for floating docking windows.")
    WINDOW_SHADOW,
    /** Ручка изменения размера окна в покое. */
    @AiDoc(description = "Color of window resize grip in idle state.")
    GRIP,
    /** Ручка изменения размера окна под курсором. */
    @AiDoc(description = "Color of window resize grip when hovered.")
    GRIP_HOVER,
    /** Ручка изменения размера окна во время растягивания. */
    @AiDoc(description = "Color of window resize grip while actively resizing.")
    GRIP_ACTIVE,
    /** Заливка «призрачного» окна, пока его тащат компактным. */
    @AiDoc(description = "Fill color for tear-off ghost preview windows during drag.")
    GHOST_FILL
}

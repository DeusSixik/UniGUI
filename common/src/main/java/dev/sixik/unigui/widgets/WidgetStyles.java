package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;

/**
 * Общие стили компонентов и готовые варианты оформления встроенных виджетов.
 */
public final class WidgetStyles {
    /**
     * Запрещает создавать экземпляр каталога стилей.
     */
    private WidgetStyles() {
    }

    /**
     * Стили однострочных и многострочных текстовых полей.
     */
    public static final class TextInput {
        /**
         * Оформление поля по умолчанию; уступает стилям темы и экземпляра.
         */
        public static final Style DEFAULT_CHROME = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.08f, 0.086f, 0.10f, 1.0f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.18f, 0.20f, 0.23f, 1.0f))
                .put(StyleKeys.RADIUS, 8.0f);

        /**
         * Вариант без рамки и кольца фокуса, сохраняющий остальное оформление поля.
         */
        public static final Style UNCHROMED = new MutableStyle()
                .put(StyleKeys.BORDER_VISIBLE, false)
                .put(StyleKeys.BORDER_COLOR, WidgetState.NORMAL, transparent())
                .put(StyleKeys.BORDER_COLOR, WidgetState.HOVERED, transparent())
                .put(StyleKeys.BORDER_COLOR, WidgetState.FOCUSED, transparent())
                .put(StyleKeys.FOCUS_RING_COLOR, transparent());

        /**
         * Запрещает создавать экземпляр пространства имён стилей.
         */
        private TextInput() {
        }
    }

    /**
     * Варианты поверхности для элементов, которые рисуют собственное содержимое.
     */
    public static final class Surface {
        /**
         * Скрывает стандартные фон и рамку.
         */
        public static final Style UNSURFACED = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, false)
                .put(StyleKeys.BORDER_VISIBLE, false);

        /**
         * Запрещает создавать экземпляр пространства имён стилей.
         */
        private Surface() {
        }
    }

    /**
     * Стили обычных кнопок.
     */
    public static final class Button {
        /**
         * Стандартная поверхность; цвета задаются темой.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BORDER_VISIBLE, true);

        /**
         * Запрещает создавать экземпляр пространства имён стилей.
         */
        private Button() {
        }
    }

    /**
     * Стили многострочных текстовых полей.
     */
    public static final class TextArea {
        /**
         * Поверхность редактора по умолчанию; уступает стилям темы и экземпляра.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.04f, 0.04f, 0.04f, 0.82f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.35f, 0.35f, 0.35f, 1.0f));

        /**
         * Запрещает создавать экземпляр пространства имён стилей.
         */
        private TextArea() {
        }
    }

    /**
     * Стили контейнеров-карточек {@code View}.
     */
    public static final class View {
        /**
         * Поверхность {@code View} по умолчанию; уступает стилям темы и экземпляра.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.030f, 0.036f, 0.050f, 0.92f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.20f, 0.28f, 0.38f, 0.82f))
                .put(StyleKeys.RADIUS, 5.0f);

        /**
         * Запрещает создавать экземпляр пространства имён стилей.
         */
        private View() {
        }
    }

    /**
     * Стили поверхностей предпросмотра предметов и сущностей Minecraft.
     */
    public static final class MinecraftPreview {
        /**
         * Поверхность предпросмотра по умолчанию; уступает стилям темы и экземпляра.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.035f, 0.040f, 0.055f, 0.94f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.20f, 0.23f, 0.30f, 0.92f))
                .put(StyleKeys.RADIUS, 4.0f);

        /**
         * Запрещает создавать экземпляр пространства имён стилей.
         */
        private MinecraftPreview() {
        }
    }

    /**
     * Стили визуальной дорожки разделителя панелей.
     */
    public static final class Splitter {
        /**
         * Дорожка разделителя по умолчанию; уступает стилям темы и экземпляра.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.09f, 0.10f, 0.12f, 0.95f))
                .put(StyleKeys.BORDER_VISIBLE, false)
                .put(StyleKeys.RADIUS, 2.0f);

        /**
         * Запрещает создавать экземпляр пространства имён стилей.
         */
        private Splitter() {
        }
    }

    /**
     * Стили элементов навигации и их поверхностей.
     */
    public static final class Navigation {
        /**
         * Форма элементов пути навигации; цвета состояний задаются во время работы.
         */
        public static final Style BREADCRUMB_ITEM = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BORDER_VISIBLE, false)
                .put(StyleKeys.RADIUS, 3.0f);

        /**
         * Запрещает создавать экземпляр пространства имён стилей.
         */
        private Navigation() {
        }
    }

    /**
     * Стили поверхностей стыковки панелей и административной консоли.
     */
    public static final class Tools {
        /**
         * Поверхность корня стыковки; уступает стилям темы и экземпляра.
         */
        public static final Style DOCKING_ROOT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.025f, 0.030f, 0.042f, 0.98f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.18f, 0.22f, 0.30f, 0.95f))
                .put(StyleKeys.BORDER_WIDTH, 1.0f)
                .put(StyleKeys.RADIUS, 4.0f);

        /**
         * Стандартная корневая поверхность расширяемой административной консоли.
         */
        public static final Style ADMIN_CONSOLE = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.025f, 0.030f, 0.038f, 0.96f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.28f, 0.36f, 0.44f, 0.82f))
                .put(StyleKeys.RADIUS, 6.0f);

        /**
         * Запрещает создавать экземпляр пространства имён стилей.
         */
        private Tools() {
        }
    }

    /**
     * Стили заголовков и вариантов списков выбора.
     */
    public static final class Selection {
        /**
         * Поверхность без рамки для кнопок-заголовков {@code ComboBox} и {@code DropDownBox}.
         */
        public static final Style DROPDOWN_HEADER = new MutableStyle()
                .put(StyleKeys.BORDER_VISIBLE, false);

        /** Стандартная поверхность списка ComboBox; уступает теме и локальным стилям. */
        public static final Style DROPDOWN_HOST = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.118f, 0.129f, 0.149f, 0.97f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.180f, 0.200f, 0.231f, 1.0f))
                .put(StyleKeys.RADIUS, 8.0f);

        /**
         * Прозрачная поверхность без рамки для строк вариантов {@code ComboBox}.
         */
        public static final Style DROPDOWN_OPTION = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.0f, 0.0f, 0.0f, 0.0f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, MutableColor.rgba(0.24f, 0.28f, 0.35f, 0.92f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.PRESSED, MutableColor.rgba(0.24f, 0.28f, 0.35f, 0.92f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.CHECKED, MutableColor.rgba(0.149f, 0.251f, 0.369f, 1.0f))
                .put(StyleKeys.BORDER_VISIBLE, false)
                .put(StyleKeys.RADIUS, 4.0f);

        /**
         * Запрещает создавать экземпляр пространства имён стилей.
         */
        private Selection() {
        }
    }

    /**
     * Стили перемещаемых окон с изменяемым размером.
     */
    public static final class Window {
        /**
         * Поверхность окна по умолчанию; уступает стилям темы и экземпляра.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.030f, 0.035f, 0.050f, 0.98f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.25f, 0.78f, 1.0f, 0.85f))
                .put(StyleKeys.RADIUS, 4.0f);

        /**
         * Запрещает создавать экземпляр пространства имён стилей.
         */
        private Window() {
        }
    }

    /**
     * Стили поверхностей всплывающих подсказок.
     */
    public static final class Tooltip {
        /**
         * Поверхность подсказки по умолчанию; уступает стилям темы и экземпляра.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.02f, 0.025f, 0.035f, 0.94f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.25f, 0.78f, 1.0f, 0.85f))
                .put(StyleKeys.RADIUS, 3.0f);

        /**
         * Запрещает создавать экземпляр пространства имён стилей.
         */
        private Tooltip() {
        }
    }

    /**
     * Стили временных уведомлений.
     */
    public static final class Toast {
        /**
         * Поверхность уведомления по умолчанию; уступает стилям темы и экземпляра.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.035f, 0.043f, 0.060f, 0.96f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.30f, 0.78f, 1.0f, 0.75f))
                .put(StyleKeys.RADIUS, 5.0f);

        /**
         * Запрещает создавать экземпляр пространства имён стилей.
         */
        private Toast() {
        }
    }

    /**
     * Стили поверхностей всплывающих окон, привязанных к элементу.
     */
    public static final class Popup {
        /**
         * Поверхность всплывающего окна по умолчанию; уступает стилям темы и экземпляра.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.025f, 0.030f, 0.040f, 0.97f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.25f, 0.78f, 1.0f, 0.90f))
                .put(StyleKeys.RADIUS, 3.0f);

        /**
         * Запрещает создавать экземпляр пространства имён стилей.
         */
        private Popup() {
        }
    }

    /**
     * Стили поверхностей контекстных меню.
     */
    public static final class ContextMenu {
        /**
         * Поверхность контекстного меню по умолчанию; уступает стилям темы и экземпляра.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.025f, 0.030f, 0.040f, 0.98f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.25f, 0.78f, 1.0f, 0.85f))
                .put(StyleKeys.RADIUS, 3.0f);

        /**
         * Запрещает создавать экземпляр пространства имён стилей.
         */
        private ContextMenu() {
        }
    }

    /**
     * Создаёт новый прозрачный цвет для объявления стиля.
     *
     * @return отдельный полностью прозрачный цвет
     */
    private static MutableColor transparent() {
        return MutableColor.rgba(0.0f, 0.0f, 0.0f, 0.0f);
    }
}

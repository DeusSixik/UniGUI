package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;

/**
 * Reusable component defaults and appearance presets for built-in widgets.
 */
public final class WidgetStyles {
    /**
     * Prevents construction of the static style catalog.
     */
    private WidgetStyles() {
    }

    /**
     * Styles shared by single-line and multiline text controls.
     */
    public static final class TextInput {
        /**
         * Default field chrome below theme and instance styles.
         */
        public static final Style DEFAULT_CHROME = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.08f, 0.086f, 0.10f, 1.0f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.18f, 0.20f, 0.23f, 1.0f))
                .put(StyleKeys.RADIUS, 8.0f);

        /**
         * Preset that hides the border and focus ring while preserving other field styling.
         */
        public static final Style UNCHROMED = new MutableStyle()
                .put(StyleKeys.BORDER_VISIBLE, false)
                .put(StyleKeys.BORDER_COLOR, WidgetState.NORMAL, transparent())
                .put(StyleKeys.BORDER_COLOR, WidgetState.HOVERED, transparent())
                .put(StyleKeys.BORDER_COLOR, WidgetState.FOCUSED, transparent())
                .put(StyleKeys.FOCUS_RING_COLOR, transparent());

        /**
         * Prevents construction of the nested style namespace.
         */
        private TextInput() {
        }
    }

    /**
     * Surface participation presets for controls that draw their own visual content.
     */
    public static final class Surface {
        /**
         * Hides generic background and border surfaces.
         */
        public static final Style UNSURFACED = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, false)
                .put(StyleKeys.BORDER_VISIBLE, false);

        /**
         * Prevents construction of the nested style namespace.
         */
        private Surface() {
        }
    }

    /**
     * Styles for standard push buttons.
     */
    public static final class Button {
        /**
         * Default surface participation; colors are supplied by the theme.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BORDER_VISIBLE, true);

        /**
         * Prevents construction of the nested style namespace.
         */
        private Button() {
        }
    }

    /**
     * Styles for multiline text editors.
     */
    public static final class TextArea {
        /**
         * Default editor surface below theme and instance styles.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.04f, 0.04f, 0.04f, 0.82f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.35f, 0.35f, 0.35f, 1.0f));

        /**
         * Prevents construction of the nested style namespace.
         */
        private TextArea() {
        }
    }

    /**
     * Styles for card-like {@code View} containers.
     */
    public static final class View {
        /**
         * Default view surface below theme and instance styles.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.030f, 0.036f, 0.050f, 0.92f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.20f, 0.28f, 0.38f, 0.82f))
                .put(StyleKeys.RADIUS, 5.0f);

        /**
         * Prevents construction of the nested style namespace.
         */
        private View() {
        }
    }

    /**
     * Styles for Minecraft item and entity preview surfaces.
     */
    public static final class MinecraftPreview {
        /**
         * Default preview surface below theme and instance styles.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.035f, 0.040f, 0.055f, 0.94f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.20f, 0.23f, 0.30f, 0.92f))
                .put(StyleKeys.RADIUS, 4.0f);

        /**
         * Prevents construction of the nested style namespace.
         */
        private MinecraftPreview() {
        }
    }

    /**
     * Styles for the visual track of a split-panel divider.
     */
    public static final class Splitter {
        /**
         * Default splitter track below theme and instance styles.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.09f, 0.10f, 0.12f, 0.95f))
                .put(StyleKeys.BORDER_VISIBLE, false)
                .put(StyleKeys.RADIUS, 2.0f);

        /**
         * Prevents construction of the nested style namespace.
         */
        private Splitter() {
        }
    }

    /**
     * Styles for navigation controls and their reusable item surfaces.
     */
    public static final class Navigation {
        /**
         * Default shape for breadcrumb items; state colors remain runtime-owned.
         */
        public static final Style BREADCRUMB_ITEM = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BORDER_VISIBLE, false)
                .put(StyleKeys.RADIUS, 3.0f);

        /**
         * Prevents construction of the nested style namespace.
         */
        private Navigation() {
        }
    }

    /**
     * Styles for docking and administrative tool surfaces.
     */
    public static final class Tools {
        /**
         * Default docking root surface below theme and instance styles.
         */
        public static final Style DOCKING_ROOT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.025f, 0.030f, 0.042f, 0.98f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.18f, 0.22f, 0.30f, 0.95f))
                .put(StyleKeys.BORDER_WIDTH, 1.0f)
                .put(StyleKeys.RADIUS, 4.0f);

        /**
         * Default root surface for the extensible administration console.
         */
        public static final Style ADMIN_CONSOLE = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.025f, 0.030f, 0.038f, 0.96f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.28f, 0.36f, 0.44f, 0.82f))
                .put(StyleKeys.RADIUS, 6.0f);

        /**
         * Prevents construction of the nested style namespace.
         */
        private Tools() {
        }
    }

    /**
     * Styles for selection headers and option rows.
     */
    public static final class Selection {
        /**
         * Borderless surface used by ComboBox and DropDownBox header buttons.
         */
        public static final Style DROPDOWN_HEADER = new MutableStyle()
                .put(StyleKeys.BORDER_VISIBLE, false);

        /**
         * Borderless, transparent surface used by ComboBox option rows.
         */
        public static final Style DROPDOWN_OPTION = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BORDER_VISIBLE, false)
                .put(StyleKeys.RADIUS, 4.0f);

        /**
         * Prevents construction of the nested style namespace.
         */
        private Selection() {
        }
    }

    /**
     * Styles for movable and resizable windows.
     */
    public static final class Window {
        /**
         * Default window surface below theme and instance styles.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.030f, 0.035f, 0.050f, 0.98f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.25f, 0.78f, 1.0f, 0.85f))
                .put(StyleKeys.RADIUS, 4.0f);

        /**
         * Prevents construction of the nested style namespace.
         */
        private Window() {
        }
    }

    /**
     * Styles for tooltip surfaces.
     */
    public static final class Tooltip {
        /**
         * Default tooltip surface below theme and instance styles.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.02f, 0.025f, 0.035f, 0.94f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.25f, 0.78f, 1.0f, 0.85f))
                .put(StyleKeys.RADIUS, 3.0f);

        /**
         * Prevents construction of the nested style namespace.
         */
        private Tooltip() {
        }
    }

    /**
     * Styles for transient toast notifications.
     */
    public static final class Toast {
        /**
         * Default toast surface below theme and instance styles.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.035f, 0.043f, 0.060f, 0.96f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.30f, 0.78f, 1.0f, 0.75f))
                .put(StyleKeys.RADIUS, 5.0f);

        /**
         * Prevents construction of the nested style namespace.
         */
        private Toast() {
        }
    }

    /**
     * Styles for anchored popup surfaces.
     */
    public static final class Popup {
        /**
         * Default popup surface below theme and instance styles.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.025f, 0.030f, 0.040f, 0.97f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.25f, 0.78f, 1.0f, 0.90f))
                .put(StyleKeys.RADIUS, 3.0f);

        /**
         * Prevents construction of the nested style namespace.
         */
        private Popup() {
        }
    }

    /**
     * Styles for context-menu surfaces.
     */
    public static final class ContextMenu {
        /**
         * Default context-menu surface below theme and instance styles.
         */
        public static final Style DEFAULT = new MutableStyle()
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.025f, 0.030f, 0.040f, 0.98f))
                .put(StyleKeys.BORDER_VISIBLE, true)
                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.25f, 0.78f, 1.0f, 0.85f))
                .put(StyleKeys.RADIUS, 3.0f);

        /**
         * Prevents construction of the nested style namespace.
         */
        private ContextMenu() {
        }
    }

    /**
     * Creates a fresh transparent color for style declarations.
     *
     * @return independent fully transparent color
     */
    private static MutableColor transparent() {
        return MutableColor.rgba(0.0f, 0.0f, 0.0f, 0.0f);
    }
}

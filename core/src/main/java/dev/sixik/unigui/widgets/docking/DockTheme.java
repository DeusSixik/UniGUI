package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

import java.util.Objects;

/**
 * Тема визуального оформления докинга: цвета всех элементов, размеры вкладок, разделителей, тени и
 * скругления.
 *
 * <p>UniGUI поставляет две готовые темы: {@link #editor()} (современная тёмная) и {@link #classic()}
 * (прежний сине-голубой стиль). Свою тему собирают вызовом {@link #copy()} от готовой и точечной
 * настройкой.</p>
 */
@AiDoc(summary = "Styling theme for the docking system configuring palette colors (DockColor), tab geometry, split handles, shadows, and window resize grips."
)
@WikiPage(
        title = "DockTheme",
        slug = "dock-theme",
        category = "<key:docking.category>",
        summary = "<key:dock.theme.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:dock.theme.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:dock.theme.example.custom.title>",
        code = "DockTheme theme = DockTheme.editor().copy()\n"
                + "        .tabHeight(24.0f)\n"
                + "        .splitThickness(6.0f);"
)
@WikiLink(
        label = "DockColor",
        target = "page:dock-color",
        description = "<key:dock.color.summary>"
)
@WikiLink(
        label = "DockingRoot",
        target = "page:docking-root",
        description = "<key:docking.root.summary>"
)
public final class DockTheme {
    private static volatile DockTheme defaultTheme = editor();

    private final MutableColor[] colors = new MutableColor[DockColor.values().length];

    private float tabHeight = 22.0f;
    private float tabRadius = 4.0f;
    private float tabPaddingX = 8.0f;
    private float iconSize = 14.0f;
    private float iconGap = 5.0f;
    private float closeSize = 14.0f;
    private float closeGap = 4.0f;
    private float tabMinWidth = 56.0f;
    private float tabMaxWidth = 180.0f;
    private float accentThickness = 2.0f;
    private float splitThickness = 4.0f;
    private DockingRoot.SplitHandleStyle splitStyle = DockingRoot.SplitHandleStyle.SEAM;
    private float windowShadowSize = 10.0f;
    private float windowRadius = 4.0f;
    private float tearOffWidth = 128.0f;
    private float tearOffHeight = 128.0f;
    private float gripSize = 14.0f;
    private ResizeGripStyle resizeGrip = ResizeGripStyle.TRIANGLE;

    private DockTheme() {
        for (int i = 0; i < colors.length; i++) {
            colors[i] = new MutableColor();
        }
    }

    /**
     * Глобальная тема, которую используют все root и окна без собственной темы.
     *
     * @return текущая тема по умолчанию (никогда не {@code null})
     */
    @AiDoc(summary = "Returns the global default docking theme.")
    @WikiMethod(summary = "<key:dock.theme.method.defaultThemeGet>")
    @WikiReturn("<key:dock.theme.return.theme>")
    public static DockTheme defaultTheme() {
        return defaultTheme;
    }

    /**
     * Заменяет глобальную тему по умолчанию.
     *
     * @param theme новая тема; {@code null} возвращает {@link #editor()}
     */
    @AiDoc(summary = "Replaces the global default docking theme.")
    @WikiMethod(summary = "<key:dock.theme.method.defaultThemeSet>")
    public static void defaultTheme(
            @WikiParameter(description = "<key:dock.theme.param.theme>")
            DockTheme theme
    ) {
        defaultTheme = theme == null ? editor() : theme;
    }

    /**
     * Современная нейтральная тёмная тема: серые панели, синий акцент у активной вкладки, плоские
     * разделители в одну линию, тень у плавающих окон.
     *
     * @return новый экземпляр темы
     */
    @AiDoc(summary = "Creates a modern dark editor docking theme instance.")
    @WikiMethod(summary = "<key:dock.theme.method.editor>")
    @WikiReturn("<key:dock.theme.return.theme>")
    public static DockTheme editor() {
        DockTheme t = new DockTheme();
        t.set(DockColor.STRIP_BACKGROUND, 0.125f, 0.125f, 0.133f, 1.0f);
        t.set(DockColor.STRIP_BORDER, 0.070f, 0.070f, 0.075f, 1.0f);
        t.set(DockColor.TAB_BACKGROUND, 0.165f, 0.165f, 0.173f, 1.0f);
        t.set(DockColor.TAB_HOVER, 0.215f, 0.215f, 0.227f, 1.0f);
        t.set(DockColor.TAB_SELECTED, 0.220f, 0.220f, 0.235f, 1.0f);
        t.set(DockColor.TAB_SELECTED_INACTIVE, 0.195f, 0.195f, 0.207f, 1.0f);
        t.set(DockColor.TAB_PRESSED, 0.190f, 0.190f, 0.200f, 1.0f);
        t.set(DockColor.TAB_SEPARATOR, 0.070f, 0.070f, 0.075f, 1.0f);
        t.set(DockColor.TAB_TEXT, 0.93f, 0.93f, 0.95f, 1.0f);
        t.set(DockColor.TAB_TEXT_MUTED, 0.66f, 0.66f, 0.70f, 1.0f);
        t.set(DockColor.TAB_ACCENT, 0.24f, 0.55f, 0.95f, 1.0f);
        t.set(DockColor.TAB_ACCENT_INACTIVE, 0.38f, 0.38f, 0.42f, 1.0f);
        t.set(DockColor.TAB_DIRTY, 1.0f, 0.66f, 0.24f, 1.0f);
        t.set(DockColor.TAB_DRAG, 0.24f, 0.55f, 0.95f, 0.95f);
        t.set(DockColor.CLOSE_GLYPH, 0.60f, 0.60f, 0.64f, 1.0f);
        t.set(DockColor.CLOSE_GLYPH_HOVER, 1.0f, 1.0f, 1.0f, 1.0f);
        t.set(DockColor.CLOSE_HOVER_BACKGROUND, 0.82f, 0.25f, 0.25f, 0.90f);
        t.set(DockColor.CONTENT_BACKGROUND, 0.220f, 0.220f, 0.235f, 1.0f);
        t.set(DockColor.PANE_BORDER, 0.070f, 0.070f, 0.075f, 1.0f);
        t.set(DockColor.SPLIT, 0.070f, 0.070f, 0.075f, 1.0f);
        t.set(DockColor.SPLIT_HOVER, 0.24f, 0.55f, 0.95f, 0.75f);
        t.set(DockColor.SPLIT_PRESSED, 0.24f, 0.55f, 0.95f, 1.0f);
        t.set(DockColor.OVERFLOW_BACKGROUND, 0.140f, 0.140f, 0.150f, 0.99f);
        t.set(DockColor.OVERFLOW_BORDER, 0.070f, 0.070f, 0.075f, 1.0f);
        t.set(DockColor.OVERFLOW_SELECTED, 0.200f, 0.300f, 0.470f, 1.0f);
        t.set(DockColor.OVERFLOW_HOVER, 0.240f, 0.240f, 0.255f, 1.0f);
        t.set(DockColor.OVERFLOW_TEXT, 0.90f, 0.90f, 0.93f, 1.0f);
        t.set(DockColor.OVERFLOW_GLYPH, 0.72f, 0.72f, 0.76f, 1.0f);
        t.set(DockColor.DROP_FILL, 0.24f, 0.55f, 0.95f, 0.28f);
        t.set(DockColor.DROP_STROKE, 0.35f, 0.65f, 1.0f, 0.95f);
        t.set(DockColor.ZONE_FILL, 0.130f, 0.130f, 0.140f, 0.92f);
        t.set(DockColor.ZONE_FILL_ACTIVE, 0.24f, 0.55f, 0.95f, 0.95f);
        t.set(DockColor.ZONE_STROKE, 0.38f, 0.38f, 0.42f, 1.0f);
        t.set(DockColor.ZONE_GLYPH, 0.85f, 0.85f, 0.90f, 1.0f);
        t.set(DockColor.EMPTY_TEXT, 0.55f, 0.55f, 0.60f, 1.0f);
        t.set(DockColor.WINDOW_BORDER, 0.070f, 0.070f, 0.075f, 1.0f);
        t.set(DockColor.WINDOW_BORDER_ACTIVE, 0.24f, 0.55f, 0.95f, 0.85f);
        t.set(DockColor.WINDOW_SHADOW, 0.0f, 0.0f, 0.0f, 0.34f);
        t.set(DockColor.GHOST_FILL, 0.220f, 0.220f, 0.235f, 0.82f);
        t.set(DockColor.GRIP, 0.24f, 0.55f, 0.95f, 0.30f);
        t.set(DockColor.GRIP_HOVER, 0.24f, 0.55f, 0.95f, 0.67f);
        t.set(DockColor.GRIP_ACTIVE, 0.35f, 0.65f, 1.0f, 0.95f);
        return t;
    }

    /**
     * Прежний сине-голубой вид докинга, сохранённый для обратной совместимости.
     *
     * <p>Фон содержимого прозрачный — листы показывают стиль самого root, как раньше.</p>
     *
     * @return новый экземпляр темы
     */
    @AiDoc(summary = "Creates a classic blue-tinted docking theme instance.")
    @WikiMethod(summary = "<key:dock.theme.method.classic>")
    @WikiReturn("<key:dock.theme.return.theme>")
    public static DockTheme classic() {
        DockTheme t = new DockTheme();
        t.set(DockColor.STRIP_BACKGROUND, 0.075f, 0.085f, 0.115f, 0.96f);
        t.set(DockColor.STRIP_BORDER, 0.24f, 0.28f, 0.36f, 0.95f);
        t.set(DockColor.TAB_BACKGROUND, 0.085f, 0.095f, 0.120f, 0.98f);
        t.set(DockColor.TAB_HOVER, 0.18f, 0.24f, 0.34f, 0.96f);
        t.set(DockColor.TAB_SELECTED, 0.12f, 0.16f, 0.23f, 0.98f);
        t.set(DockColor.TAB_SELECTED_INACTIVE, 0.12f, 0.16f, 0.23f, 0.98f);
        t.set(DockColor.TAB_PRESSED, 0.08f, 0.11f, 0.17f, 0.98f);
        t.set(DockColor.TAB_SEPARATOR, 0.26f, 0.32f, 0.42f, 0.0f);
        t.set(DockColor.TAB_TEXT, 0.88f, 0.93f, 1.0f, 1.0f);
        t.set(DockColor.TAB_TEXT_MUTED, 0.62f, 0.68f, 0.78f, 0.88f);
        t.set(DockColor.TAB_ACCENT, 0.28f, 0.78f, 1.0f, 0.85f);
        t.set(DockColor.TAB_ACCENT_INACTIVE, 0.26f, 0.32f, 0.42f, 0.95f);
        t.set(DockColor.TAB_DIRTY, 1.0f, 0.66f, 0.24f, 1.0f);
        t.set(DockColor.TAB_DRAG, 0.25f, 0.78f, 1.0f, 0.92f);
        t.set(DockColor.CLOSE_GLYPH, 0.62f, 0.68f, 0.78f, 0.88f);
        t.set(DockColor.CLOSE_GLYPH_HOVER, 1.0f, 1.0f, 1.0f, 1.0f);
        t.set(DockColor.CLOSE_HOVER_BACKGROUND, 0.70f, 0.22f, 0.26f, 0.90f);
        t.set(DockColor.CONTENT_BACKGROUND, 0.0f, 0.0f, 0.0f, 0.0f);
        t.set(DockColor.PANE_BORDER, 0.24f, 0.28f, 0.36f, 0.95f);
        t.set(DockColor.SPLIT, 0.055f, 0.062f, 0.080f, 0.98f);
        t.set(DockColor.SPLIT_HOVER, 0.35f, 0.88f, 1.0f, 0.75f);
        t.set(DockColor.SPLIT_PRESSED, 0.45f, 0.95f, 1.0f, 1.0f);
        t.set(DockColor.OVERFLOW_BACKGROUND, 0.045f, 0.052f, 0.070f, 0.98f);
        t.set(DockColor.OVERFLOW_BORDER, 0.25f, 0.78f, 1.0f, 0.85f);
        t.set(DockColor.OVERFLOW_SELECTED, 0.12f, 0.16f, 0.23f, 0.98f);
        t.set(DockColor.OVERFLOW_HOVER, 0.18f, 0.24f, 0.34f, 0.96f);
        t.set(DockColor.OVERFLOW_TEXT, 0.88f, 0.93f, 1.0f, 1.0f);
        t.set(DockColor.OVERFLOW_GLYPH, 0.28f, 0.78f, 1.0f, 0.85f);
        t.set(DockColor.DROP_FILL, 0.18f, 0.56f, 1.0f, 0.22f);
        t.set(DockColor.DROP_STROKE, 0.28f, 0.78f, 1.0f, 0.92f);
        t.set(DockColor.ZONE_FILL, 0.20f, 0.32f, 0.52f, 0.55f);
        t.set(DockColor.ZONE_FILL_ACTIVE, 0.25f, 0.55f, 1.0f, 0.80f);
        t.set(DockColor.ZONE_STROKE, 0.35f, 0.55f, 0.85f, 0.70f);
        t.set(DockColor.ZONE_GLYPH, 0.88f, 0.93f, 1.0f, 1.0f);
        t.set(DockColor.EMPTY_TEXT, 0.62f, 0.68f, 0.78f, 0.8f);
        t.set(DockColor.WINDOW_BORDER, 0.25f, 0.78f, 1.0f, 0.85f);
        t.set(DockColor.WINDOW_BORDER_ACTIVE, 0.25f, 0.78f, 1.0f, 0.85f);
        t.set(DockColor.WINDOW_SHADOW, 0.0f, 0.0f, 0.0f, 0.0f);
        t.set(DockColor.GHOST_FILL, 0.030f, 0.035f, 0.050f, 0.80f);
        t.set(DockColor.GRIP, 0.28f, 0.78f, 1.0f, 0.30f);
        t.set(DockColor.GRIP_HOVER, 0.28f, 0.78f, 1.0f, 0.67f);
        t.set(DockColor.GRIP_ACTIVE, 0.45f, 0.95f, 1.0f, 0.95f);
        t.tabRadius = 3.0f;
        t.tabMinWidth = 56.0f;
        t.tabMaxWidth = 132.0f;
        t.accentThickness = 1.5f;
        t.splitThickness = 5.0f;
        t.splitStyle = DockingRoot.SplitHandleStyle.BLOCK;
        t.windowShadowSize = 0.0f;
        return t;
    }

    /**
     * Создаёт независимую копию темы, которую можно менять, не затрагивая оригинал.
     *
     * @return копия темы
     */
    @AiDoc(summary = "Clones this theme into an independent mutable copy.")
    @WikiMethod(summary = "<key:dock.theme.method.copy>")
    @WikiReturn("<key:dock.theme.return.theme>")
    public DockTheme copy() {
        DockTheme copy = new DockTheme();
        for (int i = 0; i < colors.length; i++) {
            copy.colors[i].set(colors[i]);
        }
        copy.tabHeight = tabHeight;
        copy.tabRadius = tabRadius;
        copy.tabPaddingX = tabPaddingX;
        copy.iconSize = iconSize;
        copy.iconGap = iconGap;
        copy.closeSize = closeSize;
        copy.closeGap = closeGap;
        copy.tabMinWidth = tabMinWidth;
        copy.tabMaxWidth = tabMaxWidth;
        copy.accentThickness = accentThickness;
        copy.splitThickness = splitThickness;
        copy.splitStyle = splitStyle;
        copy.windowShadowSize = windowShadowSize;
        copy.windowRadius = windowRadius;
        copy.tearOffWidth = tearOffWidth;
        copy.tearOffHeight = tearOffHeight;
        copy.gripSize = gripSize;
        copy.resizeGrip = resizeGrip;
        return copy;
    }

    /**
     * Возвращает изменяемый цвет роли.
     *
     * @param role роль цвета
     * @return живой цвет темы; изменения видны на следующем кадре
     */
    @AiDoc(summary = "Returns the live mutable color corresponding to the specified role.")
    @WikiMethod(summary = "<key:dock.theme.method.colorGet>")
    @WikiReturn("<key:dock.theme.return.mutableColor>")
    public MutableColor color(
            @WikiParameter(description = "<key:dock.theme.param.role>")
            DockColor role
    ) {
        return colors[role.ordinal()];
    }

    /**
     * Задаёт цвет роли.
     *
     * @param role роль цвета
     * @param color новый цвет
     * @return эта тема для fluent-настройки
     */
    @AiDoc(summary = "Sets the color assigned to a specific role.")
    @WikiMethod(summary = "<key:dock.theme.method.colorSet>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme color(
            @WikiParameter(description = "<key:dock.theme.param.role>")
            DockColor role,
            @WikiParameter(description = "<key:dock.theme.param.color>")
            ColorView color
    ) {
        colors[role.ordinal()].set(Objects.requireNonNull(color, "color"));
        return this;
    }

    private void set(DockColor role, float r, float g, float b, float a) {
        colors[role.ordinal()].set(r, g, b, a);
    }

    /** @return высота полосы вкладок и заголовка окна в UI-пикселях */
    @AiDoc(summary = "Returns the height of tab strips and window title bars in pixels.")
    @WikiMethod(summary = "<key:dock.theme.method.tabHeightGet>")
    @WikiReturn("<key:dock.theme.return.tabHeight>")
    public float tabHeight() {
        return tabHeight;
    }

    /**
     * @param tabHeight высота полосы вкладок, не меньше 14
     * @return эта тема
     */
    @AiDoc(summary = "Sets the height of tab strips and title bars.")
    @WikiMethod(summary = "<key:dock.theme.method.tabHeightSet>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme tabHeight(
            @WikiParameter(description = "<key:dock.theme.param.tabHeight>")
            float tabHeight
    ) {
        this.tabHeight = finiteMax(tabHeight, 14.0f, this.tabHeight);
        return this;
    }

    /** @return радиус верхних углов вкладки */
    @AiDoc(summary = "Returns the top corner radius of tabs.")
    @WikiMethod(summary = "<key:dock.theme.method.tabRadiusGet>")
    @WikiReturn("<key:dock.theme.return.tabRadius>")
    public float tabRadius() {
        return tabRadius;
    }

    /**
     * @param tabRadius радиус верхних углов вкладки, не меньше 0
     * @return эта тема
     */
    @AiDoc(summary = "Sets the top corner radius of tabs.")
    @WikiMethod(summary = "<key:dock.theme.method.tabRadiusSet>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme tabRadius(
            @WikiParameter(description = "<key:dock.theme.param.tabRadius>")
            float tabRadius
    ) {
        this.tabRadius = finiteMax(tabRadius, 0.0f, this.tabRadius);
        return this;
    }

    /** @return внутренний горизонтальный отступ вкладки */
    @AiDoc(summary = "Returns the horizontal inner padding of tabs.")
    @WikiMethod(summary = "<key:dock.theme.method.tabPaddingXGet>")
    @WikiReturn("<key:dock.theme.return.tabPaddingX>")
    public float tabPaddingX() {
        return tabPaddingX;
    }

    /**
     * @param tabPaddingX внутренний отступ вкладки слева и справа, не меньше 0
     * @return эта тема
     */
    @AiDoc(summary = "Sets the horizontal inner padding of tabs.")
    @WikiMethod(summary = "<key:dock.theme.method.tabPaddingXSet>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme tabPaddingX(
            @WikiParameter(description = "<key:dock.theme.param.tabPaddingX>")
            float tabPaddingX
    ) {
        this.tabPaddingX = finiteMax(tabPaddingX, 0.0f, this.tabPaddingX);
        return this;
    }

    /** @return сторона квадрата иконки вкладки */
    @AiDoc(summary = "Returns the tab icon dimension in pixels.")
    @WikiMethod(summary = "<key:dock.theme.method.iconSizeGet>")
    @WikiReturn("<key:dock.theme.return.iconSize>")
    public float iconSize() {
        return iconSize;
    }

    /**
     * @param iconSize сторона иконки вкладки, не меньше 8
     * @return эта тема
     */
    @AiDoc(summary = "Sets the tab icon size.")
    @WikiMethod(summary = "<key:dock.theme.method.iconSizeSet>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme iconSize(
            @WikiParameter(description = "<key:dock.theme.param.iconSize>")
            float iconSize
    ) {
        this.iconSize = finiteMax(iconSize, 8.0f, this.iconSize);
        return this;
    }

    /** @return зазор между иконкой и текстом вкладки */
    @AiDoc(summary = "Returns the gap between tab icon and title text.")
    @WikiMethod(summary = "<key:dock.theme.method.iconGapGet>")
    @WikiReturn("<key:dock.theme.return.iconGap>")
    public float iconGap() {
        return iconGap;
    }

    /** @return сторона квадратной кнопки закрытия вкладки */
    @AiDoc(summary = "Returns the size of the tab close button.")
    @WikiMethod(summary = "<key:dock.theme.method.closeSizeGet>")
    @WikiReturn("<key:dock.theme.return.closeSize>")
    public float closeSize() {
        return closeSize;
    }

    /**
     * @param closeSize сторона кнопки закрытия, не меньше 8
     * @return эта тема
     */
    @AiDoc(summary = "Sets the size of the tab close button.")
    @WikiMethod(summary = "<key:dock.theme.method.closeSizeSet>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme closeSize(
            @WikiParameter(description = "<key:dock.theme.param.closeSize>")
            float closeSize
    ) {
        this.closeSize = finiteMax(closeSize, 8.0f, this.closeSize);
        return this;
    }

    /** @return зазор между текстом и кнопкой закрытия */
    @AiDoc(summary = "Returns the gap between tab text and close button.")
    @WikiMethod(summary = "<key:dock.theme.method.closeGapGet>")
    @WikiReturn("<key:dock.theme.return.closeGap>")
    public float closeGap() {
        return closeGap;
    }

    /** @return минимальная ширина вкладки */
    @AiDoc(summary = "Returns the minimum width of a tab.")
    @WikiMethod(summary = "<key:dock.theme.method.tabMinWidthGet>")
    @WikiReturn("<key:dock.theme.return.tabMinWidth>")
    public float tabMinWidth() {
        return tabMinWidth;
    }

    /**
     * @param tabMinWidth минимальная ширина вкладки, не меньше 24
     * @return эта тема
     */
    @AiDoc(summary = "Sets the minimum tab width.")
    @WikiMethod(summary = "<key:dock.theme.method.tabMinWidthSet>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme tabMinWidth(
            @WikiParameter(description = "<key:dock.theme.param.tabMinWidth>")
            float tabMinWidth
    ) {
        this.tabMinWidth = finiteMax(tabMinWidth, 24.0f, this.tabMinWidth);
        this.tabMaxWidth = Math.max(this.tabMaxWidth, this.tabMinWidth);
        return this;
    }

    /** @return максимальная ширина вкладки */
    @AiDoc(summary = "Returns the maximum width of a tab.")
    @WikiMethod(summary = "<key:dock.theme.method.tabMaxWidthGet>")
    @WikiReturn("<key:dock.theme.return.tabMaxWidth>")
    public float tabMaxWidth() {
        return tabMaxWidth;
    }

    /**
     * @param tabMaxWidth максимальная ширина вкладки, не меньше минимальной
     * @return эта тема
     */
    @AiDoc(summary = "Sets the maximum tab width.")
    @WikiMethod(summary = "<key:dock.theme.method.tabMaxWidthSet>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme tabMaxWidth(
            @WikiParameter(description = "<key:dock.theme.param.tabMaxWidth>")
            float tabMaxWidth
    ) {
        this.tabMaxWidth = finiteMax(tabMaxWidth, this.tabMinWidth, this.tabMaxWidth);
        return this;
    }

    /** @return толщина акцентной линии выбранной вкладки */
    @AiDoc(summary = "Returns the thickness of the active tab accent stripe.")
    @WikiMethod(summary = "<key:dock.theme.method.accentThicknessGet>")
    @WikiReturn("<key:dock.theme.return.accentThickness>")
    public float accentThickness() {
        return accentThickness;
    }

    /**
     * @param accentThickness толщина акцентной линии, не меньше 0
     * @return эта тема
     */
    @AiDoc(summary = "Sets the thickness of the active tab accent stripe.")
    @WikiMethod(summary = "<key:dock.theme.method.accentThicknessSet>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme accentThickness(
            @WikiParameter(description = "<key:dock.theme.param.accentThickness>")
            float accentThickness
    ) {
        this.accentThickness = finiteMax(accentThickness, 0.0f, this.accentThickness);
        return this;
    }

    /** @return толщина разделителя панелей */
    @AiDoc(summary = "Returns the thickness of split handles between docked leaves.")
    @WikiMethod(summary = "<key:dock.theme.method.splitThicknessGet>")
    @WikiReturn("<key:dock.theme.return.splitThickness>")
    public float splitThickness() {
        return splitThickness;
    }

    /**
     * @param splitThickness толщина разделителя панелей, не меньше 1
     * @return эта тема
     */
    @AiDoc(summary = "Sets the thickness of split handles.")
    @WikiMethod(summary = "<key:dock.theme.method.splitThicknessSet>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme splitThickness(
            @WikiParameter(description = "<key:dock.theme.param.splitThickness>")
            float splitThickness
    ) {
        this.splitThickness = finiteMax(splitThickness, 1.0f, this.splitThickness);
        return this;
    }

    /** @return стиль отрисовки разделителя панелей */
    @AiDoc(summary = "Returns the split handle visual style (BLOCK or SEAM).")
    @WikiMethod(summary = "<key:dock.theme.method.splitStyleGet>")
    @WikiReturn("<key:dock.theme.return.splitStyle>")
    public DockingRoot.SplitHandleStyle splitStyle() {
        return splitStyle;
    }

    /**
     * @param splitStyle стиль разделителя; {@code null} означает {@code SEAM}
     * @return эта тема
     */
    @AiDoc(summary = "Sets the split handle visual style.")
    @WikiMethod(summary = "<key:dock.theme.method.splitStyleSet>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme splitStyle(
            @WikiParameter(description = "<key:dock.theme.param.splitStyle>")
            DockingRoot.SplitHandleStyle splitStyle
    ) {
        this.splitStyle = splitStyle == null ? DockingRoot.SplitHandleStyle.SEAM : splitStyle;
        return this;
    }

    /** @return ширина мягкой тени плавающего окна; 0 отключает тень */
    @AiDoc(summary = "Returns the blur size of floating window drop shadows.")
    @WikiMethod(summary = "<key:dock.theme.method.windowShadowSizeGet>")
    @WikiReturn("<key:dock.theme.return.windowShadowSize>")
    public float windowShadowSize() {
        return windowShadowSize;
    }

    /**
     * @param windowShadowSize ширина тени плавающего окна, не меньше 0
     * @return эта тема
     */
    @AiDoc(summary = "Sets the blur size of floating window drop shadows (0 to disable).")
    @WikiMethod(summary = "<key:dock.theme.method.windowShadowSizeSet>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme windowShadowSize(
            @WikiParameter(description = "<key:dock.theme.param.windowShadowSize>")
            float windowShadowSize
    ) {
        this.windowShadowSize = finiteMax(windowShadowSize, 0.0f, this.windowShadowSize);
        return this;
    }

    /** @return радиус скругления плавающего окна */
    @AiDoc(summary = "Returns the corner radius of floating dock windows.")
    @WikiMethod(summary = "<key:dock.theme.method.windowRadiusGet>")
    @WikiReturn("<key:dock.theme.return.windowRadius>")
    public float windowRadius() {
        return windowRadius;
    }

    /**
     * @param windowRadius радиус скругления окна, не меньше 0
     * @return эта тема
     */
    @AiDoc(summary = "Sets the corner radius of floating dock windows.")
    @WikiMethod(summary = "<key:dock.theme.method.windowRadiusSet>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme windowRadius(
            @WikiParameter(description = "<key:dock.theme.param.windowRadius>")
            float windowRadius
    ) {
        this.windowRadius = finiteMax(windowRadius, 0.0f, this.windowRadius);
        return this;
    }

    /** @return ширина компактного окна, пока вкладку вытягивают из дока */
    @AiDoc(summary = "Returns the tear-off preview window width.")
    @WikiMethod(summary = "<key:dock.theme.method.tearOffWidthGet>")
    @WikiReturn("<key:dock.theme.return.tearOffWidth>")
    public float tearOffWidth() {
        return tearOffWidth;
    }

    /** @return высота компактного окна, пока вкладку вытягивают из дока */
    @AiDoc(summary = "Returns the tear-off preview window height.")
    @WikiMethod(summary = "<key:dock.theme.method.tearOffHeightGet>")
    @WikiReturn("<key:dock.theme.return.tearOffHeight>")
    public float tearOffHeight() {
        return tearOffHeight;
    }

    /**
     * Размер компактного «призрачного» окна, которым становится вытянутая вкладка на время перетаскивания.
     * После отпускания (если окно ни к чему не пристыковалось) оно принимает свой обычный размер.
     *
     * @param width ширина; 0 или меньше отключает компактный режим
     * @param height высота; 0 или меньше отключает компактный режим
     * @return эта тема
     */
    @AiDoc(summary = "Configures compact preview dimensions while a tab is being torn off from the dock.")
    @WikiMethod(summary = "<key:dock.theme.method.tearOffPreviewSize>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme tearOffPreviewSize(
            @WikiParameter(description = "<key:dock.theme.param.tearOffWidth>")
            float width,
            @WikiParameter(description = "<key:dock.theme.param.tearOffHeight>")
            float height
    ) {
        this.tearOffWidth = Float.isFinite(width) ? Math.max(0.0f, width) : this.tearOffWidth;
        this.tearOffHeight = Float.isFinite(height) ? Math.max(0.0f, height) : this.tearOffHeight;
        return this;
    }

    /**
     * Включён ли компактный режим вытягивания.
     *
     * @return {@code true}, если оба размера положительны
     */
    @AiDoc(summary = "Returns true if tear-off compact preview sizing is enabled.")
    @WikiMethod(summary = "<key:dock.theme.method.tearOffPreviewEnabled>")
    @WikiReturn("<key:dock.theme.return.tearOffPreviewEnabled>")
    public boolean tearOffPreviewEnabled() {
        return tearOffWidth > 0.0f && tearOffHeight > 0.0f;
    }

    /** @return сторона квадрата ручки изменения размера окна (и её зоны нажатия) */
    @AiDoc(summary = "Returns the side size of the bottom-right window resize grip.")
    @WikiMethod(summary = "<key:dock.theme.method.gripSizeGet>")
    @WikiReturn("<key:dock.theme.return.gripSize>")
    public float gripSize() {
        return gripSize;
    }

    /**
     * @param gripSize сторона ручки изменения размера в правом нижнем углу окна, не меньше 6
     * @return эта тема
     */
    @AiDoc(summary = "Sets the side size of the window resize grip.")
    @WikiMethod(summary = "<key:dock.theme.method.gripSizeSet>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme gripSize(
            @WikiParameter(description = "<key:dock.theme.param.gripSize>")
            float gripSize
    ) {
        this.gripSize = finiteMax(gripSize, 6.0f, this.gripSize);
        return this;
    }

    /** @return рисунок ручки изменения размера окна */
    @AiDoc(summary = "Returns the current window resize grip style.")
    @WikiMethod(summary = "<key:dock.theme.method.resizeGripGet>")
    @WikiReturn("<key:dock.theme.return.resizeGrip>")
    public ResizeGripStyle resizeGrip() {
        return resizeGrip;
    }

    /**
     * Задаёт рисунок ручки изменения размера окна: {@link ResizeGripStyle#TRIANGLE} (по умолчанию),
     * {@link ResizeGripStyle#LINES}, {@link ResizeGripStyle#DOTS}, {@link ResizeGripStyle#NONE} или своя
     * лямбда.
     *
     * @param resizeGrip рисунок ручки; {@code null} означает треугольник
     * @return эта тема
     */
    @AiDoc(summary = "Sets the window resize grip visual style.")
    @WikiMethod(summary = "<key:dock.theme.method.resizeGripSet>")
    @WikiReturn("<key:dock.theme.return.this>")
    public DockTheme resizeGrip(
            @WikiParameter(description = "<key:dock.theme.param.resizeGrip>")
            ResizeGripStyle resizeGrip
    ) {
        this.resizeGrip = resizeGrip == null ? ResizeGripStyle.TRIANGLE : resizeGrip;
        return this;
    }

    private static float finiteMax(float value, float min, float fallback) {
        return Float.isFinite(value) ? Math.max(min, value) : fallback;
    }
}

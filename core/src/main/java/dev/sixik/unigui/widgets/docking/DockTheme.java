package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

import java.util.Objects;

/**
 * Визуальная тема системы докинга: цвета по ролям ({@link DockColor}) и геометрические метрики.
 *
 * <p>Тема общая для {@link DockingRoot} (вкладки, разделители, зоны дропа, меню переполнения) и для
 * {@code WindowWidget} (заголовок, рамка, тень), поэтому плавающее окно и докнутая панель выглядят как
 * части одного редактора. Готовые наборы: {@link #editor()} (современный нейтральный тёмный стиль в духе
 * Unity/Godot, значение по умолчанию) и {@link #classic()} (прежний сине-голубой вид).</p>
 *
 * <p>Тема — обычный изменяемый объект: {@code theme.color(DockColor.TAB_ACCENT).set(...)} перекрашивает
 * все виджеты, использующие эту тему, на следующем кадре. Глобальная тема по умолчанию меняется через
 * {@link #defaultTheme(DockTheme)}; отдельный root получает свою через {@link DockingRoot#theme(DockTheme)}.</p>
 */
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
    public static DockTheme defaultTheme() {
        return defaultTheme;
    }

    /**
     * Заменяет глобальную тему по умолчанию.
     *
     * @param theme новая тема; {@code null} возвращает {@link #editor()}
     */
    public static void defaultTheme(DockTheme theme) {
        defaultTheme = theme == null ? editor() : theme;
    }

    /**
     * Современная нейтральная тёмная тема: серые панели, синий акцент у активной вкладки, плоские
     * разделители в одну линию, тень у плавающих окон.
     *
     * @return новый экземпляр темы
     */
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
    public MutableColor color(DockColor role) {
        return colors[role.ordinal()];
    }

    /**
     * Задаёт цвет роли.
     *
     * @param role роль цвета
     * @param color новый цвет
     * @return эта тема для fluent-настройки
     */
    public DockTheme color(DockColor role, ColorView color) {
        colors[role.ordinal()].set(Objects.requireNonNull(color, "color"));
        return this;
    }

    private void set(DockColor role, float r, float g, float b, float a) {
        colors[role.ordinal()].set(r, g, b, a);
    }

    /** @return высота полосы вкладок и заголовка окна в UI-пикселях */
    public float tabHeight() {
        return tabHeight;
    }

    /**
     * @param tabHeight высота полосы вкладок, не меньше 14
     * @return эта тема
     */
    public DockTheme tabHeight(float tabHeight) {
        this.tabHeight = finiteMax(tabHeight, 14.0f, this.tabHeight);
        return this;
    }

    /** @return радиус верхних углов вкладки */
    public float tabRadius() {
        return tabRadius;
    }

    /**
     * @param tabRadius радиус верхних углов вкладки, не меньше 0
     * @return эта тема
     */
    public DockTheme tabRadius(float tabRadius) {
        this.tabRadius = finiteMax(tabRadius, 0.0f, this.tabRadius);
        return this;
    }

    /** @return внутренний горизонтальный отступ вкладки */
    public float tabPaddingX() {
        return tabPaddingX;
    }

    /**
     * @param tabPaddingX внутренний отступ вкладки слева и справа, не меньше 0
     * @return эта тема
     */
    public DockTheme tabPaddingX(float tabPaddingX) {
        this.tabPaddingX = finiteMax(tabPaddingX, 0.0f, this.tabPaddingX);
        return this;
    }

    /** @return сторона квадрата иконки вкладки */
    public float iconSize() {
        return iconSize;
    }

    /**
     * @param iconSize сторона иконки вкладки, не меньше 8
     * @return эта тема
     */
    public DockTheme iconSize(float iconSize) {
        this.iconSize = finiteMax(iconSize, 8.0f, this.iconSize);
        return this;
    }

    /** @return зазор между иконкой и текстом вкладки */
    public float iconGap() {
        return iconGap;
    }

    /** @return сторона квадратной кнопки закрытия вкладки */
    public float closeSize() {
        return closeSize;
    }

    /**
     * @param closeSize сторона кнопки закрытия, не меньше 8
     * @return эта тема
     */
    public DockTheme closeSize(float closeSize) {
        this.closeSize = finiteMax(closeSize, 8.0f, this.closeSize);
        return this;
    }

    /** @return зазор между текстом и кнопкой закрытия */
    public float closeGap() {
        return closeGap;
    }

    /** @return минимальная ширина вкладки */
    public float tabMinWidth() {
        return tabMinWidth;
    }

    /**
     * @param tabMinWidth минимальная ширина вкладки, не меньше 24
     * @return эта тема
     */
    public DockTheme tabMinWidth(float tabMinWidth) {
        this.tabMinWidth = finiteMax(tabMinWidth, 24.0f, this.tabMinWidth);
        this.tabMaxWidth = Math.max(this.tabMaxWidth, this.tabMinWidth);
        return this;
    }

    /** @return максимальная ширина вкладки */
    public float tabMaxWidth() {
        return tabMaxWidth;
    }

    /**
     * @param tabMaxWidth максимальная ширина вкладки, не меньше минимальной
     * @return эта тема
     */
    public DockTheme tabMaxWidth(float tabMaxWidth) {
        this.tabMaxWidth = finiteMax(tabMaxWidth, this.tabMinWidth, this.tabMaxWidth);
        return this;
    }

    /** @return толщина акцентной линии выбранной вкладки */
    public float accentThickness() {
        return accentThickness;
    }

    /**
     * @param accentThickness толщина акцентной линии, не меньше 0
     * @return эта тема
     */
    public DockTheme accentThickness(float accentThickness) {
        this.accentThickness = finiteMax(accentThickness, 0.0f, this.accentThickness);
        return this;
    }

    /** @return толщина разделителя панелей */
    public float splitThickness() {
        return splitThickness;
    }

    /**
     * @param splitThickness толщина разделителя панелей, не меньше 1
     * @return эта тема
     */
    public DockTheme splitThickness(float splitThickness) {
        this.splitThickness = finiteMax(splitThickness, 1.0f, this.splitThickness);
        return this;
    }

    /** @return стиль отрисовки разделителя панелей */
    public DockingRoot.SplitHandleStyle splitStyle() {
        return splitStyle;
    }

    /**
     * @param splitStyle стиль разделителя; {@code null} означает {@code SEAM}
     * @return эта тема
     */
    public DockTheme splitStyle(DockingRoot.SplitHandleStyle splitStyle) {
        this.splitStyle = splitStyle == null ? DockingRoot.SplitHandleStyle.SEAM : splitStyle;
        return this;
    }

    /** @return ширина мягкой тени плавающего окна; 0 отключает тень */
    public float windowShadowSize() {
        return windowShadowSize;
    }

    /**
     * @param windowShadowSize ширина тени плавающего окна, не меньше 0
     * @return эта тема
     */
    public DockTheme windowShadowSize(float windowShadowSize) {
        this.windowShadowSize = finiteMax(windowShadowSize, 0.0f, this.windowShadowSize);
        return this;
    }

    /** @return радиус скругления плавающего окна */
    public float windowRadius() {
        return windowRadius;
    }

    /**
     * @param windowRadius радиус скругления окна, не меньше 0
     * @return эта тема
     */
    public DockTheme windowRadius(float windowRadius) {
        this.windowRadius = finiteMax(windowRadius, 0.0f, this.windowRadius);
        return this;
    }

    /** @return ширина компактного окна, пока вкладку вытягивают из дока */
    public float tearOffWidth() {
        return tearOffWidth;
    }

    /** @return высота компактного окна, пока вкладку вытягивают из дока */
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
    public DockTheme tearOffPreviewSize(float width, float height) {
        this.tearOffWidth = Float.isFinite(width) ? Math.max(0.0f, width) : this.tearOffWidth;
        this.tearOffHeight = Float.isFinite(height) ? Math.max(0.0f, height) : this.tearOffHeight;
        return this;
    }

    /**
     * Включён ли компактный режим вытягивания.
     *
     * @return {@code true}, если оба размера положительны
     */
    public boolean tearOffPreviewEnabled() {
        return tearOffWidth > 0.0f && tearOffHeight > 0.0f;
    }

    /** @return сторона квадрата ручки изменения размера окна (и её зоны нажатия) */
    public float gripSize() {
        return gripSize;
    }

    /**
     * @param gripSize сторона ручки изменения размера в правом нижнем углу окна, не меньше 6
     * @return эта тема
     */
    public DockTheme gripSize(float gripSize) {
        this.gripSize = finiteMax(gripSize, 6.0f, this.gripSize);
        return this;
    }

    /** @return рисунок ручки изменения размера окна */
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
    public DockTheme resizeGrip(ResizeGripStyle resizeGrip) {
        this.resizeGrip = resizeGrip == null ? ResizeGripStyle.TRIANGLE : resizeGrip;
        return this;
    }

    private static float finiteMax(float value, float min, float fallback) {
        return Float.isFinite(value) ? Math.max(min, value) : fallback;
    }
}

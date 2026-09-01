package dev.sixik.unigui.widgets.world;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.RoutableWidgetEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.event.ViewportChangedEvent;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.viewport.Viewport2D;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Базовый 2D-холст с панорамированием, зумом и привязкой обычных виджетов к
 * координатам виртуального мира.
 *
 * <p>{@code WorldCanvas} сам ничего не знает про карты, ноды или игровые
 * координаты. Он хранит только {@link Viewport2D}: смещение экрана
 * {@code x/y}, текущий {@code zoom}, диапазон зума и опциональные границы мира.
 * Все координаты {@code worldX/worldY} задаются в произвольных единицах
 * вызывающего кода.</p>
 *
 * <p>Есть два способа рисовать содержимое:</p>
 *
 * <ul>
 *     <li>{@link WorldLayer} — лёгкий render-only слой для сеток, линий,
 *     фоновых шейпов и прочей геометрии, которой не нужны layout/events.</li>
 *     <li>{@link AnchorLayer} / {@link AnchorWidget} — слой обычных виджетов,
 *     которые проецируются из world-space в screen-space. Размер таких
 *     виджетов задаётся в пикселях экрана и не масштабируется зумом, если
 *     пользователь явно не сделает это сам.</li>
 * </ul>
 *
 * <p>Минимальный пример:</p>
 *
 * <pre>{@code
 * WorldCanvas canvas = new WorldCanvas()
 *         .viewport(140.0f, 92.0f, 1.0f)
 *         .zoomRange(0.45f, 3.5f)
 *         .worldBounds(-2048.0f, -1024.0f, 4096.0f, 2048.0f)
 *         .clampToWorldBounds(true);
 *
 * canvas.addWorldLayer((world, draw) -> {
 *     float x = world.worldToRootX(nodeWorldX);
 *     float y = world.worldToRootY(nodeWorldY);
 *     draw.addCircleFilled(x, y, 4.0f, color);
 * });
 *
 * canvas.anchorLayer().add("shop", shopWorldX, shopWorldY, shopButton)
 *         .screenSize(88.0f, 24.0f)
 *         .pivot(0.5f, 0.5f);
 * }</pre>
 *
 * @see Viewport2D
 * @see WorldLayer
 * @see AnchorLayer
 * @see dev.sixik.unigui.widgets.map.MapCanvas
 */
@XmlWidgetName("WorldCanvas")
public class WorldCanvas extends WidgetBase {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.WORLD_CANVAS;

    public static final float DEFAULT_PREFERRED_WIDTH = 320.0f;
    public static final float DEFAULT_PREFERRED_HEIGHT = 240.0f;

    private Viewport2D viewport = new Viewport2D();
    /**
     * Screen-space виджеты, привязанные к world-space точкам.
     */
    private final AnchorLayer anchorLayer = new AnchorLayer(this);
    /**
     * Render-only слои. Они рисуются до {@link #anchorLayer}, то есть под
     * интерактивными якорями.
     */
    private final ObjectArrayList<WorldLayer> worldLayers = new ObjectArrayList<>();
    private final List<WorldLayer> worldLayersView = Collections.unmodifiableList(worldLayers);
    private final ObjectArrayList<Widget> children = new ObjectArrayList<>();
    private final List<Widget> childrenView = Collections.unmodifiableList(children);

    /*
     * Снапшоты нужны для горячих циклов tick/render: структура меняется редко,
     * а обходить массив дешевле, чем каждый кадр создавать iterator.
     */
    private Widget[] childSnapshot = new Widget[0];
    private boolean childSnapshotDirty = true;
    private WorldLayer[] worldLayerSnapshot = new WorldLayer[0];
    private boolean worldLayerSnapshotDirty = true;

    private boolean clippingEnabled = true;
    private boolean panningEnabled = true;
    private boolean zoomEnabled = true;
    private boolean wheelPanningEnabled = true;
    private boolean consumeWheelWhileHovered = true;
    private float wheelPanStep = 32.0f;
    private float zoomStep = 1.1f;
    private PointerButton panButton = PointerButton.PRIMARY;
    private float preferredWidth = DEFAULT_PREFERRED_WIDTH;
    private float preferredHeight = DEFAULT_PREFERRED_HEIGHT;
    private PanDragState panDrag;

    /**
     * Возвращает живую модель viewport'а.
     *
     * <p>Через неё можно читать текущие {@code x/y/zoom}. Если нужно менять
     * viewport и автоматически получить invalidate + {@link ViewportChangedEvent},
     * предпочитай fluent-методы самого {@code WorldCanvas}: {@link #viewport(float, float, float)},
     * {@link #zoomRange(float, float)}, {@link #worldBounds(float, float, float, float)}.</p>
     *
     * @return текущий {@link Viewport2D}
     */
    public Viewport2D viewport() {
        return viewport;
    }

    /**
     * Подменяет модель viewport'а.
     *
     * <p>{@code null} безопасно заменяется на новый пустой {@link Viewport2D}.
     * Метод полезен, если один и тот же viewport нужно шарить между несколькими
     * компонентами или заранее настроить снаружи.</p>
     *
     * @param viewport новая модель viewport'а
     * @return этот canvas для fluent-настройки
     */
    public WorldCanvas viewport(Viewport2D viewport) {
        Viewport2D next = viewport == null ? new Viewport2D() : viewport;
        if (this.viewport == next) return this;
        float oldX = this.viewport.x();
        float oldY = this.viewport.y();
        float oldZoom = this.viewport.zoom();
        this.viewport = next;
        afterViewportChanged(oldX, oldY, oldZoom);
        return this;
    }

    /**
     * Меняет только screen-space смещение viewport'а, сохраняя текущий zoom.
     *
     * <p>Смещение задаётся в пикселях локального пространства canvas'а:
     * {@code screenX = viewportX + worldX * zoom}.</p>
     *
     * @param x смещение по X в screen-space
     * @param y смещение по Y в screen-space
     * @return этот canvas для fluent-настройки
     */
    public WorldCanvas viewport(float x, float y) {
        return viewport(x, y, viewport.zoom());
    }

    /**
     * Меняет screen-space смещение и масштаб viewport'а.
     *
     * <p>{@code zoom} автоматически приводится к текущему диапазону
     * {@link #zoomRange(float, float)}.</p>
     *
     * @param x screen-space смещение по X
     * @param y screen-space смещение по Y
     * @param zoom масштаб мира относительно экрана
     * @return этот canvas для fluent-настройки
     */
    public WorldCanvas viewport(float x, float y, float zoom) {
        float oldX = viewport.x();
        float oldY = viewport.y();
        float oldZoom = viewport.zoom();
        if (!viewport.set(x, y, zoom)) return this;
        afterViewportChanged(oldX, oldY, oldZoom);
        return this;
    }

    /**
     * Задаёт допустимый диапазон зума.
     *
     * <p>Если текущий zoom выходит за новый диапазон, он будет сразу зажат, а
     * anchors будут переложены под новое состояние viewport'а.</p>
     *
     * @param minZoom минимальный положительный zoom
     * @param maxZoom максимальный zoom, не меньше {@code minZoom}
     * @return этот canvas для fluent-настройки
     */
    public WorldCanvas zoomRange(float minZoom, float maxZoom) {
        float oldX = viewport.x();
        float oldY = viewport.y();
        float oldZoom = viewport.zoom();
        viewport.zoomRange(minZoom, maxZoom);
        if (oldX != viewport.x() || oldY != viewport.y() || oldZoom != viewport.zoom()) {
            afterViewportChanged(oldX, oldY, oldZoom);
        }
        return this;
    }

    /**
     * Задаёт прямоугольник доступной области мира.
     *
     * <p>Сами по себе границы только сохраняются в {@link Viewport2D}. Чтобы
     * реально зажимать pan/zoom внутри них, включи {@link #clampToWorldBounds(boolean)}.</p>
     *
     * @param x левая граница мира
     * @param y верхняя граница мира
     * @param width ширина мира
     * @param height высота мира
     * @return этот canvas для fluent-настройки
     */
    public WorldCanvas worldBounds(float x, float y, float width, float height) {
        viewport.worldBounds(x, y, width, height);
        clampViewport();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Убирает сохранённые границы мира.
     *
     * @return этот canvas для fluent-настройки
     */
    public WorldCanvas clearWorldBounds() {
        viewport.clearWorldBounds();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Включает или выключает удержание viewport'а внутри {@link #worldBounds(float, float, float, float)}.
     *
     * <p>Если контент меньше самого canvas'а, viewport центрируется по этой оси.</p>
     *
     * @param clampToWorldBounds {@code true}, чтобы зажимать pan/zoom по границам мира
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "clampToWorldBounds", category = "Behavior", defaultValue = "false", description = "Whether pan/zoom is constrained to configured world bounds.")
    public WorldCanvas clampToWorldBounds(boolean clampToWorldBounds) {
        viewport.clampToWorldBounds(clampToWorldBounds);
        clampViewport();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает слой якорей — обычных виджетов, привязанных к world-space координатам.
     *
     * <p>Через него добавляются маркеры, кнопки, тултипы и любые другие виджеты,
     * которые должны стоять в точке мира, но оставаться screen-space UI.</p>
     *
     * @return слой якорей этого canvas'а
     */
    public AnchorLayer anchorLayer() {
        return anchorLayer;
    }

    /**
     * Возвращает read-only список render-only слоёв в порядке отрисовки.
     *
     * @return неизменяемое представление слоёв мира
     */
    public List<WorldLayer> worldLayers() {
        return worldLayersView;
    }

    /**
     * Добавляет render-only слой мира.
     *
     * <p>Слой будет вызываться каждый render до отрисовки anchor-виджетов.
     * Это правильное место для сетки, линий связей, фона, glow-геометрии и
     * прочих элементов, которым не нужны layout и события.</p>
     *
     * @param layer слой для добавления
     * @return этот canvas для fluent-настройки
     */
    public WorldCanvas addWorldLayer(WorldLayer layer) {
        if (layer == null) return this;
        worldLayers.add(layer);
        worldLayerSnapshotDirty = true;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Удаляет render-only слой мира.
     *
     * @param layer слой для удаления
     * @return этот canvas для fluent-настройки
     */
    public WorldCanvas removeWorldLayer(WorldLayer layer) {
        if (layer == null || !worldLayers.remove(layer)) return this;
        worldLayerSnapshotDirty = true;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Удаляет все render-only слои мира.
     *
     * <p>Anchor-виджеты при этом не трогаются. Для них используй
     * {@link #anchorLayer()}.{@code clear()}.</p>
     *
     * @return этот canvas для fluent-настройки
     */
    public WorldCanvas clearWorldLayers() {
        if (worldLayers.isEmpty()) return this;
        worldLayers.clear();
        worldLayerSnapshotDirty = true;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Проверяет, включён ли clip по границам canvas'а.
     *
     * @return {@code true}, если world layers и anchors режутся по layout bounds
     */
    public boolean clippingEnabled() {
        return clippingEnabled;
    }

    /**
     * Управляет clip'ом содержимого по bounds canvas'а.
     *
     * <p>Обычно стоит держать включённым. Выключать имеет смысл для debug-оверлеев
     * или эффектов, которые должны выходить за пределы canvas'а.</p>
     *
     * @param clippingEnabled {@code true}, чтобы резать содержимое по bounds
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "clippingEnabled", category = "Behavior", defaultValue = "true", description = "Whether world layers and anchors are clipped to canvas bounds.")
    public WorldCanvas clippingEnabled(boolean clippingEnabled) {
        if (this.clippingEnabled == clippingEnabled) return this;
        this.clippingEnabled = clippingEnabled;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Проверяет, включён ли drag-pan мышью.
     *
     * @return {@code true}, если canvas реагирует на {@link #panButton()}
     */
    public boolean panningEnabled() {
        return panningEnabled;
    }

    /**
     * Включает или выключает drag-pan мышью.
     *
     * @param panningEnabled {@code true}, чтобы разрешить перетаскивание viewport'а
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "panningEnabled", category = "Behavior", defaultValue = "true", description = "Whether pointer drag can pan the viewport.")
    public WorldCanvas panningEnabled(boolean panningEnabled) {
        this.panningEnabled = panningEnabled;
        return this;
    }

    /**
     * Проверяет, включён ли zoom через {@code Ctrl + колесо}.
     *
     * @return {@code true}, если zoom жестом разрешён
     */
    public boolean zoomEnabled() {
        return zoomEnabled;
    }

    /**
     * Включает или выключает zoom через {@code Ctrl + колесо}.
     *
     * @param zoomEnabled {@code true}, чтобы разрешить интерактивный zoom
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "zoomEnabled", category = "Behavior", defaultValue = "true", description = "Whether Ctrl-wheel zoom gestures are enabled.")
    public WorldCanvas zoomEnabled(boolean zoomEnabled) {
        this.zoomEnabled = zoomEnabled;
        return this;
    }

    /**
     * Проверяет, включён ли pan колесом/тачпадом без {@code Ctrl}.
     *
     * @return {@code true}, если scroll двигает viewport
     */
    public boolean wheelPanningEnabled() {
        return wheelPanningEnabled;
    }

    /**
     * Включает или выключает pan колесом/тачпадом без {@code Ctrl}.
     *
     * <p>{@code Shift + колесо} превращает вертикальный scroll в горизонтальный.</p>
     *
     * @param wheelPanningEnabled {@code true}, чтобы scroll двигал viewport
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "wheelPanningEnabled", category = "Behavior", defaultValue = "true", description = "Whether wheel gestures pan the viewport.")
    public WorldCanvas wheelPanningEnabled(boolean wheelPanningEnabled) {
        this.wheelPanningEnabled = wheelPanningEnabled;
        return this;
    }

    /**
     * Проверяет, будет ли canvas поглощать scroll, даже если viewport не изменился.
     *
     * @return {@code true}, если scroll внутри hovered canvas отменяется
     */
    public boolean consumeWheelWhileHovered() {
        return consumeWheelWhileHovered;
    }

    /**
     * Управляет поглощением scroll-событий, пока курсор находится над canvas'ом.
     *
     * <p>Если включено, родительские scroll-контейнеры не будут прокручиваться,
     * когда пользователь работает с картой/графом.</p>
     *
     * @param consumeWheelWhileHovered {@code true}, чтобы отменять scroll внутри canvas'а
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "consumeWheelWhileHovered", category = "Behavior", defaultValue = "true", description = "Whether wheel events are consumed while the canvas is hovered.")
    public WorldCanvas consumeWheelWhileHovered(boolean consumeWheelWhileHovered) {
        this.consumeWheelWhileHovered = consumeWheelWhileHovered;
        return this;
    }

    /**
     * Возвращает множитель перемещения viewport'а на один шаг scroll.
     *
     * @return шаг pan в screen-space пикселях
     */
    public float wheelPanStep() {
        return wheelPanStep;
    }

    /**
     * Задаёт множитель перемещения viewport'а на один шаг scroll.
     *
     * @param wheelPanStep шаг pan в screen-space пикселях, отрицательные/NaN значения нормализуются
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "wheelPanStep", category = "Behavior", defaultValue = "32", description = "Viewport pan distance applied per wheel delta unit.")
    public WorldCanvas wheelPanStep(float wheelPanStep) {
        this.wheelPanStep = Float.isFinite(wheelPanStep) ? Math.max(0.0f, wheelPanStep) : 32.0f;
        return this;
    }

    /**
     * Возвращает multiplicative factor для zoom-шага.
     *
     * @return множитель зума на один шаг колеса
     */
    public float zoomStep() {
        return zoomStep;
    }

    /**
     * Задаёт multiplicative factor для {@code Ctrl + колесо}.
     *
     * <p>Например, {@code 1.1f} означает примерно +10%/-9% за один шаг.</p>
     *
     * @param zoomStep положительный множитель зума
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "zoomStep", category = "Behavior", defaultValue = "1.1", description = "Multiplicative zoom factor applied per wheel delta unit.")
    public WorldCanvas zoomStep(float zoomStep) {
        this.zoomStep = Float.isFinite(zoomStep) && zoomStep > 0.0f ? zoomStep : 1.1f;
        return this;
    }

    /**
     * Возвращает кнопку мыши, которая начинает drag-pan.
     *
     * @return текущая кнопка pan
     */
    public PointerButton panButton() {
        return panButton;
    }

    /**
     * Задаёт кнопку мыши для drag-pan.
     *
     * @param panButton кнопка pan; {@code null} возвращает {@link PointerButton#PRIMARY}
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "panButton", category = "Behavior", defaultValue = "primary", description = "Pointer button that starts drag panning.")
    public WorldCanvas panButton(PointerButton panButton) {
        this.panButton = panButton == null ? PointerButton.PRIMARY : panButton;
        return this;
    }

    /**
     * Возвращает intrinsic-ширину canvas'а, используемую при {@code AUTO} layout-size.
     *
     * @return preferred ширина в screen-space пикселях
     */
    public float preferredWidth() {
        return preferredWidth;
    }

    /**
     * Задаёт intrinsic-ширину canvas'а без записи явного размера в layout-style.
     *
     * @param preferredWidth preferred ширина; неположительные/NaN значения сбрасываются к default
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "preferredWidth", category = "Layout", defaultValue = "320", description = "Intrinsic canvas width before layout constraints are applied.")
    public WorldCanvas preferredWidth(float preferredWidth) {
        float normalized = positiveOr(preferredWidth, DEFAULT_PREFERRED_WIDTH);
        if (this.preferredWidth == normalized) return this;
        this.preferredWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает intrinsic-высоту canvas'а, используемую при {@code AUTO} layout-size.
     *
     * @return preferred высота в screen-space пикселях
     */
    public float preferredHeight() {
        return preferredHeight;
    }

    /**
     * Задаёт intrinsic-высоту canvas'а без записи явного размера в layout-style.
     *
     * @param preferredHeight preferred высота; неположительные/NaN значения сбрасываются к default
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "preferredHeight", category = "Layout", defaultValue = "240", description = "Intrinsic canvas height before layout constraints are applied.")
    public WorldCanvas preferredHeight(float preferredHeight) {
        float normalized = positiveOr(preferredHeight, DEFAULT_PREFERRED_HEIGHT);
        if (this.preferredHeight == normalized) return this;
        this.preferredHeight = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Задаёт intrinsic-размер canvas'а для авто-компоновки.
     *
     * @param width preferred ширина
     * @param height preferred высота
     * @return этот canvas для fluent-настройки
     */
    public WorldCanvas preferredSize(float width, float height) {
        return preferredWidth(width).preferredHeight(height);
    }

    /**
     * Переводит X из world-space в root-space.
     *
     * <p>Root-space — координаты верхнего UI-дерева/экрана, то есть уже с
     * учётом позиции самого {@code WorldCanvas}.</p>
     *
     * @param worldX X в координатах мира
     * @return X в root-space
     */
    public float worldToRootX(float worldX) {
        return layoutBounds().x() + viewport.worldToScreenX(worldX);
    }

    /**
     * Переводит Y из world-space в root-space.
     *
     * @param worldY Y в координатах мира
     * @return Y в root-space
     */
    public float worldToRootY(float worldY) {
        return layoutBounds().y() + viewport.worldToScreenY(worldY);
    }

    /**
     * Переводит X из root-space обратно в world-space.
     *
     * @param rootX X в координатах root UI
     * @return X в координатах мира
     */
    public float rootToWorldX(float rootX) {
        return viewport.screenToWorldX(rootX - layoutBounds().x());
    }

    /**
     * Переводит Y из root-space обратно в world-space.
     *
     * @param rootY Y в координатах root UI
     * @return Y в координатах мира
     */
    public float rootToWorldY(float rootY) {
        return viewport.screenToWorldY(rootY - layoutBounds().y());
    }

    /**
     * Переводит X из world-space в local-space canvas'а.
     *
     * <p>Local-space начинается в левом верхнем углу самого canvas'а.</p>
     *
     * @param worldX X в координатах мира
     * @return X внутри canvas'а
     */
    public float worldToLocalX(float worldX) {
        return viewport.worldToScreenX(worldX);
    }

    /**
     * Переводит Y из world-space в local-space canvas'а.
     *
     * @param worldY Y в координатах мира
     * @return Y внутри canvas'а
     */
    public float worldToLocalY(float worldY) {
        return viewport.worldToScreenY(worldY);
    }

    /**
     * Переводит X из local-space canvas'а в world-space.
     *
     * @param localX X внутри canvas'а
     * @return X в координатах мира
     */
    public float localToWorldX(float localX) {
        return viewport.screenToWorldX(localX);
    }

    /**
     * Переводит Y из local-space canvas'а в world-space.
     *
     * @param localY Y внутри canvas'а
     * @return Y в координатах мира
     */
    public float localToWorldY(float localY) {
        return viewport.screenToWorldY(localY);
    }

    /**
     * Подписывает слушатель на изменение viewport'а.
     *
     * <p>Событие приходит после drag-pan, wheel-pan, zoom, программного
     * изменения viewport'а и clamp'а, если фактические {@code x/y/zoom}
     * изменились.</p>
     *
     * @param listener слушатель события
     * @return подписка, которую можно отменить
     */
    public EventSubscription onViewportChanged(EventListener<? super ViewportChangedEvent> listener) {
        return on(ViewportChangedEvent.TYPE, listener);
    }

    @Override
    public List<Widget> children() {
        return childrenView;
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        Object[] raw = children.elements();
        for (int i = 0, size = children.size(); i < size; i++) {
            Widget child = (Widget) raw[i];
            if (child instanceof WidgetBase base) {
                base.setUiContextInternal(uiContext);
            }
        }
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        LayoutContext childContext = new LayoutContext(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        AnchorWidget[] anchors = anchorLayer.snapshot();
        for (AnchorWidget anchor : anchors) {
            Widget child = anchor.widget();
            if (child.visibility() != Visibility.COLLAPSED) {
                child.measure(childContext);
            }
        }
        setDesiredSize(resolveDesiredSize(context, preferredWidth, preferredHeight));
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        clampViewport();
        arrangeAnchors();
    }

    @Override
    public void tick(FrameContext frame) {
        if (visibility() != Visibility.VISIBLE) return;
        super.tick(frame);
        Widget[] snapshot = childSnapshot();
        for (Widget child : snapshot) {
            if (child.visibility() == Visibility.VISIBLE) {
                child.tick(frame);
            }
        }
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE || context == null) return;
        if (layoutBounds().width() <= 0.0f || layoutBounds().height() <= 0.0f) return;

        pushOpacity(context);
        boolean pushedClip = false;
        try {
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (clippingEnabled) {
                draw.pushClip(layoutBounds().x(), layoutBounds().y(), layoutBounds().width(), layoutBounds().height());
                pushedClip = true;
            }

            // Сначала рисуем дешёвые render-only слои: фон, сетку, связи, декоративную геометрию.
            WorldLayer[] layers = worldLayerSnapshot();
            for (WorldLayer layer : layers) {
                layer.render(this, draw);
            }

            // Потом рендерим обычные виджеты-якоря поверх world layers.
            AnchorWidget[] anchors = anchorLayer.snapshot();
            for (AnchorWidget anchor : anchors) {
                if (!anchor.arrangedVisible()) continue;
                renderChildWithInheritedTransform(context, anchor.widget());
            }
        } finally {
            if (pushedClip) {
                context.popClip();
            }
            popOpacity(context);
        }
    }

    @Override
    public void handle(Event event) {
        if (visibility() != Visibility.VISIBLE || !enabled()) return;
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.CAPTURE) return;

        if (event instanceof PointerPressedEvent pointer) {
            handlePointerPressed(pointer, event);
        } else if (event instanceof PointerMovedEvent pointer) {
            handlePointerMoved(pointer);
        } else if (event instanceof PointerReleasedEvent pointer) {
            handlePointerReleased(pointer, event);
        } else if (event instanceof ScrollEvent scroll) {
            handleScroll(scroll, event);
        }
    }

    @Override
    public void dispose() {
        anchorLayer.clear();
        clearWorldLayers();
        super.dispose();
    }

    /**
     * Подключает widget из {@link AnchorLayer} к обычному children-дереву.
     *
     * <p>Метод package-private: его должен вызывать только {@link AnchorLayer},
     * чтобы у якорей и списка детей не расходилось состояние.</p>
     */
    void attachAnchor(AnchorWidget anchor) {
        Widget child = anchor.widget();
        if (children.contains(child)) {
            throw new IllegalArgumentException("Anchor widget is already attached to this WorldCanvas");
        }
        children.add(child);
        childSnapshotDirty = true;
        if (child instanceof WidgetBase base) {
            base.setParentInternal(this);
            base.setUiContextInternal(uiContext());
        }
    }

    /**
     * Отключает widget из {@link AnchorLayer} от обычного children-дерева.
     */
    void detachAnchor(AnchorWidget anchor) {
        Widget child = anchor.widget();
        if (!children.remove(child)) return;
        childSnapshotDirty = true;
        if (child instanceof WidgetBase base) {
            base.setParentInternal(null);
            base.setUiContextInternal(null);
        }
    }

    private void handlePointerPressed(PointerPressedEvent pointer, Event sourceEvent) {
        if (!panningEnabled || pointer.button() != panButton) return;
        if (pointer.target() != this) return;

        panDrag = new PanDragState(pointer.pointerId(), pointer.rootX(), pointer.rootY(), viewport.x(), viewport.y());
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointer.pointerId(), this);
        }
        sourceEvent.cancel();
    }

    private void handlePointerMoved(PointerMovedEvent pointer) {
        if (panDrag == null || panDrag.pointerId != pointer.pointerId()) return;

        float oldX = viewport.x();
        float oldY = viewport.y();
        float oldZoom = viewport.zoom();
        float nextX = panDrag.startViewportX + pointer.rootX() - panDrag.startRootX;
        float nextY = panDrag.startViewportY + pointer.rootY() - panDrag.startRootY;
        if (!viewport.setPosition(nextX, nextY)) return;
        afterViewportChanged(oldX, oldY, oldZoom);
    }

    private void handlePointerReleased(PointerReleasedEvent pointer, Event sourceEvent) {
        if (panDrag == null || panDrag.pointerId != pointer.pointerId()) return;
        UIContext context = uiContext();
        if (context != null) {
            context.releasePointer(pointer.pointerId(), this);
        }
        panDrag = null;
        sourceEvent.cancel();
    }

    private void handleScroll(ScrollEvent scroll, Event sourceEvent) {
        if (scroll.phase() == EventPhase.CAPTURE) return;

        boolean handled = false;
        boolean ctrlWheel = KeyModifiers.has(scroll.modifiers(), KeyModifiers.CONTROL);
        if (ctrlWheel) {
            // Ctrl + колесо — zoom-to-cursor, чтобы точка под курсором визуально оставалась на месте.
            if (zoomEnabled && scroll.deltaY() != 0.0f) {
                float factor = (float) Math.pow(zoomStep, scroll.deltaY());
                handled = zoomAt(scroll.rootX(), scroll.rootY(), factor);
            } else {
                handled = true;
            }
        } else if (wheelPanningEnabled && (scroll.deltaX() != 0.0f || scroll.deltaY() != 0.0f)) {
            float deltaX = scroll.deltaX();
            float deltaY = scroll.deltaY();
            if (KeyModifiers.has(scroll.modifiers(), KeyModifiers.SHIFT) && deltaY != 0.0f) {
                deltaX = deltaY;
                deltaY = 0.0f;
            }

            float oldX = viewport.x();
            float oldY = viewport.y();
            float oldZoom = viewport.zoom();
            if (viewport.panBy(deltaX * wheelPanStep, deltaY * wheelPanStep)) {
                afterViewportChanged(oldX, oldY, oldZoom);
                handled = true;
            }
        }

        if (handled || consumeWheelWhileHovered) {
            sourceEvent.cancel();
        }
    }

    private boolean zoomAt(float rootX, float rootY, float factor) {
        float oldX = viewport.x();
        float oldY = viewport.y();
        float oldZoom = viewport.zoom();

        float localX = rootX - layoutBounds().x();
        float localY = rootY - layoutBounds().y();
        if (!viewport.zoomAt(localX, localY, factor)) return false;
        afterViewportChanged(oldX, oldY, oldZoom);
        return true;
    }

    private void clampViewport() {
        if (viewport.clamp(layoutBounds().width(), layoutBounds().height())) {
            arrangeAnchors();
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }
    }

    private void afterViewportChanged(float oldX, float oldY, float oldZoom) {
        arrangeAnchors();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        dispatch(new ViewportChangedEvent(this, oldX, oldY, oldZoom, viewport.x(), viewport.y(), viewport.zoom()));
    }

    /**
     * Перекладывает anchor-виджеты после layout/viewport changes.
     *
     * <p>Каждый anchor проецируется world→root, затем размещается как обычный
     * widget с учётом pivot, screenSize и zoom visibility range.</p>
     */
    private void arrangeAnchors() {
        AnchorWidget[] anchors = anchorLayer.snapshot();
        if (anchors.length == 0) return;

        LayoutContext childContext = new LayoutContext(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        for (AnchorWidget anchor : anchors) {
            Widget child = anchor.widget();
            if (child.visibility() == Visibility.COLLAPSED) {
                anchor.arrangedVisible(false);
                continue;
            }

            child.measure(childContext);
            float width = anchor.screenWidth() >= 0.0f ? anchor.screenWidth() : child.desiredSize().width();
            float height = anchor.screenHeight() >= 0.0f ? anchor.screenHeight() : child.desiredSize().height();
            width = sanitizeSize(width);
            height = sanitizeSize(height);

            float rootX = worldToRootX(anchor.worldX());
            float rootY = worldToRootY(anchor.worldY());
            float x = rootX - width * anchor.pivotX();
            float y = rootY - height * anchor.pivotY();

            boolean visible = anchor.visibleAtZoom(viewport.zoom())
                    && (!anchor.cullOutsideViewport()
                    || rectsIntersect(x, y, width, height,
                    layoutBounds().x(), layoutBounds().y(), layoutBounds().width(), layoutBounds().height()));
            anchor.arrangedVisible(visible);
            anchor.projectedRoot(rootX, rootY);

            if (visible) {
                child.arrange(new MutableRect(x, y, width, height));
            } else {
                child.arrange(new MutableRect(rootX, rootY, 0.0f, 0.0f));
            }
        }
    }

    private Widget[] childSnapshot() {
        if (childSnapshotDirty) {
            childSnapshot = children.toArray(new Widget[children.size()]);
            childSnapshotDirty = false;
        }
        return childSnapshot;
    }

    private WorldLayer[] worldLayerSnapshot() {
        if (worldLayerSnapshotDirty) {
            worldLayerSnapshot = worldLayers.toArray(new WorldLayer[worldLayers.size()]);
            worldLayerSnapshotDirty = false;
        }
        return worldLayerSnapshot;
    }

    private <T extends Event> T dispatch(T event) {
        UIContext context = uiContext();
        if (context != null && event instanceof RoutableWidgetEvent widgetEvent) {
            context.routedEvents().dispatch(widgetEvent);
        } else {
            emit(event);
        }
        return event;
    }

    private static float sanitizeSize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private static boolean rectsIntersect(float ax, float ay, float aw, float ah,
                                          float bx, float by, float bw, float bh) {
        return ax + aw >= bx && bx + bw >= ax && ay + ah >= by && by + bh >= ay;
    }

    private record PanDragState(
            int pointerId,
            float startRootX,
            float startRootY,
            float startViewportX,
            float startViewportY
    ) {
    }
}

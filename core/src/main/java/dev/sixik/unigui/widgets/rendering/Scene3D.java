package dev.sixik.unigui.widgets.rendering;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.BaseEvent;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec2;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Vec3View;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.Scene3DRenderer;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.OrbitCameraController;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.ScenePicker;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.ZOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Offscreen-виджет с 3D-сценой и обычными UI-якорями поверх текстуры.
 * Левая кнопка вращает камеру и выбирает узлы коротким кликом, средняя или правая панорамирует,
 * колесо меняет масштаб. Бэкенд отрисовывает сцену через {@link RenderBackend#scene3D()}.
 */
public final class Scene3D extends RenderToTexture {
    /**
     * Цель с глубиной. Автоочистка 2D отключена, очистка принадлежит 3D-проходу.
     */
    private static final RenderTargetOptions SCENE_TARGET = new RenderTargetOptions(true, false, "scene_3d");
    /**
     * Суммарное смещение от нажатия, после которого жест считается перетаскиванием, в единицах UI.
     */
    private static final float DRAG_THRESHOLD = 3.0f;
    /**
     * Угол поворота камеры на логический пиксель перетаскивания, в радианах.
     */
    private static final double ROTATION_PER_PIXEL = 0.01;
    /**
     * Во сколько раз меняется дистанция или высота при одном шаге колеса.
     */
    private static final double WHEEL_SCALE = 0.85;
    /**
     * Допуск при проекции точки ровно на ближней плоскости, в единицах глубины от 0 до 1.
     */
    private static final double DEPTH_ROUNDOFF = 8.0 * Math.ulp(1.0);

    /**
     * Сцена, принадлежащая логической модели виджета.
     */
    private final Scene scene;
    /**
     * Камера с viewport, соответствующим логическому холсту.
     */
    private final Camera3D camera = new Camera3D();
    /**
     * Управление камерой вокруг точки интереса.
     */
    private final OrbitCameraController orbit = new OrbitCameraController(camera, new MutableVec3(), 5);
    /**
     * Переиспользуемый поиск узлов под курсором.
     */
    private final ScenePicker picker = new ScenePicker();
    /**
     * Геометрия размещения текстуры, общая для picking и якорей.
     */
    private final SceneViewport viewport = new SceneViewport();
    /**
     * Рабочая экранная или логическая точка в единицах UI.
     */
    private final MutableVec2 mapped = new MutableVec2();
    /**
     * Рабочая точка якоря, затем экранная проекция с глубиной в Z.
     */
    private final MutableVec3 anchorPoint = new MutableVec3();
    /**
     * Рабочая матрица мирового преобразования или проекции.
     */
    private final MutableMat4 matrix = new MutableMat4();
    /**
     * UI-якоря в порядке добавления.
     */
    private final List<SceneAnchor3D> anchors = new ArrayList<>();
    /**
     * Дочерний виджет RTT и UI-якоря в порядке дерева.
     */
    private final List<Widget> children = new ArrayList<>();
    /**
     * Живое представление дочернего списка.
     */
    private final List<Widget> childrenView = Collections.unmodifiableList(children);
    /**
     * Декларации XAML в порядке добавления, с соответствующими корневыми узлами.
     */
    private final List<NodeContent> nodeContent = new ArrayList<>();

    /**
     * Последний известный коэффициент физических пикселей на единицу внешнего UI.
     */
    private float pixelScale = 1;
    /**
     * Дробная часть тика из последнего кадра.
     */
    private float partialTick;
    /**
     * Версия сцены, переданная последнему 3D-проходу.
     */
    private long renderedSceneRevision = -1;
    /**
     * Версия камеры, переданная последнему 3D-проходу.
     */
    private long renderedCameraRevision = -1;
    /**
     * Включены ли вращение, панорамирование и зум мышью.
     */
    private boolean cameraControls = true;
    /**
     * Разрешена ли подсветка единичных ячеек под мышью.
     */
    private boolean highlightBlocks;
    /**
     * Нижний угол ячейки, найденной при последнем наведении.
     */
    private final MutableVec3 hoverCell = new MutableVec3();
    /**
     * Ячейка, показанная в этом viewport, и её рабочие данные.
     */
    private HoverHighlight highlight;

    /**
     * Узел под указателем или null.
     */
    private SceneNode hoveredNode;
    /**
     * Находится ли указатель на самой сцене.
     */
    private boolean pointerOnScene;
    /**
     * Последняя локальная координата X указателя в единицах UI.
     */
    private float pointerX;
    /**
     * Последняя локальная координата Y указателя в единицах UI.
     */
    private float pointerY;
    /**
     * Действителен ли последний результат picking для наведения.
     */
    private boolean hoverSampleValid;
    /**
     * Координата X последней проверки наведения в единицах UI.
     */
    private float sampledX;
    /**
     * Координата Y последней проверки наведения в единицах UI.
     */
    private float sampledY;
    /**
     * Версия модели при последней проверке наведения.
     */
    private long sampledSceneRevision;
    /**
     * Версия камеры при последней проверке наведения.
     */
    private long sampledCameraRevision;
    /**
     * Версия размещения при последней проверке наведения.
     */
    private long sampledViewportRevision;
    /**
     * Кнопка активного жеста или null.
     */
    private PointerButton gestureButton;
    /**
     * Идентификатор указателя, который начал жест.
     */
    private int gesturePointer;
    /**
     * Узел, на котором началось нажатие, или null.
     */
    private SceneNode pressedNode;
    /**
     * Координата X начала жеста в единицах UI.
     */
    private float startX;
    /**
     * Координата Y начала жеста в единицах UI.
     */
    private float startY;
    /**
     * Координата X последнего обработанного смещения.
     */
    private float dragX;
    /**
     * Координата Y последнего обработанного смещения.
     */
    private float dragY;
    /**
     * Превысило ли движение порог перетаскивания.
     */
    private boolean dragging;
    /**
     * Получен ли захват от UI-контекста для текущего жеста.
     */
    private boolean captured;

    /**
     * Создаёт виджет с пустой сценой и обновлением ON_CHANGE.
     */
    public Scene3D() {
        this(new Scene());
    }

    /**
     * Создаёт виджет для заданной сцены.
     *
     * @param scene граф сцены, может использоваться несколькими виджетами.
     */
    public Scene3D(Scene scene) {
        this.scene = Objects.requireNonNull(scene, "scene");
        super.targetOptions(SCENE_TARGET);
        updateMode(UpdateMode.ON_CHANGE);
    }

    /**
     * Читает граф сцены для изменения узлов и света.
     *
     * @return модель сцены.
     */
    public Scene scene() {
        return scene;
    }

    /**
     * Читает камеру виджета. Размер её viewport устанавливает виджет.
     *
     * @return камера для настройки позы и проекции.
     */
    public Camera3D camera() {
        return camera;
    }

    /**
     * Читает контроллер, который используется мышью.
     *
     * @return контроллер для настройки цели, углов и дистанции.
     */
    public OrbitCameraController orbit() {
        return orbit;
    }

    /**
     * Управляет камерой мышью. Picking остаётся доступным при отключении управления.
     *
     * @param enabled true разрешает перетаскивание и колесо.
     * @return этот виджет.
     */
    public Scene3D cameraControls(boolean enabled) {
        if (cameraControls != enabled) {
            cameraControls = enabled;
            releaseGesture();
        }
        return this;
    }

    /**
     * Читает состояние управления мышью.
     *
     * @return true, если камера реагирует на перетаскивание и колесо.
     */
    public boolean cameraControls() {
        return cameraControls;
    }

    /**
     * Включает подсветку блоков, которые поддерживают выбор единичной ячейки.
     *
     * @param enabled true включает подсветку.
     * @return этот виджет.
     */
    public Scene3D highlightBlocks(boolean enabled) {
        if (highlightBlocks != enabled) {
            highlightBlocks = enabled;
            hoverSampleValid = false;
            if (!enabled) updateHighlight(null);
            else refreshHover();
        }
        return this;
    }

    /**
     * Читает настройку подсветки.
     *
     * @return true, если поддерживаемый блок под курсором подсвечивается.
     */
    public boolean highlightBlocks() {
        return highlightBlocks;
    }

    /**
     * Помещает узел из декларации XAML в сцену.
     *
     * @param content декларация, предоставляющая готовый узел.
     */
    public void addNodeContent(NodeContent content) {
        scene.addRoot(content.sceneNode());
        nodeContent.add(content);
        content.onAddedToScene(this);
    }

    /**
     * Возвращает текущие декларации сцены для экспорта XAML.
     *
     * @return список логических дочерних элементов сцены.
     */
    public List<Widget> xamlChildren() {
        List<Widget> result = new ArrayList<>();
        for (NodeContent entry : nodeContent) {
            SceneNode node = entry.sceneNode();
            if (node.scene() == scene && node.parent() == null) result.add(entry);
        }
        if (content() != null) result.add(content());
        return result;
    }

    /**
     * Невизуальный элемент разметки, создающий узел графа сцены.
     */
    public interface NodeContent extends Widget {
        /**
         * Читает узел, построенный при загрузке декларации.
         *
         * @return узел декларации для размещения в сцене.
         */
        SceneNode sceneNode();

        /**
         * Настраивает поведение декларации после добавления узла.
         *
         * @param viewport владеющий виджет, в котором уже находится узел.
         */
        default void onAddedToScene(Scene3D viewport) {
        }
    }

    /**
     * Сохраняет глубину и режим композиции при смене параметров цели.
     *
     * @param options задаёт отладочное имя цели или null для стандартного имени.
     * @return этот виджет.
     */
    @Override
    public Scene3D targetOptions(RenderTargetOptions options) {
        super.targetOptions(options == null ? SCENE_TARGET
                : new RenderTargetOptions(true, false, options.debugName()));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scene3D content(Widget content) {
        if (content == content()) return this;
        if (content != null) requireFreeWidget(content);
        super.content(content);
        rebuildChildren();
        recomputeSubtreeInvalidation();
        return this;
    }

    /**
     * Добавляет UI-виджет с привязкой к мировой точке.
     *
     * @param widget свободный дочерний виджет.
     * @param point  конечная точка в мировых координатах.
     * @return якорь для настройки позиции и pivot.
     */
    public SceneAnchor3D addAnchor(Widget widget, Vec3View point) {
        return addAnchor(widget, null, point);
    }

    /**
     * Добавляет UI-виджет с привязкой к локальной точке узла.
     *
     * @param widget свободный дочерний виджет.
     * @param node   узел привязки или null для мировой точки.
     * @param point  конечная точка в координатах узла или сцены.
     * @return якорь для настройки позиции и pivot.
     * @throws IllegalArgumentException если виджет уже принадлежит дереву или создал бы цикл.
     */
    public SceneAnchor3D addAnchor(Widget widget, SceneNode node, Vec3View point) {
        requireFreeWidget(widget);
        SceneAnchor3D anchor = new SceneAnchor3D(widget, node, point);
        anchors.add(anchor);
        rebuildChildren();
        anchor.setParentInternal(this);
        anchor.setUiContextInternal(uiContext());
        invalidate(InvalidationFlags.VISUAL);
        return anchor;
    }

    /**
     * Удаляет якорь, освобождая дочерний виджет для повторного использования.
     *
     * @param anchor удаляемый якорь.
     * @return true, если этот виджет владел якорем.
     */
    public boolean removeAnchor(SceneAnchor3D anchor) {
        if (!anchors.remove(anchor)) return false;
        rebuildChildren();
        anchor.hideProjection();
        anchor.setParentInternal(null);
        anchor.setUiContextInternal(null);
        anchor.releaseWidget();
        recomputeSubtreeInvalidation();
        invalidate(InvalidationFlags.VISUAL);
        return true;
    }

    /**
     * Ищет узел в показанной части холста.
     *
     * @param x координата X относительно границы виджета.
     * @param y координата Y относительно границы виджета.
     * @return ближайший видимый узел или null.
     */
    public SceneNode pickNode(float x, float y) {
        prepareViewport();
        return pickPrepared(x, y);
    }

    /**
     * Ищет узел с уже рассчитанным viewport.
     *
     * @param x горизонтальная координата относительно виджета.
     * @param y вертикальная координата относительно виджета.
     * @return ближайший узел или null.
     */
    private SceneNode pickPrepared(float x, float y) {
        return pickPrepared(x, y, null);
    }

    /**
     * Ищет узел и заполняет координаты его ячейки, если она найдена.
     *
     * @param x    горизонтальная координата относительно виджета.
     * @param y    вертикальная координата относительно виджета.
     * @param cell выход для нижнего угла ячейки или null.
     * @return ближайший видимый узел или null.
     */
    private SceneNode pickPrepared(float x, float y, MutableVec3 cell) {
        if (!visible() || !displayed() || !viewport.toCanvas(x, y, mapped)) return null;
        return picker.pick(scene, camera, mapped.x(), mapped.y(), cell);
    }

    /**
     * Читает текущее наведение в 3D-сцене.
     *
     * @return узел под указателем или null.
     */
    public SceneNode hoveredNode() {
        return hoveredNode;
    }

    /**
     * Подписывается на переходы наведения между узлами и пустой областью.
     *
     * @param listener обработчик изменения.
     * @return подписка.
     */
    public EventSubscription onNodeHover(EventListener<? super NodeHoverEvent> listener) {
        return on(NodeHoverEvent.TYPE, listener);
    }

    /**
     * Подписывается на короткий клик по узлу.
     *
     * @param listener обработчик клика.
     * @return подписка.
     */
    public EventSubscription onNodeClick(EventListener<? super NodeClickEvent> listener) {
        return on(NodeClickEvent.TYPE, listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Widget> children() {
        return childrenView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUiContextInternal(UIContext context) {
        boolean changed = uiContext() != context;
        if (changed) releaseGesture();
        super.setUiContextInternal(context);
        for (int i = 0; i < anchors.size(); i++) anchors.get(i).setUiContextInternal(context);
        if (changed) clearHover();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scene3D enabled(boolean enabled) {
        super.enabled(enabled);
        if (!enabled) {
            resetPointer();
            for (int i = 0; i < anchors.size(); i++) anchors.get(i).clearInteraction();
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scene3D visibility(Visibility visibility) {
        super.visibility(visibility);
        if (!visible()) {
            resetPointer();
            projectAnchors();
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scene3D displayed(boolean displayed) {
        super.displayed(displayed);
        if (!displayed) {
            resetPointer();
            projectAnchors();
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        prepareViewport();
        projectAnchors();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void tick(FrameContext frame) {
        if (!visible()) return;
        super.tick(frame);
        if (frame != null) partialTick = frame.partialTick();
        if (captured && uiContext() != null && uiContext().capturedPointer(gesturePointer) != this) {
            releaseGesture();
        }
        prepareViewport();
        synchronizeTexture();
        projectAnchors();
        refreshHover();
        for (int i = 0; i < anchors.size(); i++) {
            SceneAnchor3D anchor = anchors.get(i);
            if (anchor.visible()) anchor.tick(frame);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(RenderContext context) {
        if (!visible() || context == null || !(layoutBounds().width() > 0 && layoutBounds().height() > 0)) return;
        pixelScale = context.pixelScale();
        prepareViewport();
        synchronizeTexture();
        refreshHover();
        super.render(context);
        projectAnchors();
        renderAnchors(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasRawPass() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderRawPass(RenderBackend backend, RenderTarget target, float logicalWidth,
                                 float logicalHeight, float coordinateScale) {
        camera.setViewport(logicalWidth, logicalHeight);
        long sceneRevision = scene.revision();
        long cameraRevision = camera.revision();
        backend.scene3D().render(target, scene, camera,
                Math.min(target.width(), logicalWidth * coordinateScale),
                Math.min(target.height(), logicalHeight * coordinateScale), clearColor(), partialTick,
                highlight == null ? null : highlight.renderable());
        renderedSceneRevision = sceneRevision;
        renderedCameraRevision = cameraRevision;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(Event event) {
        if (!visible() || !enabled() || !displayed()) return;
        super.handle(event);
        if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.TARGET
                && pointer.target() == this) {
            if (pointer instanceof PointerReleasedEvent released && matchesGesture(released)) {
                finishGesture(released);
            } else if (!event.isCancelled() || pointer instanceof PointerExitedEvent) {
                handlePointer(pointer);
            }
        } else if (event instanceof ScrollEvent scroll && scroll.phase() == EventPhase.TARGET
                && scroll.target() == this && !event.isCancelled()) {
            zoom(scroll);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MouseCursor mouseCursorAt(float localX, float localY) {
        if (!enabled() || !displayed()) return super.mouseCursorAt(localX, localY);
        if (dragging && cameraControls) return MouseCursor.CROSSHAIR;
        SceneNode hit = pointerOnScene && localX == pointerX && localY == pointerY
                ? hoveredNode : pickNode(localX, localY);
        return hit == null ? super.mouseCursorAt(localX, localY) : MouseCursor.POINTER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        releaseGesture();
        pointerOnScene = false;
        hoveredNode = null;
        while (!anchors.isEmpty()) {
            SceneAnchor3D anchor = anchors.get(anchors.size() - 1);
            removeAnchor(anchor);
            anchor.widget().dispose();
            anchor.dispose();
        }
        super.dispose();
        rebuildChildren();
    }

    /**
     * Обновляет размеры камеры и соответствие между холстом и виджетом.
     */
    private void prepareViewport() {
        TargetSize size = targetSize(pixelScale);
        camera.setViewport(size.logicalWidth(), size.logicalHeight());
        viewport.update(layoutBounds(), fit(), size);
    }

    /**
     * Просит обновить текстуру в ON_CHANGE при изменении модели или камеры.
     */
    private void synchronizeTexture() {
        if (updateMode() == UpdateMode.ON_CHANGE && (renderedCameraRevision != camera.revision()
                || renderedSceneRevision != scene.revision())) {
            requestRender();
        }
    }

    /**
     * Проецирует якоря с учётом видимости узла, отсечения и размещения текстуры.
     */
    private void projectAnchors() {
        for (int i = 0; i < anchors.size(); i++) {
            SceneAnchor3D anchor = anchors.get(i);
            if (displayed() && visible() && worldAnchorPoint(anchor) && camera.project(anchorPoint, anchorPoint)
                    && anchorPoint.z() >= -DEPTH_ROUNDOFF && anchorPoint.z() <= 1
                    && viewport.fromCanvas(anchorPoint.x(), anchorPoint.y(), mapped)) {
                anchor.projectAt(mapped.x(), mapped.y());
            } else {
                anchor.hideProjection();
            }
        }
    }

    /**
     * Переводит точку якоря в мировые координаты с проверкой его узла и предков.
     *
     * @param anchor якорь сцены.
     * @return true, если его мировая точка определена и узел не скрыт.
     */
    private boolean worldAnchorPoint(SceneAnchor3D anchor) {
        anchor.position(anchorPoint);
        SceneNode node = anchor.node();
        if (node == null) return true;
        if (node.scene() != scene) return false;
        for (SceneNode current = node; current != null; current = current.parent()) {
            if (!current.visible()) return false;
        }
        return node.worldMatrix(matrix).transformPoint(anchorPoint, anchorPoint);
    }

    /**
     * Рисует UI-якоря поверх текстуры с преобразованием и отсечением по границам виджета.
     *
     * @param context экранный контекст.
     */
    private void renderAnchors(RenderContext context) {
        if (anchors.isEmpty() || !displayed()) return;
        RectView bounds = layoutBounds();
        pushOpacity(context);
        context.pushTransform(bounds, transform());
        try {
            context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
            try {
                List<Widget> order = ZOrder.paintOrder(childrenView);
                for (int i = 0; i < order.size(); i++) {
                    Widget child = order.get(i);
                    if (child != content() && child.visible()) child.renderCached(context);
                }
            } finally {
                context.popClip();
            }
        } finally {
            context.popTransform();
            popOpacity(context);
        }
    }

    /**
     * Синхронизирует дерево UI с содержимым RTT и порядком якорей.
     */
    private void rebuildChildren() {
        children.clear();
        if (content() != null) children.add(content());
        children.addAll(anchors);
    }

    /**
     * Проверяет, что виджет можно подсоединить без перемещения из другого дерева или создания цикла.
     *
     * @param widget добавляемый виджет.
     * @throws NullPointerException     если widget равен null.
     * @throws IllegalArgumentException если уже есть родитель или widget является предком этой сцены.
     */
    private void requireFreeWidget(Widget widget) {
        Objects.requireNonNull(widget, "widget");
        if (widget.parent() != null) throw new IllegalArgumentException("Widget already has a parent");
        for (Widget ancestor = this; ancestor != null; ancestor = ancestor.parent()) {
            if (widget == ancestor) throw new IllegalArgumentException("Scene cannot contain an ancestor");
        }
    }

    /**
     * Обрабатывает ввод для самой сцены. События дочерних виджетов обрабатываются ими.
     *
     * @param pointer событие указателя в локальных координатах сцены.
     */
    private void handlePointer(PointerEvent pointer) {
        if (pointer instanceof PointerExitedEvent) {
            if (!captured) releaseGesture();
            clearHover();
        } else if (pointer instanceof PointerPressedEvent pressed) {
            beginGesture(pressed);
        } else if (pointer instanceof PointerMovedEvent || pointer instanceof PointerEnteredEvent) {
            pointerOnScene = true;
            pointerX = pointer.localX();
            pointerY = pointer.localY();
            if (pointer instanceof PointerMovedEvent && gestureButton != null && gesturePointer == pointer.pointerId()) {
                moveGesture(pointerX, pointerY);
                if (dragging && cameraControls) pointer.cancel();
            }
            refreshHover();
        }
    }

    /**
     * Запоминает нажатие и захватывает указатель, если мышь управляет камерой или нажат узел.
     *
     * @param pointer нажатие в локальных координатах виджета.
     */
    private void beginGesture(PointerPressedEvent pointer) {
        if (gestureButton != null) return;
        PointerButton button = pointer.button();
        boolean primary = button == PointerButton.PRIMARY;
        if (!primary && (!cameraControls || (button != PointerButton.SECONDARY && button != PointerButton.MIDDLE)))
            return;
        prepareViewport();
        if (!viewport.toCanvas(pointer.localX(), pointer.localY(), mapped)) return;
        pressedNode = primary ? picker.pick(scene, camera, mapped.x(), mapped.y()) : null;
        if (!cameraControls && pressedNode == null) return;
        gestureButton = button;
        gesturePointer = pointer.pointerId();
        startX = dragX = pointer.localX();
        startY = dragY = pointer.localY();
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(gesturePointer, this);
            captured = context.capturedPointer(gesturePointer) == this;
        }
        pointer.cancel();
    }

    /**
     * Проверяет, что отпущена кнопка текущего жеста.
     *
     * @param pointer событие отпускания.
     * @return true для того же указателя и той же кнопки.
     */
    private boolean matchesGesture(PointerReleasedEvent pointer) {
        return gestureButton == pointer.button() && gesturePointer == pointer.pointerId();
    }

    /**
     * Завершает жест и отправляет клик при отпускании над тем же узлом.
     *
     * @param pointer событие отпускания.
     */
    private void finishGesture(PointerReleasedEvent pointer) {
        SceneNode pressed = pressedNode;
        boolean click = pressed != null && !dragging && !pointer.isCancelled()
                && withinClickThreshold(pointer.localX(), pointer.localY());
        releaseGesture();
        if (click && pickNode(pointer.localX(), pointer.localY()) == pressed) {
            emit(new NodeClickEvent(pressed, pointer));
        }
        pointer.cancel();
        refreshHover();
    }

    /**
     * Освобождает захват и сбрасывает текущее нажатие.
     */
    private void releaseGesture() {
        if (captured && uiContext() != null) uiContext().releasePointer(gesturePointer, this);
        captured = false;
        gestureButton = null;
        pressedNode = null;
        dragging = false;
    }

    /**
     * Проверяет порог клика по квадрату расстояния от начала нажатия.
     *
     * @param x локальная координата X.
     * @param y локальная координата Y.
     * @return true внутри радиуса DRAG_THRESHOLD.
     */
    private boolean withinClickThreshold(float x, float y) {
        float dx = x - startX, dy = y - startY;
        return dx * dx + dy * dy < DRAG_THRESHOLD * DRAG_THRESHOLD;
    }

    /**
     * Вращает или панорамирует камеру при перемещении захваченного указателя.
     * Для панорамирования вертикальный размер видимой области перспективы на дистанции d равен
     * {@code 2*d / projection[1,1]}. Деление на высоту холста переводит смещение в мировые единицы.
     *
     * @param x локальная координата X.
     * @param y локальная координата Y.
     */
    private void moveGesture(float x, float y) {
        float dx = x - dragX, dy = y - dragY;
        if (!Float.isFinite(dx) || !Float.isFinite(dy) || (!dragging && withinClickThreshold(x, y))) return;
        dragging = true;
        if (!cameraControls) return;
        dragX = x;
        dragY = y;
        if (gestureButton == PointerButton.PRIMARY) {
            orbit.rotate(-dx * ROTATION_PER_PIXEL, dy * ROTATION_PER_PIXEL);
        } else {
            double height = camera.orthographic() ? camera.orthographicHeight()
                    : 2.0 * orbit.distance() / camera.projection(matrix).get(1, 1);
            double scale = height / viewport.canvasHeight();
            float right = (float) (-viewport.canvasDeltaX(dx) * scale);
            float up = (float) (viewport.canvasDeltaY(dy) * scale);
            if (Float.isFinite(right) && Float.isFinite(up)) orbit.pan(right, up);
        }
        synchronizeTexture();
        projectAnchors();
    }

    /**
     * Меняет масштаб по формуле {@code factor = WHEEL_SCALE^delta}.
     * Перспективное расстояние ограничено текущими плоскостями отсечения.
     *
     * @param scroll колесо в локальных координатах сцены.
     */
    private void zoom(ScrollEvent scroll) {
        float delta = scroll.deltaY();
        if (!cameraControls || delta == 0 || !Float.isFinite(delta)) return;
        prepareViewport();
        if (!viewport.toCanvas(scroll.localX(), scroll.localY(), mapped)) return;
        double factor = Math.pow(WHEEL_SCALE, Math.max(-10, Math.min(10, delta)));
        if (!camera.orthographic()) {
            double distance = orbit.distance();
            factor = Math.max(camera.near() / distance, Math.min(camera.far() / distance, factor));
        }
        float zoom = (float) factor;
        float scaled = (camera.orthographic() ? camera.orthographicHeight() : orbit.distance()) * zoom;
        if (!(scaled > 0) || !Float.isFinite(scaled) || !(zoom > 0) || !Float.isFinite(zoom)) return;
        orbit.zoom(zoom);
        scroll.cancel();
        synchronizeTexture();
        projectAnchors();
        refreshHover();
    }

    /**
     * Обновляет наведение по последней позиции, в том числе после движения камеры.
     */
    private void refreshHover() {
        if (!pointerOnScene || dragging || !enabled()) {
            hoverSampleValid = false;
            updateHighlight(null);
            setHoveredNode(null);
            return;
        }
        prepareViewport();
        long sceneRevision = scene.revision();
        long cameraRevision = camera.revision();
        long viewportRevision = viewport.revision();
        if (hoverSampleValid && sampledX == pointerX && sampledY == pointerY
                && sampledSceneRevision == sceneRevision && sampledCameraRevision == cameraRevision
                && sampledViewportRevision == viewportRevision) {
            return;
        }
        sampledX = pointerX;
        sampledY = pointerY;
        sampledSceneRevision = sceneRevision;
        sampledCameraRevision = cameraRevision;
        sampledViewportRevision = viewportRevision;
        hoverSampleValid = true;
        SceneNode hit = pickPrepared(pointerX, pointerY, highlightBlocks ? hoverCell : null);
        if (highlightBlocks) updateHighlight(hit);
        setHoveredNode(hit);
    }

    /**
     * Сбрасывает положение указателя и наведение.
     */
    private void clearHover() {
        pointerOnScene = false;
        hoverSampleValid = false;
        updateHighlight(null);
        setHoveredNode(null);
    }

    /**
     * Сбрасывает все состояния ввода.
     */
    private void resetPointer() {
        releaseGesture();
        clearHover();
    }

    /**
     * Публикует переход наведения, если цель изменилась.
     *
     * @param node новый узел или null.
     */
    private void setHoveredNode(SceneNode node) {
        if (hoveredNode != node) {
            SceneNode previous = hoveredNode;
            hoveredNode = node;
            emit(new NodeHoverEvent(previous, node));
        }
    }

    /**
     * Меняет подсвеченную ячейку текущего viewport.
     *
     * @param node узел с найденной ячейкой или null для сброса.
     */
    private void updateHighlight(SceneNode node) {
        if (node == null || !Float.isFinite(hoverCell.x())) {
            if (highlight == null || highlight.node == null) return;
            highlight.node = null;
        } else {
            if (highlight == null) highlight = new HoverHighlight();
            if (highlight.node == node && highlight.cell.x() == hoverCell.x()
                    && highlight.cell.y() == hoverCell.y() && highlight.cell.z() == hoverCell.z()) return;
            highlight.node = node;
            highlight.cell.set(hoverCell);
        }
        if (updateMode() == UpdateMode.ON_CHANGE) requestRender();
    }

    /**
     * Строит подсветку на основе локального угла ячейки и текущей матрицы узла.
     */
    private static final class HoverHighlight {
        /**
         * Отступ граней от блока в единицах сцены, чтобы избежать совпадения глубины.
         */
        private static final float OFFSET = 0.003f;
        /**
         * Одна полупрозрачная оболочка вокруг единичной ячейки.
         */
        private static final Mesh3D MESH = new Mesh3D(new float[]{
                -OFFSET, -OFFSET, -OFFSET, 1 + OFFSET, -OFFSET, -OFFSET,
                1 + OFFSET, 1 + OFFSET, -OFFSET, -OFFSET, 1 + OFFSET, -OFFSET,
                -OFFSET, -OFFSET, 1 + OFFSET, 1 + OFFSET, -OFFSET, 1 + OFFSET,
                1 + OFFSET, 1 + OFFSET, 1 + OFFSET, -OFFSET, 1 + OFFSET, 1 + OFFSET
        }, null, null, null, new int[]{
                0, 3, 2, 0, 2, 1, 4, 5, 6, 4, 6, 7,
                0, 4, 7, 0, 7, 3, 1, 2, 6, 1, 6, 5,
                0, 1, 5, 0, 5, 4, 3, 7, 6, 3, 6, 2
        });
        /**
         * Тёплый полупрозрачный цвет для выделения поверхности.
         */
        private static final Material MATERIAL = new Material(0x73FFE18C, null, Material.Shading.UNLIT,
                Material.AlphaMode.TRANSLUCENT, 0, Material.Culling.BACK);
        /**
         * Нижний угол ячейки в локальных координатах узла.
         */
        private final MutableVec3 cell = new MutableVec3();
        /**
         * Вращение единичного преобразования.
         */
        private final MutableQuat rotation = new MutableQuat();
        /**
         * Масштаб единичного преобразования.
         */
        private final MutableVec3 scale = new MutableVec3(1, 1, 1);
        /**
         * Перенос от начала ячейки к началу узла.
         */
        private final MutableMat4 local = new MutableMat4();
        /**
         * Преобразование геометрии ячейки в мировые координаты.
         */
        private final MutableMat4 world = new MutableMat4();
        /**
         * Переиспользуемый пакет для рендерера.
         */
        private final Scene3DRenderer.Highlight renderable = new Scene3DRenderer.Highlight(MESH, MATERIAL, world);
        /**
         * Подсвеченный узел или null.
         */
        private SceneNode node;

        /**
         * Умножает мировое преобразование узла на перенос ячейки.
         *
         * @return подсветка этого viewport или null.
         */
        private Scene3DRenderer.Highlight renderable() {
            if (node == null) return null;
            local.setTransform(cell, rotation, scale);
            world.setProduct(node.worldMatrix(world), local);
            return renderable;
        }
    }

    /**
     * Изменение узла под указателем.
     */
    public static final class NodeHoverEvent extends BaseEvent {
        /**
         * Тип локальной подписки на наведение.
         */
        public static final EventType<NodeHoverEvent> TYPE = EventType.create("scene3d.node.hover");
        /**
         * Узел, с которого ушёл указатель, или null.
         */
        private final SceneNode previous;
        /**
         * Узел под указателем или null.
         */
        private final SceneNode node;

        /**
         * Создаёт событие смены наведения.
         *
         * @param previous прежний узел или null.
         * @param node     новый узел или null.
         */
        private NodeHoverEvent(SceneNode previous, SceneNode node) {
            this.previous = previous;
            this.node = node;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public EventType<NodeHoverEvent> type() {
            return TYPE;
        }

        /**
         * Читает прежнюю цель наведения.
         *
         * @return узел или null.
         */
        public SceneNode previous() {
            return previous;
        }

        /**
         * Читает текущую цель наведения.
         *
         * @return узел или null.
         */
        public SceneNode node() {
            return node;
        }
    }

    /**
     * Короткий клик основной кнопкой по узлу.
     */
    public static final class NodeClickEvent extends BaseEvent {
        /**
         * Тип локальной подписки на клики по узлам.
         */
        public static final EventType<NodeClickEvent> TYPE = EventType.create("scene3d.node.click");
        /**
         * Выбранный узел.
         */
        private final SceneNode node;
        /**
         * Событие отпускания указателя с экранными и локальными координатами.
         */
        private final PointerReleasedEvent pointer;

        /**
         * Создаёт событие клика.
         *
         * @param node    выбранный узел.
         * @param pointer событие отпускания указателя.
         */
        private NodeClickEvent(SceneNode node, PointerReleasedEvent pointer) {
            this.node = node;
            this.pointer = pointer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public EventType<NodeClickEvent> type() {
            return TYPE;
        }

        /**
         * Читает выбранный узел.
         *
         * @return узел клика.
         */
        public SceneNode node() {
            return node;
        }

        /**
         * Читает исходное событие указателя.
         *
         * @return отпускание основной кнопки.
         */
        public PointerReleasedEvent pointer() {
            return pointer;
        }
    }
}

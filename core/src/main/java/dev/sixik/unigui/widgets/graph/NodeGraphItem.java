package dev.sixik.unigui.widgets.graph;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.graph.node.NodeLayoutContext;
import dev.sixik.unigui.widgets.graph.node.NodeModule;
import dev.sixik.unigui.widgets.graph.node.NodeStyle;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@AiDoc(summary = "Node item container placed in a NodeGraph canvas hosting a content widget and pin ports.")
@WikiPage(
        title = "NodeGraphItem",
        slug = "node-graph-item",
        category = "<key:graph.category>",
        summary = "<key:node.graph.item.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:node.graph.item.section.overview.body>",
        order = 10
)
public final class NodeGraphItem {
    private static final float MODULAR_BOTTOM_PADDING = 4.0f;

    private final String id;
    private final Widget content;
    private final ObjectArrayList<NodeGraphPort> ports = new ObjectArrayList<>();
    private final ObjectArrayList<NodeModule> modules = new ObjectArrayList<>();
    private NodeStyle style = new NodeStyle();
    private String typeId = "";
    private boolean collapsed;
    private float[] moduleTop = new float[0];
    private float[] moduleHeight = new float[0];
    private boolean[] moduleShown = new boolean[0];
    private List<Widget> hostedWidgets = List.of();
    private boolean hostedDirty = true;
    private float x;
    private float y;
    private float width;
    private float height;
    private float arrangedWidth;
    private float arrangedHeight;
    private boolean selectable = true;
    private boolean movable = true;
    private boolean resizable = true;
    private boolean selected;
    private boolean visible = true;
    private NodeGraph owner;

    @AiDoc(summary = "Constructs a NodeGraphItem with unique ID and content widget.")
    @WikiMethod(summary = "<key:node.graph.item.method.ctorContent>")
    public NodeGraphItem(@WikiParameter(name = "id", description = "<key:node.graph.item.param.id>") String id, @WikiParameter(name = "content", description = "<key:node.graph.item.param.content>") dev.sixik.unigui.api.widget.Widget content) {
        this(id, content, 0.0f, 0.0f);
    }

    @AiDoc(summary = "Constructs a NodeGraphItem with unique ID, content widget, and initial coordinates.")
    @WikiMethod(summary = "<key:node.graph.item.method.ctorFull>")
    public NodeGraphItem(@WikiParameter(name = "id", description = "<key:node.graph.item.param.id>") String id, @WikiParameter(name = "content", description = "<key:node.graph.item.param.content>") Widget content, @WikiParameter(name = "x", description = "<key:node.graph.item.param.x>") float x, @WikiParameter(name = "y", description = "<key:node.graph.item.param.y>") float y) {
        this.id = normalizeId(id);
        this.content = Objects.requireNonNull(content, "content");
        this.x = sanitize(x);
        this.y = sanitize(y);
    }

    /**
     * Создаёт модульную ноду без content-виджета. Содержимое собирается из {@link NodeModule}.
     *
     * @param id уникальный id ноды
     */
    public NodeGraphItem(String id) {
        this(id, 0.0f, 0.0f);
    }

    /**
     * Создаёт модульную ноду без content-виджета в заданной точке.
     *
     * @param id уникальный id ноды
     * @param x X в world-координатах
     * @param y Y в world-координатах
     */
    public NodeGraphItem(String id, float x, float y) {
        this.id = normalizeId(id);
        this.content = null;
        this.x = sanitize(x);
        this.y = sanitize(y);
        // Высоту модульной ноды задаёт её содержимое, поэтому растягивание рамкой по умолчанию выключено.
        this.resizable = false;
    }

    @AiDoc(summary = "Returns the unique identifier of this node item.")
    @WikiMethod(summary = "<key:node.graph.item.method.id>")
    @WikiReturn("<key:node.graph.item.return.id>")
    public String id() {
        return id;
    }

    @AiDoc(summary = "Returns the content widget hosted inside this node, or null for a modular node built from NodeModules.")
    @WikiMethod(summary = "<key:node.graph.item.method.contentGet>")
    @WikiReturn("<key:node.graph.item.return.content>")
    public dev.sixik.unigui.api.widget.Widget content() {
        return content;
    }

    /**
     * @return {@code true}, если нода собрана из модулей и не имеет единого content-виджета
     */
    public boolean modular() {
        return content == null;
    }

    /**
     * Добавляет модуль в конец ноды и вызывает {@link NodeModule#attach(NodeGraphItem)}.
     *
     * @param module модуль; {@code null} игнорируется
     * @return эта нода
     * @throws IllegalStateException если нода создана с content-виджетом
     * @throws IllegalArgumentException если модуль с таким id уже есть
     */
    public NodeGraphItem addModule(NodeModule module) {
        if (module == null) return this;
        if (content != null) {
            throw new IllegalStateException("A content-widget node cannot host modules: " + id);
        }
        if (module(module.id()) != null) {
            throw new IllegalArgumentException("Duplicate node module id: " + module.id());
        }
        modules.add(module);
        module.attach(this);
        if (owner != null) {
            for (Widget widget : module.allWidgets()) {
                owner.adoptModuleWidget(widget);
            }
        }
        refreshModules();
        return this;
    }

    /**
     * Удаляет модуль и его порты.
     *
     * @param moduleId id модуля
     * @return эта нода
     */
    public NodeGraphItem removeModule(String moduleId) {
        NodeModule module = module(moduleId);
        if (module == null) return this;
        module.detach(this);
        modules.remove(module);
        for (Widget widget : module.allWidgets()) {
            widget.dispose();
        }
        refreshModules();
        return this;
    }

    /** @return модули сверху вниз, только для чтения */
    public List<NodeModule> modules() {
        return Collections.unmodifiableList(modules);
    }

    /**
     * @param moduleId id модуля
     * @return модуль или {@code null}
     */
    public NodeModule module(String moduleId) {
        if (moduleId == null) return null;
        for (int i = 0, size = modules.size(); i < size; i++) {
            NodeModule module = modules.get(i);
            if (module.id().equals(moduleId)) return module;
        }
        return null;
    }

    /**
     * @param type класс модуля
     * @param <T> тип модуля
     * @return первый модуль этого класса или {@code null}
     */
    public <T extends NodeModule> T module(Class<T> type) {
        for (int i = 0, size = modules.size(); i < size; i++) {
            NodeModule module = modules.get(i);
            if (type.isInstance(module)) return type.cast(module);
        }
        return null;
    }

    /** @return стиль модульной ноды */
    public NodeStyle style() {
        return style;
    }

    /**
     * Заменяет стиль ноды.
     *
     * @param style новый стиль; {@code null} возвращает стиль по умолчанию
     * @return эта нода
     */
    public NodeGraphItem style(NodeStyle style) {
        this.style = style == null ? new NodeStyle() : style;
        invalidateOwner(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /** @return id типа из {@code NodeTypeRegistry} или пустая строка */
    public String typeId() {
        return typeId;
    }

    /**
     * @param typeId id типа ноды; нужен для сохранения и восстановления модульных нод
     * @return эта нода
     */
    public NodeGraphItem typeId(String typeId) {
        this.typeId = typeId == null ? "" : typeId;
        return this;
    }

    /** @return {@code true}, если нода свёрнута до заголовка */
    public boolean collapsed() {
        return collapsed;
    }

    /**
     * Сворачивает или разворачивает ноду. Свёрнутая нода показывает только модули с
     * {@link NodeModule#visibleWhenCollapsed()}; провода скрытых портов приходят на край заголовка.
     *
     * @param collapsed {@code true}, чтобы свернуть
     * @return эта нода
     */
    public NodeGraphItem collapsed(boolean collapsed) {
        if (this.collapsed == collapsed) return this;
        this.collapsed = collapsed;
        refreshModules();
        if (owner != null) {
            owner.itemCollapsedChanged(this);
        }
        return this;
    }

    /** @return эта нода */
    public NodeGraphItem toggleCollapsed() {
        return collapsed(!collapsed);
    }

    /**
     * Сообщает, что набор видимых виджетов модулей изменился. Модули вызывают это, когда меняют результат
     * {@link NodeModule#widgets()}.
     */
    public void refreshModules() {
        hostedDirty = true;
        if (owner != null) {
            owner.itemHostedChanged();
        }
        invalidateOwner(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    /**
     * @return виджеты, участвующие в раскладке, рендере и hit-test: content-виджет или виджеты видимых модулей
     */
    public List<Widget> hostedWidgets() {
        if (content != null) {
            if (hostedDirty) {
                hostedWidgets = List.of(content);
                hostedDirty = false;
            }
            return hostedWidgets;
        }
        if (hostedDirty) {
            ObjectArrayList<Widget> widgets = new ObjectArrayList<>();
            for (int i = 0, size = modules.size(); i < size; i++) {
                NodeModule module = modules.get(i);
                if (collapsed && !module.visibleWhenCollapsed()) continue;
                widgets.addAll(module.widgets());
            }
            hostedWidgets = Collections.unmodifiableList(widgets);
            hostedDirty = false;
        }
        return hostedWidgets;
    }

    /** @return все виджеты ноды, включая скрытые; нужны для dispose и UI-контекста */
    public List<Widget> allWidgets() {
        if (content != null) return List.of(content);
        ObjectArrayList<Widget> widgets = new ObjectArrayList<>();
        for (int i = 0, size = modules.size(); i < size; i++) {
            widgets.addAll(modules.get(i).allWidgets());
        }
        return widgets;
    }

    /**
     * Добавляет порт и привязывает его к модулю: при сворачивании ноды его провод уходит на край заголовка.
     *
     * @param port порт
     * @param module модуль-владелец порта
     * @return этот порт
     */
    public NodeGraphPort addPort(NodeGraphPort port, NodeModule module) {
        addPort(port);
        port.module(module);
        return port;
    }

    /**
     * @param port порт этой ноды
     * @return {@code true}, если маркер порта нужно рисовать и можно за него хватать
     */
    public boolean portShown(NodeGraphPort port) {
        if (port == null || !port.visible()) return false;
        if (!collapsed || port.module() == null) return true;
        return port.module().visibleWhenCollapsed();
    }

    int moduleCount() {
        return modules.size();
    }

    NodeModule moduleAt(int index) {
        return modules.get(index);
    }

    boolean moduleShown(int index) {
        return index < moduleShown.length && moduleShown[index];
    }

    float moduleTop(int index) {
        return moduleTop[index];
    }

    float moduleHeight(int index) {
        return moduleHeight[index];
    }

    /**
     * Измеряет модули и вычисляет размер ноды. Виджеты модулей должны быть измерены заранее.
     */
    void measureModules(NodeLayoutContext context) {
        int count = modules.size();
        if (moduleTop.length != count) {
            moduleTop = new float[count];
            moduleHeight = new float[count];
            moduleShown = new boolean[count];
        }
        float natural = style.minWidth();
        for (int i = 0; i < count; i++) {
            NodeModule module = modules.get(i);
            boolean shown = !collapsed || module.visibleWhenCollapsed();
            moduleShown[i] = shown;
            if (shown) natural = Math.max(natural, module.measureWidth(context));
        }
        float nodeWidth = autoWidth() ? natural : Math.max(width, natural);
        float cursor = 0.0f;
        int shownCount = 0;
        for (int i = 0; i < count; i++) {
            if (!moduleShown[i]) continue;
            float height = Math.max(0.0f, modules.get(i).measureHeight(context, nodeWidth));
            moduleTop[i] = cursor;
            moduleHeight[i] = height;
            cursor += height;
            shownCount++;
        }
        if (shownCount > 1) cursor += MODULAR_BOTTOM_PADDING;
        arrangedSize(nodeWidth, Math.max(1.0f, cursor));
    }

    /** Размещает виджеты и якоря портов; вызывать после {@link #measureModules}. */
    void arrangeModules(NodeLayoutContext context) {
        float nodeWidth = arrangedWidth;
        float headerCenter = 0.0f;
        boolean headerFound = false;
        for (int i = 0, size = modules.size(); i < size; i++) {
            if (!moduleShown(i)) continue;
            if (!headerFound) {
                headerCenter = moduleTop[i] + moduleHeight[i] * 0.5f;
                headerFound = true;
            }
            modules.get(i).arrange(context, 0.0f, moduleTop[i], nodeWidth, moduleHeight[i]);
        }
        if (!collapsed) return;
        for (int i = 0, size = ports.size(); i < size; i++) {
            NodeGraphPort port = ports.get(i);
            NodeModule module = port.module();
            if (module == null || module.visibleWhenCollapsed()) continue;
            port.anchor(port.side() == NodeGraphPortSide.RIGHT ? nodeWidth : 0.0f, headerCenter);
        }
    }

    /** Передаёт нажатие модулю под курсором. */
    boolean pressModule(float localX, float localY) {
        for (int i = 0, size = modules.size(); i < size; i++) {
            if (!moduleShown(i)) continue;
            float top = moduleTop[i];
            float height = moduleHeight[i];
            if (localY < top || localY > top + height) continue;
            return modules.get(i).onPress(localX, localY, 0.0f, top, arrangedWidth, height);
        }
        return false;
    }

    /** Уведомляет модуль порта об изменении числа соединений. */
    void portConnectionsChanged(NodeGraphPort port) {
        NodeModule module = port.module();
        if (module == null) return;
        module.onPortConnectionsChanged(port, port.connectionCount());
    }

    @AiDoc(summary = "Creates and attaches a port with specified ID, direction kind, side, and offset.")
    @WikiMethod(summary = "<key:node.graph.item.method.addPortFull>")
    @WikiReturn("<key:node.graph.item.return.createdPort>")
    public NodeGraphPort addPort(@WikiParameter(name = "id", description = "<key:node.graph.port.param.id>") String id, @WikiParameter(name = "kind", description = "<key:node.graph.port.param.kind>") NodeGraphPortKind kind, @WikiParameter(name = "side", description = "<key:node.graph.port.param.side>") NodeGraphPortSide side, @WikiParameter(name = "offset", description = "<key:node.graph.port.param.offset>") float offset) {
        NodeGraphPort port = new NodeGraphPort(id, kind, side, offset);
        addPort(port);
        return port;
    }

    @AiDoc(summary = "Attaches an existing port instance to this node.")
    @WikiMethod(summary = "<key:node.graph.item.method.addPortInstance>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem addPort(@WikiParameter(name = "port", description = "<key:node.graph.item.param.port>") NodeGraphPort port) {
        if (port == null || ports.contains(port)) return this;
        if (port.owner() != null && port.owner() != this) {
            throw new IllegalArgumentException("NodeGraphPort already belongs to another NodeGraphItem");
        }
        if (port(port.id()) != null) {
            throw new IllegalArgumentException("Duplicate NodeGraph port id: " + port.id());
        }
        ports.add(port);
        port.owner(this);
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Removes the port with the specified ID.")
    @WikiMethod(summary = "<key:node.graph.item.method.removePort>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem removePort(@WikiParameter(name = "id", description = "<key:node.graph.port.param.id>") String id) {
        NodeGraphPort port = port(id);
        if (port == null) return this;
        ports.remove(port);
        port.owner(null);
        if (owner != null) {
            owner.removeConnectionsForPort(new NodeGraphPortRef(this.id, port.id()));
        }
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns the port with the specified ID, or null if not found.")
    @WikiMethod(summary = "<key:node.graph.item.method.port>")
    @WikiReturn("<key:node.graph.item.return.port>")
    public NodeGraphPort port(@WikiParameter(name = "id", description = "<key:node.graph.port.param.id>") String id) {
        if (id == null) return null;
        Object[] rawPorts = ports.elements();
        for (int portIndex = 0, portSize = ports.size(); portIndex < portSize; portIndex++) {
            NodeGraphPort port = (NodeGraphPort) rawPorts[portIndex];
            if (port.id().equals(id)) return port;
        }
        return null;
    }

    @AiDoc(summary = "Returns an unmodifiable list of all ports attached to this node.")
    @WikiMethod(summary = "<key:node.graph.item.method.ports>")
    @WikiReturn("<key:node.graph.item.return.ports>")
    public List<NodeGraphPort> ports() {
        return Collections.unmodifiableList(ports);
    }

    ObjectArrayList<NodeGraphPort> rawPorts() {
        return ports;
    }

    @AiDoc(summary = "Returns the X position of this node in canvas world coordinates.")
    @WikiMethod(summary = "<key:node.graph.item.method.x>")
    @WikiReturn("<key:node.graph.item.return.x>")
    public float x() {
        return x;
    }

    @AiDoc(summary = "Returns the Y position of this node in canvas world coordinates.")
    @WikiMethod(summary = "<key:node.graph.item.method.y>")
    @WikiReturn("<key:node.graph.item.return.y>")
    public float y() {
        return y;
    }

    @AiDoc(summary = "Sets the (X, Y) world position of this node on the canvas.")
    @WikiMethod(summary = "<key:node.graph.item.method.position>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem position(@WikiParameter(name = "x", description = "<key:node.graph.item.param.x>") float x, @WikiParameter(name = "y", description = "<key:node.graph.item.param.y>") float y) {
        float nextX = sanitize(x);
        float nextY = sanitize(y);
        if (this.x == nextX && this.y == nextY) return this;
        this.x = nextX;
        this.y = nextY;
        invalidateOwner(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns the explicit configured width of this node, or <= 0 for auto-sizing.")
    @WikiMethod(summary = "<key:node.graph.item.method.width>")
    @WikiReturn("<key:node.graph.item.return.width>")
    public float width() {
        return width;
    }

    @AiDoc(summary = "Returns the explicit configured height of this node, or <= 0 for auto-sizing.")
    @WikiMethod(summary = "<key:node.graph.item.method.height>")
    @WikiReturn("<key:node.graph.item.return.height>")
    public float height() {
        return height;
    }

    @AiDoc(summary = "Sets explicit width and height dimensions for this node.")
    @WikiMethod(summary = "<key:node.graph.item.method.size>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem size(@WikiParameter(name = "width", description = "<key:node.graph.item.param.width>") float width, @WikiParameter(name = "height", description = "<key:node.graph.item.param.height>") float height) {
        float nextWidth = sanitizeSize(width);
        float nextHeight = sanitizeSize(height);
        if (this.width == nextWidth && this.height == nextHeight) return this;
        this.width = nextWidth;
        this.height = nextHeight;
        invalidateOwner(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Resets width and height to automatic sizing based on content.")
    @WikiMethod(summary = "<key:node.graph.item.method.autoSize>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem autoSize() {
        return size(0.0f, 0.0f);
    }

    @AiDoc(summary = "Returns true if width is determined automatically by content layout.")
    @WikiMethod(summary = "<key:node.graph.item.method.autoWidth>")
    @WikiReturn("<key:node.graph.item.return.autoWidth>")
    public boolean autoWidth() {
        return width <= 0.0f;
    }

    @AiDoc(summary = "Returns true if height is determined automatically by content layout.")
    @WikiMethod(summary = "<key:node.graph.item.method.autoHeight>")
    @WikiReturn("<key:node.graph.item.return.autoHeight>")
    public boolean autoHeight() {
        return height <= 0.0f;
    }

    @AiDoc(summary = "Returns the actual arranged layout width.")
    @WikiMethod(summary = "<key:node.graph.item.method.arrangedWidth>")
    @WikiReturn("<key:node.graph.item.return.arrangedWidth>")
    public float arrangedWidth() {
        return arrangedWidth;
    }

    @AiDoc(summary = "Returns the actual arranged layout height.")
    @WikiMethod(summary = "<key:node.graph.item.method.arrangedHeight>")
    @WikiReturn("<key:node.graph.item.return.arrangedHeight>")
    public float arrangedHeight() {
        return arrangedHeight;
    }

    @AiDoc(summary = "Returns true if this node can be selected by clicks or lasso selection.")
    @WikiMethod(summary = "<key:node.graph.item.method.selectableGet>")
    @WikiReturn("<key:node.graph.item.return.selectable>")
    public boolean selectable() {
        return selectable;
    }

    @AiDoc(summary = "Sets whether this node can be selected.")
    @WikiMethod(summary = "<key:node.graph.item.method.selectableSet>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem selectable(@WikiParameter(name = "selectable", description = "<key:node.graph.item.param.selectable>") boolean selectable) {
        if (this.selectable == selectable) return this;
        this.selectable = selectable;
        if (!selectable) {
            selectedInternal(false);
        }
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns true if this node can be dragged to move across the canvas.")
    @WikiMethod(summary = "<key:node.graph.item.method.movableGet>")
    @WikiReturn("<key:node.graph.item.return.movable>")
    public boolean movable() {
        return movable;
    }

    @AiDoc(summary = "Sets whether this node can be dragged to move.")
    @WikiMethod(summary = "<key:node.graph.item.method.movableSet>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem movable(@WikiParameter(name = "movable", description = "<key:node.graph.item.param.movable>") boolean movable) {
        if (this.movable == movable) return this;
        this.movable = movable;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns true if this node displays resize handles and can be interactively resized.")
    @WikiMethod(summary = "<key:node.graph.item.method.resizableGet>")
    @WikiReturn("<key:node.graph.item.return.resizable>")
    public boolean resizable() {
        return resizable;
    }

    @AiDoc(summary = "Sets whether this node can be resized by dragging corner handles.")
    @WikiMethod(summary = "<key:node.graph.item.method.resizableSet>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem resizable(@WikiParameter(name = "resizable", description = "<key:node.graph.item.param.resizable>") boolean resizable) {
        if (this.resizable == resizable) return this;
        this.resizable = resizable;
        invalidateOwner(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Returns true if this node is currently selected.")
    @WikiMethod(summary = "<key:node.graph.item.method.selected>")
    @WikiReturn("<key:node.graph.item.return.selected>")
    public boolean selected() {
        return selected;
    }

    @AiDoc(summary = "Returns true if this node is visible.")
    @WikiMethod(summary = "<key:node.graph.item.method.visibleGet>")
    @WikiReturn("<key:node.graph.item.return.visible>")
    public boolean visible() {
        return visible;
    }

    @AiDoc(summary = "Sets whether this node is visible.")
    @WikiMethod(summary = "<key:node.graph.item.method.visibleSet>")
    @WikiReturn("<key:node.graph.item.return.self>")
    public NodeGraphItem visible(@WikiParameter(name = "visible", description = "<key:node.graph.item.param.visible>") boolean visible) {
        if (this.visible == visible) return this;
        this.visible = visible;
        invalidateOwner(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    NodeGraph owner() {
        return owner;
    }

    void owner(NodeGraph owner) {
        this.owner = owner;
    }

    void arrangedSize(float width, float height) {
        this.arrangedWidth = sanitizeSize(width);
        this.arrangedHeight = sanitizeSize(height);
    }

    void selectedInternal(boolean selected) {
        this.selected = selected && selectable;
    }

    private void invalidateOwner(int flags) {
        if (owner != null) {
            owner.invalidate(flags);
        }
    }

    private static String normalizeId(String id) {
        String normalized = id == null ? "" : id.trim();
        return normalized.isEmpty() ? "item" : normalized;
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float sanitizeSize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}

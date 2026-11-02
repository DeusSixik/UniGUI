package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.widgets.graph.node.NodeType;
import dev.sixik.unigui.widgets.graph.node.PortTypeRegistry;
import dev.sixik.unigui.widgets.graph.node.RerouteModule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Операции редактирования графа поверх публичной модели: копирование, вставка, выравнивание, группировка,
 * показ, вставка reroute. Каждая операция выполняется как одна запись истории.
 */
final class NodeGraphEditing {
    private static final float GROUP_PADDING = 16.0f;
    private static final float FRAME_MARGIN = 48.0f;
    private static final float FRAME_MAX_ZOOM = 1.5f;
    private static final float DUPLICATE_OFFSET = 24.0f;

    private NodeGraphEditing() {
    }

    static List<NodeGraphItem> selectedItems(NodeGraph graph) {
        List<NodeGraphItem> selected = new ArrayList<>();
        for (NodeGraphItem item : graph.items()) {
            if (item.selected() && item.visible()) selected.add(item);
        }
        return selected;
    }

    /**
     * Копирует выделенные ноды зарегистрированных типов и провода между ними.
     */
    static NodeGraphClipboard copy(NodeGraph graph) {
        List<NodeGraphClipboard.Node> nodes = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (NodeGraphItem item : selectedItems(graph)) {
            if (item.typeId().isEmpty() || graph.nodeTypes().get(item.typeId()) == null) continue;
            nodes.add(new NodeGraphClipboard.Node(item.typeId(), item.id(), item.x(), item.y(),
                    item.width(), item.collapsed(), item.saveState()));
            ids.add(item.id());
        }
        List<NodeGraphClipboard.Wire> wires = new ArrayList<>();
        for (NodeGraphConnection connection : graph.connections()) {
            if (ids.contains(connection.fromItemId()) && ids.contains(connection.toItemId())) {
                wires.add(new NodeGraphClipboard.Wire(connection.fromItemId(), connection.fromPortId(),
                        connection.toItemId(), connection.toPortId()));
            }
        }
        return new NodeGraphClipboard(nodes, wires);
    }

    /**
     * Вставляет буфер так, чтобы левый верхний угол группы оказался в точке.
     *
     * @return созданные ноды, выделенные после вставки
     */
    static List<NodeGraphItem> paste(NodeGraph graph, NodeGraphClipboard clipboard, float x, float y) {
        List<NodeGraphItem> created = new ArrayList<>();
        if (clipboard == null || clipboard.isEmpty()) return created;
        float dx = x - clipboard.minX();
        float dy = y - clipboard.minY();
        graph.history().transact("Paste", () -> {
            Map<String, NodeGraphItem> byOldId = new HashMap<>();
            for (NodeGraphClipboard.Node node : clipboard.nodes()) {
                NodeType type = graph.nodeTypes().get(node.typeId());
                if (type == null) continue;
                NodeGraphItem item = type.create(graph.generateItemId(node.typeId()));
                item.position(node.x() + dx, node.y() + dy);
                if (node.width() > 0.0f) item.size(node.width(), 0.0f);
                item.collapsed(node.collapsed());
                item.loadState(node.state());
                graph.addItem(item);
                byOldId.put(node.id(), item);
                created.add(item);
            }
            for (NodeGraphClipboard.Wire wire : clipboard.wires()) {
                NodeGraphItem from = byOldId.get(wire.fromNode());
                NodeGraphItem to = byOldId.get(wire.toNode());
                if (from == null || to == null) continue;
                try {
                    graph.addConnection("", new NodeGraphPortRef(from.id(), wire.fromPort()),
                            new NodeGraphPortRef(to.id(), wire.toPort()));
                } catch (IllegalArgumentException ignored) {
                    // Тип узла мог измениться с момента копирования; невалидный провод просто не восстанавливается.
                }
            }
            // После вставки выделены только вставленные ноды: иначе Delete заденет и ранее выделенный провод.
            graph.clearSelection();
            graph.clearConnectionSelection();
            graph.selectRegion(null);
            for (NodeGraphItem item : created) {
                graph.selectItem(item, true);
            }
        });
        return created;
    }

    static List<NodeGraphItem> duplicate(NodeGraph graph) {
        NodeGraphClipboard snapshot = copy(graph);
        if (snapshot.isEmpty()) return new ArrayList<>();
        return paste(graph, snapshot, snapshot.minX() + DUPLICATE_OFFSET, snapshot.minY() + DUPLICATE_OFFSET);
    }

    /**
     * Выравнивает выделенные ноды по общей рамке. Нужно минимум две ноды.
     */
    static boolean align(NodeGraph graph, NodeGraphAlign mode) {
        List<NodeGraphItem> items = movable(selectedItems(graph));
        if (items.size() < 2) return false;
        float left = Float.POSITIVE_INFINITY;
        float top = Float.POSITIVE_INFINITY;
        float right = Float.NEGATIVE_INFINITY;
        float bottom = Float.NEGATIVE_INFINITY;
        for (NodeGraphItem item : items) {
            left = Math.min(left, item.x());
            top = Math.min(top, item.y());
            right = Math.max(right, item.x() + item.arrangedWidth());
            bottom = Math.max(bottom, item.y() + item.arrangedHeight());
        }
        float[] xs = new float[items.size()];
        float[] ys = new float[items.size()];
        for (int i = 0; i < items.size(); i++) {
            NodeGraphItem item = items.get(i);
            xs[i] = item.x();
            ys[i] = item.y();
            switch (mode) {
                case LEFT -> xs[i] = left;
                case RIGHT -> xs[i] = right - item.arrangedWidth();
                case TOP -> ys[i] = top;
                case BOTTOM -> ys[i] = bottom - item.arrangedHeight();
                case CENTER_X -> xs[i] = (left + right) * 0.5f - item.arrangedWidth() * 0.5f;
                case CENTER_Y -> ys[i] = (top + bottom) * 0.5f - item.arrangedHeight() * 0.5f;
            }
        }
        return applyPositions(graph, "Align nodes", items, xs, ys);
    }

    /**
     * Равномерно распределяет выделенные ноды между крайними. Крайние остаются на месте, нужно минимум три ноды.
     */
    static boolean distribute(NodeGraph graph, boolean horizontal) {
        List<NodeGraphItem> items = movable(selectedItems(graph));
        if (items.size() < 3) return false;
        items.sort(Comparator.comparingDouble(item -> horizontal ? item.x() : item.y()));
        NodeGraphItem first = items.get(0);
        NodeGraphItem last = items.get(items.size() - 1);
        float start = horizontal ? first.x() : first.y();
        float end = horizontal ? last.x() + last.arrangedWidth() : last.y() + last.arrangedHeight();
        float occupied = 0.0f;
        for (NodeGraphItem item : items) {
            occupied += horizontal ? item.arrangedWidth() : item.arrangedHeight();
        }
        float gap = (end - start - occupied) / (items.size() - 1);
        float[] xs = new float[items.size()];
        float[] ys = new float[items.size()];
        float cursor = start;
        for (int i = 0; i < items.size(); i++) {
            NodeGraphItem item = items.get(i);
            xs[i] = horizontal ? cursor : item.x();
            ys[i] = horizontal ? item.y() : cursor;
            cursor += (horizontal ? item.arrangedWidth() : item.arrangedHeight()) + gap;
        }
        return applyPositions(graph, "Distribute nodes", items, xs, ys);
    }

    private static List<NodeGraphItem> movable(List<NodeGraphItem> items) {
        items.removeIf(item -> !item.movable());
        return items;
    }

    private static boolean applyPositions(NodeGraph graph, String name, List<NodeGraphItem> items,
                                          float[] xs, float[] ys) {
        boolean[] changed = new boolean[1];
        graph.history().transact(name, () -> {
            for (int i = 0; i < items.size(); i++) {
                NodeGraphItem item = items.get(i);
                float fromX = item.x();
                float fromY = item.y();
                float toX = xs[i];
                float toY = ys[i];
                if (fromX == toX && fromY == toY) continue;
                changed[0] = true;
                graph.moveItemTo(item, toX, toY);
                graph.history().record(NodeGraphHistory.Operation.of(
                        () -> graph.moveItemTo(item, fromX, fromY), () -> graph.moveItemTo(item, toX, toY)));
            }
        });
        return changed[0];
    }

    /**
     * Оборачивает выделенные ноды в регион-комментарий.
     */
    static NodeGraphRegion group(NodeGraph graph, String title) {
        List<NodeGraphItem> items = selectedItems(graph);
        NodeGraphRegion inner = graph.selectedRegion();
        if (items.isEmpty() && inner == null) return null;
        float left = Float.POSITIVE_INFINITY;
        float top = Float.POSITIVE_INFINITY;
        float right = Float.NEGATIVE_INFINITY;
        float bottom = Float.NEGATIVE_INFINITY;
        for (NodeGraphItem item : items) {
            left = Math.min(left, item.x());
            top = Math.min(top, item.y());
            right = Math.max(right, item.x() + item.arrangedWidth());
            bottom = Math.max(bottom, item.y() + item.arrangedHeight());
        }
        if (inner != null) {
            // Выделенный регион тоже входит в группу: так регионы вкладываются друг в друга.
            left = Math.min(left, inner.x());
            top = Math.min(top, inner.y());
            right = Math.max(right, inner.x() + inner.width());
            bottom = Math.max(bottom, inner.y() + inner.height());
        }
        float x = left - GROUP_PADDING;
        float y = top - GROUP_PADDING - NodeGraphRegion.HEADER_HEIGHT;
        float width = right - left + GROUP_PADDING * 2.0f;
        float height = bottom - top + GROUP_PADDING * 2.0f + NodeGraphRegion.HEADER_HEIGHT;
        NodeGraphRegion[] holder = new NodeGraphRegion[1];
        graph.history().transact("Group nodes", () -> {
            holder[0] = graph.addRegion("", title, x, y, width, height);
        });
        graph.clearSelection();
        graph.clearConnectionSelection();
        graph.selectRegion(holder[0]);
        return holder[0];
    }

    /**
     * Подбирает положение и зум так, чтобы прямоугольник влез в окно с полями.
     */
    static void frame(NodeGraph graph, float minX, float minY, float maxX, float maxY) {
        RectView bounds = graph.layoutBounds();
        if (bounds.width() <= 0.0f || bounds.height() <= 0.0f) return;
        float spanX = Math.max(1.0f, maxX - minX);
        float spanY = Math.max(1.0f, maxY - minY);
        float fit = Math.min((bounds.width() - FRAME_MARGIN * 2.0f) / spanX,
                (bounds.height() - FRAME_MARGIN * 2.0f) / spanY);
        float zoom = Math.max(graph.minZoom(), Math.min(Math.min(graph.maxZoom(), FRAME_MAX_ZOOM), fit));
        graph.viewport(bounds.width() * 0.5f - (minX + maxX) * 0.5f * zoom,
                bounds.height() * 0.5f - (minY + maxY) * 0.5f * zoom, zoom);
    }

    static void frameAll(NodeGraph graph) {
        float[] box = bounds(graph, false);
        if (box != null) frame(graph, box[0], box[1], box[2], box[3]);
    }

    static void frameSelection(NodeGraph graph) {
        float[] box = bounds(graph, true);
        if (box == null) box = bounds(graph, false);
        if (box != null) frame(graph, box[0], box[1], box[2], box[3]);
    }

    /**
     * @return {@code minX, minY, maxX, maxY} нод, регионов (или только выделенного) либо {@code null}
     */
    private static float[] bounds(NodeGraph graph, boolean selectedOnly) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (NodeGraphItem item : graph.items()) {
            if (!item.visible() || (selectedOnly && !item.selected())) continue;
            minX = Math.min(minX, item.x());
            minY = Math.min(minY, item.y());
            maxX = Math.max(maxX, item.x() + item.arrangedWidth());
            maxY = Math.max(maxY, item.y() + item.arrangedHeight());
        }
        for (NodeGraphRegion region : graph.regions()) {
            if (selectedOnly && !region.selected()) continue;
            minX = Math.min(minX, region.x());
            minY = Math.min(minY, region.y());
            maxX = Math.max(maxX, region.x() + region.width());
            maxY = Math.max(maxY, region.y() + region.height());
        }
        return Float.isFinite(minX) ? new float[]{minX, minY, maxX, maxY} : null;
    }

    /**
     * Вставляет reroute в провод: провод заменяется парой «источник → reroute → приёмник».
     *
     * @return созданная нода или {@code null}, если порты провода не найдены
     */
    static NodeGraphItem insertReroute(NodeGraph graph, NodeGraphConnection connection, float worldX, float worldY) {
        if (connection == null) return null;
        NodeGraphPort from = graph.port(connection.from());
        NodeGraphPort to = graph.port(connection.to());
        if (from == null || to == null) return null;
        boolean flow = graph.portTypes().get(from.type()).flow();
        NodeGraphPortRef fromRef = connection.from();
        NodeGraphPortRef toRef = connection.to();
        NodeGraphItem[] holder = new NodeGraphItem[1];
        graph.history().transact("Insert reroute", () -> {
            String id = graph.generateItemId(flow ? RerouteModule.FLOW_TYPE_ID : RerouteModule.DATA_TYPE_ID);
            NodeGraphItem reroute = RerouteModule.createNode(id, flow);
            reroute.position(worldX - 18.0f, worldY - 10.0f);
            graph.removeConnection(connection);
            graph.addItem(reroute);
            graph.addConnection("", fromRef, new NodeGraphPortRef(id, RerouteModule.INPUT));
            graph.addConnection("", new NodeGraphPortRef(id, RerouteModule.OUTPUT), toRef);
            holder[0] = reroute;
        });
        return holder[0];
    }

    /**
     * @return id типа порта с учётом reroute: тип провода после reroute равен типу источника
     */
    static String effectiveType(NodeGraph graph, NodeGraphPort port) {
        return chainRoot(graph, port).type();
    }

    /**
     * Идёт от порта вверх по цепочке data-reroute до настоящего источника.
     *
     * @return порт-источник или, если цепочка ни к чему не подключена, последний reroute-порт
     */
    private static NodeGraphPort chainRoot(NodeGraph graph, NodeGraphPort port) {
        NodeGraphPort current = port;
        for (int depth = 0; depth < 32; depth++) {
            if (!isDataReroute(current)) return current;
            NodeGraphItem owner = graph.item(current.itemId());
            NodeGraphPort input = owner == null ? null : owner.port(RerouteModule.INPUT);
            if (input == null) return current;
            NodeGraphPort upstream = null;
            NodeGraphPortRef inputRef = input.ref();
            for (NodeGraphConnection connection : graph.connections()) {
                if (connection.to().equals(inputRef)) {
                    upstream = graph.port(connection.from());
                    break;
                }
            }
            if (upstream == null) return current;
            current = upstream;
        }
        return current;
    }

    private static boolean isDataReroute(NodeGraphPort port) {
        return port != null && port.module() instanceof RerouteModule reroute && !reroute.flow();
    }

    private static boolean isAnyType(String id) {
        return id == null || id.isEmpty() || PortTypeRegistry.ANY.id().equals(id);
    }

    /**
     * Проверяет типы соединения, в котором участвует data-reroute. Сам reroute принимает любой тип, поэтому
     * сравнивать нужно настоящий источник (за цепочкой reroute) с настоящими приёмниками (за ней же).
     *
     * @return причина отказа или {@code null}, если reroute ничему не мешает
     */
    static String rerouteConflict(NodeGraph graph, NodeGraphPort from, NodeGraphPort to) {
        boolean fromReroute = isDataReroute(from);
        boolean toReroute = isDataReroute(to);
        if (!fromReroute && !toReroute) return null;
        PortTypeRegistry types = graph.portTypes();
        List<String> sinks = new ArrayList<>();
        if (toReroute) {
            collectSinks(graph, to.itemId(), sinks, new HashSet<>());
        } else {
            sinks.add(to.type());
        }
        NodeGraphPort root = chainRoot(graph, from);
        if (!isDataReroute(root)) {
            for (String sink : sinks) {
                if (!types.canConnect(root.type(), sink)) return "Port type mismatch";
            }
            return null;
        }
        // У цепочки ещё нет источника: будущий источник должен подойти всем её приёмникам сразу.
        List<String> existing = new ArrayList<>();
        collectSinks(graph, root.itemId(), existing, new HashSet<>());
        for (String added : sinks) {
            for (String present : existing) {
                if (!types.canConnect(added, present) && !types.canConnect(present, added)) {
                    return "Port type mismatch";
                }
            }
        }
        return null;
    }

    /**
     * Собирает типы настоящих (не reroute) приёмников, достижимых с выхода reroute.
     */
    private static void collectSinks(NodeGraph graph, String rerouteItemId, List<String> result, Set<String> visited) {
        if (!visited.add(rerouteItemId)) return;
        NodeGraphPortRef output = new NodeGraphPortRef(rerouteItemId, RerouteModule.OUTPUT);
        for (NodeGraphConnection connection : graph.connections()) {
            if (!connection.from().equals(output)) continue;
            NodeGraphPort target = graph.port(connection.to());
            if (target == null) continue;
            if (isDataReroute(target)) {
                collectSinks(graph, target.itemId(), result, visited);
            } else if (!isAnyType(target.type())) {
                result.add(target.type());
            }
        }
    }
}

package dev.sixik.unigui.widgets.world;


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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Слой screen-space виджетов, закрепленных за world-точками {@link WorldCanvas}.
 *
 * <p>{@code AnchorLayer} решает типичную задачу карты/нод-графа: объект находится
 * в world/map координатах, но сам виджет должен оставаться обычным UI-виджетом.
 * Например маркер карты может иметь постоянный экранный размер, tooltip, hover,
 * click и внутренние дочерние элементы, но его центр проецируется из world-точки.</p>
 *
 * <p>Для карты чаще используют обертки {@link dev.sixik.unigui.widgets.map.MapCanvas}
 * и {@link dev.sixik.unigui.widgets.map.MapMarkerHandle}, но прямой доступ к
 * {@code anchorLayer()} полезен для generic {@link WorldCanvas}.</p>
 */
@AiDoc(summary = "Layer managing screen-space UI widgets pinned to 3D/2D world coordinates inside WorldCanvas.")
@WikiPage(
        title = "AnchorLayer",
        slug = "anchor-layer",
        category = "<key:world.category>",
        summary = "<key:anchor.layer.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:anchor.layer.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:anchor.layer.example.basic.title>",
        code = "AnchorWidget anchor = canvas.anchorLayer().add(\"flag\", worldX, worldY, new Button(\"Waypoint\"))\n    .screenSize(80.0f, 24.0f)\n    .pivot(0.5f, 1.0f);"
)
@WikiLink(
        label = "WorldCanvas",
        target = "page:world-canvas",
        description = "<key:world.canvas.summary>"
)
@WikiLink(
        label = "AnchorWidget",
        target = "page:anchor-widget",
        description = "<key:anchor.widget.summary>"
)
public final class AnchorLayer {
    private final WorldCanvas owner;
    private final ObjectArrayList<AnchorWidget> anchors = new ObjectArrayList<>();
    private final List<AnchorWidget> anchorsView = Collections.unmodifiableList(anchors);
    private AnchorWidget[] snapshot = new AnchorWidget[0];
    private boolean snapshotDirty = true;

    AnchorLayer(WorldCanvas owner) {
        this.owner = owner;
    }

    /**
     * Добавляет виджет, закрепленный за world-точкой.
     *
     * <p>{@code worldX/worldY} — это координаты в системе {@link WorldCanvas}, а
     * не пиксели экрана. При pan/zoom канвас сам пересчитает позицию виджета.</p>
     *
     * @param id     опциональный id; непустой id должен быть уникален внутри слоя
     * @param worldX world X, к которому привязан anchor
     * @param worldY world Y, к которому привязан anchor
     * @param widget обычный UI-виджет, который будет отрендерен поверх слоев
     * @return созданный anchor для настройки размера, pivot, видимости и culling
     */
        @AiDoc(summary = "Adds a UI widget anchored to the specified world coordinates.")
    @WikiMethod(summary = "<key:anchor.layer.method.add>")
    @WikiReturn("<key:anchor.layer.return.anchorWidget>")
    public AnchorWidget add(
            @WikiParameter(description = "<key:anchor.layer.param.id>")
            String id,
            @WikiParameter(description = "<key:anchor.layer.param.worldX>")
            float worldX,
            @WikiParameter(description = "<key:anchor.layer.param.worldY>")
            float worldY,
            @WikiParameter(description = "<key:anchor.layer.param.widget>")
            Widget widget
    ) {
        if (widget == null) {
            throw new IllegalArgumentException("Anchor widget cannot be null");
        }
        String normalizedId = normalizeId(id);
        if (!normalizedId.isEmpty() && anchor(normalizedId) != null) {
            throw new IllegalArgumentException("Duplicate anchor id: " + normalizedId);
        }
        AnchorWidget anchor = new AnchorWidget(normalizedId, worldX, worldY, widget);
        anchor.owner(this);
        anchors.add(anchor);
        snapshotDirty = true;
        owner.attachAnchor(anchor);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return anchor;
    }

    /**
     * Ищет anchor по id.
     *
     * @param id id, переданный в {@link #add(String, float, float, Widget)}
     * @return anchor или {@code null}, если id пустой/не найден
     */
        @AiDoc(summary = "Finds an AnchorWidget by its string identifier, or returns null if not found.")
    @WikiMethod(summary = "<key:anchor.layer.method.anchor>")
    @WikiReturn("<key:anchor.layer.return.anchorWidget>")
    public AnchorWidget anchor(
            @WikiParameter(description = "<key:anchor.layer.param.id>")
            String id
    ) {
        String normalizedId = normalizeId(id);
        Object[] raw = anchors.elements();
        for (int i = 0, size = anchors.size(); i < size; i++) {
            AnchorWidget anchor = (AnchorWidget) raw[i];
            if (anchor.id().equals(normalizedId)) return anchor;
        }
        return null;
    }

    /**
     * Удаляет anchor по id вместе с его виджетом из {@link WorldCanvas}.
     */
        @AiDoc(summary = "Removes an anchored widget by its string identifier.")
    @WikiMethod(summary = "<key:anchor.layer.method.removeById>")
    @WikiReturn("<key:anchor.layer.return.success>")
    public boolean remove(
            @WikiParameter(description = "<key:anchor.layer.param.id>")
            String id
    ) {
        AnchorWidget anchor = anchor(id);
        return anchor != null && remove(anchor);
    }

    /**
     * Удаляет конкретный anchor вместе с его виджетом из {@link WorldCanvas}.
     */
        @AiDoc(summary = "Removes the specified AnchorWidget from this layer.")
    @WikiMethod(summary = "<key:anchor.layer.method.removeByAnchor>")
    @WikiReturn("<key:anchor.layer.return.success>")
    public boolean remove(
            @WikiParameter(description = "<key:anchor.layer.param.anchor>")
            AnchorWidget anchor
    ) {
        if (anchor == null || !anchors.remove(anchor)) return false;
        snapshotDirty = true;
        anchor.owner(null);
        owner.detachAnchor(anchor);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return true;
    }

    /**
     * Удаляет все anchors и отсоединяет их виджеты от {@link WorldCanvas}.
     */
        @AiDoc(summary = "Removes and detaches all anchors from this layer.")
    @WikiMethod(summary = "<key:anchor.layer.method.clear>")
    public void clear() {
        Object[] raw = anchors.elements();
        for (int i = 0, size = anchors.size(); i < size; i++) {
            AnchorWidget anchor = (AnchorWidget) raw[i];
            anchor.owner(null);
            owner.detachAnchor(anchor);
        }
        anchors.clear();
        snapshotDirty = true;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    /**
     * Возвращает read-only список anchors.
     */
        @AiDoc(summary = "Returns an unmodifiable list of all active anchors in this layer.")
    @WikiMethod(summary = "<key:anchor.layer.method.anchors>")
    @WikiReturn("<key:anchor.layer.return.anchors>")
    public List<AnchorWidget> anchors() {
        return anchorsView;
    }

    /**
     * Количество anchors в слое.
     */
        @AiDoc(summary = "Returns the count of active anchors in this layer.")
    @WikiMethod(summary = "<key:anchor.layer.method.size>")
    @WikiReturn("<key:anchor.layer.return.count>")
    public int size() {
        return anchors.size();
    }

    AnchorWidget[] snapshot() {
        if (snapshotDirty) {
            snapshot = anchors.toArray(new AnchorWidget[anchors.size()]);
            snapshotDirty = false;
        }
        return snapshot;
    }

    void invalidate(int flags) {
        owner.invalidate(flags);
    }

    private static String normalizeId(String id) {
        return id == null ? "" : id.trim();
    }
}

package dev.sixik.unigui.widgets.world;

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
    public AnchorWidget add(String id, float worldX, float worldY, Widget widget) {
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
    public AnchorWidget anchor(String id) {
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
    public boolean remove(String id) {
        AnchorWidget anchor = anchor(id);
        return anchor != null && remove(anchor);
    }

    /**
     * Удаляет конкретный anchor вместе с его виджетом из {@link WorldCanvas}.
     */
    public boolean remove(AnchorWidget anchor) {
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
    public List<AnchorWidget> anchors() {
        return anchorsView;
    }

    /**
     * Количество anchors в слое.
     */
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

package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.widget.Widget;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Screen-space widgets projected from world-space points.
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

    public AnchorWidget anchor(String id) {
        String normalizedId = normalizeId(id);
        Object[] raw = anchors.elements();
        for (int i = 0, size = anchors.size(); i < size; i++) {
            AnchorWidget anchor = (AnchorWidget) raw[i];
            if (anchor.id().equals(normalizedId)) return anchor;
        }
        return null;
    }

    public boolean remove(String id) {
        AnchorWidget anchor = anchor(id);
        return anchor != null && remove(anchor);
    }

    public boolean remove(AnchorWidget anchor) {
        if (anchor == null || !anchors.remove(anchor)) return false;
        snapshotDirty = true;
        anchor.owner(null);
        owner.detachAnchor(anchor);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return true;
    }

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

    public List<AnchorWidget> anchors() {
        return anchorsView;
    }

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

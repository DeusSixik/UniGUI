package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.input.HitTestResult;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.Visibility;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Координатор drag-and-drop для одного runtime-дерева.
 *
 * <p>{@link DragSource} отвечает за жест указателя и pointer capture, а
 * {@link DropTarget} — за проверку payload и обработку drop. Этот менеджер
 * связывает их: на каждом перемещении ищет target под курсором, обновляет
 * preview и при отпускании вызывает {@link DropTarget#requestDrop}.</p>
 *
 * <p>Менеджер не создаёт отдельный input pipeline. Он использует hit tester
 * текущего {@link dev.sixik.unigui.api.core.UIContext}, поэтому корректно
 * учитывает порядок детей и transform'ы.</p>
 */
public final class DragAndDropManager {
    private final Widget root;
    private final Set<DragSource> sources = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<DropTarget> targets = Collections.newSetFromMap(new IdentityHashMap<>());
    private DragSource activeSource;
    private DragPayload activePayload;
    private DropTarget activeTarget;

    public DragAndDropManager(Widget root) {
        this.root = root;
    }

    /** @return корень дерева, в котором выполняется поиск drop target'ов */
    public Widget root() {
        return root;
    }

    /** Регистрирует источник и подключает его к этому менеджеру. */
    public DragAndDropManager register(DragSource source) {
        if (source != null) {
            sources.add(source);
            source.dragAndDropManager(this);
        }
        return this;
    }

    /** Отключает источник от этого менеджера. */
    public DragAndDropManager unregister(DragSource source) {
        if (source != null) {
            sources.remove(source);
            source.dragAndDropManager(null);
            if (activeSource == source) cancel(source);
        }
        return this;
    }

    /** Регистрирует target. Динамические targets внутри root находятся автоматически. */
    public DragAndDropManager register(DropTarget target) {
        if (target != null) targets.add(target);
        return this;
    }

    /** Убирает target из явного списка регистрации. */
    public DragAndDropManager unregister(DropTarget target) {
        if (target != null) {
            targets.remove(target);
            if (activeTarget == target) {
                target.clearDropPreview();
                activeTarget = null;
            }
        }
        return this;
    }

    public DragSource activeSource() {
        return activeSource;
    }

    public DragPayload activePayload() {
        return activePayload;
    }

    public DropTarget activeTarget() {
        return activeTarget;
    }

    /** Очищает зарегистрированные источники, target'ы и текущий drag-сеанс. */
    public DragAndDropManager clear() {
        for (DragSource source : List.copyOf(sources)) {
            if (source.pointerActive()) source.cancelDrag();
            source.dragAndDropManager(null);
        }
        clearSession();
        sources.clear();
        targets.clear();
        return this;
    }

    void sourceStarted(DragSource source, float rootX, float rootY) {
        if (source == null || !sources.contains(source)) return;
        activeSource = source;
        activePayload = source.payload();
        updateTarget(rootX, rootY);
    }

    void sourceMoved(DragSource source, float rootX, float rootY) {
        if (source == activeSource) updateTarget(rootX, rootY);
    }

    DropTarget.DropResult sourceDropped(DragSource source, float rootX, float rootY) {
        if (source != activeSource) return DropTarget.DropResult.IGNORED;
        updateTarget(rootX, rootY);
        DropTarget target = activeTarget;
        DropTarget.DropResult result = target == null
                ? DropTarget.DropResult.IGNORED
                : target.requestDrop(activePayload == null ? source.payload() : activePayload, rootX, rootY);
        clearActiveTarget();
        return result;
    }

    void sourceFinished(DragSource source) {
        if (source == activeSource) clearSession();
    }

    void sourceCancelled(DragSource source) {
        if (source == activeSource) clearSession();
    }

    private void updateTarget(float rootX, float rootY) {
        DropTarget next = findTarget(rootX, rootY);
        if (next == activeTarget) {
            if (next != null) next.previewDrop(activePayload, rootX, rootY);
            return;
        }
        clearActiveTarget();
        activeTarget = next;
        if (next != null) next.previewDrop(activePayload, rootX, rootY);
    }

    private DropTarget findTarget(float rootX, float rootY) {
        if (root == null || activePayload == null) return null;
        Set<DropTarget> candidates = Collections.newSetFromMap(new IdentityHashMap<>());
        candidates.addAll(targets);
        collectTargets(root, candidates);

        HitTestResult hit = root.uiContext() == null
                ? null
                : root.uiContext().hitTester().hitTest(root, rootX, rootY).orElse(null);
        DropTarget best = null;
        int bestDepth = -1;
        for (DropTarget target : candidates) {
            if (target.visibility() != Visibility.VISIBLE || !target.enabled()) continue;
            if (!containsInHitPath(hit == null ? null : hit.widget(), target)) continue;
            int depth = depth(target);
            if (depth > bestDepth) {
                best = target;
                bestDepth = depth;
            }
        }
        return best;
    }

    private static void collectTargets(Widget widget, Set<DropTarget> result) {
        if (widget == null || widget.visibility() == Visibility.COLLAPSED) return;
        if (widget instanceof DropTarget target) result.add(target);
        for (Widget child : widget.children()) collectTargets(child, result);
    }

    private static boolean containsInHitPath(Widget hit, DropTarget target) {
        Widget current = hit;
        while (current != null) {
            if (current == target) return true;
            current = current.parent();
        }
        return false;
    }

    private static int depth(Widget widget) {
        int depth = 0;
        for (Widget current = widget; current != null; current = current.parent()) depth++;
        return depth;
    }

    private void clearActiveTarget() {
        if (activeTarget != null) activeTarget.clearDropPreview();
        activeTarget = null;
    }

    private void clearSession() {
        clearActiveTarget();
        activeSource = null;
        activePayload = null;
    }

    private void cancel(DragSource source) {
        if (source == activeSource) source.cancelDrag();
        else clearSession();
    }
}

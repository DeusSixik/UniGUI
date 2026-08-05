package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PanelWidget extends WidgetBase {
    private final List<Widget> children = new ArrayList<>();
    private final Queue<ChildMutation> mutations = new ConcurrentLinkedQueue<>();

    public void addChild(Widget child) {
        if (child == null) return;
        mutations.add(new ChildMutation(ChildMutationType.ADD, child));
        invalidate(InvalidationFlags.LAYOUT);
    }

    public void removeChild(Widget child) {
        if (child == null) return;
        mutations.add(new ChildMutation(ChildMutationType.REMOVE, child));
        invalidate(InvalidationFlags.LAYOUT);
    }

    public void clearChildren() {
        mutations.add(new ChildMutation(ChildMutationType.CLEAR, null));
        invalidate(InvalidationFlags.LAYOUT);
    }

    public void applyQueuedMutations() {
        ChildMutation mutation;
        while ((mutation = mutations.poll()) != null) {
            switch (mutation.type) {
                case ADD -> applyAdd(mutation.child);
                case REMOVE -> applyRemove(mutation.child);
                case CLEAR -> applyClear();
            }
        }
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        for (Widget child : children) {
            if (child instanceof WidgetBase base) {
                base.setUiContextInternal(uiContext);
            }
        }
    }

    @Override
    public List<Widget> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void measure(LayoutContext context) {
        applyQueuedMutations();
        for (Widget child : snapshotChildren()) {
            child.measure(context);
        }
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        for (Widget child : snapshotChildren()) {
            child.arrange(bounds);
        }
    }

    @Override
    public void render(RenderContext context) {
        applyQueuedMutations();
        for (Widget child : snapshotChildren()) {
            child.render(context);
        }
    }

    @Override
    public void tick(FrameContext frame) {
        applyQueuedMutations();
        for (Widget child : snapshotChildren()) {
            child.tick(frame);
        }
    }

    @Override
    public void dispose() {
        applyClear();
    }

    private List<Widget> snapshotChildren() {
        return List.copyOf(children);
    }

    private void applyAdd(Widget child) {
        if (children.contains(child)) return;
        children.add(child);
        if (child instanceof WidgetBase base) {
            base.setParentInternal(this);
            base.setUiContextInternal(uiContext());
        }
    }

    private void applyRemove(Widget child) {
        if (!children.remove(child)) return;
        if (child instanceof WidgetBase base) {
            base.setParentInternal(null);
            base.setUiContextInternal(null);
        }
    }

    private void applyClear() {
        for (Widget child : children) {
            if (child instanceof WidgetBase base) {
                base.setParentInternal(null);
                base.setUiContextInternal(null);
            }
            child.dispose();
        }
        children.clear();
    }

    private enum ChildMutationType {
        ADD,
        REMOVE,
        CLEAR
    }

    private static final class ChildMutation {
        private final ChildMutationType type;
        private final Widget child;

        private ChildMutation(ChildMutationType type, Widget child) {
            this.type = type;
            this.child = child;
        }
    }
}

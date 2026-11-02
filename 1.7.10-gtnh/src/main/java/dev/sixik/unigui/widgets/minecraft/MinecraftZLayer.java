package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.input.HitTestChildrenOnly;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.gtnh.backend.GtnhGuiRenderBackend;
import dev.sixik.unigui.impl.layout.AbsoluteLayoutEngine;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.feedback.OverlayHostAware;
import org.lwjgl.opengl.GL11;

/**
 * Overlay layer whose children are drawn in one block at a given depth, after everything the owner has queued.
 *
 * <p>Items and entities are drawn with the vanilla depth-tested renderers; a panel that must stay above them is put
 * in this layer. On 1.7.10 the UI is drawn with the depth test off and previews clear their own depth, so the layer
 * mostly keeps its draw order and applies the {@code z} translation for renderers that do test depth.</p>
 */
public final class MinecraftZLayer extends PanelWidget implements OverlayHostAware, HitTestChildrenOnly {
    private final float z;

    public MinecraftZLayer(Widget content, float z) {
        this.z = Float.isFinite(z) ? z : 0.0f;
        // The layer itself must take part in hit testing: only its own surface is transparent to the pointer,
        // so that clicks fall through to the widgets behind it.
        enabled(true);
        addChild(content);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        for (Widget child : children()) {
            if (child.visibility() != Visibility.COLLAPSED) {
                child.measure(context);
            }
        }
        setDesiredSize(resolveDesiredSize(context, 0.0f, 0.0f));
    }

    @Override
    public void arrange(RectView bounds) {
        arrangeInHost(bounds);
    }

    @Override
    public void arrangeInHost(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        for (Widget child : children()) {
            if (child.visibility() == Visibility.COLLAPSED) continue;
            if (child instanceof OverlayHostAware hostAware) {
                hostAware.arrangeInHost(bounds);
            } else {
                AbsoluteLayoutEngine.arrange(child, bounds);
            }
        }
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE) return;
        prepareChildrenForRender();
        if (!(context.backend() instanceof GtnhGuiRenderBackend)) {
            super.render(context);
            return;
        }

        DrawList nestedDrawList = new DrawList();
        DefaultRenderContext nestedContext = new DefaultRenderContext(nestedDrawList)
                .backend(context.backend());
        nestedContext.pushOpacity(context.opacityMultiplier());
        pushOpacity(nestedContext);
        try {
            renderChildren(nestedContext);
        } finally {
            popOpacity(nestedContext);
            nestedContext.popOpacity();
        }
        if (nestedDrawList.size() == 0) return;

        context.custom((MinecraftScaledCustomDraw) (backend, scale) -> {
            if (!(backend instanceof GtnhGuiRenderBackend minecraftBackend)) return;
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(0.0f, 0.0f, z);
                minecraftBackend.renderNested(nestedDrawList, scale);
            } finally {
                GL11.glPopMatrix();
            }
        });
    }

    private void prepareChildrenForRender() {
        applyQueuedMutations();
        LayoutContext context = new LayoutContext(layoutBounds().width(), layoutBounds().height());
        for (Widget child : children()) {
            if (child.visibility() == Visibility.COLLAPSED) continue;
            child.measure(context);
            if (child instanceof OverlayHostAware hostAware) {
                hostAware.arrangeInHost(layoutBounds());
            } else {
                AbsoluteLayoutEngine.arrange(child, layoutBounds());
            }
        }
    }
}
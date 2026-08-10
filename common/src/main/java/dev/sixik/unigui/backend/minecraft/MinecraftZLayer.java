package dev.sixik.unigui.backend.minecraft;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.OverlayHostAware;
import dev.sixik.unigui.widgets.PanelWidget;

/**
 * Overlay helper that raises nested regular draw commands above Minecraft custom preview renders.
 */
final class MinecraftZLayer extends PanelWidget implements OverlayHostAware {
    private final float z;

    MinecraftZLayer(Widget content, float z) {
        this.z = Float.isFinite(z) ? z : 0.0f;
        enabled(false);
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
                child.arrange(bounds);
            }
        }
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE) return;
        prepareChildrenForRender();
        if (!(context.backend() instanceof MinecraftGuiRenderBackend)) {
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
            if (!(backend instanceof MinecraftGuiRenderBackend minecraftBackend)) return;
            float normalizedScale = Float.isFinite(scale) && scale > 0.0f ? scale : 1.0f;
            minecraftBackend.graphics().pose().pushPose();
            try {
                minecraftBackend.graphics().pose().scale(normalizedScale, normalizedScale, 1.0f);
                minecraftBackend.graphics().pose().translate(0.0f, 0.0f, z);
                minecraftBackend.renderNested(nestedDrawList, normalizedScale);
            } finally {
                minecraftBackend.graphics().pose().popPose();
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
                child.arrange(layoutBounds());
            }
        }
    }
}

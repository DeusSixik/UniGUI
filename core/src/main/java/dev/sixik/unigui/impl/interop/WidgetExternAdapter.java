package dev.sixik.unigui.impl.interop;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.core.UiDispatcher;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.interop.ExternMeasureContext;
import dev.sixik.unigui.api.interop.ExternRenderContext;
import dev.sixik.unigui.api.interop.WidgetExtern;
import dev.sixik.unigui.api.interop.WidgetExternHost;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.core.ImmediateUiDispatcher;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.Objects;

public final class WidgetExternAdapter extends WidgetBase implements WidgetExternHost {
    private final WidgetExtern extern;

    public WidgetExternAdapter(WidgetExtern extern) {
        this.extern = Objects.requireNonNull(extern, "extern");
        this.extern.onAttach(this);
    }

    public WidgetExtern extern() {
        return extern;
    }

    @Override
    public void measure(LayoutContext context) {
        ExternMeasureContext externContext = new ExternMeasureContext(context);
        extern.measure(externContext);
        setDesiredSize(resolveDesiredSize(context, externContext.desiredSize().width(), externContext.desiredSize().height()));
    }

    @Override
    public void render(RenderContext context) {
        extern.render(new ExternRenderContext(context));
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (!event.isCancelled()) {
            extern.handle(event);
        }
    }

    @Override
    public void tick(dev.sixik.unigui.api.core.FrameContext frame) {
        extern.tick(frame);
    }

    @Override
    public void dispose() {
        extern.dispose();
        extern.onDetach();
    }

    @Override
    public Widget widget() {
        return this;
    }

    @Override
    public UIContext ui() {
        return uiContext();
    }

    @Override
    public UiDispatcher dispatcher() {
        UIContext context = uiContext();
        return context == null ? ImmediateUiDispatcher.INSTANCE : context.dispatcher();
    }

    @Override
    public void invalidateLayout() {
        invalidate(InvalidationFlags.LAYOUT);
    }

    @Override
    public void invalidateVisual() {
        invalidate(InvalidationFlags.VISUAL);
    }

    @Override
    public void invalidateTexture() {
        invalidate(InvalidationFlags.TEXTURE);
    }
}

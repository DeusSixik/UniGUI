package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

/**
 * Визуальный «призрак» перетаскиваемого объекта, который рисуется в overlay под курсором.
 *
 * <p>Ghost получает {@link RenderContext} overlay-слоя: его команды рисуются поверх всего UI и не
 * обрезаются clip'ами контейнеров, из которых объект тянут.</p>
 */
@FunctionalInterface
@AiDoc(
        description = "Renderer for visual ghost representation of dragged elements displayed in the overlay layer beneath the cursor."
)
@WikiPage(
        title = "DragGhost",
        slug = "drag-ghost",
        category = "<key:dnd.category>",
        summary = "<key:drag.ghost.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:drag.ghost.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:drag.ghost.example.widget.title>",
        code = "DragGhost ghost = DragGhost.widget(itemWidget, 0.8f);"
)
@WikiLink(
        label = "DragController",
        target = "page:drag-controller",
        description = "<key:drag.controller.summary>"
)
@WikiLink(
        label = "DragSession",
        target = "page:drag-session",
        description = "<key:drag.session.summary>"
)
public interface DragGhost {
    /**
     * Рисует ghost для текущего кадра.
     *
     * @param context контекст рендера overlay-слоя
     * @param session активная сессия: смещение и позиция указателя
     */
    @AiDoc(description = "Renders the ghost visualization into the overlay render context.")
    @WikiMethod(description = "<key:drag.ghost.method.render>")
    void render(
            @WikiParameter(description = "<key:drag.ghost.param.context>")
            RenderContext context,
            @WikiParameter(description = "<key:drag.ghost.param.session>")
            DragSession session
    );

    /**
     * Ghost, повторяющий живой виджет: его команды воспроизводятся в overlay со сдвигом на
     * перемещение указателя и общей прозрачностью.
     *
     * @param widget  виджет, который нужно повторить (например, заголовок секции)
     * @param opacity прозрачность ghost'а {@code [0, 1]}
     * @return ghost
     */
    @AiDoc(description = "Creates a ghost that clones the cached rendering of a live widget with specified opacity.")
    @WikiMethod(description = "<key:drag.ghost.method.widget>")
    @WikiReturn("<key:drag.ghost.return.ghost>")
    static DragGhost widget(
            @WikiParameter(description = "<key:drag.ghost.param.widget>")
            Widget widget,
            @WikiParameter(description = "<key:drag.ghost.param.opacity>")
            float opacity
    ) {
        Objects.requireNonNull(widget, "widget");
        float alpha = Math.max(0.0f, Math.min(1.0f, opacity));
        Transform offset = new Transform();
        return new DragGhost() {
            private DragSession currentSession;
            private float initialX;
            private float initialY;

            @Override
            public void render(RenderContext context, DragSession session) {
                if (currentSession != session) {
                    currentSession = session;
                    initialX = widget.layoutBounds().x();
                    initialY = widget.layoutBounds().y();
                }
                offset.position().set(
                        session.ghostOffsetX(widget, initialX),
                        session.ghostOffsetY(widget, initialY)
                );
                context.pushOpacity(alpha);
                context.pushTransform(widget.layoutBounds(), offset);
                try {
                    widget.renderCached(context);
                } finally {
                    context.popTransform();
                    context.popOpacity();
                }
            }
        };
    }

    /**
     * Ghost-плашка с текстом payload.preview, следующая за курсором.
     *
     * @return текстовый ghost с полупрозрачным фоном
     */
    @AiDoc(description = "Creates a lightweight text badge ghost displaying the payload preview text.")
    @WikiMethod(description = "<key:drag.ghost.method.text>")
    @WikiReturn("<key:drag.ghost.return.textGhost>")
    static DragGhost text() {
        MutableColor background = MutableColor.rgba(0.12f, 0.14f, 0.18f, 0.88f);
        MutableColor border = MutableColor.rgba(0.30f, 0.60f, 1.0f, 0.95f);
        MutableColor foreground = MutableColor.rgba(0.92f, 0.94f, 0.98f, 1.0f);
        return (context, session) -> {
            String label = session.payload().preview();
            RenderBackend backend = context.backend();
            float textWidth = backend != null ? backend.measureTextWidth(label) : label.length() * 7.0f;
            float width = textWidth + 16.0f;
            float height = 22.0f;
            float x = session.pointerX() + 10.0f;
            float y = session.pointerY() + 10.0f;
            context.roundedRect(x, y, width, height, 4.0f, Paint.fill(background));
            context.roundedRect(x, y, width, height, 4.0f, Paint.stroke(border, 1.0f));
            context.text(label, x + 8.0f, y + 4.0f, textWidth, height - 8.0f, Paint.fill(foreground));
        };
    }
}

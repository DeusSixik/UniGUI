package dev.sixik.unigui.impl.input;

import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Выбирает курсор мыши для виджета под указателем.
 *
 * <p>Правило: сначала собственный {@link Widget#mouseCursorAt} глубочайшего виджета, затем проход вверх по
 * предкам; первый предок с ненулевым {@link Widget#mouseCursorOverrideAt} перекрывает ответ. Так край
 * окна показывает курсор изменения размера, даже если его целиком закрывает дочерний виджет.</p>
 */
public final class CursorResolver {
    private CursorResolver() {
    }

    /**
     * Определяет курсор.
     *
     * @param hit виджет под указателем или {@code null}
     * @param localX X указателя в координатах {@code hit}
     * @param localY Y указателя в координатах {@code hit}
     * @param rootX X указателя в root-координатах
     * @param rootY Y указателя в root-координатах
     * @return курсор, не {@code null}
     */
    public static MouseCursor resolve(Widget hit, float localX, float localY, float rootX, float rootY) {
        if (hit == null) return MouseCursor.DEFAULT;
        for (Widget current = hit; current != null; current = current.parent()) {
            MouseCursor override = current.mouseCursorOverrideAt(rootX, rootY);
            if (override != null) return override;
        }
        MouseCursor own = hit.mouseCursorAt(localX, localY);
        return own == null ? MouseCursor.DEFAULT : own;
    }
}

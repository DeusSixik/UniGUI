package dev.sixik.unigui.api.xml;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Вспомогательные методы исходного документа для editor drag/resize handles поверх layout-атрибутов.
 *
 * <p>Методы не мутируют переданный документ. Они создают копию, читают числовые атрибуты
 * {@code x/y/width/height}, применяют move/resize и возвращают {@link XmlWidgetDocumentResult}
 * с новой версией документа. Если атрибуты содержат неподходящие значения, результат содержит
 * диагностики, а документ остаётся без частично применённой правки.</p>
 */
public final class XmlWidgetLayoutHandles {
    private static final float DEFAULT_MIN_SIZE = 1.0f;

    private XmlWidgetLayoutHandles() {
    }

    /**
     * Сдвигает layout frame выбранного элемента.
     *
     * @param document исходный документ
     * @param path path элемента; {@code null} означает root
     * @param deltaX смещение по X
     * @param deltaY смещение по Y
     * @return результат с копией документа и diagnostics
     */
    public static XmlWidgetDocumentResult move(XmlWidgetDocument document, XmlWidgetNodePath path, float deltaX, float deltaY) {
        return transform(document, path, frame -> frame.move(deltaX, deltaY));
    }

    /**
     * Меняет размер frame с минимальным размером по умолчанию.
     *
     * @param document исходный документ
     * @param path path элемента; {@code null} означает root
     * @param handle активный resize handle; {@code null} означает south-east
     * @param deltaX смещение указателя по X
     * @param deltaY смещение указателя по Y
     * @return результат с копией документа и diagnostics
     */
    public static XmlWidgetDocumentResult resize(XmlWidgetDocument document,
                                                 XmlWidgetNodePath path,
                                                 XmlWidgetLayoutHandle handle,
                                                 float deltaX,
                                                 float deltaY) {
        return resize(document, path, handle, deltaX, deltaY, DEFAULT_MIN_SIZE, DEFAULT_MIN_SIZE);
    }

    /**
     * Меняет размер frame с явно заданным минимальным размером.
     *
     * @param document исходный документ
     * @param path path элемента; {@code null} означает root
     * @param handle активный resize handle; {@code null} означает south-east
     * @param deltaX смещение указателя по X
     * @param deltaY смещение указателя по Y
     * @param minWidth минимальная ширина
     * @param minHeight минимальная высота
     * @return результат с копией документа и diagnostics
     */
    public static XmlWidgetDocumentResult resize(XmlWidgetDocument document,
                                                 XmlWidgetNodePath path,
                                                 XmlWidgetLayoutHandle handle,
                                                 float deltaX,
                                                 float deltaY,
                                                 float minWidth,
                                                 float minHeight) {
        XmlWidgetLayoutHandle normalized = handle == null ? XmlWidgetLayoutHandle.SOUTH_EAST : handle;
        if (normalized.move()) return move(document, path, deltaX, deltaY);
        return transform(document, path, frame -> frame.resize(normalized, deltaX, deltaY, minWidth, minHeight));
    }

    /**
     * Читает numeric layout frame из XML-элемента.
     *
     * @param element source XML element
     * @return frame или empty, если элемент отсутствует либо атрибуты нельзя распарсить
     */
    public static Optional<XmlWidgetLayoutFrame> frame(XmlWidgetElement element) {
        if (element == null) return Optional.empty();
        ArrayList<XmlWidgetDiagnostic> diagnostics = new ArrayList<>();
        Optional<XmlWidgetLayoutFrame> frame = readFrame(element, diagnostics);
        return diagnostics.isEmpty() ? frame : Optional.empty();
    }

    private static XmlWidgetDocumentResult transform(XmlWidgetDocument document,
                                                     XmlWidgetNodePath path,
                                                     FrameTransform transform) {
        if (document == null) throw new IllegalArgumentException("XML layout handle document must not be null");
        if (transform == null) throw new IllegalArgumentException("XML layout handle transform must not be null");

        XmlWidgetDocument copy = document.copy();
        XmlWidgetNodePath normalizedPath = path == null ? XmlWidgetNodePath.root() : path;
        ArrayList<XmlWidgetDiagnostic> diagnostics = new ArrayList<>();
        Optional<XmlWidgetElement> element = normalizedPath.resolveElement(copy);
        if (element.isEmpty()) {
            diagnostics.add(new XmlWidgetDiagnostic("XML layout handle target '" + normalizedPath + "' was not found."));
            return new XmlWidgetDocumentResult(copy, diagnostics);
        }

        Optional<XmlWidgetLayoutFrame> frame = readFrame(element.get(), diagnostics);
        if (frame.isEmpty() || !diagnostics.isEmpty()) return new XmlWidgetDocumentResult(copy, diagnostics);

        writeFrame(element.get(), transform.apply(frame.get()));
        return new XmlWidgetDocumentResult(copy, diagnostics);
    }

    private static Optional<XmlWidgetLayoutFrame> readFrame(XmlWidgetElement element, List<XmlWidgetDiagnostic> diagnostics) {
        Float x = readFloat(element, "x", 0.0f, diagnostics);
        Float y = readFloat(element, "y", 0.0f, diagnostics);
        Float width = readFloat(element, "width", 0.0f, diagnostics);
        Float height = readFloat(element, "height", 0.0f, diagnostics);
        if (x == null || y == null || width == null || height == null) return Optional.empty();
        return Optional.of(new XmlWidgetLayoutFrame(x, y, width, height));
    }

    private static Float readFloat(XmlWidgetElement element,
                                   String attributeName,
                                   float fallback,
                                   List<XmlWidgetDiagnostic> diagnostics) {
        Optional<String> value = element.attribute(attributeName);
        if (value.isEmpty()) return fallback;
        String normalized = value.get().trim();
        if (normalized.endsWith("px")) normalized = normalized.substring(0, normalized.length() - 2).trim();
        try {
            float parsed = Float.parseFloat(normalized);
            if (!Float.isFinite(parsed)) throw new NumberFormatException("non-finite");
            return parsed;
        } catch (NumberFormatException failure) {
            diagnostics.add(new XmlWidgetDiagnostic("XML layout attribute '" + attributeName
                    + "' must be a numeric pixel value for editor handles, got '" + value.get() + "'.",
                    element.line(),
                    element.column()));
            return null;
        }
    }

    private static void writeFrame(XmlWidgetElement element, XmlWidgetLayoutFrame frame) {
        element.setAttribute("x", format(frame.x()));
        element.setAttribute("y", format(frame.y()));
        element.setAttribute("width", format(frame.width()));
        element.setAttribute("height", format(frame.height()));
    }

    private static String format(float value) {
        float rounded = Math.round(value * 1000.0f) / 1000.0f;
        return BigDecimal.valueOf(rounded).stripTrailingZeros().toPlainString();
    }

    @FunctionalInterface
    private interface FrameTransform {
        XmlWidgetLayoutFrame apply(XmlWidgetLayoutFrame frame);
    }
}

package dev.sixik.unigui.api.xml;

/** Рамка редактора исходного документа, связанная с XML layout-атрибутами x/y/width/height. */
public record XmlWidgetLayoutFrame(float x, float y, float width, float height) {
    public XmlWidgetLayoutFrame {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(width) || !Float.isFinite(height)) {
            throw new IllegalArgumentException("XML widget layout frame values must be finite");
        }
        width = Math.max(0.0f, width);
        height = Math.max(0.0f, height);
    }

    public XmlWidgetLayoutFrame move(float deltaX, float deltaY) {
        return new XmlWidgetLayoutFrame(x + deltaX, y + deltaY, width, height);
    }

    public XmlWidgetLayoutFrame resize(XmlWidgetLayoutHandle handle, float deltaX, float deltaY, float minWidth, float minHeight) {
        XmlWidgetLayoutHandle normalized = handle == null ? XmlWidgetLayoutHandle.SOUTH_EAST : handle;
        float nextX = x;
        float nextY = y;
        float nextWidth = width;
        float nextHeight = height;
        float safeMinWidth = Math.max(0.0f, minWidth);
        float safeMinHeight = Math.max(0.0f, minHeight);

        if (normalized.west()) {
            float right = x + width;
            nextX = Math.min(x + deltaX, right - safeMinWidth);
            nextWidth = right - nextX;
        } else if (normalized.east()) {
            nextWidth = Math.max(safeMinWidth, width + deltaX);
        }

        if (normalized.north()) {
            float bottom = y + height;
            nextY = Math.min(y + deltaY, bottom - safeMinHeight);
            nextHeight = bottom - nextY;
        } else if (normalized.south()) {
            nextHeight = Math.max(safeMinHeight, height + deltaY);
        }

        return new XmlWidgetLayoutFrame(nextX, nextY, nextWidth, nextHeight);
    }
}

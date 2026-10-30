package dev.sixik.unigui.api.input;

/** Standard mouse cursor shapes supported by UI backends. */
public enum MouseCursor {
    DEFAULT,
    POINTER,
    TEXT,
    CROSSHAIR,
    RESIZE_HORIZONTAL,
    RESIZE_VERTICAL,
    /** Диагональное изменение размера «северо-запад — юго-восток» (углы верх-лево и низ-право). */
    RESIZE_NWSE,
    /** Диагональное изменение размера «северо-восток — юго-запад» (углы верх-право и низ-лево). */
    RESIZE_NESW
}

package dev.sixik.unigui.api.xml;

/**
 * Категории ассетов, доступные XML picker-ам редактора.
 *
 * <p>Категория определяет, в какие XML-атрибуты asset обычно подставляется. Например,
 * {@link #TEXTURE} используется для {@code texture/backgroundTexture}, а {@link #FONT} — для будущих text styles.</p>
 */
public enum XmlWidgetAssetKind {
    /** Текстура или изображение, выбираемое по id. */
    TEXTURE,
    /** Font resource, выбираемый по id. */
    FONT,
    /** Shader/material resource, выбираемый по id. */
    SHADER
}

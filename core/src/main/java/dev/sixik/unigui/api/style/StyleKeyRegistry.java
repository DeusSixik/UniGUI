package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.SliceBorders;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureGroup;
import dev.sixik.unigui.api.render.TextureLayer;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.widget.visual.BackgroundKind;
import dev.sixik.unigui.api.text.TextTransform;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry style-свойств, которые можно парсить, сериализовать и редактировать как данные.
 *
 * <p>Сам {@link StyleKey} знает только id и Java-тип. Registry добавляет metadata:
 * отображаемое имя, категорию, значение по умолчанию и {@link StyleValueCodec}. XML loader
 * и editor inspector используют registry, чтобы не хардкодить список свойств в UI.</p>
 */
public final class StyleKeyRegistry {
    private final Map<String, StylePropertyDescriptor<?>> descriptors = new LinkedHashMap<>();

    /**
     * Создаёт пустой registry.
     *
     * @return новый registry без зарегистрированных свойств
     */
    public static StyleKeyRegistry empty() {
        return new StyleKeyRegistry();
    }

    /**
     * Создаёт registry стандартных UniGUI style-свойств.
     *
     * @return registry с базовыми цветами, border, radius и texture fit свойствами
     */
    public static StyleKeyRegistry builtIns() {
        StyleValueCodec<ColorView> color = StyleValueCodec.color();
        StyleValueCodec<Float> number = StyleValueCodec.floatingPoint();
        StyleValueCodec<Integer> integer = StyleValueCodec.of(value -> {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Expected integer value.");
            }
            return Integer.parseInt(value.trim());
        }, String::valueOf);
        StyleValueCodec<ImageFit> imageFit = StyleValueCodec.enumCodec(ImageFit.class);
        StyleValueCodec<Boolean> bool = StyleValueCodec.of(value -> {
            if ("true".equalsIgnoreCase(value)) return true;
            if ("false".equalsIgnoreCase(value)) return false;
            throw new IllegalArgumentException("Expected true or false.");
        }, value -> Boolean.toString(value));
        StyleValueCodec<SliceBorders> sliceBorders = StyleValueCodec.of(
                dev.sixik.unigui.api.xml.XmlValueParsers.SLICE_BORDERS::parse,
                value -> value.left() + " " + value.top() + " " + value.right() + " " + value.bottom());
        StyleValueCodec<TextureHandle> texture = StyleValueCodec.of(value -> {
            String[] parts = value.trim().split("\\s+");
            if (parts.length != 1 && parts.length != 3) {
                throw new IllegalArgumentException("Expected texture id and optional width/height.");
            }
            int width = parts.length == 3 ? Integer.parseInt(parts[1]) : 16;
            int height = parts.length == 3 ? Integer.parseInt(parts[2]) : 16;
            return new SimpleTextureHandle(parts[0], width, height);
        }, value -> value.id() + " " + value.width() + " " + value.height());
        StyleValueCodec<TextureGroup> textureGroup = StyleValueCodec.of(
                StyleKeyRegistry::parseTextureGroup,
                StyleKeyRegistry::formatTextureGroup);
        StyleValueCodec<AnimationEasing> easing = StyleValueCodec.enumCodec(AnimationEasing.class);
        StyleValueCodec<BackgroundKind> backgroundKind = StyleValueCodec.enumCodec(BackgroundKind.class);
        StyleValueCodec<VirtualListViewMode> virtualListViewMode = StyleValueCodec.enumCodec(VirtualListViewMode.class);
        StyleValueCodec<EdgeInsets> edgeInsets = StyleValueCodec.of(
                dev.sixik.unigui.api.xml.XmlValueParsers.INSETS::parse,
                value -> value.top() + " " + value.right() + " " + value.bottom() + " " + value.left());
        StyleValueCodec<TreeRowStyle> treeRowStyle = StyleValueCodec.enumCodec(TreeRowStyle.class);
        StyleValueCodec<TreeDisclosureStyle> treeDisclosureStyle = StyleValueCodec.enumCodec(TreeDisclosureStyle.class);
        StyleValueCodec<ShaderHandle> shader = StyleValueCodec.of(
                value -> value == null || value.isBlank() ? null : ShaderHandle.resource(value.trim()),
                value -> value == null ? "" : value.id());
        return empty()
                .register(new StylePropertyDescriptor<>(StyleKeys.BACKGROUND_KIND, "Background kind", "Background", null, backgroundKind, "Background source: none, color, texture or shader."))
                .register(new StylePropertyDescriptor<>(StyleKeys.BACKGROUND_COLOR, "Background color", "Background", null, color, "Main fill color."))
                .register(new StylePropertyDescriptor<>(StyleKeys.BACKGROUND_VISIBLE, "Background visible", "Background", false, bool, "Whether the surface background is rendered."))
                .register(new StylePropertyDescriptor<>(StyleKeys.BACKGROUND_SHADER, "Background shader", "Background", null, shader, "Shader resource id used when background kind is shader."))
                .register(new StylePropertyDescriptor<>(StyleKeys.BACKGROUND_TEXTURE, "Background texture", "Background", null, texture, "Texture id, optionally followed by source width and height."))
                .register(new StylePropertyDescriptor<>(StyleKeys.BACKGROUND_TEXTURE_GROUP, "Background texture group", "Background", null, textureGroup, "Ordered texture layers: id,width,height,fit,u,v,uw,vh,left,top,right,bottom,tint,slice-left,slice-top,slice-right,slice-bottom; separate layers with semicolons."))
                .register(new StylePropertyDescriptor<>(StyleKeys.BACKGROUND_TEXTURE_TINT, "Texture tint", "Background", null, color, "Tint applied over the background texture."))
                .register(new StylePropertyDescriptor<>(StyleKeys.BACKGROUND_TEXTURE_FIT, "Texture fit", "Background", ImageFit.STRETCH, imageFit, "Texture placement mode."))
                .register(new StylePropertyDescriptor<>(StyleKeys.SLICE_BORDERS, "Slice borders", "Background", SliceBorders.ZERO, sliceBorders, "Nine-slice borders in texture pixels: left top right bottom."))
                .register(new StylePropertyDescriptor<>(StyleKeys.BORDER_COLOR, "Border color", "Border", null, color, "Stroke color."))
                .register(new StylePropertyDescriptor<>(StyleKeys.BORDER_VISIBLE, "Border visible", "Border", false, bool, "Whether the surface border is rendered."))
                .register(new StylePropertyDescriptor<>(StyleKeys.BORDER_WIDTH, "Border width", "Border", 0.0f, number, "Stroke width in logical pixels."))
                .register(new StylePropertyDescriptor<>(StyleKeys.RADIUS, "Radius", "Border", 0.0f, number, "Corner radius in logical pixels."))
                .register(new StylePropertyDescriptor<>(StyleKeys.SHAPE_STROKE, "Shape stroke", "Shape", null, bool, "Whether a Shape uses its border as a stroke."))
                .register(new StylePropertyDescriptor<>(StyleKeys.SHAPE_COLOR, "Shape color", "Shape", null, color, "Shape fill color."))
                .register(new StylePropertyDescriptor<>(StyleKeys.SHAPE_TYPE, "Shape type", "Shape", null, StyleValueCodec.string(), "Shape primitive: rect, rounded-rect, circle or line."))
                .register(new StylePropertyDescriptor<>(StyleKeys.PATH_COLOR, "Path color", "Path", null, color, "Vector path fill or stroke color."))
                .register(new StylePropertyDescriptor<>(StyleKeys.PATH_STROKE, "Path stroke", "Path", null, bool, "Whether a vector path is rendered as a stroke."))
                .register(new StylePropertyDescriptor<>(StyleKeys.PATH_STROKE_WIDTH, "Path stroke width", "Path", null, number, "Vector path stroke width in logical pixels."))
                .register(new StylePropertyDescriptor<>(StyleKeys.VIRTUAL_LIST_VIEW_MODE, "View mode", "VirtualListView", VirtualListViewMode.LIST, virtualListViewMode, "Whether virtual items are arranged as a list or grid."))
                .register(new StylePropertyDescriptor<>(StyleKeys.VIRTUAL_LIST_ITEM_HEIGHT, "Item height", "VirtualListView", 18.0f, number, "Height of one item in list mode."))
                .register(new StylePropertyDescriptor<>(StyleKeys.VIRTUAL_LIST_GRID_CELL_WIDTH, "Grid cell width", "VirtualListView", 96.0f, number, "Preferred grid cell width used by responsive auto-fit."))
                .register(new StylePropertyDescriptor<>(StyleKeys.VIRTUAL_LIST_GRID_CELL_HEIGHT, "Grid cell height", "VirtualListView", 96.0f, number, "Fixed grid cell height."))
                .register(new StylePropertyDescriptor<>(StyleKeys.VIRTUAL_LIST_GRID_COLUMNS, "Grid columns", "VirtualListView", 0, integer, "Fixed grid column count; zero enables responsive auto-fit."))
                .register(new StylePropertyDescriptor<>(StyleKeys.VIRTUAL_LIST_ROW_GAP, "Row gap", "VirtualListView", 0.0f, number, "Vertical gap between virtual rows."))
                .register(new StylePropertyDescriptor<>(StyleKeys.VIRTUAL_LIST_COLUMN_GAP, "Column gap", "VirtualListView", 0.0f, number, "Horizontal gap between grid cells."))
                .register(new StylePropertyDescriptor<>(StyleKeys.VIRTUAL_LIST_PADDING, "Padding", "VirtualListView", EdgeInsets.ZERO, edgeInsets, "Viewport padding in CSS order: top right bottom left."))
                .register(new StylePropertyDescriptor<>(StyleKeys.VIRTUAL_LIST_OVERSCAN, "Overscan", "VirtualListView", 1, integer, "Extra virtual rows realized before and after the viewport."))
                .register(new StylePropertyDescriptor<>(StyleKeys.VIRTUAL_LIST_OFFSCREEN_CACHE_SIZE, "Offscreen cache size", "VirtualListView", 0, integer, "Maximum number of offscreen item widgets retained by index."))
                .register(new StylePropertyDescriptor<>(StyleKeys.VIRTUAL_LIST_SCROLL_STEP, "Scroll step", "VirtualListView", 16.0f, number, "UI pixels scrolled per wheel delta unit."))
                .register(new StylePropertyDescriptor<>(StyleKeys.VIRTUAL_LIST_CONSUME_WHEEL_AT_BOUNDS, "Consume wheel at bounds", "VirtualListView", true, bool, "Whether the view retains wheel ownership at scroll bounds."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TREE_ROW_HEIGHT, "Tree row height", "TreeView", 28.0f, number, "Height of each visible tree row in logical pixels."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TREE_ROW_STYLE, "Tree row style", "TreeView", TreeRowStyle.PILL, treeRowStyle, "Whether row highlight is inset or full-width."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TREE_DISCLOSURE_STYLE, "Tree disclosure style", "TreeView", TreeDisclosureStyle.CHEVRON, treeDisclosureStyle, "Shape of the branch expansion marker."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TREE_ROW_RADIUS, "Tree row radius", "TreeView", 4.0f, number, "Corner radius of inset row highlights."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TREE_ROW_INSET_X, "Tree row horizontal inset", "TreeView", 4.0f, number, "Horizontal inset of row highlights."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TREE_ROW_INSET_Y, "Tree row vertical inset", "TreeView", 1.0f, number, "Vertical inset of row highlights."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TREE_INDENT_WIDTH, "Tree indent width", "TreeView", 18.0f, number, "Indent added for each tree depth."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TREE_FOLDER_FILL_ENABLED, "Tree filled folder icons", "TreeView", false, bool, "Whether built-in folder icons are filled."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TREE_HOVER_COLOR, "Tree hover color", "TreeView", null, color, "Background color of a hovered tree row."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TREE_SELECTED_COLOR, "Tree selected color", "TreeView", null, color, "Background color of the selected tree row."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TREE_TEXT_COLOR, "Tree text color", "TreeView", null, color, "Text color of an ordinary tree row."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TREE_SELECTED_TEXT_COLOR, "Tree selected text color", "TreeView", null, color, "Text color of the selected tree row."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TREE_DISABLED_TEXT_COLOR, "Tree disabled text color", "TreeView", null, color, "Text color of a non-selectable tree row."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TREE_DISCLOSURE_COLOR, "Tree disclosure color", "TreeView", null, color, "Color of branch expansion markers."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TREE_ICON_COLOR, "Tree icon color", "TreeView", null, color, "Color of built-in folder and file icons."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TEXT_COLOR, "Text color", "Text", null, color, "Primary text color."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TEXT_SIZE, "Text size", "Text", null, number, "The text size in logical pixels."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TEXT_TRACKING, "Character spacing", "Text", null, number, "Additional spacing between characters in logical pixels."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TEXT_TRANSFORM, "Text conversion", "Text", null,
                        StyleValueCodec.enumCodec(TextTransform.class), "Text transformation before measurement and rendering."))
                .register(new StylePropertyDescriptor<>(StyleKeys.CHECKBOX_BACKGROUND_COLOR,
                        "The color of the flag indicator", "Controls", null, color,
                        "The fill color of the square Checkbox indicator for the state."))
                .register(new StylePropertyDescriptor<>(StyleKeys.PLACEHOLDER_COLOR, "Placeholder color", "Text", null, color, "Placeholder text color."))
                .register(new StylePropertyDescriptor<>(StyleKeys.CARET_COLOR, "Caret color", "Text", null, color, "Text editor caret color."))
                .register(new StylePropertyDescriptor<>(StyleKeys.FOCUS_RING_COLOR, "Focus ring color", "Focus", null, color, "Outer focus ring color for text inputs."))
                .register(new StylePropertyDescriptor<>(StyleKeys.ACCENT_COLOR, "Accent color", "Accent", null, color, "Accent color for active controls."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TRACK_COLOR, "Track color", "Controls", null, color, "Track color for slider/progress controls."))
                .register(new StylePropertyDescriptor<>(StyleKeys.THUMB_COLOR, "Thumb color", "Controls", null, color, "Thumb color for slider/scroll controls."))
                .register(new StylePropertyDescriptor<>(StyleKeys.THUMB_RADIUS, "Slider radius", "Controls", null, number,
                        "The radius of the slider’s rounding; half of its size forms a circle."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TRACK_THICKNESS, "Track thickness", "Controls", null, number,
                        "Track thickness in logical pixels; zero uses the widget's automatic size."))
                .register(new StylePropertyDescriptor<>(StyleKeys.THUMB_THICKNESS, "Thumb thickness", "Controls", null, number,
                        "Slider thumb width or ScrollBar thumb thickness in logical pixels."))
                .register(new StylePropertyDescriptor<>(StyleKeys.HOLD_COLOR, "Hold color", "Controls", null, color,
                        "HoldButton progress fill color."))
                .register(new StylePropertyDescriptor<>(StyleKeys.RADIO_OUTER_SIZE, "RadioButton outer size", "Controls", null, number,
                        "RadioButton indicator outer size in logical pixels."))
                .register(new StylePropertyDescriptor<>(StyleKeys.RADIO_INNER_SIZE, "RadioButton inner size", "Controls", null, number,
                        "RadioButton selected mark size in logical pixels."))
                .register(new StylePropertyDescriptor<>(StyleKeys.RADIO_TEXT_GAP, "RadioButton text gap", "Controls", null, number,
                        "Gap between the RadioButton indicator and its label in logical pixels."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TRANSITION_DURATION, "Transition duration", "Animation", 0.0f, number, "Automatic style transition duration in seconds."))
                .register(new StylePropertyDescriptor<>(StyleKeys.TRANSITION_EASING, "Transition easing", "Animation", AnimationEasing.EASE_OUT, easing, "Automatic style transition easing curve."));
    }

    /**
     * Регистрирует descriptor свойства.
     *
     * @param descriptor metadata свойства
     * @param <T>        Java-тип значения свойства
     * @return этот registry для fluent-настройки
     */
    public <T> StyleKeyRegistry register(StylePropertyDescriptor<T> descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        descriptors.put(descriptor.key().id(), descriptor);
        return this;
    }

    /**
     * Ищет descriptor по строковому id свойства.
     *
     * @param propertyId id свойства из XML/StylePack
     * @return descriptor или {@link Optional#empty()}
     */
    public Optional<StylePropertyDescriptor<?>> descriptor(String propertyId) {
        return Optional.ofNullable(descriptors.get(normalize(propertyId)));
    }

    /**
     * Ищет descriptor по типизированному ключу и проверяет совместимость типов.
     *
     * @param key ключ свойства
     * @param <T> Java-тип значения свойства
     * @return типизированный descriptor или {@link Optional#empty()}
     */
    public <T> Optional<StylePropertyDescriptor<T>> descriptor(StyleKey<T> key) {
        Objects.requireNonNull(key, "key");
        StylePropertyDescriptor<?> descriptor = descriptors.get(key.id());
        if (descriptor == null || !descriptor.key().type().equals(key.type())) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        StylePropertyDescriptor<T> typed = (StylePropertyDescriptor<T>) descriptor;
        return Optional.of(typed);
    }

    /**
     * Возвращает зарегистрированные descriptors.
     *
     * @return read-only view descriptors в порядке регистрации
     */
    public Collection<StylePropertyDescriptor<?>> descriptors() {
        return Collections.unmodifiableCollection(descriptors.values());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static TextureGroup parseTextureGroup(String value) {
        TextureGroup.Builder group = TextureGroup.builder();
        if (value == null || value.isBlank()) return group.build();
        for (String encodedLayer : value.split(";")) {
            String[] parts = encodedLayer.trim().split(",", -1);
            if (parts.length != 17) throw new IllegalArgumentException("Expected 17 comma-separated texture layer fields.");
            TextureHandle texture = new SimpleTextureHandle(parts[0].trim(), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            TextureLayer layer = new TextureLayer(texture)
                    .fit(ImageFit.valueOf(parts[3].trim().toUpperCase(java.util.Locale.ROOT)))
                    .source(Float.parseFloat(parts[4]), Float.parseFloat(parts[5]), Float.parseFloat(parts[6]), Float.parseFloat(parts[7]))
                    .insets(Float.parseFloat(parts[8]), Float.parseFloat(parts[9]), Float.parseFloat(parts[10]), Float.parseFloat(parts[11]))
                    .tint(MutableColor.fromHex(parts[12]))
                    .sliceBorders(new SliceBorders(Float.parseFloat(parts[13]), Float.parseFloat(parts[14]), Float.parseFloat(parts[15]), Float.parseFloat(parts[16])));
            group.add(layer);
        }
        return group.build();
    }

    private static String formatTextureGroup(TextureGroup group) {
        StringBuilder encoded = new StringBuilder();
        for (TextureLayer layer : group.layers()) {
            if (encoded.length() > 0) encoded.append(';');
            TextureHandle texture = layer.texture();
            if (texture == null) throw new IllegalArgumentException("Texture groups in StylePack cannot contain empty layers.");
            SliceBorders slice = layer.sliceBorders();
            encoded.append(texture.id()).append(',').append(texture.width()).append(',').append(texture.height())
                    .append(',').append(layer.fit()).append(',')
                    .append(layer.source().x()).append(',').append(layer.source().y()).append(',')
                    .append(layer.source().width()).append(',').append(layer.source().height()).append(',')
                    .append(layer.insetLeft()).append(',').append(layer.insetTop()).append(',')
                    .append(layer.insetRight()).append(',').append(layer.insetBottom()).append(',')
                    .append(MutableColor.rgba(layer.tint().r(), layer.tint().g(), layer.tint().b(), layer.tint().a()).toHexString(true)).append(',')
                    .append(slice.left()).append(',').append(slice.top()).append(',').append(slice.right()).append(',').append(slice.bottom());
        }
        return encoded.toString();
    }
}

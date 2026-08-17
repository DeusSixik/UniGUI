package dev.sixik.unigui.api.xml;

/**
 * Read-only metadata for one XML attribute in editor palettes and inspectors.
 */
public record XmlAttributeDescriptor(
        String name,
        String displayName,
        String category,
        String defaultValue,
        String description,
        XmlAttributeValueType valueType) {
    public XmlAttributeDescriptor(String name,
                                  String displayName,
                                  String category,
                                  String defaultValue,
                                  String description) {
        this(name, displayName, category, defaultValue, description, null);
    }

    public XmlAttributeDescriptor {
        name = normalizeRequired(name, "name");
        displayName = normalize(displayName, displayNameFor(name));
        category = normalize(category, categoryFor(name));
        defaultValue = normalize(defaultValue, "");
        description = normalize(description, descriptionFor(name));
        valueType = valueType == null ? valueTypeFor(name, defaultValue) : valueType;
    }

    public static XmlAttributeDescriptor of(String name) {
        return new XmlAttributeDescriptor(name, null, null, null, null, null);
    }

    public XmlAttributeDescriptor displayName(String displayName) {
        return new XmlAttributeDescriptor(name, displayName, category, defaultValue, description, valueType);
    }

    public XmlAttributeDescriptor category(String category) {
        return new XmlAttributeDescriptor(name, displayName, category, defaultValue, description, valueType);
    }

    public XmlAttributeDescriptor defaultValue(String defaultValue) {
        XmlAttributeValueType nextValueType = valueType == valueTypeFor(name, this.defaultValue) ? null : valueType;
        return new XmlAttributeDescriptor(name, displayName, category, defaultValue, description, nextValueType);
    }

    public XmlAttributeDescriptor description(String description) {
        return new XmlAttributeDescriptor(name, displayName, category, defaultValue, description, valueType);
    }

    public XmlAttributeDescriptor valueType(XmlAttributeValueType valueType) {
        return new XmlAttributeDescriptor(name, displayName, category, defaultValue, description, valueType);
    }

    private static String normalizeRequired(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("XML attribute descriptor " + field + " must not be blank");
        }
        return normalized;
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String categoryFor(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("texture") || lower.contains("background") || lower.contains("border")
                || lower.contains("color") || lower.equals("radius") || lower.equals("tint")) {
            return "Appearance";
        }
        if (lower.contains("width") || lower.contains("height") || lower.contains("padding")
                || lower.contains("margin") || lower.contains("flex") || lower.contains("align")
                || lower.contains("justify") || lower.contains("overflow") || lower.equals("x")
                || lower.equals("y") || lower.equals("left") || lower.equals("top")
                || lower.equals("right") || lower.equals("bottom") || lower.contains("gap")
                || lower.equals("position")) {
            return "Layout";
        }
        if (lower.contains("text") || lower.equals("wrap") || lower.contains("marquee")) {
            return "Content";
        }
        if (lower.equals("value") || lower.equals("min") || lower.equals("max") || lower.equals("step")
                || lower.contains("checked") || lower.equals("state") || lower.contains("enabled")) {
            return "Behavior";
        }
        return "Common";
    }

    private static XmlAttributeValueType valueTypeFor(String name, String defaultValue) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        String candidate = defaultValue == null ? "" : defaultValue.trim();
        if (isBooleanLiteral(candidate) || lower.startsWith("is") || lower.startsWith("has")
                || lower.startsWith("allow") || lower.startsWith("show") || lower.endsWith("enabled")
                || lower.endsWith("visible") || lower.endsWith("checked") || lower.equals("open")
                || lower.equals("opened")) {
            return XmlAttributeValueType.BOOLEAN;
        }
        if (lower.contains("color") || lower.contains("tint") || looksLikeColor(candidate)) {
            return XmlAttributeValueType.COLOR;
        }
        if (lower.contains("padding") || lower.contains("margin") || lower.contains("inset")) {
            return XmlAttributeValueType.INSETS;
        }
        if (!lower.equals("id") && (lower.contains("texture") || lower.contains("resource")
                || lower.equals("item") || lower.equals("block") || lower.equals("entity")
                || lower.equals("entitytype") || lower.endsWith("id"))) {
            return XmlAttributeValueType.RESOURCE_ID;
        }
        if (lower.contains("binding") || lower.contains("action") || lower.contains("command") || lower.startsWith("on")) {
            return XmlAttributeValueType.BINDING_OR_ACTION;
        }
        if (isEnumName(lower)) {
            return XmlAttributeValueType.ENUM;
        }
        if (isSizeName(lower)) {
            return XmlAttributeValueType.SIZE_VALUE;
        }
        if (isNumericLiteral(candidate) || isNumericName(lower)) {
            return XmlAttributeValueType.NUMBER;
        }
        return XmlAttributeValueType.STRING;
    }

    private static String displayNameFor(String name) {
        StringBuilder builder = new StringBuilder();
        char previous = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(previous)) {
                builder.append(' ');
            }
            builder.append(i == 0 ? Character.toUpperCase(c) : c);
            previous = c;
        }
        return builder.toString().replace('-', ' ').replace('_', ' ');
    }

    private static String descriptionFor(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        return switch (lower) {
            case "id" -> "Runtime/debug/editor identifier for code-behind lookup.";
            case "class", "styleclass" -> "Optional style class name preserved for editor/theme integration.";
            case "enabled" -> "Whether the widget can receive user interaction.";
            case "visible" -> "Whether the widget is visible without collapsing layout space.";
            case "visibility" -> "Visibility mode: visible, hidden or collapsed.";
            case "opacity" -> "Widget opacity clamped between 0 and 1.";
            case "rotation" -> "Rotation in degrees applied to the widget transform.";
            case "x" -> "Local transform offset on the X axis, in pixels.";
            case "y" -> "Local transform offset on the Y axis, in pixels.";
            case "scale" -> "Uniform local transform scale applied on both axes.";
            case "scalex" -> "Local transform scale on the X axis.";
            case "scaley" -> "Local transform scale on the Y axis.";
            case "width" -> "Preferred layout width; accepts px, percent or auto values.";
            case "height" -> "Preferred layout height; accepts px, percent or auto values.";
            case "minwidth" -> "Minimum layout width constraint.";
            case "minheight" -> "Minimum layout height constraint.";
            case "maxwidth" -> "Maximum layout width constraint; auto means unlimited.";
            case "maxheight" -> "Maximum layout height constraint; auto means unlimited.";
            case "padding" -> "Inner content padding; accepts one, two, three or four inset values.";
            case "margin" -> "Outer layout margin; accepts one, two, three or four inset values.";
            case "flexgrow" -> "Flex grow weight inside flex-capable parent layouts.";
            case "flexshrink" -> "Flex shrink weight inside flex-capable parent layouts.";
            case "flexdirection" -> "Primary child layout direction for flex-capable widgets.";
            case "flexwrap" -> "Whether flex children wrap onto additional rows or columns.";
            case "rowgap" -> "Spacing between layout rows.";
            case "columngap" -> "Spacing between layout columns.";
            case "align" -> "Legacy shorthand that applies the same alignment to both axes.";
            case "alignitems" -> "Cross-axis alignment applied to child widgets.";
            case "alignself" -> "Per-widget alignment override inside the parent layout.";
            case "justifycontent" -> "Main-axis distribution for children in flex-capable layouts.";
            case "overflow" -> "Overflow mode applied to both axes.";
            case "overflowx" -> "Horizontal overflow mode.";
            case "overflowy" -> "Vertical overflow mode.";
            case "position" -> "Layout positioning mode: relative or absolute.";
            case "left" -> "Absolute-position left inset; used when position is absolute.";
            case "top" -> "Absolute-position top inset; used when position is absolute.";
            case "right" -> "Absolute-position right inset; used when position is absolute.";
            case "bottom" -> "Absolute-position bottom inset; used when position is absolute.";
            case "text" -> "Text content shown by the widget.";
            case "color", "textcolor", "trackcolor", "fillcolor", "knobcolor",
                    "checkedbackground", "uncheckedbackground", "scrollbartrackcolor", "scrollbarthumbcolor" ->
                    "Color value for this visual state or element.";
            case "backgroundtexture", "texture" -> "Texture resource identifier used by the widget.";
            case "onclick", "command" -> "Command or action id invoked by this widget.";
            case "checked" -> "Initial checked state.";
            case "state" -> "Initial state value.";
            case "value" -> "Initial value.";
            case "min" -> "Minimum allowed value.";
            case "max" -> "Maximum allowed value.";
            case "step" -> "Increment step used by value editing.";
            case "spacing" -> "Spacing between child widgets.";
            case "orientation" -> "Widget orientation.";
            case "preferredwidth" -> "Preferred pixel width for layout.";
            case "preferredheight" -> "Preferred pixel height for layout.";
            case "indeterminate" -> "Whether the progress indicator starts in indeterminate mode.";
            case "indeterminatespeed" -> "Animation speed for indeterminate progress.";
            case "contentwidth" -> "Scrollable content width override.";
            case "contentheight" -> "Scrollable content height override.";
            case "scrollstep" -> "Mouse-wheel scroll distance per step.";
            case "scrollbargap" -> "Gap between content and scrollbar.";
            case "scrollingenabled" -> "Whether user scrolling is enabled.";
            case "consumewheelatscrollbounds" -> "Whether wheel events are consumed at scroll bounds.";
            case "interactiontransitions" -> "Whether built-in hover/press visual transitions are enabled.";
            case "textpaddingx" -> "Horizontal padding around button text.";
            case "textpaddingy" -> "Vertical padding around button text.";
            case "tristate" -> "Whether the checkbox supports an indeterminate state.";
            case "boxsize" -> "Checkbox box size in pixels.";
            case "checksize" -> "Checkbox check mark size in pixels.";
            case "textgap" -> "Gap between the control glyph and text.";
            case "labelleft" -> "Whether the label is placed before the control glyph.";
            case "icon" -> "Icon identifier shown by the widget.";
            case "label" -> "Secondary label shown by the widget.";
            case "tooltip" -> "Tooltip text shown for the widget.";
            case "displaymode" -> "Display mode for icon/text presentation.";
            case "linespacing" -> "Spacing between wrapped layout lines.";
            case "thickness" -> "Rendered line or separator thickness.";
            default -> "Editor-facing XML attribute '" + name + "'.";
        };
    }

    private static boolean isEnumName(String name) {
        return name.endsWith("mode") || name.endsWith("state") || name.endsWith("alignment")
                || name.endsWith("orientation") || name.endsWith("direction") || name.equals("selectionmode")
                || name.equals("visibility") || name.equals("overflow") || name.equals("overflowx")
                || name.equals("overflowy") || name.equals("position") || name.equals("flexdirection")
                || name.equals("flexwrap") || name.equals("align") || name.equals("alignitems")
                || name.equals("alignself") || name.equals("justifycontent") || name.equals("dockarea")
                || name.equals("targetoptions") || name.equals("type") || name.equals("placement");
    }

    private static boolean isSizeName(String name) {
        return name.equals("width") || name.equals("height") || name.equals("minwidth") || name.equals("minheight")
                || name.equals("maxwidth") || name.equals("maxheight") || name.equals("left") || name.equals("top")
                || name.equals("right") || name.equals("bottom") || name.endsWith("width") || name.endsWith("height")
                || name.endsWith("size");
    }

    private static boolean isNumericName(String name) {
        return name.equals("x") || name.equals("y") || name.endsWith("index") || name.endsWith("count")
                || name.contains("gap") || name.contains("radius") || name.contains("opacity")
                || name.contains("speed") || name.contains("step") || name.contains("seconds")
                || name.contains("duration") || name.contains("lines") || name.contains("phase");
    }

    private static boolean isBooleanLiteral(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private static boolean isNumericLiteral(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean looksLikeColor(String value) {
        if (value == null) return false;
        String normalized = value.trim();
        return normalized.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")
                || normalized.matches("#[0-9a-fA-F]{8}");
    }
}

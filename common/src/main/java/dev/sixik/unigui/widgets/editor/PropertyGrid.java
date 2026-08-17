package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.api.xml.XmlPropertyChildDescriptor;
import dev.sixik.unigui.api.xml.XmlWidgetElement;
import dev.sixik.unigui.api.xml.XmlWidgetInspector;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.xml.XmlWidgetRegistry;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@XmlWidgetName("PropertyGrid")
public class PropertyGrid extends LinearBox {
    private static final float DEFAULT_LABEL_WIDTH = 112.0f;

    private final VBox content = new VBox();
    private final ScrollView scrollView = new ScrollView(content);
    private final List<PropertyFieldRow> rows = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();
    private final List<XmlPropertyChildDescriptor> propertyChildSlots = new ArrayList<>();

    private XmlWidgetRegistry registry = XmlWidgetRegistry.builtIns();
    private XmlWidgetInspector.Inspection inspection;
    private boolean showUnsetAttributes = true;
    private boolean showPropertyChildren = true;
    private float labelWidth = DEFAULT_LABEL_WIDTH;
    private Consumer<AttributeChange> attributeChanged = change -> {
    };

    public PropertyGrid() {
        super(Orientation.VERTICAL);
        spacing(3.0f);
        layout(style -> style.overflow(Overflow.HIDDEN));
        content.spacing(2.0f);
        content.layout(style -> style.widthPercent(100.0f).flexGrow(0.0f).flexShrink(0.0f));
        scrollView.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));
        addChild(scrollView);
        applyQueuedMutations();
        rebuild();
    }

    public XmlWidgetRegistry registry() {
        return registry;
    }

    public PropertyGrid registry(XmlWidgetRegistry registry) {
        this.registry = registry == null ? XmlWidgetRegistry.builtIns() : registry;
        if (inspection != null) {
            inspection = XmlWidgetInspector.inspect(inspection.element(), this.registry);
        }
        rebuild();
        return this;
    }

    public XmlWidgetInspector.Inspection inspection() {
        return inspection;
    }

    public PropertyGrid inspection(XmlWidgetInspector.Inspection inspection) {
        this.inspection = inspection;
        rebuild();
        return this;
    }

    public PropertyGrid inspect(XmlWidgetElement element) {
        return inspection(element == null ? null : XmlWidgetInspector.inspect(element, registry));
    }

    public boolean showUnsetAttributes() {
        return showUnsetAttributes;
    }

    @XmlAttribute(value = "showUnsetAttributes", category = "Behavior", defaultValue = "true", description = "Whether descriptor attributes missing from the selected XML element are shown with defaults.")
    public PropertyGrid showUnsetAttributes(boolean showUnsetAttributes) {
        if (this.showUnsetAttributes == showUnsetAttributes) return this;
        this.showUnsetAttributes = showUnsetAttributes;
        rebuild();
        return this;
    }

    public boolean showPropertyChildren() {
        return showPropertyChildren;
    }

    @XmlAttribute(value = "showPropertyChildren", category = "Behavior", defaultValue = "true", description = "Whether property-child slots are shown below attribute rows.")
    public PropertyGrid showPropertyChildren(boolean showPropertyChildren) {
        if (this.showPropertyChildren == showPropertyChildren) return this;
        this.showPropertyChildren = showPropertyChildren;
        rebuild();
        return this;
    }

    public float labelWidth() {
        return labelWidth;
    }

    @XmlAttribute(value = "labelWidth", category = "Layout", defaultValue = "112", description = "Width of property labels in UI pixels.")
    public PropertyGrid labelWidth(float labelWidth) {
        float normalized = Float.isFinite(labelWidth) && labelWidth > 0.0f ? labelWidth : DEFAULT_LABEL_WIDTH;
        if (this.labelWidth == normalized) return this;
        this.labelWidth = normalized;
        for (PropertyFieldRow row : rows) {
            row.labelWidth(normalized);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public List<PropertyFieldRow> rows() {
        return List.copyOf(rows);
    }

    public int rowCount() {
        return rows.size();
    }

    public Optional<PropertyFieldRow> row(String attributeName) {
        String normalized = normalizeName(attributeName);
        return rows.stream()
                .filter(row -> row.name().equals(normalized) || row.descriptor().name().equals(normalized))
                .findFirst();
    }

    public List<Category> categories() {
        return List.copyOf(categories);
    }

    public List<XmlPropertyChildDescriptor> propertyChildSlots() {
        return List.copyOf(propertyChildSlots);
    }

    public int errorCount() {
        int count = 0;
        for (PropertyFieldRow row : rows) {
            if (row.validationState() == PropertyFieldRow.ValidationState.ERROR) count++;
        }
        return count;
    }

    public EventSubscription onAttributeChanged(Consumer<AttributeChange> listener) {
        attributeChanged = listener == null ? change -> {
        } : listener;
        return () -> attributeChanged = change -> {
        };
    }

    public PropertyGrid setAttributeValue(String name, String value) {
        if (inspection == null) return this;
        String normalizedName = normalizeName(name);
        XmlWidgetElement element = inspection.element();
        String oldValue = element.attribute(normalizedName).orElse("");
        element.setAttribute(normalizedName, value);
        refreshInspection();
        attributeChanged.accept(new AttributeChange(ChangeKind.SET, normalizedName, oldValue, value == null ? "" : value));
        return this;
    }

    public PropertyGrid resetAttribute(String name) {
        if (inspection == null) return this;
        String normalizedName = normalizeName(name);
        String defaultValue = row(normalizedName)
                .map(PropertyFieldRow::defaultValue)
                .orElse("");
        String oldValue = inspection.element().attribute(normalizedName).orElse("");
        inspection.element().setAttribute(normalizedName, defaultValue);
        refreshInspection();
        attributeChanged.accept(new AttributeChange(ChangeKind.RESET, normalizedName, oldValue, defaultValue));
        return this;
    }

    public PropertyGrid removeAttribute(String name) {
        if (inspection == null) return this;
        String normalizedName = normalizeName(name);
        String oldValue = inspection.element().attribute(normalizedName).orElse("");
        boolean removed = inspection.element().removeAttribute(normalizedName);
        if (!removed) return this;
        refreshInspection();
        attributeChanged.accept(new AttributeChange(ChangeKind.REMOVE, normalizedName, oldValue, ""));
        return this;
    }

    public ScrollView scrollView() {
        return scrollView;
    }

    public VBox content() {
        return content;
    }

    private void refreshInspection() {
        if (inspection != null) {
            inspection = XmlWidgetInspector.inspect(inspection.element(), registry);
        }
        rebuild();
    }

    private void rebuild() {
        rows.clear();
        categories.clear();
        propertyChildSlots.clear();
        content.clearChildren();
        content.applyQueuedMutations();

        if (inspection == null) {
            content.addChild(new Label("No XML selection"));
            content.applyQueuedMutations();
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
            return;
        }

        Label header = new Label(inspection.displayName() + " - " + inspection.category());
        header.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
        content.addChild(header);

        Map<String, List<PropertyFieldRow>> grouped = new LinkedHashMap<>();
        Set<String> seen = new LinkedHashSet<>();
        for (XmlWidgetInspector.Attribute attribute : inspection.attributes()) {
            XmlAttributeDescriptor descriptor = attribute.descriptor()
                    .orElseGet(() -> XmlAttributeDescriptor.of(attribute.name()).category("Unknown"));
            String rowName = attribute.descriptor().map(XmlAttributeDescriptor::name).orElse(attribute.name());
            seen.add(rowName);
            addRow(grouped, descriptor, attribute.source().name(), attribute.value(), true);
        }

        if (showUnsetAttributes) {
            for (XmlAttributeDescriptor descriptor : inspection.availableAttributes()) {
                if (!seen.contains(descriptor.name())) {
                    addRow(grouped, descriptor, descriptor.name(), descriptor.defaultValue(), false);
                }
            }
        }

        for (Map.Entry<String, List<PropertyFieldRow>> entry : grouped.entrySet()) {
            categories.add(new Category(entry.getKey(), entry.getValue()));
            addCategory(entry.getKey(), entry.getValue());
        }

        if (showPropertyChildren && !inspection.propertyChildren().isEmpty()) {
            propertyChildSlots.addAll(inspection.propertyChildren());
            addPropertyChildSlots();
        }

        content.applyQueuedMutations();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void addRow(Map<String, List<PropertyFieldRow>> grouped,
                        XmlAttributeDescriptor descriptor,
                        String attributeName,
                        String value,
                        boolean present) {
        PropertyFieldRow row = new PropertyFieldRow(descriptor, attributeName, value, present)
                .labelWidth(labelWidth)
                .onResetRequested(() -> resetAttribute(attributeName))
                .onRemoveRequested(() -> removeAttribute(attributeName));
        row.onValueChanged(change -> setAttributeValue(change.attributeName(), change.newValue()));
        rows.add(row);
        grouped.computeIfAbsent(row.category(), ignored -> new ArrayList<>()).add(row);
    }

    private void addCategory(String category, List<PropertyFieldRow> categoryRows) {
        Label categoryHeader = new Label(category);
        categoryHeader.layout(style -> style.height(18.0f).flexGrow(0.0f).flexShrink(0.0f));
        content.addChild(categoryHeader);
        for (PropertyFieldRow row : categoryRows) {
            content.addChild(row);
        }
    }

    private void addPropertyChildSlots() {
        Label header = new Label("Children");
        header.layout(style -> style.height(18.0f).flexGrow(0.0f).flexShrink(0.0f));
        content.addChild(header);
        for (XmlPropertyChildDescriptor slot : propertyChildSlots) {
            Label row = new Label(slot.displayName() + " - " + slot.category());
            row.layout(style -> style.height(18.0f).flexGrow(0.0f).flexShrink(0.0f));
            content.addChild(row);
        }
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Property attribute name must not be blank");
        return normalized;
    }

    public enum ChangeKind {
        SET,
        RESET,
        REMOVE
    }

    public record AttributeChange(ChangeKind kind, String attributeName, String oldValue, String newValue) {
        public AttributeChange {
            if (kind == null) kind = ChangeKind.SET;
            attributeName = normalizeName(attributeName);
            oldValue = oldValue == null ? "" : oldValue;
            newValue = newValue == null ? "" : newValue;
        }
    }

    public record Category(String name, List<PropertyFieldRow> rows) {
        public Category {
            name = name == null || name.isBlank() ? "Common" : name;
            rows = List.copyOf(rows == null ? List.of() : rows);
        }
    }
}

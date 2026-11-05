package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.LoadContext;
import dev.sixik.unigui.xaml.schema.Meta;
import dev.sixik.unigui.xaml.schema.PropertyInfo;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SlotInfo;
import dev.sixik.unigui.xaml.schema.TypeInfo;
import dev.sixik.unigui.xaml.schema.ValueKind;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SchemaTest {
    private static final LoadContext CONTEXT = LoadContext.DEFAULT;

    // ------------------------------------------------------------------ тестовые виджеты

    enum Tone {INFO, WARNING}

    public static class Badge extends TextWidget {
        private float importance;
        private boolean active;
        private Tone tone = Tone.INFO;
        private final MutableColor accent = new MutableColor();

        private float importance() {
            return importance;
        }

        @XamlProperty(value = "importance", category = "Behavior", defaultValue = "0", description = "Sort priority.")
        public Badge importance(float importance) {
            this.importance = importance;
            return this;
        }

        public boolean active() {
            return active;
        }

        @XamlProperty(value = "active", category = "Behavior", defaultValue = "false")
        public Badge active(boolean active) {
            this.active = active;
            return this;
        }

        @XamlProperty(value = "tone", category = "Content", defaultValue = "info")
        public Badge tone(Tone tone) {
            this.tone = tone == null ? Tone.INFO : tone;
            return this;
        }

        public Tone tone() {
            return tone;
        }

        @XamlProperty(value = "accent", category = "Appearance", defaultValue = "#FFFFFFFF")
        public Badge accent(MutableColor accent) {
            this.accent.set(accent);
            return this;
        }

        /** Тип параметра не поддержан: атрибут должен быть молча пропущен, а не ронять сканер. */
        @XamlProperty("unsupported")
        public Badge unsupported(Object value) {
            return this;
        }
    }

    public static class LoudBadge extends Badge {
        boolean activeCalled;

        /** Переопределение без аннотации: свойство должно остаться, а вызов — прийти в этот метод. */
        @Override
        public Badge active(boolean active) {
            activeCalled = true;
            return super.active(active);
        }

        /** Аннотация потомка перекрывает аннотацию предка. */
        @Override
        @XamlProperty(value = "importance", category = "Appearance", defaultValue = "99")
        public Badge importance(float importance) {
            return super.importance(importance * 2);
        }
    }

    private static <T extends Widget> TypeInfo<T> typeOf(Class<T> type, java.util.function.Supplier<T> factory) {
        return TypeInfo.of(UiName.of("T"), type, factory).build();
    }

    private static <T extends Widget> PropertyInfo<? super T, ?> property(TypeInfo<T> type, String name) {
        return type.property(name).orElseThrow(() -> new AssertionError("missing property " + name));
    }

    // ------------------------------------------------------------------ сканер аннотаций

    @Test
    void scannerExposesAnnotatedSettersWithTypedMetadata() {
        TypeInfo<Badge> type = typeOf(Badge.class, Badge::new);

        PropertyInfo<? super Badge, ?> importance = property(type, "importance");
        assertEquals(ValueKind.NUMBER, importance.kind());
        assertEquals("Behavior", importance.category());
        assertEquals("0", importance.defaultValue());
        assertEquals("Sort priority.", importance.description());
        assertEquals(ValueKind.BOOLEAN, property(type, "active").kind());
        assertEquals(ValueKind.ENUM, property(type, "tone").kind());
        assertEquals(ValueKind.COLOR, property(type, "accent").kind());
        assertTrue(type.property("unsupported").isEmpty(), "unsupported parameter types are skipped");
    }

    @Test
    void scannedSettersApplyConvertedValuesIncludingPrimitivesAndEnums() {
        TypeInfo<Badge> type = typeOf(Badge.class, Badge::new);
        Badge badge = type.create();

        property(type, "importance").apply(badge, " 2.5 ", CONTEXT);
        property(type, "active").apply(badge, "yes", CONTEXT);
        property(type, "tone").apply(badge, "warning", CONTEXT);
        property(type, "accent").apply(badge, "#11223344", CONTEXT);

        assertEquals(2.5f, property(type, "importance").get(badge), "getter found by naming convention (even private)");
        assertTrue(badge.active());
        assertEquals(Tone.WARNING, badge.tone());
    }

    @Test
    void invalidValuesFailWithAClearException() {
        TypeInfo<Badge> type = typeOf(Badge.class, Badge::new);
        Badge badge = type.create();
        assertThrows(IllegalArgumentException.class, () -> property(type, "importance").apply(badge, "heavy", CONTEXT));
        assertThrows(IllegalArgumentException.class, () -> property(type, "tone").apply(badge, "loud", CONTEXT));
        assertThrows(IllegalArgumentException.class, () -> property(type, "active").apply(badge, "maybe", CONTEXT));
    }

    @Test
    void overridingWithoutAnnotationKeepsPropertyAndDispatchesVirtually() {
        TypeInfo<LoudBadge> type = typeOf(LoudBadge.class, LoudBadge::new);
        LoudBadge badge = type.create();
        property(type, "active").apply(badge, "true", CONTEXT);
        assertTrue(badge.activeCalled, "setter must reach the subclass override");
        assertTrue(badge.active());
    }

    @Test
    void closestAnnotationWins() {
        TypeInfo<LoudBadge> type = typeOf(LoudBadge.class, LoudBadge::new);
        PropertyInfo<? super LoudBadge, ?> importance = property(type, "importance");
        assertEquals("99", importance.defaultValue());
        assertEquals("Appearance", importance.category());

        LoudBadge badge = type.create();
        importance.apply(badge, "3", CONTEXT);
        assertEquals(6.0f, importance.get(badge), "subclass override doubles the value");
    }

    // ------------------------------------------------------------------ наследование метаданных

    @Test
    void manualPropertyKeepsMetadataOfTheAnnotationItOverrides() {
        TypeInfo<Badge> type = TypeInfo.of(UiName.of("Badge"), Badge.class, Badge::new)
                .property("importance", Converters.FLOAT, (badge, value) -> badge.importance(value * 10))
                .build();
        PropertyInfo<? super Badge, ?> importance = property(type, "importance");
        assertEquals("Behavior", importance.category(), "category inherited from the annotation");
        assertEquals("0", importance.defaultValue());
        assertEquals("Sort priority.", importance.description());

        Badge badge = type.create();
        importance.apply(badge, "2", CONTEXT);
        assertEquals(20.0f, importance.get(badge), "behaviour comes from the manual setter");
    }

    @Test
    void explicitMetadataBeatsInheritedOne() {
        TypeInfo<Badge> type = TypeInfo.of(UiName.of("Badge"), Badge.class, Badge::new)
                .property("importance", Converters.FLOAT, Badge::importance,
                        Meta.category("Layout").defaultValue("7"))
                .build();
        PropertyInfo<? super Badge, ?> importance = property(type, "importance");
        assertEquals("Layout", importance.category());
        assertEquals("7", importance.defaultValue());
        assertEquals("Sort priority.", importance.description(), "unset fields still fall back");
    }

    @Test
    void bindableRequiresAnExistingPropertyAndRecordsTheValueType() {
        TypeInfo<Badge> type = TypeInfo.of(UiName.of("Badge"), Badge.class, Badge::new).bindable("importance").build();
        assertEquals(Float.class, property(type, "importance").bindingType());
        assertTrue(property(type, "importance").bindable());
        assertTrue(property(type, "active").bindable(), "annotated setters are bindable by default");
        assertEquals(Boolean.class, property(type, "active").bindingType());
        assertFalse(property(type, "width").bindable(), "common layout properties are not bindable");

        assertThrows(IllegalArgumentException.class,
                () -> TypeInfo.of(UiName.of("Badge"), Badge.class, Badge::new).bindable("nope").build());
    }

    @Test
    void duplicateSlotsAreRejected() {
        TypeInfo.Builder<VBox> builder = TypeInfo.of(UiName.of("VBox"), VBox.class, VBox::new)
                .slot(SlotInfo.many("Children", (parent, child) -> parent.addChild(child), ""));
        assertThrows(IllegalArgumentException.class,
                () -> builder.slot(SlotInfo.many("Children", (parent, child) -> parent.addChild(child), "")));
        assertThrows(IllegalArgumentException.class, () -> SlotInfo.many("A.B", (parent, child) -> { }, ""));
    }

    // ------------------------------------------------------------------ реестр

    @Test
    void definitionsAreLazyAndBuiltExactlyOnce() {
        AtomicInteger builds = new AtomicInteger();
        Schema schema = Schema.empty().define("Lazy", () -> {
            builds.incrementAndGet();
            return TypeInfo.of(Schema.name("Lazy"), Label.class, Label::new).build();
        });
        assertEquals(0, builds.get(), "defining must not build");
        assertTrue(schema.contains(UiName.of("Lazy")));
        assertEquals(0, builds.get(), "contains must not build");

        TypeInfo<?> first = schema.type("Lazy").orElseThrow();
        TypeInfo<?> second = schema.type(Schema.name("Lazy")).orElseThrow();
        assertSame(first, second);
        assertEquals(1, builds.get());
    }

    @Test
    void builtInsDoNotBuildAnyTypeUntilAskedFor() {
        Schema schema = Schema.builtIns();
        assertTrue(schema.contains(UiName.of("Button")));
        assertTrue(schema.contains(UiName.of("Border")), "aliases are known without building");
        assertTrue(schema.type("Button").isPresent());
        assertSame(schema.type("Border").orElseThrow(), schema.type("Box").orElseThrow(), "alias resolves to its target");
        assertTrue(schema.type("NoSuchWidget").isEmpty());
    }

    @Test
    void emptyNamespaceAndUiNamespaceSeeTheSameTypes() {
        Schema schema = Schema.builtIns();
        assertSame(schema.type(UiName.of("Label")).orElseThrow(),
                schema.type(UiName.of(Schema.UI_NAMESPACE, "Label")).orElseThrow());
        assertTrue(schema.type(UiName.of("urn:other", "Label")).isEmpty(), "other namespaces are separate");
    }

    @Test
    void customNamespacesAreIsolated() {
        Schema schema = Schema.builtIns().define("urn:mc", "Badge",
                () -> TypeInfo.of(new UiName("urn:mc", "Badge", "mc"), Badge.class, Badge::new).build());
        assertTrue(schema.type(new UiName("urn:mc", "Badge", "mc")).isPresent());
        assertTrue(schema.type("Badge").isEmpty(), "not visible in the default namespace");
    }

    @Test
    void duplicateTypesAndAliasesAreRejected() {
        Schema schema = Schema.builtIns();
        assertThrows(IllegalArgumentException.class, () -> schema.define("Button", () -> null));
        assertThrows(IllegalArgumentException.class, () -> schema.alias("Border", "Box"));
        assertThrows(IllegalArgumentException.class, () -> schema.alias("Fresh", "Missing"));
        assertThrows(IllegalArgumentException.class, () -> schema.alias("Label", "Box"), "alias may not shadow a type");
    }

    // ------------------------------------------------------------------ портированные типы

    private static TypeInfo<?> builtIn(String name) {
        return Schema.builtIns().type(name).orElseThrow();
    }

    @Test
    @SuppressWarnings("unchecked")
    void commonLayoutPropertiesApplyToBuiltInWidgets() {
        TypeInfo<Button> type = (TypeInfo<Button>) builtIn("Button");
        Button button = type.create();
        property(type, "text").apply(button, "Save", CONTEXT);
        property(type, "width").apply(button, "120", CONTEXT);
        property(type, "height").apply(button, "50%", CONTEXT);
        property(type, "opacity").apply(button, "0.5", CONTEXT);
        property(type, "enabled").apply(button, "off", CONTEXT);

        assertEquals("Save", button.text());
        assertEquals(SizeValue.px(120), button.layoutStyle().width());
        assertEquals(SizeValue.percent(50), button.layoutStyle().height());
        assertEquals(0.5f, button.opacity());
        assertFalse(button.enabled());
        assertTrue(property(type, "text").bindable());
        assertTrue(property(type, "opacity").bindable());
        assertFalse(property(type, "width").bindable());
    }

    @Test
    @SuppressWarnings("unchecked")
    void onClickResolvesCommandsFromTheExplicitLoadContext() {
        TypeInfo<Button> type = (TypeInfo<Button>) builtIn("Button");
        Button button = type.create();
        assertThrows(IllegalArgumentException.class,
                () -> property(type, "onClick").apply(button, "missing", CONTEXT), "unknown command");

        AtomicInteger executed = new AtomicInteger();
        var commands = dev.sixik.unigui.xaml.bind.XamlCommandRegistry.empty()
                .register("save", (source, event) -> executed.incrementAndGet());
        LoadContext context = new LoadContext(null, commands);
        property(type, "onClick").apply(button, "save", context);
        assertEquals(ValueKind.BINDING_OR_ACTION, property(type, "onClick").kind());
    }

    @Test
    @SuppressWarnings("unchecked")
    void containersAcceptChildrenThroughDirectContentAndSlots() {
        TypeInfo<VBox> vbox = (TypeInfo<VBox>) builtIn("VBox");
        VBox box = vbox.create();
        assertTrue(vbox.acceptsChildren());
        vbox.addChild(box, new Label());
        vbox.slot("Children").orElseThrow().sink().add(box, new Label());
        assertEquals(2, box.children().size());

        TypeInfo<Label> label = (TypeInfo<Label>) builtIn("Label");
        assertFalse(label.acceptsChildren());
        assertThrows(IllegalArgumentException.class, () -> label.addChild(label.create(), new Label()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void scrollViewAcceptsExactlyOneContentChild() {
        TypeInfo<ScrollView> type = (TypeInfo<ScrollView>) builtIn("ScrollView");
        ScrollView scroll = type.create();
        type.addChild(scroll, new Label());
        assertNotNull(scroll.content());
        assertThrows(IllegalArgumentException.class, () -> type.addChild(scroll, new Label()));
        assertTrue(type.slot("Content").orElseThrow().single());
    }

    @Test
    void everyBuiltInTypeBuildsAndCreatesAnInstance() {
        Schema schema = Schema.builtIns();
        assertEquals(69, schema.types().size());
        for (TypeInfo<?> type : schema.types()) {
            Widget widget = type.create();
            assertNotNull(widget, type.name().local());
            assertTrue(type.javaType().isInstance(widget), type.name().local());
            if (WidgetBase.class.isAssignableFrom(type.javaType())) {
                assertTrue(type.property("width").isPresent(), type.name().local() + " must inherit common layout");
                assertTrue(type.property("id").isPresent(), type.name().local() + " must expose id from WidgetBase");
            }
        }
    }

    @Test
    void warmUpRunsOffThreadAndLeavesTheSchemaUsable() throws Exception {
        Thread caller = Thread.currentThread();
        java.util.concurrent.atomic.AtomicReference<Thread> worker = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.Executor executor = task -> new Thread(() -> {
            worker.set(Thread.currentThread());
            task.run();
        }, "schema-warmup").start();

        Schema.warmUp(executor).get(30, java.util.concurrent.TimeUnit.SECONDS);
        Schema.warmUp(Runnable::run).get(30, java.util.concurrent.TimeUnit.SECONDS);   // idempotent
        assertTrue(worker.get() != null && worker.get() != caller, "warm-up must not run on the caller thread");
        assertTrue(Schema.builtIns().type("VBox").isPresent());
    }

    @Test
    void convertersParseTheDocumentedSyntax() {
        assertEquals(SizeValue.auto(), Converters.SIZE.convert("auto", CONTEXT));
        assertEquals(SizeValue.px(12), Converters.SIZE.convert("12px", CONTEXT));
        assertEquals(SizeValue.percent(40), Converters.SIZE.convert("40%", CONTEXT));
        var all = Converters.INSETS.convert("3", CONTEXT);
        assertEquals(3.0f, all.left());
        assertEquals(3.0f, all.bottom());
        var vertical = Converters.INSETS.convert("1 2", CONTEXT);
        assertEquals(2.0f, vertical.left(), "two values are vertical horizontal");
        assertEquals(1.0f, vertical.top());
        assertEquals(4, Converters.GRID_TRACKS.convert("100px repeat(3, 1fr)", CONTEXT).size());
        assertEquals(Tone.WARNING, Converters.enumOf(Tone.class).convert(" Warning ", CONTEXT));
        assertEquals(Boolean.TRUE, Converters.BOOLEAN.convert("ON", CONTEXT));
        assertThrows(IllegalArgumentException.class, () -> Converters.BOOLEAN.convert("perhaps", CONTEXT));
        assertThrows(IllegalArgumentException.class, () -> Converters.INSETS.convert("1 2 3", CONTEXT));
        assertNull(Converters.forType(Thread.class, false), "unsupported types yield null, not an exception");
    }
}

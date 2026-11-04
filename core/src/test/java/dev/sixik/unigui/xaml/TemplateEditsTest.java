package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.TemplateEdits;
import dev.sixik.unigui.xaml.struct.StructType;
import dev.sixik.unigui.xaml.struct.Structs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TemplateEditsTest {
    private final EditorSession session = EditorSession.create(
            "<VirtualListView id=\"list\" itemHeight=\"20\"/>");

    @BeforeEach
    void registerStructs() {
        Structs.register("""
                <Struct x:Name="TplProduct" xmlns:x="https://unigui.dev/xaml">
                  <Field Name="title" Type="string"/>
                </Struct>""");
        Structs.register("""
                <Struct x:Name="TplLine" xmlns:x="https://unigui.dev/xaml">
                  <Field Name="product" Type="struct:TplProduct"/>
                  <Field Name="quantity" Type="int"/>
                </Struct>""");
    }

    @AfterEach
    void unregisterStructs() {
        Structs.unregister("TplLine");
        Structs.unregister("TplProduct");
    }

    private UiNode list() {
        return session.document().root();
    }

    @Test
    void aTemplateIsSetBoundAndReplacedWithUndo() {
        assertTrue(TemplateEdits.template(list()).isEmpty());
        assertTrue(session.apply(TemplateEdits.setSource(list(), "catalog")));
        assertTrue(session.text().contains("{Binding catalog}"), session.text());

        UiNode label = UiNode.of("Label");
        assertTrue(session.apply(TemplateEdits.setTemplate(list(), label)));
        UiNode root = TemplateEdits.template(list()).orElseThrow();
        assertTrue(session.apply(TemplateEdits.bindItem(root, "text", "title")));
        assertTrue(session.text().contains("{Item title}"), session.text());

        List<TemplateEdits.ItemBinding> bindings = TemplateEdits.itemBindings(root);
        assertEquals(1, bindings.size());
        assertEquals("text", bindings.get(0).property());
        assertEquals("title", bindings.get(0).path());

        assertTrue(session.apply(TemplateEdits.setTemplate(list(), UiNode.of("Button"))), "a new template replaces the old");
        assertEquals("Button", TemplateEdits.template(list()).orElseThrow().type().local());
        assertTrue(session.undo());
        assertEquals("Label", TemplateEdits.template(list()).orElseThrow().type().local());
    }

    @Test
    void theWholeRecordAndNestedTemplateWidgetsAreFound() {
        UiNode box = dev.sixik.unigui.xaml.format.XamlFormat.INSTANCE.read(
                "<HBox><Label text=\"{Item product.title}\"/><Label text=\"{Item}\"/></HBox>").root();
        assertTrue(session.apply(TemplateEdits.setTemplate(list(), box)));

        List<TemplateEdits.ItemBinding> bindings = TemplateEdits.itemBindings(TemplateEdits.template(list()).orElseThrow());
        assertEquals(List.of("product.title", ""), bindings.stream().map(TemplateEdits.ItemBinding::path).toList());
    }

    @Test
    void clearingWorksOnlyWhenThereIsATemplate() {
        assertFalse(session.apply(TemplateEdits.clearTemplate(list())));
        assertTrue(session.apply(TemplateEdits.setTemplate(list(), UiNode.of("Label"))));
        assertTrue(session.apply(TemplateEdits.clearTemplate(list())));
        assertTrue(TemplateEdits.template(list()).isEmpty());
        assertTrue(session.apply(TemplateEdits.setSource(list(), "x")));
        assertTrue(session.apply(TemplateEdits.setSource(list(), "")), "an empty path clears the source");
        assertFalse(session.text().contains("items="), session.text());
    }

    @Test
    void itemPathsListFieldsIncludingNestedStructs() {
        StructType line = Structs.require("TplLine");
        assertEquals(List.of("product", "product.title", "quantity"), TemplateEdits.itemPaths(line, Structs::find, 3));
        assertEquals(List.of("product", "quantity"), TemplateEdits.itemPaths(line, Structs::find, 1), "depth limits nesting");
        assertEquals(List.of("product", "quantity"), TemplateEdits.itemPaths(line, null, 3), "without a resolver nothing nests");
    }
}

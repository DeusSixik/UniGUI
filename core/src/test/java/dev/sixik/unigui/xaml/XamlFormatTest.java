package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.format.UiFormat;
import dev.sixik.unigui.xaml.format.XamlFormat;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XamlFormatTest {
    private static final XamlFormat XAML = XamlFormat.INSTANCE;

    private static UiParseException parseFails(String source) {
        return assertThrows(UiParseException.class, () -> XAML.read(source), source);
    }

    // ------------------------------------------------------------------ reading

    @Test
    void readsNodesPropertiesAndLocations() {
        UiDocument document = XAML.read("""
                <VBox id="root" spacing="6">
                    <Label text="Hi"/>
                    <Button text="Go" enabled="false">Tail</Button>
                </VBox>
                """);
        UiNode root = document.root();
        assertEquals("VBox", root.type().local());
        assertEquals(new SourceLocation(1, 1), root.location());
        assertEquals(new UiValue.Literal("root"), root.property("id").orElseThrow().value());
        assertEquals(new SourceLocation(1, 7), root.property("id").orElseThrow().location(), "attribute position");
        assertEquals(new SourceLocation(1, 17), root.property("spacing").orElseThrow().location());

        List<UiNode> children = root.children();
        assertEquals(2, children.size());
        assertEquals(new SourceLocation(2, 5), children.get(0).location());
        assertEquals("Tail", children.get(1).text());
        assertEquals(new SourceLocation(3, 23), children.get(1).property("enabled").orElseThrow().location());
    }

    @Test
    void resolvesNamespacesAndPrefixes() {
        UiDocument document = XAML.read("""
                <Screen xmlns="urn:ui" xmlns:x="urn:x" xmlns:mc="urn:mc" x:Name="main">
                    <mc:ItemIcon item="stone"/>
                </Screen>
                """);
        UiNode root = document.root();
        assertEquals(new UiName("urn:ui", "Screen", ""), root.type());
        assertEquals(List.of("", "x", "mc"), List.copyOf(root.namespaces().keySet()));
        UiProperty name = root.property(null, new UiName("urn:x", "Name", "x")).orElseThrow();
        assertEquals("x:Name", name.displayName());
        UiNode icon = root.children().get(0);
        assertEquals("urn:mc", icon.type().namespace());
        assertEquals("item", icon.properties().get(0).name().local());
        assertEquals("", icon.properties().get(0).name().namespace(), "unprefixed attributes have no namespace");
        assertEquals("urn:ui", root.type().namespace());
    }

    @Test
    void defaultNamespaceAppliesToChildrenButNotToAttributes() {
        UiNode root = XAML.read("<A xmlns=\"urn:ui\"><B c=\"1\"/></A>").root();
        UiNode child = root.children().get(0);
        assertEquals("urn:ui", child.type().namespace());
        assertEquals("", child.properties().get(0).name().namespace());
    }

    @Test
    void readsAttachedAndPropertyElements() {
        UiNode root = XAML.read("""
                <Grid>
                    <Button Grid.Row="1" Grid.Column="2">
                        <Button.Content>Save &amp; exit</Button.Content>
                        <Button.Tooltip><Label text="tip"/></Button.Tooltip>
                    </Button>
                </Grid>
                """).root();
        UiNode button = root.children().get(0);
        assertTrue(button.children().isEmpty(), "property elements are not children");

        UiProperty row = button.property(UiName.of("Grid"), UiName.of("Row")).orElseThrow();
        assertEquals(new UiValue.Literal("1"), row.value());
        assertEquals(UiProperty.Form.ATTRIBUTE, row.form());
        assertEquals("Grid.Row", row.displayName());

        UiProperty content = button.property(UiName.of("Button"), UiName.of("Content")).orElseThrow();
        assertEquals(UiProperty.Form.ELEMENT, content.form());
        assertEquals(new UiValue.Literal("Save & exit"), content.value());

        UiProperty tooltip = button.property(UiName.of("Button"), UiName.of("Tooltip")).orElseThrow();
        UiValue.Elements elements = assertInstanceOf(UiValue.Elements.class, tooltip.value());
        assertEquals("Label", ((UiNode) elements.content().get(0)).type().local());
    }

    @Test
    void propertyElementTextMayBeAMarkupExtensionWithOuterPrefix() {
        UiNode root = XAML.read("""
                <Box xmlns:x="urn:x">
                    <Label><Label.Text>{x:Static Foo}</Label.Text></Label>
                </Box>
                """).root();
        UiProperty text = root.children().get(0).property(UiName.of("Label"), UiName.of("Text")).orElseThrow();
        UiValue.Extension extension = assertInstanceOf(UiValue.Extension.class, text.value());
        assertEquals("urn:x", extension.name().namespace());
    }

    @Test
    void parsesMarkupExtensionsInAttributes() {
        UiNode label = XAML.read("<Label text=\"{Binding Path=Health, Mode=OneWay}\"/>").root();
        UiValue.Extension binding = assertInstanceOf(UiValue.Extension.class,
                label.property("text").orElseThrow().value());
        assertEquals(new UiValue.Literal("Health"), binding.named().get("Path"));
    }

    @Test
    void decodesEntitiesCdataAndNormalizesAttributeWhitespace() {
        UiNode root = XAML.read("<T a=\"x&#10;y&lt;&#x41;\" b=\"p\tq\">1 &amp; 2 <![CDATA[<raw>]]></T>").root();
        assertEquals(new UiValue.Literal("x\ny<A"), root.property("a").orElseThrow().value(),
                "character references are kept as written, including &#10;");
        assertEquals(new UiValue.Literal("p q"), root.property("b").orElseThrow().value(),
                "a literal tab inside an attribute is normalized to a space");
        assertEquals("1 & 2 <raw>", root.text());
    }

    @Test
    void keepsCommentsAndDropsFormattingWhitespace() {
        UiDocument document = XAML.read("<!-- head --><A>\n  <!-- inside -->\n  <B/>\n</A>");
        assertEquals(List.of(" head "), document.prologComments());
        List<UiContent> content = document.root().content();
        assertEquals(2, content.size(), "whitespace-only text next to elements is formatting");
        assertEquals(new UiContent.Comment(" inside "), content.get(0));
        assertInstanceOf(UiNode.class, content.get(1));
    }

    @Test
    void acceptsBomXmlDeclarationAndProcessingInstructions() {
        UiNode root = XAML.read("﻿<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<?pi data?>\n<A/>\n").root();
        assertEquals("A", root.type().local());
        assertEquals(new SourceLocation(3, 1), root.location());
    }

    // ------------------------------------------------------------------ errors

    @Test
    void reportsErrorsWithPositions() {
        UiParseException mismatch = parseFails("<A>\n  <B>\n  </C>\n</A>");
        assertEquals(3, mismatch.location().line());
        assertTrue(mismatch.getMessage().contains("</C>"), mismatch.getMessage());

        UiParseException duplicate = parseFails("<A x=\"1\"\n   x=\"2\"/>");
        assertEquals(new SourceLocation(2, 4), duplicate.location());
        assertTrue(duplicate.getMessage().contains("Duplicate attribute 'x'"));

        UiParseException unquoted = parseFails("<A x=1/>");
        assertTrue(unquoted.getMessage().contains("quoted"));

        UiParseException prefix = parseFails("<A>\n<q:B/>\n</A>");
        assertEquals(2, prefix.location().line());
        assertTrue(prefix.getMessage().contains("Unknown namespace prefix 'q'"));

        assertTrue(parseFails("<A><B></A>").getMessage().contains("does not match"));
        assertTrue(parseFails("<A>").getMessage().contains("Unterminated"));
        assertTrue(parseFails("<A/><B/>").getMessage().contains("after the root"));
        assertTrue(parseFails("   ").getMessage().contains("blank"));
        assertTrue(parseFails("text").getMessage().contains("root element"));
        assertTrue(parseFails("<A b=\"&bogus;\"/>").getMessage().contains("Unknown entity"));
        assertTrue(parseFails("<A b=\"&#xZZ;\"/>").getMessage().contains("Invalid character reference"));
        assertTrue(parseFails("<A b=\"a<b\"/>").getMessage().contains("'<'"));
    }

    @Test
    void rejectsDoctypeAndEntityDefinitions() {
        UiParseException doctype = parseFails("<!DOCTYPE A [<!ENTITY x \"boom\">]><A>&x;</A>");
        assertTrue(doctype.getMessage().contains("DOCTYPE"));
    }

    @Test
    void enforcesDepthLimit() {
        String deep = "<A>".repeat(10) + "</A>".repeat(10);
        assertEquals("A", XAML.read(deep, new UiFormat.ReadOptions(10)).root().type().local());
        UiParseException tooDeep = assertThrows(UiParseException.class,
                () -> XAML.read(deep, new UiFormat.ReadOptions(9)));
        assertTrue(tooDeep.getMessage().contains("deeper"));
    }

    @Test
    void rejectsBadPropertyElementsAndExtensions() {
        assertTrue(parseFails("<A><A.B x=\"1\"/></A>").getMessage().contains("cannot have attributes"));
        assertTrue(parseFails("<A><A.B.C/></A>").getMessage().contains("Expected Owner.Property"));
        UiParseException badValue = parseFails("<A\n text=\"{Binding Health\"/>");
        assertEquals(new SourceLocation(2, 2), badValue.location());
        assertTrue(badValue.getMessage().contains("text"));
    }

    // ------------------------------------------------------------------ writing and round trips

    private static void assertRoundTrip(String source) {
        UiDocument first = XAML.read(source);
        String written = XAML.write(first);
        UiDocument second = XAML.read(written);
        assertTrue(first.sameStructure(second), "structure changed by write/read:\n" + written);
        assertEquals(written, XAML.write(second), "writer output must be a fixed point");
    }

    @Test
    void roundTripsFeatureSamples() {
        assertRoundTrip("<A/>");
        assertRoundTrip("<A x=\"1\" y=\"a &amp; b &lt; c &quot;q&quot;\"/>");
        assertRoundTrip("<A text=\"line1&#10;line2&#9;tab\"/>");
        assertRoundTrip("<A xmlns=\"urn:ui\" xmlns:x=\"urn:x\" x:Name=\"n\"><x:B/></A>");
        assertRoundTrip("<A text=\"{Binding Path=Health, Mode=OneWay}\" tip=\"{}{literal}\"/>");
        assertRoundTrip("<!--c--><A><!--inner--><B/>text<C/></A>");
        assertRoundTrip("<Grid><Button Grid.Row=\"1\"><Button.Content>Hi &lt;</Button.Content>"
                + "<Button.Tooltip><Label text=\"t\"/><!--k--></Button.Tooltip></Button></Grid>");
        assertRoundTrip("<A xmlns:x=\"urn:x\"><L><L.Text>{x:Static Foo}</L.Text></L></A>");
    }

    @Test
    void wrapsLongTagsOneAttributePerLine() {
        String written = XAML.write(XAML.read("<Box id=\"aaaaaaaaaaaaaaaaaaaa\" width=\"bbbbbbbbbbbbbbbbbbbbbb\" "
                + "height=\"cccccccccccccccccccccc\" padding=\"dddddddddddddddddddd\" margin=\"eeeeeeeeeeeeee\"/>"));
        assertTrue(written.lines().count() > 3, written);
        assertTrue(written.contains("\n    width="), written);
        assertFalse(XAML.write(XAML.read("<A x=\"1\" y=\"2\"/>")).contains("\n    "), "short tags stay on one line");
    }

    @Test
    void writesDeclarationWhenRequested() {
        UiDocument document = UiDocument.of(UiNode.of("A").set("x", "1"));
        assertTrue(XAML.write(document, new UiFormat.WriteOptions(2, true, 100)).startsWith("<?xml version=\"1.0\""));
    }

    @Test
    void programmaticTreesWriteAndRead() {
        UiNode root = UiNode.of("VBox").set("spacing", "4");
        root.add(UiNode.of("Button").set("text", "Go"));
        root.set(new UiProperty(UiName.of("Tag"), UiName.of("VBox"),
                new UiValue.Literal("meta"), UiProperty.Form.ELEMENT, null));
        UiDocument parsed = XAML.read(XAML.write(UiDocument.of(root)));
        assertTrue(parsed.sameStructure(UiDocument.of(root)));
    }

    @Test
    void copyIsDeepAndIndependent() {
        UiDocument original = XAML.read("<A x=\"1\"><B/></A>");
        UiDocument copy = original.copy();
        copy.root().set("x", "2");
        copy.root().children().get(0).set("y", "3");
        assertEquals(new UiValue.Literal("1"), original.root().property("x").orElseThrow().value());
        assertTrue(original.root().children().get(0).properties().isEmpty());
        assertFalse(original.sameStructure(copy));
    }

    // ------------------------------------------------------------------ real documents

    private static String resource(String name) {
        try (InputStream stream = XamlFormatTest.class.getClassLoader().getResourceAsStream("assets/unigui/xml/" + name)) {
            if (stream == null) throw new AssertionError("Missing test resource " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new AssertionError(failure);
        }
    }

    private static int countNodes(UiNode node) {
        int count = 1;
        for (UiNode child : node.children()) count += countNodes(child);
        return count;
    }

    private static long goldenLines(String file) throws IOException {
        return Files.readAllLines(Path.of("src", "test", "resources", "golden", file)).stream()
                .filter(line -> !line.isBlank()).count();
    }

    @Test
    void realDocumentsParseAndRoundTrip() throws IOException {
        for (String name : List.of("overview", "xml_demo", "xml_demo_2")) {
            String source = resource(name + ".xml");
            UiDocument document = XAML.read(source);
            assertEquals(goldenLines("tree-" + name + ".txt"), countNodes(document.root()),
                    name + ": node count must equal the widget count recorded in the golden tree");
            assertRoundTrip(source);
        }
    }
}

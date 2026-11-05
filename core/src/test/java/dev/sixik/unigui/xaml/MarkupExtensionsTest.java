package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.format.MarkupExtensions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MarkupExtensionsTest {
    private static final Map<String, String> PREFIXES = Map.of("", "urn:ui", "x", "urn:x");

    private static UiValue parse(String raw) {
        return MarkupExtensions.parse(raw, PREFIXES::get);
    }

    @Test
    void plainTextIsLiteralAndKeepsWhitespace() {
        assertEquals(new UiValue.Literal(" 12 px "), parse(" 12 px "));
        assertEquals(new UiValue.Literal(""), parse(""));
        assertEquals(new UiValue.Literal(""), MarkupExtensions.parse(null, null));
    }

    @Test
    void legacyBindingFormIsPositionalPath() {
        UiValue.Extension binding = assertInstanceOf(UiValue.Extension.class, parse("{Binding screen.title}"));
        assertEquals("Binding", binding.name().local());
        assertEquals("urn:ui", binding.name().namespace());
        assertEquals(new UiValue.Literal("screen.title"), binding.positional().get(0));
        assertTrue(binding.named().isEmpty());
    }

    @Test
    void namedArgumentsKeepOrderAndPositionalFallback() {
        UiValue.Extension binding = assertInstanceOf(UiValue.Extension.class,
                parse("{Binding Health, Mode=OneWay, Converter=PercentFormat}"));
        assertEquals(new UiValue.Literal("Health"), binding.argument("Path", 0));
        assertEquals(new UiValue.Literal("OneWay"), binding.argument("Mode", 5));
        assertEquals(java.util.List.of("Mode", "Converter"), java.util.List.copyOf(binding.named().keySet()));
    }

    @Test
    void prefixedNameResolvesNamespace() {
        UiValue.Extension extension = assertInstanceOf(UiValue.Extension.class, parse("{x:Static Foo.Bar}"));
        assertEquals("urn:x", extension.name().namespace());
        assertEquals("Static", extension.name().local());
        assertEquals("x", extension.name().prefix());
    }

    @Test
    void nestedExtensionsAndQuotedValues() {
        UiValue.Extension outer = assertInstanceOf(UiValue.Extension.class,
                parse("{Binding Path='a, b}', Fallback={StaticResource Missing}}"));
        assertEquals(new UiValue.Literal("a, b}"), outer.named().get("Path"));
        UiValue.Extension fallback = assertInstanceOf(UiValue.Extension.class, outer.named().get("Fallback"));
        assertEquals("StaticResource", fallback.name().local());
        assertEquals(new UiValue.Literal("Missing"), fallback.positional().get(0));
    }

    @Test
    void emptyBracesEscapeALiteralThatLooksLikeAnExtension() {
        assertEquals(new UiValue.Literal("{not an extension}"), parse("{}{not an extension}"));
        assertFalse(MarkupExtensions.isExtension("{}x"));
        assertTrue(MarkupExtensions.isExtension("  {Binding X}"));
    }

    @Test
    void malformedExtensionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> parse("{Binding Health"));
        assertThrows(IllegalArgumentException.class, () -> parse("{Binding Path=}"));
        assertThrows(IllegalArgumentException.class, () -> parse("{Binding A, }, B}"), "text after the closing brace");
        assertThrows(IllegalArgumentException.class, () -> parse("{y:Thing}"), "unknown prefix");
        assertThrows(IllegalArgumentException.class, () -> parse("{Binding 'oops}"));
        assertThrows(IllegalArgumentException.class, () -> parse("{Binding A} trailing"));
    }

    @Test
    void writeThenParseIsIdentity() {
        String[] samples = {
                "{Binding Health}",
                "{Binding Path=Health, Mode=OneWay}",
                "{Binding Path='a, b', Fallback={StaticResource Missing}}",
                "{x:Static Foo.Bar}",
                "{}{literal}",
                "plain",
        };
        for (String sample : samples) {
            UiValue first = parse(sample);
            UiValue second = parse(MarkupExtensions.write(first));
            assertEquals(first, second, "round-trip of " + sample + " via " + MarkupExtensions.write(first));
        }
    }

    @Test
    void literalThatStartsWithBraceIsEscapedOnWrite() {
        assertEquals("{}{x}", MarkupExtensions.write(new UiValue.Literal("{x}")));
        assertEquals("a{x}", MarkupExtensions.write(new UiValue.Literal("a{x}")));
    }

    @Test
    void quotesAreEscapedInsideArguments() {
        UiValue.Extension extension = new UiValue.Extension(UiName.of("Binding"),
                java.util.List.of(new UiValue.Literal("it's a \\ path")), Map.of());
        String written = MarkupExtensions.write(extension);
        assertEquals(extension, MarkupExtensions.parse(written, null), written);
    }
}

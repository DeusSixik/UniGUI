package dev.sixik.unigui.impl.render.shaders;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the contract between the shared GLSL sources and the instance layout constants. Backends bind attributes
 * and strides from the constants, so a shader edit that is not mirrored there would corrupt rendering silently.
 */
class SdfShaderSourcesTest {
    private static final Pattern ATTRIBUTE = Pattern.compile("(?m)^\\s*in\\s+(?:vec([234])|float)\\s+(\\w+);");

    @Test
    void shapeInstanceFloats_matchComponentSum() {
        assertEquals(SdfShapeShaderSources.INSTANCE_FLOATS, sum(SdfShapeShaderSources.INSTANCED_ATTRIBUTE_COMPONENTS));
    }

    @Test
    void shapeInstancedNames_areQuadPlusInstanceAttributes() {
        assertEquals(1 + SdfShapeShaderSources.INSTANCED_ATTRIBUTE_COMPONENTS.length,
                SdfShapeShaderSources.INSTANCED_ATTRIBUTE_NAMES.length);
    }

    @Test
    void shapeInstancedVertex_declaresExactlyTheBoundAttributesWithMatchingWidths() {
        assertAttributesMatch(SdfShapeShaderSources.INSTANCED_VERTEX, SdfShapeShaderSources.INSTANCED_ATTRIBUTE_NAMES,
                widths(2, SdfShapeShaderSources.INSTANCED_ATTRIBUTE_COMPONENTS));
    }

    @Test
    void shapeBasicVertex_declaresBoundAttributes() {
        List<String> declared = declaredNames(SdfShapeShaderSources.VERTEX);
        assertEquals(List.of(SdfShapeShaderSources.BASIC_ATTRIBUTE_NAMES), declared);
    }

    @Test
    void shapeInstancedFragment_hasNoUniformShapeParameters() {
        String fragment = SdfShapeShaderSources.INSTANCED_FRAGMENT;
        assertFalse(fragment.contains("uniform"), "all shape parameters must come from flat varyings");
        for (String name : new String[]{"Color", "Size", "LineStart", "LineEnd", "Radius", "StrokeWidth",
                "ShapeType", "IsStroke"}) {
            assertTrue(fragment.contains("flat in ") && fragment.matches("(?s).*flat in \\w+ " + name + ";.*"),
                    "missing flat varying " + name);
            assertTrue(SdfShapeShaderSources.INSTANCED_VERTEX.matches("(?s).*flat out \\w+ " + name + ";.*"),
                    "vertex shader does not forward " + name);
        }
    }

    @Test
    void shapeBasicFragment_keepsUniformsForDirectUpload() {
        for (String name : new String[]{"Color", "Size", "LineStart", "LineEnd", "Radius", "StrokeWidth",
                "ShapeType", "IsStroke"}) {
            assertTrue(SdfShapeShaderSources.FRAGMENT.matches("(?s).*uniform \\w+ " + name + ";.*"),
                    "missing uniform " + name);
        }
    }

    @Test
    void shapeTypes_matchShaderBranches() {
        assertEquals(0, SdfShapeShaderSources.SHAPE_ROUNDED_RECT);
        assertEquals(1, SdfShapeShaderSources.SHAPE_ELLIPSE);
        assertEquals(2, SdfShapeShaderSources.SHAPE_LINE);
        assertTrue(SdfShapeShaderSources.FRAGMENT.contains("ShapeType == 1"));
        assertTrue(SdfShapeShaderSources.FRAGMENT.contains("ShapeType == 2"));
    }

    @Test
    void textInstanceFloats_matchComponentSum() {
        assertEquals(SdfTextShaderSources.FLOATS_PER_INSTANCE, sum(SdfTextShaderSources.INSTANCE_ATTRIBUTE_COMPONENTS));
        assertEquals(SdfTextShaderSources.INSTANCE_ATTRIBUTE_NAMES.length,
                SdfTextShaderSources.INSTANCE_ATTRIBUTE_COMPONENTS.length);
    }

    @Test
    void textVertex_declaresQuadPlusInstanceAttributesInLayoutOrder() {
        List<String> expected = new ArrayList<>();
        expected.add("Position");
        expected.addAll(List.of(SdfTextShaderSources.INSTANCE_ATTRIBUTE_NAMES));
        assertEquals(expected, declaredNames(SdfTextShaderSources.VERTEX));

        List<Integer> declaredWidths = declaredWidths(SdfTextShaderSources.VERTEX);
        List<Integer> expectedWidths = new ArrayList<>();
        expectedWidths.add(2);
        for (int components : SdfTextShaderSources.INSTANCE_ATTRIBUTE_COMPONENTS) expectedWidths.add(components);
        assertEquals(expectedWidths, declaredWidths);
    }

    @Test
    void textInstanceLocations_doNotCollideWithQuadPosition() {
        assertTrue(SdfTextShaderSources.FIRST_INSTANCE_LOCATION > SdfTextShaderSources.POSITION_LOCATION);
    }

    @Test
    void allSources_targetGlsl150() {
        for (String source : new String[]{SdfShapeShaderSources.VERTEX, SdfShapeShaderSources.FRAGMENT,
                SdfShapeShaderSources.INSTANCED_VERTEX, SdfShapeShaderSources.INSTANCED_FRAGMENT,
                SdfTextShaderSources.VERTEX, SdfTextShaderSources.FRAGMENT}) {
            assertTrue(source.startsWith("#version 150"));
        }
    }

    private static void assertAttributesMatch(String vertex, String[] names, List<Integer> expectedWidths) {
        assertEquals(List.of(names), declaredNames(vertex));
        assertEquals(expectedWidths, declaredWidths(vertex));
    }

    /** Width of every attribute in declaration order: the quad position ({@code quadWidth}) then the instance parts. */
    private static List<Integer> widths(int quadWidth, int[] instanceComponents) {
        List<Integer> result = new ArrayList<>();
        result.add(quadWidth);
        for (int components : instanceComponents) result.add(components);
        return result;
    }

    private static List<String> declaredNames(String source) {
        List<String> names = new ArrayList<>();
        Matcher matcher = ATTRIBUTE.matcher(source);
        while (matcher.find()) names.add(matcher.group(2));
        return names;
    }

    private static List<Integer> declaredWidths(String source) {
        List<Integer> widths = new ArrayList<>();
        Matcher matcher = ATTRIBUTE.matcher(source);
        while (matcher.find()) widths.add(matcher.group(1) == null ? 1 : Integer.parseInt(matcher.group(1)));
        return widths;
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int value : values) total += value;
        return total;
    }
}

package dev.sixik.unigui.api.scene;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Разбор строки блока, общей для всех версий.
 */
class BlockSpecTest {
    @Test
    void parse_idWithProperties_keepsOrderAndText() {
        BlockSpec spec = BlockSpec.parse(" minecraft:furnace[facing=north, lit=true] ");

        assertEquals("minecraft:furnace[facing=north, lit=true]", spec.text());
        assertEquals("minecraft:furnace", spec.id());
        assertEquals(Map.of("facing", "north", "lit", "true"), spec.properties());
        assertEquals(java.util.List.of("facing", "lit"), java.util.List.copyOf(spec.properties().keySet()));
        assertFalse(spec.hasMeta());
    }

    @Test
    void parse_idWithoutNamespace_usesMinecraft() {
        assertEquals("minecraft:stone", BlockSpec.parse("stone").id());
    }

    @Test
    void parse_metadata_isReadAndModIdCaseIsKept() {
        BlockSpec spec = BlockSpec.parse("IC2:blockMachine@3");

        assertEquals("IC2:blockMachine", spec.id());
        assertEquals(3, spec.meta());
        assertTrue(spec.hasMeta());
        assertTrue(spec.properties().isEmpty());
    }

    @Test
    void parse_emptyBrackets_meansNoProperties() {
        assertTrue(BlockSpec.parse("minecraft:stone[]").properties().isEmpty());
    }

    @Test
    void parse_invalidSyntax_isRejected() {
        for (String value : new String[]{"", "  ", "minecraft:furnace[facing=north", "minecraft:furnace[facing]",
                "minecraft:furnace[=north]", "minecraft:furnace[a=1,a=2]", "minecraft:wool@-1", "minecraft:wool@x",
                "minecraft:furnace[facing=north]@3", "a:b:c", ":stone", "minecraft:", "minecraft:stone]",
                "minecraft:stone tail"}) {
            assertThrows(IllegalArgumentException.class, () -> BlockSpec.parse(value), value);
        }
    }

    @Test
    void properties_areImmutable() {
        BlockSpec spec = BlockSpec.parse("minecraft:furnace[lit=true]");
        assertThrows(UnsupportedOperationException.class, () -> spec.properties().put("x", "y"));
    }
}

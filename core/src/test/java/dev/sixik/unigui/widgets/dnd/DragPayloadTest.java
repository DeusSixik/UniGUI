package dev.sixik.unigui.widgets.dnd;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragPayloadTest {
    @Test
    void constructor_blankValues_appliesStableDefaults() {
        DragPayload payload = new DragPayload(" item ", " ", " ");

        assertEquals("item", payload.id());
        assertEquals("generic", payload.type());
        assertEquals("item", payload.preview());
        assertFalse(payload.hasData());
    }

    @Test
    void matchesType_exactAndWildcard_filtersAsExpected() {
        DragPayload payload = DragPayload.of("item", "asset", "Item");

        assertTrue(payload.matchesType("asset"));
        assertTrue(payload.matchesType("*"));
        assertFalse(payload.matchesType("widget"));
    }

    @Test
    void typedData_requestedType_returnsOnlyCompatibleValue() {
        Object value = new Object();
        DragPayload payload = DragPayload.of("item", "asset", "Item", value);

        assertTrue(payload.hasData());
        assertSame(value, payload.data(Object.class));
        assertNull(payload.data(String.class));
        assertNull(payload.data(null));
    }
}

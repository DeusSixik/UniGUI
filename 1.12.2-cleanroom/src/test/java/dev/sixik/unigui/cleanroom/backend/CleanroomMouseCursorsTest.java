package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.input.MouseCursor;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CleanroomMouseCursorsTest {
    // SDL 3 SDL_SystemCursor ids (SDL_mouse.h), spelled out so a wrong mapping cannot hide behind the same constant.
    private static final int DEFAULT = 0;
    private static final int TEXT = 1;
    private static final int CROSSHAIR = 3;
    private static final int NWSE_RESIZE = 5;
    private static final int NESW_RESIZE = 6;
    private static final int EW_RESIZE = 7;
    private static final int NS_RESIZE = 8;
    private static final int POINTER = 11;

    @Test
    void shapes_matchTheSdlSystemCursorIds() {
        assertEquals(DEFAULT, CleanroomMouseCursors.shape(MouseCursor.DEFAULT));
        assertEquals(POINTER, CleanroomMouseCursors.shape(MouseCursor.POINTER));
        assertEquals(TEXT, CleanroomMouseCursors.shape(MouseCursor.TEXT));
        assertEquals(CROSSHAIR, CleanroomMouseCursors.shape(MouseCursor.CROSSHAIR));
        assertEquals(EW_RESIZE, CleanroomMouseCursors.shape(MouseCursor.RESIZE_HORIZONTAL));
        assertEquals(NS_RESIZE, CleanroomMouseCursors.shape(MouseCursor.RESIZE_VERTICAL));
        assertEquals(NWSE_RESIZE, CleanroomMouseCursors.shape(MouseCursor.RESIZE_NWSE));
        assertEquals(NESW_RESIZE, CleanroomMouseCursors.shape(MouseCursor.RESIZE_NESW));
    }

    @Test
    void everyCursorHasItsOwnShape() {
        Set<Integer> shapes = new HashSet<>();
        for (MouseCursor cursor : MouseCursor.values()) shapes.add(CleanroomMouseCursors.shape(cursor));
        assertEquals(MouseCursor.values().length, shapes.size());
    }
}

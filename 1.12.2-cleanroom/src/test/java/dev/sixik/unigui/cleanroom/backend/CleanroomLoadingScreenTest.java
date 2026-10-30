package dev.sixik.unigui.cleanroom.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanroomLoadingScreenTest {
    @AfterEach
    void reset() {
        CleanroomLoadingScreen.end();
    }

    @Test
    void noContextWhenNothingLoads() {
        assertNull(CleanroomLoadingScreen.context());
        assertFalse(CleanroomLoadingScreen.isLoading());
    }

    @Test
    void levelLoadingExposesTitleMessageAndProgress() {
        CleanroomLoadingHooks.levelLoading("Loading level");
        CleanroomLoadingHooks.message("Preparing spawn area");
        CleanroomLoadingHooks.progress(25);
        CleanroomLoadingScreen.Context context = CleanroomLoadingScreen.context();
        assertNotNull(context);
        assertEquals(CleanroomLoadingScreen.Kind.LEVEL_LOADING, context.kind());
        assertEquals("Loading level", context.title());
        assertEquals("Preparing spawn area", context.message());
        assertEquals(0.25f, context.progress(), 1.0e-6f);
    }

    @Test
    void unknownProgressIsMinusOneAndOverflowIsClamped() {
        CleanroomLoadingHooks.levelSaving("Saving level");
        assertEquals(-1.0f, CleanroomLoadingScreen.context().progress());
        CleanroomLoadingHooks.progress(180);
        assertEquals(1.0f, CleanroomLoadingScreen.context().progress());
    }

    @Test
    void beginResetsPreviousMessageAndProgress() {
        CleanroomLoadingHooks.levelLoading("Loading level");
        CleanroomLoadingHooks.message("old");
        CleanroomLoadingHooks.progress(90);
        CleanroomLoadingHooks.levelSaving("Saving level");
        CleanroomLoadingScreen.Context context = CleanroomLoadingScreen.context();
        assertEquals(CleanroomLoadingScreen.Kind.LEVEL_SAVING, context.kind());
        assertEquals("", context.message());
        assertEquals(-1.0f, context.progress());
    }

    @Test
    void doneEndsLevelLoadingButNotTheTerrainScreen() {
        CleanroomLoadingHooks.levelLoading("Loading level");
        CleanroomLoadingHooks.done();
        assertFalse(CleanroomLoadingScreen.isLoading());

        CleanroomLoadingScreen.begin(CleanroomLoadingScreen.Kind.TERRAIN_DOWNLOAD, "");
        CleanroomLoadingHooks.done();
        assertTrue(CleanroomLoadingScreen.isLoading(), "the terrain screen ends when the screen changes");
        CleanroomLoadingHooks.screenChanged();
        assertFalse(CleanroomLoadingScreen.isLoading());
    }

    @Test
    void screenChangeDoesNotEndLevelLoading() {
        CleanroomLoadingHooks.levelLoading("Loading level");
        CleanroomLoadingHooks.screenChanged();
        assertTrue(CleanroomLoadingScreen.isLoading());
    }

    @Test
    void messageWithoutBeginStartsLevelLoading() {
        CleanroomLoadingHooks.message("Building terrain");
        assertEquals(CleanroomLoadingScreen.Kind.LEVEL_LOADING, CleanroomLoadingScreen.context().kind());
    }
}
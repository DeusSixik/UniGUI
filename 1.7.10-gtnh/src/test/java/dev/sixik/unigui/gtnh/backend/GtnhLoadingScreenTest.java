package dev.sixik.unigui.gtnh.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtnhLoadingScreenTest {
    @AfterEach
    void reset() {
        GtnhLoadingScreen.end();
    }

    @Test
    void noContextWhenNothingLoads() {
        assertNull(GtnhLoadingScreen.context());
        assertFalse(GtnhLoadingScreen.isLoading());
    }

    @Test
    void levelLoadingExposesTitleMessageAndProgress() {
        GtnhLoadingHooks.levelLoading("Loading level");
        GtnhLoadingHooks.message("Preparing spawn area");
        GtnhLoadingHooks.progress(25);
        GtnhLoadingScreen.Context context = GtnhLoadingScreen.context();
        assertNotNull(context);
        assertEquals(GtnhLoadingScreen.Kind.LEVEL_LOADING, context.kind());
        assertEquals("Loading level", context.title());
        assertEquals("Preparing spawn area", context.message());
        assertEquals(0.25f, context.progress(), 1.0e-6f);
    }

    @Test
    void unknownProgressIsMinusOneAndOverflowIsClamped() {
        GtnhLoadingHooks.levelSaving("Saving level");
        assertEquals(-1.0f, GtnhLoadingScreen.context().progress());
        GtnhLoadingHooks.progress(180);
        assertEquals(1.0f, GtnhLoadingScreen.context().progress());
    }

    @Test
    void beginResetsPreviousMessageAndProgress() {
        GtnhLoadingHooks.levelLoading("Loading level");
        GtnhLoadingHooks.message("old");
        GtnhLoadingHooks.progress(90);
        GtnhLoadingHooks.levelSaving("Saving level");
        GtnhLoadingScreen.Context context = GtnhLoadingScreen.context();
        assertEquals(GtnhLoadingScreen.Kind.LEVEL_SAVING, context.kind());
        assertEquals("", context.message());
        assertEquals(-1.0f, context.progress());
    }

    @Test
    void doneEndsLevelLoadingButNotTheTerrainScreen() {
        GtnhLoadingHooks.levelLoading("Loading level");
        GtnhLoadingHooks.done();
        assertFalse(GtnhLoadingScreen.isLoading());

        GtnhLoadingScreen.begin(GtnhLoadingScreen.Kind.TERRAIN_DOWNLOAD, "");
        GtnhLoadingHooks.done();
        assertTrue(GtnhLoadingScreen.isLoading(), "the terrain screen ends when the screen changes");
        GtnhLoadingHooks.screenChanged();
        assertFalse(GtnhLoadingScreen.isLoading());
    }

    @Test
    void screenChangeDoesNotEndLevelLoading() {
        GtnhLoadingHooks.levelLoading("Loading level");
        GtnhLoadingHooks.screenChanged();
        assertTrue(GtnhLoadingScreen.isLoading());
    }

    @Test
    void messageWithoutBeginStartsLevelLoading() {
        GtnhLoadingHooks.message("Building terrain");
        assertEquals(GtnhLoadingScreen.Kind.LEVEL_LOADING, GtnhLoadingScreen.context().kind());
    }
}
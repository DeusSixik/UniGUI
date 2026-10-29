package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.screen.UniRenderLayer;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.resource.UniResources;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.widgets.containers.Box;

public final class UniApiSelfTest {
    public static void main(String[] args) {
        check(UniResources.parse("unigui:fonts/ui/default").toString()
                .equals("unigui:fonts/ui/default"), "namespaced resource");
        check(UniResources.parse("textures/gui/widgets").toString()
                .equals("minecraft:textures/gui/widgets"), "default resource namespace");

        expectIllegalArgument(() -> UniResources.parse("Bad:resource"));
        expectIllegalArgument(() -> UniResources.parse("mod:bad path"));
        expectIllegalArgument(() -> new UniScreen(new Box()).uiScale(Float.NaN));
        expectIllegalArgument(() -> new UniRenderLayer(new Box()).uiScale(0.0f));

        UniScreen screen = new UniScreen("Example", new Box()).renderPolicy(UiRenderPolicy.onDirty());
        check(screen.title().equals("Example"), "screen title");
        check(screen.renderPolicy().mode() == UiRenderPolicy.Mode.ON_DIRTY, "screen render policy");

        System.out.println("UniApiSelfTest passed");
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

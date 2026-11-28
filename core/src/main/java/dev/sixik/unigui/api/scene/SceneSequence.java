package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;

import java.util.Objects;

/** A scene and its seekable animation, ready to bind to a viewport or custom controls. */
@WikiPage(title = "SceneSequence", slug = "scene-sequence", category = "<key:scene.category>", order = 176,
        summary = "<key:scene.sequence.summary>")
@WikiSection(title = "<key:scene.sequence.playback.title>", order = 10,
        markdown = "<key:scene.sequence.playback.body>")
@WikiLink(label = "SceneBuilder", target = "page:scene-builder")
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@WikiLink(label = "SceneTimeline", target = "page:scene-timeline")
@WikiLink(label = "ScenePlaybackController", target = "page:scene-playback-controller")
@AiDoc(summary = "Pairs a scene graph and the timeline that animates it.",
        criticalRules = {
                "Use scene() for the viewport and timeline() to configure additional tracks.",
                "newPlaybackController() captures the current duration, applies frame zero and starts paused."
        }, related = {"SceneBuilder", "Scene3D", "SceneTimeline", "ScenePlaybackController"})
public record SceneSequence(
        @WikiParameter(description = "<key:scene.sequence.scene>") Scene scene,
        @WikiParameter(description = "<key:scene.sequence.timeline>") SceneTimeline timeline) {
    /**
     * Creates a sequence from a scene and the timeline that animates it.
     *
     * @param scene scene containing the animated nodes
     * @param timeline timeline used to sample a frame
     * @throws NullPointerException if the scene or timeline is null
     */
    @WikiMethod(order = 10, summary = "<key:scene.sequence.create>")
    @WikiThrows(type = NullPointerException.class, description = "<key:scene.sequence.null>")
    @AiDoc(order = 10, summary = "Pairs a nonnull scene and timeline.")
    public SceneSequence {
        Objects.requireNonNull(scene, "scene");
        Objects.requireNonNull(timeline, "timeline");
    }

    /**
     * Applies the first frame and creates a controller paused at zero using the current timeline duration.
     *
     * @return controller for this sequence's scene and timeline
     */
    @WikiMethod(order = 20, summary = "<key:scene.sequence.controller>")
    @WikiReturn("<key:scene.sequence.controller.return>")
    @AiDoc(order = 20, summary = "Applies frame zero and returns a paused controller with the current timeline duration.")
    public ScenePlaybackController newPlaybackController() {
        var playback = new ScenePlaybackController(timeline.duration(), timeline::sample);
        playback.seek(0);
        return playback;
    }
}

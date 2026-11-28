package dev.sixik.unigui.api.scene;

import java.util.Objects;

/** A scene and its seekable animation, ready to bind to a viewport or custom controls. */
public record SceneSequence(Scene scene, SceneTimeline timeline) {
    /**
     * Creates a sequence from a scene and the timeline that animates it.
     *
     * @param scene scene containing the animated nodes
     * @param timeline timeline used to sample a frame
     * @throws NullPointerException if the scene or timeline is null
     */
    public SceneSequence {
        Objects.requireNonNull(scene, "scene");
        Objects.requireNonNull(timeline, "timeline");
    }

    /**
     * Applies the first frame and creates a controller paused at zero using the current timeline duration.
     *
     * @return controller for this sequence's scene and timeline
     */
    public ScenePlaybackController newPlaybackController() {
        var playback = new ScenePlaybackController(timeline.duration(), timeline::sample);
        playback.seek(0);
        return playback;
    }
}

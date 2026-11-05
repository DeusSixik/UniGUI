package dev.sixik.unigui.api.animation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

import java.util.List;

/**
 * Неизменяемое описание набора синхронно проигрываемых property-track'ов.
 */
public final class Storyboard {
    private final ObjectList<PropertyTrack<?>> tracks;
    private final float durationSeconds;

    public Storyboard(List<? extends PropertyTrack<?>> tracks) {
        ObjectArrayList<PropertyTrack<?>> copy = new ObjectArrayList<>();
        if (tracks != null) copy.addAll(tracks);
        float duration = 0.0f;
        for (PropertyTrack<?> track : copy) {
            if (track == null) throw new IllegalArgumentException("Storyboard не поддерживает null track.");
            duration = Math.max(duration, track.durationSeconds());
        }
        this.tracks = ObjectLists.unmodifiable(copy);
        this.durationSeconds = duration;
    }

    public static Storyboard of(PropertyTrack<?>... tracks) {
        ObjectArrayList<PropertyTrack<?>> values = new ObjectArrayList<>();
        if (tracks != null) {
            for (PropertyTrack<?> track : tracks) values.add(track);
        }
        return new Storyboard(values);
    }

    public ObjectList<PropertyTrack<?>> tracks() { return tracks; }

    public float durationSeconds() { return durationSeconds; }

    public boolean empty() { return tracks.isEmpty(); }
}

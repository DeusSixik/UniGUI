package dev.sixik.unigui.api.debug;

import java.util.Collections;
import java.util.List;

public final class ProfilerSnapshot {
    public static final ProfilerSnapshot EMPTY = new ProfilerSnapshot(0L, Collections.emptyList());

    private final long frameIndex;
    private final List<ProfileScopeSnapshot> scopes;

    public ProfilerSnapshot(long frameIndex, List<ProfileScopeSnapshot> scopes) {
        this.frameIndex = frameIndex;
        this.scopes = scopes == null ? Collections.emptyList() : List.copyOf(scopes);
    }

    public long frameIndex() {
        return frameIndex;
    }

    public List<ProfileScopeSnapshot> scopes() {
        return scopes;
    }
}

package dev.sixik.unigui.impl.debug;

import dev.sixik.unigui.api.debug.ProfileScope;
import dev.sixik.unigui.api.debug.ProfileScopeSnapshot;
import dev.sixik.unigui.api.debug.ProfilerSnapshot;
import dev.sixik.unigui.api.debug.UiProfiler;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FrameProfiler implements UiProfiler {
    private final Map<String, MutableScopeStats> scopes = new LinkedHashMap<>();
    private long frameIndex;

    @Override
    public void beginFrame(long frameIndex) {
        this.frameIndex = frameIndex;
        scopes.clear();
    }

    @Override
    public ProfileScope scope(String name) {
        return new ActiveScope(normalize(name));
    }

    @Override
    public ProfilerSnapshot snapshot() {
        List<ProfileScopeSnapshot> snapshots = new ObjectArrayList<>(scopes.size());
        for (MutableScopeStats stats : scopes.values()) {
            snapshots.add(new ProfileScopeSnapshot(stats.name, stats.calls, stats.totalNanos));
        }
        return new ProfilerSnapshot(frameIndex, snapshots);
    }

    private void record(String name, long nanos) {
        MutableScopeStats stats = scopes.computeIfAbsent(name, MutableScopeStats::new);
        stats.calls++;
        stats.totalNanos += Math.max(0L, nanos);
    }

    private static String normalize(String name) {
        return name == null || name.isBlank() ? "scope" : name;
    }

    private final class ActiveScope implements ProfileScope {
        private final String name;
        private final long startNanos = System.nanoTime();
        private boolean closed;

        private ActiveScope(String name) {
            this.name = name;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            record(name, System.nanoTime() - startNanos);
        }
    }

    private static final class MutableScopeStats {
        private final String name;
        private int calls;
        private long totalNanos;

        private MutableScopeStats(String name) {
            this.name = name;
        }
    }
}

package dev.sixik.unigui.api.debug;

public final class ProfileScopeSnapshot {
    private final String name;
    private final int calls;
    private final long totalNanos;

    public ProfileScopeSnapshot(String name, int calls, long totalNanos) {
        this.name = name == null || name.isBlank() ? "scope" : name;
        this.calls = Math.max(0, calls);
        this.totalNanos = Math.max(0L, totalNanos);
    }

    public String name() {
        return name;
    }

    public int calls() {
        return calls;
    }

    public long totalNanos() {
        return totalNanos;
    }

    public float totalMillis() {
        return totalNanos / 1_000_000.0f;
    }
}

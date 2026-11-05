package dev.sixik.unigui.api.debug;

public interface ProfileScope extends AutoCloseable {
    ProfileScope NOOP = new ProfileScope() {
        @Override
        public void close() {
        }
    };

    @Override
    void close();
}

package dev.sixik.unigui.api.backend;

import dev.sixik.unigui.api.screen.UniScreen;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UniGuiRuntimeLoggerTest {
    @Test
    void logger_createdBeforeBackendInstall_usesInstalledBackend() {
        UniGuiLogger logger = UniGuiRuntime.logger(UniGuiRuntimeLoggerTest.class);
        List<String> calls = new ArrayList<>();
        try {
            UniGuiRuntime.install(new TestBackend(calls));

            logger.warn("message {}", "value");

            assertEquals(List.of("WARN:message {}:value"), calls);
        } finally {
            UniGuiRuntime.uninstall();
        }
    }

    private static final class TestBackend implements UniGuiBackend {
        private final List<String> calls;

        private TestBackend(List<String> calls) {
            this.calls = calls;
        }

        @Override
        public void openScreen(UniScreen screen) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UniGuiLogger logger(String name) {
            return new UniGuiLogger() {
                @Override public void debug(String message, Object... arguments) { record("DEBUG", message, arguments); }
                @Override public void info(String message, Object... arguments) { record("INFO", message, arguments); }
                @Override public void warn(String message, Object... arguments) { record("WARN", message, arguments); }
                @Override public void error(String message, Object... arguments) { record("ERROR", message, arguments); }

                private void record(String level, String message, Object... arguments) {
                    calls.add(level + ":" + message + ":" + arguments[0]);
                }
            };
        }
    }
}

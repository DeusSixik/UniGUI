package dev.sixik.unigui.api.backend;

/**
 * Logging facade whose implementation is supplied by the active game backend.
 */
public interface UniGuiLogger {
    void debug(String message, Object... arguments);

    void info(String message, Object... arguments);

    void warn(String message, Object... arguments);

    void error(String message, Object... arguments);
}

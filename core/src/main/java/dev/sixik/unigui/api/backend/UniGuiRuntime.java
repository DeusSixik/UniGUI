package dev.sixik.unigui.api.backend;

import java.util.Objects;

/**
 * Holds the backend installed by the active loader/version module.
 */
public final class UniGuiRuntime {
    private static volatile UniGuiBackend backend;
    private static final java.util.concurrent.ConcurrentMap<String, UniGuiLogger> LOGGERS =
            new java.util.concurrent.ConcurrentHashMap<>();

    private UniGuiRuntime() {
    }

    public static void install(UniGuiBackend backend) {
        UniGuiRuntime.backend = Objects.requireNonNull(backend, "backend");
    }

    static void uninstall() {
        backend = null;
    }

    public static UniGuiBackend backend() {
        UniGuiBackend current = backend;
        if (current == null) {
            throw new IllegalStateException("UniGUI backend has not been installed by the client runtime");
        }
        return current;
    }

    /**
     * Returns a logger that follows the backend installed by the active runtime.
     */
    public static UniGuiLogger logger(Class<?> owner) {
        String name = Objects.requireNonNull(owner, "owner").getName();
        return LOGGERS.computeIfAbsent(name, UniGuiRuntime::createLogger);
    }

    private static UniGuiLogger createLogger(String name) {
        return new UniGuiLogger() {
            private volatile UniGuiBackend lastBackend;
            private volatile UniGuiLogger delegate;

            private UniGuiLogger current() {
                UniGuiBackend currentBackend = backend;
                if (currentBackend == null) return fallbackLogger(name);
                if (currentBackend != lastBackend) {
                    synchronized (this) {
                        if (currentBackend != lastBackend) {
                            delegate = currentBackend.logger(name);
                            lastBackend = currentBackend;
                        }
                    }
                }
                return delegate;
            }

            @Override public void debug(String message, Object... arguments) { current().debug(message, arguments); }
            @Override public void info(String message, Object... arguments) { current().info(message, arguments); }
            @Override public void warn(String message, Object... arguments) { current().warn(message, arguments); }
            @Override public void error(String message, Object... arguments) { current().error(message, arguments); }
        };
    }

    static UniGuiLogger fallbackLogger(String name) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(name);
        return new UniGuiLogger() {
            @Override public void debug(String message, Object... arguments) { log(java.util.logging.Level.FINE, message, arguments); }
            @Override public void info(String message, Object... arguments) { log(java.util.logging.Level.INFO, message, arguments); }
            @Override public void warn(String message, Object... arguments) { log(java.util.logging.Level.WARNING, message, arguments); }
            @Override public void error(String message, Object... arguments) { log(java.util.logging.Level.SEVERE, message, arguments); }

            private void log(java.util.logging.Level level, String message, Object... arguments) {
                Throwable failure = arguments.length > 0 && arguments[arguments.length - 1] instanceof Throwable
                        ? (Throwable) arguments[arguments.length - 1] : null;
                int valueCount = arguments.length - (failure == null ? 0 : 1);
                StringBuilder formatted = new StringBuilder();
                int from = 0;
                for (int i = 0; i < valueCount; i++) {
                    int placeholder = message.indexOf("{}", from);
                    if (placeholder < 0) break;
                    formatted.append(message, from, placeholder).append(String.valueOf(arguments[i]));
                    from = placeholder + 2;
                }
                formatted.append(message.substring(from));
                if (logger.isLoggable(level)) logger.log(level, formatted.toString(), failure);
            }
        };
    }
}

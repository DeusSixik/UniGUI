package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.backend.UniGuiLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class Log4j2UniGuiLogger implements UniGuiLogger {
    private final Logger logger;

    Log4j2UniGuiLogger(String name) {
        this.logger = LogManager.getLogger(name);
    }

    @Override public void debug(String message, Object... arguments) { logger.debug(message, arguments); }
    @Override public void info(String message, Object... arguments) { logger.info(message, arguments); }
    @Override public void warn(String message, Object... arguments) { logger.warn(message, arguments); }
    @Override public void error(String message, Object... arguments) { logger.error(message, arguments); }
}

package dev.sixik.unigui.api.core;

/**
 * Поставщик масштаба между логическими UI-пикселями и backend/framebuffer пикселями.
 *
 * <p>UniGUI рассчитывает layout в логических координатах. Render backend может работать в другом
 * масштабе: Minecraft GUI scale, физическое разрешение окна, DPI или собственный canvas scaler.
 * {@code UIScaleProvider} централизует это преобразование, чтобы текст, scissor и render bounds
 * не расходились на разных разрешениях.</p>
 *
 * <p>Реализация может быть статичной ({@link #fixed(float)}) или зависеть от viewport'а
 * ({@link UnityLikeUIScaleProvider}). Значение масштаба всегда нормализуется: невалидный или
 * неположительный scale трактуется как {@code 1.0f}.</p>
 */
public interface UIScaleProvider {
    /** Поставщик без масштабирования: 1 логический пиксель равен 1 backend пикселю. */
    UIScaleProvider IDENTITY = new UIScaleProvider() {
        @Override
        public float scale() {
            return 1.0f;
        }
    };

    /**
     * Возвращает текущий коэффициент масштаба.
     *
     * @return положительный scale, где {@code logical * scale = backend pixels}
     */
    float scale();

    /**
     * Создаёт provider с постоянным scale.
     *
     * @param scale желаемый scale; невалидные значения заменяются на {@code 1.0f}
     * @return immutable provider с нормализованным scale
     */
    static UIScaleProvider fixed(float scale) {
        float normalized = sanitize(scale);
        return () -> normalized;
    }

    /**
     * Переводит логические UI-пиксели в backend pixels.
     *
     * @param logicalPixels значение в координатах UniGUI layout/render API
     * @return значение в пикселях backend'а
     */
    default float toBackendPixels(float logicalPixels) {
        return logicalPixels * sanitize(scale());
    }

    /**
     * Сообщает provider'у текущий размер viewport'а.
     *
     * <p>Статичные реализации могут игнорировать вызов. Адаптивные реализации используют его,
     * чтобы пересчитать scale для нового разрешения окна или framebuffer'а.</p>
     *
     * @param width ширина viewport'а в backend pixels
     * @param height высота viewport'а в backend pixels
     */
    default void viewportSize(float width, float height) {
    }

    /**
     * Переводит backend pixels обратно в логические UI-пиксели.
     *
     * @param backendPixels значение в координатах render backend'а
     * @return значение в логических координатах UniGUI
     */
    default float toLogicalPixels(float backendPixels) {
        return backendPixels / sanitize(scale());
    }

    /**
     * Нормализует scale для безопасного деления и умножения.
     *
     * @param scale исходное значение
     * @return {@code scale}, если оно конечное и положительное, иначе {@code 1.0f}
     */
    static float sanitize(float scale) {
        return Float.isFinite(scale) && scale > 0.0f ? scale : 1.0f;
    }
}
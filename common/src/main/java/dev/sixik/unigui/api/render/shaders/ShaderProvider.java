package dev.sixik.unigui.api.render.shaders;

import java.util.Optional;

/**
 * Разрешает shader source code для backend-specific или application-specific shader id.
 *
 * <p>Provider можно зарегистрировать вручную через {@link ShaderProviders#register(ShaderProvider)}
 * или обнаружить через Java {@link java.util.ServiceLoader}, добавив файл
 * {@code META-INF/services/dev.sixik.unigui.api.render.shaders.ShaderProvider}.</p>
 */
public interface ShaderProvider {
    /**
     * Загружает source для shader handle.
     *
     * @param handle handle шейдера
     * @return source или {@link Optional#empty()}, если provider не знает этот shader
     */
    Optional<ShaderSource> load(ShaderHandle handle);

    /**
     * Загружает source без промежуточного {@link Optional}.
     *
     * <p>Метод нужен backend'ам, работающим в горячем render path. Реализации,
     * которые поддерживают только старый метод {@link #load(ShaderHandle)},
     * получают совместимый fallback с преобразованием {@code Optional} в
     * {@code null}.</p>
     *
     * @param handle handle шейдера
     * @return source или {@code null}, если provider не знает этот shader
     */
    default ShaderSource loadOrNull(ShaderHandle handle) {
        Optional<ShaderSource> source = load(handle);
        return source == null ? null : source.orElse(null);
    }
}

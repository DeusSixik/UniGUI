package dev.sixik.unigui.api.style;

import dev.sixik.unigui.xaml.UiDiagnostic;
import dev.sixik.unigui.xaml.load.XamlLoadException;

import java.util.List;
import java.util.Optional;

/**
 * Результат tolerant-загрузки {@link StylePack} из XML для редактора и hot-reload сценариев.
 *
 * <p>В отличие от strict {@link StylePackXml#parse(String)}, editor path должен вернуть частично
 * загруженный pack даже при ошибках, чтобы пользователь видел максимум доступных данных и список
 * diagnostics. Метод {@link #throwIfDiagnostics()} превращает результат обратно в strict-поведение.</p>
 *
 * @param pack загруженный или частично загруженный style pack
 * @param diagnostics diagnostics, найденные при загрузке
 */
public record StylePackResult(StylePack pack, List<UiDiagnostic> diagnostics) {
    /** Нормализует отсутствующий pack и diagnostics. */
    public StylePackResult {
        pack = pack == null ? StylePack.create("style-pack") : pack;
        diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    /**
     * @return {@code true}, если загрузка вернула хотя бы один diagnostic
     */
    public boolean hasDiagnostics() {
        return !diagnostics.isEmpty();
    }

    /**
     * @return {@code true}, если diagnostics отсутствуют
     */
    public boolean valid() {
        return diagnostics.isEmpty();
    }

    /**
     * @return первый diagnostic или {@link Optional#empty()}
     */
    public Optional<UiDiagnostic> firstDiagnostic() {
        return diagnostics.isEmpty() ? Optional.empty() : Optional.of(diagnostics.get(0));
    }

    /**
     * @return только текстовые сообщения diagnostics
     */
    public List<String> diagnosticMessages() {
        return diagnostics.stream().map(UiDiagnostic::message).toList();
    }

    /**
     * Бросает {@link XamlLoadException}, если результат содержит diagnostics.
     *
     * @return этот результат, если diagnostics отсутствуют
     */
    public StylePackResult throwIfDiagnostics() {
        if (hasDiagnostics()) {
            throw new XamlLoadException(diagnostics);
        }
        return this;
    }
}
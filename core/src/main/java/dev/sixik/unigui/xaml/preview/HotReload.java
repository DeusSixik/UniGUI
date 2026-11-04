package dev.sixik.unigui.xaml.preview;

import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiParseException;
import dev.sixik.unigui.xaml.edit.DocumentSource;
import dev.sixik.unigui.xaml.format.XamlFormat;

import java.util.Objects;
import java.util.Optional;

/**
 * Горячая перезагрузка: следит за источником документа и обновляет {@link Preview}, когда текст изменился.
 *
 * <p>Класс сам ничего не запускает по таймеру: хост (экран игры, редактор) вызывает {@link #poll()} раз в кадр или
 * раз в секунду. Опрос дёшев — сравнивается только прочитанный текст — и не бросает исключений: ошибки чтения и
 * разбора становятся результатом, а предпросмотр сохраняет последнее удачное дерево.</p>
 */
public final class HotReload {
    private final DocumentSource source;
    private final Preview preview;
    private String lastText;

    /**
     * Создаёт наблюдателя и сразу показывает текущее содержимое источника.
     *
     * @param source  источник документа (файл, память)
     * @param preview предпросмотр, который обновляется
     */
    public HotReload(DocumentSource source, Preview preview) {
        this.source = Objects.requireNonNull(source, "source");
        this.preview = Objects.requireNonNull(preview, "preview");
        poll();
    }

    /**
     * Проверяет источник и, если текст изменился с прошлого раза, пересобирает предпросмотр.
     *
     * @return результат пересборки или пусто, если текст не менялся
     */
    public Optional<Preview.Result> poll() {
        String text;
        try {
            text = source.readText();
        } catch (RuntimeException failure) {
            // Файл могли заменить именно сейчас: пробуем снова при следующем опросе, прежнее дерево остаётся.
            return Optional.of(new Preview.Result(false, "Cannot read " + source.title() + ": " + failure.getMessage()));
        }
        if (text.equals(lastText)) return Optional.empty();
        lastText = text;
        UiDocument document;
        try {
            document = XamlFormat.INSTANCE.read(text);
        } catch (UiParseException failure) {
            return Optional.of(new Preview.Result(false,
                    "Cannot parse " + source.title() + ": " + failure.reason() + " (" + failure.location() + ")"));
        }
        return Optional.of(preview.show(document));
    }

    /** @return источник, за которым идёт наблюдение */
    public DocumentSource source() {
        return source;
    }
}

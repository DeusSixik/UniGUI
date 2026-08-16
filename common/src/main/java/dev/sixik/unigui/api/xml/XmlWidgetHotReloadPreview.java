package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.PanelWidget;

import java.util.List;
import java.util.function.Consumer;

/**
 * Обёртка предпросмотра с одним дочерним виджетом, которая перезагружает XML при изменении версии источника.
 *
 * <p>Preview хранит последний успешно созданный widget. Если следующая reload-попытка падает,
 * старый widget остаётся на экране, а статус получает diagnostics. Это делает hot-reload удобным
 * для редактора: пользователь видит ошибку, но не теряет последнюю валидную сцену.</p>
 *
 * @param <T> ожидаемый тип корневого widget-а
 */
public final class XmlWidgetHotReloadPreview<T extends Widget> extends PanelWidget {
    private final XmlWidgetHotReloadSource source;
    private final Class<T> widgetType;
    private XmlWidgetRegistry registry = XMLWidget.registry();
    private XmlWidgetOptions options = XmlWidgetOptions.DEFAULT;
    private Consumer<? super T> reloadHandler = widget -> {};
    private Consumer<Status> statusHandler = status -> {};
    private T content;
    private Status status;
    private long sourceVersion = Long.MIN_VALUE;
    private int reloadCount;
    private boolean autoReload = true;
    private float reloadIntervalSeconds = 0.5f;
    private float reloadElapsedSeconds;

    /**
     * Создаёт hot-reload preview для указанного источника и типа root widget-а.
     *
     * @param source источник XML; не может быть {@code null}
     * @param widgetType ожидаемый тип root widget-а; не может быть {@code null}
     */
    public XmlWidgetHotReloadPreview(XmlWidgetHotReloadSource source, Class<T> widgetType) {
        if (source == null) throw new IllegalArgumentException("XML hot reload source must not be null");
        if (widgetType == null) throw new IllegalArgumentException("XML hot reload widget type must not be null");
        this.source = source;
        this.widgetType = widgetType;
        this.status = Status.waiting(source.label());
    }

    /**
     * Возвращает источник XML.
     *
     * @return hot-reload source
     */
    public XmlWidgetHotReloadSource source() {
        return source;
    }

    /**
     * Возвращает текущий успешно загруженный widget.
     *
     * @return root widget или {@code null}, если reload ещё не был успешным
     */
    public T content() {
        return content;
    }

    /**
     * Возвращает текущий статус preview.
     *
     * @return status загрузки или ошибки
     */
    public Status status() {
        return status;
    }

    /**
     * Задаёт XML-реестр для создания runtime widget-ов.
     *
     * @param registry реестр виджетов; {@code null} заменяется дефолтным registry
     * @return этот preview для fluent-настройки
     */
    public XmlWidgetHotReloadPreview<T> registry(XmlWidgetRegistry registry) {
        this.registry = registry == null ? XMLWidget.registry() : registry;
        return this;
    }

    /**
     * Задаёт options загрузки XML.
     *
     * @param options настройки загрузки; {@code null} заменяется {@link XmlWidgetOptions#DEFAULT}
     * @return этот preview для fluent-настройки
     */
    public XmlWidgetHotReloadPreview<T> options(XmlWidgetOptions options) {
        this.options = options == null ? XmlWidgetOptions.DEFAULT : options;
        return this;
    }

    /**
     * Задаёт callback успешной перезагрузки.
     *
     * <p>Callback вызывается до замены текущего content-а, чтобы вызывающий код мог дополнительно
     * настроить новый widget.</p>
     *
     * @param reloadHandler callback нового widget-а; {@code null} отключает callback
     * @return этот preview для fluent-настройки
     */
    public XmlWidgetHotReloadPreview<T> onReload(Consumer<? super T> reloadHandler) {
        this.reloadHandler = reloadHandler == null ? widget -> {} : reloadHandler;
        return this;
    }

    /**
     * Задаёт callback изменения статуса.
     *
     * @param statusHandler callback status-а; {@code null} отключает callback
     * @return этот preview для fluent-настройки
     */
    public XmlWidgetHotReloadPreview<T> onStatus(Consumer<Status> statusHandler) {
        this.statusHandler = statusHandler == null ? status -> {} : statusHandler;
        return this;
    }

    /**
     * Включает или выключает автоматическую проверку версии в {@link #tick(FrameContext)}.
     *
     * @param autoReload {@code true}, чтобы preview сам проверял source
     * @return этот preview для fluent-настройки
     */
    public XmlWidgetHotReloadPreview<T> autoReload(boolean autoReload) {
        this.autoReload = autoReload;
        return this;
    }

    /**
     * Возвращает флаг автоматической перезагрузки.
     *
     * @return {@code true}, если auto-reload включён
     */
    public boolean autoReload() {
        return autoReload;
    }

    /**
     * Задаёт интервал проверки source version.
     *
     * @param reloadIntervalSeconds интервал в секундах; минимальное значение 0.05
     * @return этот preview для fluent-настройки
     */
    public XmlWidgetHotReloadPreview<T> reloadIntervalSeconds(float reloadIntervalSeconds) {
        this.reloadIntervalSeconds = Float.isFinite(reloadIntervalSeconds)
                ? Math.max(0.05f, reloadIntervalSeconds)
                : 0.5f;
        return this;
    }

    /**
     * Возвращает интервал автоматической проверки source version.
     *
     * @return интервал в секундах
     */
    public float reloadIntervalSeconds() {
        return reloadIntervalSeconds;
    }

    /**
     * Проверяет версию источника и перезагружает XML только при изменении.
     *
     * @return актуальный status после проверки
     */
    public Status checkForReload() {
        try {
            long version = source.version();
            if (content == null || version != sourceVersion) {
                return reloadNow(version);
            }
            return status;
        } catch (RuntimeException failure) {
            return setStatus(Status.failed(source.label(), sourceVersion, reloadCount, content != null, failure));
        }
    }

    /**
     * Принудительно перезагружает XML независимо от версии.
     *
     * @return status после reload-попытки
     */
    public Status reloadNow() {
        try {
            return reloadNow(source.version());
        } catch (RuntimeException failure) {
            return setStatus(Status.failed(source.label(), sourceVersion, reloadCount, content != null, failure));
        }
    }

    /**
     * Обновляет auto-reload таймер и затем передаёт tick дочерним виджетам.
     *
     * @param frame frame context текущего кадра
     */
    @Override
    public void tick(FrameContext frame) {
        if (visibility() != Visibility.VISIBLE) return;
        if (autoReload && frame != null) {
            reloadElapsedSeconds += Math.max(0.0f, frame.deltaSeconds());
            if (reloadElapsedSeconds >= reloadIntervalSeconds) {
                reloadElapsedSeconds = 0.0f;
                try {
                    checkForReload();
                } catch (RuntimeException failure) {
                    setStatus(Status.failed(source.label(), sourceVersion, reloadCount, content != null, failure));
                }
            }
        }
        super.tick(frame);
    }

    private Status reloadNow(long version) {
        try {
            T next = XMLWidget.create(source.read(), widgetType, registry, options);
            reloadHandler.accept(next);
            replaceContent(next);
            sourceVersion = version;
            reloadCount++;
            return setStatus(Status.loaded(source.label(), sourceVersion, reloadCount));
        } catch (RuntimeException failure) {
            return setStatus(Status.failed(source.label(), sourceVersion, reloadCount, content != null, failure));
        }
    }

    private void replaceContent(T next) {
        T previous = content;
        if (previous != null) removeChild(previous);
        content = next;
        addChild(next);
        applyQueuedMutations();
        if (previous != null) previous.dispose();
    }

    private Status setStatus(Status status) {
        this.status = status;
        statusHandler.accept(status);
        return status;
    }

    /**
     * Snapshot состояния hot-reload preview.
     *
     * @param loaded есть ли валидный content для отображения
     * @param failed завершилась ли последняя reload-попытка ошибкой
     * @param sourceLabel подпись источника XML
     * @param sourceVersion версия source, с которой работала последняя попытка
     * @param reloadCount количество успешных reload-ов
     * @param message короткое сообщение для UI
     * @param diagnostics diagnostics последней ошибки или пустой список
     */
    public record Status(
            boolean loaded,
            boolean failed,
            String sourceLabel,
            long sourceVersion,
            int reloadCount,
            String message,
            List<XmlWidgetDiagnostic> diagnostics) {
        public Status {
            sourceLabel = sourceLabel == null ? "XML source" : sourceLabel;
            message = message == null ? "" : message;
            diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
        }

        /**
         * Создаёт начальный статус до первой reload-попытки.
         *
         * @param sourceLabel подпись источника
         * @return waiting status
         */
        public static Status waiting(String sourceLabel) {
            return new Status(false, false, sourceLabel, Long.MIN_VALUE, 0, "Waiting for XML reload.", List.of());
        }

        /**
         * Создаёт статус успешной перезагрузки.
         *
         * @param sourceLabel подпись источника
         * @param sourceVersion версия source
         * @param reloadCount количество успешных reload-ов
         * @return loaded status
         */
        public static Status loaded(String sourceLabel, long sourceVersion, int reloadCount) {
            return new Status(true, false, sourceLabel, sourceVersion, reloadCount,
                    "Reloaded XML preview " + reloadCount + " time" + (reloadCount == 1 ? "" : "s") + ".",
                    List.of());
        }

        /**
         * Создаёт статус неуспешной reload-попытки.
         *
         * @param sourceLabel подпись источника
         * @param sourceVersion последняя известная версия source
         * @param reloadCount количество успешных reload-ов до ошибки
         * @param hasContent есть ли предыдущий валидный content
         * @param failure исключение загрузки
         * @return failed status с diagnostics
         */
        public static Status failed(String sourceLabel,
                                    long sourceVersion,
                                    int reloadCount,
                                    boolean hasContent,
                                    RuntimeException failure) {
            List<XmlWidgetDiagnostic> diagnostics = failure instanceof XmlWidgetLoadException loadFailure
                    ? loadFailure.diagnostics()
                    : List.of(new XmlWidgetDiagnostic(failure == null ? "XML hot reload failed." : failure.getMessage()));
            String message = diagnostics.isEmpty()
                    ? "XML hot reload failed."
                    : diagnostics.get(0).toString();
            if (hasContent) message += " Keeping last valid preview.";
            return new Status(hasContent, true, sourceLabel, sourceVersion, reloadCount, message, diagnostics);
        }

        /**
         * Проверяет, что последняя reload-попытка не завершилась ошибкой.
         *
         * @return {@code true}, если status не failed
         */
        public boolean valid() {
            return !failed;
        }

        /**
         * Проверяет наличие diagnostics.
         *
         * @return {@code true}, если diagnostics не пусты
         */
        public boolean hasDiagnostics() {
            return !diagnostics.isEmpty();
        }
    }
}

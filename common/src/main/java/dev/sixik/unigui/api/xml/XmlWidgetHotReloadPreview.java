package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.PanelWidget;

import java.util.List;
import java.util.function.Consumer;

/** Обертка предпросмотра с одним дочерним виджетом, которая перезагружает XML при изменении версии источника. */
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

    public XmlWidgetHotReloadPreview(XmlWidgetHotReloadSource source, Class<T> widgetType) {
        if (source == null) throw new IllegalArgumentException("XML hot reload source must not be null");
        if (widgetType == null) throw new IllegalArgumentException("XML hot reload widget type must not be null");
        this.source = source;
        this.widgetType = widgetType;
        this.status = Status.waiting(source.label());
    }

    public XmlWidgetHotReloadSource source() {
        return source;
    }

    public T content() {
        return content;
    }

    public Status status() {
        return status;
    }

    public XmlWidgetHotReloadPreview<T> registry(XmlWidgetRegistry registry) {
        this.registry = registry == null ? XMLWidget.registry() : registry;
        return this;
    }

    public XmlWidgetHotReloadPreview<T> options(XmlWidgetOptions options) {
        this.options = options == null ? XmlWidgetOptions.DEFAULT : options;
        return this;
    }

    public XmlWidgetHotReloadPreview<T> onReload(Consumer<? super T> reloadHandler) {
        this.reloadHandler = reloadHandler == null ? widget -> {} : reloadHandler;
        return this;
    }

    public XmlWidgetHotReloadPreview<T> onStatus(Consumer<Status> statusHandler) {
        this.statusHandler = statusHandler == null ? status -> {} : statusHandler;
        return this;
    }

    public XmlWidgetHotReloadPreview<T> autoReload(boolean autoReload) {
        this.autoReload = autoReload;
        return this;
    }

    public boolean autoReload() {
        return autoReload;
    }

    public XmlWidgetHotReloadPreview<T> reloadIntervalSeconds(float reloadIntervalSeconds) {
        this.reloadIntervalSeconds = Float.isFinite(reloadIntervalSeconds)
                ? Math.max(0.05f, reloadIntervalSeconds)
                : 0.5f;
        return this;
    }

    public float reloadIntervalSeconds() {
        return reloadIntervalSeconds;
    }

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

    public Status reloadNow() {
        try {
            return reloadNow(source.version());
        } catch (RuntimeException failure) {
            return setStatus(Status.failed(source.label(), sourceVersion, reloadCount, content != null, failure));
        }
    }

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

        public static Status waiting(String sourceLabel) {
            return new Status(false, false, sourceLabel, Long.MIN_VALUE, 0, "Waiting for XML reload.", List.of());
        }

        public static Status loaded(String sourceLabel, long sourceVersion, int reloadCount) {
            return new Status(true, false, sourceLabel, sourceVersion, reloadCount,
                    "Reloaded XML preview " + reloadCount + " time" + (reloadCount == 1 ? "" : "s") + ".",
                    List.of());
        }

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

        public boolean valid() {
            return !failed;
        }

        public boolean hasDiagnostics() {
            return !diagnostics.isEmpty();
        }
    }
}

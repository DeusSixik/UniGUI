package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIScaleProvider;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XMLWidget;
import dev.sixik.unigui.api.xml.XmlWidgetLoadException;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.xml.XmlWidgetOptions;
import dev.sixik.unigui.api.xml.XmlWidgetScreen;
import dev.sixik.unigui.api.xml.XmlWidgetSerializationOptions;
import dev.sixik.unigui.api.xml.editor.XmlEditorDiagnosticChannel;
import dev.sixik.unigui.api.xml.editor.XmlEditorMode;
import dev.sixik.unigui.api.xml.editor.XmlEditorSession;
import dev.sixik.unigui.api.xml.editor.XmlEditorSessionChange;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.StackPanel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Runtime/play-mode host for the current XML editor snapshot. */
@XmlWidgetName("XmlRuntimeViewPane")
public final class XmlRuntimeViewPane extends PanelWidget {
    private static final XmlWidgetSerializationOptions RUNTIME_XML =
            XmlWidgetSerializationOptions.COMPACT.xmlDeclaration(false);

    private final Box background = new Box();
    private final StackPanel runtimeHost = new StackPanel();
    private XmlEditorSession session;
    private EventSubscription sessionSubscription;
    private XmlWidgetScreen<Widget> runtimeScreen;
    private Widget runtimeRoot;
    private String runtimeXml = "";
    private String runtimeError = "";
    private boolean running;

    public XmlRuntimeViewPane() {
        background.themeEnabled(false)
                .backgroundVisible(true)
                .borderVisible(true)
                .radius(4.0f);
        background.background().set(0.012f, 0.015f, 0.020f, 0.98f);
        background.borderColor().set(0.16f, 0.24f, 0.32f, 0.88f);

        stretch(background);
        stretch(runtimeHost);
        addChild(background);
        addChild(runtimeHost);
        applyQueuedMutations();
    }

    public Optional<XmlEditorSession> session() {
        return Optional.ofNullable(session);
    }

    public XmlRuntimeViewPane session(XmlEditorSession session) {
        if (this.session == session) return this;
        if (sessionSubscription != null) {
            sessionSubscription.close();
            sessionSubscription = null;
        }
        this.session = session;
        if (session != null) {
            sessionSubscription = session.onChanged(change -> {
                if (change.kind() == XmlEditorSessionChange.Kind.RUNTIME_OPTIONS_CHANGED
                        && session.mode() == XmlEditorMode.RUNTIME) {
                    rebuildRuntime(true);
                } else {
                    refreshFromSession();
                }
            });
        }
        refreshFromSession();
        return this;
    }

    public boolean running() {
        return running;
    }

    public Optional<XmlWidgetScreen<Widget>> runtimeScreen() {
        return Optional.ofNullable(runtimeScreen);
    }

    public Optional<Widget> runtimeRoot() {
        return Optional.ofNullable(runtimeRoot);
    }

    public StackPanel runtimeHost() {
        return runtimeHost;
    }

    public UIScaleProvider scaleProvider() {
        return runtimeScreen == null ? UIScaleProvider.IDENTITY : runtimeScreen.scaleProvider();
    }

    public boolean scaleWithMinecraftGui() {
        return runtimeScreen == null || runtimeScreen.scaleWithMinecraftGui();
    }

    public String runtimeXml() {
        return runtimeXml;
    }

    public String runtimeError() {
        return runtimeError;
    }

    public boolean start() {
        if (session == null) return false;
        if (session.mode() != XmlEditorMode.RUNTIME) {
            session.mode(XmlEditorMode.RUNTIME);
            return running;
        }
        return rebuildRuntime(true);
    }

    public boolean stop() {
        if (session != null && session.mode() == XmlEditorMode.RUNTIME) {
            session.mode(XmlEditorMode.DESIGN);
            return true;
        }
        return stopRuntime();
    }

    public boolean reload() {
        return session != null && session.mode() == XmlEditorMode.RUNTIME && rebuildRuntime(true);
    }

    public void refreshFromSession() {
        if (session == null) {
            stopRuntime();
            return;
        }
        if (session.mode() == XmlEditorMode.RUNTIME) {
            rebuildRuntime(false);
        } else {
            stopRuntime();
        }
    }

    @Override
    public void dispose() {
        if (sessionSubscription != null) {
            sessionSubscription.close();
            sessionSubscription = null;
        }
        super.dispose();
    }

    private boolean rebuildRuntime(boolean force) {
        String xml = session.document().toXmlString(RUNTIME_XML);
        if (!force && runtimeRoot == null && !runtimeError.isEmpty() && Objects.equals(runtimeXml, xml)) {
            return false;
        }
        if (!force && running && runtimeRoot != null && Objects.equals(runtimeXml, xml)) {
            return true;
        }
        try {
            XmlWidgetScreen<Widget> nextScreen = XMLWidget.createScreen(
                    xml,
                    session.registry(),
                    XmlWidgetOptions.lenient().commands(session.commands()));
            replaceRuntimeScreen(nextScreen, xml);
            runtimeError = "";
            session.setDiagnostics(XmlEditorDiagnosticChannel.RUNTIME, List.of());
            return true;
        } catch (XmlWidgetLoadException failure) {
            runtimeError = failure.getMessage();
            if (runtimeRoot == null) {
                runtimeXml = xml;
            }
            session.setDiagnostics(XmlEditorDiagnosticChannel.RUNTIME, failure.diagnostics());
            invalidate(InvalidationFlags.VISUAL);
            return false;
        }
    }

    private void replaceRuntimeScreen(XmlWidgetScreen<Widget> nextScreen, String xml) {
        runtimeHost.clearChildren();
        runtimeHost.applyQueuedMutations();
        runtimeScreen = nextScreen;
        runtimeRoot = nextScreen == null ? null : nextScreen.root();
        if (runtimeRoot != null) {
            runtimeHost.addChild(runtimeRoot);
        }
        runtimeHost.applyQueuedMutations();
        runtimeXml = xml == null ? "" : xml;
        running = runtimeRoot != null;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private boolean stopRuntime() {
        if (!running && runtimeRoot == null && runtimeScreen == null && runtimeXml.isEmpty() && runtimeError.isEmpty()) {
            return false;
        }
        runtimeHost.clearChildren();
        runtimeHost.applyQueuedMutations();
        runtimeScreen = null;
        runtimeRoot = null;
        runtimeXml = "";
        runtimeError = "";
        running = false;
        if (session != null) {
            session.setDiagnostics(XmlEditorDiagnosticChannel.RUNTIME, List.of());
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return true;
    }

    private static void stretch(dev.sixik.unigui.impl.widget.WidgetBase widget) {
        widget.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .align(Alignment.STRETCH, Alignment.STRETCH)
                .flexGrow(1.0f)
                .flexShrink(1.0f));
    }
}

package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.editor.CommandManager;
import dev.sixik.unigui.api.editor.EditorCommand;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnosticsModel;
import dev.sixik.unigui.widgets.docking.DockArea;
import dev.sixik.unigui.widgets.docking.DockPane;
import dev.sixik.unigui.widgets.docking.DockPaneKind;
import dev.sixik.unigui.widgets.docking.DockingManager;
import dev.sixik.unigui.widgets.docking.DockingRoot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Coordinates editor pane visibility on top of a DockingRoot workspace. */
public final class PaneVisibilityController {
    private static final String DEFAULT_VIEW_COMMAND_PREFIX = "view.";
    private static final float TOOL_PANE_FRACTION = 0.28f;

    private final DockingRoot dockingRoot;
    private final Map<String, PaneEntry> panes = new LinkedHashMap<>();
    private final List<Consumer<PaneVisibilityChange>> listeners = new ArrayList<>();
    private CommandManager commandManager;
    private String viewCommandPrefix = DEFAULT_VIEW_COMMAND_PREFIX;
    private String diagnosticsPaneId = "";
    private boolean autoOpenDiagnosticsOnErrors = true;

    public PaneVisibilityController(DockingRoot dockingRoot) {
        this.dockingRoot = Objects.requireNonNull(dockingRoot, "dockingRoot");
    }

    public DockingRoot dockingRoot() {
        return dockingRoot;
    }

    public PaneVisibilityController registerPane(DockPane pane, DockArea preferredArea) {
        return registerPane(pane, preferredArea, "", true);
    }

    public PaneVisibilityController registerPane(DockPane pane, DockArea preferredArea, boolean visible) {
        return registerPane(pane, preferredArea, "", visible);
    }

    public PaneVisibilityController registerPane(DockPane pane, DockArea preferredArea, String targetPaneId, boolean visible) {
        DockPane normalizedPane = Objects.requireNonNull(pane, "pane");
        String paneId = normalizePaneId(normalizedPane.id());
        PaneEntry existing = panes.get(paneId);
        if (existing != null && existing.pane != normalizedPane) {
            throw new IllegalArgumentException("Pane id is already registered: " + paneId);
        }
        PaneEntry entry = existing == null
                ? new PaneEntry(normalizedPane, preferredArea, targetPaneId)
                : existing.update(preferredArea, targetPaneId);
        panes.put(paneId, entry);
        entry.pinned = normalizedPane.pinned();
        entry.visible = dockingRoot.manager().containsPane(paneId);
        registerViewCommand(entry);
        if (visible) {
            showPane(paneId, ChangeReason.REGISTERED);
        } else if (entry.visible) {
            hidePane(paneId, ChangeReason.REGISTERED);
        }
        notifyCommand(entry);
        return this;
    }

    public boolean isRegistered(String paneId) {
        return panes.containsKey(normalizePaneId(paneId));
    }

    public Optional<DockPane> pane(String paneId) {
        PaneEntry entry = panes.get(normalizePaneId(paneId));
        return entry == null ? Optional.empty() : Optional.of(entry.pane);
    }

    public List<PaneState> panes() {
        return panes.values().stream().map(PaneEntry::snapshot).toList();
    }

    public List<String> visiblePaneIds() {
        return panes.values().stream().filter(PaneEntry::visible).map(entry -> entry.pane.id()).toList();
    }

    public List<String> hiddenPaneIds() {
        return panes.values().stream().filter(entry -> !entry.visible()).map(entry -> entry.pane.id()).toList();
    }

    public boolean isVisible(String paneId) {
        PaneEntry entry = panes.get(normalizePaneId(paneId));
        return entry != null && entry.visible();
    }

    public boolean showPane(String paneId) {
        return showPane(paneId, ChangeReason.SHOWN);
    }

    public boolean hidePane(String paneId) {
        return hidePane(paneId, ChangeReason.HIDDEN);
    }

    public boolean togglePane(String paneId) {
        return isVisible(paneId)
                ? hidePane(paneId, ChangeReason.TOGGLED)
                : showPane(paneId, ChangeReason.TOGGLED);
    }

    public boolean pinned(String paneId) {
        PaneEntry entry = entry(paneId);
        return entry.pinned;
    }

    public boolean pinned(String paneId, boolean pinned) {
        PaneEntry entry = entry(paneId);
        if (entry.pinned == pinned && entry.pane.pinned() == pinned) return false;
        entry.pinned = pinned;
        entry.pane.pinned(pinned);
        emit(entry, ChangeReason.PINNED);
        notifyCommand(entry);
        return true;
    }

    public PaneVisibilityController diagnosticsPaneId(String diagnosticsPaneId) {
        this.diagnosticsPaneId = normalizeOptionalPaneId(diagnosticsPaneId);
        return this;
    }

    public String diagnosticsPaneId() {
        return diagnosticsPaneId;
    }

    public PaneVisibilityController autoOpenDiagnosticsOnErrors(boolean autoOpenDiagnosticsOnErrors) {
        this.autoOpenDiagnosticsOnErrors = autoOpenDiagnosticsOnErrors;
        return this;
    }

    public boolean autoOpenDiagnosticsOnErrors() {
        return autoOpenDiagnosticsOnErrors;
    }

    public boolean updateDiagnostics(XmlWidgetDiagnosticsModel model) {
        if (!autoOpenDiagnosticsOnErrors || diagnosticsPaneId.isEmpty()) return false;
        XmlWidgetDiagnosticsModel normalized = model == null ? XmlWidgetDiagnosticsModel.empty() : model;
        if (!normalized.hasErrors()) return false;
        return showPane(diagnosticsPaneId, ChangeReason.DIAGNOSTICS);
    }

    public PaneVisibilityController bindViewCommands(CommandManager commandManager) {
        return bindViewCommands(commandManager, DEFAULT_VIEW_COMMAND_PREFIX);
    }

    public PaneVisibilityController bindViewCommands(CommandManager commandManager, String commandPrefix) {
        this.commandManager = Objects.requireNonNull(commandManager, "commandManager");
        this.viewCommandPrefix = normalizeCommandPrefix(commandPrefix);
        for (PaneEntry entry : panes.values()) {
            registerViewCommand(entry);
        }
        return this;
    }

    public String viewCommandId(String paneId) {
        return viewCommandPrefix + sanitizeCommandSegment(normalizePaneId(paneId)) + ".toggle";
    }

    public EventSubscription onVisibilityChanged(Consumer<PaneVisibilityChange> listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private boolean showPane(String paneId, ChangeReason reason) {
        PaneEntry entry = entry(paneId);
        DockingManager manager = dockingRoot.manager();
        if (manager.containsPane(entry.pane.id())) {
            entry.visible = true;
            manager.selectPane(entry.pane.id());
            notifyCommand(entry);
            return false;
        }
        entry.pane.pinned(entry.pinned);
        DockPane target = targetPane(entry);
        DockArea area = entry.preferredArea == null ? defaultArea(entry.pane) : entry.preferredArea;
        if (target == null) {
            manager.addPane(entry.pane);
        } else if (entry.pane.document() || area == DockArea.CENTER || area == DockArea.TAB) {
            manager.tabPane(target.id(), entry.pane);
        } else {
            manager.splitPane(target.id(), area, entry.pane, splitRatio(area));
        }
        entry.visible = true;
        manager.selectPane(entry.pane.id());
        emit(entry, reason);
        notifyCommand(entry);
        return true;
    }

    private boolean hidePane(String paneId, ChangeReason reason) {
        PaneEntry entry = entry(paneId);
        DockingManager manager = dockingRoot.manager();
        if (!manager.containsPane(entry.pane.id())) {
            if (!entry.visible) return false;
            entry.visible = false;
            emit(entry, reason);
            notifyCommand(entry);
            return true;
        }
        manager.closePane(entry.pane.id());
        if (manager.containsPane(entry.pane.id())) return false;
        entry.visible = false;
        emit(entry, reason);
        notifyCommand(entry);
        return true;
    }

    private DockPane targetPane(PaneEntry entry) {
        if (!entry.targetPaneId.isEmpty()) {
            DockPane target = dockingRoot.manager().findPane(entry.targetPaneId);
            if (target != null && !target.id().equals(entry.pane.id())) return target;
        }
        for (DockPane pane : dockingRoot.manager().panes()) {
            if (pane.document() && !pane.id().equals(entry.pane.id())) return pane;
        }
        DockPane selected = dockingRoot.manager().selectedPane();
        if (selected != null && !selected.id().equals(entry.pane.id())) return selected;
        for (DockPane pane : dockingRoot.manager().panes()) {
            if (!pane.id().equals(entry.pane.id())) return pane;
        }
        return null;
    }

    private PaneEntry entry(String paneId) {
        String normalized = normalizePaneId(paneId);
        PaneEntry entry = panes.get(normalized);
        if (entry == null) throw new IllegalArgumentException("Unknown editor pane: " + normalized);
        return entry;
    }

    private void registerViewCommand(PaneEntry entry) {
        if (commandManager == null || entry == null) return;
        String commandId = viewCommandId(entry.pane.id());
        EditorCommand command = new EditorCommand(commandId, entry.pane.title())
                .action(() -> togglePane(entry.pane.id()))
                .checkedWhen(() -> isVisible(entry.pane.id()))
                .enabledWhen(() -> isRegistered(entry.pane.id()));
        commandManager.register(command);
    }

    private void notifyCommand(PaneEntry entry) {
        if (commandManager == null || entry == null) return;
        commandManager.command(viewCommandId(entry.pane.id())).ifPresent(EditorCommand::notifyChanged);
    }

    private void emit(PaneEntry entry, ChangeReason reason) {
        PaneVisibilityChange change = new PaneVisibilityChange(
                this, entry.pane, entry.pane.id(), entry.visible(), entry.pinned, reason);
        List<Consumer<PaneVisibilityChange>> snapshot = List.copyOf(listeners);
        for (Consumer<PaneVisibilityChange> listener : snapshot) {
            listener.accept(change);
        }
    }

    private static DockArea defaultArea(DockPane pane) {
        return pane != null && pane.document() ? DockArea.CENTER : DockArea.RIGHT;
    }

    private static float splitRatio(DockArea area) {
        return area != null && area.insertsBeforeTarget() ? TOOL_PANE_FRACTION : 1.0f - TOOL_PANE_FRACTION;
    }

    private static String normalizePaneId(String paneId) {
        String normalized = paneId == null ? "" : paneId.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Editor pane id must not be blank");
        return normalized;
    }

    private static String normalizeOptionalPaneId(String paneId) {
        return paneId == null ? "" : paneId.trim();
    }

    private static String normalizeCommandPrefix(String prefix) {
        String normalized = prefix == null ? "" : prefix.trim();
        return normalized.isEmpty() ? DEFAULT_VIEW_COMMAND_PREFIX : normalized;
    }

    private static String sanitizeCommandSegment(String paneId) {
        StringBuilder builder = new StringBuilder(paneId.length());
        for (int i = 0; i < paneId.length(); i++) {
            char c = paneId.charAt(i);
            builder.append(Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' ? c : '_');
        }
        return builder.toString();
    }

    private static final class PaneEntry {
        private final DockPane pane;
        private DockArea preferredArea;
        private String targetPaneId;
        private boolean visible;
        private boolean pinned;

        private PaneEntry(DockPane pane, DockArea preferredArea, String targetPaneId) {
            this.pane = pane;
            this.preferredArea = preferredArea == null ? defaultArea(pane) : preferredArea;
            this.targetPaneId = normalizeOptionalPaneId(targetPaneId);
            this.pinned = pane.pinned();
        }

        private PaneEntry update(DockArea preferredArea, String targetPaneId) {
            this.preferredArea = preferredArea == null ? this.preferredArea : preferredArea;
            this.targetPaneId = normalizeOptionalPaneId(targetPaneId);
            return this;
        }

        private boolean visible() {
            return visible;
        }

        private PaneState snapshot() {
            return new PaneState(
                    pane.id(), pane.title(), pane.kind(), preferredArea, targetPaneId, visible(), pinned);
        }
    }

    public enum ChangeReason {
        REGISTERED,
        SHOWN,
        HIDDEN,
        TOGGLED,
        PINNED,
        DIAGNOSTICS
    }

    public record PaneState(String paneId,
                            String title,
                            DockPaneKind kind,
                            DockArea preferredArea,
                            String targetPaneId,
                            boolean visible,
                            boolean pinned) {
    }

    public record PaneVisibilityChange(PaneVisibilityController controller,
                                       DockPane pane,
                                       String paneId,
                                       boolean visible,
                                       boolean pinned,
                                       ChangeReason reason) {
    }
}

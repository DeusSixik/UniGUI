package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.TextBlock;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.interaction.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Standard modal dialog shell for editor flows that need consistent buttons and results. */
@XmlWidgetName("Dialog")
public class Dialog extends WindowWidget {
    private static final String DEFAULT_BUTTONS = "ok:OK|cancel:Cancel";

    private final VBox body = new VBox();
    private final TextBlock messageBlock = new TextBlock();
    private final LinearBox contentHost = new LinearBox(Orientation.VERTICAL);
    private final HBox buttonRow = new HBox();
    private final List<DialogButton> buttons = new ArrayList<>();
    private final List<Consumer<DialogResult>> resultListeners = new ArrayList<>();
    private Widget dialogContent;
    private String defaultResult = "ok";
    private String cancelResult = "cancel";
    private String lastResult = "";
    private boolean closeOnResult = true;

    public Dialog() {
        super();
        super.title("Dialog");
        super.modal(true);
        super.fixedModal(true);
        super.resizable(false);
        super.padding(EdgeInsets.all(10.0f));

        body.spacing(8.0f);
        body.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));
        messageBlock.visibility(Visibility.COLLAPSED);
        messageBlock.layout(style -> style.height(LayoutConstraints.AUTO).flexGrow(0.0f).flexShrink(0.0f));
        contentHost.spacing(4.0f);
        contentHost.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));
        buttonRow.spacing(6.0f);
        buttonRow.layout(style -> style.height(22.0f).flexGrow(0.0f).flexShrink(0.0f));

        body.addChild(messageBlock);
        body.addChild(contentHost);
        body.addChild(buttonRow);
        body.applyQueuedMutations();
        super.content(body);
        buttons(DEFAULT_BUTTONS);
        closeButton().onClick(event -> requestCancel());
    }

    public WindowWidget window() {
        return this;
    }

    public VBox body() {
        return body;
    }

    public TextBlock messageBlock() {
        return messageBlock;
    }

    public LinearBox contentHost() {
        return contentHost;
    }

    public HBox buttonRow() {
        return buttonRow;
    }

    public String message() {
        return messageBlock.text();
    }

    @XmlAttribute(value = "message", category = "Content", defaultValue = "", description = "Dialog message text shown above the content slot.")
    public Dialog message(String message) {
        String normalized = message == null ? "" : message.trim();
        messageBlock.text(normalized);
        messageBlock.visibility(normalized.isEmpty() ? Visibility.COLLAPSED : Visibility.VISIBLE);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public Widget content() {
        return dialogContent;
    }

    @Override
    public Dialog content(Widget content) {
        if (dialogContent == content) return this;
        if (dialogContent != null) {
            contentHost.removeChild(dialogContent);
        }
        dialogContent = content;
        if (content != null) {
            contentHost.addChild(content);
        }
        contentHost.applyQueuedMutations();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Widget shellContent() {
        return super.content();
    }

    public List<DialogButton> buttons() {
        return List.copyOf(buttons);
    }

    @XmlAttribute(value = "buttons", category = "Content", defaultValue = "ok:OK|cancel:Cancel", description = "Dialog buttons as id:Label pairs separated by pipes.")
    public Dialog buttons(String buttons) {
        this.buttons.clear();
        String normalized = buttons == null || buttons.isBlank() ? DEFAULT_BUTTONS : buttons;
        for (String token : normalized.split("\\|")) {
            DialogButton button = parseButton(token);
            if (button != null && this.buttons.stream().noneMatch(existing -> existing.id().equals(button.id()))) {
                this.buttons.add(button);
            }
        }
        rebuildButtons();
        return this;
    }

    public Dialog buttons(List<DialogButton> buttons) {
        this.buttons.clear();
        if (buttons != null) {
            for (DialogButton button : buttons) {
                if (button != null && this.buttons.stream().noneMatch(existing -> existing.id().equals(button.id()))) {
                    this.buttons.add(button);
                }
            }
        }
        if (this.buttons.isEmpty()) {
            this.buttons.add(new DialogButton("ok", "OK"));
            this.buttons.add(new DialogButton("cancel", "Cancel"));
        }
        rebuildButtons();
        return this;
    }

    public Optional<DialogButton> button(String resultId) {
        String id = normalizeId(resultId);
        return buttons.stream().filter(button -> button.id().equals(id)).findFirst();
    }

    public String defaultResult() {
        return defaultResult;
    }

    @XmlAttribute(value = "defaultResult", category = "Behavior", defaultValue = "ok", description = "Result id emitted by requestDefault().")
    public Dialog defaultResult(String defaultResult) {
        this.defaultResult = normalizeId(defaultResult);
        rebuildButtons();
        return this;
    }

    public String cancelResult() {
        return cancelResult;
    }

    @XmlAttribute(value = "cancelResult", category = "Behavior", defaultValue = "cancel", description = "Result id emitted by requestCancel() and by the window close button.")
    public Dialog cancelResult(String cancelResult) {
        this.cancelResult = normalizeId(cancelResult);
        rebuildButtons();
        return this;
    }

    public boolean closeOnResult() {
        return closeOnResult;
    }

    @XmlAttribute(value = "closeOnResult", category = "Behavior", defaultValue = "true", description = "Whether emitting a dialog result closes the dialog window.")
    public Dialog closeOnResult(boolean closeOnResult) {
        this.closeOnResult = closeOnResult;
        return this;
    }

    public String lastResult() {
        return lastResult;
    }

    public boolean requestDefault() {
        return requestResult(defaultResult);
    }

    public boolean requestCancel() {
        return requestResult(cancelResult);
    }

    public boolean requestResult(String resultId) {
        String id = normalizeId(resultId);
        if (id.isEmpty()) return false;
        DialogButton button = button(id).orElseGet(() -> new DialogButton(id, defaultLabel(id)));
        emitResult(button);
        return true;
    }

    public EventSubscription onDialogResult(Consumer<DialogResult> listener) {
        Objects.requireNonNull(listener, "listener");
        resultListeners.add(listener);
        return () -> resultListeners.remove(listener);
    }

    @Override
    @XmlAttribute(value = "title", category = "Content", defaultValue = "Dialog", description = "Dialog window title text.")
    public Dialog title(String title) {
        super.title(title == null || title.isBlank() ? "Dialog" : title.trim());
        return this;
    }

    @Override
    @XmlAttribute(value = "modal", category = "Behavior", defaultValue = "true", description = "Whether the dialog blocks interaction outside itself while open.")
    public Dialog modal(boolean modal) {
        super.modal(modal);
        return this;
    }

    @Override
    @XmlAttribute(value = "fixedModal", category = "Behavior", defaultValue = "true", description = "Whether modal state disables dragging and resizing.")
    public Dialog fixedModal(boolean fixedModal) {
        super.fixedModal(fixedModal);
        return this;
    }

    @Override
    @XmlAttribute(value = "resizable", category = "Behavior", defaultValue = "false", description = "Whether the dialog can be resized by edge handles.")
    public Dialog resizable(boolean resizable) {
        super.resizable(resizable);
        return this;
    }

    @Override
    @XmlAttribute(value = "open", category = "Behavior", defaultValue = "false", description = "Whether the dialog is visible.")
    public Dialog open(boolean open) {
        super.open(open);
        return this;
    }

    @Override
    public Dialog open() {
        super.open();
        return this;
    }

    @Override
    public Dialog openModal() {
        super.openModal();
        return this;
    }

    @Override
    public Dialog close() {
        super.close();
        return this;
    }

    private void rebuildButtons() {
        buttonRow.clearChildren();
        buttonRow.applyQueuedMutations();
        for (DialogButton button : buttons) {
            Button control = new Button(buttonLabel(button));
            control.layout(style -> style.height(22.0f).flexGrow(0.0f).flexShrink(0.0f));
            control.onClick(event -> requestResult(button.id()));
            buttonRow.addChild(control);
        }
        buttonRow.applyQueuedMutations();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private String buttonLabel(DialogButton button) {
        String prefix = button.id().equals(defaultResult) ? "* " : button.id().equals(cancelResult) ? "x " : "";
        return prefix + button.label();
    }

    private void emitResult(DialogButton button) {
        lastResult = button.id();
        DialogResult result = new DialogResult(this, button.id(), button.label(),
                button.id().equals(defaultResult), button.id().equals(cancelResult));
        List<Consumer<DialogResult>> snapshot = List.copyOf(resultListeners);
        for (Consumer<DialogResult> listener : snapshot) {
            listener.accept(result);
        }
        if (closeOnResult) {
            close();
        }
    }

    private static DialogButton parseButton(String token) {
        String normalized = token == null ? "" : token.trim();
        if (normalized.isEmpty()) return null;
        int colon = normalized.indexOf(':');
        if (colon < 0) return new DialogButton(normalized, defaultLabel(normalized));
        return new DialogButton(normalized.substring(0, colon), normalized.substring(colon + 1));
    }

    private static String normalizeId(String value) {
        return value == null ? "" : value.trim();
    }

    private static String defaultLabel(String resultId) {
        String id = normalizeId(resultId);
        if (id.isEmpty()) return "";
        String lower = id.replace('-', ' ').replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public record DialogButton(String id, String label) {
        public DialogButton {
            id = normalizeId(id);
            if (id.isEmpty()) throw new IllegalArgumentException("Dialog button id must not be blank");
            label = label == null || label.isBlank() ? defaultLabel(id) : label.trim();
        }
    }

    public record DialogResult(Dialog source,
                               String resultId,
                               String label,
                               boolean defaultResult,
                               boolean cancelResult) {
        public DialogResult {
            Objects.requireNonNull(source, "source");
            resultId = normalizeId(resultId);
            label = label == null ? "" : label.trim();
        }
    }
}

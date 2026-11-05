package dev.sixik.unigui.editor.style;

import dev.sixik.unigui.api.event.FocusLostEvent;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.widgets.interaction.TextField;

import java.util.function.Consumer;

/**
 * Подпись, которую можно править на месте: в покое выглядит текстом, а значение применяется целиком по Enter или при
 * потере фокуса. Нужна для имён, где правка на каждую букву запускала бы переименование по всему документу.
 */
public final class FlatField extends TextField {
    private static final MutableColor CLEAR = MutableColor.rgba255(0, 0, 0, 0);

    private final Consumer<String> onCommit;
    private String committed;
    private boolean reportUnchanged;
    private boolean focusPending;

    /**
     * @param text     начальный текст
     * @param onCommit получает новое значение, если оно отличается от прежнего
     */
    public FlatField(String text, Consumer<String> onCommit) {
        super(text == null ? "" : text);
        this.committed = text == null ? "" : text;
        this.onCommit = onCommit;
        themeEnabled(false);
        background().set(CLEAR);
        borderVisible(true);
        borderColor().set(CLEAR);
        radius(EditorStyles.RADIUS);
        textColor().set(EditorStyles.TEXT_PRIMARY);
        textPaddingX(2.0f);
        layout(style -> style.height(EditorStyles.ROW_HEIGHT).flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
        on(KeyPressedEvent.TYPE, event -> {
            if (event.keyCode() == KeyCodes.ENTER || event.keyCode() == KeyCodes.KEYPAD_ENTER) commit();
        });
        on(FocusLostEvent.TYPE, event -> commit());
    }

    /**
     * Вид обычного поля ввода: для мест, где поле стоит в ряду других полей.
     *
     * @return это поле
     */
    public FlatField boxed() {
        background().set(EditorStyles.BG_INPUT);
        borderColor().set(EditorStyles.BORDER_INPUT);
        textPaddingX(5.0f);
        return this;
    }

    /**
     * Сообщать о завершении правки, даже если текст не менялся: нужно, когда поле временное и должно закрыться.
     *
     * @return это поле
     */
    public FlatField reportUnchanged() {
        this.reportUnchanged = true;
        return this;
    }

    /** Просит фокус на ближайшем кадре: сейчас поле может быть ещё не в дереве окна. */
    public void beginEdit() {
        this.focusPending = true;
    }

    @Override
    public void tick(dev.sixik.unigui.api.core.FrameContext frame) {
        super.tick(frame);
        if (!focusPending || uiContext() == null) return;
        focusPending = false;
        uiContext().focusManager().requestFocus(this);
    }

    /** Применяет набранный текст. */
    public void commit() {
        String next = text().trim();
        if (next.isEmpty()) {
            text(committed);
            next = committed;
        }
        if (next.equals(committed)) {
            if (reportUnchanged) onCommit.accept(next);
            return;
        }
        committed = next;
        onCommit.accept(next);
    }
}

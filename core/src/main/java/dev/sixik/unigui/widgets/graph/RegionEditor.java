package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.WidgetStyles;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayDismissable;
import dev.sixik.unigui.widgets.feedback.OverlayHostAware;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.TextArea;
import dev.sixik.unigui.widgets.interaction.TextField;

import java.util.function.Consumer;

/**
 * Всплывающий редактор текста региона: однострочное поле для названия или многострочное для комментария.
 *
 * <p>Редактор открывается в точке экрана поверх {@link OverlayLayer} и сразу берёт фокус. Название применяется по
 * {@code Enter}, комментарий по {@code Ctrl+Enter} ({@code Enter} в нём переносит строку). {@code Escape}
 * закрывает редактор без изменений, нажатие вне него применяет введённое, чтобы текст не терялся. Клавиши
 * перехватываются до поля ввода, поэтому работают, пока фокус в поле.</p>
 */
@AiDoc(
        summary = "Small floating text editor for a graph region's title (single line) or comment (multi-line), opened over an OverlayLayer.",
        purpose = "Lets users rename a region and write its comment without leaving the canvas.",
        useWhen = {
                "NodeGraph opens it for F2 and for the region context menu; open it yourself with `NodeGraph.openRegionEditor`."
        },
        criticalRules = {
                "Enter applies a title, Ctrl+Enter applies a comment; Escape cancels; a click outside applies.",
                "It is an overlay: it only works inside an OverlayLayer and removes itself when closed.",
                "The commit callback runs after the editor has closed."
        },
        related = {"NodeGraph", "NodeGraphRegion", "OverlayLayer"})
@WikiPage(title = "RegionEditor", slug = "region-editor", category = "<key:graph.category>", summary = "<key:region.editor.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:region.editor.overview>", order = 10)
public class RegionEditor extends Box implements OverlayHostAware, OverlayDismissable {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.CONTEXT_MENU;

    /**
     * Что редактируется.
     */
    public enum Mode {
        /**
         * Название региона: одна строка.
         */
        TITLE,
        /**
         * Комментарий региона: несколько строк.
         */
        COMMENT
    }

    private static final float TITLE_MIN_WIDTH = 200.0f;
    private static final float COMMENT_WIDTH = 300.0f;
    private static final float FIELD_HEIGHT = 26.0f;
    private static final int COMMENT_LINES = 5;
    private static final int MAX_TITLE_LENGTH = 80;

    private final Mode mode;
    private final Consumer<String> onCommit;
    private final VBox column = new VBox();
    private final TextField titleField;
    private final TextArea commentField;
    private final EdgeInsets padding = EdgeInsets.all(5.0f);
    private boolean open;
    private boolean focusPending;
    private float x;
    private float y;

    /**
     * @param mode        что редактировать
     * @param initialText исходный текст
     * @param width       ширина поля для названия (комментарий всегда имеет свою ширину)
     * @param onCommit    получает итоговый текст, когда пользователь применил правку
     */
    public RegionEditor(Mode mode, String initialText, float width, Consumer<String> onCommit) {
        this.mode = mode == null ? Mode.TITLE : mode;
        this.onCommit = onCommit == null ? text -> {
        } : onCommit;
        applyComponentStyleDefaults(WidgetStyles.ContextMenu.DEFAULT);
        layout(style -> style.position(PositionType.ABSOLUTE).overflow(Overflow.VISIBLE));
        background(new MutableColor(0.105f, 0.11f, 0.125f, 0.98f));
        border(new MutableColor(0.30f, 0.31f, 0.36f, 1.0f));
        radius(6.0f);
        visible(false);
        focusable(true);
        column.spacing(4.0f);
        String text = initialText == null ? "" : initialText;
        if (this.mode == Mode.TITLE) {
            float fieldWidth = Math.max(TITLE_MIN_WIDTH, Float.isFinite(width) ? width : TITLE_MIN_WIDTH);
            titleField = new TextField(text);
            titleField.maxLength(MAX_TITLE_LENGTH);
            titleField.placeholder("Region name");
            titleField.layout(style -> style.size(fieldWidth, FIELD_HEIGHT));
            commentField = null;
            column.addChild(titleField);
        } else {
            commentField = new TextArea(text);
            commentField.placeholder("Comment...");
            commentField.visibleLines(COMMENT_LINES);
            commentField.layout(style -> style.size(COMMENT_WIDTH, FIELD_HEIGHT * 0.6f * COMMENT_LINES + 8.0f));
            titleField = null;
            Label hint = new Label("Ctrl+Enter - apply, Esc - cancel");
            hint.color(new MutableColor(0.62f, 0.64f, 0.68f, 1.0f));
            column.addChild(commentField);
            column.addChild(hint);
        }
        column.applyQueuedMutations();
        addChild(column);
        applyQueuedMutations();
    }

    @Override
    protected Style defaultStyle() {
        return WidgetStyles.ContextMenu.DEFAULT;
    }

    /**
     * @return что редактирует этот редактор
     */
    public Mode mode() {
        return mode;
    }

    /**
     * @return текст, который сейчас в поле
     */
    public String text() {
        return mode == Mode.TITLE ? titleField.text() : commentField.text();
    }

    /**
     * Заменяет текст в поле.
     *
     * @param text новый текст
     * @return этот редактор
     */
    public RegionEditor text(String text) {
        String next = text == null ? "" : text;
        if (mode == Mode.TITLE) titleField.text(next);
        else commentField.text(next);
        return this;
    }

    /**
     * @return {@code true}, если редактор открыт
     */
    public boolean opened() {
        return open;
    }

    /**
     * @return виджет ввода: {@link TextField} для названия или {@link TextArea} для комментария
     */
    public Widget field() {
        return mode == Mode.TITLE ? titleField : commentField;
    }

    /**
     * Открывает редактор в точке и просит фокус для поля. Название выделяется целиком, чтобы набор заменял его.
     *
     * @param rootX X точки в root-координатах
     * @param rootY Y точки в root-координатах
     * @return этот редактор
     */
    public RegionEditor openAt(float rootX, float rootY) {
        this.x = Float.isFinite(rootX) ? rootX : 0.0f;
        this.y = Float.isFinite(rootY) ? rootY : 0.0f;
        if (!open) {
            open = true;
            visible(true);
        }
        if (mode == Mode.TITLE) titleField.selectAll();
        else commentField.cursorIndex(commentField.text().length());
        focusPending = true;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Применяет введённое: закрывает редактор и передаёт текст в {@code onCommit}.
     *
     * @return {@code true}, если редактор был открыт
     */
    public boolean commit() {
        if (!open) return false;
        String text = text();
        close();
        onCommit.accept(text);
        return true;
    }

    /**
     * Закрывает редактор без изменений и убирает его из слоя оверлеев.
     *
     * @return этот редактор
     */
    public RegionEditor close() {
        if (!open) return this;
        open = false;
        focusPending = false;
        visible(false);
        if (parent() instanceof OverlayLayer layer) {
            layer.removeOverlay(this);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void handle(Event event) {
        if (open && !event.isCancelled() && event instanceof KeyPressedEvent key) {
            int code = key.keyCode();
            if (code == KeyCodes.ESCAPE) {
                close();
                event.cancel();
                return;
            }
            boolean enter = code == KeyCodes.ENTER || code == KeyCodes.KEYPAD_ENTER;
            if (enter && (mode == Mode.TITLE || KeyModifiers.has(key.modifiers(), KeyModifiers.CONTROL))) {
                commit();
                event.cancel();
                return;
            }
        }
        super.handle(event);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED || !open) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        if (focusPending && uiContext() != null) {
            focusPending = false;
            uiContext().focusManager().requestFocus(field());
        }
        column.measure(context);
        setDesiredSize(resolveDesiredSize(context,
                column.desiredSize().width() + padding.horizontal(),
                column.desiredSize().height() + padding.vertical()));
    }

    @Override
    public void arrange(RectView bounds) {
        arrangeInHost(bounds);
    }

    @Override
    public void arrangeInHost(RectView hostBounds) {
        if (!open || visibility() == Visibility.COLLAPSED) {
            mutableLayoutBounds().set(hostBounds.x(), hostBounds.y(), 0.0f, 0.0f);
            return;
        }
        float width = desiredSize().width();
        float height = desiredSize().height();
        float placedX = Math.max(hostBounds.x(), Math.min(x, hostBounds.x() + Math.max(0.0f, hostBounds.width() - width)));
        float placedY = Math.max(hostBounds.y(), Math.min(y, hostBounds.y() + Math.max(0.0f, hostBounds.height() - height)));
        mutableLayoutBounds().set(placedX, placedY, width, height);
        column.arrange(new MutableRect(
                placedX + padding.left(),
                placedY + padding.top(),
                Math.max(0.0f, width - padding.horizontal()),
                Math.max(0.0f, height - padding.vertical())));
    }

    @Override
    public boolean isOpenOverlay() {
        return open;
    }

    @Override
    public boolean dismissOnOutsideClick() {
        return true;
    }

    @Override
    public boolean isInteractionTarget(Widget target) {
        for (Widget current = target; current != null; current = current.parent()) {
            if (current == this) return true;
        }
        return false;
    }

    /**
     * Нажатие вне редактора применяет введённое.
     */
    @Override
    public void dismiss() {
        commit();
    }
}

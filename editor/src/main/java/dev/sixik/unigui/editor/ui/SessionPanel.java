package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.EditorPanel;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.SessionChange;

import java.util.Optional;

/**
 * Основа панелей UI-редактора: следит за тем, какой XAML-документ текущий, и сообщает панели о смене документа и о
 * каждом изменении его сессии.
 *
 * <p>Панель реализует {@link #build}, {@link #bound} и {@link #changed}; подписки на шину и на сессию, а также
 * переключение при смене документа берёт на себя этот класс.</p>
 */
abstract class SessionPanel implements EditorPanel {
    private EditorContext context;
    private EditorSession session;
    private EventSubscription sessionSubscription;

    @Override
    public final Widget create(EditorContext editorContext) {
        this.context = editorContext;
        Widget root = build(editorContext);
        editorContext.events().on(EditorContext.DocumentChanged.class, event -> rebind());
        rebind();
        return root;
    }

    /**
     * Создаёт содержимое панели; документа к этому моменту может ещё не быть.
     *
     * @param editorContext контекст редактора
     * @return корневой виджет
     */
    protected abstract Widget build(EditorContext editorContext);

    /**
     * Вызывается, когда текущим стал другой документ или документ закрыт.
     *
     * @param next новая сессия или {@code null}, если XAML-документа нет
     */
    protected void bound(EditorSession next) {
    }

    /**
     * Вызывается при каждом изменении сессии текущего документа.
     *
     * @param current сессия
     * @param change  что изменилось
     */
    protected void changed(EditorSession current, SessionChange change) {
    }

    /** @return общее состояние наведения; создаётся при первом обращении */
    protected final HoverState hover() {
        return context.service(HoverState.class).orElseGet(() -> {
            HoverState state = new HoverState();
            context.provide(HoverState.class, state);
            return state;
        });
    }

    /** @return контекст редактора */
    protected final EditorContext context() {
        return context;
    }

    /** @return сессия текущего XAML-документа */
    protected final Optional<EditorSession> session() {
        return Optional.ofNullable(session);
    }

    private void rebind() {
        if (sessionSubscription != null) sessionSubscription.unsubscribe();
        sessionSubscription = null;
        session = context.document()
                .filter(XamlEditorDocument.class::isInstance)
                .map(document -> ((XamlEditorDocument) document).session())
                .orElse(null);
        if (session != null) {
            EditorSession bound = session;
            sessionSubscription = bound.onChanged(change -> changed(bound, change));
        }
        bound(session);
    }
}

package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.editor.EditorCommand;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.EditorLayout;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.EditorTheme;
import dev.sixik.unigui.editor.ToolbarSection;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.SessionChange;
import dev.sixik.unigui.xaml.load.LoadOptions;

import java.util.function.Function;

/**
 * Редактор интерфейса: собирает каркас с панелями Hierarchy, Palette, Inspector, Viewport, XAML и Problems вокруг
 * документа XAML.
 *
 * <pre>{@code
 * XamlEditorDocument document = new XamlEditorDocument("demo", "Demo",
 *         EditorSession.open(DocumentSource.file(path)));
 * EditorShell editor = UiEditor.shell(document);
 * screenRoot.addChild(editor.root());
 * }</pre>
 *
 * <p>Все панели работают с одной {@link EditorSession}: правка из любой панели видна в остальных, попадает в историю
 * отмены и сохраняется командой File → Save.</p>
 */
public final class UiEditor {
    /** Идентификатор панели иерархии. */
    public static final String HIERARCHY = "hierarchy";
    /** Идентификатор панели палитры. */
    public static final String PALETTE = "palette";
    /** Идентификатор панели свойств. */
    public static final String INSPECTOR = "inspector";
    /** Идентификатор панели предпросмотра. */
    public static final String VIEWPORT = "viewport";
    /** Идентификатор панели текста. */
    public static final String CODE = "code";
    /** Идентификатор панели диагностики. */
    public static final String DIAGNOSTICS = "diagnostics";

    /** Имя раскладки «дизайн»: предпросмотр в центре. */
    public static final String DESIGN_LAYOUT = "Design";
    /** Имя раскладки «текст»: редактор XAML в центре. */
    public static final String CODE_LAYOUT = "Code";

    private UiEditor() {
    }

    /**
     * Собирает редактор документа.
     *
     * @param document документ XAML
     * @return каркас; его {@link EditorShell#root()} вставляется в экран
     */
    public static EditorShell shell(XamlEditorDocument document) {
        return shell(document, LoadOptions.DEFAULT, EditorTheme.dark(), shellBuilder -> shellBuilder);
    }

    /**
     * Собирает редактор документа с настройками предпросмотра и темой.
     *
     * @param document       документ XAML
     * @param previewOptions настройки загрузки предпросмотра: резолвер текстур, команды, источники привязок
     * @param theme          тема каркаса
     * @param customize      позволяет добавить панели, команды и раскладки до сборки
     * @return каркас
     */
    public static EditorShell shell(XamlEditorDocument document, LoadOptions previewOptions, EditorTheme theme,
                                    Function<EditorShell.Builder, EditorShell.Builder> customize) {
        EditorShell.Builder builder = common(document.title(), previewOptions, theme, new ComponentView())
                .layout(EditorLayout.named(DESIGN_LAYOUT)
                        .left(0.2f, HIERARCHY)
                        .leftLower(0.52f, PALETTE)
                        .right(0.26f, INSPECTOR)
                        .bottom(0.2f, DIAGNOSTICS)
                        .center(VIEWPORT, CODE)
                        .active(VIEWPORT)
                        .build())
                .layout(EditorLayout.named(CODE_LAYOUT)
                        .right(0.28f, INSPECTOR)
                        .bottom(0.25f, DIAGNOSTICS)
                        .center(CODE, VIEWPORT)
                        .active(CODE)
                        .build());
        return finish(customize.apply(builder).build(), document);
    }

    /**
     * Каркас с общими для экрана и компонента панелями и командами; раскладки и дополнительные панели добавляет
     * вызывающий.
     */
    static EditorShell.Builder common(String title, LoadOptions previewOptions, EditorTheme theme,
                                      ComponentView componentView) {
        return EditorShell.builder()
                .theme(theme)
                .title(title)
                .menus("File", "Edit", "Window")
                .standardCommands()
                .command(editCommand("edit.delete", "Delete", "Delete", 10, UiEdits::deleteSelected))
                .command(editCommand("edit.duplicate", "Duplicate", "Ctrl+D", 11, UiEdits::duplicateSelected))
                .command(editCommand("edit.moveUp", "Move Up", "", 12, session -> UiEdits.moveSelected(session, -1)))
                .command(editCommand("edit.moveDown", "Move Down", "", 13, session -> UiEdits.moveSelected(session, 1)))
                .menuBarLeading(EditorChrome::logo)
                .toolbar(ToolbarSection.LEFT, shell -> EditorChrome.documentActions(shell))
                .toolbar(ToolbarSection.CENTER, EditorChrome::layoutSwitch)
                .statusBar(ToolbarSection.LEFT, EditorChrome::selectionStatus)
                .statusBar(ToolbarSection.RIGHT, EditorChrome::documentStatus)
                .panel(new HierarchyPanel())
                .panel(new PalettePanel())
                .panel(new InspectorPanel())
                .panel(new ViewportPanel(previewOptions, componentView))
                .panel(new CodePanel())
                .panel(new DiagnosticsPanel());
    }

    /** Завершает сборку: мост выбора и подключение документа. */
    static EditorShell finish(EditorShell shell, XamlEditorDocument document) {
        installSelectionBridge(shell.context());
        if (document != null) shell.context().document(document);
        return shell;
    }

    /** Команда меню Edit, действующая на выбранный узел текущей сессии. */
    private static EditorCommand editCommand(String id, String title, String shortcut, int order,
                                             java.util.function.Predicate<EditorSession> action) {
        return EditorCommand.builder(id).title(title).menu("Edit").order(order).shortcut(shortcut)
                .enabledWhen(context -> sessionOf(context) != null && UiEdits.hasMovableSelection(sessionOf(context)))
                .action(context -> action.test(sessionOf(context)))
                .build();
    }

    /** @return сессия текущего документа или {@code null}, если открыт документ другого вида */
    static EditorSession sessionOf(EditorContext context) {
        return context.document().filter(XamlEditorDocument.class::isInstance)
                .map(document -> ((XamlEditorDocument) document).session()).orElse(null);
    }

    /**
     * Дублирует выбор сессии в общий выбор контекста, чтобы панели сторонних авторов видели выбранный узел, не
     * зная о сессиях.
     */
    private static void installSelectionBridge(EditorContext context) {
        EventSubscription[] current = new EventSubscription[1];
        context.events().on(EditorContext.DocumentChanged.class, event -> {
            if (current[0] != null) current[0].unsubscribe();
            current[0] = null;
            EditorSession session = sessionOf(context);
            if (session == null) return;
            current[0] = session.onChanged(change -> {
                if (change.kind() != SessionChange.Kind.SELECTION_CHANGED) return;
                session.selected().ifPresentOrElse(node -> context.selection().set(node), () -> context.selection().clear());
            });
        });
    }
}

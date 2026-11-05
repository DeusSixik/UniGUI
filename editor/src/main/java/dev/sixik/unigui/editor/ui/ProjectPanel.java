package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.ContextMenus;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.EditorPanel;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.editor.style.ToolButton;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.project.Project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Проект: файлы по видам (экраны, компоненты, структуры, ресурсы), создание, переименование, удаление с проверкой
 * ссылок, проверка всего проекта, сохранение в папку и экспорт пака. Щелчок по документу открывает его во всех
 * остальных панелях.
 *
 * <p>Имя нового файла и новое имя при переименовании вводятся в одно поле над списком; пустое поле даёт имя по
 * умолчанию ({@code Screen1}, {@code Component2}…). Идентификаторы виджетов {@code project.*} стабильны для тестов.</p>
 */
final class ProjectPanel implements EditorPanel {
    /** Идентификатор панели. */
    static final String ID = "project";

    private final Project project;
    private final Path saveFolder;
    private final Path exportFile;
    private final VBox list = new VBox();
    private final VBox problems = new VBox();
    private final TextField name = new TextField();
    private final Label status = new Label("");
    private final Map<String, XamlEditorDocument> documents = new HashMap<>();
    private EditorContext context;
    private String selected;

    ProjectPanel(Project project, Path saveFolder, Path exportFile) {
        this.project = project;
        this.saveFolder = saveFolder;
        this.exportFile = exportFile;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String title() {
        return "Project";
    }

    @Override
    public dev.sixik.unigui.api.render.UiIcon icon() {
        return dev.sixik.unigui.editor.style.EditorIcons.PROJECT;
    }

    @Override
    public PanelSlot slot() {
        return PanelSlot.LEFT;
    }

    @Override
    public float share() {
        return 0.22f;
    }

    @Override
    public Widget create(EditorContext editorContext) {
        this.context = editorContext;
        VBox root = new VBox();
        root.spacing(4.0f);
        root.layout(style -> style.sizePercent(100.0f, 100.0f).padding(5.0f));

        name.id("project.name");
        name.placeholder("name (new file or rename)");
        root.addChild(name);
        root.addChild(row(button("project.new.screen", "Screen", () -> create(Project.Kind.SCREEN)),
                button("project.new.component", "Component", () -> create(Project.Kind.COMPONENT)),
                button("project.new.struct", "Struct", () -> create(Project.Kind.STRUCT))));
        root.addChild(tools(
                tool("project.rename", EditorIcons.RENAME, "Rename", this::rename),
                tool("project.delete", EditorIcons.CLEAR, "Delete", this::delete),
                tool("project.check", EditorIcons.CHECK, "Check", this::check),
                tool("project.undo", EditorIcons.UNDO, "Undo", this::undo),
                tool("project.redo", EditorIcons.REDO, "Redo", this::redo),
                tool("project.save", EditorIcons.SAVE, "Save", this::save),
                tool("project.export", EditorIcons.EXPORT, "Export", this::export)));
        status.color(EditorStyles.TEXT_MUTED);
        status.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));
        root.addChild(status);

        list.spacing(1.0f);
        list.layout(style -> style.widthPercent(100.0f));
        ScrollView scroll = new ScrollView(list);
        EditorStyles.styleScroll(scroll);
        scroll.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));
        root.addChild(scroll);
        problems.spacing(1.0f);
        root.addChild(problems);
        root.applyQueuedMutations();

        EditorStyles.apply(root);
        project.onChanged(this::refresh);
        context.events().on(EditorContext.DocumentEdited.class, event -> refresh());
        refresh();
        return root;
    }

    private static HBox row(Button... children) {
        HBox row = new HBox();
        row.spacing(3.0f);
        row.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));
        for (Button child : children) {
            child.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
            row.addChild(child);
        }
        row.applyQueuedMutations();
        return row;
    }

    /** Ряд кнопок-иконок: файловые действия отделены от истории и сохранения. */
    private static HBox tools(Button... children) {
        HBox row = new HBox();
        row.spacing(1.0f);
        row.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));
        for (int i = 0; i < children.length; i++) {
            if (i == 3 || i == 5) row.addChild(EditorChrome.divider());
            row.addChild(children[i]);
        }
        row.applyQueuedMutations();
        return row;
    }

    private static Button button(String id, String text, Runnable action) {
        ToolButton button = new ToolButton(EditorIcons.PLUS, text, ToolButton.Variant.SOLID);
        button.id(id);
        button.onClick(click -> action.run());
        return button;
    }

    private static Button tool(String id, UiIcon icon, String title, Runnable action) {
        ToolButton button = new ToolButton(icon, title);
        button.id(id);
        button.onClick(click -> action.run());
        return button;
    }

    private static UiIcon iconOf(Project.Kind kind) {
        return switch (kind) {
            case SCREEN -> EditorIcons.VIEWPORT;
            case COMPONENT -> EditorIcons.GRAPH;
            case STRUCT -> EditorIcons.STRUCT;
            default -> EditorIcons.CONTRACT;
        };
    }

    /** Открывает первый файл проекта, если ничего не открыто: окно не стартует пустым. */
    void openFirst() {
        if (selected != null) return;
        // Экран нагляднее всего, поэтому он открывается первым, даже если файлы лежат в другом порядке.
        project.entries(Project.Kind.SCREEN).stream().findFirst().or(() -> project.entries().stream()
                .filter(entry -> entry.kind().document()).findFirst()).ifPresent(entry -> open(entry.path()));
    }

    // ------------------------------------------------------------------ список

    private void refresh() {
        list.clearChildren();
        for (Project.Kind kind : List.of(Project.Kind.SCREEN, Project.Kind.COMPONENT, Project.Kind.STRUCT,
                Project.Kind.TEXTURE, Project.Kind.OTHER)) {
            List<Project.Entry> entries = project.entries(kind);
            if (entries.isEmpty()) continue;
            Label group = EditorStyles.caption(switch (kind) {
                case SCREEN -> "Screens";
                case COMPONENT -> "Components";
                case STRUCT -> "Structs";
                case TEXTURE -> "Resources";
                default -> "Other";
            });
            group.layout(style -> style.margin(2.0f, 5.0f, 0.0f, 2.0f));
            list.addChild(group);
            for (Project.Entry entry : entries) list.addChild(entryRow(entry));
        }
        list.applyQueuedMutations();
        EditorStyles.apply(list);
    }

    private Widget entryRow(Project.Entry entry) {
        String label = (project.dirty(entry.path()) ? "• " : "") + entry.path().substring(entry.kind().folder().length());
        ToolButton button = new ToolButton(iconOf(entry.kind()), label, ToolButton.Variant.GHOST).alignStart();
        button.id("project.file." + entry.path());
        button.layout(style -> style.widthPercent(100.0f));
        button.active(entry.path().equals(selected));
        button.onClick(click -> open(entry.path()));
        ContextMenus.onSecondaryClick(button, context, () -> List.of(
                ContextMenus.Entry.of("Open", () -> open(entry.path())),
                ContextMenus.Entry.of("Rename", entry.kind().document(), () -> {
                    selected = entry.path();
                    rename();
                }),
                ContextMenus.Entry.of("Delete", () -> {
                    selected = entry.path();
                    delete();
                })));
        return button;
    }

    // ------------------------------------------------------------------ действия

    /** Открывает документ во всех панелях; ресурсы только выбираются. */
    void open(String path) {
        selected = path;
        Project.Entry entry = project.entries().stream().filter(e -> e.path().equals(path)).findFirst().orElse(null);
        if (entry != null && entry.kind().document()) {
            XamlEditorDocument document = documents.computeIfAbsent(path,
                    key -> new XamlEditorDocument(project.manifest().id() + "/" + key,
                            key.substring(key.lastIndexOf('/') + 1), project.session(key)));
            context.document(document);
        }
        refresh();
    }

    private void create(Project.Kind kind) {
        String base = switch (kind) {
            case SCREEN -> "Screen";
            case COMPONENT -> "Component";
            default -> "Struct";
        };
        String requested = name.text().trim();
        try {
            Project.Entry created = requested.isEmpty() ? createFresh(kind, base) : createNamed(kind, requested);
            name.text("");
            say("Created " + created.path(), false);
            open(created.path());
        } catch (IllegalArgumentException failure) {
            say(failure.getMessage(), true);
        }
    }

    private Project.Entry createNamed(Project.Kind kind, String requested) {
        return switch (kind) {
            case SCREEN -> project.createScreen(requested);
            case COMPONENT -> project.createComponent(requested);
            default -> project.createStruct(requested);
        };
    }

    private Project.Entry createFresh(Project.Kind kind, String base) {
        for (int i = 1; ; i++) {
            try {
                return createNamed(kind, base + i);
            } catch (IllegalArgumentException taken) {
                if (!taken.getMessage().contains("already exists")) throw taken;
            }
        }
    }

    private void rename() {
        if (selected == null) {
            say("Select a file first.", true);
            return;
        }
        String target = name.text().trim();
        if (target.isEmpty()) {
            say("Type the new name in the field above.", true);
            return;
        }
        Project.Entry before = project.entries().stream().filter(e -> e.path().equals(selected)).findFirst().orElse(null);
        boolean wasOpen = context.document().filter(doc -> documents.containsValue(doc)).isPresent();
        String reason = project.rename(selected, target);
        if (!reason.isEmpty()) {
            say(reason, true);
            return;
        }
        // Переименование пересоздаёт сессии документов: прежние обёртки больше не связаны с проектом.
        documents.clear();
        String renamed = before == null ? null : Project.pathOf(before.kind(), target);
        name.text("");
        say("Renamed to " + target, false);
        if (renamed != null && project.has(renamed) && wasOpen) {
            open(renamed);
        } else {
            context.document(null);
            selected = null;
            refresh();
        }
    }

    private void delete() {
        if (selected == null) {
            say("Select a file first.", true);
            return;
        }
        String reason = project.deleteIfUnused(selected);
        if (!reason.isEmpty()) {
            say(reason, true);
            return;
        }
        documents.remove(selected);
        say("Deleted " + selected, false);
        selected = null;
        context.document(null);
        refresh();
    }

    /** Отменяет структурную правку проекта (создание, удаление, переименование файла). */
    private void undo() {
        String label = project.undoLabel().orElse("");
        if (!project.undo()) {
            say("Nothing to undo.", true);
            return;
        }
        afterHistoryStep("Undid " + label);
    }

    private void redo() {
        String label = project.redoLabel().orElse("");
        if (!project.redo()) {
            say("Nothing to redo.", true);
            return;
        }
        afterHistoryStep("Redid " + label);
    }

    /** Сессии затронутых файлов закрыты проектом: прежние обёртки документов отбрасываются, а открытый файл открывается заново. */
    private void afterHistoryStep(String message) {
        boolean wasOpen = selected != null && project.has(selected);
        documents.clear();
        context.document(null);
        say(message, false);
        if (wasOpen) open(selected);
        else {
            selected = null;
            refresh();
        }
    }

    private void check() {
        problems.clearChildren();
        List<Project.Problem> found = project.check();
        say(found.isEmpty() ? "No problems." : found.size() + " problem(s).", !found.isEmpty());
        for (Project.Problem problem : found) {
            ToolButton row = new ToolButton(EditorIcons.ERROR, problem.path() + ": " + problem.message(),
                    ToolButton.Variant.GHOST).alignStart().iconColor(EditorStyles.ERROR_RED);
            row.layout(style -> style.widthPercent(100.0f));
            row.onClick(click -> {
                if (project.has(problem.path())) open(problem.path());
            });
            problems.addChild(row);
        }
        problems.applyQueuedMutations();
        EditorStyles.apply(problems);
    }

    private void save() {
        if (saveFolder == null) {
            say("This project has no folder to save to.", true);
            return;
        }
        try {
            project.save(saveFolder);
            say("Saved to " + saveFolder, false);
            refresh();
        } catch (IOException failure) {
            say("Cannot save: " + failure.getMessage(), true);
        }
    }

    private void export() {
        if (exportFile == null) {
            say("This project has no export file.", true);
            return;
        }
        try {
            if (exportFile.getParent() != null) Files.createDirectories(exportFile.getParent());
            Files.write(exportFile, project.toArchive());
            say("Exported to " + exportFile, false);
            refresh();
        } catch (IOException failure) {
            say("Cannot export: " + failure.getMessage(), true);
        }
    }

    private void say(String text, boolean problem) {
        status.text(text);
        if (problem) status.color(EditorStyles.ERROR_RED);
        else status.color(MutableColor.rgba255(200, 200, 200, 255));
    }

    /** @return текст строки состояния; нужен тестам */
    String statusText() {
        return status.text();
    }

    /** @return путь выбранного файла или {@code null}; нужен тестам */
    String selectedPath() {
        return selected;
    }
}

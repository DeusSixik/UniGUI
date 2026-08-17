package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.editor.CommandManager;
import dev.sixik.unigui.api.editor.EditorCommand;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Compact editor panel for current project actions and recent project selection. */
@XmlWidgetName("ProjectPickerPanel")
public class ProjectPickerPanel extends LinearBox {
    public static final String COMMAND_NEW_PROJECT = "new_project";
    public static final String COMMAND_OPEN_PROJECT = "open_project";
    public static final String COMMAND_SAVE_PROJECT = "save_project";
    public static final String COMMAND_LAST_PROJECTS = "last_projects";

    private final Label titleLabel = new Label("Project");
    private final Label currentProjectLabel = new Label("No project open");
    private final HBox actionRow = new HBox();
    private final Button newButton = new Button("New");
    private final Button openButton = new Button("Open");
    private final Button saveButton = new Button("Save");
    private final Label recentHeader = new Label("Recent Projects");
    private final VBox recentList = new VBox();
    private final List<ProjectReference> recentProjects = new ArrayList<>();
    private final List<Consumer<ProjectAction>> actionListeners = new ArrayList<>();

    private ProjectReference currentProject;
    private UnsavedChangePrompt unsavedChangePrompt = action -> true;
    private int maxRecentProjects = 8;
    private boolean dirty;

    public ProjectPickerPanel() {
        super(Orientation.VERTICAL);
        spacing(5.0f);
        layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));

        titleLabel.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
        currentProjectLabel.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
        actionRow.spacing(4.0f);
        actionRow.layout(style -> style.height(24.0f).flexGrow(0.0f).flexShrink(0.0f));
        recentHeader.layout(style -> style.height(18.0f).flexGrow(0.0f).flexShrink(0.0f));
        recentList.spacing(2.0f);
        recentList.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));

        newButton.onClick(event -> requestNewProject());
        openButton.onClick(event -> requestOpenProject());
        saveButton.onClick(event -> requestSaveProject());
        actionRow.addChild(newButton);
        actionRow.addChild(openButton);
        actionRow.addChild(saveButton);
        actionRow.applyQueuedMutations();

        addChild(titleLabel);
        addChild(currentProjectLabel);
        addChild(actionRow);
        addChild(recentHeader);
        addChild(recentList);
        applyQueuedMutations();
        rebuildRecentList();
        refreshCurrentProjectLabel();
    }

    public String title() {
        return titleLabel.text();
    }

    @XmlAttribute(value = "title", category = "Content", defaultValue = "Project", description = "Panel title shown above project actions.")
    public ProjectPickerPanel title(String title) {
        titleLabel.text(normalize(title, "Project"));
        return this;
    }

    public boolean dirty() {
        return dirty;
    }

    @XmlAttribute(value = "dirty", category = "State", defaultValue = "false", description = "Whether the current project has unsaved changes.")
    public ProjectPickerPanel dirty(boolean dirty) {
        if (this.dirty == dirty) return this;
        this.dirty = dirty;
        refreshCurrentProjectLabel();
        return this;
    }

    public int maxRecentProjects() {
        return maxRecentProjects;
    }

    @XmlAttribute(value = "maxRecentProjects", category = "Behavior", defaultValue = "8", description = "Maximum number of recent projects shown in the panel.")
    public ProjectPickerPanel maxRecentProjects(int maxRecentProjects) {
        int normalized = Math.max(1, maxRecentProjects);
        if (this.maxRecentProjects == normalized) return this;
        this.maxRecentProjects = normalized;
        trimRecentProjects();
        rebuildRecentList();
        return this;
    }

    public Optional<ProjectReference> currentProject() {
        return Optional.ofNullable(currentProject);
    }

    public ProjectPickerPanel currentProject(ProjectReference project) {
        this.currentProject = project;
        if (project != null) addRecentProject(project);
        refreshCurrentProjectLabel();
        return this;
    }

    public String currentProjectName() {
        return currentProject == null ? "" : currentProject.displayName();
    }

    @XmlAttribute(value = "currentProjectName", category = "Content", defaultValue = "", description = "Display name for the current project label.")
    public ProjectPickerPanel currentProjectName(String displayName) {
        ProjectReference previous = currentProject;
        currentProject = new ProjectReference(
                previous == null ? "" : previous.id(),
                normalize(displayName, "Untitled Project"),
                previous == null ? "" : previous.path(),
                previous == null ? "" : previous.resourceId());
        refreshCurrentProjectLabel();
        return this;
    }

    public String currentPath() {
        return currentProject == null ? "" : currentProject.path();
    }

    @XmlAttribute(value = "currentPath", category = "Content", defaultValue = "", description = "Filesystem path for the current project label.")
    public ProjectPickerPanel currentPath(String path) {
        ProjectReference previous = currentProject;
        currentProject = new ProjectReference(
                previous == null ? "" : previous.id(),
                previous == null ? "Untitled Project" : previous.displayName(),
                path,
                previous == null ? "" : previous.resourceId());
        refreshCurrentProjectLabel();
        return this;
    }

    public String currentResource() {
        return currentProject == null ? "" : currentProject.resourceId();
    }

    @XmlAttribute(value = "currentResource", category = "Content", defaultValue = "", description = "Resource id for the current project label when no filesystem path is available.")
    public ProjectPickerPanel currentResource(String resourceId) {
        ProjectReference previous = currentProject;
        currentProject = new ProjectReference(
                previous == null ? "" : previous.id(),
                previous == null ? "Untitled Project" : previous.displayName(),
                previous == null ? "" : previous.path(),
                resourceId);
        refreshCurrentProjectLabel();
        return this;
    }

    public List<ProjectReference> recentProjects() {
        return List.copyOf(recentProjects);
    }

    public ProjectPickerPanel recentProjects(List<ProjectReference> projects) {
        recentProjects.clear();
        if (projects != null) {
            for (ProjectReference project : projects) {
                addRecentProject(project, false);
            }
        }
        trimRecentProjects();
        rebuildRecentList();
        return this;
    }

    public ProjectPickerPanel addRecentProject(ProjectReference project) {
        return addRecentProject(project, true);
    }

    public boolean removeRecentProject(String projectId) {
        String normalized = normalize(projectId, "");
        if (normalized.isEmpty()) return false;
        boolean removed = recentProjects.removeIf(project -> project.id().equals(normalized));
        if (removed) rebuildRecentList();
        return removed;
    }

    public ProjectPickerPanel clearRecentProjects() {
        recentProjects.clear();
        rebuildRecentList();
        return this;
    }

    public ProjectPickerPanel unsavedChangePrompt(UnsavedChangePrompt prompt) {
        unsavedChangePrompt = prompt == null ? action -> true : prompt;
        return this;
    }

    public EventSubscription onProjectAction(Consumer<ProjectAction> listener) {
        Objects.requireNonNull(listener, "listener");
        actionListeners.add(listener);
        return () -> actionListeners.remove(listener);
    }

    public boolean requestNewProject() {
        ProjectAction action = new ProjectAction(ActionKind.NEW_PROJECT, null, "", false);
        if (!confirmUnsavedChanges(action)) return false;
        emit(action);
        return true;
    }

    public boolean requestOpenProject() {
        return requestOpenProject("");
    }

    public boolean requestOpenProject(String path) {
        ProjectAction action = new ProjectAction(ActionKind.OPEN_PROJECT, null, normalize(path, ""), false);
        if (!confirmUnsavedChanges(action)) return false;
        emit(action);
        return true;
    }

    public boolean requestSaveProject() {
        ProjectAction action = new ProjectAction(ActionKind.SAVE_PROJECT, currentProject, currentPath(), false);
        emit(action);
        dirty(false);
        return true;
    }

    public boolean requestLastProjects() {
        emit(new ProjectAction(ActionKind.LAST_PROJECTS, null, "", false));
        return true;
    }

    public boolean openRecentProject(String projectId) {
        ProjectReference project = recentProject(projectId).orElse(null);
        if (project == null) return false;
        ProjectAction action = new ProjectAction(ActionKind.OPEN_PROJECT, project, project.path(), true);
        if (!confirmUnsavedChanges(action)) return false;
        currentProject(project);
        emit(action);
        return true;
    }

    public Optional<ProjectReference> recentProject(String projectId) {
        String normalized = normalize(projectId, "");
        if (normalized.isEmpty()) return Optional.empty();
        return recentProjects.stream().filter(project -> project.id().equals(normalized)).findFirst();
    }

    public ProjectPickerPanel registerCommands(CommandManager commandManager) {
        CommandManager manager = Objects.requireNonNull(commandManager, "commandManager");
        manager.register(new EditorCommand(COMMAND_NEW_PROJECT, "New Project")
                .action(this::requestNewProject));
        manager.register(new EditorCommand(COMMAND_OPEN_PROJECT, "Open Project")
                .action(this::requestOpenProject));
        manager.register(new EditorCommand(COMMAND_SAVE_PROJECT, "Save Project")
                .action(this::requestSaveProject)
                .enabledWhen(() -> currentProject != null || dirty));
        manager.register(new EditorCommand(COMMAND_LAST_PROJECTS, "Recent Projects")
                .action(this::requestLastProjects)
                .enabledWhen(() -> !recentProjects.isEmpty()));
        return this;
    }

    public Label titleLabel() {
        return titleLabel;
    }

    public Label currentProjectLabel() {
        return currentProjectLabel;
    }

    public HBox actionRow() {
        return actionRow;
    }

    public VBox recentList() {
        return recentList;
    }

    public Button newButton() {
        return newButton;
    }

    public Button openButton() {
        return openButton;
    }

    public Button saveButton() {
        return saveButton;
    }

    private ProjectPickerPanel addRecentProject(ProjectReference project, boolean rebuild) {
        if (project == null) return this;
        recentProjects.removeIf(existing -> existing.id().equals(project.id()));
        recentProjects.add(0, project);
        trimRecentProjects();
        if (rebuild) rebuildRecentList();
        return this;
    }

    private boolean confirmUnsavedChanges(ProjectAction action) {
        return !dirty || unsavedChangePrompt.confirm(action);
    }

    private void emit(ProjectAction action) {
        List<Consumer<ProjectAction>> snapshot = List.copyOf(actionListeners);
        for (Consumer<ProjectAction> listener : snapshot) {
            listener.accept(action);
        }
    }

    private void refreshCurrentProjectLabel() {
        if (currentProject == null) {
            currentProjectLabel.text(dirty ? "Unsaved project *" : "No project open");
        } else {
            currentProjectLabel.text(currentProject.displayName()
                    + (dirty ? " *" : "")
                    + " - "
                    + currentProject.locationLabel());
        }
        saveButton.enabled(currentProject != null || dirty);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void rebuildRecentList() {
        recentList.clearChildren();
        recentList.applyQueuedMutations();
        if (recentProjects.isEmpty()) {
            Label empty = new Label("No recent projects");
            empty.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
            recentList.addChild(empty);
        } else {
            for (ProjectReference project : recentProjects) {
                Button row = new Button(project.displayName() + " - " + project.locationLabel());
                row.layout(style -> style.height(22.0f).flexGrow(0.0f).flexShrink(0.0f));
                row.onClick(event -> openRecentProject(project.id()));
                recentList.addChild(row);
            }
        }
        recentList.applyQueuedMutations();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void trimRecentProjects() {
        while (recentProjects.size() > maxRecentProjects) {
            recentProjects.remove(recentProjects.size() - 1);
        }
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    @FunctionalInterface
    public interface UnsavedChangePrompt {
        boolean confirm(ProjectAction action);
    }

    public enum ActionKind {
        NEW_PROJECT,
        OPEN_PROJECT,
        SAVE_PROJECT,
        LAST_PROJECTS
    }

    public record ProjectAction(ActionKind kind, ProjectReference project, String path, boolean fromRecent) {
        public ProjectAction {
            kind = kind == null ? ActionKind.OPEN_PROJECT : kind;
            path = path == null ? "" : path.trim();
        }
    }

    public record ProjectReference(String id, String displayName, String path, String resourceId) {
        public ProjectReference {
            path = path == null ? "" : path.trim();
            resourceId = resourceId == null ? "" : resourceId.trim();
            displayName = normalize(displayName, fallbackDisplayName(path, resourceId));
            id = normalize(id, !path.isEmpty() ? path : !resourceId.isEmpty() ? resourceId : displayName);
        }

        public static ProjectReference path(String id, String displayName, String path) {
            return new ProjectReference(id, displayName, path, "");
        }

        public static ProjectReference resource(String id, String displayName, String resourceId) {
            return new ProjectReference(id, displayName, "", resourceId);
        }

        public String locationLabel() {
            if (!path.isEmpty()) return path;
            if (!resourceId.isEmpty()) return resourceId;
            return "unsaved";
        }

        private static String fallbackDisplayName(String path, String resourceId) {
            if (path != null && !path.isBlank()) return path;
            if (resourceId != null && !resourceId.isBlank()) return resourceId;
            return "Untitled Project";
        }
    }
}

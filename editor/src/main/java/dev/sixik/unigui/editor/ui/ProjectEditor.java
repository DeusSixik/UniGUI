package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.editor.EditorLayout;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.EditorTheme;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.project.Project;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * Редактор проекта: панель проекта со списком файлов плюс все панели редактора экрана и компонента. Открытый в панели
 * проекта документ показывается во всех остальных панелях; для экрана контракт и граф пусты, для компонента заняты.
 *
 * <pre>{@code
 * Project project = Project.open(folder);
 * EditorShell editor = ProjectEditor.shell(project, folder, folder.resolveSibling("shop.zip"));
 * }</pre>
 */
public final class ProjectEditor {
    /** Идентификатор панели проекта. */
    public static final String PROJECT = ProjectPanel.ID;
    /** Идентификатор панели полей структуры. */
    public static final String STRUCT = StructPanel.ID;
    /** Идентификатор панели стилей. */
    public static final String STYLES = StylePanel.ID;

    private ProjectEditor() {
    }

    /**
     * Собирает редактор проекта, который сохраняется в папку проектов библиотеки, а экспортируется в её папку паков:
     * экспортированный пак игра подхватывает сама.
     *
     * @param project проект
     * @param library библиотека с папками {@code projects/} и {@code packs/}
     * @return каркас
     */
    public static EditorShell shell(Project project, dev.sixik.unigui.xaml.pack.PackLibrary library) {
        return shell(project, library.projects().resolve(project.manifest().id()),
                library.packs().resolve(project.manifest().id() + ".zip"));
    }

    /**
     * Открывает первый файл проекта, если ничего не открыто: окно редактора не стартует пустым.
     *
     * @param shell каркас редактора проекта
     */
    public static void openFirst(EditorShell shell) {
        shell.panel(PROJECT).filter(ProjectPanel.class::isInstance).map(ProjectPanel.class::cast)
                .ifPresent(ProjectPanel::openFirst);
    }

    /**
     * Собирает редактор проекта.
     *
     * @param project    проект
     * @param saveFolder папка для кнопки Save или {@code null}
     * @param exportFile файл для кнопки Export или {@code null}
     * @return каркас
     */
    public static EditorShell shell(Project project, Path saveFolder, Path exportFile) {
        return shell(project, saveFolder, exportFile, LoadOptions.DEFAULT, EditorTheme.dark(), builder -> builder);
    }

    /**
     * Собирает редактор проекта с настройками предпросмотра и темой.
     *
     * @param project        проект
     * @param saveFolder     папка для кнопки Save или {@code null}
     * @param exportFile     файл для кнопки Export или {@code null}
     * @param previewOptions настройки загрузки предпросмотра
     * @param theme          тема каркаса
     * @param customize      позволяет добавить панели, команды и раскладки до сборки
     * @return каркас
     */
    public static EditorShell shell(Project project, Path saveFolder, Path exportFile, LoadOptions previewOptions,
                                    EditorTheme theme, Function<EditorShell.Builder, EditorShell.Builder> customize) {
        ComponentView view = new ComponentView();
        EditorShell.Builder builder = UiEditor.common(project.manifest().name(), previewOptions, theme, view)
                .panel(new ContractPanel(view))
                .panel(new GraphPanel(view))
                .panel(new RunPanel(view))
                .panel(new ProjectPanel(project, saveFolder, exportFile))
                .panel(new StructPanel())
                .panel(new StylePanel())
                .layout(EditorLayout.named(UiEditor.DESIGN_LAYOUT)
                        // По две вкладки на группу: при трёх подписи уже не умещаются в ширину колонки.
                        .left(0.22f, PROJECT, ComponentEditor.CONTRACT)
                        .leftLower(0.62f, UiEditor.HIERARCHY, UiEditor.PALETTE)
                        .right(0.27f, UiEditor.INSPECTOR, STRUCT)
                        .bottom(0.2f, UiEditor.DIAGNOSTICS, ComponentEditor.RUN, STYLES)
                        .center(UiEditor.VIEWPORT, ComponentEditor.GRAPH, UiEditor.CODE)
                        .active(UiEditor.VIEWPORT)
                        .build())
                .layout(EditorLayout.named(UiEditor.CODE_LAYOUT)
                        .left(0.24f, PROJECT, ComponentEditor.CONTRACT)
                        .right(0.28f, UiEditor.INSPECTOR, STRUCT, STYLES)
                        .bottom(0.25f, UiEditor.DIAGNOSTICS)
                        .center(UiEditor.CODE, UiEditor.VIEWPORT, ComponentEditor.GRAPH)
                        .active(UiEditor.CODE)
                        .build());
        EditorShell built = customize.apply(builder).build();
        // Инспектор предлагает текстуры, шрифты и шейдеры проекта списком.
        built.context().provide(dev.sixik.unigui.xaml.edit.ResourceCatalog.class, project.resources());
        return UiEditor.finish(built, null);
    }
}

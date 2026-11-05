package dev.sixik.unigui.demo.ui.editor;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.EditorTheme;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.ToolButton;
import dev.sixik.unigui.editor.ui.ComponentEditor;
import dev.sixik.unigui.editor.ui.ProjectEditor;
import dev.sixik.unigui.editor.ui.UiEditor;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.xaml.edit.DocumentSource;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.pack.PackLibrary;
import dev.sixik.unigui.xaml.pack.PackManifest;
import dev.sixik.unigui.xaml.project.Project;

import java.nio.file.Path;

/**
 * Демонстрация редактора интерфейса: документ XAML с иерархией, палитрой виджетов, инспектором свойств, живым
 * предпросмотром, текстом документа и списком проблем. Документ хранится в памяти: сохранение (File → Save)
 * записывает его в источник, но не на диск.
 */
public final class UiEditorScreen {
    /** Документ, который открывается в редакторе. */
    public static final String SAMPLE = """
            <VBox id="root" spacing="6" padding="10">
              <Label id="title" text="Profile settings"/>
              <HBox id="nameRow" spacing="6">
                <Label text="Name"/>
                <TextField id="name" text="Steve" width="160" height="22"/>
              </HBox>
              <Checkbox id="sound" text="Sound enabled" checked="true"/>
              <Slider id="volume" min="0" max="100" value="60"/>
              <ProgressBar id="loading" value="0.4"/>
              <HBox id="buttons" spacing="6">
                <Button id="save" text="Save"/>
                <Button id="cancel" text="Cancel"/>
              </HBox>
            </VBox>
            """;

    /** Компонент, который открывается в редакторе компонентов: карточка товара с контрактом и графом. */
    public static final String COMPONENT_SAMPLE = """
            <Component x:Name="ProductCard" xmlns:x="https://unigui.dev/xaml">
              <Component.Contract>
                <Param Name="Title" Type="string" Default="Iron Sword"/>
                <Param Name="Price" Type="int" Default="25"/>
                <Event Name="Buy" Direction="Out" Args="title:string"/>
                <Event Name="Refresh" Direction="In"/>
              </Component.Contract>
              <VBox id="card" spacing="4" padding="8">
                <Label id="title" text="{Param Title}"/>
                <Label id="price" text="{Param Price}"/>
                <Button id="buy" text="Buy" onClick="{Raise Buy}"/>
              </VBox>
              <Component.Graph>
                <Node Id="refresh" Type="Event.Refresh" X="0" Y="0"/>
                <Node Id="price" Type="Param.Get" Name="Price" X="0" Y="110"/>
                <Node Id="free" Type="Value.Literal" ValueType="int" Value="0" X="0" Y="190"/>
                <Node Id="paid" Type="Math.Compare" Op="&gt;" X="190" Y="120"/>
                <Node Id="check" Type="Flow.Branch" X="380" Y="0"/>
                <Node Id="reset" Type="Param.Set" Name="Title" value="Refreshed" X="570" Y="-30"/>
                <Node Id="sold" Type="Param.Set" Name="Title" value="Free" X="570" Y="110"/>
                <Wire From="refresh.then" To="check.exec"/>
                <Wire From="price.value" To="paid.a"/>
                <Wire From="free.value" To="paid.b"/>
                <Wire From="paid.result" To="check.condition"/>
                <Wire From="check.true" To="reset.exec"/>
                <Wire From="check.false" To="sold.exec"/>
              </Component.Graph>
            </Component>
            """;

    private UiEditorScreen() {
    }

    public static void open() {
        createScreen().openGui();
    }

    /** @return экран редактора компонентов; из него можно перейти к другим редакторам демо */
    public static UniScreen createComponentScreen() {
        return screen("Component Editor Demo", 1);
    }

    public static UniScreen createScreen() {
        return screen("UI Editor Demo", 0);
    }

    /** @return экран редактора проекта: список файлов, создание, проверка, сохранение и экспорт пака */
    public static UniScreen createProjectScreen() {
        return screen("Project Editor Demo", 2);
    }

    /** Проект демо: экран профиля, компонент карточки и структура товара; лежит в памяти, пока его не сохранят. */
    public static Project sampleProject() {
        Project project = Project.create(new PackManifest("demo", "1.0", "Demo shop", "Sample project of the editor demo"));
        project.createScreen("profile");
        project.createComponent("ProductCard");
        project.createStruct("Product");
        project.session("screens/profile.xaml").replaceText(SAMPLE);
        project.session("components/productCard.xaml").replaceText(COMPONENT_SAMPLE.replace("ProductCard", "ProductCard"));
        project.sync();
        return project;
    }

    private static UniScreen screen(String title, int mode) {
        DefaultUIContext context = new DefaultUIContext();
        context.scaleProvider(new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 6.0f)
                .userScale(1.8f));

        UniScreen[] screen = new UniScreen[1];
        Runnable close = () -> {
            if (screen[0] != null) screen[0].closeGui();
        };
        Runnable swap = () -> {
            close.run();
            (mode == 0 ? createComponentScreen() : mode == 1 ? createProjectScreen() : createScreen()).openGui();
        };
        Widget root = mode == 1 ? buildComponentRoot(close, swap) : mode == 2 ? buildProjectRoot(close, swap)
                : buildRoot(close, swap);
        screen[0] = new UniScreen(title, root, context)
                .scaleWithGameGui(false)
                .renderPolicy(UiRenderPolicy.vsync()).defaultFont(Fonts.defaultFace());
        return screen[0];
    }

    private static Widget buildComponentRoot(Runnable closeAction, Runnable swapAction) {
        String id = "demo/product_card.xaml";
        XamlEditorDocument document = new XamlEditorDocument(id, "product_card.xaml",
                EditorSession.open(DocumentSource.memory(id, "product_card.xaml", COMPONENT_SAMPLE)));
        EditorShell shell = ComponentEditor.shell(document, LoadOptions.DEFAULT, EditorTheme.dark(),
                builder -> builder.menuBarTrailing(owner -> swapButton("Project editor", swapAction))
                        .menuBarTrailing(owner -> closeButton(closeAction)));
        return shell.root();
    }

    private static Widget buildProjectRoot(Runnable closeAction, Runnable swapAction) {
        // Папка настроек библиотеки: проекты в config/unigui/projects, готовые паки в config/unigui/packs.
        PackLibrary library = PackLibrary.open(Path.of("config", "unigui"));
        Project sample = sampleProject();
        EditorShell shell = ProjectEditor.shell(sample, library.projects().resolve(sample.manifest().id()),
                library.packs().resolve(sample.manifest().id() + ".zip"),
                LoadOptions.DEFAULT, EditorTheme.dark(),
                builder -> builder.menuBarTrailing(owner -> swapButton("Screen editor", swapAction))
                        .menuBarTrailing(owner -> closeButton(closeAction)));
        ProjectEditor.openFirst(shell);
        return shell.root();
    }

    private static Widget buildRoot(Runnable closeAction, Runnable swapAction) {
        String id = "demo/profile.xaml";
        XamlEditorDocument document = new XamlEditorDocument(id, "profile.xaml",
                EditorSession.open(DocumentSource.memory(id, "profile.xaml", SAMPLE)));
        EditorShell shell = UiEditor.shell(document, LoadOptions.DEFAULT, EditorTheme.dark(),
                builder -> builder.menuBarTrailing(owner -> swapButton("Component editor", swapAction))
                        .menuBarTrailing(owner -> closeButton(closeAction)));
        return shell.root();
    }

    private static ToolButton swapButton(String target, Runnable swapAction) {
        ToolButton swap = new ToolButton(EditorIcons.SWAP, target, ToolButton.Variant.SOLID);
        swap.onClick(event -> swapAction.run());
        return swap;
    }

    private static ToolButton closeButton(Runnable closeAction) {
        ToolButton close = new ToolButton(EditorIcons.CLOSE, "Close Demo", ToolButton.Variant.GHOST);
        close.onClick(event -> closeAction.run());
        return close;
    }
}

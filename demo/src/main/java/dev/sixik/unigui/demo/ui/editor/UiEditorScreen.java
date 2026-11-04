package dev.sixik.unigui.demo.ui.editor;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.demo.ui.unity.UnityEditorTheme;
import dev.sixik.unigui.demo.ui.unity.UnityStyles;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.editor.ui.UiEditor;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.xaml.edit.DocumentSource;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.load.LoadOptions;

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
                <TextField id="name" text="Steve"/>
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

    private UiEditorScreen() {
    }

    public static void open() {
        createScreen().openGui();
    }

    public static UniScreen createScreen() {
        DefaultUIContext context = new DefaultUIContext();
        context.scaleProvider(new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 6.0f)
                .userScale(1.8f));

        UniScreen[] screen = new UniScreen[1];
        Widget root = buildRoot(() -> {
            if (screen[0] != null) screen[0].closeGui();
        });
        screen[0] = new UniScreen("UI Editor Demo", root, context)
                .scaleWithGameGui(false)
                .renderPolicy(UiRenderPolicy.vsync()).defaultFont(Fonts.defaultFace());
        return screen[0];
    }

    private static Widget buildRoot(Runnable closeAction) {
        String id = "demo/profile.xaml";
        XamlEditorDocument document = new XamlEditorDocument(id, "profile.xaml",
                EditorSession.open(DocumentSource.memory(id, "profile.xaml", SAMPLE)));
        EditorShell shell = UiEditor.shell(document, LoadOptions.DEFAULT, new UnityEditorTheme(),
                builder -> builder.menuBarTrailing(owner -> closeButton(closeAction)));
        return shell.root();
    }

    private static Button closeButton(Runnable closeAction) {
        Button close = new Button("✕ Close Demo");
        close.themeEnabled(false);
        close.backgroundVisible(true);
        close.background().set(UnityStyles.BG_BUTTON);
        close.borderVisible(true);
        close.borderColor().set(UnityStyles.BORDER_BUTTON);
        close.radius(2.0f);
        close.focusRingVisible(false);
        close.focusable(false);
        close.textColor().set(UnityStyles.TEXT_BOLD);
        close.layout(style -> style.size(90.0f, 18.0f));
        close.onClick(event -> closeAction.run());
        return close;
    }
}

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
import dev.sixik.unigui.editor.ui.ComponentEditor;
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
                <Node Id="reset" Type="Param.Set" Name="Title" value="Refreshed" X="220" Y="0"/>
                <Wire From="refresh.then" To="reset.exec"/>
              </Component.Graph>
            </Component>
            """;

    private UiEditorScreen() {
    }

    public static void open() {
        createScreen().openGui();
    }

    /** @return экран редактора компонентов; из него можно перейти в редактор экрана и обратно */
    public static UniScreen createComponentScreen() {
        return screen("Component Editor Demo", true);
    }

    public static UniScreen createScreen() {
        return screen("UI Editor Demo", false);
    }

    private static UniScreen screen(String title, boolean component) {
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
            (component ? createScreen() : createComponentScreen()).openGui();
        };
        Widget root = component ? buildComponentRoot(close, swap) : buildRoot(close, swap);
        screen[0] = new UniScreen(title, root, context)
                .scaleWithGameGui(false)
                .renderPolicy(UiRenderPolicy.vsync()).defaultFont(Fonts.defaultFace());
        return screen[0];
    }

    private static Widget buildComponentRoot(Runnable closeAction, Runnable swapAction) {
        String id = "demo/product_card.xaml";
        XamlEditorDocument document = new XamlEditorDocument(id, "product_card.xaml",
                EditorSession.open(DocumentSource.memory(id, "product_card.xaml", COMPONENT_SAMPLE)));
        EditorShell shell = ComponentEditor.shell(document, LoadOptions.DEFAULT, new UnityEditorTheme(),
                builder -> builder.menuBarTrailing(owner -> swapButton("Screen editor", swapAction))
                        .menuBarTrailing(owner -> closeButton(closeAction)));
        return shell.root();
    }

    private static Widget buildRoot(Runnable closeAction, Runnable swapAction) {
        String id = "demo/profile.xaml";
        XamlEditorDocument document = new XamlEditorDocument(id, "profile.xaml",
                EditorSession.open(DocumentSource.memory(id, "profile.xaml", SAMPLE)));
        EditorShell shell = UiEditor.shell(document, LoadOptions.DEFAULT, new UnityEditorTheme(),
                builder -> builder.menuBarTrailing(owner -> swapButton("Component editor", swapAction))
                        .menuBarTrailing(owner -> closeButton(closeAction)));
        return shell.root();
    }

    private static Button swapButton(String target, Runnable swapAction) {
        Button swap = styledButton("⇄ " + target, 120.0f);
        swap.onClick(event -> swapAction.run());
        return swap;
    }

    private static Button closeButton(Runnable closeAction) {
        Button close = styledButton("✕ Close Demo", 90.0f);
        close.onClick(event -> closeAction.run());
        return close;
    }

    private static Button styledButton(String text, float width) {
        Button close = new Button(text);
        close.themeEnabled(false);
        close.backgroundVisible(true);
        close.background().set(UnityStyles.BG_BUTTON);
        close.borderVisible(true);
        close.borderColor().set(UnityStyles.BORDER_BUTTON);
        close.radius(2.0f);
        close.focusRingVisible(false);
        close.focusable(false);
        close.textColor().set(UnityStyles.TEXT_BOLD);
        close.layout(style -> style.size(width, 18.0f));
        return close;
    }
}

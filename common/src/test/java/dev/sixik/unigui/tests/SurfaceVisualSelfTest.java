package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.FocusGainedEvent;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.SliceBorders;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.style.MutableTheme;
import dev.sixik.unigui.api.style.StylePack;
import dev.sixik.unigui.api.style.StylePackResult;
import dev.sixik.unigui.api.style.StylePackXml;
import dev.sixik.unigui.api.style.StyleKeyRegistry;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.widget.visual.BackgroundKind;
import dev.sixik.unigui.api.widget.visual.Surface;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.TextField;

/**
 * Проверяет ядро визуала: композируемый Surface вместо двойных renderer'ов.
 *
 * <p>Покрывает автовывод {@link BackgroundKind}, композицию Button поверх Surface
 * вместо наследования Box и однократную отрисовку фона overlay-виджетов.</p>
 */
public final class SurfaceVisualSelfTest {
    private SurfaceVisualSelfTest() {
    }

    public static void main(String[] args) {
        expect(BackgroundKind.parse("color") == BackgroundKind.COLOR, "parse color");
        expect(BackgroundKind.parse("fill") == BackgroundKind.COLOR, "parse fill alias");
        expect(BackgroundKind.parse("texture") == BackgroundKind.TEXTURE, "parse texture");
        expect(BackgroundKind.parse("image") == BackgroundKind.TEXTURE, "parse image alias");
        expect(BackgroundKind.parse("shader") == BackgroundKind.SHADER, "parse shader");
        expect(BackgroundKind.parse("none") == BackgroundKind.NONE, "parse none");
        expect(BackgroundKind.parse(null) == null, "parse null");
        expect(BackgroundKind.parse("  ") == null, "parse blank");
        expectThrows(() -> BackgroundKind.parse("bogus"), "parse bogus should throw");

        Surface auto = new Surface(null);
        expect(auto.effectiveKind() == BackgroundKind.NONE, "default kind is NONE");
        auto.backgroundVisible(true);
        expect(auto.effectiveKind() == BackgroundKind.COLOR, "visible surface defaults to COLOR");

        Button button = new Button("Test");
        expect(!(((Object) button) instanceof Box), "button no longer inherits Box visual");
        expect(button.surface() != null, "button owns a surface");
        expect(button.backgroundVisible() && button.borderVisible(), "button chrome enabled by default");
        expect(button.renderer() == null, "button has no custom renderer by default");
        expect(button.backgroundKind() == null, "button kind is automatic by default");

        Box box = new Box();
        expect(box.surface() != null, "box owns a surface");
        expect(box.renderer() == null, "box has no custom renderer by default");
        expect(new Checkbox("Check") instanceof Button, "checkbox still is a button");
        expect(!(((Object) new Checkbox("Check")) instanceof Box), "checkbox no longer inherits Box visual");

        // LinearBox owns a surface too, but renders transparent by default so
        // existing mods see no visual change after the SurfaceWidget migration.
        VBox vbox = new VBox();
        expect(!vbox.boxVisualEnabled(), "linear box surface is disabled by default");
        expect(!vbox.themeEnabled(), "linear box theme lookup is disabled by default");
        expect(!vbox.backgroundVisible() && !vbox.borderVisible(), "linear box chrome is hidden by default");
        vbox.measure(new LayoutContext(120.0f, 60.0f));
        vbox.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 60.0f));
        DrawList vboxDraw = new DrawList();
        vbox.render(new DefaultRenderContext(vboxDraw));
        expect(vboxDraw.size() == 0, "linear box draws nothing by default");

        // First explicit background opt-ins into surface rendering automatically.
        HBox hbox = new HBox();
        hbox.background(new dev.sixik.unigui.api.math.MutableColor(0.10f, 0.20f, 0.30f, 0.40f));
        expect(hbox.boxVisualEnabled(), "explicit background enables linear box surface");
        expect(hbox.backgroundVisible(), "explicit background marks background visible");
        hbox.measure(new LayoutContext(120.0f, 60.0f));
        hbox.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 60.0f));
        DrawList hboxDraw = new DrawList();
        hbox.render(new DefaultRenderContext(hboxDraw));
        expect(countFill(hboxDraw, 0.10f, 0.20f, 0.30f, 0.40f) == 1,
                "explicit background is drawn exactly once by the linear box surface");

        // Overlay windows draw their background exactly once through the surface.
        WindowWidget window = new WindowWidget("Title", new Label("Body"));
        window.open();
        window.measure(new LayoutContext(220.0f, 120.0f));
        window.arrange(new MutableRect(0.0f, 0.0f, 220.0f, 120.0f));
        DrawList windowDraw = new DrawList();
        window.render(new DefaultRenderContext(windowDraw));
        expect(countFill(windowDraw, 0.030f, 0.035f, 0.050f, 0.98f) == 1,
                "window background is drawn exactly once by the surface");

        // Tooltip background comes from the surface as well.
        Box anchor = new Box();
        anchor.arrange(new MutableRect(0.0f, 0.0f, 60.0f, 20.0f));
        Tooltip tooltip = new Tooltip(anchor, "Hi");
        anchor.handle(new PointerEnteredEvent(anchor, anchor, EventPhase.TARGET, 0.0f, 0.0f, 0.0f, 0.0f, 0));
        expect(tooltip.showing(), "hovered anchor shows tooltip");
        tooltip.measure(new LayoutContext(220.0f, 120.0f));
        tooltip.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 40.0f));
        DrawList tooltipDraw = new DrawList();
        tooltip.render(new DefaultRenderContext(tooltipDraw));
        expect(countFill(tooltipDraw, 0.02f, 0.025f, 0.035f, 0.94f) == 1,
                "tooltip background is drawn exactly once by the surface");

        // Nine-slice background emits 9 texture cells through the surface.
        Box sliced = new Box();
        sliced.themeEnabled(false);
        sliced.backgroundTexture(new dev.sixik.unigui.api.render.SimpleTextureHandle("test:panel", 20, 20));
        sliced.backgroundVisible(true);
        sliced.backgroundTextureFit(dev.sixik.unigui.api.render.ImageFit.NINE_SLICE);
        sliced.sliceBorders(6.0f);
        sliced.measure(new LayoutContext(120.0f, 60.0f));
        sliced.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 60.0f));
        DrawList slicedDraw = new DrawList();
        sliced.render(new DefaultRenderContext(slicedDraw));
        expect(countType(slicedDraw, dev.sixik.unigui.api.render.DrawCommandType.TEXTURE) == 9,
                "nine-slice background should emit 9 texture cells");

        // Overlay texture stack renders after the base background, in order.
        Box layered = new Box();
        layered.themeEnabled(false);
        layered.background(new dev.sixik.unigui.api.math.MutableColor(0.10f, 0.10f, 0.10f, 1.0f));
        layered.backgroundTexture(new dev.sixik.unigui.api.render.SimpleTextureHandle("test:base", 16, 16));
        layered.overlayTexture(new dev.sixik.unigui.api.render.SimpleTextureHandle("test:first", 16, 16));
        layered.overlayTexture(new dev.sixik.unigui.api.render.SimpleTextureHandle("test:second", 16, 16));
        layered.overlayTexture(new dev.sixik.unigui.api.render.TextureLayer(null).visible(false));
        layered.measure(new LayoutContext(120.0f, 60.0f));
        layered.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 60.0f));
        DrawList layeredDraw = new DrawList();
        layered.render(new DefaultRenderContext(layeredDraw));
        expect(textureIds(layeredDraw).equals(java.util.List.of("test:base", "test:first", "test:second")),
                "overlay textures should render after the base in insertion order, skipping empty layers");
        expect(layered.overlayTextures().size() == 3, "overlay stack should expose added layers");

        layered.clearOverlayTextures();
        DrawList clearedDraw = new DrawList();
        layered.render(new DefaultRenderContext(clearedDraw));
        expect(textureIds(clearedDraw).equals(java.util.List.of("test:base")),
                "clearOverlayTextures should leave only the base background texture");

        Box localStyled = new Box();
        DefaultUIContext localStyleContext = new DefaultUIContext();
        localStyled.setUiContextInternal(localStyleContext);
        MutableTheme localStyleTheme = new MutableTheme().put("Box",
                new MutableStyle().put(StyleKeys.BACKGROUND_VISIBLE, true)
                        .put(StyleKeys.BACKGROUND_COLOR,
                                dev.sixik.unigui.api.math.MutableColor.rgba(0.8f, 0.1f, 0.1f, 1.0f)));
        localStyleContext.theme(localStyleTheme, localStyled);
        localStyled.themeEnabled(false);
        localStyled.style(style -> style.put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_COLOR,
                        dev.sixik.unigui.api.math.MutableColor.rgba(0.2f, 0.3f, 0.4f, 1.0f)));
        localStyled.measure(new LayoutContext(40.0f, 20.0f));
        localStyled.arrange(new MutableRect(0.0f, 0.0f, 40.0f, 20.0f));
        DrawList localStyleDraw = new DrawList();
        localStyled.render(new DefaultRenderContext(localStyleDraw));
        expect(countFill(localStyleDraw, 0.2f, 0.3f, 0.4f, 1.0f) == 1,
                "local style should apply while global theme lookup is disabled");
        expect(countFill(localStyleDraw, 0.8f, 0.1f, 0.1f, 1.0f) == 0,
                "global theme values should be ignored while theme lookup is disabled");

        int[] localRendererCalls = {0};
        Box localRenderer = new Box();
        localRenderer.themeEnabled(false);
        localRenderer.style(style -> style.renderer(
                (WidgetRender) (draw, widget) -> localRendererCalls[0]++));
        localRenderer.measure(new LayoutContext(40.0f, 20.0f));
        localRenderer.arrange(new MutableRect(0.0f, 0.0f, 40.0f, 20.0f));
        localRenderer.render(new DefaultRenderContext(new DrawList()));
        expect(localRendererCalls[0] == 1,
                "local renderer override should apply while global theme lookup is disabled");

        TextField search = new TextField();
        search.boxVisualEnabled(true);
        search.themeEnabled(false);
        search.style(style -> style
                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                .put(StyleKeys.BACKGROUND_KIND, BackgroundKind.TEXTURE)
                .put(StyleKeys.BACKGROUND_TEXTURE, WidgetState.NORMAL,
                        new SimpleTextureHandle("test:search-normal", 20, 20))
                .put(StyleKeys.BACKGROUND_TEXTURE, WidgetState.FOCUSED,
                        new SimpleTextureHandle("test:search-focused", 20, 20))
                .put(StyleKeys.BACKGROUND_TEXTURE_FIT, WidgetState.NORMAL, ImageFit.NINE_SLICE)
                .put(StyleKeys.BACKGROUND_TEXTURE_FIT, WidgetState.FOCUSED, ImageFit.STRETCH)
                .put(StyleKeys.SLICE_BORDERS, WidgetState.NORMAL, SliceBorders.all(4.0f))
                .put(StyleKeys.BORDER_VISIBLE, false)
                .put(StyleKeys.CARET_COLOR, dev.sixik.unigui.api.math.MutableColor.rgba(1, 1, 1, 1))
                .put(StyleKeys.FOCUS_RING_COLOR, WidgetState.FOCUSED,
                        dev.sixik.unigui.api.math.MutableColor.rgba(0, 0, 0, 0)));
        search.measure(new LayoutContext(80.0f, 20.0f));
        search.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 20.0f));
        DrawList normalSearchDraw = new DrawList();
        search.render(new DefaultRenderContext(normalSearchDraw));
        expect(textureIds(normalSearchDraw).equals(java.util.Collections.nCopies(9, "test:search-normal")),
                "normal state should use its texture and nine-slice fit");
        expect(near(search.caretColor().r(), 1.0f),
                "local caret style should apply with global theme disabled");

        search.handle(new FocusGainedEvent(search, null));
        DrawList focusedSearchDraw = new DrawList();
        search.render(new DefaultRenderContext(focusedSearchDraw));
        expect(textureIds(focusedSearchDraw).equals(java.util.List.of("test:search-focused")),
                "focused state should override texture and use its own fit");
        expect(near(search.focusRingColor().a(), 0.0f),
                "focused local focus-ring style should override the default ring");

        String stylePackXml = "<StylePack id=\"state-textures\"><Style id=\"Search\" target=\"TextField\">"
                + "<Setter property=\"background.kind\" value=\"texture\"/>"
                + "<Setter property=\"background.texture\" value=\"test:normal 20 20\"/>"
                + "<Setter property=\"background.texture.fit\" value=\"nine-slice\"/>"
                + "<Setter property=\"background.texture.slice-borders\" value=\"6\"/>"
                + "<State name=\"FOCUSED\"><Setter property=\"background.texture\" value=\"test:focused 20 20\"/>"
                + "<Setter property=\"background.texture.fit\" value=\"stretch\"/></State>"
                + "</Style></StylePack>";
        StylePackResult parsedStyles = StylePackXml.parseEditor(stylePackXml, StyleKeyRegistry.builtIns());
        expect(parsedStyles.valid(), "state texture style pack should parse: " + parsedStyles.diagnosticMessages());
        StylePack roundTripped = StylePackXml.parse(
                StylePackXml.toXmlString(parsedStyles.pack()), StyleKeyRegistry.builtIns());
        expect(roundTripped.styleFor("TextField")
                        .get(StyleKeys.BACKGROUND_TEXTURE, WidgetState.FOCUSED, null).id().equals("test:focused"),
                "StylePack should round-trip focused texture handles");
        expect(roundTripped.styleFor("TextField")
                        .get(StyleKeys.SLICE_BORDERS, WidgetState.NORMAL, null).equals(SliceBorders.all(6.0f)),
                "StylePack should round-trip nine-slice borders");

        System.out.println("SurfaceVisualSelfTest passed");
    }

    private static int countFill(DrawList drawList, float r, float g, float b, float a) {
        int count = 0;
        Object[] commands = drawList.commandElements();
        for (int i = 0, size = drawList.size(); i < size; i++) {
            if (commands[i] instanceof DrawCommand command
                    && command.paint() != null
                    && !command.paint().isStroke()
                    && near(command.paint().color().r(), r)
                    && near(command.paint().color().g(), g)
                    && near(command.paint().color().b(), b)
                    && near(command.paint().color().a(), a)) {
                count++;
            }
        }
        return count;
    }

    private static boolean near(float actual, float expected) {
        return Math.abs(actual - expected) <= 0.01f;
    }

    private static int countType(DrawList drawList, dev.sixik.unigui.api.render.DrawCommandType type) {
        int count = 0;
        Object[] commands = drawList.commandElements();
        for (int i = 0, size = drawList.size(); i < size; i++) {
            if (commands[i] instanceof DrawCommand command && command.type() == type) {
                count++;
            }
        }
        return count;
    }

    private static java.util.List<String> textureIds(DrawList drawList) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        Object[] commands = drawList.commandElements();
        for (int i = 0, size = drawList.size(); i < size; i++) {
            if (commands[i] instanceof DrawCommand command
                    && command.type() == dev.sixik.unigui.api.render.DrawCommandType.TEXTURE
                    && command.texture() != null) {
                ids.add(command.texture().id());
            }
        }
        return ids;
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectThrows(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }
}

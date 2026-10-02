package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.render.plan.RenderPlan;
import dev.sixik.unigui.api.render.plan.RenderPrimitive;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeyRegistry;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.widget.visual.BackgroundKind;
import dev.sixik.unigui.api.widget.visual.Surface;
import dev.sixik.unigui.api.widget.visual.SurfaceSnapshot;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.render.BoxState;
import dev.sixik.unigui.widgets.render.SurfacePlans;

/**
 * Проверяет новое ядро визуала: композируемый Surface вместо двойных renderer'ов.
 *
 * <p>Покрывает автовывод {@link BackgroundKind}, планы поверхности для цвета,
 * текстуры и шейдера, legacy-совместимость {@link BoxState} и композицию
 * Button поверх Surface вместо наследования Box.</p>
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

        expect(StyleKeys.BACKGROUND_KIND.id().equals("background.kind"), "kind key id");
        expect(StyleKeys.BACKGROUND_SHADER.id().equals("background.shader"), "shader key id");
        StyleKeyRegistry registry = StyleKeyRegistry.builtIns();
        expect(registry.descriptor(StyleKeys.BACKGROUND_KIND).isPresent(), "kind descriptor registered");
        expect(registry.descriptor(StyleKeys.BACKGROUND_KIND).orElseThrow().parse("texture") == BackgroundKind.TEXTURE,
                "kind descriptor parses texture");
        expect(registry.descriptor(StyleKeys.BACKGROUND_SHADER).isPresent(), "shader descriptor registered");
        expect("unigui:noise".equals(registry.descriptor(StyleKeys.BACKGROUND_SHADER).orElseThrow()
                .parse("unigui:noise").id()), "shader descriptor parses resource id");

        TextureHandle texture = new SimpleTextureHandle("test:tex", 16, 16);
        ShaderHandle shader = ShaderHandle.resource("test:shader");

        Surface auto = new Surface(null);
        expect(auto.effectiveKind() == BackgroundKind.NONE, "default kind is NONE");
        auto.backgroundVisible(true);
        expect(auto.effectiveKind() == BackgroundKind.COLOR, "visible surface defaults to COLOR");
        auto.backgroundTexture(texture);
        expect(auto.effectiveKind() == BackgroundKind.TEXTURE, "texture switches kind to TEXTURE");
        auto.backgroundShader(shader);
        expect(auto.effectiveKind() == BackgroundKind.SHADER, "shader wins over texture");
        auto.backgroundKind(BackgroundKind.NONE);
        expect(auto.effectiveKind() == BackgroundKind.NONE, "explicit kind overrides auto");
        auto.backgroundKind(BackgroundKind.TEXTURE);
        auto.backgroundTexture(null);
        auto.backgroundShader(null);
        expect(auto.effectiveKind() == BackgroundKind.TEXTURE, "explicit kind kept without texture");

        Surface colored = new Surface(null);
        colored.backgroundVisible(true);
        colored.background().set(1.0f, 0.0f, 0.0f, 1.0f);
        SurfaceSnapshot colorSnapshot = colored.snapshot(new MutableRect(0.0f, 0.0f, 10.0f, 10.0f));
        expect(colorSnapshot.kind() == BackgroundKind.COLOR && colorSnapshot.backgroundVisible(),
                "color snapshot kind");
        RenderPlan colorPlan = SurfacePlans.defaultPlan(colorSnapshot);
        expect(colorPlan.primitives().size() == 1
                        && colorPlan.primitives().get(0) instanceof RenderPrimitive.RoundedRect,
                "color plan is a single rounded rect");

        SurfaceSnapshot bordered = new SurfaceSnapshot(0.0f, 0.0f, 10.0f, 10.0f,
                BackgroundKind.COLOR, true, new MutableColor(1.0f, 0.0f, 0.0f, 1.0f),
                null, null, null, ImageFit.STRETCH, null, null, null,
                2.0f, true, new MutableColor(1.0f, 1.0f, 1.0f, 1.0f), 1.0f);
        expect(SurfacePlans.defaultPlan(bordered).primitives().size() == 2, "border adds a primitive");

        SurfaceSnapshot hidden = new SurfaceSnapshot(0.0f, 0.0f, 10.0f, 10.0f,
                BackgroundKind.NONE, false, null, null, null, null, ImageFit.STRETCH,
                null, null, null, 0.0f, false, null, 0.0f);
        expect(SurfacePlans.defaultPlan(hidden).empty(), "none kind draws nothing");

        TexturePlacement placement = new TexturePlacement(0.0f, 0.0f, 10.0f, 10.0f, 0.0f, 0.0f, 1.0f, 1.0f);
        SurfaceSnapshot textured = new SurfaceSnapshot(0.0f, 0.0f, 10.0f, 10.0f,
                BackgroundKind.TEXTURE, true, new MutableColor(0.0f, 0.0f, 0.0f, 1.0f),
                texture, new MutableColor(1.0f, 1.0f, 1.0f, 1.0f), placement, ImageFit.STRETCH,
                null, null, null, 0.0f, false, null, 0.0f);
        RenderPlan texturePlan = SurfacePlans.defaultPlan(textured);
        expect(texturePlan.primitives().size() == 2
                        && texturePlan.primitives().get(1) instanceof RenderPrimitive.Texture,
                "texture kind draws underlay plus texture");

        ShaderUniforms uniforms = ShaderUniforms.create().setFloat("u_time", 1.0f);
        SurfaceSnapshot shaded = new SurfaceSnapshot(0.0f, 0.0f, 10.0f, 10.0f,
                BackgroundKind.SHADER, true, null, null, null, null, ImageFit.STRETCH,
                shader, uniforms, ShaderDrawOptions.defaults(), 0.0f, false, null, 0.0f);
        RenderPlan shaderPlan = SurfacePlans.defaultPlan(shaded);
        expect(shaderPlan.primitives().size() == 1
                        && shaderPlan.primitives().get(0) instanceof RenderPrimitive.Shader,
                "shader kind draws a shader primitive");
        RenderPlan styledShaderPlan = SurfacePlans.styledPlan(shaded, Style.EMPTY, WidgetState.NORMAL);
        expect(styledShaderPlan.primitives().size() == 1
                        && styledShaderPlan.primitives().get(0) instanceof RenderPrimitive.Shader,
                "empty style keeps shader plan");

        DrawList drawList = new DrawList();
        DefaultRenderContext renderContext = new DefaultRenderContext(drawList);
        shaderPlan.render(new DrawScope(renderContext, null));
        expect(drawList.size() == 1, "shader plan writes one command");
        Object[] commands = drawList.commandElements();
        expect(commands[0] instanceof DrawCommand command && command.type() == DrawCommandType.SHADER,
                "shader plan writes a SHADER command");

        BoxState legacyColor = new BoxState(0.0f, 0.0f, 10.0f, 10.0f, true,
                new MutableColor(1.0f, 0.0f, 0.0f, 1.0f), null, null, null, ImageFit.STRETCH,
                0.0f, false, null, 0.0f);
        expect(legacyColor.backgroundKind() == BackgroundKind.COLOR, "legacy color maps to COLOR kind");
        BoxState legacyTexture = new BoxState(0.0f, 0.0f, 10.0f, 10.0f, true,
                new MutableColor(0.0f, 0.0f, 0.0f, 1.0f), texture, null, placement, ImageFit.STRETCH,
                0.0f, false, null, 0.0f);
        expect(legacyTexture.backgroundKind() == BackgroundKind.TEXTURE, "legacy texture maps to TEXTURE kind");
        BoxState roundTripped = BoxState.fromSurface(shaded);
        expect(roundTripped.backgroundKind() == BackgroundKind.SHADER
                        && roundTripped.backgroundShader() == shader
                        && roundTripped.toSurface().backgroundShader() == shader,
                "surface roundtrip keeps shader");

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

        System.out.println("SurfaceVisualSelfTest passed");
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

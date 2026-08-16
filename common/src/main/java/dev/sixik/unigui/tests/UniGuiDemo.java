package dev.sixik.unigui.tests;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.brigadier.CommandDispatcher;
import dev.sixik.unigui.api.core.MutableUIScaleProvider;
import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.animation.TransformOrigin;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.BlendMode;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import dev.sixik.unigui.api.selection.SelectionMode;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.widget.CheckboxState;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XMLWidget;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnosticsPanel;
import dev.sixik.unigui.api.xml.XmlWidgetHotReloadPreview;
import dev.sixik.unigui.api.xml.XmlWidgetHotReloadSource;
import dev.sixik.unigui.widgets.minecraft.MinecraftBlockPreviewWidget;
import dev.sixik.unigui.backend.minecraft.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft.UniGuiTextures;
import dev.sixik.unigui.widgets.minecraft.MinecraftEntityPreviewWidget;
import dev.sixik.unigui.backend.minecraft.MinecraftFonts;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemPickerWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemTooltip;
import dev.sixik.unigui.widgets.minecraft.MinecraftTexturePickerWidget;
import dev.sixik.unigui.backend.minecraft.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.map.*;
import dev.sixik.unigui.widgets.render.DockSplitHandleRenderers;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.time.Duration;
import java.time.LocalDate;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import dev.sixik.unigui.widgets.containers.Border;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.DockPanel;
import dev.sixik.unigui.widgets.containers.DockSide;
import dev.sixik.unigui.widgets.containers.GridBox;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.containers.PanelRowWidget;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.SettingRow;
import dev.sixik.unigui.widgets.containers.SplitPanel;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.containers.View;
import dev.sixik.unigui.widgets.containers.WrapPanel;
import dev.sixik.unigui.widgets.core.Widgets;
import dev.sixik.unigui.widgets.data.VirtualListView;
import dev.sixik.unigui.widgets.data.VirtualTableColumn;
import dev.sixik.unigui.widgets.data.VirtualTableView;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Chart;
import dev.sixik.unigui.widgets.display.ImageView;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.Path;
import dev.sixik.unigui.widgets.display.RichTextView;
import dev.sixik.unigui.widgets.display.Separator;
import dev.sixik.unigui.widgets.display.Shape;
import dev.sixik.unigui.widgets.display.Sparkline;
import dev.sixik.unigui.widgets.display.Text;
import dev.sixik.unigui.widgets.display.TextBlock;
import dev.sixik.unigui.widgets.display.TextureWidget;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.docking.DockArea;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.docking.DockPane;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.LoadingIndicator;
import dev.sixik.unigui.widgets.feedback.NotificationView;
import dev.sixik.unigui.widgets.feedback.Popup;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import dev.sixik.unigui.widgets.feedback.Spinner;
import dev.sixik.unigui.widgets.feedback.Toast;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import dev.sixik.unigui.widgets.feedback.WindowManager;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.graph.GraphView;
import dev.sixik.unigui.widgets.graph.NodeGraph;
import dev.sixik.unigui.widgets.graph.NodeGraphItem;
import dev.sixik.unigui.widgets.graph.NodeGraphPortKind;
import dev.sixik.unigui.widgets.graph.NodeGraphPortRef;
import dev.sixik.unigui.widgets.graph.NodeGraphPortSide;
import dev.sixik.unigui.widgets.graph.NodeGraphSelectionMode;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.ColorPicker;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.DatePicker;
import dev.sixik.unigui.widgets.interaction.DropDownBox;
import dev.sixik.unigui.widgets.interaction.HoldButton;
import dev.sixik.unigui.widgets.interaction.NumberField;
import dev.sixik.unigui.widgets.interaction.PasswordField;
import dev.sixik.unigui.widgets.interaction.RadioButton;
import dev.sixik.unigui.widgets.interaction.RadioGroup;
import dev.sixik.unigui.widgets.interaction.SearchField;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.widgets.interaction.TimeSpanField;
import dev.sixik.unigui.widgets.interaction.ToggleButton;
import dev.sixik.unigui.widgets.interaction.ToggleSwitch;
import dev.sixik.unigui.widgets.interaction.TreeListPicker;
import dev.sixik.unigui.widgets.navigation.Accordion;
import dev.sixik.unigui.widgets.navigation.Breadcrumb;
import dev.sixik.unigui.widgets.navigation.Carousel;
import dev.sixik.unigui.widgets.navigation.ExpandablePanel;
import dev.sixik.unigui.widgets.navigation.PageView;
import dev.sixik.unigui.widgets.navigation.TabControl;
import dev.sixik.unigui.widgets.navigation.TreeList;
import dev.sixik.unigui.widgets.navigation.TreeView;
import dev.sixik.unigui.widgets.navigation.TreeViewNode;
import dev.sixik.unigui.widgets.world.WorldCanvas;
import org.intellij.lang.annotations.Language;

public final class UniGuiDemo {
    private static final MutableUIScaleProvider SCALE = new MutableUIScaleProvider(2.0f);
    private static final String XML_OVERVIEW_RESOURCE = "assets/unigui/xml/overview.xml";
    private static final String XML_DEMO_RESOURCE = "assets/unigui/xml/xml_demo.xml";
    private static final String XML_DEMO_CLOUD_TEXTURE_ID = "unigui:dynamic/xml_demo_uniformclouds";
    private static final String XML_DEMO_CLOUD_TEXTURE_RESOURCE = "assets/unigui_testmod/textures/gui/uniformclouds-1.png";
    private static boolean xmlDemoCloudTextureLoadFailed;

    private static final ShaderHandle MAP_AURORA_BACKGROUND_SHADER = ShaderHandle.source(
            "unigui:demo_map_aurora_background",
            """
            #version 150

            in vec3 Position;

            out vec2 mapUv;
            out vec2 localCoord;
            out vec2 quadSize;

            uniform vec2 ScreenSize;
            uniform vec4 SquareVertex;

            void main() {
                vec2 rawUv = Position.xy * 0.5 + 0.5;
                mapUv = vec2(rawUv.x, 1.0 - rawUv.y);

                vec2 rectMin = min(SquareVertex.xy, SquareVertex.zw);
                vec2 rectMax = max(SquareVertex.xy, SquareVertex.zw);
                quadSize = max(rectMax - rectMin, vec2(1.0));
                localCoord = mapUv * quadSize;

                vec2 pixel = mix(rectMin, rectMax, mapUv);
                vec2 ndc = vec2(
                        pixel.x / max(ScreenSize.x, 1.0) * 2.0 - 1.0,
                        1.0 - pixel.y / max(ScreenSize.y, 1.0) * 2.0);
                gl_Position = vec4(ndc, 0.0, 1.0);
            }
            """,
            """
            #version 150

            in vec2 mapUv;
            in vec2 localCoord;
            in vec2 quadSize;

            out vec4 FragColor;

            uniform float Time;
            uniform vec4 AuroraColor;
            uniform vec4 BackgroundColor1;
            uniform vec4 BackgroundColor2;
            uniform float AuroraIntensity;
            uniform float StarBrightness;

            const float PI = 3.14159265358979323846264;
            const mat2 M2 = mat2(0.95534, 0.29552, -0.29552, 0.95534);

            mat2 mm2(float a) {
                float c = cos(a);
                float s = sin(a);
                return mat2(c, s, -s, c);
            }

            float tri(float x) {
                return clamp(abs(fract(x) - 0.5), 0.01, 0.49);
            }

            vec2 tri2(vec2 p) {
                return vec2(tri(p.x) + tri(p.y), tri(p.y + tri(p.x)));
            }

            float hash21(vec2 n) {
                return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
            }

            vec3 hash33(vec3 p) {
                p = fract(p * vec3(0.1031, 0.1030, 0.0973));
                p += dot(p, p.yxz + 33.33);
                return fract((p.xxy + p.yxx) * p.zyx);
            }

            float triNoise2d(vec2 p, float spd) {
                float z = 1.8;
                float z2 = 2.5;
                float rz = 0.0;
                p = p * mm2(p.x * 0.06);
                vec2 bp = p;
                for (int octave = 0; octave < 5; octave++) {
                    vec2 dg = tri2(bp * 1.85) * 0.75;
                    dg = dg * mm2(Time * spd);
                    p -= dg / z2;

                    bp *= 1.3;
                    z2 *= 0.45;
                    z *= 0.42;
                    p *= 1.21 + (rz - 1.0) * 0.02;

                    rz += tri(p.x + tri(p.y)) * z;
                    p = p * (M2 * -1.0);
                }
                return clamp(1.0 / pow(rz * 29.0, 1.3), 0.0, 0.55);
            }

            vec4 aurora(vec3 ro, vec3 rd, vec2 fragCoord) {
                vec4 col = vec4(0.0);
                vec4 avgCol = vec4(0.0);

                for (int sampleIndex = 0; sampleIndex < 50; sampleIndex++) {
                    float i = float(sampleIndex);
                    float of = 0.006 * hash21(fragCoord) * smoothstep(0.0, 15.0, i);
                    float denom = rd.y * 2.0 + 0.4;
                    denom = abs(denom) < 0.03 ? (denom < 0.0 ? -0.03 : 0.03) : denom;
                    float pt = ((0.8 + pow(i, 1.4) * 0.002) - ro.y) / denom;
                    pt -= of;

                    vec3 bpos = ro + pt * rd;
                    float rzt = triNoise2d(bpos.zx, 0.06);
                    vec4 col2 = vec4(0.0, 0.0, 0.0, rzt);

                    vec3 colorVariation = sin(1.0 - vec3(2.15, -0.5, 1.2) + i * 0.043) * 0.5 + 0.5;
                    col2.rgb = AuroraColor.rgb * colorVariation * rzt;

                    avgCol = mix(avgCol, col2, 0.5);
                    col += avgCol * exp2(-i * 0.065 - 2.5) * smoothstep(0.0, 5.0, i);
                }

                col *= clamp(rd.y * 15.0 + 0.4, 0.0, 1.0);
                return col * AuroraIntensity;
            }

            vec3 stars(vec3 p, vec2 res) {
                vec3 c = vec3(0.0);
                float resVal = max(min(res.x, res.y), 1.0);
                for (int starLayer = 0; starLayer < 4; starLayer++) {
                    float i = float(starLayer);
                    vec3 q = fract(p * (0.15 * resVal)) - 0.5;
                    vec3 id = floor(p * (0.15 * resVal));
                    vec2 rn = hash33(id).xy;
                    float c2 = 1.0 - smoothstep(0.0, 0.6, length(q));
                    c2 *= step(rn.x, 0.0005 + i * i * 0.001);
                    c += c2 * (mix(vec3(1.0, 0.49, 0.1), vec3(0.75, 0.9, 1.0), rn.y) * 0.1 + 0.9);
                    p *= 1.3;
                }
                return c * c * StarBrightness;
            }

            vec3 bg(vec3 rd) {
                float sd = dot(normalize(vec3(-0.5, -0.6, 0.9)), rd) * 0.5 + 0.5;
                sd = pow(sd, 5.0);
                return mix(BackgroundColor1.rgb, BackgroundColor2.rgb, sd) * 0.63;
            }

            void main() {
                vec2 res = max(quadSize, vec2(1.0));
                vec2 uv = clamp(mapUv, vec2(0.0), vec2(1.0));
                vec2 fragCoord = uv * res;

                vec3 ro = vec3(0.0, 0.0, -6.7);
                float theta = uv.y * PI;
                float phi = (uv.x - 0.5) * 2.0 * PI;
                vec3 rd = vec3(sin(theta) * sin(phi), sin(theta) * cos(phi), cos(theta));

                float timeRot = Time * 0.05;
                rd.yz = rd.yz * mm2(0.4);
                rd.xz = rd.xz * mm2(sin(timeRot) * 0.2);

                vec3 col;
                float fade = smoothstep(0.0, 0.01, abs(rd.y)) * 0.1 + 0.9;

                if (rd.y > 0.0) {
                    col = bg(rd) * fade;
                    vec4 aur = smoothstep(vec4(0.0), vec4(1.5), aurora(ro, rd, fragCoord)) * fade;
                    col += stars(rd, res);
                    col = col * (1.0 - aur.a) + aur.rgb;
                } else {
                    vec3 rrd = rd;
                    rrd.y = abs(rrd.y);
                    col = bg(rrd) * fade * 0.6;

                    vec4 aur = smoothstep(vec4(0.0), vec4(2.5), aurora(ro, rrd, fragCoord));
                    col += stars(rrd, res) * 0.1;
                    col = col * (1.0 - aur.a) + aur.rgb;

                    float waterDenom = max(rrd.y, 0.04);
                    vec3 pos = ro + ((0.5 - ro.y) / waterDenom) * rrd;
                    float nz2 = triNoise2d(pos.xz * vec2(0.5, 0.7), 0.0);
                    col += mix(vec3(0.2, 0.25, 0.5) * 0.08, vec3(0.3, 0.3, 0.5) * 0.7, nz2 * 0.4);
                }

                float vignette = smoothstep(0.0, 0.08, uv.x)
                        * smoothstep(1.0, 0.92, uv.x)
                        * smoothstep(0.0, 0.06, uv.y)
                        * smoothstep(1.0, 0.90, uv.y);
                col *= mix(0.72, 1.0, vignette);
                FragColor = vec4(max(col, vec3(0.0)), 1.0);
            }
            """);
    private static final ShaderDrawOptions MAP_AURORA_BACKGROUND_OPTIONS = ShaderDrawOptions.defaults()
            .blend(true)
            .squareVertexOffset(0.0f);

    private static final ShaderHandle MAP_DESTINY_BACKGROUND_SHADER = ShaderHandle.source(
            "unigui:demo_map_destiny_background",
            """
            #version 150

            in vec3 Position;

            out vec2 mapUv;
            out vec2 localCoord;
            out vec2 quadSize;

            uniform vec2 ScreenSize;
            uniform vec4 SquareVertex;

            void main() {
                vec2 rawUv = Position.xy * 0.5 + 0.5;
                mapUv = vec2(rawUv.x, 1.0 - rawUv.y);

                vec2 rectMin = min(SquareVertex.xy, SquareVertex.zw);
                vec2 rectMax = max(SquareVertex.xy, SquareVertex.zw);
                quadSize = max(rectMax - rectMin, vec2(1.0));
                localCoord = mapUv * quadSize;

                vec2 pixel = mix(rectMin, rectMax, mapUv);
                vec2 ndc = vec2(
                        pixel.x / max(ScreenSize.x, 1.0) * 2.0 - 1.0,
                        1.0 - pixel.y / max(ScreenSize.y, 1.0) * 2.0);
                gl_Position = vec4(ndc, 0.0, 1.0);
            }
            """,
            """
            #version 150

            in vec2 mapUv;
            in vec2 localCoord;
            in vec2 quadSize;

            out vec4 FragColor;

            uniform float Time;
            uniform vec2 ViewportOffset;
            uniform vec2 MapSize;
            uniform float Zoom;

            float hash21(vec2 p) {
                p = fract(p * vec2(123.34, 456.21));
                p += dot(p, p + 45.32);
                return fract(p.x * p.y);
            }

            vec2 hash22(vec2 p) {
                float n = hash21(p);
                return vec2(n, hash21(p + n + 19.19));
            }

            float noise(vec2 p) {
                vec2 i = floor(p);
                vec2 f = fract(p);
                vec2 u = f * f * (3.0 - 2.0 * f);
                float a = hash21(i);
                float b = hash21(i + vec2(1.0, 0.0));
                float c = hash21(i + vec2(0.0, 1.0));
                float d = hash21(i + vec2(1.0, 1.0));
                return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
            }

            float fbm(vec2 p) {
                float v = 0.0;
                float amp = 0.52;
                for (int octave = 0; octave < 5; octave++) {
                    v += noise(p) * amp;
                    p = mat2(1.62, 1.08, -1.08, 1.62) * p + 7.13;
                    amp *= 0.53;
                }
                return v;
            }

            float ridge(vec2 p) {
                float n = fbm(p);
                return 1.0 - abs(n * 2.0 - 1.0);
            }

            float blob(vec2 uv, vec2 center, vec2 radius) {
                vec2 p = (uv - center) / radius;
                return exp(-dot(p, p));
            }

            float starDust(vec2 uv) {
                vec2 cell = floor(uv * 180.0);
                vec2 local = fract(uv * 180.0) - 0.5;
                vec2 rnd = hash22(cell);
                float dotShape = smoothstep(0.18, 0.0, length(local - (rnd - 0.5) * 0.36));
                float mask = step(0.975, rnd.x);
                return dotShape * mask;
            }

            float mapScratches(vec2 uv, vec2 drift) {
                float n = fbm(uv * vec2(15.0, 9.0) + drift * 3.0);
                float contour = abs(fract(n * 7.0 + uv.x * 1.7 + uv.y * 0.9) - 0.5);
                float lines = smoothstep(0.035, 0.0, contour) * 0.28;

                float scan = abs(fract((uv.x + uv.y * 0.23) * 48.0 + n * 0.8) - 0.5);
                lines += smoothstep(0.020, 0.0, scan) * 0.08;
                return lines;
            }

            void main() {
                vec2 uv = clamp(mapUv, vec2(0.0), vec2(1.0));
                vec2 safeMapSize = max(MapSize, vec2(1.0));
                float safeZoom = max(Zoom, 0.001);

                vec2 cameraUv = ViewportOffset / (safeMapSize * safeZoom);
                vec2 drift = cameraUv * vec2(0.16, 0.16);

                vec2 hazeUv = uv + drift;
                vec2 slowUv = uv + drift * 0.55 + vec2(Time * 0.006, -Time * 0.004);
                vec2 detailUv = uv + drift * 1.75 + vec2(Time * 0.011, Time * 0.007);

                float largeA = fbm(hazeUv * vec2(2.2, 1.35) + vec2(0.0, Time * 0.015));
                float largeB = fbm((hazeUv + vec2(8.4, 2.1)) * vec2(3.1, 1.9) - vec2(Time * 0.010, 0.0));
                float veils = ridge(slowUv * vec2(4.0, 2.6)) * 0.55 + largeA * 0.45;

                vec3 base = mix(vec3(0.030, 0.038, 0.045), vec3(0.070, 0.078, 0.085), uv.y);
                base *= 0.78 + 0.22 * largeB;

                vec3 greenHaze = vec3(0.18, 0.48, 0.34) * blob(hazeUv, vec2(0.13, 0.45), vec2(0.34, 0.42));
                vec3 amberHaze = vec3(0.58, 0.30, 0.18) * blob(hazeUv, vec2(0.34, 0.18), vec2(0.36, 0.32));
                vec3 redHaze = vec3(0.46, 0.18, 0.18) * blob(hazeUv, vec2(0.46, 0.79), vec2(0.42, 0.30));
                vec3 blueHaze = vec3(0.18, 0.36, 0.65) * blob(hazeUv, vec2(0.84, 0.42), vec2(0.38, 0.46));
                vec3 tealHaze = vec3(0.10, 0.50, 0.52) * blob(hazeUv, vec2(0.78, 0.88), vec2(0.48, 0.24));

                vec3 haze = (greenHaze + amberHaze + redHaze + blueHaze + tealHaze) * (0.48 + veils * 0.62);
                vec3 col = base + haze;

                float paper = fbm(detailUv * vec2(18.0, 12.0));
                float clouds = fbm(detailUv * vec2(7.0, 5.0) + paper);
                col += vec3(0.080, 0.095, 0.105) * smoothstep(0.38, 0.92, clouds) * 0.42;

                float linework = mapScratches(uv, drift);
                col += vec3(0.44, 0.48, 0.46) * linework;

                float dust = starDust(uv + drift * 2.4 + vec2(Time * 0.003, -Time * 0.002));
                col += vec3(0.52, 0.74, 0.68) * dust * 0.65;

                float grain = hash21(localCoord + vec2(Time * 37.0, Time * 11.0)) - 0.5;
                col += grain * 0.035;

                float edge = smoothstep(0.0, 0.08, uv.x)
                        * smoothstep(1.0, 0.92, uv.x)
                        * smoothstep(0.0, 0.08, uv.y)
                        * smoothstep(1.0, 0.92, uv.y);
                col *= mix(0.56, 1.0, edge);

                float fadedCorners = blob(uv, vec2(0.0, 0.0), vec2(0.55, 0.45))
                        + blob(uv, vec2(1.0, 1.0), vec2(0.55, 0.45));
                col += vec3(0.11, 0.12, 0.10) * fadedCorners * 0.12;

                FragColor = vec4(max(col, vec3(0.0)), 1.0);
            }
            """);


    private UniGuiDemo() {
    }

    public static void openDemo() {
        RenderSystem.recordRenderCall(UniGuiDemo::openDemoClient);
    }

    public static void openXmlDemo() {
        RenderSystem.recordRenderCall(UniGuiDemo::openXmlDemoClient);
    }

    public static void openXmlHotReloadDemo() {
        RenderSystem.recordRenderCall(UniGuiDemo::openXmlHotReloadDemoClient);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unigui")
                .executes(ctx -> {
                    openDemo();
                    return 0;
                })
                .then(Commands.literal("xml").executes(ctx -> {
                    openXmlDemo();
                    return 0;
                }))
                .then(Commands.literal("xmlhot").executes(ctx -> {
                    openXmlHotReloadDemo();
                    return 0;
                }))
                .then(Commands.literal("xml-hot").executes(ctx -> {
                    openXmlHotReloadDemo();
                    return 0;
                })));
    }

    private static MinecraftWidgetScreen openScreen(Component title, Widget root, DefaultUIContext context) {
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(title, root, context);
        screen.renderPolicy(UiRenderPolicy.vsync());
        screen.scaleWithMinecraftGui(false);
        Minecraft.getInstance().setScreen(screen);
        return screen;
    }

    private static void openDemoClient() {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService())
                .scaleProvider(SCALE);
        Widget root = demoScreenWidget(context);
        openScreen(Component.literal("UniGUI Demo"), root, context);
    }

    private static void openXmlDemoClient() {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService())
                .scaleProvider(SCALE);
        Widget root = xmlDemoScreenWidget();
        openScreen(Component.literal("UniGUI XML Demo"), root, context);
    }

    private static void openXmlHotReloadDemoClient() {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService())
                .scaleProvider(SCALE);
        Widget root = xmlHotReloadDemoScreenWidget();
        openScreen(Component.literal("UniGUI XML Hot Reload"), root, context);
    }

    private static Widget xmlDemoScreenWidget() {
        StackPanel viewport = new StackPanel();
        viewport.addChild(backgroundFrame());

        ensureXmlDemoTextures();

        Box panel = XMLWidget.createResource(XML_DEMO_RESOURCE, Box.class);

        wireXmlDemoPanel(panel);

        viewport.addChild(panel);
        return new OverlayLayer(viewport);
    }

    private static Widget xmlHotReloadDemoScreenWidget() {
        StackPanel viewport = new StackPanel();
        viewport.addChild(backgroundFrame());

        ensureXmlDemoTextures();

        java.nio.file.Path sourcePath = xmlDemoHotReloadPath();
        Label reloadStatus = new Label("Watching " + sourcePath.getFileName());
        reloadStatus.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(1).flexShrink(1.0f));

        XmlWidgetDiagnosticsPanel diagnostics = new XmlWidgetDiagnosticsPanel()
                .entryLimit(5);
        diagnostics.layout(style -> style
                .margin(10.0f)
                .size(408.0f, 154.0f)
                .align(Alignment.END, Alignment.START)
                .flexGrow(0)
                .flexShrink(0.0f));

        XmlWidgetHotReloadPreview<Box> preview = new XmlWidgetHotReloadPreview<>(
                XmlWidgetHotReloadSource.path(sourcePath),
                Box.class)
                .reloadIntervalSeconds(0.35f)
                .onReload(UniGuiDemo::wireXmlDemoPanel)
                .onStatus(status -> {
                    reloadStatus.text(xmlHotReloadStatusText(status));
                    diagnostics.status(status);
                });
        preview.layout(style -> style.flexGrow(1).flexShrink(1.0f));
        preview.reloadNow();

        Box footer = panelBox(0.035f, 0.040f, 0.052f, 0.94f);
        footer.layout(style -> style
                .margin(10.0f)
                .size(LayoutConstraints.AUTO, 34.0f)
                .align(Alignment.CENTER, Alignment.END)
                .flexGrow(0)
                .flexShrink(0.0f));

        HBox row = new HBox();
        row.spacing(8.0f);
        row.layout(style -> style.margin(8.0f, 6.0f).flexGrow(1).flexShrink(1.0f));

        Label command = new Label("/unigui xmlhot");
        command.layout(style -> style.size(106.0f, 18.0f).flexGrow(0).flexShrink(0.0f));
        Button reload = new Button("Reload");
        reload.layout(style -> style.size(70.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        reload.onClick(event -> preview.reloadNow());
        Button close = new Button("Close");
        close.layout(style -> style.size(70.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        close.onClick(event -> Minecraft.getInstance().setScreen(null));

        row.addChild(command);
        row.addChild(reloadStatus);
        row.addChild(reload);
        row.addChild(close);
        footer.addChild(row);

        viewport.addChild(preview);
        viewport.addChild(diagnostics);
        viewport.addChild(footer);
        return new OverlayLayer(viewport);
    }

    private static void wireXmlDemoPanel(Box panel) {
        if (panel == null) return;

        Slider amount = XMLWidget.getWidget(panel, "amount", Slider.class);
        ProgressBar meter = XMLWidget.getWidget(panel, "meter", ProgressBar.class);
        Checkbox strict = XMLWidget.getWidget(panel, "strict", Checkbox.class);
        Button apply = XMLWidget.getWidget(panel, "apply", Button.class);
        Button cycle = XMLWidget.getWidget(panel, "cycle", Button.class);
        Button close = XMLWidget.getWidget(panel, "close", Button.class);
        Label status = XMLWidget.getWidget(panel, "status", Label.class);

        amount.onValueChanged(event -> {
            meter.value(event.newValue());
            status.text(String.format(Locale.ROOT, "Slider -> %.0f, strict=%s", event.newValue(), strict.checked()));
        });
        apply.onClick(event -> status.text(String.format(Locale.ROOT,
                "Apply clicked: value %.0f, strict=%s", amount.value(), strict.checked())));
        strict.onCheckedChanged(event -> status.text("Strict XML: " + event.newValue()));
        float[] cycleValues = {12.0f, 42.0f, 68.0f, 91.0f};
        int[] cycleIndex = {1};
        cycle.onClick(event -> {
            cycleIndex[0] = (cycleIndex[0] + 1) % cycleValues.length;
            amount.value(cycleValues[cycleIndex[0]]);
        });
        close.onClick(event -> Minecraft.getInstance().setScreen(null));
    }

    private static String xmlHotReloadStatusText(XmlWidgetHotReloadPreview.Status status) {
        if (status == null) return "XML hot reload: waiting.";
        String prefix = status.failed() ? "XML hot reload failed: " : "XML hot reload: ";
        return prefix + status.message();
    }

    private static java.nio.file.Path xmlDemoHotReloadPath() {
        String relativePath = "common/src/main/resources/" + XML_DEMO_RESOURCE;
        java.nio.file.Path workingDirectory = java.nio.file.Paths.get("").toAbsolutePath().normalize();
        for (java.nio.file.Path current = workingDirectory; current != null; current = current.getParent()) {
            java.nio.file.Path candidate = current.resolve(relativePath).normalize();
            if (java.nio.file.Files.exists(candidate)) return candidate;
        }
        return workingDirectory.resolve(relativePath).normalize();
    }

    private static void ensureXmlDemoTextures() {
        if (xmlDemoCloudTextureLoadFailed || UniGuiTextures.get(XML_DEMO_CLOUD_TEXTURE_ID) != null) return;

        ClassLoader loader = UniGuiDemo.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(XML_DEMO_CLOUD_TEXTURE_RESOURCE)) {
            if (stream == null) {
                xmlDemoCloudTextureLoadFailed = true;
                return;
            }
            UniGuiTextures.replace(XML_DEMO_CLOUD_TEXTURE_ID, NativeImage.read(stream), TextureOptions.linear());
        } catch (IOException | RuntimeException failure) {
            xmlDemoCloudTextureLoadFailed = true;
        }
    }

    private static Widget demoScreenWidget(DefaultUIContext context) {
        StackPanel viewport = new StackPanel();
        viewport.addChild(backgroundFrame());

        DockPanel app = new DockPanel();
        app.layout(style -> style.margin(10.0f));
        viewport.addChild(app);

        VBox shell = new VBox();
        shell.spacing(8.0f);
        shell.layout(style -> style.flexGrow(1).flexShrink(1.0f));

        HBox header = new HBox();
        header.spacing(8.0f);
        header.layout(style -> style.size(LayoutConstraints.AUTO, 34.0f).flexGrow(0).flexShrink(0.0f));

        Label title = new Label("UniGUI Widget Demo");
        title.layout(style -> style.size(210.0f, 24.0f).align(Alignment.START, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
        Label hint = new Label("Every tab is interactive: click, type, drag, scroll, resize, open overlays.");
        hint.layout(style -> style.size(LayoutConstraints.AUTO, 24.0f).align(Alignment.START, Alignment.CENTER).flexGrow(1).flexShrink(1.0f));
        Button scale = new Button("Scale 200%");
        scale.layout(style -> style.size(92.0f, 22.0f).align(Alignment.END, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
        float[] scales = {0.50f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.25f, 2.5f, 2.75f, 3.0f, 3.25f, 3.5f};
        int[] scaleIndex = {6};
        scale.onClick(event -> {
            scaleIndex[0] = (scaleIndex[0] + 1) % scales.length;
            SCALE.scale(scales[scaleIndex[0]]);
            scale.text("Scale " + Math.round(SCALE.scale() * 100.0f) + "%");
        });

        ToggleButton debug = new ToggleButton("Debug");
        debug.layout(style -> style.size(72.0f, 22.0f).align(Alignment.END, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
        debug.onCheckedChanged(event -> {
            if (event.newValue()) {
                context.enableDebugFlags(DebugFlags.PROFILER_OVERLAY | DebugFlags.DRAW_COMMANDS | DebugFlags.BATCHES);
            } else {
                context.disableDebugFlags(DebugFlags.PROFILER_OVERLAY | DebugFlags.DRAW_COMMANDS | DebugFlags.BATCHES);
            }
        });

        header.addChild(title);
        header.addChild(hint);
        header.addChild(scale);
        header.addChild(debug);

        TabControl tabs = new TabControl();
        tabs.layout(style -> style.flexGrow(1).flexShrink(1.0f));
        tabs.addTab("Overview", scroll(overviewPage()));
        tabs.addTab("Controls", scroll(controlsPage()));
        tabs.addTab("Text", scroll(textPage()));
        tabs.addTab("Containers", scroll(containersPage()));
        tabs.addTab("Data", scroll(dataPage()));
        tabs.addTab("Custom Renders", scroll(customRendersPage()));
        tabs.addTab("Animations", scroll(animationsPage()));
        tabs.addTab("Node Graph", nodeGraphPage());
        tabs.addTab("World Canvas", worldCanvasPage());
        tabs.addTab("Map Canvas", mapCanvasPage());
        tabs.addTab("Overlays", overlaysPage());
        tabs.addTab("Minecraft", scroll(minecraftPage()));
        tabs.addTab("Stress", scroll(stressPage()));

        shell.addChild(header);
        shell.addChild(tabs);

        Box status = panelBox(0.040f, 0.045f, 0.060f, 0.94f);
        status.layout(style -> style.size(LayoutConstraints.AUTO, 24.0f).flexGrow(0).flexShrink(0.0f));
        Label statusText = new Label("/unigui demo  |  independent UI scale  |  tabs cover controls, layout, data, overlays, Minecraft previews and pickers");
        statusText.layout(style -> style.margin(8.0f, 4.0f).size(LayoutConstraints.AUTO, 16.0f).flexGrow(0).flexShrink(0.0f));
        status.addChild(statusText);
        app.addChild(status, DockSide.BOTTOM);
        app.addChild(shell);

        return new OverlayLayer(viewport);
    }

    private static ScrollView scroll(Widget content) {
        ScrollView scroll = new ScrollView(content);
        scroll.layout(style -> style.flexGrow(1).flexShrink(1.0f));
        return scroll;
    }


    private static VBox overviewPage() {
        VBox page = XMLWidget.createResource(XML_OVERVIEW_RESOURCE, VBox.class);
        VBox quickFactoriesSlot = XMLWidget.getWidget(page, "quickFactoriesSlot", VBox.class);
        quickFactoriesSlot.addChild(factoryGallery());
        quickFactoriesSlot.applyQueuedMutations();
        return page;
    }

    private static Widget factoryGallery() {
        WrapPanel row = wrap();
        row.addChild(new Border().layout(style -> style.size(64.0f, 28.0f).flexGrow(0).flexShrink(0.0f)));
        row.addChild(new Separator().layout(style -> style.size(120.0f, 10.0f).flexGrow(0).flexShrink(0.0f)));
        row.addChild(new TextureWidget(new SimpleTextureHandle("minecraft:textures/block/stone.png", 16, 16))
                .layout(style -> style.size(38.0f, 38.0f).flexGrow(0).flexShrink(0.0f)));
        row.addChild(new ImageView(new SimpleTextureHandle("minecraft:textures/item/diamond.png", 16, 16))
                .layout(style -> style.size(42.0f, 42.0f).flexGrow(0).flexShrink(0.0f)));
        row.addChild(new CanvasWidget().layout(style -> style.size(76.0f, 42.0f).flexGrow(0).flexShrink(0.0f)));
        row.addChild(new Path().layout(style -> style.size(76.0f, 42.0f).flexGrow(0).flexShrink(0.0f)));
        row.addChild(new Shape().layout(style -> style.size(76.0f, 42.0f).flexGrow(0).flexShrink(0.0f)));
        return row;
    }

    private static VBox controlsPage() {
        VBox page = page("Controls", "Interactive controls with events wired to status labels.");

        Label status = new Label("Ready");
        status.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(status);

        WrapPanel buttons = wrap();
        Button button = new Button("Button");
        ToggleButton toggle = new ToggleButton("Toggle");
        ToggleSwitch switchControl = new ToggleSwitch("Switch").checked(true);
        HoldButton hold = new HoldButton("Hold").holdDurationSeconds(0.75f);
        Checkbox checkbox = new Checkbox("Checkbox");
        Checkbox partialTree = new Checkbox("Tree parent (partial)")
                .triState(true)
                .state(CheckboxState.INDETERMINATE);
        button.layout(style -> style.size(76.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        toggle.layout(style -> style.size(76.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        switchControl.layout(style -> style.size(112.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        hold.layout(style -> style.size(92.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        checkbox.layout(style -> style.size(96.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        partialTree.layout(style -> style.size(150.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        button.onClick(event -> status.text("Button clicked"));
        toggle.onCheckedChanged(event -> status.text("Toggle: " + event.newValue()));
        switchControl.onCheckedChanged(event -> status.text("Switch: " + event.newValue()));
        hold.onHoldCompleted(event -> status.text(String.format(Locale.ROOT, "Hold completed in %.2fs", event.holdDurationSeconds())));
        checkbox.onCheckedChanged(event -> status.text("Checkbox: " + event.newValue()));
        partialTree.onStateChanged(event -> status.text("Tree parent: " + event.newState()));
        buttons.addChild(button);
        buttons.addChild(toggle);
        buttons.addChild(switchControl);
        buttons.addChild(hold);
        buttons.addChild(checkbox);
        buttons.addChild(partialTree);
        page.addChild(section("Buttons", buttons));

        WrapPanel inputs = wrap();
        TextInput input = new TextInput("raw input");
        TextField field = new TextField("TextField");
        PasswordField password = new PasswordField("secret");
        SearchField search = new SearchField("filter recipes");
        NumberField number = new NumberField().range(0.0d, 100.0d).value(42.0d);
        input.layout(style -> style.size(120.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        field.layout(style -> style.size(120.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        password.layout(style -> style.size(120.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        search.layout(style -> style.size(140.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        number.layout(style -> style.size(84.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        field.onTextChanged(event -> status.text("TextField: " + event.newText()));
        search.onSearchSubmitted(event -> status.text("Search submitted: " + search.text()));
        number.onValueChanged(event -> status.text(String.format(Locale.ROOT, "Number: %.1f", event.newValue())));
        inputs.addChild(input);
        inputs.addChild(field);
        inputs.addChild(password);
        inputs.addChild(search);
        inputs.addChild(number);
        page.addChild(section("Text input", inputs));

        WrapPanel choice = wrap();
        ComboBox combo = new ComboBox()
                .items(List.of("Dark", "Light", "High Contrast", "Minecraft"))
                .silentSelectedIndex(0);
        combo.dropDownSameWidth();
        combo.layout(style -> style.size(142.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        combo.onSelectionChanged(event -> status.text("Combo: " + combo.selectedItem()));
        VBox dropContent = new VBox();
        dropContent.spacing(2.0f);
        dropContent.addChild(new Label("Arbitrary popup content"));
        dropContent.addChild(new Button("Action"));
        DropDownBox drop = new DropDownBox()
                .headerText("DropDownBox")
                .content(dropContent);
        drop.dropDownSameWidth();
        drop.layout(style -> style.size(130.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        RadioButton compact = new RadioButton("Compact", "compact");
        RadioButton normal = new RadioButton("Normal", "normal");
        RadioButton detailed = new RadioButton("Detailed", "detailed");
        compact.layout(style -> style.size(78.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        normal.layout(style -> style.size(70.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        detailed.layout(style -> style.size(78.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        new RadioGroup().add(compact).add(normal).add(detailed).silentSelectedValue("normal");
        choice.addChild(combo);
        choice.addChild(drop);
        choice.addChild(compact);
        choice.addChild(normal);
        choice.addChild(detailed);
        page.addChild(section("Selection", choice));

        VBox settings = new VBox();
        settings.spacing(3.0f);
        settings.layout(style -> style.size(360.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));

        ToggleSwitch crossplay = new ToggleSwitch().checked(true);
        crossplay.layout(style -> style.size(LayoutConstraints.AUTO, 20.0f).flexGrow(0).flexShrink(0.0f));
        crossplay.onCheckedChanged(event -> status.text("Crossplay: " + event.newValue()));
        settings.addChild(new SettingRow("CROSSPLAY", crossplay)
                .rowHeight(24.0f)
                .gap(14.0f));

        ComboBox fireteamPrivacy = new ComboBox()
                .items(List.of("Closed", "Friends Only", "Public"))
                .silentSelectedIndex(0);
        fireteamPrivacy.dropDownSameWidth();
        fireteamPrivacy.layout(style -> style.size(142.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        fireteamPrivacy.onSelectionChanged(event -> status.text("Fireteam privacy: " + fireteamPrivacy.selectedItem()));
        settings.addChild(new SettingRow("FIRETEAM PRIVACY", fireteamPrivacy)
                .rowHeight(28.0f)
                .gap(14.0f)
                .controlWidth(142.0f));

        Checkbox textChat = new Checkbox();
        textChat.layout(style -> style.size(LayoutConstraints.AUTO, 20.0f).flexGrow(0).flexShrink(0.0f));
        textChat.onCheckedChanged(event -> status.text("Text chat: " + event.newValue()));
        settings.addChild(new SettingRow("TEXT CHAT", textChat)
                .rowHeight(24.0f)
                .gap(14.0f));

        NumberField fieldOfView = new NumberField().range(55.0d, 120.0d).step(1.0d).value(104.0d);
        fieldOfView.layout(style -> style.size(84.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        fieldOfView.onValueChanged(event -> status.text(String.format(Locale.ROOT, "Field of view: %.0f", event.newValue())));
        settings.addChild(new SettingRow("FIELD OF VIEW", fieldOfView)
                .rowHeight(28.0f)
                .gap(14.0f)
                .controlWidth(84.0f));

        page.addChild(section("Settings rows", settings));

        VBox panelRows = new VBox();
        panelRows.spacing(4.0f);
        panelRows.layout(style -> style.size(420.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));

        Label titleLeft = new Label("SETTINGS");
        Label titleRight = new Label("GAMEPLAY");
        titleLeft.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        titleRight.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        PanelRowWidget titlePanel = new PanelRowWidget()
                .rowHeight(28.0f)
                .gap(16.0f)
                .addLeft(titleLeft)
                .addRight(titleRight);
        titlePanel.layout(style -> style.size(420.0f, 28.0f).flexGrow(0).flexShrink(0.0f));

        Button gameplayTab = new Button("GAMEPLAY");
        Button videoTab = new Button("VIDEO");
        Button soundTab = new Button("SOUND");
        Button accessibilityTab = new Button("ACCESSIBILITY");
        gameplayTab.layout(style -> style.size(LayoutConstraints.AUTO, 22.0f).flexGrow(0).flexShrink(0.0f));
        videoTab.layout(style -> style.size(LayoutConstraints.AUTO, 22.0f).flexGrow(0).flexShrink(0.0f));
        soundTab.layout(style -> style.size(LayoutConstraints.AUTO, 22.0f).flexGrow(0).flexShrink(0.0f));
        accessibilityTab.layout(style -> style.size(LayoutConstraints.AUTO, 22.0f).flexGrow(0).flexShrink(0.0f));
        PanelRowWidget navigationPanel = new PanelRowWidget()
                .rowHeight(28.0f)
                .leftGap(6.0f)
                .addLeft(gameplayTab)
                .addLeft(videoTab)
                .addLeft(soundTab)
                .addLeft(accessibilityTab);
        navigationPanel.layout(style -> style.size(420.0f, 28.0f).flexGrow(0).flexShrink(0.0f));
        gameplayTab.onClick(event -> status.text("Panel tab: Gameplay"));
        videoTab.onClick(event -> status.text("Panel tab: Video"));
        soundTab.onClick(event -> status.text("Panel tab: Sound"));
        accessibilityTab.onClick(event -> status.text("Panel tab: Accessibility"));

        Button restoreDefaults = new Button("RESTORE DEFAULTS");
        Button applySettings = new Button("APPLY");
        restoreDefaults.layout(style -> style.size(LayoutConstraints.AUTO, 22.0f).flexGrow(0).flexShrink(0.0f));
        applySettings.layout(style -> style.size(LayoutConstraints.AUTO, 22.0f).flexGrow(0).flexShrink(0.0f));
        PanelRowWidget actionPanel = new PanelRowWidget()
                .rowHeight(28.0f)
                .rightGap(6.0f)
                .addRight(restoreDefaults)
                .addRight(applySettings);
        actionPanel.layout(style -> style.size(420.0f, 28.0f).flexGrow(0).flexShrink(0.0f));
        restoreDefaults.onClick(event -> status.text("Panel action: restore defaults"));
        applySettings.onClick(event -> status.text("Panel action: apply"));

        panelRows.addChild(titlePanel);
        panelRows.addChild(navigationPanel);
        panelRows.addChild(new SettingRow("HUD OPACITY", new Slider().range(0.0f, 100.0f).value(72.0f)
                        .layout(style -> style.size(140.0f, 22.0f).flexGrow(0).flexShrink(0.0f)))
                .rowHeight(28.0f)
                .gap(14.0f)
                .controlWidth(140.0f));
        panelRows.addChild(new SettingRow("SUBTITLES", new ToggleSwitch().checked(true)
                        .layout(style -> style.size(LayoutConstraints.AUTO, 20.0f).flexGrow(0).flexShrink(0.0f)))
                .rowHeight(24.0f)
                .gap(14.0f));
        panelRows.addChild(actionPanel);
        page.addChild(section("Panel rows", panelRows));

        WrapPanel feedback = wrap();
        Slider slider = new Slider().range(0.0f, 100.0f).step(5.0f).value(42.0f);
        ProgressBar progress = new ProgressBar().range(0.0f, 100.0f).value(42.0f);
        LoadingIndicator ring = new Spinner().speed(1.2f).segments(12);
        LoadingIndicator dots = new LoadingIndicator().mode(LoadingIndicator.Mode.DOTS);
        LoadingIndicator bar = new LoadingIndicator().mode(LoadingIndicator.Mode.BAR);
        slider.layout(style -> style.size(160.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        progress.layout(style -> style.size(130.0f, 12.0f).align(Alignment.START, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
        ring.layout(style -> style.size(24.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        dots.layout(style -> style.size(72.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        bar.layout(style -> style.size(118.0f, 8.0f).align(Alignment.START, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
        slider.onValueChanged(event -> progress.value(event.newValue()));
        feedback.addChild(slider);
        feedback.addChild(progress);
        feedback.addChild(ring);
        feedback.addChild(dots);
        feedback.addChild(bar);
        page.addChild(section("Slider / Progress / Loading", feedback));

        WrapPanel pickers = wrap();
        DatePicker date = new DatePicker().value(LocalDate.of(2026, 8, 10));
        TimeSpanField span = new TimeSpanField().value(Duration.ofMinutes(7).plusSeconds(30));
        ColorPicker color = new ColorPicker();
        date.layout(style -> style.size(168.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        span.layout(style -> style.size(168.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        color.layout(style -> style.size(116.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        date.onDateChanged(event -> status.text("Date: " + event.newValue()));
        color.onColorChanged(event -> status.text(String.format(Locale.ROOT, "Color: #%08X", event.newArgb())));
        pickers.addChild(date);
        pickers.addChild(span);
        pickers.addChild(color);
        page.addChild(section("Pickers", pickers));
        return page;
    }

    private static VBox textPage() {
        VBox page = page("Text & Fonts", "TextWidget, Label, TextBlock overflow modes and RichText runs.");
        WrapPanel simple = wrap();
        simple.addChild(new Text("Text").layout(style -> style.size(90.0f, 20.0f).flexGrow(0).flexShrink(0.0f)));
        simple.addChild(new Label("Label").layout(style -> style.size(90.0f, 20.0f).flexGrow(0).flexShrink(0.0f)));
        simple.addChild(new TextBlock("TextBlock wraps long retained text across multiple lines.").wrap(true)
                .layout(style -> style.size(240.0f, 42.0f).flexGrow(0).flexShrink(0.0f)));
        page.addChild(section("Plain widgets", simple));

        TextBlock clip = new TextBlock("CLIP: this text is intentionally too long for its box and should clip cleanly.");
        clip.overflowMode(TextOverflowMode.CLIP);
        clip.layout(style -> style.size(260.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        TextBlock shrink = new TextBlock("SHRINK_TO_FIT: compact text into the available width.");
        shrink.overflowMode(TextOverflowMode.SHRINK_TO_FIT);
        shrink.layout(style -> style.size(260.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        TextBlock marquee = new TextBlock("MARQUEE_ON_HOVER: hover this line to scroll a long status message.");
        marquee.overflowMode(TextOverflowMode.MARQUEE_ON_HOVER);
        marquee.layout(style -> style.size(260.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        VBox overflow = new VBox();
        overflow.spacing(5.0f);
        overflow.addChild(clip);
        overflow.addChild(shrink);
        overflow.addChild(marquee);
        page.addChild(section("Overflow modes", overflow));

        RichText rich = RichText.builder()
                .font(Fonts.defaultFace()).size(18.0f).color(MutableColor.rgba(0.25f, 0.85f, 1.0f, 1.0f)).append("SDF ")
                .font(MinecraftFonts.defaultFace()).size(13.0f).color(MutableColor.rgba(1.0f, 0.75f, 0.2f, 1.0f)).append("Minecraft ")
                .font(MinecraftFonts.uniformFace()).size(14.0f).color(MutableColor.rgba(0.35f, 1.0f, 0.45f, 1.0f)).append("Uniform ")
                .font(MinecraftFonts.altFace()).size(16.0f).color(MutableColor.rgba(1.0f, 0.4f, 0.8f, 1.0f)).append("Alt")
                .build();
        TextBlock richBlock = new TextBlock();
        richBlock.richText(rich);
        richBlock.layout(style -> style.size(LayoutConstraints.AUTO, 30.0f).flexGrow(0).flexShrink(0.0f));
        RichTextView richView = new RichTextView(rich);
        richView.layout(style -> style.size(LayoutConstraints.AUTO, 42.0f).flexGrow(0).flexShrink(0.0f));
        VBox richBox = new VBox();
        richBox.spacing(6.0f);
        richBox.addChild(richBlock);
        richBox.addChild(richView);
        page.addChild(section("RichText / Fonts", richBox));
        return page;
    }

    private static VBox containersPage() {
        VBox page = page("Containers", "Layout and composition widgets used by real screens.");

        WrapPanel panels = wrap();
        panels.addChild(smokeTile("Box", 82.0f, 42.0f, 0.10f, 0.16f, 0.22f));
        panels.addChild(smokeTile("PanelWidget", 112.0f, 42.0f, 0.12f, 0.18f, 0.12f));
        panels.addChild(smokeTile("HBox", 82.0f, 42.0f, 0.18f, 0.12f, 0.16f));
        panels.addChild(smokeTile("VBox", 82.0f, 42.0f, 0.15f, 0.14f, 0.24f));
        panels.addChild(smokeTile("GridBox", 100.0f, 42.0f, 0.20f, 0.15f, 0.10f));
        panels.addChild(smokeTile("WrapPanel", 116.0f, 42.0f, 0.10f, 0.20f, 0.20f));
        page.addChild(section("Basic containers", panels));

        Breadcrumb breadcrumb = new Breadcrumb().items(List.of("UniGUI", "Demo", "Containers")).silentSelectedIndex(2);
        breadcrumb.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        page.addChild(section("Breadcrumb", breadcrumb));

        SplitPanel split = new SplitPanel(samplePane("Left Pane", "Drag the divider."), samplePane("Right Pane", "Min sizes prevent collapse."));
        split.splitRatio(0.38f).minFirstSize(90.0f).minSecondSize(120.0f);
        split.layout(style -> style.size(LayoutConstraints.AUTO, 92.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(section("SplitPanel", split));

        TabControl tabs = new TabControl();
        tabs.addTab("Tab A", samplePane("TabControl", "Selected content stays retained."));
        tabs.addTab("Tab B", samplePane("Second page", "Switch without rebuilding the whole UI."));
        tabs.layout(style -> style.size(LayoutConstraints.AUTO, 96.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(section("TabControl", tabs));

        Accordion accordion = new Accordion();
        accordion.addPanel(expandable("Graphics", "Render policies, caches and preview widgets."))
                .addPanel(expandable("Input", "Focus, capture, hover and keyboard routing."))
                .addPanel(expandable("Debug", "Profiler overlay and draw command counters."));
        page.addChild(section("Accordion / ExpandablePanel", accordion));

        TreeView tree = new TreeView();
        TreeViewNode root = tree.addRoot("UniGUI");
        TreeViewNode widgets = root.addChild("Widgets");
        widgets.addChild("Button");
        widgets.addChild("ComboBox");
        widgets.addChild("TreeView");
        widgets.addChild("Very long TreeView row name that clips and scrolls on hover");
        root.addChild("Minecraft Backend").addChild("Preview Widgets");
        tree.silentSelect(widgets.child(2));
        tree.rowTextHoverScrollSpeed(18.0f);
        tree.layout(style -> style.size(190.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        TreeList treeList = new TreeList()
                .addPath("Assets", "Textures", "Buttons")
                .addPath("Assets", "Shaders", "SDF")
                .addPath("Screens", "Inventory", "Crafting")
                .addPath("Screens", "Recipe Machine", "Very long nested recipe category label");
        treeList.rowTextHoverScrollSpeed(36.0f);
        treeList.layout(style -> style.size(190.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        TreeListPicker<String> picker = new TreeListPicker<String>()
                .values(List.of("Blocks/Crafting Table", "Items/Diamond", "Entities/Zombie", "Very/Long/Category/That/Matches/Widget/Width"))
                .labelProvider(value -> "Pick: " + value);
        picker.dropDownSameWidth();
        picker.layout(style -> style.size(210.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        WrapPanel trees = wrap();
        trees.addChild(tree);
        trees.addChild(treeList);
        trees.addChild(picker);
        page.addChild(section("TreeView / TreeList / TreeListPicker", trees));

        Carousel carousel = new Carousel()
                .addPage(samplePane("Page 1", "Carousel keeps one page visible."))
                .addPage(samplePane("Page 2", "Use arrows to switch pages."))
                .addPage(samplePane("Page 3", "PageView owns retained pages without carousel chrome."));
        carousel.layout(style -> style.size(LayoutConstraints.AUTO, 116.0f).flexGrow(0).flexShrink(0.0f));
        View view = new View("View").addContent(paragraph("Titled content surface for feature modules."));
        view.layout(style -> style.size(LayoutConstraints.AUTO, 72.0f).flexGrow(0).flexShrink(0.0f));
        VBox pageWidgets = new VBox();
        pageWidgets.spacing(8.0f);
        pageWidgets.addChild(carousel);
        pageWidgets.addChild(view);
        page.addChild(section("Carousel / PageView / View", pageWidgets));

        DockingRoot docking = compactDockingRoot();
        docking.layout(style -> style.size(LayoutConstraints.AUTO, 150.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(section("DockingRoot", docking));
        return page;
    }

    private static VBox dataPage() {
        VBox page = page("Data", "Virtualized rows, tables and lightweight visualizations.");

        VirtualListView list = new VirtualListView()
                .itemCount(1000)
                .itemHeight(22.0f)
                .selectionMode(SelectionMode.MULTIPLE)
                .itemFactory(index -> {
                    Label row = new Label("Virtual row #" + index);
                    row.layout(style -> style.size(LayoutConstraints.AUTO, 22.0f).flexGrow(1).flexShrink(1.0f));
                    return row;
                });
        list.layout(style -> style.size(LayoutConstraints.AUTO, 132.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(section("VirtualListView", list));

        VirtualTableView table = new VirtualTableView()
                .rowCount(400)
                .rowHeight(22.0f)
                .selectionMode(SelectionMode.MULTIPLE)
                .cellRichTextProvider((row, column) -> {
                    String state = switch (row % 3) {
                        case 0 -> "Queued";
                        case 1 -> "Running";
                        default -> "Done";
                    };
                    int score = (row * 17) % 100;
                    return switch (column) {
                        case 0 -> RichText.of("Recipe " + row, MinecraftFonts.defaultFace(), 11.0f);
                        case 1 -> RichText.of(state, MinecraftFonts.defaultFace(), 11.0f);
                        case 2 -> RichText.of(String.valueOf(score), MinecraftFonts.defaultFace(), 11.0f);
                        default -> RichText.plain("");
                    };
                })
                .sortKeyProvider((row, column) -> column == 2 ? (row * 17) % 100 : row);
        table.columns(List.of(
                new VirtualTableColumn(RichText.plain("Name"), 130.0f),
                new VirtualTableColumn(RichText.plain("State"), 90.0f).align(Alignment.CENTER, Alignment.CENTER),
                new VirtualTableColumn(RichText.plain("Score"), 72.0f).align(Alignment.END, Alignment.CENTER)
        ));
        table.layout(style -> style.size(LayoutConstraints.AUTO, 150.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(section("VirtualTableView", table));

        WrapPanel visuals = wrap();
        List<Integer> series = List.of(8, 14, 10, 22, 18, 30, 24, 36, 28);
        Chart chart = new Chart().values(series).type(Chart.Type.BAR);
        Sparkline spark = new Sparkline().values(series);
        GraphView graph = new GraphView()
                .addNode("A", 0.15f, 0.30f)
                .addNode("B", 0.48f, 0.16f)
                .addNode("C", 0.78f, 0.34f)
                .addNode("D", 0.42f, 0.78f)
                .addEdge("A", "B")
                .addEdge("B", "C")
                .addEdge("B", "D")
                .addEdge("A", "D");
        Label visualStatus = new Label("Click a bar, spark point, or graph node");
        chart.layout(style -> style.size(220.0f, 120.0f).flexGrow(0).flexShrink(0.0f));
        spark.layout(style -> style.size(160.0f, 40.0f).flexGrow(0).flexShrink(0.0f));
        graph.layout(style -> style.size(220.0f, 120.0f).flexGrow(0).flexShrink(0.0f));
        visualStatus.layout(style -> style.size(360.0f, 16.0f).flexGrow(0).flexShrink(0.0f));
        chart.onBarClick(event -> visualStatus.text(String.format(Locale.ROOT, "Chart bar #%d = %.2f", event.index(), event.value())));
        spark.onPointClick(event -> visualStatus.text(String.format(Locale.ROOT, "Spark point #%d = %.2f", event.index(), event.value())));
        graph.onNodeClick(event -> visualStatus.text(String.format(Locale.ROOT, "Graph node %s @ %.2f, %.2f", event.id(), event.normalizedX(), event.normalizedY())));
        visuals.addChild(chart);
        visuals.addChild(spark);
        visuals.addChild(graph);
        visuals.addChild(visualStatus);
        page.addChild(section("Chart / Sparkline / GraphView", visuals));
        return page;
    }

    private static VBox customRendersPage() {
        VBox page = page("Custom Renders", "ImGui-inspired custom spinner styles drawn through UniGUI DrawScope primitives.");
        page.addChild(paragraph("These examples use path strokes, variable line thickness, filled circles, alpha fades and discrete motion without texture assets."));

        WrapPanel circular = wrap();
        circular.addChild(spinnerTile("Arc Sweep", Spinner.Style.ARC_SWEEP, 1.05f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Ring Arc", Spinner.Style.RING_ARC, 0.82f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Dotted Trail", Spinner.Style.DOTTED_TRAIL, 0.95f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Dotted Pulse", Spinner.Style.DOTTED_PULSE, 0.72f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Discrete Fade", Spinner.Style.DISCRETE_FADE, 0.65f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Gradient Arc", Spinner.Style.GRADIENT_ARC, 0.90f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Multi Arc", Spinner.Style.MULTI_ARC, 0.76f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Growing Arcs", Spinner.Style.GROWING_ARCS, 0.78f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Section Fade", Spinner.Style.SECTION_FADE, 0.42f, 48.0f, 48.0f));
        page.addChild(section("Circular spinners", circular));

        WrapPanel dots = wrap();
        dots.addChild(spinnerTile("Dots Y", Spinner.Style.DOTS_Y, 1.10f, 86.0f, 32.0f));
        dots.addChild(spinnerTile("Dots Fade", Spinner.Style.DOTS_FADE, 1.00f, 86.0f, 32.0f));
        dots.addChild(spinnerTile("Dots Radius", Spinner.Style.DOTS_RADIUS, 0.92f, 86.0f, 32.0f));
        dots.addChild(spinnerTile("Dots Moving", Spinner.Style.DOTS_MOVING, 0.72f, 86.0f, 32.0f));
        page.addChild(section("Linear dot spinners", dots));
        return page;
    }

    private static VBox animationsPage() {
        VBox page = page("Animations", "Retained widget animations: motion, shake, transform origins, rotation, colors, texture crossfades and loops.");
        page.addChild(paragraph("Click the controls below. The examples are intentionally small so the API usage is visible and easy to copy into real screens."));

        TransitionSpec quick = TransitionSpec.of(0.18f, AnimationEasing.EASE_OUT);
        TransitionSpec smooth = TransitionSpec.of(0.45f, AnimationEasing.EASE_IN_OUT);

        WrapPanel transformRow = new WrapPanel();
        transformRow.spacing(8.0f);
        transformRow.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));

        Button errorButton = new Button("Pseudo error");
        errorButton.themeEnabled(false);
        errorButton.background().set(0.10f, 0.04f, 0.055f, 0.96f);
        errorButton.borderColor().set(0.95f, 0.25f, 0.25f, 0.85f);
        errorButton.textColor().set(1.0f, 0.80f, 0.80f, 1.0f);
        errorButton.transformOrigin(TransformOrigin.CENTER);
        errorButton.layout(style -> style.size(116.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        errorButton.onClick(event -> {
            errorButton.shake(7.0f, 0.0f, 0.34f, 5);
            errorButton.animateBackgroundColor(new MutableColor(0.22f, 0.035f, 0.060f, 0.98f), quick);
            errorButton.animateBorderColor(new MutableColor(1.0f, 0.38f, 0.32f, 1.0f), quick);
            errorButton.animateRotation(-3.0f, quick);
        });

        Button moveButton = new Button("A -> B + rotate");
        moveButton.themeEnabled(false);
        moveButton.background().set(0.055f, 0.095f, 0.16f, 0.96f);
        moveButton.borderColor().set(0.25f, 0.78f, 1.0f, 0.80f);
        moveButton.textColor().set(0.80f, 0.94f, 1.0f, 1.0f);
        moveButton.transformOrigin(TransformOrigin.CENTER);
        moveButton.layout(style -> style.size(132.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        final boolean[] moved = {false};
        moveButton.onClick(event -> {
            moved[0] = !moved[0];
            float startX = moved[0] ? -22.0f : 22.0f;
            float endX = moved[0] ? 22.0f : -22.0f;
            float startY = moved[0] ? 0.0f : 8.0f;
            float endY = moved[0] ? 8.0f : 0.0f;
            moveButton.animatePositionFrom(startX, startY, endX, endY, smooth);
            moveButton.animateRotation(moved[0] ? 8.0f : -8.0f, smooth);
        });

        Button pressButton = new Button("Hover / press");
        pressButton.transformOrigin(TransformOrigin.CENTER);
        pressButton.interactionTransitions(true)
                .interactionTransition(TransitionSpec.of(0.10f, AnimationEasing.EASE_OUT))
                .interactionScales(1.0f, 1.08f, 0.92f)
                .interactionOpacities(1.0f, 0.78f, 0.45f);
        pressButton.layout(style -> style.size(112.0f, 24.0f).flexGrow(0).flexShrink(0.0f));

        transformRow.addChild(errorButton);
        transformRow.addChild(moveButton);
        transformRow.addChild(pressButton);
        page.addChild(section("Transform / interaction", transformRow));


        WrapPanel originRow = new WrapPanel();
        originRow.spacing(8.0f);
        originRow.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));

        Box originLeft = originDemoTile(TransformOrigin.LEFT_TOP,
                new MutableColor(0.12f, 0.06f, 0.16f, 0.96f),
                new MutableColor(0.72f, 0.38f, 1.0f, 0.85f));
        Box originCenter = originDemoTile(TransformOrigin.CENTER,
                new MutableColor(0.055f, 0.105f, 0.145f, 0.96f),
                new MutableColor(0.24f, 0.84f, 1.0f, 0.85f));
        Box originRight = originDemoTile(TransformOrigin.RIGHT_BOTTOM,
                new MutableColor(0.10f, 0.12f, 0.045f, 0.96f),
                new MutableColor(0.95f, 0.85f, 0.24f, 0.85f));
        Button spinOrigins = new Button("Spin origins");
        spinOrigins.layout(style -> style.size(112.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        final boolean[] originSpin = {false};
        spinOrigins.onClick(event -> {
            originSpin[0] = !originSpin[0];
            float targetRotation = originSpin[0] ? 26.0f : -26.0f;
            float targetScale = originSpin[0] ? 1.10f : 0.96f;
            for (Box tile : List.of(originLeft, originCenter, originRight)) {
                tile.animateRotation(targetRotation, smooth);
                tile.animateScale(targetScale, targetScale, smooth);
            }
        });

        originRow.addChild(originDemoStack("Left top", originLeft));
        originRow.addChild(originDemoStack("Center", originCenter));
        originRow.addChild(originDemoStack("Right bottom", originRight));
        originRow.addChild(spinOrigins);
        page.addChild(section("Transform origins", originRow));

        WrapPanel entranceRow = new WrapPanel();
        entranceRow.spacing(8.0f);
        entranceRow.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));

        Box toastCard = panelBox(0.055f, 0.070f, 0.090f, 0.94f);
        toastCard.layout(style -> style.size(210.0f, 46.0f).flexGrow(0).flexShrink(0.0f));
        toastCard.transformOrigin(TransformOrigin.LEFT_CENTER);
        toastCard.opacity(0.0f);
        toastCard.transform().position().set(-32.0f, -8.0f);
        Label toastText = new Label("Slide-in notification");
        toastText.opacity(0.0f);
        toastText.layout(style -> style.margin(8.0f).size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        toastCard.addChild(toastText);

        Button replayToast = new Button("Replay slide-in");
        replayToast.layout(style -> style.size(118.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        replayToast.onClick(event -> {
            toastCard.animatePositionFrom(-32.0f, -8.0f, 0.0f, 0.0f, smooth);
            toastCard.animateOpacity(1.0f, smooth);
            toastText.animateOpacity(1.0f, smooth);
            toastCard.animateBackgroundColor(new MutableColor(0.060f, 0.130f, 0.105f, 0.96f), smooth);
            toastCard.animateBorderColor(new MutableColor(0.30f, 1.0f, 0.62f, 0.88f), smooth);
            toastCard.animateRadius(10.0f, smooth);
        });

        Button dismissToast = new Button("Dismiss");
        dismissToast.layout(style -> style.size(82.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        dismissToast.onClick(event -> {
            toastCard.animatePositionFrom(0.0f, 0.0f, 28.0f, -8.0f, quick);
            toastCard.animateOpacity(0.0f, quick);
            toastText.animateOpacity(0.0f, quick);
            toastCard.animateRadius(4.0f, quick);
        });

        entranceRow.addChild(toastCard);
        entranceRow.addChild(replayToast);
        entranceRow.addChild(dismissToast);
        page.addChild(section("Entrance / exit", entranceRow));

        WrapPanel visualRow = new WrapPanel();
        visualRow.spacing(8.0f);
        visualRow.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));

        Box colorCard = panelBox(0.045f, 0.052f, 0.068f, 0.92f);
        colorCard.layout(style -> style.size(142.0f, 64.0f).flexGrow(0).flexShrink(0.0f));
        Label colorLabel = new Label("Color + radius");
        colorLabel.layout(style -> style.margin(8.0f).size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        colorCard.addChild(colorLabel);

        Button colorButton = new Button("Pulse card");
        colorButton.layout(style -> style.size(100.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        final boolean[] warm = {false};
        colorButton.onClick(event -> {
            warm[0] = !warm[0];
            colorCard.animateBackgroundColor(warm[0]
                    ? new MutableColor(0.18f, 0.075f, 0.05f, 0.96f)
                    : new MutableColor(0.045f, 0.052f, 0.068f, 0.92f), smooth);
            colorCard.animateBorderColor(warm[0]
                    ? new MutableColor(1.0f, 0.58f, 0.25f, 0.95f)
                    : new MutableColor(0.20f, 0.28f, 0.36f, 0.75f), smooth);
            colorCard.animateRadius(warm[0] ? 12.0f : 4.0f, smooth);
        });

        TextureWidget texture = new TextureWidget(new SimpleTextureHandle("minecraft:textures/block/stone.png", 16, 16));
        texture.layout(style -> style.size(48.0f, 48.0f).flexGrow(0).flexShrink(0.0f));
        texture.radius(6.0f).transformOrigin(TransformOrigin.CENTER);
        Button textureButton = new Button("Crossfade texture");
        textureButton.layout(style -> style.size(130.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        SimpleTextureHandle stone = new SimpleTextureHandle("minecraft:textures/block/stone.png", 16, 16);
        SimpleTextureHandle diamond = new SimpleTextureHandle("minecraft:textures/item/diamond.png", 16, 16);
        final boolean[] diamondVisible = {false};
        textureButton.onClick(event -> {
            diamondVisible[0] = !diamondVisible[0];
            texture.animateTexture(diamondVisible[0] ? diamond : stone, smooth);
            texture.animateTint(diamondVisible[0]
                    ? new MutableColor(0.75f, 1.0f, 1.0f, 1.0f)
                    : new MutableColor(1.0f, 1.0f, 1.0f, 1.0f), smooth);
            texture.animateRotation(diamondVisible[0] ? 12.0f : -12.0f, smooth);
        });

        VBox colorStack = new VBox();
        colorStack.spacing(6.0f);
        colorStack.layout(style -> style.size(154.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        colorStack.addChild(colorCard);
        colorStack.addChild(colorButton);

        VBox textureStack = new VBox();
        textureStack.spacing(6.0f);
        textureStack.layout(style -> style.size(154.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        textureStack.addChild(texture);
        textureStack.addChild(textureButton);

        visualRow.addChild(colorStack);
        visualRow.addChild(textureStack);
        page.addChild(section("Visual properties", visualRow));

        WrapPanel loopRow = new WrapPanel();
        loopRow.spacing(8.0f);
        loopRow.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));

        TransitionSpec pulseLoop = TransitionSpec.of(0.70f, AnimationEasing.EASE_IN_OUT).loop().yoyo();
        TransitionSpec spinLoop = TransitionSpec.of(1.20f, AnimationEasing.LINEAR).loop();

        Box loopCard = panelBox(0.052f, 0.052f, 0.090f, 0.94f);
        loopCard.transformOrigin(TransformOrigin.CENTER);
        loopCard.layout(style -> style.size(150.0f, 46.0f).flexGrow(0).flexShrink(0.0f));
        Label loopLabel = new Label("Looping pulse");
        loopLabel.layout(style -> style.margin(8.0f).size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        loopCard.addChild(loopLabel);

        Button startLoop = new Button("Start loop");
        startLoop.layout(style -> style.size(92.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        startLoop.onClick(event -> {
            loopCard.stopAnimations();
            loopCard.opacity(0.58f);
            loopCard.animateOpacity(1.0f, pulseLoop);
            loopCard.animateScale(1.08f, 1.08f, pulseLoop);
            loopCard.animateBorderColor(new MutableColor(0.60f, 0.45f, 1.0f, 1.0f), pulseLoop);
            loopCard.rotationDegrees(0.0f);
            loopCard.animateRotation(360.0f, spinLoop);
        });

        Button stopLoop = new Button("Stop loop");
        stopLoop.layout(style -> style.size(86.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        stopLoop.onClick(event -> {
            loopCard.stopAnimations();
            loopCard.animateOpacity(1.0f, quick);
            loopCard.animateScale(1.0f, 1.0f, quick);
            loopCard.animateRotation(0.0f, quick);
            loopCard.animateBorderColor(new MutableColor(0.20f, 0.28f, 0.36f, 0.75f), quick);
        });

        loopRow.addChild(loopCard);
        loopRow.addChild(startLoop);
        loopRow.addChild(stopLoop);
        page.addChild(section("Loop / stop", loopRow));

        page.addChild(paragraph("Shader transitions should normally be represented as a blend uniform or a two-pass crossfade, similar to the texture example above."));
        return page;
    }


    private static VBox originDemoStack(String text, Box tile) {
        VBox stack = new VBox();
        stack.spacing(3.0f);
        stack.layout(style -> style.size(112.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        stack.addChild(tile);
        Label label = new Label(text);
        label.layout(style -> style.size(LayoutConstraints.AUTO, 14.0f).flexGrow(0).flexShrink(0.0f));
        stack.addChild(label);
        return stack;
    }

    private static Box originDemoTile(TransformOrigin origin, MutableColor background, MutableColor border) {
        Box tile = panelBox(background.r(), background.g(), background.b(), background.a());
        tile.borderColor().set(border);
        tile.transformOrigin(origin);
        tile.layout(style -> style.size(112.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        return tile;
    }

    private static Widget nodeGraphPage() {
        VBox page = page("Node Graph", "Drag nodes, pan with wheel, Ctrl+wheel zooms, drag ports to connect.");

        Label status = new Label("NodeGraph: ready");
        status.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        NodeGraph graph = new NodeGraph()
                .selectionMode(NodeGraphSelectionMode.MULTIPLE)
                .gridSize(24.0f);
        graph.layout(style -> style.flexGrow(1).flexShrink(1.0f));

        NodeGraphItem input = graph.addItem("input", nodePane("Input", "2 ingredients"), 36.0f, 42.0f).size(180.0f, 108.0f);
        input.addPort("item-out", NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.42f).type("item");
        input.addPort("fluid-out", NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.76f).type("fluid");
        NodeGraphItem machine = graph.addItem("machine", nodePane("Machine", "Consumes item + fluid"), 300.0f, 80.0f).size(210.0f, 124.0f);
        machine.addPort("item-in", NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.32f).type("item");
        machine.addPort("fluid-in", NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.66f).type("fluid");
        machine.addPort("result-out", NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.50f).type("item");
        NodeGraphItem output = graph.addItem("output", nodePane("Output", "Result slot"), 600.0f, 56.0f).size(170.0f, 106.0f);
        output.addPort("result-in", NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.50f).type("item");
        graph.addConnection("item-route", new NodeGraphPortRef("input", "item-out"), new NodeGraphPortRef("machine", "item-in")).type("item");
        graph.addConnection("fluid-route", new NodeGraphPortRef("input", "fluid-out"), new NodeGraphPortRef("machine", "fluid-in")).type("fluid");
        graph.addConnection("result-route", new NodeGraphPortRef("machine", "result-out"), new NodeGraphPortRef("output", "result-in")).type("item");

        graph.onItemMoveEnded(event -> status.text("NodeGraph: moved " + event.itemId()));
        graph.onSelectionChanged(event -> status.text(event.newSelection().isEmpty()
                ? "NodeGraph: selection cleared"
                : "NodeGraph: selected " + String.join(", ", event.newSelection())));
        graph.onConnectionCreated(event -> status.text("NodeGraph: connected " + event.from().itemId() + " -> " + event.to().itemId()));
        graph.onConnectionRemoved(event -> status.text("NodeGraph: removed " + event.connectionId()));
        graph.onViewportChanged(event -> status.text(String.format(Locale.ROOT, "NodeGraph: zoom %.2fx", event.newZoom())));

        page.addChild(status);
        page.addChild(graph);
        return page;
    }


    private static Widget worldCanvasPage() {
        VBox page = page("World Canvas", "Generic pan/zoom viewport with screen-space anchors. Drag empty canvas, Ctrl+wheel zooms.");

        Label status = new Label("WorldCanvas: drag empty space, Ctrl+wheel zoom, click markers");
        status.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));

        WorldCanvas canvas = new WorldCanvas()
                .viewport(140.0f, 92.0f, 1.0f)
                .zoomRange(0.45f, 3.5f)
                .wheelPanStep(28.0f);
        canvas.layout(style -> style.size(LayoutConstraints.AUTO, 360.0f).flexGrow(1).flexShrink(1.0f));

        canvas.addWorldLayer((world, draw) -> {
            float left = world.layoutBounds().x();
            float top = world.layoutBounds().y();
            float width = world.layoutBounds().width();
            float height = world.layoutBounds().height();

            draw.addRectFilled(left, top, width, height, new MutableColor(0.025f, 0.032f, 0.050f, 0.96f));

            float worldLeft = -240.0f;
            float worldTop = -140.0f;
            float worldWidth = 760.0f;
            float worldHeight = 420.0f;
            float mapX = world.worldToRootX(worldLeft);
            float mapY = world.worldToRootY(worldTop);
            float mapW = world.viewport().zoom() * worldWidth;
            float mapH = world.viewport().zoom() * worldHeight;

            draw.addRectFilled(mapX, mapY, mapW, mapH, new MutableColor(0.055f, 0.075f, 0.115f, 0.90f));
            draw.addRect(mapX, mapY, mapW, mapH, new MutableColor(0.27f, 0.42f, 0.70f, 0.85f), 1.0f);

            for (int x = -200; x <= 480; x += 80) {
                float gx = world.worldToRootX(x);
                draw.addLine(gx, mapY, gx, mapY + mapH, new MutableColor(0.20f, 0.30f, 0.46f, 0.34f), 1.0f);
            }
            for (int y = -120; y <= 260; y += 80) {
                float gy = world.worldToRootY(y);
                draw.addLine(mapX, gy, mapX + mapW, gy, new MutableColor(0.20f, 0.30f, 0.46f, 0.34f), 1.0f);
            }

            float routeAx = world.worldToRootX(-80.0f);
            float routeAy = world.worldToRootY(40.0f);
            float routeBx = world.worldToRootX(180.0f);
            float routeBy = world.worldToRootY(-30.0f);
            float routeCx = world.worldToRootX(340.0f);
            float routeCy = world.worldToRootY(130.0f);
            draw.addLine(routeAx, routeAy, routeBx, routeBy, new MutableColor(0.65f, 0.82f, 1.0f, 0.78f), 2.0f);
            draw.addLine(routeBx, routeBy, routeCx, routeCy, new MutableColor(0.65f, 0.82f, 1.0f, 0.78f), 2.0f);
        });

        addMapMarker(canvas, status, "Camp", -80.0f, 40.0f);
        addMapMarker(canvas, status, "Vault", 180.0f, -30.0f);
        addMapMarker(canvas, status, "Forge", 340.0f, 130.0f);
        addMapMarker(canvas, status, "Orbit", 60.0f, 210.0f);

        canvas.onViewportChanged(event -> status.text(String.format(Locale.ROOT,
                "WorldCanvas: offset %.0f, %.0f | zoom %.2fx | anchors %d",
                event.newX(), event.newY(), event.newZoom(), canvas.anchorLayer().size())));

        page.addChild(status);
        page.addChild(canvas);
        return page;
    }

    private static void addMapMarker(WorldCanvas canvas, Label status, String title, float worldX, float worldY) {
        Button marker = new Button(title);
        marker.onClick(event -> status.text(String.format(Locale.ROOT,
                "WorldCanvas: clicked %s at world %.0f, %.0f", title, worldX, worldY)));
        canvas.anchorLayer().add(title.toLowerCase(Locale.ROOT), worldX, worldY, marker)
                .screenSize(72.0f, 22.0f)
                .pivot(0.5f, 0.5f);
    }


    private static Widget mapCanvasPage() {
        VBox page = page("Map Canvas", "Map-specific wrapper over WorldCanvas: projected markers, calibrated coordinates and route layer.");

        Label status = new Label("MapCanvas: Ctrl+wheel zoom, drag empty map, click markers");
        status.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));

        MapProjection projection = MapProjection.affine()
                .worldPoint(-2048.0f, -1024.0f).mapPoint(64.0f, 64.0f)
                .worldPoint(2048.0f, 1024.0f).mapPoint(4032.0f, 1984.0f)
                .build();

        MapCanvas map = new MapCanvas()
                .mapSize(4096.0f, 2048.0f)
                .projection(projection)
                .viewport(118.0f, 74.0f, 0.44f)
                .zoomRange(0.16f, 1.65f)
                .gridSize(256.0f)
                .gridVisible(false)
                .mapBorderVisible(false)
                .wheelPanStep(36.0f)
                .clampToMapBounds(true);
        map.layout(style -> style.size(LayoutConstraints.AUTO, 380.0f).flexGrow(1).flexShrink(1.0f));
        map.backgroundColor().set(0.018f, 0.023f, 0.036f, 0.98f);
        map.mapColor().set(0.040f, 0.055f, 0.088f, 0.96f);
        map.borderColor().set(0.42f, 0.58f, 0.92f, 0.86f);
        map.gridColor().set(0.25f, 0.36f, 0.55f, 0.22f);

        addDestinyMapBackground(map);

        map.addWorldLayer((world, draw) -> {
            float campX = map.worldToRootX(map.externalToMapX(-1300.0f));
            float campY = map.worldToRootY(map.externalToMapY(-320.0f));
            float vaultX = map.worldToRootX(map.externalToMapX(280.0f));
            float vaultY = map.worldToRootY(map.externalToMapY(-160.0f));
            float forgeX = map.worldToRootX(map.externalToMapX(1200.0f));
            float forgeY = map.worldToRootY(map.externalToMapY(520.0f));
            float playerX = map.worldToRootX(map.externalToMapX(-180.0f));
            float playerY = map.worldToRootY(map.externalToMapY(620.0f));

            float dashPhase = (System.nanoTime() / 1_000_000_000.0f * 18.0f) % 23.0f;
            Paint routeGlow = Paint.stroke(new MutableColor(0.32f, 0.58f, 1.0f, 0.18f), 6.0f)
                    .blend(BlendMode.ADDITIVE);
            Paint routeStroke = Paint.stroke(new MutableColor(0.55f, 0.78f, 1.0f, 0.76f), 2.0f)
                    .dash(14.0f, 9.0f)
                    .dashOffset(dashPhase)
                    .blend(BlendMode.ADDITIVE);
            Paint playerRoute = Paint.stroke(new MutableColor(1.0f, 0.82f, 0.38f, 0.62f), 1.5f)
                    .dash(9.0f, 7.0f)
                    .dashOffset(dashPhase * 0.65f)
                    .blend(BlendMode.ADDITIVE);
            draw.line(campX, campY, vaultX, vaultY, routeGlow);
            draw.line(vaultX, vaultY, forgeX, forgeY, routeGlow);
            draw.line(campX, campY, vaultX, vaultY, routeStroke);
            draw.line(vaultX, vaultY, forgeX, forgeY, routeStroke);
            draw.line(vaultX, vaultY, playerX, playerY, playerRoute);
        });

        addDestinationMarker(map, status, "Camp", -1300.0f, -320.0f, true);
        addDestinationMarker(map, status, "Vault", 280.0f, -160.0f, false);
        addDestinationMarker(map, status, "Forge", 1200.0f, 520.0f, false);
        addDestinationMarker(map, status, "Quest", 780.0f, 860.0f, false);
        addDestinationMarker(map, status, "Player", -180.0f, 620.0f, false);

        map.onViewportChanged(event -> status.text(String.format(Locale.ROOT,
                "MapCanvas: offset %.0f, %.0f | zoom %.2fx | markers %d",
                event.newX(), event.newY(), event.newZoom(), map.anchorLayer().size())));

        page.addChild(status);
        page.addChild(map);
        return page;
    }

    private static void addDestinyMapBackground(MapCanvas map) {
        map.addWorldLayer((world, draw) -> {
            float mapX = map.worldToRootX(0.0f);
            float mapY = map.worldToRootY(0.0f);
            float mapW = map.viewport().zoom() * map.mapWidth();
            float mapH = map.viewport().zoom() * map.mapHeight();
            if (mapW <= 1.0f || mapH <= 1.0f) return;

            ShaderUniforms uniforms = ShaderUniforms.create()
                    .setVec2("ViewportOffset", map.viewport().x(), map.viewport().y())
                    .setVec2("MapSize", map.mapWidth(), map.mapHeight())
                    .setFloat("Zoom", map.viewport().zoom());

            draw.addDrawCmd(DrawCommand.shader(
                    MAP_DESTINY_BACKGROUND_SHADER,
                    new MutableRect(mapX, mapY, mapW, mapH),
                    uniforms).shaderOptions(MAP_AURORA_BACKGROUND_OPTIONS));

            if (map.gridVisible() || map.mapBorderVisible()) {
                renderAuroraMapGrid(map, draw, mapX, mapY, mapW, mapH);
            }
        });
    }


    private static void renderAuroraMapGrid(MapCanvas map, DrawScope draw,
                                            float mapX, float mapY, float mapW, float mapH) {
        if (map.gridVisible()) {
            float safeGrid = Math.max(1.0f, map.gridSize());
            for (float x = 0.0f; x <= map.mapWidth() + 0.0001f; x += safeGrid) {
                float gx = map.worldToRootX(x);
                draw.addLine(gx, mapY, gx, mapY + mapH, map.gridColor(), 1.0f);
            }
            for (float y = 0.0f; y <= map.mapHeight() + 0.0001f; y += safeGrid) {
                float gy = map.worldToRootY(y);
                draw.addLine(mapX, gy, mapX + mapW, gy, map.gridColor(), 1.0f);
            }
        }
        if (map.mapBorderVisible()) {
            draw.addRect(mapX, mapY, mapW, mapH, map.borderColor(), 1.0f);
        }
    }
    private static Button addDestinationMarker(MapCanvas map, Label status, String title,
                                               float worldX, float worldY, boolean selected) {
        Button marker = destinationMarkerButton(title, selected);
        map.addProjectedMarkerWidget(title.toLowerCase(Locale.ROOT), worldX, worldY, marker)
                .screenSize(destinationMarkerWidth(title), 22.0f)
                .pivot(0.5f, 0.5f);
        marker.onClick(event -> status.text(String.format(Locale.ROOT,
                "MapCanvas: %s world %.0f, %.0f -> map %.0f, %.0f",
                title, worldX, worldY, map.externalToMapX(worldX), map.externalToMapY(worldY))));
        return marker;
    }

    private static Button destinationMarkerButton(String title, boolean selected) {
        Button marker = new Button(RichText.builder()
                .uppercase()
                .tracking(0.12f)
                .append(title)
                .build());
        marker.textPaddingX(6.0f);
        marker.interactionTransitions(true);
        marker.backgroundVisible(true);
        marker.borderVisible(true);
        marker.borderWidth(selected ? 2.0f : 1.0f);
        marker.radius(4.0f);

        switch (title.toLowerCase(Locale.ROOT)) {
            case "camp" -> applyMarkerColors(marker,
                    0.060f, 0.130f, 0.110f, 0.94f,
                    0.42f, 0.88f, 0.70f, 0.95f,
                    0.88f, 1.00f, 0.95f, 1.00f);
            case "vault" -> applyMarkerColors(marker,
                    0.100f, 0.085f, 0.145f, 0.94f,
                    0.74f, 0.62f, 1.00f, 0.95f,
                    0.96f, 0.92f, 1.00f, 1.00f);
            case "forge" -> applyMarkerColors(marker,
                    0.145f, 0.078f, 0.045f, 0.94f,
                    1.00f, 0.62f, 0.32f, 0.95f,
                    1.00f, 0.93f, 0.84f, 1.00f);
            case "quest" -> applyMarkerColors(marker,
                    0.140f, 0.120f, 0.045f, 0.94f,
                    1.00f, 0.86f, 0.35f, 0.95f,
                    1.00f, 0.98f, 0.86f, 1.00f);
            case "player" -> applyMarkerColors(marker,
                    0.045f, 0.105f, 0.150f, 0.94f,
                    0.35f, 0.88f, 1.00f, 0.95f,
                    0.86f, 0.98f, 1.00f, 1.00f);
            default -> applyMarkerColors(marker,
                    0.075f, 0.095f, 0.130f, 0.92f,
                    0.58f, 0.72f, 0.95f, 0.92f,
                    0.92f, 0.96f, 1.00f, 1.00f);
        }
        return marker;
    }

    private static void applyMarkerColors(Button marker,
                                          float br, float bg, float bb, float ba,
                                          float rr, float rg, float rb, float ra,
                                          float tr, float tg, float tb, float ta) {
        marker.background().set(br, bg, bb, ba);
        marker.borderColor().set(rr, rg, rb, ra);
        marker.textColor().set(tr, tg, tb, ta);
    }

    private static float destinationMarkerWidth(String title) {
        return switch (title.toLowerCase(Locale.ROOT)) {
            case "vault" -> 74.0f;
            case "forge" -> 76.0f;
            case "quest" -> 78.0f;
            default -> 72.0f;
        };
    }

    private static OverlayLayer overlaysPage() {
        VBox page = page("Overlays", "Popup, Tooltip, ContextMenu, Toast and WindowWidget above normal layout.");

        WrapPanel row = wrap();
        Button tooltipAnchor = new Button("Hover me");
        Button vanillaTooltipAnchor = new Button("Item tip");
        Button popupAnchor = new Button("Popup");
        Button menuButton = new Button("Context");
        Button toastButton = new Button("Toast");
        Button windowButton = new Button("Window");
        Button modalButton = new Button("Modal");
        ToggleButton freeDrag = new ToggleButton("Free drag");
        for (Button b : List.of(tooltipAnchor, vanillaTooltipAnchor, popupAnchor, menuButton, toastButton, windowButton, modalButton, freeDrag)) {
            b.layout(style -> style.size(86.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
            row.addChild(b);
        }
        page.addChild(section("Overlay controls", row));

        WrapPanel vanillaItemTooltips = wrap();
        Button swordTooltip = new Button("Sword tooltip");
        Button dynamicTooltip = new Button("Apple tooltip");
        Button cycleTooltipStack = new Button("Cycle stack");
        MinecraftItemPreviewWidget previewTooltip = itemPreview("Preview tooltip", Items.NETHERITE_PICKAXE);
        for (Button b : List.of(swordTooltip, dynamicTooltip, cycleTooltipStack)) {
            b.layout(style -> style.size(112.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
            vanillaItemTooltips.addChild(b);
        }
        vanillaItemTooltips.addChild(previewTooltip);
        page.addChild(section("Vanilla item tooltips", vanillaItemTooltips));

        ItemStack[] dynamicTooltipStack = {new ItemStack(Items.APPLE)};
        Runnable updateDynamicTooltipLabel = () -> dynamicTooltip.text(
                dynamicTooltipStack[0].getHoverName().getString() + " tooltip");
        cycleTooltipStack.onClick(event -> {
            dynamicTooltipStack[0] = dynamicTooltipStack[0].is(Items.APPLE)
                    ? new ItemStack(Items.EMERALD)
                    : dynamicTooltipStack[0].is(Items.EMERALD)
                    ? new ItemStack(Items.DIAMOND_PICKAXE)
                    : new ItemStack(Items.APPLE);
            updateDynamicTooltipLabel.run();
        });
        updateDynamicTooltipLabel.run();

        Label status = new Label("WindowManager: idle");
        status.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(status);
        page.addChild(paragraph("Tooltips do not capture input. Popups are anchored. Windows drag/resize. Modal windows block input below them."));

        OverlayLayer layer = new OverlayLayer(page);
        Popup popup = new Popup(popupAnchor, samplePane("Popup content", "Anchored retained popup."));
        ContextMenu createSubmenu = new ContextMenu()
                .item("Recipe", () -> status.text("Context: Create Recipe"))
                .item("Machine", () -> status.text("Context: Create Machine"));
        ContextMenu menu = new ContextMenu()
                .item("Inspect", () -> status.text("Context: Inspect"))
                .submenu("Create", createSubmenu)
                .item("Duplicate", () -> status.text("Context: Duplicate"))
                .separator()
                .item("Delete", () -> status.text("Context: Delete"));
        Toast toast = new Toast("Toast / NotificationView").duration(2.5f);
        NotificationView notification = new NotificationView("NotificationView: persistent info card").duration(0.0f);
        notification.layout(style -> style.size(260.0f, 46.0f).flexGrow(0).flexShrink(0.0f));

        WindowWidget window = new WindowWidget("Example Window", samplePane("Dialog body", "Drag title, resize corners, close with x."))
                .position(260.0f, 74.0f)
                .closeOnOutsideClick(false);
        window.layout(style -> style.size(230.0f, 132.0f).flexGrow(0).flexShrink(0.0f));
        WindowWidget modal = new WindowWidget("Modal Window", samplePane("Modal body", "Input below this dialog is blocked until closed."))
                .position(230.0f, 112.0f)
                .modal(true)
                .closeOnOutsideClick(false);
        modal.layout(style -> style.size(250.0f, 136.0f).flexGrow(0).flexShrink(0.0f));

        popupAnchor.onClick(event -> popup.toggle());
        menuButton.onClick(event -> menu.toggle(menuButton.layoutBounds().x(), menuButton.layoutBounds().y() + menuButton.layoutBounds().height() + 4.0f));
        toastButton.onClick(event -> toast.toast("Saved UniGUI demo state."));
        windowButton.onClick(event -> window.toggle());
        modalButton.onClick(event -> modal.openModal());
        freeDrag.onCheckedChanged(event -> window.constrainToHost(!event.newValue()));
        window.onOpened(event -> status.text("WindowManager: opened"));
        window.onClosed(event -> status.text("WindowManager: closed"));
        window.onMoved(event -> status.text(String.format(Locale.ROOT, "Window moved %.0f, %.0f", event.newX(), event.newY())));
        window.onResized(event -> status.text(String.format(Locale.ROOT, "Window resized %.0fx%.0f", event.newWidth(), event.newHeight())));
        modal.onModalOpened(event -> status.text("Modal opened depth " + event.stackDepth()));
        modal.onModalClosed(event -> status.text("Modal closed depth " + event.stackDepth()));

        layer.addOverlay(new Tooltip(tooltipAnchor, "Tooltip through OverlayLayer"));
        layer.addOverlay(new MinecraftItemTooltip(vanillaTooltipAnchor, new ItemStack(Items.DIAMOND_SWORD)));
        layer.addOverlay(new MinecraftItemTooltip(swordTooltip, new ItemStack(Items.DIAMOND_SWORD)));
        layer.addOverlay(new MinecraftItemTooltip(dynamicTooltip, () -> dynamicTooltipStack[0]));
        previewTooltip.addVanillaTooltip(layer);
        layer.addOverlay(new Tooltip(popupAnchor, "Click to toggle Popup"));
        layer.addOverlay(popup);
        layer.addOverlay(menu);
        layer.addOverlay(toast);
        layer.addOverlay(notification);
        layer.addOverlay(window);
        layer.addOverlay(modal);
        return layer;
    }

    private static VBox minecraftPage() {
        VBox page = page("Minecraft", "Backend-specific item, block and entity previews plus item/texture registry pickers.");

        WrapPanel previews = wrap();
        previews.addChild(itemPreview("Diamond", Items.DIAMOND));
        previews.addChild(itemPreview("Apple", Items.APPLE));
        previews.addChild(itemPreview("Pickaxe", Items.DIAMOND_PICKAXE));
        previews.addChild(blockPreview("Crafting", Blocks.CRAFTING_TABLE));
        previews.addChild(blockPreview("Furnace", Blocks.FURNACE));
        previews.addChild(blockPreview("Chest", Blocks.CHEST));
        previews.addChild(entityPreview("Zombie", EntityType.ZOMBIE));
        previews.addChild(entityPreview("Creeper", EntityType.CREEPER));
        previews.addChild(entityPreview("Villager", EntityType.VILLAGER));
        page.addChild(section("Preview widgets", previews));

        VBox registryPickers = new VBox();
        registryPickers.spacing(6.0f);
        registryPickers.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        Label pickerStatus = new Label("Click a picker to open a searchable modal icon grid.");
        pickerStatus.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));

        MinecraftItemPickerWidget itemPicker = new MinecraftItemPickerWidget();
        itemPicker.layout(style -> style.size(304.0f, 38.0f).flexGrow(0).flexShrink(0.0f));
        itemPicker.selectItem(Items.DIAMOND);

        MinecraftTexturePickerWidget texturePicker = new MinecraftTexturePickerWidget();
        texturePicker.layout(style -> style.size(304.0f, 38.0f).flexGrow(0).flexShrink(0.0f));
        texturePicker.selectId(ResourceLocation.tryBuild("minecraft", "textures/block/stone.png"));

        Runnable updatePickerStatus = () -> pickerStatus.text("Item: "
                + (itemPicker.selectedId() == null ? "<none>" : itemPicker.selectedId())
                + "  |  Texture: "
                + (texturePicker.selectedId() == null ? "<none>" : texturePicker.selectedId()));
        itemPicker.onSelectionChanged(event -> updatePickerStatus.run());
        texturePicker.onSelectionChanged(event -> updatePickerStatus.run());
        updatePickerStatus.run();

        WrapPanel pickerRow = wrap();
        pickerRow.addChild(itemPicker);
        pickerRow.addChild(texturePicker);
        registryPickers.addChild(pickerStatus);
        registryPickers.addChild(pickerRow);
        page.addChild(section("Registry pickers", registryPickers));

        WrapPanel mutable = wrap();
        Button swap = new Button("Swap item");
        swap.layout(style -> style.size(82.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        MinecraftItemPreviewWidget item = itemPreview("Mutable", Items.APPLE);
        swap.onClick(event -> item.stack(new ItemStack(item.stack().is(Items.APPLE) ? Items.EMERALD : Items.APPLE)));
        mutable.addChild(swap);
        mutable.addChild(item);
        page.addChild(section("Mutable ItemStack", mutable));
        return page;
    }

    private static VBox stressPage() {
        VBox page = page("Stress", "Dense retained-widget and Minecraft-preview smoke test.");
        WrapPanel entities = wrap();
        entities.spacing(1.0f);
        entities.lineSpacing(1.0f);
        List<EntityType<? extends LivingEntity>> types = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.VILLAGER, EntityType.PIG, EntityType.COW, EntityType.SHEEP, EntityType.CHICKEN);
        for (int i = 0; i < 96; i++) {
            MinecraftEntityPreviewWidget entity = new MinecraftEntityPreviewWidget("", types.get(i % types.size()));
            entity.labelVisible(false);
            entity.backgroundVisible(false);
            entity.borderVisible(false);
            entity.previewSize(16.0f);
            entity.look(7.0f, 4.0f);
            entity.layout(style -> style.size(26.0f, 26.0f).flexGrow(0).flexShrink(0.0f));
            entities.addChild(entity);
        }
        page.addChild(section("Entity grid", entities));
        return page;
    }

    private static DockingRoot compactDockingRoot() {
        DockingRoot docking = new DockingRoot();
        docking.addDocument("scene", "Scene", samplePane("Scene", "Center document tab."))
                .addDocument("recipe", "Recipe", samplePane("Recipe", "Dirty document tab."))
                .addToolPane("assets", "Assets", samplePane("Assets", "Left tool pane."), DockArea.LEFT)
                .addToolPane("inspector", "Inspector", samplePane("Inspector", "Right tool pane."), DockArea.RIGHT)
                .addToolPane("log", "Log", samplePane("Log", "Bottom output."), DockArea.BOTTOM)
                .selectPane("scene");
        DockPane recipe = docking.manager().findPane("recipe");
        if (recipe != null) recipe.dirty(true);
        DockPane assets = docking.manager().findPane("assets");
        if (assets != null) assets.pinned(false);
        docking.splitHandleRenderer(DockSplitHandleRenderers.IMGUI_STYLE);
        return docking;
    }

    private static VBox page(String title, String subtitle) {
        VBox page = new VBox();
        page.spacing(8.0f);
        page.layout(style -> style.margin(8.0f).flexGrow(1).flexShrink(1.0f));

        Label heading = new Label(title);
        heading.layout(style -> style.size(LayoutConstraints.AUTO, 20.0f).flexGrow(0).flexShrink(0.0f));
        TextBlock sub = paragraph(subtitle);
        sub.layout(style -> style.size(LayoutConstraints.AUTO, 34.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(heading);
        page.addChild(sub);
        return page;
    }

    private static Box section(String title, Widget body) {
        Box box = panelBox(0.045f, 0.052f, 0.068f, 0.88f);
        box.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        VBox content = new VBox();
        content.spacing(6.0f);
        content.layout(style -> style.margin(8.0f).flexGrow(0).flexShrink(0.0f));
        Label label = new Label(title);
        label.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        content.addChild(label);
        content.addChild(body);
        box.addChild(content);
        return box;
    }

    private static VBox samplePane(String title, String text) {
        VBox pane = new VBox();
        pane.spacing(4.0f);
        pane.layout(style -> style.margin(6.0f).flexGrow(0).flexShrink(0.0f));
        Label label = new Label(title);
        label.layout(style -> style.size(LayoutConstraints.AUTO, 16.0f).flexGrow(0).flexShrink(0.0f));
        TextBlock body = paragraph(text);
        body.layout(style -> style.size(LayoutConstraints.AUTO, 42.0f).flexGrow(0).flexShrink(0.0f));
        pane.addChild(label);
        pane.addChild(body);
        return pane;
    }

    private static VBox nodePane(String title, String text) {
        VBox pane = samplePane(title, text);
        Button button = new Button("Action");
        button.layout(style -> style.size(72.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        pane.addChild(button);
        return pane;
    }

    private static ExpandablePanel expandable(String title, String text) {
        ExpandablePanel panel = new ExpandablePanel(title);
        panel.addContent(paragraph(text));
        panel.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        return panel;
    }

    private static TextBlock paragraph(String text) {
        TextBlock block = new TextBlock(text);
        block.wrap(true);
        block.overflowMode(TextOverflowMode.CLIP);
        return block;
    }

    private static WrapPanel wrap() {
        WrapPanel wrap = new WrapPanel();
        wrap.spacing(8.0f);
        wrap.lineSpacing(8.0f);
        wrap.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        return wrap;
    }

    private static Box infoCard(String title, String body) {
        Box card = panelBox(0.055f, 0.064f, 0.085f, 0.92f);
        card.layout(style -> style.size(174.0f, LayoutConstraints.AUTO).minHeight(86.0f).flexGrow(0).flexShrink(0.0f));

        VBox content = new VBox();
        content.spacing(4.0f);
        content.layout(style -> style.margin(6.0f).flexGrow(0).flexShrink(0.0f));

        Label label = new Label(title);
        label.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        TextBlock text = paragraph(body);
        text.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));

        content.addChild(label);
        content.addChild(text);
        card.addChild(content);
        return card;
    }

    private static Box spinnerTile(String title, Spinner.Style style, float speed, float width, float height) {
        Box tile = panelBox(0.045f, 0.052f, 0.068f, 0.90f);
        tile.layout(layout -> layout.size(132.0f, 86.0f).flexGrow(0).flexShrink(0.0f));

        VBox content = new VBox();
        content.spacing(5.0f);
        content.layout(layout -> layout
                .margin(6.0f)
                .size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .align(Alignment.STRETCH, Alignment.CENTER)
                .flexGrow(0)
                .flexShrink(0.0f));

        Spinner spinner = new Spinner(style)
                .speed(speed)
                .thickness(style.name().startsWith("DOTS") ? 3.8f : 3.0f)
                .dots(style == Spinner.Style.DOTS_MOVING ? 5 : 9)
                .activeDots(4)
                .arcs(style == Spinner.Style.SECTION_FADE ? 5 : 3)
                .segments(32);
        spinner.accentColor().set(0.25f, 0.78f, 1.0f, 1.0f);
        spinner.secondaryColor().set(1.0f, 1.0f, 1.0f, 0.95f);
        spinner.trackColor().set(0.14f, 0.17f, 0.22f, 0.42f);
        spinner.layout(layout -> layout
                .size(width, height)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0)
                .flexShrink(0.0f));

        Label label = new Label(title);
        label.noWrap().clipOverflow();
        label.layout(layout -> layout
                .size(120.0f, 16.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0)
                .flexShrink(0.0f));

        content.addChild(spinner);
        content.addChild(label);
        tile.addChild(content);
        return tile;
    }

    private static Box smokeTile(String title, float width, float height, float r, float g, float b) {
        Box tile = panelBox(r, g, b, 0.90f);
        tile.layout(style -> style.size(width, height).flexGrow(0).flexShrink(0.0f));
        Label label = new Label(title);
        label.layout(style -> style.margin(6.0f, 4.0f).size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).align(Alignment.CENTER, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
        tile.addChild(label);
        return tile;
    }

    private static MinecraftItemPreviewWidget itemPreview(String label, net.minecraft.world.level.ItemLike item) {
        MinecraftItemPreviewWidget preview = new MinecraftItemPreviewWidget(label, item);
        preview.previewSize(34.0f);
        preview.layout(style -> style.size(86.0f, 64.0f).flexGrow(0).flexShrink(0.0f));
        return preview;
    }

    private static MinecraftBlockPreviewWidget blockPreview(String label, net.minecraft.world.level.block.Block block) {
        MinecraftBlockPreviewWidget preview = new MinecraftBlockPreviewWidget(label, block);
        preview.previewSize(34.0f);
        preview.layout(style -> style.size(86.0f, 64.0f).flexGrow(0).flexShrink(0.0f));
        return preview;
    }

    private static MinecraftEntityPreviewWidget entityPreview(String label, EntityType<? extends LivingEntity> type) {
        MinecraftEntityPreviewWidget preview = new MinecraftEntityPreviewWidget(label, type);
        preview.previewSize(38.0f);
        preview.layout(style -> style.size(90.0f, 70.0f).flexGrow(0).flexShrink(0.0f));
        return preview;
    }

    private static Box backgroundFrame() {
        Box frame = panelBox(0.020f, 0.024f, 0.032f, 0.98f);
        frame.layout(style -> style.align(Alignment.STRETCH, Alignment.STRETCH).margin(6.0f).flexGrow(1).flexShrink(1.0f));
        return frame;
    }

    private static Box panelBox(float r, float g, float b, float a) {
        Box box = new Box();
        box.themeEnabled(false);
        box.backgroundVisible(true);
        box.borderVisible(true);
        box.radius(4.0f);
        box.background().set(r, g, b, a);
        box.borderColor().set(0.20f, 0.28f, 0.36f, 0.75f);
        return box;
    }
}

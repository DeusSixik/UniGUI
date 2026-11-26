package dev.sixik.unigui.demo.ui.scene;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.rendering.Scene3D;

/**
 * Рисует звёзды и дымку за прозрачным вьюпортом туториала о печке.
 */
final class FurnaceSky extends WidgetBase {
    /**
     * Цвет фона при недоступном шейдере.
     */
    private static final MutableColor NIGHT = MutableColor.fromArgb(0xFF0C142B);
    /**
     * Непрозрачный проход ровно по границам виджета.
     */
    private static final ShaderDrawOptions OPTIONS = ShaderDrawOptions.defaults().blend(false).squareVertexOffset(0);
    /**
     * GLSL 1.50 для OpenGL и конвертера Minecraft RenderPipeline.
     */
    private static final ShaderHandle SKY = ShaderHandle.source("unigui:furnace_sky", """
            #version 150
            
            in vec3 Position;
            out vec2 uv;
            uniform vec2 ScreenSize;
            uniform vec4 SquareVertex;
            
            void main() {
                uv = vec2(Position.x, -Position.y) * 0.5 + 0.5;
                vec2 p = mix(SquareVertex.xy, SquareVertex.zw, uv) / max(ScreenSize, vec2(1.0));
                gl_Position = vec4(2.0 * p.x - 1.0, 1.0 - 2.0 * p.y, 0.0, 1.0);
            }
            """, """
            #version 150
            
            in vec2 uv;
            out vec4 FragColor;
            uniform vec4 SquareVertex;
            uniform vec4 Orbit;
            uniform float Time;
            
            float hash(vec2 p) {
                vec3 q = fract(vec3(p.xyx) * 0.1031);
                q += dot(q, q.yzx + 33.33);
                return fract((q.x + q.y) * q.z);
            }
            
            float noise(vec2 p) {
                vec2 cell = floor(p);
                vec2 f = fract(p);
                f = f * f * (3.0 - 2.0 * f);
                float top = mix(hash(cell), hash(cell + vec2(1.0, 0.0)), f.x);
                float bottom = mix(hash(cell + vec2(0.0, 1.0)), hash(cell + vec2(1.0)), f.x);
                return mix(top, bottom, f.y);
            }
            
            float smoke(vec2 p) {
                float density = 0.0;
                float weight = 0.52;
                for (int i = 0; i < 4; ++i) {
                    density += weight * noise(p);
                    p = mat2(1.8, 0.9, -0.9, 1.8) * p + vec2(13.4, 7.1);
                    weight *= 0.5;
                }
                return density;
            }
            
            vec3 stars(vec2 p, float frequency, float radius, float threshold) {
                vec2 grid = p * frequency;
                vec2 cell = floor(grid);
                float seed = hash(cell);
                vec2 center = 0.20 + 0.60 * vec2(hash(cell + 3.1), hash(cell - 7.8));
                float r = length(fract(grid) - center);
                float aa = 0.65 * length(fwidth(grid));
                float core = 1.0 - smoothstep(max(0.0, radius - aa), radius + aa, r);
                float halo = 1.0 - smoothstep(0.0, 4.0 * radius + aa, r);
                float flicker = 0.78 + 0.22 * sin(seed * 40.0 + Time * (0.5 + seed));
                vec3 tint = mix(vec3(0.56, 0.72, 1.0), vec3(1.0, 0.86, 0.72), hash(cell + 23.9));
                return tint * (core + 0.24 * halo * halo) * flicker * step(threshold, seed);
            }
            
            void main() {
                vec2 size = SquareVertex.zw - SquareVertex.xy;
                vec2 p = (uv - 0.5) * vec2(size.x / max(size.y, 1.0), 1.0);
                // Orbit.xy holds sin(yaw), cos(yaw), so crossing ±pi is continuous.
                vec2 shift = vec2(0.23 * Orbit.x, -0.19 * Orbit.z + 0.10 * Orbit.y);
                float zoom = clamp(pow(Orbit.w, 0.23), 0.72, 1.50);
                vec2 field = p * zoom;
                // Keep the cloud band in view even after the client has been open for hours.
                vec2 drift = vec2(0.18, -0.12) * sin(Time * vec2(0.08, 0.067));
                vec2 cloud = field + shift * 0.36 + drift;
                float warp = noise(cloud * 2.4 + vec2(2.7, 4.1));
                cloud += vec2(warp, -warp) * 0.42;
                float band = 1.0 - smoothstep(0.08, 0.64, abs(cloud.y + 0.23 * cloud.x + 0.05));
                float broad = smoothstep(0.32, 0.72, smoke(cloud * vec2(2.7, 4.6)));
                float wisps = smoothstep(0.38, 0.76, smoke(cloud * vec2(4.0, 8.0) + 12.0));
            
                vec3 color = vec3(0.024, 0.037, 0.088);
                color += vec3(0.15, 0.25, 0.43) * broad * band;
                color += vec3(0.17, 0.09, 0.28) * wisps * band;
                color += stars(field + shift * 0.45 + 7.2, 32.0, 0.041, 0.74) * 0.35;
                color += stars(field + shift * 0.85 - 4.3, 19.0, 0.041, 0.80) * 0.64;
                color += stars(field + shift * 1.30 + 9.7, 10.0, 0.033, 0.88) * 0.90;
            
                float edge = dot(uv - 0.5, uv - 0.5);
                color *= mix(1.0, 0.50, smoothstep(0.08, 0.51, edge));
                FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
            }
            """);

    /**
     * Камера, за движением которой следует параллакс.
     */
    private final Scene3D viewport;
    /**
     * Начальная высота ортографической проекции в единицах сцены.
     */
    private final float referenceHeight;
    /**
     * Переиспользуемые параметры одного шейдерного прохода.
     */
    private final ShaderUniforms uniforms = ShaderUniforms.create();

    /**
     * Запоминает начальное приближение камеры и привязывает фон к её орбите.
     *
     * @param viewport вьюпорт с настроенной ортографической камерой.
     */
    FurnaceSky(Scene3D viewport) {
        this.viewport = viewport;
        referenceHeight = viewport.camera().orthographicHeight();
    }

    /**
     * Запрашивает обновление фона даже на паузе сценария.
     */
    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (visible()) invalidate(InvalidationFlags.VISUAL);
    }

    /**
     * Рисует подложку, передавая {@code Orbit = (sin(yaw), cos(yaw), pitch, h / h0)}.
     * Углы орбиты заданы в радианах, h — текущая высота проекции, h0 — начальная.
     * Синус и косинус сохраняют непрерывность при переходе yaw через границу ±π.
     *
     * @param context контекст рисования подложки в координатах UI.
     */
    @Override
    public void render(RenderContext context) {
        var b = layoutBounds();
        if (!visible() || b.width() <= 0 || b.height() <= 0) return;
        var orbit = viewport.orbit();
        uniforms.setVec4("Orbit", (float) Math.sin(orbit.yaw()), (float) Math.cos(orbit.yaw()),
                (float) orbit.pitch(), viewport.camera().orthographicHeight() / referenceHeight);
        context.addRectFilled(b.x(), b.y(), b.width(), b.height(), NIGHT);
        context.shader(SKY, b.x(), b.y(), b.width(), b.height(), uniforms, OPTIONS);
        SceneTutorialStage.drawFrame(context, b);
    }
}

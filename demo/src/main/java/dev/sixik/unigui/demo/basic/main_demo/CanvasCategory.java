package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.map.MapCanvas;
import dev.sixik.unigui.widgets.map.MapProjection;
import dev.sixik.unigui.widgets.world.WorldCanvas;

import java.util.List;
import java.util.Locale;

final class CanvasCategory extends DemoCategory {

    private static final ShaderHandle MAP_SHADER = ShaderHandle.source("unigui:demo_media_map_shader", """
            #version 150
            in vec3 Position;
            out vec2 uv;
            uniform vec2 ScreenSize;
            uniform vec4 SquareVertex;
            void main() {
                vec2 corner = vec2(Position.x * 0.5 + 0.5, 0.5 - Position.y * 0.5);
                vec2 p = mix(SquareVertex.xy, SquareVertex.zw, corner);
                gl_Position = vec4(p.x / max(ScreenSize.x, 1.0) * 2.0 - 1.0,
                        1.0 - p.y / max(ScreenSize.y, 1.0) * 2.0, 0.0, 1.0);
                uv = corner;
            }
            """, """
            #version 150
            in vec2 uv;
            out vec4 FragColor;
            uniform float Time;
            uniform vec2 MapSize;
            void main() {
                vec2 p = uv * MapSize / 180.0;
                float ridge = sin(p.x * 2.1 + sin(p.y * 1.7 + Time * 0.18) * 1.1);
                float current = sin(p.y * 3.0 - p.x * 0.8 + Time * 0.12);
                float glow = smoothstep(0.35, 0.96, ridge * 0.35 + current * 0.45 + 0.45);
                vec3 deep = vec3(0.018, 0.050, 0.105);
                vec3 sea = vec3(0.025, 0.31, 0.36);
                vec3 aurora = vec3(0.30, 0.72, 0.83);
                vec3 color = mix(deep, sea, smoothstep(0.05, 0.95, uv.y));
                color = mix(color, aurora, glow * 0.72);
                FragColor = vec4(color, 1.0);
            }
            """);
    private static final ShaderDrawOptions MAP_SHADER_OPTIONS = ShaderDrawOptions.defaults()
            .blend(true)
            .squareVertexOffset(0.0f);

    private static final List<Control> CONTROLS = List.of(
            new Control("world-canvas", "WorldCanvas", "Pan and zoom a generic world with screen-sized interactive anchors."),
            new Control("map-canvas", "MapCanvas", "Project external coordinates onto a bounded map with a grid and markers."));

    @Override
    String title() {
        return "Canvas";
    }

    @Override
    boolean expandedByDefault() {
        return false;
    }

    @Override
    List<Control> controls() {
        return CONTROLS;
    }

    @Override
    void addDemo(String controlId, VBox root) {
        switch (controlId) {
            case "world-canvas" -> addWorldCanvas(root);
            case "map-canvas" -> addMapCanvas(root);
            default -> root.addChild(samplePanel("Preview", "Interactive sample", new Label(controlId), ""));
        }
    }

    private void addMarker(MapCanvas map, Label status, String name, float worldX, float worldY) {
        Button marker = new Button(name);
        marker.onClick(event -> status.text(String.format(Locale.ROOT,
                "%s: world %.0f, %.0f -> map %.0f, %.0f", name, worldX, worldY,
                map.externalToMapX(worldX), map.externalToMapY(worldY))));
        map.addProjectedMarkerWidget(name.toLowerCase(Locale.ROOT), worldX, worldY, marker)
                .screenSize(68.0f, 22.0f)
                .pivot(0.5f, 0.5f);
    }

    private void addWorldCanvas(VBox root) {
        Label status = label("World coordinates: use drag to pan and Ctrl+wheel to zoom.",
                UniGuiDemo.GalleryTemplate.TEXT_DARK);
        WorldCanvas canvas = new WorldCanvas()
                .viewport(150.0f, 86.0f, 0.82f)
                .zoomRange(0.4f, 2.8f)
                .worldBounds(-360.0f, -220.0f, 760.0f, 480.0f)
                .clampToWorldBounds(true)
                .wheelPanStep(30.0f);
        canvas.layout(style -> style.widthPercent(100.0f).height(330.0f));
        canvas.addWorldLayer((world, draw) -> {
            float left = world.layoutBounds().x();
            float top = world.layoutBounds().y();
            float width = world.layoutBounds().width();
            float height = world.layoutBounds().height();
            draw.addRectFilled(left, top, width, height, new MutableColor(0.035f, 0.050f, 0.075f, 1.0f));

            float mapX = world.worldToRootX(-300.0f);
            float mapY = world.worldToRootY(-160.0f);
            float mapW = 650.0f * world.viewport().zoom();
            float mapH = 380.0f * world.viewport().zoom();
            draw.addRectFilled(mapX, mapY, mapW, mapH, new MutableColor(0.075f, 0.105f, 0.145f, 1.0f));
            draw.addRect(mapX, mapY, mapW, mapH, new MutableColor(0.26f, 0.42f, 0.60f, 0.9f), 1.0f);
            for (int x = -280; x <= 320; x += 80) {
                float gridX = world.worldToRootX(x);
                draw.addLine(gridX, mapY, gridX, mapY + mapH,
                        new MutableColor(0.25f, 0.38f, 0.51f, 0.28f), 1.0f);
            }
            for (int y = -120; y <= 200; y += 80) {
                float gridY = world.worldToRootY(y);
                draw.addLine(mapX, gridY, mapX + mapW, gridY,
                        new MutableColor(0.25f, 0.38f, 0.51f, 0.28f), 1.0f);
            }
            draw.addLine(world.worldToRootX(-120.0f), world.worldToRootY(40.0f),
                    world.worldToRootX(40.0f), world.worldToRootY(-40.0f),
                    new MutableColor(0.34f, 0.76f, 0.95f, 0.9f), 2.0f);
            draw.addLine(world.worldToRootX(40.0f), world.worldToRootY(-40.0f),
                    world.worldToRootX(220.0f), world.worldToRootY(100.0f),
                    new MutableColor(0.34f, 0.76f, 0.95f, 0.9f), 2.0f);
        });

        addWorldMarker(canvas, status, "Camp", -120.0f, 40.0f);
        addWorldMarker(canvas, status, "Vault", 40.0f, -40.0f);
        addWorldMarker(canvas, status, "Forge", 220.0f, 100.0f);
        canvas.onViewportChanged(event -> status.text(String.format(Locale.ROOT,
                "Offset %.0f, %.0f | zoom %.2fx | anchors %d", event.newX(), event.newY(),
                event.newZoom(), canvas.anchorLayer().size())));

        VBox sample = new VBox();
        sample.spacing(10.0f);
        sample.layout(style -> style.widthPercent(100.0f));
        sample.addChild(status);
        sample.addChild(canvas);
        root.addChild(samplePanel("World-space drawing and anchors", "The route is rendered in world coordinates; marker buttons retain a constant screen size while zooming.", sample,
                "WorldCanvas canvas = new WorldCanvas()\n        .viewport(150, 86, 0.82f)\n        .zoomRange(0.4f, 2.8f);\ncanvas.addWorldLayer((world, draw) -> draw.addLine(\n        world.worldToRootX(x1), world.worldToRootY(y1),\n        world.worldToRootX(x2), world.worldToRootY(y2), paint));\ncanvas.anchorLayer().add(\"camp\", x, y, new Button(\"Camp\"))\n        .screenSize(72, 22).pivot(0.5f, 0.5f);"));
    }

    private void addWorldMarker(WorldCanvas canvas, Label status, String title, float x, float y) {
        Button marker = new Button(title);
        marker.onClick(event -> status.text(String.format(Locale.ROOT,
                "%s marker at world %.0f, %.0f", title, x, y)));
        canvas.anchorLayer().add(title.toLowerCase(Locale.ROOT), x, y, marker)
                .screenSize(70.0f, 22.0f)
                .pivot(0.5f, 0.5f);
    }

    private void addMapCanvas(VBox root) {
        Label status = label("External coordinates are projected into map space.", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        MapProjection projection = MapProjection.affine()
                .worldPoint(-500.0f, -300.0f).mapPoint(0.0f, 0.0f)
                .worldPoint(500.0f, 300.0f).mapPoint(1200.0f, 720.0f)
                .build();
        MapCanvas map = new MapCanvas()
                .mapSize(1200.0f, 720.0f)
                .projection(projection)
                .viewport(42.0f, 34.0f, 0.48f)
                .zoomRange(0.18f, 1.6f)
                .gridSize(120.0f)
                .gridVisible(true)
                .mapBorderVisible(true)
                .clampToMapBounds(true);
        map.layout(style -> style.widthPercent(100.0f).height(330.0f));
        map.backgroundColor().set(0.035f, 0.050f, 0.075f, 1.0f);
        map.mapColor().set(0.10f, 0.18f, 0.20f, 1.0f);
        map.gridColor().set(0.43f, 0.62f, 0.57f, 0.35f);
        map.borderColor().set(0.34f, 0.76f, 0.72f, 0.9f);
        map.addWorldLayer((world, draw) -> {
            float campX = map.worldToRootX(map.externalToMapX(-320.0f));
            float campY = map.worldToRootY(map.externalToMapY(-160.0f));
            float lakeX = map.worldToRootX(map.externalToMapX(20.0f));
            float lakeY = map.worldToRootY(map.externalToMapY(-40.0f));
            float forgeX = map.worldToRootX(map.externalToMapX(300.0f));
            float forgeY = map.worldToRootY(map.externalToMapY(180.0f));
            draw.addLine(campX, campY, lakeX, lakeY, new MutableColor(0.92f, 0.75f, 0.35f, 0.9f), 2.0f);
            draw.addLine(lakeX, lakeY, forgeX, forgeY, new MutableColor(0.92f, 0.75f, 0.35f, 0.9f), 2.0f);
        });
        addMapMarker(map, status, "Camp", -320.0f, -160.0f);
        addMapMarker(map, status, "Lake", 20.0f, -40.0f);
        addMapMarker(map, status, "Forge", 300.0f, 180.0f);
        map.onViewportChanged(event -> status.text(String.format(Locale.ROOT,
                "Map view | zoom %.2fx | projected markers %d", event.newZoom(), map.anchorLayer().size())));

        VBox sample = new VBox();
        sample.spacing(10.0f);
        sample.layout(style -> style.widthPercent(100.0f));
        sample.addChild(status);
        sample.addChild(map);
        root.addChild(samplePanel("Projected map", "A map projection converts external coordinates to map coordinates; the grid, route and markers share the map coordinate system.", sample,
                "MapCanvas map = new MapCanvas()\n        .mapSize(1200, 720)\n        .projection(MapProjection.affine()\n                .worldPoint(-500, -300).mapPoint(0, 0)\n                .worldPoint(500, 300).mapPoint(1200, 720).build())\n        .gridSize(120).gridVisible(true);\nmap.addProjectedMarkerWidget(\"camp\", worldX, worldY, button);"));

        // MAP with shaders
        Label status2 = label("MapCanvas world layer: shader background + projected markers.",
                UniGuiDemo.GalleryTemplate.TEXT_DARK);
        MapCanvas map2 = new MapCanvas()
                .mapSize(1000.0f, 620.0f)
                .projection(MapProjection.affine()
                        .worldPoint(-500.0f, -310.0f).mapPoint(0.0f, 0.0f)
                        .worldPoint(500.0f, 310.0f).mapPoint(1000.0f, 620.0f)
                        .build())
                .viewport(45.0f, 28.0f, 0.55f)
                .zoomRange(0.2f, 1.6f)
                .gridVisible(false)
                .mapBorderVisible(false)
                .clampToMapBounds(true);
        map2.layout(style -> style.widthPercent(100.0f).height(320.0f));
        map2.backgroundColor().set(0.025f, 0.04f, 0.07f, 1.0f);

        map2.addWorldLayer((world, draw) -> {
            float x = map2.worldToRootX(0.0f);
            float y = map2.worldToRootY(0.0f);
            float width = map2.mapWidth() * map2.viewport().zoom();
            float height = map2.mapHeight() * map2.viewport().zoom();
            if (width <= 1.0f || height <= 1.0f) return;
            ShaderUniforms uniforms = ShaderUniforms.create()
                    .setFloat("Time", (System.nanoTime() % 120_000_000_000L) / 1_000_000_000.0f)
                    .setVec2("MapSize", map2.mapWidth(), map2.mapHeight());
            draw.addDrawCmd(DrawCommand.shader(MAP_SHADER, new MutableRect(x, y, width, height), uniforms)
                    .shaderOptions(MAP_SHADER_OPTIONS));
        });
        map2.addWorldLayer((world, draw) -> {
            float x = map2.worldToRootX(0.0f);
            float y = map2.worldToRootY(0.0f);
            float width = map2.mapWidth() * map2.viewport().zoom();
            float height = map2.mapHeight() * map2.viewport().zoom();
            for (float mx = 0.0f; mx <= map2.mapWidth(); mx += map2.gridSize()) {
                float gx = map2.worldToRootX(mx);
                draw.addLine(gx, y, gx, y + height, new MutableColor(0.49f, 0.77f, 0.82f, 0.18f), 1.0f);
            }
            for (float my = 0.0f; my <= map2.mapHeight(); my += map2.gridSize()) {
                float gy = map2.worldToRootY(my);
                draw.addLine(x, gy, x + width, gy, new MutableColor(0.49f, 0.77f, 0.82f, 0.18f), 1.0f);
            }
            float campX = map2.worldToRootX(map2.externalToMapX(-310.0f));
            float campY = map2.worldToRootY(map2.externalToMapY(-150.0f));
            float portX = map2.worldToRootX(map2.externalToMapX(20.0f));
            float portY = map2.worldToRootY(map2.externalToMapY(-20.0f));
            float peakX = map2.worldToRootX(map2.externalToMapX(300.0f));
            float peakY = map2.worldToRootY(map2.externalToMapY(170.0f));
            draw.addLine(campX, campY, portX, portY, new MutableColor(1.0f, 0.75f, 0.34f, 0.9f), 2.0f);
            draw.addLine(portX, portY, peakX, peakY, new MutableColor(1.0f, 0.75f, 0.34f, 0.9f), 2.0f);
        });
        addMarker(map2, status2, "Camp", -310.0f, -150.0f);
        addMarker(map2, status2, "Port", 20.0f, -20.0f);
        addMarker(map2, status2, "Peak", 300.0f, 170.0f);
        map2.onViewportChanged(event -> status2.text(String.format(Locale.ROOT,
                "Shader map | zoom %.2fx | projected markers %d", event.newZoom(), map2.anchorLayer().size())));

        VBox sample2 = new VBox();
        sample2.spacing(10.0f);
        sample2.layout(style -> style.widthPercent(100.0f));
        sample2.addChild(status2);
        sample2.addChild(map2);
        root.addChild(samplePanel("Procedural shader background", "A GLSL shader is drawn into a MapCanvas world layer. The grid, route and screen-sized markers are ordinary canvas layers above it.", sample2,
                "ShaderHandle shader = ShaderHandle.source(\"demo:map\", vertexSource, fragmentSource);\nmap.addWorldLayer((world, draw) -> {\n    ShaderUniforms uniforms = ShaderUniforms.create()\n            .setFloat(\"Time\", time)\n            .setVec2(\"MapSize\", map.mapWidth(), map.mapHeight());\n    draw.addDrawCmd(DrawCommand.shader(shader, mapBounds, uniforms)\n            .shaderOptions(ShaderDrawOptions.defaults()));\n});"));

        Label destinyReference = label(
                "For the shader source, see the mod code: CanvasCategory$BetterShader#MAP_DESTINY_BACKGROUND_SHADER.",
                UniGuiDemo.GalleryTemplate.TEXT_DARK);
        destinyReference.wrap(true);
        destinyReference.layout(style -> style.widthPercent(100.0f).height(34.0f));
        MapCanvas destinyMap = new MapCanvas()
                .mapSize(1000.0f, 620.0f)
                .projection(MapProjection.affine()
                        .worldPoint(-500.0f, -310.0f).mapPoint(0.0f, 0.0f)
                        .worldPoint(500.0f, 310.0f).mapPoint(1000.0f, 620.0f)
                        .build())
                .viewport(45.0f, 28.0f, 0.55f)
                .zoomRange(0.2f, 1.6f)
                .gridSize(100.0f)
                .gridVisible(true)
                .mapBorderVisible(true)
                .clampToMapBounds(true);
        destinyMap.layout(style -> style.widthPercent(100.0f).height(320.0f));
        destinyMap.backgroundColor().set(0.025f, 0.04f, 0.07f, 1.0f);
        destinyMap.gridColor().set(0.50f, 0.69f, 0.66f, 0.26f);
        destinyMap.borderColor().set(0.50f, 0.69f, 0.66f, 0.65f);
        BetterShader.addDestinyMapBackground(destinyMap);
        addMarker(destinyMap, destinyReference, "Camp", -310.0f, -150.0f);
        addMarker(destinyMap, destinyReference, "Port", 20.0f, -20.0f);
        addMarker(destinyMap, destinyReference, "Peak", 300.0f, 170.0f);
        VBox destinySample = new VBox();
        destinySample.spacing(10.0f);
        destinySample.layout(style -> style.widthPercent(100.0f));
        destinySample.addChild(destinyReference);
        destinySample.addChild(destinyMap);
        root.addChild(samplePanel("Destiny-style shader background", "MapCanvas can use a custom procedural shader as its map surface.", destinySample, ""));
    }

    private void addMapMarker(MapCanvas map, Label status, String title, float x, float y) {
        Button marker = new Button(title);
        marker.onClick(event -> status.text(String.format(Locale.ROOT,
                "%s at world %.0f, %.0f -> map %.0f, %.0f", title, x, y,
                map.externalToMapX(x), map.externalToMapY(y))));
        map.addProjectedMarkerWidget(title.toLowerCase(Locale.ROOT), x, y, marker)
                .screenSize(66.0f, 22.0f)
                .pivot(0.5f, 0.5f);
    }

    private static class BetterShader {

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
        private static final ShaderDrawOptions MAP_AURORA_BACKGROUND_OPTIONS = ShaderDrawOptions.defaults()
                .blend(true)
                .squareVertexOffset(0.0f);


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
    }
}

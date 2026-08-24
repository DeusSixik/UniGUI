package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyboardState;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.backend.minecraft.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft.MinecraftFonts;
import dev.sixik.unigui.backend.minecraft.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.world.WorldCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Тестовый экран мини-игры с навигацией корабля по сидированной 2D-системе.
 *
 * <p>Это намеренно client-only пример в TestMod: он показывает, как использовать
 * {@link WorldCanvas} как основу для игрового UI, а {@link KeyboardState} — для
 * удерживаемого WASD input внутри обычного UniGUI screen.</p>
 */
public final class SolarNavigationScreen {
    private SolarNavigationScreen() {
    }

    public static void openGui() {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService())
                .scaleProvider(new UnityLikeUIScaleProvider()
                        .referenceResolution(1920.0f, 1080.0f)
                        .matchBalanced()
                        .scaleRange(0.60f, 2.50f));

        SolarNavigationCanvas canvas = new SolarNavigationCanvas(0x5EED_51A7L);
        canvas.layout(style -> style
                .align(Alignment.STRETCH, Alignment.STRETCH)
                .flexGrow(1.0f)
                .flexShrink(1.0f));

        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(
                Component.literal("UniGUI Solar Navigation"),
                new OverlayLayer(canvas),
                context);
        screen.renderPolicy(UiRenderPolicy.vsync());
        screen.scaleWithMinecraftGui(false);
        Minecraft.getInstance().setScreen(screen);
    }

    /**
     * WorldCanvas-сцена: вся симуляция хранится в world-space, а UI-отрисовка
     * каждый кадр проецирует объекты в root-space через методы canvas'а.
     */
    private static final class SolarNavigationCanvas extends WorldCanvas {
        private static final float WORLD_HALF = 2400.0f;
        private static final float WORLD_SIZE = WORLD_HALF * 2.0f;
        private static final float SHIP_RADIUS = 18.0f;
        private static final float SHIP_NOSE = 28.0f;
        private static final float SHIP_TAIL = 17.0f;
        private static final float SHIP_HALF_WIDTH = 12.0f;
        private static final float TURN_SPEED = 2.65f;
        private static final float THRUST = 520.0f;
        private static final float REVERSE_THRUST = 300.0f;
        private static final float MAX_SPEED = 520.0f;
        private static final float CAMERA_ZOOM = 0.42f;

        private static final MutableColor BACKGROUND_TOP = MutableColor.fromHex("#050814");
        private static final MutableColor BACKGROUND_BOTTOM = MutableColor.fromHex("#10172A");
        private static final MutableColor GRID_MAJOR = MutableColor.rgba(0.20f, 0.45f, 0.65f, 0.16f);
        private static final MutableColor GRID_MINOR = MutableColor.rgba(0.15f, 0.32f, 0.48f, 0.075f);
        private static final MutableColor HUD_BG = MutableColor.rgba(0.015f, 0.025f, 0.045f, 0.78f);
        private static final MutableColor HUD_BORDER = MutableColor.rgba(0.28f, 0.58f, 0.72f, 0.65f);
        private static final MutableColor CYAN = MutableColor.fromHex("#60D8FF");
        private static final MutableColor CYAN_DIM = MutableColor.rgba(0.30f, 0.86f, 1.0f, 0.40f);
        private static final MutableColor AMBER = MutableColor.fromHex("#F7C45A");
        private static final MutableColor ORANGE = MutableColor.fromHex("#FF8A4C");
        private static final MutableColor RED = MutableColor.fromHex("#FF5F6F");
        private static final MutableColor WHITE = MutableColor.fromHex("#EAF7FF");
        private static final MutableColor MUTED = MutableColor.rgba(0.70f, 0.82f, 0.92f, 0.72f);
        private static final MutableColor ASTEROID = MutableColor.fromHex("#6C7482");
        private static final MutableColor ASTEROID_DARK = MutableColor.fromHex("#323945");
        private static final MutableColor STATION = MutableColor.fromHex("#8AE6FF");
        private static final MutableColor STATION_FILL = MutableColor.rgba(0.10f, 0.38f, 0.52f, 0.58f);
        private static final MutableColor STAR = MutableColor.rgba(0.88f, 0.96f, 1.0f, 0.55f);

        private final List<Asteroid> asteroids = new ArrayList<>();
        private final List<Station> stations = new ArrayList<>();
        private final List<Star> stars = new ArrayList<>();
        private final long seed;

        private Station questStation;
        private float shipX;
        private float shipY;
        private float velocityX;
        private float velocityY;
        private float angle = -0.45f;
        private float timeSeconds;
        private float dockMessageSeconds;
        private String dockMessage = "Find the quest station";

        private SolarNavigationCanvas(long seed) {
            this.seed = seed;
            preferredSize(1280.0f, 720.0f);
            viewport(0.0f, 0.0f, CAMERA_ZOOM);
            worldBounds(-WORLD_HALF, -WORLD_HALF, WORLD_SIZE, WORLD_SIZE);
            panningEnabled(false);
            zoomEnabled(false);
            wheelPanningEnabled(false);
            consumeWheelWhileHovered(false);
            addWorldLayer(this::renderScene);
            generateSystem();
        }

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            float delta = frame == null ? 1.0f / 60.0f : frame.deltaSeconds();
            delta = clamp(delta, 1.0f / 240.0f, 1.0f / 20.0f);
            timeSeconds += delta;

            updateInput(delta);
            updatePhysics(delta);
            updateCamera();

            if (dockMessageSeconds > 0.0f) {
                dockMessageSeconds = Math.max(0.0f, dockMessageSeconds - delta);
            }
            invalidate(InvalidationFlags.VISUAL);
        }

        private void generateSystem() {
            Random random = new Random(seed);
            for (int i = 0; i < 150; i++) {
                stars.add(new Star(
                        randomRange(random, -WORLD_HALF, WORLD_HALF),
                        randomRange(random, -WORLD_HALF, WORLD_HALF),
                        randomRange(random, 0.8f, 2.2f),
                        randomRange(random, 0.28f, 0.95f)));
            }

            for (int i = 0; i < 34; i++) {
                float x;
                float y;
                do {
                    x = randomRange(random, -WORLD_HALF + 160.0f, WORLD_HALF - 160.0f);
                    y = randomRange(random, -WORLD_HALF + 160.0f, WORLD_HALF - 160.0f);
                } while (distanceSquared(x, y, 0.0f, 0.0f) < 300.0f * 300.0f);

                asteroids.add(new Asteroid(
                        x,
                        y,
                        randomRange(random, 28.0f, 74.0f),
                        randomRange(random, 0.0f, (float) Math.PI * 2.0f),
                        9 + random.nextInt(6)));
            }

            String[] prefixes = {"Kappa", "Vega", "Astra", "Orion", "Helio", "Nova", "Rhea", "Ceres"};
            String[] suffixes = {"Gate", "Relay", "Port", "Array", "Dock", "Spire", "Hold", "Foundry"};
            for (int i = 0; i < 7; i++) {
                float angle = ((float) Math.PI * 2.0f / 7.0f) * i + randomRange(random, -0.22f, 0.22f);
                float radius = randomRange(random, 760.0f, 2050.0f);
                Station station = new Station(
                        (float) Math.cos(angle) * radius,
                        (float) Math.sin(angle) * radius,
                        prefixes[random.nextInt(prefixes.length)] + " " + suffixes[random.nextInt(suffixes.length)],
                        56.0f + random.nextFloat() * 18.0f,
                        i == 3);
                stations.add(station);
                if (station.quest()) {
                    questStation = station;
                }
            }
        }

        private void updateInput(float delta) {
            KeyboardState keyboard = uiContext() == null ? KeyboardState.NONE : uiContext().keyboard();
            if (keyboard.isDown(KeyCodes.A)) {
                angle -= TURN_SPEED * delta;
            }
            if (keyboard.isDown(KeyCodes.D)) {
                angle += TURN_SPEED * delta;
            }

            float directionX = (float) Math.cos(angle);
            float directionY = (float) Math.sin(angle);
            if (keyboard.isDown(KeyCodes.W)) {
                velocityX += directionX * THRUST * delta;
                velocityY += directionY * THRUST * delta;
            }
            if (keyboard.isDown(KeyCodes.S)) {
                velocityX -= directionX * REVERSE_THRUST * delta;
                velocityY -= directionY * REVERSE_THRUST * delta;
            }
            if (keyboard.wasPressed(KeyCodes.SPACE)) {
                tryDock();
            }
        }

        private void updatePhysics(float delta) {
            float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
            if (speed > MAX_SPEED) {
                float scale = MAX_SPEED / speed;
                velocityX *= scale;
                velocityY *= scale;
            }

            shipX += velocityX * delta;
            shipY += velocityY * delta;

            float damping = (float) Math.pow(0.74f, delta);
            velocityX *= damping;
            velocityY *= damping;

            resolveAsteroidCollisions();
            clampShipToSystem();
        }

        private void resolveAsteroidCollisions() {
            for (Asteroid asteroid : asteroids) {
                float minDistance = asteroid.radius() + SHIP_RADIUS;
                float dx = shipX - asteroid.x();
                float dy = shipY - asteroid.y();
                float distanceSq = dx * dx + dy * dy;
                if (distanceSq <= 0.001f || distanceSq >= minDistance * minDistance) continue;

                float distance = (float) Math.sqrt(distanceSq);
                float normalX = dx / distance;
                float normalY = dy / distance;
                float push = minDistance - distance;
                shipX += normalX * push;
                shipY += normalY * push;

                float dot = velocityX * normalX + velocityY * normalY;
                if (dot < 0.0f) {
                    velocityX -= normalX * dot * 1.55f;
                    velocityY -= normalY * dot * 1.55f;
                }
                dockMessage = "Hull impact: asteroid collision";
                dockMessageSeconds = 1.2f;
            }
        }

        private void clampShipToSystem() {
            if (shipX < -WORLD_HALF + SHIP_RADIUS) {
                shipX = -WORLD_HALF + SHIP_RADIUS;
                velocityX = Math.abs(velocityX) * 0.35f;
            } else if (shipX > WORLD_HALF - SHIP_RADIUS) {
                shipX = WORLD_HALF - SHIP_RADIUS;
                velocityX = -Math.abs(velocityX) * 0.35f;
            }
            if (shipY < -WORLD_HALF + SHIP_RADIUS) {
                shipY = -WORLD_HALF + SHIP_RADIUS;
                velocityY = Math.abs(velocityY) * 0.35f;
            } else if (shipY > WORLD_HALF - SHIP_RADIUS) {
                shipY = WORLD_HALF - SHIP_RADIUS;
                velocityY = -Math.abs(velocityY) * 0.35f;
            }
        }

        private void updateCamera() {
            RectView bounds = layoutBounds();
            if (bounds.width() <= 0.0f || bounds.height() <= 0.0f) return;
            viewport(bounds.width() * 0.5f - shipX * CAMERA_ZOOM,
                    bounds.height() * 0.5f - shipY * CAMERA_ZOOM,
                    CAMERA_ZOOM);
        }

        private void tryDock() {
            Station nearest = null;
            float nearestDistanceSq = Float.MAX_VALUE;
            for (Station station : stations) {
                float distanceSq = distanceSquared(shipX, shipY, station.x(), station.y());
                float dockRadius = station.radius() + 42.0f;
                if (distanceSq <= dockRadius * dockRadius && distanceSq < nearestDistanceSq) {
                    nearest = station;
                    nearestDistanceSq = distanceSq;
                }
            }

            if (nearest == null) {
                dockMessage = "No docking target in range";
                dockMessageSeconds = 1.5f;
                return;
            }

            dockMessage = nearest.quest()
                    ? "Docked: " + nearest.name() + " / quest route complete"
                    : "Docked: " + nearest.name();
            dockMessageSeconds = 3.0f;
            velocityX *= 0.25f;
            velocityY *= 0.25f;
        }

        private void renderScene(WorldCanvas canvas, DrawScope draw) {
            RectView b = canvas.layoutBounds();
            float x = b.x();
            float y = b.y();
            float w = b.width();
            float h = b.height();
            if (w <= 0.0f || h <= 0.0f) return;

            draw.addRectFilledMultiColor(x, y, w, h, BACKGROUND_TOP, BACKGROUND_TOP, BACKGROUND_BOTTOM, BACKGROUND_BOTTOM);
            drawGrid(canvas, draw, x, y, w, h);
            drawStars(canvas, draw);
            drawAsteroids(canvas, draw);
            drawStations(canvas, draw);
            drawShip(canvas, draw);
            drawQuestEdgeMarker(canvas, draw, x, y, w, h);
            drawHud(draw, x, y, w, h);
        }

        private void drawGrid(WorldCanvas canvas, DrawScope draw, float x, float y, float w, float h) {
            float minWorldX = canvas.rootToWorldX(x);
            float maxWorldX = canvas.rootToWorldX(x + w);
            float minWorldY = canvas.rootToWorldY(y);
            float maxWorldY = canvas.rootToWorldY(y + h);

            drawWorldGrid(canvas, draw, minWorldX, maxWorldX, minWorldY, maxWorldY, 100.0f, GRID_MINOR, 1.0f);
            drawWorldGrid(canvas, draw, minWorldX, maxWorldX, minWorldY, maxWorldY, 500.0f, GRID_MAJOR, 1.25f);
        }

        private void drawWorldGrid(WorldCanvas canvas, DrawScope draw,
                                   float minWorldX, float maxWorldX,
                                   float minWorldY, float maxWorldY,
                                   float step, MutableColor color, float thickness) {
            float firstX = (float) Math.floor(minWorldX / step) * step;
            for (float gx = firstX; gx <= maxWorldX; gx += step) {
                float rootX = canvas.worldToRootX(gx);
                draw.addLine(rootX, canvas.worldToRootY(minWorldY), rootX, canvas.worldToRootY(maxWorldY), color, thickness);
            }
            float firstY = (float) Math.floor(minWorldY / step) * step;
            for (float gy = firstY; gy <= maxWorldY; gy += step) {
                float rootY = canvas.worldToRootY(gy);
                draw.addLine(canvas.worldToRootX(minWorldX), rootY, canvas.worldToRootX(maxWorldX), rootY, color, thickness);
            }
        }

        private void drawStars(WorldCanvas canvas, DrawScope draw) {
            for (Star star : stars) {
                float sx = canvas.worldToRootX(star.x());
                float sy = canvas.worldToRootY(star.y());
                float alphaPulse = 0.65f + 0.35f * (float) Math.sin(timeSeconds * 1.7f + star.x() * 0.017f);
                MutableColor color = MutableColor.rgba(STAR.r(), STAR.g(), STAR.b(), STAR.a() * star.alpha() * alphaPulse);
                draw.addCircleFilled(sx, sy, star.size(), color, 8);
            }
        }

        private void drawAsteroids(WorldCanvas canvas, DrawScope draw) {
            for (Asteroid asteroid : asteroids) {
                float sx = canvas.worldToRootX(asteroid.x());
                float sy = canvas.worldToRootY(asteroid.y());
                float radius = asteroid.radius() * canvas.viewport().zoom();
                if (!visible(canvas.layoutBounds(), sx, sy, radius + 6.0f)) continue;

                draw.addCircleFilled(sx + 3.0f, sy + 4.0f, radius + 1.0f, MutableColor.rgba(0.0f, 0.0f, 0.0f, 0.28f), asteroid.segments());
                draw.addNgonFilled(sx, sy, radius, ASTEROID, asteroid.segments());
                draw.addNgon(sx, sy, radius, ASTEROID_DARK, asteroid.segments(), 2.0f);
                draw.addCircleFilled(sx - radius * 0.23f, sy - radius * 0.18f, Math.max(2.0f, radius * 0.18f), ASTEROID_DARK, 8);
            }
        }

        private void drawStations(WorldCanvas canvas, DrawScope draw) {
            for (Station station : stations) {
                float sx = canvas.worldToRootX(station.x());
                float sy = canvas.worldToRootY(station.y());
                float radius = station.radius() * canvas.viewport().zoom();
                if (!visible(canvas.layoutBounds(), sx, sy, radius + 80.0f)) continue;

                MutableColor ring = station.quest() ? AMBER : STATION;
                float pulse = station.quest() ? 1.0f + 0.10f * (float) Math.sin(timeSeconds * 4.0f) : 1.0f;
                float r = radius * pulse;
                draw.addCircleFilled(sx, sy, r * 0.65f, STATION_FILL, 24);
                draw.addCircle(sx, sy, r, ring, 32, station.quest() ? 2.5f : 1.5f);
                draw.addLine(sx - r * 1.15f, sy, sx + r * 1.15f, sy, ring, 1.4f);
                draw.addLine(sx, sy - r * 1.15f, sx, sy + r * 1.15f, ring, 1.4f);
                draw.addRectFilled(sx - r * 0.42f, sy - r * 0.42f, r * 0.84f, r * 0.84f, 4.0f, MutableColor.rgba(0.03f, 0.12f, 0.18f, 0.78f));
                draw.addRect(sx - r * 0.42f, sy - r * 0.42f, r * 0.84f, r * 0.84f, 4.0f, ring, 1.5f);

                text(draw, station.name(), sx - 120.0f, sy - r - 28.0f, 240.0f, 20.0f, 13.0f,
                        station.quest() ? AMBER : WHITE);
            }
        }

        private void drawShip(WorldCanvas canvas, DrawScope draw) {
            float dirX = (float) Math.cos(angle);
            float dirY = (float) Math.sin(angle);
            float sideX = -dirY;
            float sideY = dirX;

            float noseX = canvas.worldToRootX(shipX + dirX * SHIP_NOSE);
            float noseY = canvas.worldToRootY(shipY + dirY * SHIP_NOSE);
            float leftX = canvas.worldToRootX(shipX - dirX * SHIP_TAIL + sideX * SHIP_HALF_WIDTH);
            float leftY = canvas.worldToRootY(shipY - dirY * SHIP_TAIL + sideY * SHIP_HALF_WIDTH);
            float rightX = canvas.worldToRootX(shipX - dirX * SHIP_TAIL - sideX * SHIP_HALF_WIDTH);
            float rightY = canvas.worldToRootY(shipY - dirY * SHIP_TAIL - sideY * SHIP_HALF_WIDTH);
            float centerX = canvas.worldToRootX(shipX);
            float centerY = canvas.worldToRootY(shipY);

            float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
            if (speed > 18.0f) {
                float trailLength = clamp(speed * 0.12f, 18.0f, 74.0f);
                draw.addLine(centerX - dirX * 12.0f, centerY - dirY * 12.0f,
                        centerX - dirX * trailLength, centerY - dirY * trailLength,
                        MutableColor.rgba(0.38f, 0.86f, 1.0f, 0.35f), 3.0f);
            }

            draw.addTriangleFilled(new DrawPoint(noseX, noseY), new DrawPoint(leftX, leftY), new DrawPoint(rightX, rightY), CYAN);
            draw.addTriangle(new DrawPoint(noseX, noseY), new DrawPoint(leftX, leftY), new DrawPoint(rightX, rightY), WHITE, 1.25f);
            draw.addCircle(centerX, centerY, SHIP_RADIUS * 0.9f, CYAN_DIM, 28, 1.0f);
        }

        private void drawQuestEdgeMarker(WorldCanvas canvas, DrawScope draw, float x, float y, float w, float h) {
            if (questStation == null) return;
            float targetX = canvas.worldToRootX(questStation.x());
            float targetY = canvas.worldToRootY(questStation.y());
            float margin = 28.0f;
            if (targetX >= x + margin && targetX <= x + w - margin && targetY >= y + margin && targetY <= y + h - margin) {
                return;
            }

            float centerX = x + w * 0.5f;
            float centerY = y + h * 0.5f;
            float dx = targetX - centerX;
            float dy = targetY - centerY;
            float length = Math.max(0.001f, (float) Math.sqrt(dx * dx + dy * dy));
            dx /= length;
            dy /= length;

            float edgeX = centerX + dx * (w * 0.5f - margin);
            float edgeY = centerY + dy * (h * 0.5f - margin);
            edgeX = clamp(edgeX, x + margin, x + w - margin);
            edgeY = clamp(edgeY, y + margin, y + h - margin);

            float sideX = -dy;
            float sideY = dx;
            DrawPoint p1 = new DrawPoint(edgeX + dx * 16.0f, edgeY + dy * 16.0f);
            DrawPoint p2 = new DrawPoint(edgeX - dx * 12.0f + sideX * 10.0f, edgeY - dy * 12.0f + sideY * 10.0f);
            DrawPoint p3 = new DrawPoint(edgeX - dx * 12.0f - sideX * 10.0f, edgeY - dy * 12.0f - sideY * 10.0f);
            draw.addTriangleFilled(p1, p2, p3, AMBER);
            draw.addTriangle(p1, p2, p3, MutableColor.rgba(1.0f, 1.0f, 1.0f, 0.70f), 1.0f);

            float distance = (float) Math.sqrt(distanceSquared(shipX, shipY, questStation.x(), questStation.y()));
            text(draw, Math.round(distance) + "u", edgeX - 34.0f, edgeY + 18.0f, 68.0f, 18.0f, 12.0f, AMBER);
        }

        private void drawHud(DrawScope draw, float x, float y, float w, float h) {
            float panelX = x + 22.0f;
            float panelY = y + 18.0f;
            float panelW = 390.0f;
            float panelH = dockMessageSeconds > 0.0f ? 134.0f : 112.0f;
            draw.addRectFilled(panelX, panelY, panelW, panelH, 8.0f, HUD_BG);
            draw.addRect(panelX, panelY, panelW, panelH, 8.0f, HUD_BORDER, 1.25f);
            text(draw, "SOLAR NAVIGATION TERMINAL", panelX + 16.0f, panelY + 12.0f, panelW - 32.0f, 18.0f, 14.0f, CYAN);

            float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
            text(draw, "W/S thrust   A/D rotate   SPACE dock   ESC close", panelX + 16.0f, panelY + 38.0f, panelW - 32.0f, 18.0f, 11.0f, MUTED);
            text(draw, "POS " + Math.round(shipX) + ", " + Math.round(shipY)
                            + "    SPD " + Math.round(speed)
                            + "    SEED " + Long.toHexString(seed).toUpperCase(),
                    panelX + 16.0f, panelY + 62.0f, panelW - 32.0f, 18.0f, 11.0f, WHITE);

            if (questStation != null) {
                float questDistance = (float) Math.sqrt(distanceSquared(shipX, shipY, questStation.x(), questStation.y()));
                text(draw, "QUEST: dock with " + questStation.name() + " / " + Math.round(questDistance) + "u",
                        panelX + 16.0f, panelY + 84.0f, panelW - 32.0f, 18.0f, 11.0f, AMBER);
            }
            if (dockMessageSeconds > 0.0f) {
                text(draw, dockMessage, panelX + 16.0f, panelY + 108.0f, panelW - 32.0f, 18.0f, 12.0f,
                        dockMessage.contains("impact") || dockMessage.contains("No ") ? RED : ORANGE);
            }

            float radarSize = Math.min(170.0f, Math.max(120.0f, Math.min(w, h) * 0.18f));
            float radarX = x + w - radarSize - 26.0f;
            float radarY = y + 22.0f;
            drawRadar(draw, radarX, radarY, radarSize);
        }

        private void drawRadar(DrawScope draw, float x, float y, float size) {
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float radius = size * 0.5f;
            draw.addCircleFilled(cx, cy, radius, MutableColor.rgba(0.02f, 0.04f, 0.07f, 0.68f), 40);
            draw.addCircle(cx, cy, radius, HUD_BORDER, 40, 1.1f);
            draw.addCircle(cx, cy, radius * 0.55f, MutableColor.rgba(0.28f, 0.58f, 0.72f, 0.25f), 36, 1.0f);
            draw.addLine(cx - radius, cy, cx + radius, cy, MutableColor.rgba(0.28f, 0.58f, 0.72f, 0.22f), 1.0f);
            draw.addLine(cx, cy - radius, cx, cy + radius, MutableColor.rgba(0.28f, 0.58f, 0.72f, 0.22f), 1.0f);
            draw.addCircleFilled(cx, cy, 3.5f, CYAN, 12);

            float scale = radius / WORLD_HALF;
            for (Station station : stations) {
                float sx = cx + (station.x() - shipX) * scale;
                float sy = cy + (station.y() - shipY) * scale;
                if (distanceSquared(sx, sy, cx, cy) > radius * radius) continue;
                draw.addCircleFilled(sx, sy, station.quest() ? 4.0f : 2.7f, station.quest() ? AMBER : STATION, 12);
            }
        }

        private void text(DrawScope draw, String value, float x, float y, float width, float height,
                          float size, MutableColor color) {
            RichText text = RichText.of(value, MinecraftFonts.defaultFace(), size, color);
            draw.addText(text, x, y, width, height, color);
        }

        private static boolean visible(RectView bounds, float x, float y, float radius) {
            return x + radius >= bounds.x()
                    && x - radius <= bounds.x() + bounds.width()
                    && y + radius >= bounds.y()
                    && y - radius <= bounds.y() + bounds.height();
        }

        private static float randomRange(Random random, float min, float max) {
            return min + random.nextFloat() * (max - min);
        }

        private static float distanceSquared(float ax, float ay, float bx, float by) {
            float dx = ax - bx;
            float dy = ay - by;
            return dx * dx + dy * dy;
        }

        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }

        private record Asteroid(float x, float y, float radius, float rotation, int segments) {
        }

        private record Station(float x, float y, String name, float radius, boolean quest) {
        }

        private record Star(float x, float y, float size, float alpha) {
        }
    }
}

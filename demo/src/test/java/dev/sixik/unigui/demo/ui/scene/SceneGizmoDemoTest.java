package dev.sixik.unigui.demo.ui.scene;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.input.InputHandle;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.scene.BlockSpec;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.ScenePlatform;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.ToggleButton;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Проверяет инструменты сцены через публичные маршруты ввода виджетов. */
class SceneGizmoDemoTest {
    /** Допуск учитывает несколько последовательных операций с float в проекции и преобразованиях. */
    private static final float EPSILON = 5e-5f;

    /** Снимает тестовую сцену-платформу после каждого запуска. */
    @AfterEach
    void clearPlatform() {
        ScenePlatform.install(null);
    }

    /** Проверяет выбор блока кликом и перенос каждой из трёх осей указателем. */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void move_dragAxisOnSelectedBlock_updatesOnlyThatCoordinate(int axis) {
        var fixture = layout();
        SceneNode log = fixture.viewport.scene().roots().get(2);
        MutableVec3 initial = log.position(new MutableVec3());
        MutableVec3 point = project(fixture.viewport.camera(), initial);
        fixture.clickScene(point.x(), point.y(), PointerButton.PRIMARY);

        MutableVec3 direction = new MutableVec3(axis == 0 ? 1 : 0, axis == 1 ? 1 : 0, axis == 2 ? 1 : 0);
        Segment handle = linearHandle(fixture.viewport.camera(), initial, direction);
        fixture.pressScene(handle.b.x(), handle.b.y(), PointerButton.PRIMARY);
        assertSame(fixture.viewport, fixture.context.capturedPointer(0));
        fixture.releaseScene(handle.b.x() + 0.5f * handle.dx(), handle.b.y() + 0.5f * handle.dy(),
                PointerButton.PRIMARY);
        assertNull(fixture.context.capturedPointer(0));

        float delta = handle.worldRadius * 0.5f;
        assertVector(initial.add(delta * direction.x(), delta * direction.y(), delta * direction.z()),
                log.position(new MutableVec3()));
    }

    /** Проверяет кольцо Z при взгляде вдоль этой оси и освобождение указателя вне сцены. */
    @Test
    void rotate_dragRing_rotatesAboutWorldAxisAndReleasesOutside() {
        var fixture = layout();
        SceneNode block = fixture.viewport.scene().roots().get(1);
        MutableVec3 pivot = block.position(new MutableVec3());
        var camera = fixture.viewport.camera();
        fixture.viewport.orbit().setOrbit(pivot, 10, 0, 0);
        fixture.tool(1).click();
        assertFalse(fixture.tool(0).checked());
        assertTrue(fixture.tool(1).checked());
        fixture.tool(1).click();
        assertTrue(fixture.tool(1).checked());

        float radius = linearHandle(camera, pivot, new MutableVec3(1, 0, 0)).worldRadius * 1.24f;
        MutableVec3 a = ringPoint(camera, pivot, radius, Math.PI / 4);
        MutableVec3 b = ringPoint(camera, pivot, radius, 3 * Math.PI / 10);
        Segment segment = new Segment(a, b, radius);
        float x = (a.x() + b.x()) * 0.5f, y = (a.y() + b.y()) * 0.5f;
        float multiplier = (float) (31 / Math.hypot(segment.dx(), segment.dy()));
        fixture.pressScene(x, y, PointerButton.PRIMARY);
        fixture.moveScene(x + multiplier * segment.dx(), y + multiplier * segment.dy());
        var expected = new MutableVec3((float) Math.cos(0.5), (float) Math.sin(0.5), 0);
        assertVector(expected, block.rotation(new MutableQuat()).rotate(1, 0, 0, new MutableVec3()));
        fixture.releaseScene(-40, -40, PointerButton.PRIMARY);
        assertNull(fixture.context.capturedPointer(0));
    }

    /** Проверяет локальную ось уже повёрнутого блока, предел масштаба и перехват захвата. */
    @Test
    void scale_dragRotatedHandle_changesLocalScaleAndStopsAfterCaptureLoss() {
        var fixture = layout();
        SceneNode block = fixture.viewport.scene().roots().get(1);
        var rotation = new MutableQuat().setYawPitch(Math.PI * 0.5, 0);
        block.setRotation(rotation);
        fixture.tool(2).click();
        var handle = linearHandle(fixture.viewport.camera(), block.position(new MutableVec3()),
                rotation.rotate(1, 0, 0, new MutableVec3()));
        fixture.pressScene(handle.b.x(), handle.b.y(), PointerButton.PRIMARY);
        fixture.moveScene(handle.b.x() + handle.dx(), handle.b.y() + handle.dy());
        assertVector(new MutableVec3(2, 1, 1), block.scale(new MutableVec3()));
        fixture.moveScene(handle.b.x() - 3 * handle.dx(), handle.b.y() - 3 * handle.dy());
        assertVector(new MutableVec3(0.15f, 1, 1), block.scale(new MutableVec3()));

        fixture.context.clearPointerCapture(0);
        fixture.demo.tick(null);
        fixture.moveScene(handle.b.x() + handle.dx(), handle.b.y() + handle.dy());
        assertVector(new MutableVec3(0.15f, 1, 1), block.scale(new MutableVec3()));
    }

    /** Проверяет режим камеры, непрерывное движение, фокус и возврат на орбиту. */
    @Test
    void flyCamera_respectsFocusAndRestoresOrbitWhenUnchecked() {
        var fixture = layout();
        var camera = fixture.viewport.camera();
        var orbitPosition = camera.position(new MutableVec3());
        var facing = camera.orientation(new MutableQuat());
        assertTrue(fixture.viewport.cameraControls());
        fixture.toggleFly();
        assertFalse(fixture.viewport.cameraControls());
        assertSame(fixture.viewport, fixture.context.focusManager().focusedWidget());

        var displacement = facing.rotate(0, 0, -0.5f, new MutableVec3());
        assertTrue(fixture.input.keyPressed(KeyCodes.W, 0, 0));
        fixture.tick();
        fixture.tick();
        fixture.input.keyReleased(KeyCodes.W, 0, 0);
        MutableVec3 traveled = new MutableVec3().set(orbitPosition)
                .add(2 * displacement.x(), 2 * displacement.y(), 2 * displacement.z());
        assertVector(traveled, camera.position(new MutableVec3()));

        fixture.pressScene(100, 100, PointerButton.SECONDARY);
        fixture.moveScene(140, 100);
        fixture.releaseScene(140, 100, PointerButton.SECONDARY);
        assertNull(fixture.context.capturedPointer(0));
        var turned = camera.orientation(new MutableQuat());
        // A world yaw rotates the horizontal components while preserving the downward pitch.
        var worldYaw = new MutableQuat().setYawPitch(-0.24, 0);
        var previousForward = facing.rotate(0, 0, -1, new MutableVec3());
        var expectedForward = worldYaw.rotate(previousForward.x(), previousForward.y(), previousForward.z(),
                new MutableVec3());
        assertVector(expectedForward, turned.rotate(0, 0, -1, new MutableVec3()));

        fixture.context.focusManager().clearFocus();
        fixture.input.keyPressed(KeyCodes.W, 0, 0);
        fixture.tick();
        assertVector(traveled, camera.position(new MutableVec3()));
        fixture.input.keyReleased(KeyCodes.W, 0, 0);
        fixture.toggleFly();
        assertTrue(fixture.viewport.cameraControls());
        assertVector(orbitPosition, camera.position(new MutableVec3()));
    }

    /** Проверяет, что стрелки и кольца пишутся в 2D-слой Scene3D. */
    @Test
    void painter_selectedBlock_drawsProjectedHandles() {
        var fixture = layout();
        var drawList = new DrawList();
        fixture.viewport.painter().paint(new DefaultRenderContext(drawList), 600, 340);
        assertEquals(6, drawList.commands().stream().filter(cmd -> cmd.type() == DrawCommandType.LINE).count());
        drawList.clear();
        fixture.tool(1).click();
        fixture.viewport.painter().paint(new DefaultRenderContext(drawList), 600, 340);
        assertTrue(drawList.commands().stream().filter(cmd -> cmd.type() == DrawCommandType.LINE).count() > 90);
    }

    /**
     * Собирает демо с раскладкой и реальным маршрутизатором ввода.
     *
     * @return готовая тестовая сцена.
     */
    private static Fixture layout() {
        installPlatform();
        var demo = new SceneGizmoDemo(SceneShowcase.Blocks.MODERN);
        var context = new DefaultUIContext();
        demo.setUiContextInternal(context);
        demo.measure(new LayoutContext(600, 500));
        demo.arrange(new MutableRect(0, 0, 600, 500));
        var viewport = (Scene3D) demo.children().get(0).children().get(1);
        return new Fixture(demo, context, viewport, new InputHandle(demo, context));
    }

    /** Подставляет строковые состояния блоков для headless picking. */
    private static void installPlatform() {
        ScenePlatform.install(new ScenePlatform() {
            @Override
            public Object resolveBlock(BlockSpec spec) {
                return spec.text();
            }

            @Override
            public Object nativeBlock(Object block, int meta) {
                return block;
            }
        });
    }

    /** Проецирует мировую точку в вьюпорт теста. */
    private static MutableVec3 project(Camera3D camera, MutableVec3 world) {
        var screen = new MutableVec3();
        assertTrue(camera.project(world, screen));
        return screen;
    }

    /** Вычисляет экранные концы оси по заданной точке опоры и мировому направлению. */
    private static Segment linearHandle(Camera3D camera, MutableVec3 pivot, MutableVec3 axis) {
        MutableVec3 a = project(camera, pivot);
        var offset = new MutableVec3();
        assertTrue(camera.unproject(a.x() + 62, a.y(), a.z(), offset));
        float radius = (float) Math.sqrt(Math.pow(offset.x() - pivot.x(), 2)
                + Math.pow(offset.y() - pivot.y(), 2) + Math.pow(offset.z() - pivot.z(), 2));
        var endpoint = new MutableVec3().set(pivot).add(radius * axis.x(), radius * axis.y(), radius * axis.z());
        return new Segment(a, project(camera, endpoint), radius);
    }

    /** Проецирует точку кольца Z с заданным углом в радианах. */
    private static MutableVec3 ringPoint(Camera3D camera, MutableVec3 pivot, float radius, double angle) {
        return project(camera, new MutableVec3().set(pivot)
                .add(radius * (float) Math.cos(angle), radius * (float) Math.sin(angle), 0));
    }

    /** Сравнивает три компоненты векторов в пределах погрешности float. */
    private static void assertVector(MutableVec3 expected, MutableVec3 actual) {
        assertArrayEquals(new float[]{expected.x(), expected.y(), expected.z()},
                new float[]{actual.x(), actual.y(), actual.z()}, EPSILON);
    }

    /** Отрезок на холсте с соответствующим радиусом гизмо в блоках. */
    private record Segment(MutableVec3 a, MutableVec3 b, float worldRadius) {
        /** @return горизонтальное смещение в единицах UI. */
        float dx() {
            return b.x() - a.x();
        }

        /** @return вертикальное смещение в единицах UI. */
        float dy() {
            return b.y() - a.y();
        }
    }

    /** Демо, строитель событий и вьюпорт с раскладкой. */
    private record Fixture(SceneGizmoDemo demo, DefaultUIContext context, Scene3D viewport, InputHandle input) {
        /** Возвращает кнопку инструмента по индексу в тулбаре. */
        ToggleButton tool(int index) {
            return (ToggleButton) viewport.parent().children().get(0).children().get(index);
        }

        /** Переключает флажок кликом с реальным фокусом. */
        void toggleFly() {
            var checkbox = (Checkbox) tool(0).parent().children().get(3);
            RectView bounds = checkbox.layoutBounds();
            float x = bounds.x() + bounds.width() / 2, y = bounds.y() + bounds.height() / 2;
            input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
            input.pointerReleased(x, y, 0, PointerButton.PRIMARY);
        }

        /** Передаёт кадр длиной 100 мс. */
        void tick() {
            demo.tick(new FrameContext(0, 0.1f, 0, FramePhase.ANIMATION));
        }

        /** Кликает по точке вьюпорта в его локальных координатах. */
        void clickScene(float x, float y, PointerButton button) {
            pressScene(x, y, button);
            releaseScene(x, y, button);
        }

        /** Нажимает кнопку по локальным координатам вьюпорта. */
        void pressScene(float x, float y, PointerButton button) {
            input.pointerPressed(viewport.layoutBounds().x() + x, viewport.layoutBounds().y() + y, 0, button);
        }

        /** Двигает указатель по локальным координатам вьюпорта. */
        void moveScene(float x, float y) {
            input.pointerMoved(viewport.layoutBounds().x() + x, viewport.layoutBounds().y() + y, 0);
        }

        /** Отпускает кнопку по локальным координатам вьюпорта. */
        void releaseScene(float x, float y, PointerButton button) {
            input.pointerReleased(viewport.layoutBounds().x() + x, viewport.layoutBounds().y() + y, 0, button);
        }
    }
}

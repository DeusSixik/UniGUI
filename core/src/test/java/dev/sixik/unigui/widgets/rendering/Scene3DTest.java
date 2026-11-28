package dev.sixik.unigui.widgets.rendering;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.input.InputHandle;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;
import dev.sixik.unigui.api.math.Vec3View;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.Scene3DRenderer;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.SceneTimeline;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.render.SimpleRenderTarget;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.interaction.Button;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class Scene3DTest {
    // Допуск учитывает вычисление UV и координат раскладки в float.
    private static final float EPS = 1e-4f;
    private static final FrameContext FRAME = new FrameContext(1, 0.016f, 0.375f, FramePhase.ANIMATION);
    private static final Aabb UNIT_BOX = new Aabb(-1, -1, -1, 1, 1, 1);
    /** Треугольник подсветки с вершинами в локальных единицах сцены. */
    private static final Mesh3D HOVER_TRIANGLE = new Mesh3D(
            new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0}, null, null, null, new int[]{0, 1, 2});
    /** Полупрозрачный зелёный материал подсветки с ARGB-цветом. */
    private static final Material HOVER_MATERIAL = new Material(0x8044FF88, null, Material.Shading.UNLIT,
            Material.AlphaMode.TRANSLUCENT, 0, Material.Culling.NONE);

    @Test
    void render_sceneAndPainter_shareOneDepthTargetWithNoSecondClear() {
        Harness h = new Harness();
        h.scene.clearColor().set(0.1f, 0.2f, 0.3f, 0.4f);
        Paint fill = Paint.fill(new MutableColor(1, 1, 1, 1));
        h.scene.painter((context, width, height) -> {
            h.backend.passes.add("painter");
            context.rect(2, 3, 4, 5, fill);
        });
        h.scene.targetOptions(new RenderTargetOptions(false, true, "custom_scene"));
        h.context.pixelScale(2);
        h.scene.tick(FRAME);

        h.render();

        assertEquals(List.of("3D", "painter", "2D"), h.backend.passes);
        assertTrue(h.backend.options.useDepth());
        assertFalse(h.backend.options.clearBeforeRender());
        assertEquals("custom_scene", h.backend.options.debugName());
        assertSame(h.backend.rawTarget, h.backend.overlayTarget);
        assertSame(h.backend.rawTarget.colorTexture().nativeHandle(), h.scene.texture().nativeHandle());
        assertEquals(400.0f, h.backend.viewportWidth, EPS);
        assertEquals(240.0f, h.backend.viewportHeight, EPS);
        assertEquals(0.375f, h.backend.partialTick, EPS);
        assertEquals(0.4f, h.backend.clearAlpha, EPS);
        assertEquals(1, h.backend.overlayCommands.size());
        assertEquals(2.0f, h.backend.overlayCommands.get(0).bounds().x(), EPS);
        assertEquals(DrawCommandType.TEXTURE, h.screen.commands().get(0).type());
    }

    /** Проверяет удвоение плотности выборок с линейной фильтрацией при выводе. */
    @Test
    void resolutionScale_atTwo_rendersFourSamplesPerPhysicalPixel() {
        Harness h = new Harness();
        h.context.pixelScale(2);
        h.scene.resolutionScale(2);

        h.render();

        assertEquals(800, h.backend.rawTarget.width());
        assertEquals(480, h.backend.rawTarget.height());
        assertEquals(800, h.backend.viewportWidth, EPS);
        assertEquals(480, h.backend.viewportHeight, EPS);
        assertEquals(TextureFilter.LINEAR, h.scene.texture().options().minFilter());
        assertEquals(200, h.screen.commands().get(0).bounds().width(), EPS);
    }

    @Test
    void onChange_sceneAndCameraUpdates_invalidateCachedTexture() {
        Harness h = new Harness();
        h.render();
        h.render();
        assertEquals(1, h.backend.rawCalls);

        h.scene.clearInvalidation(InvalidationFlags.ALL);
        h.node.setVisible(false);
        h.scene.tick(FRAME);
        assertTrue(InvalidationFlags.has(h.scene.invalidationFlags(), InvalidationFlags.TEXTURE));
        h.render();
        h.render();
        assertEquals(2, h.backend.rawCalls);

        h.scene.camera().setPosition(1, 0, 5);
        h.render();
        h.render();
        assertEquals(3, h.backend.rawCalls);
        assertSame(h.node.scene(), h.backend.lastScene);
        assertSame(h.scene.camera(), h.backend.lastCamera);
        assertEquals(1, h.backend.targetCreations);
    }

    @Test
    void everyFrame_runsRawPassForEachFrameAndPassesUpdatedPartialTick() {
        Harness h = new Harness();
        h.scene.updateMode(RenderToTexture.UpdateMode.EVERY_FRAME);

        h.scene.tick(FRAME);
        h.render();
        h.scene.tick(new FrameContext(2, 0.016f, 0.75f, FramePhase.ANIMATION));
        h.render();

        assertEquals(2, h.backend.rawCalls);
        assertEquals(0.75f, h.backend.partialTick, EPS);
        assertEquals(1, h.backend.targetCreations);
    }

    @Test
    void unsupportedBackend_reportsMissingSceneRenderer() {
        Harness h = new Harness();
        h.backend.supports3D = false;
        assertThrows(UnsupportedOperationException.class, h::render);
        assertTrue(h.backend.passes.isEmpty());
        assertEquals(0, h.scene.renderCount());
    }

    @Test
    void failedOverlay_keepsFramePendingForRetry() {
        Harness h = new Harness();
        h.backend.failOverlay = true;
        assertThrows(IllegalStateException.class, h::render);
        assertEquals(0, h.scene.renderCount());

        h.backend.failOverlay = false;
        h.render();
        h.render();
        assertEquals(1, h.scene.renderCount());
        assertEquals(2, h.backend.rawCalls);
        assertEquals(1, h.backend.targetCreations);
    }

    @Test
    void pointerRoutes_hoverClickAndExit_useNearestNode() {
        Harness h = new Harness();
        SceneNode nearer = new SceneNode().setBounds(UNIT_BOX).setPosition(0, 0, 2);
        h.scene.scene().addRoot(nearer);
        List<Scene3D.NodeHoverEvent> hover = new ArrayList<>();
        List<Scene3D.NodeClickEvent> click = new ArrayList<>();
        h.scene.onNodeHover(hover::add);
        h.scene.onNodeClick(click::add);

        h.input.pointerMoved(130, 80, 0);
        assertSame(nearer, h.scene.hoveredNode());
        assertEquals(MouseCursor.POINTER, h.scene.mouseCursorAt(100, 60));
        h.input.pointerPressed(130, 80, 0, PointerButton.PRIMARY);
        assertSame(h.scene, h.ui.capturedPointer(0));
        h.input.pointerReleased(130, 80, 0, PointerButton.PRIMARY);
        assertNull(h.ui.capturedPointer(0));
        assertEquals(1, click.size());
        assertSame(nearer, click.get(0).node());
        assertEquals(PointerButton.PRIMARY, click.get(0).pointer().button());

        nearer.setVisible(false);
        h.scene.tick(FRAME);
        assertSame(h.node, h.scene.hoveredNode());
        h.input.pointerMoved(29, 80, 0);
        assertNull(h.scene.hoveredNode());
        assertEquals(3, hover.size());
        assertSame(nearer, hover.get(0).node());
        assertSame(nearer, hover.get(1).previous());
        assertSame(h.node, hover.get(1).node());
        assertNull(hover.get(2).node());
    }

    /** Проверяет независимую подсветку viewport и обновление при переходе внутри одного узла. */
    @Test
    void highlight_onMovingWithinSameNode_updatesWithoutMutatingScene() {
        Harness h = new Harness();
        h.scene.highlightBlocks(true);
        var cells = new SceneNode() {
            /** {@inheritDoc} */
            @Override
            protected double intersectPickBounds(Ray ray, double entry, double limit, MutableVec3 cellOrigin) {
                ray.pointAt(entry, cellOrigin);
                cellOrigin.set(cellOrigin.x() < 0 ? -1 : 0, 0, 0);
                return entry;
            }
        }.setBounds(UNIT_BOX).setPosition(0, 0, 2);
        h.scene.scene().addRoot(cells);
        long sceneRevision = h.scene.scene().revision();
        h.input.pointerMoved(124, 80, 0);
        h.render();
        assertSame(cells, h.scene.hoveredNode());
        assertNotNull(h.backend.highlight);
        assertEquals(-1, h.backend.highlight.world().get(0, 3), 0);
        assertEquals(2, h.backend.highlight.world().get(2, 3), 0);

        h.input.pointerMoved(136, 80, 0);
        h.render();
        assertSame(cells, h.scene.hoveredNode());
        assertEquals(0, h.backend.highlight.world().get(0, 3), 0);
        assertEquals(sceneRevision, h.scene.scene().revision());
        h.input.pointerMoved(29, 80, 0);
        h.render();
        assertNull(h.backend.highlight);
    }

    /** Проверяет отдельное включение подсветки ячеек и рамки с трансформацией объекта. */
    @Test
    void highlightObjects_whenHoverMovesBetweenObjectAndBlock_usesTheEnabledHighlight() {
        Harness h = new Harness();
        h.scene.camera().setOrthographic(6, 0.1f, 20);
        h.node.setBounds(new Aabb(-0.25f, -0.5f, -0.125f, 0.25f, 0.5f, 0.125f))
                .setPosition(1.5f, 0, 0).setScale(2, 1, 1.5f);
        var block = new SceneNode() {
            /** {@inheritDoc} */
            @Override
            protected double intersectPickBounds(Ray ray, double entry, double limit, MutableVec3 cellOrigin) {
                cellOrigin.set(0, 0, 0);
                return entry;
            }
        }.setBounds(UNIT_BOX).setPosition(-1.5f, 0, 0);
        h.scene.scene().addRoot(block);
        long revision = h.scene.scene().revision();

        h.input.pointerMoved(160, 80, 0);
        h.render();
        assertSame(h.node, h.scene.hoveredNode());
        assertNull(h.backend.highlight);

        h.scene.highlightObjects(true);
        h.render();
        var frame = h.backend.highlight;
        assertNotNull(frame);
        assertEquals(0.92, frame.world().get(0, 3), EPS);
        assertEquals(-0.54, frame.world().get(1, 3), EPS);
        assertEquals(1.16, frame.world().get(0, 0), EPS);
        assertEquals(1.08, frame.world().get(1, 1), EPS);
        assertEquals(0.495, frame.world().get(2, 2), EPS);

        h.input.pointerMoved(100, 80, 0);
        h.render();
        assertSame(block, h.scene.hoveredNode());
        assertNull(h.backend.highlight);

        h.scene.highlightBlocks(true);
        h.render();
        assertNotNull(h.backend.highlight);
        assertNotSame(frame.mesh(), h.backend.highlight.mesh());
        h.scene.highlightObjects(false);
        h.render();
        assertNotNull(h.backend.highlight);

        h.scene.highlightObjects(true);
        h.input.pointerMoved(160, 80, 0);
        h.render();
        assertSame(frame, h.backend.highlight);
        h.scene.highlightBlocks(false);
        h.render();
        assertSame(frame, h.backend.highlight);
        assertEquals(revision, h.scene.scene().revision());

        h.input.pointerMoved(29, 80, 0);
        h.render();
        assertNull(h.backend.highlight);
    }

    /** Проверяет пользовательскую геометрию, иерархию и возврат к рамке без движения указателя. */
    @Test
    void hoverHighlight_customObject_usesNodeTransformAndRefreshesWhenInvalidated() {
        Harness h = new Harness();
        h.scene.updateMode(RenderToTexture.UpdateMode.ON_CHANGE);
        h.scene.highlightObjects(true);
        var node = new SceneNode() {
            /** Текущая подсветка узла или null для стандартной рамки. */
            HoverHighlight style = new HoverHighlight(HOVER_TRIANGLE, HOVER_MATERIAL);

            /** {@inheritDoc} */
            @Override
            public HoverHighlight hoverHighlight(Vec3View cellOrigin) {
                assertNull(cellOrigin);
                return style;
            }
        };
        node.setBounds(UNIT_BOX).setPosition(-0.125f, 0, 0.5f).setScale(0.5f, 2, 1);
        SceneNode parent = new SceneNode().setPosition(0.25f, 0, 1.5f).setScale(2, 1.5f, 1);
        parent.addChild(node);
        h.scene.scene().addRoot(parent);

        h.input.pointerMoved(130, 80, 0);
        h.render();
        assertSame(node, h.scene.hoveredNode());
        var custom = h.backend.highlight;
        assertNotNull(custom);
        assertSame(HOVER_TRIANGLE, custom.mesh());
        assertSame(HOVER_MATERIAL, custom.material());
        assertEquals(0, custom.world().get(0, 3), EPS);
        assertEquals(2, custom.world().get(2, 3), EPS);
        assertEquals(1, custom.world().get(0, 0), EPS);
        assertEquals(3, custom.world().get(1, 1), EPS);

        node.style = new SceneNode.HoverHighlight(HOVER_TRIANGLE, Material.DEFAULT);
        node.invalidateContent();
        h.render();
        assertSame(HOVER_TRIANGLE, h.backend.highlight.mesh());
        assertSame(Material.DEFAULT, h.backend.highlight.material());

        node.style = null;
        node.invalidateContent();
        h.render();
        assertNotNull(h.backend.highlight);
        assertNotSame(HOVER_TRIANGLE, h.backend.highlight.mesh());

        h.scene.highlightObjects(false);
        h.render();
        assertSame(node, h.scene.hoveredNode());
        assertNull(h.backend.highlight);
    }

    /** Проверяет материальный вариант для выбранной ячейки и перенос меша к её началу. */
    @Test
    void hoverHighlight_customCell_usesCellOriginAndBlockToggle() {
        Harness h = new Harness();
        h.scene.highlightObjects(true);
        var left = new SceneNode.HoverHighlight(HOVER_TRIANGLE, HOVER_MATERIAL);
        var right = new SceneNode.HoverHighlight(HOVER_TRIANGLE, Material.DEFAULT);
        var node = new SceneNode() {
            /** {@inheritDoc} */
            @Override
            protected double intersectPickBounds(Ray ray, double entry, double limit, MutableVec3 cellOrigin) {
                ray.pointAt(entry, cellOrigin);
                cellOrigin.set(cellOrigin.x() < 0 ? -1 : 0, 0, 0);
                return entry;
            }

            /** {@inheritDoc} */
            @Override
            public HoverHighlight hoverHighlight(Vec3View cellOrigin) {
                return cellOrigin.x() < 0 ? left : right;
            }
        }.setBounds(UNIT_BOX).setPosition(0, 0, 2);
        h.scene.scene().addRoot(node);

        h.input.pointerMoved(124, 80, 0);
        h.render();
        assertSame(node, h.scene.hoveredNode());
        assertNull(h.backend.highlight);

        h.scene.highlightBlocks(true);
        h.render();
        assertSame(HOVER_TRIANGLE, h.backend.highlight.mesh());
        assertSame(HOVER_MATERIAL, h.backend.highlight.material());
        assertEquals(-1, h.backend.highlight.world().get(0, 3), EPS);
        assertEquals(2, h.backend.highlight.world().get(2, 3), EPS);

        h.input.pointerMoved(136, 80, 0);
        h.render();
        assertSame(Material.DEFAULT, h.backend.highlight.material());
        assertEquals(0, h.backend.highlight.world().get(0, 3), EPS);

        h.input.pointerMoved(29, 80, 0);
        h.render();
        assertNull(h.backend.highlight);
    }

    @Test
    void drag_capturesPointerOutsideViewportWithoutClick_andSupportsPanAndZoom() {
        Harness h = new Harness();
        AtomicInteger clicks = new AtomicInteger();
        h.scene.onNodeClick(event -> clicks.incrementAndGet());
        long startRevision = h.scene.camera().revision();

        h.input.pointerPressed(130, 80, 0, PointerButton.PRIMARY);
        h.input.pointerMoved(250, 55, 0);
        assertSame(h.scene, h.ui.capturedPointer(0));
        assertTrue(h.scene.camera().revision() > startRevision);
        assertEquals(MouseCursor.CROSSHAIR, h.scene.mouseCursorAt(220, 35));
        h.input.pointerReleased(250, 55, 0, PointerButton.PRIMARY);
        assertNull(h.ui.capturedPointer(0));
        assertEquals(0, clicks.get());

        h.input.pointerPressed(130, 80, 0, PointerButton.MIDDLE);
        h.input.pointerMoved(145, 90, 0);
        h.input.pointerReleased(145, 90, 0, PointerButton.MIDDLE);
        assertNull(h.ui.capturedPointer(0));
        MutableVec3 target = h.scene.orbit().target(new MutableVec3());
        assertTrue(Math.abs(target.x()) + Math.abs(target.y()) + Math.abs(target.z()) > 0.01f);
        assertTrue(h.input.pointerScrolled(125, 75, 0, 1, 0, 0));
        assertEquals(4.25f, h.scene.orbit().distance(), EPS);
    }

    /** Проверяет лимит зума и запас глубины для геометрии за центром орбиты. */
    @Test
    void wheelZoom_withMaximumOrbitRadius_leavesRoomBeforeFarPlane() {
        Harness h = new Harness();
        h.scene.camera().setClipPlanes(0.1f, 48);
        h.scene.maxZoomDistance(20);
        h.scene.orbit().setDistance(10);

        assertTrue(h.input.pointerScrolled(125, 75, 0, -10, 0, 0));
        assertEquals(20, h.scene.orbit().distance(), EPS);
        assertEquals(48, h.scene.camera().far(), EPS);
        MutableVec3 screen = new MutableVec3();
        assertTrue(h.scene.camera().project(new MutableVec3(0, 0, -1), screen));
        assertTrue(screen.z() < 1);

        h.scene.maxZoomDistance(0);
        assertTrue(h.input.pointerScrolled(125, 75, 0, -10, 0, 0));
        assertEquals(48, h.scene.orbit().distance(), EPS);
        assertThrows(IllegalArgumentException.class, () -> h.scene.maxZoomDistance(-1));
    }

    @Test
    void disablingScene_releasesPointerAndAllowsNewGestureAfterReenable() {
        Harness h = new Harness();
        h.input.pointerPressed(130, 80, 0, PointerButton.PRIMARY);
        assertSame(h.scene, h.ui.capturedPointer(0));
        h.scene.enabled(false);
        assertNull(h.ui.capturedPointer(0));
        assertNull(h.scene.hoveredNode());

        h.scene.enabled(true);
        h.input.pointerPressed(130, 80, 0, PointerButton.PRIMARY);
        assertSame(h.scene, h.ui.capturedPointer(0));
    }

    @Test
    void anchor_projectsNodeLocalPointAndRoutesButtonClickAboveTheScene() {
        Harness h = new Harness();
        h.scene.camera().setOrthographic(6, 0.1f, 20);
        Button button = new Button("Anchor");
        AtomicInteger clicks = new AtomicInteger();
        AtomicInteger sceneClicks = new AtomicInteger();
        button.onClick(event -> clicks.incrementAndGet());
        h.scene.onNodeClick(event -> sceneClicks.incrementAndGet());
        SceneAnchor3D anchor = h.scene.addAnchor(button, h.node, new MutableVec3(1, 1, 0));

        h.render();

        assertEquals(150.0f, centerX(button), EPS);
        assertEquals(60.0f, centerY(button), EPS);
        assertSame(button, h.ui.hitTester().hitTest(h.scene, 150, 60).orElseThrow().widget());
        assertEquals(DrawCommandType.TEXTURE, h.screen.commands().get(0).type());
        assertEquals(DrawCommandType.PUSH_CLIP, h.screen.commands().get(1).type());
        h.scene.transform().position().set(10, 5);
        h.input.pointerPressed(160, 65, 0, PointerButton.PRIMARY);
        h.input.pointerReleased(160, 65, 0, PointerButton.PRIMARY);
        assertEquals(1, clicks.get());
        assertEquals(0, sceneClicks.get());

        h.node.setPosition(1, 0, 0);
        h.scene.tick(FRAME);
        assertEquals(170.0f, centerX(button), EPS);
        h.node.setVisible(false);
        h.render();
        assertEquals(Visibility.HIDDEN, anchor.visibility());
        assertFalse(anchor.visible());
        assertSame(h.scene, h.ui.hitTester().hitTest(h.scene, 180, 65).orElseThrow().widget());
        assertEquals(3, h.screen.size());

        assertTrue(h.scene.removeAnchor(anchor));
        assertNull(button.parent());
        assertNull(button.uiContext());
    }

    @Test
    void anchorHiddenAfterPress_clearsFocusAndPreventsClickOnStalePosition() {
        Harness h = new Harness();
        Button button = new Button("Anchor");
        AtomicInteger clicks = new AtomicInteger();
        button.onClick(event -> clicks.incrementAndGet());
        SceneAnchor3D anchor = h.scene.addAnchor(button, new MutableVec3());
        h.render();

        h.input.pointerPressed(130, 80, 0, PointerButton.PRIMARY);
        assertSame(button, h.ui.capturedPointer(0));
        anchor.position(0, 0, 10);
        h.render();

        assertNull(h.ui.capturedPointer(0));
        assertNull(h.ui.focusManager().focusedWidget());
        h.input.pointerReleased(130, 80, 0, PointerButton.PRIMARY);
        assertEquals(0, clicks.get());
    }

    @Test
    void anchorMovement_altersOverlayWithoutRefreshingSceneTexture() {
        Harness h = new Harness();
        Button button = new Button("World");
        SceneAnchor3D anchor = h.scene.addAnchor(button, new MutableVec3());
        h.render();
        float initial = centerX(button);

        anchor.position(1, 0, 0);
        h.render();

        assertTrue(centerX(button) > initial);
        assertEquals(1, h.backend.rawCalls);
        anchor.position(0, 0, 10);
        h.render();
        assertFalse(anchor.visible());
        assertEquals(1, h.backend.rawCalls);
    }

    /** Drives both coordinate spaces and an interactive widget subtree from one seekable stage. */
    @Test
    void anchorTrack_seekMovesAndHidesWidgetSubtree() {
        Harness h = new Harness();
        h.scene.camera().setOrthographic(6, 0.1f, 20);
        var panel = new VBox();
        var button = new Button("Anchor");
        var clicks = new AtomicInteger();
        button.onClick(event -> clicks.incrementAndGet());
        panel.addChild(button);
        var anchor = h.scene.addAnchor(panel, h.node, 0, 0, 0);
        var timeline = new SceneTimeline();
        var stage = timeline.stage(1);
        var track = stage.trackAnchor(anchor).visibleBetween(0, 2)
                .position(0, 0, 0, 0).position(2, 2, 1, 0, AnimationEasing.QUAD_IN)
                .screenOffset(0, 0, 0, null).screenOffset(2, 20, -8, null);
        assertSame(track, stage.trackAnchor(anchor));
        assertEquals(3, timeline.duration(), EPS);

        timeline.sample(0);
        h.render();
        assertFalse(anchor.visible());
        timeline.sample(1);
        h.render();
        assertTrue(anchor.visible());
        assertEquals(130, centerX(button), EPS);
        assertEquals(80, centerY(button), EPS);

        timeline.sample(2);
        h.render();
        // The eased point adds (10, -5) UI units after projection. The offset track adds (10, -4).
        assertEquals(150, centerX(button), EPS);
        assertEquals(71, centerY(button), EPS);
        assertEquals(1, h.backend.rawCalls);
        h.input.pointerPressed(150, 71, 0, PointerButton.PRIMARY);
        h.input.pointerReleased(150, 71, 0, PointerButton.PRIMARY);
        assertEquals(1, clicks.get());
        h.input.pointerPressed(150, 71, 0, PointerButton.PRIMARY);
        assertSame(button, h.ui.capturedPointer(0));

        timeline.sample(3);
        h.render();
        assertFalse(anchor.visible());
        assertNull(h.ui.capturedPointer(0));
        h.input.pointerReleased(150, 71, 0, PointerButton.PRIMARY);
        assertEquals(1, clicks.get());
        timeline.sample(1);
        h.render();
        assertTrue(anchor.visible());
        assertEquals(130, centerX(button), EPS);
        assertEquals(80, centerY(button), EPS);
    }

    @Test
    void pickingAndAnchorsRespectLetterboxAndUnusedPixelsOfFixedTexture() {
        Harness h = new Harness();
        h.scene.canvasSize(100, 50).textureSize(200, 100).fit(ImageFit.CONTAIN);
        h.scene.arrange(new MutableRect(30, 20, 100, 100));
        Button button = new Button("Center");
        h.scene.addAnchor(button, new MutableVec3());

        h.render();

        assertNull(h.scene.pickNode(50, 10));
        assertSame(h.node, h.scene.pickNode(50, 50));
        assertEquals(70.0f, centerY(button), EPS);

        h.scene.textureSize(200, 200).fit(ImageFit.STRETCH);
        h.render();
        assertEquals(100.0f, h.backend.viewportHeight, EPS);
        assertEquals(45.0f, centerY(button), EPS);
        assertSame(h.node, h.scene.pickNode(50, 25));
        assertNull(h.scene.pickNode(50, 75));
    }

    private static float centerX(Button button) {
        return button.layoutBounds().x() + button.layoutBounds().width() / 2;
    }

    private static float centerY(Button button) {
        return button.layoutBounds().y() + button.layoutBounds().height() / 2;
    }

    private static final class Harness {
        final Scene3D scene = new Scene3D();
        final SceneNode node = new SceneNode().setBounds(UNIT_BOX);
        final DefaultUIContext ui = new DefaultUIContext();
        final InputHandle input = new InputHandle(scene, ui);
        final Backend backend = new Backend();
        final DrawList screen = new DrawList();
        final DefaultRenderContext context = new DefaultRenderContext(screen).backend(backend);

        Harness() {
            scene.setUiContextInternal(ui);
            scene.scene().addRoot(node);
            scene.arrange(new MutableRect(30, 20, 200, 120));
        }

        void render() {
            screen.clear();
            scene.render(context);
        }
    }

    private static final class Backend implements RenderBackend, Scene3DRenderer {
        final List<String> passes = new ArrayList<>();
        final List<DrawCommand> overlayCommands = new ArrayList<>();
        boolean supports3D = true;
        boolean failOverlay;
        RenderTargetOptions options;
        RenderTarget rawTarget;
        RenderTarget overlayTarget;
        Scene lastScene;
        Camera3D lastCamera;
        int rawCalls;
        int targetCreations;
        float viewportWidth;
        float viewportHeight;
        float clearAlpha;
        float partialTick;
        /** Последний меш подсветки, переданный фейковому бэкенду, или null. */
        Scene3DRenderer.Highlight highlight;

        @Override
        public RenderTarget createRenderTarget(int width, int height, RenderTargetOptions options) {
            this.options = options;
            targetCreations++;
            return new SimpleRenderTarget(width, height);
        }

        @Override
        public Scene3DRenderer scene3D() {
            return supports3D ? this : RenderBackend.super.scene3D();
        }

        @Override
        public void render(RenderTarget target, Scene scene, Camera3D camera, float width, float height,
                           ColorView clearColor, float partialTick) {
            passes.add("3D");
            rawTarget = target;
            lastScene = scene;
            lastCamera = camera;
            viewportWidth = width;
            viewportHeight = height;
            clearAlpha = clearColor.a();
            this.partialTick = partialTick;
            rawCalls++;
        }

        /** {@inheritDoc} */
        @Override
        public void render(RenderTarget target, Scene scene, Camera3D camera, float width, float height,
                           ColorView clearColor, float partialTick, Scene3DRenderer.Highlight highlight) {
            this.highlight = highlight;
            render(target, scene, camera, width, height, clearColor, partialTick);
        }

        @Override
        public void render(DrawList drawList, RenderTarget target, float coordinateScale) {
            if (failOverlay) throw new IllegalStateException("Incomplete overlay");
            passes.add("2D");
            overlayTarget = target;
            overlayCommands.clear();
            overlayCommands.addAll(drawList.commands());
        }

        @Override
        public void render(DrawList drawList, RenderTarget target) {
            render(drawList, target, 1);
        }

        @Override
        public void beginFrame(FrameContext frame) {}

        @Override
        public void endFrame() {}
    }
}

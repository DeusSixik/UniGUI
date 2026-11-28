package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.rendering.Scene3D;

/**
 * Unity-style Game view with a fixed camera rendering the editable scene.
 */
public class UnityEditorGameView extends LinearBox {
    /**
     * Preview viewport with its own camera and no camera input.
     */
    private final Scene3D viewport;

    private boolean isPlaying = false;
    /**
     * Whether Play is temporarily frozen.
     */
    private boolean paused;
    private boolean statsVisible = false;
    private boolean maximizeOnPlay = false;
    private boolean muteAudio = false;

    private static final MutableColor STATS_BG = new MutableColor(0.12f, 0.12f, 0.14f, 0.88f);
    private static final MutableColor STATS_BORDER = new MutableColor(0.25f, 0.25f, 0.28f, 0.8f);

    /**
     * Creates an empty Game view.
     */
    public UnityEditorGameView() {
        this(new Scene());
    }

    /**
     * Creates a preview of the editor's shared scene.
     *
     * @param scene graph shared with the Scene tab
     */
    /**
     * Keeps the shared scene lit by its directional light object while only the Game tab is shown.
     *
     * @param frame frame timing
     */
    @Override
    public void tick(dev.sixik.unigui.api.core.FrameContext frame) {
        UnityPlayMode.tick(viewport.scene(), frame);
        UnitySceneOverlay.syncSceneLighting(viewport.scene());
        super.tick(frame);
    }

    UnityEditorGameView(Scene scene) {
        super(Orientation.VERTICAL);
        viewport = new Scene3D(scene).cameraControls(false);
        viewport.clearColor().set(0.09f, 0.105f, 0.135f, 1.0f);
        viewport.camera().setPerspective((float) Math.toRadians(45.0), 0.05f, 128.0f);
        viewport.orbit().setOrbit(new MutableVec3(0, 0.55f, 0), 8.0f, -0.28, 0.3);
        viewport.layout(style -> style.widthPercent(100.0f).height(0.0f).flexGrow(1.0f));
        spacing(0.0f);
        layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));

        // 1. Game View toolbar
        HBox toolbar = new HBox();
        toolbar.spacing(6.0f);
        toolbar.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .padding(6.0f, 0.0f, 6.0f, 0.0f)
                .alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        toolbar.addChild(createToolBtn("Display 1 v"));
        toolbar.addChild(createToolBtn("Free Aspect v"));
        toolbar.addChild(createToolBtn("Scale 1x"));

        Button maxBtn = createToggleBtn("Maximize on Play", maximizeOnPlay);
        maxBtn.onClick(e -> {
            maximizeOnPlay = !maximizeOnPlay;
            updateToggleVisual(maxBtn, maximizeOnPlay);
        });
        toolbar.addChild(maxBtn);

        Button muteBtn = createToggleBtn("Mute Audio", muteAudio);
        muteBtn.onClick(e -> {
            muteAudio = !muteAudio;
            updateToggleVisual(muteBtn, muteAudio);
        });
        toolbar.addChild(muteBtn);

        Button statsBtn = createToggleBtn("Stats", statsVisible);
        statsBtn.onClick(e -> {
            statsVisible = !statsVisible;
            updateToggleVisual(statsBtn, statsVisible);
            viewport.requestRender();
        });
        toolbar.addChild(statsBtn);

        toolbar.addChild(createToolBtn("Gizmos v"));

        addChild(toolbar);
        addChild(UnityStyles.separatorLine());

        viewport.painter((context, width, height) -> {
            if (!isPlaying || paused) {
                context.addText(paused ? "Play mode paused" : "Play mode stopped. Press ▶ in toolbar to run.",
                        8.0f, height - 20.0f, Math.max(0.0f, width - 16.0f), 18.0f, UnityStyles.TEXT_MUTED);
            }
            if (statsVisible) {
                float overlayWidth = Math.min(200.0f, width - 20.0f);
                if (overlayWidth > 0)
                    drawStatsOverlay(context, width - overlayWidth - 10.0f, 10.0f, overlayWidth, 130.0f);
            }
        });
        addChild(viewport);
    }

    private void drawStatsOverlay(RenderContext context, float sx, float sy, float sw, float sh) {
        context.rectFilled(sx, sy, sw, sh, STATS_BG);
        context.addRect(sx, sy, sw, sh, STATS_BORDER, 1.0f);

        float px = sx + 8.0f;
        float py = sy + 6.0f;
        float tw = sw - 16.0f;

        context.addText("Statistics", px, py, tw, 18.0f, UnityStyles.TEXT_BOLD);
        py += 18.0f;
        context.addLine(sx, py, sx + sw, py, STATS_BORDER, 1.0f);
        py += 6.0f;

        boolean running = isPlaying && !paused;
        String fps = running ? "60.0 FPS (16.6ms)" : "0.0 FPS (paused)";
        context.addText(fps, px, py, tw, 16.0f, running ? UnityStyles.PLAY_GREEN : UnityStyles.TEXT_MUTED);
        py += 15.0f;

        context.addText("Batches: 14   Saved: 6", px, py, tw, 15.0f, UnityStyles.TEXT_PRIMARY);
        py += 14.0f;

        context.addText("Tris: 2.4k    Verts: 4.8k", px, py, tw, 15.0f, UnityStyles.TEXT_PRIMARY);
        py += 14.0f;

        context.addText("Screen: 1920x1080 - 1.2 MB", px, py, tw, 15.0f, UnityStyles.TEXT_MUTED);
        py += 14.0f;

        context.addText("SetPass calls: 12", px, py, tw, 15.0f, UnityStyles.TEXT_MUTED);
        py += 14.0f;

        context.addText("Shadow casters: 1", px, py, tw, 15.0f, UnityStyles.TEXT_MUTED);
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        paused = false;
        UnityPlayMode.setRunning(viewport.scene(), playing);
        viewport.requestRender();
    }

    /**
     * Updates the transport status while keeping the fixed preview camera active.
     *
     * @param paused whether the current Play session is paused
     */
    void setPaused(boolean paused) {
        this.paused = paused;
        UnityPlayMode.setRunning(viewport.scene(), isPlaying && !paused);
        viewport.requestRender();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    private static Button createToolBtn(String label) {
        Button btn = new Button(label);
        UnityStyles.styleButton(btn);
        btn.focusRingVisible(false);
        btn.focusable(false);
        btn.layout(style -> style.height(18.0f).padding(5.0f, 0.0f));
        return btn;
    }

    private static Button createToggleBtn(String label, boolean initial) {
        Button btn = new Button(label);
        btn.themeEnabled(false);
        btn.backgroundVisible(true);
        btn.background().set(initial ? UnityStyles.BG_BUTTON_ACTIVE : UnityStyles.BG_INPUT);
        btn.borderVisible(true);
        btn.borderColor().set(UnityStyles.BORDER_INPUT);
        btn.radius(2.0f);
        btn.focusRingVisible(false);
        btn.focusable(false);
        btn.textColor().set(UnityStyles.TEXT_PRIMARY);
        btn.layout(style -> style.height(18.0f).padding(5.0f, 0.0f));
        return btn;
    }

    private static void updateToggleVisual(Button btn, boolean active) {
        btn.background().set(active ? UnityStyles.BG_BUTTON_ACTIVE : UnityStyles.BG_INPUT);
    }
}

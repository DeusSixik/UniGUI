package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Графический контекст UniGUI для Minecraft 26.1: замена {@code GuiGraphics} из 1.20/1.21.
 *
 * <p>В 26.1 {@code GuiGraphics} заменён на {@link GuiGraphicsExtractor}, у которого поза двумерная
 * ({@link Matrix3x2fStack}), а всё нарисованное попадает в отложенную очередь. UniGUI рисует сам и сразу, поэтому
 * держит собственную трёхмерную позу ({@link PoseStack}, как в других версиях) и обращается к extractor только
 * для ванильных элементов через {@link #submit(Consumer)}: перед этим нарисованный слой UniGUI переносится в
 * очередь, чтобы порядок наложения не нарушился.</p>
 */
public final class UniGuiGraphics {
    private final PoseStack pose = new PoseStack();
    private GuiGraphicsExtractor extractor;

    /**
     * @param extractor extractor текущего кадра
     */
    public UniGuiGraphics(GuiGraphicsExtractor extractor) {
        extractor(extractor);
    }

    /**
     * Меняет extractor и берёт его текущую позу как исходную.
     *
     * @param extractor extractor текущего кадра
     * @return этот контекст
     */
    public UniGuiGraphics extractor(GuiGraphicsExtractor extractor) {
        this.extractor = Objects.requireNonNull(extractor, "extractor");
        syncPoseFromExtractor();
        return this;
    }

    /**
     * Сбрасывает собственную позу к текущей позе extractor.
     */
    public void syncPoseFromExtractor() {
        while (!pose.isEmpty()) pose.popPose();
        Matrix3x2f source = extractor.pose();
        Matrix4f target = pose.last().pose();
        target.identity();
        target.m00(source.m00());
        target.m01(source.m01());
        target.m10(source.m10());
        target.m11(source.m11());
        target.m30(source.m20());
        target.m31(source.m21());
        pose.last().normal().identity();
    }

    /**
     * @return поза UniGUI
     */
    public PoseStack pose() {
        return pose;
    }

    /**
     * Возвращает extractor для прямой отправки ванильных элементов. Перед этим нарисованный слой UniGUI
     * переносится в очередь GUI. Поза extractor не синхронизируется: для этого есть {@link #submit(Consumer)}.
     *
     * @return extractor текущего кадра
     */
    public GuiGraphicsExtractor extractor() {
        UiLayerCompositor.flush();
        return extractor;
    }

    /**
     * extractor без переноса слоя (для служебного кода бэкенда).
     */
    GuiGraphicsExtractor rawExtractor() {
        return extractor;
    }

    /**
     * Отправляет ванильные элементы в ванильных единицах GUI с единичной позой.
     *
     * @param action отправка элементов
     */
    public void submitInGuiSpace(Consumer<GuiGraphicsExtractor> action) {
        UiLayerCompositor.flush();
        Matrix3x2fStack extractorPose = extractor.pose();
        extractorPose.pushMatrix();
        try {
            extractorPose.identity();
            action.accept(extractor);
        } finally {
            extractorPose.popMatrix();
        }
    }

    /**
     * Отправляет ванильные элементы с текущей позой UniGUI (её двумерной частью).
     *
     * @param action отправка элементов
     */
    public void submit(Consumer<GuiGraphicsExtractor> action) {
        submit(action, true);
    }

    /**
     * Отправляет ванильные элементы в единицах GUI с единичной позой, но с обрезкой по scissor UniGUI.
     * Нужен для picture-in-picture элементов (сущности): их прямоугольник не преобразуется позой.
     *
     * @param action отправка элементов
     */
    public void submitClippedInGuiSpace(Consumer<GuiGraphicsExtractor> action) {
        submit(action, false);
    }

    private void submit(Consumer<GuiGraphicsExtractor> action, boolean applyPose) {
        UiLayerCompositor.flush();
        Matrix3x2fStack extractorPose = extractor.pose();
        extractorPose.pushMatrix();
        boolean scissor = false;
        try {
            extractorPose.identity();
            scissor = pushVanillaScissor();
            if (applyPose) {
                Matrix4f source = pose.last().pose();
                extractorPose.set(source.m00(), source.m01(), source.m10(), source.m11(), source.m30(), source.m31());
            }
            action.accept(extractor);
        } finally {
            if (scissor) extractor.disableScissor();
            extractorPose.popMatrix();
        }
    }

    /**
     * Переносит текущий scissor UniGUI в extractor, чтобы ванильные элементы (предметы, сущности) обрезались
     * так же, как остальной UI. Вызывается при единичной позе extractor'а.
     *
     * @return {@code true}, если scissor включён и его надо снять через {@code disableScissor()}
     */
    private boolean pushVanillaScissor() {
        if (!UiLayerCompositor.drawingToScreen() || !UiGpu.scissorEnabled()) return false;
        UiGpu.Target target = UiGpu.target();
        double scaleX = extractor.guiWidth() / (double) target.width();
        double scaleY = extractor.guiHeight() / (double) target.height();
        // Scissor UiGpu — в пикселях цели с началом снизу, extractor — в единицах GUI с началом сверху.
        int top = target.height() - (UiGpu.scissorY() + UiGpu.scissorHeight());
        int x0 = (int) Math.floor(UiGpu.scissorX() * scaleX);
        int y0 = (int) Math.floor(top * scaleY);
        int x1 = (int) Math.ceil((UiGpu.scissorX() + UiGpu.scissorWidth()) * scaleX);
        int y1 = (int) Math.ceil((top + UiGpu.scissorHeight()) * scaleY);
        extractor.enableScissor(x0, y0, Math.max(x0, x1), Math.max(y0, y1));
        return true;
    }

    /**
     * Ширина GUI в ванильных единицах.
     */
    public int guiWidth() {
        return extractor.guiWidth();
    }

    /**
     * Высота GUI в ванильных единицах.
     */
    public int guiHeight() {
        return extractor.guiHeight();
    }

    /**
     * Совместимость с 1.20/1.21: там {@code flush()} отправлял накопленную геометрию. Здесь переносит слой
     * UniGUI в очередь GUI.
     */
    public void flush() {
        UiLayerCompositor.flush();
    }
}

package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/**
 * Согласует API позы и RenderType.
 */
final class ScenePoseCompat {
    private ScenePoseCompat() {
    }

    /**
     * Применяет преобразование позы и её нормалей.
     *
     * @param pose      выходная поза.
     * @param transform мировая матрица узла в пространстве камеры.
     */
    static void multiply(PoseStack pose, Matrix4f transform) {
        pose.mulPose(transform);
    }

    /**
     * Проверяет необходимость сортировки ванильных квадов.
     *
     * @param type тип геометрии Minecraft.
     * @return true для типов с прозрачными сортируемыми квадами.
     */
    static boolean sortsOnUpload(RenderType type) {
        return type.sortOnUpload();
    }
}

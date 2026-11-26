package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.RenderType;

/**
 * Буфер одного RenderType в 1.20.1.
 */
final class SceneFeatureBuffer {
    /**
     * Переиспользуемое хранилище вершин и отсортированных индексов.
     */
    private final BufferBuilder data = new BufferBuilder(4096);

    /**
     * Начинает запись очередного слоя.
     *
     * @param type формат и топология ванильной геометрии.
     * @return потребитель вершин.
     */
    VertexConsumer begin(RenderType type) {
        data.begin(type.mode(), type.format());
        return data;
    }

    /**
     * Отправляет накопленные вершины с текущим шейдером.
     *
     * @param sorting порядок сортировки квадов или null для исходного порядка.
     */
    void draw(VertexSorting sorting) {
        if (sorting != null) data.setQuadSorting(sorting);
        var mesh = data.endOrDiscardIfEmpty();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    /**
     * Сбрасывает незавершённый слой перед переиспользованием.
     */
    void discard() {
        if (data.building()) {
            var mesh = data.endOrDiscardIfEmpty();
            if (mesh != null) mesh.release();
        }
    }
}

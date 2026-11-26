package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.RenderType;

/**
 * Буфер одного RenderType в 1.21.1.
 */
final class SceneFeatureBuffer {
    /**
     * Переиспользуемое хранилище вершин и отсортированных индексов.
     */
    private final ByteBufferBuilder data = new ByteBufferBuilder(4096);
    /**
     * Формат и указатель записи текущего слоя.
     */
    private BufferBuilder builder;

    /**
     * Начинает запись очередного слоя.
     *
     * @param type формат и топология ванильной геометрии.
     * @return потребитель вершин.
     */
    VertexConsumer begin(RenderType type) {
        builder = new BufferBuilder(data, type.mode(), type.format());
        return builder;
    }

    /**
     * Отправляет накопленные вершины с текущим шейдером.
     *
     * @param sorting порядок сортировки квадов или null для исходного порядка.
     */
    void draw(VertexSorting sorting) {
        var mesh = builder.build();
        builder = null;
        if (mesh == null) return;
        if (sorting != null) {
            mesh.sortQuads(data, sorting);
        }
        BufferUploader.drawWithShader(mesh);
    }

    /**
     * Сбрасывает незавершённый слой перед переиспользованием.
     */
    void discard() {
        builder = null;
        data.clear();
    }
}

package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Aabb;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Проверяет чтение запечённых квадов модели предмета в Minecraft 1.20.1 и 1.21.1.
 */
final class SceneItemModelTest {
    /**
     * Допуск учитывает погрешность float при повороте на 90 градусов через кватернион.
     */
    private static final float EPS = 1e-6f;

    /**
     * Проверяет центровку, масштаб, поворот и перенос FIXED для обеих групп квадов.
     */
    @Test
    void bounds_thinModel_rotatesThicknessAndIncludesDirectionalQuads() {
        var fixed = new ItemTransform(new Vector3f(0, 90, 0), new Vector3f(0.25f, 0, -0.125f),
                new Vector3f(1, 2, 0.5f));
        var model = model(fixed, List.of(face(0.53125f)), List.of(face(0.46875f)));

        Aabb bounds = SceneItemModel.bounds(model, ItemDisplayContext.FIXED);

        assertNotNull(bounds);
        assertEquals(0.234375f, bounds.minX(), EPS);
        assertEquals(0.265625f, bounds.maxX(), EPS);
        assertEquals(-0.5f, bounds.minY(), EPS);
        assertEquals(0.5f, bounds.maxY(), EPS);
        assertEquals(-0.5f, bounds.minZ(), EPS);
        assertEquals(0.25f, bounds.maxZ(), EPS);
    }

    /**
     * Проверяет отсутствие границ у модели без квадов.
     */
    @Test
    void bounds_noQuads_returnsNull() {
        assertNull(SceneItemModel.bounds(model(ItemTransform.NO_TRANSFORM, List.of(), List.of()), ItemDisplayContext.FIXED));
    }

    /**
     * Создаёт запечённую модель с отдельными общими и направленными квадами.
     *
     * @param fixed    трансформация контекста FIXED.
     * @param unculled общие квады.
     * @param north    квады направления NORTH.
     * @return модель с заданной геометрией.
     */
    private static SimpleBakedModel model(ItemTransform fixed, List<BakedQuad> unculled, List<BakedQuad> north) {
        var none = ItemTransform.NO_TRANSFORM;
        var transforms = new ItemTransforms(none, none, none, none, none, none, none, fixed);
        var directional = new EnumMap<Direction, List<BakedQuad>>(Direction.class);
        for (Direction side : Direction.values()) directional.put(side, side == Direction.NORTH ? north : List.of());
        return new SimpleBakedModel(unculled, directional, false, false, true, null, transforms, ItemOverrides.EMPTY);
    }

    /**
     * Создаёт квад с четырьмя запечёнными позициями.
     *
     * @param z глубина всех вершин в блоках до центровки.
     * @return квад в стандартном формате запечённой модели.
     */
    private static BakedQuad face(float z) {
        int[] vertices = new int[32];
        for (int i = 0; i < 4; i++) {
            int start = i * 8;
            vertices[start] = Float.floatToRawIntBits(i % 2 == 0 ? 0.125f : 0.875f);
            vertices[start + 1] = Float.floatToRawIntBits(i < 2 ? 0.25f : 0.75f);
            vertices[start + 2] = Float.floatToRawIntBits(z);
        }
        return new BakedQuad(vertices, -1, Direction.NORTH, null, false);
    }
}

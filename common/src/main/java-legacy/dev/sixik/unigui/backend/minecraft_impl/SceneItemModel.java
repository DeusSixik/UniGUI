package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.backend.minecraft_impl.custom_renders.MinecraftRendererPlatformHook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;

/**
 * Читает геометрию запечённых предметов Minecraft 1.20.1 и 1.21.1.
 */
final class SceneItemModel {
    /**
     * Ключ модели трезубца для FIXED.
     */
    private static final ModelResourceLocation TRIDENT_MODEL = ModelResourceLocation.vanilla("trident", "inventory");
    /**
     * Ключ модели подзорной трубы для FIXED.
     */
    private static final ModelResourceLocation SPYGLASS_MODEL = ModelResourceLocation.vanilla("spyglass", "inventory");
    /**
     * Направления, по которым ванильный ItemRenderer запрашивает запечённые грани.
     */
    private static final Direction[] DIRECTIONS = Direction.values();
    /**
     * Ванильный seed для выбора квадов по каждому направлению.
     */
    private static final long QUAD_SEED = 42L;

    /**
     * Создание экземпляров не требуется.
     */
    private SceneItemModel() {
    }

    /**
     * Читает идентификатор текущей загрузки моделей.
     *
     * @param client клиент Minecraft.
     * @return объект, меняющийся при перезагрузке ресурсов, или null до загрузки моделей.
     */
    static Object generation(Minecraft client) {
        var manager = client.getModelManager();
        return manager == null || client.getItemRenderer() == null ? null : manager.getMissingModel();
    }

    /**
     * Разрешает запечённую модель так же, как ItemRenderer для FIXED, и вычисляет её границы.
     *
     * @param client клиент с загруженными моделями.
     * @param stack  стек, источник свойств модели.
     * @return локальный AABB или null для моделей без доступной запечённой геометрии.
     */
    static Aabb bounds(Minecraft client, ItemStack stack) {
        BakedModel model;
        if (stack.is(Items.TRIDENT)) model = client.getModelManager().getModel(TRIDENT_MODEL);
        else if (stack.is(Items.SPYGLASS)) model = client.getModelManager().getModel(SPYGLASS_MODEL);
        else model = client.getItemRenderer().getModel(stack, client.level, null, 0);
        if (MinecraftRendererPlatformHook.hasCustomItemRendererImpl(stack)) return null;
        return bounds(model);
    }

    /**
     * Находит минимальную и максимальную координату по каждой оси среди преобразованных вершин.
     * Позиция в узле равна {@code fixed * (p - center)}, где p взята из запечённого квада,
     * center равен (0.5, 0.5, 0.5) блока, fixed — ванильная матрица для рамки.
     *
     * @param model модель с четырьмя вершинами на квад, в координатах запекания Minecraft.
     * @return локальный AABB или null, если у модели нет запечённых вершин.
     */
    static Aabb bounds(BakedModel model) {
        if (model == null || model.isCustomRenderer()) return null;
        PoseStack pose = new PoseStack();
        model.getTransforms().getTransform(ItemDisplayContext.FIXED).apply(false, pose);
        pose.translate(-0.5f, -0.5f, -0.5f);
        var transform = pose.last().pose();
        RandomSource random = RandomSource.create(QUAD_SEED);
        Vector3f vertex = new Vector3f();
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (int side = 0; side <= DIRECTIONS.length; side++) {
            Direction direction = side == DIRECTIONS.length ? null : DIRECTIONS[side];
            random.setSeed(QUAD_SEED);
            for (var quad : model.getQuads(null, direction, random)) {
                int[] data = quad.getVertices();
                int stride = data.length / 4;
                if (stride < 3) continue;
                for (int i = 0; i < 4; i++) {
                    int start = i * stride;
                    transform.transformPosition(Float.intBitsToFloat(data[start]),
                            Float.intBitsToFloat(data[start + 1]), Float.intBitsToFloat(data[start + 2]), vertex);
                    if (!vertex.isFinite()) continue;
                    minX = Math.min(minX, vertex.x);
                    minY = Math.min(minY, vertex.y);
                    minZ = Math.min(minZ, vertex.z);
                    maxX = Math.max(maxX, vertex.x);
                    maxY = Math.max(maxY, vertex.y);
                    maxZ = Math.max(maxZ, vertex.z);
                }
            }
        }
        return minX == Float.POSITIVE_INFINITY ? null : new Aabb(minX, minY, minZ, maxX, maxY, maxZ);
    }
}

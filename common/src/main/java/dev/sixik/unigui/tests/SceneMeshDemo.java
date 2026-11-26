package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.resource.UniResource;
import dev.sixik.unigui.api.scene.AmbientLight;
import dev.sixik.unigui.api.scene.DirectionalLight;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClientCompat;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftScene;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftTextureHandle;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.api.scene.StructureNode;
import dev.sixik.unigui.widgets.rendering.RenderToTexture;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;

/**
 * Сцена для отладочного захвата. Куб рисуется перед перекрывающимися дальними мешами.
 */
public final class SceneMeshDemo {
    private SceneMeshDemo() {
    }

    /**
     * Открывает сцену на клиентском потоке.
     */
    public static void openDemo() {
        Minecraft.getInstance().execute(() -> MinecraftClientCompat.setScreen(
                new MinecraftWidgetScreen(Component.literal("UniGUI 3D Scene"), create())));
    }

    /**
     * Открывает сцену проверки ванильных блоков, tint, слоёв и внутренней геометрии.
     */
    public static void openBlocksDemo() {
        Minecraft.getInstance().execute(() -> MinecraftClientCompat.setScreen(
                new MinecraftWidgetScreen(Component.literal("UniGUI Block Scene"), createBlocks())));
    }

    /**
     * Создаёт ванильный макет 5×5×5 и отдельно стоящую печь.
     *
     * @return корневой виджет демонстрации.
     */
    private static Scene3D createBlocks() {
        Scene3D widget = new Scene3D();
        widget.highlightBlocks(true).highlightObjects(true);
        widget.updateMode(RenderToTexture.UpdateMode.EVERY_FRAME);
        widget.clearColor().set(0.046f, 0.071f, 0.11f, 1);
        widget.camera().setPerspective((float) Math.toRadians(45), 0.2f, 128);
        widget.maxZoomDistance(60);
        widget.orbit().setOrbit(new MutableVec3(2.5f, 1.8f, 2.5f), 12.5f, 0.46, 0.43);
        StructureNode structure = new StructureNode();
        for (int z = 0; z < 5; z++) {
            for (int x = 0; x < 5; x++) {
                boolean water = x > 0 && x < 4 && z > 0 && z < 4;
                structure.setBlock(x, 0, z,
                        (water ? Blocks.STONE_BRICKS : Blocks.GRASS_BLOCK).defaultBlockState());
                if (water) structure.setBlock(x, 1, z, Blocks.WATER.defaultBlockState());
            }
        }
        for (int y = 1; y < 5; y++) {
            structure.setBlock(0, y, 0,
                    (y < 3 ? Blocks.OAK_LOG : Blocks.OAK_LEAVES).defaultBlockState());
            structure.setBlock(1, y, 0, Blocks.OAK_LEAVES.defaultBlockState());
        }
        for (int z = 1; z < 4; z++) structure.setBlock(4, 1, z, Blocks.GLASS.defaultBlockState());
        structure.setBlock(0, 1, 2, Blocks.FURNACE.defaultBlockState());
        structure.setBlock(4, 1, 4, Blocks.CHEST.defaultBlockState());
        BlockNode furnace = BlockNode.of(Blocks.FURNACE.defaultBlockState());
        furnace.setPosition(3.6f, 0, 5.4f).setRotation(new MutableQuat().setYawPitch(Math.PI, 0));
        BlockNode secondFurnace = BlockNode.of(MinecraftScene.state(furnace));
        secondFurnace.setPosition(4.8f, 0, 5.4f).setRotation(new MutableQuat().setYawPitch(Math.PI * 0.75, 0));
        EntityNode zombie = EntityNode.of(MinecraftClientCompat.ZOMBIE);
        zombie.setPosition(1.1f, 0, 5.9f).setRotation(new MutableQuat().setYawPitch(0.38, 0));
        widget.scene().addRoot(structure).addRoot(furnace).addRoot(secondFurnace).addRoot(zombie);
        ItemNode item = ItemNode.of(SceneDemoItems.sword());
        item.setPosition(4.72f, 2.8f, 4.55f).setScale(1.6f, 1.6f, 1.6f)
                .setRotation(new MutableQuat().setYawPitch(0.32, 0));
        widget.scene().addRoot(item);
        return widget;
    }

    /**
     * Создаёт сцену с кубом, сферой и текстурированным щитом.
     *
     * @return корневой виджет демонстрации.
     */
    private static Scene3D create() {
        Scene3D widget = new Scene3D();
        widget.updateMode(RenderToTexture.UpdateMode.EVERY_FRAME);
        widget.clearColor().set(0.046f, 0.071f, 0.11f, 1);
        widget.camera().setPerspective((float) Math.toRadians(45), 0.2f, 64);
        widget.maxZoomDistance(32);
        widget.orbit().setOrbit(new MutableVec3(0.2f, 0.05f, 0), 6.2f, 0.12, 0.22);

        SceneNode cube = new SceneNode().setMesh(cube()).setPosition(-1.08f, -0.03f, 0.64f).setScale(1.48f, 1.48f, 1.48f)
                .setRotation(new MutableQuat().setYawPitch(0.43, -0.07))
                .setMaterial(lit(0xFFF5A848, null));
        SceneNode sphere = new SceneNode().setMesh(sphere(32, 20)).setPosition(0.12f, -0.04f, -0.24f)
                .setScale(0.98f, 0.98f, 0.98f).setMaterial(lit(0xFF58C5EB, null));
        MinecraftTextureHandle planks = new MinecraftTextureHandle(
                new UniResource("minecraft", "textures/block/oak_planks.png"), 16, 16);
        SceneNode textured = new SceneNode().setMesh(panel()).setPosition(1.63f, -0.10f, -0.52f)
                .setRotation(new MutableQuat().setYawPitch(-0.24, -0.10))
                .setMaterial(lit(0xFFFFFFFF, planks));
        widget.scene().setAmbientLight(new AmbientLight(0xE0EDFF, 0.34f))
                .setDirectionalLight(new DirectionalLight(0.45f, -0.72f, -0.52f, 0xFFF0DF, 0.86f))
                .addRoot(cube).addRoot(sphere).addRoot(textured);

        Paint title = Paint.fill(MutableColor.fromArgb(0xFFE3EDF7));
        widget.painter((context, width, height) -> context.text("UniGUI 3D Scene", 16, 14, 150, 16, title));
        return widget;
    }

    /**
     * Создаёт непрозрачный материал с освещением.
     *
     * @param color   базовый ARGB.
     * @param texture текстура или null.
     * @return материал поверхности.
     */
    private static Material lit(int color, MinecraftTextureHandle texture) {
        return new Material(color, texture, Material.Shading.LIT, Material.AlphaMode.OPAQUE, 0.5f, Material.Culling.BACK);
    }

    /**
     * Собирает куб со своей нормалью и четырьмя вершинами на каждую грань.
     *
     * @return единичный куб с центром в начале координат.
     */
    private static Mesh3D cube() {
        float n = -0.5f, p = 0.5f;
        float[] positions = {
                p, n, p, p, n, n, p, p, n, p, p, p,
                n, n, n, n, n, p, n, p, p, n, p, n,
                n, p, p, p, p, p, p, p, n, n, p, n,
                n, n, n, p, n, n, p, n, p, n, n, p,
                n, n, p, p, n, p, p, p, p, n, p, p,
                p, n, n, n, n, n, n, p, n, p, p, n
        };
        int[][] directions = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        float[] normals = new float[positions.length];
        int[] indices = new int[36];
        for (int face = 0; face < 6; face++) {
            int base = face * 4;
            for (int i = 0; i < 4; i++) {
                int component = (base + i) * 3;
                normals[component] = directions[face][0];
                normals[component + 1] = directions[face][1];
                normals[component + 2] = directions[face][2];
            }
            quad(indices, face * 6, base, base + 1, base + 2, base + 3);
        }
        return new Mesh3D(positions, normals, null, null, indices);
    }

    /**
     * Строит сферу по меридианам и параллелям. Координаты и нормали имеют вид
     * {@code (sin(phi)*cos(theta), cos(phi), sin(phi)*sin(theta))}, где phi идёт между полюсами
     * от 0 до π, а theta вдоль параллели от 0 до 2π.
     *
     * @param columns число сегментов вдоль параллели.
     * @param rows    число сегментов между полюсами.
     * @return индексированная сфера единичного радиуса.
     */
    private static Mesh3D sphere(int columns, int rows) {
        int stride = columns + 1;
        float[] positions = new float[stride * (rows + 1) * 3];
        float[] uv = new float[stride * (rows + 1) * 2];
        int[] indices = new int[rows * columns * 6];
        for (int row = 0; row <= rows; row++) {
            double phi = Math.PI * row / rows;
            for (int column = 0; column <= columns; column++) {
                double theta = 2 * Math.PI * column / columns;
                int vertex = row * stride + column;
                positions[vertex * 3] = (float) (Math.sin(phi) * Math.cos(theta));
                positions[vertex * 3 + 1] = (float) Math.cos(phi);
                positions[vertex * 3 + 2] = (float) (Math.sin(phi) * Math.sin(theta));
                uv[vertex * 2] = (float) column / columns;
                uv[vertex * 2 + 1] = (float) row / rows;
                if (row < rows && column < columns) {
                    quad(indices, (row * columns + column) * 6, vertex, vertex + 1, vertex + stride + 1, vertex + stride);
                }
            }
        }
        return new Mesh3D(positions, positions, uv, null, indices);
    }

    /**
     * Создаёт прямоугольник с UV от верхнего левого угла текстуры.
     *
     * @return плоский меш с нормалью +Z.
     */
    private static Mesh3D panel() {
        return new Mesh3D(new float[]{-0.75f, -0.9f, 0, 0.75f, -0.9f, 0, 0.75f, 0.9f, 0, -0.75f, 0.9f, 0},
                null, new float[]{0, 1, 1, 1, 1, 0, 0, 0}, null, new int[]{0, 1, 2, 0, 2, 3});
    }

    /**
     * Разбивает четырёхугольник CCW на треугольники по диагонали a–c.
     *
     * @param indices целевой индексный массив.
     * @param offset  позиция записи первого из шести индексов.
     * @param a       индекс первой вершины.
     * @param b       индекс второй вершины.
     * @param c       индекс третьей вершины.
     * @param d       индекс четвёртой вершины.
     */
    private static void quad(int[] indices, int offset, int a, int b, int c, int d) {
        indices[offset] = a;
        indices[offset + 1] = b;
        indices[offset + 2] = c;
        indices[offset + 3] = a;
        indices[offset + 4] = c;
        indices[offset + 5] = d;
    }
}

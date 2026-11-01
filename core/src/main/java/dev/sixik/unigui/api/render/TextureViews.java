package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.MutableRect;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

/**
 * Внутренние helper'ы «текстур-видов»: композиция UV и раскрытие вида в команде рисования.
 *
 * <p>Вид ({@link TextureRegion}, {@link AnimatedTexture}) не доходит до backend'а: backend'ы привязывают
 * только свои handle'ы или разбирают {@link TextureHandle#id()}. Поэтому {@link DrawList} подменяет вид на
 * физическую текстуру ({@link TextureHandle#base()}) и пересчитывает UV команды в область вида.</p>
 */
final class TextureViews {
    private TextureViews() {
    }

    /**
     * Вычисляет UV-область, которую вид занимает в физической текстуре.
     *
     * <p>Если родитель сам является видом, область вкладывается в его текущую область, так что регион
     * внутри анимации или регион региона дают корректный результат.</p>
     *
     * @param parent  родитель вида
     * @param u       начало области внутри родителя, нормализовано
     * @param v       начало области внутри родителя, нормализовано
     * @param uWidth  ширина области внутри родителя
     * @param vHeight высота области внутри родителя
     * @param out     куда записать результат относительно {@code parent.base()}
     */
    static void compose(TextureHandle parent, float u, float v, float uWidth, float vHeight, MutableRect out) {
        if (parent.resolveUv(out)) {
            float parentU = out.x();
            float parentV = out.y();
            float parentWidth = out.width();
            float parentHeight = out.height();
            out.set(parentU + u * parentWidth, parentV + v * parentHeight,
                    uWidth * parentWidth, vHeight * parentHeight);
        } else {
            out.set(u, v, uWidth, vHeight);
        }
    }

    /**
     * Если текстура команды является видом, подменяет её на физическую и пересчитывает UV.
     *
     * <p>Обычные handle'ы проходят без аллокаций: стоимость — один виртуальный вызов.
     * Для {@link DrawCommandType#MESH} создаётся новый mesh, потому что исходный может принадлежать вызывающему.</p>
     *
     * @param command команда, уже принадлежащая draw list
     * @param scratch временный прямоугольник, переиспользуемый между вызовами
     */
    static void resolve(DrawCommand command, MutableRect scratch) {
        TextureHandle texture = command.texture();
        if (texture == null || !texture.resolveUv(scratch)) return;
        float ru = scratch.x();
        float rv = scratch.y();
        float rw = scratch.width();
        float rh = scratch.height();

        switch (command.type()) {
            case TEXTURE -> {
                var uv = command.uv();
                command.uv(ru + uv.x() * rw, rv + uv.y() * rh, uv.width() * rw, uv.height() * rh);
            }
            case TEXTURED_QUAD -> command.texturedQuad(
                    command.quadX1(), command.quadY1(), command.quadX2(), command.quadY2(),
                    command.quadX3(), command.quadY3(), command.quadX4(), command.quadY4(),
                    ru + command.quadU1() * rw, rv + command.quadV1() * rh,
                    ru + command.quadU2() * rw, rv + command.quadV2() * rh,
                    ru + command.quadU3() * rw, rv + command.quadV3() * rh,
                    ru + command.quadU4() * rw, rv + command.quadV4() * rh);
            case MESH -> {
                DrawMesh mesh = command.mesh();
                if (mesh != null) command.meshOwned(remap(mesh, ru, rv, rw, rh));
            }
            default -> {
                // Другие типы не используют UV текстуры команды.
            }
        }
        command.texture(texture.base());
    }

    private static DrawMesh remap(DrawMesh mesh, float ru, float rv, float rw, float rh) {
        List<DrawVertex> source = mesh.vertices();
        ObjectArrayList<DrawVertex> mapped = new ObjectArrayList<>(source.size());
        for (int i = 0, size = source.size(); i < size; i++) {
            DrawVertex vertex = source.get(i);
            mapped.add(new DrawVertex(vertex.x(), vertex.y(),
                    ru + vertex.u() * rw, rv + vertex.v() * rh, vertex.color()));
        }
        return DrawMesh.owned(mapped);
    }
}

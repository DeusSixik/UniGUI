package dev.sixik.unigui.backend.minecraft_impl;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.lwjgl.opengl.GL20;

/**
 * Кэш location-ов uniform для OpenGL-программ.
 *
 * <p>Location uniform-а стабилен до тех пор, пока программа не будет перелинкована.
 * Кэш разделён по идентификаторам программ, поскольку location-ы принадлежат конкретной
 * программе. Результат {@code -1} для отсутствующего или оптимизированного uniform-а
 * также сохраняется.</p>
 */
final class MinecraftUniformLocationCache {
    private static final int NOT_CACHED = Integer.MIN_VALUE;

    private final Int2ObjectMap<Object2IntMap<String>> locationsByProgram = new Int2ObjectOpenHashMap<>();

    /**
     * Возвращает location uniform-а, обращаясь к OpenGL только при первом запросе
     * для пары «программа + имя uniform-а».
     *
     * @param program идентификатор OpenGL-программы
     * @param name    имя uniform-а
     * @return location или {@code -1}, если uniform отсутствует
     */
    int get(int program, String name) {
        if (program <= 0 || name == null || name.isEmpty()) return -1;

        Object2IntMap<String> locations = locationsByProgram.get(program);
        if (locations == null) {
            locations = new Object2IntOpenHashMap<>();
            locations.defaultReturnValue(NOT_CACHED);
            locationsByProgram.put(program, locations);
        }

        int location = locations.getInt(name);
        if (location != NOT_CACHED) return location;

        location = GL20.glGetUniformLocation(program, name);
        locations.put(name, location);
        return location;
    }

    /**
     * Удаляет location-ы одной программы перед её удалением или перелинковкой.
     */
    void remove(int program) {
        locationsByProgram.remove(program);
    }

    /**
     * Очищает все location-ы, принадлежащие этому рендереру.
     */
    void clear() {
        locationsByProgram.clear();
    }
}

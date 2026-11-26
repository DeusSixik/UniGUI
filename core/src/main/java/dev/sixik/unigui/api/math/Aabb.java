package dev.sixik.unigui.api.math;

/**
 * Замкнутый параллелепипед с гранями, параллельными осям. Все координаты конечны.
 * Нижняя и верхняя грани одной оси могут совпадать.
 *
 * @param minX нижняя граница по X в единицах сцены.
 * @param minY нижняя граница по Y в единицах сцены.
 * @param minZ нижняя граница по Z в единицах сцены.
 * @param maxX верхняя граница по X в единицах сцены.
 * @param maxY верхняя граница по Y в единицах сцены.
 * @param maxZ верхняя граница по Z в единицах сцены.
 */
public record Aabb(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
    /**
     * Проверяет и сохраняет границы параллелепипеда.
     *
     * @param minX нижняя граница по X.
     * @param minY нижняя граница по Y.
     * @param minZ нижняя граница по Z.
     * @param maxX верхняя граница по X.
     * @param maxY верхняя граница по Y.
     * @param maxZ верхняя граница по Z.
     * @throws IllegalArgumentException если граница не конечна или нижняя граница выше верхней.
     */
    public Aabb {
        if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(minZ)
                || !Float.isFinite(maxX) || !Float.isFinite(maxY) || !Float.isFinite(maxZ)
                || minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("AABB requires finite, ordered bounds on each axis");
        }
    }
}

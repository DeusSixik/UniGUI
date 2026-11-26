package dev.sixik.unigui.backend.minecraft_impl;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Выдаёт локальные ID сущностям превью, которые существуют вне списка сущностей мира.
 */
final class PreviewEntityIds {
    /**
     * Последний выданный отрицательный ID. Ноль служит начальным значением счётчика.
     */
    private static final AtomicInteger LAST_ID = new AtomicInteger();

    /**
     * Закрывает создание экземпляров класса.
     */
    private PreviewEntityIds() {
    }

    /**
     * Назначает вновь созданной сущности превью ID из отрицательного диапазона, отдельного от ID мира.
     *
     * @param entity новая сущность превью или null, если фабрика не создала сущность.
     * @param <T>    класс сущности.
     * @return переданная сущность с назначенным ID или null.
     */
    static <T extends Entity> @Nullable T assign(@Nullable T entity) {
        if (entity != null) {
            entity.setId(LAST_ID.updateAndGet(previous -> previous == Integer.MIN_VALUE ? -1 : previous - 1));
        }
        return entity;
    }

    /**
     * Подготавливает переданную пользователем сущность для ванильного рендерера.
     * У 26.1.2 пустой ID доступен как ноль, у 26.2 и 26.3 его чтение бросает исключение.
     *
     * @param entity сущность для узла сцены. Выданный ранее ID сохраняется.
     * @throws IllegalStateException если чтение ID прервалось по другой причине.
     */
    static void ensureAssigned(Entity entity) {
        int id;
        try {
            id = entity.getId();
        } catch (IllegalStateException failure) {
            if (!"Tried to access entity ID before ID assignment".equals(failure.getMessage())) throw failure;
            id = 0;
        }
        if (id == 0) assign(entity);
    }
}

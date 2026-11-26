package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.Aabb;

/**
 * Связь узлов Minecraft-контента ({@link BlockNode}, {@link ItemNode}, {@link EntityNode},
 * {@link StructureNode}) с классами конкретной версии игры.
 *
 * <p>Узлы в core хранят только строку или нативный объект пользователя. Платформа превращает их в
 * собственные объекты версии: {@code BlockState} на 1.20+, {@code IBlockState} на 1.12.2, пару блок + meta
 * на 1.7.10. Для core эти объекты непрозрачны, к своим типам их приводят платформа и её рендерер.</p>
 *
 * <p>Методы вызываются в клиентском потоке. Метод {@code resolve*} или {@code native*} может вернуть
 * {@code null}, если контент пока недоступен (например, нет мира для сущности): узел повторит попытку позже.
 * Неизвестный идентификатор или неподходящий тип объекта — {@link IllegalArgumentException}, а
 * неподдерживаемый вид контента — {@link UnsupportedOperationException}. Такие ошибки узел запоминает и
 * больше не повторяет.</p>
 */
public interface ScenePlatform {
    /**
     * Находит состояние блока по разобранной строке.
     *
     * @param spec строка блока.
     * @return нативное состояние или null, если блоки ещё недоступны.
     * @throws IllegalArgumentException      если блок, свойство или значение неизвестны.
     * @throws UnsupportedOperationException если версия не поддерживает такую форму строки.
     */
    Object resolveBlock(BlockSpec spec);

    /**
     * Проверяет и приводит нативный объект пользователя к состоянию блока.
     *
     * @param block нативное состояние или блок.
     * @param meta  метаданные или {@code -1}, если не заданы.
     * @return нативное состояние или null, если оно пока недоступно.
     * @throws IllegalArgumentException если тип объекта не подходит этой версии.
     */
    Object nativeBlock(Object block, int meta);

    /**
     * Проверяет, что состояние описывает пустоту и не имеет геометрии.
     *
     * @param state состояние из {@link #resolveBlock} или {@link #nativeBlock}.
     * @return true для воздуха.
     */
    default boolean isAirBlock(Object state) {
        return false;
    }

    /**
     * Создаёт или обновляет block entity для состояния вне игрового мира.
     *
     * @param state    новое состояние блока.
     * @param previous прежний объект этой позиции или null. Совместимый объект стоит переиспользовать,
     *                 чтобы сохранить его данные и анимацию.
     * @param x        локальная координата блока: 0 для {@link BlockNode}, позиция в {@link StructureNode}.
     * @param y        локальная координата блока по Y.
     * @param z        локальная координата блока по Z.
     * @return block entity или null для блока без неё либо версии без поддержки.
     */
    default Object blockEntity(Object state, Object previous, int x, int y, int z) {
        return null;
    }

    /**
     * Находит предмет по идентификатору.
     *
     * @param id идентификатор предмета, например {@code minecraft:diamond_sword}.
     * @return нативный предмет или null, если предметы ещё недоступны.
     * @throws IllegalArgumentException      если предмет неизвестен.
     * @throws UnsupportedOperationException если версия не умеет рисовать предметы в сцене.
     */
    default Object resolveItem(String id) {
        throw unsupported("Items");
    }

    /**
     * Проверяет и копирует нативный предмет пользователя.
     *
     * @param item стек или предмет версии.
     * @return нативный предмет или null, если он пока недоступен.
     * @throws IllegalArgumentException      если тип объекта не подходит этой версии.
     * @throws UnsupportedOperationException если версия не умеет рисовать предметы в сцене.
     */
    default Object nativeItem(Object item) {
        throw unsupported("Items");
    }

    /**
     * Читает границы модели предмета.
     *
     * @param item предмет из {@link #resolveItem} или {@link #nativeItem}.
     * @return локальные границы, null для пустого предмета.
     */
    default Aabb itemBounds(Object item) {
        return null;
    }

    /**
     * Читает границы модели предмета в заданном расположении.
     *
     * @param item    предмет из {@link #resolveItem} или {@link #nativeItem}.
     * @param display расположение модели.
     * @return локальные границы, null для пустого предмета. По умолчанию — {@link #itemBounds(Object)}.
     */
    default Aabb itemBounds(Object item, ItemNode.Display display) {
        return itemBounds(item);
    }

    /**
     * Находит сущность по идентификатору типа.
     *
     * @param id идентификатор типа, например {@code minecraft:zombie}.
     * @return нативная сущность или null, если её пока нельзя создать.
     * @throws IllegalArgumentException      если тип неизвестен.
     * @throws UnsupportedOperationException если версия не умеет рисовать сущности в сцене.
     */
    default Object resolveEntity(String id) {
        throw unsupported("Entities");
    }

    /**
     * Проверяет нативную сущность пользователя.
     *
     * @param entity сущность, её тип или другой объект, который понимает версия.
     * @return нативная сущность или null, если её пока нельзя создать.
     * @throws IllegalArgumentException      если тип объекта не подходит этой версии.
     * @throws UnsupportedOperationException если версия не умеет рисовать сущности в сцене.
     */
    default Object nativeEntity(Object entity) {
        throw unsupported("Entities");
    }

    /**
     * Читает границы сущности.
     *
     * @param entity сущность из {@link #resolveEntity} или {@link #nativeEntity}.
     * @return локальные границы или null.
     */
    default Aabb entityBounds(Object entity) {
        return null;
    }

    /**
     * Загружает файл структуры по идентификатору ресурса, например {@code minecraft:igloo/top}.
     * Формат и место поиска определяет версия: на 1.20+ это шаблон структуры {@code .nbt}.
     *
     * @param id идентификатор ресурса без папки {@code structure/} и расширения.
     * @return новая структура этой платформы.
     * @throws java.io.IOException           если файл не найден или повреждён.
     * @throws IllegalArgumentException      если идентификатор некорректен.
     * @throws UnsupportedOperationException если версия не умеет читать структуры.
     */
    default StructureNode loadStructure(String id) throws java.io.IOException {
        throw unsupported("Structure files");
    }

    /**
     * Устанавливает платформу текущего клиента. Вызывается при старте клиентского рантайма.
     *
     * @param platform платформа или null, чтобы снять установку.
     */
    static void install(ScenePlatform platform) {
        ScenePlatformHolder.current = platform;
    }

    /**
     * Читает установленную платформу. Узлы используют её для границ и выбора мышью.
     *
     * @return платформа или null, если клиентский рантайм её не установил.
     */
    static ScenePlatform current() {
        return ScenePlatformHolder.current;
    }

    /**
     * Создаёт ошибку для вида контента, которого нет в этой версии.
     *
     * @param kind вид контента.
     * @return исключение для броска.
     */
    private UnsupportedOperationException unsupported(String kind) {
        return new UnsupportedOperationException(kind + " are not supported in 3D scenes by "
                + getClass().getSimpleName() + " yet");
    }
}

/**
 * Хранилище установленной платформы.
 */
final class ScenePlatformHolder {
    /**
     * Платформа текущего клиента или null.
     */
    static volatile ScenePlatform current;

    private ScenePlatformHolder() {
    }
}

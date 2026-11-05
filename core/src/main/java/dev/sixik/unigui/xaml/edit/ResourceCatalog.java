package dev.sixik.unigui.xaml.edit;

import java.util.List;

/**
 * Список ресурсов, которые можно выбрать в значении свойства: текстуры, шрифты, шейдеры проекта.
 */
@FunctionalInterface
public interface ResourceCatalog {
    /**
     * Каталог без ресурсов.
     */
    ResourceCatalog NONE = folder -> List.of();

    /**
     * @param folder папка вида {@code textures/}, {@code fonts/} или {@code shaders/}
     * @return идентификаторы ресурсов внутри папки (без самой папки), по алфавиту
     */
    List<String> ids(String folder);
}

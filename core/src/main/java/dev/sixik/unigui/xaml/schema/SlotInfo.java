package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.widget.Widget;

/**
 * Именованный «слот» с вложенными виджетами: {@code <TabControl.Tabs>}, {@code <ScrollView.Content>}.
 *
 * @param name        имя слота после точки
 * @param sink        правило добавления ребёнка
 * @param displayName заголовок для инспектора
 * @param category    категория в инспекторе
 * @param description описание
 * @param single      {@code true}, если слот принимает только один виджет
 * @param template    не {@code null}, если слот принимает шаблон, а не готовые виджеты
 * @param <T>         тип родителя
 */
public record SlotInfo<T extends Widget>(
        String name,
        ChildSink<T> sink,
        String displayName,
        String category,
        String description,
        boolean single,
        TemplateSlot<T> template) {

    public SlotInfo {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("slot name must not be blank");
        if (name.indexOf('.') >= 0) throw new IllegalArgumentException("slot name must not contain '.': " + name);
        if (sink == null && template == null) throw new IllegalArgumentException("slot '" + name + "' needs a child sink");
        name = name.trim();
        displayName = displayName == null || displayName.isBlank() ? name : displayName.trim();
        category = category == null || category.isBlank() ? "Content" : category.trim();
        description = description == null ? "" : description.trim();
    }

    /**
     * Создаёт слот, принимающий несколько виджетов.
     *
     * @param name        имя слота
     * @param sink        правило добавления
     * @param description описание
     * @param <T>         тип родителя
     * @return слот
     */
    public static <T extends Widget> SlotInfo<T> many(String name, ChildSink<T> sink, String description) {
        return new SlotInfo<>(name, sink, null, null, description, false, null);
    }

    /**
     * Создаёт слот, принимающий ровно один виджет.
     *
     * @param name        имя слота
     * @param sink        правило добавления
     * @param description описание
     * @param <T>         тип родителя
     * @return слот
     */
    public static <T extends Widget> SlotInfo<T> single(String name, ChildSink<T> sink, String description) {
        return new SlotInfo<>(name, sink, null, null, description, true, null);
    }

    /**
     * Создаёт слот, принимающий один шаблон: виджеты из него создаются позже и многократно.
     *
     * @param name        имя слота
     * @param template    правило приёма и чтения шаблона
     * @param description описание
     * @param <T>         тип родителя
     * @return слот
     */
    public static <T extends Widget> SlotInfo<T> template(String name, TemplateSlot<T> template, String description) {
        return new SlotInfo<>(name, null, null, null, description, true, template);
    }
}

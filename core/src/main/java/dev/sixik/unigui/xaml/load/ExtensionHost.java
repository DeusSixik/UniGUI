package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.xaml.UiValue;

/**
 * Возможности загрузчика, которые нужны расширениям разметки сверх записи значения в свойство.
 */
public interface ExtensionHost {
    /**
     * Вычисляет значение аргумента расширения: текст как есть, вложенное расширение ({@code {Item id}},
     * {@code {Param Title}}, {@code {StaticResource Key}}) через его реализацию.
     *
     * @param value аргумент
     * @return значение; может быть {@code null}
     * @throws IllegalArgumentException если вложенное расширение неизвестно или не годится для вычисления
     */
    Object evaluate(UiValue value);

    /**
     * Привязывает значение параметра к команде и возвращает имя, под которым привязанная команда доступна
     * свойству-команде текущей загрузки.
     *
     * @param command   имя зарегистрированной команды
     * @param parameter значение параметра
     * @return имя привязанной команды для {@link PropertyTarget#setText(String)}
     * @throws IllegalArgumentException если команды нет или она не принимает параметр
     */
    String bindCommand(String command, Object parameter);
}

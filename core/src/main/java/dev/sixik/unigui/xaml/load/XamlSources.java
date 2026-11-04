package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.xaml.UiValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Запоминает, каким выражением свойство было задано в документе, чтобы живой виджет можно было сохранить обратно без
 * потери смысла.
 *
 * <p>Привязка {@code {Binding user.name}} в виджете превращается в текущее значение и подписку: выражения в нём
 * не остаётся. Команда {@code onClick="shop.buy"} превращается в обработчик. Без этой памяти сохранение записало бы
 * значение привязки на момент сохранения, а команду потеряло бы совсем. Здесь хранится только то, что иначе не
 * восстановить: выражения привязок и команд и значения свойств, которые нельзя прочитать с виджета.</p>
 *
 * <p>Таблица слабая: запись живёт, пока жив виджет.</p>
 */
public final class XamlSources {
    private static final Map<Widget, Map<String, UiValue>> SOURCES = Collections.synchronizedMap(new WeakHashMap<>());

    private XamlSources() {
    }

    /**
     * Запоминает исходное значение свойства.
     *
     * @param widget   виджет
     * @param property имя свойства
     * @param value    исходная запись из документа
     */
    public static void record(Widget widget, String property, UiValue value) {
        SOURCES.computeIfAbsent(widget, key -> new LinkedHashMap<>()).put(property, value);
    }

    /**
     * Забывает исходное значение: свойство задали из кода, и прежнее выражение больше не верно.
     *
     * @param widget   виджет
     * @param property имя свойства
     */
    public static void forget(Widget widget, String property) {
        Map<String, UiValue> found = SOURCES.get(widget);
        if (found != null) found.remove(property);
    }

    /**
     * @param widget   виджет
     * @param property имя свойства
     * @return исходная запись или пусто
     */
    public static Optional<UiValue> find(Widget widget, String property) {
        Map<String, UiValue> found = SOURCES.get(widget);
        return found == null ? Optional.empty() : Optional.ofNullable(found.get(property));
    }

    /**
     * Нужно ли запоминать значение: выражения привязок и команд, а также любые значения, которые с виджета не
     * прочитать.
     *
     * @param value    значение из документа
     * @param readable {@code true}, если свойство можно прочитать с виджета
     * @return {@code true}, если значение стоит запомнить
     */
    static boolean worthKeeping(UiValue value, boolean readable) {
        if (value instanceof UiValue.Extension extension) {
            String name = extension.name().local();
            return name.equals("Binding") || name.equals("Command");
        }
        return value instanceof UiValue.Literal && !readable;
    }
}

package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.xml.XmlWidgetLoader;
import dev.sixik.unigui.widgets.containers.ScrollView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Фасад для создания обычных деревьев виджетов UniGUI из XML.
 *
 * <p>Содержит удобные перегрузки для строк, UTF-8 потоков и classpath-ресурсов,
 * а также вспомогательные методы поиска виджетов по {@code id} после загрузки.
 * Все методы материализуют XML сразу в runtime-виджеты, то есть не сохраняют
 * исходный документ. Для редакторских сценариев с сохранением комментариев,
 * строк и undo-команд используй {@link XmlWidgetDocument}.</p>
 *
 * <p>Минимальный runtime-путь:</p>
 *
 * <pre>{@code
 * Widget root = XMLWidget.create("""
 *         <VBox id="root" spacing="4">
 *             <Button id="save" text="Save" />
 *         </VBox>
 *         """);
 * Button save = XMLWidget.getWidget(root, "save", Button.class);
 * }</pre>
 *
 * <p>Если XML использует пользовательские виджеты, передай собственный
 * {@link XmlWidgetRegistry}. Если XML использует текстуры или команды, настрой
 * {@link XmlWidgetOptions}.</p>
 */
public final class XMLWidget {
    private XMLWidget() {
    }

    /**
     * Создаёт новый реестр со встроенными XML-виджетами UniGUI.
     *
     * @return независимый built-in registry с применёнными глобальными contributions
     */
    public static XmlWidgetRegistry registry() {
        return XmlWidgetRegistry.builtIns();
    }

    /**
     * Создаёт пустой XML-реестр.
     *
     * @return registry без built-in типов, удобный для тестов и ограниченных DSL
     */
    public static XmlWidgetRegistry emptyRegistry() {
        return XmlWidgetRegistry.empty();
    }

    /**
     * Загружает XML-строку через стандартный built-in registry и строгие настройки.
     *
     * @param xml исходный XML виджетов
     * @return корневой виджет документа
     */
    public static Widget create(String xml) {
        return create(xml, registry(), XmlWidgetOptions.DEFAULT);
    }

    /**
     * Загружает XML-строку через стандартный registry и явные настройки.
     *
     * @param xml исходный XML виджетов
     * @param options настройки loader'а; {@code null} заменяется дефолтными
     * @return корневой виджет документа
     */
    public static Widget create(String xml, XmlWidgetOptions options) {
        return create(xml, registry(), options);
    }

    /**
     * Загружает XML-строку, предварительно изменив дефолтные настройки.
     *
     * <p>Эта перегрузка удобна для коротких вызовов вида
     * {@code XMLWidget.create(xml, options -> options.strictAttributes(false))}.</p>
     *
     * @param xml исходный XML виджетов
     * @param options функция изменения {@link XmlWidgetOptions#DEFAULT}; может быть {@code null}
     * @return корневой виджет документа
     */
    public static Widget create(String xml, UnaryOperator<XmlWidgetOptions> options) {
        return create(xml, applyOptions(options));
    }

    /**
     * Загружает XML-строку через указанный registry и дефолтные настройки.
     *
     * @param xml исходный XML виджетов
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @return корневой виджет документа
     */
    public static Widget create(String xml, XmlWidgetRegistry registry) {
        return create(xml, registry, XmlWidgetOptions.DEFAULT);
    }

    /**
     * Загружает XML-строку через указанный registry и настройки.
     *
     * <p>Это базовая перегрузка, на которую сводятся остальные string/input/resource
     * методы. Ошибки парсинга, неизвестные типы и ошибки применения атрибутов
     * выбрасываются как {@link XmlWidgetLoadException}.</p>
     *
     * @param xml исходный XML виджетов
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @param options настройки loader'а; {@code null} заменяется дефолтными
     * @return корневой виджет документа
     */
    public static Widget create(String xml, XmlWidgetRegistry registry, XmlWidgetOptions options) {
        return loader(registry, options).load(xml);
    }

    /**
     * Загружает XML-строку и проверяет runtime-тип корневого виджета.
     *
     * @param xml исходный XML виджетов
     * @param widgetType ожидаемый тип корня
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T create(String xml, Class<T> widgetType) {
        return create(xml, widgetType, registry(), XmlWidgetOptions.DEFAULT);
    }

    /**
     * Загружает XML-строку и проверяет тип корневого виджета с явными настройками.
     *
     * @param xml исходный XML виджетов
     * @param widgetType ожидаемый тип корня
     * @param options настройки loader-а; {@code null} заменяется дефолтными
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T create(String xml, Class<T> widgetType, XmlWidgetOptions options) {
        return create(xml, widgetType, registry(), options);
    }

    /**
     * Загружает XML-строку и проверяет тип корня, предварительно изменив дефолтные настройки.
     *
     * @param xml исходный XML виджетов
     * @param widgetType ожидаемый тип корня
     * @param options функция изменения {@link XmlWidgetOptions#DEFAULT}; может быть {@code null}
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T create(String xml, Class<T> widgetType, UnaryOperator<XmlWidgetOptions> options) {
        return create(xml, widgetType, applyOptions(options));
    }

    /**
     * Загружает XML-строку через указанный registry и проверяет тип корня.
     *
     * @param xml исходный XML виджетов
     * @param widgetType ожидаемый тип корня
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T create(String xml, Class<T> widgetType, XmlWidgetRegistry registry) {
        return create(xml, widgetType, registry, XmlWidgetOptions.DEFAULT);
    }

    /**
     * Загружает XML-строку через указанный registry/options и проверяет тип корня.
     *
     * @param xml исходный XML виджетов
     * @param widgetType ожидаемый тип корня
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @param options настройки loader-а; {@code null} заменяется дефолтными
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T create(String xml,
                                             Class<T> widgetType,
                                             XmlWidgetRegistry registry,
                                             XmlWidgetOptions options) {
        return castRoot(create(xml, registry, options), widgetType);
    }

    /**
     * Загружает XML из UTF-8 потока.
     *
     * <p>Метод читает поток полностью, но не закрывает его. Закрытие остаётся на
     * вызывающей стороне, кроме перегрузок {@code createResource(...)}.</p>
     *
     * @param xml поток с XML-текстом в UTF-8
     * @return корневой виджет документа
     */
    public static Widget create(InputStream xml) {
        return create(readUtf8(xml));
    }

    /**
     * Загружает XML из UTF-8 потока с явными настройками.
     *
     * @param xml поток с XML-текстом; метод не закрывает поток
     * @param options настройки loader-а; {@code null} заменяется дефолтными
     * @return корневой виджет документа
     */
    public static Widget create(InputStream xml, XmlWidgetOptions options) {
        return create(readUtf8(xml), options);
    }

    /**
     * Загружает XML из UTF-8 потока, предварительно изменив дефолтные настройки.
     *
     * @param xml поток с XML-текстом; метод не закрывает поток
     * @param options функция изменения {@link XmlWidgetOptions#DEFAULT}; может быть {@code null}
     * @return корневой виджет документа
     */
    public static Widget create(InputStream xml, UnaryOperator<XmlWidgetOptions> options) {
        return create(readUtf8(xml), options);
    }

    /**
     * Загружает XML из UTF-8 потока через указанный registry.
     *
     * @param xml поток с XML-текстом; метод не закрывает поток
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @return корневой виджет документа
     */
    public static Widget create(InputStream xml, XmlWidgetRegistry registry) {
        return create(readUtf8(xml), registry);
    }

    /**
     * Загружает XML из UTF-8 потока через указанный registry/options.
     *
     * @param xml поток с XML-текстом; метод не закрывает поток
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @param options настройки loader-а; {@code null} заменяется дефолтными
     * @return корневой виджет документа
     */
    public static Widget create(InputStream xml, XmlWidgetRegistry registry, XmlWidgetOptions options) {
        return create(readUtf8(xml), registry, options);
    }

    /**
     * Загружает XML из UTF-8 потока и проверяет тип корневого виджета.
     *
     * @param xml поток с XML-текстом; метод не закрывает поток
     * @param widgetType ожидаемый тип корня
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T create(InputStream xml, Class<T> widgetType) {
        return create(readUtf8(xml), widgetType);
    }

    /**
     * Загружает XML из UTF-8 потока и проверяет тип корня с явными настройками.
     *
     * @param xml поток с XML-текстом; метод не закрывает поток
     * @param widgetType ожидаемый тип корня
     * @param options настройки loader-а; {@code null} заменяется дефолтными
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T create(InputStream xml, Class<T> widgetType, XmlWidgetOptions options) {
        return create(readUtf8(xml), widgetType, options);
    }

    /**
     * Загружает XML из UTF-8 потока и проверяет тип корня, предварительно изменив настройки.
     *
     * @param xml поток с XML-текстом; метод не закрывает поток
     * @param widgetType ожидаемый тип корня
     * @param options функция изменения {@link XmlWidgetOptions#DEFAULT}; может быть {@code null}
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T create(InputStream xml,
                                             Class<T> widgetType,
                                             UnaryOperator<XmlWidgetOptions> options) {
        return create(readUtf8(xml), widgetType, options);
    }

    /**
     * Загружает XML из UTF-8 потока через указанный registry и проверяет тип корня.
     *
     * @param xml поток с XML-текстом; метод не закрывает поток
     * @param widgetType ожидаемый тип корня
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T create(InputStream xml, Class<T> widgetType, XmlWidgetRegistry registry) {
        return create(readUtf8(xml), widgetType, registry);
    }

    /**
     * Загружает XML из UTF-8 потока через указанный registry/options и проверяет тип корня.
     *
     * @param xml поток с XML-текстом; метод не закрывает поток
     * @param widgetType ожидаемый тип корня
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @param options настройки loader-а; {@code null} заменяется дефолтными
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T create(InputStream xml,
                                             Class<T> widgetType,
                                             XmlWidgetRegistry registry,
                                             XmlWidgetOptions options) {
        return create(readUtf8(xml), widgetType, registry, options);
    }

    /**
     * Legacy-алиас для {@link #create(String)}.
     *
     * <p>Оставлен для читаемости старого кода, где важно подчеркнуть загрузку
     * именно корневого виджета.</p>
     *
     * @param xml исходный XML виджетов
     * @return корневой виджет документа
     */
    public static Widget createRoot(String xml) {
        return create(xml);
    }

    /**
     * Legacy-алиас для {@link #create(String, XmlWidgetOptions)}.
     *
     * @param xml исходный XML виджетов
     * @param options настройки loader-а
     * @return корневой виджет документа
     */
    public static Widget createRoot(String xml, XmlWidgetOptions options) {
        return create(xml, options);
    }

    /**
     * Legacy-алиас для {@link #create(String, UnaryOperator)}.
     *
     * @param xml исходный XML виджетов
     * @param options функция изменения дефолтных настроек
     * @return корневой виджет документа
     */
    public static Widget createRoot(String xml, UnaryOperator<XmlWidgetOptions> options) {
        return create(xml, options);
    }

    /**
     * Legacy-алиас для {@link #create(String, XmlWidgetRegistry)}.
     *
     * @param xml исходный XML виджетов
     * @param registry реестр XML-типов
     * @return корневой виджет документа
     */
    public static Widget createRoot(String xml, XmlWidgetRegistry registry) {
        return create(xml, registry);
    }

    /**
     * Legacy-алиас для {@link #create(String, Class)}.
     *
     * @param xml исходный XML виджетов
     * @param widgetType ожидаемый тип корня
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T createRoot(String xml, Class<T> widgetType) {
        return create(xml, widgetType);
    }

    /**
     * Legacy-алиас для {@link #create(String, Class, XmlWidgetOptions)}.
     *
     * @param xml исходный XML виджетов
     * @param widgetType ожидаемый тип корня
     * @param options настройки loader-а
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T createRoot(String xml, Class<T> widgetType, XmlWidgetOptions options) {
        return create(xml, widgetType, options);
    }

    /**
     * Legacy-алиас для {@link #create(String, Class, UnaryOperator)}.
     *
     * @param xml исходный XML виджетов
     * @param widgetType ожидаемый тип корня
     * @param options функция изменения дефолтных настроек
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T createRoot(String xml,
                                                 Class<T> widgetType,
                                                 UnaryOperator<XmlWidgetOptions> options) {
        return create(xml, widgetType, options);
    }

    /**
     * Legacy-алиас для {@link #create(String, Class, XmlWidgetRegistry)}.
     *
     * @param xml исходный XML виджетов
     * @param widgetType ожидаемый тип корня
     * @param registry реестр XML-типов
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T createRoot(String xml, Class<T> widgetType, XmlWidgetRegistry registry) {
        return create(xml, widgetType, registry);
    }

    /**
     * Legacy-алиас для {@link #create(InputStream)}.
     *
     * @param xml поток с XML-текстом; метод не закрывает поток
     * @return корневой виджет документа
     */
    public static Widget createRoot(InputStream xml) {
        return create(xml);
    }

    /**
     * Legacy-алиас для {@link #create(InputStream, Class)}.
     *
     * @param xml поток с XML-текстом; метод не закрывает поток
     * @param widgetType ожидаемый тип корня
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T createRoot(InputStream xml, Class<T> widgetType) {
        return create(xml, widgetType);
    }

    /**
     * Загружает XML из classpath-ресурса.
     *
     * <p>{@code resourcePath} ищется через context class loader, затем через
     * class loader {@code XMLWidget}. Ведущий {@code /} допускается и будет
     * отброшен перед поиском.</p>
     *
     * @param resourcePath путь ресурса в classpath
     * @return корневой виджет документа
     */
    public static Widget createResource(String resourcePath) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    /**
     * Загружает XML из classpath-ресурса с явными настройками.
     *
     * @param resourcePath путь ресурса в classpath
     * @param options настройки loader-а; {@code null} заменяется дефолтными
     * @return корневой виджет документа
     */
    public static Widget createResource(String resourcePath, XmlWidgetOptions options) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream, options);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    /**
     * Загружает XML из classpath-ресурса, предварительно изменив дефолтные настройки.
     *
     * @param resourcePath путь ресурса в classpath
     * @param options функция изменения {@link XmlWidgetOptions#DEFAULT}; может быть {@code null}
     * @return корневой виджет документа
     */
    public static Widget createResource(String resourcePath, UnaryOperator<XmlWidgetOptions> options) {
        return createResource(resourcePath, applyOptions(options));
    }

    /**
     * Загружает XML из classpath-ресурса через указанный registry.
     *
     * @param resourcePath путь ресурса в classpath
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @return корневой виджет документа
     */
    public static Widget createResource(String resourcePath, XmlWidgetRegistry registry) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream, registry);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    /**
     * Загружает XML из classpath-ресурса через указанный registry/options.
     *
     * @param resourcePath путь ресурса в classpath
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @param options настройки loader-а; {@code null} заменяется дефолтными
     * @return корневой виджет документа
     */
    public static Widget createResource(String resourcePath, XmlWidgetRegistry registry, XmlWidgetOptions options) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream, registry, options);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    /**
     * Загружает XML из classpath-ресурса и проверяет тип корня.
     *
     * @param resourcePath путь ресурса в classpath
     * @param widgetType ожидаемый тип корня
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T createResource(String resourcePath, Class<T> widgetType) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream, widgetType);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    /**
     * Загружает XML из classpath-ресурса и проверяет тип корня с явными настройками.
     *
     * @param resourcePath путь ресурса в classpath
     * @param widgetType ожидаемый тип корня
     * @param options настройки loader-а; {@code null} заменяется дефолтными
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T createResource(String resourcePath,
                                                     Class<T> widgetType,
                                                     XmlWidgetOptions options) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream, widgetType, options);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    /**
     * Загружает XML из classpath-ресурса и проверяет тип корня, предварительно изменив настройки.
     *
     * @param resourcePath путь ресурса в classpath
     * @param widgetType ожидаемый тип корня
     * @param options функция изменения {@link XmlWidgetOptions#DEFAULT}; может быть {@code null}
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T createResource(String resourcePath,
                                                     Class<T> widgetType,
                                                     UnaryOperator<XmlWidgetOptions> options) {
        return createResource(resourcePath, widgetType, applyOptions(options));
    }

    /**
     * Загружает XML из classpath-ресурса через registry и проверяет тип корня.
     *
     * @param resourcePath путь ресурса в classpath
     * @param widgetType ожидаемый тип корня
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T createResource(String resourcePath,
                                                     Class<T> widgetType,
                                                     XmlWidgetRegistry registry) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream, widgetType, registry);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    /**
     * Загружает XML из classpath-ресурса через registry/options и проверяет тип корня.
     *
     * @param resourcePath путь ресурса в classpath
     * @param widgetType ожидаемый тип корня
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @param options настройки loader-а; {@code null} заменяется дефолтными
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T createResource(String resourcePath,
                                                     Class<T> widgetType,
                                                     XmlWidgetRegistry registry,
                                                     XmlWidgetOptions options) {
        try (InputStream stream = openResource(resourcePath)) {
            return create(stream, widgetType, registry, options);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget resource '" + resourcePath + "'.", failure);
        }
    }

    /**
     * Legacy-алиас для {@link #createResource(String)}.
     *
     * @param resourcePath путь ресурса в classpath
     * @return корневой виджет документа
     */
    public static Widget createRootResource(String resourcePath) {
        return createResource(resourcePath);
    }

    /**
     * Legacy-алиас для {@link #createResource(String, Class)}.
     *
     * @param resourcePath путь ресурса в classpath
     * @param widgetType ожидаемый тип корня
     * @param <T> тип корневого виджета
     * @return корневой виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T createRootResource(String resourcePath, Class<T> widgetType) {
        return createResource(resourcePath, widgetType);
    }

    /**
     * Находит обязательный виджет по {@code id} внутри уже загруженного дерева.
     *
     * <p>Поиск выполняется в глубину и дополнительно учитывает content-слот
     * {@link ScrollView}, который не всегда представлен в обычном списке children.</p>
     *
     * @param root корень дерева поиска
     * @param id значение XML/runtime {@code id}
     * @return найденный виджет
     * @throws XmlWidgetLoadException если виджет не найден
     */
    public static Widget getWidget(Widget root, String id) {
        return findWidget(root, id)
                .orElseThrow(() -> new XmlWidgetLoadException(missingWidgetMessage(root, id)));
    }

    /**
     * Находит обязательный виджет по {@code id} и проверяет его runtime-тип.
     *
     * @param root корень дерева поиска
     * @param id значение XML/runtime {@code id}
     * @param widgetType ожидаемый тип найденного виджета
     * @param <T> тип найденного виджета
     * @return найденный виджет, приведённый к {@code widgetType}
     */
    public static <T extends Widget> T getWidget(Widget root, String id, Class<T> widgetType) {
        Widget widget = getWidget(root, id);
        return castLookup(id, widget, widgetType);
    }

    /**
     * Пытается найти виджет по {@code id} внутри уже загруженного дерева.
     *
     * @param root корень дерева поиска
     * @param id значение XML/runtime {@code id}
     * @return найденный виджет или {@link Optional#empty()}, если id пустой или отсутствует
     */
    public static Optional<Widget> findWidget(Widget root, String id) {
        if (root == null || id == null || id.isBlank()) return Optional.empty();

        ArrayDeque<Widget> stack = new ArrayDeque<>();
        Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        stack.push(root);
        while (!stack.isEmpty()) {
            Widget widget = stack.pop();
            if (widget == null || !visited.add(widget)) continue;
            if (id.equals(widget.id())) return Optional.of(widget);

            List<Widget> children = new ArrayList<>(widget.children());
            if (widget instanceof ScrollView scrollView && scrollView.content() != null) {
                children.add(scrollView.content());
            }
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
        return Optional.empty();
    }

    /**
     * Пытается найти виджет по {@code id} и привести его к ожидаемому типу.
     *
     * @param root корень дерева поиска
     * @param id значение XML/runtime {@code id}
     * @param widgetType ожидаемый тип найденного виджета
     * @param <T> тип найденного виджета
     * @return найденный и приведённый виджет или empty, если id не найден
     */
    public static <T extends Widget> Optional<T> findWidget(Widget root, String id, Class<T> widgetType) {
        return findWidget(root, id).map(widget -> castLookup(id, widget, widgetType));
    }

    private static XmlWidgetLoader loader(XmlWidgetRegistry registry, XmlWidgetOptions options) {
        XmlWidgetRegistry normalizedRegistry = registry == null ? registry() : registry;
        XmlWidgetOptions normalizedOptions = options == null ? XmlWidgetOptions.DEFAULT : options;
        return new XmlWidgetLoader(normalizedRegistry.delegate(), normalizedOptions);
    }

    private static XmlWidgetOptions applyOptions(UnaryOperator<XmlWidgetOptions> options) {
        if (options == null) return XmlWidgetOptions.DEFAULT;
        XmlWidgetOptions applied = options.apply(XmlWidgetOptions.DEFAULT);
        return applied == null ? XmlWidgetOptions.DEFAULT : applied;
    }

    private static <T extends Widget> T castRoot(Widget root, Class<T> widgetType) {
        if (widgetType == null) throw new IllegalArgumentException("widgetType must not be null");
        if (widgetType.isInstance(root)) return widgetType.cast(root);
        throw new XmlWidgetLoadException("XML root is " + simpleName(root) + ", expected " + widgetType.getSimpleName() + ".");
    }

    private static <T extends Widget> T castLookup(String id, Widget widget, Class<T> widgetType) {
        if (widgetType == null) throw new IllegalArgumentException("widgetType must not be null");
        if (widgetType.isInstance(widget)) return widgetType.cast(widget);
        throw new XmlWidgetLoadException("Widget id '" + id + "' exists, but is "
                + simpleName(widget) + ", not " + widgetType.getSimpleName() + ".");
    }

    private static String missingWidgetMessage(Widget root, String id) {
        return "Widget id '" + id + "' was not found under root '" + rootLabel(root) + "'.";
    }

    private static String rootLabel(Widget root) {
        if (root == null) return "null";
        String id = root.id();
        return id == null || id.isBlank() ? simpleName(root) : id;
    }

    private static String simpleName(Widget widget) {
        return widget == null ? "null" : widget.getClass().getSimpleName();
    }

    private static InputStream openResource(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new XmlWidgetLoadException("XML widget resource path must not be blank.");
        }
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = XMLWidget.class.getClassLoader();
        InputStream stream = loader.getResourceAsStream(normalized);
        if (stream == null && loader != XMLWidget.class.getClassLoader()) {
            stream = XMLWidget.class.getClassLoader().getResourceAsStream(normalized);
        }
        if (stream == null) {
            throw new XmlWidgetLoadException("XML widget resource '" + resourcePath + "' was not found.");
        }
        return stream;
    }

    private static String readUtf8(InputStream xml) {
        if (xml == null) throw new XmlWidgetLoadException("XML widget input stream must not be null.");
        try {
            return new String(xml.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot read XML widget input stream.", failure);
        }
    }
}

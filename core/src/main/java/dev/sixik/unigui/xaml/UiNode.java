package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.format.MarkupExtensions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Элемент дерева разметки: тип, свойства и содержимое.
 *
 * <p>Это общий формат для рантайма, редактора и сериализации: XAML, Java-билдеры и скриптовые языки читают и
 * пишут одно и то же дерево. Узел изменяемый, чтобы редактор мог применять команды без перестройки документа;
 * для независимых правок используйте {@link #copy()}.</p>
 *
 * <p>Идентичность узла — ссылочная ({@code equals} не переопределён); для сравнения по содержимому служит
 * {@link #sameStructure(UiNode)}.</p>
 */
public final class UiNode implements UiContent {
    private UiName type;
    private SourceLocation location = SourceLocation.UNKNOWN;
    private final Map<String, String> namespaces = new LinkedHashMap<>();
    private final List<UiProperty> properties = new ArrayList<>();
    private final List<UiContent> content = new ArrayList<>();

    /**
     * Создаёт узел указанного типа.
     *
     * @param type имя типа
     */
    public UiNode(UiName type) {
        this.type = require(type);
    }

    /**
     * Создаёт узел типа без namespace.
     *
     * @param typeName локальное имя типа
     * @return новый узел
     */
    public static UiNode of(String typeName) {
        return new UiNode(UiName.of(typeName));
    }

    /**
     * Возвращает тип узла.
     *
     * @return имя типа
     */
    public UiName type() {
        return type;
    }

    /**
     * Меняет тип узла.
     *
     * @param type новое имя типа
     * @return этот узел
     */
    public UiNode type(UiName type) {
        this.type = require(type);
        return this;
    }

    /**
     * Возвращает позицию начала элемента в исходном тексте.
     *
     * @return позиция или {@link SourceLocation#UNKNOWN}
     */
    public SourceLocation location() {
        return location;
    }

    /**
     * Запоминает позицию узла (используется читателями формата).
     *
     * @param location позиция
     * @return этот узел
     */
    public UiNode location(SourceLocation location) {
        this.location = location == null ? SourceLocation.UNKNOWN : location;
        return this;
    }

    /**
     * Возвращает namespace-объявления, сделанные на этом узле, в порядке записи.
     *
     * @return неизменяемое отображение {@code prefix -> uri}; ключ {@code ""} — namespace по умолчанию
     */
    public Map<String, String> namespaces() {
        return Collections.unmodifiableMap(namespaces);
    }

    /**
     * Объявляет namespace на этом узле.
     *
     * @param prefix префикс; пустая строка для namespace по умолчанию
     * @param uri    URI пространства имён
     * @return этот узел
     */
    public UiNode declareNamespace(String prefix, String uri) {
        namespaces.put(prefix == null ? "" : prefix, uri == null ? "" : uri);
        return this;
    }

    /**
     * Возвращает свойства в порядке записи.
     *
     * @return неизменяемое представление
     */
    public List<UiProperty> properties() {
        return Collections.unmodifiableList(properties);
    }

    /**
     * Ищет обычный атрибут без владельца и без namespace.
     *
     * @param name имя члена
     * @return свойство, если оно задано
     */
    public Optional<UiProperty> property(String name) {
        return property(null, UiName.of(name));
    }

    /**
     * Ищет свойство по ключу (владелец + имя).
     *
     * @param owner владелец или {@code null}
     * @param name  имя члена
     * @return свойство, если оно задано
     */
    public Optional<UiProperty> property(UiName owner, UiName name) {
        for (UiProperty property : properties) {
            if (property.sameKey(owner, name)) return Optional.of(property);
        }
        return Optional.empty();
    }

    /**
     * Возвращает значение обычного атрибута как исходный текст.
     *
     * <p>Удобно для форматов, где значения не бывают выражениями (стили, storyboard): если значение случайно
     * записано как {@code {...}}, оно возвращается в той форме, в какой было написано, а не теряется.</p>
     *
     * @param name имя атрибута без префикса и владельца
     * @return текст или пусто, если атрибута нет или он задан вложенными элементами
     */
    public Optional<String> attributeText(String name) {
        for (UiProperty property : properties) {
            if (property.owner() == null && property.name().namespace().isEmpty()
                    && property.name().local().equals(name)) {
                if (property.value() instanceof UiValue.Elements) return Optional.empty();
                return Optional.of(MarkupExtensions.write(property.value()));
            }
        }
        return Optional.empty();
    }

    /**
     * То же, что {@link #attributeText(String)}, но с запасным значением.
     *
     * @param name     имя атрибута
     * @param fallback значение, если атрибута нет
     * @return текст атрибута или {@code fallback}
     */
    public String attributeText(String name, String fallback) {
        return attributeText(name).orElse(fallback);
    }

    /**
     * Добавляет свойство или заменяет существующее с тем же ключом, сохраняя его место в порядке.
     *
     * @param property новое свойство
     * @return этот узел
     */
    public UiNode set(UiProperty property) {
        if (property == null) throw new IllegalArgumentException("property must not be null");
        for (int i = 0; i < properties.size(); i++) {
            if (properties.get(i).sameKey(property)) {
                properties.set(i, property);
                return this;
            }
        }
        properties.add(property);
        return this;
    }

    /**
     * Возвращает позицию свойства среди всех свойств узла.
     *
     * @param owner владелец или {@code null}
     * @param name  имя члена
     * @return индекс или {@code -1}, если свойства нет
     */
    public int propertyIndex(UiName owner, UiName name) {
        for (int i = 0; i < properties.size(); i++) {
            if (properties.get(i).sameKey(owner, name)) return i;
        }
        return -1;
    }

    /**
     * Вставляет свойство на заданную позицию. Если свойство с таким ключом уже есть, оно заменяется на месте.
     * Нужно для точного отката правок: удалённое свойство возвращается туда, где стояло.
     *
     * @param index    позиция; значение за пределами списка приводится к границе
     * @param property свойство
     * @return этот узел
     */
    public UiNode insert(int index, UiProperty property) {
        if (property == null) throw new IllegalArgumentException("property must not be null");
        int existing = propertyIndex(property.owner(), property.name());
        if (existing >= 0) {
            properties.set(existing, property);
            return this;
        }
        properties.add(Math.max(0, Math.min(index, properties.size())), property);
        return this;
    }

    /**
     * Задаёт обычный атрибут со строковым значением.
     *
     * @param name  имя
     * @param value значение
     * @return этот узел
     */
    public UiNode set(String name, String value) {
        return set(UiProperty.attribute(name, value));
    }

    /**
     * Удаляет свойство по ключу.
     *
     * @param owner владелец или {@code null}
     * @param name  имя члена
     * @return {@code true}, если свойство было удалено
     */
    public boolean remove(UiName owner, UiName name) {
        return properties.removeIf(property -> property.sameKey(owner, name));
    }

    /**
     * Возвращает всё содержимое в порядке записи, включая текст и комментарии.
     *
     * @return неизменяемое представление
     */
    public List<UiContent> content() {
        return Collections.unmodifiableList(content);
    }

    /**
     * Возвращает только вложенные узлы.
     *
     * @return новый список дочерних узлов
     */
    public List<UiNode> children() {
        List<UiNode> nodes = new ArrayList<>();
        for (UiContent item : content) {
            if (item instanceof UiNode node) nodes.add(node);
        }
        return nodes;
    }

    /**
     * Добавляет элемент содержимого в конец.
     *
     * @param item узел, текст или комментарий
     * @return этот узел
     */
    public UiNode add(UiContent item) {
        if (item == null) throw new IllegalArgumentException("content item must not be null");
        content.add(item);
        return this;
    }

    /**
     * Вставляет элемент содержимого на позицию.
     *
     * @param index позиция среди {@link #content()}
     * @param item  узел, текст или комментарий
     * @return этот узел
     */
    public UiNode add(int index, UiContent item) {
        if (item == null) throw new IllegalArgumentException("content item must not be null");
        content.add(index, item);
        return this;
    }

    /**
     * Удаляет элемент содержимого по ссылке.
     *
     * @param item удаляемый элемент
     * @return {@code true}, если элемент был найден
     */
    public boolean remove(UiContent item) {
        for (int i = 0; i < content.size(); i++) {
            if (content.get(i) == item) {
                content.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Возвращает склеенный прямой текст узла (без вложенных элементов).
     *
     * @return текст; пустая строка, если текста нет
     */
    public String text() {
        StringBuilder builder = new StringBuilder();
        for (UiContent item : content) {
            if (item instanceof UiContent.Text text) builder.append(text.value());
        }
        return builder.toString();
    }

    /**
     * Создаёт глубокую копию узла вместе с позициями.
     *
     * @return независимая копия
     */
    public UiNode copy() {
        UiNode copy = new UiNode(type).location(location);
        copy.namespaces.putAll(namespaces);
        for (UiProperty property : properties) {
            copy.properties.add(new UiProperty(property.name(), property.owner(), copyValue(property.value()),
                    property.form(), property.location()));
        }
        for (UiContent item : content) {
            copy.content.add(item instanceof UiNode node ? node.copy() : item);
        }
        return copy;
    }

    /**
     * Сравнивает деревья по смыслу: типы, свойства (с порядком) и содержимое. Позиции, префиксы, пробельный
     * текст между элементами и пробелы по краям текста, соседствующего с элементами, не учитываются.
     *
     * @param other другой узел
     * @return {@code true}, если деревья эквивалентны
     */
    public boolean sameStructure(UiNode other) {
        if (other == null || !type.sameAs(other.type) || properties.size() != other.properties.size()) return false;
        for (int i = 0; i < properties.size(); i++) {
            UiProperty left = properties.get(i);
            UiProperty right = other.properties.get(i);
            if (!left.sameKey(right) || !valueSame(left.value(), right.value())) return false;
        }
        return contentSame(content, other.content);
    }

    private static boolean contentSame(List<UiContent> left, List<UiContent> right) {
        List<UiContent> a = significant(left);
        List<UiContent> b = significant(right);
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            UiContent x = a.get(i);
            UiContent y = b.get(i);
            if (x instanceof UiNode nx) {
                if (!(y instanceof UiNode ny) || !nx.sameStructure(ny)) return false;
            } else if (!x.equals(y)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Приводит содержимое к виду, пригодному для сравнения. Если рядом с текстом есть элементы или комментарии,
     * пробельный текст между ними — форматирование и отбрасывается, а пробелы по краям значимого текста обрезаются.
     * Текст без соседей-элементов сравнивается как есть.
     */
    private static List<UiContent> significant(List<UiContent> items) {
        boolean structural = false;
        for (UiContent item : items) {
            if (!(item instanceof UiContent.Text)) {
                structural = true;
                break;
            }
        }
        if (!structural) return items;
        List<UiContent> result = new ArrayList<>(items.size());
        for (UiContent item : items) {
            if (item instanceof UiContent.Text text) {
                if (!text.blank()) result.add(new UiContent.Text(text.value().trim()));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    private static boolean valueSame(UiValue left, UiValue right) {
        if (left instanceof UiValue.Elements a && right instanceof UiValue.Elements b) {
            return contentSame(a.content(), b.content());
        }
        if (left instanceof UiValue.Extension a && right instanceof UiValue.Extension b) {
            if (!a.name().sameAs(b.name()) || a.positional().size() != b.positional().size()
                    || !a.named().keySet().equals(b.named().keySet())) {
                return false;
            }
            for (int i = 0; i < a.positional().size(); i++) {
                if (!valueSame(a.positional().get(i), b.positional().get(i))) return false;
            }
            for (Map.Entry<String, UiValue> entry : a.named().entrySet()) {
                if (!valueSame(entry.getValue(), b.named().get(entry.getKey()))) return false;
            }
            return true;
        }
        return left.equals(right);
    }

    private static UiValue copyValue(UiValue value) {
        if (value instanceof UiValue.Elements elements) {
            List<UiContent> copy = new ArrayList<>(elements.content().size());
            for (UiContent item : elements.content()) copy.add(item instanceof UiNode node ? node.copy() : item);
            return new UiValue.Elements(copy);
        }
        return value;
    }

    private static UiName require(UiName type) {
        if (type == null) throw new IllegalArgumentException("node type must not be null");
        return type;
    }

    @Override
    public String toString() {
        return "UiNode<" + type.qualified() + ">@" + location;
    }
}

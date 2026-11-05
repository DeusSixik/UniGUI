package dev.sixik.unigui.xaml.component;

import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.Edits;
import dev.sixik.unigui.xaml.schema.LoadContext;
import dev.sixik.unigui.xaml.schema.PropertyInfo;
import dev.sixik.unigui.xaml.schema.TypeInfo;
import dev.sixik.unigui.xaml.schema.ValueKind;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Превращает свойство виджета тела компонента в значение контракта: параметр ({@code {Param X}}) или переменную
 * ({@code {Var X}}), к которым можно обращаться из нод графа. Числа, логические значения, цвета и текстуры сохраняют
 * свой тип; перечисления, размеры и отступы хранятся текстом в записи свойства ({@code auto}, {@code 50%}, {@code Row}).
 *
 * <p>Текущее значение свойства становится значением по умолчанию, поэтому вид компонента не меняется. Всё действие —
 * одна транзакция сессии: один шаг отмены.</p>
 */
public final class Promotions {
    /**
     * Чем становится свойство.
     */
    public enum Kind {
        /**
         * Параметр: виден снаружи как свойство тега компонента.
         */
        PARAM("Param"),
        /**
         * Переменная: внутреннее состояние компонента.
         */
        VARIABLE("Var");

        private final String element;

        Kind(String element) {
            this.element = element;
        }

        /**
         * @return имя элемента контракта и расширения разметки
         */
        public String element() {
            return element;
        }
    }

    /**
     * Типы, в которые можно превратить свойство; порядок важен: первым берётся самый точный.
     */
    private static final List<ParamType> CANDIDATES = List.of(ParamType.BOOL, ParamType.INT, ParamType.FLOAT,
            ParamType.DOUBLE, ParamType.COLOR, ParamType.TEXTURE);

    private Promotions() {
    }

    /**
     * @param session  сессия
     * @param node     узел документа
     * @param property имя свойства
     * @return {@code true}, если свойство можно превратить: это значение виджета тела компонента, ещё не привязанное
     */
    public static boolean supported(EditorSession session, UiNode node, String property) {
        UiNode root = session.document().root();
        if (!root.type().local().equals("Component") || node == root || binding(node, property).isPresent())
            return false;
        if (property.equals("id") || property.equals("class")) return false;
        return info(session, node, property).filter(found -> found.kind() != ValueKind.BINDING_OR_ACTION)
                .flatMap(found -> typeFor(found.valueType())).isPresent();
    }

    /**
     * @param node     узел документа
     * @param property имя свойства
     * @return имя параметра или переменной, к которой привязано свойство
     */
    public static Optional<String> binding(UiNode node, String property) {
        return extension(node, property).flatMap(use -> use.positional().isEmpty() ? Optional.empty()
                : use.positional().get(0) instanceof UiValue.Literal name ? Optional.of(name.text().trim())
                : Optional.empty());
    }

    /**
     * Создаёт значение контракта и привязывает к нему свойство.
     *
     * @param session  сессия документа компонента
     * @param node     узел тела
     * @param property имя свойства
     * @param kind     параметр или переменная
     * @return имя созданного значения или пусто, если свойство превратить нельзя
     */
    public static Optional<String> promote(EditorSession session, UiNode node, String property, Kind kind) {
        if (!supported(session, node, property)) return Optional.empty();
        UiNode root = session.document().root();
        PropertyInfo<?, ?> info = info(session, node, property).orElseThrow();
        ParamType type = typeFor(info.valueType()).orElseThrow();
        String name = freshName(root, node, property);
        String defaultText = defaultText(node, property, info, type);
        boolean done = session.transaction("Promote " + property + " to " + name, () -> {
            boolean added = session.apply(kind == Kind.PARAM
                    ? ContractEdits.addParam(root, name, type.name(), defaultText)
                    : ContractEdits.addVar(root, name, type.name(), defaultText));
            return added && session.apply(Edits.setProperty(node, null, UiName.of(property), new UiValue.Extension(
                    UiName.of(kind.element()), List.of(new UiValue.Literal(name)), Map.of())));
        });
        return done ? Optional.of(name) : Optional.empty();
    }

    /**
     * @param session  сессия документа компонента
     * @param node     узел тела
     * @param property имя свойства
     * @param kind     параметр или переменная
     * @param name     имя записи контракта
     * @return {@code true}, если значение этого типа можно записать в свойство
     */
    public static boolean canBind(EditorSession session, UiNode node, String property, Kind kind, String name) {
        UiNode root = session.document().root();
        if (!root.type().local().equals("Component") || node == root || property.equals("id") || property.equals("class")) {
            return false;
        }
        PropertyInfo<?, ?> info = info(session, node, property).orElse(null);
        UiNode entry = entry(root, kind.element(), name);
        if (info == null || entry == null || info.kind() == ValueKind.BINDING_OR_ACTION) return false;
        try {
            ParamType type = ParamType.parse(entry.attributeText("Type", ""));
            return type.fits(info.valueType()) || (type == ParamType.STRING && ParamType.needsText(info.valueType()));
        } catch (IllegalArgumentException unknownType) {
            return false;
        }
    }

    /**
     * Привязывает свойство к существующей записи контракта.
     *
     * @param session  сессия
     * @param node     узел тела
     * @param property имя свойства
     * @param kind     параметр или переменная
     * @param name     имя записи
     * @return {@code true}, если привязано
     */
    public static boolean bind(EditorSession session, UiNode node, String property, Kind kind, String name) {
        return canBind(session, node, property, kind, name) && session.apply(Edits.setProperty(node, null,
                UiName.of(property), new UiValue.Extension(UiName.of(kind.element()), List.of(new UiValue.Literal(name)), Map.of())));
    }

    /**
     * @param session  сессия
     * @param node     узел тела
     * @param property имя свойства-действия
     * @param event    имя события
     * @return {@code true}, если действие может поднимать это событие: только исходящее и только на свойстве-действии
     */
    public static boolean canRaise(EditorSession session, UiNode node, String property, String event) {
        UiNode root = session.document().root();
        if (!root.type().local().equals("Component") || node == root) return false;
        UiNode entry = entry(root, "Event", event);
        return entry != null && entry.attributeText("Direction", "Out").equals("Out")
                && info(session, node, property).filter(found -> found.kind() == ValueKind.BINDING_OR_ACTION
                && found.valueType() == String.class).isPresent();
    }

    /**
     * Назначает действию свойства исходящее событие: {@code onClick="{Raise Buy}"}.
     *
     * @param session  сессия
     * @param node     узел тела
     * @param property имя свойства-действия
     * @param event    имя события
     * @return {@code true}, если назначено
     */
    public static boolean raise(EditorSession session, UiNode node, String property, String event) {
        return canRaise(session, node, property, event) && session.apply(Edits.setProperty(node, null, UiName.of(property),
                new UiValue.Extension(UiName.of("Raise"), List.of(new UiValue.Literal(event)), Map.of())));
    }

    private static UiNode entry(UiNode root, String element, String name) {
        for (var item : dev.sixik.unigui.xaml.edit.ElementListEdit.items(root, "Component", "Contract")) {
            if (item instanceof UiNode node && node.type().local().equals(element)
                    && name.equals(node.attributeText("Name", ""))) return node;
        }
        return null;
    }

    /**
     * Снимает привязку: свойство снова хранит обычное значение, равное значению по умолчанию из контракта. Запись
     * контракта остаётся, потому что на неё могут ссылаться ноды и другие свойства.
     *
     * @param session  сессия
     * @param node     узел тела
     * @param property имя свойства
     * @return {@code true}, если привязка снята
     */
    public static boolean unbind(EditorSession session, UiNode node, String property) {
        String name = binding(node, property).orElse(null);
        if (name == null) return false;
        String defaultText = "";
        for (var item : dev.sixik.unigui.xaml.edit.ElementListEdit.items(session.document().root(), "Component", "Contract")) {
            if (item instanceof UiNode entry && name.equals(entry.attributeText("Name", ""))) {
                defaultText = entry.attributeText("Default", "");
            }
        }
        return session.apply(defaultText.isEmpty() ? Edits.removeProperty(node, property)
                : Edits.setProperty(node, null, UiName.of(property), new UiValue.Literal(defaultText)));
    }

    // ------------------------------------------------------------------ внутреннее

    private static Optional<UiValue.Extension> extension(UiNode node, String property) {
        for (UiProperty candidate : node.properties()) {
            if (candidate.name().local().equals(property) && candidate.value() instanceof UiValue.Extension use
                    && (use.name().local().equals("Param") || use.name().local().equals("Var"))) {
                return Optional.of(use);
            }
        }
        return Optional.empty();
    }

    private static Optional<PropertyInfo<?, ?>> info(EditorSession session, UiNode node, String property) {
        TypeInfo<?> type = session.schema().type(node.type()).orElse(null);
        if (type == null) return Optional.empty();
        for (PropertyInfo<?, ?> candidate : type.properties()) {
            if (candidate.name().equals(property)) return Optional.of(candidate);
        }
        return Optional.empty();
    }

    /**
     * Тип контракта для класса значения свойства; значения без своего типа хранятся текстом ({@link ParamType#needsText}).
     */
    private static Optional<ParamType> typeFor(Class<?> wanted) {
        if (wanted == String.class || ParamType.needsText(wanted)) return Optional.of(ParamType.STRING);
        if (wanted == Object.class) return Optional.empty();
        for (ParamType type : CANDIDATES) {
            if (type.javaType() == wanted || wanted.isAssignableFrom(type.javaType())) return Optional.of(type);
        }
        return Optional.empty();
    }

    private static String defaultText(UiNode node, String property, PropertyInfo<?, ?> info, ParamType type) {
        String text = node.attributeText(property).orElse(info.defaultValue());
        if (text == null || text.isEmpty()) return null;
        try {
            if (ParamType.needsText(info.valueType())) info.convert(text, LoadContext.DEFAULT);
            else type.parse(text, LoadContext.DEFAULT);
            return text;
        } catch (RuntimeException notThisType) {
            return null;
        }
    }

    /**
     * Имя из идентификатора виджета и свойства: {@code volume} + {@code value} дают {@code VolumeValue}.
     */
    private static String freshName(UiNode root, UiNode node, String property) {
        String owner = node.attributeText("id").filter(id -> !id.isBlank()).orElse(node.type().local());
        String base = capitalize(owner.replaceAll("[^A-Za-z0-9_]", "")) + capitalize(property);
        java.util.Set<String> used = new java.util.HashSet<>();
        for (var item : dev.sixik.unigui.xaml.edit.ElementListEdit.items(root, "Component", "Contract")) {
            if (item instanceof UiNode entry) entry.attributeText("Name").ifPresent(used::add);
        }
        if (!used.contains(base)) return base;
        for (int i = 2; ; i++) {
            if (!used.contains(base + i)) return base + i;
        }
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}

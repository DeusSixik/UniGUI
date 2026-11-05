package dev.sixik.unigui.xaml.component;

import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.edit.Edit;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.Edits;
import dev.sixik.unigui.xaml.edit.ElementListEdit;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.logic.GraphCompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Переименование с обновлением ссылок: параметр, переменная и событие контракта компонента меняют имя везде, где на
 * них ссылаются, — в контракте, в {@code {Param}} и {@code {Raise}} тела и в нодах графа.
 *
 * <p>Всё переименование — одна транзакция сессии: один шаг отмены. Промежуточные состояния (контракт уже новый,
 * ссылки ещё старые) не проверяются; проверяется итог: документ должен разбираться, а граф — собираться в мягком
 * режиме. Если итог неверен (имя занято, не идентификатор), документ остаётся прежним, а причина доступна в
 * {@link EditorSession.Channel#EDIT}.</p>
 */
public final class Renames {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private Renames() {
    }

    /**
     * Переименовывает параметр.
     *
     * @param session сессия документа компонента
     * @param oldName прежнее имя
     * @param newName новое имя
     * @return {@code true}, если переименовано
     */
    public static boolean param(EditorSession session, String oldName, String newName) {
        return rename(session, "Param", oldName, newName, "Param", List.of("Param.Get", "Param.Set"), List.of());
    }

    /**
     * Переименовывает переменную графа.
     *
     * @param session сессия документа компонента
     * @param oldName прежнее имя
     * @param newName новое имя
     * @return {@code true}, если переименовано
     */
    public static boolean variable(EditorSession session, String oldName, String newName) {
        return rename(session, "Var", oldName, newName, "Var", List.of("Var.Get", "Var.Set"), List.of());
    }

    /**
     * Переименовывает событие: входящее ({@code Event.Имя} в графе) или исходящее ({@code {Raise}} и
     * {@code Raise.Имя}).
     *
     * @param session сессия документа компонента
     * @param oldName прежнее имя
     * @param newName новое имя
     * @return {@code true}, если переименовано
     */
    public static boolean event(EditorSession session, String oldName, String newName) {
        return rename(session, "Event", oldName, newName, "Raise", List.of(), List.of("Event.", "Raise."));
    }

    /**
     * Переименовывает пользовательское событие графа: объявление и все ноды, которые его вызывают или подписывают.
     *
     * @param session сессия документа компонента
     * @param oldName прежнее имя
     * @param newName новое имя
     * @return {@code true}, если переименовано
     */
    public static boolean customEvent(EditorSession session, String oldName, String newName) {
        UiNode root = session.document().root();
        if (!root.type().local().equals("Component")) return fail(session, "Only a component has a graph.");
        if (oldName.equals(newName)) return true;
        if (!IDENTIFIER.matcher(newName).matches()) {
            return fail(session, "'" + newName + "' is not a valid name. Use letters, digits and underscores.");
        }
        String[] why = {""};
        boolean done = session.transaction("Rename " + oldName + " to " + newName, () -> {
            Edit edit = ElementListEdit.of(root, "Component", "Graph", "Rename custom event", items -> {
                for (int i = 0; i < items.size(); i++) {
                    if (!(items.get(i) instanceof UiNode node) || !node.type().local().equals("Node")) continue;
                    String type = node.attributeText("Type", "");
                    boolean named = (type.equals("Event.Custom") || type.equals("Event.Call"))
                            && oldName.equals(node.attributeText("Name", ""));
                    boolean handler = oldName.equals(node.attributeText("Handler", ""));
                    if (!named && !handler) continue;
                    UiNode copy = node.copy();
                    copy.set(named ? "Name" : "Handler", newName);
                    items.set(i, copy);
                }
            }, null, null);
            if (!step(session, edit, why)) return false;
            String problem = verify(session);
            if (problem != null) why[0] = problem;
            return problem == null;
        });
        if (!done) fail(session, why[0].isEmpty() ? "The rename was rejected." : why[0]);
        return done;
    }

    /**
     * Заменяет список аргументов события контракта или пользовательского события. Провода к исчезнувшим пинам
     * убираются вместе с аргументом, поэтому граф остаётся собираемым.
     *
     * @param session сессия документа компонента
     * @param event   имя события
     * @param args    аргументы в записи {@code имя:тип, имя:тип}; пустая строка убирает все
     * @return {@code true}, если список изменён
     */
    public static boolean arguments(EditorSession session, String event, String args) {
        return changeArguments(session, event, args, null, null);
    }

    /**
     * Переименовывает аргумент события: меняются список, провода от его пина и значения на нодах {@code Raise}.
     *
     * @param session сессия документа компонента
     * @param event   имя события
     * @param args    новый список аргументов
     * @param oldName прежнее имя аргумента
     * @param newName новое имя аргумента
     * @return {@code true}, если переименовано
     */
    public static boolean argument(EditorSession session, String event, String args, String oldName, String newName) {
        if (!IDENTIFIER.matcher(newName).matches()) {
            return fail(session, "'" + newName + "' is not a valid name. Use letters, digits and underscores.");
        }
        return changeArguments(session, event, args, oldName, newName);
    }

    private static boolean changeArguments(EditorSession session, String event, String args, String oldName,
                                           String newName) {
        UiNode root = session.document().root();
        boolean contractEvent = declared(root, "Event", event);
        String customId = contractEvent ? null : customNode(root, event);
        if (!contractEvent && customId == null) return fail(session, "No event '" + event + "'.");
        List<String> kept = argumentNames(args);
        String[] why = {""};
        boolean done = session.transaction("Change arguments of " + event, () -> {
            Edit declaration = contractEvent
                    ? ContractEdits.setAttribute(root, "Event", event, "Args", args)
                    : dev.sixik.unigui.xaml.logic.GraphEdits.setAttribute(root, customId, "Args", args.isEmpty() ? null : args);
            if (!step(session, declaration, why)) return false;
            Edit wires = ElementListEdit.of(root, "Component", "Graph", "Update event pins",
                    items -> rewritePins(items, event, kept, oldName, newName), null, null);
            if (!step(session, wires, why)) return false;
            String problem = verify(session);
            if (problem != null) why[0] = problem;
            return problem == null;
        });
        if (!done) fail(session, why[0].isEmpty() ? "The change was rejected." : why[0]);
        return done;
    }

    private static List<String> argumentNames(String args) {
        List<String> names = new ArrayList<>();
        for (String part : args.split(",")) {
            int colon = part.indexOf(':');
            if (colon > 0) names.add(part.substring(0, colon).trim());
        }
        return names;
    }

    private static String customNode(UiNode root, String event) {
        for (UiContent item : ElementListEdit.items(root, "Component", "Graph")) {
            if (item instanceof UiNode node && node.attributeText("Type", "").equals("Event.Custom")
                    && event.equals(node.attributeText("Name", ""))) return node.attributeText("Id", "");
        }
        return null;
    }

    /**
     * Ноды, у которых аргументы события видны как пины.
     */
    private static boolean ownsPins(UiNode node, String event) {
        String type = node.attributeText("Type", "");
        if (type.equals("Event." + event) || type.equals("Raise." + event)) return true;
        return (type.equals("Event.Custom") || type.equals("Event.Call")) && event.equals(node.attributeText("Name", ""));
    }

    private static void rewritePins(List<UiContent> items, String event, List<String> kept, String oldName,
                                    String newName) {
        java.util.Set<String> owners = new java.util.HashSet<>();
        for (UiContent item : items) {
            if (item instanceof UiNode node && node.type().local().equals("Node") && ownsPins(node, event)) {
                owners.add(node.attributeText("Id", ""));
            }
        }
        for (int i = items.size() - 1; i >= 0; i--) {
            if (!(items.get(i) instanceof UiNode node)) continue;
            if (node.type().local().equals("Node") && oldName != null && owners.contains(node.attributeText("Id", ""))
                    && node.attributeText(oldName).isPresent()) {
                // Значение пина ноды Raise хранится атрибутом с именем аргумента.
                UiNode copy = node.copy();
                copy.set(newName, node.attributeText(oldName, ""));
                copy.remove(null, dev.sixik.unigui.xaml.UiName.of(oldName));
                items.set(i, copy);
            } else if (node.type().local().equals("Wire")) {
                String from = pin(node.attributeText("From", ""), owners, kept, oldName, newName);
                String to = pin(node.attributeText("To", ""), owners, kept, oldName, newName);
                if (from == null || to == null) {
                    items.remove(i);
                } else if (!from.equals(node.attributeText("From", "")) || !to.equals(node.attributeText("To", ""))) {
                    UiNode copy = node.copy();
                    copy.set("From", from);
                    copy.set("To", to);
                    items.set(i, copy);
                }
            }
        }
    }

    /**
     * @return новый конец провода или {@code null}, если пина больше нет
     */
    private static String pin(String end, java.util.Set<String> owners, List<String> kept, String oldName, String newName) {
        int dot = end.indexOf('.');
        if (dot < 0 || !owners.contains(end.substring(0, dot))) return end;
        String node = end.substring(0, dot);
        String port = end.substring(dot + 1);
        if (port.equals("exec") || port.equals("then") || port.equals("sender")) return end;
        if (port.equals(oldName)) return node + "." + newName;
        return kept.contains(port) ? end : null;
    }

    // ------------------------------------------------------------------ общее

    private static boolean rename(EditorSession session, String kind, String oldName, String newName,
                                  String extension, List<String> namedTypes, List<String> typePrefixes) {
        UiNode root = session.document().root();
        if (!root.type().local().equals("Component")) return fail(session, "Only a component has a contract.");
        if (oldName.equals(newName)) return true;
        if (!IDENTIFIER.matcher(newName).matches()) {
            return fail(session, "'" + newName + "' is not a valid name. Use letters, digits and underscores.");
        }
        if (!declared(root, kind, oldName)) return fail(session, "No " + kind + " '" + oldName + "' in the contract.");

        String[] why = {""};
        boolean done = session.transaction("Rename " + oldName + " to " + newName, () -> {
            // 1. Контракт.
            if (!step(session, renameDeclaration(root, kind, oldName, newName), why)) return false;
            // 2. {Param X} / {Raise X} в теле.
            if (extension != null) {
                for (Edit edit : bodyReferences(root, extension, oldName, newName)) {
                    if (!step(session, edit, why)) return false;
                }
            }
            // 3. Ноды графа.
            if (!step(session, rewriteGraph(root, oldName, newName, namedTypes, typePrefixes), why)) return false;
            String problem = verify(session);
            if (problem != null) why[0] = problem;
            return problem == null;
        });
        // Откат сбрасывает диагностику правок, поэтому причина возвращается в неё уже после отката.
        if (!done) fail(session, why[0].isEmpty() ? "The rename was rejected." : why[0]);
        return done;
    }

    private static boolean step(EditorSession session, Edit edit, String[] why) {
        if (session.apply(edit)) return true;
        var found = session.diagnostics(EditorSession.Channel.EDIT);
        why[0] = found.isEmpty() ? "The rename was rejected." : found.get(0).message();
        return false;
    }

    private static boolean fail(EditorSession session, String reason) {
        session.apply(new Refused(reason));
        return false;
    }

    private static boolean declared(UiNode root, String kind, String name) {
        for (UiContent item : ElementListEdit.items(root, "Component", "Contract")) {
            if (item instanceof UiNode node && node.type().local().equals(kind)
                    && name.equals(node.attributeText("Name", ""))) return true;
        }
        return false;
    }

    private static Edit renameDeclaration(UiNode root, String kind, String oldName, String newName) {
        return ElementListEdit.of(root, "Component", "Contract", "Rename " + kind.toLowerCase() + " " + oldName,
                items -> {
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i) instanceof UiNode node && node.type().local().equals(kind)
                                && oldName.equals(node.attributeText("Name", ""))) {
                            UiNode copy = node.copy();
                            copy.set("Name", newName);
                            items.set(i, copy);
                            return;
                        }
                    }
                }, null, null);
    }

    /**
     * Правки каждого свойства тела, значение которого — {@code {extension oldName}}.
     */
    private static List<Edit> bodyReferences(UiNode root, String extension, String oldName, String newName) {
        List<Edit> edits = new ArrayList<>();
        for (UiNode child : root.children()) collect(child, extension, oldName, newName, edits);
        return edits;
    }

    private static void collect(UiNode node, String extension, String oldName, String newName, List<Edit> edits) {
        for (UiProperty property : node.properties()) {
            if (property.value() instanceof UiValue.Extension use && use.name().local().equals(extension)
                    && use.positional().size() == 1 && use.named().isEmpty()
                    && use.positional().get(0) instanceof UiValue.Literal literal
                    && literal.text().trim().equals(oldName)) {
                edits.add(Edits.setProperty(node, property.owner(), property.name(),
                        new UiValue.Extension(use.name(), List.of(new UiValue.Literal(newName)), Map.of())));
            } else if (property.value() instanceof UiValue.Elements elements) {
                for (UiContent item : elements.content()) {
                    if (item instanceof UiNode nested) collect(nested, extension, oldName, newName, edits);
                }
            }
        }
        for (UiNode child : node.children()) collect(child, extension, oldName, newName, edits);
    }

    /**
     * Одна правка графа: всё переименованное внутри заменяется разом.
     */
    private static Edit rewriteGraph(UiNode root, String oldName, String newName, List<String> namedTypes,
                                     List<String> typePrefixes) {
        return ElementListEdit.of(root, "Component", "Graph", "Rename in graph", items -> {
            for (int i = 0; i < items.size(); i++) {
                if (!(items.get(i) instanceof UiNode node) || !node.type().local().equals("Node")) continue;
                String type = node.attributeText("Type", "");
                if (namedTypes.contains(type) && oldName.equals(node.attributeText("Name", ""))) {
                    UiNode copy = node.copy();
                    copy.set("Name", newName);
                    items.set(i, copy);
                    continue;
                }
                for (String prefix : typePrefixes) {
                    if (type.equals(prefix + oldName)) {
                        UiNode copy = node.copy();
                        copy.set("Type", prefix + newName);
                        items.set(i, copy);
                        break;
                    }
                }
            }
        }, null, null);
    }

    /**
     * Итог должен разбираться и собираться; иначе транзакция откатывается.
     */
    private static String verify(EditorSession session) {
        try {
            ComponentDefinition definition = ComponentDefinition.parse("rename", UiDocument.of(session.document().root()));
            GraphCompiler.check("rename", definition.graph(), definition.contract());
            return null;
        } catch (XamlLoadException failure) {
            return failure.getMessage();
        }
    }

    /**
     * Правка-отказ: ничего не меняет, но кладёт причину в диагностику редактора.
     */
    private record Refused(String reason) implements Edit {
        @Override
        public String label() {
            return "Rename";
        }

        @Override
        public java.util.Optional<String> check(dev.sixik.unigui.xaml.schema.Schema schema) {
            return java.util.Optional.of(reason);
        }

        @Override
        public void apply() {
            throw new IllegalStateException(reason);
        }

        @Override
        public void undo() {
            throw new IllegalStateException(reason);
        }
    }
}

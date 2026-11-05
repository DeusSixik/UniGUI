package dev.sixik.unigui.xaml.struct;

import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.Edits;
import dev.sixik.unigui.xaml.load.XamlLoadException;

import java.util.List;

/**
 * Правки документа структуры ({@code <Struct>}): добавить, убрать, переименовать поле, сменить его тип, переставить.
 *
 * <p>Каждая правка идёт транзакцией сессии с проверкой итога: после неё документ должен читаться как структура
 * ({@link StructType#parse}). Дубли, неверные имена и неизвестные типы отклоняются с причиной, документ остаётся
 * прежним. Ссылки на поле из нод графа ({@code Struct.Make}, {@code Struct.Break}) правка не обновляет: их
 * показывает проверка компонентов.</p>
 */
public final class StructEdits {
    private StructEdits() {
    }

    /**
     * Добавляет поле в конец.
     *
     * @param session сессия документа структуры
     * @param name    имя поля
     * @param type    тип в записи контракта ({@code string}, {@code int}, {@code struct:Other}…)
     * @return {@code true}, если поле добавлено
     */
    public static boolean addField(EditorSession session, String name, String type) {
        UiNode root = session.document().root();
        return run(session, "Add field " + name, () -> session.apply(Edits.addChild(root, root.children().size(),
                UiNode.of("Field").set("Name", name).set("Type", type))));
    }

    /**
     * Убирает поле.
     *
     * @param session сессия документа структуры
     * @param name    имя поля
     * @return {@code true}, если поле убрано
     */
    public static boolean removeField(EditorSession session, String name) {
        UiNode field = find(session, name);
        if (field == null) return refuse(session, "No field '" + name + "'.");
        return run(session, "Remove field " + name, () -> session.apply(Edits.remove(session.document(), field)));
    }

    /**
     * Меняет тип поля.
     *
     * @param session сессия документа структуры
     * @param name    имя поля
     * @param type    новый тип
     * @return {@code true}, если тип изменён
     */
    public static boolean setType(EditorSession session, String name, String type) {
        UiNode field = find(session, name);
        if (field == null) return refuse(session, "No field '" + name + "'.");
        return run(session, "Set type of " + name, () -> session.apply(Edits.setProperty(field, "Type", type)));
    }

    /**
     * Переименовывает поле.
     *
     * @param session сессия документа структуры
     * @param name    прежнее имя
     * @param newName новое имя
     * @return {@code true}, если поле переименовано
     */
    public static boolean renameField(EditorSession session, String name, String newName) {
        UiNode field = find(session, name);
        if (field == null) return refuse(session, "No field '" + name + "'.");
        return run(session, "Rename field " + name, () -> session.apply(Edits.setProperty(field, "Name", newName)));
    }

    /**
     * Сдвигает поле среди остальных.
     *
     * @param session сессия документа структуры
     * @param name    имя поля
     * @param delta   на сколько позиций: -1 выше, 1 ниже
     * @return {@code true}, если поле сдвинуто
     */
    public static boolean move(EditorSession session, String name, int delta) {
        UiNode field = find(session, name);
        if (field == null) return refuse(session, "No field '" + name + "'.");
        UiNode root = session.document().root();
        int target = root.children().indexOf(field) + delta;
        if (target < 0 || target >= root.children().size()) return false;
        return run(session, "Move field " + name, () -> session.apply(Edits.move(session.document(), field, root, target)));
    }

    // ------------------------------------------------------------------ внутреннее

    private static UiNode find(EditorSession session, String name) {
        List<UiNode> fields = session.document().root().children();
        for (UiNode field : fields) {
            if (field.type().local().equals("Field") && name.equals(field.attributeText("Name", ""))) return field;
        }
        return null;
    }

    /**
     * Правка с проверкой итога; причина отказа остаётся в диагностике правок сессии.
     */
    private static boolean run(EditorSession session, String label, java.util.function.BooleanSupplier change) {
        String[] why = {""};
        boolean done = session.transaction(label, () -> {
            if (!change.getAsBoolean()) {
                var found = session.diagnostics(EditorSession.Channel.EDIT);
                why[0] = found.isEmpty() ? "The edit was rejected." : found.get(0).message();
                return false;
            }
            try {
                StructType.parse(session.text());
                return true;
            } catch (XamlLoadException failure) {
                why[0] = failure.getMessage();
                return false;
            }
        });
        if (!done) refuse(session, why[0].isEmpty() ? "The edit was rejected." : why[0]);
        return done;
    }

    /**
     * Кладёт причину в диагностику правок (откат транзакции её сбрасывает).
     */
    private static boolean refuse(EditorSession session, String reason) {
        session.apply(new dev.sixik.unigui.xaml.edit.Edit() {
            @Override
            public String label() {
                return "Refused";
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
        });
        return false;
    }
}

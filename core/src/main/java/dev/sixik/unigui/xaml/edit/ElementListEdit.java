package dev.sixik.unigui.xaml.edit;

import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.schema.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Основа правок свойства-элемента, содержимое которого — список узлов ({@code Component.Contract},
 * {@code Component.Graph}).
 *
 * <p>Правка меняет свойство целиком: прежнее и новое значения хранятся рядом, применение и отмена — подмена одного на
 * другое. Поэтому любая сложная правка (несколько узлов и проводов сразу) атомарна, а отмена точна. Перед применением
 * правка проверяется: она временно ставится в документ, проверка смотрит результат, и документ возвращается назад.</p>
 */
public final class ElementListEdit {
    private ElementListEdit() {
    }

    /**
     * Изменение списка: бросает {@link IllegalArgumentException}, если предусловие не выполнено.
     */
    @FunctionalInterface
    public interface Change {
        /**
         * @param items копия текущего списка; изменяется на месте
         */
        void apply(List<UiContent> items);
    }

    /**
     * Проверка документа с уже поставленной правкой.
     */
    @FunctionalInterface
    public interface Check {
        /**
         * @param root корневой узел документа с применённой правкой
         * @return описание проблемы или пусто, если правка допустима
         */
        Optional<String> verify(UiNode root);
    }

    /**
     * Содержимое свойства-элемента.
     *
     * @param root   корневой узел
     * @param owner  локальное имя владельца ({@code Component})
     * @param member имя члена ({@code Graph})
     * @return узлы и тексты в порядке документа; пусто, если свойства нет
     */
    public static List<UiContent> items(UiNode root, String owner, String member) {
        UiProperty property = find(root, owner, member).orElse(null);
        if (property != null && property.value() instanceof UiValue.Elements elements) return elements.content();
        return List.of();
    }

    /**
     * Создаёт правку.
     *
     * @param root     корневой узел документа
     * @param owner    локальное имя владельца свойства
     * @param member   имя свойства
     * @param label    подпись шага отмены
     * @param change   изменение списка
     * @param check    проверка результата; {@code null} — не проверять
     * @param mergeKey ключ слияния подряд идущих правок (перетаскивание) или {@code null}
     * @return правка; если предусловие не выполнено, правка, которая отклоняется с причиной
     */
    public static Edit of(UiNode root, String owner, String member, String label, Change change, Check check,
                          String mergeKey) {
        UiProperty before = find(root, owner, member).orElse(null);
        List<UiContent> items = new ArrayList<>(items(root, owner, member));
        try {
            change.apply(items);
        } catch (IllegalArgumentException failure) {
            return new Rejected(label, failure.getMessage());
        }
        UiProperty after = new UiProperty(before == null ? UiName.of(member) : before.name(),
                before == null ? root.type() : before.owner(), new UiValue.Elements(items),
                UiProperty.Form.ELEMENT, before == null ? null : before.location());
        return new Swap(root, label, before, after, check, mergeKey);
    }

    private static Optional<UiProperty> find(UiNode root, String owner, String member) {
        for (UiProperty property : root.properties()) {
            if (property.owner() != null && property.owner().local().equals(owner)
                    && property.name().local().equals(member)) return Optional.of(property);
        }
        return Optional.empty();
    }

    /**
     * Правка, которую нельзя применить: причина доходит до диагностики редактора.
     */
    private record Rejected(String label, String reason) implements Edit {
        @Override
        public Optional<String> check(Schema schema) {
            return Optional.of(reason);
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

    private static final class Swap implements Edit {
        private final UiNode root;
        private final String label;
        private final UiProperty before;
        private final UiProperty after;
        private final Check check;
        private final String mergeKey;

        private Swap(UiNode root, String label, UiProperty before, UiProperty after, Check check, String mergeKey) {
            this.root = root;
            this.label = label;
            this.before = before;
            this.after = after;
            this.check = check;
            this.mergeKey = mergeKey;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public Optional<String> check(Schema schema) {
            if (check == null) return Optional.empty();
            install(after);
            try {
                return check.verify(root);
            } finally {
                install(before);
            }
        }

        @Override
        public void apply() {
            install(after);
        }

        @Override
        public void undo() {
            install(before);
        }

        @Override
        public Edit mergeWith(Edit next) {
            if (mergeKey == null || !(next instanceof Swap later) || !mergeKey.equals(later.mergeKey)
                    || later.root != root) {
                return null;
            }
            return new Swap(root, label, before, later.after, check, mergeKey);
        }

        private void install(UiProperty property) {
            if (property != null) root.set(property);
            else root.remove(after.owner(), after.name());
        }
    }
}

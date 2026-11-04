package dev.sixik.unigui.xaml.edit;

import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Адрес узла в документе: цепочка шагов от корня.
 *
 * <p>Редактор обычно ссылается на узлы объектами, потому что они изменяемы и сохраняют идентичность при правках.
 * Путь нужен там, где идентичность теряется: после повторного разбора текста выбранный узел находится заново
 * по пути, который был записан до разбора.</p>
 *
 * <p>Шаг — это либо индекс среди прямых детей, либо индекс внутри property-элемента ({@code ScrollView.Content}).</p>
 */
public record NodePath(List<Step> steps) {
    /**
     * Шаг пути.
     *
     * @param slot  имя property-элемента или {@code null} для прямого ребёнка
     * @param index индекс среди узлов в этом месте
     */
    public record Step(String slot, int index) {
        @Override
        public String toString() {
            return slot == null ? Integer.toString(index) : slot + "[" + index + "]";
        }
    }

    /** Путь к корню. */
    public static final NodePath ROOT = new NodePath(List.of());

    public NodePath {
        steps = List.copyOf(steps);
    }

    /**
     * Строит путь из индексов прямых детей.
     *
     * @param indices индексы по очереди от корня
     * @return путь
     */
    public static NodePath of(int... indices) {
        List<Step> steps = new ArrayList<>(indices.length);
        for (int index : indices) steps.add(new Step(null, index));
        return new NodePath(steps);
    }

    /** @return {@code true}, если путь указывает на корень */
    public boolean isRoot() {
        return steps.isEmpty();
    }

    /**
     * Путь к родителю.
     *
     * @return путь родителя; для корня тоже {@link #ROOT}
     */
    public NodePath parent() {
        return steps.isEmpty() ? this : new NodePath(steps.subList(0, steps.size() - 1));
    }

    /**
     * Находит узел по пути.
     *
     * @param document документ
     * @return узел или пусто, если путь больше не ведёт к узлу
     */
    public Optional<UiNode> resolve(UiDocument document) {
        UiNode current = document.root();
        for (Step step : steps) {
            List<UiNode> nodes = nodesAt(current, step.slot());
            if (step.index() < 0 || step.index() >= nodes.size()) return Optional.empty();
            current = nodes.get(step.index());
        }
        return Optional.of(current);
    }

    /**
     * Вычисляет путь к узлу, сравнивая узлы по ссылке.
     *
     * @param document документ
     * @param target   искомый узел
     * @return путь или пусто, если узла нет в документе
     */
    public static Optional<NodePath> find(UiDocument document, UiNode target) {
        if (target == null) return Optional.empty();
        List<Step> trail = new ArrayList<>();
        return search(document.root(), target, trail) ? Optional.of(new NodePath(trail)) : Optional.empty();
    }

    private static boolean search(UiNode current, UiNode target, List<Step> trail) {
        if (current == target) return true;
        for (UiProperty property : current.properties()) {
            if (!(property.value() instanceof UiValue.Elements)) continue;
            List<UiNode> nodes = nodesAt(current, property.name().local());
            for (int i = 0; i < nodes.size(); i++) {
                trail.add(new Step(property.name().local(), i));
                if (search(nodes.get(i), target, trail)) return true;
                trail.remove(trail.size() - 1);
            }
        }
        List<UiNode> children = current.children();
        for (int i = 0; i < children.size(); i++) {
            trail.add(new Step(null, i));
            if (search(children.get(i), target, trail)) return true;
            trail.remove(trail.size() - 1);
        }
        return false;
    }

    /** Узлы в указанном месте: прямые дети или содержимое property-элемента. */
    static List<UiNode> nodesAt(UiNode parent, String slot) {
        if (slot == null) return parent.children();
        List<UiNode> nodes = new ArrayList<>();
        for (UiProperty property : parent.properties()) {
            if (property.name().local().equals(slot) && property.value() instanceof UiValue.Elements elements) {
                for (UiContent item : elements.content()) if (item instanceof UiNode node) nodes.add(node);
            }
        }
        return nodes;
    }

    @Override
    public String toString() {
        if (steps.isEmpty()) return "/";
        StringBuilder builder = new StringBuilder();
        for (Step step : steps) builder.append('/').append(step);
        return builder.toString();
    }
}

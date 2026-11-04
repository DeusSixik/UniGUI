package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.Edits;
import dev.sixik.unigui.xaml.edit.Hierarchy;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Якоря: положение виджета в слоте родителя без вёрстки потоком, как у RectTransform в Unity, в упрощённой форме.
 *
 * <p>Якорь — это пара выравниваний ({@code horizontalAlignment}, {@code verticalAlignment}: start, center, end,
 * stretch) и отступы {@code margin}. В {@code StackPanel} все дети лежат друг над другом в одном слоте, поэтому такой
 * виджет привязан к углу, стороне или центру родителя и держит это положение при любом размере экрана. Свободное
 * перемещение мышью меняет только отступы.</p>
 */
final class Anchors {
    private static final List<String> ALIGNMENTS = List.of("start", "center", "end");
    private static final List<String> ROW_NAMES = List.of("Top", "Middle", "Bottom");
    private static final List<String> COLUMN_NAMES = List.of("Left", "Center", "Right");

    private Anchors() {
    }

    /** @return {@code true}, если родитель узла — {@code StackPanel}, где якоря имеют смысл */
    static boolean supported(EditorSession session, UiNode node) {
        if (node == session.document().root()) return false;
        Optional<UiNode> parent = Hierarchy.parentOf(session.document(), node);
        if (parent.isEmpty()) return false;
        String name = parent.get().type().local();
        return name.equals("StackPanel") || name.equals("Stack");
    }

    /** @return выравнивание по оси: {@code horizontalAlignment}/{@code verticalAlignment}, по умолчанию stretch */
    static String alignment(UiNode node, boolean horizontal) {
        return node.attributeText(horizontal ? "horizontalAlignment" : "verticalAlignment", "stretch")
                .trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Ставит якорь.
     *
     * @param session    сессия
     * @param node       узел
     * @param horizontal {@code start}, {@code center}, {@code end} или {@code stretch}
     * @param vertical   то же по вертикали
     */
    static void anchor(EditorSession session, UiNode node, String horizontal, String vertical) {
        session.apply(Edits.setProperty(node, "horizontalAlignment", horizontal));
        session.apply(Edits.setProperty(node, "verticalAlignment", vertical));
    }

    /**
     * Сдвигает виджет на {@code dx, dy} в пикселях раскладки, меняя только отступы так, чтобы якорь остался прежним.
     * Подряд идущие сдвиги сливаются в один шаг отмены.
     *
     * @param session сессия
     * @param node    узел
     * @param dx      сдвиг вправо
     * @param dy      сдвиг вниз
     * @return {@code true}, если отступы изменены
     */
    static boolean move(EditorSession session, UiNode node, float dx, float dy) {
        float[] margin = margin(node); // верх, право, низ, лево
        if (margin == null) return false;
        String horizontal = alignment(node, true);
        String vertical = alignment(node, false);
        if (horizontal.equals("end")) margin[1] -= dx;
        else if (horizontal.equals("center")) {
            margin[3] += dx;
            margin[1] -= dx;
        } else margin[3] += dx;
        if (vertical.equals("end")) margin[2] -= dy;
        else if (vertical.equals("center")) {
            margin[0] += dy;
            margin[2] -= dy;
        } else margin[0] += dy;
        return session.apply(Edits.setProperty(node, "margin", format(margin)));
    }

    /** @return отступы {@code [верх, право, низ, лево]} или {@code null}, если запись не разобрать */
    static float[] margin(UiNode node) {
        String text = node.attributeText("margin", "0").trim();
        String[] parts = text.isEmpty() ? new String[]{"0"} : text.split("[\\s,]+");
        try {
            float[] values = new float[parts.length];
            for (int i = 0; i < parts.length; i++) values[i] = Float.parseFloat(parts[i]);
            return switch (values.length) {
                case 1 -> new float[]{values[0], values[0], values[0], values[0]};
                case 2 -> new float[]{values[0], values[1], values[0], values[1]};
                case 4 -> values;
                default -> null;
            };
        } catch (NumberFormatException failure) {
            return null;
        }
    }

    private static String format(float[] margin) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) text.append(' ');
            float value = Math.round(margin[i] * 10.0f) / 10.0f;
            text.append(value == Math.rint(value) ? Long.toString((long) value) : Float.toString(value));
        }
        return text.toString();
    }

    /**
     * Выбор якоря сеткой 3×3 и кнопками растяжения.
     *
     * @param session сессия
     * @param node    узел
     * @param changed вызывается после смены якоря
     * @return виджет
     */
    static Widget picker(EditorSession session, UiNode node, Runnable changed) {
        VBox box = new VBox();
        box.spacing(2.0f);
        String currentH = alignment(node, true);
        String currentV = alignment(node, false);
        for (int row = 0; row < 3; row++) {
            HBox line = new HBox();
            line.spacing(2.0f);
            for (int column = 0; column < 3; column++) {
                String h = ALIGNMENTS.get(column);
                String v = ALIGNMENTS.get(row);
                boolean active = h.equals(currentH) && v.equals(currentV);
                Button cell = new Button(active ? "●" : "○");
                cell.id("anchor." + ROW_NAMES.get(row) + "." + COLUMN_NAMES.get(column));
                cell.onClick(click -> {
                    anchor(session, node, h, v);
                    changed.run();
                });
                line.addChild(cell);
            }
            line.applyQueuedMutations();
            box.addChild(line);
        }
        HBox stretch = new HBox();
        stretch.spacing(2.0f);
        stretch.addChild(stretchButton(session, node, "Fill", "stretch", "stretch", changed));
        stretch.addChild(stretchButton(session, node, "Width", "stretch", currentV.equals("stretch") ? "start" : currentV, changed));
        stretch.addChild(stretchButton(session, node, "Height", currentH.equals("stretch") ? "start" : currentH, "stretch", changed));
        stretch.applyQueuedMutations();
        box.addChild(stretch);
        box.addChild(new Label("Anchor " + currentH + " / " + currentV));
        box.applyQueuedMutations();
        return box;
    }

    private static Button stretchButton(EditorSession session, UiNode node, String title, String h, String v,
                                        Runnable changed) {
        Button button = new Button(title);
        button.id("anchor." + title);
        button.onClick(click -> {
            anchor(session, node, h, v);
            changed.run();
        });
        return button;
    }
}

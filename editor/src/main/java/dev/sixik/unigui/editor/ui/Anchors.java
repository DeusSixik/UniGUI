package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.editor.style.ToolButton;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.Anchoring;

import java.util.List;

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

    /** @return {@code true}, если для узла возможны якоря (слот стека или абсолютное положение) */
    static boolean supported(EditorSession session, UiNode node) {
        return Anchoring.mode(session, node) != Anchoring.Mode.NONE;
    }

    /** @return привязка по оси: start, center, end или stretch */
    static String alignment(EditorSession session, UiNode node, boolean horizontal) {
        return Anchoring.alignment(session, node, horizontal);
    }

    /** Ставит якорь; логика живёт в ядре и проверена без интерфейса. */
    static boolean anchor(EditorSession session, UiNode node, String horizontal, String vertical) {
        return Anchoring.anchor(session, node, horizontal, vertical);
    }

    /** Сдвигает виджет, меняя только отступы. */
    static boolean move(EditorSession session, UiNode node, float dx, float dy) {
        return Anchoring.move(session, node, dx, dy);
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
        box.spacing(1.0f);
        box.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f).padding(15.0f, 2.0f, 0.0f, 6.0f));
        String currentH = alignment(session, node, true);
        String currentV = alignment(session, node, false);
        for (int row = 0; row < 3; row++) {
            HBox line = new HBox();
            line.spacing(1.0f);
            for (int column = 0; column < 3; column++) {
                String h = ALIGNMENTS.get(column);
                String v = ALIGNMENTS.get(row);
                boolean active = h.equals(currentH) && v.equals(currentV);
                ToolButton cell = new ToolButton(active ? EditorIcons.BREAKPOINT : EditorIcons.ANCHOR_OFF, "Anchor");
                cell.active(active);
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
        box.addChild(EditorStyles.caption(currentH + " / " + currentV));
        box.applyQueuedMutations();
        return box;
    }

    private static Button stretchButton(EditorSession session, UiNode node, String title, String h, String v,
                                        Runnable changed) {
        ToolButton button = new ToolButton(null, title, ToolButton.Variant.SOLID);
        button.id("anchor." + title);
        button.onClick(click -> {
            anchor(session, node, h, v);
            changed.run();
        });
        return button;
    }
}

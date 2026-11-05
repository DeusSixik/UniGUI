package dev.sixik.unigui.xaml.edit;

import dev.sixik.unigui.xaml.UiNode;

import java.util.Locale;
import java.util.Optional;

/**
 * Якоря: положение виджета относительно родителя без вёрстки потоком, как у RectTransform в Unity, в упрощённой
 * форме. Работают двумя способами в зависимости от того, где лежит виджет.
 *
 * <ul>
 *   <li><b>Слот стека или ячейка сетки</b> (ребёнок {@code StackPanel} или {@code GridBox}): выравнивания
 *       {@code horizontalAlignment} и {@code verticalAlignment} (start, center, end, stretch) плюс {@code margin} как
 *       отступы. Все дети стека лежат в одном слоте, ребёнок сетки — в своей ячейке, поэтому виджет держит угол,
 *       сторону или центр слота при любом его размере.</li>
 *   <li><b>Абсолютное положение</b> ({@code position="absolute"} в любом родителе): якорь задают отступы
 *       {@code left}/{@code right}/{@code top}/{@code bottom}. Заданный {@code left} — привязка к левому краю,
 *       {@code right} — к правому, оба — растяжение. Центрирование здесь недоступно: положение по центру нужно
 *       выражать слотом стека.</li>
 * </ul>
 *
 * <p>Перемещение мышью меняет только отступы, поэтому якорь остаётся прежним.</p>
 */
public final class Anchoring {
    private static final java.util.List<String> ALIGNMENTS = java.util.List.of("start", "center", "end", "stretch");

    private Anchoring() {
    }

    /** Способ размещения. */
    public enum Mode {
        /** Якоря не применимы. */
        NONE,
        /** Слот {@code StackPanel} или ячейка {@code GridBox}: выравнивания плюс {@code margin}. */
        SLOT,
        /** {@code position="absolute"}. */
        ABSOLUTE
    }

    /**
     * Определяет способ размещения узла.
     *
     * @param session сессия
     * @param node    узел
     * @return способ или {@link Mode#NONE}
     */
    public static Mode mode(EditorSession session, UiNode node) {
        if (node == session.document().root()) return Mode.NONE;
        String position = node.attributeText("position", "").trim().toLowerCase(Locale.ROOT);
        if (position.equals("absolute") || position.equals("fixed")) return Mode.ABSOLUTE;
        Optional<UiNode> parent = Hierarchy.parentOf(session.document(), node);
        if (parent.isEmpty()) return Mode.NONE;
        String name = parent.get().type().local();
        boolean slot = name.equals("StackPanel") || name.equals("Stack") || name.equals("GridBox") || name.equals("Grid");
        return slot ? Mode.SLOT : Mode.NONE;
    }

    /**
     * Текущая привязка по оси.
     *
     * @param session    сессия
     * @param node       узел
     * @param horizontal {@code true} для горизонтали
     * @return {@code start}, {@code center}, {@code end} или {@code stretch}
     */
    public static String alignment(EditorSession session, UiNode node, boolean horizontal) {
        if (mode(session, node) == Mode.ABSOLUTE) {
            boolean first = node.attributeText(horizontal ? "left" : "top").isPresent();
            boolean last = node.attributeText(horizontal ? "right" : "bottom").isPresent();
            if (first && last) return "stretch";
            return last ? "end" : "start";
        }
        String value = node.attributeText(horizontal ? "horizontalAlignment" : "verticalAlignment", "stretch")
                .trim().toLowerCase(Locale.ROOT);
        return ALIGNMENTS.contains(value) ? value : "stretch";
    }

    /**
     * Ставит якорь.
     *
     * @param session    сессия
     * @param node       узел
     * @param horizontal {@code start}, {@code center}, {@code end} или {@code stretch}
     * @param vertical   то же по вертикали
     * @return {@code true}, если якорь поставлен; иначе причина в {@link EditorSession.Channel#EDIT}
     */
    public static boolean anchor(EditorSession session, UiNode node, String horizontal, String vertical) {
        Mode mode = mode(session, node);
        if (mode == Mode.NONE) return refuse(session, "Anchors need a StackPanel parent or position=\"absolute\".");
        if (!ALIGNMENTS.contains(horizontal) || !ALIGNMENTS.contains(vertical)) {
            return refuse(session, "An anchor is start, center, end or stretch on each axis.");
        }
        if (mode == Mode.SLOT) {
            return session.transaction("Set anchor", () -> session.apply(Edits.setProperty(node, "horizontalAlignment", horizontal))
                    && session.apply(Edits.setProperty(node, "verticalAlignment", vertical)));
        }
        if (horizontal.equals("center") || vertical.equals("center")) {
            return refuse(session, "A centered anchor needs a StackPanel slot; absolute positions anchor to edges.");
        }
        return session.transaction("Set anchor", () -> insets(session, node, true, horizontal)
                && insets(session, node, false, vertical));
    }

    /** Абсолютный якорь по оси: какие из двух отступов заданы; прежнее значение сохраняется, иначе 0. */
    private static boolean insets(EditorSession session, UiNode node, boolean horizontal, String alignment) {
        String first = horizontal ? "left" : "top";
        String last = horizontal ? "right" : "bottom";
        boolean wantFirst = alignment.equals("start") || alignment.equals("stretch");
        boolean wantLast = alignment.equals("end") || alignment.equals("stretch");
        return apply(session, node, first, wantFirst) && apply(session, node, last, wantLast);
    }

    private static boolean apply(EditorSession session, UiNode node, String property, boolean wanted) {
        boolean has = node.attributeText(property).isPresent();
        if (wanted == has) return true;
        return session.apply(wanted ? Edits.setProperty(node, property, "0") : Edits.removeProperty(node, property));
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
    public static boolean move(EditorSession session, UiNode node, float dx, float dy) {
        Mode mode = mode(session, node);
        if (mode == Mode.NONE) return false;
        String horizontal = alignment(session, node, true);
        String vertical = alignment(session, node, false);
        if (mode == Mode.SLOT) {
            ValueEditing.InsetsValue margin;
            try {
                margin = ValueEditing.InsetsValue.parse(node.attributeText("margin", "0"));
            } catch (IllegalArgumentException failure) {
                return false;
            }
            double top = margin.top();
            double right = margin.right();
            double bottom = margin.bottom();
            double left = margin.left();
            if (horizontal.equals("end")) right -= dx;
            else if (horizontal.equals("center")) {
                left += dx;
                right -= dx;
            } else left += dx;
            if (vertical.equals("end")) bottom -= dy;
            else if (vertical.equals("center")) {
                top += dy;
                bottom -= dy;
            } else top += dy;
            return session.apply(Edits.setProperty(node, "margin",
                    new ValueEditing.InsetsValue(top, right, bottom, left).format()));
        }
        return session.transaction("Move", () -> shift(session, node, "left", horizontal.equals("end") ? 0 : dx)
                && shift(session, node, "right", horizontal.equals("start") ? 0 : -dx)
                && shift(session, node, "top", vertical.equals("end") ? 0 : dy)
                && shift(session, node, "bottom", vertical.equals("start") ? 0 : -dy));
    }

    /** Меняет заданный абсолютный отступ на величину; незаданный не трогает. */
    private static boolean shift(EditorSession session, UiNode node, String property, float delta) {
        if (delta == 0.0f) return true;
        Optional<String> current = node.attributeText(property);
        if (current.isEmpty()) return true;
        double base;
        try {
            base = Double.parseDouble(current.get().trim().replace("px", ""));
        } catch (NumberFormatException failure) {
            return true; // проценты и auto не сдвигаем
        }
        return session.apply(Edits.setProperty(node, property, ValueEditing.SizeValue.parse(String.valueOf(base + delta)).format()));
    }

    private static boolean refuse(EditorSession session, String reason) {
        session.apply(new Edit() {
            @Override
            public String label() {
                return "Anchor";
            }

            @Override
            public Optional<String> check(dev.sixik.unigui.xaml.schema.Schema schema) {
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
        });
        return false;
    }
}

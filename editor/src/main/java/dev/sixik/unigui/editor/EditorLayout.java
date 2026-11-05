package dev.sixik.unigui.editor;

import dev.sixik.unigui.widgets.docking.DockLayoutSnapshot;
import dev.sixik.unigui.widgets.docking.DockNode;
import dev.sixik.unigui.widgets.docking.DockSplitOrientation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Именованная раскладка панелей («Default», «2 by 3»).
 *
 * <p>Обычную раскладку из четырёх областей собирает {@link #named(String)}. Если нужно дерево другой формы,
 * {@link #custom(String, Supplier)} принимает готовый снимок докинга.</p>
 */
public final class EditorLayout {
    private final String name;
    private final Supplier<DockLayoutSnapshot> snapshot;

    private EditorLayout(String name, Supplier<DockLayoutSnapshot> snapshot) {
        this.name = name;
        this.snapshot = snapshot;
    }

    /**
     * Раскладка из готового снимка докинга.
     *
     * @param name     имя
     * @param snapshot поставщик снимка; вызывается при каждом применении раскладки
     * @return раскладка
     */
    public static EditorLayout custom(String name, Supplier<DockLayoutSnapshot> snapshot) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("layout name must not be blank");
        return new EditorLayout(name.trim(), Objects.requireNonNull(snapshot, "snapshot"));
    }

    /**
     * Начинает раскладку из областей.
     *
     * @param name имя
     * @return билдер
     */
    public static Builder named(String name) {
        return new Builder(name);
    }

    /** @return имя раскладки */
    public String name() {
        return name;
    }

    /** @return снимок докинга; панели, которых в раскладке нет, остаются закрытыми */
    public DockLayoutSnapshot snapshot() {
        return snapshot.get();
    }

    /** Билдер раскладки из областей. Доли — часть окна, которую занимает область. */
    public static final class Builder {
        private final String name;
        private List<String> center = List.of();
        private List<String> left = List.of();
        private List<String> right = List.of();
        private List<String> bottom = List.of();
        private List<String> leftLower = List.of();
        private float leftUpperShare = 0.55f;
        private float leftShare = 0.2f;
        private float rightShare = 0.25f;
        private float bottomShare = 0.3f;
        private String active = "";

        private Builder(String name) {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("layout name must not be blank");
            this.name = name.trim();
        }

        /**
         * @param ids панели центральной области (вкладками)
         * @return этот билдер
         */
        public Builder center(String... ids) {
            this.center = List.of(ids);
            return this;
        }

        /**
         * @param share доля ширины
         * @param ids   панели левой колонки
         * @return этот билдер
         */
        public Builder left(float share, String... ids) {
            this.leftShare = share;
            this.left = List.of(ids);
            return this;
        }

        /**
         * Делит левую колонку на две части: панели {@link #left} остаются сверху, эти ложатся под ними.
         *
         * @param upperShare доля высоты верхней части
         * @param ids        панели нижней части левой колонки
         * @return этот билдер
         */
        public Builder leftLower(float upperShare, String... ids) {
            this.leftUpperShare = upperShare;
            this.leftLower = List.of(ids);
            return this;
        }

        /**
         * @param share доля ширины
         * @param ids   панели правой колонки
         * @return этот билдер
         */
        public Builder right(float share, String... ids) {
            this.rightShare = share;
            this.right = List.of(ids);
            return this;
        }

        /**
         * @param share доля высоты центральной колонки
         * @param ids   панели нижней области
         * @return этот билдер
         */
        public Builder bottom(float share, String... ids) {
            this.bottomShare = share;
            this.bottom = List.of(ids);
            return this;
        }

        /**
         * @param id панель, которая выбрана при применении раскладки
         * @return этот билдер
         */
        public Builder active(String id) {
            this.active = id == null ? "" : id;
            return this;
        }

        /** @return раскладка */
        public EditorLayout build() {
            List<String> centerIds = List.copyOf(center);
            List<String> leftIds = List.copyOf(left);
            List<String> rightIds = List.copyOf(right);
            List<String> bottomIds = List.copyOf(bottom);
            List<String> leftLowerIds = List.copyOf(leftLower);
            float leftUpperPart = leftUpperShare;
            float leftPart = leftShare;
            float rightPart = rightShare;
            float bottomPart = bottomShare;
            String activeId = active.isEmpty() && !centerIds.isEmpty() ? centerIds.get(0) : active;
            return new EditorLayout(name, () -> {
                DockLayoutSnapshot.Node core = leaf(centerIds);
                if (!bottomIds.isEmpty()) {
                    core = split(DockSplitOrientation.VERTICAL, complement(bottomPart), core, leaf(bottomIds));
                }
                if (!rightIds.isEmpty()) {
                    core = split(DockSplitOrientation.HORIZONTAL, complement(rightPart), core, leaf(rightIds));
                }
                if (!leftIds.isEmpty()) {
                    DockLayoutSnapshot.Node column = leftLowerIds.isEmpty() ? leaf(leftIds)
                            : split(DockSplitOrientation.VERTICAL, leftUpperPart, leaf(leftIds), leaf(leftLowerIds));
                    core = split(DockSplitOrientation.HORIZONTAL, leftPart, column, core);
                }
                return new DockLayoutSnapshot(core, activeId);
            });
        }

        /** {@code 1 - share} без накопления ошибки float: для 0.35 получается ровно привычное 0.65. */
        private static float complement(float share) {
            return java.math.BigDecimal.ONE.subtract(new java.math.BigDecimal(Float.toString(share))).floatValue();
        }

        private static DockLayoutSnapshot.Node leaf(List<String> ids) {
            return new DockLayoutSnapshot.Node(DockNode.Kind.LEAF, null, 1.0f, ids,
                    ids.isEmpty() ? "" : ids.get(0), null, null);
        }

        private static DockLayoutSnapshot.Node split(DockSplitOrientation orientation, float ratio,
                                                     DockLayoutSnapshot.Node first, DockLayoutSnapshot.Node second) {
            return new DockLayoutSnapshot.Node(DockNode.Kind.SPLIT, orientation, ratio, new ArrayList<>(), "",
                    first, second);
        }
    }
}

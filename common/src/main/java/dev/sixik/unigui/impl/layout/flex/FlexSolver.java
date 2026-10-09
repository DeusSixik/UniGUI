package dev.sixik.unigui.impl.layout.flex;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.AlignContent;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.AutoMargins;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Единое flexbox-ядро для живого пути виджетов и пути снапшотов.
 *
 * <p>Терминология следует CSS Flexbox Level 1: главная ось (main axis) — направление flex,
 * поперечная ось (cross axis) ей перпендикулярна. <b>Гипотетический главный размер</b>
 * каждого элемента (flex base) вычисляется из {@code flex-basis} поверх главного размера,
 * затем ограничивается min/max и распределяется через grow/shrink. При включённом переносе
 * элементы группируются во flex-<b>линии</b> (lines).</p>
 *
 * <p>Решатель не зависит от измерения: фронтенды заранее измеряют детей и передают размеры
 * контента через {@link ItemInput}. Размещения возвращаются в координатах контента контейнера
 * на главной/поперечной осях; фронтенды отображают их в x/y и применяют к виджетам или
 * результатам снапшотов. Абсолютно позиционированные дети и фильтрация видимости остаются
 * во фронтендах.</p>
 */
public final class FlexSolver {
    private static final float EPSILON = 0.001f;

    private FlexSolver() {
    }

    /**
     * Конфигурация flex-контейнера на главной/поперечной осях.
     *
     * @param direction направление главной оси
     * @param wrap переносятся ли дети на несколько линий
     * @param padding внутренний отступ контейнера; размеры контента его исключают
     * @param mainGap промежуток между элементами на главной оси
     * @param crossGap промежуток между линиями на поперечной оси
     * @param alignItems выравнивание детей по поперечной оси по умолчанию
     * @param alignContent распределение линий на поперечной оси
     * @param justifyContent распределение свободного места на главной оси
     */
    public record ContainerConfig(
            FlexDirection direction,
            FlexWrap wrap,
            EdgeInsets padding,
            float mainGap,
            float crossGap,
            Align alignItems,
            AlignContent alignContent,
            Justify justifyContent) {
        public ContainerConfig {
            direction = direction == null ? FlexDirection.COLUMN : direction;
            wrap = wrap == null ? FlexWrap.NOWRAP : wrap;
            padding = padding == null ? EdgeInsets.ZERO : padding;
            mainGap = sanitizeGap(mainGap);
            crossGap = sanitizeGap(crossGap);
            alignItems = alignItems == null ? Align.STRETCH : alignItems;
            alignContent = alignContent == null ? AlignContent.STRETCH : alignContent;
            justifyContent = justifyContent == null ? Justify.START : justifyContent;
        }

        /**
         * Строит конфигурацию из стиля контейнера с явными переопределениями осей.
         *
         * <p>Живые контейнеры передают направление/перенос/промежутки из состояния ориентации;
         * путь снапшотов передаёт собственные значения стиля узла.</p>
         *
         * @param container стиль контейнера или {@code null} для значений по умолчанию
         * @param direction направление главной оси
         * @param wrap режим переноса
         * @param rowGap вертикальный промежуток
         * @param columnGap горизонтальный промежуток
         * @return конфигурация контейнера с промежутками, отображёнными на главную/поперечную оси
         */
        public static ContainerConfig of(FlexStyleView container,
                                         FlexDirection direction,
                                         FlexWrap wrap,
                                         float rowGap,
                                         float columnGap) {
            FlexDirection resolved = direction == null ? FlexDirection.COLUMN : direction;
            boolean row = isRow(resolved);
            EdgeInsets padding = container == null ? EdgeInsets.ZERO : container.padding();
            Align alignItems = container == null ? Align.STRETCH : container.alignItems();
            AlignContent alignContent = container == null ? AlignContent.STRETCH : container.alignContent();
            Justify justify = container == null ? Justify.START : container.justifyContent();
            return new ContainerConfig(resolved, wrap, padding,
                    row ? columnGap : rowGap, row ? rowGap : columnGap,
                    alignItems, alignContent, justify);
        }

        private static float sanitizeGap(float value) {
            return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
        }
    }

    /**
     * Один участвующий flex-элемент с заранее измеренным размером контента.
     *
     * @param style стиль элемента
     * @param legacyCrossFallback устаревшее выравнивание по поперечной оси для живого пути
     *                            или {@code null} для чистого CSS-поведения выравнивания
     * @param measuredMain заранее измеренный размер контента на главной оси
     * @param measuredCross заранее измеренный размер контента на поперечной оси
     * @param baseline базовая линия контента от начального края поперечной оси рамки
     *                 или {@code NaN}, если у элемента её нет
     * @param order порядок раскладки; элементы сортируются по возрастанию, равные сохраняют порядок дерева
     * @param intrinsic собственные размеры контента или {@code null}, если не измерены
     */
    public record ItemInput(
            FlexStyleView style,
            Alignment legacyCrossFallback,
            float measuredMain,
            float measuredCross,
            float baseline,
            int order,
            IntrinsicContent intrinsic) {
        public ItemInput {
            Objects.requireNonNull(style, "style");
            measuredMain = sanitizeMeasured(measuredMain);
            measuredCross = sanitizeMeasured(measuredCross);
        }

        /**
         * Создаёт входные данные элемента с порядком по умолчанию.
         *
         * @param style стиль элемента
         * @param legacyCrossFallback устаревшее выравнивание по поперечной оси или {@code null}
         * @param measuredMain заранее измеренный размер контента на главной оси
         * @param measuredCross заранее измеренный размер контента на поперечной оси
         */
        public ItemInput(FlexStyleView style,
                         Alignment legacyCrossFallback,
                         float measuredMain,
                         float measuredCross) {
            this(style, legacyCrossFallback, measuredMain, measuredCross, Float.NaN, style.order(), null);
        }

        /**
         * Создаёт входные данные элемента с базовой линией и собственными размерами по умолчанию.
         *
         * @param style стиль элемента
         * @param legacyCrossFallback устаревшее выравнивание по поперечной оси или {@code null}
         * @param measuredMain заранее измеренный размер контента на главной оси
         * @param measuredCross заранее измеренный размер контента на поперечной оси
         * @param baseline базовая линия контента или {@code NaN}
         * @param order порядок раскладки
         */
        public ItemInput(FlexStyleView style,
                         Alignment legacyCrossFallback,
                         float measuredMain,
                         float measuredCross,
                         float baseline,
                         int order) {
            this(style, legacyCrossFallback, measuredMain, measuredCross, baseline, order, null);
        }

        private static float sanitizeMeasured(float value) {
            return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
        }
    }

    /**
     * Вычисленный прямоугольник элемента в координатах контента контейнера на главной/поперечной осях.
     * Отступы (margins) уже включены в {@code mainStart}/{@code crossStart}.
     *
     * @param index индекс во входном списке фронтенда
     * @param mainStart смещение на главной оси с учётом начального отступа
     * @param crossStart смещение на поперечной оси с учётом начального отступа
     * @param mainSize вычисленный размер контента на главной оси
     * @param crossSize вычисленный размер контента на поперечной оси
     */
    public record CellPlacement(
            int index,
            float mainStart,
            float crossStart,
            float mainSize,
            float crossSize) {
    }

    /**
     * Желаемый размер контента без отступов контейнера.
     *
     * @param main размер контента на главной оси
     * @param cross размер контента на поперечной оси
     */
    public record ContentSize(float main, float cross) {
        public ContentSize {
            main = Float.isFinite(main) ? Math.max(0.0f, main) : 0.0f;
            cross = Float.isFinite(cross) ? Math.max(0.0f, cross) : 0.0f;
        }
    }

    /**
     * Измеряет желаемый размер контента для уже измеренных элементов.
     *
     * @param config конфигурация контейнера
     * @param items участвующие элементы
     * @param availableMain доступный размер на главной оси
     * @param availableCross доступный размер на поперечной оси
     * @return размер контента без отступов контейнера
     */
    public static ContentSize measureContent(ContainerConfig config,
                                             List<ItemInput> items,
                                             float availableMain,
                                             float availableCross) {
        if (config == null || items == null || items.isEmpty()) {
            return new ContentSize(0.0f, 0.0f);
        }
        List<Item> resolved = buildItems(config, items, availableMain, availableCross);
        List<Line> lines = buildLines(resolved, availableMain, config.mainGap(),
                isWrapping(config.wrap()), false);
        float main = 0.0f;
        float cross = 0.0f;
        for (int index = 0; index < lines.size(); index++) {
            Line line = lines.get(index);
            main = Math.max(main, line.outerBaseMain(config.mainGap()));
            cross += line.lineCrossSize();
            if (index > 0) {
                cross += config.crossGap();
            }
        }
        return new ContentSize(main, cross);
    }

    /**
     * Раскладывает уже измеренные элементы внутри заданного размера контента.
     *
     * @param config конфигурация контейнера
     * @param items участвующие элементы
     * @param availableMain размер контента на главной оси
     * @param availableCross размер контента на поперечной оси
     * @return размещения в координатах главной/поперечной осей контента контейнера
     */
    public static List<CellPlacement> arrange(ContainerConfig config,
                                              List<ItemInput> items,
                                              float availableMain,
                                              float availableCross) {
        List<CellPlacement> output = new ArrayList<>();
        if (config == null || items == null || items.isEmpty()) {
            return output;
        }
        List<Item> resolved = buildItems(config, items, availableMain, availableCross);
        boolean wrapping = isWrapping(config.wrap());
        List<Line> lines = buildLines(resolved, availableMain, config.mainGap(), wrapping, true);
        if (!wrapping) {
            float crossCursor = 0.0f;
            for (Line line : lines) {
                arrangeLine(config, line, availableMain, availableCross, crossCursor, output);
                crossCursor += availableCross + config.crossGap();
            }
            return mirrorReverse(config, output, availableMain, availableCross);
        }
        float totalCross = 0.0f;
        for (Line line : lines) {
            totalCross += line.lineCrossSize();
        }
        totalCross += config.crossGap() * (lines.size() - 1);
        float free = Math.max(0.0f, availableCross - totalCross);
        float crossCursor = 0.0f;
        float lineGap = config.crossGap();
        float stretchShare = 0.0f;
        switch (config.alignContent()) {
            case CENTER -> crossCursor = free * 0.5f;
            case END -> crossCursor = free;
            case SPACE_BETWEEN -> {
                if (lines.size() > 1) {
                    lineGap += free / (lines.size() - 1);
                }
            }
            case SPACE_AROUND -> {
                float share = free / lines.size();
                crossCursor = share * 0.5f;
                lineGap += share;
            }
            case SPACE_EVENLY -> {
                float share = free / (lines.size() + 1);
                crossCursor = share;
                lineGap += share;
            }
            case START -> {
            }
            case STRETCH -> {
                if (Float.isFinite(availableCross)) {
                    stretchShare = free / lines.size();
                }
            }
        }
        for (Line line : lines) {
            float lineCross = line.lineCrossSize() + stretchShare;
            arrangeLine(config, line, availableMain, lineCross, crossCursor, output);
            crossCursor += lineCross + lineGap;
        }
        return mirrorReverse(config, output, availableMain, availableCross);
    }

    private static List<CellPlacement> mirrorReverse(ContainerConfig config,
                                                     List<CellPlacement> placements,
                                                     float availableMain,
                                                     float availableCross) {
        boolean main = isReverse(config.direction()) && Float.isFinite(availableMain);
        boolean cross = isCrossReverse(config.wrap()) && Float.isFinite(availableCross);
        if (!main && !cross) {
            return placements;
        }
        List<CellPlacement> output = new ArrayList<>(placements.size());
        for (CellPlacement cell : placements) {
            float mainStart = main
                    ? availableMain - (cell.mainStart() + cell.mainSize())
                    : cell.mainStart();
            float crossStart = cross
                    ? availableCross - (cell.crossStart() + cell.crossSize())
                    : cell.crossStart();
            output.add(new CellPlacement(cell.index(), mainStart, crossStart, cell.mainSize(), cell.crossSize()));
        }
        return output;
    }

    private static List<Item> buildItems(ContainerConfig config,
                                         List<ItemInput> inputs,
                                         float availableMain,
                                         float availableCross) {
        float availableWidth = isRow(config.direction()) ? availableMain : availableCross;
        float availableHeight = isRow(config.direction()) ? availableCross : availableMain;
        List<Item> output = new ArrayList<>(inputs.size());
        for (int index = 0; index < inputs.size(); index++) {
            output.add(new Item(index, inputs.get(index), config.direction(), availableWidth, availableHeight));
        }
        for (Item item : output) {
            item.effectiveAlign = item.crossAlignment(config.alignItems());
        }
        output.sort((first, second) -> {
            int compared = Integer.compare(first.order, second.order);
            return compared != 0 ? compared : Integer.compare(first.index, second.index);
        });
        return output;
    }

    private static List<Line> buildLines(List<Item> items,
                                         float availableMain,
                                         float gap,
                                         boolean wrapping,
                                         boolean clampOversizedWrapItems) {
        List<Line> lines = new ArrayList<>();
        Line line = new Line();
        for (Item item : items) {
            if (wrapping && clampOversizedWrapItems && Float.isFinite(availableMain)) {
                item.baseMain = Math.min(item.baseMain, Math.max(0.0f, availableMain - item.mainMargin()));
                item.resolvedMain = item.baseMain;
            }
            float candidate = line.items.isEmpty()
                    ? item.outerBaseMain()
                    : line.outerBaseMain(gap) + gap + item.outerBaseMain();
            if (wrapping && !line.items.isEmpty() && Float.isFinite(availableMain) && candidate > availableMain) {
                lines.add(line);
                line = new Line();
            }
            line.add(item);
        }
        if (!line.items.isEmpty()) {
            lines.add(line);
        }
        return lines;
    }

    private static void arrangeLine(ContainerConfig config,
                                    Line line,
                                    float availableMain,
                                    float lineCross,
                                    float crossStart,
                                    List<CellPlacement> output) {
        resolveMainSizes(line.items, availableMain, config.mainGap());
        float occupied = totalResolvedMain(line.items) + totalMainMargins(line.items)
                + config.mainGap() * Math.max(0, line.items.size() - 1);
        float free = Math.max(0.0f, availableMain - occupied);
        int autoMainCount = 0;
        for (Item item : line.items) {
            autoMainCount += item.autoMainCount();
        }
        float autoShare = autoMainCount > 0 ? free / autoMainCount : 0.0f;
        float freeForJustify = autoMainCount > 0
                ? Math.max(0.0f, free - autoShare * autoMainCount)
                : free;
        float offset = 0.0f;
        float gap = config.mainGap();
        Justify justify = config.justifyContent();
        if (justify == Justify.CENTER) {
            offset = freeForJustify * 0.5f;
        } else if (justify == Justify.END) {
            offset = freeForJustify;
        } else if (justify == Justify.SPACE_BETWEEN && line.items.size() > 1) {
            gap += freeForJustify / (line.items.size() - 1);
        } else if (justify == Justify.SPACE_AROUND && !line.items.isEmpty()) {
            float share = freeForJustify / line.items.size();
            offset = share * 0.5f;
            gap += share;
        } else if (justify == Justify.SPACE_EVENLY && !line.items.isEmpty()) {
            float share = freeForJustify / (line.items.size() + 1);
            offset = share;
            gap += share;
        }

        FlexDirection direction = config.direction();
        boolean row = isRow(direction);
        float cursor = offset;
        for (Item item : line.items) {
            EdgeInsets margin = item.style.margin();
            AutoMargins auto = item.style.marginAuto();
            boolean mainBeforeAuto = row ? auto.left() : auto.top();
            boolean mainAfterAuto = row ? auto.right() : auto.bottom();
            float mainBefore = mainBeforeAuto ? autoShare : (row ? margin.left() : margin.top());
            float mainAfter = mainAfterAuto ? autoShare : (row ? margin.right() : margin.bottom());
            boolean crossBeforeAuto = row ? auto.top() : auto.left();
            boolean crossAfterAuto = row ? auto.bottom() : auto.right();
            float crossPxBefore = crossBeforeAuto ? 0.0f : (row ? margin.top() : margin.left());
            float crossPxAfter = crossAfterAuto ? 0.0f : (row ? margin.bottom() : margin.right());
            float crossAvailable = Math.max(0.0f, lineCross - crossPxBefore - crossPxAfter);
            Alignment alignment = item.effectiveAlign;
            float crossSize = item.resolveCrossSize(crossAvailable, alignment);
            float crossBefore;
            float crossOffset;
            if (row && alignment == Alignment.BASELINE && Float.isFinite(item.baseline)) {
                crossBefore = crossPxBefore;
                crossOffset = Math.max(0.0f, line.maxBaseline - item.baselineOuter());
                crossSize = item.resolveCrossSize(crossAvailable, Alignment.START);
            } else if (crossBeforeAuto && crossAfterAuto) {
                crossBefore = 0.0f;
                crossOffset = Math.max(0.0f, crossAvailable - crossSize) * 0.5f;
            } else if (crossBeforeAuto) {
                crossBefore = Math.max(0.0f, crossAvailable - crossSize);
                crossOffset = 0.0f;
            } else if (crossAfterAuto) {
                crossBefore = 0.0f;
                crossOffset = 0.0f;
            } else {
                crossBefore = crossPxBefore;
                crossOffset = alignOffset(crossAvailable, crossSize, alignment);
            }
            output.add(new CellPlacement(item.index,
                    cursor + mainBefore, crossStart + crossBefore + crossOffset,
                    item.resolvedMain, crossSize));
            cursor += item.resolvedMain + mainBefore + mainAfter + gap;
        }
    }

    private static void resolveMainSizes(List<Item> items, float availableMain, float gap) {
        for (Item item : items) {
            item.resolvedMain = clamp(item.baseMain, item.minMain, item.maxMain);
        }
        float availableForContent = Math.max(0.0f,
                availableMain - gap * Math.max(0, items.size() - 1) - totalMainMargins(items));
        float contentTotal = totalResolvedMain(items);
        if (contentTotal < availableForContent) {
            grow(items, availableForContent - contentTotal);
        } else if (contentTotal > availableForContent) {
            shrink(items, contentTotal - availableForContent);
        }
    }

    private static void grow(List<Item> items, float remaining) {
        while (remaining > EPSILON) {
            float totalGrow = 0.0f;
            for (Item item : items) {
                if (item.grow > 0.0f && item.resolvedMain + EPSILON < item.maxMain) {
                    totalGrow += item.grow;
                }
            }
            if (totalGrow <= 0.0f) {
                return;
            }
            float consumed = 0.0f;
            for (Item item : items) {
                if (item.grow <= 0.0f || item.resolvedMain + EPSILON >= item.maxMain) {
                    continue;
                }
                float share = remaining * (item.grow / totalGrow);
                float next = Math.min(item.maxMain, item.resolvedMain + share);
                consumed += next - item.resolvedMain;
                item.resolvedMain = next;
            }
            if (consumed <= EPSILON) {
                return;
            }
            remaining -= consumed;
        }
    }

    private static void shrink(List<Item> items, float remaining) {
        while (remaining > EPSILON) {
            float totalWeight = 0.0f;
            for (Item item : items) {
                if (item.shrink > 0.0f && item.resolvedMain > item.minMain + EPSILON) {
                    totalWeight += item.shrink * item.baseMain;
                }
            }
            if (totalWeight <= 0.0f) {
                return;
            }
            float consumed = 0.0f;
            for (Item item : items) {
                if (item.shrink <= 0.0f || item.resolvedMain <= item.minMain + EPSILON) {
                    continue;
                }
                float share = remaining * (item.shrink * item.baseMain / totalWeight);
                float next = Math.max(item.minMain, item.resolvedMain - share);
                consumed += item.resolvedMain - next;
                item.resolvedMain = next;
            }
            if (consumed <= EPSILON) {
                return;
            }
            remaining -= consumed;
        }
    }

    private static float totalResolvedMain(List<Item> items) {
        float output = 0.0f;
        for (Item item : items) {
            output += item.resolvedMain;
        }
        return output;
    }

    private static float totalMainMargins(List<Item> items) {
        float output = 0.0f;
        for (Item item : items) {
            output += item.mainMargin();
        }
        return output;
    }

    private static float alignOffset(float available, float size, Alignment alignment) {
        return switch (alignment == null ? Alignment.STRETCH : alignment) {
            case CENTER -> Math.max(0.0f, available - size) * 0.5f;
            case END -> Math.max(0.0f, available - size);
            case START, STRETCH, BASELINE -> 0.0f;
        };
    }

    private static Alignment toAlignment(Align align) {
        return switch (align == null ? Align.STRETCH : align) {
            case START -> Alignment.START;
            case CENTER -> Alignment.CENTER;
            case END -> Alignment.END;
            case BASELINE -> Alignment.BASELINE;
            case AUTO, STRETCH -> Alignment.STRETCH;
        };
    }

    /**
     * Проверяет, горизонтальна ли главная ось (row или row-reverse).
     *
     * @param direction направление flex
     * @return {@code true} для осей-строк
     */
    public static boolean isRow(FlexDirection direction) {
        return direction == FlexDirection.ROW || direction == FlexDirection.ROW_REVERSE;
    }

    /**
     * Проверяет, реверсирована ли главная ось.
     *
     * @param direction направление flex
     * @return {@code true} для row-reverse и column-reverse
     */
    public static boolean isReverse(FlexDirection direction) {
        return direction == FlexDirection.ROW_REVERSE || direction == FlexDirection.COLUMN_REVERSE;
    }

    /**
     * Проверяет, переносятся ли дети на несколько линий.
     *
     * @param wrap режим переноса
     * @return {@code true} для wrap и wrap-reverse
     */
    public static boolean isWrapping(FlexWrap wrap) {
        return wrap == FlexWrap.WRAP || wrap == FlexWrap.WRAP_REVERSE;
    }

    /**
     * Проверяет, складываются ли линии в обратном порядке поперечной оси.
     *
     * @param wrap режим переноса
     * @return {@code true} для wrap-reverse
     */
    public static boolean isCrossReverse(FlexWrap wrap) {
        return wrap == FlexWrap.WRAP_REVERSE;
    }

    /**
     * Вычисляет явный размер относительно доступного места с запасным значением.
     *
     * @param value значение размера или {@code null} для запасного значения
     * @param available доступное место; проценты откатываются при неопределённом месте
     * @param fallback значение для {@code auto}
     * @return вычисленный размер
     */
    public static float resolveSize(SizeValue value, float available, float fallback) {
        if (value == null || value.isAuto() || value.isContent()) {
            return fallback;
        }
        if (value.isPercent()) {
            return Float.isFinite(available) ? Math.max(0.0f, available * value.value() / 100.0f) : fallback;
        }
        return value.value();
    }

    /**
     * Вычисляет максимальный размер; {@code auto} означает отсутствие ограничения.
     *
     * @param value значение размера или {@code null}
     * @param available доступное место
     * @return вычисленный максимум
     */
    public static float resolveMaximum(SizeValue value, float available) {
        if (value == null || value.isAuto()) {
            return Float.POSITIVE_INFINITY;
        }
        return resolveSize(value, available, Float.POSITIVE_INFINITY);
    }

    /**
     * Вычитает занятое место из доступного, сохраняя бесконечность бесконечной.
     *
     * @param available доступное место
     * @param consumed занятое место
     * @return оставшееся место
     */
    public static float subtractAvailable(float available, float consumed) {
        return Float.isFinite(available)
                ? Math.max(0.0f, available - Math.max(0.0f, consumed))
                : Float.POSITIVE_INFINITY;
    }

    /**
     * Вычисляет сдвиг относительного позиционирования на горизонтальной оси.
     *
     * @param style стиль элемента или {@code null}
     * @param hostWidth ширина содержащего блока для процентных отступов
     * @return сдвиг в пикселях, ноль для неотносительно позиционированных
     */
    public static float relativeOffsetX(FlexStyleView style, float hostWidth) {
        if (style == null || style.position() != PositionType.RELATIVE) {
            return 0.0f;
        }
        float left = resolveInset(style.left(), hostWidth);
        if (Float.isFinite(left)) {
            return left;
        }
        float right = resolveInset(style.right(), hostWidth);
        return Float.isFinite(right) ? -right : 0.0f;
    }

    /**
     * Вычисляет сдвиг относительного позиционирования на вертикальной оси.
     *
     * @param style стиль элемента или {@code null}
     * @param hostHeight высота содержащего блока для процентных отступов
     * @return сдвиг в пикселях, ноль для неотносительно позиционированных
     */
    public static float relativeOffsetY(FlexStyleView style, float hostHeight) {
        if (style == null || style.position() != PositionType.RELATIVE) {
            return 0.0f;
        }
        float top = resolveInset(style.top(), hostHeight);
        if (Float.isFinite(top)) {
            return top;
        }
        float bottom = resolveInset(style.bottom(), hostHeight);
        return Float.isFinite(bottom) ? -bottom : 0.0f;
    }

    /**
     * Вычисляет одну сторону отступа относительно доступного места.
     *
     * @param value значение отступа или {@code null}
     * @param available доступное место
     * @return вычисленный отступ или {@code NaN} для {@code auto}
     */
    public static float resolveInset(SizeValue value, float available) {
        if (value == null || value.isAuto()) {
            return Float.NaN;
        }
        return resolveSize(value, available, Float.NaN);
    }

    /**
     * Собственные размеры контента одного элемента, если измерены.
     *
     * @param minMain минимальный контент на главной оси
     * @param maxMain максимальный контент на главной оси
     * @param minCross минимальный контент на поперечной оси
     * @param maxCross максимальный контент на поперечной оси
     */
    public record IntrinsicContent(float minMain, float maxMain, float minCross, float maxCross) {
    }

    /**
     * Вычисляет собственное или обычное значение размера.
     *
     * @param value значение размера или {@code null} для запасного значения
     * @param minContent минимальный размер контента или {@code NaN}
     * @param maxContent максимальный размер контента или {@code NaN}
     * @param available доступное место для процентов
     * @param fallback значение для {@code auto}
     * @return вычисленный размер
     */
    public static float resolveIntrinsic(SizeValue value,
                                          float minContent,
                                          float maxContent,
                                          float available,
                                          float fallback) {
        if (value == null || value.isAuto()) {
            return fallback;
        }
        if (value.isMinContent()) {
            return Float.isFinite(minContent) ? Math.max(0.0f, minContent) : fallback;
        }
        if (value.isMaxContent()) {
            return Float.isFinite(maxContent) ? Math.max(0.0f, maxContent) : fallback;
        }
        if (value.isFitContent()) {
            float min = Float.isFinite(minContent) ? Math.max(0.0f, minContent) : 0.0f;
            float max = Float.isFinite(maxContent) ? Math.max(0.0f, maxContent) : fallback;
            return Math.min(max, Math.max(min, value.value()));
        }
        return resolveSize(value, available, fallback);
    }

    /**
     * Проверяет, нужны ли flex-разрешению собственные размеры контента элемента.
     *
     * @param style стиль элемента или {@code null}
     * @param row горизонтальна ли главная ось
     * @return {@code true}, если любой размер main/min/max/cross собственный или минимум — auto
     */
    public static boolean needsIntrinsicContent(FlexStyleView style, boolean row) {
        if (style == null) {
            return false;
        }
        SizeValue main = row ? style.width() : style.height();
        SizeValue cross = row ? style.height() : style.width();
        SizeValue min = row ? style.minWidth() : style.minHeight();
        SizeValue max = row ? style.maxWidth() : style.maxHeight();
        SizeValue crossMin = row ? style.minHeight() : style.minWidth();
        SizeValue crossMax = row ? style.maxHeight() : style.maxWidth();
        SizeValue basis = style.flexBasis();
        return isIntrinsic(main)
                || isAutoOrIntrinsic(min)
                || isIntrinsic(max)
                || isIntrinsic(cross)
                || isAutoOrIntrinsic(crossMin)
                || isIntrinsic(crossMax)
                || isIntrinsic(basis);
    }

    private static boolean isIntrinsic(SizeValue value) {
        return value != null && value.isIntrinsic();
    }

    private static boolean isAutoOrIntrinsic(SizeValue value) {
        return value != null && (value.isIntrinsic() || value.isAuto());
    }

    /**
     * Проверяет, определён ли размер относительно доступного места.
     *
     * @param value значение размера или {@code null}
     * @param available доступное место
     * @return {@code true} для пикселей и для процентов при конечном доступном месте
     */
    public static boolean isDefiniteSize(SizeValue value, float available) {
        if (value == null || value.isAuto() || value.isContent() || value.isIntrinsic()) {
            return false;
        }
        if (value.isPercent()) {
            return Float.isFinite(available);
        }
        return true;
    }

    /**
     * Ограничивает размер между минимумом и максимумом, не ниже нуля.
     *
     * @param value ограничиваемое значение
     * @param min минимум
     * @param max максимум
     * @return ограниченное значение
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, Math.max(0.0f, value)));
    }

    private static final class Line {
        private final List<Item> items = new ArrayList<>();
        private float crossSize;
        private float maxBaseline;
        private float maxDescent;
        private boolean hasBaseline;

        private void add(Item item) {
            items.add(item);
            crossSize = Math.max(crossSize, item.outerCross());
            if (item.effectiveAlign == Alignment.BASELINE && Float.isFinite(item.baseline)) {
                float outer = item.baselineOuter();
                float descent = Math.max(0.0f, item.outerCross() - outer);
                if (!hasBaseline || outer > maxBaseline) {
                    maxBaseline = outer;
                }
                maxDescent = Math.max(maxDescent, descent);
                hasBaseline = true;
            }
        }

        private float lineCrossSize() {
            return hasBaseline ? Math.max(crossSize, maxBaseline + maxDescent) : crossSize;
        }

        private float outerBaseMain(float gap) {
            float output = 0.0f;
            for (Item item : items) {
                output += item.outerBaseMain();
            }
            return output + gap * Math.max(0, items.size() - 1);
        }
    }

    private static final class Item {
        private final int index;
        private final int order;
        private final FlexStyleView style;
        private final Alignment legacyCrossFallback;
        private final FlexDirection direction;
        private final float availableWidth;
        private final float availableHeight;
        private final float measuredMain;
        private final float measuredCross;
        private final float baseline;
        private final IntrinsicContent intrinsicContent;
        private final float minMain;
        private final float maxMain;
        private final float grow;
        private final float shrink;
        private final float aspect;
        private final boolean mainFromRatio;
        private final boolean crossFollowsMain;
        private Alignment effectiveAlign;
        private float baseMain;
        private float resolvedMain;

        private Item(int index,
                     ItemInput input,
                     FlexDirection direction,
                     float availableWidth,
                     float availableHeight) {
            this.index = index;
            this.order = input.order();
            this.style = input.style();
            this.legacyCrossFallback = input.legacyCrossFallback();
            this.direction = direction;
            this.availableWidth = availableWidth;
            this.availableHeight = availableHeight;
            this.measuredMain = input.measuredMain();
            this.measuredCross = input.measuredCross();
            this.baseline = input.baseline();
            this.intrinsicContent = input.intrinsic();
            this.effectiveAlign = Alignment.STRETCH;
            float availableMain = isRow(direction) ? availableWidth : availableHeight;
            SizeValue mainSize = isRow(direction) ? style.width() : style.height();
            SizeValue crossValueAxis = isRow(direction) ? style.height() : style.width();
            float crossAvailAxis = isRow(direction) ? availableHeight : availableWidth;
            float contentMinMain = contentMin(true);
            float contentMaxMain = contentMax(true);
            float preferredMain = resolveIntrinsic(mainSize, contentMinMain, contentMaxMain, availableMain, measuredMain);
            SizeValue basis = style.flexBasis();
            boolean basisDefinite = basis != null && !basis.isAuto() && !basis.isContent() && !basis.isIntrinsic();
            boolean basisContent = basis != null && basis.isContent();
            boolean mainAuto = mainSize.isAuto() || mainSize.isContent();
            float aspectRatio = style.aspectRatio();
            boolean ratioOk = Float.isFinite(aspectRatio) && aspectRatio > 0.0f;
            boolean fromRatio = false;
            if (basisContent) {
                preferredMain = measuredMain;
            } else if (basisDefinite) {
                preferredMain = resolveSize(basis, availableMain, preferredMain);
            } else if (basis != null && basis.isIntrinsic()) {
                preferredMain = resolveIntrinsic(basis, contentMinMain, contentMaxMain, availableMain, preferredMain);
            } else if (ratioOk && mainAuto && isDefiniteSize(crossValueAxis, crossAvailAxis)) {
                float crossMin = resolveSize(
                        isRow(direction) ? style.minHeight() : style.minWidth(), crossAvailAxis, 0.0f);
                float crossMax = resolveMaximum(
                        isRow(direction) ? style.maxHeight() : style.maxWidth(), crossAvailAxis);
                float crossResolved = clamp(resolveSize(crossValueAxis, crossAvailAxis, 0.0f), crossMin, crossMax);
                preferredMain = crossResolved * (isRow(direction) ? aspectRatio : 1.0f / aspectRatio);
                fromRatio = true;
            } else if (style.flexGrow() > 0.0f && mainSize.isAuto()) {
                preferredMain = 0.0f;
            }
            SizeValue minValue = isRow(direction) ? style.minWidth() : style.minHeight();
            SizeValue maxValue = isRow(direction) ? style.maxWidth() : style.maxHeight();
            if (minValue.isAuto()) {
                this.minMain = Float.isFinite(contentMinMain) ? Math.max(0.0f, contentMinMain) : 0.0f;
            } else if (minValue.isIntrinsic()) {
                this.minMain = resolveIntrinsic(minValue, contentMinMain, contentMaxMain, availableMain, 0.0f);
            } else {
                this.minMain = resolveSize(minValue, availableMain, 0.0f);
            }
            if (maxValue.isIntrinsic()) {
                this.maxMain = resolveIntrinsic(maxValue, contentMinMain, contentMaxMain, availableMain,
                        Float.POSITIVE_INFINITY);
            } else {
                this.maxMain = resolveMaximum(maxValue, availableMain);
            }
            this.baseMain = clamp(preferredMain, minMain, maxMain);
            this.resolvedMain = baseMain;
            this.grow = style.flexGrow();
            this.shrink = style.flexShrink();
            this.aspect = aspectRatio;
            this.mainFromRatio = fromRatio;
            boolean crossAuto = crossValueAxis.isAuto() || crossValueAxis.isContent();
            this.crossFollowsMain = ratioOk && crossAuto
                    && (!mainAuto || basisDefinite || basisContent || fromRatio);
        }

        private float outerBaseMain() {
            return baseMain + mainMargin();
        }

        private float mainMargin() {
            AutoMargins auto = style.marginAuto();
            EdgeInsets margin = style.margin();
            if (isRow(direction)) {
                return (auto.left() ? 0.0f : margin.left()) + (auto.right() ? 0.0f : margin.right());
            }
            return (auto.top() ? 0.0f : margin.top()) + (auto.bottom() ? 0.0f : margin.bottom());
        }

        private float crossMargin() {
            AutoMargins auto = style.marginAuto();
            EdgeInsets margin = style.margin();
            if (isRow(direction)) {
                return (auto.top() ? 0.0f : margin.top()) + (auto.bottom() ? 0.0f : margin.bottom());
            }
            return (auto.left() ? 0.0f : margin.left()) + (auto.right() ? 0.0f : margin.right());
        }

        private int autoMainCount() {
            AutoMargins auto = style.marginAuto();
            if (isRow(direction)) {
                return auto.horizontalCount();
            }
            return (auto.top() ? 1 : 0) + (auto.bottom() ? 1 : 0);
        }

        private float contentMin(boolean mainAxis) {
            if (intrinsicContent == null) {
                return Float.NaN;
            }
            if (isRow(direction) == mainAxis) {
                return intrinsicContent.minMain();
            }
            return intrinsicContent.minCross();
        }

        private float contentMax(boolean mainAxis) {
            if (intrinsicContent == null) {
                return Float.NaN;
            }
            if (isRow(direction) == mainAxis) {
                return intrinsicContent.maxMain();
            }
            return intrinsicContent.maxCross();
        }

        private float outerCross() {
            float available = isRow(direction) ? availableHeight : availableWidth;
            SizeValue crossValue = isRow(direction) ? style.height() : style.width();
            SizeValue crossMin = isRow(direction) ? style.minHeight() : style.minWidth();
            SizeValue crossMax = isRow(direction) ? style.maxHeight() : style.maxWidth();
            float min = crossMin.isAuto()
                    ? (Float.isFinite(contentMin(false)) ? Math.max(0.0f, contentMin(false)) : 0.0f)
                    : crossMin.isIntrinsic()
                            ? resolveIntrinsic(crossMin, contentMin(false), contentMax(false), available, 0.0f)
                            : resolveSize(crossMin, available, 0.0f);
            float max = crossMax.isIntrinsic()
                    ? resolveIntrinsic(crossMax, contentMin(false), contentMax(false), available,
                            Float.POSITIVE_INFINITY)
                    : resolveMaximum(crossMax, available);
            float resolved;
            if (crossFollowsMain) {
                resolved = clamp(baseMain * crossFactor(), min, max);
            } else {
                resolved = clamp(resolveIntrinsic(crossValue, contentMin(false), contentMax(false),
                        available, measuredCross), min, max);
            }
            return resolved + crossMargin();
        }

        private float crossFactor() {
            return isRow(direction) ? 1.0f / aspect : aspect;
        }

        /**
         * Возвращает смещение базовой линии от начального края поперечной оси margin-области.
         *
         * @return базовая линия относительно margin-области или {@code NaN}
         */
        private float baselineOuter() {
            if (!Float.isFinite(baseline)) {
                return Float.NaN;
            }
            EdgeInsets margin = style.margin();
            AutoMargins auto = style.marginAuto();
            float before = isRow(direction)
                    ? (auto.top() ? 0.0f : margin.top())
                    : (auto.left() ? 0.0f : margin.left());
            return before + baseline;
        }

        private Alignment crossAlignment(Align parentAlign) {
            if (style.alignSelf() != Align.AUTO) {
                return toAlignment(style.alignSelf());
            }
            if (legacyCrossFallback != null) {
                if (legacyCrossFallback == Alignment.STRETCH && parentAlign != Align.STRETCH) {
                    return toAlignment(parentAlign);
                }
                return legacyCrossFallback;
            }
            return toAlignment(parentAlign);
        }

        private float resolveCrossSize(float available, Alignment alignment) {
            SizeValue crossValue = isRow(direction) ? style.height() : style.width();
            SizeValue crossMin = isRow(direction) ? style.minHeight() : style.minWidth();
            SizeValue crossMax = isRow(direction) ? style.maxHeight() : style.maxWidth();
            float min = crossMin.isAuto()
                    ? (Float.isFinite(contentMin(false)) ? Math.max(0.0f, contentMin(false)) : 0.0f)
                    : crossMin.isIntrinsic()
                            ? resolveIntrinsic(crossMin, contentMin(false), contentMax(false), available, 0.0f)
                            : resolveSize(crossMin, available, 0.0f);
            float max = crossMax.isIntrinsic()
                    ? resolveIntrinsic(crossMax, contentMin(false), contentMax(false), available,
                            Float.POSITIVE_INFINITY)
                    : resolveMaximum(crossMax, available);
            if (crossFollowsMain) {
                return clamp(resolvedMain * crossFactor(), min, max);
            }
            float resolved = resolveIntrinsic(crossValue, contentMin(false), contentMax(false),
                    available, measuredCross);
            if (alignment == Alignment.STRETCH && crossValue.isAuto()) {
                resolved = available;
            }
            return Math.min(available, clamp(resolved, min, max));
        }
    }
}

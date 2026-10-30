package dev.sixik.unigui.api.layout;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import dev.sixik.unigui.widgets.core.Orientation;

/**
 * Набор правил, по которым родитель вычисляет размер и положение виджета.
 *
 * <p>Layout (компоновка) — это автоматическое размещение элементов интерфейса.
 * Вместо того чтобы вручную задавать каждому виджету координаты экрана, вы
 * описываете отношения между ним и родителем: желаемый размер, отступы,
 * возможность занять свободное место и выравнивание. Когда содержимое или
 * размер родителя меняется, layout пересчитывает дочерние элементы.</p>
 *
 * <p>Обычно стиль настраивают прямо на виджете:</p>
 *
 * <pre>{@code
 * button.layout(style -> style
 *         .width(120.0f)       // ширина 120 UI-пикселей
 *         .height(32.0f)       // высота 32 UI-пикселя
 *         .flexNone());        // не растягивать и не сжимать flex-правилами
 * }</pre>
 *
 * <p>Для чтения остальных примеров полезны несколько простых понятий:</p>
 * <ul>
 *     <li><b>Родитель</b> — контейнер, который размещает виджет; <b>ребёнок</b> —
 *         виджет внутри контейнера.</li>
 *     <li><b>Предпочтительный размер</b> — размер, который виджет просит у layout.
 *         Это не всегда итоговый размер: ограничения родителя, flex-распределение
 *         и min/max могут его изменить.</li>
 *     <li><b>{@code auto}</b> — попросить layout подобрать размер по содержимому
 *         или доступному месту. Это не синоним 100%.</li>
 *     <li><b>Flex-контейнер</b> — контейнер, который размещает детей подряд в строку
 *         или колонку. Его направление задаёт главную ось: у {@link FlexDirection#ROW}
 *         это ширина, у {@link FlexDirection#COLUMN} — высота. Перпендикулярное
 *         направление называется поперечной осью.</li>
 *     <li><b>Свободное место</b> — часть размера родителя, которая осталась после
 *         размещения базовых размеров детей и отступов. {@code flexGrow} делит это
 *         место между детьми как вес, а {@code flexShrink} задаёт, кто уступает
 *         место, если его не хватает.</li>
 * </ul>
 *
 * <p>Внешний {@link #margin(EdgeInsets) margin} отделяет виджет от соседей.
 * Внутренний {@link #padding(EdgeInsets) padding} отодвигает содержимое виджета
 * от его краёв. Эти отступы влияют на размещение внутри контейнера, но не
 * перемещают сам родительский контейнер.</p>
 *
 * <p>Пример: заголовок сохраняет высоту, а область содержимого забирает остаток
 * высоты колонки и прокручивается при переполнении:</p>
 *
 * <pre>{@code
 * VBox root = new VBox();
 * header.layout(style -> style.height(24.0f).flexNone());
 * body.layout(style -> style.expand().overflowY(Overflow.AUTO));
 * root.addChild(header);
 * root.addChild(body);
 * }</pre>
 *
 * <p>Часто используемые сокращения: {@link #fixed(float, float)} задаёт размер,
 * который flex не меняет; {@link #fill()} просит 100% ширины и высоты;
 * {@link #expand()} забирает свободное место только по главной оси родителя;
 * {@link #center()} центрирует детей контейнера. Эти методы не взаимозаменяемы.</p>
 *
 * <p>API использует знакомые CSS-понятия, но не обещает полную совместимость с CSS.
 * Отличия и ограничения конкретных свойств описаны рядом с методами.</p>
 *
 * @see SizeValue
 * @see FlexDirection
 * @see Align
 * @see Justify
 * @see PositionType
 */
@AiDoc(
        summary = "Fluent rules used by the layout system to measure and place a widget.",
        purpose = "Defines size requests, spacing, flex/grid layout, positioning, and overflow without manually calculating screen coordinates.",
        useWhen = {
                "Configure a widget through `widget.layout(style -> ...)`.",
                "Implement a custom container or update several layout properties as one batch."
        },
        criticalRules = {
                "`width` and `height` are requested sizes, not guaranteed final sizes: the parent, flex/grid rules, and min/max constraints may change them.",
                "`fill()` requests 100% width and height; `expand()` takes remaining space only on the flex parent's main axis.",
                "`margin` is outside the widget and separates it from siblings; `padding` is inside and reduces content space.",
                "Grow/shrink are flex-distribution weights, not percentages or standalone dimensions.",
                "`justifyContent` works on the main axis, `alignItems` on the cross axis; `alignContent` matters only when wrapping creates multiple lines.",
                "In this implementation `PositionType.FIXED` is relative to the nearest layout host, not the screen or window.",
                "The CSS-like properties are a UniGUI layout subset and do not promise full CSS compatibility."
        },
        related = {"WidgetBase", "SizeValue", "EdgeInsets", "GridTrack", "FlexDirection", "PositionType"},
        examples = {
                @AiDoc.Example(
                        title = "Fixed header and expanding content",
                        order = 10,
                        code = """
                                VBox root = new VBox();
                                Label header = new Label("Files");
                                VBox content = new VBox();

                                header.layout(style -> style.height(24.0f).flexNone());
                                content.layout(style -> style.expand().padding(12.0f));
                                root.addChild(header);
                                root.addChild(content);
                                """),
                @AiDoc.Example(
                        title = "Grid with fixed and flexible columns",
                        order = 20,
                        code = """
                                LayoutStyle grid = new LayoutStyle()
                                        .gridTemplateColumns(GridTrack.px(96.0f), GridTrack.fr(1.0f))
                                        .gridAutoRowsOf(GridTrack.auto())
                                        .gap(8.0f, 12.0f);
                                """)
        })
@WikiPage(
        title = "<key:layout.style.title>",
        slug = "layout-style",
        category = "<key:layout.style.category>",
        summary = "<key:layout.style.summary>",
        order = 20)
@WikiSection(
        title = "<key:layout.style.section.overview.title>",
        markdown = "<key:layout.style.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:layout.style.section.size.title>",
        markdown = "<key:layout.style.section.size.body>",
        order = 20)
@WikiSection(
        title = "<key:layout.style.section.spacing.title>",
        markdown = "<key:layout.style.section.spacing.body>",
        order = 30)
@WikiSection(
        title = "<key:layout.style.section.flex.title>",
        markdown = "<key:layout.style.section.flex.body>",
        order = 40)
@WikiSection(
        title = "<key:layout.style.section.grid.title>",
        markdown = "<key:layout.style.section.grid.body>",
        order = 50)
@WikiSection(
        title = "<key:layout.style.section.update.title>",
        markdown = "<key:layout.style.section.update.body>",
        order = 60)
@WikiExample(
        title = "<key:layout.style.example.panel.title>",
        code = "<key:layout.style.example.panel.code>",
        order = 10)
@WikiExample(
        title = "<key:layout.style.example.grid.title>",
        code = "<key:layout.style.example.grid.code>",
        order = 20)
public final class LayoutStyle {
    /**
     * Callback, который уведомляет владельца об изменении правил компоновки.
     */
    private Runnable onChanged;
    /**
     * Глубина вложенных пакетных обновлений.
     */
    private int updateDepth;
    /**
     * Флаг отложенного уведомления до завершения пакетного обновления.
     */
    private boolean pendingChange;

    /**
     * Режим участия виджета в потоке компоновки.
     */
    private PositionType position = PositionType.RELATIVE;
    /**
     * Желаемая ширина; {@code auto} означает размер по содержимому или родителю.
     */
    private SizeValue width = SizeValue.auto();
    /**
     * Желаемая высота; {@code auto} означает размер по содержимому или родителю.
     */
    private SizeValue height = SizeValue.auto();
    /**
     * Минимальная ширина; по умолчанию ограничение отсутствует кроме нуля.
     */
    private SizeValue minWidth = SizeValue.px(0.0f);
    /**
     * Минимальная высота; по умолчанию ограничение отсутствует кроме нуля.
     */
    private SizeValue minHeight = SizeValue.px(0.0f);
    /**
     * Максимальная ширина; {@code auto} означает отсутствие верхней границы.
     */
    private SizeValue maxWidth = SizeValue.auto();
    /**
     * Максимальная высота; {@code auto} означает отсутствие верхней границы.
     */
    private SizeValue maxHeight = SizeValue.auto();
    /**
     * Фиксированные внешние отступы вокруг виджета в UI-пикселях.
     */
    private EdgeInsets margin = EdgeInsets.ZERO;
    /**
     * Стороны внешнего отступа, которым разрешено поглощать свободное место.
     */
    private AutoMargins marginAuto = AutoMargins.NONE;
    /**
     * Внутренние отступы между краем контейнера и его содержимым в UI-пикселях.
     */
    private EdgeInsets padding = EdgeInsets.ZERO;
    /**
     * Правило отображения содержимого, выходящего за левую или правую границу.
     */
    private Overflow overflowX = Overflow.VISIBLE;
    /**
     * Правило отображения содержимого, выходящего за верхнюю или нижнюю границу.
     */
    private Overflow overflowY = Overflow.VISIBLE;
    /**
     * Направление главной оси flex-контейнера.
     */
    private FlexDirection flexDirection = FlexDirection.COLUMN;
    /**
     * Определяет, переносить ли детей flex-контейнера на дополнительные строки или колонки.
     */
    private FlexWrap flexWrap = FlexWrap.NOWRAP;
    /**
     * Расстояние между строками/линиями в UI-пикселях.
     */
    private float rowGap;
    /**
     * Расстояние между колонками/детьми в строке в UI-пикселях.
     */
    private float columnGap;
    /**
     * Вес для получения свободного места на главной оси; не процент.
     */
    private float flexGrow;
    /**
     * Вес для уступания места при нехватке размера на главной оси.
     */
    private float flexShrink;
    /**
     * Базовый размер до применения flexGrow/flexShrink.
     */
    private SizeValue flexBasis = SizeValue.auto();
    /**
     * Дополнительный порядок среди соседей; меньшее значение размещается раньше.
     */
    private int order;
    /**
     * Желаемое отношение ширины к высоте или {@code NaN}, если оно не задано.
     */
    private float aspectRatio = Float.NaN;
    /**
     * Шаблон явных колонок сетки.
     */
    private List<GridTrack> gridTemplateColumns = List.of();
    /**
     * Шаблон явных строк сетки.
     */
    private List<GridTrack> gridTemplateRows = List.of();
    /**
     * Шаблоны колонок, которые сетка создаёт автоматически.
     */
    private List<GridTrack> gridAutoColumns = List.of();
    /**
     * Шаблоны строк, которые сетка создаёт автоматически.
     */
    private List<GridTrack> gridAutoRows = List.of();
    /**
     * Направление автоматического размещения детей сетки.
     */
    private GridAutoFlow gridAutoFlow = GridAutoFlow.ROW;
    /**
     * Начальная колонка ребёнка в сетке; неположительное значение означает автоматическое место.
     */
    private int gridColumnStart;
    /**
     * Число колонок, занимаемых ребёнком сетки.
     */
    private int gridColumnSpan = 1;
    /**
     * Начальная строка ребёнка в сетке; неположительное значение означает автоматическое место.
     */
    private int gridRowStart;
    /**
     * Число строк, занимаемых ребёнком сетки.
     */
    private int gridRowSpan = 1;
    /**
     * Порядок рисования и проверки нажатий относительно соседей.
     */
    private int zIndex;
    /**
     * Выравнивание детей flex-контейнера поперёк главной оси.
     */
    private Align alignItems = Align.STRETCH;
    /**
     * Переопределение выравнивания одного ребёнка внутри родителя.
     */
    private Align alignSelf = Align.AUTO;
    /**
     * Распределение нескольких flex-линий по поперечной оси.
     */
    private AlignContent alignContent = AlignContent.STRETCH;
    /**
     * Legacy-выравнивание слотов по горизонтали.
     */
    private Alignment horizontalAlignment = Alignment.STRETCH;
    /**
     * Legacy-выравнивание слотов по вертикали.
     */
    private Alignment verticalAlignment = Alignment.STRETCH;
    /**
     * Распределение детей flex-контейнера вдоль главной оси.
     */
    private Justify justifyContent = Justify.START;
    /**
     * Отступ от левого края родителя для абсолютного/relative-позиционирования.
     */
    private SizeValue left = SizeValue.auto();
    /**
     * Отступ от верхнего края родителя для абсолютного/relative-позиционирования.
     */
    private SizeValue top = SizeValue.auto();
    /**
     * Отступ от правого края родителя для абсолютного/relative-позиционирования.
     */
    private SizeValue right = SizeValue.auto();
    /**
     * Отступ от нижнего края родителя для абсолютного/relative-позиционирования.
     */
    private SizeValue bottom = SizeValue.auto();

    /**
     * Создаёт стиль с начальными правилами компоновки.
     *
     * <p>В начальном состоянии виджет остаётся в обычном порядке детей родителя,
     * просит размер {@code auto}, не растёт и не сжимается автоматически,
     * а дети flex-контейнера растягиваются поперёк главной оси.</p>
     */
    public LayoutStyle() {
    }

    /**
     * Создаёт стиль и задаёт callback, который вызывается при изменении layout-свойств.
     *
     * @param onChanged callback инвалидации layout'а; может быть {@code null}
     */
    public LayoutStyle(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    /**
     * Заменяет callback, который узнаёт об изменении стиля.
     *
     * <p>Обычно callback назначает сам виджет: после изменения он помечает
     * расположение устаревшим, чтобы пересчитать его перед следующим показом.
     * При ручном использовании callback вызывается после каждого реального
     * изменения, а внутри {@link #update(Consumer)} — один раз после всей группы.</p>
     *
     * @param onChanged новый callback изменения; может быть {@code null}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    /**
     * Выполняет несколько изменений и отправляет одно уведомление в конце.
     *
     * <p>Например, при смене ширины и высоты layout не будет дважды просить
     * родителя пересчитать расположение. Метод безопасно поддерживает вложенные
     * вызовы: уведомление произойдёт после выхода из самого внешнего блока.</p>
     *
     * @param update блок изменений
     * @return этот стиль для fluent-настройки
     */
    @WikiMethod(
            title = "<key:layout.style.method.update.title>",
            summary = "<key:layout.style.method.update.summary>",
            markdown = "<key:layout.style.method.update.markdown>",
            order = 10)
    @WikiReturn("<key:layout.style.method.fluent.return>")
    @AiDoc(section = "Batching & Copies", sectionOrder = 10, order = 10,
            summary = "Applies several changes and invokes the change callback at most once after the outer block completes.")
    public LayoutStyle update(
            @WikiParameter(name = "update", description = "<key:layout.style.method.update.parameter>")
            Consumer<LayoutStyle> update) {
        if (update == null) return this;
        updateDepth++;
        try {
            update.accept(this);
        } finally {
            updateDepth--;
            if (updateDepth == 0 && pendingChange) {
                pendingChange = false;
                notifyChanged();
            }
        }
        return this;
    }

    /**
     * Создаёт независимую копию текущих layout-настроек.
     *
     * @return новый {@code LayoutStyle} с теми же значениями, но без callback'а изменения
     */
    @AiDoc(section = "Batching & Copies", sectionOrder = 10, order = 20,
            summary = "Returns an independent copy of the settings without the owner's change callback.")
    public LayoutStyle copy() {
        return new LayoutStyle().copyFrom(this);
    }

    /**
     * Копирует все layout-свойства другого объекта в этот объект.
     *
     * <p>Текущий объект остаётся тем же, поэтому виджет-владелец и его callback
     * не теряются. Изменения отправляют не более одного уведомления. Если
     * {@code other == null}, устанавливаются начальные значения, как у нового стиля.</p>
     *
     * @param other источник настроек
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Batching & Copies", sectionOrder = 10, order = 30,
            summary = "Copies all layout properties from another style while preserving this object's callback.")
    public LayoutStyle copyFrom(LayoutStyle other) {
        LayoutStyle source = other == null ? new LayoutStyle() : other;
        return update(style -> {
            style.position(source.position);
            style.width(source.width);
            style.height(source.height);
            style.minWidth(source.minWidth);
            style.minHeight(source.minHeight);
            style.maxWidth(source.maxWidth);
            style.maxHeight(source.maxHeight);
            style.margin(source.margin);
            style.marginAuto(source.marginAuto);
            style.padding(source.padding);
            style.overflowX(source.overflowX);
            style.overflowY(source.overflowY);
            style.flexDirection(source.flexDirection);
            style.flexWrap(source.flexWrap);
            style.rowGap(source.rowGap);
            style.columnGap(source.columnGap);
            style.flexGrow(source.flexGrow);
            style.flexShrink(source.flexShrink);
            style.flexBasis(source.flexBasis);
            style.order(source.order);
            style.aspectRatio(source.aspectRatio);
            style.gridTemplateColumns(source.gridTemplateColumns);
            style.gridTemplateRows(source.gridTemplateRows);
            style.gridAutoColumns(source.gridAutoColumns);
            style.gridAutoRows(source.gridAutoRows);
            style.gridAutoFlow(source.gridAutoFlow);
            style.gridColumn(source.gridColumnStart, source.gridColumnSpan);
            style.gridRow(source.gridRowStart, source.gridRowSpan);
            style.zIndex(source.zIndex);
            style.alignItems(source.alignItems);
            style.alignSelf(source.alignSelf);
            style.alignContent(source.alignContent);
            style.horizontalAlignment = source.horizontalAlignment;
            style.verticalAlignment = source.verticalAlignment;
            style.justifyContent(source.justifyContent);
            style.left(source.left);
            style.top(source.top);
            style.right(source.right);
            style.bottom(source.bottom);
        });
    }

    /**
     * Возвращает, как родитель размещает виджет относительно остальных детей.
     *
     * @return текущий режим; обычно {@link PositionType#RELATIVE}
     */
    public PositionType position() {
        return position;
    }

    /**
     * Выбирает, должен ли виджет занимать место в обычном порядке детей.
     *
     * <p>При {@link PositionType#RELATIVE} родитель сначала ставит виджет рядом
     * с остальными детьми, затем при необходимости сдвигает его через insets.
     * При {@link PositionType#ABSOLUTE} координаты задаются относительно родителя,
     * а соседние дети ведут себя так, будто этого виджета нет. У {@link PositionType#FIXED}
     * в текущей версии нет привязки к экрану: он ведёт себя как absolute относительно
     * ближайшего layout-хоста.</p>
     *
     * <p>Пример абсолютного бейджа внутри панели:</p>
     * <pre>{@code
     * badge.layout(style -> style
     *         .position(PositionType.ABSOLUTE)
     *         .right(4.0f)
     *         .top(4.0f));
     * }</pre>
     *
     * @param position режим позиционирования; {@code null} нормализуется в {@link PositionType#RELATIVE}
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Size & Position", sectionOrder = 20, order = 10,
            summary = "Selects normal-flow participation (`RELATIVE`/`STATIC`) or out-of-flow positioning (`ABSOLUTE`/`FIXED`).")
    public LayoutStyle position(PositionType position) {
        PositionType normalized = position == null ? PositionType.RELATIVE : position;
        if (this.position == normalized) return this;
        this.position = normalized;
        changed();
        return this;
    }

    /**
     * Возвращает ширину, которую виджет просит у родителя.
     *
     * <p>Это запрос, а не обещание итогового размера: родитель может ограничить
     * ширину через min/max или изменить её правилами flex/grid.</p>
     *
     * @return ширина в пикселях, процентах либо специальное значение вроде {@code auto}
     */
    public SizeValue width() {
        return width;
    }

    /**
     * Задаёт желаемую ширину виджета.
     *
     * <p>Например, фиксированное значение удобно для кнопки. {@code auto} просит
     * родителя подобрать размер по содержимому и доступному месту. Процент задаёт
     * долю ширины родителя, если её можно определить. Если родитель ограничивает
     * доступный размер, итог может отличаться от запроса.</p>
     *
     * @param width новая ширина; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle width(SizeValue width) {
        return setSize(this.width, width, value -> this.width = value);
    }

    /**
     * Задаёт желаемую ширину в UI-пикселях.
     *
     * <p>Передача значения {@link LayoutConstraints#AUTO} из старого API также означает
     * {@code auto}. Остальные невалидные/отрицательные значения нормализуются через
     * {@link SizeValue#px(float)}.</p>
     *
     * @param pixels ширина в пикселях или {@link LayoutConstraints#AUTO}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle width(float pixels) {
        return width(fromLayoutFloat(pixels));
    }

    /**
     * Задаёт ширину как долю доступной ширины родителя.
     *
     * <p>Например, {@code 50} просит половину ширины родителя, а {@code 100} —
     * всю доступную ширину. Если ширина родителя ещё неизвестна, движок может
     * отложить или иначе разрешить процентное значение.</p>
     *
     * @param percent процент от ширины родителя; {@code 100.0f} означает 100%
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle widthPercent(float percent) {
        return width(SizeValue.percent(percent));
    }

    /**
     * Задаёт желаемые ширину и высоту одной операцией.
     *
     * <p>Значения {@link SizeValue#auto()}, пиксели и проценты можно смешивать.
     * Например, фиксированная ширина вместе с автоматической высотой позволяет
     * высоте подстроиться под текст.</p>
     *
     * @param width  желаемая ширина; {@code null} означает {@code auto}
     * @param height желаемая высота; {@code null} означает {@code auto}
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Size & Position", sectionOrder = 20, order = 20,
            summary = "Sets width and height as `auto`, pixels, percentages, or intrinsic `SizeValue` values.")
    public LayoutStyle size(SizeValue width, SizeValue height) {
        return update(style -> {
            style.width(width);
            style.height(height);
        });
    }

    /**
     * Задаёт ширину и высоту в UI-пикселях одной операцией.
     *
     * @param width  ширина в пикселях или {@link LayoutConstraints#AUTO}
     * @param height высота в пикселях или {@link LayoutConstraints#AUTO}
     * @return этот стиль для fluent-настройки
     */
    @WikiMethod(
            title = "<key:layout.style.method.size.title>",
            summary = "<key:layout.style.method.size.summary>",
            markdown = "<key:layout.style.method.size.markdown>",
            order = 20)
    @WikiReturn("<key:layout.style.method.fluent.return>")
    @AiDoc(section = "Size & Position", sectionOrder = 20, order = 30,
            summary = "Sets width and height in UI pixels; legacy `AUTO` is converted to `auto`.")
    public LayoutStyle size(
            @WikiParameter(name = "width", description = "<key:layout.style.method.size.width>") float width,
            @WikiParameter(name = "height", description = "<key:layout.style.method.size.height>") float height) {
        return update(style -> {
            style.width(width);
            style.height(height);
        });
    }

    /**
     * Задаёт долю доступной ширины и высоты родителя.
     *
     * @param widthPercent  ширина в процентах
     * @param heightPercent высота в процентах
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Size & Position", sectionOrder = 20, order = 40,
            summary = "Sets width and height as percentages of the parent's available size.")
    public LayoutStyle sizePercent(float widthPercent, float heightPercent) {
        return size(SizeValue.percent(widthPercent), SizeValue.percent(heightPercent));
    }

    /**
     * Задаёт фиксированные ширину и высоту, которые flex не будет менять.
     *
     * <p>В отличие от {@link #size(float, float)}, этот помощник также выключает
     * автоматическое расширение и сжатие на главной оси. Например, фиксированный
     * заголовок не отдаст часть высоты растущей области содержимого.</p>
     *
     * @param width  ширина в пикселях
     * @param height высота в пикселях
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Size & Position", sectionOrder = 20, order = 50,
            summary = "Sets the size and disables flex growth and shrink for a fixed element.")
    public LayoutStyle fixed(float width, float height) {
        return update(style -> {
            style.size(width, height);
            style.flexBasis(SizeValue.auto());
            style.flexGrow(0.0f);
            style.flexShrink(0.0f);
        });
    }

    /**
     * Просит 100% доступной ширины и 100% доступной высоты родителя.
     *
     * <p>Это процентный запрос по обеим осям. Он сам по себе не означает
     * «занять то, что осталось после соседей». Для этого у flex-контейнера
     * используйте {@link #expand()}.</p>
     *
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Size & Position", sectionOrder = 20, order = 60,
            summary = "Requests 100% of the available width and height; this is not the same as `expand()`.")
    public LayoutStyle fill() {
        return sizePercent(100.0f, 100.0f);
    }

    /**
     * Возвращает высоту, которую виджет просит у родителя.
     *
     * @return высота: {@code auto}, пиксели или проценты
     */
    public SizeValue height() {
        return height;
    }

    /**
     * Задаёт желаемую высоту виджета.
     *
     * <p>Итоговая высота может быть ограничена родителем, min/max или правилами
     * распределения свободного места. {@code auto} оставляет выбор layout-у.</p>
     *
     * @param height новая высота; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle height(SizeValue height) {
        return setSize(this.height, height, value -> this.height = value);
    }

    /**
     * Задаёт желаемую высоту в UI-пикселях.
     *
     * @param pixels высота в пикселях или значение {@link LayoutConstraints#AUTO} из старого API
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle height(float pixels) {
        return height(fromLayoutFloat(pixels));
    }

    /**
     * Задаёт высоту как долю доступной высоты родителя.
     *
     * @param percent процент от высоты родителя; {@code 100.0f} означает 100%
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle heightPercent(float percent) {
        return height(SizeValue.percent(percent));
    }

    /**
     * Возвращает нижнюю границу ширины.
     *
     * @return минимально допустимая ширина
     */
    public SizeValue minWidth() {
        return minWidth;
    }

    /**
     * Не позволяет виджету стать уже заданного значения.
     *
     * <p>Родитель может пытаться сжать ребёнка, чтобы уместить соседей. Минимум
     * задаёт предел такого сжатия. Если минимум больше доступного места, виджет
     * может выйти за границы контейнера.</p>
     *
     * @param minWidth минимальная ширина; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle minWidth(SizeValue minWidth) {
        return setSize(this.minWidth, minWidth, value -> this.minWidth = value);
    }

    /**
     * Задаёт минимальную ширину в UI-пикселях.
     *
     * @param pixels минимальная ширина
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle minWidth(float pixels) {
        return minWidth(SizeValue.px(pixels));
    }

    /**
     * Задаёт минимальную ширину как долю ширины родителя.
     *
     * @param percent минимальная ширина в процентах
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle minWidthPercent(float percent) {
        return minWidth(SizeValue.percent(percent));
    }

    /**
     * Возвращает нижнюю границу высоты.
     *
     * @return нижняя граница высоты
     */
    public SizeValue minHeight() {
        return minHeight;
    }

    /**
     * Не позволяет виджету стать ниже заданного значения.
     *
     * @param minHeight минимальная высота; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle minHeight(SizeValue minHeight) {
        return setSize(this.minHeight, minHeight, value -> this.minHeight = value);
    }

    /**
     * Задаёт минимальную высоту в UI-пикселях.
     *
     * @param pixels минимальная высота
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle minHeight(float pixels) {
        return minHeight(SizeValue.px(pixels));
    }

    /**
     * Задаёт минимальную высоту как долю высоты родителя.
     *
     * @param percent минимальная высота в процентах
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle minHeightPercent(float percent) {
        return minHeight(SizeValue.percent(percent));
    }

    /**
     * Задаёт нижние границы ширины и высоты одной операцией.
     *
     * @param width  минимальная ширина
     * @param height минимальная высота
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle minSize(SizeValue width, SizeValue height) {
        return update(style -> {
            style.minWidth(width);
            style.minHeight(height);
        });
    }

    /**
     * Задаёт минимальные ширину и высоту в UI-пикселях.
     *
     * @param width  минимальная ширина
     * @param height минимальная высота
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle minSize(float width, float height) {
        return minSize(SizeValue.px(width), SizeValue.px(height));
    }

    /**
     * Возвращает верхнюю границу ширины.
     *
     * @return верхняя граница ширины или {@code auto}, если ограничение не задано
     */
    public SizeValue maxWidth() {
        return maxWidth;
    }

    /**
     * Не позволяет виджету стать шире заданного значения.
     *
     * <p>Родитель может распределять свободное место между растущими детьми.
     * Максимум останавливает рост этого виджета. {@code auto} снимает верхний предел.</p>
     *
     * @param maxWidth максимальная ширина; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle maxWidth(SizeValue maxWidth) {
        return setSize(this.maxWidth, maxWidth, value -> this.maxWidth = value);
    }

    /**
     * Задаёт максимальную ширину в UI-пикселях.
     *
     * <p>Бесконечность или NaN трактуются как {@code auto}, то есть без явного max.</p>
     *
     * @param pixels максимальная ширина
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle maxWidth(float pixels) {
        return maxWidth(fromMaximumLayoutFloat(pixels));
    }

    /**
     * Задаёт максимальную ширину как долю ширины родителя.
     *
     * @param percent максимальная ширина в процентах
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle maxWidthPercent(float percent) {
        return maxWidth(SizeValue.percent(percent));
    }

    /**
     * Возвращает верхнюю границу высоты.
     *
     * @return верхняя граница высоты или {@code auto}, если ограничение не задано
     */
    public SizeValue maxHeight() {
        return maxHeight;
    }

    /**
     * Не позволяет виджету стать выше заданного значения.
     *
     * @param maxHeight максимальная высота; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle maxHeight(SizeValue maxHeight) {
        return setSize(this.maxHeight, maxHeight, value -> this.maxHeight = value);
    }

    /**
     * Задаёт максимальную высоту в UI-пикселях.
     *
     * <p>Бесконечность или NaN трактуются как {@code auto}, то есть без явного max.</p>
     *
     * @param pixels максимальная высота
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle maxHeight(float pixels) {
        return maxHeight(fromMaximumLayoutFloat(pixels));
    }

    /**
     * Задаёт максимальную высоту как долю высоты родителя.
     *
     * @param percent максимальная высота в процентах
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle maxHeightPercent(float percent) {
        return maxHeight(SizeValue.percent(percent));
    }

    /**
     * Задаёт верхние границы ширины и высоты одной операцией.
     *
     * @param width  максимальная ширина
     * @param height максимальная высота
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle maxSize(SizeValue width, SizeValue height) {
        return update(style -> {
            style.maxWidth(width);
            style.maxHeight(height);
        });
    }

    /**
     * Задаёт максимальные ширину и высоту в UI-пикселях.
     *
     * @param width  максимальная ширина
     * @param height максимальная высота
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle maxSize(float width, float height) {
        return update(style -> {
            style.maxWidth(width);
            style.maxHeight(height);
        });
    }

    /**
     * Задаёт максимальные ширину и высоту как доли размеров родителя.
     *
     * @param widthPercent  максимальная ширина в процентах
     * @param heightPercent максимальная высота в процентах
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle maxSizePercent(float widthPercent, float heightPercent) {
        return maxSize(SizeValue.percent(widthPercent), SizeValue.percent(heightPercent));
    }

    /**
     * Возвращает внешний отступ виджета.
     *
     * @return margin вокруг виджета
     */
    public EdgeInsets margin() {
        return margin;
    }

    /**
     * Задаёт внешний отступ виджета.
     *
     * <p>Margin — свободное пространство снаружи границ виджета. Оно отделяет
     * виджет от соседей и учитывается при расчёте места, которое занимают дети
     * в родителе. Само содержимое виджета margin не отодвигает — для этого нужен
     * {@link #padding(EdgeInsets) padding}.</p>
     *
     * @param margin внешний отступ; {@code null} нормализуется в {@link EdgeInsets#ZERO}
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Spacing & Overflow", sectionOrder = 30, order = 10,
            summary = "Sets outer spacing that separates the widget from siblings without moving its inner content.")
    public LayoutStyle margin(EdgeInsets margin) {
        EdgeInsets normalized = margin == null ? EdgeInsets.ZERO : margin;
        if (this.margin.equals(normalized)) return this;
        this.margin = normalized;
        changed();
        return this;
    }

    /**
     * Задаёт одинаковый внешний отступ со всех четырёх сторон.
     *
     * <p>Например, {@code margin(4)} оставляет по 4 UI-пикселя между виджетом
     * и соседями сверху, справа, снизу и слева.</p>
     *
     * @param all отступ слева, сверху, справа и снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle margin(float all) {
        return margin(EdgeInsets.all(all));
    }

    /**
     * Задаёт внешний отступ отдельно для горизонтальных и вертикальных сторон.
     *
     * @param horizontal отступ слева и справа
     * @param vertical   отступ сверху и снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle margin(float horizontal, float vertical) {
        return margin(EdgeInsets.symmetric(horizontal, vertical));
    }

    /**
     * Задаёт внешний отступ независимо для каждой стороны.
     *
     * @param left   отступ слева
     * @param top    отступ сверху
     * @param right  отступ справа
     * @param bottom отступ снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle margin(float left, float top, float right, float bottom) {
        return margin(new EdgeInsets(left, top, right, bottom));
    }

    /**
     * Возвращает стороны, у которых внешний отступ распределяет свободное место.
     *
     * @return флаги автоматических margin, никогда {@code null}
     */
    public AutoMargins marginAuto() {
        return marginAuto;
    }

    /**
     * Задаёт автоматические margin.
     *
     * <p>Автоматический margin поглощает свободное место вместо фиксированного
     * отступа: на главной оси делит его с другими auto-margin и отменяет
     * {@code justify-content}, на поперечной центрирует (пара) или прижимает
     * к противоположной стороне (один). Пиксельные margin из {@link #margin()}
     * на auto-сторонах игнорируются в layout-математике.</p>
     *
     * @param marginAuto флаги; {@code null} нормализуется в {@link AutoMargins#NONE}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle marginAuto(AutoMargins marginAuto) {
        AutoMargins normalized = marginAuto == null ? AutoMargins.NONE : marginAuto;
        if (this.marginAuto.equals(normalized)) return this;
        this.marginAuto = normalized;
        changed();
        return this;
    }

    /**
     * Включает автоматические отступы слева и справа.
     *
     * <p>В горизонтальном контейнере пара таких отступов обычно центрирует
     * виджет, а один автоматический отступ может прижать его к одному из краёв.
     * Точный результат зависит от направления главной оси родителя.</p>
     *
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle marginAutoHorizontal() {
        return marginAuto(AutoMargins.of(true, marginAuto.top(), true, marginAuto.bottom()));
    }

    /**
     * Включает автоматические отступы сверху и снизу. В вертикальном контейнере
     * они распределяют свободную высоту вокруг виджета.
     *
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle marginAutoVertical() {
        return marginAuto(AutoMargins.of(marginAuto.left(), true, marginAuto.right(), true));
    }

    /**
     * Возвращает внутренний отступ от краёв виджета до его содержимого.
     *
     * @return padding внутри виджета
     */
    public EdgeInsets padding() {
        return padding;
    }

    /**
     * Задаёт внутренний отступ контейнера.
     *
     * <p>Padding — пространство внутри границ контейнера. Например, padding сверху
     * отодвигает дочерние виджеты от верхнего края. Чем больше padding, тем
     * меньше места остаётся для содержимого. В отличие от {@link #margin()},
     * этот отступ не отделяет контейнер от его соседей.</p>
     *
     * @param padding внутренний отступ; {@code null} нормализуется в {@link EdgeInsets#ZERO}
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Spacing & Overflow", sectionOrder = 30, order = 20,
            summary = "Sets inner spacing between the container bounds and its content.")
    public LayoutStyle padding(EdgeInsets padding) {
        EdgeInsets normalized = padding == null ? EdgeInsets.ZERO : padding;
        if (this.padding.equals(normalized)) return this;
        this.padding = normalized;
        changed();
        return this;
    }

    /**
     * Задаёт одинаковый внутренний отступ со всех четырёх сторон.
     *
     * @param all отступ слева, сверху, справа и снизу
     * @return этот стиль для fluent-настройки
     */
    @WikiMethod(
            title = "<key:layout.style.method.padding.title>",
            summary = "<key:layout.style.method.padding.summary>",
            order = 30)
    @WikiReturn("<key:layout.style.method.fluent.return>")
    public LayoutStyle padding(
            @WikiParameter(name = "all", description = "<key:layout.style.method.padding.parameter>") float all) {
        return padding(EdgeInsets.all(all));
    }

    /**
     * Задаёт внутренний отступ отдельно для горизонтальных и вертикальных сторон.
     *
     * @param horizontal отступ слева и справа
     * @param vertical   отступ сверху и снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle padding(float horizontal, float vertical) {
        return padding(EdgeInsets.symmetric(horizontal, vertical));
    }

    /**
     * Задаёт внутренний отступ независимо для каждой стороны.
     *
     * @param left   отступ слева
     * @param top    отступ сверху
     * @param right  отступ справа
     * @param bottom отступ снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle padding(float left, float top, float right, float bottom) {
        return padding(new EdgeInsets(left, top, right, bottom));
    }

    /**
     * Возвращает правило для содержимого, которое выходит за левую или правую границу.
     *
     * @return режим overflow по X
     */
    public Overflow overflowX() {
        return overflowX;
    }

    /**
     * Задаёт, что делать с содержимым, которое шире виджета.
     *
     * <p>В зависимости от {@link Overflow} содержимое может оставаться видимым,
     * обрезаться по границе или прокручиваться. Прокрутку по этой оси выполняет
     * {@code ScrollView}.</p>
     *
     * @param overflowX режим overflow; {@code null} нормализуется в {@link Overflow#VISIBLE}
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Spacing & Overflow", sectionOrder = 30, order = 30,
            summary = "Controls content that exceeds the width (`VISIBLE`, `HIDDEN`, or `AUTO`).")
    public LayoutStyle overflowX(Overflow overflowX) {
        Overflow normalized = overflowX == null ? Overflow.VISIBLE : overflowX;
        if (this.overflowX == normalized) return this;
        this.overflowX = normalized;
        changed();
        return this;
    }

    /**
     * Возвращает правило для содержимого, которое выходит за верхнюю или нижнюю границу.
     *
     * @return режим overflow по Y
     */
    public Overflow overflowY() {
        return overflowY;
    }

    /**
     * Задаёт, что делать с содержимым, которое выше виджета.
     *
     * <p>Например, {@link Overflow#AUTO} включает прокрутку, когда содержимое
     * не помещается по высоте. Прокрутку выполняет {@code ScrollView}.</p>
     *
     * @param overflowY режим overflow; {@code null} нормализуется в {@link Overflow#VISIBLE}
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Spacing & Overflow", sectionOrder = 30, order = 40,
            summary = "Controls content that exceeds the height; scrolling is implemented by `ScrollView`.")
    public LayoutStyle overflowY(Overflow overflowY) {
        Overflow normalized = overflowY == null ? Overflow.VISIBLE : overflowY;
        if (this.overflowY == normalized) return this;
        this.overflowY = normalized;
        changed();
        return this;
    }

    /**
     * Задаёт одинаковое правило переполнения по ширине и высоте.
     *
     * @param overflow режим overflow для обеих осей
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Spacing & Overflow", sectionOrder = 30, order = 50,
            summary = "Sets overflow behavior for both axes at once.")
    public LayoutStyle overflow(Overflow overflow) {
        return update(style -> {
            style.overflowX(overflow);
            style.overflowY(overflow);
        });
    }

    /**
     * Возвращает направление главной оси flex-контейнера.
     *
     * @return {@link FlexDirection#ROW} для горизонтального ряда или {@link FlexDirection#COLUMN} для вертикальной колонки
     */
    public FlexDirection flexDirection() {
        return flexDirection;
    }

    /**
     * Задаёт направление главной оси flex-контейнера.
     *
     * <p>У контейнера это определяет, как раскладываются его дети. У дочернего
     * виджета это влияет на раскладку его собственных детей, если он сам является
     * контейнером.</p>
     *
     * @param flexDirection направление; {@code null} нормализуется в {@link FlexDirection#COLUMN}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle flexDirection(FlexDirection flexDirection) {
        FlexDirection normalized = flexDirection == null ? FlexDirection.COLUMN : flexDirection;
        if (this.flexDirection == normalized) return this;
        this.flexDirection = normalized;
        changed();
        return this;
    }

    /**
     * Возвращает правило переноса детей, если они не помещаются на одной линии.
     *
     * @return текущий режим wrap
     */
    public FlexWrap flexWrap() {
        return flexWrap;
    }

    /**
     * Задаёт, должны ли дети переходить на следующие строки или колонки,
     * когда места на одной линии не хватает.
     *
     * <p>При {@link FlexWrap#NOWRAP} все дети остаются на одной линии; при
     * {@link FlexWrap#WRAP} лишние дети переходят на следующую. Направление
     * линии зависит от {@link #flexDirection()}.</p>
     *
     * @param flexWrap режим переноса; {@code null} нормализуется в {@link FlexWrap#NOWRAP}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle flexWrap(FlexWrap flexWrap) {
        FlexWrap normalized = flexWrap == null ? FlexWrap.NOWRAP : flexWrap;
        if (this.flexWrap == normalized) return this;
        this.flexWrap = normalized;
        changed();
        return this;
    }

    /**
     * Возвращает вертикальное расстояние между рядами элементов.
     *
     * @return row gap в пикселях
     */
    public float rowGap() {
        return rowGap;
    }

    /**
     * Задаёт расстояние сверху вниз между рядами элементов.
     *
     * <p>Ряд появляется, например, если включён перенос детей или используется
     * сетка. Для расстояния между элементами в горизонтальном ряду используйте
     * {@link #columnGap(float)}.</p>
     *
     * @param rowGap промежуток в пикселях; отрицательные/NaN значения станут 0
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle rowGap(float rowGap) {
        float normalized = sanitize(rowGap);
        if (this.rowGap == normalized) return this;
        this.rowGap = normalized;
        changed();
        return this;
    }

    /**
     * Возвращает горизонтальное расстояние между колонками элементов.
     *
     * @return column gap в пикселях
     */
    public float columnGap() {
        return columnGap;
    }

    /**
     * Задаёт расстояние слева направо между элементами в строке или между
     * колонками сетки.
     *
     * @param columnGap промежуток в пикселях; отрицательные/NaN значения станут 0
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle columnGap(float columnGap) {
        float normalized = sanitize(columnGap);
        if (this.columnGap == normalized) return this;
        this.columnGap = normalized;
        changed();
        return this;
    }

    /**
     * Задаёт одинаковое расстояние между соседними рядами и колонками.
     *
     * @param gap промежуток в пикселях
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Flex Layout", sectionOrder = 40, order = 20,
            summary = "Sets the same gap between rows and columns.")
    public LayoutStyle gap(float gap) {
        return update(style -> {
            style.rowGap(gap);
            style.columnGap(gap);
        });
    }

    /**
     * Задаёт расстояние между соседями на главной оси и сбрасывает поперечный gap.
     *
     * <p>{@link Orientation#HORIZONTAL} задаёт {@code columnGap}, а
     * {@link Orientation#VERTICAL} — {@code rowGap}. Метод не меняет направление
     * flex и режим переноса.</p>
     *
     * @param orientation направление главной оси; {@code null} трактуется как {@link Orientation#VERTICAL}
     * @param spacing расстояние в логических пикселях; отрицательные и неконечные значения нормализуются в ноль
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Flex Layout", sectionOrder = 40, order = 30,
            summary = "Sets spacing along the selected orientation and clears the cross-axis gap.")
    public LayoutStyle spacing(Orientation orientation, float spacing) {
        Orientation resolved = orientation == null ? Orientation.VERTICAL : orientation;
        return update(style -> {
            style.rowGap(resolved == Orientation.VERTICAL ? spacing : 0.0f);
            style.columnGap(resolved == Orientation.HORIZONTAL ? spacing : 0.0f);
        });
    }

    /**
     * Задаёт вертикальное и горизонтальное расстояние между элементами.
     *
     * <p>Первое число относится к расстоянию между рядами (сверху вниз), второе —
     * между колонками или соседями в строке (слева направо).</p>
     *
     * @param rowGap    расстояние между flex-строками или линиями переноса
     * @param columnGap расстояние между flex-колонками или элементами строки
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Flex Layout", sectionOrder = 40, order = 40,
            summary = "Sets row and column gaps independently.")
    public LayoutStyle gap(float rowGap, float columnGap) {
        return update(style -> {
            style.rowGap(rowGap);
            style.columnGap(columnGap);
        });
    }

    /**
     * Возвращает grow-вес виджета на главной оси родителя.
     *
     * @return grow-вес; 0 означает "не забирать свободное место"
     */
    public float flexGrow() {
        return flexGrow;
    }

    /**
     * Задаёт, как дочерний виджет забирает свободное место у flex-родителя.
     *
     * <p>{@code flexGrow} работает только когда у родителя после размещения базовых
     * размеров осталось свободное место на главной оси. Значение — это вес, а не
     * процент:</p>
     *
     * <ul>
     *     <li>{@code 0} — не расширяться;</li>
     *     <li>{@code 1} — забирать свободное место наравне с другими детьми с {@code grow = 1};</li>
     *     <li>{@code 2} — получить в два раза больше свободного места, чем sibling с {@code grow = 1}.</li>
     * </ul>
     *
     * <p>Для {@link FlexDirection#COLUMN} главная ось — высота, для {@link FlexDirection#ROW}
     * главная ось — ширина.</p>
     *
     * @param flexGrow grow-вес; отрицательные/NaN значения станут 0
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle flexGrow(float flexGrow) {
        float normalized = sanitize(flexGrow);
        if (this.flexGrow == normalized) return this;
        this.flexGrow = normalized;
        changed();
        return this;
    }

    /**
     * Возвращает shrink-вес виджета на главной оси родителя.
     *
     * <p>Дефолт — {@code 0} (не сжиматься): осознанное отличие от CSS, где дефолт
     * {@code 1}. Игровые UI обычно задают размеры явно, и автосжатие даёт
     * неожиданные сплющивания. Для веб-поведения задайте {@code 1} явно.</p>
     *
     * @return shrink-вес; 0 означает "не сжиматься из-за нехватки места"
     */
    public float flexShrink() {
        return flexShrink;
    }

    /**
     * Задаёт, как дочерний виджет отдаёт место, когда flex-родителю не хватает размера.
     *
     * <p>{@code flexShrink = 0} — дефолт: виджет не ужимается. {@code 1} полезен,
     * когда виджет может отдавать место соседям.</p>
     *
     * @param flexShrink shrink-вес; отрицательные/NaN значения станут 0
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle flexShrink(float flexShrink) {
        float normalized = sanitize(flexShrink);
        if (this.flexShrink == normalized) return this;
        this.flexShrink = normalized;
        changed();
        return this;
    }

    /**
     * Возвращает базовый размер виджета на главной оси до применения grow/shrink.
     *
     * @return flex basis: {@code auto}, пиксели или проценты
     */
    public SizeValue flexBasis() {
        return flexBasis;
    }

    /**
     * Задаёт начальный размер ребёнка вдоль главной оси до распределения свободного места.
     *
     * <p>{@code auto} обычно означает: взять {@code width/height} по главной оси или
     * измеренный размер содержимого.</p>
     *
     * @param flexBasis базовый размер; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle flexBasis(SizeValue flexBasis) {
        return setSize(this.flexBasis, flexBasis, value -> this.flexBasis = value);
    }

    /**
     * Задаёт flex basis в пикселях.
     *
     * @param pixels базовый размер в пикселях или значение {@link LayoutConstraints#AUTO} из старого API
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle flexBasis(float pixels) {
        return flexBasis(fromLayoutFloat(pixels));
    }

    /**
     * Возвращает порядок раскладки виджета среди детей того же родителя.
     *
     * @return order; по умолчанию 0, меньшие идут раньше
     */
    public int order() {
        return order;
    }

    /**
     * Задаёт порядок раскладки виджета среди детей того же родителя.
     *
     * <p>Дети сортируются по возрастанию перед раскладкой; при одинаковом
     * значении сохраняется их исходный порядок у родителя. Отрицательные значения
     * разрешены.</p>
     *
     * @param order порядок раскладки
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle order(int order) {
        if (this.order == order) return this;
        this.order = order;
        changed();
        return this;
    }

    /**
     * Задаёт {@code flexGrow}, {@code flexShrink} и {@code flexBasis} одной операцией.
     *
     * <p>Аналог CSS shorthand {@code flex: grow shrink basis}.</p>
     *
     * @param grow   grow-вес
     * @param shrink shrink-вес
     * @param basis  базовый размер на главной оси
     * @return этот стиль для fluent-настройки
     */
    @WikiMethod(
            title = "<key:layout.style.method.flex.title>",
            summary = "<key:layout.style.method.flex.summary>",
            markdown = "<key:layout.style.method.flex.markdown>",
            order = 40)
    @WikiReturn("<key:layout.style.method.fluent.return>")
    @AiDoc(section = "Flex Layout", sectionOrder = 40, order = 50,
            summary = "Sets flex grow/shrink and the base size before free-space distribution.",
            criticalRules = "Grow and shrink are weights; `basis` is not the final width or height.")
    public LayoutStyle flex(
            @WikiParameter(name = "grow", description = "<key:layout.style.method.flex.grow>") float grow,
            @WikiParameter(name = "shrink", description = "<key:layout.style.method.flex.shrink>") float shrink,
            @WikiParameter(name = "basis", description = "<key:layout.style.method.flex.basis>") SizeValue basis) {
        return update(style -> {
            style.flexGrow(grow);
            style.flexShrink(shrink);
            style.flexBasis(basis);
        });
    }

    /**
     * Задаёт {@code flexGrow}, {@code flexShrink} и пиксельный {@code flexBasis} одной операцией.
     *
     * @param grow        grow-вес
     * @param shrink      shrink-вес
     * @param basisPixels базовый размер на главной оси в пикселях
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle flex(float grow, float shrink, float basisPixels) {
        return flex(grow, shrink, SizeValue.px(basisPixels));
    }

    /**
     * Возвращает предпочитаемое соотношение сторон (ширина / высота).
     *
     * @return ratio или {@code NaN}, если не задано
     */
    public float aspectRatio() {
        return aspectRatio;
    }

    /**
     * Задаёт предпочитаемое соотношение сторон (ширина / высота).
     *
     * <p>Когда ровно одна ось {@code auto}, она выводится из другой через ratio:
     * {@code width = height × ratio}, {@code height = width ÷ ratio}.
     * Явные размеры, {@code flex-basis} и {@code min/max} имеют приоритет и могут
     * нарушить ratio, как в вебе. Невалидные ({@code NaN}, ≤ 0, бесконечные)
     * значения означают отсутствие ratio.</p>
     *
     * @param ratio соотношение ширины к высоте, например {@code 16.0f / 9.0f}
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Grid & Alignment", sectionOrder = 50, order = 10,
            summary = "Sets a positive width-to-height ratio when one axis remains `auto`.")
    public LayoutStyle aspectRatio(float ratio) {
        float normalized = Float.isFinite(ratio) && ratio > 0.0f ? ratio : Float.NaN;
        if (Float.compare(this.aspectRatio, normalized) == 0) return this;
        this.aspectRatio = normalized;
        changed();
        return this;
    }

    /**
     * Возвращает заданную последовательность колонок сетки.
     *
     * <p>Каждый {@link GridTrack} описывает ширину одной колонки: например,
     * фиксированную ширину, долю свободного места или размер по содержимому.
     * Если шаблон пуст, сетка подбирает колонки автоматически.</p>
     *
     * @return немодифицируемый список треков, пустой если не задан
     */
    public List<GridTrack> gridTemplateColumns() {
        return gridTemplateColumns;
    }

    /**
     * Задаёт колонки сетки и ширину каждой из них.
     *
     * <p>Например, {@code gridTemplateColumns(GridTrack.px(80), GridTrack.fr(1))}
     * создаёт узкую колонку шириной 80 пикселей и вторую колонку, которая занимает
     * оставшееся место. Один {@link GridTrack} описывает одну колонку.</p>
     *
     * @param tracks треки; {@code null} означает пустой шаблон
     * @return этот стиль для fluent-настройки
     */
    @WikiMethod(
            title = "<key:layout.style.method.grid.columns.title>",
            summary = "<key:layout.style.method.grid.columns.summary>",
            markdown = "<key:layout.style.method.grid.columns.markdown>",
            order = 50)
    @WikiReturn("<key:layout.style.method.fluent.return>")
    @AiDoc(section = "Grid & Alignment", sectionOrder = 50, order = 20,
            summary = "Sets an explicit column template from `GridTrack`; an empty list keeps automatic sizing.")
    public LayoutStyle gridTemplateColumns(
            @WikiParameter(name = "tracks", description = "<key:layout.style.method.grid.tracks>")
            List<GridTrack> tracks) {
        List<GridTrack> normalized = tracks == null ? List.of() : List.copyOf(tracks);
        if (this.gridTemplateColumns.equals(normalized)) return this;
        this.gridTemplateColumns = normalized;
        changed();
        return this;
    }

    /**
     * Задаёт колонки сетки и ширину каждой из них.
     *
     * @param tracks треки
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle gridTemplateColumns(GridTrack... tracks) {
        return gridTemplateColumns(tracks == null ? List.of() : List.of(tracks));
    }

    /**
     * Возвращает заданную последовательность строк сетки.
     *
     * <p>Каждый {@link GridTrack} описывает высоту одной строки. Пустой шаблон
     * позволяет сетке создавать строки по мере необходимости.</p>
     *
     * @return немодифицируемый список треков, пустой если не задан
     */
    public List<GridTrack> gridTemplateRows() {
        return gridTemplateRows;
    }

    /**
     * Задаёт строки сетки и высоту каждой из них.
     *
     * <p>Например, строка {@code GridTrack.px(32)} имеет постоянную высоту
     * 32 пикселя. Один {@link GridTrack} описывает одну строку.</p>
     *
     * @param tracks треки; {@code null} означает пустой шаблон
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Grid & Alignment", sectionOrder = 50, order = 30,
            summary = "Sets an explicit row template from `GridTrack`.")
    public LayoutStyle gridTemplateRows(List<GridTrack> tracks) {
        List<GridTrack> normalized = tracks == null ? List.of() : List.copyOf(tracks);
        if (this.gridTemplateRows.equals(normalized)) return this;
        this.gridTemplateRows = normalized;
        changed();
        return this;
    }

    /**
     * Задаёт строки сетки и высоту каждой из них.
     *
     * @param tracks треки
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle gridTemplateRows(GridTrack... tracks) {
        return gridTemplateRows(tracks == null ? List.of() : List.of(tracks));
    }

    /**
     * Возвращает размеры колонок, которые сетка создаёт автоматически.
     *
     * @return немодифицируемый список треков
     */
    public List<GridTrack> gridAutoColumns() {
        return gridAutoColumns;
    }

    /**
     * Задаёт размеры автоматически создаваемых колонок сетки.
     *
     * <p>Явные колонки задаются через {@link #gridTemplateColumns(GridTrack...)}.
     * Если детей больше, чем мест в заданном шаблоне, сетка создаёт дополнительные
     * колонки и использует для них этот шаблон.</p>
     *
     * @param tracks треки; {@code null} означает пустой список
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Grid & Alignment", sectionOrder = 50, order = 40,
            summary = "Sets sizes for automatically created columns when explicit columns are exhausted.")
    public LayoutStyle gridAutoColumns(List<GridTrack> tracks) {
        List<GridTrack> normalized = tracks == null ? List.of() : List.copyOf(tracks);
        if (this.gridAutoColumns.equals(normalized)) return this;
        this.gridAutoColumns = normalized;
        changed();
        return this;
    }

    /**
     * Задаёт размеры автоматически создаваемых колонок сетки.
     *
     * @param tracks треки; {@code null} означает пустой список
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle gridAutoColumnsOf(GridTrack... tracks) {
        return gridAutoColumns(tracks == null ? List.of() : List.of(tracks));
    }

    /**
     * Возвращает размеры строк, которые сетка создаёт автоматически.
     *
     * @return немодифицируемый список треков
     */
    public List<GridTrack> gridAutoRows() {
        return gridAutoRows;
    }

    /**
     * Задаёт размеры автоматически создаваемых строк сетки.
     *
     * <p>Явные строки задаются через {@link #gridTemplateRows(GridTrack...)}.
     * Если для размещения детей строк не хватает, дополнительные строки берут
     * размеры из этого шаблона.</p>
     *
     * @param tracks треки; {@code null} означает пустой список
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Grid & Alignment", sectionOrder = 50, order = 50,
            summary = "Sets sizes for automatically created rows when explicit rows are exhausted.")
    public LayoutStyle gridAutoRows(List<GridTrack> tracks) {
        List<GridTrack> normalized = tracks == null ? List.of() : List.copyOf(tracks);
        if (this.gridAutoRows.equals(normalized)) return this;
        this.gridAutoRows = normalized;
        changed();
        return this;
    }

    /**
     * Задаёт размеры автоматически создаваемых строк сетки.
     *
     * @param tracks треки; {@code null} означает пустой список
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle gridAutoRowsOf(GridTrack... tracks) {
        return gridAutoRows(tracks == null ? List.of() : List.of(tracks));
    }

    /**
     * Возвращает направление автопозиционирования в гриде.
     *
     * @return текущий auto-flow
     */
    public GridAutoFlow gridAutoFlow() {
        return gridAutoFlow;
    }

    /**
     * Задаёт направление, в котором сетка автоматически ищет свободные ячейки.
     *
     * <p>Ячейка — пересечение одной строки и одной колонки. Например, режим
     * {@link GridAutoFlow#ROW} сначала заполняет текущую строку слева направо,
     * а затем переходит к следующей.</p>
     *
     * @param flow направление; {@code null} нормализуется в {@link GridAutoFlow#ROW}
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Grid & Alignment", sectionOrder = 50, order = 60,
            summary = "Selects the direction used to find free cells during auto-placement.")
    public LayoutStyle gridAutoFlow(GridAutoFlow flow) {
        GridAutoFlow normalized = flow == null ? GridAutoFlow.ROW : flow;
        if (this.gridAutoFlow == normalized) return this;
        this.gridAutoFlow = normalized;
        changed();
        return this;
    }

    /**
     * Возвращает номер линии, с которой ребёнок занимает колонки сетки.
     *
     * @return стартовая линия или неположительное для auto
     */
    public int gridColumnStart() {
        return gridColumnStart;
    }

    /**
     * Возвращает число колонок сетки, занимаемых ребёнком.
     *
     * @return span, минимум 1
     */
    public int gridColumnSpan() {
        return gridColumnSpan;
    }

    /**
     * Возвращает номер линии, с которой ребёнок занимает строки сетки.
     *
     * @return стартовая линия или неположительное для auto
     */
    public int gridRowStart() {
        return gridRowStart;
    }

    /**
     * Возвращает число строк сетки, занимаемых ребёнком.
     *
     * @return span, минимум 1
     */
    public int gridRowSpan() {
        return gridRowSpan;
    }

    /**
     * Задаёт начальную колонку и ширину ребёнка в сетке.
     *
     * <p>Линии нумеруются с 1: значение {@code start = 1} означает первую колонку.
     * {@code start <= 0} поручает сетке найти место автоматически. Параметр
     * {@code span} задаёт, сколько колонок подряд занимает ребёнок.</p>
     *
     * @param start стартовая линия 1-based, ≤ 0 означает auto
     * @param span  span, значения меньше 1 нормализуются в 1
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Grid & Alignment", sectionOrder = 50, order = 70,
            summary = "Pins a child to a grid column and sets its span; `start <= 0` means auto-placement.")
    public LayoutStyle gridColumn(int start, int span) {
        return update(style -> {
            if (style.gridColumnStart != start) {
                style.gridColumnStart = start;
                style.changed();
            }
            int normalizedSpan = Math.max(1, span);
            if (style.gridColumnSpan != normalizedSpan) {
                style.gridColumnSpan = normalizedSpan;
                style.changed();
            }
        });
    }

    /**
     * Задаёт начальную строку и высоту ребёнка в сетке.
     *
     * <p>Линии нумеруются с 1: значение {@code start = 1} означает первую строку.
     * {@code start <= 0} поручает сетке найти место автоматически. Параметр
     * {@code span} задаёт, сколько строк подряд занимает ребёнок.</p>
     *
     * @param start стартовая линия 1-based, ≤ 0 означает auto
     * @param span  span, значения меньше 1 нормализуются в 1
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Grid & Alignment", sectionOrder = 50, order = 80,
            summary = "Pins a child to a grid row and sets its span; `start <= 0` means auto-placement.")
    public LayoutStyle gridRow(int start, int span) {
        return update(style -> {
            if (style.gridRowStart != start) {
                style.gridRowStart = start;
                style.changed();
            }
            int normalizedSpan = Math.max(1, span);
            if (style.gridRowSpan != normalizedSpan) {
                style.gridRowSpan = normalizedSpan;
                style.changed();
            }
        });
    }

    /**
     * Возвращает порядок рисования виджета относительно его соседей.
     *
     * @return z-index, по умолчанию 0
     */
    public int zIndex() {
        return zIndex;
    }

    /**
     * Задаёт порядок рисования виджета относительно его соседей.
     *
     * <p>Дети рисуются от меньшего значения к большему; при одинаковых значениях
     * сохраняется их порядок в родителе. Проверка, какой виджет находится под
     * курсором или нажатием, выполняется в обратном порядке: сначала проверяются
     * виджеты, нарисованные поверх остальных. Отрицательное значение рисует
     * виджет раньше остальных детей его родителя. Вложенный контейнер при этом
     * не изолирует порядок рисования своих детей в отдельный слой.</p>
     *
     * @param zIndex z-index
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Grid & Alignment", sectionOrder = 50, order = 90,
            summary = "Sets draw order among siblings; lower values are drawn earlier.")
    public LayoutStyle zIndex(int zIndex) {
        if (this.zIndex == zIndex) return this;
        this.zIndex = zIndex;
        changed();
        return this;
    }

    /**
     * Одновременно задаёт направление размещения детей и правило переноса.
     *
     * @param direction направление главной оси
     * @param wrap      режим переноса
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Flex Layout", sectionOrder = 40, order = 10,
            summary = "Sets the flex container's main axis and wrapping mode together.")
    public LayoutStyle flexFlow(FlexDirection direction, FlexWrap wrap) {
        return update(style -> {
            style.flexDirection(direction);
            style.flexWrap(wrap);
        });
    }

    /**
     * Задаёт краткую форму flex-настроек с указанным весом роста.
     *
     * <p>Эквивалентно {@code flex(grow, 1, 0%)}: ребёнок может расти и сжиматься,
     * а начальный размер на главной оси равен нулю.</p>
     */
    public LayoutStyle flex(float grow) {
        return flex(grow, 1.0f, SizeValue.percent(0.0f));
    }

    /**
     * Разрешает виджету расти и сжиматься, начиная с заданного размера или
     * размера содержимого (эквивалент {@code grow = 1, shrink = 1, basis = auto}).
     */
    @AiDoc(section = "Flex Layout", sectionOrder = 40, order = 60,
            summary = "Enables `grow = 1`, `shrink = 1`, and `basis = auto`.")
    public LayoutStyle flexAuto() {
        return flex(1.0f, 1.0f, SizeValue.auto());
    }

    /**
     * Оставляет размер по настройке или содержимому, запрещает рост и разрешает
     * сжатие при нехватке места (эквивалент {@code 0 1 auto}).
     */
    @AiDoc(section = "Flex Layout", sectionOrder = 40, order = 70,
            summary = "Keeps the base size and allows only flex shrink (`0 1 auto`).")
    public LayoutStyle flexInitial() {
        return flex(0.0f, 1.0f, SizeValue.auto());
    }

    /**
     * Оставляет размер по настройке или содержимому и запрещает flex-рост
     * и flex-сжатие (эквивалент {@code 0 0 auto}).
     */
    @AiDoc(section = "Flex Layout", sectionOrder = 40, order = 80,
            summary = "Disables flex growth and shrink (`0 0 auto`).")
    public LayoutStyle flexNone() {
        return flex(0.0f, 0.0f, SizeValue.auto());
    }

    /**
     * Занимает свободное место на главной оси flex-контейнера.
     *
     * <p>Эквивалентно {@code flex(1, 1, 0)}. В {@link FlexDirection#COLUMN}
     * это оставшаяся высота, в {@link FlexDirection#ROW} — оставшаяся ширина.</p>
     *
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Flex Layout", sectionOrder = 40, order = 90,
            summary = "Takes remaining free space on the main axis (`1 1 0px`).")
    public LayoutStyle expand() {
        return flex(1.0f, 1.0f, SizeValue.px(0.0f));
    }

    /**
     * Запрещает виджету отдавать часть своего размера, когда flex-родителю
     * не хватает места на главной оси.
     *
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle noShrink() {
        return flexShrink(0.0f);
    }

    /**
     * Возвращает, как контейнер выравнивает детей поперёк направления их ряда.
     *
     * @return текущий align-items контейнера
     */
    public Align alignItems() {
        return alignItems;
    }

    /**
     * Задаёт выравнивание детей контейнера на поперечной оси.
     *
     * <p>Для {@link FlexDirection#ROW} поперечная ось — вертикальная, для
     * {@link FlexDirection#COLUMN} — горизонтальная. Например, {@link Align#CENTER}
     * центрирует детей поперёк направления раскладки.</p>
     *
     * @param alignItems выравнивание детей; {@code null} нормализуется в {@link Align#STRETCH}
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Grid & Alignment", sectionOrder = 50, order = 100,
            summary = "Aligns a container's children on the cross axis.")
    public LayoutStyle alignItems(Align alignItems) {
        Align normalized = alignItems == null ? Align.STRETCH : alignItems;
        if (this.alignItems == normalized) return this;
        this.alignItems = normalized;
        changed();
        return this;
    }

    /**
     * Возвращает переопределение выравнивания для этого ребёнка.
     *
     * @return align-self дочернего виджета
     */
    public Align alignSelf() {
        return alignSelf;
    }

    /**
     * Задаёт индивидуальное выравнивание этого виджета внутри родителя.
     *
     * <p>Используется дочерним виджетом, чтобы переопределить {@link #alignItems()} родителя.
     * {@link Align#AUTO} означает "использовать поведение по умолчанию". Метод также
     * синхронизирует legacy {@link #horizontalAlignment()} и {@link #verticalAlignment()}.</p>
     *
     * @param alignSelf выравнивание виджета; {@code null} нормализуется в {@link Align#AUTO}
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Grid & Alignment", sectionOrder = 50, order = 110,
            summary = "Overrides the parent's cross-axis alignment for this child.")
    public LayoutStyle alignSelf(Align alignSelf) {
        Align normalized = alignSelf == null ? Align.AUTO : alignSelf;
        Alignment nextHorizontal;
        Alignment nextVertical;
        if (normalized == Align.AUTO) {
            nextHorizontal = Alignment.STRETCH;
            nextVertical = Alignment.STRETCH;
        } else {
            nextHorizontal = toLegacyAlignment(normalized);
            nextVertical = nextHorizontal;
        }
        if (this.alignSelf == normalized
                && horizontalAlignment == nextHorizontal
                && verticalAlignment == nextVertical) return this;
        this.alignSelf = normalized;
        horizontalAlignment = nextHorizontal;
        verticalAlignment = nextVertical;
        changed();
        return this;
    }

    /**
     * Возвращает, как контейнер распределяет несколько перенесённых линий детей.
     *
     * @return текущий align-content контейнера
     */
    public AlignContent alignContent() {
        return alignContent;
    }

    /**
     * Задаёт распределение wrap-линий контейнера на поперечной оси.
     *
     * <p>Работает только когда контейнер переносит детей на несколько линий
     * ({@link FlexWrap#WRAP}). Одиночная линия всегда ведёт себя по
     * {@link #alignItems()}.</p>
     *
     * @param alignContent распределение линий; {@code null} нормализуется в {@link AlignContent#STRETCH}
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Grid & Alignment", sectionOrder = 50, order = 120,
            summary = "Distributes multiple wrapped lines on the cross axis.",
            criticalRules = "Has no visible effect when wrapping produces only one line.")
    public LayoutStyle alignContent(AlignContent alignContent) {
        AlignContent normalized = alignContent == null ? AlignContent.STRETCH : alignContent;
        if (this.alignContent == normalized) return this;
        this.alignContent = normalized;
        changed();
        return this;
    }

    /**
     * Возвращает, как контейнер размещает группу детей вдоль направления ряда.
     *
     * @return текущий justify-content контейнера
     */
    public Justify justifyContent() {
        return justifyContent;
    }

    /**
     * Задаёт распределение детей контейнера на главной оси.
     *
     * <p>Например, {@link Justify#CENTER} центрирует всю группу детей, а
     * {@link Justify#SPACE_BETWEEN} распределяет свободное место между ними.</p>
     *
     * @param justifyContent распределение; {@code null} нормализуется в {@link Justify#START}
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Grid & Alignment", sectionOrder = 50, order = 130,
            summary = "Distributes a container's children on the main axis.")
    public LayoutStyle justifyContent(Justify justifyContent) {
        Justify normalized = justifyContent == null ? Justify.START : justifyContent;
        if (this.justifyContent == normalized) return this;
        this.justifyContent = normalized;
        changed();
        return this;
    }

    /**
     * Центрирует детей контейнера по главной и поперечной осям.
     *
     * <p>Обычно вызывается на {@code VBox}, {@code HBox} или другом flex-контейнере.</p>
     *
     * @return этот стиль для fluent-настройки
     */
    @WikiMethod(
            title = "<key:layout.style.method.center.title>",
            summary = "<key:layout.style.method.center.summary>",
            order = 60)
    @WikiReturn("<key:layout.style.method.fluent.return>")
    @AiDoc(section = "Grid & Alignment", sectionOrder = 50, order = 140,
            summary = "Centers a container's children on both the main and cross axes.")
    public LayoutStyle center() {
        return update(style -> {
            style.alignItems(Align.CENTER);
            style.justifyContent(Justify.CENTER);
        });
    }

    /**
     * Центрирует конкретный flex-элемент на поперечной оси его родителя.
     *
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Grid & Alignment", sectionOrder = 50, order = 150,
            summary = "Centers this flex item on its parent's cross axis.")
    public LayoutStyle centerSelf() {
        return alignSelf(Align.CENTER);
    }

    /**
     * Возвращает расстояние от левого края родителя до левого края виджета.
     *
     * @return left inset
     */
    public SizeValue left() {
        return left;
    }

    /**
     * Задаёт расстояние от левого края родителя до левого края виджета.
     *
     * <p>Обычно используется вместе с {@link #position(PositionType)} = {@link PositionType#ABSOLUTE}.</p>
     *
     * @param left отступ слева; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle left(SizeValue left) {
        return setSize(this.left, left, value -> this.left = value);
    }

    /**
     * Задаёт левый inset в пикселях.
     *
     * @param pixels отступ слева
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle left(float pixels) {
        return left(SizeValue.px(pixels));
    }

    /**
     * Возвращает расстояние от верхнего края родителя до верхнего края виджета.
     *
     * @return top inset
     */
    public SizeValue top() {
        return top;
    }

    /**
     * Задаёт расстояние от верхнего края родителя до верхнего края виджета.
     *
     * @param top отступ сверху; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle top(SizeValue top) {
        return setSize(this.top, top, value -> this.top = value);
    }

    /**
     * Задаёт верхний inset в пикселях.
     *
     * @param pixels отступ сверху
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle top(float pixels) {
        return top(SizeValue.px(pixels));
    }

    /**
     * Возвращает расстояние от правого края родителя до правого края виджета.
     *
     * @return right inset
     */
    public SizeValue right() {
        return right;
    }

    /**
     * Задаёт расстояние от правого края родителя до правого края виджета.
     *
     * @param right отступ справа; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle right(SizeValue right) {
        return setSize(this.right, right, value -> this.right = value);
    }

    /**
     * Задаёт правый inset в пикселях.
     *
     * @param pixels отступ справа
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle right(float pixels) {
        return right(SizeValue.px(pixels));
    }

    /**
     * Возвращает расстояние от нижнего края родителя до нижнего края виджета.
     *
     * @return bottom inset
     */
    public SizeValue bottom() {
        return bottom;
    }

    /**
     * Задаёт расстояние от нижнего края родителя до нижнего края виджета.
     *
     * @param bottom отступ снизу; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle bottom(SizeValue bottom) {
        return setSize(this.bottom, bottom, value -> this.bottom = value);
    }

    /**
     * Задаёт нижний inset в пикселях.
     *
     * @param pixels отступ снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle bottom(float pixels) {
        return bottom(SizeValue.px(pixels));
    }

    /**
     * Задаёт расстояния до всех четырёх краёв родителя одной операцией.
     *
     * <p>Эти значения называют inset. Обычно они нужны при {@link PositionType#ABSOLUTE}:
     * например, {@code inset(0)} прижимает виджет ко всем краям родителя. Если
     * противоположные стороны заданы одновременно, они могут также определять
     * размер виджета между этими сторонами.</p>
     *
     * @param left   отступ слева
     * @param top    отступ сверху
     * @param right  отступ справа
     * @param bottom отступ снизу
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Size & Position", sectionOrder = 20, order = 70,
            summary = "Sets distances to all parent edges, typically for `ABSOLUTE` positioning.")
    public LayoutStyle inset(SizeValue left, SizeValue top, SizeValue right, SizeValue bottom) {
        return update(style -> {
            style.left(left);
            style.top(top);
            style.right(right);
            style.bottom(bottom);
        });
    }

    /**
     * Задаёт одинаковое расстояние до всех четырёх краёв родителя.
     *
     * @param all отступ слева, сверху, справа и снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle inset(float all) {
        return inset(all, all, all, all);
    }

    /**
     * Задаёт одинаковые горизонтальные и вертикальные расстояния до краёв.
     *
     * @param horizontal отступ слева и справа
     * @param vertical   отступ сверху и снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle inset(float horizontal, float vertical) {
        return inset(horizontal, vertical, horizontal, vertical);
    }

    /**
     * Задаёт расстояние до каждого края родителя в UI-пикселях.
     *
     * @param left   отступ слева
     * @param top    отступ сверху
     * @param right  отступ справа
     * @param bottom отступ снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle inset(float left, float top, float right, float bottom) {
        return inset(
                SizeValue.px(left), SizeValue.px(top),
                SizeValue.px(right), SizeValue.px(bottom));
    }

    private LayoutStyle setSize(SizeValue current, SizeValue next, Consumer<SizeValue> setter) {
        SizeValue normalized = next == null ? SizeValue.auto() : next;
        if (Objects.equals(current, normalized)) return this;
        setter.accept(normalized);
        changed();
        return this;
    }

    private void changed() {
        if (updateDepth > 0) {
            pendingChange = true;
            return;
        }
        notifyChanged();
    }

    private void notifyChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }

    private static SizeValue fromLayoutFloat(float value) {
        return LayoutConstraints.isAuto(value) ? SizeValue.auto() : SizeValue.px(value);
    }

    private static SizeValue fromMaximumLayoutFloat(float value) {
        return Float.isFinite(value) ? SizeValue.px(value) : SizeValue.auto();
    }

    private static Align commonAlignment(Alignment horizontal, Alignment vertical) {
        if (horizontal != vertical) return Align.AUTO;
        return switch (horizontal == null ? Alignment.STRETCH : horizontal) {
            case START -> Align.START;
            case CENTER -> Align.CENTER;
            case END -> Align.END;
            case BASELINE -> Align.BASELINE;
            case STRETCH -> Align.AUTO;
        };
    }

    private static Alignment toLegacyAlignment(Align align) {
        return switch (align == null ? Align.AUTO : align) {
            case START -> Alignment.START;
            case CENTER -> Alignment.CENTER;
            case END -> Alignment.END;
            case BASELINE -> Alignment.BASELINE;
            case AUTO, STRETCH -> Alignment.STRETCH;
        };
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }

    void setLegacyAlignmentInternal(Alignment horizontal, Alignment vertical) {
        horizontalAlignment = horizontal == null ? Alignment.STRETCH : horizontal;
        verticalAlignment = vertical == null ? Alignment.STRETCH : vertical;
        alignSelf = commonAlignment(horizontalAlignment, verticalAlignment);
    }

    Alignment legacyHorizontalAlignmentInternal() {
        return horizontalAlignment;
    }

    Alignment legacyVerticalAlignmentInternal() {
        return verticalAlignment;
    }

    // -------------------------------------------------------------------------
    // API совместимости со старым форматом
    // -------------------------------------------------------------------------

    /**
     * Переносит старые {@link LayoutConstraints} в {@code LayoutStyle}.
     *
     * <p>Метод переносит предпочтительные и ограничивающие размеры, margin, grow
     * и выравнивание старого API. Если ограничения отсутствуют, используется
     * {@link LayoutConstraints#DEFAULT}.</p>
     *
     * @param constraints ограничения старого API или {@code null}
     * @return этот стиль для fluent-настройки
     * @deprecated используйте {@code LayoutStyle} напрямую; метод оставлен для
     * переходного слоя и старых интеграций
     */
    @Deprecated(forRemoval = false)
    public LayoutStyle applyLegacyConstraints(LayoutConstraints constraints) {
        return copyFrom(LayoutStyleLegacyAdapter.fromConstraints(constraints));
    }

    /**
     * Преобразует текущий стиль в старый формат {@link LayoutConstraints}.
     *
     * <p>Проценты не имеют прямого аналога в ограничениях старого API, поэтому для них
     * используется значение из {@code fallback}. {@code auto} для preferred-размера
     * становится {@link LayoutConstraints#AUTO}, а {@code auto} для max-размера —
     * {@link Float#POSITIVE_INFINITY}.</p>
     *
     * @param fallback значения для неподдерживаемых и процентных полей
     * @return представление текущего стиля layout в старом формате
     * @deprecated используйте {@code LayoutStyle} или V3 snapshot напрямую;
     * метод оставлен для обратной совместимости
     */
    @Deprecated(forRemoval = false)
    public LayoutConstraints toLegacyConstraints(LayoutConstraints fallback) {
        return LayoutStyleLegacyAdapter.toConstraints(this, fallback);
    }

    /**
     * Возвращает legacy-выравнивание по горизонтали.
     *
     * @return горизонтальное выравнивание
     * @deprecated используйте {@link #alignItems(Align)} или
     * {@link #alignSelf(Align)} для flex-выравнивания
     */
    @Deprecated(forRemoval = false)
    public Alignment horizontalAlignment() {
        return horizontalAlignment;
    }

    /**
     * Задаёт старое горизонтальное выравнивание слота виджета.
     *
     * <p>Этот метод сохранён для кода, написанного до flex-выравнивания. В новом
     * коде обычно задают {@link #alignItems(Align)} у родителя или
     * {@link #alignSelf(Align)} у ребёнка.</p>
     *
     * @param horizontal горизонтальное выравнивание
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle horizontalAlignment(Alignment horizontal) {
        Alignment normalized = horizontal == null ? Alignment.STRETCH : horizontal;
        if (horizontalAlignment == normalized) return this;
        horizontalAlignment = normalized;
        alignSelf = commonAlignment(normalized, verticalAlignment);
        changed();
        return this;
    }

    /**
     * Возвращает legacy-выравнивание по вертикали.
     *
     * @return вертикальное выравнивание
     * @deprecated используйте {@link #alignItems(Align)} или
     * {@link #alignSelf(Align)} для flex-выравнивания
     */
    @Deprecated(forRemoval = false)
    public Alignment verticalAlignment() {
        return verticalAlignment;
    }

    /**
     * Задаёт старое вертикальное выравнивание слота виджета.
     *
     * <p>Этот метод сохранён для совместимости. В flex-контейнере выравнивание
     * зависит от направления главной оси, поэтому обычно удобнее применять
     * {@link #alignItems(Align)} к родителю или {@link #alignSelf(Align)} к ребёнку.</p>
     *
     * @param vertical вертикальное выравнивание
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle verticalAlignment(Alignment vertical) {
        Alignment normalized = vertical == null ? Alignment.STRETCH : vertical;
        if (verticalAlignment == normalized) return this;
        verticalAlignment = normalized;
        alignSelf = commonAlignment(horizontalAlignment, normalized);
        changed();
        return this;
    }

    /**
     * Задаёт legacy-выравнивание по горизонтали и вертикали.
     *
     * @param horizontal горизонтальное выравнивание; {@code null} нормализуется в {@link Alignment#STRETCH}
     * @param vertical   вертикальное выравнивание; {@code null} нормализуется в {@link Alignment#STRETCH}
     * @return этот стиль для fluent-настройки
     * @deprecated для flex-контейнеров используйте {@link #alignItems(Align)}
     * на родителе и {@link #alignSelf(Align)} на дочернем виджете
     */
    @Deprecated(forRemoval = false)
    public LayoutStyle align(Alignment horizontal, Alignment vertical) {
        Alignment normalizedHorizontal = horizontal == null ? Alignment.STRETCH : horizontal;
        Alignment normalizedVertical = vertical == null ? Alignment.STRETCH : vertical;
        if (horizontalAlignment == normalizedHorizontal && verticalAlignment == normalizedVertical) return this;
        horizontalAlignment = normalizedHorizontal;
        verticalAlignment = normalizedVertical;
        alignSelf = commonAlignment(normalizedHorizontal, normalizedVertical);
        changed();
        return this;
    }
}

package dev.sixik.unigui.api.layout;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Изменяемый стиль layout'а, который хранит размеры, отступы, flex-настройки,
 * overflow и позиционирование виджета.
 *
 * <p>Обычно этот объект не создают вручную, а настраивают через
 * {@code widget.layout(style -> ...)}. Все setter-методы возвращают этот же
 * {@code LayoutStyle}, поэтому их удобно вызывать цепочкой.</p>
 *
 * <p>По смыслу API близок к CSS/Flexbox:</p>
 *
 * <ul>
 *     <li>{@code width/height/min/max} задают предпочтительные и ограничивающие размеры;</li>
 *     <li>{@code margin} — внешний отступ виджета, {@code padding} — внутренний отступ его детей;</li>
 *     <li>{@code flexGrow/flexShrink/flexBasis} работают на главной оси flex-контейнера;</li>
 *     <li>{@code alignItems/alignSelf} работают на поперечной оси;</li>
 *     <li>{@code justifyContent} распределяет детей на главной оси;</li>
 *     <li>{@code position(ABSOLUTE)} выводит виджет из обычного потока и использует {@code inset}.</li>
 * </ul>
 *
 * <p>Основной публичный путь — методы размеров, flex и position. Для типичных
 * случаев доступны CSS-подобные помощники {@link #fixed(float, float)},
 * {@link #fill()}, {@link #expand()}, {@link #center()} и {@link #flexNone()}.
 * Методы
 * совместимости со старыми {@link LayoutConstraints} и {@link Alignment}
 * собраны в конце файла и помечены {@link Deprecated}.</p>
 *
 * <p>Пример: растянуть центральную область, но оставить фиксированную высоту панели.</p>
 *
 * <pre>{@code
 * VBox root = new VBox();
 *
 * Label header = new Label("Header");
 * header.layout(style -> style.height(24.0f).flexNone());
 *
 * ScrollView body = new ScrollView(content);
 * body.layout(style -> style
 *         .flex(1.0f)
 *         .overflowY(Overflow.AUTO));
 *
 * root.addChild(header);
 * root.addChild(body);
 * }</pre>
 *
 * <p>Важно: {@code flexGrow(1)} не означает "размер 100%". Это вес, с которым
 * дочерний виджет забирает свободное место у родителя на главной оси. В
 * {@code VBox}/{@link FlexDirection#COLUMN} это высота, в {@code HBox}/{@link FlexDirection#ROW}
 * это ширина.</p>
 *
 * @see SizeValue
 * @see FlexDirection
 * @see Align
 * @see Justify
 * @see PositionType
 */
public final class LayoutStyle {
    private Runnable onChanged;
    private int updateDepth;
    private boolean pendingChange;

    private PositionType position = PositionType.RELATIVE;
    private SizeValue width = SizeValue.auto();
    private SizeValue height = SizeValue.auto();
    private SizeValue minWidth = SizeValue.px(0.0f);
    private SizeValue minHeight = SizeValue.px(0.0f);
    private SizeValue maxWidth = SizeValue.auto();
    private SizeValue maxHeight = SizeValue.auto();
    private EdgeInsets margin = EdgeInsets.ZERO;
    private EdgeInsets padding = EdgeInsets.ZERO;
    private Overflow overflowX = Overflow.VISIBLE;
    private Overflow overflowY = Overflow.VISIBLE;
    private FlexDirection flexDirection = FlexDirection.COLUMN;
    private FlexWrap flexWrap = FlexWrap.NOWRAP;
    private float rowGap;
    private float columnGap;
    private float flexGrow;
    private float flexShrink = 1.0f;
    private SizeValue flexBasis = SizeValue.auto();
    private Align alignItems = Align.STRETCH;
    private Align alignSelf = Align.AUTO;
    private Alignment horizontalAlignment = Alignment.STRETCH;
    private Alignment verticalAlignment = Alignment.STRETCH;
    private Justify justifyContent = Justify.START;
    private SizeValue left = SizeValue.auto();
    private SizeValue top = SizeValue.auto();
    private SizeValue right = SizeValue.auto();
    private SizeValue bottom = SizeValue.auto();

    /**
     * Создаёт стиль с дефолтными layout-настройками.
     *
     * <p>Дефолты: {@link PositionType#RELATIVE}, {@code width/height = auto},
     * {@code minSize = 0}, {@code maxSize = auto}, {@code flexDirection = COLUMN},
     * {@code flexGrow = 0}, {@code flexShrink = 1}, {@code alignItems = STRETCH}.</p>
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
     * Заменяет callback изменения.
     *
     * <p>Обычно используется инфраструктурой виджета. При ручном использовании
     * callback вызывается после каждого фактического изменения, кроме пакетных
     * изменений внутри {@link #update(Consumer)}.</p>
     *
     * @param onChanged новый callback изменения; может быть {@code null}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    /**
     * Выполняет несколько изменений как одну layout-операцию.
     *
     * <p>Если внутри {@code update} поменять несколько свойств, {@code onChanged}
     * будет вызван один раз в конце. Это полезно для больших chained-настроек и
     * уменьшает лишние invalidation'ы.</p>
     *
     * @param update блок изменений
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle update(Consumer<LayoutStyle> update) {
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
    public LayoutStyle copy() {
        return new LayoutStyle().copyFrom(this);
    }

    /**
     * Копирует все layout-свойства из другого стиля в текущий.
     *
     * <p>Callback текущего объекта сохраняется. Если {@code other == null},
     * копируются дефолтные значения нового {@code LayoutStyle}.</p>
     *
     * @param other источник настроек
     * @return этот стиль для fluent-настройки
     */
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
            style.alignItems(source.alignItems);
            style.alignSelf(source.alignSelf);
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
     * Возвращает режим позиционирования виджета.
     *
     * @return {@link PositionType#RELATIVE} для обычного потока или {@link PositionType#ABSOLUTE} для абсолютного позиционирования
     */
    public PositionType position() {
        return position;
    }

    /**
     * Задаёт режим позиционирования.
     *
     * <p>{@link PositionType#RELATIVE} участвует в обычном layout-потоке родителя.
     * {@link PositionType#ABSOLUTE} позиционируется через {@link #left()}, {@link #top()},
     * {@link #right()} и {@link #bottom()} и не занимает место среди обычных siblings.</p>
     *
     * @param position режим позиционирования; {@code null} нормализуется в {@link PositionType#RELATIVE}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle position(PositionType position) {
        PositionType normalized = position == null ? PositionType.RELATIVE : position;
        if (this.position == normalized) return this;
        this.position = normalized;
        changed();
        return this;
    }

    /**
     * Возвращает предпочтительную ширину.
     *
     * @return ширина: {@code auto}, пиксели или проценты
     */
    public SizeValue width() {
        return width;
    }

    /**
     * Задаёт предпочтительную ширину.
     *
     * <p>{@link SizeValue#auto()} означает "пусть layout измерит содержимое или
     * использует ограничения родителя". Проценты считаются от доступной ширины
     * родителя, если текущий layout engine может её определить.</p>
     *
     * @param width новая ширина; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle width(SizeValue width) {
        return setSize(this.width, width, value -> this.width = value);
    }

    /**
     * Задаёт предпочтительную ширину в пикселях.
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
     * Задаёт предпочтительную ширину в процентах от доступной ширины родителя.
     *
     * @param percent процент, где {@code 100.0f} означает 100%
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle widthPercent(float percent) {
        return width(SizeValue.percent(percent));
    }

    /**
     * Задаёт ширину и высоту одной операцией.
     *
     * @param width  предпочтительная ширина
     * @param height предпочтительная высота
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle size(SizeValue width, SizeValue height) {
        return update(style -> {
            style.width(width);
            style.height(height);
        });
    }

    /**
     * Задаёт ширину и высоту в пикселях одной операцией.
     *
     * @param width  ширина в пикселях или {@link LayoutConstraints#AUTO}
     * @param height высота в пикселях или {@link LayoutConstraints#AUTO}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle size(float width, float height) {
        return update(style -> {
            style.width(width);
            style.height(height);
        });
    }

    /**
     * Задаёт ширину и высоту в процентах от доступного размера родителя.
     *
     * @param widthPercent  ширина в процентах
     * @param heightPercent высота в процентах
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle sizePercent(float widthPercent, float heightPercent) {
        return size(SizeValue.percent(widthPercent), SizeValue.percent(heightPercent));
    }

    /**
     * Задаёт фиксированный размер и запрещает flex-изменение размера.
     *
     * <p>Удобно для кнопок, заголовков, боковых панелей и других элементов, которые
     * должны сохранить заданные размеры внутри flex-контейнера.</p>
     *
     * @param width  ширина в пикселях
     * @param height высота в пикселях
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle fixed(float width, float height) {
        return update(style -> {
            style.size(width, height);
            style.flexBasis(SizeValue.auto());
            style.flexGrow(0.0f);
            style.flexShrink(0.0f);
        });
    }

    /**
     * Растягивает виджет до 100% доступной ширины и высоты родителя.
     *
     * <p>Это аналог явных {@code width: 100%} и {@code height: 100%}. Для
     * занятия только свободного места на главной оси используйте {@link #expand()}.</p>
     *
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle fill() {
        return sizePercent(100.0f, 100.0f);
    }

    /**
     * Возвращает предпочтительную высоту.
     *
     * @return высота: {@code auto}, пиксели или проценты
     */
    public SizeValue height() {
        return height;
    }

    /**
     * Задаёт предпочтительную высоту.
     *
     * @param height новая высота; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle height(SizeValue height) {
        return setSize(this.height, height, value -> this.height = value);
    }

    /**
     * Задаёт предпочтительную высоту в пикселях.
     *
     * @param pixels высота в пикселях или значение {@link LayoutConstraints#AUTO} из старого API
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle height(float pixels) {
        return height(fromLayoutFloat(pixels));
    }

    /**
     * Задаёт предпочтительную высоту в процентах от доступной высоты родителя.
     *
     * @param percent процент, где {@code 100.0f} означает 100%
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle heightPercent(float percent) {
        return height(SizeValue.percent(percent));
    }

    /**
     * Возвращает минимальную ширину.
     *
     * @return нижняя граница ширины
     */
    public SizeValue minWidth() {
        return minWidth;
    }

    /**
     * Задаёт минимальную ширину.
     *
     * <p>Финальная ширина не должна стать меньше этого значения, даже если
     * {@code flexShrink} пытается сжать виджет.</p>
     *
     * @param minWidth минимальная ширина; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle minWidth(SizeValue minWidth) {
        return setSize(this.minWidth, minWidth, value -> this.minWidth = value);
    }

    /**
     * Задаёт минимальную ширину в пикселях.
     *
     * @param pixels минимальная ширина
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle minWidth(float pixels) {
        return minWidth(SizeValue.px(pixels));
    }

    /**
     * Задаёт минимальную ширину в процентах от доступной ширины родителя.
     *
     * @param percent минимальная ширина в процентах
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle minWidthPercent(float percent) {
        return minWidth(SizeValue.percent(percent));
    }

    /**
     * Возвращает минимальную высоту.
     *
     * @return нижняя граница высоты
     */
    public SizeValue minHeight() {
        return minHeight;
    }

    /**
     * Задаёт минимальную высоту.
     *
     * @param minHeight минимальная высота; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle minHeight(SizeValue minHeight) {
        return setSize(this.minHeight, minHeight, value -> this.minHeight = value);
    }

    /**
     * Задаёт минимальную высоту в пикселях.
     *
     * @param pixels минимальная высота
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle minHeight(float pixels) {
        return minHeight(SizeValue.px(pixels));
    }

    /**
     * Задаёт минимальную высоту в процентах от доступной высоты родителя.
     *
     * @param percent минимальная высота в процентах
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle minHeightPercent(float percent) {
        return minHeight(SizeValue.percent(percent));
    }

    /**
     * Задаёт минимальные ширину и высоту одной операцией.
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
     * Задаёт минимальные ширину и высоту в пикселях.
     *
     * @param width  минимальная ширина
     * @param height минимальная высота
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle minSize(float width, float height) {
        return minSize(SizeValue.px(width), SizeValue.px(height));
    }

    /**
     * Возвращает максимальную ширину.
     *
     * @return верхняя граница ширины или {@code auto}, если ограничение не задано
     */
    public SizeValue maxWidth() {
        return maxWidth;
    }

    /**
     * Задаёт максимальную ширину.
     *
     * <p>Финальная ширина не должна стать больше этого значения, даже если
     * {@code flexGrow} пытается расширить виджет.</p>
     *
     * @param maxWidth максимальная ширина; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle maxWidth(SizeValue maxWidth) {
        return setSize(this.maxWidth, maxWidth, value -> this.maxWidth = value);
    }

    /**
     * Задаёт максимальную ширину в пикселях.
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
     * Задаёт максимальную ширину в процентах от доступной ширины родителя.
     *
     * @param percent максимальная ширина в процентах
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle maxWidthPercent(float percent) {
        return maxWidth(SizeValue.percent(percent));
    }

    /**
     * Возвращает максимальную высоту.
     *
     * @return верхняя граница высоты или {@code auto}, если ограничение не задано
     */
    public SizeValue maxHeight() {
        return maxHeight;
    }

    /**
     * Задаёт максимальную высоту.
     *
     * @param maxHeight максимальная высота; {@code null} нормализуется в {@code auto}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle maxHeight(SizeValue maxHeight) {
        return setSize(this.maxHeight, maxHeight, value -> this.maxHeight = value);
    }

    /**
     * Задаёт максимальную высоту в пикселях.
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
     * Задаёт максимальную высоту в процентах от доступной высоты родителя.
     *
     * @param percent максимальная высота в процентах
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle maxHeightPercent(float percent) {
        return maxHeight(SizeValue.percent(percent));
    }

    /**
     * Задаёт максимальные ширину и высоту одной операцией.
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
     * Задаёт максимальные ширину и высоту в пикселях.
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
     * Задаёт максимальные ширину и высоту в процентах.
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
     * <p>Margin занимает место снаружи виджета и влияет на размещение siblings.</p>
     *
     * @param margin внешний отступ; {@code null} нормализуется в {@link EdgeInsets#ZERO}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle margin(EdgeInsets margin) {
        EdgeInsets normalized = margin == null ? EdgeInsets.ZERO : margin;
        if (this.margin.equals(normalized)) return this;
        this.margin = normalized;
        changed();
        return this;
    }

    /**
     * Задаёт одинаковый внешний отступ со всех сторон.
     *
     * @param all отступ слева, сверху, справа и снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle margin(float all) {
        return margin(EdgeInsets.all(all));
    }

    /**
     * Задаёт симметричный внешний отступ.
     *
     * @param horizontal отступ слева и справа
     * @param vertical   отступ сверху и снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle margin(float horizontal, float vertical) {
        return margin(EdgeInsets.symmetric(horizontal, vertical));
    }

    /**
     * Задаёт внешний отступ по каждой стороне.
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
     * Возвращает внутренний отступ контейнера.
     *
     * @return padding внутри виджета
     */
    public EdgeInsets padding() {
        return padding;
    }

    /**
     * Задаёт внутренний отступ контейнера.
     *
     * <p>Padding уменьшает область, в которой размещаются дочерние виджеты.</p>
     *
     * @param padding внутренний отступ; {@code null} нормализуется в {@link EdgeInsets#ZERO}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle padding(EdgeInsets padding) {
        EdgeInsets normalized = padding == null ? EdgeInsets.ZERO : padding;
        if (this.padding.equals(normalized)) return this;
        this.padding = normalized;
        changed();
        return this;
    }

    /**
     * Задаёт одинаковый внутренний отступ со всех сторон.
     *
     * @param all отступ слева, сверху, справа и снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle padding(float all) {
        return padding(EdgeInsets.all(all));
    }

    /**
     * Задаёт симметричный внутренний отступ.
     *
     * @param horizontal отступ слева и справа
     * @param vertical   отступ сверху и снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle padding(float horizontal, float vertical) {
        return padding(EdgeInsets.symmetric(horizontal, vertical));
    }

    /**
     * Задаёт внутренний отступ по каждой стороне.
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
     * Возвращает overflow-режим по горизонтальной оси.
     *
     * @return режим overflow по X
     */
    public Overflow overflowX() {
        return overflowX;
    }

    /**
     * Задаёт overflow-режим по горизонтальной оси.
     *
     * @param overflowX режим overflow; {@code null} нормализуется в {@link Overflow#VISIBLE}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle overflowX(Overflow overflowX) {
        Overflow normalized = overflowX == null ? Overflow.VISIBLE : overflowX;
        if (this.overflowX == normalized) return this;
        this.overflowX = normalized;
        changed();
        return this;
    }

    /**
     * Возвращает overflow-режим по вертикальной оси.
     *
     * @return режим overflow по Y
     */
    public Overflow overflowY() {
        return overflowY;
    }

    /**
     * Задаёт overflow-режим по вертикальной оси.
     *
     * @param overflowY режим overflow; {@code null} нормализуется в {@link Overflow#VISIBLE}
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle overflowY(Overflow overflowY) {
        Overflow normalized = overflowY == null ? Overflow.VISIBLE : overflowY;
        if (this.overflowY == normalized) return this;
        this.overflowY = normalized;
        changed();
        return this;
    }

    /**
     * Задаёт одинаковый overflow-режим по X и Y.
     *
     * @param overflow режим overflow для обеих осей
     * @return этот стиль для fluent-настройки
     */
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
     * Возвращает режим переноса flex-детей на новые строки/колонки.
     *
     * @return текущий режим wrap
     */
    public FlexWrap flexWrap() {
        return flexWrap;
    }

    /**
     * Задаёт режим переноса flex-детей.
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
     * Возвращает вертикальный промежуток между рядами layout'а.
     *
     * @return row gap в пикселях
     */
    public float rowGap() {
        return rowGap;
    }

    /**
     * Задаёт вертикальный промежуток между рядами layout'а.
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
     * Возвращает горизонтальный промежуток между колонками layout'а.
     *
     * @return column gap в пикселях
     */
    public float columnGap() {
        return columnGap;
    }

    /**
     * Задаёт горизонтальный промежуток между колонками layout'а.
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
     * Задаёт одинаковый {@link #rowGap(float)} и {@link #columnGap(float)}.
     *
     * @param gap промежуток в пикселях
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle gap(float gap) {
        return update(style -> {
            style.rowGap(gap);
            style.columnGap(gap);
        });
    }

    /**
     * CSS-сокращение {@code row-gap} и {@code column-gap} в пикселях UI-пространства.
     *
     * @param rowGap    расстояние между flex-строками или линиями переноса
     * @param columnGap расстояние между flex-колонками или элементами строки
     * @return этот стиль для fluent-настройки
     */
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
     * @return shrink-вес; 0 означает "не сжиматься из-за нехватки места"
     */
    public float flexShrink() {
        return flexShrink;
    }

    /**
     * Задаёт, как дочерний виджет отдаёт место, когда flex-родителю не хватает размера.
     *
     * <p>{@code flexShrink = 1} — дефолт: виджет может ужиматься. {@code 0} полезен
     * для фиксированных header/footer/sidebar, которые не должны сжиматься.</p>
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
     * Задаёт базовый размер виджета на главной оси до применения {@code flexGrow} и {@code flexShrink}.
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
     * Задаёт {@code flexGrow}, {@code flexShrink} и {@code flexBasis} одной операцией.
     *
     * <p>Аналог CSS shorthand {@code flex: grow shrink basis}.</p>
     *
     * @param grow   grow-вес
     * @param shrink shrink-вес
     * @param basis  базовый размер на главной оси
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle flex(float grow, float shrink, SizeValue basis) {
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
     * Числовая форма CSS {@code flex}: разворачивается в {@code grow 1 0%}.
     */
    public LayoutStyle flex(float grow) {
        return flex(grow, 1.0f, SizeValue.percent(0.0f));
    }

    /**
     * CSS {@code flex: auto} ({@code 1 1 auto}): растёт и сжимается от размера или содержимого.
     */
    public LayoutStyle flexAuto() {
        return flex(1.0f, 1.0f, SizeValue.auto());
    }

    /**
     * CSS {@code flex: initial} ({@code 0 1 auto}): использует размер или содержимое и допускает сжатие.
     */
    public LayoutStyle flexInitial() {
        return flex(0.0f, 1.0f, SizeValue.auto());
    }

    /**
     * CSS {@code flex: none} ({@code 0 0 auto}): использует размер или содержимое без flex-изменений.
     */
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
    public LayoutStyle expand() {
        return flex(1.0f, 1.0f, SizeValue.px(0.0f));
    }

    /**
     * Запрещает виджету сжиматься из-за нехватки места у flex-родителя.
     *
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle noShrink() {
        return flexShrink(0.0f);
    }

    /**
     * Возвращает выравнивание детей контейнера на поперечной оси.
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
    public LayoutStyle alignItems(Align alignItems) {
        Align normalized = alignItems == null ? Align.STRETCH : alignItems;
        if (this.alignItems == normalized) return this;
        this.alignItems = normalized;
        changed();
        return this;
    }

    /**
     * Возвращает индивидуальное выравнивание этого виджета внутри родителя.
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
     * Возвращает распределение детей контейнера на главной оси.
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
    public LayoutStyle centerSelf() {
        return alignSelf(Align.CENTER);
    }

    /**
     * Возвращает левый inset для абсолютного позиционирования.
     *
     * @return left inset
     */
    public SizeValue left() {
        return left;
    }

    /**
     * Задаёт левый inset.
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
     * Возвращает верхний inset для абсолютного позиционирования.
     *
     * @return top inset
     */
    public SizeValue top() {
        return top;
    }

    /**
     * Задаёт верхний inset.
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
     * Возвращает правый inset для абсолютного позиционирования.
     *
     * @return right inset
     */
    public SizeValue right() {
        return right;
    }

    /**
     * Задаёт правый inset.
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
     * Возвращает нижний inset для абсолютного позиционирования.
     *
     * @return bottom inset
     */
    public SizeValue bottom() {
        return bottom;
    }

    /**
     * Задаёт нижний inset.
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
     * Задаёт все inset-значения одной операцией.
     *
     * <p>Для абсолютного позиционирования это аналог CSS {@code left/top/right/bottom}.</p>
     *
     * @param left   отступ слева
     * @param top    отступ сверху
     * @param right  отступ справа
     * @param bottom отступ снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle inset(SizeValue left, SizeValue top, SizeValue right, SizeValue bottom) {
        return update(style -> {
            style.left(left);
            style.top(top);
            style.right(right);
            style.bottom(bottom);
        });
    }

    /**
     * Задаёт одинаковый inset со всех сторон.
     *
     * @param all отступ слева, сверху, справа и снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle inset(float all) {
        return inset(all, all, all, all);
    }

    /**
     * Задаёт симметричный inset.
     *
     * @param horizontal отступ слева и справа
     * @param vertical   отступ сверху и снизу
     * @return этот стиль для fluent-настройки
     */
    public LayoutStyle inset(float horizontal, float vertical) {
        return inset(horizontal, vertical, horizontal, vertical);
    }

    /**
     * Задаёт inset по каждой стороне в пикселях.
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
            case STRETCH -> Align.AUTO;
        };
    }

    private static Alignment toLegacyAlignment(Align align) {
        return switch (align == null ? Align.AUTO : align) {
            case START -> Alignment.START;
            case CENTER -> Alignment.CENTER;
            case END -> Alignment.END;
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
     * Преобразует текущий стиль обратно в {@link LayoutConstraints}.
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

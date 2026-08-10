package dev.sixik.unigui.api.layout;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Изменяемый стиль компоновки v2, прикреплённый к каждому {@code WidgetBase}.
 *
 * <p>Перегрузки с пикселями дают краткие значения по умолчанию; перегрузки с {@link SizeValue} поддерживают
 * {@link SizeValue#auto()}, пиксели и проценты. Несколько изменений внутри
 * {@link #update(Consumer)} отправляют одно уведомление об изменении.</p>
 *
 * <pre>{@code
 * widget.layout(style -> style
 *         .sizePercent(100.0f, 50.0f)
 *         .minSize(80.0f, 24.0f)
 *         .padding(8.0f)
 *         .flex(1.0f, 1.0f, SizeValue.px(120.0f))
 *         .overflowY(Overflow.AUTO));
 * }</pre>
 */
public final class LayoutStyle {
    /**
     * Хранит состояние или настройку {@code onChanged}, используемую логикой объекта.
     */
    private Runnable onChanged;
    /**
     * Хранит числовой параметр {@code updateDepth}, используемый в расчётах, вводе или отрисовке.
     */
    private int updateDepth;
    /**
     * Флаг {@code pendingChange} хранит текущее состояние или режим работы объекта.
     */
    private boolean pendingChange;

    /**
     * Хранит числовой параметр {@code position}, используемый в расчётах, вводе или отрисовке.
     */
    private PositionType position = PositionType.RELATIVE;
    /**
     * Хранит ширину области или ресурса в пикселях.
     */
    private SizeValue width = SizeValue.auto();
    /**
     * Хранит высоту области или ресурса в пикселях.
     */
    private SizeValue height = SizeValue.auto();
    /**
     * Хранит текстовое или идентификационное значение {@code minWidth}.
     */
    private SizeValue minWidth = SizeValue.px(0.0f);
    /**
     * Хранит числовой параметр {@code minHeight}, используемый в расчётах, вводе или отрисовке.
     */
    private SizeValue minHeight = SizeValue.px(0.0f);
    /**
     * Хранит текстовое или идентификационное значение {@code maxWidth}.
     */
    private SizeValue maxWidth = SizeValue.auto();
    /**
     * Хранит числовой параметр {@code maxHeight}, используемый в расчётах, вводе или отрисовке.
     */
    private SizeValue maxHeight = SizeValue.auto();
    /**
     * Хранит состояние или настройку {@code margin}, используемую логикой объекта.
     */
    private EdgeInsets margin = EdgeInsets.ZERO;
    /**
     * Хранит числовой параметр {@code padding}, используемый в расчётах, вводе или отрисовке.
     */
    private EdgeInsets padding = EdgeInsets.ZERO;
    /**
     * Хранит числовой параметр {@code overflowX}, используемый в расчётах, вводе или отрисовке.
     */
    private Overflow overflowX = Overflow.VISIBLE;
    /**
     * Хранит числовой параметр {@code overflowY}, используемый в расчётах, вводе или отрисовке.
     */
    private Overflow overflowY = Overflow.VISIBLE;
    /**
     * Хранит ссылку {@code flexDirection} на связанный объект или ресурс.
     */
    private FlexDirection flexDirection = FlexDirection.COLUMN;
    /**
     * Хранит состояние или настройку {@code flexWrap}, используемую логикой объекта.
     */
    private FlexWrap flexWrap = FlexWrap.NOWRAP;
    /**
     * Хранит числовой параметр {@code rowGap}, используемый в расчётах, вводе или отрисовке.
     */
    private float rowGap;
    /**
     * Хранит числовой параметр {@code columnGap}, используемый в расчётах, вводе или отрисовке.
     */
    private float columnGap;
    /**
     * Хранит числовой параметр {@code flexGrow}, используемый в расчётах, вводе или отрисовке.
     */
    private float flexGrow;
    /**
     * Хранит состояние или настройку {@code flexShrink}, используемую логикой объекта.
     */
    private float flexShrink = 1.0f;
    /**
     * Хранит состояние или настройку {@code flexBasis}, используемую логикой объекта.
     */
    private SizeValue flexBasis = SizeValue.auto();
    /**
     * Хранит коллекцию {@code alignItems}, с которой работает этот объект.
     */
    private Align alignItems = Align.STRETCH;
    /**
     * Хранит состояние или настройку {@code alignSelf}, используемую логикой объекта.
     */
    private Align alignSelf = Align.AUTO;
    /**
     * Хранит числовой параметр {@code horizontalAlignment}, используемый в расчётах, вводе или отрисовке.
     */
    private Alignment horizontalAlignment = Alignment.STRETCH;
    /**
     * Хранит состояние или настройку {@code verticalAlignment}, используемую логикой объекта.
     */
    private Alignment verticalAlignment = Alignment.STRETCH;
    /**
     * Хранит ссылку {@code justifyContent} на связанный объект или ресурс.
     */
    private Justify justifyContent = Justify.START;
    /**
     * Хранит состояние или настройку {@code left}, используемую логикой объекта.
     */
    private SizeValue left = SizeValue.auto();
    /**
     * Хранит состояние или настройку {@code top}, используемую логикой объекта.
     */
    private SizeValue top = SizeValue.auto();
    /**
     * Хранит состояние или настройку {@code right}, используемую логикой объекта.
     */
    private SizeValue right = SizeValue.auto();
    /**
     * Хранит состояние или настройку {@code bottom}, используемую логикой объекта.
     */
    private SizeValue bottom = SizeValue.auto();

    /**
     * Создаёт экземпляр {@code LayoutStyle} и подготавливает его начальное состояние.
     */
    public LayoutStyle() {
    }

    /**
     * Создаёт экземпляр {@code LayoutStyle} и подготавливает его начальное состояние.
     */
    public LayoutStyle(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    /**
     * Регистрирует или обрабатывает обратный вызов, связанный с {@code onChanged}.
     */
    public LayoutStyle onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    /**
     * Обновляет внутреннее состояние через операцию {@code update}.
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
                /** Возвращает текущее значение или выполняет операцию {@code notifyChanged}. */
                notifyChanged();
            }
        }
        return this;
    }

    /**
     * Создаёт копию текущего значения для безопасной передачи или изменения.
     */
    public LayoutStyle copy() {
        return new LayoutStyle().copyFrom(this);
    }

    /**
     * Создаёт копию или вариант объекта через операцию {@code copyFrom}.
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
     * Выполняет операцию {@code applyLegacyConstraints} с переданными параметрами.
     */
    public LayoutStyle applyLegacyConstraints(LayoutConstraints constraints) {
        LayoutConstraints source = constraints == null ? LayoutConstraints.DEFAULT : constraints;
        return update(style -> {
            style.width(fromLegacyPreferred(source.preferredWidth()));
            style.height(fromLegacyPreferred(source.preferredHeight()));
            style.minWidth(SizeValue.px(source.minWidth()));
            style.minHeight(SizeValue.px(source.minHeight()));
            style.maxWidth(fromLegacyMaximum(source.maxWidth()));
            style.maxHeight(fromLegacyMaximum(source.maxHeight()));
            style.margin(source.margin());
            style.flexGrow(source.grow());
            style.flexShrink(source.grow() > 0.0f ? 1.0f : 0.0f);
            style.horizontalAlignment = source.horizontalAlignment();
            style.verticalAlignment = source.verticalAlignment();
            style.alignSelf = commonAlignment(source.horizontalAlignment(), source.verticalAlignment());
        });
    }

    /**
     * Преобразует объект в представление, заданное операцией {@code toLegacyConstraints}.
     */
    public LayoutConstraints toLegacyConstraints(LayoutConstraints fallback) {
        LayoutConstraints source = fallback == null ? LayoutConstraints.DEFAULT : fallback;
        float preferredWidth = legacyPreferred(width, source.preferredWidth());
        float preferredHeight = legacyPreferred(height, source.preferredHeight());
        float resolvedMinWidth = legacyMinimum(minWidth, source.minWidth());
        float resolvedMinHeight = legacyMinimum(minHeight, source.minHeight());
        float resolvedMaxWidth = legacyMaximum(maxWidth, source.maxWidth());
        float resolvedMaxHeight = legacyMaximum(maxHeight, source.maxHeight());
        Alignment horizontal = horizontalAlignment;
        Alignment vertical = verticalAlignment;
        return new LayoutConstraints(
                preferredWidth, preferredHeight,
                resolvedMinWidth, resolvedMinHeight,
                resolvedMaxWidth, resolvedMaxHeight,
                margin,
                horizontal, vertical,
                flexGrow);
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code position}.
     */
    public PositionType position() {
        return position;
    }

    /**
     * Выполняет операцию {@code position} с переданными параметрами.
     */
    public LayoutStyle position(PositionType position) {
        PositionType normalized = position == null ? PositionType.RELATIVE : position;
        if (this.position == normalized) return this;
        this.position = normalized;
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Возвращает ширину области или ресурса.
     */
    public SizeValue width() {
        return width;
    }

    /**
     * Возвращает ширину области или ресурса.
     */
    public LayoutStyle width(SizeValue width) {
        return setSize(this.width, width, value -> this.width = value);
    }

    /**
     * Возвращает ширину области или ресурса.
     */
    public LayoutStyle width(float pixels) {
        return width(fromLayoutFloat(pixels));
    }

    /**
     * Выполняет операцию {@code widthPercent} с переданными параметрами.
     */
    public LayoutStyle widthPercent(float percent) {
        return width(SizeValue.percent(percent));
    }

    /**
     * Выполняет операцию {@code size} с переданными параметрами.
     */
    public LayoutStyle size(SizeValue width, SizeValue height) {
        return update(style -> {
            style.width(width);
            style.height(height);
        });
    }

    /**
     * Выполняет операцию {@code size} с переданными параметрами.
     */
    public LayoutStyle size(float width, float height) {
        return update(style -> {
            style.width(width);
            style.height(height);
        });
    }

    /**
     * Выполняет операцию {@code sizePercent} с переданными параметрами.
     */
    public LayoutStyle sizePercent(float widthPercent, float heightPercent) {
        return size(SizeValue.percent(widthPercent), SizeValue.percent(heightPercent));
    }

    /**
     * Возвращает высоту области или ресурса.
     */
    public SizeValue height() {
        return height;
    }

    /**
     * Возвращает высоту области или ресурса.
     */
    public LayoutStyle height(SizeValue height) {
        return setSize(this.height, height, value -> this.height = value);
    }

    /**
     * Возвращает высоту области или ресурса.
     */
    public LayoutStyle height(float pixels) {
        return height(fromLayoutFloat(pixels));
    }

    /**
     * Выполняет операцию {@code heightPercent} с переданными параметрами.
     */
    public LayoutStyle heightPercent(float percent) {
        return height(SizeValue.percent(percent));
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code minWidth}.
     */
    public SizeValue minWidth() {
        return minWidth;
    }

    /**
     * Выполняет операцию {@code minWidth} с переданными параметрами.
     */
    public LayoutStyle minWidth(SizeValue minWidth) {
        return setSize(this.minWidth, minWidth, value -> this.minWidth = value);
    }

    /**
     * Выполняет операцию {@code minWidth} с переданными параметрами.
     */
    public LayoutStyle minWidth(float pixels) {
        return minWidth(SizeValue.px(pixels));
    }

    /**
     * Выполняет операцию {@code minWidthPercent} с переданными параметрами.
     */
    public LayoutStyle minWidthPercent(float percent) {
        return minWidth(SizeValue.percent(percent));
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code minHeight}.
     */
    public SizeValue minHeight() {
        return minHeight;
    }

    /**
     * Выполняет операцию {@code minHeight} с переданными параметрами.
     */
    public LayoutStyle minHeight(SizeValue minHeight) {
        return setSize(this.minHeight, minHeight, value -> this.minHeight = value);
    }

    /**
     * Выполняет операцию {@code minHeight} с переданными параметрами.
     */
    public LayoutStyle minHeight(float pixels) {
        return minHeight(SizeValue.px(pixels));
    }

    /**
     * Выполняет операцию {@code minHeightPercent} с переданными параметрами.
     */
    public LayoutStyle minHeightPercent(float percent) {
        return minHeight(SizeValue.percent(percent));
    }

    /**
     * Выполняет операцию {@code minSize} с переданными параметрами.
     */
    public LayoutStyle minSize(SizeValue width, SizeValue height) {
        return update(style -> {
            style.minWidth(width);
            style.minHeight(height);
        });
    }

    /**
     * Выполняет операцию {@code minSize} с переданными параметрами.
     */
    public LayoutStyle minSize(float width, float height) {
        return minSize(SizeValue.px(width), SizeValue.px(height));
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code maxWidth}.
     */
    public SizeValue maxWidth() {
        return maxWidth;
    }

    /**
     * Выполняет операцию {@code maxWidth} с переданными параметрами.
     */
    public LayoutStyle maxWidth(SizeValue maxWidth) {
        return setSize(this.maxWidth, maxWidth, value -> this.maxWidth = value);
    }

    /**
     * Выполняет операцию {@code maxWidth} с переданными параметрами.
     */
    public LayoutStyle maxWidth(float pixels) {
        return maxWidth(fromMaximumLayoutFloat(pixels));
    }

    /**
     * Выполняет операцию {@code maxWidthPercent} с переданными параметрами.
     */
    public LayoutStyle maxWidthPercent(float percent) {
        return maxWidth(SizeValue.percent(percent));
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code maxHeight}.
     */
    public SizeValue maxHeight() {
        return maxHeight;
    }

    /**
     * Выполняет операцию {@code maxHeight} с переданными параметрами.
     */
    public LayoutStyle maxHeight(SizeValue maxHeight) {
        return setSize(this.maxHeight, maxHeight, value -> this.maxHeight = value);
    }

    /**
     * Выполняет операцию {@code maxHeight} с переданными параметрами.
     */
    public LayoutStyle maxHeight(float pixels) {
        return maxHeight(fromMaximumLayoutFloat(pixels));
    }

    /**
     * Выполняет операцию {@code maxHeightPercent} с переданными параметрами.
     */
    public LayoutStyle maxHeightPercent(float percent) {
        return maxHeight(SizeValue.percent(percent));
    }

    /**
     * Выполняет операцию {@code maxSize} с переданными параметрами.
     */
    public LayoutStyle maxSize(SizeValue width, SizeValue height) {
        return update(style -> {
            style.maxWidth(width);
            style.maxHeight(height);
        });
    }

    /**
     * Выполняет операцию {@code maxSize} с переданными параметрами.
     */
    public LayoutStyle maxSize(float width, float height) {
        return update(style -> {
            style.maxWidth(width);
            style.maxHeight(height);
        });
    }

    /**
     * Выполняет операцию {@code maxSizePercent} с переданными параметрами.
     */
    public LayoutStyle maxSizePercent(float widthPercent, float heightPercent) {
        return maxSize(SizeValue.percent(widthPercent), SizeValue.percent(heightPercent));
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code margin}.
     */
    public EdgeInsets margin() {
        return margin;
    }

    /**
     * Выполняет операцию {@code margin} с переданными параметрами.
     */
    public LayoutStyle margin(EdgeInsets margin) {
        EdgeInsets normalized = margin == null ? EdgeInsets.ZERO : margin;
        if (this.margin.equals(normalized)) return this;
        this.margin = normalized;
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Выполняет операцию {@code margin} с переданными параметрами.
     */
    public LayoutStyle margin(float all) {
        return margin(EdgeInsets.all(all));
    }

    /**
     * Выполняет операцию {@code margin} с переданными параметрами.
     */
    public LayoutStyle margin(float horizontal, float vertical) {
        return margin(EdgeInsets.symmetric(horizontal, vertical));
    }

    /**
     * Выполняет операцию {@code margin} с переданными параметрами.
     */
    public LayoutStyle margin(float left, float top, float right, float bottom) {
        return margin(new EdgeInsets(left, top, right, bottom));
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code padding}.
     */
    public EdgeInsets padding() {
        return padding;
    }

    /**
     * Выполняет операцию {@code padding} с переданными параметрами.
     */
    public LayoutStyle padding(EdgeInsets padding) {
        EdgeInsets normalized = padding == null ? EdgeInsets.ZERO : padding;
        if (this.padding.equals(normalized)) return this;
        this.padding = normalized;
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Выполняет операцию {@code padding} с переданными параметрами.
     */
    public LayoutStyle padding(float all) {
        return padding(EdgeInsets.all(all));
    }

    /**
     * Выполняет операцию {@code padding} с переданными параметрами.
     */
    public LayoutStyle padding(float horizontal, float vertical) {
        return padding(EdgeInsets.symmetric(horizontal, vertical));
    }

    /**
     * Выполняет операцию {@code padding} с переданными параметрами.
     */
    public LayoutStyle padding(float left, float top, float right, float bottom) {
        return padding(new EdgeInsets(left, top, right, bottom));
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code overflowX}.
     */
    public Overflow overflowX() {
        return overflowX;
    }

    /**
     * Выполняет операцию {@code overflowX} с переданными параметрами.
     */
    public LayoutStyle overflowX(Overflow overflowX) {
        Overflow normalized = overflowX == null ? Overflow.VISIBLE : overflowX;
        if (this.overflowX == normalized) return this;
        this.overflowX = normalized;
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code overflowY}.
     */
    public Overflow overflowY() {
        return overflowY;
    }

    /**
     * Выполняет операцию {@code overflowY} с переданными параметрами.
     */
    public LayoutStyle overflowY(Overflow overflowY) {
        Overflow normalized = overflowY == null ? Overflow.VISIBLE : overflowY;
        if (this.overflowY == normalized) return this;
        this.overflowY = normalized;
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Выполняет операцию {@code overflow} с переданными параметрами.
     */
    public LayoutStyle overflow(Overflow overflow) {
        return update(style -> {
            style.overflowX(overflow);
            style.overflowY(overflow);
        });
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code flexDirection}.
     */
    public FlexDirection flexDirection() {
        return flexDirection;
    }

    /**
     * Выполняет операцию {@code flexDirection} с переданными параметрами.
     */
    public LayoutStyle flexDirection(FlexDirection flexDirection) {
        FlexDirection normalized = flexDirection == null ? FlexDirection.COLUMN : flexDirection;
        if (this.flexDirection == normalized) return this;
        this.flexDirection = normalized;
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code flexWrap}.
     */
    public FlexWrap flexWrap() {
        return flexWrap;
    }

    /**
     * Выполняет операцию {@code flexWrap} с переданными параметрами.
     */
    public LayoutStyle flexWrap(FlexWrap flexWrap) {
        FlexWrap normalized = flexWrap == null ? FlexWrap.NOWRAP : flexWrap;
        if (this.flexWrap == normalized) return this;
        this.flexWrap = normalized;
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code rowGap}.
     */
    public float rowGap() {
        return rowGap;
    }

    /**
     * Выполняет операцию {@code rowGap} с переданными параметрами.
     */
    public LayoutStyle rowGap(float rowGap) {
        float normalized = sanitize(rowGap);
        if (this.rowGap == normalized) return this;
        this.rowGap = normalized;
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code columnGap}.
     */
    public float columnGap() {
        return columnGap;
    }

    /**
     * Выполняет операцию {@code columnGap} с переданными параметрами.
     */
    public LayoutStyle columnGap(float columnGap) {
        float normalized = sanitize(columnGap);
        if (this.columnGap == normalized) return this;
        this.columnGap = normalized;
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Выполняет операцию {@code gap} с переданными параметрами.
     */
    public LayoutStyle gap(float gap) {
        return update(style -> {
            style.rowGap(gap);
            style.columnGap(gap);
        });
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code flexGrow}.
     */
    public float flexGrow() {
        return flexGrow;
    }

    /**
     * Выполняет операцию {@code flexGrow} с переданными параметрами.
     */
    public LayoutStyle flexGrow(float flexGrow) {
        float normalized = sanitize(flexGrow);
        if (this.flexGrow == normalized) return this;
        this.flexGrow = normalized;
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code flexShrink}.
     */
    public float flexShrink() {
        return flexShrink;
    }

    /**
     * Выполняет операцию {@code flexShrink} с переданными параметрами.
     */
    public LayoutStyle flexShrink(float flexShrink) {
        float normalized = sanitize(flexShrink);
        if (this.flexShrink == normalized) return this;
        this.flexShrink = normalized;
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code flexBasis}.
     */
    public SizeValue flexBasis() {
        return flexBasis;
    }

    /**
     * Выполняет операцию {@code flexBasis} с переданными параметрами.
     */
    public LayoutStyle flexBasis(SizeValue flexBasis) {
        return setSize(this.flexBasis, flexBasis, value -> this.flexBasis = value);
    }

    /**
     * Выполняет операцию {@code flexBasis} с переданными параметрами.
     */
    public LayoutStyle flexBasis(float pixels) {
        return flexBasis(fromLayoutFloat(pixels));
    }

    /**
     * Выполняет операцию {@code flex} с переданными параметрами.
     */
    public LayoutStyle flex(float grow, float shrink, SizeValue basis) {
        return update(style -> {
            style.flexGrow(grow);
            style.flexShrink(shrink);
            style.flexBasis(basis);
        });
    }

    /**
     * Выполняет операцию {@code flex} с переданными параметрами.
     */
    public LayoutStyle flex(float grow, float shrink, float basisPixels) {
        return flex(grow, shrink, SizeValue.px(basisPixels));
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code alignItems}.
     */
    public Align alignItems() {
        return alignItems;
    }

    /**
     * Выполняет операцию {@code alignItems} с переданными параметрами.
     */
    public LayoutStyle alignItems(Align alignItems) {
        Align normalized = alignItems == null ? Align.STRETCH : alignItems;
        if (this.alignItems == normalized) return this;
        this.alignItems = normalized;
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code alignSelf}.
     */
    public Align alignSelf() {
        return alignSelf;
    }

    /**
     * Выполняет операцию {@code alignSelf} с переданными параметрами.
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
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code horizontalAlignment}.
     */
    public Alignment horizontalAlignment() {
        return horizontalAlignment;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code verticalAlignment}.
     */
    public Alignment verticalAlignment() {
        return verticalAlignment;
    }

    /**
     * Выполняет операцию {@code align} с переданными параметрами.
     */
    public LayoutStyle align(Alignment horizontal, Alignment vertical) {
        Alignment normalizedHorizontal = horizontal == null ? Alignment.STRETCH : horizontal;
        Alignment normalizedVertical = vertical == null ? Alignment.STRETCH : vertical;
        if (horizontalAlignment == normalizedHorizontal && verticalAlignment == normalizedVertical) return this;
        horizontalAlignment = normalizedHorizontal;
        verticalAlignment = normalizedVertical;
        alignSelf = commonAlignment(normalizedHorizontal, normalizedVertical);
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code justifyContent}.
     */
    public Justify justifyContent() {
        return justifyContent;
    }

    /**
     * Выполняет операцию {@code justifyContent} с переданными параметрами.
     */
    public LayoutStyle justifyContent(Justify justifyContent) {
        Justify normalized = justifyContent == null ? Justify.START : justifyContent;
        if (this.justifyContent == normalized) return this;
        this.justifyContent = normalized;
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code left}.
     */
    public SizeValue left() {
        return left;
    }

    /**
     * Выполняет операцию {@code left} с переданными параметрами.
     */
    public LayoutStyle left(SizeValue left) {
        return setSize(this.left, left, value -> this.left = value);
    }

    /**
     * Выполняет операцию {@code left} с переданными параметрами.
     */
    public LayoutStyle left(float pixels) {
        return left(SizeValue.px(pixels));
    }

    /**
     * Преобразует объект в представление, заданное операцией {@code top}.
     */
    public SizeValue top() {
        return top;
    }

    /**
     * Преобразует объект в представление, заданное операцией {@code top}.
     */
    public LayoutStyle top(SizeValue top) {
        return setSize(this.top, top, value -> this.top = value);
    }

    /**
     * Преобразует объект в представление, заданное операцией {@code top}.
     */
    public LayoutStyle top(float pixels) {
        return top(SizeValue.px(pixels));
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code right}.
     */
    public SizeValue right() {
        return right;
    }

    /**
     * Выполняет операцию {@code right} с переданными параметрами.
     */
    public LayoutStyle right(SizeValue right) {
        return setSize(this.right, right, value -> this.right = value);
    }

    /**
     * Выполняет операцию {@code right} с переданными параметрами.
     */
    public LayoutStyle right(float pixels) {
        return right(SizeValue.px(pixels));
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code bottom}.
     */
    public SizeValue bottom() {
        return bottom;
    }

    /**
     * Выполняет операцию {@code bottom} с переданными параметрами.
     */
    public LayoutStyle bottom(SizeValue bottom) {
        return setSize(this.bottom, bottom, value -> this.bottom = value);
    }

    /**
     * Выполняет операцию {@code bottom} с переданными параметрами.
     */
    public LayoutStyle bottom(float pixels) {
        return bottom(SizeValue.px(pixels));
    }

    /**
     * Выполняет операцию {@code inset} с переданными параметрами.
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
     * Выполняет операцию {@code inset} с переданными параметрами.
     */
    public LayoutStyle inset(float all) {
        return inset(all, all, all, all);
    }

    /**
     * Выполняет операцию {@code inset} с переданными параметрами.
     */
    public LayoutStyle inset(float horizontal, float vertical) {
        return inset(horizontal, vertical, horizontal, vertical);
    }

    /**
     * Выполняет операцию {@code inset} с переданными параметрами.
     */
    public LayoutStyle inset(float left, float top, float right, float bottom) {
        return inset(
                SizeValue.px(left), SizeValue.px(top),
                SizeValue.px(right), SizeValue.px(bottom));
    }

    /**
     * Задаёт значение или создаёт вариант объекта через операцию {@code setSize}.
     */
    private LayoutStyle setSize(SizeValue current, SizeValue next, Consumer<SizeValue> setter) {
        SizeValue normalized = next == null ? SizeValue.auto() : next;
        if (Objects.equals(current, normalized)) return this;
        setter.accept(normalized);
        /** Возвращает текущее значение или выполняет операцию {@code changed}. */
        changed();
        return this;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code changed}.
     */
    private void changed() {
        if (updateDepth > 0) {
            pendingChange = true;
            return;
        }
        /** Возвращает текущее значение или выполняет операцию {@code notifyChanged}. */
        notifyChanged();
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code notifyChanged}.
     */
    private void notifyChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }

    /**
     * Создаёт или восстанавливает объект через операцию {@code fromLegacyPreferred}.
     */
    private static SizeValue fromLegacyPreferred(float value) {
        return LayoutConstraints.isAuto(value) ? SizeValue.auto() : SizeValue.px(value);
    }

    /**
     * Создаёт или восстанавливает объект через операцию {@code fromLegacyMaximum}.
     */
    private static SizeValue fromLegacyMaximum(float value) {
        return Float.isFinite(value) ? SizeValue.px(value) : SizeValue.auto();
    }

    /**
     * Создаёт или восстанавливает объект через операцию {@code fromLayoutFloat}.
     */
    private static SizeValue fromLayoutFloat(float value) {
        return LayoutConstraints.isAuto(value) ? SizeValue.auto() : SizeValue.px(value);
    }

    /**
     * Создаёт или восстанавливает объект через операцию {@code fromMaximumLayoutFloat}.
     */
    private static SizeValue fromMaximumLayoutFloat(float value) {
        return Float.isFinite(value) ? SizeValue.px(value) : SizeValue.auto();
    }

    /**
     * Выполняет операцию {@code legacyPreferred} с переданными параметрами.
     */
    private static float legacyPreferred(SizeValue value, float fallback) {
        return value.isAuto() ? LayoutConstraints.AUTO : value.isPixels() ? value.value() : fallback;
    }

    /**
     * Выполняет операцию {@code legacyMinimum} с переданными параметрами.
     */
    private static float legacyMinimum(SizeValue value, float fallback) {
        return value.isAuto() ? 0.0f : value.isPixels() ? value.value() : fallback;
    }

    /**
     * Выполняет операцию {@code legacyMaximum} с переданными параметрами.
     */
    private static float legacyMaximum(SizeValue value, float fallback) {
        return value.isAuto() ? Float.POSITIVE_INFINITY : value.isPixels() ? value.value() : fallback;
    }

    /**
     * Выполняет операцию {@code commonAlignment} с переданными параметрами.
     */
    private static Align commonAlignment(Alignment horizontal, Alignment vertical) {
        if (horizontal != vertical) return Align.AUTO;
        return switch (horizontal == null ? Alignment.STRETCH : horizontal) {
            case START -> Align.START;
            case CENTER -> Align.CENTER;
            case END -> Align.END;
            case STRETCH -> Align.AUTO;
        };
    }

    /**
     * Преобразует объект в представление, заданное операцией {@code toLegacyAlignment}.
     */
    private static Alignment toLegacyAlignment(Align align) {
        return switch (align == null ? Align.AUTO : align) {
            case START -> Alignment.START;
            case CENTER -> Alignment.CENTER;
            case END -> Alignment.END;
            case AUTO, STRETCH -> Alignment.STRETCH;
        };
    }

    /**
     * Приводит входное значение к безопасному или допустимому диапазону.
     */
    private static float sanitize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}

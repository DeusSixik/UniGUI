package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.*;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.Popup;
import dev.sixik.unigui.widgets.WidgetStyles;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(
        summary = "Selection dropdown widget allowing picking one item from a list.",
        purpose = "Provides a standard single-selection dropdown list with hover highlighting and popup overlay.",
        useWhen = {
                "Select one option from a fixed or dynamic list of text options.",
                "Quality, resolution, language, or preset selector menus."
        },
        criticalRules = {
                "selectedIndex() returns -1 if nothing is selected.",
                "Selecting an option automatically closes the dropdown popup and emits selection events."
        },
        related = {"DropDownBox", "TreeListPicker", "Button", "Popup"})
@WikiPage(
        title = "<key:combo.box.title>",
        slug = "combo-box",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:combo.box.summary>",
        order = 210)
@WikiSection(
        title = "<key:combo.box.section.overview.title>",
        markdown = "<key:combo.box.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:combo.box.example.basic.title>",
        code = "<key:combo.box.example.basic.code>",
        order = 10)
@XmlWidgetName("ComboBox")
public class ComboBox extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.COMBO_BOX;

    private static final float HEADER_HEIGHT = 36.0f;
    private static final float OPTION_HEIGHT = 28.0f;
    private static final float OPTIONS_INSET = 2.0f;
    private static final int DEFAULT_MAX_VISIBLE_OPTIONS = 6;
    /**
     * Горизонтальный отступ строк-пилюль от краёв списка.
     */
    private static final float OPTION_SIDE_MARGIN = 4.0f;

    private final Button headerButton = new Button();
    private final Box optionsHost = new ComboBoxOptionsHost();
    private final VBox optionsList = new VBox();
    private final ScrollView optionsScroll = new ScrollView(optionsList);
    private final Popup dropDownPopup = new Popup();
    private final List<String> items = new ObjectArrayList<>();
    private final List<RichText> richItems = new ObjectArrayList<>();
    private final List<ToggleButton> optionButtons = new ObjectArrayList<>();
    /**
     * Цвет подсветки строки при наведении.
     */
    private final MutableColor optionHoverBackground = new MutableColor(0.24f, 0.28f, 0.35f, 0.92f);
    private boolean customOptionHoverBackground;
    /**
     * Текстура подсветки строки при наведении.
     */
    private TextureHandle optionHoverTexture;
    /**
     * Шейдер подсветки строки при наведении.
     */
    private ShaderHandle optionHoverShader;
    /**
     * Uniforms шейдера подсветки строк.
     */
    private ShaderUniforms optionHoverShaderUniforms = ShaderUniforms.empty();
    /**
     * Параметры отрисовки шейдера подсветки строк.
     */
    private ShaderDrawOptions optionHoverShaderOptions = ShaderDrawOptions.defaults();
    private int selectedIndex = -1;
    private boolean opened;
    private String placeholder = "Select...";
    private RichText richPlaceholder = RichText.plain(placeholder);
    private final RichText defaultHeaderIndicatorClosed = DropDownBox.createHeaderArrow(false);
    private final RichText defaultHeaderIndicatorOpened = DropDownBox.createHeaderArrow(true);
    private RichText headerIndicator = defaultHeaderIndicatorClosed;
    private boolean customHeaderIndicator;
    private DropDownMode dropDownMode = DropDownMode.OVERLAY;
    private OverlayLayer explicitOverlayLayer;
    private OverlayLayer attachedOverlayLayer;
    private float dropDownWidth;
    private boolean dropDownMatchesWidgetWidth;
    private float optionRowHeight = OPTION_HEIGHT;
    private int maxVisibleOptions = DEFAULT_MAX_VISIBLE_OPTIONS;
    private boolean syncingPopup;

    public ComboBox() {
        super(Orientation.VERTICAL);
        optionHoverBackground.onChanged(() -> {
            customOptionHoverBackground = true;
            syncOptionHoverAppearance();
        });
        spacing(2.0f);
        focusable(true);

        headerButton.style(style -> style.putAll(WidgetStyles.Selection.DROPDOWN_HEADER));
        headerButton.layout(style -> style.size(LayoutConstraints.AUTO, HEADER_HEIGHT).flexGrow(0).flexShrink(0.0f));
        headerButton.textPaddingX(12.0f);
        headerButton.chevronVisible(true);
        headerButton.onClick(event -> toggle());

        optionsHost.visibility(Visibility.COLLAPSED);
        optionsHost.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));

        optionsList.spacing(1.0f);
        optionsList.layout(style -> style.margin(0.0f));
        optionsList.layout(style -> style.flexGrow(0).flexShrink(0.0f));

        optionsScroll.scrollStep(optionRowHeight);
        // Классический reserved-скроллбар: у строк своя полоса, скролл
        // не плавает поверх пилюль (overlay default здесь неуместен).
        optionsScroll.overlayScrollBars(false);
        optionsScroll.scrollbarGap(1.0f);
        optionsScroll.scrollbarTrackColor().set(0.0f, 0.0f, 0.0f, 0.36f);
        optionsScroll.scrollbarThumbColor().set(0.82f, 0.84f, 0.88f, 0.82f);
        optionsScroll.layout(style -> style
                .size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .margin(OPTIONS_INSET)
                .overflowX(Overflow.HIDDEN)
                .overflowY(Overflow.AUTO)
                .flexGrow(0)
                .flexShrink(0.0f));
        optionsHost.addChild(optionsScroll);

        dropDownPopup.anchor(headerButton);
        dropDownPopup.offset(0.0f, 6.0f);
        dropDownPopup.padding(EdgeInsets.all(0.0f));
        dropDownPopup.backgroundVisible(false);
        dropDownPopup.borderVisible(false);
        dropDownPopup.closeOnOutsideClick(true);
        dropDownPopup.onOpenChanged(() -> {
            if (!syncingPopup && dropDownMode == DropDownMode.OVERLAY && opened != dropDownPopup.opened()) {
                opened = dropDownPopup.opened();
                updateHeaderText();
                invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
            }
        });

        super.addChild(headerButton);
        updateHeaderText();
    }

    /**
     * Возвращает изменяемый цвет фона строки при наведении.
     *
     * @return live-цвет подсветки строк
     */
    public MutableColor optionHoverBackground() {
        return optionHoverBackground;
    }

    /**
     * Задаёт цвет фона строки при наведении.
     *
     * @param color цвет подсветки; {@code null} делает подсветку прозрачной
     * @return этот ComboBox для fluent-настройки
     */
    @XmlAttribute(value = "optionHoverBackground", category = "Appearance", defaultValue = "#3D4759EB", description = "Цвет строки при наведении.")
    @WikiMethod(
            title = "<key:combo.box.method.hover_bg.title>",
            summary = "<key:combo.box.method.hover_bg.summary>")
    public ComboBox optionHoverBackground(ColorView color) {
        if (color == null) optionHoverBackground.set(0.0f, 0.0f, 0.0f, 0.0f);
        else optionHoverBackground.set(color);
        syncOptionHoverAppearance();
        return this;
    }

    /**
     * Задаёт текстуру поверх фона строки при наведении.
     *
     * @param texture текстура или {@code null}, чтобы убрать слой
     * @return этот ComboBox для fluent-настройки
     */
    @XmlAttribute(value = "optionHoverTexture", category = "Assets", defaultValue = "", description = "Текстура строки при наведении.")
    public ComboBox optionHoverTexture(TextureHandle texture) {
        optionHoverTexture = texture;
        syncOptionHoverAppearance();
        return this;
    }

    /**
     * Задаёт шейдер поверх фона и текстуры строки при наведении.
     *
     * @param shader шейдер или {@code null}, чтобы убрать слой
     * @return этот ComboBox для fluent-настройки
     */
    @XmlAttribute(value = "optionHoverShader", category = "Assets", defaultValue = "", description = "Шейдер строки при наведении.")
    public ComboBox optionHoverShader(ShaderHandle shader) {
        optionHoverShader = shader;
        syncOptionHoverAppearance();
        return this;
    }

    @WikiMethod(
            title = "<key:combo.box.method.items.title>",
            summary = "<key:combo.box.method.items.summary>")
    public List<String> items() {
        return Collections.unmodifiableList(items);
    }

    @WikiMethod(
            title = "<key:combo.box.method.set_items.title>",
            summary = "<key:combo.box.method.set_items.summary>")
    public ComboBox items(List<String> items) {
        this.items.clear();
        this.richItems.clear();
        if (items != null) {
            for (String item : items) {
                addItemInternal(RichText.resolve(item));
            }
        }
        if (selectedIndex >= this.items.size()) {
            selectedIndex = this.items.isEmpty() ? -1 : this.items.size() - 1;
        }
        rebuildOptions();
        syncSelectionState();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @XmlAttribute(value = "items", category = "Content", defaultValue = "", description = "Dropdown options separated by '|', ';' or line breaks.")
    public ComboBox items(String items) {
        if (items == null || items.isBlank()) {
            return items(List.of());
        }
        List<String> parsed = new ObjectArrayList<>();
        for (String item : items.split("[|;\\r\\n]+")) {
            String normalized = item.trim();
            if (!normalized.isEmpty()) parsed.add(normalized);
        }
        return items(parsed);
    }

    public ComboBox richItems(List<RichText> items) {
        this.items.clear();
        this.richItems.clear();
        if (items != null) {
            for (RichText item : items) {
                addItemInternal(item);
            }
        }
        if (selectedIndex >= this.items.size()) {
            selectedIndex = this.items.isEmpty() ? -1 : this.items.size() - 1;
        }
        rebuildOptions();
        syncSelectionState();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(
            title = "<key:combo.box.method.add_item.title>",
            summary = "<key:combo.box.method.add_item.summary>")
    public ComboBox addItem(String item) {
        addItemInternal(RichText.resolve(item));
        rebuildOptions();
        if (selectedIndex < 0) {
            setSelectedIndex(0, false);
        } else {
            syncSelectionState();
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ComboBox addItem(RichText item) {
        addItemInternal(item);
        rebuildOptions();
        if (selectedIndex < 0) {
            setSelectedIndex(0, false);
        } else {
            syncSelectionState();
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(
            title = "<key:combo.box.method.remove_item.title>",
            summary = "<key:combo.box.method.remove_item.summary>")
    public ComboBox removeItem(int index) {
        if (index < 0 || index >= items.size()) return this;
        int oldSelection = selectedIndex;
        items.remove(index);
        richItems.remove(index);
        rebuildOptions();
        if (items.isEmpty()) {
            setSelectedIndex(-1, oldSelection != -1);
        } else if (oldSelection == index) {
            setSelectedIndex(Math.min(index, items.size() - 1), true);
        } else if (index < oldSelection) {
            selectedIndex = oldSelection - 1;
            syncSelectionState();
        } else {
            syncSelectionState();
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(
            title = "<key:combo.box.method.item_count.title>",
            summary = "<key:combo.box.method.item_count.summary>")
    public int itemCount() {
        return items.size();
    }

    @WikiMethod(
            title = "<key:combo.box.method.get_selected_index.title>",
            summary = "<key:combo.box.method.get_selected_index.summary>")
    public int selectedIndex() {
        return selectedIndex;
    }

    @WikiMethod(
            title = "<key:combo.box.method.selected_item.title>",
            summary = "<key:combo.box.method.selected_item.summary>")
    public String selectedItem() {
        return selectedIndex >= 0 && selectedIndex < items.size() ? items.get(selectedIndex) : "";
    }

    public RichText selectedRichItem() {
        return selectedIndex >= 0 && selectedIndex < richItems.size() ? richItems.get(selectedIndex) : RichText.plain("");
    }

    @WikiMethod(
            title = "<key:combo.box.method.selected_index.title>",
            summary = "<key:combo.box.method.selected_index.summary>")
    public ComboBox selectedIndex(@WikiParameter(description = "<key:combo.box.param.index>") int index) {
        setSelectedIndex(index, true);
        return this;
    }

    @XmlAttribute(value = "selectedIndex", category = "Behavior", defaultValue = "-1", description = "Initial selected option index without emitting change events during XML load.")
    @WikiMethod(
            title = "<key:combo.box.method.silent_selected_index.title>",
            summary = "<key:combo.box.method.silent_selected_index.summary>")
    public ComboBox silentSelectedIndex(int index) {
        setSelectedIndex(index, false);
        return this;
    }

    @WikiMethod(
            title = "<key:combo.box.method.opened.title>",
            summary = "<key:combo.box.method.opened.summary>")
    public boolean opened() {
        return opened;
    }

    @WikiMethod(
            title = "<key:combo.box.method.open.title>",
            summary = "<key:combo.box.method.open.summary>")
    public ComboBox open() {
        return opened(true);
    }

    @WikiMethod(
            title = "<key:combo.box.method.close.title>",
            summary = "<key:combo.box.method.close.summary>")
    public ComboBox close() {
        return opened(false);
    }

    @WikiMethod(
            title = "<key:combo.box.method.toggle.title>",
            summary = "<key:combo.box.method.toggle.summary>")
    public ComboBox toggle() {
        return opened(!opened);
    }

    @XmlAttribute(value = "opened", category = "Behavior", defaultValue = "false", description = "Whether the dropdown list starts open.")
    public ComboBox opened(boolean opened) {
        if (this.opened == opened) return this;
        this.opened = opened;
        syncDropDownAttachment();
        syncDropDownVisibility();
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public DropDownMode dropDownMode() {
        return dropDownMode;
    }

    @XmlAttribute(value = "dropDownMode", category = "Behavior", defaultValue = "overlay", description = "Whether dropdown options are shown inline or in an overlay popup.")
    public ComboBox dropDownMode(DropDownMode dropDownMode) {
        DropDownMode normalized = dropDownMode == null ? DropDownMode.INLINE : dropDownMode;
        if (this.dropDownMode == normalized) return this;
        this.dropDownMode = normalized;
        syncDropDownAttachment();
        syncDropDownVisibility();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public OverlayLayer overlayLayer() {
        return explicitOverlayLayer != null ? explicitOverlayLayer : findTopmostOverlayLayer();
    }

    public ComboBox overlayLayer(OverlayLayer overlayLayer) {
        if (this.explicitOverlayLayer == overlayLayer) return this;
        detachFromOverlay();
        this.explicitOverlayLayer = overlayLayer;
        syncDropDownAttachment();
        syncDropDownVisibility();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ComboBox useOverlay() {
        return dropDownMode(DropDownMode.OVERLAY);
    }

    public ComboBox useOverlay(OverlayLayer overlayLayer) {
        overlayLayer(overlayLayer);
        return dropDownMode(DropDownMode.OVERLAY);
    }

    public Popup dropDownPopup() {
        return dropDownPopup;
    }

    public float dropDownWidth() {
        return dropDownWidth;
    }

    @XmlAttribute(value = "dropDownWidth", category = "Layout", defaultValue = "0", description = "Explicit dropdown width; 0 uses automatic sizing.")
    public ComboBox dropDownWidth(float width) {
        float normalized = Float.isFinite(width) ? Math.max(0.0f, width) : 0.0f;
        if (dropDownWidth == normalized && !dropDownMatchesWidgetWidth) return this;
        dropDownWidth = normalized;
        dropDownMatchesWidgetWidth = false;
        syncDropDownSize();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean dropDownMatchesWidgetWidth() {
        return dropDownMatchesWidgetWidth;
    }

    @XmlAttribute(value = "dropDownMatchesWidgetWidth", category = "Layout", defaultValue = "false", description = "Whether dropdown width follows the combo box width.")
    public ComboBox dropDownMatchesWidgetWidth(boolean match) {
        if (dropDownMatchesWidgetWidth == match && (match || dropDownWidth <= 0.0f)) return this;
        dropDownMatchesWidgetWidth = match;
        if (match) {
            dropDownWidth = 0.0f;
        }
        syncDropDownSize();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ComboBox dropDownSameWidth() {
        return dropDownMatchesWidgetWidth(true);
    }

    public ComboBox autoDropDownWidth() {
        boolean changed = dropDownWidth > 0.0f || dropDownMatchesWidgetWidth;
        dropDownWidth = 0.0f;
        dropDownMatchesWidgetWidth = false;
        if (changed) {
            syncDropDownSize();
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }
        return this;
    }

    public float optionRowHeight() {
        return optionRowHeight;
    }

    @XmlAttribute(value = "optionRowHeight", category = "Layout", defaultValue = "28", description = "Height of each option row in UI pixels.")
    public ComboBox optionRowHeight(float height) {
        float normalized = Float.isFinite(height) && height > 0.0f ? height : OPTION_HEIGHT;
        if (optionRowHeight == normalized) return this;
        optionRowHeight = normalized;
        optionsScroll.scrollStep(optionRowHeight);
        for (ToggleButton option : optionButtons) {
            option.layout(style -> style.size(LayoutConstraints.AUTO, optionRowHeight).flexGrow(0).flexShrink(0.0f));
        }
        syncDropDownSize();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public int maxVisibleOptions() {
        return maxVisibleOptions;
    }

    @XmlAttribute(value = "maxVisibleOptions", category = "Layout", defaultValue = "6", description = "Maximum visible options before dropdown scrolling is used; 0 disables the cap.")
    public ComboBox maxVisibleOptions(int count) {
        int normalized = Math.max(0, count);
        if (maxVisibleOptions == normalized) return this;
        maxVisibleOptions = normalized;
        syncDropDownSize();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public OverlayLayer attachedOverlayLayer() {
        return attachedOverlayLayer;
    }

    public String placeholder() {
        return placeholder;
    }

    @XmlAttribute(value = "placeholder", category = "Content", defaultValue = "Select...", description = "Header text shown when no item is selected.")
    public ComboBox placeholder(String placeholder) {
        String normalized = normalize(placeholder);
        if (Objects.equals(this.placeholder, normalized)) return this;
        this.placeholder = normalized;
        this.richPlaceholder = RichText.resolve(normalized);
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public RichText richPlaceholder() {
        return richPlaceholder;
    }

    public ComboBox richPlaceholder(RichText placeholder) {
        RichText normalized = placeholder == null ? RichText.plain("") : placeholder;
        if (Objects.equals(this.richPlaceholder, normalized)) return this;
        this.richPlaceholder = normalized;
        this.placeholder = normalized.plainText();
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Button headerButton() {
        return headerButton;
    }

    public RichText headerIndicator() {
        return headerIndicator;
    }

    public ComboBox headerIndicator(String indicator) {
        return headerIndicator(RichText.resolve(normalize(indicator)));
    }

    public ComboBox headerIndicator(RichText indicator) {
        RichText normalized = indicator == null ? RichText.plain("") : indicator;
        if (Objects.equals(this.headerIndicator, normalized)) return this;
        this.headerIndicator = normalized;
        this.customHeaderIndicator = true;
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Box optionsHost() {
        return optionsHost;
    }

    public ScrollView optionsScroll() {
        return optionsScroll;
    }

    public ToggleButton optionButton(int index) {
        return optionButtons.get(index);
    }

    public EventSubscription onSelectionChanged(EventListener<? super SelectionChangedEvent> listener) {
        return on(SelectionChangedEvent.TYPE, listener);
    }

    @Override
    public void setParentInternal(Widget parent) {
        super.setParentInternal(parent);
        syncDropDownAttachment();
        syncDropDownVisibility();
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        syncDropDownAttachment();
        syncDropDownVisibility();
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof KeyPressedEvent key
                && key.phase() == EventPhase.TARGET
                && uiContext() != null
                && uiContext().focusManager().isFocused(this)) {
            if (key.keyCode() == KeyCodes.SPACE || key.keyCode() == KeyCodes.ENTER || key.keyCode() == KeyCodes.KEYPAD_ENTER) {
                toggle();
                event.cancel();
            } else if (key.keyCode() == KeyCodes.ESCAPE && opened) {
                close();
                event.cancel();
            } else if (key.keyCode() == KeyCodes.UP) {
                selectRelative(-1);
                event.cancel();
            } else if (key.keyCode() == KeyCodes.DOWN) {
                selectRelative(1);
                event.cancel();
            }
        }
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        syncDropDownSize();
    }

    private void selectRelative(int delta) {
        if (items.isEmpty()) return;
        int base = selectedIndex < 0 ? 0 : selectedIndex;
        setSelectedIndex(Math.max(0, Math.min(items.size() - 1, base + delta)), true);
    }

    private void setSelectedIndex(int index, boolean emitChange) {
        int normalized = items.isEmpty() ? -1 : Math.max(0, Math.min(index, items.size() - 1));
        if (selectedIndex == normalized) {
            syncSelectionState();
            return;
        }
        int oldSelection = selectedIndex;
        selectedIndex = normalized;
        syncSelectionState();
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new SelectionChangedEvent(this, selectionList(oldSelection), selectionList(selectedIndex)));
        }
    }

    private void rebuildOptions() {
        optionsList.clearChildren();
        optionButtons.clear();
        for (int index = 0; index < items.size(); index++) {
            final int itemIndex = index;
            ToggleButton option = new ComboBoxOptionButton(richItems.get(index));
            // Отступы сохраняют вид строки-пилюли, а состояние её поверхности
            // задаётся компонентным или внешним стилем.
            option.textAlignment(Alignment.START);
            option.textPaddingX(12.0f);
            option.hoverTexture(optionHoverTexture);
            option.hoverShader(optionHoverShader)
                    .hoverShaderUniforms(optionHoverShaderUniforms)
                    .hoverShaderOptions(optionHoverShaderOptions);
            option.layout(style -> style.size(LayoutConstraints.AUTO, optionRowHeight)
                    .margin(OPTION_SIDE_MARGIN, 0.0f)
                    .flexGrow(0).flexShrink(0.0f));
            option.onClick(event -> {
                selectedIndex(itemIndex);
                close();
            });
            optionButtons.add(option);
            optionsList.addChild(option);
        }
        optionsScroll.scrollTo(0.0f, 0.0f);
        syncDropDownSize();
    }

    /**
     * Задаёт uniforms шейдера подсветки всех строк.
     *
     * @param uniforms набор uniforms или {@code null} для пустого набора
     * @return этот ComboBox для fluent-настройки
     */
    public ComboBox optionHoverShaderUniforms(ShaderUniforms uniforms) {
        optionHoverShaderUniforms = uniforms == null ? ShaderUniforms.empty() : uniforms;
        syncOptionHoverAppearance();
        return this;
    }

    /**
     * Задаёт параметры отрисовки шейдера подсветки всех строк.
     *
     * @param options параметры отрисовки или {@code null} для значений по умолчанию
     * @return этот ComboBox для fluent-настройки
     */
    public ComboBox optionHoverShaderOptions(ShaderDrawOptions options) {
        optionHoverShaderOptions = options == null ? ShaderDrawOptions.defaults() : options;
        syncOptionHoverAppearance();
        return this;
    }

    private void syncOptionHoverAppearance() {
        for (ToggleButton option : optionButtons) {
            if (customOptionHoverBackground) {
                option.style(style -> style.put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED,
                        MutableColor.rgba(0.0f, 0.0f, 0.0f, 0.0f)));
            }
            option.hoverBackground(customOptionHoverBackground ? optionHoverBackground : null)
                    .hoverTexture(optionHoverTexture)
                    .hoverShader(optionHoverShader)
                    .hoverShaderUniforms(optionHoverShaderUniforms)
                    .hoverShaderOptions(optionHoverShaderOptions);
        }
    }

    private void syncSelectionState() {
        for (int i = 0; i < optionButtons.size(); i++) {
            ToggleButton option = optionButtons.get(i);
            option.silentChecked(i == selectedIndex);
        }
    }

    private static final class ComboBoxOptionButton extends ToggleButton {
        private ComboBoxOptionButton(RichText text) {
            super(text);
            applyComponentStyleDefaults(WidgetStyles.Selection.DROPDOWN_OPTION);
        }

        @Override
        protected String styleType() {
            return ToggleButton.STYLE_TYPE;
        }

        @Override
        protected dev.sixik.unigui.api.style.Style defaultStyle() {
            return WidgetStyles.Selection.DROPDOWN_OPTION;
        }
    }

    private static final class ComboBoxOptionsHost extends Box {
        private ComboBoxOptionsHost() {
            applyComponentStyleDefaults(WidgetStyles.Selection.DROPDOWN_HOST);
        }

        @Override
        protected String styleType() {
            return Box.STYLE_TYPE;
        }

        @Override
        protected dev.sixik.unigui.api.style.Style defaultStyle() {
            return WidgetStyles.Selection.DROPDOWN_HOST;
        }
    }

    private void updateHeaderText() {
        RichText value = selectedItem().isEmpty() ? richPlaceholder : selectedRichItem();
        if (customHeaderIndicator) {
            // Legacy inline-индикатор: как раньше текстом, слот-шеврон прячем,
            // чтобы глиф не дублировался.
            headerButton.chevronVisible(false);
            headerButton.richText(headerIndicator.plainText().isEmpty() ? value : value.append(headerIndicator));
            return;
        }
        headerIndicator = opened ? defaultHeaderIndicatorOpened : defaultHeaderIndicatorClosed;
        headerButton.chevronVisible(true);
        headerButton.richText(value);
        headerButton.chevronUp(opened);
    }

    private void addItemInternal(RichText item) {
        RichText normalized = item == null ? RichText.plain("") : item;
        richItems.add(normalized);
        items.add(normalized.plainText());
    }

    private void syncDropDownAttachment() {
        syncDropDownSize();
        if (dropDownMode == DropDownMode.INLINE) {
            detachFromOverlay();
            clearPopupContent();
            if (optionsHost.parent() != this) {
                super.addChild(optionsHost);
            }
            return;
        }

        detachOptionsFromInlineLayout();

        OverlayLayer targetOverlay = overlayLayer();
        if (targetOverlay != null) {
            if (dropDownPopup.content() != optionsHost) {
                dropDownPopup.content(optionsHost);
            }
            if (attachedOverlayLayer != targetOverlay) {
                detachFromOverlay();
                attachedOverlayLayer = targetOverlay;
                attachedOverlayLayer.addOverlay(dropDownPopup);
            }
        } else {
            detachFromOverlay();
            clearPopupContent();
        }
    }

    private void syncDropDownVisibility() {
        syncDropDownSize();
        if (dropDownMode == DropDownMode.OVERLAY) {
            optionsHost.visibility(attachedOverlayLayer != null ? Visibility.VISIBLE : Visibility.COLLAPSED);
            syncingPopup = true;
            try {
                dropDownPopup.open(opened && attachedOverlayLayer != null);
            } finally {
                syncingPopup = false;
            }
        } else {
            syncingPopup = true;
            try {
                dropDownPopup.close();
            } finally {
                syncingPopup = false;
            }
            optionsHost.visibility(opened ? Visibility.VISIBLE : Visibility.COLLAPSED);
        }
    }

    private void detachOptionsFromInlineLayout() {
        if (optionsHost.parent() == this) {
            super.removeChild(optionsHost);
            applyQueuedMutations();
        }
    }

    private void clearPopupContent() {
        syncingPopup = true;
        try {
            dropDownPopup.close();
            if (dropDownPopup.content() != null) {
                dropDownPopup.content(null);
                dropDownPopup.applyQueuedMutations();
            }
        } finally {
            syncingPopup = false;
        }
    }

    private void detachFromOverlay() {
        if (attachedOverlayLayer != null) {
            attachedOverlayLayer.removeOverlay(dropDownPopup);
            attachedOverlayLayer = null;
        }
    }

    private void syncDropDownSize() {
        float resolvedWidth = resolvedDropDownWidth();
        float resolvedContentHeight = resolvedDropDownContentHeight();
        float width = resolvedWidth > 0.0f ? resolvedWidth : LayoutConstraints.AUTO;
        float height = resolvedContentHeight > 0.0f ? resolvedContentHeight + OPTIONS_INSET * 2.0f : LayoutConstraints.AUTO;
        float scrollWidth = resolvedWidth > OPTIONS_INSET * 2.0f ? resolvedWidth - OPTIONS_INSET * 2.0f : LayoutConstraints.AUTO;
        float scrollHeight = resolvedContentHeight > 0.0f ? resolvedContentHeight : LayoutConstraints.AUTO;
        Overflow verticalOverflow = maxVisibleOptions <= 0
                ? Overflow.AUTO
                : shouldScrollDropDown() ? Overflow.SCROLL : Overflow.HIDDEN;

        optionsHost.layout(style -> style.size(width, height).flexGrow(0).flexShrink(0.0f));
        optionsScroll.layout(style -> style
                .size(scrollWidth, scrollHeight)
                .margin(OPTIONS_INSET)
                .overflowX(Overflow.HIDDEN)
                .overflowY(verticalOverflow)
                .flexGrow(0)
                .flexShrink(0.0f));
    }

    private float resolvedDropDownContentHeight() {
        if (maxVisibleOptions <= 0 || items.isEmpty()) {
            return 0.0f;
        }
        int visibleOptions = Math.min(items.size(), maxVisibleOptions);
        return optionRowHeight * visibleOptions
                + optionsList.spacing() * Math.max(0, visibleOptions - 1);
    }

    private boolean shouldScrollDropDown() {
        return maxVisibleOptions > 0 && items.size() > maxVisibleOptions;
    }

    private float resolvedDropDownWidth() {
        if (dropDownWidth > 0.0f) {
            return dropDownWidth;
        }
        if (dropDownMatchesWidgetWidth) {
            float width = layoutBounds().width();
            if (width <= 0.0f && desiredSize().width() > 0.0f) {
                width = desiredSize().width();
            }
            if (width <= 0.0f && headerButton.layoutBounds().width() > 0.0f) {
                width = headerButton.layoutBounds().width();
            }
            if (width <= 0.0f && headerButton.desiredSize().width() > 0.0f) {
                width = headerButton.desiredSize().width();
            }
            return Math.max(0.0f, width);
        }
        return 0.0f;
    }

    private OverlayLayer findTopmostOverlayLayer() {
        OverlayLayer result = null;
        Widget current = this;
        while (current != null) {
            if (current instanceof OverlayLayer layer) {
                result = layer;
            }
            current = current.parent();
        }
        return result;
    }

    private static List<Integer> selectionList(int index) {
        return index < 0 ? List.of() : List.of(index);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }

    public enum DropDownMode {
        INLINE,
        OVERLAY
    }
}
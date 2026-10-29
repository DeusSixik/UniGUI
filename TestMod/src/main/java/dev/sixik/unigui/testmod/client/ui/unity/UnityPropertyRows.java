package dev.sixik.unigui.testmod.client.ui.unity;

import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.CheckboxState;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.NumberField;
import dev.sixik.unigui.widgets.interaction.Slider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * Фабрика стандартных строк свойств инспектора Unity (Vector3, Slider, ComboBox, Checkbox, Color, HelpBox).
 */
public final class UnityPropertyRows {
    private UnityPropertyRows() {
    }

    /**
     * Строка Vector3 (Position, Rotation, Scale):
     * Label слева, поля X, Y, Z с цветными метками справа.
     */
    public static HBox vector3Row(String labelText, double xVal, double yVal, double zVal,
                                  DoubleConsumer onX, DoubleConsumer onY, DoubleConsumer onZ) {
        HBox row = new HBox();
        row.spacing(6.0f);
        row.layout(style -> style.widthPercent(100.0f).height(UnityStyles.ROW_HEIGHT)
                .align(Alignment.START, Alignment.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        row.addChild(UnityStyles.label(labelText));

        HBox fields = new HBox();
        fields.spacing(4.0f);
        fields.layout(style -> style.heightPercent(100.0f).flexGrow(1.0f));

        // X
        HBox xBox = new HBox();
        xBox.spacing(2.0f);
        xBox.layout(style -> style.heightPercent(100.0f).flexGrow(1.0f).align(Alignment.START, Alignment.CENTER));
        Label xLabel = new Label("X");
        xLabel.color(UnityStyles.AXIS_X);
        xLabel.layout(style -> style.width(10.0f).height(UnityStyles.ROW_HEIGHT));
        NumberField xField = new NumberField();
        UnityStyles.styleNumberField(xField);
        xField.value(xVal);
        xField.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));
        if (onX != null) xField.onValueChanged(e -> onX.accept(e.newValue()));
        xBox.addChild(xLabel);
        xBox.addChild(xField);

        // Y
        HBox yBox = new HBox();
        yBox.spacing(2.0f);
        yBox.layout(style -> style.heightPercent(100.0f).flexGrow(1.0f).align(Alignment.START, Alignment.CENTER));
        Label yLabel = new Label("Y");
        yLabel.color(UnityStyles.AXIS_Y);
        yLabel.layout(style -> style.width(10.0f).height(UnityStyles.ROW_HEIGHT));
        NumberField yField = new NumberField();
        UnityStyles.styleNumberField(yField);
        yField.value(yVal);
        yField.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));
        if (onY != null) yField.onValueChanged(e -> onY.accept(e.newValue()));
        yBox.addChild(yLabel);
        yBox.addChild(yField);

        // Z
        HBox zBox = new HBox();
        zBox.spacing(2.0f);
        zBox.layout(style -> style.heightPercent(100.0f).flexGrow(1.0f).align(Alignment.START, Alignment.CENTER));
        Label zLabel = new Label("Z");
        zLabel.color(UnityStyles.AXIS_Z);
        zLabel.layout(style -> style.width(10.0f).height(UnityStyles.ROW_HEIGHT));
        NumberField zField = new NumberField();
        UnityStyles.styleNumberField(zField);
        zField.value(zVal);
        zField.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));
        if (onZ != null) zField.onValueChanged(e -> onZ.accept(e.newValue()));
        zBox.addChild(zLabel);
        zBox.addChild(zField);

        fields.addChild(xBox);
        fields.addChild(yBox);
        fields.addChild(zBox);

        row.addChild(fields);
        return row;
    }

    /**
     * Слайдер с числовым полем ввода (как Field of View в Camera):
     * Перемещение ползунка синхронизируется с числовым полем и наоборот.
     */
    public static HBox sliderWithFieldRow(String labelText, float min, float max, float initialVal,
                                          Consumer<Float> onValueChanged) {
        HBox row = new HBox();
        row.spacing(6.0f);
        row.layout(style -> style.widthPercent(100.0f).height(UnityStyles.ROW_HEIGHT)
                .align(Alignment.START, Alignment.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        row.addChild(UnityStyles.label(labelText));

        HBox controls = new HBox();
        controls.spacing(6.0f);
        controls.layout(style -> style.heightPercent(100.0f).flexGrow(1.0f).align(Alignment.START, Alignment.CENTER));

        Slider slider = new Slider();
        slider.themeEnabled(false);
        slider.min(min);
        slider.max(max);
        slider.value(initialVal);
        slider.trackColor().set(UnityStyles.BORDER_SUBTLE);
        slider.fillColor().set(UnityStyles.BORDER_DEFAULT);
        slider.knobColor().set(UnityStyles.TEXT_MUTED);
        slider.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));

        NumberField field = new NumberField();
        UnityStyles.styleNumberField(field);
        field.range(min, max);
        field.value(initialVal);
        field.layout(style -> style.width(42.0f).height(UnityStyles.ROW_HEIGHT).flexGrow(0.0f).flexShrink(0.0f));

        boolean[] syncing = {false};
        slider.onValueChanged(event -> {
            if (syncing[0]) return;
            syncing[0] = true;
            field.value(Math.round(event.newValue()));
            syncing[0] = false;
            if (onValueChanged != null) onValueChanged.accept(event.newValue());
        });

        field.onValueChanged(event -> {
            if (syncing[0]) return;
            syncing[0] = true;
            slider.value((float) event.newValue());
            syncing[0] = false;
            if (onValueChanged != null) onValueChanged.accept((float) event.newValue());
        });

        controls.addChild(slider);
        controls.addChild(field);
        row.addChild(controls);
        return row;
    }

    /**
     * Строка выпадающего списка (ComboBox).
     */
    public static HBox dropdownRow(String labelText, List<String> items, int defaultIndex,
                                   Consumer<String> onSelected) {
        HBox row = new HBox();
        row.spacing(6.0f);
        row.layout(style -> style.widthPercent(100.0f).height(UnityStyles.ROW_HEIGHT)
                .align(Alignment.START, Alignment.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        row.addChild(UnityStyles.label(labelText));

        ComboBox combo = new ComboBox();
        combo.items(items);
        combo.selectedIndex(defaultIndex);
        UnityStyles.styleComboBox(combo);
        combo.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));

        if (onSelected != null) {
            combo.onSelectionChanged(event -> onSelected.accept(combo.selectedItem()));
        }

        row.addChild(combo);
        return row;
    }

    /**
     * Строка одиночного чекбокса в стиле Unity с центрированием и тонкой белой галочкой.
     */
    public static HBox checkboxRow(String labelText, boolean checked, Consumer<Boolean> onStateChanged) {
        HBox row = new HBox();
        row.spacing(6.0f);
        row.layout(style -> style.widthPercent(100.0f).height(UnityStyles.ROW_HEIGHT)
                .alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        row.addChild(UnityStyles.label(labelText));

        Checkbox checkbox = new Checkbox();
        UnityStyles.styleCheckbox(checkbox);
        checkbox.checked(checked);
        checkbox.layout(style -> style.size(14.0f, UnityStyles.ROW_HEIGHT).alignSelf(Align.CENTER));

        if (onStateChanged != null) {
            checkbox.onStateChanged(event -> onStateChanged.accept(event.newState() == CheckboxState.CHECKED));
        }

        row.addChild(checkbox);
        return row;
    }

    /**
     * Строка поля цвета (Background с плашкой цвета и пипеткой).
     */
    public static HBox colorFieldRow(String labelText, MutableColor initialColor, OverlayLayer overlayLayer) {
        HBox row = new HBox();
        row.spacing(6.0f);
        row.layout(style -> style.widthPercent(100.0f).height(UnityStyles.ROW_HEIGHT)
                .align(Alignment.START, Alignment.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        row.addChild(UnityStyles.label(labelText));

        HBox colorHost = new HBox();
        colorHost.spacing(2.0f);
        colorHost.layout(style -> style.heightPercent(100.0f).flexGrow(1.0f).align(Alignment.START, Alignment.CENTER));

        // Плашка с цветом
        Box swatch = new Box();
        swatch.themeEnabled(false);
        swatch.backgroundVisible(true);
        swatch.background().set(initialColor);
        swatch.borderVisible(true);
        swatch.borderColor().set(UnityStyles.BORDER_DEFAULT);
        swatch.borderWidth(1.0f);
        swatch.radius(2.0f);
        swatch.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));

        // Кнопка пипетки
        Button pickerBtn = new Button();
        pickerBtn.themeEnabled(false);
        pickerBtn.backgroundVisible(true);
        pickerBtn.background().set(UnityStyles.BG_INPUT);
        pickerBtn.borderVisible(true);
        pickerBtn.borderColor().set(UnityStyles.BORDER_INPUT);
        pickerBtn.radius(2.0f);
        pickerBtn.layout(style -> style.size(18.0f, UnityStyles.ROW_HEIGHT).flexGrow(0.0f).flexShrink(0.0f));
        pickerBtn.addChild(UnityIcons.eyedropperIcon(14.0f));

        // При клике циклически меняем популярные цвета Unity Camera (Белый -> Синий -> Тёмный -> Серый)
        MutableColor[] presets = new MutableColor[]{
                MutableColor.rgba255(255, 255, 255, 255), // White (как на скриншоте)
                MutableColor.rgba255(49, 77, 121, 255),   // Unity Default Skybox Blue
                MutableColor.rgba255(25, 25, 25, 255),     // Solid Dark
                MutableColor.rgba255(115, 115, 115, 255)   // Medium Gray
        };
        int[] colorIndex = {0};

        swatch.on(PointerPressedEvent.TYPE, event -> {
            if (event.button() == PointerButton.PRIMARY) {
                colorIndex[0] = (colorIndex[0] + 1) % presets.length;
                initialColor.set(presets[colorIndex[0]]);
                swatch.background().set(presets[colorIndex[0]]);
                event.cancel();
            }
        });

        pickerBtn.onClick(event -> {
            colorIndex[0] = (colorIndex[0] + 1) % presets.length;
            initialColor.set(presets[colorIndex[0]]);
            swatch.background().set(presets[colorIndex[0]]);
        });

        colorHost.addChild(swatch);
        colorHost.addChild(pickerBtn);
        row.addChild(colorHost);
        return row;
    }

    /**
     * Строка с двумя числами (например Clipping Planes: Near / Far).
     */
    public static HBox twoFieldsRow(String labelText, String sub1, double val1, String sub2, double val2,
                                    DoubleConsumer onVal1, DoubleConsumer onVal2) {
        HBox row = new HBox();
        row.spacing(6.0f);
        row.layout(style -> style.widthPercent(100.0f).height(UnityStyles.ROW_HEIGHT)
                .align(Alignment.START, Alignment.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        row.addChild(UnityStyles.label(labelText));

        HBox fields = new HBox();
        fields.spacing(4.0f);
        fields.layout(style -> style.heightPercent(100.0f).flexGrow(1.0f).align(Alignment.START, Alignment.CENTER));

        // Sub 1
        Label l1 = new Label(sub1);
        l1.color(UnityStyles.TEXT_MUTED);
        l1.layout(style -> style.width(26.0f).height(UnityStyles.ROW_HEIGHT));
        NumberField f1 = new NumberField();
        UnityStyles.styleNumberField(f1);
        f1.value(val1);
        f1.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));
        if (onVal1 != null) f1.onValueChanged(e -> onVal1.accept(e.newValue()));

        // Sub 2
        Label l2 = new Label(sub2);
        l2.color(UnityStyles.TEXT_MUTED);
        l2.layout(style -> style.width(24.0f).height(UnityStyles.ROW_HEIGHT));
        NumberField f2 = new NumberField();
        UnityStyles.styleNumberField(f2);
        f2.value(val2);
        f2.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));
        if (onVal2 != null) f2.onValueChanged(e -> onVal2.accept(e.newValue()));

        fields.addChild(l1);
        fields.addChild(f1);
        fields.addChild(l2);
        fields.addChild(f2);

        row.addChild(fields);
        return row;
    }

    /**
     * Строка Viewport Rect: 2 строки (X, Y и W, H).
     */
    public static VBox viewportRectBlock(String labelText, double x, double y, double w, double h) {
        VBox block = new VBox();
        block.spacing(2.0f);
        block.layout(style -> style.widthPercent(100.0f).flexGrow(0.0f).flexShrink(0.0f));

        HBox row1 = twoFieldsRow(labelText, "X", x, "Y", y, null, null);
        HBox row2 = twoFieldsRow("", "W", w, "H", h, null, null);

        block.addChild(row1);
        block.addChild(row2);
        return block;
    }

    /**
     * Строка одного числового поля (например Depth).
     */
    public static HBox singleNumberRow(String labelText, double val, DoubleConsumer onVal) {
        HBox row = new HBox();
        row.spacing(6.0f);
        row.layout(style -> style.widthPercent(100.0f).height(UnityStyles.ROW_HEIGHT)
                .align(Alignment.START, Alignment.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        row.addChild(UnityStyles.label(labelText));

        NumberField field = new NumberField();
        UnityStyles.styleNumberField(field);
        field.value(val);
        field.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));
        if (onVal != null) field.onValueChanged(e -> onVal.accept(e.newValue()));

        row.addChild(field);
        return row;
    }

    /**
     * Строка выбора объекта (Target Texture: None (Render Texture) + ⊙).
     */
    public static HBox objectPickerRow(String labelText, String objectName, String typeName) {
        HBox row = new HBox();
        row.spacing(6.0f);
        row.layout(style -> style.widthPercent(100.0f).height(UnityStyles.ROW_HEIGHT)
                .align(Alignment.START, Alignment.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        row.addChild(UnityStyles.label(labelText));

        HBox objHost = new HBox();
        objHost.spacing(2.0f);
        objHost.layout(style -> style.heightPercent(100.0f).flexGrow(1.0f).align(Alignment.START, Alignment.CENTER));

        Box nameBox = new Box();
        nameBox.themeEnabled(false);
        nameBox.backgroundVisible(true);
        nameBox.background().set(UnityStyles.BG_INPUT);
        nameBox.borderVisible(true);
        nameBox.borderColor().set(UnityStyles.BORDER_INPUT);
        nameBox.radius(2.0f);
        nameBox.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f).padding(4.0f, 0.0f));

        Label textLbl = new Label(objectName + " (" + typeName + ")");
        textLbl.color(UnityStyles.TEXT_MUTED);
        textLbl.layout(style -> style.height(UnityStyles.ROW_HEIGHT).align(Alignment.START, Alignment.CENTER));
        nameBox.addChild(textLbl);

        Button targetBtn = new Button();
        targetBtn.themeEnabled(false);
        targetBtn.boxVisualEnabled(false);
        targetBtn.focusRingVisible(false);
        targetBtn.focusable(false);
        targetBtn.renderer(UnityStyles.TARGET_PICKER_RENDERER);
        targetBtn.layout(style -> style.size(18.0f, UnityStyles.ROW_HEIGHT).flexGrow(0.0f).flexShrink(0.0f));

        objHost.addChild(nameBox);
        objHost.addChild(targetBtn);
        row.addChild(objHost);
        return row;
    }

    /**
     * Блок предупреждения HelpBox (как предупреждение о Deferred + MSAA на скриншоте).
     * Контейнер HBox с корректными отступами со всех сторон (включая низ).
     */
    public static HBox helpBox(String message) {
        HBox box = new HBox();
        box.themeEnabled(false);
        box.backgroundVisible(true);
        box.background().set(UnityStyles.BG_HELP_BOX);
        box.borderVisible(true);
        box.borderColor().set(UnityStyles.BORDER_INPUT);
        box.radius(3.0f);
        box.spacing(6.0f);
        box.layout(style -> style.widthPercent(100.0f)
                .padding(8.0f, 7.0f, 8.0f, 11.0f)
                .margin(0.0f, 3.0f)
                .alignItems(Align.START)
                .flexGrow(0.0f).flexShrink(0.0f));

        CanvasWidget icon = UnityIcons.warningIcon(18.0f);
        icon.layout(style -> style.size(18.0f, 18.0f).margin(1.0f, 0.0f, 0.0f, 0.0f).flexGrow(0.0f).flexShrink(0.0f));
        box.addChild(icon);

        Label label = new Label(message);
        label.color(UnityStyles.TEXT_HELP_BOX);
        label.wrap(true);
        label.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        box.addChild(label);

        return box;
    }

    /**
     * Секция Command Buffers со списком и кнопкой "Remove all".
     */
    public static VBox commandBuffersBlock() {
        VBox block = new VBox();
        block.spacing(2.0f);
        block.layout(style -> style.widthPercent(100.0f).flexGrow(0.0f).flexShrink(0.0f));

        List<String> buffers = new ArrayList<>(List.of(
                "BeforeLighting: Deferred Ambient Occlusion (0 B)",
                "BeforeImageEffectsOpaque: Opaque Only Post-processing (0 B)",
                "BeforeImageEffects: Post-processing (0 B)",
                "BeforeReflections: Deferred Ambient Occlusion (0 B)"
        ));

        HBox foldoutHeader = new HBox();
        foldoutHeader.spacing(4.0f);
        foldoutHeader.layout(style -> style.widthPercent(100.0f).height(UnityStyles.ROW_HEIGHT)
                .align(Alignment.START, Alignment.CENTER));

        boolean[] open = {true};
        CanvasWidget arrow = UnityIcons.foldoutArrow(10.0f, open[0]);
        Label title = new Label(buffers.size() + " command buffers");
        title.color(UnityStyles.TEXT_MUTED);
        title.layout(style -> style.height(UnityStyles.ROW_HEIGHT));

        foldoutHeader.addChild(arrow);
        foldoutHeader.addChild(title);

        VBox listContainer = new VBox();
        listContainer.spacing(2.0f);
        listContainer.layout(style -> style.widthPercent(100.0f).padding(14.0f, 0.0f, 0.0f, 0.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        Runnable rebuildList = () -> {
            listContainer.clearChildren();
            title.text(buffers.size() + " command buffers");
            for (int i = 0; i < buffers.size(); i++) {
                String name = buffers.get(i);
                int index = i;
                HBox itemRow = new HBox();
                itemRow.spacing(4.0f);
                itemRow.layout(style -> style.widthPercent(100.0f).height(16.0f)
                        .align(Alignment.START, Alignment.CENTER));

                Label itemLbl = new Label(name);
                itemLbl.color(UnityStyles.TEXT_MUTED);
                itemLbl.layout(style -> style.flexGrow(1.0f).height(16.0f));

                Button minusBtn = new Button("-");
                minusBtn.themeEnabled(false);
                minusBtn.boxVisualEnabled(false);
                minusBtn.focusRingVisible(false);
                minusBtn.focusable(false);
                minusBtn.textColor().set(UnityStyles.TEXT_MUTED);
                minusBtn.layout(style -> style.size(16.0f, 14.0f));
                minusBtn.onClick(e -> {
                    buffers.remove(index);
                    listContainer.clearChildren();
                    title.text(buffers.size() + " command buffers");
                });

                itemRow.addChild(itemLbl);
                itemRow.addChild(minusBtn);
                listContainer.addChild(itemRow);
            }

            if (!buffers.isEmpty()) {
                HBox removeRow = new HBox();
                removeRow.layout(style -> style.widthPercent(100.0f).height(20.0f)
                        .align(Alignment.END, Alignment.CENTER));

                Button removeAll = new Button("Remove all");
                UnityStyles.styleButton(removeAll);
                removeAll.layout(style -> style.size(72.0f, 18.0f));
                removeAll.onClick(e -> {
                    buffers.clear();
                    listContainer.clearChildren();
                    title.text("0 command buffers");
                });
                removeRow.addChild(removeAll);
                listContainer.addChild(removeRow);
            }
        };

        rebuildList.run();

        foldoutHeader.on(PointerPressedEvent.TYPE, event -> {
            if (event.button() == PointerButton.PRIMARY) {
                open[0] = !open[0];
                listContainer.visibility(open[0] ? Visibility.VISIBLE : Visibility.COLLAPSED);
                event.cancel();
            }
        });

        block.addChild(foldoutHeader);
        block.addChild(listContainer);
        return block;
    }
}

package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.text.InlineContentAlignment;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.CheckboxState;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.*;

import java.util.List;

public final class BasicInputCategory extends DemoCategory {
    private static final List<Control> CONTROLS = List.of(
            new Control("button", "Button", "Run an action with a clear, keyboard-accessible command."),
            new Control("combo-box", "ComboBox", "Choose one option from a dropdown list."),
            new Control("toggle", "ToggleButton", "Switch a persistent option on or off."),
            new Control("checkbox", "CheckBox", "Represent an independent yes/no setting."),
            new Control("radio", "RadioButton", "Choose exactly one option from a related group."),
            new Control("slider", "Slider", "Adjust a numeric value across a bounded range."));

    @Override
    String title() {
        return "Basic Input";
    }

    @Override
    boolean expandedByDefault() {
        return true;
    }

    @Override
    List<Control> controls() {
        return CONTROLS;
    }

    @Override
    void addDemo(String controlId, VBox root) {
        switch (controlId) {
            case "button" -> addButton(root);
            case "combo-box" -> addComboBox(root);
            case "toggle" -> addToggle(root);
            case "checkbox" -> addCheckbox(root);
            case "radio" -> addRadio(root);
            case "slider" -> addSlider(root);
            default -> addPreview(root, controlId);
        }
    }

    private void addComboBox(VBox root) {
        Label themeStatus = label("Theme: Dark", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        ComboBox theme = new ComboBox()
                .items(List.of("Dark", "Light", "High Contrast", "Minecraft"))
                .silentSelectedIndex(0)
                .dropDownSameWidth();
        theme.layout(style -> style.size(180.0f, LayoutConstraints.AUTO).flexGrow(0.0f).flexShrink(0.0f));
        theme.onSelectionChanged(event -> themeStatus.text("Theme: " + theme.selectedItem()));

        VBox themeSample = new VBox();
        themeSample.spacing(12.0f);
        themeSample.layout(style -> style.widthPercent(100.0f));
        themeSample.addChild(theme);
        themeSample.addChild(themeStatus);
        root.addChild(samplePanel("Single selection",
                "Selecting an option updates the value and closes the dropdown.", themeSample,
                "ComboBox theme = new ComboBox()\n        .items(List.of(\"Dark\", \"Light\", \"High Contrast\", \"Minecraft\"))\n        .silentSelectedIndex(0)\n        .dropDownSameWidth();\ntheme.onSelectionChanged(event -> applyTheme(theme.selectedItem()));"));

        Label resolutionStatus = label("Resolution: 1920x1080", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        ComboBox resolution = new ComboBox()
                .items(List.of("1280x720", "1600x900", "1920x1080", "2560x1440", "3840x2160",
                        "7680x4320", "800x600", "1024x768", "1366x768", "1536x864"))
                .silentSelectedIndex(2)
                .maxVisibleOptions(5)
                .dropDownSameWidth();
        resolution.layout(style -> style.size(180.0f, LayoutConstraints.AUTO).flexGrow(0.0f).flexShrink(0.0f));
        resolution.onSelectionChanged(event -> resolutionStatus.text("Resolution: " + resolution.selectedItem()));

        VBox resolutionSample = new VBox();
        resolutionSample.spacing(12.0f);
        resolutionSample.layout(style -> style.widthPercent(100.0f));
        resolutionSample.addChild(resolution);
        resolutionSample.addChild(resolutionStatus);
        root.addChild(samplePanel("Overlay with a long list",
                "The popup floats above the layout, keeps the control row in place, and scrolls through the available options.",
                resolutionSample,
                "ComboBox resolution = new ComboBox()\n        .items(resolutions)\n        .silentSelectedIndex(2)\n        .maxVisibleOptions(5)\n        .dropDownSameWidth();"));
    }

    private void addButton(VBox root) {
        Label result = label("No action yet", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        Button button = new Button("Run action");
        styleInteractiveButton(button, gallery.accent);
        button.layout(style -> style.width(150.0f).height(38.0f));
        button.onClick(event -> result.text("Action triggered"));
        HBox row = new HBox();
        row.spacing(18.0f);
        row.layout(style -> style.widthPercent(100.0f).alignItems(Align.CENTER));
        row.addChild(button);
        row.addChild(result);
        root.addChild(samplePanel("Button states", "A command button should have a short, explicit action label.", row,
                "Button button = new Button(\"Run action\");\nbutton.hoverBackground(hoverColor)\n        .pressedBackground(pressedColor)\n        .interactionTransitions(true);\nbutton.onClick(event -> runAction());"));

        Button graphicalButton = new Button();
        styleInteractiveButton(graphicalButton, gallery.accent);
        graphicalButton.richText(graphicalButtonText());
        graphicalButton.layout(style -> style.width(200.0f).height(40.0f));
        graphicalButton.onClick(event -> result.text("Project launch requested"));
        HBox graphicalRow = new HBox();
        graphicalRow.spacing(18.0f);
        graphicalRow.layout(style -> style.widthPercent(100.0f).alignItems(Align.CENTER));
        graphicalRow.addChild(graphicalButton);
        graphicalRow.addChild(label("Inline vector icon", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        root.addChild(samplePanel("Button with graphical content",
                "Combine a procedural inline icon and a concise action label in one button.", graphicalRow,
                """
                Button button = new Button();
                RichText content = RichText.builder()
                        .size(13)
                        .color(white)
                        .inline("launch-icon", "[>]", 16, 16, InlineContentAlignment.CENTER, (draw, icon) -> {
                            float x = icon.x(), y = icon.y();
                            draw.roundedRectStroke(x + 0.5f, y + 0.5f, icon.width() - 1, icon.height() - 1, 4, icon.color(), 1.25f);
                            draw.addTriangleFilled(
                                    new DrawPoint(x + 6.4f, y + 4.5f),
                                    new DrawPoint(x + 6.4f, y + 11.5f),
                                    new DrawPoint(x + 11.2f, y + 8),
                                    icon.color());
                        })
                        .append(" Launch project")
                        .build();
                button.richText(content);"""));


        Label status = label("Hold to confirm", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        HoldButton hold = new HoldButton("Hold to confirm")
                .holdDurationSeconds(1.2f)
                .cancelOnPointerExit(true);
        styleInteractiveButton(hold, gallery.accent);
        hold.holdColor().set(0.2f, 0.75f, 1.0f, 0.55f);
        hold.layout(style -> style.width(190.0f).height(40.0f));
        hold.onHoldCompleted(event -> status.text("Confirmed after "
                + UniGuiDemo.GalleryTemplate.format(event.holdDurationSeconds()) + " seconds"));

        HBox holdRow = new HBox();
        holdRow.spacing(18.0f);
        holdRow.layout(style -> style.widthPercent(100.0f).alignItems(Align.CENTER));
        holdRow.addChild(hold);
        holdRow.addChild(status);
        root.addChild(samplePanel("Hold Button",
                "Keep the primary pointer pressed until the progress fill completes. Leaving the button cancels the hold.",
                holdRow,
                "HoldButton button = new HoldButton(\"Hold to confirm\")\n        .holdDurationSeconds(1.2f)\n        .cancelOnPointerExit(true);\nbutton.onHoldCompleted(event -> confirmAction());"));

    }

    private static void styleInteractiveButton(Button button, MutableColor background) {
        UniGuiDemo.GalleryTemplate.styleButton(button, background, UniGuiDemo.GalleryTemplate.color(255, 255, 255));
        button.hoverBackground(UniGuiDemo.GalleryTemplate.color(64, 173, 241));
        button.pressedBackground(UniGuiDemo.GalleryTemplate.color(18, 83, 151));
        button.interactionTransitions(true).interactionScales(1.0f, 1.025f, 0.97f);
    }

    /**
     * Содержимое кнопки: векторная иконка запуска и подпись в одной строке.
     *
     * <p>Иконка рисуется процедурно в рамке {@code 16x16}: скруглённая обводка и заливной треугольник «play»,
     * сдвинутый на долю ширины вправо, чтобы оптический центр совпал с центром рамки. Выравнивание
     * {@link InlineContentAlignment#CENTER} ставит иконку по вертикальному центру строки, поэтому ведущие
     * пробелы не нужны: отступ до подписи задаёт один пробел перед текстом.</p>
     */
    private static RichText graphicalButtonText() {
        ColorView color = UniGuiDemo.GalleryTemplate.color(255, 255, 255);
        return RichText.builder()
                .size(13.0f)
                .color(color)
                .inline("launch-icon", "[>]", 16.0f, 16.0f, InlineContentAlignment.CENTER, (draw, icon) -> {
                    float x = icon.x();
                    float y = icon.y();
                    float width = icon.width();
                    float height = icon.height();
                    draw.roundedRectStroke(x + 0.5f, y + 0.5f, width - 1.0f, height - 1.0f, 4.0f, icon.color(), 1.25f);
                    float left = x + width * 0.40f;
                    float right = x + width * 0.70f;
                    float top = y + height * 0.28f;
                    float bottom = y + height * 0.72f;
                    draw.addTriangleFilled(new DrawPoint(left, top), new DrawPoint(left, bottom),
                            new DrawPoint(right, y + height * 0.5f), icon.color());
                })
                .append(" Launch project")
                .build();
    }

    private void addToggle(VBox root) {
        ToggleButton toggle = new ToggleButton("Enable option");
        Label state = label("Off", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        toggle.onCheckedChanged(event -> state.text(event.newValue() ? "On" : "Off"));
        HBox row = new HBox();
        row.spacing(16.0f);
        row.layout(style -> style.widthPercent(100.0f).alignItems(Align.CENTER));
        row.addChild(toggle);
        row.addChild(state);
        root.addChild(samplePanel("Toggle state", "Use a toggle for a persistent setting rather than a one-shot action.", row,
                "ToggleButton toggle = new ToggleButton(\"Enable option\");\ntoggle.checked(true);"));

        ToggleSwitch toggleSwitch = new ToggleSwitch("Enable option");
        Label state2 = label("Off", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        toggleSwitch.onCheckedChanged(event -> state2.text(event.newValue() ? "On" : "Off"));

        HBox switchRow = new HBox();
        switchRow.spacing(16.0f);
        switchRow.layout(style -> style.widthPercent(100.0f).alignItems(Align.CENTER));
        switchRow.addChild(toggleSwitch);
        switchRow.addChild(state2);

        root.addChild(samplePanel("", "", switchRow,
                "ToggleSwitch toggleSwitch = new ToggleSwitch(\"Enable option\");\ntoggleSwitch.checked(true);"));
    }

    private void addCheckbox(VBox root) {
        Checkbox checkbox = new Checkbox("Enable snapping");
        Label state = label("Disabled", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        checkbox.onCheckedChanged(event -> state.text(event.newValue() ? "Enabled" : "Disabled"));
        VBox row = new VBox();
        row.spacing(10.0f);
        row.addChild(checkbox);
        row.addChild(state);
        root.addChild(samplePanel("Independent options", "Checkboxes represent settings that can be enabled independently.", row,
                "Checkbox checkbox = new Checkbox(\"Enable snapping\");\ncheckbox.checked(true);"));

        Checkbox partialTree = new Checkbox("Tree parent (partial)")
                .triState(true)
                .state(CheckboxState.INDETERMINATE);
        Label partialTreeState = label("Disabled", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        partialTree.onStateChanged(event -> {
            switch (event.newState()) {
                case CHECKED -> partialTreeState.text("Enabled");
                case UNCHECKED -> partialTreeState.text("Disabled");
                case INDETERMINATE -> partialTreeState.text("Indeterminate");
            }
        });
        VBox partialTreeRow = new VBox();
        partialTreeRow.spacing(10.0f);
        partialTreeRow.addChild(partialTree);
        partialTreeRow.addChild(partialTreeState);
        root.addChild(samplePanel("Tri-state checkbox",
                "With triState enabled, the checkbox can represent unchecked, checked, or indeterminate. This is useful for a parent option with only some child options selected.",
                partialTreeRow,
                """
                        Checkbox checkbox = new Checkbox("Tree parent (partial)")
                                .triState(true)
                                .state(CheckboxState.INDETERMINATE);"""));
    }

    private void addRadio(VBox root) {
        RadioGroup group = new RadioGroup();
        RadioButton low = new RadioButton("Low", "low");
        RadioButton medium = new RadioButton("Medium", "medium");
        RadioButton high = new RadioButton("High", "high");
        group.add(low).add(medium).add(high).selectedValue("medium");
        Label state = label("Selected: medium", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        low.onCheckedChanged(event -> state.text("Selected: " + group.selectedValue()));
        medium.onCheckedChanged(event -> state.text("Selected: " + group.selectedValue()));
        high.onCheckedChanged(event -> state.text("Selected: " + group.selectedValue()));
        VBox options = new VBox();
        options.spacing(8.0f);
        options.addChild(low);
        options.addChild(medium);
        options.addChild(high);
        HBox row = new HBox();
        row.spacing(36.0f);
        row.layout(style -> style.widthPercent(100.0f));
        row.addChild(options);
        row.addChild(state);
        root.addChild(samplePanel("Mutually exclusive choices", "Group related options so only one value is active.", row,
                "RadioGroup group = new RadioGroup();\ngroup.add(low).add(medium).add(high);"));
    }

    private void addSlider(VBox root) {
        VBox examples = new VBox();
        examples.spacing(10.0f);
        examples.addChild(sliderRow("Continuous", slider(0.0f, 100.0f, 0.0f, 37.0f), "37.0"));
        examples.addChild(sliderRow("10-unit step", slider(0.0f, 100.0f, 10.0f, 52.0f), "52.0"));
        examples.addChild(sliderRow("Stepped", slider(0.0f, 100.0f, 25.0f, 50.0f), "50.0"));
        examples.addChild(sliderRow("Fine range", slider(-1.0f, 1.0f, 0.1f, 0.3f), "0.3"));
        root.addChild(samplePanel("Range and step", "The value is clamped to its range. A positive step quantizes changes.", examples,
                "Slider slider = new Slider().range(0, 100).step(10).value(40);"));

        HBox verticals = new HBox();
        verticals.spacing(34.0f);
        verticals.layout(style -> style.widthPercent(100.0f).alignItems(Align.CENTER));
        verticals.addChild(verticalSliderColumn("Continuous", 0.0f, 100.0f, 0.0f, 35.0f));
        verticals.addChild(verticalSliderColumn("10-unit step", 0.0f, 100.0f, 10.0f, 60.0f));
        verticals.addChild(verticalSliderColumn("Top is max", 0.0f, 100.0f, 0.0f, 100.0f));
        verticals.addChild(verticalSliderColumn("Fine range", -1.0f, 1.0f, 0.1f, 0.3f));
        root.addChild(samplePanel("Vertical sliders",
                "Orientation.VERTICAL puts min at the bottom and max at the top. Drag, or focus a slider and use Up/Down.",
                verticals,
                "Slider slider = new Slider().orientation(Orientation.VERTICAL).range(0, 100);"));

        var snapSlider = slider(0.0f, 100.0f, 10.0f, 40.0f);
        Label snapValue = UniGuiDemo.GalleryTemplate.valueLabel(snapSlider.value());
        snapSlider.onValueChanged(event -> snapValue.text(UniGuiDemo.GalleryTemplate.format(event.newValue())));
        HBox snapRow = new HBox();
        snapRow.spacing(14.0f);
        snapRow.layout(style -> style.widthPercent(100.0f).alignItems(Align.CENTER));
        snapRow.addChild(label("Snap to ticks", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        snapRow.addChild(UniGuiDemo.GalleryTemplate.expandSpacer());
        snapRow.addChild(snapSlider);
        snapRow.addChild(snapValue);
        Checkbox snap = new Checkbox("Enable");
        snap.checked(true);
        snap.onCheckedChanged(event -> snapSlider.step(event.newValue() ? 10.0f : 0.0f));
        VBox snapContent = new VBox();
        snapContent.spacing(12.0f);
        snapContent.layout(style -> style.widthPercent(100.0f));
        snapContent.addChild(snapRow);
        snapContent.addChild(snap);
        root.addChild(samplePanel("Snap to ticks", "Toggle snapping while dragging to compare stepped and continuous values.", snapContent,
                "slider.step(enabled ? 10.0f : 0.0f);"));
    }

    private void addPreview(VBox root, String controlId) {
        Control control = CONTROLS.stream().filter(item -> item.id().equals(controlId)).findFirst().orElseThrow();
        root.addChild(samplePanel("Preview", "Interactive sample", new Label(control.title()), ""));
    }
}

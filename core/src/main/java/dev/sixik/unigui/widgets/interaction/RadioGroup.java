package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@AiDoc(
        summary = "Logical coordinator for a group of mutually exclusive RadioButton elements.",
        purpose = "Enforces single-selection invariant across member radio buttons and tracks active value.",
        useWhen = {"Grouping radio buttons to ensure only one is selected."},
        related = {"RadioButton"}
)
@WikiPage(
        title = "<key:radio.group.title>",
        slug = "radio-group",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:radio.group.summary>",
        order = 100
)
@WikiSection(
        title = "<key:radio.group.section.overview.title>",
        markdown = "<key:radio.group.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:radio.group.example.basic.title>",
        code = "<key:radio.group.example.basic.code>",
        order = 10
)
@WikiLink(label = "RadioButton", target = "page:radio-button")
public final class RadioGroup {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.RADIO_GROUP;

    private final List<RadioButton> buttons = new ObjectArrayList<>();
    private RadioButton selectedButton;

    @WikiMethod(order = 10, title = "<key:radio.group.method.add.title>", summary = "<key:radio.group.method.add.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public RadioGroup add(RadioButton button) {
        if (button == null || buttons.contains(button)) return this;
        RadioGroup previousGroup = button.group();
        if (previousGroup != null && previousGroup != this) {
            previousGroup.remove(button);
        }
        buttons.add(button);
        button.setGroupInternal(this);
        if (button.checked()) {
            select(button, false);
        }
        return this;
    }

    public RadioGroup remove(RadioButton button) {
        if (button == null || !buttons.remove(button)) return this;
        if (selectedButton == button) {
            selectedButton = null;
            button.setCheckedFromGroup(false, false);
        }
        button.setGroupInternal(null);
        return this;
    }

    public List<RadioButton> buttons() {
        return Collections.unmodifiableList(buttons);
    }

    public RadioButton selectedButton() {
        return selectedButton;
    }

    @WikiMethod(order = 20, title = "<key:radio.group.method.selected_value.title>", summary = "<key:radio.group.method.selected_value.summary>")
    public String selectedValue() {
        return selectedButton == null ? "" : selectedButton.value();
    }

    public RadioGroup selectedValue(String value) {
        selectValue(value, true);
        return this;
    }

    public RadioGroup silentSelectedValue(String value) {
        selectValue(value, false);
        return this;
    }

    public RadioGroup select(RadioButton button) {
        select(button, true);
        return this;
    }

    public RadioGroup clearSelection() {
        clearSelection(true);
        return this;
    }

    void select(RadioButton button, boolean emitChange) {
        if (button == null) {
            clearSelection(emitChange);
            return;
        }
        if (!buttons.contains(button)) {
            add(button);
        }
        if (selectedButton == button) {
            button.setCheckedFromGroup(true, emitChange);
            return;
        }

        RadioButton previous = selectedButton;
        selectedButton = button;
        if (previous != null) {
            previous.setCheckedFromGroup(false, emitChange);
        }
        button.setCheckedFromGroup(true, emitChange);
    }

    void clearSelection(boolean emitChange) {
        RadioButton previous = selectedButton;
        selectedButton = null;
        if (previous != null) {
            previous.setCheckedFromGroup(false, emitChange);
        }
    }

    private void selectValue(String value, boolean emitChange) {
        String normalized = value == null ? "" : value;
        for (RadioButton button : buttons) {
            if (Objects.equals(button.value(), normalized)) {
                select(button, emitChange);
                return;
            }
        }
        clearSelection(emitChange);
    }
}

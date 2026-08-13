package dev.sixik.unigui.widgets;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RadioGroup {
    private final List<RadioButton> buttons = new ObjectArrayList<>();
    private RadioButton selectedButton;

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

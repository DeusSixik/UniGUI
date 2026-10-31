package dev.sixik.unigui.demo.ui;

import dev.sixik.unigui.api.utils.reactive.BindingKeys;
import dev.sixik.unigui.api.utils.reactive.BooleanSignal;
import dev.sixik.unigui.api.utils.reactive.StringSignal;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.widgets.interaction.ToggleButton;

import static dev.sixik.unigui.demo.ui.DemoScaffold.*;

/**
 * Demonstrates shared two-way signal bindings across multiple widgets.
 */
public final class BindingDemoScreen {
    private BindingDemoScreen() {
    }

    public static void open() {
        DemoScaffold.open("Signal Binding Demo", "BINDING", null, BindingDemoScreen::build);
    }

    private static void build(DemoScaffold.DemoShell shell) {
        StringSignal sharedText = new StringSignal("Edit either field");
        BooleanSignal sharedChecked = new BooleanSignal(true);

        VBox workspace = new VBox();
        workspace.spacing(12.0f);
        workspace.layout(style -> style.sizePercent(100.0f, 100.0f).padding(18.0f));
        workspace.addChild(fullWidth(label("TWO TEXT FIELDS / ONE SIGNAL", TEXT), 24.0f));
        workspace.addChild(fullWidth(label("Changes in either editor are sent to the other.", MUTED), 22.0f));

        TextField firstEditor = new TextField();
        firstEditor.layout(style -> style.widthPercent(100.0f).height(34.0f));
        firstEditor.bind(BindingKeys.TEXT_INPUT_TEXT, sharedText);
        workspace.addChild(firstEditor);

        TextField secondEditor = new TextField();
        secondEditor.layout(style -> style.widthPercent(100.0f).height(34.0f));
        secondEditor.bind(BindingKeys.TEXT_INPUT_TEXT, sharedText);
        workspace.addChild(secondEditor);
        shell.content(workspace);

        Checkbox checkbox = new Checkbox("Enabled");
        checkbox.bind(BindingKeys.TOGGLE_BUTTON_CHECKED, sharedChecked);
        ToggleButton toggle = new ToggleButton("Enabled");
        toggle.bind(BindingKeys.TOGGLE_BUTTON_CHECKED, sharedChecked);
        shell.add(checkbox);
        shell.add(toggle);
        shell.add(fullWidth(button("Set text from model", () -> sharedText.set("Updated by model")), 28.0f));
        shell.add(fullWidth(button("Toggle model value", () -> sharedChecked.set(!sharedChecked.get())), 28.0f));
    }
}

package dev.sixik.unigui.demo;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.demo.basic.main_demo.UniGuiDemo;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.RadioButton;
import dev.sixik.unigui.widgets.navigation.TreeView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Стили демо: на светлой карточке Checkbox, RadioButton и TreeView должны получать тёмный текст.
 */
class UniGuiDemoStyleTest {
    private static void themed(VBox root, DefaultUIContext context) {
        context.theme(UniGuiDemo.PACK, root);
        for (var child : root.children()) {
            ((dev.sixik.unigui.impl.widget.WidgetBase) child).setUiContextInternal(context);
        }
        root.measure(new LayoutContext(400.0f, 400.0f));
        root.arrange(new MutableRect(0.0f, 0.0f, 400.0f, 400.0f));
        root.render(new DefaultRenderContext(new DrawList()));
    }

    private static String rgb(dev.sixik.unigui.api.math.ColorView c) {
        return c.r() + "," + c.g() + "," + c.b() + "," + c.a();
    }

    private static boolean dark(float r, float g, float b) {
        return r < 0.35f && g < 0.35f && b < 0.45f;
    }

    private static void assertBlue(ColorView color, String description) {
        assertTrue(color.b() > color.r() && color.b() > color.g(),
                description + " should use a blue accent, got " + rgb(color));
    }

    @Test
    void builtInControlAccents_useBluePalette() {
        assertBlue(UniGuiDemo.PACK.resolveStyleFor("ToggleButton", "", List.of())
                .get(StyleKeys.BACKGROUND_COLOR, WidgetState.CHECKED, null), "toggle checked background");
        assertBlue(UniGuiDemo.PACK.resolveStyleFor("Checkbox", "", List.of())
                .get(StyleKeys.CHECKBOX_BACKGROUND_COLOR, WidgetState.CHECKED, null), "checkbox checked background");
        assertBlue(UniGuiDemo.PACK.resolveStyleFor("Slider", "", List.of())
                .get(StyleKeys.ACCENT_COLOR, WidgetState.NORMAL, null), "slider fill");
        assertBlue(UniGuiDemo.PACK.resolveStyleFor("TextField", "", List.of())
                .get(StyleKeys.BORDER_COLOR, WidgetState.FOCUSED, null), "focused text field border");
        assertBlue(UniGuiDemo.PACK.resolveStyleFor("ProgressBar", "", List.of())
                .get(StyleKeys.ACCENT_COLOR, WidgetState.NORMAL, null), "progress fill");
    }

    @Test
    void checkboxAndRadioText_isDarkInEveryState() {
        DefaultUIContext context = new DefaultUIContext();
        VBox root = new VBox();
        Checkbox checkbox = new Checkbox("Enable").checkAnimation(0.0f);
        RadioButton radio = new RadioButton("Low", "low");
        root.addChild(checkbox);
        root.addChild(radio);
        root.applyQueuedMutations();
        themed(root, context);
        assertTrue(dark(checkbox.textColor().r(), checkbox.textColor().g(), checkbox.textColor().b()),
                "checkbox normal text " + rgb(checkbox.textColor()));
        assertTrue(dark(radio.textColor().r(), radio.textColor().g(), radio.textColor().b()),
                "radio normal text " + rgb(radio.textColor()));

        checkbox.silentState(dev.sixik.unigui.api.widget.CheckboxState.CHECKED);
        radio.silentChecked(true);
        themed(root, context);
        assertTrue(dark(checkbox.textColor().r(), checkbox.textColor().g(), checkbox.textColor().b()),
                "checkbox checked text " + rgb(checkbox.textColor()));
        assertTrue(dark(radio.textColor().r(), radio.textColor().g(), radio.textColor().b()),
                "radio checked text " + rgb(radio.textColor()));
    }

    @Test
    void treeViewText_isDark() {
        DefaultUIContext context = new DefaultUIContext();
        VBox root = new VBox();
        TreeView tree = UniGuiDemo.cardTree(new TreeView());
        tree.addRoot("Project").addChild("Scenes");
        root.addChild(tree);
        root.applyQueuedMutations();
        themed(root, context);

        root.arrange(new MutableRect(0.0f, 0.0f, 400.0f, 400.0f));
        DrawList list = new DrawList();
        root.render(new DefaultRenderContext(list));

        int texts = 0;
        for (var command : list.commands()) {
            if (command.type() != dev.sixik.unigui.api.render.DrawCommandType.TEXT) continue;
            texts++;
            var color = command.paint().color();
            assertTrue(dark(color.r(), color.g(), color.b()), "tree text " + rgb(color));
        }
        assertTrue(texts > 0, "the tree draws its row labels");
    }
}

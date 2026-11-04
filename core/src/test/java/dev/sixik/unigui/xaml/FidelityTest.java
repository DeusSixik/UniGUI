package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.xaml.bind.XamlBindingContext;
import dev.sixik.unigui.xaml.bind.XamlCommandRegistry;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.DocumentValidator;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.snapshot.Snapshotter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Живой виджет, загруженный из документа, сохраняется обратно без потери смысла: привязки и команды остаются
 * выражениями, а не превращаются в текущее значение. Здесь же — attached-свойства вида {@code Grid.Row}.
 */
final class FidelityTest {
    private static String save(Widget widget) {
        return XamlFormat.INSTANCE.write(new Snapshotter(Xaml.defaultSchema()).snapshot(widget).document());
    }

    private static dev.sixik.unigui.api.layout.LayoutStyle style(Widget widget) {
        return ((dev.sixik.unigui.impl.widget.WidgetBase) widget).layoutStyle();
    }

    private static LoadOptions options() {
        XamlBindingContext data = XamlBindingContext.empty();
        data.mutable("user.name", String.class, "Steve");
        data.mutable("volume", Float.class, 0.5f);
        XamlCommandRegistry commands = XamlCommandRegistry.empty()
                .register("shop.buy", (source, event) -> { })
                .register("save", (source, event) -> { });
        return LoadOptions.DEFAULT.bindingContext(data).commands(commands);
    }

    // ------------------------------------------------------------------ привязки и команды

    @Test
    void aBindingStaysAnExpressionWhenTheWidgetIsSaved() {
        Widget label = Xaml.load("<Label id=\"l\" text=\"{Binding user.name}\"/>", options());
        assertEquals("Steve", ((Label) label).text(), "the bound value is shown");
        String text = save(label);
        assertTrue(text.contains("{Binding user.name}"), "the expression is kept: " + text);
        assertFalse(text.contains("Steve"), "the current value is not frozen into the document: " + text);
    }

    @Test
    void savingIsAFixedPointForBindingsAndCommands() {
        String source = "<VBox><Label id=\"l\" text=\"{Binding user.name}\"/>"
                + "<Button id=\"b\" text=\"Buy\" onClick=\"shop.buy\"/>"
                + "<Button id=\"c\" text=\"Save\" onClick=\"{Command save}\"/></VBox>";
        Widget original = Xaml.load(source, options());
        String first = save(original);
        Widget reloaded = Xaml.load(first, options());
        assertEquals(first, save(reloaded), "a second round trip changes nothing: " + first);
        assertTrue(first.contains("onClick=\"shop.buy\""), first);
        assertTrue(first.contains("{Command save}"), first);
    }

    @Test
    void aSavedCommandStillWorksAfterReloading() {
        AtomicInteger calls = new AtomicInteger();
        XamlCommandRegistry commands = XamlCommandRegistry.empty().register("save", (source, event) -> calls.incrementAndGet());
        LoadOptions withCommand = LoadOptions.DEFAULT.commands(commands);
        Widget button = Xaml.load("<Button id=\"b\" onClick=\"save\"/>", withCommand);
        Widget again = Xaml.load(save(button), withCommand);
        ((Button) again).click();
        assertEquals(1, calls.get());
    }

    @Test
    void bindingModesSurviveTheTrip() {
        Widget slider = Xaml.load("<Slider id=\"s\" min=\"0\" max=\"1\" value=\"{Binding volume, Mode=TwoWay}\"/>", options());
        String text = save(slider);
        assertTrue(text.contains("Mode=TwoWay"), text);
        assertTrue(text.contains("{Binding volume"), text);
    }

    @Test
    void aPlainLiteralPropertyIsStillSavedByValue() {
        Widget label = Xaml.load("<Label id=\"l\" text=\"Hello\"/>", options());
        String text = save(label);
        assertTrue(text.contains("text=\"Hello\""), text);
    }

    // ------------------------------------------------------------------ attached-свойства

    @Test
    void gridPlacementCanBeWrittenAsAttachedProperties() {
        Widget grid = Xaml.load("""
                <GridBox id="g" gridTemplateColumns="1fr 1fr">
                  <Label id="a" text="A" Grid.Row="2" Grid.Column="2" Grid.ColumnSpan="2"/>
                </GridBox>""");
        Widget label = Xaml.get(grid, "a");
        assertEquals(2, style(label).gridRowStart());
        assertEquals(2, style(label).gridColumnStart());
        assertEquals(2, style(label).gridColumnSpan());

        // Сохранение пишет обычные свойства потомка; результат загружается так же.
        String text = save(grid);
        assertTrue(text.contains("gridRow=\"2\""), text);
        Widget again = Xaml.load(text);
        assertEquals(2, style(Xaml.get(again, "a")).gridRowStart());
    }

    @Test
    void flexAttachedPropertiesMapToTheChildProperties() {
        Widget row = Xaml.load("<HBox><Label id=\"a\" Flex.Grow=\"1\" Flex.Basis=\"40\"/></HBox>");
        assertEquals(1.0f, style(Xaml.get(row, "a")).flexGrow());
    }

    @Test
    void unknownAttachedPropertiesAreErrorsAndTheValidatorKnowsTheKnownOnes() {
        XamlLoadException failure = assertThrows(XamlLoadException.class,
                () -> Xaml.load("<GridBox><Label Grid.Nope=\"1\"/></GridBox>"));
        assertTrue(failure.getMessage().contains("Unknown attribute 'Grid.Nope'"), failure.getMessage());

        DocumentValidator validator = new DocumentValidator(Xaml.defaultSchema());
        assertTrue(validator.validate("<GridBox><Label Grid.Row=\"1\"/></GridBox>").valid());
        assertFalse(validator.validate("<GridBox><Label Grid.Nope=\"1\"/></GridBox>").valid());
    }
}

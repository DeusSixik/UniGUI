package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.data.VirtualListView;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.bind.XamlBindingContext;
import dev.sixik.unigui.xaml.bind.XamlBindingListener;
import dev.sixik.unigui.xaml.bind.XamlCommandRegistry;
import dev.sixik.unigui.xaml.bind.XamlMutableObservableValue;
import dev.sixik.unigui.xaml.bind.XamlObservableValue;
import dev.sixik.unigui.xaml.bind.XamlParameterCommand;
import dev.sixik.unigui.xaml.component.Components;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.load.LoadedScreen;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TwoWayAndParameterTest {
    private static LoadOptions with(XamlBindingContext data) {
        return LoadOptions.DEFAULT.bindingContext(data);
    }

    // ------------------------------------------------------------------ TwoWay

    @Test
    void textFollowsTheSourceAndWritesBackWhenTheUserTypes() {
        XamlBindingContext data = XamlBindingContext.empty();
        XamlMutableObservableValue<String> name = data.mutable("name", String.class, "start");
        TextField field = Xaml.load("<TextField text=\"{Binding name, Mode=TwoWay}\"/>", TextField.class, with(data));
        assertEquals("start", field.text());

        field.text("typed");
        assertEquals("typed", name.get(), "an edit reaches the source");

        name.set("remote");
        assertEquals("remote", field.text(), "a source change reaches the widget");
    }

    @Test
    void checkboxAndSliderWorkBothWays() {
        XamlBindingContext data = XamlBindingContext.empty();
        XamlMutableObservableValue<Boolean> enabled = data.mutable("enabled", Boolean.class, false);
        XamlMutableObservableValue<Float> volume = data.mutable("volume", Float.class, 0.25f);
        Widget root = Xaml.load("""
                <VBox>
                  <Checkbox id="box" checked="{Binding enabled, Mode=TwoWay}"/>
                  <Slider id="slider" min="0" max="1" value="{Binding volume, Mode=TwoWay}"/>
                </VBox>
                """, with(data));
        Checkbox box = Xaml.get(root, "box", Checkbox.class);
        Slider slider = Xaml.get(root, "slider", Slider.class);
        assertEquals(0.25f, slider.value(), 0.0001f);

        box.checked(true);
        assertTrue(enabled.get());
        enabled.set(false);
        assertEquals(false, box.checked());

        slider.value(0.75f);
        assertEquals(0.75f, volume.get(), 0.0001f);
        volume.set(0.5f);
        assertEquals(0.5f, slider.value(), 0.0001f);
    }

    @Test
    void closingTheScreenStopsWritingBack() {
        XamlBindingContext data = XamlBindingContext.empty();
        XamlMutableObservableValue<String> name = data.mutable("name", String.class, "a");
        LoadedScreen<Widget> screen = Xaml.loadScreen("<TextField text=\"{Binding name, Mode=TwoWay}\"/>", with(data));
        TextField field = (TextField) screen.root();
        field.text("b");
        assertEquals("b", name.get());
        screen.close();
        field.text("c");
        assertEquals("b", name.get(), "a closed screen no longer reports edits");
    }

    @Test
    void oneWayStaysOneWay() {
        XamlBindingContext data = XamlBindingContext.empty();
        XamlMutableObservableValue<String> name = data.mutable("name", String.class, "a");
        TextField field = Xaml.load("<TextField text=\"{Binding name}\"/>", TextField.class, with(data));
        field.text("typed");
        assertEquals("a", name.get(), "without TwoWay the widget does not write back");
    }

    @Test
    void twoWayMistakesAreReportedClearly() {
        XamlBindingContext data = XamlBindingContext.empty();
        data.mutable("name", String.class, "a");
        XamlObservableValue<String> readOnly = new XamlObservableValue<>() {
            @Override
            public String name() {
                return "fixed";
            }

            @Override
            public Class<String> valueType() {
                return String.class;
            }

            @Override
            public String get() {
                return "fixed";
            }

            @Override
            public EventSubscription onChanged(XamlBindingListener<String> listener) {
                return () -> { };
            }
        };
        data.value("fixed", readOnly);

        XamlLoadException notObservable = assertThrows(XamlLoadException.class,
                () -> Xaml.load("<Label text=\"{Binding name, Mode=TwoWay}\"/>", with(data)));
        assertTrue(notObservable.getMessage().contains("does not support Mode=TwoWay"), notObservable.getMessage());

        XamlLoadException immutable = assertThrows(XamlLoadException.class,
                () -> Xaml.load("<TextField text=\"{Binding fixed, Mode=TwoWay}\"/>", with(data)));
        assertTrue(immutable.getMessage().contains("needs a mutable source"), immutable.getMessage());

        XamlLoadException missing = assertThrows(XamlLoadException.class,
                () -> Xaml.load("<TextField text=\"{Binding nowhere, Mode=TwoWay}\"/>", with(data)));
        assertTrue(missing.getMessage().contains("'nowhere' is missing"), missing.getMessage());
    }

    // ------------------------------------------------------------------ параметры команд

    private static final class Calls {
        final List<String> log = new ArrayList<>();
        final XamlCommandRegistry registry = XamlCommandRegistry.empty()
                .register("shop.buy", (XamlParameterCommand) (source, event, parameter) -> log.add("buy:" + parameter))
                .register("plain", (source, event) -> log.add("plain"));
    }

    @Test
    void aLiteralParameterIsPassedToTheCommand() {
        Calls calls = new Calls();
        Button button = Xaml.load("<Button onClick=\"{Command shop.buy, Parameter=sword}\"/>", Button.class,
                LoadOptions.DEFAULT.commands(calls.registry));
        button.click();
        assertEquals(List.of("buy:sword"), calls.log);
    }

    @Test
    void parameterCanComeFromAResource() {
        Calls calls = new Calls();
        Button button = Xaml.load("""
                <Button>
                  <Button.Resources><x:String x:Key="Sku" xmlns:x="https://unigui.dev/xaml">sku-9</x:String></Button.Resources>
                  <Button.onClick>{Command shop.buy, Parameter={StaticResource Sku}}</Button.onClick>
                </Button>
                """, Button.class, LoadOptions.DEFAULT.commands(calls.registry));
        button.click();
        assertEquals(List.of("buy:sku-9"), calls.log);
    }

    @Test
    void everyRowOfAListPassesItsOwnParameter() {
        Calls calls = new Calls();
        XamlBindingContext data = XamlBindingContext.empty()
                .list("catalog", List.of(new Row("a"), new Row("b"), new Row("c")));
        VirtualListView list = Xaml.load("""
                <VirtualListView itemHeight="20" overscan="0" items="{Binding catalog}">
                  <VirtualListView.ItemTemplate>
                    <Button onClick="{Command shop.buy, Parameter={Item id}}"/>
                  </VirtualListView.ItemTemplate>
                </VirtualListView>
                """, VirtualListView.class, LoadOptions.DEFAULT.bindingContext(data).commands(calls.registry));
        list.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 60.0f));

        List<Button> buttons = new ArrayList<>();
        collect(list, buttons);
        assertEquals(3, buttons.size());
        buttons.forEach(Button::click);
        assertEquals(List.of("buy:a", "buy:b", "buy:c"), calls.log);
    }

    record Row(String id) {
    }

    private static void collect(Widget widget, List<Button> into) {
        if (widget instanceof Button button) into.add(button);
        for (Widget child : widget.children()) collect(child, into);
    }

    @Test
    void parameterInsideAComponentReadsItsParam() {
        Components.register("t:buycard", """
                <Component x:Name="BuyCard" xmlns:x="https://unigui.dev/xaml">
                  <Component.Contract><Param Name="Sku" Type="string"/></Component.Contract>
                  <Button id="go" onClick="{Command shop.buy, Parameter={Param Sku}}"/>
                </Component>
                """);
        try {
            Calls calls = new Calls();
            Widget root = Xaml.load("<BuyCard Sku=\"axe-1\"/>", LoadOptions.DEFAULT.commands(calls.registry));
            Xaml.get(root, "go", Button.class).click();
            assertEquals(List.of("buy:axe-1"), calls.log);
        } finally {
            Components.unregister("t:buycard");
        }
    }

    @Test
    void parameterMistakesAreReportedAtLoad() {
        Calls calls = new Calls();
        LoadOptions options = LoadOptions.DEFAULT.commands(calls.registry);

        XamlLoadException plain = assertThrows(XamlLoadException.class, () -> Xaml.load(
                "<Button onClick=\"{Command plain, Parameter=x}\"/>", options));
        assertTrue(plain.getMessage().contains("does not take a parameter"), plain.getMessage());

        XamlLoadException unknown = assertThrows(XamlLoadException.class, () -> Xaml.load(
                "<Button onClick=\"{Command nope, Parameter=x}\"/>", options));
        assertTrue(unknown.getMessage().contains("Unknown XML command 'nope'"), unknown.getMessage());

        XamlLoadException live = assertThrows(XamlLoadException.class, () -> Xaml.load(
                "<Button onClick=\"{Command shop.buy, Parameter={Binding x}}\"/>", options));
        assertTrue(live.getMessage().contains("live Binding cannot be used"), live.getMessage());

        XamlLoadException option = assertThrows(XamlLoadException.class, () -> Xaml.load(
                "<Button onClick=\"{Command shop.buy, Argument=x}\"/>", options));
        assertTrue(option.getMessage().contains("Unsupported Command option 'Argument'"), option.getMessage());

        XamlLoadException outside = assertThrows(XamlLoadException.class, () -> Xaml.load(
                "<Button onClick=\"{Command shop.buy, Parameter={Item id}}\"/>", options));
        assertTrue(outside.getMessage().contains("only be used inside an ItemTemplate"), outside.getMessage());
    }

    @Test
    void commandsWithoutParametersKeepWorkingAndOldRegistrationStyleCompiles() {
        Calls calls = new Calls();
        Label label = Xaml.load("<Label text=\"x\"/>", Label.class);
        assertEquals("x", label.text());
        Button button = Xaml.load("<Button onClick=\"plain\"/>", Button.class, LoadOptions.DEFAULT.commands(calls.registry));
        button.click();
        assertEquals(List.of("plain"), calls.log);
    }
}

package dev.sixik.unigui.api.utils.reactive;

import org.junit.jupiter.api.Test;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.widgets.interaction.ToggleButton;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SignalTest {
    @Test
    void textBindingIsTwoWayAndDisposedWithWidget() {
        StringSignal signal = new StringSignal("initial");
        TextInput input = new TextInput();
        input.bind(BindingKeys.TEXT_INPUT_TEXT, signal);

        assertEquals("initial", input.text());
        signal.set("from model");
        assertEquals("from model", input.text());
        input.text("from widget");
        assertEquals("from widget", signal.get());

        input.dispose();
        signal.set("after dispose");
        assertEquals("from widget", input.text());
    }

    @Test
    void checkedBindingCanBeReplaced() {
        BooleanSignal first = new BooleanSignal(false);
        BooleanSignal second = new BooleanSignal(true);
        ToggleButton button = new ToggleButton();
        button.bind(BindingKeys.TOGGLE_BUTTON_CHECKED, first);
        button.bind(BindingKeys.TOGGLE_BUTTON_CHECKED, second);

        assertEquals(false, first.get());
        assertEquals(true, button.checked());
        first.set(false);
        assertEquals(true, button.checked());
        button.checked(false);
        assertEquals(false, second.get());
    }

    @Test
    void subscriptionCanBeClosedIdempotently() {
        IntegerSignal signal = new IntegerSignal(1);
        List<Integer> values = new ArrayList<>();
        SignalSubscription subscription = signal.subscribe(values::add);

        signal.set(2);
        subscription.close();
        subscription.unsubscribe();
        signal.set(3);

        assertEquals(List.of(1, 2), values);
    }

    @Test
    void nestedSetIsDeliveredAfterCurrentNotification() {
        IntegerSignal signal = new IntegerSignal();
        List<Integer> observed = new ArrayList<>();
        signal.subscribe(value -> {
            if (value == 1) signal.set(2);
        });
        signal.subscribe(observed::add);

        signal.set(1);

        assertEquals(List.of(0, 1, 2), observed);
        assertEquals(2, signal.get());
    }

    @Test
    void typedSignalUsesValueEquality() {
        TypedSignal<String> signal = new TypedSignal<>(new String("same"));
        List<String> values = new ArrayList<>();
        signal.subscribe(values::add);

        signal.set(new String("same"));

        assertEquals(List.of("same"), values);
    }
}

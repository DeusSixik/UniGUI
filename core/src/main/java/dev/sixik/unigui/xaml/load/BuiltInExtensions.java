package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.xaml.bind.XamlBinding;
import dev.sixik.unigui.xaml.bind.XamlMutableObservableValue;
import dev.sixik.unigui.xaml.bind.XamlObservableValue;
import dev.sixik.unigui.xaml.component.ParamType;
import dev.sixik.unigui.xaml.UiValue;

import java.util.Set;

/**
 * Встроенные markup extension.
 */
final class BuiltInExtensions {
    private static final Set<String> BINDING_OPTIONS = Set.of("Path", "Mode");
    private static final Set<String> COMMAND_OPTIONS = Set.of("Name", "Parameter");

    private BuiltInExtensions() {
    }

    /**
     * {@code {Binding Path=player.health, Mode=OneWay}} или {@code {Binding player.health}}.
     *
     * <p>Поддержаны режимы {@code OneWay} (по умолчанию) и {@code OneTime}. Двусторонние режимы требуют
     * обратной связи от виджета к источнику и пока отклоняются явной ошибкой, а не молча игнорируются.</p>
     */
    static final MarkupExtension BINDING = (context, use, target) -> {
        for (String option : use.named().keySet()) {
            if (!BINDING_OPTIONS.contains(option)) {
                throw new IllegalArgumentException("Unsupported Binding option '" + option
                        + "'. Supported options: Path, Mode.");
            }
        }
        String path = literal(use.argument("Path", 0), "Binding Path");
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Binding requires a Path, for example {Binding player.health}");
        }
        String mode = use.named().containsKey("Mode") ? literal(use.named().get("Mode"), "Binding Mode") : "OneWay";
        String normalizedMode = mode.trim().toLowerCase(java.util.Locale.ROOT);
        boolean twoWay = normalizedMode.equals("twoway");
        boolean oneTime = switch (normalizedMode) {
            case "oneway", "twoway" -> false;
            case "onetime" -> true;
            case "onewaytosource" ->
                    throw new IllegalArgumentException("Binding Mode=" + mode.trim() + " is not supported yet");
            default -> throw new IllegalArgumentException("Unknown Binding Mode '" + mode.trim()
                    + "'. Supported modes: OneWay, OneTime, TwoWay.");
        };
        if (!target.bindable()) {
            throw new IllegalArgumentException("Attribute '" + target.name() + "' does not support XML binding");
        }
        @SuppressWarnings("unchecked")
        Class<Object> valueType = (Class<Object>) target.bindingType();
        XamlMutableObservableValue<Object> mutable = null;
        if (twoWay) {
            XamlObservableValue<?> source = context.bindings().value(path.trim()).orElse(null);
            if (!(source instanceof XamlMutableObservableValue<?> found)) {
                throw new IllegalArgumentException("Binding Mode=TwoWay needs a mutable source, but '" + path.trim()
                        + "' is " + (source == null ? "missing" : "read-only"));
            }
            @SuppressWarnings("unchecked")
            XamlMutableObservableValue<Object> cast = (XamlMutableObservableValue<Object>) found;
            mutable = cast;
        }
        XamlBinding<Object> binding = context.bindings().bind(path.trim(), valueType, target::setValue, target.describe());
        context.session().add(binding);
        if (oneTime) binding.close();
        if (twoWay) {
            XamlMutableObservableValue<Object> source = mutable;
            EventSubscription edits = target.observe(
                    value -> source.set(ParamType.adapt(value, source.valueType())));
            if (edits == null) {
                throw new IllegalArgumentException("Attribute '" + target.name()
                        + "' does not support Mode=TwoWay: the widget does not report edits");
            }
            context.session().onClose(edits::unsubscribe);
        }
    };

    /** {@code {StaticResource Key}}: подставляет текст ресурса и разбирает его конвертером свойства. */
    static final MarkupExtension STATIC_RESOURCE = (context, use, target) -> {
        String key = literal(use.argument("ResourceKey", 0), "StaticResource key");
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("StaticResource requires a key, for example {StaticResource Accent}");
        }
        String value = context.resources().find(key.trim())
                .orElseThrow(() -> new IllegalArgumentException("Resource '" + key.trim() + "' was not found"));
        target.setText(value);
    };

    /**
     * {@code {Command video.apply}}: явная запись имени команды. Эквивалентна простому значению
     * {@code onClick="video.apply"}, но читается как действие и годится для любого свойства-команды.
     */
    static final MarkupExtension COMMAND = (context, use, target) -> {
        for (String option : use.named().keySet()) {
            if (!COMMAND_OPTIONS.contains(option)) {
                throw new IllegalArgumentException("Unsupported Command option '" + option
                        + "'. Supported options: Name, Parameter.");
            }
        }
        String name = literal(use.argument("Name", 0), "Command name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Command requires a name, for example {Command video.apply}");
        }
        UiValue parameter = use.named().get("Parameter");
        if (parameter == null) {
            target.setText(name.trim());
            return;
        }
        if (context.host() == null) {
            throw new IllegalArgumentException("Command parameters are not available in this context");
        }
        Object value = context.host().evaluate(parameter);
        target.setText(context.host().bindCommand(name.trim(), value));
    };

    /**
     * {@code {Item}} вне шаблона: расширение известно (валидатор его принимает), но значения у него нет. Внутри
     * {@code ItemTemplate} загрузчик подставляет рабочую реализацию ({@link ItemExtension}).
     */
    static final MarkupExtension ITEM_OUTSIDE_TEMPLATE = (context, use, target) -> {
        throw new IllegalArgumentException("{Item} can only be used inside an ItemTemplate");
    };

    private static String literal(UiValue value, String what) {
        if (value == null) return null;
        if (value instanceof UiValue.Literal literal) return literal.text();
        throw new IllegalArgumentException(what + " must be plain text, not a nested expression");
    }
}

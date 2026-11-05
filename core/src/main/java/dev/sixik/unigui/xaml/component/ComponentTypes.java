package dev.sixik.unigui.xaml.component;

import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.Meta;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.TypeInfo;
import dev.sixik.unigui.xaml.schema.ValueConverter;
import dev.sixik.unigui.xaml.schema.ValueKind;

import java.util.ArrayList;
import java.util.List;

/**
 * Описание компонента как типа схемы: параметры становятся свойствами тега, исходящие события — свойствами-командами.
 *
 * <p>Благодаря этому ни загрузчик, ни валидатор, ни инспектор не знают о компонентах отдельно: тег
 * {@code <ProductCard Title="..." Buy="{Command BuyItem}"/>} обрабатывается теми же путями, что и {@code <Button>}.</p>
 */
final class ComponentTypes {
    private ComponentTypes() {
    }

    static TypeInfo<?> typeFor(ComponentDefinition definition, Schema schema) {
        TypeInfo.Builder<ComponentHost> builder = TypeInfo.of(definition.tag(), ComponentHost.class,
                        () -> new ComponentHost(definition, schema))
                .withoutAnnotations()
                .withoutChildExport()
                .describe(definition.tag().local(), "Components", definition.description())
                .afterLoad((host, context) -> host.build(context));

        builder.property("key", Converters.STRING, ComponentHost::key, ComponentHost::key,
                Meta.category("Component").defaultValue("")
                        .description("Stable key of the record this component shows; used to find the instance."));

        List<String> bindable = new ArrayList<>();
        for (Contract.Param param : definition.contract().params()) {
            addParam(builder, param);
            bindable.add(param.name());
        }
        builder.bindable(bindable.toArray(String[]::new));

        for (Contract.Event event : definition.contract().events()) {
            if (event.direction() != Contract.Direction.OUT) continue;
            builder.propertyWithContext(event.name(), Converters.STRING, (host, commandName, context) -> {
                String name = commandName == null ? "" : commandName.trim();
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Event '" + event.name() + "' command name must not be blank");
                }
                var command = context.commands().command(name).orElseThrow(
                        () -> new IllegalArgumentException("Unknown XAML command '" + name + "'"));
                host.instance().onEvent(event.name(),
                        args -> command.execute(host, new ComponentEvent(host, event.name(), args)));
            }, Meta.category("Events").kind(ValueKind.BINDING_OR_ACTION)
                    .description("Command invoked when the component raises " + event.name() + "."));
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static void addParam(TypeInfo.Builder<ComponentHost> builder, Contract.Param param) {
        ValueConverter<Object> converter = (ValueConverter<Object>) param.type().converter();
        Meta meta = Meta.category("Component")
                .defaultValue(param.defaultText() == null ? defaultText(param) : param.defaultText())
                .description(param.description().isEmpty()
                        ? "Component parameter of type " + param.type().name() + "."
                        : param.description());
        if (!param.type().textual()) meta = meta.kind(ValueKind.BINDING_OR_ACTION);
        builder.property(param.name(), converter,
                (host, value) -> host.instance().set(param.name(), value),
                host -> host.instance().get(param.name()), meta);
    }

    private static String defaultText(Contract.Param param) {
        Object value = param.value();
        if (value == null || !param.type().converter().canFormat()) return "";
        return param.type().converter().format(value);
    }
}

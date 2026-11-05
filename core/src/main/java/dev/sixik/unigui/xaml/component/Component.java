package dev.sixik.unigui.xaml.component;

import dev.sixik.unigui.xaml.Xaml;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.schema.LoadContext;

/**
 * Зарегистрированный компонент, из которого Java создаёт экземпляры.
 *
 * <pre>{@code
 * ComponentInstance card = Xaml.component("mymod:product_card").create(Args.of("Title", "Sword", "Price", 50));
 * card.onEvent("Buy", args -> shop.buy(args.get("productId", String.class)));
 * catalogPanel.addChild(card.widget());
 * }</pre>
 */
public final class Component {
    private final ComponentDefinition definition;

    Component(ComponentDefinition definition) {
        this.definition = definition;
    }

    /**
     * @return определение компонента
     */
    public ComponentDefinition definition() {
        return definition;
    }

    /**
     * @return контракт компонента
     */
    public Contract contract() {
        return definition.contract();
    }

    /**
     * Создаёт экземпляр со значениями параметров по умолчанию.
     *
     * @return экземпляр
     */
    public ComponentInstance create() {
        return create(Args.empty(), LoadOptions.DEFAULT);
    }

    /**
     * Создаёт экземпляр.
     *
     * @param args значения параметров; имена проверяются по контракту
     * @return экземпляр
     * @throws IllegalArgumentException если параметра нет или значение не того типа
     */
    public ComponentInstance create(Args args) {
        return create(args, LoadOptions.DEFAULT);
    }

    /**
     * Создаёт экземпляр с настройками загрузки тела (текстуры, команды).
     *
     * @param args    значения параметров
     * @param options настройки; берутся только резолвер текстур и команды
     * @return экземпляр
     */
    public ComponentInstance create(Args args, LoadOptions options) {
        LoadOptions effective = options == null ? LoadOptions.DEFAULT : options;
        ComponentHost host = new ComponentHost(definition, Xaml.defaultSchema());
        host.instance().set(args == null ? Args.empty() : args);
        host.build(new LoadContext(effective.textureResolver(), effective.commands()));
        return host.instance();
    }
}

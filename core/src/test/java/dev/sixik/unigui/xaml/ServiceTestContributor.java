package dev.sixik.unigui.xaml;

import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SchemaContributor;
import dev.sixik.unigui.xaml.schema.TypeInfo;

/**
 * Расширение, найденное через {@code META-INF/services} в тестовых ресурсах: доказывает, что подключение
 * виджетов бэкенда и модов работает без единой строки регистрации в коде.
 */
public final class ServiceTestContributor implements SchemaContributor {
    /** Пространство имён тестового «мода». */
    public static final String NAMESPACE = "urn:unigui-test:service";

    /** Виджет тестового «мода». */
    public static final class ServiceBadge extends TextWidget {
        private float importance;

        public float importance() {
            return importance;
        }

        public ServiceBadge importance(float importance) {
            this.importance = importance;
            return this;
        }
    }

    @Override
    public void contribute(Schema schema) {
        schema.define(NAMESPACE, "ServiceBadge", () -> TypeInfo.of(new UiName(NAMESPACE, "ServiceBadge", "svc"),
                        ServiceBadge.class, ServiceBadge::new)
                .describe("Service Badge", "Test", "Declared through ServiceLoader.")
                .property("importance", Converters.FLOAT, ServiceBadge::importance,
                        ServiceBadge::importance, dev.sixik.unigui.xaml.schema.Meta.category("Behavior"))
                .build());
    }
}

package dev.sixik.unigui.xaml.schema;

/**
 * Точка расширения, через которую бэкенды и моды добавляют свои виджеты в общий реестр.
 *
 * <p>Реализацию можно зарегистрировать двумя способами: через {@link SchemaContributions#register} в рантайме
 * или файлом {@code META-INF/services/dev.sixik.unigui.xaml.schema.SchemaContributor} (ServiceLoader). Виджеты
 * лучше определять в собственном пространстве имён, чтобы не сталкиваться с чужими:</p>
 *
 * <pre>{@code
 * schema.define("urn:mymod", "Gauge", () -> TypeInfo.of(new UiName("urn:mymod", "Gauge", "my"),
 *         Gauge.class, Gauge::new).build());
 * }</pre>
 */
@FunctionalInterface
public interface SchemaContributor {
    /**
     * Добавляет типы в реестр. Вызывается для каждого нового реестра, поэтому не должен хранить состояние
     * между вызовами и не должен строить схемы сразу: достаточно {@link Schema#define}, который ленив.
     *
     * @param schema реестр, который нужно дополнить
     */
    void contribute(Schema schema);
}

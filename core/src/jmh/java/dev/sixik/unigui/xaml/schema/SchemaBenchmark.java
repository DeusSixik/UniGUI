package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.format.XamlFormat;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Прогретые бюджеты новой схемы и парсера. Холодный старт JMH не показывает; его фиксируют отдельные
 * single-shot замеры в плане миграции.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class SchemaBenchmark {
    private Schema schema;
    private TypeInfo<Button> button;
    private PropertyInfo<? super Button, ?> width;
    private String large;

    @Setup
    @SuppressWarnings("unchecked")
    public void setup() {
        schema = Schema.builtIns();
        button = (TypeInfo<Button>) schema.type("Button").orElseThrow();
        width = button.property("width").orElseThrow();
        StringBuilder xml = new StringBuilder("<VBox spacing=\"4\" padding=\"8\">");
        for (int i = 0; i < 200; i++) {
            xml.append("<HBox spacing=\"2\" height=\"20\"><Label text=\"Row ").append(i)
                    .append("\" width=\"80\"/><Button text=\"Go\" width=\"40\" opacity=\"0.9\"/></HBox>");
        }
        large = xml.append("</VBox>").toString();
    }

    /** Регистрация всех встроенных типов: должна стоить копейки, схемы не строятся. */
    @Benchmark
    public Schema registerBuiltIns() {
        return Schema.builtIns();
    }

    /** Поиск уже построенного типа. */
    @Benchmark
    public TypeInfo<?> lookupBuiltType() {
        return schema.type("Button").orElseThrow();
    }

    /** Разбор и применение одного свойства: сердце загрузки документа. */
    @Benchmark
    public Button applyCommonProperty() {
        Button instance = button.create();
        width.apply(instance, "120", LoadContext.DEFAULT);
        return instance;
    }

    /** Разбор документа на ~400 узлов в языково-нейтральное дерево (без создания виджетов). */
    @Benchmark
    public UiDocument parseLargeXaml() {
        return XamlFormat.INSTANCE.read(large);
    }
}

package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.xaml.schema.Schema;
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
 * Бюджеты производительности XAML-подсистемы.
 *
 * <p>Измеряет прогретые сценарии: построение реестра, загрузку маленького документа и документа на
 * ~400 виджетов. Холодный старт (загрузка классов) JMH не показывает, его измеряет отдельный
 * single-shot харнес; здесь защищаемся от регрессий вида «реестр пересобирается за O(N²)».</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class XamlLoadBenchmark {
    private static final String SIMPLE =
            "<VBox spacing=\"4\"><Button text=\"Save\"/><Label text=\"Hi\"/></VBox>";

    private String large;

    @Setup
    public void setup() {
        StringBuilder xml = new StringBuilder("<VBox spacing=\"4\" padding=\"8\">");
        for (int i = 0; i < 200; i++) {
            xml.append("<HBox spacing=\"2\" height=\"20\"><Label text=\"Row ").append(i)
                    .append("\" width=\"80\"/><Button text=\"Go\" width=\"40\" opacity=\"0.9\"/></HBox>");
        }
        large = xml.append("</VBox>").toString();
        // Схема строится лениво; без прогрева первые итерации измерили бы построение типов, а не загрузку.
        Xaml.load(SIMPLE);
        Xaml.load(large);
    }

    /** Построение реестра встроенных типов (прогретое). Старый реестр строился ~574 мс. */
    @Benchmark
    public Schema buildSchema() {
        return Schema.builtIns();
    }

    /** Загрузка маленького документа. Старый загрузчик: ~23 мкс. */
    @Benchmark
    public Widget loadSimple() {
        return Xaml.load(SIMPLE);
    }

    /** Загрузка документа на ~400 виджетов. Старый загрузчик: ~1197 мкс. */
    @Benchmark
    public Widget loadLarge() {
        return Xaml.load(large);
    }
}

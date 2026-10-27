package dev.sixik.unigui.impl.widget;

import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleDefinition;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.StylePack;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.api.widget.render.WidgetRenderRegistry;
import dev.sixik.unigui.api.widget.render.WidgetRole;
import dev.sixik.unigui.widgets.interaction.Button;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Измеряет стоимость повторного разрешения renderer'а из StylePack. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class StyleRendererResolutionBenchmark {
    /** Registry id renderer'а, используемого в измерении. */
    private static final String RENDERER_ID = "benchmark:style-button";
    /** Неизменяемый набор классов для selector'а кнопки в benchmark-сценарии. */
    private static final List<String> NO_STYLE_CLASSES = List.of();

    /** StylePack, каскад которого измеряется. */
    private StylePack stylePack;
    /** Registry, в котором зарегистрирован тестовый renderer. */
    private WidgetRenderRegistry registry;
    /** Виджет с настроенным контекстом и прогретым renderer-кэшем. */
    private BenchmarkButton button;

    /** Подготавливает неизменный StylePack, реестр renderer'ов и тестовую кнопку. */
    @Setup
    public void setup() {
        WidgetRender renderer = (draw, widget) -> { };
        registry = WidgetRenderRegistry.global();
        registry.register(RENDERER_ID, WidgetRole.BUTTON, renderer);
        stylePack = StylePack.create("renderer-resolution-benchmark")
                .put(StyleDefinition.custom("benchmark.button", RENDERER_ID, Style.EMPTY)
                        .target(StyleIds.Widget.BUTTON));
        button = new BenchmarkButton();
        DefaultUIContext context = new DefaultUIContext();
        button.setUiContextInternal(context);
        context.theme(stylePack, button);
    }

    /** Измеряет быстрый путь с уже разрешённым renderer'ом в кэше виджета. */
    @Benchmark
    public WidgetRender cachedWidgetResolution() {
        return button.resolveStyleRenderer();
    }

    /** Измеряет исходное разрешение каскада и поиск renderer id на каждом вызове. */
    @Benchmark
    public WidgetRender uncachedStylePackResolution() {
        String type = StyleIds.Widget.BUTTON;
        Style themeStyle = stylePack.resolveStyleFor(type, "", NO_STYLE_CLASSES);
        Object value = themeStyle.get(StyleKeys.RENDERER, null, null);
        if (value == null || value.equals("")) {
            value = stylePack.rendererIdFor(type, "", NO_STYLE_CLASSES);
        }
        return registry.resolve(WidgetRole.BUTTON, value, null);
    }

    /** Открывает защищённое разрешение renderer'а для двух сценариев бенчмарка. */
    private static final class BenchmarkButton extends Button {
        /** Создаёт кнопку без текста для измерения только style lookup. */
        private BenchmarkButton() {
            super("");
        }

        /** Возвращает renderer, разрешённый для текущего каскада стилей. */
        private WidgetRender resolveStyleRenderer() {
            return styleRenderOverride();
        }
    }
}

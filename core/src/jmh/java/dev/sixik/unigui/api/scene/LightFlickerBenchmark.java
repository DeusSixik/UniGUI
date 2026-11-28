package dev.sixik.unigui.api.scene;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/** Measures sampling one configured flame per animation frame. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class LightFlickerBenchmark {
    /** Flicker preset reused on every sample. */
    private final LightFlicker flicker = LightFlicker.fire(42);
    /** Demo flame with a three-unit range. */
    private final PointLight base = new PointLight(0xFF9E55, 1.65f, 3);
    /** Current elapsed time in seconds, advanced once per call. */
    private float time;

    /** @return sampled light for the next frame at 60 frames per second */
    @Benchmark
    public LocalLight sampleFire() {
        time += 1f / 60;
        return flicker.sample(base, time);
    }
}

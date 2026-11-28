package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.scene.LightNode;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.SpotLight;
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

/** Measures the per-frame rigid spotlight pose and perspective projection. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class SceneLightTransformBenchmark {
    /** Spotlight with a rotated parent. */
    private LightNode spot;
    /** Reused transform output. */
    private SceneLightTransform transform;

    /** Creates the input hierarchy once outside the timing loop. */
    @Setup
    public void setup() {
        spot = new LightNode(new SpotLight(0xFFFFFF, 1, 9, 0.4f, 0.6f));
        new SceneNode().setPosition(2, 3, 4).setRotation(new MutableQuat().setYawPitch(0.8, -0.4)).addChild(spot);
        transform = new SceneLightTransform();
    }

    /** @return one perspective matrix element to keep the result observable */
    @Benchmark
    public double updateSpotTransform() {
        transform.update(spot);
        return transform.clip().get(2, 3);
    }
}

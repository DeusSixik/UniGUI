package dev.sixik.unigui.backend.minecraft_impl;

import net.minecraft.SharedConstants;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.Bootstrap;
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

/** Measures the per-frame cost of evaluating an ongoing emitter at its sprite budget. */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class SceneParticleEmitterBenchmark {
    /** Continuous source whose two-second candidate window saturates the 128 sprite budget. */
    private MinecraftParticleEmitter emitter;
    /** Simulated scene time in seconds, with a long interval since first emission. */
    private double time = 3600;

    /** Initializes the registry and builds the test emitter once per trial. */
    @Setup
    public void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        emitter = new MinecraftParticleEmitter(ParticleTypes.SMOKE, 0, Double.POSITIVE_INFINITY, 128);
    }

    /** @return sum of candidate ages to retain all schedule arithmetic in the measurement */
    @Benchmark
    public double candidateAges() {
        time = time >= 3601 ? 3600 : time + 1.0 / 60;
        emitter.setTime(time);
        int limit = emitter.emissionLimit();
        double ages = 0;
        for (int index = emitter.firstEmission(limit, 2); index < limit; index++) {
            ages += emitter.age(index);
        }
        return ages;
    }
}

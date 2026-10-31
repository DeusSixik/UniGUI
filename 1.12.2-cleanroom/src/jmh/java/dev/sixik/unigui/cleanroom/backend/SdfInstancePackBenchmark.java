package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.impl.render.shaders.SdfShapeShaderSources;
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

import java.nio.FloatBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Cost of packing 256 shapes (rounded rects, circles, lines, strokes) into the instance buffer of the SDF shader:
 * the CPU work of one typical batch before the single instanced draw call.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class SdfInstancePackBenchmark {
    private static final int COUNT = 256;
    private final DrawCommand[] commands = new DrawCommand[COUNT];
    private final float[] scratch = new float[2];
    private FloatBuffer buffer;

    @Setup
    public void setup() {
        MutableColor color = new MutableColor(0.2f, 0.4f, 0.8f, 1.0f);
        for (int i = 0; i < COUNT; i++) {
            MutableRect bounds = new MutableRect(i * 3.0f, i * 2.0f, 40.0f, 20.0f);
            commands[i] = switch (i % 4) {
                case 0 -> new DrawCommand(DrawCommandType.ROUNDED_RECT).bounds(bounds).paint(Paint.fill(color)).radius(6.0f);
                case 1 -> DrawCommand.circle(bounds, Paint.fill(color));
                case 2 -> DrawCommand.line(bounds, Paint.stroke(color, 2.0f));
                default -> new DrawCommand(DrawCommandType.ROUNDED_RECT).bounds(bounds).paint(Paint.stroke(color, 1.5f)).radius(4.0f);
            };
        }
        buffer = FloatBuffer.allocate(COUNT * SdfShapeShaderSources.INSTANCE_FLOATS);
    }

    @Benchmark
    public int pack() {
        buffer.clear();
        int packed = 0;
        for (int i = 0; i < COUNT; i++) {
            if (CleanroomSdfShapeInstances.pack(buffer, commands[i], 1.0f, scratch)) packed++;
        }
        buffer.flip();
        return packed;
    }
}
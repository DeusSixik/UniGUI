package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.VectorPath;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Cost of turning one draw command into triangles on the CPU: the fallback path that runs whenever the SDF shaders
 * are unavailable, and the only path for paths, meshes and textures. Run with {@code ./gradlew jmh}; the GC
 * profiler ({@code -Pjmh.profilers=gc}) must report about 0 B/op.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class ShapeTessellatorBenchmark {
    private static final MutableColor WHITE = new MutableColor(1, 1, 1, 1);

    @Param({"rect", "rectStroke", "rounded", "circle", "line", "pathFill", "pathStroke"})
    public String shape;

    private final CleanroomShapeTessellator tessellator = new CleanroomShapeTessellator();
    private final CleanroomVertexList out = new CleanroomVertexList();
    private DrawCommand command;

    @Setup
    public void setup() {
        MutableRect bounds = new MutableRect(0, 0, 40, 24);
        VectorPath path = new VectorPath().moveTo(0, 0).lineTo(30, 0).quadraticTo(40, 10, 30, 20)
                .cubicTo(20, 30, 10, 30, 0, 20).close();
        command = switch (shape) {
            case "rect" -> DrawCommand.rect(bounds, Paint.fill(WHITE));
            case "rectStroke" -> DrawCommand.rect(bounds, Paint.stroke(WHITE, 2.0f));
            case "rounded" -> new DrawCommand(DrawCommandType.ROUNDED_RECT).bounds(bounds)
                    .paint(Paint.fill(WHITE)).radius(6.0f);
            case "circle" -> DrawCommand.circle(bounds, Paint.fill(WHITE)).segments(24);
            case "line" -> DrawCommand.line(bounds, Paint.stroke(WHITE, 3.0f));
            case "pathFill" -> DrawCommand.path(path, bounds, Paint.fill(WHITE));
            case "pathStroke" -> DrawCommand.path(path, bounds, Paint.stroke(WHITE, 2.0f));
            default -> throw new IllegalArgumentException(shape);
        };
    }

    @Benchmark
    public int tessellate() {
        out.clear();
        tessellator.begin(out, 2.0f);
        return tessellator.append(command, 1.0f);
    }
}
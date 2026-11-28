package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.SceneNode;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
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
import java.util.function.Consumer;

/** Compares native model tessellation to reusing the same posed mesh on the CPU. */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
public class SceneNativeMeshesBenchmark {
    /** Scene node rendered on every iteration. */
    private final SceneNode node = new SceneNode();
    /** Identity pose of the node-local submission. */
    private final PoseStack pose = new PoseStack();
    /** Native six-part model comparable in size to a basic humanoid. */
    private Model<Void> model;
    /** Standard vanilla layer used by the model. */
    private RenderType type;
    /** Reused command writer for the cached path. */
    private Consumer<SubmitNodeStorage> submit;
    /** Actual scene cache with a CPU-only texture binding. */
    private SceneNativeMeshes cache;

    /** Prepares the immutable model and primes its cached entry. */
    @Setup
    public void setup() {
        MeshDefinition mesh = new MeshDefinition();
        for (int i = 0; i < 6; i++) {
            mesh.getRoot().addOrReplaceChild("part" + i, CubeListBuilder.create().addBox(0, 0, 0, 4, 8, 4),
                    PartPose.offset(i, 0, 0));
        }
        model = new Model<>(LayerDefinition.create(mesh, 32, 32).bakeRoot(), RenderTypes::entityCutout) {};
        type = model.renderType(Identifier.withDefaultNamespace("textures/entity/benchmark.png"));
        submit = storage -> storage.order(0).solid.submit(
                new ModelFeatureRenderer.Submit<>(type, pose.last().copy(), model, null, 0, 0, 0xFFFFFFFF, null, null));
        var view = new OpenView();
        var binding = new UiTextureBinding(view, null, false, false, TextureOptions.defaults(), view);
        cache = new SceneNativeMeshes(mesh3D -> {}, (layer, style) -> new SceneNativeLayer(binding, null, style));
        cache.bake(node, submit);
    }

    /** @return the local mesh built afresh from the native model on each invocation */
    @Benchmark
    public Mesh3D rebuildVertices() {
        model.setupAnim(null);
        var vertices = new SceneBlockMeshes.MeshBuilder(0, 0, 0);
        model.renderToBuffer(pose, vertices, 0, 0, 0xFFFFFFFF);
        return vertices.build();
    }

    /** @return the retained mesh after submitting the model and comparing its pose */
    @Benchmark
    public Mesh3D reuseVertices() {
        return cache.bake(node, submit).getFirst().mesh();
    }

    /** Test view that keeps the texture validity check independent of a GPU. */
    private static final class OpenView implements GpuTextureView {
        /** {@inheritDoc} */
        @Override
        public boolean isClosed() { return false; }
        /** {@inheritDoc} */
        @Override
        public GpuTexture texture() { return null; }
        /** {@inheritDoc} */
        @Override
        public int baseMipLevel() { return 0; }
        /** {@inheritDoc} */
        @Override
        public int mipLevels() { return 1; }
        /** {@inheritDoc} */
        @Override
        public int getWidth(int level) { return 1; }
        /** {@inheritDoc} */
        @Override
        public int getHeight(int level) { return 1; }
        /** {@inheritDoc} */
        @Override
        public void close() {}
    }
}

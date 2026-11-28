package dev.sixik.unigui.backend.minecraft_impl;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.LightCoordsUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests capture of the native particle's tick endpoints without a client world or GPU. */
final class SceneParticlesTest {
    /** Exactly representable test coordinates stay within a float ULP of one block. */
    private static final float EPSILON = Math.ulp(1f);

    /** Native moves, lifetime and sprite data survive the conversion to seekable frames. */
    @Test
    void sample_linearParticle_capturesStartAndEachTickEndpoint() {
        var frames = SceneParticles.sample(new LinearParticle(), new Camera(), new SceneParticles.Capture());
        assertEquals(4, frames.length);
        assertEquals(1, frames[0].x(), EPSILON);
        assertEquals(1.25f, frames[1].x(), EPSILON);
        assertEquals(1.75f, frames[3].x(), EPSILON);
        assertEquals(2, frames[3].y(), EPSILON);
        assertEquals(3, frames[3].z(), EPSILON);
        assertEquals(0.5f, frames[1].size(), EPSILON);
        assertEquals(0.75f, frames[1].u1(), EPSILON);
        assertEquals(SingleQuadParticle.Layer.OPAQUE, frames[1].layer());
    }

    /** A constant-velocity sprite with a known lifetime and UV rectangle. */
    private static final class LinearParticle extends SingleQuadParticle {
        /** Starts at (1, 2, 3) and advances one quarter of a block along X each native tick. */
        private LinearParticle() {
            super(null, 1, 2, 3, null);
            setLifetime(3);
            setParticleSpeed(0.25, 0, 0);
            friction = 1;
            hasPhysics = false;
            quadSize = 0.5f;
        }

        /** {@inheritDoc} */
        @Override
        protected Layer getLayer() { return Layer.OPAQUE; }

        /** {@inheritDoc} */
        @Override
        protected int getLightCoords(float partial) { return LightCoordsUtil.FULL_BRIGHT; }

        /** {@inheritDoc} */
        @Override
        protected float getU0() { return 0.25f; }

        /** {@inheritDoc} */
        @Override
        protected float getU1() { return 0.75f; }

        /** {@inheritDoc} */
        @Override
        protected float getV0() { return 0.25f; }

        /** {@inheritDoc} */
        @Override
        protected float getV1() { return 0.75f; }
    }
}

package dev.sixik.unigui.api.math;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class RayTest {
    // Входы дают целые расстояния или небольшие числа с вычислением в double.
    private static final double EPS = 1e-12;

    @ParameterizedTest
    @CsvSource({"0,0,0,-1,2", "0,0,-2.5,-1,0", "1,1,0,-1,2",
            "2,0,0,-1,Infinity", "0,0,0,1,Infinity", "0,0,-5,1,2"})
    void intersectAabb_axesAndBoundaries_returnsFirstNonnegativeDistance(float x, float y, float z,
                                                                        double dz, double expected) {
        Ray ray = new Ray();
        assertTrue(ray.set(new MutableVec3(x, y, z), 0, 0, dz));
        assertEquals(expected, ray.intersectAabb(new MutableVec3(-1, -1, -3), new MutableVec3(1, 1, -2)), EPS);
    }

    @Test
    void intersectAabb_disjointSlabIntervals_misses() {
        Ray ray = new Ray();
        assertTrue(ray.set(new MutableVec3(), 1, 0, -1));
        assertEquals(Double.POSITIVE_INFINITY,
                ray.intersectAabb(new MutableVec3(0, -1, -4), new MutableVec3(1, 1, -3)), EPS);
    }

    @Test
    void intersectTriangle_backfaceAndSharedEdge_areIncluded() {
        MutableVec3 a = new MutableVec3(-1, -1, -3);
        MutableVec3 b = new MutableVec3(1, -1, -3);
        MutableVec3 c = new MutableVec3(-1, 1, -3);
        Ray ray = new Ray();

        assertEquals(3, ray.intersectTriangle(a, b, c), EPS);
        assertEquals(3, ray.intersectTriangle(c, b, a), EPS);
        assertEquals(Double.POSITIVE_INFINITY, ray.intersectTriangle(a, b, b), EPS);

        assertTrue(ray.set(new MutableVec3(0.5f, 0.5f, 0), 0, 0, -1));
        assertEquals(Double.POSITIVE_INFINITY, ray.intersectTriangle(a, b, c), EPS);
        assertTrue(ray.set(new MutableVec3(), 0, 0, 1));
        assertEquals(Double.POSITIVE_INFINITY, ray.intersectTriangle(a, b, c), EPS);
    }

    @Test
    void intersectTriangle_tinyTriangle_remainsPickable() {
        float s = Float.MIN_VALUE;
        assertEquals(1, new Ray().intersectTriangle(new MutableVec3(-s, -s, -1),
                new MutableVec3(s, -s, -1), new MutableVec3(0, s, -1)), EPS);
    }

    @Test
    void intersectPlane_distanceUsesNormalizedDirectionAndArbitraryNormal() {
        Ray ray = new Ray();
        assertTrue(ray.set(new MutableVec3(0, 0, 2), 0, 0, -Double.MAX_VALUE));

        assertEquals(2, ray.intersectPlane(new MutableVec3(), new MutableVec3(0, 0, 5)), EPS);
        assertEquals(Double.POSITIVE_INFINITY, ray.intersectPlane(new MutableVec3(), new MutableVec3(0, 1, 0)), EPS);
        assertEquals(Double.POSITIVE_INFINITY, ray.intersectPlane(new MutableVec3(0, 0, 3),
                new MutableVec3(0, 0, 1)), EPS);
    }

    @Test
    void set_invalidArguments_preservesRay() {
        Ray ray = new Ray();
        assertFalse(ray.set(new MutableVec3(), 0, 0, 0));
        assertFalse(ray.set(new MutableVec3(), 1, Double.NaN, 0));
        assertFalse(ray.set(new MutableVec3(Float.POSITIVE_INFINITY, 0, 0), 1, 0, 0));
        MutableVec3 point = ray.pointAt(2, new MutableVec3());
        assertEquals(-2.0f, point.z(), (float) EPS);
    }
}

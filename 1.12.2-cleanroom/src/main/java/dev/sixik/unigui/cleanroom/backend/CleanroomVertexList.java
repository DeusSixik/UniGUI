package dev.sixik.unigui.cleanroom.backend;

import java.util.Arrays;

/**
 * Growable triangle-list vertex storage filled by the CPU tessellators and drained by the GL renderer.
 *
 * <p>Struct-of-arrays on purpose: positions, texture coordinates and colors live in primitive arrays that are reused
 * between frames, so tessellating a batch allocates nothing once the arrays have grown to their working size.</p>
 */
final class CleanroomVertexList {
    private float[] x = new float[256];
    private float[] y = new float[256];
    private float[] u = new float[256];
    private float[] v = new float[256];
    private int[] argb = new int[256];
    private int count;

    /** Removes all vertices, keeping the capacity. */
    void clear() {
        count = 0;
    }

    /** @return number of vertices */
    int size() {
        return count;
    }

    /** Appends an untextured vertex. */
    void add(float px, float py, int color) {
        add(px, py, 0.0f, 0.0f, color);
    }

    /** Appends a vertex with texture coordinates. */
    void add(float px, float py, float pu, float pv, int color) {
        if (count == x.length) grow();
        x[count] = px;
        y[count] = py;
        u[count] = pu;
        v[count] = pv;
        argb[count] = color;
        count++;
    }

    float x(int index) {
        return x[index];
    }

    float y(int index) {
        return y[index];
    }

    float u(int index) {
        return u[index];
    }

    float v(int index) {
        return v[index];
    }

    int argb(int index) {
        return argb[index];
    }

    private void grow() {
        int capacity = x.length * 2;
        x = Arrays.copyOf(x, capacity);
        y = Arrays.copyOf(y, capacity);
        u = Arrays.copyOf(u, capacity);
        v = Arrays.copyOf(v, capacity);
        argb = Arrays.copyOf(argb, capacity);
    }
}

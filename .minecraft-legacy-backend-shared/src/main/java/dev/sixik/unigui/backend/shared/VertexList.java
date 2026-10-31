package dev.sixik.unigui.backend.shared;

import java.util.Arrays;

/** Reusable struct-of-arrays storage for CPU-tessellated triangle vertices. */
public class VertexList {
    private float[] x = new float[256];
    private float[] y = new float[256];
    private float[] u = new float[256];
    private float[] v = new float[256];
    private int[] argb = new int[256];
    private int count;

    public void clear() { count = 0; }
    public int size() { return count; }
    public void add(float px, float py, int color) { add(px, py, 0.0f, 0.0f, color); }

    public void add(float px, float py, float pu, float pv, int color) {
        if (count == x.length) grow();
        x[count] = px;
        y[count] = py;
        u[count] = pu;
        v[count] = pv;
        argb[count] = color;
        count++;
    }

    public float x(int index) { return x[index]; }
    public float y(int index) { return y[index]; }
    public float u(int index) { return u[index]; }
    public float v(int index) { return v[index]; }
    public int argb(int index) { return argb[index]; }

    private void grow() {
        int capacity = x.length * 2;
        x = Arrays.copyOf(x, capacity);
        y = Arrays.copyOf(y, capacity);
        u = Arrays.copyOf(u, capacity);
        v = Arrays.copyOf(v, capacity);
        argb = Arrays.copyOf(argb, capacity);
    }
}

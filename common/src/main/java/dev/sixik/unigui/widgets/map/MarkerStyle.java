package dev.sixik.unigui.widgets.map;

import dev.sixik.unigui.api.math.MutableColor;

public enum MarkerStyle {
    DEFAULT(72.0f, 22.0f,
            0.075f, 0.095f, 0.130f, 0.92f,
            0.58f, 0.72f, 0.95f, 0.92f,
            0.92f, 0.96f, 1.00f, 1.00f),
    CAMP(72.0f, 22.0f,
            0.060f, 0.130f, 0.110f, 0.94f,
            0.42f, 0.88f, 0.70f, 0.95f,
            0.88f, 1.00f, 0.95f, 1.00f),
    VAULT(74.0f, 22.0f,
            0.100f, 0.085f, 0.145f, 0.94f,
            0.74f, 0.62f, 1.00f, 0.95f,
            0.96f, 0.92f, 1.00f, 1.00f),
    FORGE(76.0f, 22.0f,
            0.145f, 0.078f, 0.045f, 0.94f,
            1.00f, 0.62f, 0.32f, 0.95f,
            1.00f, 0.93f, 0.84f, 1.00f),
    QUEST(78.0f, 22.0f,
            0.140f, 0.120f, 0.045f, 0.94f,
            1.00f, 0.86f, 0.35f, 0.95f,
            1.00f, 0.98f, 0.86f, 1.00f),
    PLAYER(72.0f, 22.0f,
            0.045f, 0.105f, 0.150f, 0.94f,
            0.35f, 0.88f, 1.00f, 0.95f,
            0.86f, 0.98f, 1.00f, 1.00f);

    private final float width;
    private final float height;
    private final float br;
    private final float bg;
    private final float bb;
    private final float ba;
    private final float rr;
    private final float rg;
    private final float rb;
    private final float ra;
    private final float tr;
    private final float tg;
    private final float tb;
    private final float ta;

    MarkerStyle(float width, float height,
                float br, float bg, float bb, float ba,
                float rr, float rg, float rb, float ra,
                float tr, float tg, float tb, float ta) {
        this.width = width;
        this.height = height;
        this.br = br;
        this.bg = bg;
        this.bb = bb;
        this.ba = ba;
        this.rr = rr;
        this.rg = rg;
        this.rb = rb;
        this.ra = ra;
        this.tr = tr;
        this.tg = tg;
        this.tb = tb;
        this.ta = ta;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public MutableColor backgroundColor() {
        return new MutableColor(br, bg, bb, ba);
    }

    public MutableColor ringColor() {
        return new MutableColor(rr, rg, rb, ra);
    }

    public MutableColor textColor() {
        return new MutableColor(tr, tg, tb, ta);
    }
}

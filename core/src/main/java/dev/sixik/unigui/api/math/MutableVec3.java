package dev.sixik.unigui.api.math;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Mutable 3D vector suitable for reuse as an output parameter.
 */
@WikiPage(
        title = "MutableVec3", slug = "mutable-vec-3",
        category = "API/3D Scene", order = 280,
        summary = "Reusable three-component vector for scene data and output parameters.")
@WikiSection(title = "Reusing vectors", order = 10, markdown = """
        Allocate a vector once to read scene positions or reuse it as the result of `Camera3D.project`. `setCross(a,
        b)` uses the right-handed cross product and can write over either input.
        """)
@WikiExample(title = "Copy the position of a node", code = """
        MutableVec3 position = node.position(new MutableVec3());
        """)
@WikiLink(label = "Vec3View", target = "page:vec-3-view")
@WikiLink(label = "Camera3D", target = "page:camera-3d")
@WikiLink(label = "Ray", target = "page:scene-ray")
@WikiLink(label = "SceneNode", target = "page:scene-node")
@AiDoc(
        summary = "Reusable three-component vector for scene data and output parameters.",
        purpose = "Store positions, directions, scale values or projected points.",
        criticalRules = {
                "Operations mutate the vector. Each API determines the units, such as scene units, a unit direction, or logical canvas coordinates.",
                "`normalize()` returns false and keeps the old components if the length is zero or nonfinite."
        },
        related = {"Vec3View", "Camera3D", "Ray", "SceneNode"},
        examples = @AiDoc.Example(title = "Copy the position of a node", code = """
                MutableVec3 position = node.position(new MutableVec3());
                """))
public final class MutableVec3 implements Vec3View {
    /**
     * X component in vector units.
     */
    private float x;
    /**
     * Y component in vector units.
     */
    private float y;
    /**
     * Z component in vector units.
     */
    private float z;

    /**
     * Creates a zero vector.
     */
    @WikiMethod(section = "Vector", sectionOrder = 10, order = 10,
            summary = "Creates a zero vector.")
    @AiDoc(section = "Vector", sectionOrder = 10, order = 10,
            summary = "Creates a zero vector.")
    public MutableVec3() {
    }

    /**
     * Creates a vector from components.
     *
     * @param x X component.
     * @param y Y component.
     * @param z Z component.
     */
    @WikiMethod(section = "Vector", sectionOrder = 10, order = 10,
            summary = "Creates a vector from components.")
    @AiDoc(section = "Vector", sectionOrder = 10, order = 10,
            summary = "Creates a vector from components.")
    public MutableVec3(
            @WikiParameter(description = "X component.") float x,
            @WikiParameter(description = "Y component.") float y,
            @WikiParameter(description = "Z component.") float z) {
        set(x, y, z);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float x() {
        return x;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float y() {
        return y;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float z() {
        return z;
    }

    /**
     * Sets the vector components.
     *
     * @param x X component.
     * @param y Y component.
     * @param z Z component.
     * @return this vector.
     */
    @WikiMethod(section = "Vector", sectionOrder = 10, order = 20,
            summary = "Sets the vector components.")
    @WikiReturn("this vector")
    @AiDoc(section = "Vector", sectionOrder = 10, order = 20,
            summary = "Sets the vector components.")
    public MutableVec3 set(
            @WikiParameter(description = "X component.") float x,
            @WikiParameter(description = "Y component.") float y,
            @WikiParameter(description = "Z component.") float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /**
     * Copies vector components.
     *
     * @param source input vector.
     * @return this vector.
     */
    @WikiMethod(section = "Vector", sectionOrder = 10, order = 20,
            summary = "Copies vector components.")
    @WikiReturn("this vector")
    @AiDoc(section = "Vector", sectionOrder = 10, order = 20,
            summary = "Copies vector components.")
    public MutableVec3 set(
            @WikiParameter(description = "input vector.") Vec3View source) {
        return set(source.x(), source.y(), source.z());
    }

    /**
     * Adds offsets in the same units as the vector.
     *
     * @param x offset along X.
     * @param y offset along Y.
     * @param z offset along Z.
     * @return this vector.
     */
    @WikiMethod(section = "Vector", sectionOrder = 10, order = 30,
            summary = "Adds offsets in the same units as the vector.")
    @WikiReturn("this vector")
    @AiDoc(section = "Vector", sectionOrder = 10, order = 30,
            summary = "Adds offsets in the same units as the vector.")
    public MutableVec3 add(
            @WikiParameter(description = "offset along X.") float x,
            @WikiParameter(description = "offset along Y.") float y,
            @WikiParameter(description = "offset along Z.") float z) {
        return set(this.x + x, this.y + y, this.z + z);
    }

    /**
     * Stores the cross product a × b. The X component is {@code a.y * b.z - a.z * b.y}.
     * Compute Y and Z by cycling the axes.
     *
     * @param a first vector, may alias this vector.
     * @param b second vector, may alias this vector.
     * @return this vector.
     */
    @WikiMethod(section = "Vector", sectionOrder = 10, order = 40,
            summary = "Stores the cross product a × b. The X component is `a.y * b.z - a.z * b.y`. Compute Y and Z by cycling the axes.")
    @WikiReturn("this vector")
    @AiDoc(section = "Vector", sectionOrder = 10, order = 40,
            summary = "Stores the cross product a × b. The X component is `a.y * b.z - a.z * b.y`. Compute Y and Z by cycling the axes.")
    public MutableVec3 setCross(
            @WikiParameter(description = "first vector, may alias this vector.") Vec3View a,
            @WikiParameter(description = "second vector, may alias this vector.") Vec3View b) {
        return set(
                (float) ((double) a.y() * b.z() - (double) a.z() * b.y()),
                (float) ((double) a.z() * b.x() - (double) a.x() * b.z()),
                (float) ((double) a.x() * b.y() - (double) a.y() * b.x()));
    }

    /**
     * Divides the components by their length {@code sqrt(x*x + y*y + z*z)}.
     *
     * @return true after normalization. A zero or nonfinite vector is left unchanged.
     */
    @WikiMethod(section = "Vector", sectionOrder = 10, order = 50,
            summary = "Divides the components by their length `sqrt(x*x + y*y + z*z)`.")
    @WikiReturn("true after normalization. A zero or nonfinite vector is left unchanged")
    @AiDoc(section = "Vector", sectionOrder = 10, order = 50,
            summary = "Divides the components by their length `sqrt(x*x + y*y + z*z)`.")
    public boolean normalize() {
        double length = Math.sqrt((double) x * x + (double) y * y + (double) z * z);
        if (!(length > 0.0) || !Double.isFinite(length)) return false;
        set((float) (x / length), (float) (y / length), (float) (z / length));
        return true;
    }
}

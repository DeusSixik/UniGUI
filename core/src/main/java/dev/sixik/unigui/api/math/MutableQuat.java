package dev.sixik.unigui.api.math;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;

/**
 * Mutable unit quaternion representing an active rotation in a right-handed coordinate system.
 */
@WikiPage(
        title = "MutableQuat", slug = "mutable-quat",
        category = "API/3D Scene", order = 290,
        summary = "Reusable unit quaternion for active 3D rotations.")
@WikiSection(title = "Composition", order = 10, markdown = """
        `setYawPitch(yaw, pitch)` composes yaw around Y with pitch around local X. `setAxisAngle(axis, angle)`
        normalizes the axis. `rotate(...)` writes the rotated vector to your output.
        """)
@WikiExample(title = "Turn a node about its vertical axis", code = """
        MutableQuat turn = new MutableQuat().setAxisAngle(new MutableVec3(0, 1, 0), Math.PI / 4);
        node.setRotation(turn);
        """)
@WikiLink(label = "QuatView", target = "page:quat-view")
@WikiLink(label = "MutableVec3", target = "page:mutable-vec-3")
@WikiLink(label = "MutableMat4", target = "page:mutable-mat-4")
@WikiLink(label = "SceneNode", target = "page:scene-node")
@AiDoc(
        summary = "Reusable unit quaternion for active 3D rotations.",
        purpose = "Rotate scene nodes and cameras by axis-angle, yaw and pitch or composed rotations.",
        criticalRules = {
                "Angles are in radians. Axis-angle requires a finite nonzero axis.",
                "`setProduct(a, b)` applies b first and a second. Either input may alias the output."
        },
        related = {"QuatView", "MutableVec3", "MutableMat4", "SceneNode"},
        examples = @AiDoc.Example(title = "Turn a node about its vertical axis", code = """
                MutableQuat turn = new MutableQuat().setAxisAngle(new MutableVec3(0, 1, 0), Math.PI / 4);
                node.setRotation(turn);
                """))
public final class MutableQuat implements QuatView {
    /**
     * Dimensionless X vector component.
     */
    private float x;
    /**
     * Dimensionless Y vector component.
     */
    private float y;
    /**
     * Dimensionless Z vector component.
     */
    private float z;
    /**
     * Dimensionless scalar component.
     */
    private float w = 1.0f;

    /**
     * Creates the identity rotation.
     */
    @WikiMethod(section = "Quaternion", sectionOrder = 10, order = 10,
            summary = "Creates the identity rotation.")
    @AiDoc(section = "Quaternion", sectionOrder = 10, order = 10,
            summary = "Creates the identity rotation.")
    public MutableQuat() {
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
     * {@inheritDoc}
     */
    @Override
    public float w() {
        return w;
    }

    /**
     * Sets the components after dividing by {@code sqrt(x*x + y*y + z*z + w*w)}.
     *
     * @param x X vector component.
     * @param y Y vector component.
     * @param z Z vector component.
     * @param w scalar component.
     * @return this quaternion.
     * @throws IllegalArgumentException if the length is zero or nonfinite.
     */
    @WikiMethod(section = "Quaternion", sectionOrder = 10, order = 20,
            summary = "Sets the components after dividing by `sqrt(x*x + y*y + z*z + w*w)`.")
    @WikiReturn("this quaternion")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the length is zero or nonfinite.")
    @AiDoc(section = "Quaternion", sectionOrder = 10, order = 20,
            summary = "Sets the components after dividing by `sqrt(x*x + y*y + z*z + w*w)`.")
    public MutableQuat set(
            @WikiParameter(description = "X vector component.") float x,
            @WikiParameter(description = "Y vector component.") float y,
            @WikiParameter(description = "Z vector component.") float z,
            @WikiParameter(description = "scalar component.") float w) {
        double length = Math.sqrt((double) x * x + (double) y * y + (double) z * z + (double) w * w);
        if (!(length > 0.0) || !Double.isFinite(length)) {
            throw new IllegalArgumentException("Quaternion must have a finite nonzero length");
        }
        this.x = (float) (x / length);
        this.y = (float) (y / length);
        this.z = (float) (z / length);
        this.w = (float) (w / length);
        return this;
    }

    /**
     * Copies a unit quaternion.
     *
     * @param source input rotation.
     * @return this quaternion.
     */
    @WikiMethod(section = "Quaternion", sectionOrder = 10, order = 20,
            summary = "Copies a unit quaternion.")
    @WikiReturn("this quaternion")
    @AiDoc(section = "Quaternion", sectionOrder = 10, order = 20,
            summary = "Copies a unit quaternion.")
    public MutableQuat set(
            @WikiParameter(description = "input rotation.") QuatView source) {
        x = source.x();
        y = source.y();
        z = source.z();
        w = source.w();
        return this;
    }

    /**
     * Sets {@code q = (axis / |axis| * sin(angle/2), cos(angle/2))}.
     *
     * @param axis finite nonzero rotation axis.
     * @param angle finite angle in radians.
     * @return this quaternion.
     * @throws IllegalArgumentException if the axis is zero or an argument is nonfinite.
     */
    @WikiMethod(section = "Quaternion", sectionOrder = 10, order = 30,
            summary = "Sets `q = (axis / |axis| * sin(angle/2), cos(angle/2))`.")
    @WikiReturn("this quaternion")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the axis is zero or an argument is nonfinite.")
    @AiDoc(section = "Quaternion", sectionOrder = 10, order = 30,
            summary = "Sets `q = (axis / |axis| * sin(angle/2), cos(angle/2))`.")
    public MutableQuat setAxisAngle(
            @WikiParameter(description = "finite nonzero rotation axis.") Vec3View axis,
            @WikiParameter(description = "finite angle in radians.") double angle) {
        double ax = axis.x();
        double ay = axis.y();
        double az = axis.z();
        double length = Math.sqrt(ax * ax + ay * ay + az * az);
        if (!(length > 0.0) || !Double.isFinite(length) || !Double.isFinite(angle)) {
            throw new IllegalArgumentException("Rotation requires a finite nonzero axis and a finite angle");
        }
        double scale = Math.sin(angle * 0.5) / length;
        return set((float) (ax * scale), (float) (ay * scale), (float) (az * scale),
                (float) Math.cos(angle * 0.5));
    }

    /**
     * Sets {@code qY(yaw) * qX(pitch)}, combining rotations about Y and X.
     *
     * @param yaw finite angle about world Y in radians.
     * @param pitch finite angle about local X in radians.
     * @return this quaternion.
     * @throws IllegalArgumentException if either angle is nonfinite.
     */
    @WikiMethod(section = "Quaternion", sectionOrder = 10, order = 40,
            summary = "Sets `qY(yaw) * qX(pitch)`, combining rotations about Y and X.")
    @WikiReturn("this quaternion")
    @WikiThrows(type = IllegalArgumentException.class, description = "if either angle is nonfinite.")
    @AiDoc(section = "Quaternion", sectionOrder = 10, order = 40,
            summary = "Sets `qY(yaw) * qX(pitch)`, combining rotations about Y and X.")
    public MutableQuat setYawPitch(
            @WikiParameter(description = "finite angle about world Y in radians.") double yaw,
            @WikiParameter(description = "finite angle about local X in radians.") double pitch) {
        double sy = Math.sin(yaw * 0.5);
        double cy = Math.cos(yaw * 0.5);
        double sx = Math.sin(pitch * 0.5);
        double cx = Math.cos(pitch * 0.5);
        return set((float) (cy * sx), (float) (sy * cx), (float) (-sy * sx), (float) (cy * cx));
    }

    /**
     * Stores the Hamilton product {@code a * b}, which applies b before a. Its vector part is
     * {@code a.w*b.xyz + b.w*a.xyz + a.xyz × b.xyz} and its scalar part is {@code a.w*b.w - dot(a.xyz, b.xyz)}.
     *
     * @param a outer rotation, may alias this quaternion.
     * @param b inner rotation, may alias this quaternion.
     * @return this quaternion.
     */
    @WikiMethod(section = "Quaternion", sectionOrder = 10, order = 50,
            summary = "Stores the Hamilton product `a * b`, which applies b before a. Its vector part is `a.w*b.xyz + b.w*a.xyz + a.xyz × b.xyz` and its scalar part is `a.w*b.w - dot(a.xyz, b.xyz)`.")
    @WikiReturn("this quaternion")
    @AiDoc(section = "Quaternion", sectionOrder = 10, order = 50,
            summary = "Stores the Hamilton product `a * b`, which applies b before a. Its vector part is `a.w*b.xyz + b.w*a.xyz + a.xyz × b.xyz` and its scalar part is `a.w*b.w - dot(a.xyz, b.xyz)`.")
    public MutableQuat setProduct(
            @WikiParameter(description = "outer rotation, may alias this quaternion.") QuatView a,
            @WikiParameter(description = "inner rotation, may alias this quaternion.") QuatView b) {
        double ax = a.x(), ay = a.y(), az = a.z(), aw = a.w();
        double bx = b.x(), by = b.y(), bz = b.z(), bw = b.w();
        return set((float) (aw * bx + bw * ax + ay * bz - az * by),
                (float) (aw * by + bw * ay + az * bx - ax * bz),
                (float) (aw * bz + bw * az + ax * by - ay * bx),
                (float) (aw * bw - ax * bx - ay * by - az * bz));
    }

    /**
     * Rotates v according to {@code v + w*t + q.xyz × t}, where {@code t = 2*(q.xyz × v)/|q|²}.
     * The squared length compensates for rounding of the stored quaternion components.
     *
     * @param vx X component of input v.
     * @param vy Y component of input v.
     * @param vz Z component of input v.
     * @param out reusable result vector in the units of v.
     * @return out.
     */
    @WikiMethod(section = "Quaternion", sectionOrder = 10, order = 60,
            summary = "Rotates v according to `v + w*t + q.xyz × t`, where `t = 2*(q.xyz × v)/|q|²`. The squared length compensates for rounding of the stored quaternion components.")
    @WikiReturn("out")
    @AiDoc(section = "Quaternion", sectionOrder = 10, order = 60,
            summary = "Rotates v according to `v + w*t + q.xyz × t`, where `t = 2*(q.xyz × v)/|q|²`. The squared length compensates for rounding of the stored quaternion components.")
    public MutableVec3 rotate(
            @WikiParameter(description = "X component of input v.") float vx,
            @WikiParameter(description = "Y component of input v.") float vy,
            @WikiParameter(description = "Z component of input v.") float vz,
            @WikiParameter(description = "reusable result vector in the units of v.") MutableVec3 out) {
        double factor = 2.0 / ((double) x * x + (double) y * y + (double) z * z + (double) w * w);
        double tx = factor * ((double) y * vz - (double) z * vy);
        double ty = factor * ((double) z * vx - (double) x * vz);
        double tz = factor * ((double) x * vy - (double) y * vx);
        return out.set((float) (vx + w * tx + y * tz - z * ty),
                (float) (vy + w * ty + z * tx - x * tz),
                (float) (vz + w * tz + x * ty - y * tx));
    }
}

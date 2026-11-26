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
 * Mutable ray {@code origin + t * direction} for {@code t >= 0}. The direction has unit length,
 * so t is a distance in scene units. Intersection methods return {@link Double#POSITIVE_INFINITY} for a miss.
 */
@WikiPage(
        title = "Ray", slug = "scene-ray",
        category = "API/3D Scene", order = 240,
        summary = "Normalized 3D ray with AABB, triangle and plane intersections.")
@WikiSection(title = "Ray parameter", order = 10, markdown = """
        The ray is `origin + t * direction` for `t >= 0`. Direction is normalized. The AABB test returns zero for an
        origin on or inside the box. The triangle test accepts either face.
        """)
@WikiExample(title = "Intersect the ground plane at Y = 0", code = """
        Ray ray = new Ray();
        if (camera.screenRay(canvasX, canvasY, ray)) {
            double distance = ray.intersectPlane(new MutableVec3(), new MutableVec3(0, 1, 0));
            if (Double.isFinite(distance)) {
                MutableVec3 point = ray.pointAt(distance, new MutableVec3());
            }
        }
        """)
@WikiLink(label = "Camera3D", target = "page:camera-3d")
@WikiLink(label = "ScenePicker", target = "page:scene-picker")
@WikiLink(label = "Aabb", target = "page:aabb")
@WikiLink(label = "MutableVec3", target = "page:mutable-vec-3")
@AiDoc(
        summary = "Normalized 3D ray with AABB, triangle and plane intersections.",
        purpose = "Implement geometric picking and viewport manipulation tools.",
        criticalRules = {
                "A hit parameter is a distance in the ray's coordinate space. Positive infinity signals a miss.",
                "`set` returns false and retains the previous ray when the direction is zero or invalid.",
                "Camera3D.screenRay starts on the near plane. Use ScenePicker to enforce both clipping planes in picking."
        },
        related = {"Camera3D", "ScenePicker", "Aabb", "MutableVec3"},
        examples = @AiDoc.Example(title = "Intersect the ground plane at Y = 0", code = """
                Ray ray = new Ray();
                if (camera.screenRay(canvasX, canvasY, ray)) {
                    double distance = ray.intersectPlane(new MutableVec3(), new MutableVec3(0, 1, 0));
                    if (Double.isFinite(distance)) {
                        MutableVec3 point = ray.pointAt(distance, new MutableVec3());
                    }
                }
                """))
public final class Ray {
    /**
     * Ray origin in scene units.
     */
    private final MutableVec3 origin = new MutableVec3();
    /**
     * Dimensionless X component of the unit direction.
     */
    private double dx;
    /**
     * Dimensionless Y component of the unit direction.
     */
    private double dy;
    /**
     * Dimensionless Z component of the unit direction.
     */
    private double dz = -1.0;

    /**
     * Creates a ray from the origin along -Z.
     */
    @WikiMethod(section = "Ray", sectionOrder = 10, order = 10,
            summary = "Creates a ray from the origin along -Z.")
    @AiDoc(section = "Ray", sectionOrder = 10, order = 10,
            summary = "Creates a ray from the origin along -Z.")
    public Ray() {
    }

    /**
     * Sets the ray and normalizes its direction. To avoid overflow in the squared length, first divides
     * the direction components by their maximum absolute value.
     *
     * @param origin origin in scene units.
     * @param x X direction component.
     * @param y Y direction component.
     * @param z Z direction component.
     * @return true on success. A zero-length direction or nonfinite input leaves the ray unchanged.
     */
    @WikiMethod(section = "Ray", sectionOrder = 10, order = 20,
            summary = "Sets the ray and normalizes its direction. To avoid overflow in the squared length, first divides the direction components by their maximum absolute value.")
    @WikiReturn("true on success. A zero-length direction or nonfinite input leaves the ray unchanged")
    @AiDoc(section = "Ray", sectionOrder = 10, order = 20,
            summary = "Sets the ray and normalizes its direction. To avoid overflow in the squared length, first divides the direction components by their maximum absolute value.")
    public boolean set(
            @WikiParameter(description = "origin in scene units.") Vec3View origin,
            @WikiParameter(description = "X direction component.") double x,
            @WikiParameter(description = "Y direction component.") double y,
            @WikiParameter(description = "Z direction component.") double z) {
        float ox = origin.x(), oy = origin.y(), oz = origin.z();
        double largest = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
        if (!(largest > 0.0) || !Double.isFinite(largest)
                || !Float.isFinite(ox) || !Float.isFinite(oy) || !Float.isFinite(oz)) {
            return false;
        }
        double sx = x / largest, sy = y / largest, sz = z / largest;
        double length = Math.sqrt(sx * sx + sy * sy + sz * sz);
        this.origin.set(ox, oy, oz);
        dx = sx / length;
        dy = sy / length;
        dz = sz / length;
        return true;
    }

    /**
     * Copies the ray origin.
     *
     * @param out reusable vector in scene units.
     * @return out.
     */
    @WikiMethod(section = "Ray", sectionOrder = 10, order = 30,
            summary = "Copies the ray origin.")
    @WikiReturn("out")
    @AiDoc(section = "Ray", sectionOrder = 10, order = 30,
            summary = "Copies the ray origin.")
    public MutableVec3 origin(
            @WikiParameter(description = "reusable vector in scene units.") MutableVec3 out) {
        return out.set(origin);
    }

    /**
     * Copies the unit direction.
     *
     * @param out reusable dimensionless vector.
     * @return out.
     */
    @WikiMethod(section = "Ray", sectionOrder = 10, order = 40,
            summary = "Copies the unit direction.")
    @WikiReturn("out")
    @AiDoc(section = "Ray", sectionOrder = 10, order = 40,
            summary = "Copies the unit direction.")
    public MutableVec3 direction(
            @WikiParameter(description = "reusable dimensionless vector.") MutableVec3 out) {
        return out.set((float) dx, (float) dy, (float) dz);
    }

    /**
     * Evaluates {@code origin + t * direction}.
     *
     * @param t distance from the origin in scene units. A negative value extends the line backward.
     * @param out reusable vector in scene units.
     * @return out.
     */
    @WikiMethod(section = "Ray", sectionOrder = 10, order = 50,
            summary = "Evaluates `origin + t * direction`.")
    @WikiReturn("out")
    @AiDoc(section = "Ray", sectionOrder = 10, order = 50,
            summary = "Evaluates `origin + t * direction`.")
    public MutableVec3 pointAt(
            @WikiParameter(description =
                    "distance from the origin in scene units. A negative value extends the line backward.")
            double t,
            @WikiParameter(description = "reusable vector in scene units.") MutableVec3 out) {
        return out.set((float) (origin.x() + t * dx), (float) (origin.y() + t * dy),
                (float) (origin.z() + t * dz));
    }

    /**
     * Intersects a closed AABB with the slab method. Along each axis, the slab interval lies
     * between {@code (min - origin) / direction} and {@code (max - origin) / direction}. The hit t is
     * the start of the intersection of all three intervals with {@code [0, +∞)}. A zero direction component
     * requires the origin to lie inside the corresponding slab.
     *
     * @param min lower coordinates in scene units.
     * @param max upper coordinates in scene units.
     * @return entry distance, zero on or inside the box, or positive infinity for a miss or invalid bounds.
     */
    @WikiMethod(section = "Intersections", sectionOrder = 20, order = 10,
            summary = "Intersects a closed AABB with the slab method. Along each axis, the slab interval lies between `(min - origin) / direction` and `(max - origin) / direction`. The hit t is the start of the intersection of all three intervals with `[0, +∞)`. A zero direction component requires the origin to lie inside the corresponding slab.")
    @WikiReturn("entry distance, zero on or inside the box, or positive infinity for a miss or invalid bounds")
    @AiDoc(section = "Intersections", sectionOrder = 20, order = 10,
            summary = "Intersects a closed AABB with the slab method. Along each axis, the slab interval lies between `(min - origin) / direction` and `(max - origin) / direction`. The hit t is the start of the intersection of all three intervals with `[0, +∞)`. A zero direction component requires the origin to lie inside the corresponding slab.")
    public double intersectAabb(
            @WikiParameter(description = "lower coordinates in scene units.") Vec3View min,
            @WikiParameter(description = "upper coordinates in scene units.") Vec3View max) {
        return intersectAabb(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
    }

    /**
     * Intersects a closed AABB specified by two corners. Uses the slab method from
     * {@link #intersectAabb(Vec3View, Vec3View)}.
     *
     * @param minX minimum X.
     * @param minY minimum Y.
     * @param minZ minimum Z.
     * @param maxX maximum X.
     * @param maxY maximum Y.
     * @param maxZ maximum Z.
     * @return entry distance, zero on or inside the box, or positive infinity for a miss.
     */
    @WikiMethod(section = "Intersections", sectionOrder = 20, order = 10,
            summary = "Intersects a closed AABB specified by two corners. Uses the slab method from `intersectAabb(Vec3View, Vec3View)`.")
    @WikiReturn("entry distance, zero on or inside the box, or positive infinity for a miss")
    @AiDoc(section = "Intersections", sectionOrder = 20, order = 10,
            summary = "Intersects a closed AABB specified by two corners. Uses the slab method from `intersectAabb(Vec3View, Vec3View)`.")
    public double intersectAabb(
            @WikiParameter(description = "minimum X.") float minX,
            @WikiParameter(description = "minimum Y.") float minY,
            @WikiParameter(description = "minimum Z.") float minZ,
            @WikiParameter(description = "maximum X.") float maxX,
            @WikiParameter(description = "maximum Y.") float maxY,
            @WikiParameter(description = "maximum Z.") float maxZ) {
        double enter = 0.0;
        double exit = Double.POSITIVE_INFINITY;
        for (int axis = 0; axis < 3; axis++) {
            float lower = axis == 0 ? minX : axis == 1 ? minY : minZ;
            float upper = axis == 0 ? maxX : axis == 1 ? maxY : maxZ;
            if (!(lower <= upper) || !Float.isFinite(lower) || !Float.isFinite(upper)) {
                return Double.POSITIVE_INFINITY;
            }
            double start = component(origin, axis);
            double direction = axis == 0 ? dx : axis == 1 ? dy : dz;
            if (direction == 0.0) {
                if (start < lower || start > upper) return Double.POSITIVE_INFINITY;
            } else {
                double first = (lower - start) / direction;
                double second = (upper - start) / direction;
                enter = Math.max(enter, Math.min(first, second));
                exit = Math.min(exit, Math.max(first, second));
                if (enter > exit) return Double.POSITIVE_INFINITY;
            }
        }
        return enter;
    }

    /**
     * Intersects both sides of a triangle using Möller-Trumbore. For edges {@code e1 = b - a} and
     * {@code e2 = c - a}, solves {@code origin + t*d = a + u*e1 + v*e2}, where d is the unit ray direction.
     * Set {@code p = d × e2}, {@code s = origin - a} and {@code q = s × e1}. Then
     * {@code u = dot(s,p)/dot(e1,p)}, {@code v = dot(d,q)/dot(e1,p)} and {@code t = dot(e2,q)/dot(e1,p)}.
     * The point lies in the triangle when {@code u >= 0, v >= 0, u + v <= 1}.
     *
     * @param a first vertex in scene units.
     * @param b second vertex in scene units.
     * @param c third vertex in scene units.
     * @return intersection distance, or positive infinity for a miss, parallel ray or degenerate triangle.
     */
    @WikiMethod(section = "Intersections", sectionOrder = 20, order = 20,
            summary = "Intersects both sides of a triangle using Möller-Trumbore. For edges `e1 = b - a` and `e2 = c - a`, solves `origin + t*d = a + u*e1 + v*e2`, where d is the unit ray direction. Set `p = d × e2`, `s = origin - a` and `q = s × e1`. Then `u = dot(s,p)/dot(e1,p)`, `v = dot(d,q)/dot(e1,p)` and `t = dot(e2,q)/dot(e1,p)`. The point lies in the triangle when `u >= 0, v >= 0, u + v <= 1`.")
    @WikiReturn("intersection distance, or positive infinity for a miss, parallel ray or degenerate triangle")
    @AiDoc(section = "Intersections", sectionOrder = 20, order = 20,
            summary = "Intersects both sides of a triangle using Möller-Trumbore. For edges `e1 = b - a` and `e2 = c - a`, solves `origin + t*d = a + u*e1 + v*e2`, where d is the unit ray direction. Set `p = d × e2`, `s = origin - a` and `q = s × e1`. Then `u = dot(s,p)/dot(e1,p)`, `v = dot(d,q)/dot(e1,p)` and `t = dot(e2,q)/dot(e1,p)`. The point lies in the triangle when `u >= 0, v >= 0, u + v <= 1`.")
    public double intersectTriangle(
            @WikiParameter(description = "first vertex in scene units.") Vec3View a,
            @WikiParameter(description = "second vertex in scene units.") Vec3View b,
            @WikiParameter(description = "third vertex in scene units.") Vec3View c) {
        double ax = a.x(), ay = a.y(), az = a.z();
        double e1x = b.x() - ax, e1y = b.y() - ay, e1z = b.z() - az;
        double e2x = c.x() - ax, e2y = c.y() - ay, e2z = c.z() - az;
        double px = dy * e2z - dz * e2y;
        double py = dz * e2x - dx * e2z;
        double pz = dx * e2y - dy * e2x;
        double det = e1x * px + e1y * py + e1z * pz;
        if (det == 0.0 || !Double.isFinite(det)) return Double.POSITIVE_INFINITY;

        double sx = origin.x() - ax, sy = origin.y() - ay, sz = origin.z() - az;
        double u = (sx * px + sy * py + sz * pz) / det;
        if (!(u >= 0.0 && u <= 1.0)) return Double.POSITIVE_INFINITY;
        double qx = sy * e1z - sz * e1y;
        double qy = sz * e1x - sx * e1z;
        double qz = sx * e1y - sy * e1x;
        double v = (dx * qx + dy * qy + dz * qz) / det;
        if (!(v >= 0.0 && u + v <= 1.0)) return Double.POSITIVE_INFINITY;
        double t = (e2x * qx + e2y * qy + e2z * qz) / det;
        return t >= 0.0 && Double.isFinite(t) ? t : Double.POSITIVE_INFINITY;
    }

    /**
     * Intersects a plane using {@code t = dot(point - origin, n) / dot(d, n)}, where n is the plane
     * normal and d is the unit ray direction.
     *
     * @param point point on the plane in scene units.
     * @param normal finite nonzero normal of arbitrary length.
     * @return intersection distance, or positive infinity if behind the ray origin, parallel or undefined.
     */
    @WikiMethod(section = "Intersections", sectionOrder = 20, order = 30,
            summary = "Intersects a plane using `t = dot(point - origin, n) / dot(d, n)`, where n is the plane normal and d is the unit ray direction.")
    @WikiReturn("intersection distance, or positive infinity if behind the ray origin, parallel or undefined")
    @AiDoc(section = "Intersections", sectionOrder = 20, order = 30,
            summary = "Intersects a plane using `t = dot(point - origin, n) / dot(d, n)`, where n is the plane normal and d is the unit ray direction.")
    public double intersectPlane(
            @WikiParameter(description = "point on the plane in scene units.") Vec3View point,
            @WikiParameter(description = "finite nonzero normal of arbitrary length.") Vec3View normal) {
        double nx = normal.x(), ny = normal.y(), nz = normal.z();
        double denominator = dx * nx + dy * ny + dz * nz;
        if (denominator == 0.0 || !Double.isFinite(denominator)) return Double.POSITIVE_INFINITY;
        double t = ((point.x() - (double) origin.x()) * nx + (point.y() - (double) origin.y()) * ny
                + (point.z() - (double) origin.z()) * nz) / denominator;
        return t >= 0.0 && Double.isFinite(t) ? t : Double.POSITIVE_INFINITY;
    }

    /**
     * Reads a vector component by numeric axis index.
     *
     * @param vector source vector.
     * @param axis 0, 1 or 2.
     * @return X, Y or Z respectively.
     */
    private static float component(Vec3View vector, int axis) {
        return axis == 0 ? vector.x() : axis == 1 ? vector.y() : vector.z();
    }
}

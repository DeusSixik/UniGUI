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
import java.util.Objects;

/**
 * Mutable 4-by-4 matrix for column vectors. Matrix construction, composition and inversion reuse
 * the result storage. Projections are right-handed, with -Z forward and NDC depth from -1 to 1.
 */
@WikiPage(
        title = "MutableMat4", slug = "mutable-mat-4",
        category = "API/3D Scene", order = 300,
        summary = "Reusable matrix for affine transforms, view and projection.")
@WikiSection(title = "Transforms", order = 10, markdown = """
        `setTransform` builds `T * R * S`. `setView` inverts the world-space camera pose. Perspective and
        orthographic builders use right-handed camera space and NDC depth from -1 to 1.
        """)
@WikiExample(title = "Copy a world transform for upload", code = """
        MutableMat4 world = node.worldMatrix(new MutableMat4());
        float[] columnMajor = new float[16];
        world.copyColumnMajor(columnMajor, 0);
        """)
@WikiLink(label = "Mat4View", target = "page:mat-4-view")
@WikiLink(label = "MutableQuat", target = "page:mutable-quat")
@WikiLink(label = "Camera3D", target = "page:camera-3d")
@WikiLink(label = "Frustum3D", target = "page:frustum-3d")
@AiDoc(
        summary = "Reusable matrix for affine transforms, view and projection.",
        purpose = "Compose scene transforms or pass matrices to a renderer.",
        criticalRules = {
                "Matrices act on column vectors. `setProduct(left, right)` applies right first.",
                "Use W = 1 for points and W = 0 for directions in homogeneous transforms.",
                "`setInverse` leaves the result unchanged if inversion fails. `copyColumnMajor` writes 16 floats for GPU upload."
        },
        related = {"Mat4View", "MutableQuat", "Camera3D", "Frustum3D"},
        examples = @AiDoc.Example(title = "Copy a world transform for upload", code = """
                MutableMat4 world = node.worldMatrix(new MutableMat4());
                float[] columnMajor = new float[16];
                world.copyColumnMajor(columnMajor, 0);
                """))
public final class MutableMat4 implements Mat4View {
    /**
     * Sixteen elements in column-major order. Units depend on the transform.
     */
    private final double[] elements = new double[16];

    /**
     * Creates an identity matrix.
     */
    @WikiMethod(section = "Matrices", sectionOrder = 10, order = 10,
            summary = "Creates an identity matrix.")
    @AiDoc(section = "Matrices", sectionOrder = 10, order = 10,
            summary = "Creates an identity matrix.")
    public MutableMat4() {
        identity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double get(int row, int column) {
        return elements[Objects.checkIndex(column, 4) * 4 + Objects.checkIndex(row, 4)];
    }

    /**
     * Writes ones to the diagonal and zeros elsewhere.
     *
     * @return this matrix.
     */
    @WikiMethod(section = "Matrices", sectionOrder = 10, order = 20,
            summary = "Writes ones to the diagonal and zeros elsewhere.")
    @WikiReturn("this matrix")
    @AiDoc(section = "Matrices", sectionOrder = 10, order = 20,
            summary = "Writes ones to the diagonal and zeros elsewhere.")
    public MutableMat4 identity() {
        return setEntries(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
    }

    /**
     * Copies a matrix.
     *
     * @param source source matrix, may alias this matrix.
     * @return this matrix.
     */
    @WikiMethod(section = "Matrices", sectionOrder = 10, order = 30,
            summary = "Copies a matrix.")
    @WikiReturn("this matrix")
    @AiDoc(section = "Matrices", sectionOrder = 10, order = 30,
            summary = "Copies a matrix.")
    public MutableMat4 set(
            @WikiParameter(description = "source matrix, may alias this matrix.") Mat4View source) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                elements[column * 4 + row] = source.get(row, column);
            }
        }
        return this;
    }

    /**
     * Copies column-major elements as floats for the renderer.
     *
     * @param out array with space for 16 consecutive values.
     * @param offset index of the first output value.
     * @throws IndexOutOfBoundsException if the range exceeds the array.
     */
    @WikiMethod(section = "Matrices", sectionOrder = 10, order = 60,
            summary = "Copies column-major elements as floats for the renderer.")
    @WikiThrows(type = IndexOutOfBoundsException.class, description = "if the range exceeds the array.")
    @AiDoc(section = "Matrices", sectionOrder = 10, order = 60,
            summary = "Copies column-major elements as floats for the renderer.")
    public void copyColumnMajor(
            @WikiParameter(description = "array with space for 16 consecutive values.") float[] out,
            @WikiParameter(description = "index of the first output value.") int offset) {
        Objects.checkFromIndexSize(offset, 16, out.length);
        for (int i = 0; i < 16; i++) {
            out[offset + i] = (float) elements[i];
        }
    }

    /**
     * Sets {@code T * R * S}, where S scales the local axes, R rotates at the origin, and T translates into parent space.
     *
     * @param position translation in scene units.
     * @param rotation unit active rotation quaternion.
     * @param scale dimensionless local scale factors.
     * @return this matrix.
     */
    @WikiMethod(section = "Transforms", sectionOrder = 20, order = 10,
            summary = "Sets `T * R * S`, where S scales the local axes, R rotates at the origin, and T translates into parent space.")
    @WikiReturn("this matrix")
    @AiDoc(section = "Transforms", sectionOrder = 20, order = 10,
            summary = "Sets `T * R * S`, where S scales the local axes, R rotates at the origin, and T translates into parent space.")
    public MutableMat4 setTransform(
            @WikiParameter(description = "translation in scene units.") Vec3View position,
            @WikiParameter(description = "unit active rotation quaternion.") QuatView rotation,
            @WikiParameter(description = "dimensionless local scale factors.") Vec3View scale) {
        setRotation(rotation.x(), rotation.y(), rotation.z(), rotation.w());
        for (int row = 0; row < 3; row++) {
            elements[row] *= scale.x();
            elements[4 + row] *= scale.y();
            elements[8 + row] *= scale.z();
        }
        elements[12] = position.x();
        elements[13] = position.y();
        elements[14] = position.z();
        return this;
    }

    /**
     * Sets the inverse of the camera pose, {@code R^-1 * T(-eye)}. A unit quaternion's conjugate
     * represents R^-1. The translation column is {@code -R^-1 * eye}.
     *
     * @param eye camera position in scene units.
     * @param orientation unit camera-to-world quaternion.
     * @return this matrix.
     */
    @WikiMethod(section = "Transforms", sectionOrder = 20, order = 20,
            summary = "Sets the inverse of the camera pose, `R^-1 * T(-eye)`. A unit quaternion's conjugate represents R^-1. The translation column is `-R^-1 * eye`.")
    @WikiReturn("this matrix")
    @AiDoc(section = "Transforms", sectionOrder = 20, order = 20,
            summary = "Sets the inverse of the camera pose, `R^-1 * T(-eye)`. A unit quaternion's conjugate represents R^-1. The translation column is `-R^-1 * eye`.")
    public MutableMat4 setView(
            @WikiParameter(description = "camera position in scene units.") Vec3View eye,
            @WikiParameter(description = "unit camera-to-world quaternion.") QuatView orientation) {
        setRotation(-orientation.x(), -orientation.y(), -orientation.z(), orientation.w());
        for (int row = 0; row < 3; row++) {
            elements[12 + row] = -transformComponent(row, eye.x(), eye.y(), eye.z(), 0.0);
        }
        return this;
    }

    /**
     * Sets a symmetric perspective projection. With {@code f = 1/tan(fovY/2)}, X and Y scale by
     * {@code f/aspect} and {@code f}, and {@code clip.w = -view.z}. The depth coefficients
     * {@code -(far+near)/(far-near)} and {@code -2*far*near/(far-near)} map view Z at -near and -far
     * to NDC depths -1 and 1.
     *
     * @param fovY vertical field of view in radians, strictly between zero and π.
     * @param aspect positive width divided by height.
     * @param near positive near-plane distance in scene units.
     * @param far far-plane distance in scene units, greater than near.
     * @return this matrix.
     * @throws IllegalArgumentException if an argument is outside its range or the projection is nonfinite.
     */
    @WikiMethod(section = "Transforms", sectionOrder = 20, order = 30,
            summary = "Sets a symmetric perspective projection. With `f = 1/tan(fovY/2)`, X and Y scale by `f/aspect` and `f`, and `clip.w = -view.z`. The depth coefficients `-(far+near)/(far-near)` and `-2*far*near/(far-near)` map view Z at -near and -far to NDC depths -1 and 1.")
    @WikiReturn("this matrix")
    @WikiThrows(type = IllegalArgumentException.class, description = "if an argument is outside its range or the projection is nonfinite.")
    @AiDoc(section = "Transforms", sectionOrder = 20, order = 30,
            summary = "Sets a symmetric perspective projection. With `f = 1/tan(fovY/2)`, X and Y scale by `f/aspect` and `f`, and `clip.w = -view.z`. The depth coefficients `-(far+near)/(far-near)` and `-2*far*near/(far-near)` map view Z at -near and -far to NDC depths -1 and 1.")
    public MutableMat4 setPerspective(
            @WikiParameter(description = "vertical field of view in radians, strictly between zero and π.") double fovY,
            @WikiParameter(description = "positive width divided by height.") double aspect,
            @WikiParameter(description = "positive near-plane distance in scene units.") double near,
            @WikiParameter(description = "far-plane distance in scene units, greater than near.") double far) {
        checkProjection(aspect, near, far);
        double f = 1.0 / Math.tan(fovY * 0.5);
        double scaleX = f / aspect;
        double nearRatio = near / (far - near);
        double depthScale = -1.0 - 2.0 * nearRatio;
        double depthOffset = -2.0 * near * (far / (far - near));
        if (!(fovY > 0.0 && fovY < Math.PI) || !Double.isFinite(f) || !(scaleX > 0.0) || !Double.isFinite(scaleX)
                || !Double.isFinite(depthScale) || !Double.isFinite(depthOffset)) {
            throw new IllegalArgumentException("Invalid perspective projection");
        }
        return setEntries(scaleX, 0, 0, 0, 0, f, 0, 0,
                0, 0, depthScale, -1, 0, 0, depthOffset, 0);
    }

    /**
     * Sets a symmetric orthographic projection. X and Y scale by {@code 2/(height*aspect)} and
     * {@code 2/height}. View-space depth maps linearly as {@code zNdc = -2*(view.z+near)/(far-near) - 1}.
     *
     * @param height positive visible height in scene units.
     * @param aspect positive width divided by height.
     * @param near positive near-plane distance in scene units.
     * @param far far-plane distance in scene units, greater than near.
     * @return this matrix.
     * @throws IllegalArgumentException if an argument is outside its range or the projection is nonfinite.
     */
    @WikiMethod(section = "Transforms", sectionOrder = 20, order = 40,
            summary = "Sets a symmetric orthographic projection. X and Y scale by `2/(height*aspect)` and `2/height`. View-space depth maps linearly as `zNdc = -2*(view.z+near)/(far-near) - 1`.")
    @WikiReturn("this matrix")
    @WikiThrows(type = IllegalArgumentException.class, description = "if an argument is outside its range or the projection is nonfinite.")
    @AiDoc(section = "Transforms", sectionOrder = 20, order = 40,
            summary = "Sets a symmetric orthographic projection. X and Y scale by `2/(height*aspect)` and `2/height`. View-space depth maps linearly as `zNdc = -2*(view.z+near)/(far-near) - 1`.")
    public MutableMat4 setOrthographic(
            @WikiParameter(description = "positive visible height in scene units.") double height,
            @WikiParameter(description = "positive width divided by height.") double aspect,
            @WikiParameter(description = "positive near-plane distance in scene units.") double near,
            @WikiParameter(description = "far-plane distance in scene units, greater than near.") double far) {
        checkProjection(aspect, near, far);
        double scaleY = 2.0 / height;
        double scaleX = scaleY / aspect;
        double scaleZ = -2.0 / (far - near);
        double depthOffset = -1.0 + scaleZ * near;
        if (!(height > 0.0) || !Double.isFinite(height) || !(scaleX > 0.0)
                || !Double.isFinite(scaleX) || !Double.isFinite(scaleY)
                || !Double.isFinite(scaleZ) || !Double.isFinite(depthOffset)) {
            throw new IllegalArgumentException("Invalid orthographic projection");
        }
        return setEntries(scaleX, 0, 0, 0, 0, scaleY, 0, 0,
                0, 0, scaleZ, 0, 0, 0, depthOffset, 1);
    }

    /**
     * Sets {@code left * right}. Element (r,c) sums {@code left[r,k] * right[k,c]} for k from zero through three.
     *
     * @param left left factor, may alias this matrix.
     * @param right right factor, may alias this matrix.
     * @return this matrix.
     */
    @WikiMethod(section = "Matrices", sectionOrder = 10, order = 40,
            summary = "Sets `left * right`. Element (r,c) sums `left[r,k] * right[k,c]` for k from zero through three.")
    @WikiReturn("this matrix")
    @AiDoc(section = "Matrices", sectionOrder = 10, order = 40,
            summary = "Sets `left * right`. Element (r,c) sums `left[r,k] * right[k,c]` for k from zero through three.")
    public MutableMat4 setProduct(
            @WikiParameter(description = "left factor, may alias this matrix.") Mat4View left,
            @WikiParameter(description = "right factor, may alias this matrix.") Mat4View right) {
        return setEntries(
                dot(left, right, 0, 0), dot(left, right, 1, 0), dot(left, right, 2, 0), dot(left, right, 3, 0),
                dot(left, right, 0, 1), dot(left, right, 1, 1), dot(left, right, 2, 1), dot(left, right, 3, 1),
                dot(left, right, 0, 2), dot(left, right, 1, 2), dot(left, right, 2, 2), dot(left, right, 3, 2),
                dot(left, right, 0, 3), dot(left, right, 1, 3), dot(left, right, 2, 3), dot(left, right, 3, 3));
    }

    /**
     * Sets the inverse {@code adj(A)/det(A)}. Entries in adj(A) are transposed 3-by-3 minors with
     * sign {@code (-1)^(row+column)}. Expanding those minors reuses the 2-by-2 minors of the upper
     * pair of rows (s) and lower pair of rows (c).
     *
     * @param source invertible matrix A, may alias this matrix.
     * @return true if the inverse is finite and stored. On failure this matrix is unchanged.
     */
    @WikiMethod(section = "Matrices", sectionOrder = 10, order = 50,
            summary = "Sets the inverse `adj(A)/det(A)`. Entries in adj(A) are transposed 3-by-3 minors with sign `(-1)^(row+column)`. Expanding those minors reuses the 2-by-2 minors of the upper pair of rows (s) and lower pair of rows (c).")
    @WikiReturn("true if the inverse is finite and stored. On failure this matrix is unchanged")
    @AiDoc(section = "Matrices", sectionOrder = 10, order = 50,
            summary = "Sets the inverse `adj(A)/det(A)`. Entries in adj(A) are transposed 3-by-3 minors with sign `(-1)^(row+column)`. Expanding those minors reuses the 2-by-2 minors of the upper pair of rows (s) and lower pair of rows (c).")
    public boolean setInverse(
            @WikiParameter(description = "invertible matrix A, may alias this matrix.") Mat4View source) {
        double a00 = source.get(0, 0), a01 = source.get(0, 1), a02 = source.get(0, 2), a03 = source.get(0, 3);
        double a10 = source.get(1, 0), a11 = source.get(1, 1), a12 = source.get(1, 2), a13 = source.get(1, 3);
        double a20 = source.get(2, 0), a21 = source.get(2, 1), a22 = source.get(2, 2), a23 = source.get(2, 3);
        double a30 = source.get(3, 0), a31 = source.get(3, 1), a32 = source.get(3, 2), a33 = source.get(3, 3);

        double s0 = a00 * a11 - a10 * a01;
        double s1 = a00 * a12 - a10 * a02;
        double s2 = a00 * a13 - a10 * a03;
        double s3 = a01 * a12 - a11 * a02;
        double s4 = a01 * a13 - a11 * a03;
        double s5 = a02 * a13 - a12 * a03;
        double c0 = a20 * a31 - a30 * a21;
        double c1 = a20 * a32 - a30 * a22;
        double c2 = a20 * a33 - a30 * a23;
        double c3 = a21 * a32 - a31 * a22;
        double c4 = a21 * a33 - a31 * a23;
        double c5 = a22 * a33 - a32 * a23;

        double det = s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0;
        if (det == 0.0 || !Double.isFinite(det)) return false;

        double r00 = (a11 * c5 - a12 * c4 + a13 * c3) / det;
        double r01 = (-a01 * c5 + a02 * c4 - a03 * c3) / det;
        double r02 = (a31 * s5 - a32 * s4 + a33 * s3) / det;
        double r03 = (-a21 * s5 + a22 * s4 - a23 * s3) / det;
        double r10 = (-a10 * c5 + a12 * c2 - a13 * c1) / det;
        double r11 = (a00 * c5 - a02 * c2 + a03 * c1) / det;
        double r12 = (-a30 * s5 + a32 * s2 - a33 * s1) / det;
        double r13 = (a20 * s5 - a22 * s2 + a23 * s1) / det;
        double r20 = (a10 * c4 - a11 * c2 + a13 * c0) / det;
        double r21 = (-a00 * c4 + a01 * c2 - a03 * c0) / det;
        double r22 = (a30 * s4 - a31 * s2 + a33 * s0) / det;
        double r23 = (-a20 * s4 + a21 * s2 - a23 * s0) / det;
        double r30 = (-a10 * c3 + a11 * c1 - a12 * c0) / det;
        double r31 = (a00 * c3 - a01 * c1 + a02 * c0) / det;
        double r32 = (-a30 * s3 + a31 * s1 - a32 * s0) / det;
        double r33 = (a20 * s3 - a21 * s1 + a22 * s0) / det;

        if (!Double.isFinite(r00) || !Double.isFinite(r01) || !Double.isFinite(r02) || !Double.isFinite(r03)
                || !Double.isFinite(r10) || !Double.isFinite(r11) || !Double.isFinite(r12) || !Double.isFinite(r13)
                || !Double.isFinite(r20) || !Double.isFinite(r21) || !Double.isFinite(r22) || !Double.isFinite(r23)
                || !Double.isFinite(r30) || !Double.isFinite(r31) || !Double.isFinite(r32) || !Double.isFinite(r33)) {
            return false;
        }
        setEntries(r00, r10, r20, r30, r01, r11, r21, r31, r02, r12, r22, r32, r03, r13, r23, r33);
        return true;
    }

    /**
     * Multiplies one row by the homogeneous vector {@code (x, y, z, w)}.
     *
     * @param row result component index from 0 to 3.
     * @param x first input component.
     * @param y second input component.
     * @param z third input component.
     * @param w fourth input component, usually one for a point or zero for a direction.
     * @return dot product of the row and input vector.
     * @throws IndexOutOfBoundsException if row is outside its range.
     */
    @WikiMethod(section = "Transforms", sectionOrder = 20, order = 60,
            summary = "Multiplies one row by the homogeneous vector `(x, y, z, w)`.")
    @WikiReturn("dot product of the row and input vector")
    @WikiThrows(type = IndexOutOfBoundsException.class, description = "if row is outside its range.")
    @AiDoc(section = "Transforms", sectionOrder = 20, order = 60,
            summary = "Multiplies one row by the homogeneous vector `(x, y, z, w)`.")
    public double transformComponent(
            @WikiParameter(description = "result component index from 0 to 3.") int row,
            @WikiParameter(description = "first input component.") double x,
            @WikiParameter(description = "second input component.") double y,
            @WikiParameter(description = "third input component.") double z,
            @WikiParameter(description = "fourth input component, usually one for a point or zero for a direction.") double w) {
        return elements[Objects.checkIndex(row, 4)] * x + elements[row + 4] * y
                + elements[row + 8] * z + elements[row + 12] * w;
    }

    /**
     * Transforms a point as {@code out = (M * (point, 1)).xyz / (M * (point, 1)).w}, where M is this matrix.
     *
     * @param point input point, may alias out.
     * @param out output point in the transformed coordinate space.
     * @return true for a finite result. Zero W or invalid output leaves out unchanged.
     */
    @WikiMethod(section = "Transforms", sectionOrder = 20, order = 70,
            summary = "Transforms a point as `out = (M * (point, 1)).xyz / (M * (point, 1)).w`, where M is this matrix.")
    @WikiReturn("true for a finite result. Zero W or invalid output leaves out unchanged")
    @AiDoc(section = "Transforms", sectionOrder = 20, order = 70,
            summary = "Transforms a point as `out = (M * (point, 1)).xyz / (M * (point, 1)).w`, where M is this matrix.")
    public boolean transformPoint(
            @WikiParameter(description = "input point, may alias out.") Vec3View point,
            @WikiParameter(description = "output point in the transformed coordinate space.") MutableVec3 out) {
        double x = point.x(), y = point.y(), z = point.z();
        double w = transformComponent(3, x, y, z, 1.0);
        if (w == 0.0 || !Double.isFinite(w)) return false;
        float px = (float) (transformComponent(0, x, y, z, 1.0) / w);
        float py = (float) (transformComponent(1, x, y, z, 1.0) / w);
        float pz = (float) (transformComponent(2, x, y, z, 1.0) / w);
        if (!Float.isFinite(px) || !Float.isFinite(py) || !Float.isFinite(pz)) return false;
        out.set(px, py, pz);
        return true;
    }

    /**
     * Writes the orthonormal basis for quaternion q. Each column rotates a unit basis vector v via
     * {@code v + 2*w*(q.xyz × v) + 2*q.xyz × (q.xyz × v)}. The factor of two is divided by the
     * squared length of q to compensate for rounding of the stored components.
     *
     * @param x first vector component of q.
     * @param y second vector component of q.
     * @param z third vector component of q.
     * @param w scalar component of q.
     */
    private void setRotation(double x, double y, double z, double w) {
        double factor = 2.0 / (x * x + y * y + z * z + w * w);
        double xx = factor * x * x, yy = factor * y * y, zz = factor * z * z;
        double xy = factor * x * y, xz = factor * x * z, yz = factor * y * z;
        double wx = factor * w * x, wy = factor * w * y, wz = factor * w * z;
        setEntries(1 - yy - zz, xy + wz, xz - wy, 0,
                xy - wz, 1 - xx - zz, yz + wx, 0,
                xz + wy, yz - wx, 1 - xx - yy, 0,
                0, 0, 0, 1);
    }

    /**
     * Validates the ranges shared by both projections.
     *
     * @param aspect width divided by height.
     * @param near near-plane distance in scene units.
     * @param far far-plane distance in scene units.
     * @throws IllegalArgumentException if a value is nonfinite or violates {@code aspect > 0, 0 < near < far}.
     */
    private static void checkProjection(double aspect, double near, double far) {
        if (!(aspect > 0.0 && near > 0.0 && far > near) || !Double.isFinite(aspect) || !Double.isFinite(far)) {
            throw new IllegalArgumentException("Projection requires a positive aspect ratio and 0 < near < far");
        }
    }

    /**
     * Computes an element of the matrix product.
     *
     * @param left left factor.
     * @param right right factor.
     * @param row row of the left factor.
     * @param column column of the right factor.
     * @return dot product of the selected row and column.
     */
    private static double dot(Mat4View left, Mat4View right, int row, int column) {
        return left.get(row, 0) * right.get(0, column) + left.get(row, 1) * right.get(1, column)
                + left.get(row, 2) * right.get(2, column) + left.get(row, 3) * right.get(3, column);
    }

    /**
     * Stores every element after the arguments have been evaluated. This permits overwriting a source matrix.
     *
     * @param m00 row 0, column 0.
     * @param m10 row 1, column 0.
     * @param m20 row 2, column 0.
     * @param m30 row 3, column 0.
     * @param m01 row 0, column 1.
     * @param m11 row 1, column 1.
     * @param m21 row 2, column 1.
     * @param m31 row 3, column 1.
     * @param m02 row 0, column 2.
     * @param m12 row 1, column 2.
     * @param m22 row 2, column 2.
     * @param m32 row 3, column 2.
     * @param m03 row 0, column 3.
     * @param m13 row 1, column 3.
     * @param m23 row 2, column 3.
     * @param m33 row 3, column 3.
     * @return this matrix.
     */
    private MutableMat4 setEntries(double m00, double m10, double m20, double m30,
                                   double m01, double m11, double m21, double m31,
                                   double m02, double m12, double m22, double m32,
                                   double m03, double m13, double m23, double m33) {
        elements[0] = m00;
        elements[1] = m10;
        elements[2] = m20;
        elements[3] = m30;
        elements[4] = m01;
        elements[5] = m11;
        elements[6] = m21;
        elements[7] = m31;
        elements[8] = m02;
        elements[9] = m12;
        elements[10] = m22;
        elements[11] = m32;
        elements[12] = m03;
        elements[13] = m13;
        elements[14] = m23;
        elements[15] = m33;
        return this;
    }

    /**
     * Replaces one translation component.
     *
     * @param row 0, 1 or 2 for X, Y or Z.
     * @param value new component.
     * @return this matrix.
     * @throws IndexOutOfBoundsException if row is outside its range.
     */
    @WikiMethod(section = "Transforms", sectionOrder = 20, order = 50,
            summary = "Replaces one translation component.")
    @WikiReturn("this matrix")
    @WikiThrows(type = IndexOutOfBoundsException.class, description = "if row is outside its range.")
    @AiDoc(section = "Transforms", sectionOrder = 20, order = 50,
            summary = "Replaces one translation component.")
    public MutableMat4 setTranslationComponent(
            @WikiParameter(description = "0, 1 or 2 for X, Y or Z.") int row,
            @WikiParameter(description = "new component.") double value) {
        if (row < 0 || row > 2) throw new IndexOutOfBoundsException("Translation row must be 0..2: " + row);
        elements[12 + row] = value;
        return this;
    }
}

package dev.sixik.unigui.api.math;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class Math3DTest {
    // Допуск для результатов, округлённых до float, на несколько ULP около единицы.
    private static final float VECTOR_EPS = 1e-6f;
    // Запас на операции с double в произведении и инверсии матриц размера 4×4.
    private static final double MATRIX_EPS = 1e-12;

    @ParameterizedTest
    @ValueSource(floats = {Float.MIN_VALUE, Float.MAX_VALUE, -2.0f})
    void normalize_extremeFiniteVector_preservesDirection(float scale) {
        MutableVec3 vector = new MutableVec3(scale, 0.0f, scale);

        assertTrue(vector.normalize());

        float expected = (float) Math.copySign(Math.sqrt(0.5), scale);
        assertVector(vector, expected, 0.0f, expected);
    }

    @Test
    void normalize_invalidLength_preservesVector() {
        MutableVec3 vector = new MutableVec3();
        assertFalse(vector.normalize());
        assertVector(vector, 0, 0, 0);
        vector.set(1, Float.POSITIVE_INFINITY, 0);
        assertFalse(vector.normalize());
        assertEquals(Float.POSITIVE_INFINITY, vector.y(), 0.0f);
    }

    @Test
    void setCross_outputAliasesSource_usesRightHandedBasis() {
        MutableVec3 x = new MutableVec3(1, 0, 0);
        assertVector(x.setCross(x, new MutableVec3(0, 1, 0)), 0, 0, 1);
    }

    @Test
    void setProduct_quaternions_applyRightRotationFirst() {
        MutableQuat yaw = new MutableQuat().setAxisAngle(new MutableVec3(0, 2, 0), Math.PI / 2);
        MutableQuat pitch = new MutableQuat().setAxisAngle(new MutableVec3(1, 0, 0), Math.PI / 2);
        MutableVec3 out = new MutableVec3();

        yaw.setProduct(yaw, pitch).rotate(0, 0, 1, out);
        assertVector(out, 0, -1, 0);
        new MutableQuat().setYawPitch(Math.PI / 2, Math.PI / 2).rotate(0, 0, 1, out);
        assertVector(out, 0, -1, 0);
    }

    @Test
    void setQuaternion_invalidLength_preservesRotation() {
        MutableQuat rotation = new MutableQuat();
        assertThrows(IllegalArgumentException.class, () -> rotation.set(0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> rotation.set(Float.NaN, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> rotation.setAxisAngle(new MutableVec3(), 1.0));
        assertEquals(1.0f, rotation.w(), VECTOR_EPS);
    }

    @Test
    void transformPoint_nonUniformScaleAndTranslation_appliesScaleThenRotationThenTranslation() {
        MutableMat4 matrix = new MutableMat4().setTransform(new MutableVec3(2, 3, 4),
                new MutableQuat().setAxisAngle(new MutableVec3(0, 0, 1), Math.PI / 2),
                new MutableVec3(3, 2, 1));
        MutableVec3 point = new MutableVec3(1, 2, 3);

        assertTrue(matrix.transformPoint(point, point));

        assertVector(point, -2, 6, 7);
        float[] exported = new float[18];
        matrix.copyColumnMajor(exported, 1);
        assertEquals(3.0f, exported[2], VECTOR_EPS);
        assertEquals(2.0f, exported[13], VECTOR_EPS);
        assertEquals(3.0f, exported[14], VECTOR_EPS);
        assertEquals(4.0f, exported[15], VECTOR_EPS);
        assertEquals(1.0f, exported[16], VECTOR_EPS);
    }

    @Test
    void setProduct_generalMatrices_matchesRowColumnProductWithAliasing() {
        double[][] a = {{1, 2, 3, 4}, {2, 4, -1, 0}, {0, 1, 2, -2}, {-1, 3, 0, 1}};
        double[][] b = {{0, 1, 2, 3}, {2, -4, 6, 1}, {1, -1, 0, -2}, {-1, 4, 0, 2}};
        MutableMat4 left = new MutableMat4().set((row, column) -> a[row][column]);
        MutableMat4 right = new MutableMat4().set((row, column) -> b[row][column]);
        MutableMat4 leftAlias = new MutableMat4().set(left);
        MutableMat4 rightAlias = new MutableMat4().set(right);

        leftAlias.setProduct(leftAlias, right);
        rightAlias.setProduct(left, rightAlias);

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                double expected = 0;
                for (int k = 0; k < 4; k++) expected += a[row][k] * b[k][column];
                assertEquals(expected, leftAlias.get(row, column), MATRIX_EPS);
                assertEquals(expected, rightAlias.get(row, column), MATRIX_EPS);
            }
        }
    }

    @Test
    void setInverse_generalMatrixWithSelfAlias_multipliesBackToIdentity() {
        double[][] original = {{1, 2, 3, 4}, {2, 4, -1, 0}, {0, 1, 2, -2}, {-1, 3, 0, 1}};
        Mat4View input = (row, column) -> original[row][column];
        MutableMat4 inverse = new MutableMat4().set(input);

        assertTrue(inverse.setInverse(inverse));

        assertIdentity(new MutableMat4().setProduct(input, inverse));
        assertIdentity(new MutableMat4().setProduct(inverse, input));
    }

    @Test
    void setInverse_singularOrNonFinite_preservesDestination() {
        MutableMat4 inverse = new MutableMat4();
        assertFalse(inverse.setInverse((row, column) -> 0.0));
        assertFalse(inverse.setInverse((row, column) -> Double.NaN));
        assertIdentity(inverse);
    }

    @Test
    void setView_matchesInverseOfCameraPose() {
        MutableVec3 eye = new MutableVec3(2, -3, 4);
        MutableQuat orientation = new MutableQuat().setYawPitch(0.4, -0.7);
        MutableMat4 cameraToWorld = new MutableMat4().setTransform(eye, orientation, new MutableVec3(1, 1, 1));
        MutableMat4 worldToCamera = new MutableMat4().setView(eye, orientation);

        assertIdentity(new MutableMat4().setProduct(worldToCamera, cameraToWorld));
    }

    @Test
    void invalidProjectionsAndHomogeneousPoints_preserveOutput() {
        MutableMat4 matrix = new MutableMat4();
        assertThrows(IllegalArgumentException.class, () -> matrix.setPerspective(0, 1, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> matrix.setPerspective(1, 0, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> matrix.setOrthographic(Double.NaN, 1, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> matrix.setOrthographic(1, 1, 2, 1));
        assertIdentity(matrix);

        matrix.setPerspective(Math.PI / 2, 1, 1, 10);
        MutableVec3 out = new MutableVec3(1, 2, 3);
        assertFalse(matrix.transformPoint(new MutableVec3(), out));
        assertVector(out, 1, 2, 3);
    }

    private static void assertIdentity(Mat4View matrix) {
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                assertEquals(row == column ? 1 : 0, matrix.get(row, column), MATRIX_EPS);
            }
        }
    }

    private static void assertVector(Vec3View vector, float x, float y, float z) {
        assertEquals(x, vector.x(), VECTOR_EPS);
        assertEquals(y, vector.y(), VECTOR_EPS);
        assertEquals(z, vector.z(), VECTOR_EPS);
    }
}

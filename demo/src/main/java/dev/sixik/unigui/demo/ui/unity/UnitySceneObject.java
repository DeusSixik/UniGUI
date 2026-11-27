package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraftHost.SceneElementKind;

import static dev.sixik.unigui.demo.ui.unity.UnityEditorHierarchyView.GameObjectType;

/**
 * An editable scene node carrying the name and type displayed in the editor.
 */
final class UnitySceneObject extends SceneNode {
    /**
     * Unit cube with separate vertices per face for flat lighting.
     */
    private static final Mesh3D CUBE = cube();
    /**
     * Unit-diameter sphere shared by sphere objects.
     */
    private static final Mesh3D SPHERE = roundedMesh(0);
    /**
     * Unit-diameter capsule with a one-unit cylinder between the caps.
     */
    private static final Mesh3D CAPSULE = roundedMesh(0.5f);
    /**
     * Unit square in XZ with an upward normal.
     */
    private static final Mesh3D PLANE = new Mesh3D(
            new float[]{-0.5f, 0, -0.5f, -0.5f, 0, 0.5f, 0.5f, 0, 0.5f, 0.5f, 0, -0.5f},
            new float[]{0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0}, null, null, new int[]{0, 1, 2, 0, 2, 3});
    /**
     * Local coordinate axes used when converting the inspector's Euler fields.
     */
    private static final MutableVec3 X = new MutableVec3(1, 0, 0),
            Y = new MutableVec3(0, 1, 0), Z = new MutableVec3(0, 0, 1);

    /**
     * Editable label displayed in the hierarchy.
     */
    private String name;
    /**
     * Kind of content shown in the create menu and inspector.
     */
    private final GameObjectType type;
    /**
     * Registry ID for a Minecraft object, or null for regular editor objects.
     */
    private final String minecraftId;
    /**
     * Local rotations in degrees, composed in X, Y, Z order.
     */
    private final MutableVec3 angles = new MutableVec3();
    /**
     * Quaternion corresponding to the last values entered in the Inspector.
     */
    private final MutableQuat anglesRotation = new MutableQuat();

    /**
     * Creates a detached object at the origin.
     *
     * @param name label displayed in the editor
     * @param type kind of editable content
     */
    UnitySceneObject(String name, GameObjectType type) {
        this(name, type, null);
    }

    /**
     * Creates an editor object and attaches a native render node for Minecraft content.
     *
     * @param name        label displayed in the editor
     * @param type        kind of editable content
     * @param minecraftId registry ID, required for a Minecraft object
     * @throws IllegalArgumentException if a Minecraft object lacks a source ID
     */
    UnitySceneObject(String name, GameObjectType type, String minecraftId) {
        if (type.isMinecraft() && (minecraftId == null || minecraftId.isBlank())) {
            throw new IllegalArgumentException("Missing Minecraft scene element ID");
        }
        this.name = name;
        this.type = type;
        this.minecraftId = minecraftId;
        switch (type) {
            case MINECRAFT_BLOCK -> addChild(new BlockNode(minecraftId).setPosition(-0.5f, -0.5f, -0.5f));
            case MINECRAFT_ITEM -> addChild(new ItemNode(minecraftId));
            case MINECRAFT_ENTITY -> addChild(new EntityNode(minecraftId));
            default -> {
            }
        }
        setMesh(switch (type) {
            case CUBE -> CUBE;
            case SPHERE -> SPHERE;
            case CAPSULE -> CAPSULE;
            case PLANE -> PLANE;
            default -> null;
        });
        setMaterial(material(switch (type) {
            case CUBE -> 0xFF62ADD7;
            case SPHERE -> 0xFFBF88DA;
            case CAPSULE -> 0xFF71C8A5;
            case PLANE -> 0xFF495361;
            default -> 0xFFFFFFFF;
        }));
    }

    /**
     * @return the label shown in the tree
     */
    String name() {
        return name;
    }

    /**
     * Updates the hierarchy label.
     *
     * @param value new display name
     */
    void rename(String value) {
        name = value;
    }

    /**
     * @return the object type
     */
    GameObjectType type() {
        return type;
    }

    /**
     * @return the version-specific registry ID, or null for regular editor objects
     */
    String minecraftId() {
        return minecraftId;
    }

    /**
     * Creates a detached editor object from an entry in a Minecraft registry.
     *
     * @param kind registry that contains the entry
     * @param name display name in the editor
     * @param id   registry ID passed to the platform renderer
     * @return object at the origin with its native render node attached
     */
    static UnitySceneObject minecraft(SceneElementKind kind, String name, String id) {
        GameObjectType type = switch (kind) {
            case BLOCK -> GameObjectType.MINECRAFT_BLOCK;
            case ITEM -> GameObjectType.MINECRAFT_ITEM;
            case ENTITY -> GameObjectType.MINECRAFT_ENTITY;
        };
        return new UnitySceneObject(name, type, id);
    }

    /**
     * Copies the node and all descendants in hierarchy order.
     *
     * @return detached subtree with independent transforms, sharing the immutable mesh and material
     */
    UnitySceneObject copySubtree() {
        UnitySceneObject copy = new UnitySceneObject(name, type, minecraftId);
        MutableVec3 value = position(new MutableVec3());
        copy.setPosition(value.x(), value.y(), value.z());
        copy.restoreRotation(rotation(new MutableQuat()), angles(value));
        scale(value);
        copy.setScale(value.x(), value.y(), value.z());
        origin(value);
        copy.setOrigin(value.x(), value.y(), value.z());
        copy.setVisible(visible()).setMesh(mesh()).setMaterial(material());
        for (SceneNode child : children()) {
            if (child instanceof UnitySceneObject object) copy.addChild(object.copySubtree());
        }
        return copy;
    }

    /**
     * Reads the local XYZ angles from the current orientation, preserving values typed in the Inspector.
     *
     * @return Euler angles in degrees in a new vector
     */
    MutableVec3 angles() {
        return angles(new MutableVec3());
    }

    /**
     * Extracts angles from {@code R = Rz * Ry * Rx}. Away from gimbal lock the angles are
     * {@code x = atan2(R21, R22)}, {@code y = atan2(-R20, hypot(R00, R10))} and
     * {@code z = atan2(R10, R00)}. The R entries are indexed from zero.
     * At gimbal lock, X is set to zero and Z absorbs the combined rotation.
     *
     * @param out destination for the XYZ angles in degrees
     * @return the populated destination
     */
    MutableVec3 angles(MutableVec3 out) {
        MutableQuat current = rotation(new MutableQuat());
        if (current.x() == anglesRotation.x() && current.y() == anglesRotation.y()
                && current.z() == anglesRotation.z() && current.w() == anglesRotation.w()) return out.set(angles);

        double x = current.x(), y = current.y(), z = current.z(), w = current.w();
        double sinY = 2 * (w * y - x * z);
        double r00 = 1 - 2 * (y * y + z * z);
        double r10 = 2 * (w * z + x * y);
        double cosY = Math.hypot(r00, r10);
        if (cosY < 1e-6) {
            double combinedZ = Math.atan2(2 * (w * z - x * y), 1 - 2 * (x * x + z * z));
            return out.set(0, Math.copySign(90.0f, (float) sinY), (float) Math.toDegrees(combinedZ));
        }
        return out.set((float) Math.toDegrees(Math.atan2(2 * (w * x + y * z), 1 - 2 * (x * x + y * y))),
                (float) Math.toDegrees(Math.atan2(sinY, cosY)),
                (float) Math.toDegrees(Math.atan2(r10, r00)));
    }

    /**
     * Applies the local rotation as {@code q = qZ(z) * qY(y) * qX(x)}, where each {@code qAxis}
     * is the quaternion for rotation about that axis and the angles are converted to radians.
     *
     * @param x rotation about X in degrees
     * @param y rotation about Y in degrees
     * @param z rotation about Z in degrees
     */
    void setAngles(float x, float y, float z) {
        angles.set(x, y, z);
        MutableQuat pitch = new MutableQuat().setAxisAngle(X, Math.toRadians(x));
        MutableQuat yaw = new MutableQuat().setAxisAngle(Y, Math.toRadians(y));
        MutableQuat roll = new MutableQuat().setAxisAngle(Z, Math.toRadians(z));
        anglesRotation.setProduct(roll, yaw.setProduct(yaw, pitch));
        setRotation(anglesRotation);
    }

    /**
     * Restores a captured orientation and its Inspector representation.
     *
     * @param rotation      exact local quaternion
     * @param displayAngles matching XYZ angles in degrees
     */
    void restoreRotation(MutableQuat rotation, MutableVec3 displayAngles) {
        angles.set(displayAngles);
        anglesRotation.set(rotation);
        setRotation(rotation);
    }

    /**
     * Creates a lit, opaque surface.
     *
     * @param argb base color in ARGB order
     * @return the material
     */
    private static Material material(int argb) {
        return new Material(argb, null, Material.Shading.LIT, Material.AlphaMode.OPAQUE,
                0.5f, Material.Culling.NONE);
    }

    /**
     * @return a unit cube with one outward normal per face
     */
    private static Mesh3D cube() {
        float lo = -0.5f, hi = 0.5f;
        float[] positions = {
                hi, lo, hi, hi, lo, lo, hi, hi, lo, hi, hi, hi,
                lo, lo, lo, lo, lo, hi, lo, hi, hi, lo, hi, lo,
                lo, hi, hi, hi, hi, hi, hi, hi, lo, lo, hi, lo,
                lo, lo, lo, hi, lo, lo, hi, lo, hi, lo, lo, hi,
                lo, lo, hi, hi, lo, hi, hi, hi, hi, lo, hi, hi,
                hi, lo, lo, lo, lo, lo, lo, hi, lo, hi, hi, lo
        };
        int[] directions = {1, 0, 0, -1, 0, 0, 0, 1, 0, 0, -1, 0, 0, 0, 1, 0, 0, -1};
        float[] normals = new float[positions.length];
        int[] indices = new int[36];
        for (int face = 0; face < 6; face++) {
            int vertex = face * 4;
            for (int corner = 0; corner < 4; corner++) {
                for (int axis = 0; axis < 3; axis++)
                    normals[(vertex + corner) * 3 + axis] = directions[face * 3 + axis];
            }
            quad(indices, face * 6, vertex, vertex + 1, vertex + 2, vertex + 3);
        }
        return new Mesh3D(positions, normals, null, null, indices);
    }

    /**
     * Builds two joined hemispheres with normals {@code n = (sin(phi) cos(theta), cos(phi), sin(phi) sin(theta))}.
     * Here {@code phi} is the angle from the top pole and {@code theta} is the angle around Y.
     * A vertex is {@code 0.5 * n + (0, offset, 0)}, where the offset is positive for the upper cap and negative
     * for the lower cap. The equator rings bound the cylinder between the caps.
     *
     * @param offset distance in scene units from the origin to each cap center, zero for a sphere
     * @return indexed sphere or capsule with outward normals
     */
    private static Mesh3D roundedMesh(float offset) {
        int columns = 24;
        int halfRows = 8;
        int rows = 2 * halfRows + (offset == 0 ? 0 : 1);
        float[] positions = new float[(rows + 1) * columns * 3];
        float[] normals = new float[positions.length];
        int[] indices = new int[rows * columns * 6];
        for (int row = 0; row <= rows; row++) {
            boolean top = row <= halfRows;
            int latitude = top ? row : row - (rows - 2 * halfRows);
            double phi = Math.PI * latitude / (2 * halfRows);
            for (int column = 0; column < columns; column++) {
                double theta = 2 * Math.PI * column / columns;
                int vertex = row * columns + column;
                int component = vertex * 3;
                normals[component] = (float) (Math.sin(phi) * Math.cos(theta));
                normals[component + 1] = (float) Math.cos(phi);
                normals[component + 2] = (float) (Math.sin(phi) * Math.sin(theta));
                positions[component] = 0.5f * normals[component];
                positions[component + 1] = 0.5f * normals[component + 1] + (top ? offset : -offset);
                positions[component + 2] = 0.5f * normals[component + 2];
                if (row < rows) {
                    int next = row * columns + (column + 1) % columns;
                    quad(indices, vertex * 6, vertex, next, next + columns, vertex + columns);
                }
            }
        }
        return new Mesh3D(positions, normals, null, null, indices);
    }

    /**
     * Writes two triangles with a shared diagonal.
     *
     * @param indices destination index array
     * @param offset  first index to write
     * @param a       first vertex
     * @param b       second vertex
     * @param c       third vertex
     * @param d       fourth vertex
     */
    private static void quad(int[] indices, int offset, int a, int b, int c, int d) {
        indices[offset] = a;
        indices[offset + 1] = b;
        indices[offset + 2] = c;
        indices[offset + 3] = a;
        indices[offset + 4] = c;
        indices[offset + 5] = d;
    }
}

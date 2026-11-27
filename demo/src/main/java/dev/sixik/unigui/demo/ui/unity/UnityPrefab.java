package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.BlockSpec;
import dev.sixik.unigui.api.scene.SceneNode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dev.sixik.unigui.demo.ui.unity.UnityEditorHierarchyView.GameObjectType;

/**
 * A saved template for a demo scene subtree.
 */
final class UnityPrefab {
    /**
     * Identifies prefab drags from the Project panel.
     */
    static final String DRAG_TYPE = "unity.prefab";
    /**
     * Format used before any objects could refer to a Minecraft registry.
     */
    private static final int FORMAT_V1 = 0x55504601;
    /**
     * File signature and format version.
     */
    private static final int FORMAT = 0x55504602;
    /**
     * Upper bound on nesting for the recursive file reader and writer.
     */
    private static final int MAX_DEPTH = 128;
    /**
     * Stable identifier used as the filename on disk.
     */
    private final UUID id;
    /**
     * Display name without the .prefab extension.
     */
    private final String name;
    /**
     * Private snapshot of the root and descendants.
     */
    private final Node root;

    /**
     * Creates a prefab record with a captured subtree.
     *
     * @param id   stable file identifier
     * @param name asset name without the extension
     * @param root captured root
     */
    private UnityPrefab(UUID id, String name, Node root) {
        this.id = id;
        this.name = name;
        this.root = root;
    }

    /**
     * Captures a scene object and its descendants without retaining live nodes.
     *
     * @param name   asset name without the extension
     * @param object root of the scene subtree
     * @return a detached template with a new asset identifier
     * @throws IOException if the subtree exceeds the format's depth limit
     */
    static UnityPrefab capture(String name, UnitySceneObject object) throws IOException {
        return new UnityPrefab(UUID.randomUUID(), name, Node.capture(object, 0));
    }

    /**
     * @return stable identifier of the file containing this template
     */
    UUID id() {
        return id;
    }

    /**
     * @return display name without the extension, also used for new instance roots
     */
    String name() {
        return name;
    }

    /**
     * Changes the asset name while preserving identity and content.
     *
     * @param name new asset name without the extension
     * @return the renamed asset
     */
    UnityPrefab renamed(String name) {
        return new UnityPrefab(id, name, root);
    }

    /**
     * @return a fresh, detached copy of the prefab subtree with the asset name at its root
     */
    UnitySceneObject instantiate() {
        UnitySceneObject object = root.instantiate();
        object.rename(name);
        return object;
    }

    /**
     * Writes the template data, using the storage filename for its identifier.
     *
     * @param output destination stream
     * @throws IOException if a name is too long for the format or a write fails
     */
    void write(DataOutputStream output) throws IOException {
        output.writeInt(FORMAT);
        output.writeUTF(name);
        root.write(output);
    }

    /**
     * Reads and validates one complete template.
     *
     * @param id    identifier decoded from the storage filename
     * @param input source stream
     * @return the decoded prefab
     * @throws IOException              for unsupported data or an incomplete file
     * @throws IllegalArgumentException for an unknown object type or invalid quaternion
     */
    static UnityPrefab read(UUID id, DataInputStream input) throws IOException {
        int format = input.readInt();
        if (format != FORMAT && format != FORMAT_V1) throw new IOException("Unsupported prefab format");
        String name = input.readUTF();
        if (name.isBlank()) throw new IOException("Missing prefab name");
        Node root = Node.read(input, 0, format != FORMAT_V1);
        if (input.read() != -1) throw new IOException("Extra data after prefab");
        return new UnityPrefab(id, name, root);
    }

    /**
     * Captured local values of a scene object. The vectors and quaternion are private copies.
     *
     * @param name        object label
     * @param type        demo object kind, which determines its mesh and material
     * @param minecraftId registry ID for native Minecraft content, or null
     * @param visible     local visibility flag
     * @param position    local position in scene units
     * @param rotation    local orientation
     * @param angles      local Euler angles in degrees, for the Inspector
     * @param scale       local scale factors
     * @param origin      local pivot in scene units
     * @param children    child snapshots in hierarchy order
     */
    private record Node(String name, GameObjectType type, String minecraftId, boolean visible, MutableVec3 position,
                        MutableQuat rotation,
                        MutableVec3 angles, MutableVec3 scale, MutableVec3 origin, List<Node> children) {
        /**
         * Copies the editable values and child order from a live object.
         *
         * @param object node to capture
         * @param depth  nesting relative to the saved root
         * @return private snapshot of the subtree
         * @throws IOException if the maximum file depth is exceeded
         */
        static Node capture(UnitySceneObject object, int depth) throws IOException {
            checkDepth(depth);
            List<Node> children = new ArrayList<>();
            for (SceneNode child : object.children()) {
                if (child instanceof UnitySceneObject editable) children.add(capture(editable, depth + 1));
            }
            return new Node(object.name(), object.type(), object.minecraftId(), object.visible(), object.position(new MutableVec3()),
                    object.rotation(new MutableQuat()), object.angles(), object.scale(new MutableVec3()),
                    object.origin(new MutableVec3()), List.copyOf(children));
        }

        /**
         * @return fresh subtree with the captured local transforms and visibility
         */
        UnitySceneObject instantiate() {
            UnitySceneObject object = new UnitySceneObject(name, type, minecraftId);
            object.setVisible(visible);
            object.setPosition(position.x(), position.y(), position.z());
            object.restoreRotation(rotation, angles);
            object.setScale(scale.x(), scale.y(), scale.z());
            object.setOrigin(origin.x(), origin.y(), origin.z());
            for (Node child : children) object.addChild(child.instantiate());
            return object;
        }

        /**
         * Writes the object and then its children in scene order.
         *
         * @param output destination stream
         * @throws IOException if writing fails
         */
        void write(DataOutputStream output) throws IOException {
            output.writeUTF(name);
            output.writeUTF(type.name());
            if (type.isMinecraft()) output.writeUTF(minecraftId);
            output.writeBoolean(visible);
            writeVector(output, position);
            output.writeFloat(rotation.x());
            output.writeFloat(rotation.y());
            output.writeFloat(rotation.z());
            output.writeFloat(rotation.w());
            writeVector(output, angles);
            writeVector(output, scale);
            writeVector(output, origin);
            output.writeInt(children.size());
            for (Node child : children) child.write(output);
        }

        /**
         * Reads one object and its declared children.
         *
         * @param input            source stream
         * @param depth            nesting relative to the saved root
         * @param withMinecraftIds whether the format permits Minecraft object types and their IDs
         * @return decoded subtree
         * @throws IOException if the data is incomplete or values fall outside the file format
         */
        static Node read(DataInputStream input, int depth, boolean withMinecraftIds) throws IOException {
            checkDepth(depth);
            String name = input.readUTF();
            GameObjectType type = GameObjectType.valueOf(input.readUTF());
            String minecraftId = null;
            if (type.isMinecraft()) {
                if (!withMinecraftIds) throw new IOException("Minecraft object in version 1 prefab");
                minecraftId = input.readUTF();
                if (minecraftId.isBlank()) throw new IOException("Missing Minecraft scene element ID");
                if (type == GameObjectType.MINECRAFT_BLOCK) BlockSpec.parse(minecraftId);
            }
            boolean visible = input.readBoolean();
            MutableVec3 position = readVector(input);
            MutableQuat rotation = new MutableQuat().set(input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat());
            MutableVec3 angles = readVector(input);
            MutableVec3 scale = readVector(input);
            MutableVec3 origin = readVector(input);
            int count = input.readInt();
            if (count < 0) throw new IOException("Invalid prefab child count");
            List<Node> children = new ArrayList<>();
            for (int i = 0; i < count; i++) children.add(read(input, depth + 1, withMinecraftIds));
            return new Node(name, type, minecraftId, visible, position, rotation, angles, scale, origin, List.copyOf(children));
        }

        /**
         * Validates nesting before visiting a node.
         *
         * @param depth nesting relative to the root
         * @throws IOException if the node is below the supported depth
         */
        private static void checkDepth(int depth) throws IOException {
            if (depth > MAX_DEPTH) throw new IOException("Prefab hierarchy is too deep");
        }

        /**
         * Writes a vector in XYZ order.
         *
         * @param output destination stream
         * @param vector local transform components
         * @throws IOException if writing fails
         */
        private static void writeVector(DataOutputStream output, MutableVec3 vector) throws IOException {
            output.writeFloat(vector.x());
            output.writeFloat(vector.y());
            output.writeFloat(vector.z());
        }

        /**
         * Reads and validates three local transform components.
         *
         * @param input source stream
         * @return the decoded vector
         * @throws IOException for truncated or non-finite values
         */
        private static MutableVec3 readVector(DataInputStream input) throws IOException {
            float x = input.readFloat(), y = input.readFloat(), z = input.readFloat();
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z))
                throw new IOException("Invalid prefab transform");
            return new MutableVec3(x, y, z);
        }
    }
}

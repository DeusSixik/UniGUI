package dev.sixik.unigui.demo.ui.unity;

import java.util.EnumSet;
import java.util.List;

import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.FogVolume;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.LightNode;
import dev.sixik.unigui.api.scene.PointLight;
import dev.sixik.unigui.api.scene.SpotLight;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.SceneMaterialEffect;
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
     * Light component settings, or null without a Light component. Light objects start with one.
     */
    private UnityLight light;
    /**
     * Rotator component speed around the local Y axis in degrees per second, or NaN without a Rotator.
     */
    private float rotatorSpeed = Float.NaN;
    /**
     * Mesh Renderer shape, or null without a Mesh Renderer.
     */
    private GameObjectType meshShape;
    /**
     * Material Effect component settings, or null without the component.
     */
    private UnityMaterialEffect effect;
    /**
     * Components switched off by their Inspector checkbox. They keep their settings but have no effect.
     */
    private final EnumSet<Component> disabled = EnumSet.noneOf(Component.class);

    /**
     * Components that can be added, removed and switched off; Transform is fixed.
     */
    enum Component {
        MESH_RENDERER, LIGHT, ROTATOR, MATERIAL_EFFECT, FOG_VOLUME
    }
    /**
     * Fog Volume component settings, or null without the component.
     */
    private UnityFog fog;
    /**
     * Render node of the enabled Fog Volume, or null.
     */
    private FogVolume fogNode;
    /**
     * Render node of a point or spot light, or null.
     */
    private LightNode lightNode;

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
            case MINECRAFT_BLOCK -> addChild(new BlockNode(blockPreviewState(minecraftId)).setPosition(-0.5f, -0.5f, -0.5f));
            case MINECRAFT_ITEM -> {
                var item = new ItemNode(minecraftId);
                if (minecraftId.equals("minecraft:furnace")) item.setDisplay(ItemNode.Display.GUI);
                addChild(item);
            }
            case MINECRAFT_ENTITY -> addChild(new EntityNode(minecraftId));
            case LIGHT -> setLight(UnityLight.defaults(UnityLight.Kind.POINT));
            case FOG_VOLUME -> setFog(UnityFog.defaults(FogVolume.Shape.BOX));
            default -> {
            }
        }
        meshShape = MESH_SHAPES.contains(type) ? type : null;
        setMesh(shapeMesh(meshShape));
        setMaterial(material(shapeColor(type)));
    }

    /**
     * Faces directional props toward the default scene and preview cameras, which look from positive Z.
     *
     * @param id registry identifier or a block specification with explicit state
     * @return block specification for the visual node
     */
    private static String blockPreviewState(String id) {
        return switch (id) {
            case "minecraft:furnace", "minecraft:lit_furnace", "minecraft:chest", "minecraft:trapped_chest",
                    "minecraft:ender_chest" -> id + "[facing=south]";
            default -> id;
        };
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
        copy.setVisible(visible());
        copy.setDisabledComponents(disabled);
        copy.setMeshShape(meshShape);
        copy.setMaterial(material());
        copy.setLight(light);
        copy.setFog(fog);
        copy.setRotatorSpeed(rotatorSpeed);
        copy.setMaterialEffectSettings(effect);
        for (SceneNode child : children()) {
            if (child instanceof UnitySceneObject object) copy.addChild(object.copySubtree());
        }
        return copy;
    }

    /**
     * Creates a light with the defaults of its type. Directional lights start tilted down like Unity's,
     * spotlights start pointing straight down.
     *
     * @param name display name
     * @param kind light type
     * @return detached light object at the origin
     */
    static UnitySceneObject light(String name, UnityLight.Kind kind) {
        UnitySceneObject object = new UnitySceneObject(name, GameObjectType.LIGHT);
        object.setLight(UnityLight.defaults(kind));
        switch (kind) {
            case DIRECTIONAL -> object.setAngles(-50, -30, 0);
            case SPOT -> object.setAngles(-90, 0, 0);
            default -> {
            }
        }
        return object;
    }

    /**
     * Creates a fog volume two units wide and deep and one unit high: the node has unit half-extents, so the scale
     * is half the size.
     *
     * @param name  display name
     * @param shape volume shape
     * @return detached fog object at the origin
     */
    static UnitySceneObject fog(String name, FogVolume.Shape shape) {
        UnitySceneObject object = new UnitySceneObject(name, GameObjectType.FOG_VOLUME);
        object.setFog(UnityFog.defaults(shape));
        object.setScale(1, 0.5f, 1);
        return object;
    }

    /**
     * @return Fog Volume settings, or null without the component
     */
    UnityFog fog() {
        return fog;
    }

    /**
     * Adds, changes or removes the Fog Volume component and updates its render node. Any object can carry fog; the
     * volume follows the object's transform.
     *
     * @param settings new settings, or null to remove the component
     */
    void setFog(UnityFog settings) {
        fog = settings;
        applyFog();
    }

    /**
     * Creates, updates or removes the fog node. A shape change replaces the node, since its shape is fixed.
     */
    private void applyFog() {
        UnityFog settings = fog;
        if (settings == null || disabled.contains(Component.FOG_VOLUME)) {
            if (fogNode != null) {
                fogNode.detach();
                fogNode = null;
            }
            return;
        }
        if (fogNode != null && fogNode.shape() == settings.shape()) {
            settings.apply(fogNode);
            return;
        }
        if (fogNode != null) fogNode.detach();
        fogNode = settings.toNode();
        addChild(fogNode);
    }

    /**
     * @return Light component settings, or null without a Light component
     */
    UnityLight light() {
        return light;
    }

    /**
     * Adds, replaces or removes the Light component and updates the render node. Any object can carry a light.
     * A directional light has no node: the editor writes it to the scene's single directional light instead.
     *
     * @param settings new settings, or null to remove the component
     */
    void setLight(UnityLight settings) {
        light = settings;
        applyLight();
    }

    /**
     * Creates, updates or removes the render node of an enabled point or spot light.
     */
    private void applyLight() {
        UnityLight settings = light;
        if (settings == null || settings.kind() == UnityLight.Kind.DIRECTIONAL || disabled.contains(Component.LIGHT)) {
            if (lightNode != null) {
                lightNode.detach();
                lightNode = null;
            }
            return;
        }
        var source = settings.kind() == UnityLight.Kind.SPOT
                ? new SpotLight(settings.colorRgb(), settings.intensity(), settings.range(),
                (float) Math.toRadians(settings.innerSpotAngle() * 0.5), (float) Math.toRadians(settings.spotAngle() * 0.5))
                .withShadows(settings.toSpotShadows())
                : new PointLight(settings.colorRgb(), settings.intensity(), settings.range());
        if (lightNode == null) {
            lightNode = new LightNode(source);
            addChild(lightNode);
        } else {
            lightNode.setLight(source);
        }
    }

    /**
     * Primitive shapes a Mesh Renderer can draw.
     */
    static final List<GameObjectType> MESH_SHAPES = List.of(GameObjectType.CUBE, GameObjectType.SPHERE,
            GameObjectType.CAPSULE, GameObjectType.PLANE);

    /**
     * @return the Mesh Renderer shape, or null without a Mesh Renderer
     */
    GameObjectType meshShape() {
        return meshShape;
    }

    /**
     * Adds, changes or removes the Mesh Renderer. The surface takes the shape's default color.
     *
     * @param shape one of {@link #MESH_SHAPES}, or null to remove the component
     * @throws IllegalArgumentException for a type that is not a primitive shape
     */
    void setMeshShape(GameObjectType shape) {
        if (shape != null && !MESH_SHAPES.contains(shape)) throw new IllegalArgumentException("Not a mesh shape: " + shape);
        if (shape == meshShape) return;
        meshShape = shape;
        applyMesh();
        setMaterial(material(shapeColor(shape)));
    }

    /**
     * Shows the Mesh Renderer shape while the component is enabled.
     */
    private void applyMesh() {
        setMesh(meshShape != null && !disabled.contains(Component.MESH_RENDERER) ? shapeMesh(meshShape) : null);
    }

    /**
     * @param component optional component
     * @return false when its checkbox is off
     */
    boolean componentEnabled(Component component) {
        return !disabled.contains(component);
    }

    /**
     * Switches a component on or off, keeping its settings.
     *
     * @param component optional component
     * @param enabled   new checkbox state
     */
    void setComponentEnabled(Component component, boolean enabled) {
        if (enabled == !disabled.contains(component)) return;
        if (enabled) disabled.remove(component);
        else disabled.add(component);
        applyComponents();
    }

    /**
     * @return components that are switched off, as a copy
     */
    EnumSet<Component> disabledComponents() {
        return EnumSet.copyOf(disabled);
    }

    /**
     * Replaces the switched-off components.
     *
     * @param components components to switch off
     */
    void setDisabledComponents(java.util.Set<Component> components) {
        disabled.clear();
        disabled.addAll(components);
        applyComponents();
    }

    /**
     * Applies the enabled state to the mesh, the light node and the material effect.
     */
    private void applyComponents() {
        applyMesh();
        applyLight();
        applyFog();
        appliedEffectStart = Float.NaN;
        tickMaterialEffect(lastSceneTime);
    }

    /**
     * Scene time of the last effect update, for re-applying it when the component changes.
     */
    private float lastSceneTime;
    /**
     * Loop start of the effect currently on the node, or NaN when it must be rebuilt.
     */
    private float appliedEffectStart = Float.NaN;

    /**
     * @return Material Effect settings, or null without the component
     */
    UnityMaterialEffect materialEffectSettings() {
        return effect;
    }

    /**
     * Adds, changes or removes the Material Effect component.
     *
     * @param settings shader and parameters, or null to remove the component
     */
    void setMaterialEffectSettings(UnityMaterialEffect settings) {
        effect = settings;
        appliedEffectStart = Float.NaN;
        tickMaterialEffect(lastSceneTime);
    }

    /**
     * Keeps the shader effect on the node timed to the loop containing the scene time. The node only changes when
     * a new loop starts or the settings change.
     *
     * @param sceneTime scene material time in seconds
     */
    void tickMaterialEffect(float sceneTime) {
        lastSceneTime = sceneTime;
        if (effect == null || disabled.contains(Component.MATERIAL_EFFECT)) {
            appliedEffectStart = Float.NaN;
            applyMaterialEffect(null);
            return;
        }
        float start = (float) Math.floor(sceneTime / effect.period()) * effect.period();
        if (start == appliedEffectStart) return;
        appliedEffectStart = start;
        applyMaterialEffect(effect.toEffect(sceneTime));
    }

    /**
     * Sets the effect on this object and on its Minecraft content. A block, item or entity is a child node of the
     * object, and the renderer reads the effect of each node separately; it is not inherited from the parent.
     * Child objects keep their own components, and light nodes have no surface.
     *
     * @param value shader and parameters, or null for the default shader
     */
    private void applyMaterialEffect(SceneMaterialEffect value) {
        setMaterialEffect(value);
        for (SceneNode child : children()) {
            if (child instanceof BlockNode || child instanceof ItemNode || child instanceof EntityNode) {
                child.setMaterialEffect(value);
            }
        }
    }

    /**
     * @param shape primitive type
     * @return shared mesh of the shape, or null for other types
     */
    private static Mesh3D shapeMesh(GameObjectType shape) {
        return shape == null ? null : switch (shape) {
            case CUBE -> CUBE;
            case SPHERE -> SPHERE;
            case CAPSULE -> CAPSULE;
            case PLANE -> PLANE;
            default -> null;
        };
    }

    /**
     * @param shape primitive type, or null
     * @return default surface color of the shape
     */
    private static int shapeColor(GameObjectType shape) {
        return shape == null ? 0xFFFFFFFF : switch (shape) {
            case CUBE -> 0xFF62ADD7;
            case SPHERE -> 0xFFBF88DA;
            case CAPSULE -> 0xFF71C8A5;
            case PLANE -> 0xFF495361;
            default -> 0xFFFFFFFF;
        };
    }

    /**
     * @return Rotator speed in degrees per second, or NaN without a Rotator component
     */
    float rotatorSpeed() {
        return rotatorSpeed;
    }

    /**
     * Adds, changes or removes the Rotator component, which spins the object around its local Y axis in Play mode.
     *
     * @param degreesPerSecond speed, or NaN to remove the component
     */
    void setRotatorSpeed(float degreesPerSecond) {
        rotatorSpeed = Float.isNaN(degreesPerSecond) ? Float.NaN : Math.max(-3600, Math.min(3600, degreesPerSecond));
    }

    /**
     * Advances the Rotator: the Y angle grows by {@code speed * seconds}.
     *
     * @param seconds elapsed Play time
     */
    void tickRotator(float seconds) {
        if (Float.isNaN(rotatorSpeed) || seconds <= 0 || disabled.contains(Component.ROTATOR)) return;
        MutableVec3 current = angles();
        float y = (current.y() + rotatorSpeed * seconds) % 360;
        setAngles(current.x(), y, current.z());
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

package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;

import java.util.Objects;

/**
 * A Minecraft block with its local origin at the lower corner of the unit cell.
 *
 * <p>Pass a {@link BlockSpec} string to the constructor or an object from the current game version to {@code of}.</p>
 * <pre>{@code
 * new BlockNode("minecraft:furnace[facing=north]");
 * new BlockNode("minecraft:wool@14");             // 1.7.10 and 1.12.2
 * BlockNode.of(Blocks.FURNACE.defaultBlockState()); // 1.20+ and 26.x
 * BlockNode.of(Blocks.FURNACE.getDefaultState());   // 1.12.2
 * BlockNode.of(Blocks.furnace, 3);                  // 1.7.10
 * }</pre>
 * <p>The constructor checks syntax. The platform resolves the block on demand during rendering or
 * when queried. For typed access use the platform helper, such as {@code MinecraftScene.state(node)}.</p>
 */
@WikiPage(
        title = "BlockNode", slug = "block-node",
        category = "API/3D Scene", order = 130,
        summary = "A single Minecraft block in the scene graph.")
@WikiSection(title = "Native and portable sources", order = 10, markdown = """
        Use `BlockNode(String)` for XAML-friendly configurations or `BlockNode.of(...)` for native block objects.
        The state helper is `MinecraftScene.state(node)` on modern versions, `CleanroomScene.state(node)` on 1.12.2
        and `GtnhScene.state(node)` on 1.7.10.
        """)
@WikiExample(title = "Place a south-facing furnace", code = """
        BlockNode furnace = new BlockNode("minecraft:furnace[facing=south]");
        furnace.centerOrigin().setPosition(2, 0, 1);
        viewport.scene().addRoot(furnace);
        """)
@WikiLink(label = "SceneNode", target = "page:scene-node")
@WikiLink(label = "BlockSpec", target = "page:block-spec")
@WikiLink(label = "StructureNode", target = "page:structure-node")
@WikiLink(label = "ScenePlatform", target = "page:scene-platform")
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@AiDoc(
        summary = "A single Minecraft block in the scene graph.",
        purpose = "Preview and transform a block, including supported block entity visuals.",
        criticalRules = {
                "The local origin is at the lower corner. `centerOrigin()` selects the center of the base.",
                "ID syntax is validated immediately. Platform data resolves lazily. Use `requireNativeState` to propagate resolution errors.",
                "Use the block-state properties for the target version. Furnace lighting is modeled differently between versions.",
                "Call `invalidateContent()` after editing native block entity data in an on-change viewport."
        },
        related = {"SceneNode", "BlockSpec", "StructureNode", "ScenePlatform", "Scene3D"},
        examples = @AiDoc.Example(title = "Place a south-facing furnace", code = """
                BlockNode furnace = new BlockNode("minecraft:furnace[facing=south]");
                furnace.centerOrigin().setPosition(2, 0, 1);
                viewport.scene().addRoot(furnace);
                """))
public final class BlockNode extends SceneNode {
    /**
     * Bounds of a full block in scene units.
     */
    private static final Aabb BLOCK_BOUNDS = new Aabb(0, 0, 0, 1, 1, 1);

    /**
     * Parsed specification, or null for native input.
     */
    private BlockSpec spec;
    /**
     * Native source object, or null for a string.
     */
    private Object nativeSource;
    /**
     * Explicit legacy metadata, or -1.
     */
    private int nativeMeta = -1;
    /**
     * Cache of the resolved state.
     */
    private final ResolvedContent state = new ResolvedContent();
    /**
     * Block entity for the last resolved state, or null.
     */
    private Object blockEntity;
    /**
     * State associated with {@link #blockEntity}.
     */
    private Object blockEntityState;
    /**
     * Platform that created {@link #blockEntity}.
     */
    private ScenePlatform blockEntityPlatform;

    /**
     * Creates a node from a block specification.
     *
     * @param state text such as {@code minecraft:furnace[facing=north]} or {@code minecraft:wool@14}.
     * @throws IllegalArgumentException if the syntax is invalid.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 10,
            summary = "Creates a node from a block specification.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the syntax is invalid.")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 10,
            summary = "Creates a node from a block specification.")
    public BlockNode(
            @WikiParameter(description = "text such as `minecraft:furnace[facing=north]` or `minecraft:wool@14`.") String state) {
        this.spec = BlockSpec.parse(state);
    }

    /**
     * Creates a node from a version-specific object.
     *
     * @param state native block state, native block or block specification string.
     * @return a new node.
     * @throws NullPointerException if state is null.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 20,
            summary = "Creates a node from a version-specific object.")
    @WikiReturn("a new node")
    @WikiThrows(type = NullPointerException.class, description = "if state is null.")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 20,
            summary = "Creates a node from a version-specific object.")
    public static BlockNode of(
            @WikiParameter(description = "native block state, native block or block specification string.") Object state) {
        if (state instanceof String text) return new BlockNode(text);
        return of(state, -1);
    }

    /**
     * Creates a node from a native block and legacy metadata.
     *
     * @param block native block.
     * @param meta metadata, or -1 for the default.
     * @return a new node.
     * @throws NullPointerException if block is null.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 20,
            summary = "Creates a node from a native block and legacy metadata.")
    @WikiReturn("a new node")
    @WikiThrows(type = NullPointerException.class, description = "if block is null.")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 20,
            summary = "Creates a node from a native block and legacy metadata.")
    public static BlockNode of(
            @WikiParameter(description = "native block.") Object block,
            @WikiParameter(description = "metadata, or -1 for the default.") int meta) {
        BlockNode node = new BlockNode((BlockSpec) null);
        node.nativeSource = Objects.requireNonNull(block, "block");
        node.nativeMeta = Math.max(-1, meta);
        return node;
    }

    /**
     * Creates a node from an already parsed specification.
     *
     * @param spec parsed specification, or null for native input.
     */
    private BlockNode(BlockSpec spec) {
        this.spec = spec;
    }

    /**
     * Reads the parsed specification.
     *
     * @return the specification, or null for native input.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 30,
            summary = "Reads the parsed specification.")
    @WikiReturn("the specification, or null for native input")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 30,
            summary = "Reads the parsed specification.")
    public BlockSpec spec() {
        return spec;
    }

    /**
     * Reads the input object.
     *
     * @return the native input object, or null for a string source.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 40,
            summary = "Reads the input object.")
    @WikiReturn("the native input object, or null for a string source")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 40,
            summary = "Reads the input object.")
    public Object nativeSource() {
        return nativeSource;
    }

    /**
     * Replaces the block using a specification. Passing the same string leaves the cache intact.
     *
     * @param state block specification.
     * @return this node.
     * @throws IllegalArgumentException if the syntax is invalid.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 50,
            summary = "Replaces the block using a specification. Passing the same string leaves the cache intact.")
    @WikiReturn("this node")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the syntax is invalid.")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 50,
            summary = "Replaces the block using a specification. Passing the same string leaves the cache intact.")
    public BlockNode setState(
            @WikiParameter(description = "block specification.") String state) {
        BlockSpec next = BlockSpec.parse(state);
        if (spec != null && spec.text().equals(next.text())) return this;
        spec = next;
        nativeSource = null;
        nativeMeta = -1;
        sourceChanged();
        return this;
    }

    /**
     * Replaces the block from a native state or specification. Reusing the same native object preserves the cache.
     *
     * @param state native block state, native block or specification string.
     * @return this node.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 50,
            summary = "Replaces the block from a native state or specification. Reusing the same native object preserves the cache.")
    @WikiReturn("this node")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 50,
            summary = "Replaces the block from a native state or specification. Reusing the same native object preserves the cache.")
    public BlockNode setState(
            @WikiParameter(description = "native block state, native block or specification string.") Object state) {
        if (state instanceof String text) return setState(text);
        return setState(state, -1);
    }

    /**
     * Replaces the block using legacy metadata.
     *
     * @param block native block.
     * @param meta metadata, or -1 for the default.
     * @return this node.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 50,
            summary = "Replaces the block using legacy metadata.")
    @WikiReturn("this node")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 50,
            summary = "Replaces the block using legacy metadata.")
    public BlockNode setState(
            @WikiParameter(description = "native block.") Object block,
            @WikiParameter(description = "metadata, or -1 for the default.") int meta) {
        Objects.requireNonNull(block, "block");
        int normalized = Math.max(-1, meta);
        if (nativeSource == block && nativeMeta == normalized) return this;
        spec = null;
        nativeSource = block;
        nativeMeta = normalized;
        sourceChanged();
        return this;
    }

    /**
     * Resolves the state for the renderer or a platform helper.
     *
     * @param platform platform adapter, or null.
     * @return the native state, or null if unavailable or invalid.
     */
    @WikiMethod(section = "Resolution", sectionOrder = 20, order = 10,
            summary = "Resolves the state for the renderer or a platform helper.")
    @WikiReturn("the native state, or null if unavailable or invalid")
    @AiDoc(section = "Resolution", sectionOrder = 20, order = 10,
            summary = "Resolves the state for the renderer or a platform helper.")
    public Object nativeState(
            @WikiParameter(description = "platform adapter, or null.") ScenePlatform platform) {
        return state.get(platform, this::resolve, this, false);
    }

    /**
     * Resolves the state and propagates resolution errors, for example when validating XAML.
     *
     * @param platform platform adapter.
     * @return the native state, or null if temporarily unavailable.
     * @throws IllegalArgumentException if the block is unknown.
     * @throws UnsupportedOperationException if the platform does not support the specification.
     */
    @WikiMethod(section = "Resolution", sectionOrder = 20, order = 20,
            summary = "Resolves the state and propagates resolution errors, for example when validating XAML.")
    @WikiReturn("the native state, or null if temporarily unavailable")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the block is unknown.")
    @WikiThrows(type = UnsupportedOperationException.class, description = "if the platform does not support the specification.")
    @AiDoc(section = "Resolution", sectionOrder = 20, order = 20,
            summary = "Resolves the state and propagates resolution errors, for example when validating XAML.")
    public Object requireNativeState(
            @WikiParameter(description = "platform adapter.") ScenePlatform platform) {
        return state.get(Objects.requireNonNull(platform, "platform"), this::resolve, this, true);
    }

    /**
     * Reads the current state's block entity. A compatible state can reuse the object and preserve its data and animation.
     * Call {@link #invalidateContent()} after editing external block-entity data in an on-change viewport.
     *
     * @param platform platform adapter, or null.
     * @return the native block entity, or null.
     */
    @WikiMethod(section = "Resolution", sectionOrder = 20, order = 30,
            summary = "Reads the current state's block entity. A compatible state can reuse the object and preserve its data and animation. Call `invalidateContent()` after editing external block-entity data in an on-change viewport.")
    @WikiReturn("the native block entity, or null")
    @AiDoc(section = "Resolution", sectionOrder = 20, order = 30,
            summary = "Reads the current state's block entity. A compatible state can reuse the object and preserve its data and animation. Call `invalidateContent()` after editing external block-entity data in an on-change viewport.")
    public Object blockEntity(
            @WikiParameter(description = "platform adapter, or null.") ScenePlatform platform) {
        Object current = nativeState(platform);
        if (current == null) return null;
        if (current != blockEntityState || platform != blockEntityPlatform) {
            Object previous = platform == blockEntityPlatform ? blockEntity : null;
            blockEntity = platform.blockEntity(current, previous, 0, 0, 0);
            blockEntityState = current;
            blockEntityPlatform = platform;
        }
        return blockEntity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Aabb contentBounds() {
        return isAir() ? super.contentBounds() : BLOCK_BOUNDS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double intersectPickBounds(Ray ray, double entry, double limit, MutableVec3 cellOrigin) {
        if (isAir()) return super.intersectPickBounds(ray, entry, limit, cellOrigin);
        double distance = ray.intersectAabb(0, 0, 0, 1, 1, 1);
        if (distance <= limit) cellOrigin.set(0, 0, 0);
        return distance;
    }

    /**
     * Formats the source for logging and debugging.
     *
     * @return a description of the specification or native object.
     */
    @Override
    public String toString() {
        if (spec != null) return "BlockNode[" + spec + "]";
        return "BlockNode[" + nativeSource + (nativeMeta >= 0 ? "@" + nativeMeta : "") + "]";
    }

    /**
     * Checks whether the platform considers the block air. Without a platform the block is treated as solid.
     *
     * @return true for air.
     */
    private boolean isAir() {
        ScenePlatform platform = ScenePlatform.current();
        Object current = nativeState(platform);
        return current != null && platform.isAirBlock(current);
    }

    /**
     * Resolves the current source with the given platform.
     *
     * @param platform platform adapter.
     * @return the native block state, or null.
     */
    private Object resolve(ScenePlatform platform) {
        return spec != null ? platform.resolveBlock(spec) : platform.nativeBlock(nativeSource, nativeMeta);
    }

    /**
     * Clears the resolution cache and retains the block entity for potential reuse.
     */
    private void sourceChanged() {
        state.reset();
        invalidateContent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockNode setPosition(float x, float y, float z) {
        super.setPosition(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockNode setRotation(dev.sixik.unigui.api.math.QuatView value) {
        super.setRotation(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockNode setScale(float x, float y, float z) {
        super.setScale(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockNode setVisible(boolean visible) {
        super.setVisible(visible);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockNode setOrigin(float x, float y, float z) {
        super.setOrigin(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockNode centerOrigin() {
        super.centerOrigin();
        return this;
    }
}

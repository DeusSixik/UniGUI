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

import java.util.Objects;

/**
 * A Minecraft item model centered at the local origin.
 *
 * <pre>{@code
 * new ItemNode("minecraft:diamond_sword");
 * ItemNode.of(new ItemStack(Items.DIAMOND_SWORD));
 * }</pre>
 * <p>The platform copies native stacks when it resolves them. For typed access use the platform helper,
 * such as {@code MinecraftScene.stack(node)}. Unsupported platforms skip rendering and log a warning once.</p>
 */
@WikiPage(
        title = "ItemNode", slug = "item-node",
        category = "API/3D Scene", order = 140,
        summary = "A centered Minecraft item model in the scene graph.")
@WikiSection(title = "Display modes", order = 10, markdown = """
        Choose `FIXED` for frame-style placement, `GROUND` for a dropped-item look, or `GUI` for inventory styling.
        Scene transforms apply after the model placement. On modern versions use `MinecraftScene.stack(node)` for a
        typed view of the resolved stack.
        """)
@WikiExample(title = "Preview a sword", code = """
        ItemNode sword = new ItemNode("minecraft:diamond_sword");
        sword.setDisplay(ItemNode.Display.FIXED).setPosition(0, 1, 0);
        viewport.highlightObjects(true).scene().addRoot(sword);
        """)
@WikiLink(label = "SceneNode", target = "page:scene-node")
@WikiLink(label = "ScenePlatform", target = "page:scene-platform")
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@AiDoc(
        summary = "A centered Minecraft item model in the scene graph.",
        purpose = "Render an item preview with a selectable vanilla display placement and scene transform.",
        criticalRules = {
                "Pass a registry ID or use `ItemNode.of` with a native item or stack. The platform copies native stacks when resolving them.",
                "`Display.FIXED` is the default. Supported display placements vary by Minecraft version.",
                "Model bounds drive object picking and highlighting when available. A centered unit cube supplies fallback bounds while a model is unavailable."
        },
        related = {"SceneNode", "ScenePlatform", "Scene3D"},
        examples = @AiDoc.Example(title = "Preview a sword", code = """
                ItemNode sword = new ItemNode("minecraft:diamond_sword");
                sword.setDisplay(ItemNode.Display.FIXED).setPosition(0, 1, 0);
                viewport.highlightObjects(true).scene().addRoot(sword);
                """))
public final class ItemNode extends SceneNode {
    /**
     * Fallback bounds before the item's model bounds become available.
     */
    private static final Aabb FALLBACK_BOUNDS = new Aabb(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);

    /**
     * Item ID, or null for native input.
     */
    private String id;
    /**
     * Native source object, or null for an item ID.
     */
    private Object nativeSource;
    /**
     * Cache of the resolved item.
     */
    private final ResolvedContent item = new ResolvedContent();
    /**
     * Model placement relative to the local origin.
     */
    private Display display = Display.FIXED;

    /**
     * Vanilla model placement presets.
     */
    public enum Display {
        /**
         * Item-frame placement, with flat items upright and block models at half size.
         */
        FIXED,
        /**
         * Dropped-item placement, typically a quarter to half a block across.
         */
        GROUND,
        /**
         * Inventory icon placement, with an isometric view of block models.
         */
        GUI
    }

    /**
     * Creates a node from an item ID.
     *
     * @param id item ID such as {@code minecraft:diamond_sword}.
     * @throws IllegalArgumentException if id is blank.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 10,
            summary = "Creates a node from an item ID.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if id is blank.")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 10,
            summary = "Creates a node from an item ID.")
    public ItemNode(
            @WikiParameter(description = "item ID such as `minecraft:diamond_sword`.") String id) {
        this.id = requireId(id);
    }

    /**
     * Creates a node from a native stack or item.
     *
     * @param item stack, item or ID string.
     * @return a new node.
     * @throws NullPointerException if item is null.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 20,
            summary = "Creates a node from a native stack or item.")
    @WikiReturn("a new node")
    @WikiThrows(type = NullPointerException.class, description = "if item is null.")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 20,
            summary = "Creates a node from a native stack or item.")
    public static ItemNode of(
            @WikiParameter(description = "stack, item or ID string.") Object item) {
        if (item instanceof String text) return new ItemNode(text);
        ItemNode node = new ItemNode();
        node.nativeSource = Objects.requireNonNull(item, "item");
        return node;
    }

    /**
     * Creates an empty node for native input.
     */
    private ItemNode() {
    }

    /**
     * Reads the model placement.
     *
     * @return the placement, defaulting to {@link Display#FIXED}.
     */
    @WikiMethod(section = "Display", sectionOrder = 20, order = 10,
            summary = "Reads the model placement.")
    @WikiReturn("the placement, defaulting to `Display#FIXED`")
    @AiDoc(section = "Display", sectionOrder = 20, order = 10,
            summary = "Reads the model placement.")
    public Display display() {
        return display;
    }

    /**
     * Changes the model placement. On 1.7.10 only {@link Display#GROUND} changes the size.
     *
     * @param display new model placement.
     * @return this node.
     */
    @WikiMethod(section = "Display", sectionOrder = 20, order = 20,
            summary = "Changes the model placement. On 1.7.10 only `Display#GROUND` changes the size.")
    @WikiReturn("this node")
    @AiDoc(section = "Display", sectionOrder = 20, order = 20,
            summary = "Changes the model placement. On 1.7.10 only `Display#GROUND` changes the size.")
    public ItemNode setDisplay(
            @WikiParameter(description = "new model placement.") Display display) {
        Objects.requireNonNull(display, "display");
        if (this.display != display) {
            this.display = display;
            invalidateContent();
        }
        return this;
    }

    /**
     * Reads the item ID.
     *
     * @return the item ID, or null for native input.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 30,
            summary = "Reads the item ID.")
    @WikiReturn("the item ID, or null for native input")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 30,
            summary = "Reads the item ID.")
    public String id() {
        return id;
    }

    /**
     * Reads the native input object.
     *
     * @return the native input object, or null for an item ID.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 40,
            summary = "Reads the native input object.")
    @WikiReturn("the native input object, or null for an item ID")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 40,
            summary = "Reads the native input object.")
    public Object nativeSource() {
        return nativeSource;
    }

    /**
     * Replaces the item by ID. Passing the same ID preserves the cache.
     *
     * @param id item ID.
     * @return this node.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 50,
            summary = "Replaces the item by ID. Passing the same ID preserves the cache.")
    @WikiReturn("this node")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 50,
            summary = "Replaces the item by ID. Passing the same ID preserves the cache.")
    public ItemNode setItem(
            @WikiParameter(description = "item ID.") String id) {
        String next = requireId(id);
        if (next.equals(this.id)) return this;
        this.id = next;
        nativeSource = null;
        sourceChanged();
        return this;
    }

    /**
     * Replaces the item using a native object. Reusing the same object preserves the cache.
     *
     * @param item stack, item or ID string.
     * @return this node.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 50,
            summary = "Replaces the item using a native object. Reusing the same object preserves the cache.")
    @WikiReturn("this node")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 50,
            summary = "Replaces the item using a native object. Reusing the same object preserves the cache.")
    public ItemNode setItem(
            @WikiParameter(description = "stack, item or ID string.") Object item) {
        if (item instanceof String text) return setItem(text);
        Objects.requireNonNull(item, "item");
        if (nativeSource == item) return this;
        id = null;
        nativeSource = item;
        sourceChanged();
        return this;
    }

    /**
     * Resolves the item for the renderer or a platform helper.
     *
     * @param platform platform adapter, or null.
     * @return the native item, or null if unavailable or invalid.
     */
    @WikiMethod(section = "Resolution", sectionOrder = 30, order = 10,
            summary = "Resolves the item for the renderer or a platform helper.")
    @WikiReturn("the native item, or null if unavailable or invalid")
    @AiDoc(section = "Resolution", sectionOrder = 30, order = 10,
            summary = "Resolves the item for the renderer or a platform helper.")
    public Object nativeItem(
            @WikiParameter(description = "platform adapter, or null.") ScenePlatform platform) {
        return item.get(platform, this::resolve, this, false);
    }

    /**
     * Resolves the item and propagates resolution errors.
     *
     * @param platform platform adapter.
     * @return the native item, or null if temporarily unavailable.
     * @throws IllegalArgumentException if the item is unknown.
     * @throws UnsupportedOperationException if the platform cannot render scene items.
     */
    @WikiMethod(section = "Resolution", sectionOrder = 30, order = 20,
            summary = "Resolves the item and propagates resolution errors.")
    @WikiReturn("the native item, or null if temporarily unavailable")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the item is unknown.")
    @WikiThrows(type = UnsupportedOperationException.class, description = "if the platform cannot render scene items.")
    @AiDoc(section = "Resolution", sectionOrder = 30, order = 20,
            summary = "Resolves the item and propagates resolution errors.")
    public Object requireNativeItem(
            @WikiParameter(description = "platform adapter.") ScenePlatform platform) {
        return item.get(Objects.requireNonNull(platform, "platform"), this::resolve, this, true);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Combines item model bounds and custom mesh bounds. Until the item model is available,
     * a unit cube centered at the origin supplies fallback bounds. An empty stack contributes no bounds.</p>
     */
    @Override
    protected Aabb contentBounds() {
        Aabb mesh = super.contentBounds();
        ScenePlatform platform = ScenePlatform.current();
        Object resolved = nativeItem(platform);
        if (resolved == null) return union(FALLBACK_BOUNDS, mesh);
        Aabb model = platform.itemBounds(resolved, display);
        return model == null ? mesh : union(model, mesh);
    }

    /**
     * Formats the source for logging and debugging.
     *
     * @return a description of the item ID or native object.
     */
    @Override
    public String toString() {
        return "ItemNode[" + (id != null ? id : nativeSource) + "]";
    }

    /**
     * Combines model and mesh bounds.
     *
     * @param model model bounds.
     * @param mesh mesh bounds, or null.
     * @return the union of the bounds.
     */
    private static Aabb union(Aabb model, Aabb mesh) {
        if (mesh == null) return model;
        return new Aabb(Math.min(model.minX(), mesh.minX()), Math.min(model.minY(), mesh.minY()),
                Math.min(model.minZ(), mesh.minZ()), Math.max(model.maxX(), mesh.maxX()),
                Math.max(model.maxY(), mesh.maxY()), Math.max(model.maxZ(), mesh.maxZ()));
    }

    /**
     * Resolves the current source with the given platform.
     *
     * @param platform platform adapter.
     * @return the native item, or null.
     */
    private Object resolve(ScenePlatform platform) {
        return id != null ? platform.resolveItem(id) : platform.nativeItem(nativeSource);
    }

    /**
     * Clears the resolution cache after a source change.
     */
    private void sourceChanged() {
        item.reset();
        invalidateContent();
    }

    /**
     * Validates the ID.
     *
     * @param id item ID.
     * @return the ID with surrounding whitespace removed.
     */
    private static String requireId(String id) {
        String trimmed = Objects.requireNonNull(id, "id").trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("Item id is empty");
        return trimmed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemNode setPosition(float x, float y, float z) {
        super.setPosition(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemNode setRotation(dev.sixik.unigui.api.math.QuatView value) {
        super.setRotation(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemNode setScale(float x, float y, float z) {
        super.setScale(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemNode setVisible(boolean visible) {
        super.setVisible(visible);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemNode setOrigin(float x, float y, float z) {
        super.setOrigin(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemNode centerOrigin() {
        super.centerOrigin();
        return this;
    }
}

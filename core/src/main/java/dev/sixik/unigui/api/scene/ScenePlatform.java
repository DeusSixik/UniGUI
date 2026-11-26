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

/**
 * Connects {@link BlockNode}, {@link ItemNode}, {@link EntityNode} and {@link StructureNode} to the current Minecraft version.
 *
 * <p>Core treats native objects as opaque. Adapters supply {@code BlockState} on modern Minecraft,
 * {@code IBlockState} on 1.12.2, and a block plus metadata on 1.7.10.</p>
 * <p>Methods run on the client thread. Resolution may return null for temporarily unavailable content,
 * which allows retrying. Nodes cache {@link IllegalArgumentException} for invalid input and
 * {@link UnsupportedOperationException} for unsupported content.</p>
 */
@WikiPage(
        title = "ScenePlatform", slug = "scene-platform",
        category = "API/3D Scene", order = 180,
        summary = "Adapter between portable scene nodes and native Minecraft content.")
@WikiSection(title = "Version support", order = 10, markdown = """
        Modern adapters use `BlockState`, Cleanroom 1.12.2 uses `IBlockState`, and GTNH 1.7.10 uses a block plus
        metadata. 1.7.10 supports metadata strings such as `minecraft:wool@14` and structures built from individual
        cells. 1.12.2 and modern adapters can load structure templates, with the exact resource path and supported
        data determined by the version.
        """)
@WikiSection(title = "Resolution and validation", order = 20, markdown = """
        Resolution happens on the client thread. Use the node's `requireNative…` accessor to surface validation
        errors. The ordinary native accessors tolerate unsupported or unavailable content. For shared inputs, prefer
        string IDs and platform-appropriate state properties.
        """)
@WikiExample(title = "Load a supported structure resource", code = """
        ScenePlatform platform = ScenePlatform.current();
        if (platform != null) {
            StructureNode igloo = platform.loadStructure("minecraft:igloo/top");
            viewport.scene().addRoot(igloo);
        }
        """)
@WikiLink(label = "BlockSpec", target = "page:block-spec")
@WikiLink(label = "BlockNode", target = "page:block-node")
@WikiLink(label = "ItemNode", target = "page:item-node")
@WikiLink(label = "EntityNode", target = "page:entity-node")
@WikiLink(label = "StructureNode", target = "page:structure-node")
@WikiLink(label = "Scene3DRenderer", target = "page:scene-3d-renderer")
@AiDoc(
        summary = "Adapter between portable scene nodes and native Minecraft content.",
        purpose = "Resolve Minecraft state, items, entities and structure templates without exposing game classes in core.",
        criticalRules = {
                "Game adapters install themselves during client startup. Scene code normally uses the installed adapter.",
                "Return null when resolution can succeed later. Use IllegalArgumentException for invalid input and UnsupportedOperationException for unsupported features.",
                "A StructureNode captures its platform on the first write and stores native states."
        },
        related = {"BlockSpec", "BlockNode", "ItemNode", "EntityNode", "StructureNode", "Scene3DRenderer"},
        examples = @AiDoc.Example(title = "Load a supported structure resource", code = """
                ScenePlatform platform = ScenePlatform.current();
                if (platform != null) {
                    StructureNode igloo = platform.loadStructure("minecraft:igloo/top");
                    viewport.scene().addRoot(igloo);
                }
                """))
public interface ScenePlatform {
    /**
     * Resolves a block specification.
     *
     * @param spec parsed block specification.
     * @return the native block state, or null when temporarily unavailable.
     * @throws IllegalArgumentException if a block, property or value is unknown.
     * @throws UnsupportedOperationException if this form is unsupported.
     */
    @WikiMethod(section = "Blocks", sectionOrder = 20, order = 10,
            summary = "Resolves a block specification.")
    @WikiReturn("the native block state, or null when temporarily unavailable")
    @WikiThrows(type = IllegalArgumentException.class, description = "if a block, property or value is unknown.")
    @WikiThrows(type = UnsupportedOperationException.class, description = "if this form is unsupported.")
    @AiDoc(section = "Blocks", sectionOrder = 20, order = 10,
            summary = "Resolves a block specification.")
    Object resolveBlock(
            @WikiParameter(description = "parsed block specification.") BlockSpec spec);

    /**
     * Checks and converts native input to a block state.
     *
     * @param block native state or block.
     * @param meta legacy metadata, or -1 when omitted.
     * @return the native state, or null when temporarily unavailable.
     * @throws IllegalArgumentException if the object type is invalid for this version.
     */
    @WikiMethod(section = "Blocks", sectionOrder = 20, order = 20,
            summary = "Checks and converts native input to a block state.")
    @WikiReturn("the native state, or null when temporarily unavailable")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the object type is invalid for this version.")
    @AiDoc(section = "Blocks", sectionOrder = 20, order = 20,
            summary = "Checks and converts native input to a block state.")
    Object nativeBlock(
            @WikiParameter(description = "native state or block.") Object block,
            @WikiParameter(description = "legacy metadata, or -1 when omitted.") int meta);

    /**
     * Checks whether the block state represents air.
     *
     * @param state state from {@link #resolveBlock} or {@link #nativeBlock}.
     * @return true for air.
     */
    @WikiMethod(section = "Blocks", sectionOrder = 20, order = 30,
            summary = "Checks whether the block state represents air.")
    @WikiReturn("true for air")
    @AiDoc(section = "Blocks", sectionOrder = 20, order = 30,
            summary = "Checks whether the block state represents air.")
    default boolean isAirBlock(
            @WikiParameter(description = "state from `resolveBlock` or `nativeBlock`.") Object state) {
        return false;
    }

    /**
     * Creates or updates a block entity outside the world.
     *
     * @param state new native block state.
     * @param previous old block entity, or null. Reuse it when compatible to preserve data and animation.
     * @param x local block X, zero for a {@link BlockNode}.
     * @param y local block Y, zero for a single block.
     * @param z local block Z, zero for a single block.
     * @return the block entity, or null if absent or unsupported.
     */
    @WikiMethod(section = "Blocks", sectionOrder = 20, order = 40,
            summary = "Creates or updates a block entity outside the world.")
    @WikiReturn("the block entity, or null if absent or unsupported")
    @AiDoc(section = "Blocks", sectionOrder = 20, order = 40,
            summary = "Creates or updates a block entity outside the world.")
    default Object blockEntity(
            @WikiParameter(description = "new native block state.") Object state,
            @WikiParameter(description =
                    "old block entity, or null. Reuse it when compatible to preserve data and animation.")
            Object previous,
            @WikiParameter(description = "local block X, zero for a `BlockNode`.") int x,
            @WikiParameter(description = "local block Y, zero for a single block.") int y,
            @WikiParameter(description = "local block Z, zero for a single block.") int z) {
        return null;
    }

    /**
     * Resolves an item ID.
     *
     * @param id item ID such as {@code minecraft:diamond_sword}.
     * @return the native item, or null when temporarily unavailable.
     * @throws IllegalArgumentException if the item is unknown.
     * @throws UnsupportedOperationException if item rendering is unsupported.
     */
    @WikiMethod(section = "Items", sectionOrder = 30, order = 10,
            summary = "Resolves an item ID.")
    @WikiReturn("the native item, or null when temporarily unavailable")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the item is unknown.")
    @WikiThrows(type = UnsupportedOperationException.class, description = "if item rendering is unsupported.")
    @AiDoc(section = "Items", sectionOrder = 30, order = 10,
            summary = "Resolves an item ID.")
    default Object resolveItem(
            @WikiParameter(description = "item ID such as `minecraft:diamond_sword`.") String id) {
        throw unsupported("Items");
    }

    /**
     * Validates and copies a native item.
     *
     * @param item a native stack or item.
     * @return the resolved native item, or null when temporarily unavailable.
     * @throws IllegalArgumentException if the input type is invalid.
     * @throws UnsupportedOperationException if item rendering is unsupported.
     */
    @WikiMethod(section = "Items", sectionOrder = 30, order = 20,
            summary = "Validates and copies a native item.")
    @WikiReturn("the resolved native item, or null when temporarily unavailable")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the input type is invalid.")
    @WikiThrows(type = UnsupportedOperationException.class, description = "if item rendering is unsupported.")
    @AiDoc(section = "Items", sectionOrder = 30, order = 20,
            summary = "Validates and copies a native item.")
    default Object nativeItem(
            @WikiParameter(description = "a native stack or item.") Object item) {
        throw unsupported("Items");
    }

    /**
     * Reads an item's model bounds.
     *
     * @param item value from {@link #resolveItem} or {@link #nativeItem}.
     * @return local bounds, or null for an empty item.
     */
    @WikiMethod(section = "Items", sectionOrder = 30, order = 30,
            summary = "Reads an item's model bounds.")
    @WikiReturn("local bounds, or null for an empty item")
    @AiDoc(section = "Items", sectionOrder = 30, order = 30,
            summary = "Reads an item's model bounds.")
    default Aabb itemBounds(
            @WikiParameter(description = "value from `resolveItem` or `nativeItem`.") Object item) {
        return null;
    }

    /**
     * Reads model bounds for the requested placement.
     *
     * @param item value from {@link #resolveItem} or {@link #nativeItem}.
     * @param display model placement.
     * @return local bounds, or null for an empty item. Defaults to {@link #itemBounds(Object)}.
     */
    @WikiMethod(section = "Items", sectionOrder = 30, order = 30,
            summary = "Reads model bounds for the requested placement.")
    @WikiReturn("local bounds, or null for an empty item. Defaults to `itemBounds(Object)`")
    @AiDoc(section = "Items", sectionOrder = 30, order = 30,
            summary = "Reads model bounds for the requested placement.")
    default Aabb itemBounds(
            @WikiParameter(description = "value from `resolveItem` or `nativeItem`.") Object item,
            @WikiParameter(description = "model placement.") ItemNode.Display display) {
        return itemBounds(item);
    }

    /**
     * Resolves an entity by type ID.
     *
     * @param id entity type ID such as {@code minecraft:zombie}.
     * @return the native entity, or null if temporarily unavailable.
     * @throws IllegalArgumentException if the type is unknown.
     * @throws UnsupportedOperationException if entity rendering is unsupported.
     */
    @WikiMethod(section = "Entities", sectionOrder = 40, order = 10,
            summary = "Resolves an entity by type ID.")
    @WikiReturn("the native entity, or null if temporarily unavailable")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the type is unknown.")
    @WikiThrows(type = UnsupportedOperationException.class, description = "if entity rendering is unsupported.")
    @AiDoc(section = "Entities", sectionOrder = 40, order = 10,
            summary = "Resolves an entity by type ID.")
    default Object resolveEntity(
            @WikiParameter(description = "entity type ID such as `minecraft:zombie`.") String id) {
        throw unsupported("Entities");
    }

    /**
     * Validates a native entity source.
     *
     * @param entity native entity, entity type or another adapter-supported object.
     * @return the native entity, or null if temporarily unavailable.
     * @throws IllegalArgumentException if the input type is invalid.
     * @throws UnsupportedOperationException if entity rendering is unsupported.
     */
    @WikiMethod(section = "Entities", sectionOrder = 40, order = 20,
            summary = "Validates a native entity source.")
    @WikiReturn("the native entity, or null if temporarily unavailable")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the input type is invalid.")
    @WikiThrows(type = UnsupportedOperationException.class, description = "if entity rendering is unsupported.")
    @AiDoc(section = "Entities", sectionOrder = 40, order = 20,
            summary = "Validates a native entity source.")
    default Object nativeEntity(
            @WikiParameter(description = "native entity, entity type or another adapter-supported object.") Object entity) {
        throw unsupported("Entities");
    }

    /**
     * Reads the entity's bounds.
     *
     * @param entity value from {@link #resolveEntity} or {@link #nativeEntity}.
     * @return local bounds, or null.
     */
    @WikiMethod(section = "Entities", sectionOrder = 40, order = 30,
            summary = "Reads the entity's bounds.")
    @WikiReturn("local bounds, or null")
    @AiDoc(section = "Entities", sectionOrder = 40, order = 30,
            summary = "Reads the entity's bounds.")
    default Aabb entityBounds(
            @WikiParameter(description = "value from `resolveEntity` or `nativeEntity`.") Object entity) {
        return null;
    }

    /**
     * Loads a structure by resource ID such as {@code minecraft:igloo/top}. The platform chooses the format
     * and location, such as a Minecraft structure template in a compressed NBT file.
     *
     * @param id resource ID without the structure directory and file extension.
     * @return a new structure bound to this platform.
     * @throws java.io.IOException if the file is missing or invalid.
     * @throws IllegalArgumentException if the ID is invalid.
     * @throws UnsupportedOperationException if structure resources are unsupported.
     */
    @WikiMethod(section = "Structures", sectionOrder = 50, order = 10,
            summary = "Loads a structure by resource ID such as `minecraft:igloo/top`. The platform chooses the format and location, such as a Minecraft structure template in a compressed NBT file.")
    @WikiReturn("a new structure bound to this platform")
    @WikiThrows(type = java.io.IOException.class, description = "if the file is missing or invalid.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the ID is invalid.")
    @WikiThrows(type = UnsupportedOperationException.class, description = "if structure resources are unsupported.")
    @AiDoc(section = "Structures", sectionOrder = 50, order = 10,
            summary = "Loads a structure by resource ID such as `minecraft:igloo/top`. The platform chooses the format and location, such as a Minecraft structure template in a compressed NBT file.")
    default StructureNode loadStructure(
            @WikiParameter(description = "resource ID without the structure directory and file extension.") String id) throws java.io.IOException {
        throw unsupported("Structure files");
    }

    /**
     * Installs the adapter during client startup.
     *
     * @param platform adapter for the current client, or null to clear it.
     */
    @WikiMethod(section = "Lifecycle", sectionOrder = 10, order = 10,
            summary = "Installs the adapter during client startup.")
    @AiDoc(section = "Lifecycle", sectionOrder = 10, order = 10,
            summary = "Installs the adapter during client startup.")
    static void install(
            @WikiParameter(description = "adapter for the current client, or null to clear it.") ScenePlatform platform) {
        ScenePlatformHolder.current = platform;
    }

    /**
     * Reads the installed adapter used by scene nodes for bounds and picking.
     *
     * @return the adapter, or null before installation.
     */
    @WikiMethod(section = "Lifecycle", sectionOrder = 10, order = 20,
            summary = "Reads the installed adapter used by scene nodes for bounds and picking.")
    @WikiReturn("the adapter, or null before installation")
    @AiDoc(section = "Lifecycle", sectionOrder = 10, order = 20,
            summary = "Reads the installed adapter used by scene nodes for bounds and picking.")
    static ScenePlatform current() {
        return ScenePlatformHolder.current;
    }

    /**
     * Creates the exception for an unsupported content category.
     *
     * @param kind content category.
     * @return the exception to throw.
     */
    private UnsupportedOperationException unsupported(String kind) {
        return new UnsupportedOperationException(kind + " are not supported in 3D scenes by "
                + getClass().getSimpleName() + " yet");
    }
}

/**
 * Stores the installed scene platform.
 */
final class ScenePlatformHolder {
    /**
     * Adapter for the current client, or null.
     */
    static volatile ScenePlatform current;

    private ScenePlatformHolder() {
    }
}

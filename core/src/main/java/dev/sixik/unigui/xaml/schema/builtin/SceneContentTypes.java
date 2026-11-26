package dev.sixik.unigui.xaml.schema.builtin;

import dev.sixik.unigui.api.backend.UniGuiLogger;
import dev.sixik.unigui.api.backend.UniGuiRuntime;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.ScenePlatform;
import dev.sixik.unigui.api.scene.StructureNode;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.Meta;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.TypeInfo;
import dev.sixik.unigui.xaml.schema.ValueKind;

import java.io.IOException;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Nonvisual Minecraft content elements inside {@code <Scene3D>}. The installed {@link ScenePlatform}
 * resolves {@code <Block>}, {@code <Item>}, {@code <Entity>} and {@code <Structure>} sources.
 *
 * <p>Unknown IDs are reported while loading. Unsupported content is skipped at render time.
 * With no platform installed, nodes are created without validation and structures stay empty.</p>
 * <pre>{@code
 * <Scene3D>
 * <Block state="minecraft:furnace[facing=south]"/>
 * <Item item="minecraft:diamond_sword" pos="1.5,0.5,0.5"/>
 * <Structure src="minecraft:igloo/top" pos="-4,0,-4" assemblySeconds="2"/>
 * </Scene3D>
 * }</pre>
 */
final class SceneContentTypes {
    /**
     * Logger for skipped content.
     */
    private static final UniGuiLogger LOGGER = UniGuiRuntime.logger(SceneContentTypes.class);

    private SceneContentTypes() {
    }

    /**
     * Registers the content elements accepted by Scene3D.
     *
     * @param schema type registry.
     */
    static void register(Schema schema) {
        schema.define("Block", () -> declaration("Block", BlockElement.class, BlockElement::new)
                .property("state", Converters.STRING, (element, value) -> element.state = value,
                        element -> element.state, Meta.category("Minecraft").defaultValue("minecraft:stone")
                                .description("Block, for example minecraft:furnace[facing=north] or, on 1.7.10 "
                                        + "and 1.12.2, minecraft:wool@14."))
                .build());
        schema.define("Item", () -> declaration("Item", ItemElement.class, ItemElement::new)
                .property("item", Converters.STRING, (element, value) -> element.item = value,
                        element -> element.item, Meta.category("Minecraft").kind(ValueKind.RESOURCE_ID)
                                .defaultValue("minecraft:apple").description("Minecraft item ID."))
                .build());
        schema.define("Entity", () -> declaration("Entity", EntityElement.class, EntityElement::new)
                .property("type", Converters.STRING, (element, value) -> element.type = value,
                        element -> element.type, Meta.category("Minecraft").kind(ValueKind.RESOURCE_ID)
                                .defaultValue("minecraft:zombie").description("Minecraft entity type."))
                .build());
        schema.define("Structure", () -> declaration("Structure", StructureElement.class, StructureElement::new)
                .property("src", Converters.STRING, (element, value) -> element.source = value,
                        element -> element.source, Meta.category("Minecraft").kind(ValueKind.RESOURCE_ID)
                                .description("Structure resource ID, for example mymod:altar."))
                .property("topVisibleY", Converters.INT, (element, value) -> element.topVisibleY = value,
                        element -> element.topVisibleY, Meta.category("Transform").defaultValue("2147483647")
                                .description("Highest visible local block layer, inclusive."))
                .property("assemblySeconds", Converters.FLOAT, (element, value) -> element.assemblySeconds = value,
                        element -> element.assemblySeconds, Meta.category("Animation").defaultValue("0")
                                .description("Duration of the layer-by-layer assembly. Zero shows the structure immediately."))
                .build());
    }

    /**
     * Defines the shared node declaration properties.
     *
     * @param name element name.
     * @param type element implementation class.
     * @param factory factory for deserialization.
     * @param <T> element type.
     * @return schema builder.
     */
    private static <T extends NodeElement> TypeInfo.Builder<T> declaration(String name, Class<T> type,
                                                                           Supplier<T> factory) {
        return TypeInfo.of(Schema.name(name), type, factory)
                .describe(name, "Minecraft", "Adds a " + name.toLowerCase(Locale.ROOT) + " to a 3D scene.")
                .property("pos", Converters.STRING, NodeElement::position, NodeElement::position,
                        Meta.category("Transform").defaultValue("0,0,0")
                                .description("X, Y, Z position in scene units."))
                .afterLoad((element, context) -> element.buildNode());
    }

    /**
     * Validates content on the installed platform. Unsupported content is allowed.
     *
     * @param check validation that throws for an invalid ID.
     * @param what content description for logging.
     */
    private static void validate(Runnable check, String what) {
        if (ScenePlatform.current() == null) return;
        try {
            check.run();
        } catch (UnsupportedOperationException unsupported) {
            LOGGER.warn("{} is not drawn: {}", what, unsupported.getMessage());
        }
    }

    /**
     * Retains the declaration for XAML export and builds a node from the loaded properties.
     */
    private abstract static class NodeElement extends WidgetBase implements Scene3D.NodeContent {
        /**
         * Markup position in X,Y,Z format, in scene units.
         */
        private String position = "0,0,0";
        /**
         * Parsed X, Y and Z in scene units.
         */
        private float x, y, z;
        /**
         * Node built from the loaded attributes.
         */
        private SceneNode node;

        /**
         * Parses the position attribute.
         *
         * @param position comma-separated X,Y,Z in scene units.
         * @throws IllegalArgumentException if position does not contain three comma-separated numbers.
         */
        final void position(String position) {
            String[] components = position.split(",", -1);
            if (components.length != 3) throw new IllegalArgumentException("Expected pos as X,Y,Z: " + position);
            try {
                x = Float.parseFloat(components[0].trim());
                y = Float.parseFloat(components[1].trim());
                z = Float.parseFloat(components[2].trim());
            } catch (NumberFormatException invalid) {
                throw new IllegalArgumentException("Expected pos as X,Y,Z: " + position, invalid);
            }
            this.position = position;
        }

        /**
         * Reads the position for export.
         *
         * @return X,Y,Z attribute text.
         */
        final String position() {
            return position;
        }

        /**
         * Builds the node and applies the configured position.
         *
         * @throws IllegalArgumentException if the source or position is invalid.
         */
        final void buildNode() {
            node = createNode().setPosition(x, y, z);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final SceneNode sceneNode() {
            return node;
        }

        /**
         * Builds a node from the declaration properties.
         *
         * @return a new node at the origin.
         */
        abstract SceneNode createNode();
    }

    /**
     * Block declaration from a state specification.
     */
    private static final class BlockElement extends NodeElement {
        /**
         * Block specification.
         */
        private String state = "minecraft:stone";

        /**
         * {@inheritDoc}
         */
        @Override
        SceneNode createNode() {
            BlockNode node = new BlockNode(state);
            // Validate the block specification during markup loading.
            validate(() -> node.requireNativeState(ScenePlatform.current()), "Block " + state);
            return node;
        }
    }

    /**
     * Item declaration.
     */
    private static final class ItemElement extends NodeElement {
        /**
         * Minecraft item ID.
         */
        private String item = "minecraft:apple";

        /**
         * {@inheritDoc}
         */
        @Override
        SceneNode createNode() {
            ItemNode node = new ItemNode(item);
            validate(() -> node.requireNativeItem(ScenePlatform.current()), "Item " + item);
            return node;
        }
    }

    /**
     * Entity preview declaration.
     */
    private static final class EntityElement extends NodeElement {
        /**
         * Minecraft entity type ID.
         */
        private String type = "minecraft:zombie";

        /**
         * {@inheritDoc}
         */
        @Override
        SceneNode createNode() {
            EntityNode node = new EntityNode(type);
            // Validate the type immediately. Instantiating the preview can wait for a world.
            validate(() -> node.requireNativeEntity(ScenePlatform.current()), "Entity " + type);
            return node;
        }
    }

    /**
     * Structure resource declaration.
     */
    private static final class StructureElement extends NodeElement {
        /**
         * Resource ID or empty text for an empty structure.
         */
        private String source = "";
        /**
         * Highest visible local layer, inclusive.
         */
        private int topVisibleY = Integer.MAX_VALUE;
        /**
         * Assembly duration in seconds. Zero shows the configured layers immediately.
         */
        private float assemblySeconds;

        /**
         * {@inheritDoc}
         */
        @Override
        SceneNode createNode() {
            return loadStructure().setTopVisibleY(topVisibleY);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAddedToScene(Scene3D viewport) {
            if (assemblySeconds != 0) {
                ((StructureNode) sceneNode()).playAssembly(viewport, assemblySeconds, topVisibleY);
            }
        }

        /**
         * Loads a structure through the installed platform.
         *
         * @return loaded structure, or an empty structure when unavailable on this platform.
         * @throws IllegalArgumentException if the resource is missing or invalid.
         */
        private StructureNode loadStructure() {
            ScenePlatform platform = ScenePlatform.current();
            if (source.isBlank() || platform == null) return new StructureNode();
            try {
                return platform.loadStructure(source);
            } catch (UnsupportedOperationException unsupported) {
                LOGGER.warn("Structure {} is not drawn: {}", source, unsupported.getMessage());
                return new StructureNode(platform);
            } catch (IOException failure) {
                throw new IllegalArgumentException("Cannot read structure " + source + ": " + failure.getMessage(),
                        failure);
            }
        }
    }
}

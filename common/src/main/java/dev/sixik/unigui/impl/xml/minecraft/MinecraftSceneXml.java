package dev.sixik.unigui.impl.xml.minecraft;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.backend.minecraft_impl.BlockNode;
import dev.sixik.unigui.backend.minecraft_impl.EntityNode;
import dev.sixik.unigui.backend.minecraft_impl.StructureNode;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.Meta;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.TypeInfo;
import dev.sixik.unigui.xaml.schema.ValueKind;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Регистрирует невизуальные элементы Minecraft внутри XAML-сцены.
 */
final class MinecraftSceneXml {
    /**
     * Закрывает создание экземпляров регистратора.
     */
    private MinecraftSceneXml() {
    }

    /**
     * Добавляет теги, доступные внутри Scene3D.
     *
     * @param schema реестр текущей версии Minecraft.
     */
    static void register(Schema schema) {
        schema.define("Block", () -> declaration("Block", BlockElement.class, BlockElement::new)
                .property("state", Converters.STRING, (element, value) -> element.state = value,
                        element -> element.state, Meta.category("Minecraft").defaultValue("minecraft:stone")
                                .description("Block state, for example minecraft:furnace[facing=north]."))
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
        schema.define("Entity", () -> declaration("Entity", EntityElement.class, EntityElement::new)
                .property("type", Converters.STRING, (element, value) -> element.type = value,
                        element -> element.type, Meta.category("Minecraft").kind(ValueKind.RESOURCE_ID)
                                .defaultValue("minecraft:zombie").description("Minecraft entity type."))
                .build());
    }

    /**
     * Описывает общие свойства декларации узла.
     *
     * @param name    имя XML-элемента.
     * @param type    класс элемента.
     * @param factory фабрика элемента для загрузчика.
     * @param <T>     тип элемента.
     * @return билдер схемы.
     */
    private static <T extends NodeElement> TypeInfo.Builder<T> declaration(
            String name, Class<T> type, Supplier<T> factory) {
        return TypeInfo.of(Schema.name(name), type, factory)
                .describe(name, "Minecraft", "Adds a " + name.toLowerCase(java.util.Locale.ROOT) + " to a 3D scene.")
                .property("pos", Converters.STRING, NodeElement::position, NodeElement::position,
                        Meta.category("Transform").defaultValue("0,0,0")
                                .description("X, Y, Z position in scene units."))
                .afterLoad((element, context) -> element.buildNode());
    }

    /**
     * Сохраняет декларацию для снимка XAML и строит её узел после загрузки свойств.
     */
    private abstract static class NodeElement extends WidgetBase implements Scene3D.NodeContent {
        /**
         * Позиция из разметки в формате X,Y,Z, в единицах сцены.
         */
        private String position = "0,0,0";
        /**
         * Узел, построенный после применения атрибутов.
         */
        private SceneNode node;

        /**
         * Принимает строковое значение позиции.
         *
         * @param position координаты X,Y,Z в единицах сцены.
         */
        final void position(String position) {
            this.position = position;
        }

        /**
         * Читает позицию для экспорта.
         *
         * @return строка координат X,Y,Z.
         */
        final String position() {
            return position;
        }

        /**
         * Строит узел и применяет конечную позицию из XAML.
         */
        final void buildNode() {
            String[] components = position.split(",", -1);
            if (components.length != 3) throw new IllegalArgumentException("Expected pos as X,Y,Z: " + position);
            float x = Float.parseFloat(components[0]);
            float y = Float.parseFloat(components[1]);
            float z = Float.parseFloat(components[2]);
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
         * Загружает данные узла из его свойств.
         *
         * @return готовый узел в начале координат.
         */
        abstract SceneNode createNode();
    }

    /**
     * Декларация ванильного блока с опциональными свойствами состояния.
     */
    private static final class BlockElement extends NodeElement {
        /**
         * Строка состояния в синтаксисе ванильной команды.
         */
        private String state = "minecraft:stone";

        /**
         * {@inheritDoc}
         */
        @Override
        SceneNode createNode() {
            try {
                return new BlockNode(SceneXmlCompat.state(state));
            } catch (CommandSyntaxException failure) {
                throw new IllegalArgumentException("Invalid block state: " + state, failure);
            }
        }
    }

    /**
     * Декларация NBT-структуры из ресурсов мода.
     */
    private static final class StructureElement extends NodeElement {
        /**
         * Логический идентификатор структуры или пустая строка для пустого узла.
         */
        private String source = "";
        /**
         * Последний видимый слой в локальных координатах, включительно.
         */
        private int topVisibleY = Integer.MAX_VALUE;
        /**
         * Длительность сборки в секундах. Ноль показывает все разрешённые слои сразу.
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
         * Разрешает NBT-ресурс из текущих пакетов игры или ресурсов модов.
         *
         * @return загруженная структура.
         */
        private StructureNode loadStructure() {
            if (source.isBlank()) return new StructureNode();
            var id = SceneXmlCompat.id(source);
            Minecraft client = Minecraft.getInstance();
            try {
                for (String directory : new String[]{"structure/", "structures/"}) {
                    String path = directory + id.getPath() + ".nbt";
                    var file = SceneXmlCompat.file(id, path);
                    if (client != null) {
                        var resource = client.getResourceManager().getResource(file);
                        if (resource.isPresent()) return StructureNode.fromNbt(resource.get().open());
                        var server = client.getSingleplayerServer();
                        if (server != null) {
                            var data = server.getResourceManager().getResource(file);
                            if (data.isPresent()) return StructureNode.fromNbt(data.get().open());
                        }
                    }
                    try (InputStream input = MinecraftSceneXml.class.getClassLoader()
                            .getResourceAsStream("data/" + id.getNamespace() + "/" + path)) {
                        if (input != null) return StructureNode.fromNbt(input);
                    }
                }
            } catch (IOException failure) {
                throw new IllegalArgumentException("Cannot read structure " + id, failure);
            }
            throw new IllegalArgumentException("Unknown structure: " + id);
        }
    }

    /**
     * Декларация ванильной сущности для превью.
     */
    private static final class EntityElement extends NodeElement {
        /**
         * Идентификатор типа сущности.
         */
        private String type = "minecraft:zombie";

        /**
         * {@inheritDoc}
         */
        @Override
        SceneNode createNode() {
            var entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(SceneXmlCompat.id(type))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown entity type: " + type));
            return EntityNode.preview(entityType);
        }
    }
}

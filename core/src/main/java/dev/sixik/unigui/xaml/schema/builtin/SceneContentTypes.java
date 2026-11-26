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
 * Невизуальные элементы Minecraft-контента внутри {@code <Scene3D>}: {@code <Block>}, {@code <Item>},
 * {@code <Entity>} и {@code <Structure>}. Работают на всех версиях: строки разрешает установленная
 * {@link ScenePlatform}.
 *
 * <p>Неизвестный идентификатор сообщается при загрузке разметки. Если версия не поддерживает вид контента,
 * разметка загружается, а узел ничего не рисует. Без установленной платформы (например в редакторе без игры)
 * узлы создаются без проверки, а структура остаётся пустой.</p>
 *
 * <pre>{@code
 * <Scene3D>
 *     <Block state="minecraft:furnace[facing=south]"/>
 *     <Item item="minecraft:diamond_sword" pos="1.5,0.5,0.5"/>
 *     <Structure src="minecraft:igloo/top" pos="-4,0,-4" assemblySeconds="2"/>
 * </Scene3D>
 * }</pre>
 */
final class SceneContentTypes {
    /**
     * Лог пропущенного неподдерживаемого контента.
     */
    private static final UniGuiLogger LOGGER = UniGuiRuntime.logger(SceneContentTypes.class);

    private SceneContentTypes() {
    }

    /**
     * Добавляет элементы, доступные внутри Scene3D.
     *
     * @param schema реестр типов.
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
     * Описывает общие свойства декларации узла.
     *
     * @param name    имя XML-элемента.
     * @param type    класс элемента.
     * @param factory фабрика элемента для загрузчика.
     * @param <T>     тип элемента.
     * @return билдер схемы.
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
     * Проверяет контент на установленной платформе. Неподдерживаемый вид контента не мешает загрузке.
     *
     * @param check проверка, бросающая исключение для неверного идентификатора.
     * @param what  описание для лога.
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
     * Сохраняет декларацию для снимка XAML и строит её узел после загрузки свойств.
     */
    private abstract static class NodeElement extends WidgetBase implements Scene3D.NodeContent {
        /**
         * Позиция из разметки в формате X,Y,Z, в единицах сцены.
         */
        private String position = "0,0,0";
        /**
         * Разобранная позиция X, Y, Z.
         */
        private float x, y, z;
        /**
         * Узел, построенный после применения атрибутов.
         */
        private SceneNode node;

        /**
         * Принимает строковое значение позиции.
         *
         * @param position координаты X,Y,Z в единицах сцены.
         * @throws IllegalArgumentException если строка не содержит три числа через запятую.
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
         * Читает позицию для экспорта.
         *
         * @return строка координат X,Y,Z.
         */
        final String position() {
            return position;
        }

        /**
         * Строит узел и применяет конечную позицию из XAML.
         *
         * @throws IllegalArgumentException если контент не найден.
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
         * Загружает данные узла из его свойств.
         *
         * @return готовый узел в начале координат.
         */
        abstract SceneNode createNode();
    }

    /**
     * Декларация блока по строке состояния.
     */
    private static final class BlockElement extends NodeElement {
        /**
         * Строка блока.
         */
        private String state = "minecraft:stone";

        /**
         * {@inheritDoc}
         */
        @Override
        SceneNode createNode() {
            BlockNode node = new BlockNode(state);
            // Неверная строка видна сразу при загрузке разметки, а не при первой отрисовке.
            validate(() -> node.requireNativeState(ScenePlatform.current()), "Block " + state);
            return node;
        }
    }

    /**
     * Декларация предмета.
     */
    private static final class ItemElement extends NodeElement {
        /**
         * Идентификатор предмета.
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
     * Декларация сущности для превью.
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
            EntityNode node = new EntityNode(type);
            // Неизвестный тип виден сразу. Сама сущность создаётся, когда появится мир.
            validate(() -> node.requireNativeEntity(ScenePlatform.current()), "Entity " + type);
            return node;
        }
    }

    /**
     * Декларация структуры из файла ресурсов.
     */
    private static final class StructureElement extends NodeElement {
        /**
         * Идентификатор структуры или пустая строка для пустого узла.
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
         * Загружает структуру через платформу версии.
         *
         * @return загруженная структура или пустая, если файлов структур нет на этой версии.
         * @throws IllegalArgumentException если структура не найдена или повреждена.
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

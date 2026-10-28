package dev.sixik.unigui.testmod.client.ui.unity;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.dnd.ReorderController;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.TextInput;

import java.util.List;
import java.util.function.Consumer;

/**
 * Точная визуальная и интерактивная реплика окна Unity Inspector.
 */
public class UnityInspectorPanel extends LinearBox {
    private final OverlayLayer overlayLayer;

    // Ссылки на секции для внешнего взаимодействия
    private UnityComponentSection transformSection;
    private UnityComponentSection cameraSection;
    private UnityComponentSection audioListenerSection;

    // Состояние инспектора
    private boolean locked = false;
    private final VBox scrollContent = new VBox();
    private TextInput objectNameInput;
    private CanvasWidget objectIcon;
    private UnityEditorHierarchyView.GameObjectType targetType = UnityEditorHierarchyView.GameObjectType.CAMERA;

    // Слушатели изменений для связи со сценой
    private Consumer<Float> onFovChanged;
    private Consumer<float[]> onPositionChanged;
    private final float[] currentPosition = new float[]{0.0f, 0.0f, 0.0f};

    public UnityInspectorPanel(OverlayLayer overlayLayer) {
        super(Orientation.VERTICAL);
        this.overlayLayer = overlayLayer;
        spacing(0.0f);
        layout(style -> style.widthPercent(100.0f).heightPercent(100.0f)
                .flexGrow(1.0f).flexShrink(1.0f));

        // 1. Скроллируемая область инспектора
        scrollContent.spacing(0.0f);
        scrollContent.layout(style -> style.widthPercent(100.0f)
                .padding(0.0f, 0.0f, 0.0f, 16.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        buildGameObjectHeader();
        buildTransformComponent();
        buildCameraComponent();
        buildAudioListenerComponent();
        buildAddComponentButton();

        ReorderController.attach(scrollContent)
                .accepts("unity.component")
                .orientation(Orientation.VERTICAL)
                .indicator(UnityStyles.BORDER_INPUT_FOCUS, 2.0f, 6.0f)
                .itemFilter(child -> child instanceof UnityComponentSection sec && sec.isDraggable());

        ScrollView scrollView = new ScrollView(scrollContent);
        scrollView.scrollStep(18.0f);
        scrollView.overlayScrollBars(false);
        scrollView.scrollbarGap(2.0f);
        scrollView.scrollbarTrackColor().set(UnityStyles.BORDER_DEFAULT);
        scrollView.scrollbarThumbColor().set(UnityStyles.TEXT_MUTED);
        scrollView.verticalScrollBar().thumbHoverColor().set(UnityStyles.TEXT_PRIMARY);
        scrollView.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f)
                .overflowX(Overflow.HIDDEN)
                .overflowY(Overflow.AUTO)
                .flexGrow(1.0f).flexShrink(1.0f));

        addChild(scrollView);
    }

    /**
     * Шапка GameObject (Иконка синего куба, галочка, имя "Main Camera", Static, Tag, Layer).
     */
    private void buildGameObjectHeader() {
        VBox goHeader = new VBox();
        goHeader.spacing(4.0f);
        goHeader.layout(style -> style.widthPercent(100.0f)
                .padding(8.0f, 6.0f, 8.0f, 6.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        // Строка 1: [Синий куб] [x] [Main Camera            ] [ ] Static ▼
        HBox row1 = new HBox();
        row1.spacing(6.0f);
        row1.layout(style -> style.widthPercent(100.0f).height(UnityStyles.ROW_HEIGHT)
                .alignItems(Align.CENTER));

        CanvasWidget cube = UnityIcons.gameObjectIcon(16.0f, () -> targetType);
        this.objectIcon = cube;
        cube.layout(style -> style.size(16.0f, 16.0f).alignSelf(Align.CENTER).flexGrow(0.0f).flexShrink(0.0f));

        Checkbox activeCheck = new Checkbox();
        UnityStyles.styleCheckbox(activeCheck);
        activeCheck.checked(true);
        activeCheck.layout(style -> style.size(14.0f, UnityStyles.ROW_HEIGHT).alignSelf(Align.CENTER).flexGrow(0.0f).flexShrink(0.0f));

        TextInput nameInput = new TextInput("Main Camera");
        UnityStyles.styleTextInput(nameInput);
        nameInput.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));
        this.objectNameInput = nameInput;

        HBox staticBox = new HBox();
        staticBox.spacing(3.0f);
        staticBox.layout(style -> style.height(UnityStyles.ROW_HEIGHT).alignItems(Align.CENTER));

        Checkbox staticCheck = new Checkbox();
        UnityStyles.styleCheckbox(staticCheck);
        staticCheck.checked(false);
        staticCheck.layout(style -> style.size(14.0f, UnityStyles.ROW_HEIGHT).alignSelf(Align.CENTER).flexGrow(0.0f).flexShrink(0.0f));

        Label staticLbl = new Label("Static");
        staticLbl.color(UnityStyles.TEXT_PRIMARY);
        staticLbl.layout(style -> style.height(UnityStyles.ROW_HEIGHT));

        CanvasWidget dropArrow = UnityIcons.foldoutArrow(8.0f, true);
        dropArrow.layout(style -> style.size(8.0f, 8.0f).alignSelf(Align.CENTER).margin(2.0f, 0.0f, 0.0f, 0.0f));

        staticBox.addChild(staticCheck);
        staticBox.addChild(staticLbl);
        staticBox.addChild(dropArrow);

        row1.addChild(cube);
        row1.addChild(activeCheck);
        row1.addChild(nameInput);
        row1.addChild(staticBox);

        // Строка 2: Tag [MainCamera  v]   Layer [Player  v]
        HBox row2 = new HBox();
        row2.spacing(8.0f);
        row2.layout(style -> style.widthPercent(100.0f).height(UnityStyles.ROW_HEIGHT)
                .alignItems(Align.CENTER));

        // Tag
        HBox tagBox = new HBox();
        tagBox.spacing(4.0f);
        tagBox.layout(style -> style.heightPercent(100.0f).flexGrow(1.0f).alignItems(Align.CENTER));
        Label tagLbl = new Label("Tag");
        tagLbl.color(UnityStyles.TEXT_LABEL);
        tagLbl.layout(style -> style.width(28.0f).height(UnityStyles.ROW_HEIGHT));
        ComboBox tagCombo = new ComboBox();
        tagCombo.items(List.of("Untagged", "Respawn", "Finish", "EditorOnly", "MainCamera", "Player", "Add Tag..."));
        tagCombo.selectedIndex(4);
        UnityStyles.styleComboBox(tagCombo);
        tagCombo.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));
        tagBox.addChild(tagLbl);
        tagBox.addChild(tagCombo);

        // Layer
        HBox layerBox = new HBox();
        layerBox.spacing(4.0f);
        layerBox.layout(style -> style.heightPercent(100.0f).flexGrow(1.0f).alignItems(Align.CENTER));
        Label layerLbl = new Label("Layer");
        layerLbl.color(UnityStyles.TEXT_LABEL);
        layerLbl.layout(style -> style.width(34.0f).height(UnityStyles.ROW_HEIGHT));
        ComboBox layerCombo = new ComboBox();
        layerCombo.items(List.of("Default", "TransparentFX", "Ignore Raycast", "Water", "UI", "Player", "Edit Layers..."));
        layerCombo.selectedIndex(5);
        UnityStyles.styleComboBox(layerCombo);
        layerCombo.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));
        layerBox.addChild(layerLbl);
        layerBox.addChild(layerCombo);

        row2.addChild(tagBox);
        row2.addChild(layerBox);

        goHeader.addChild(row1);
        goHeader.addChild(row2);

        scrollContent.addChild(goHeader);
        scrollContent.addChild(UnityStyles.separatorLine());
    }

    /**
     * Компонент Transform (Position, Rotation, Scale).
     */
    private void buildTransformComponent() {
        transformSection = new UnityComponentSection("Transform", UnityIcons.transformAxes(14.0f), false, overlayLayer);
        transformSection.draggable(false);

        // Position: X [0], Y [0], Z [0]
        transformSection.addPropertyRow(UnityPropertyRows.vector3Row(
                "Position",
                0.0, 0.0, 0.0,
                x -> {
                    currentPosition[0] = (float) x;
                    notifyPositionChanged();
                },
                y -> {
                    currentPosition[1] = (float) y;
                    notifyPositionChanged();
                },
                z -> {
                    currentPosition[2] = (float) z;
                    notifyPositionChanged();
                }
        ));

        // Rotation: X [0], Y [0], Z [0]
        transformSection.addPropertyRow(UnityPropertyRows.vector3Row(
                "Rotation",
                0.0, 0.0, 0.0,
                null, null, null
        ));

        // Scale: X [1], Y [1], Z [1]
        transformSection.addPropertyRow(UnityPropertyRows.vector3Row(
                "Scale",
                1.0, 1.0, 1.0,
                null, null, null
        ));

        scrollContent.addChild(transformSection);
    }

    /**
     * Компонент Camera с полным набором настроек, слайдером FOV, выбором цвета,
     * предупреждением HelpBox и секцией Command Buffers.
     */
    private void buildCameraComponent() {
        cameraSection = new UnityComponentSection("Camera", UnityIcons.cameraIcon(14.0f), true, overlayLayer);

        // Clear Flags
        cameraSection.addPropertyRow(UnityPropertyRows.dropdownRow(
                "Clear Flags",
                List.of("Skybox", "Solid Color", "Depth only", "Don't Clear"),
                0,
                null
        ));

        // Background
        MutableColor bgColor = MutableColor.rgba255(255, 255, 255, 255);
        cameraSection.addPropertyRow(UnityPropertyRows.colorFieldRow(
                "Background",
                bgColor,
                overlayLayer
        ));

        // Culling Mask
        cameraSection.addPropertyRow(UnityPropertyRows.dropdownRow(
                "Culling Mask",
                List.of("Mixed...", "Everything", "Nothing", "Default", "TransparentFX", "Player"),
                0,
                null
        ));

        // Projection
        cameraSection.addPropertyRow(UnityPropertyRows.dropdownRow(
                "Projection",
                List.of("Perspective", "Orthographic"),
                0,
                null
        ));

        // FOV Axis
        cameraSection.addPropertyRow(UnityPropertyRows.dropdownRow(
                "FOV Axis",
                List.of("Vertical", "Horizontal"),
                0,
                null
        ));

        // Field of View (Slider + NumberField)
        cameraSection.addPropertyRow(UnityPropertyRows.sliderWithFieldRow(
                "Field of View",
                1.0f, 179.0f, 60.0f,
                val -> {
                    if (onFovChanged != null) {
                        onFovChanged.accept(val);
                    }
                }
        ));

        // Physical Camera
        cameraSection.addPropertyRow(UnityPropertyRows.checkboxRow(
                "Physical Camera",
                false,
                null
        ));

        // Clipping Planes (Near 0.3, Far 1000)
        cameraSection.addPropertyRow(UnityPropertyRows.twoFieldsRow(
                "Clipping Planes",
                "Near", 0.3,
                "Far", 1000.0,
                null, null
        ));

        // Viewport Rect (X 0, Y 0, W 1, H 1)
        cameraSection.addPropertyRow(UnityPropertyRows.viewportRectBlock(
                "Viewport Rect",
                0.0, 0.0, 1.0, 1.0
        ));

        // Depth
        cameraSection.addPropertyRow(UnityPropertyRows.singleNumberRow(
                "Depth",
                -1.0,
                null
        ));

        // Rendering Path
        cameraSection.addPropertyRow(UnityPropertyRows.dropdownRow(
                "Rendering Path",
                List.of("Deferred", "Forward", "Legacy Vertex Lit", "Legacy Deferred"),
                0,
                null
        ));

        // Target Texture
        cameraSection.addPropertyRow(UnityPropertyRows.objectPickerRow(
                "Target Texture",
                "None", "Render Texture"
        ));

        // Occlusion Culling
        cameraSection.addPropertyRow(UnityPropertyRows.checkboxRow(
                "Occlusion Culling",
                true,
                null
        ));

        // HDR
        cameraSection.addPropertyRow(UnityPropertyRows.dropdownRow(
                "HDR",
                List.of("Use Graphics Settings", "Off", "On"),
                0,
                null
        ));

        // MSAA
        cameraSection.addPropertyRow(UnityPropertyRows.dropdownRow(
                "MSAA",
                List.of("Use Graphics Settings", "Off", "2x", "4x", "8x"),
                0,
                null
        ));

        // Allow Dynamic Resolution
        cameraSection.addPropertyRow(UnityPropertyRows.checkboxRow(
                "Allow Dynamic Resolution",
                false,
                null
        ));

        // HelpBox с предупреждением из Unity
        cameraSection.addPropertyRow(UnityPropertyRows.helpBox(
                "Deferred and MultiSampleAntiAliasing is not supported. This camera will render without MSAA buffers. Disable Deferred if you want to use MSAA."
        ));

        // Target Display
        cameraSection.addPropertyRow(UnityPropertyRows.dropdownRow(
                "Target Display",
                List.of("Display 1", "Display 2", "Display 3", "Display 4"),
                0,
                null
        ));

        // Command Buffers Foldout
        cameraSection.addPropertyRow(UnityPropertyRows.commandBuffersBlock());

        scrollContent.addChild(cameraSection);
    }

    /**
     * Компонент Audio Listener.
     */
    private void buildAudioListenerComponent() {
        audioListenerSection = new UnityComponentSection("Audio Listener", UnityIcons.audioListenerIcon(14.0f), true, overlayLayer);

        // У Audio Listener нет параметров по умолчанию, только статус
        Label hint = new Label("Audio Listener has no configurable parameters.");
        hint.color(UnityStyles.TEXT_MUTED);
        hint.layout(style -> style.height(UnityStyles.ROW_HEIGHT).padding(2.0f, 0.0f));
        audioListenerSection.addPropertyRow(hint);

        scrollContent.addChild(audioListenerSection);
    }

    /**
     * Кнопка Add Component внизу инспектора.
     */
    private void buildAddComponentButton() {
        VBox bottomHost = new VBox();
        bottomHost.spacing(6.0f);
        bottomHost.layout(style -> style.widthPercent(100.0f)
                .padding(10.0f, 0.0f, 10.0f, 0.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        Button addComponentBtn = new Button("Add Component");
        UnityStyles.styleButton(addComponentBtn);
        addComponentBtn.layout(style -> style.size(230.0f, 26.0f)
                .align(Alignment.CENTER, Alignment.CENTER));

        int[] extraCount = {0};
        addComponentBtn.onClick(event -> {
            extraCount[0]++;
            String compName = switch (extraCount[0] % 4) {
                case 1 -> "Box Collider";
                case 2 -> "Rigidbody";
                case 3 -> "Mesh Filter";
                default -> "Light";
            };
            UnityComponentSection extraComp = new UnityComponentSection(compName, UnityIcons.gameObjectCube(14.0f), true, overlayLayer);
            Label prop = new Label("Configured dynamically for " + compName);
            prop.color(UnityStyles.TEXT_LABEL);
            prop.layout(style -> style.height(UnityStyles.ROW_HEIGHT));
            extraComp.addPropertyRow(prop);
            // Добавляем перед кнопкой Add Component
            scrollContent.insertChild(scrollContent.children().size() - 1, extraComp);
        });

        bottomHost.addChild(addComponentBtn);
        scrollContent.addChild(bottomHost);
    }

    private void notifyPositionChanged() {
        if (onPositionChanged != null) {
            onPositionChanged.accept(new float[]{currentPosition[0], currentPosition[1], currentPosition[2]});
        }
    }

    public UnityInspectorPanel onFovChanged(Consumer<Float> listener) {
        this.onFovChanged = listener;
        return this;
    }

    public UnityInspectorPanel onPositionChanged(Consumer<float[]> listener) {
        this.onPositionChanged = listener;
        return this;
    }

    /**
     * Показывает в шапке выбранный объект: имя и иконку, соответствующую его типу.
     *
     * @param name имя объекта
     * @param type тип объекта; {@code null} означает пустой GameObject
     */
    public void setTargetObject(String name, UnityEditorHierarchyView.GameObjectType type) {
        targetType = type != null ? type : UnityEditorHierarchyView.GameObjectType.EMPTY;
        if (objectIcon != null) {
            objectIcon.invalidate(InvalidationFlags.VISUAL);
        }
        setTargetObjectName(name);
    }

    public void setTargetObjectName(String name) {
        if (objectNameInput != null && name != null) {
            objectNameInput.text(name);
        }
    }
}

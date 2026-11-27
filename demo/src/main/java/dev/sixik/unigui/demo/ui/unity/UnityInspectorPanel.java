package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.*;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.dnd.ReorderController;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
    /**
     * Scene node currently loaded in the inspector, or null.
     */
    private UnitySceneObject targetObject;
    /**
     * Synchronizes edits to the selected node's label and visibility with the hierarchy.
     */
    private final Consumer<UnitySceneObject> onObjectEdited;
    /**
     * Guards the name input while loading an object.
     */
    private boolean loadingTarget;
    /**
     * Checkbox bound to the selected object's own visibility.
     */
    private Checkbox objectActive;
    /**
     * Message displayed when the scene selection is empty.
     */
    private final Label emptySelection = new Label("Select an object in Hierarchy or Scene");
    /**
     * Controls for the local position in scene units.
     */
    private UnityPropertyRows.VectorFields positionFields;
    /**
     * Controls for the local Euler angles in degrees.
     */
    private UnityPropertyRows.VectorFields rotationFields;
    /**
     * Controls for the local scale factors.
     */
    private UnityPropertyRows.VectorFields scaleFields;
    /**
     * Buffer reused to refresh the three transform rows.
     */
    private final MutableVec3 transformValue = new MutableVec3();
    /**
     * Whether the inspector can apply changes to the scene.
     */
    private boolean editable = true;
    /**
     * Position in the sequence of example components added from the button.
     */
    private int extraComponentCount;
    /**
     * Snapshots of custom Camera controls whose values are stored outside standard input widgets.
     */
    private final List<Supplier<Runnable>> cameraCheckpoints = new ArrayList<>();
    /**
     * Independent model preview below the editable properties.
     */
    private final UnityScenePreview preview = new UnityScenePreview();
    /**
     * Selected Project prefab, which takes precedence over the hierarchy selection.
     */
    private UnityPrefab selectedPrefab;

    public UnityInspectorPanel(OverlayLayer overlayLayer) {
        this(overlayLayer, null);
    }

    /**
     * Creates an inspector with optional scene editing.
     *
     * @param overlayLayer   overlay for component menus
     * @param onObjectEdited scene edit callback, or null for the standalone component demo
     */
    UnityInspectorPanel(OverlayLayer overlayLayer, Consumer<UnitySceneObject> onObjectEdited) {
        super(Orientation.VERTICAL);
        this.overlayLayer = overlayLayer;
        this.onObjectEdited = onObjectEdited;
        spacing(0.0f);
        layout(style -> style.widthPercent(100.0f).heightPercent(100.0f)
                .flexGrow(1.0f).flexShrink(1.0f));

        // 1. Скроллируемая область инспектора
        scrollContent.spacing(0.0f);
        scrollContent.layout(style -> style.widthPercent(100.0f)
                .padding(0.0f, 0.0f, 0.0f, 16.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        emptySelection.color(UnityStyles.TEXT_MUTED);
        emptySelection.wrap(true);
        emptySelection.layout(style -> style.widthPercent(100.0f).padding(8.0f));
        emptySelection.visibility(Visibility.COLLAPSED);
        buildGameObjectHeader();
        buildTransformComponent();
        buildCameraComponent();
        buildAudioListenerComponent();
        buildAddComponentButton();
        ReorderController.attach(scrollContent)
                .accepts("unity.component")
                .orientation(Orientation.VERTICAL)
                .indicator(UnityStyles.BORDER_INPUT_FOCUS, 2.0f, 6.0f)
                .itemFilter(child -> editable && child instanceof UnityComponentSection sec && sec.isDraggable());

        VBox inspectorContent = new VBox();
        inspectorContent.spacing(0.0f);
        inspectorContent.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));
        inspectorContent.addChild(emptySelection);
        inspectorContent.addChild(scrollContent);
        ScrollView scrollView = new ScrollView(inspectorContent);
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
        preview.layout(style -> style.height(155).maxHeightPercent(45).flexGrow(0).flexShrink(0));
        preview.visibility(Visibility.COLLAPSED);
        addChild(preview);
    }

    /**
     * Shows the Project selection below the Inspector's scrollable properties.
     *
     * @param prefab selected template, or null to return to the hierarchy selection
     */
    void showPrefab(UnityPrefab prefab) {
        selectedPrefab = prefab;
        refreshPreview();
    }

    /**
     * Shows the prefab selection or a fresh instance of the selected Minecraft model.
     */
    private void refreshPreview() {
        if (selectedPrefab != null) {
            preview.setContent(selectedPrefab.instantiate(), selectedPrefab.name() + " (Prefab)");
            preview.visibility(Visibility.VISIBLE);
        } else if (targetObject != null && targetObject.type().isMinecraft()) {
            UnitySceneObject model = new UnitySceneObject(targetObject.name(), targetObject.type(), targetObject.minecraftId());
            preview.setContent(model, model.minecraftId());
            preview.visibility(Visibility.VISIBLE);
        } else {
            preview.setContent(null, "");
            preview.visibility(Visibility.COLLAPSED);
        }
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
        objectActive = activeCheck;
        UnityStyles.styleCheckbox(activeCheck);
        activeCheck.checked(true);
        activeCheck.layout(style -> style.size(14.0f, UnityStyles.ROW_HEIGHT).alignSelf(Align.CENTER).flexGrow(0.0f).flexShrink(0.0f));
        activeCheck.onStateChanged(event -> {
            if (editable && !loadingTarget && targetObject != null) {
                targetObject.setVisible(activeCheck.checked());
                onObjectEdited.accept(targetObject);
            }
        });

        TextInput nameInput = new TextInput("Main Camera");
        UnityStyles.styleTextInput(nameInput);
        nameInput.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));
        this.objectNameInput = nameInput;
        nameInput.onTextChanged(event -> {
            if (editable && !loadingTarget && targetObject != null && !event.newText().isBlank()) {
                targetObject.rename(event.newText().strip());
                onObjectEdited.accept(targetObject);
            }
        });

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
        transformSection.removable(false);
        scrollContent.addChild(transformSection);
        if (onObjectEdited != null) {
            positionFields = addVector("Position", 0, UnitySceneObject::setPosition);
            rotationFields = addVector("Rotation", 0, UnitySceneObject::setAngles);
            scaleFields = addVector("Scale", 1, UnitySceneObject::setScale);
            return;
        }

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
                cameraCheckpoints::add
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
        cameraSection.addPropertyRow(UnityPropertyRows.commandBuffersBlock(cameraCheckpoints::add));

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

        addComponentBtn.onClick(event -> {
            if (!editable) return;
            String compName = switch (++extraComponentCount % 4) {
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
            scrollContent.applyQueuedMutations();
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

    /**
     * Shows a real scene object and binds the Inspector's transform edits to it.
     *
     * @param object selected object, or null for an empty Inspector
     */
    void setTargetObject(UnitySceneObject object) {
        boolean changed = targetObject != object;
        targetObject = object;
        if (changed && selectedPrefab == null) refreshPreview();
        boolean selected = object != null;
        scrollContent.visibility(selected ? Visibility.VISIBLE : Visibility.COLLAPSED);
        emptySelection.visibility(selected ? Visibility.COLLAPSED : Visibility.VISIBLE);
        if (object == null) return;

        loadingTarget = true;
        try {
            setTargetObject(object.name(), object.type());
            objectActive.silentChecked(object.visible());
        } finally {
            loadingTarget = false;
        }
        refreshTransform(object);
    }

    /**
     * Synchronizes the active checkbox with a visibility change in the hierarchy.
     *
     * @param object changed scene node
     */
    void refreshVisibility(UnitySceneObject object) {
        if (targetObject == object) objectActive.silentChecked(object.visible());
    }

    /**
     * Copies the selected object's current local transform into the Inspector.
     *
     * @param object scene object edited by the gizmo
     */
    void refreshTransform(UnitySceneObject object) {
        if (object == null || targetObject != object || positionFields == null) return;
        positionFields.set(object.position(transformValue));
        rotationFields.set(object.angles(transformValue));
        scaleFields.set(object.scale(transformValue));
    }

    /**
     * Binds the three inputs to a vector property of the selected node.
     *
     * @param label   row label
     * @param initial initial value on each axis before a scene object is selected
     * @param apply   applies all components in the property's own units
     * @return editable inputs for the row
     */
    private UnityPropertyRows.VectorFields addVector(String label, float initial, VectorEdit apply) {
        UnityPropertyRows.VectorFields fields = UnityPropertyRows.vectorFields(label, initial, initial, initial);
        fields.onChange(() -> {
            if (!editable || targetObject == null) return;
            float x = (float) fields.x().value(), y = (float) fields.y().value(), z = (float) fields.z().value();
            if (Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)) apply.set(targetObject, x, y, z);
        });
        transformSection.addPropertyRow(fields.row());
        return fields;
    }

    /**
     * Writes a scene object's local transform property.
     */
    @FunctionalInterface
    private interface VectorEdit {
        /**
         * Writes the three components together.
         *
         * @param object selected scene node
         * @param x      X component
         * @param y      Y component
         * @param z      Z component
         */
        void set(UnitySceneObject object, float x, float y, float z);
    }

    /**
     * Controls whether the object's properties and components can receive editor input.
     *
     * @param editable whether edits are allowed
     */
    void setEditable(boolean editable) {
        this.editable = editable;
        scrollContent.enabled(editable);
    }

    /**
     * Captures the demo component list and its editable values for rollback.
     *
     * @return an action that restores the existing Inspector widgets and their pre-Play values
     */
    Runnable captureComponentState() {
        scrollContent.applyQueuedMutations();
        List<Widget> sections = List.copyOf(scrollContent.children());
        List<Runnable> restoreValues = new ArrayList<>();
        for (Widget section : sections) captureValues(section, restoreValues);
        for (Supplier<Runnable> checkpoint : cameraCheckpoints) restoreValues.add(checkpoint.get());
        int savedCount = extraComponentCount;
        return () -> {
            scrollContent.applyQueuedMutations();
            for (Widget section : scrollContent.children()) {
                if (!sections.contains(section) && section instanceof UnityComponentSection component)
                    component.removeSelf();
                else scrollContent.removeChild(section);
            }
            overlayLayer.applyQueuedMutations();
            for (Widget section : sections) {
                scrollContent.addChild(section);
                if (section instanceof UnityComponentSection component) component.restoreHelpTooltip();
            }
            scrollContent.applyQueuedMutations();
            extraComponentCount = savedCount;
            restoreValues.forEach(Runnable::run);
        };
    }

    /**
     * Captures the editable demo fields that are independent of the scene model.
     *
     * @param widget        current Inspector widget
     * @param restoreValues records how to restore each value
     */
    private void captureValues(Widget widget, List<Runnable> restoreValues) {
        if (widget == objectActive || widget == objectNameInput || widget == transformSection.contentHost()) return;
        if (widget instanceof UnityComponentSection section) {
            boolean expanded = section.expanded();
            restoreValues.add(() -> section.expanded(expanded));
        }
        if (widget instanceof Checkbox checkbox) {
            var state = checkbox.state();
            restoreValues.add(() -> checkbox.state(state));
        } else if (widget instanceof ComboBox combo) {
            int selection = combo.selectedIndex();
            restoreValues.add(() -> combo.selectedIndex(selection));
        } else if (widget instanceof NumberField field) {
            double value = field.value();
            restoreValues.add(() -> field.silentValue(value));
        } else if (widget instanceof Slider slider) {
            float value = slider.value();
            restoreValues.add(() -> slider.value(value));
        } else {
            if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
            for (Widget child : widget.children()) captureValues(child, restoreValues);
        }
    }

    public void setTargetObjectName(String name) {
        if (objectNameInput != null && name != null) {
            objectNameInput.text(name);
        }
    }
}

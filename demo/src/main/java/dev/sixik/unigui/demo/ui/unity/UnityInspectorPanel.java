package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Vec3View;
import dev.sixik.unigui.api.scene.FogVolume;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.*;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.dnd.ReorderController;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.*;
import dev.sixik.unigui.api.widget.CheckboxState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Accurate visual and interactive replica of the Unity Inspector window.
 */
public class UnityInspectorPanel extends LinearBox {
    private final OverlayLayer overlayLayer;

    // Section references for external interaction
    private UnityComponentSection transformSection;
    private UnityComponentSection cameraSection;
    /**
     * Mesh Renderer section and its shape dropdown; null in the standalone demo.
     */
    private UnityComponentSection meshSection;
    private ComboBox meshShape;
    /**
     * Rotator section and its speed field; null in the standalone demo.
     */
    private UnityComponentSection rotatorSection;
    private NumberField rotatorSpeed;
    /**
     * Button opening the Add Component popup for scene objects.
     */
    private Button addComponentButton;
    /**
     * Material Effect section and its controls; null in the standalone demo.
     */
    private UnityComponentSection effectSection;
    private ComboBox effectPreset;
    private ColorPicker effectColor;
    private NumberField effectStrength;
    private NumberField effectPeriod;
    /**
     * Fog Volume section and its controls; null in the standalone demo.
     */
    private UnityComponentSection fogSection;
    private ComboBox fogShape;
    private ColorPicker fogColor;
    private NumberField fogDensity, fogEdge, fogNoise, fogNoiseScale, fogScattering;
    /**
     * Local noise drift in units per second along X, Y and Z.
     */
    private UnityPropertyRows.VectorFields fogDrift;
    /**
     * Light color control.
     */
    private ColorPicker lightColor;
    private UnityComponentSection audioListenerSection;

    // Inspector state
    private boolean locked = false;
    private final VBox scrollContent = new VBox();
    private TextInput objectNameInput;
    private CanvasWidget objectIcon;
    private UnityEditorHierarchyView.GameObjectType targetType = UnityEditorHierarchyView.GameObjectType.CAMERA;

    // Change listeners for scene synchronization
    private Consumer<Float> onFovChanged;
    private Consumer<float[]> onPositionChanged;
    private final float[] currentPosition = new float[]{0.0f, 0.0f, 0.0f};
    /**
     * Active scene node loaded in the inspector, or null.
     */
    private UnitySceneObject targetObject;
    /**
     * Every selected scene node edited together, including the active one.
     */
    private List<UnitySceneObject> targets = List.of();
    /**
     * Light component, shown when every target is a light.
     */
    private UnityComponentSection lightSection;
    /**
     * Light type selector.
     */
    private ComboBox lightType;
    /**
     * Brightness multiplier.
     */
    private NumberField lightIntensity;
    /**
     * Reach of local lights, outer and inner cone angles of spotlights, and shadow quality fields.
     */
    private NumberField lightRange, lightSpotAngle, lightInnerAngle,
            lightShadowStrength, lightShadowBias, lightShadowSize;
    /**
     * Toggle and resolution selector for light shadows.
     */
    private Checkbox lightShadows;
    private ComboBox lightShadowResolution;
    /**
     * Rows hidden for light types or states that ignore them.
     */
    private HBox lightRangeRow, lightSpotRow, lightInnerRow,
            lightShadowsRow, lightShadowResRow, lightShadowStrengthRow, lightShadowBiasRow, lightShadowSizeRow;
    /**
     * Supported square shadow map resolutions in texels.
     */
    private static final int[] SHADOW_RESOLUTIONS = {128, 256, 512, 1024, 2048};
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

        // 1. Scrollable inspector area
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
        if (onObjectEdited != null) {
            // Scene objects only show the components they really have; the rest come from Add Component.
            buildMeshComponent();
            buildLightComponent();
            buildRotatorComponent();
            buildEffectComponent();
            buildFogComponent();
            buildComponentPickerButton();
        } else {
            buildCameraComponent();
            buildAudioListenerComponent();
            buildAddComponentButton();
        }
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
     * GameObject header (blue cube icon, active toggle, "Main Camera" name, Static, Tag, Layer).
     */
    private void buildGameObjectHeader() {
        VBox goHeader = new VBox();
        goHeader.spacing(4.0f);
        goHeader.layout(style -> style.widthPercent(100.0f)
                .padding(8.0f, 6.0f, 8.0f, 6.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        // Row 1: [Blue cube] [x] [Main Camera            ] [ ] Static ▼
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
                boolean visible = activeCheck.state() == CheckboxState.CHECKED;
                for (UnitySceneObject target : targets) {
                    target.setVisible(visible);
                    onObjectEdited.accept(target);
                }
            }
        });

        TextInput nameInput = new TextInput("Main Camera");
        UnityStyles.styleTextInput(nameInput);
        nameInput.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));
        this.objectNameInput = nameInput;
        nameInput.onTextChanged(event -> {
            if (editable && !loadingTarget && targetObject != null && !event.newText().isBlank()) {
                for (UnitySceneObject target : targets) {
                    target.rename(event.newText().strip());
                    onObjectEdited.accept(target);
                }
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

        // Row 2: Tag [MainCamera  v]   Layer [Player  v]
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
     * Transform component (Position, Rotation, Scale).
     */
    private void buildTransformComponent() {
        transformSection = new UnityComponentSection("Transform", UnityIcons.transformAxes(14.0f), false, overlayLayer);
        transformSection.draggable(false);
        transformSection.removable(false);
        scrollContent.addChild(transformSection);
        if (onObjectEdited != null) {
            positionFields = addVector("Position", 0, (object, out) -> object.position(out), UnitySceneObject::setPosition);
            rotationFields = addVector("Rotation", 0, UnitySceneObject::angles, UnitySceneObject::setAngles);
            scaleFields = addVector("Scale", 1, (object, out) -> object.scale(out), UnitySceneObject::setScale);
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
     * Light component: type, color, intensity and the range and cone of local lights. Edits apply to every
     * selected light; values that differ between them show a dash.
     */
    private void buildLightComponent() {
        lightSection = new UnityComponentSection("Light", UnityIcons.gameObjectIcon(14.0f,
                () -> UnityEditorHierarchyView.GameObjectType.LIGHT), true, overlayLayer);
        lightSection.draggable(false);
        lightSection.onActiveChanged(active -> editComponents(target -> {
            if (target.light() != null) target.setComponentEnabled(UnitySceneObject.Component.LIGHT, active);
        }));
        lightSection.onRemove(() -> editComponents(target -> target.setLight(null)));

        HBox typeRow = labeledRow("Type");
        lightType = new ComboBox();
        List<String> kinds = new ArrayList<>();
        for (UnityLight.Kind kind : UnityLight.Kind.values()) kinds.add(kind.label());
        lightType.items(kinds);
        UnityStyles.styleComboBox(lightType);
        lightType.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));
        lightType.onSelectionChanged(event -> {
            UnityLight.Kind kind = UnityLight.Kind.fromLabel(lightType.selectedItem());
            if (kind != null) editLights(light -> light.withKind(kind));
        });
        typeRow.addChild(lightType);
        lightSection.addPropertyRow(typeRow);

        lightColor = colorPicker(rgb -> editLights(light -> light.withColor(rgb)));
        lightSection.addPropertyRow(colorRow("Color", lightColor));

        lightIntensity = numberField(0, 64);
        lightIntensity.step(0.1);
        lightIntensity.onValueChanged(event -> editLights(light -> light.withIntensity((float) event.newValue())));
        lightSection.addPropertyRow(fieldRow("Intensity", lightIntensity));

        lightRange = numberField(0.01, 512);
        lightRange.step(0.5);
        lightRange.onValueChanged(event -> editLights(light -> light.withRange((float) event.newValue())));
        lightRangeRow = fieldRow("Range", lightRange);
        lightSection.addPropertyRow(lightRangeRow);

        lightSpotAngle = numberField(1, 170);
        lightSpotAngle.onValueChanged(event -> editLights(light -> light.withSpotAngle((float) event.newValue())));
        lightSpotRow = fieldRow("Spot Angle", lightSpotAngle);
        lightSection.addPropertyRow(lightSpotRow);

        lightInnerAngle = numberField(0, 170);
        lightInnerAngle.onValueChanged(event -> editLights(light -> light.withInnerSpotAngle((float) event.newValue())));
        lightInnerRow = fieldRow("Inner Spot Angle", lightInnerAngle);
        lightSection.addPropertyRow(lightInnerRow);

        lightShadowsRow = labeledRow("Shadows");
        lightShadows = new Checkbox();
        UnityStyles.styleCheckbox(lightShadows);
        lightShadows.layout(style -> style.size(14.0f, UnityStyles.ROW_HEIGHT).alignSelf(Align.CENTER));
        lightShadows.onStateChanged(event -> {
            boolean enabled = event.newState() == CheckboxState.CHECKED;
            editLights(light -> light.withShadows(enabled));
        });
        lightShadowsRow.addChild(lightShadows);
        lightSection.addPropertyRow(lightShadowsRow);

        lightShadowResRow = labeledRow("Shadow Resolution");
        lightShadowResolution = new ComboBox();
        List<String> resolutions = new ArrayList<>(SHADOW_RESOLUTIONS.length);
        for (int res : SHADOW_RESOLUTIONS) resolutions.add(Integer.toString(res));
        lightShadowResolution.items(resolutions);
        UnityStyles.styleComboBox(lightShadowResolution);
        lightShadowResolution.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));
        lightShadowResolution.onSelectionChanged(event -> {
            int index = lightShadowResolution.selectedIndex();
            if (index >= 0 && index < SHADOW_RESOLUTIONS.length) {
                int resolution = SHADOW_RESOLUTIONS[index];
                editLights(light -> light.withShadowResolution(resolution));
            }
        });
        lightShadowResRow.addChild(lightShadowResolution);
        lightSection.addPropertyRow(lightShadowResRow);

        lightShadowStrength = numberField(0, 1);
        lightShadowStrength.step(0.05);
        lightShadowStrength.onValueChanged(event -> editLights(light -> light.withShadowStrength((float) event.newValue())));
        lightShadowStrengthRow = fieldRow("Shadow Strength", lightShadowStrength);
        lightSection.addPropertyRow(lightShadowStrengthRow);

        lightShadowBias = numberField(0, 0.05);
        lightShadowBias.step(0.0005);
        lightShadowBias.onValueChanged(event -> editLights(light -> light.withShadowBias((float) event.newValue())));
        lightShadowBiasRow = fieldRow("Shadow Bias", lightShadowBias);
        lightSection.addPropertyRow(lightShadowBiasRow);

        lightShadowSize = numberField(0.1, 256);
        lightShadowSize.step(0.5);
        lightShadowSize.onValueChanged(event -> editLights(light -> light.withShadowSize((float) event.newValue())));
        lightShadowSizeRow = fieldRow("Shadow Size", lightShadowSize);
        lightSection.addPropertyRow(lightShadowSizeRow);

        lightSection.visibility(Visibility.COLLAPSED);
        scrollContent.addChild(lightSection);
    }

    /**
     * Mesh Renderer component: the primitive shape the object draws.
     */
    private void buildMeshComponent() {
        meshSection = new UnityComponentSection("Mesh Renderer", UnityIcons.gameObjectCube(14.0f), true, overlayLayer);
        meshSection.draggable(false);
        meshSection.onActiveChanged(active -> editComponents(target -> {
            if (target.meshShape() != null)
                target.setComponentEnabled(UnitySceneObject.Component.MESH_RENDERER, active);
        }));
        meshSection.onRemove(() -> editComponents(target -> target.setMeshShape(null)));
        HBox shapeRow = labeledRow("Mesh");
        meshShape = new ComboBox();
        List<String> shapes = new ArrayList<>();
        for (UnityEditorHierarchyView.GameObjectType shape : UnitySceneObject.MESH_SHAPES)
            shapes.add(shapeLabel(shape));
        meshShape.items(shapes);
        UnityStyles.styleComboBox(meshShape);
        meshShape.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f));
        meshShape.onSelectionChanged(event -> {
            int index = meshShape.selectedIndex();
            if (index < 0 || index >= UnitySceneObject.MESH_SHAPES.size()) return;
            UnityEditorHierarchyView.GameObjectType shape = UnitySceneObject.MESH_SHAPES.get(index);
            editComponents(target -> {
                if (target.meshShape() != null) target.setMeshShape(shape);
            });
        });
        shapeRow.addChild(meshShape);
        meshSection.addPropertyRow(shapeRow);
        meshSection.visibility(Visibility.COLLAPSED);
        scrollContent.addChild(meshSection);
    }

    /**
     * Rotator component: a small script that spins the object around its local Y axis in Play mode.
     */
    private void buildRotatorComponent() {
        rotatorSection = new UnityComponentSection("Rotator", UnityIcons.transformAxes(14.0f), true, overlayLayer);
        rotatorSection.draggable(false);
        rotatorSection.onActiveChanged(active -> editComponents(target -> {
            if (!Float.isNaN(target.rotatorSpeed()))
                target.setComponentEnabled(UnitySceneObject.Component.ROTATOR, active);
        }));
        rotatorSection.onRemove(() -> editComponents(target -> target.setRotatorSpeed(Float.NaN)));
        rotatorSpeed = numberField(-3600, 3600);
        rotatorSpeed.step(5);
        rotatorSpeed.onValueChanged(event -> editComponents(target -> {
            if (!Float.isNaN(target.rotatorSpeed())) target.setRotatorSpeed((float) event.newValue());
        }));
        rotatorSection.addPropertyRow(fieldRow("Speed (\u00B0/s)", rotatorSpeed));
        Label hint = new Label("Spins the object around its Y axis in Play mode.");
        hint.color(UnityStyles.TEXT_MUTED);
        hint.layout(style -> style.widthPercent(100.0f).height(UnityStyles.ROW_HEIGHT));
        rotatorSection.addPropertyRow(hint);
        rotatorSection.visibility(Visibility.COLLAPSED);
        scrollContent.addChild(rotatorSection);
    }

    /**
     * Material Effect component: a ready-made surface shader with its color, strength and loop length.
     */
    private void buildEffectComponent() {
        effectSection = new UnityComponentSection("Material Effect", UnityIcons.presetIcon(14.0f), true, overlayLayer);
        effectSection.draggable(false);
        effectSection.onRemove(() -> editComponents(target -> target.setMaterialEffectSettings(null)));
        effectSection.onActiveChanged(active -> editComponents(target -> {
            if (target.materialEffectSettings() != null) {
                target.setComponentEnabled(UnitySceneObject.Component.MATERIAL_EFFECT, active);
            }
        }));
        HBox shaderRow = labeledRow("Shader");
        effectPreset = new ComboBox();
        List<String> presets = new ArrayList<>();
        for (UnityMaterialEffect.Preset preset : UnityMaterialEffect.Preset.values()) presets.add(preset.label());
        effectPreset.items(presets);
        UnityStyles.styleComboBox(effectPreset);
        effectPreset.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f).flexShrink(1.0f));
        effectPreset.onSelectionChanged(event -> {
            int index = effectPreset.selectedIndex();
            if (index < 0) return;
            UnityMaterialEffect.Preset preset = UnityMaterialEffect.Preset.values()[index];
            editEffects(effect -> effect.withPreset(preset));
        });
        shaderRow.addChild(effectPreset);
        effectSection.addPropertyRow(shaderRow);
        effectColor = colorPicker(rgb -> editEffects(effect -> effect.withColor(rgb)));
        effectSection.addPropertyRow(colorRow("Color", effectColor));
        effectStrength = numberField(0, 8);
        effectStrength.step(0.1);
        effectStrength.onValueChanged(event -> editEffects(effect -> effect.withStrength((float) event.newValue())));
        effectSection.addPropertyRow(fieldRow("Strength", effectStrength));
        effectPeriod = numberField(0.25, 60);
        effectPeriod.step(0.25);
        effectPeriod.onValueChanged(event -> editEffects(effect -> effect.withPeriod((float) event.newValue())));
        effectSection.addPropertyRow(fieldRow("Loop (s)", effectPeriod));
        effectSection.visibility(Visibility.COLLAPSED);
        scrollContent.addChild(effectSection);
    }

    /**
     * Fog Volume component: shape, haze color, density, edge softness, drifting noise and spotlight scattering.
     * The volume fills the object's local box from -1 to 1 or the sphere inside it, so Transform scale sets its size.
     */
    private void buildFogComponent() {
        fogSection = new UnityComponentSection("Fog Volume", UnityIcons.gameObjectIcon(14.0f,
                () -> UnityEditorHierarchyView.GameObjectType.FOG_VOLUME), true, overlayLayer);
        fogSection.draggable(false);
        fogSection.onRemove(() -> editComponents(target -> target.setFog(null)));
        fogSection.onActiveChanged(active -> editComponents(target -> {
            if (target.fog() != null) target.setComponentEnabled(UnitySceneObject.Component.FOG_VOLUME, active);
        }));
        HBox shapeRow = labeledRow("Shape");
        fogShape = new ComboBox();
        fogShape.items(List.of("Box", "Sphere"));
        UnityStyles.styleComboBox(fogShape);
        fogShape.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f).flexShrink(1.0f));
        fogShape.onSelectionChanged(event -> {
            int index = fogShape.selectedIndex();
            if (index < 0) return;
            FogVolume.Shape shape = FogVolume.Shape.values()[index];
            editFog(fog -> fog.withShape(shape));
        });
        shapeRow.addChild(fogShape);
        fogSection.addPropertyRow(shapeRow);
        fogColor = colorPicker(rgb -> editFog(fog -> fog.withColor(rgb)));
        fogSection.addPropertyRow(colorRow("Color", fogColor));
        fogDensity = fogField(0, 8, 0.05, UnityFog::withDensity);
        fogSection.addPropertyRow(fieldRow("Density", fogDensity));
        fogEdge = fogField(0, 1, 0.05, UnityFog::withEdgeSoftness);
        fogSection.addPropertyRow(fieldRow("Edge Softness", fogEdge));
        fogNoise = fogField(0, 1, 0.05, UnityFog::withNoiseStrength);
        fogSection.addPropertyRow(fieldRow("Noise Strength", fogNoise));
        fogNoiseScale = fogField(0.05, 32, 0.1, UnityFog::withNoiseScale);
        fogSection.addPropertyRow(fieldRow("Noise Scale", fogNoiseScale));
        fogDrift = UnityPropertyRows.vectorFields("Noise Drift", 0, 0, 0);
        NumberField[] drift = {fogDrift.x(), fogDrift.y(), fogDrift.z()};
        for (int i = 0; i < 3; i++) {
            int axis = i;
            drift[i].range(-16, 16);
            drift[i].step(0.05);
            drift[i].onValueChanged(event -> {
                float value = (float) event.newValue();
                if (Float.isFinite(value)) editFog(fog -> fog.withDrift(axis, value));
            });
        }
        fogSection.addPropertyRow(fogDrift.row());
        fogScattering = fogField(0, 16, 0.1, UnityFog::withLightScattering);
        fogSection.addPropertyRow(fieldRow("Light Scattering", fogScattering));
        Label hint = new Label("Scale the object to resize the volume. Spot lights shine through it.");
        hint.color(UnityStyles.TEXT_MUTED);
        hint.wrap(true);
        hint.layout(style -> style.widthPercent(100.0f));
        fogSection.addPropertyRow(hint);
        fogSection.visibility(Visibility.COLLAPSED);
        scrollContent.addChild(fogSection);
    }

    /**
     * @param min  smallest accepted value
     * @param max  largest accepted value
     * @param step arrow and drag increment
     * @param with returns the settings with the edited property replaced
     * @return a numeric input that applies its value to every selected fog volume
     */
    private NumberField fogField(double min, double max, double step,
                                 java.util.function.BiFunction<UnityFog, Float, UnityFog> with) {
        NumberField field = numberField(min, max);
        field.step(step);
        field.onValueChanged(event -> {
            float value = (float) event.newValue();
            if (Float.isFinite(value)) editFog(fog -> with.apply(fog, value));
        });
        return field;
    }

    /**
     * Applies one change to the Fog Volume of every selected object.
     *
     * @param edit derives the new settings from each object's current settings
     */
    private void editFog(java.util.function.UnaryOperator<UnityFog> edit) {
        editComponents(target -> {
            if (target.fog() != null) target.setFog(edit.apply(target.fog()));
        });
    }

    /**
     * Shows the Fog Volume settings when every selected object has one. A value that differs shows a dash.
     */
    private void refreshFog() {
        if (fogSection == null) return;
        boolean all = !targets.isEmpty() && targets.stream().allMatch(t -> t.fog() != null);
        fogSection.visibility(all ? Visibility.VISIBLE : Visibility.COLLAPSED);
        if (!all) return;
        boolean wasLoading = loadingTarget;
        loadingTarget = true;
        try {
            UnityFog active = targetObject.fog();
            fogShape.silentSelectedIndex(active.shape().ordinal());
            fogColor.argb(0xFF000000 | active.colorRgb());
            showFog(fogDensity, UnityFog::density);
            showFog(fogEdge, UnityFog::edgeSoftness);
            showFog(fogNoise, UnityFog::noiseStrength);
            showFog(fogNoiseScale, UnityFog::noiseScale);
            showFog(fogDrift.x(), fog -> fog.drift(0));
            showFog(fogDrift.y(), fog -> fog.drift(1));
            showFog(fogDrift.z(), fog -> fog.drift(2));
            showFog(fogScattering, UnityFog::lightScattering);
            showEnabled(fogSection, UnitySceneObject.Component.FOG_VOLUME);
        } finally {
            loadingTarget = wasLoading;
        }
    }

    /**
     * Shows a fog property, or a dash if the selected volumes disagree.
     *
     * @param field input to update without sending edits
     * @param value reads the property from one volume
     */
    private void showFog(NumberField field, java.util.function.ToDoubleFunction<UnityFog> value) {
        double first = value.applyAsDouble(targetObject.fog());
        boolean mixed = false;
        for (UnitySceneObject target : targets) mixed |= value.applyAsDouble(target.fog()) != first;
        showNumber(field, first, mixed);
    }

    /**
     * Applies one change to the Material Effect of every selected object.
     *
     * @param edit derives the new settings from each object's current settings
     */
    private void editEffects(java.util.function.UnaryOperator<UnityMaterialEffect> edit) {
        editComponents(target -> {
            if (target.materialEffectSettings() != null) {
                target.setMaterialEffectSettings(edit.apply(target.materialEffectSettings()));
            }
        });
    }

    /**
     * @param onColor receives the picked color as 0xRRGGBB
     * @return a color picker whose swatch fills the rest of its row and opens the picker popup
     */
    private ColorPicker colorPicker(java.util.function.IntConsumer onColor) {
        ColorPicker picker = new ColorPicker();
        if (overlayLayer != null) picker.overlayLayer(overlayLayer);
        picker.layout(style -> style.height(UnityStyles.ROW_HEIGHT).width(0.0f).flexGrow(1.0f).flexShrink(1.0f));
        picker.onColorChanged(event -> {
            if (!loadingTarget) onColor.accept(picker.argb() & 0xFFFFFF);
        });
        return picker;
    }

    /**
     * @param label  property caption
     * @param picker color control
     * @return the property row
     */
    private static HBox colorRow(String label, ColorPicker picker) {
        HBox row = labeledRow(label);
        row.addChild(picker);
        return row;
    }

    /**
     * Shows a component header checkbox for the selection: checked, unchecked, or mixed when the objects differ.
     * The section dims its properties while the component is off, as Unity does.
     *
     * @param section   component section
     * @param component component of the section
     */
    private void showEnabled(UnityComponentSection section, UnitySceneObject.Component component) {
        if (section.activeCheckbox() == null) return;
        boolean first = targetObject.componentEnabled(component);
        boolean mixed = targets.stream().anyMatch(t -> t.componentEnabled(component) != first);
        section.activeCheckbox().silentState(mixed ? CheckboxState.INDETERMINATE
                : first ? CheckboxState.CHECKED : CheckboxState.UNCHECKED);
        boolean on = mixed || first;
        section.contentHost().opacity(on ? 1.0f : 0.5f);
        section.contentHost().enabled(on);
    }

    /**
     * Add Component button that opens the searchable component list for the selection.
     */
    private void buildComponentPickerButton() {
        VBox bottomHost = new VBox();
        bottomHost.layout(style -> style.widthPercent(100.0f)
                .padding(10.0f, 0.0f, 10.0f, 0.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));
        addComponentButton = new Button("Add Component");
        UnityStyles.styleButton(addComponentButton);
        addComponentButton.layout(style -> style.size(230.0f, 26.0f).align(Alignment.CENTER, Alignment.CENTER));
        addComponentButton.onClick(event -> openComponentPicker());
        bottomHost.addChild(addComponentButton);
        scrollContent.addChild(bottomHost);
    }

    /**
     * Opens the Add Component popup below its button.
     *
     * @return the popup, or null when nothing is selected or editing is locked
     */
    UnityAddComponentWindow openComponentPicker() {
        if (!editable || targets.isEmpty() || overlayLayer == null) return null;
        return UnityAddComponentWindow.open(overlayLayer, addComponentButton.layoutBounds(), componentEntries());
    }

    /**
     * @return the components the popup offers, each available while some selected object lacks it
     */
    private List<UnityAddComponentWindow.Entry> componentEntries() {
        return List.of(
                new UnityAddComponentWindow.Entry("Mesh Renderer", "Rendering",
                        () -> targets.stream().anyMatch(t -> t.meshShape() == null && !t.type().isMinecraft()),
                        () -> editComponents(t -> {
                            if (t.meshShape() == null && !t.type().isMinecraft())
                                t.setMeshShape(UnityEditorHierarchyView.GameObjectType.CUBE);
                        })),
                new UnityAddComponentWindow.Entry("Light", "Rendering",
                        () -> targets.stream().anyMatch(t -> t.light() == null),
                        () -> editComponents(t -> {
                            if (t.light() == null) t.setLight(UnityLight.defaults(UnityLight.Kind.POINT));
                        })),
                new UnityAddComponentWindow.Entry("Fog Volume", "Effects",
                        () -> targets.stream().anyMatch(t -> t.fog() == null),
                        () -> editComponents(t -> {
                            if (t.fog() == null) t.setFog(UnityFog.defaults(FogVolume.Shape.BOX));
                        })),
                new UnityAddComponentWindow.Entry("Material Effect", "Effects",
                        () -> targets.stream().anyMatch(t -> t.materialEffectSettings() == null),
                        () -> editComponents(t -> {
                            if (t.materialEffectSettings() == null) {
                                t.setMaterialEffectSettings(UnityMaterialEffect.defaults(UnityMaterialEffect.Preset.SCANLINE));
                            }
                        })),
                new UnityAddComponentWindow.Entry("Rotator", "Scripts",
                        () -> targets.stream().anyMatch(t -> Float.isNaN(t.rotatorSpeed())),
                        () -> editComponents(t -> {
                            if (Float.isNaN(t.rotatorSpeed())) t.setRotatorSpeed(90);
                        })));
    }

    /**
     * Applies a component change to every selected object, reports each edit and refreshes the sections.
     *
     * @param edit change for one object
     */
    private void editComponents(Consumer<UnitySceneObject> edit) {
        if (!editable || loadingTarget) return;
        for (UnitySceneObject target : targets) {
            edit.accept(target);
            if (onObjectEdited != null) onObjectEdited.accept(target);
        }
        refreshComponents();
    }

    /**
     * Shows each component section when every selected object has that component.
     */
    private void refreshComponents() {
        refreshLight();
        refreshFog();
        boolean wasLoadingState = loadingTarget;
        loadingTarget = true;
        try {
            if (lightSection != null && lightSection.visibility() == Visibility.VISIBLE) {
                showEnabled(lightSection, UnitySceneObject.Component.LIGHT);
            }
        } finally {
            loadingTarget = wasLoadingState;
        }
        if (effectSection != null) {
            boolean all = !targets.isEmpty() && targets.stream().allMatch(t -> t.materialEffectSettings() != null);
            effectSection.visibility(all ? Visibility.VISIBLE : Visibility.COLLAPSED);
            if (all) {
                boolean wasLoading = loadingTarget;
                loadingTarget = true;
                try {
                    UnityMaterialEffect active = targetObject.materialEffectSettings();
                    effectPreset.silentSelectedIndex(active.preset().ordinal());
                    effectColor.argb(0xFF000000 | active.colorRgb());
                    showNumber(effectStrength, active.strength(), targets.stream()
                            .anyMatch(t -> t.materialEffectSettings().strength() != active.strength()));
                    showNumber(effectPeriod, active.period(), targets.stream()
                            .anyMatch(t -> t.materialEffectSettings().period() != active.period()));
                    showEnabled(effectSection, UnitySceneObject.Component.MATERIAL_EFFECT);
                } finally {
                    loadingTarget = wasLoading;
                }
            }
        }
        if (meshSection != null) {
            boolean all = !targets.isEmpty() && targets.stream().allMatch(t -> t.meshShape() != null);
            meshSection.visibility(all ? Visibility.VISIBLE : Visibility.COLLAPSED);
            if (all) {
                boolean wasLoading = loadingTarget;
                loadingTarget = true;
                try {
                    meshShape.silentSelectedIndex(UnitySceneObject.MESH_SHAPES.indexOf(targetObject.meshShape()));
                    showEnabled(meshSection, UnitySceneObject.Component.MESH_RENDERER);
                } finally {
                    loadingTarget = wasLoading;
                }
            }
        }
        if (rotatorSection != null) {
            boolean all = !targets.isEmpty() && targets.stream().noneMatch(t -> Float.isNaN(t.rotatorSpeed()));
            rotatorSection.visibility(all ? Visibility.VISIBLE : Visibility.COLLAPSED);
            if (all) {
                boolean wasLoading = loadingTarget;
                loadingTarget = true;
                try {
                    float first = targetObject.rotatorSpeed();
                    showNumber(rotatorSpeed, first, targets.stream().anyMatch(t -> t.rotatorSpeed() != first));
                    showEnabled(rotatorSection, UnitySceneObject.Component.ROTATOR);
                } finally {
                    loadingTarget = wasLoading;
                }
            }
        }
        if (addComponentButton != null) addComponentButton.enabled(editable && !targets.isEmpty());
    }

    /**
     * @param shape primitive type
     * @return its name in the Mesh dropdown
     */
    private static String shapeLabel(UnityEditorHierarchyView.GameObjectType shape) {
        String name = shape.name();
        return name.charAt(0) + name.substring(1).toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * @param label property caption
     * @return a property row containing only its label
     */
    private static HBox labeledRow(String label) {
        HBox row = new HBox();
        row.spacing(4.0f);
        row.layout(style -> style.widthPercent(100.0f).height(UnityStyles.ROW_HEIGHT)
                .alignItems(Align.CENTER).flexGrow(0.0f).flexShrink(0.0f));
        row.addChild(UnityStyles.label(label));
        return row;
    }

    /**
     * @param label property caption
     * @param field numeric input filling the rest of the row
     * @return the property row
     */
    private static HBox fieldRow(String label, NumberField field) {
        HBox row = labeledRow(label);
        field.layout(style -> style.height(UnityStyles.ROW_HEIGHT).flexGrow(1.0f).flexShrink(1.0f));
        row.addChild(field);
        return row;
    }

    /**
     * @param min smallest accepted value
     * @param max largest accepted value
     * @return a Unity-styled numeric input
     */
    private static NumberField numberField(double min, double max) {
        NumberField field = new NumberField();
        UnityStyles.styleNumberField(field);
        field.range(min, max);
        field.layout(style -> style.width(36.0f).minWidth(30.0f).height(UnityStyles.ROW_HEIGHT)
                .flexGrow(1.0f).flexShrink(1.0f));
        return field;
    }

    /**
     * Applies one change to every selected light.
     *
     * @param edit derives the new settings from each light's current settings
     */
    private void editLights(java.util.function.UnaryOperator<UnityLight> edit) {
        if (!editable || loadingTarget) return;
        for (UnitySceneObject target : targets) {
            if (target.light() != null) target.setLight(edit.apply(target.light()));
        }
        refreshLight();
    }

    /**
     * Shows the light settings of the selection. A value that differs between lights shows a dash.
     */
    private void refreshLight() {
        if (lightSection == null) return;
        boolean lights = !targets.isEmpty();
        for (UnitySceneObject target : targets) lights &= target.light() != null;
        lightSection.visibility(lights ? Visibility.VISIBLE : Visibility.COLLAPSED);
        if (!lights) return;
        UnityLight active = targetObject.light();
        boolean wasLoading = loadingTarget;
        loadingTarget = true;
        try {
            lightType.silentSelectedIndex(active.kind().ordinal());
            lightColor.argb(0xFF000000 | active.colorRgb());
            showValue(lightIntensity, UnityLight::intensity);
            showValue(lightRange, UnityLight::range);
            showValue(lightSpotAngle, UnityLight::spotAngle);
            showValue(lightInnerAngle, UnityLight::innerSpotAngle);
            boolean allSpot = true, anyLocal = false, anyShadowCapable = false, anyDirectional = false;
            boolean anyShadows = false, firstShadows = false, mixedShadows = false;
            boolean hasShadowTarget = false;
            int firstResolution = 1024;
            boolean mixedResolution = false;
            for (UnitySceneObject target : targets) {
                UnityLight light = target.light();
                allSpot &= light.kind() == UnityLight.Kind.SPOT;
                anyLocal |= light.kind() != UnityLight.Kind.DIRECTIONAL;
                boolean supports = light.supportsShadows();
                anyShadowCapable |= supports;
                anyDirectional |= light.kind() == UnityLight.Kind.DIRECTIONAL;
                if (supports) {
                    if (!hasShadowTarget) {
                        hasShadowTarget = true;
                        firstShadows = light.shadows();
                        firstResolution = light.shadowResolution();
                    } else {
                        mixedShadows |= light.shadows() != firstShadows;
                        mixedResolution |= light.shadowResolution() != firstResolution;
                    }
                    anyShadows |= light.shadows();
                }
            }
            lightRangeRow.visibility(anyLocal ? Visibility.VISIBLE : Visibility.COLLAPSED);
            lightSpotRow.visibility(allSpot ? Visibility.VISIBLE : Visibility.COLLAPSED);
            lightInnerRow.visibility(allSpot ? Visibility.VISIBLE : Visibility.COLLAPSED);
            lightShadowsRow.visibility(anyShadowCapable ? Visibility.VISIBLE : Visibility.COLLAPSED);
            if (anyShadowCapable) {
                lightShadows.silentState(mixedShadows ? CheckboxState.INDETERMINATE
                        : firstShadows ? CheckboxState.CHECKED : CheckboxState.UNCHECKED);
            }
            boolean showShadowDetails = anyShadowCapable && anyShadows;
            lightShadowResRow.visibility(showShadowDetails ? Visibility.VISIBLE : Visibility.COLLAPSED);
            lightShadowStrengthRow.visibility(showShadowDetails ? Visibility.VISIBLE : Visibility.COLLAPSED);
            lightShadowBiasRow.visibility(showShadowDetails ? Visibility.VISIBLE : Visibility.COLLAPSED);
            lightShadowSizeRow.visibility(showShadowDetails && anyDirectional ? Visibility.VISIBLE : Visibility.COLLAPSED);
            if (showShadowDetails) {
                lightShadowResolution.silentSelectedIndex(mixedResolution ? -1 : resolutionIndex(firstResolution));
                showFilteredValue(lightShadowStrength, UnityLight::supportsShadows, UnityLight::shadowStrength);
                showFilteredValue(lightShadowBias, UnityLight::supportsShadows, UnityLight::shadowBias);
                if (anyDirectional) {
                    showFilteredValue(lightShadowSize, light -> light.kind() == UnityLight.Kind.DIRECTIONAL,
                            UnityLight::shadowSize);
                }
            }
        } finally {
            loadingTarget = wasLoading;
        }
    }

    /**
     * @param resolution shadow map side in texels
     * @return matching index in {@link #SHADOW_RESOLUTIONS}, or -1
     */
    private static int resolutionIndex(int resolution) {
        for (int i = 0; i < SHADOW_RESOLUTIONS.length; i++) {
            if (SHADOW_RESOLUTIONS[i] == resolution) return i;
        }
        return -1;
    }

    /**
     * Shows a light property, or a dash if the selected lights disagree.
     *
     * @param field input to update without sending edits
     * @param value reads the property from one light
     */
    private void showValue(NumberField field, java.util.function.ToDoubleFunction<UnityLight> value) {
        showFilteredValue(field, light -> true, value);
    }

    /**
     * Shows a light property across matching lights, or a dash if they disagree.
     *
     * @param field  input to update without sending edits
     * @param filter selects which selected lights use this property
     * @param value  reads the property from one light
     */
    private void showFilteredValue(NumberField field, java.util.function.Predicate<UnityLight> filter,
                                   java.util.function.ToDoubleFunction<UnityLight> value) {
        double first = Double.NaN;
        boolean hasValue = false;
        boolean mixed = false;
        for (UnitySceneObject target : targets) {
            UnityLight light = target.light();
            if (!filter.test(light)) continue;
            double v = value.applyAsDouble(light);
            if (!hasValue) {
                first = v;
                hasValue = true;
            } else {
                mixed |= v != first;
            }
        }
        if (hasValue) showNumber(field, first, mixed);
    }

    /**
     * Displays a number, or an empty field with a dash placeholder when several objects differ.
     * A mixed field holds NaN so that typing any value counts as a change.
     *
     * @param field input to update without sending edits
     * @param value shared value
     * @param mixed whether the selected objects disagree
     */
    static void showNumber(NumberField field, double value, boolean mixed) {
        field.placeholder("\u2014");
        if (!mixed) {
            field.silentValue(Math.round(value * 1e4) / 1e4);
            return;
        }
        field.silentValue(Double.NaN);
        if (!field.focused()) field.text("");
    }

    /**
     * Camera component with full settings, FOV slider, color picker,
     * warning HelpBox, and Command Buffers section.
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

        // Unity-style warning HelpBox
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
     * Audio Listener component.
     */
    private void buildAudioListenerComponent() {
        audioListenerSection = new UnityComponentSection("Audio Listener", UnityIcons.audioListenerIcon(14.0f), true, overlayLayer);

        // Audio Listener has no default parameters, status only
        Label hint = new Label("Audio Listener has no configurable parameters.");
        hint.color(UnityStyles.TEXT_MUTED);
        hint.layout(style -> style.height(UnityStyles.ROW_HEIGHT).padding(2.0f, 0.0f));
        audioListenerSection.addPropertyRow(hint);

        scrollContent.addChild(audioListenerSection);
    }

    /**
     * Add Component button at the bottom of the inspector.
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
            // Insert before the Add Component button
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
     * Displays the selected object in the header: name and an icon matching its type.
     *
     * @param name object name
     * @param type object type; {@code null} indicates an empty GameObject
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
        setTargetObjects(object == null ? List.of() : List.of(object), object);
    }

    /**
     * Shows the selection and binds edits to every selected object. With several objects the header,
     * Transform and Light show the values they share and a dash where they differ.
     *
     * @param objects every selected object
     * @param active  active object, or null for an empty Inspector
     */
    void setTargetObjects(List<UnitySceneObject> objects, UnitySceneObject active) {
        List<UnitySceneObject> next = new ArrayList<>(objects);
        if (active != null && !next.contains(active)) next.add(active);
        if (active == null) next.clear();
        boolean changed = targetObject != active;
        targetObject = active;
        targets = List.copyOf(next);
        if (changed && selectedPrefab == null) refreshPreview();
        boolean selected = active != null;
        scrollContent.visibility(selected ? Visibility.VISIBLE : Visibility.COLLAPSED);
        emptySelection.visibility(selected ? Visibility.COLLAPSED : Visibility.VISIBLE);
        if (active == null) return;

        loadingTarget = true;
        try {
            boolean sameName = true, sameType = true, allVisible = true, noneVisible = true;
            for (UnitySceneObject target : targets) {
                sameName &= target.name().equals(active.name());
                sameType &= target.type() == active.type();
                allVisible &= target.visible();
                noneVisible &= !target.visible();
            }
            setTargetObject(sameName ? active.name() : "", sameType ? active.type() : null);
            objectNameInput.placeholder(targets.size() > 1 ? "\u2014" : "");
            if (allVisible || noneVisible) objectActive.silentChecked(allVisible);
            else objectActive.silentState(CheckboxState.INDETERMINATE);
        } finally {
            loadingTarget = false;
        }
        refreshTransform(active);
        refreshComponents();
    }

    /**
     * @return every object the Inspector edits
     */
    List<UnitySceneObject> targetObjects() {
        return targets;
    }

    /**
     * Synchronizes the active checkbox with a visibility change in the hierarchy.
     *
     * @param object changed scene node
     */
    void refreshVisibility(UnitySceneObject object) {
        if (targets.contains(object)) setTargetObjects(targets, targetObject);
    }

    /**
     * Copies the selected object's current local transform into the Inspector.
     *
     * @param object scene object edited by the gizmo
     */
    void refreshTransform(UnitySceneObject object) {
        if (object == null || !targets.contains(object) || positionFields == null) return;
        showVector(positionFields, (target, out) -> target.position(out));
        showVector(rotationFields, UnitySceneObject::angles);
        showVector(scaleFields, (target, out) -> target.scale(out));
    }

    /**
     * Shows one Transform row, with a dash on each axis whose value differs between the targets.
     *
     * @param fields row inputs
     * @param read   reads the property of one object
     */
    private void showVector(UnityPropertyRows.VectorFields fields, VectorRead read) {
        MutableVec3 first = read.get(targetObject, new MutableVec3());
        boolean mixedX = false, mixedY = false, mixedZ = false;
        for (UnitySceneObject target : targets) {
            Vec3View value = read.get(target, transformValue);
            mixedX |= value.x() != first.x();
            mixedY |= value.y() != first.y();
            mixedZ |= value.z() != first.z();
        }
        if (targets.size() < 2) {
            fields.set(first);
            return;
        }
        showNumber(fields.x(), first.x(), mixedX);
        showNumber(fields.y(), first.y(), mixedY);
        showNumber(fields.z(), first.z(), mixedZ);
    }

    /**
     * Binds the three inputs to a vector property of the selected node.
     *
     * @param label   row label
     * @param initial initial value on each axis before a scene object is selected
     * @param apply   applies all components in the property's own units
     * @return editable inputs for the row
     */
    private UnityPropertyRows.VectorFields addVector(String label, float initial, VectorRead read, VectorEdit apply) {
        UnityPropertyRows.VectorFields fields = UnityPropertyRows.vectorFields(label, initial, initial, initial);
        NumberField[] axes = {fields.x(), fields.y(), fields.z()};
        for (int i = 0; i < 3; i++) {
            int axis = i;
            axes[i].onValueChanged(event -> {
                float value = (float) event.newValue();
                if (!editable || targetObject == null || !Float.isFinite(value)) return;
                // Each object keeps its own values on the other axes.
                for (UnitySceneObject target : targets) {
                    MutableVec3 current = read.get(target, new MutableVec3());
                    apply.set(target, axis == 0 ? value : current.x(), axis == 1 ? value : current.y(),
                            axis == 2 ? value : current.z());
                }
            });
        }
        transformSection.addPropertyRow(fields.row());
        return fields;
    }

    /**
     * Reads a vector property of a scene object.
     */
    @FunctionalInterface
    private interface VectorRead {
        /**
         * @param object scene node
         * @param out    destination
         * @return the destination holding the property
         */
        MutableVec3 get(UnitySceneObject object, MutableVec3 out);
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

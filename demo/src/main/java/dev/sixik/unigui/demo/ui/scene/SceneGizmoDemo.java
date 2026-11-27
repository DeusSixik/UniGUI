package dev.sixik.unigui.demo.ui.scene;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyboardState;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.FreeFlyCameraController;
import dev.sixik.unigui.api.scene.StructureNode;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.TextBlock;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.ToggleButton;
import dev.sixik.unigui.widgets.rendering.Scene3D;

/**
 * Интерактивный пример редактирования блоков и переключения между орбитальной и свободной камерами.
 */
public final class SceneGizmoDemo extends Box {
    /**
     * Скорость свободной камеры в блоках в секунду.
     */
    private static final float FLY_SPEED = 5.0f;
    /**
     * Угол поворота камеры в радианах на единицу UI при перетаскивании.
     */
    private static final double LOOK_SPEED = 0.006;

    /**
     * Вьюпорт, принимающий выбор объектов и клавиатурный ввод.
     */
    private final Scene3D viewport = new Scene3D().highlightBlocks(true);
    /**
     * Оси и кольца выбранного объекта.
     */
    private final SceneTransformGizmo gizmo = new SceneTransformGizmo(viewport, true);
    /**
     * Контроллер свободного полёта, использующий ту же позу камеры.
     */
    private final FreeFlyCameraController fly = new FreeFlyCameraController(viewport.camera());
    /**
     * Переключатель свободной камеры.
     */
    private final Checkbox flyMode = new Checkbox("Spectator Camera");
    /**
     * Короткая подсказка для активной камеры.
     */
    private final TextBlock help = new TextBlock();
    /**
     * Кнопка активного режима трансформации.
     */
    private ToggleButton activeTool;
    /**
     * Кнопка текущего жеста демо или null, когда жест завершён.
     */
    private PointerButton dragButton;
    /**
     * Идентификатор указателя, захваченного для текущего жеста.
     */
    private int dragPointer;
    /**
     * Последняя позиция указателя в координатах вьюпорта при повороте камеры.
     */
    private float lookX, lookY;

    /**
     * Собирает сцену с тремя редактируемыми блоками и панелью управления.
     *
     * @param blocks идентификаторы блоков текущей версии Minecraft.
     */
    public SceneGizmoDemo(SceneShowcase.Blocks blocks) {
        themeEnabled(false);
        layout(style -> style.widthPercent(100).flexShrink(0));
        viewport.layout(style -> style.widthPercent(100).height(340).flexShrink(0));
        viewport.fit(ImageFit.STRETCH);
        viewport.focusable(true);
        viewport.clearColor().set(0.042f, 0.063f, 0.099f, 1);
        viewport.camera().setPerspective((float) Math.toRadians(45), 0.15f, 120);
        viewport.orbit().setOrbit(new MutableVec3(0, 0.55f, 0), 10, 0.64, 0.48);
        viewport.maxZoomDistance(36);

        StructureNode floor = new StructureNode();
        for (int x = -3; x < 4; x++) {
            for (int z = -2; z < 3; z++) {
                floor.setBlock(x, -1, z, (x + z) % 2 == 0 ? blocks.floor() : blocks.roof());
            }
        }
        BlockNode furnace = block(blocks.furnace(), 0, 0);
        BlockNode log = block(blocks.pillar(), -2, 0.6f);
        BlockNode lamp = block(blocks.light(), 2, -0.6f);
        viewport.scene().addRoot(floor).addRoot(furnace).addRoot(log).addRoot(lamp);
        gizmo.select(furnace);
        viewport.onNodeClick(event -> {
            if (event.node() instanceof BlockNode block) gizmo.select(block);
        });
        viewport.painter((context, width, height) -> gizmo.paint(context));
        viewport.on(PointerPressedEvent.TYPE, this::press);
        viewport.on(PointerMovedEvent.TYPE, this::move);
        viewport.on(PointerReleasedEvent.TYPE, this::release);
        viewport.on(PointerExitedEvent.TYPE, event -> gizmo.clearHover());
        viewport.on(ScrollEvent.TYPE, event -> {
            if (event.phase() == EventPhase.TARGET && dragButton != null) event.cancel();
        });
        viewport.on(KeyPressedEvent.TYPE, event -> {
            if (event.phase() == EventPhase.TARGET && flyMode.checked() && flyKey(event.keyCode())) event.cancel();
        });

        HBox toolbar = new HBox();
        toolbar.spacing(6);
        toolbar.layout(style -> style.widthPercent(100).flexWrap(FlexWrap.WRAP).alignItems(Align.CENTER));
        toolbar.addChild(tool("Move", SceneTransformGizmo.Mode.MOVE));
        toolbar.addChild(tool("Rotate", SceneTransformGizmo.Mode.ROTATE));
        toolbar.addChild(tool("Scale", SceneTransformGizmo.Mode.SCALE));
        flyMode.layout(style -> style.height(22).flexShrink(0));
        flyMode.onCheckedChanged(event -> setFlyMode(event.newValue()));
        toolbar.addChild(flyMode);
        setFlyMode(false);

        VBox column = new VBox();
        column.spacing(7);
        column.layout(style -> style.widthPercent(100));
        column.addChild(toolbar);
        column.addChild(viewport);
        column.addChild(help);
        addChild(column);
    }

    /**
     * Создаёт блок с точкой опоры в центре.
     *
     * @param state идентификатор блока.
     * @param x     мировая позиция центра по X, в блоках.
     * @param z     мировая позиция центра по Z, в блоках.
     * @return блок, стоящий на полу при Y = 0.
     */
    private static BlockNode block(String state, float x, float z) {
        BlockNode node = new BlockNode(state);
        node.setOrigin(0.5f, 0.5f, 0.5f).setPosition(x, 0.5f, z);
        return node;
    }

    /**
     * Создаёт переключатель, оставляющий активным ровно один инструмент.
     *
     * @param label текст кнопки.
     * @param mode  режим гизмо, который включает кнопка.
     * @return переключатель инструмента.
     */
    private ToggleButton tool(String label, SceneTransformGizmo.Mode mode) {
        ToggleButton button = new ToggleButton(label);
        button.layout(style -> style.size(56, 22).flexShrink(0));
        if (activeTool == null) {
            activeTool = button;
            button.silentChecked(true);
        }
        button.onCheckedChanged(event -> {
            if (activeTool == button) {
                button.silentChecked(true);
                focusViewport();
                return;
            }
            finishDrag();
            activeTool.silentChecked(false);
            activeTool = button;
            gizmo.mode(mode);
            gizmo.space(mode == SceneTransformGizmo.Mode.SCALE
                    ? SceneTransformGizmo.Space.LOCAL : SceneTransformGizmo.Space.GLOBAL);
            focusViewport();
        });
        return button;
    }

    /**
     * Переключает контроллеры камеры. При выходе восстанавливает позу сохранённой орбиты.
     *
     * @param enabled true при включённом свободном полёте.
     */
    private void setFlyMode(boolean enabled) {
        finishDrag();
        viewport.cameraControls(!enabled);
        if (enabled) {
            focusViewport();
            help.text("WASD to fly · Space / Shift to rise or descend · Right-drag to look");
        } else {
            // Free flight changes the camera pose directly, so the orbit controller retains its parameters.
            viewport.orbit().rotate(0, 0);
            help.text("Drag away from the gizmo to orbit · Scroll to zoom");
        }
    }

    /**
     * Переводит фокус на сцену после действий с панелью управления.
     */
    private void focusViewport() {
        if (uiContext() != null) uiContext().focusManager().requestFocus(viewport);
    }

    /**
     * Начинает преобразование или поворот взгляда и захватывает указатель.
     *
     * @param event нажатие в локальных координатах вьюпорта.
     */
    private void press(PointerPressedEvent event) {
        if (!onViewport(event) || event.isCancelled() || uiContext() == null) return;
        if (dragButton != null) {
            event.cancel();
            return;
        }
        if (uiContext().capturedPointer(event.pointerId()) != null) return;
        PointerButton button = event.button();
        if (button == PointerButton.SECONDARY && flyMode.checked()) {
            lookX = event.localX();
            lookY = event.localY();
        } else if (button != PointerButton.PRIMARY || !gizmo.begin(event.localX(), event.localY())) {
            return;
        }
        dragButton = button;
        dragPointer = event.pointerId();
        uiContext().capturePointer(dragPointer, viewport);
        event.cancel();
    }

    /**
     * Обновляет наведение, преобразование объекта или направление взгляда.
     *
     * @param event новая позиция в локальных координатах вьюпорта.
     */
    private void move(PointerMovedEvent event) {
        if (!onViewport(event)) return;
        UIContext context = uiContext();
        if (context == null) return;
        if (dragButton == null) {
            if (context.capturedPointer(event.pointerId()) == null) gizmo.hover(event.localX(), event.localY());
            return;
        }
        if (event.pointerId() != dragPointer) return;
        if (dragButton == PointerButton.PRIMARY) {
            gizmo.drag(event.localX(), event.localY());
        } else {
            fly.look((lookX - event.localX()) * LOOK_SPEED, (lookY - event.localY()) * LOOK_SPEED);
            lookX = event.localX();
            lookY = event.localY();
        }
        event.cancel();
    }

    /**
     * Завершает жест на отпускании той кнопки, с которой он начался.
     *
     * @param event отпускание указателя.
     */
    private void release(PointerReleasedEvent event) {
        if (!onViewport(event) || dragButton != event.button() || dragPointer != event.pointerId()) return;
        if (dragButton == PointerButton.PRIMARY) gizmo.drag(event.localX(), event.localY());
        finishDrag();
        event.cancel();
    }

    /**
     * Освобождает указатель и снимает активную ручку.
     */
    private void finishDrag() {
        if (dragButton == null) return;
        UIContext context = uiContext();
        if (context != null) context.releasePointer(dragPointer, viewport);
        dragButton = null;
        gizmo.end();
    }

    /**
     * Проверяет, что событие адресовано самому вьюпорту.
     *
     * @param event маршрутизированное событие указателя.
     * @return true в фазе TARGET на вьюпорте.
     */
    private boolean onViewport(PointerEvent event) {
        return event.phase() == EventPhase.TARGET && event.target() == viewport;
    }

    /**
     * Перемещает камеру по удерживаемым клавишам. Нормирует вектор ввода, чтобы диагональ
     * имела ту же скорость: {@code displacement = speed * dt * input / length(input)}.
     *
     * @param frame параметры кадра, включая dt в секундах.
     */
    @Override
    public void tick(FrameContext frame) {
        if (!visible()) return;
        UIContext context = uiContext();
        boolean focused = context != null && context.focusManager().isFocused(viewport);
        if (dragButton != null && (!focused || context.capturedPointer(dragPointer) != viewport)) finishDrag();
        if (flyMode.checked() && focused && dragButton != PointerButton.PRIMARY && frame != null) {
            KeyboardState keys = context.keyboard();
            float right = axis(keys, KeyCodes.D, KeyCodes.A);
            float forward = axis(keys, KeyCodes.W, KeyCodes.S);
            float up = (keys.isDown(KeyCodes.SPACE) ? 1 : 0)
                    - (keys.isDown(KeyCodes.LEFT_SHIFT) || keys.isDown(KeyCodes.RIGHT_SHIFT) ? 1 : 0);
            float length = (float) Math.sqrt(right * right + forward * forward + up * up);
            float seconds = frame.deltaSeconds();
            if (length > 0 && seconds > 0 && Float.isFinite(seconds)) {
                float distance = FLY_SPEED * Math.min(0.1f, seconds) / length;
                fly.move(right * distance, up * distance, forward * distance);
            }
        }
        super.tick(frame);
    }

    /**
     * Вычитает состояния противоположных клавиш.
     *
     * @param keys     состояние клавиатуры.
     * @param positive код клавиши положительного направления.
     * @param negative код клавиши отрицательного направления.
     * @return направление от −1 до 1.
     */
    private static int axis(KeyboardState keys, int positive, int negative) {
        return (keys.isDown(positive) ? 1 : 0) - (keys.isDown(negative) ? 1 : 0);
    }

    /**
     * Освобождает захваченный указатель при удалении демо со страницы.
     */
    @Override
    public void dispose() {
        finishDrag();
        super.dispose();
    }

    /**
     * Проверяет, управляет ли клавиша полётом.
     *
     * @param key нормализованный код клавиши.
     * @return true для клавиш перемещения свободной камеры.
     */
    private static boolean flyKey(int key) {
        return switch (key) {
            case KeyCodes.W, KeyCodes.A, KeyCodes.S, KeyCodes.D, KeyCodes.SPACE,
                 KeyCodes.LEFT_SHIFT, KeyCodes.RIGHT_SHIFT -> true;
            default -> false;
        };
    }
}

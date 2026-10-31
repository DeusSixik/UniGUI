package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.widget.CheckboxState;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.containers.*;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.dnd.DragController;
import dev.sixik.unigui.widgets.dnd.DragPayload;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Секция компонента Unity Inspector с заголовком, стрелкой foldout,
 * чекбоксом активности, иконкой, кнопками контекстного меню и телом свойств.
 */
public class UnityComponentSection extends LinearBox {
    private final String title;
    private final Box headerSurface = new Box();
    private final HBox headerRow = new HBox();
    private final CanvasWidget arrowCanvas = new CanvasWidget();
    private final CanvasWidget iconCanvas;
    private final Checkbox activeCheckbox;
    private final Label titleLabel;
    private final HBox tools = new HBox();
    private final Button helpButton;
    private final Button presetButton;
    private final Button menuButton;
    private final VBox contentHost = new VBox();
    private final Box bottomLine;

    private OverlayLayer overlayLayer;
    private ContextMenu contextMenu;
    private Tooltip helpTooltip;
    private boolean wasMenuOpenOnPress = false;
    private boolean draggable = true;
    private DragController dragController;

    private boolean expanded = true;
    private Consumer<Boolean> onExpandChanged;
    private Consumer<Boolean> onActiveChanged;
    private Runnable onHelpClick;
    private Runnable onPresetClick;
    private Runnable onMenuClick;
    private Runnable onResetClick;
    private Runnable onMoveDownClick;
    private Runnable onCopyClick;
    private Runnable onFindReferencesClick;
    private Runnable onPropertiesClick;
    private Runnable onRemoveClick;

    public UnityComponentSection(String title, CanvasWidget icon, boolean hasEnableCheckbox) {
        this(title, icon, hasEnableCheckbox, null);
    }

    public UnityComponentSection(String title, CanvasWidget icon, boolean hasEnableCheckbox, OverlayLayer overlayLayer) {
        super(Orientation.VERTICAL);
        spacing(0.0f);
        this.title = title != null ? title : "Component";
        this.iconCanvas = icon;
        this.overlayLayer = overlayLayer;

        // Поверхность шапки
        headerSurface.themeEnabled(false);
        headerSurface.backgroundVisible(true);
        headerSurface.background().set(UnityStyles.BG_HEADER);
        headerSurface.borderVisible(true);
        headerSurface.borderColor().set(UnityStyles.BORDER_SUBTLE);
        headerSurface.borderWidth(0.5f);
        headerSurface.layout(style -> style.widthPercent(100.0f).height(UnityStyles.COMPONENT_HEADER_HEIGHT)
                .padding(8.0f, 0.0f, 6.0f, 0.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        headerRow.spacing(5.0f);
        headerRow.layout(style -> style.sizePercent(100.0f, 100.0f)
                .alignItems(Align.CENTER));

        // Стрелка сворачивания / разворачивания (аккуратная, вытянутая в высоту)
        arrowCanvas.layout(style -> style.size(10.0f, UnityStyles.COMPONENT_HEADER_HEIGHT).flexGrow(0.0f).flexShrink(0.0f));
        arrowCanvas.onDraw(context -> {
            float x = arrowCanvas.layoutBounds().x();
            float y = arrowCanvas.layoutBounds().y();
            float height = arrowCanvas.layoutBounds().height();
            float cx = x + 5.0f;
            float cy = y + height * 0.5f;

            context.pathClear();
            if (expanded) {
                // Стрелка вниз ▼
                context.pathLineTo(cx - 3.5f, cy - 2.0f);
                context.pathLineTo(cx + 3.5f, cy - 2.0f);
                context.pathLineTo(cx, cy + 2.5f);
            } else {
                // Стрелка вправо ▶ (увеличенная высота, суженная ширина)
                context.pathLineTo(cx - 1.5f, cy - 4.0f);
                context.pathLineTo(cx + 2.5f, cy);
                context.pathLineTo(cx - 1.5f, cy + 4.0f);
            }
            context.pathFillConvex(UnityStyles.TEXT_PRIMARY);
        });
        arrowCanvas.on(PointerPressedEvent.TYPE, event -> {
            if (event.button() == PointerButton.PRIMARY) {
                toggle();
                event.cancel();
            }
        });

        headerRow.addChild(arrowCanvas);

        // Опциональный чекбокс включения компонента (как у Camera, Audio Listener)
        // В Unity идёт сразу после стрелки foldout и перед иконкой
        if (hasEnableCheckbox) {
            activeCheckbox = new Checkbox();
            UnityStyles.styleCheckbox(activeCheckbox);
            activeCheckbox.checked(true);
            activeCheckbox.layout(style -> style.size(14.0f, UnityStyles.COMPONENT_HEADER_HEIGHT)
                    .alignSelf(Align.CENTER).flexGrow(0.0f).flexShrink(0.0f));
            activeCheckbox.onStateChanged(event -> {
                boolean active = event.newState() == CheckboxState.CHECKED;
                contentHost.opacity(active ? 1.0f : 0.5f);
                contentHost.enabled(active);
                if (onActiveChanged != null) {
                    onActiveChanged.accept(active);
                }
            });
            headerRow.addChild(activeCheckbox);
        } else {
            activeCheckbox = null;
        }

        // Иконка компонента
        if (iconCanvas != null) {
            headerRow.addChild(iconCanvas);
        }

        // Заголовок компонента
        titleLabel = new Label(this.title);
        titleLabel.color(UnityStyles.TEXT_BOLD);
        titleLabel.layout(style -> style.height(UnityStyles.COMPONENT_HEADER_HEIGHT).flexGrow(0.0f).flexShrink(0.0f));
        headerRow.addChild(titleLabel);

        // Распорка до правых кнопок
        Box spacer = new Box();
        spacer.themeEnabled(false);
        spacer.layout(style -> style.flexGrow(1.0f).height(1.0f));
        headerRow.addChild(spacer);

        // Правые служебные кнопки: [?] помощь, ползунки пресетов, 3 точки меню
        tools.spacing(3.0f);
        tools.layout(style -> style.height(UnityStyles.COMPONENT_HEADER_HEIGHT).alignItems(Align.CENTER));

        helpButton = createHeaderToolButton((draw, btn) -> {
            RectView bounds = btn.layoutBounds();
            float cx = bounds.x() + bounds.width() * 0.5f;
            float cy = bounds.y() + bounds.height() * 0.5f;
            float size = 12.0f;
            float r = size * 0.42f;
            ColorView col = btn.hovered() ? UnityStyles.TEXT_PRIMARY : UnityStyles.TEXT_MUTED;
            UnityIcons.drawHelpQuestionMark(draw, cx, cy, r, col);
        }, () -> {
            if (onHelpClick != null) onHelpClick.run();
        });
        helpButton.on(PointerEnteredEvent.TYPE, event -> ensureOverlays());

        presetButton = createHeaderToolButton((draw, btn) -> {
            RectView bounds = btn.layoutBounds();
            float cx = bounds.x() + bounds.width() * 0.5f;
            float cy = bounds.y() + bounds.height() * 0.5f;
            float size = 12.0f;
            float lw = size * 0.75f;
            ColorView col = btn.hovered() ? UnityStyles.TEXT_PRIMARY : UnityStyles.TEXT_MUTED;
            draw.addLine(cx - lw * 0.5f, cy - 2.5f, cx + lw * 0.5f, cy - 2.5f, col, 1.2f);
            draw.circleFilled(cx - 1.5f, cy - 4.0f, 3.0f, 3.0f, col);
            draw.addLine(cx - lw * 0.5f, cy + 2.5f, cx + lw * 0.5f, cy + 2.5f, col, 1.2f);
            draw.circleFilled(cx + 1.0f, cy + 1.0f, 3.0f, 3.0f, col);
        }, () -> {
            if (onPresetClick != null) onPresetClick.run();
        });

        menuButton = createHeaderToolButton((draw, btn) -> {
            RectView bounds = btn.layoutBounds();
            float cx = bounds.x() + bounds.width() * 0.5f;
            float cy = bounds.y() + bounds.height() * 0.5f;
            float r = 1.2f;
            ColorView col = btn.hovered() ? UnityStyles.TEXT_PRIMARY : UnityStyles.TEXT_MUTED;
            draw.circleFilled(cx - r, cy - 4.0f - r, r * 2.0f, r * 2.0f, col);
            draw.circleFilled(cx - r, cy - r, r * 2.0f, r * 2.0f, col);
            draw.circleFilled(cx - r, cy + 4.0f - r, r * 2.0f, r * 2.0f, col);
        }, null);

        menuButton.onClick(event -> {
            ensureOverlays();
            if (contextMenu != null) {
                if (!wasMenuOpenOnPress) {
                    RectView b = menuButton.layoutBounds();
                    float menuWidth = 175.0f;
                    float menuX = b.x() + b.width() - menuWidth;
                    float menuY = b.y() + b.height() + 2.0f;
                    contextMenu.openAt(menuX, menuY);
                }
                wasMenuOpenOnPress = false;
            }
            if (onMenuClick != null) onMenuClick.run();
        });

        menuButton.on(PointerPressedEvent.TYPE, event -> {
            if (event.phase() == EventPhase.BUBBLE && event.button() == PointerButton.PRIMARY) {
                wasMenuOpenOnPress = contextMenu != null && contextMenu.opened();
            }
        });

        tools.addChild(helpButton);
        tools.addChild(presetButton);
        tools.addChild(menuButton);
        headerRow.addChild(tools);

        headerSurface.addChild(headerRow);

        // 1. Drag-and-drop перетаскивание секции за заголовок:
        // Регистрируем ПЕРВЫМ, чтобы drag-жест обрабатывал события мыши раньше клика по заголовку
        // и корректно гасил PointerReleasedEvent при завершении перетаскивания.
        dragController = DragController.attach(headerSurface)
                .dragged(this)
                .payload(() -> DragPayload.of(this.title, "unity.component", this.title, this))
                .lockGhostAxis(Orientation.VERTICAL)
                .threshold(4.0f)
                .filter(event -> {
                    if (!draggable) return false;
                    Widget target = event.target();
                    if (target == activeCheckbox || isDescendantOf(target, activeCheckbox)) return false;
                    if (target == tools || isDescendantOf(target, tools)) return false;
                    if (target == arrowCanvas || isDescendantOf(target, arrowCanvas)) return false;
                    return true;
                });

        // 2. Клик по свободной области заголовка переключает развёрнутость на отпускании (если не было драга)
        headerSurface.on(PointerReleasedEvent.TYPE, event -> {
            if (event.phase() == EventPhase.CAPTURE) return;
            if (event.button() != PointerButton.PRIMARY) return;
            if (event.isCancelled()) return;
            if (dragController != null && dragController.dragging()) return;

            Widget target = event.target();
            if (target == activeCheckbox || isDescendantOf(target, activeCheckbox)) return;
            if (target == tools || isDescendantOf(target, tools)) return;
            if (target == arrowCanvas || isDescendantOf(target, arrowCanvas)) return;

            toggle();
            event.cancel();
        });

        // Контейнер свойств
        contentHost.spacing(3.0f);
        contentHost.layout(style -> style.widthPercent(100.0f)
                .padding(8.0f, 4.0f, 8.0f, 4.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        bottomLine = UnityStyles.separatorLine();

        addChild(headerSurface);
        addChild(contentHost);
        addChild(bottomLine);

        if (this.overlayLayer != null) {
            setupTooltip();
            setupContextMenu();
        }
    }

    private static boolean isDescendantOf(Widget child, Widget ancestor) {
        if (child == null || ancestor == null) return false;
        Widget current = child;
        while (current != null) {
            if (current == ancestor) return true;
            current = current.parent();
        }
        return false;
    }

    private static Button createHeaderToolButton(BiConsumer<DrawScope, Button> iconRenderer, Runnable onClick) {
        Button btn = new Button();
        btn.themeEnabled(false);
        btn.boxVisualEnabled(false);
        btn.focusRingVisible(false);
        btn.focusable(false);
        btn.layout(style -> style.size(16.0f, 16.0f).alignSelf(Align.CENTER).flexGrow(0.0f).flexShrink(0.0f));

        btn.renderer(WidgetRender.of(Button.class, (draw, b) -> {
            RectView bounds = b.layoutBounds();
            float x = bounds.x();
            float y = bounds.y();
            float w = bounds.width();
            float h = bounds.height();
            if (w <= 0.0f || h <= 0.0f) return;

            if (b.pressed()) {
                draw.roundedRectFilled(x, y, w, h, 2.0f, UnityStyles.BG_WORKSPACE);
            } else if (b.hovered()) {
                draw.roundedRectFilled(x, y, w, h, 2.0f, UnityStyles.BG_HEADER_HOVER);
            }

            iconRenderer.accept(draw, b);
        }));

        if (onClick != null) {
            btn.onClick(e -> onClick.run());
        }

        return btn;
    }

    public void addPropertyRow(Widget row) {
        contentHost.addChild(row);
    }

    public VBox contentHost() {
        return contentHost;
    }

    public boolean expanded() {
        return expanded;
    }

    public UnityComponentSection expanded(boolean expanded) {
        if (this.expanded == expanded) return this;
        this.expanded = expanded;
        contentHost.visibility(expanded ? Visibility.VISIBLE : Visibility.COLLAPSED);
        arrowCanvas.invalidate(InvalidationFlags.VISUAL);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        if (onExpandChanged != null) {
            onExpandChanged.accept(expanded);
        }
        return this;
    }

    public UnityComponentSection toggle() {
        return expanded(!expanded);
    }

    public UnityComponentSection onExpandChanged(Consumer<Boolean> listener) {
        this.onExpandChanged = listener;
        return this;
    }

    public Checkbox activeCheckbox() {
        return activeCheckbox;
    }

    public Button helpButton() {
        return helpButton;
    }

    public Button presetButton() {
        return presetButton;
    }

    public Button menuButton() {
        return menuButton;
    }

    public UnityComponentSection onHelp(Runnable action) {
        this.onHelpClick = action;
        return this;
    }

    public UnityComponentSection onPreset(Runnable action) {
        this.onPresetClick = action;
        return this;
    }

    public UnityComponentSection onMenu(Runnable action) {
        this.onMenuClick = action;
        return this;
    }

    public UnityComponentSection onActiveChanged(Consumer<Boolean> listener) {
        this.onActiveChanged = listener;
        return this;
    }

    public String title() {
        return title;
    }

    public UnityComponentSection overlayLayer(OverlayLayer overlayLayer) {
        if (this.overlayLayer == overlayLayer) return this;
        this.overlayLayer = overlayLayer;
        if (overlayLayer != null) {
            setupTooltip();
            setupContextMenu();
        }
        return this;
    }

    private void ensureOverlays() {
        if (overlayLayer == null) {
            OverlayLayer found = findOverlayLayer(this);
            if (found != null) {
                overlayLayer(found);
            }
        } else {
            setupTooltip();
            setupContextMenu();
        }
    }

    private static OverlayLayer findOverlayLayer(Widget start) {
        Widget current = start;
        while (current != null) {
            if (current instanceof OverlayLayer layer) return layer;
            current = current.parent();
        }
        return null;
    }

    private void setupTooltip() {
        if (overlayLayer == null || helpButton == null || helpTooltip != null) return;
        helpTooltip = new Tooltip(helpButton, "Open Reference for " + title);
        UnityStyles.styleTooltip(helpTooltip);
        overlayLayer.addOverlay(helpTooltip);
    }

    private void setupContextMenu() {
        if (overlayLayer == null || contextMenu != null) return;
        contextMenu = new ContextMenu();
        UnityStyles.styleContextMenu(contextMenu);

        float menuWidth = 175.0f;
        UnityStyles.addContextMenuItem(contextMenu, "Reset", true, menuWidth, () -> {
            if (onResetClick != null) onResetClick.run();
        });
        UnityStyles.addContextMenuSeparator(contextMenu, menuWidth);
        UnityStyles.addContextMenuItem(contextMenu, "Move to Front", false, menuWidth, null);
        UnityStyles.addContextMenuItem(contextMenu, "Move to Back", false, menuWidth, null);
        UnityStyles.addContextMenuItem(contextMenu, "Move Up", false, menuWidth, null);
        UnityStyles.addContextMenuItem(contextMenu, "Move Down", true, menuWidth, () -> {
            if (onMoveDownClick != null) {
                onMoveDownClick.run();
            } else {
                moveDown();
            }
        });
        UnityStyles.addContextMenuItem(contextMenu, "Copy Component", true, menuWidth, () -> {
            if (onCopyClick != null) onCopyClick.run();
        });
        UnityStyles.addContextMenuItem(contextMenu, "Paste Component As New", false, menuWidth, null);
        UnityStyles.addContextMenuItem(contextMenu, "Paste Component Values", false, menuWidth, null);
        UnityStyles.addContextMenuSeparator(contextMenu, menuWidth);
        UnityStyles.addContextMenuItem(contextMenu, "Find References In Scene", true, menuWidth, () -> {
            if (onFindReferencesClick != null) onFindReferencesClick.run();
        });
        UnityStyles.addContextMenuSeparator(contextMenu, menuWidth);
        UnityStyles.addContextMenuItem(contextMenu, "Properties...", true, menuWidth, () -> {
            if (onPropertiesClick != null) onPropertiesClick.run();
        });
        UnityStyles.addContextMenuItem(contextMenu, "Remove Component", true, menuWidth, () -> {
            if (onRemoveClick != null) {
                onRemoveClick.run();
            } else {
                removeSelf();
            }
        });

        overlayLayer.addOverlay(contextMenu);
    }

    public void moveDown() {
        Widget p = parent();
        if (p instanceof PanelWidget panel) {
            int idx = panel.children().indexOf(this);
            if (idx >= 0 && idx < panel.children().size() - 2) {
                panel.removeChild(this);
                panel.insertChild(idx + 1, this);
                panel.applyQueuedMutations();
                panel.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
            }
        }
    }

    public void removeSelf() {
        if (contextMenu != null && overlayLayer != null) {
            contextMenu.close();
            overlayLayer.removeChild(contextMenu);
        }
        if (helpTooltip != null && overlayLayer != null) {
            overlayLayer.removeChild(helpTooltip);
        }
        Widget p = parent();
        if (p instanceof PanelWidget panel) {
            panel.removeChild(this);
            panel.applyQueuedMutations();
            panel.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        } else {
            visibility(Visibility.COLLAPSED);
        }
    }

    public ContextMenu contextMenu() {
        return contextMenu;
    }

    public Tooltip helpTooltip() {
        return helpTooltip;
    }

    public UnityComponentSection onReset(Runnable action) {
        this.onResetClick = action;
        return this;
    }

    public UnityComponentSection onRemove(Runnable action) {
        this.onRemoveClick = action;
        return this;
    }

    public UnityComponentSection onCopy(Runnable action) {
        this.onCopyClick = action;
        return this;
    }

    public UnityComponentSection onMoveDown(Runnable action) {
        this.onMoveDownClick = action;
        return this;
    }

    public UnityComponentSection onFindReferences(Runnable action) {
        this.onFindReferencesClick = action;
        return this;
    }

    public UnityComponentSection onProperties(Runnable action) {
        this.onPropertiesClick = action;
        return this;
    }

    public boolean isDraggable() {
        return draggable;
    }

    public UnityComponentSection draggable(boolean draggable) {
        this.draggable = draggable;
        if (dragController != null) {
            dragController.enabled(draggable);
        }
        return this;
    }

    public DragController dragController() {
        return dragController;
    }
}

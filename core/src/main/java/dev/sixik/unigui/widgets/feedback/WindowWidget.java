package dev.sixik.unigui.widgets.feedback;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.ModalClosedEvent;
import dev.sixik.unigui.api.event.ModalOpenedEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.WindowActivatedEvent;
import dev.sixik.unigui.api.event.WindowClosedEvent;
import dev.sixik.unigui.api.event.WindowDeactivatedEvent;
import dev.sixik.unigui.api.event.WindowMoveEndedEvent;
import dev.sixik.unigui.api.event.WindowMovedEvent;
import dev.sixik.unigui.api.event.WindowMoveStartedEvent;
import dev.sixik.unigui.api.event.WindowOpenedEvent;
import dev.sixik.unigui.api.event.WindowResizeEndedEvent;
import dev.sixik.unigui.api.event.WindowResizedEvent;
import dev.sixik.unigui.api.event.WindowResizeStartedEvent;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.impl.layout.AbsoluteLayoutEngine;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;

import java.util.Objects;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.WidgetStyles;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.widgets.docking.DockChrome;
import dev.sixik.unigui.widgets.docking.DockColor;
import dev.sixik.unigui.widgets.docking.DockPane;
import dev.sixik.unigui.widgets.docking.DockPolicy;
import dev.sixik.unigui.widgets.docking.DockTheme;
import dev.sixik.unigui.widgets.docking.ResizeGripStyle;
import dev.sixik.unigui.widgets.docking.DockPaneKind;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.data.VirtualTableView;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.ColorPicker;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.DatePicker;
import dev.sixik.unigui.widgets.interaction.ScrollBar;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.widgets.navigation.TreeView;

/**
 * Retained overlay window/dialog shell hosted by {@link OverlayLayer}.
 */
@AiDoc(summary = "Desktop-style floating window widget supporting title bar, dragging, resizing, collapse, modal dialogs, and docking integrations.")
@WikiPage(
        title = "WindowWidget",
        slug = "window-widget",
        category = "<key:feedback.category>",
        summary = "<key:window.widget.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:window.widget.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:window.widget.example.basic.title>",
        code = "WindowWidget window = new WindowWidget(\"Settings\", settingsForm);\nwindow.position(100, 100).open();"
)
@WikiLink(
        label = "WindowManager",
        target = "page:window-manager",
        description = "<key:window.manager.summary>"
)
@WikiLink(
        label = "OverlayLayer",
        target = "page:overlay-layer",
        description = "<key:overlay.layer.summary>"
)
@WikiLink(
        label = "DockingRoot",
        target = "page:docking-root",
        description = "<key:docking.root.summary>"
)
@XmlWidgetName("WindowWidget")
public class WindowWidget extends Box implements OverlayHostAware {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.WINDOW_WIDGET;

    private static final float DEFAULT_WIDTH = 220.0f;
    private static final float DEFAULT_HEIGHT = 120.0f;
    private static final float DEFAULT_HEADER_HEIGHT = 22.0f;
    private static final float DEFAULT_MIN_WIDTH = 120.0f;
    private static final float DEFAULT_MIN_HEIGHT = 64.0f;
    private static final float RESIZE_HANDLE_SIZE = 6.0f;
    private static final float CAPTION_BUTTON_SIZE = 16.0f;

    private final Button closeButton = new Button("");
    private final Button collapseButton = new Button("");
    private final MutableColor headerColor = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);
    private final MutableColor headerSeparatorColor = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);
    private final MutableColor titleColor = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);
    private DockTheme theme;
    private UiIcon icon = UiIcon.NONE;
    private String title = "";
    private RichText richTitle = RichText.plain("");
    private Widget content;
    private WindowManager windowManager;
    private boolean open;
    private boolean active;
    private boolean modal;
    private boolean fixedModal;
    private boolean dockRedockLocked;
    private DockPolicy dockPolicy = DockPolicy.FULL;
    private String dockPaneId = "";
    private DockPaneKind dockKind = DockPaneKind.TOOL;
    private boolean dockHost;
    private boolean autoDockHost;
    private DockPane dockSourcePane;
    private ResizeGripStyle resizeGrip;
    private boolean resizeGripVisible = true;
    private boolean gripHovered;
    private boolean compactDrag;
    private float compactFullWidth;
    private float compactFullHeight;
    private float compactWidth;
    private DockingRoot dockingRoot;
    private int dockPaneSeq;
    private boolean draggable = true;
    private boolean dragging;
    private boolean resizable = true;
    private boolean resizing;
    private ResizeHandle resizeHandle = ResizeHandle.NONE;
    private boolean closeButtonVisible = true;
    private boolean collapseButtonVisible;
    private boolean collapsed;
    private Visibility contentVisibilityBeforeCollapse = Visibility.VISIBLE;
    private boolean closeOnOutsideClick;
    private boolean constrainToHost = true;
    private float x = 32.0f;
    private float y = 28.0f;
    private float headerHeight = DEFAULT_HEADER_HEIGHT;
    private float minWindowWidth = DEFAULT_MIN_WIDTH;
    private float minWindowHeight = DEFAULT_MIN_HEIGHT;
    private EdgeInsets padding = EdgeInsets.all(8.0f);
    private float dragOffsetX;
    private float dragOffsetY;
    private float lastDragRootX;
    private float lastDragRootY;
    private int dragPointerId = -1;
    private int resizePointerId = -1;
    private float resizeStartRootX;
    private float resizeStartRootY;
    private float resizeStartX;
    private float resizeStartY;
    private float resizeStartWidth;
    private float resizeStartHeight;
    private final MutableRect hostBounds = new MutableRect();

        @AiDoc(summary = "Constructs an empty WindowWidget with default title and dimensions.")
    @WikiMethod(summary = "<key:window.widget.method.ctorDefault>")
    public WindowWidget() {
        applyComponentStyleDefaults(WidgetStyles.Window.DEFAULT);
        focusable(true);
        closeButton.textPaddingX(0.0f);
        closeButton.layout(style -> style.size(18.0f, 18.0f).flexGrow(0).flexShrink(0.0f));
        closeButton.onClick(event -> close());
        closeButton.renderer((draw, widget) -> {
            RectView b = widget.layoutBounds();
            float size = Math.min(CAPTION_BUTTON_SIZE, Math.min(b.width(), b.height()));
            DockChrome.paintCloseButton(draw, theme(), b.x() + (b.width() - size) * 0.5f,
                    b.y() + (b.height() - size) * 0.5f, size, widget.hovered(), closeButton.pressed());
        });
        collapseButton.textPaddingX(0.0f);
        collapseButton.layout(style -> style.size(18.0f, 18.0f).flexGrow(0).flexShrink(0.0f));
        collapseButton.visible(false);
        collapseButton.onClick(event -> toggleCollapsed());
        collapseButton.renderer((draw, widget) -> {
            RectView b = widget.layoutBounds();
            float size = Math.min(CAPTION_BUTTON_SIZE, Math.min(b.width(), b.height()));
            DockChrome.paintCollapseButton(draw, theme(), b.x() + (b.width() - size) * 0.5f,
                    b.y() + (b.height() - size) * 0.5f, size, widget.hovered(), collapsed);
        });
        addChild(collapseButton);
        addChild(closeButton);
        headerColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        headerSeparatorColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        titleColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        layout(style -> style
                .position(PositionType.ABSOLUTE)
                .left(x)
                .top(y)
                .maxWidthPercent(100.0f)
                .maxHeightPercent(100.0f)
                .overflow(Overflow.HIDDEN));
        visible(false);
    }

    /**
     * Возвращает стандартный стиль поверхности окна, уступающий стилям темы и экземпляра.
     *
     * @return стандартные значения оформления окна
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.Window.DEFAULT;
    }

        @AiDoc(summary = "Constructs a WindowWidget with a plain-text title and hosted content widget.")
    @WikiMethod(summary = "<key:window.widget.method.ctorTitleContent>")
    public WindowWidget(
            @WikiParameter(description = "<key:window.widget.param.title>")
            String title,
            @WikiParameter(description = "<key:window.widget.param.content>")
            Widget content
    ) {
        this();
        title(title);
        content(content);
    }

        @AiDoc(summary = "Constructs a WindowWidget with a RichText formatted title and hosted content.")
    @WikiMethod(summary = "<key:window.widget.method.ctorRichTitleContent>")
    public WindowWidget(
            @WikiParameter(description = "<key:window.widget.param.richTitle>")
            RichText title,
            @WikiParameter(description = "<key:window.widget.param.content>")
            Widget content
    ) {
        this();
        richTitle(title);
        content(content);
    }

        @AiDoc(summary = "Returns the window title string.")
    @WikiMethod(summary = "<key:window.widget.method.titleGet>")
    @WikiReturn("<key:window.widget.return.title>")
    public String title() {
        return title;
    }

    @XmlAttribute(value = "title", category = "Content", defaultValue = "", description = "Window title text.")
        @AiDoc(summary = "Sets the window plain-text title.")
    @WikiMethod(summary = "<key:window.widget.method.titleSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget title(
            @WikiParameter(description = "<key:window.widget.param.title>")
            String title
    ) {
        String normalized = title == null ? "" : title;
        if (Objects.equals(this.title, normalized)) return this;
        this.title = normalized;
        this.richTitle = RichText.resolve(normalized);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the RichText formatted title component.")
    @WikiMethod(summary = "<key:window.widget.method.richTitleGet>")
    @WikiReturn("<key:window.widget.return.richTitle>")
    public RichText richTitle() {
        return richTitle;
    }

        @AiDoc(summary = "Sets the RichText formatted title component.")
    @WikiMethod(summary = "<key:window.widget.method.richTitleSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget richTitle(
            @WikiParameter(description = "<key:window.widget.param.richTitle>")
            RichText title
    ) {
        RichText normalized = title == null ? RichText.plain("") : title;
        if (Objects.equals(this.richTitle, normalized)) return this;
        this.richTitle = normalized;
        this.title = normalized.plainText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the hosted content widget.")
    @WikiMethod(summary = "<key:window.widget.method.contentGet>")
    @WikiReturn("<key:window.widget.return.content>")
    public Widget content() {
        return content;
    }

        @AiDoc(summary = "Sets the hosted content widget.")
    @WikiMethod(summary = "<key:window.widget.method.contentSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget content(
            @WikiParameter(description = "<key:window.widget.param.content>")
            Widget content
    ) {
        if (this.content == content) return this;
        if (this.content != null) {
            removeChild(this.content);
        }
        this.content = content;
        if (content != null) {
            if (collapsed && content instanceof dev.sixik.unigui.impl.widget.WidgetBase base) {
                base.visibility(Visibility.COLLAPSED);
            }
            addChild(content);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /** Возвращает состояние свёрнутого окна: виден только title bar. */
        @AiDoc(summary = "Returns true if the window is currently collapsed to header-only.")
    @WikiMethod(summary = "<key:window.widget.method.collapsedGet>")
    @WikiReturn("<key:window.widget.return.collapsed>")
    public boolean collapsed() {
        return collapsed;
    }

    /** Сворачивает или разворачивает содержимое окна, не закрывая само окно. */
    @XmlAttribute(value = "collapsed", category = "Behavior", defaultValue = "false", description = "Показывать только заголовок окна.")
        @AiDoc(summary = "Sets whether the window is collapsed.")
    @WikiMethod(summary = "<key:window.widget.method.collapsedSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget collapsed(
            @WikiParameter(description = "<key:window.widget.param.collapsed>")
            boolean collapsed
    ) {
        if (this.collapsed == collapsed) return this;
        this.collapsed = collapsed;
        if (content instanceof dev.sixik.unigui.impl.widget.WidgetBase base) {
            if (collapsed) {
                contentVisibilityBeforeCollapse = base.visibility();
                base.visibility(Visibility.COLLAPSED);
            } else {
                base.visibility(contentVisibilityBeforeCollapse);
            }
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /** Меняет состояние сворачивания. */
        @AiDoc(summary = "Toggles collapsed state between full and header-only.")
    @WikiMethod(summary = "<key:window.widget.method.toggleCollapsed>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget toggleCollapsed() {
        return collapsed(!collapsed);
    }

    /** Возвращает, показывается ли кнопка сворачивания в title bar. */
        @AiDoc(summary = "Returns true if the collapse toggle button is shown in the header.")
    @WikiMethod(summary = "<key:window.widget.method.collapseButtonVisibleGet>")
    @WikiReturn("<key:window.widget.return.collapseButtonVisible>")
    public boolean collapseButtonVisible() {
        return collapseButtonVisible;
    }

    /** Включает или отключает кнопку сворачивания в title bar. */
    @XmlAttribute(value = "collapseButtonVisible", category = "Behavior", defaultValue = "false", description = "Показывать кнопку сворачивания в заголовке.")
        @AiDoc(summary = "Sets the visibility of the header collapse button.")
    @WikiMethod(summary = "<key:window.widget.method.collapseButtonVisibleSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget collapseButtonVisible(
            @WikiParameter(description = "<key:window.widget.param.visible>")
            boolean visible
    ) {
        if (collapseButtonVisible == visible) return this;
        collapseButtonVisible = visible;
        collapseButton.visible(visible);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns true if the window is currently opened and active in the overlay.")
    @WikiMethod(summary = "<key:window.widget.method.opened>")
    @WikiReturn("<key:window.widget.return.opened>")
    public boolean opened() {
        return open;
    }

        @AiDoc(summary = "Returns true if this window currently holds focus in its WindowManager.")
    @WikiMethod(summary = "<key:window.widget.method.active>")
    @WikiReturn("<key:window.widget.return.active>")
    public boolean active() {
        return active;
    }

        @AiDoc(summary = "Returns true if the window is modal, blocking interaction with windows below.")
    @WikiMethod(summary = "<key:window.widget.method.modalGet>")
    @WikiReturn("<key:window.widget.return.modal>")
    public boolean modal() {
        return modal;
    }

    @XmlAttribute(value = "modal", category = "Behavior", defaultValue = "false", description = "Whether the window blocks interaction outside itself while open.")
        @AiDoc(summary = "Configures modal behavior for this window.")
    @WikiMethod(summary = "<key:window.widget.method.modalSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget modal(
            @WikiParameter(description = "<key:window.widget.param.modal>")
            boolean modal
    ) {
        if (this.modal == modal) return this;
        boolean oldModal = this.modal;
        this.modal = modal;
        if (fixedModal && modal) {
            stopDragging();
            stopResizing();
        }
        if (windowManager != null) {
            windowManager.onModalChanged(this, oldModal, modal);
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns true if the modal window is fixed and cannot be dragged.")
    @WikiMethod(summary = "<key:window.widget.method.fixedModalGet>")
    @WikiReturn("<key:window.widget.return.fixedModal>")
    public boolean fixedModal() {
        return fixedModal;
    }

    @XmlAttribute(value = "fixedModal", category = "Behavior", defaultValue = "false", description = "Whether modal state disables dragging and resizing.")
        @AiDoc(summary = "Configures whether this modal window has a fixed non-draggable position.")
    @WikiMethod(summary = "<key:window.widget.method.fixedModalSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget fixedModal(
            @WikiParameter(description = "<key:window.widget.param.fixedModal>")
            boolean fixedModal
    ) {
        if (this.fixedModal == fixedModal) return this;
        this.fixedModal = fixedModal;
        if (fixedModal && modal) {
            stopDragging();
            stopResizing();
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Тема оформления окна: собственная или, если не задана, глобальная {@link DockTheme#defaultTheme()}.
     *
     * @return тема, по которой рисуются заголовок, рамка и тень окна
     */
        @AiDoc(summary = "Returns the active DockTheme applied to window chrome.")
    @WikiMethod(summary = "<key:window.widget.method.themeGet>")
    @WikiReturn("<key:window.widget.return.theme>")
    public DockTheme theme() {
        DockTheme own = theme;
        return own != null ? own : DockTheme.defaultTheme();
    }

    /**
     * Назначает окну собственную тему.
     *
     * @param theme тема; {@code null} возвращает окно на глобальную тему
     * @return это окно для fluent-настройки
     */
        @AiDoc(summary = "Applies a custom DockTheme to window chrome and controls.")
    @WikiMethod(summary = "<key:window.widget.method.themeSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget theme(
            @WikiParameter(description = "<key:window.widget.param.theme>")
            DockTheme theme
    ) {
        this.theme = theme;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Иконка окна: рисуется в заголовке и становится иконкой вкладки при стыковке.
     *
     * @return иконка или {@link UiIcon#NONE}
     */
        @AiDoc(summary = "Returns the title bar icon.")
    @WikiMethod(summary = "<key:window.widget.method.iconGet>")
    @WikiReturn("<key:window.widget.return.icon>")
    public UiIcon icon() {
        return icon;
    }

    /**
     * Задаёт иконку окна.
     *
     * @param icon иконка; {@code null} убирает иконку
     * @return это окно для fluent-настройки
     */
        @AiDoc(summary = "Sets the title bar icon.")
    @WikiMethod(summary = "<key:window.widget.method.iconSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget icon(
            @WikiParameter(description = "<key:window.widget.param.icon>")
            UiIcon icon
    ) {
        UiIcon normalized = icon == null ? UiIcon.NONE : icon;
        if (this.icon == normalized) return this;
        this.icon = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns true if redocking this window into other hosts is locked.")
    @WikiMethod(summary = "<key:window.widget.method.dockRedockLockedGet>")
    @WikiReturn("<key:window.widget.return.dockRedockLocked>")
    public boolean dockRedockLocked() {
        return dockRedockLocked;
    }

    @XmlAttribute(value = "dockRedockLocked", category = "Behavior", defaultValue = "false", description = "Whether docking/redocking actions are locked for this window.")
        @AiDoc(summary = "Locks or unlocks redocking capability for this window.")
    @WikiMethod(summary = "<key:window.widget.method.dockRedockLockedSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget dockRedockLocked(
            @WikiParameter(description = "<key:window.widget.param.dockRedockLocked>")
            boolean dockRedockLocked
    ) {
        if (this.dockRedockLocked == dockRedockLocked) return this;
        this.dockRedockLocked = dockRedockLocked;
        if (dockRedockLocked) {
            DockingRoot.clearForeignWindowDropPreview(this);
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Политика участия окна в докинге.
     *
     * <p>По умолчанию {@link DockPolicy#FULL}: докинг — свойство любого окна. Перетаскивание заголовка
     * над {@link DockingRoot} или над другим окном показывает зоны дропа, а окно под курсором на лету
     * становится окном-хостом с вкладками. Отключается только осознанно, для диалогов и HUD.</p>
     *
     * @return текущая политика
     */
        @AiDoc(summary = "Returns the docking policy (FULL, GUEST_ONLY, DISABLED).")
    @WikiMethod(summary = "<key:window.widget.method.dockPolicyGet>")
    @WikiReturn("<key:window.widget.return.dockPolicy>")
    public DockPolicy dockPolicy() {
        return dockPolicy;
    }

    /**
     * Задаёт политику участия окна в докинге.
     *
     * @param dockPolicy новая политика; {@code null} означает {@link DockPolicy#FULL}
     * @return это окно для fluent-настройки
     */
    @XmlAttribute(value = "dockPolicy", category = "Behavior", defaultValue = "full", description = "Docking participation: full, guest_only or disabled.")
        @AiDoc(summary = "Sets the docking integration policy.")
    @WikiMethod(summary = "<key:window.widget.method.dockPolicySet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget dockPolicy(
            @WikiParameter(description = "<key:window.widget.param.dockPolicy>")
            DockPolicy dockPolicy
    ) {
        DockPolicy normalized = dockPolicy == null ? DockPolicy.FULL : dockPolicy;
        if (this.dockPolicy == normalized) return this;
        this.dockPolicy = normalized;
        if (!normalized.canBeDocked() || !normalized.acceptsDrops()) {
            DockingRoot.clearForeignWindowDropPreview(this);
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Можно ли перетащить это окно в док или на другое окно.
     *
     * <p>Истинно по умолчанию; ложно для {@link DockPolicy#DISABLED} и для модальных окон, которые
     * никогда не стыкуются.</p>
     *
     * @return {@code true}, если окно может быть источником дропа
     */
        @AiDoc(summary = "Returns true if this window can be docked into an external host.")
    @WikiMethod(summary = "<key:window.widget.method.dockableGet>")
    @WikiReturn("<key:window.widget.return.dockable>")
    public boolean dockable() {
        return dockPolicy.canBeDocked() && !modal;
    }

    /**
     * Короткая форма {@link #dockPolicy(DockPolicy)}: {@code false} отключает докинг окна совсем,
     * {@code true} возвращает политику по умолчанию, если докинг был отключён.
     *
     * @param dockable {@code true}, чтобы разрешить стыковку окна
     * @return это окно для fluent-настройки
     */
    @XmlAttribute(value = "dockable", category = "Behavior", defaultValue = "true", description = "Whether this window may be docked into a DockingRoot or another window.")
        @AiDoc(summary = "Enables or disables docking capability.")
    @WikiMethod(summary = "<key:window.widget.method.dockableSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget dockable(
            @WikiParameter(description = "<key:window.widget.param.dockable>")
            boolean dockable
    ) {
        if (dockable) {
            if (dockPolicy == DockPolicy.DISABLED) dockPolicy(DockPolicy.FULL);
        } else {
            dockPolicy(DockPolicy.DISABLED);
        }
        return this;
    }

    /**
     * Принимает ли окно прямо сейчас дропы других окон и панелей.
     *
     * @return {@code true}, если политика разрешает приём, окно открыто, не модальное и не залочено
     */
        @AiDoc(summary = "Returns true if this window accepts foreign panes dropped into it.")
    @WikiMethod(summary = "<key:window.widget.method.acceptsDocking>")
    @WikiReturn("<key:window.widget.return.acceptsDocking>")
    public boolean acceptsDocking() {
        return dockPolicy.acceptsDrops() && !modal && !dockRedockLocked && open && dockableContentHost();
    }

    /**
     * Контент окна не должен быть чужим {@link DockingRoot}: такой root уже умеет принимать дропы сам, и
     * заворачивать его в ещё один слой вкладок не нужно.
     */
    private boolean dockableContentHost() {
        return !(content instanceof DockingRoot) || content == dockingRoot;
    }

    /**
     * Id, под которым контент окна будет зарегистрирован как {@code DockPane} при докинге.
     *
     * <p>Пустая строка означает авто-id: сначала используется widget id окна,
     * иначе генерируется случайный id панели.</p>
     */
        @AiDoc(summary = "Returns the pane ID representing this window when docked.")
    @WikiMethod(summary = "<key:window.widget.method.dockPaneIdGet>")
    @WikiReturn("<key:window.widget.return.dockPaneId>")
    public String dockPaneId() {
        return dockPaneId;
    }

    /**
     * Задаёт id будущей dock-панели.
     *
     * @param dockPaneId id панели или пустая строка для авто-id
     * @return это окно для fluent-настройки
     */
    @XmlAttribute(value = "dockPaneId", category = "Behavior", defaultValue = "", description = "Pane id used when this window is docked into a DockingRoot.")
        @AiDoc(summary = "Sets the pane ID representing this window when docked.")
    @WikiMethod(summary = "<key:window.widget.method.dockPaneIdSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget dockPaneId(
            @WikiParameter(description = "<key:window.widget.param.dockPaneId>")
            String dockPaneId
    ) {
        String normalized = dockPaneId == null ? "" : dockPaneId.trim();
        if (this.dockPaneId.equals(normalized)) return this;
        this.dockPaneId = normalized;
        return this;
    }

    /**
     * Роль будущей dock-панели: документ или tool-панель.
     */
        @AiDoc(summary = "Returns the classification kind (DOCUMENT or TOOL) when docked.")
    @WikiMethod(summary = "<key:window.widget.method.dockKindGet>")
    @WikiReturn("<key:window.widget.return.dockKind>")
    public DockPaneKind dockKind() {
        return dockKind;
    }

    /**
     * Задаёт роль будущей dock-панели.
     *
     * @param dockKind роль панели; {@code null} нормализуется в {@code TOOL}
     * @return это окно для fluent-настройки
     */
    @XmlAttribute(value = "dockKind", category = "Behavior", defaultValue = "tool", description = "Dock pane kind used when this window is docked: tool or document.")
        @AiDoc(summary = "Sets the docking classification kind.")
    @WikiMethod(summary = "<key:window.widget.method.dockKindSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget dockKind(
            @WikiParameter(description = "<key:window.widget.param.dockKind>")
            DockPaneKind dockKind
    ) {
        DockPaneKind normalized = dockKind == null ? DockPaneKind.TOOL : dockKind;
        if (this.dockKind == normalized) return this;
        this.dockKind = normalized;
        return this;
    }

    /**
     * Id-источник для drop intent при перетаскивании окна над {@link DockingRoot}.
     */
        @AiDoc(summary = "Returns the source docking host ID if this window was torn off from a dock.")
    @WikiMethod(summary = "<key:window.widget.method.dockSourceId>")
    @WikiReturn("<key:window.widget.return.dockSourceId>")
    public String dockSourceId() {
        if (!dockPaneId.isEmpty()) return dockPaneId;
        String widgetId = id();
        return widgetId == null ? "" : widgetId;
    }

    /**
     * Возвращает, является ли окно сейчас док-хостом: внутри него живёт свой {@link DockingRoot}, а
     * полоса вкладок заменяет заголовок.
     *
     * <p>Хостом окно становится само, как только на него наводят другое окно, поэтому вручную включать
     * это нужно только чтобы заранее собрать окно с панелями ({@link #dockTool}, {@link #dockDocument}).</p>
     *
     * @return {@code true}, если контент окна — его внутренний docking root
     */
        @AiDoc(summary = "Returns true if docking features are actively enabled.")
    @WikiMethod(summary = "<key:window.widget.method.dockingEnabledGet>")
    @WikiReturn("<key:window.widget.return.dockingEnabled>")
    public boolean dockingEnabled() {
        return dockHost;
    }

    /**
     * Явно включает или выключает режим док-хоста.
     *
     * <p>{@code true}: текущий контент (если есть) оборачивается в панель с id {@link #dockSourceId()} и
     * заголовком окна, внутренний root становится контентом. {@code false}: одна панель разворачивается
     * обратно в контент, окно перестаёт принимать дропы ({@link DockPolicy#GUEST_ONLY}); несколько панелей
     * остаются в root, но новые дропы блокируются.</p>
     *
     * @param dockingEnabled {@code true}, чтобы превратить окно в док-хост
     * @return это окно для fluent-настройки
     */
    @XmlAttribute(value = "dockingEnabled", category = "Behavior", defaultValue = "false", description = "Whether this window hosts an internal docking area right now.")
        @AiDoc(summary = "Enables or disables docking integration.")
    @WikiMethod(summary = "<key:window.widget.method.dockingEnabledSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget dockingEnabled(
            @WikiParameter(description = "<key:window.widget.param.dockingEnabled>")
            boolean dockingEnabled
    ) {
        if (dockingEnabled) {
            if (!dockPolicy.acceptsDrops()) dockPolicy = DockPolicy.FULL;
            autoDockHost = false;
            enterDockHost();
        } else {
            DockingRoot.clearForeignWindowDropPreview(this);
            autoDockHost = false;
            if (dockPolicy == DockPolicy.FULL) dockPolicy = DockPolicy.GUEST_ONLY;
            unwrapDockingRoot();
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Внутренний {@link DockingRoot} окна-хоста или {@code null}, если окно ни разу не было хостом.
     */
        @AiDoc(summary = "Returns the internal DockingRoot hosted inside this window, or null.")
    @WikiMethod(summary = "<key:window.widget.method.dockingRoot>")
    @WikiReturn("<key:window.widget.return.dockingRoot>")
    public DockingRoot dockingRoot() {
        return dockingRoot;
    }

    /**
     * Превращает окно в док-хост на лету и возвращает его внутренний root.
     *
     * <p>Вызывается системой докинга, когда над окном держат другое окно: контент оборачивается в
     * одну панель, а заголовок окна уступает место полосе вкладок той же высоты, так что раскладка не
     * прыгает. Если дроп не состоялся, хост снимается обратно ({@link #releaseAutoDockHost()}).</p>
     *
     * @return внутренний root или {@code null}, если окно не принимает дропы
     */
        @AiDoc(summary = "Initializes and returns an embedded DockingRoot instance as content.")
    @WikiMethod(summary = "<key:window.widget.method.ensureDockHost>")
    @WikiReturn("<key:window.widget.return.dockingRoot>")
    public DockingRoot ensureDockHost() {
        if (dockHost && dockingRoot != null && content == dockingRoot) return dockingRoot;
        if (!acceptsDocking()) return null;
        autoDockHost = true;
        enterDockHost();
        relayoutNow();
        return dockingRoot;
    }

    /**
     * Откатывает автоматически созданный хост, если в нём осталась только исходная панель.
     *
     * <p>Если хост создан явно ({@link #dockingEnabled(boolean)}) или в него уже что-то стыковали,
     * метод ничего не делает.</p>
     */
        @AiDoc(summary = "Releases an automatically created docking host if empty.")
    @WikiMethod(summary = "<key:window.widget.method.releaseAutoDockHost>")
    public void releaseAutoDockHost() {
        if (!autoDockHost || !dockHost || dockingRoot == null) return;
        if (dockingRoot.manager().paneCount() > 1) {
            autoDockHost = false;
            return;
        }
        autoDockHost = false;
        unwrapDockingRoot();
        relayoutNow();
    }

    /**
     * Сразу пересчитывает раскладку окна по последним bounds хоста.
     *
     * <p>Смена режима «заголовок или вкладки» должна быть видна системе докинга немедленно: превью и
     * зоны дропа считаются по геометрии внутреннего root, а следующий кадр наступит позже, чем следующее
     * движение мыши.</p>
     */
    private void relayoutNow() {
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        if (hostBounds.width() > 0.0f || hostBounds.height() > 0.0f) {
            applyQueuedMutations();
            arrangeInHost(hostBounds);
        }
    }

    /**
     * Закрепляет хост: после реального дропа он больше не откатывается как временный.
     */
        @AiDoc(summary = "Commits an auto-created docking host, making it permanent.")
    @WikiMethod(summary = "<key:window.widget.method.commitDockHost>")
    public void commitDockHost() {
        autoDockHost = false;
    }

    /**
     * Добавляет панель во внутреннюю docking area, при необходимости включая её.
     *
     * @param pane панель для добавления; {@code null} игнорируется
     * @return это окно для fluent-настройки
     */
        @AiDoc(summary = "Docks a pane into this window's embedded docking host.")
    @WikiMethod(summary = "<key:window.widget.method.dockPane>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget dockPane(
            @WikiParameter(description = "<key:window.widget.param.pane>")
            DockPane pane
    ) {
        if (pane == null) return this;
        if (!dockHost) dockingEnabled(true);
        autoDockHost = false;
        dockingRoot.addPane(pane);
        return this;
    }

    /**
     * Добавляет документ во внутреннюю docking area.
     */
        @AiDoc(summary = "Docks a document pane into this window's docking host.")
    @WikiMethod(summary = "<key:window.widget.method.dockDocument>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget dockDocument(
            @WikiParameter(description = "<key:window.widget.param.id>")
            String id,
            @WikiParameter(description = "<key:window.widget.param.title>")
            String title,
            @WikiParameter(description = "<key:window.widget.param.content>")
            Widget content
    ) {
        return dockPane(DockPane.document(id, title, content));
    }

    /**
     * Добавляет tool-панель во внутреннюю docking area.
     */
        @AiDoc(summary = "Docks a tool pane into this window's docking host.")
    @WikiMethod(summary = "<key:window.widget.method.dockTool>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget dockTool(
            @WikiParameter(description = "<key:window.widget.param.id>")
            String id,
            @WikiParameter(description = "<key:window.widget.param.title>")
            String title,
            @WikiParameter(description = "<key:window.widget.param.content>")
            Widget content
    ) {
        return dockPane(DockPane.tool(id, title, content));
    }

    /**
     * Переносит все панели из чужого окна-хоста в свой docking root табом.
     *
     * <p>Программный мерж (drop-путь идёт через зоны
     * {@code DockingRoot.dockForeignWindow}). Источник не закрывается: закрытие решает вызывающий код.
     * Панели переносятся в порядке обхода дерева источника.</p>
     *
     * @param source окно-хост, чьи панели переносятся
     * @return {@code true}, если перенесена хотя бы одна панель
     */
        @AiDoc(summary = "Merges docking panes from another window into this window's dock host.")
    @WikiMethod(summary = "<key:window.widget.method.mergeDockingFrom>")
    @WikiReturn("<key:window.widget.return.mergeSuccess>")
    public boolean mergeDockingFrom(
            @WikiParameter(description = "<key:window.widget.param.sourceWindow>")
            WindowWidget source
    ) {
        if (source == null || source == this) return false;
        DockingRoot sourceRoot = source.dockingRoot;
        if (sourceRoot == null) return false;
        DockingRoot target = ensureDockHost();
        if (target == null) return false;
        java.util.List<DockPane> panes = java.util.List.copyOf(sourceRoot.manager().panes());
        if (panes.isEmpty()) return false;
        for (DockPane pane : panes) {
            DockPane detached = sourceRoot.manager().detachPaneForFloating(pane.id());
            if (detached != null) {
                target.manager().addPane(detached);
            }
        }
        commitDockHost();
        return true;
    }

    private void ensureDockingRoot() {
        if (dockingRoot != null) return;
        dockingRoot = new DockingRoot();
        dockingRoot.windowHost(this);
        dockingRoot.layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));
    }

    private void enterDockHost() {
        ensureDockingRoot();
        dockingRoot.theme(theme);
        wrapContentIntoDocking();
        dockHost = content == dockingRoot;
    }

    private void wrapContentIntoDocking() {
        if (dockingRoot == null) return;
        Widget current = content;
        if (current == null || current == dockingRoot) {
            if (current == null) content(dockingRoot);
            return;
        }
        DockPane pane = dockKind == DockPaneKind.DOCUMENT
                ? DockPane.document(dockSourceId(), richTitle, current)
                : DockPane.tool(dockSourceId(), richTitle, current);
        pane.closable(closeButtonVisible);
        pane.icon(icon);
        dockingRoot.addPane(pane);
        content(dockingRoot);
    }

    private void unwrapDockingRoot() {
        if (dockingRoot == null) return;
        java.util.List<DockPane> panes = dockingRoot.manager().panes();
        if (panes.size() == 1) {
            DockPane single = panes.get(0);
            DockPane detached = dockingRoot.manager().detachPaneForFloating(single.id());
            Widget restored = detached == null ? single.content() : detached.content();
            if (content == dockingRoot) {
                content(restored);
            }
        } else if (panes.isEmpty() && content == dockingRoot) {
            content(null);
        }
        // Несколько панелей: root остаётся контентом, но дропы блокируются
        // через DockingRoot.acceptsDockDrops(), пока политика не принимает дропы.
        dockHost = content == dockingRoot;
    }

    /**
     * Панель, из которой это окно было вытащено, или {@code null}.
     *
     * <p>Нужна, чтобы возврат окна в док восстановил ту же {@link DockPane} со всем её состоянием, а не
     * собрал новую из контента.</p>
     *
     * @return исходная панель или {@code null}
     */
        @AiDoc(summary = "Returns the source DockPane representation when floating.")
    @WikiMethod(summary = "<key:window.widget.method.dockSourcePaneGet>")
    @WikiReturn("<key:window.widget.return.dockSourcePane>")
    public DockPane dockSourcePane() {
        return dockSourcePane;
    }

    /**
     * Запоминает исходную панель окна.
     *
     * @param pane панель, из которой окно вытащено, или {@code null}
     * @return это окно для fluent-настройки
     */
        @AiDoc(summary = "Sets the source DockPane representation.")
    @WikiMethod(summary = "<key:window.widget.method.dockSourcePaneSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget dockSourcePane(
            @WikiParameter(description = "<key:window.widget.param.pane>")
            DockPane pane
    ) {
        this.dockSourcePane = pane;
        return this;
    }

    /**
     * Окно без собственного заголовка: его роль играет полоса вкладок внутреннего root.
     *
     * @return {@code true}, если окно сейчас док-хост
     */
        @AiDoc(summary = "Returns true if the window renders without a title bar header.")
    @WikiMethod(summary = "<key:window.widget.method.captionless>")
    @WikiReturn("<key:window.widget.return.captionless>")
    public boolean captionless() {
        return dockHost && dockingRoot != null && content == dockingRoot;
    }

    /**
     * Высота заголовка с учётом режима: у док-хоста заголовка нет, его место занимает полоса вкладок.
     *
     * @return высота заголовка в UI-пикселях
     */
    private float captionHeight() {
        return captionless() ? 0.0f : headerHeight;
    }

    /**
     * Внутренние отступы контента: у док-хоста вкладки и панели занимают окно целиком.
     *
     * @return отступы вокруг контента
     */
    private EdgeInsets contentPadding() {
        return captionless() ? EdgeInsets.ZERO : padding;
    }
        @AiDoc(summary = "Opens this window as a modal dialog.")
    @WikiMethod(summary = "<key:window.widget.method.openModal>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget openModal() {
        modal(true);
        return open();
    }

        @AiDoc(summary = "Opens the window.")
    @WikiMethod(summary = "<key:window.widget.method.open>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget open() {
        return open(true);
    }

        @AiDoc(summary = "Closes the window.")
    @WikiMethod(summary = "<key:window.widget.method.close>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget close() {
        return open(false);
    }

        @AiDoc(summary = "Toggles between open and closed states.")
    @WikiMethod(summary = "<key:window.widget.method.toggle>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget toggle() {
        return open(!open);
    }

    @XmlAttribute(value = "open", category = "Behavior", defaultValue = "false", description = "Whether the window is visible.")
        @AiDoc(summary = "Sets the open state of the window.")
    @WikiMethod(summary = "<key:window.widget.method.openState>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget open(
            @WikiParameter(description = "<key:window.widget.param.open>")
            boolean open
    ) {
        if (this.open == open) return this;
        this.open = open;
        if (!open) {
            compactDrag = false;
            stopDragging();
            stopResizing();
            DockingRoot.clearForeignWindowDropPreview(this);
            if (windowManager != null) {
                windowManager.onWindowClosed(this);
            } else {
                setActiveInternal(false);
            }
            dispatchWindowClosed();
            visible(false);
        } else {
            visible(true);
            dispatchWindowOpened();
            if (windowManager != null) {
                windowManager.onWindowOpened(this);
            }
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns true if window dragging by the header is enabled.")
    @WikiMethod(summary = "<key:window.widget.method.draggableGet>")
    @WikiReturn("<key:window.widget.return.draggable>")
    public boolean draggable() {
        return draggable;
    }

    @XmlAttribute(value = "draggable", category = "Behavior", defaultValue = "true", description = "Whether the window can be moved by dragging its header.")
        @AiDoc(summary = "Enables or disables window dragging.")
    @WikiMethod(summary = "<key:window.widget.method.draggableSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget draggable(
            @WikiParameter(description = "<key:window.widget.param.draggable>")
            boolean draggable
    ) {
        if (this.draggable == draggable) return this;
        this.draggable = draggable;
        if (!effectiveDraggable()) {
            stopDragging();
        }
        return this;
    }

        @AiDoc(summary = "Returns true if the window is currently being moved.")
    @WikiMethod(summary = "<key:window.widget.method.dragging>")
    @WikiReturn("<key:window.widget.return.dragging>")
    public boolean dragging() {
        return dragging;
    }

    /**
     * Возвращает последнюю позицию указателя во время drag окна.
     *
     * <p>Обновляется в {@code startDragging}/{@code updateDrag} координатами
     * указателя (не окна): drop-логика обязана резолвить зону по мыши, иначе
     * превью и результат расходятся, когда окно отстаёт от курсора на offset
     * захвата.</p>
     *
     * @return X указателя в root-координатах на момент последнего drag-события
     */
        @AiDoc(summary = "Returns the last pointer root X coordinate during dragging.")
    @WikiMethod(summary = "<key:window.widget.method.lastDragRootX>")
    @WikiReturn("<key:window.widget.return.coordinate>")
    public float lastDragRootX() {
        return lastDragRootX;
    }

    /**
     * Возвращает последнюю позицию указателя во время drag окна.
     *
     * @return Y указателя в root-координатах на момент последнего drag-события
     */
        @AiDoc(summary = "Returns the last pointer root Y coordinate during dragging.")
    @WikiMethod(summary = "<key:window.widget.method.lastDragRootY>")
    @WikiReturn("<key:window.widget.return.coordinate>")
    public float lastDragRootY() {
        return lastDragRootY;
    }

        @AiDoc(summary = "Returns true if window border and grip resizing is enabled.")
    @WikiMethod(summary = "<key:window.widget.method.resizableGet>")
    @WikiReturn("<key:window.widget.return.resizable>")
    public boolean resizable() {
        return resizable;
    }

    @XmlAttribute(value = "resizable", category = "Behavior", defaultValue = "true", description = "Whether the window can be resized by edge handles.")
        @AiDoc(summary = "Enables or disables border and grip resizing.")
    @WikiMethod(summary = "<key:window.widget.method.resizableSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget resizable(
            @WikiParameter(description = "<key:window.widget.param.resizable>")
            boolean resizable
    ) {
        if (this.resizable == resizable) return this;
        this.resizable = resizable;
        if (!effectiveResizable()) {
            stopResizing();
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns true if the window is actively being resized.")
    @WikiMethod(summary = "<key:window.widget.method.resizing>")
    @WikiReturn("<key:window.widget.return.resizing>")
    public boolean resizing() {
        return resizing;
    }

        @AiDoc(summary = "Returns the name of the active resize handle (e.g. BOTTOM_RIGHT).")
    @WikiMethod(summary = "<key:window.widget.method.resizeHandle>")
    @WikiReturn("<key:window.widget.return.resizeHandle>")
    public String resizeHandle() {
        return resizeHandle.publicName();
    }

        @AiDoc(summary = "Returns true if the title bar close button is visible.")
    @WikiMethod(summary = "<key:window.widget.method.closeButtonVisibleGet>")
    @WikiReturn("<key:window.widget.return.closeButtonVisible>")
    public boolean closeButtonVisible() {
        return closeButtonVisible;
    }

    @XmlAttribute(value = "closeButtonVisible", category = "Behavior", defaultValue = "true", description = "Whether the close button is shown in the header.")
        @AiDoc(summary = "Sets the visibility of the close button.")
    @WikiMethod(summary = "<key:window.widget.method.closeButtonVisibleSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget closeButtonVisible(
            @WikiParameter(description = "<key:window.widget.param.closeButtonVisible>")
            boolean closeButtonVisible
    ) {
        if (this.closeButtonVisible == closeButtonVisible) return this;
        this.closeButtonVisible = closeButtonVisible;
        closeButton.visible(closeButtonVisible);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the title bar close button widget.")
    @WikiMethod(summary = "<key:window.widget.method.closeButton>")
    @WikiReturn("<key:window.widget.return.button>")
    public Button closeButton() {
        return closeButton;
    }

        @AiDoc(summary = "Returns true if clicking outside dismisses this window.")
    @WikiMethod(summary = "<key:window.widget.method.closeOnOutsideClickGet>")
    @WikiReturn("<key:window.widget.return.closeOnOutsideClick>")
    public boolean closeOnOutsideClick() {
        return closeOnOutsideClick;
    }

    @XmlAttribute(value = "closeOnOutsideClick", category = "Behavior", defaultValue = "false", description = "Whether outside pointer presses close the window.")
        @AiDoc(summary = "Configures whether clicking outside closes the window.")
    @WikiMethod(summary = "<key:window.widget.method.closeOnOutsideClickSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget closeOnOutsideClick(
            @WikiParameter(description = "<key:window.widget.param.closeOnOutsideClick>")
            boolean closeOnOutsideClick
    ) {
        this.closeOnOutsideClick = closeOnOutsideClick;
        return this;
    }

        @AiDoc(summary = "Returns true if window coordinates are clamped within the host boundary.")
    @WikiMethod(summary = "<key:window.widget.method.constrainToHostGet>")
    @WikiReturn("<key:window.widget.return.constrainToHost>")
    public boolean constrainToHost() {
        return constrainToHost;
    }

    @XmlAttribute(value = "constrainToHost", category = "Layout", defaultValue = "true", description = "Whether arrangement clamps the window inside the overlay host.")
        @AiDoc(summary = "Configures host boundary clamping.")
    @WikiMethod(summary = "<key:window.widget.method.constrainToHostSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget constrainToHost(
            @WikiParameter(description = "<key:window.widget.param.constrainToHost>")
            boolean constrainToHost
    ) {
        if (this.constrainToHost == constrainToHost) return this;
        this.constrainToHost = constrainToHost;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the window horizontal X coordinate.")
    @WikiMethod(summary = "<key:window.widget.method.windowX>")
    @WikiReturn("<key:window.widget.return.coordinate>")
    public float windowX() {
        return x;
    }

        @AiDoc(summary = "Returns the window vertical Y coordinate.")
    @WikiMethod(summary = "<key:window.widget.method.windowY>")
    @WikiReturn("<key:window.widget.return.coordinate>")
    public float windowY() {
        return y;
    }

        @AiDoc(summary = "Sets the window coordinates in UI pixels.")
    @WikiMethod(summary = "<key:window.widget.method.position>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget position(
            @WikiParameter(description = "<key:window.widget.param.x>")
            float x,
            @WikiParameter(description = "<key:window.widget.param.y>")
            float y
    ) {
        float normalizedX = Float.isFinite(x) ? x : 0.0f;
        float normalizedY = Float.isFinite(y) ? y : 0.0f;
        if (this.x == normalizedX && this.y == normalizedY) return this;
        this.x = normalizedX;
        this.y = normalizedY;
        layout(style -> style.left(normalizedX).top(normalizedY));
        return this;
    }

        @AiDoc(summary = "Returns the title bar header height in UI pixels.")
    @WikiMethod(summary = "<key:window.widget.method.headerHeightGet>")
    @WikiReturn("<key:window.widget.return.headerHeight>")
    public float headerHeight() {
        return headerHeight;
    }

    @XmlAttribute(value = "headerHeight", category = "Layout", defaultValue = "22", description = "Window header height in UI pixels.")
        @AiDoc(summary = "Sets the title bar header height in UI pixels.")
    @WikiMethod(summary = "<key:window.widget.method.headerHeightSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget headerHeight(
            @WikiParameter(description = "<key:window.widget.param.headerHeight>")
            float headerHeight
    ) {
        float normalized = Float.isFinite(headerHeight) ? Math.max(12.0f, headerHeight) : DEFAULT_HEADER_HEIGHT;
        if (this.headerHeight == normalized) return this;
        this.headerHeight = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Sets minimum width and height limits for window resizing.")
    @WikiMethod(summary = "<key:window.widget.method.minWindowSize>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget minWindowSize(
            @WikiParameter(description = "<key:window.widget.param.width>")
            float width,
            @WikiParameter(description = "<key:window.widget.param.height>")
            float height
    ) {
        float normalizedWidth = Float.isFinite(width) ? Math.max(0.0f, width) : DEFAULT_MIN_WIDTH;
        float normalizedHeight = Float.isFinite(height) ? Math.max(0.0f, height) : DEFAULT_MIN_HEIGHT;
        if (minWindowWidth == normalizedWidth && minWindowHeight == normalizedHeight) return this;
        minWindowWidth = normalizedWidth;
        minWindowHeight = normalizedHeight;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the window content padding.")
    @WikiMethod(summary = "<key:window.widget.method.paddingGet>")
    @WikiReturn("<key:window.widget.return.padding>")
    public EdgeInsets padding() {
        return padding;
    }

    @XmlAttribute(value = "padding", category = "Layout", defaultValue = "8", description = "Inner padding around window content.")
        @AiDoc(summary = "Sets the window content padding.")
    @WikiMethod(summary = "<key:window.widget.method.paddingSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget padding(
            @WikiParameter(description = "<key:window.widget.param.padding>")
            EdgeInsets padding
    ) {
        EdgeInsets normalized = padding == null ? EdgeInsets.ZERO : padding;
        if (this.padding.equals(normalized)) return this;
        this.padding = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the mutable background color of the title bar header.")
    @WikiMethod(summary = "<key:window.widget.method.headerColor>")
    @WikiReturn("<key:window.widget.return.mutableColor>")
    public MutableColor headerColor() {
        return headerColor;
    }

        @AiDoc(summary = "Returns the mutable separator line color between header and content.")
    @WikiMethod(summary = "<key:window.widget.method.headerSeparatorColor>")
    @WikiReturn("<key:window.widget.return.mutableColor>")
    public MutableColor headerSeparatorColor() {
        return headerSeparatorColor;
    }

        @AiDoc(summary = "Returns the mutable title text color.")
    @WikiMethod(summary = "<key:window.widget.method.titleColor>")
    @WikiReturn("<key:window.widget.return.mutableColor>")
    public MutableColor titleColor() {
        return titleColor;
    }

    public EventSubscription onOpened(EventListener<? super WindowOpenedEvent> listener) {
        return on(WindowOpenedEvent.TYPE, listener);
    }

    public EventSubscription onClosed(EventListener<? super WindowClosedEvent> listener) {
        return on(WindowClosedEvent.TYPE, listener);
    }

    public EventSubscription onActivated(EventListener<? super WindowActivatedEvent> listener) {
        return on(WindowActivatedEvent.TYPE, listener);
    }

    public EventSubscription onDeactivated(EventListener<? super WindowDeactivatedEvent> listener) {
        return on(WindowDeactivatedEvent.TYPE, listener);
    }

    public EventSubscription onMoveStarted(EventListener<? super WindowMoveStartedEvent> listener) {
        return on(WindowMoveStartedEvent.TYPE, listener);
    }

    public EventSubscription onMoved(EventListener<? super WindowMovedEvent> listener) {
        return on(WindowMovedEvent.TYPE, listener);
    }

    public EventSubscription onMoveEnded(EventListener<? super WindowMoveEndedEvent> listener) {
        return on(WindowMoveEndedEvent.TYPE, listener);
    }

    public EventSubscription onResizeStarted(EventListener<? super WindowResizeStartedEvent> listener) {
        return on(WindowResizeStartedEvent.TYPE, listener);
    }

    public EventSubscription onResized(EventListener<? super WindowResizedEvent> listener) {
        return on(WindowResizedEvent.TYPE, listener);
    }

    public EventSubscription onResizeEnded(EventListener<? super WindowResizeEndedEvent> listener) {
        return on(WindowResizeEndedEvent.TYPE, listener);
    }

    public EventSubscription onModalOpened(EventListener<? super ModalOpenedEvent> listener) {
        return on(ModalOpenedEvent.TYPE, listener);
    }

    public EventSubscription onModalClosed(EventListener<? super ModalClosedEvent> listener) {
        return on(ModalClosedEvent.TYPE, listener);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED || !open) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }

        applyQueuedMutations();
        EdgeInsets padding = contentPadding();
        float headerHeight = captionHeight();
        boolean captionless = captionless();
        boolean closeShown = closeButtonVisible && !captionless;
        boolean collapseShown = collapseButtonVisible && !captionless;
        closeButton.visible(closeShown);
        if (closeShown) {
            closeButton.measure(new LayoutContext(18.0f, headerHeight));
        }
        collapseButton.visible(collapseShown);
        if (collapseShown) {
            collapseButton.measure(new LayoutContext(18.0f, headerHeight));
        }

        float availableContentWidth = context == null ? DEFAULT_WIDTH : Math.max(0.0f, context.availableWidth() - padding.horizontal());
        float availableContentHeight = context == null ? DEFAULT_HEIGHT : Math.max(0.0f, context.availableHeight() - headerHeight - padding.vertical());
        float contentWidth = 0.0f;
        float contentHeight = 0.0f;
        if (!collapsed && !compactDrag && content != null && content.visibility() != Visibility.COLLAPSED) {
            content.measure(new LayoutContext(availableContentWidth, availableContentHeight));
            contentWidth = content.desiredSize().width();
            contentHeight = content.desiredSize().height();
        }

        float closeWidth = closeShown ? closeButton.desiredSize().width() : 0.0f;
        float collapseWidth = collapseShown ? collapseButton.desiredSize().width() : 0.0f;
        float iconWidth = icon.isNone() ? 0.0f : theme().iconSize() + theme().iconGap();
        float titleWidth = captionless ? 0.0f : TextEngine.measureLineWidth(title) + iconWidth
                + padding.left() + padding.right() + closeWidth + collapseWidth + 8.0f;
        float desiredWidth = Math.max(minWindowWidth, Math.max(titleWidth, contentWidth + padding.horizontal()));
        // В collapsed-состоянии окно действительно уменьшается до title bar.
        // minWindowHeight относится к обычному режиму и не должен оставлять
        // под заголовком пустой полноразмерный фон.
        float desiredHeight = collapsed
                ? Math.max(0.0f, headerHeight)
                : Math.max(minWindowHeight, headerHeight + contentHeight + padding.vertical());
        setDesiredSize(resolveDesiredSize(context, desiredWidth, desiredHeight));
    }

    @Override
    public void arrange(RectView bounds) {
        arrangeInHost(bounds);
    }

    @Override
        @AiDoc(summary = "Positions the window inside the host boundaries.")
    @WikiMethod(summary = "<key:window.widget.method.arrangeInHost>")
    public void arrangeInHost(
            @WikiParameter(description = "<key:window.widget.param.bounds>")
            RectView bounds
    ) {
        if (!open || visibility() == Visibility.COLLAPSED) {
            mutableLayoutBounds().set(bounds.x(), bounds.y(), 0.0f, 0.0f);
            return;
        }

        applyQueuedMutations();
        EdgeInsets padding = contentPadding();
        float headerHeight = captionHeight();
        boolean closeShown = closeButtonVisible && !captionless();
        boolean collapseShown = collapseButtonVisible && !captionless();
        hostBounds.set(bounds);
        MutableRect requested = AbsoluteLayoutEngine.resolveRect(
                this, bounds,
                desiredSize().width() > 0.0f ? desiredSize().width() : DEFAULT_WIDTH,
                desiredSize().height() > 0.0f ? desiredSize().height() : DEFAULT_HEIGHT);
        if (collapsed) {
            // Явный size(), оставшийся после resize, не должен превращаться в
            // пустой фон под свернутым title bar. Сам style не меняем, чтобы
            // после раскрытия вернуть прежний размер окна.
            requested.set(requested.x(), requested.y(), requested.width(), headerHeight);
        }
        MutableRect constrained = constrainToHost
                ? AbsoluteLayoutEngine.constrainToHost(
                bounds, requested.x(), requested.y(), requested.width(), requested.height())
                : requested;
        float width = constrained.width();
        float height = constrained.height();
        float absoluteX = constrained.x();
        float absoluteY = constrained.y();
        float resolvedX = absoluteX - bounds.x();
        float resolvedY = absoluteY - bounds.y();
        if (x != resolvedX || y != resolvedY) {
            x = resolvedX;
            y = resolvedY;
            layout(style -> style.left(resolvedX).top(resolvedY));
        }
        mutableLayoutBounds().set(constrained);

        if (closeShown) {
            float buttonWidth = Math.min(18.0f, Math.max(0.0f, width - padding.horizontal()));
            float buttonHeight = Math.min(18.0f, Math.max(0.0f, headerHeight - 4.0f));
            closeButton.arrange(new MutableRect(
                    absoluteX + Math.max(0.0f, width - padding.right() - buttonWidth),
                    absoluteY + Math.max(0.0f, (headerHeight - buttonHeight) * 0.5f),
                    buttonWidth,
                    buttonHeight));
        } else {
            closeButton.arrange(new MutableRect(absoluteX, absoluteY, 0.0f, 0.0f));
        }
        if (collapseShown) {
            float buttonWidth = Math.min(18.0f, Math.max(0.0f, width - padding.horizontal()));
            float buttonHeight = Math.min(18.0f, Math.max(0.0f, headerHeight - 4.0f));
            float closeOffset = closeShown ? buttonWidth + 4.0f : 0.0f;
            collapseButton.arrange(new MutableRect(
                    absoluteX + Math.max(0.0f, width - padding.right() - buttonWidth - closeOffset),
                    absoluteY + Math.max(0.0f, (headerHeight - buttonHeight) * 0.5f),
                    buttonWidth, buttonHeight));
        } else {
            collapseButton.arrange(new MutableRect(absoluteX, absoluteY, 0.0f, 0.0f));
        }

        if (!collapsed && !compactDrag && content != null && content.visibility() != Visibility.COLLAPSED) {
            StackPanel.arrangeChild(content,
                    absoluteX + padding.left(),
                    absoluteY + headerHeight + padding.top(),
                    Math.max(0.0f, width - padding.horizontal()),
                    Math.max(0.0f, height - headerHeight - padding.vertical()));
        }
    }

    @Override
    public void handle(Event event) {
        if (visibility() != Visibility.VISIBLE || !enabled()) return;
        if (event instanceof PointerPressedEvent capture
                && capture.phase() == EventPhase.CAPTURE
                && capture.button() == PointerButton.PRIMARY
                && effectiveResizable()
                && !resizing
                && resizeHandle(capture.rootX(), capture.rootY()) != ResizeHandle.NONE) {
            // Край и ручка окна важнее любого содержимого: иначе вкладка или разделитель докинга под
            // краем перехватывали бы нажатие, и окно нельзя было бы растянуть.
            if (windowManager != null) {
                windowManager.activate(this);
            }
            startResizing(capture, resizeHandle(capture.rootX(), capture.rootY()));
            event.cancel();
            return;
        }
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof PointerEvent pointerEvent && pointerEvent.phase() == EventPhase.CAPTURE) return;
        if (event instanceof PointerMovedEvent moved) {
            gripHovered = gripEnabled() && inGrip(moved.rootX(), moved.rootY());
        }

        if (event instanceof PointerPressedEvent pointer
                && pointer.button() == PointerButton.PRIMARY
                && effectiveResizable()
                && resizeHandle(pointer.rootX(), pointer.rootY()) != ResizeHandle.NONE) {
            if (windowManager != null) {
                windowManager.activate(this);
            }
            startResizing(pointer, resizeHandle(pointer.rootX(), pointer.rootY()));
            event.cancel();
        } else if (event instanceof PointerMovedEvent pointer
                && resizing
                && pointer.pointerId() == resizePointerId) {
            updateResize(pointer.rootX(), pointer.rootY());
            event.cancel();
        } else if (event instanceof PointerReleasedEvent pointer
                && pointer.button() == PointerButton.PRIMARY
                && resizing
                && pointer.pointerId() == resizePointerId) {
            updateResize(pointer.rootX(), pointer.rootY());
            stopResizing();
            event.cancel();
        } else if (event instanceof PointerPressedEvent pointer
                && pointer.button() == PointerButton.PRIMARY
                && effectiveDraggable()
                && canStartDraggingFrom(pointer)) {
            if (windowManager != null) {
                windowManager.activate(this);
            }
            startDragging(pointer);
            event.cancel();
        } else if (event instanceof PointerMovedEvent pointer
                && dragging
                && pointer.pointerId() == dragPointerId) {
            updateDrag(pointer.rootX(), pointer.rootY());
            DockingRoot.previewForeignWindowDrop(this, pointer.rootX(), pointer.rootY());
            event.cancel();
        } else if (event instanceof PointerReleasedEvent pointer
                && pointer.button() == PointerButton.PRIMARY
                && dragging
                && pointer.pointerId() == dragPointerId) {
            updateDrag(pointer.rootX(), pointer.rootY());
            stopDragging();
            boolean docked = DockingRoot.finishForeignWindowDrop(this, pointer.rootX(), pointer.rootY());
            // Не пристыковалось — окно принимает свой настоящий размер там, где его отпустили.
            leaveCompactDrag();
            event.cancel();
            if (docked) invalidate(InvalidationFlags.VISUAL);
        }
    }

    @Override
    public MouseCursor mouseCursorAt(float localX, float localY) {
        MouseCursor override = mouseCursorOverrideAt(layoutBounds().x() + localX, layoutBounds().y() + localY);
        return override != null ? override : super.mouseCursorAt(localX, localY);
    }

    /**
     * Курсор изменения размера над краем и ручкой окна, даже если их целиком закрывает содержимое
     * (например, внутренний {@link DockingRoot}).
     *
     * <p>Во время растягивания курсор не меняется, даже если указатель ушёл с края.</p>
     */
    @Override
    public MouseCursor mouseCursorOverrideAt(float rootX, float rootY) {
        if (!enabled() || !open || !effectiveResizable()) return null;
        ResizeHandle handle = resizing ? resizeHandle : resizeHandle(rootX, rootY);
        return switch (handle) {
            case LEFT, RIGHT -> MouseCursor.RESIZE_HORIZONTAL;
            case TOP, BOTTOM -> MouseCursor.RESIZE_VERTICAL;
            case TOP_LEFT, BOTTOM_RIGHT -> MouseCursor.RESIZE_NWSE;
            case TOP_RIGHT, BOTTOM_LEFT -> MouseCursor.RESIZE_NESW;
            case NONE -> null;
        };
    }

    /**
     * Рисунок ручки изменения размера: собственный, а если не задан — из {@link #theme()}.
     *
     * @return стиль ручки
     */
        @AiDoc(summary = "Returns the active resize grip visual style.")
    @WikiMethod(summary = "<key:window.widget.method.resizeGripGet>")
    @WikiReturn("<key:window.widget.return.resizeGrip>")
    public ResizeGripStyle resizeGrip() {
        return resizeGrip != null ? resizeGrip : theme().resizeGrip();
    }

    /**
     * Задаёт окну собственный рисунок ручки изменения размера.
     *
     * @param resizeGrip стиль ручки; {@code null} возвращает стиль темы
     * @return это окно для fluent-настройки
     */
        @AiDoc(summary = "Sets the corner resize grip visual style.")
    @WikiMethod(summary = "<key:window.widget.method.resizeGripSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget resizeGrip(
            @WikiParameter(description = "<key:window.widget.param.resizeGrip>")
            ResizeGripStyle resizeGrip
    ) {
        this.resizeGrip = resizeGrip;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Показывается ли ручка изменения размера в правом нижнем углу.
     *
     * @return {@code true} по умолчанию
     */
        @AiDoc(summary = "Returns true if the corner resize grip is rendered.")
    @WikiMethod(summary = "<key:window.widget.method.resizeGripVisibleGet>")
    @WikiReturn("<key:window.widget.return.resizeGripVisible>")
    public boolean resizeGripVisible() {
        return resizeGripVisible;
    }

    /**
     * Включает или выключает ручку. Край окна при этом по-прежнему растягивает окно, если оно
     * {@link #resizable()}.
     *
     * @param visible {@code true}, чтобы показывать ручку
     * @return это окно для fluent-настройки
     */
    @XmlAttribute(value = "resizeGripVisible", category = "Appearance", defaultValue = "true", description = "Whether the resize grip is drawn in the bottom-right corner.")
        @AiDoc(summary = "Sets whether the corner resize grip is visible.")
    @WikiMethod(summary = "<key:window.widget.method.resizeGripVisibleSet>")
    @WikiReturn("<key:window.widget.return.this>")
    public WindowWidget resizeGripVisible(
            @WikiParameter(description = "<key:window.widget.param.visible>")
            boolean visible
    ) {
        if (resizeGripVisible == visible) return this;
        resizeGripVisible = visible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /** Ручка работает: окно растягивается, развёрнуто, не в режиме призрака и стиль что-то рисует. */
    private boolean gripEnabled() {
        return resizeGripVisible && open && !collapsed && !compactDrag && effectiveResizable()
                && resizeGrip() != ResizeGripStyle.NONE;
    }

    /** Сторона ручки: не больше трети меньшей стороны окна, чтобы в крошечном окне она не съедала всё. */
    private float gripSize() {
        RectView b = layoutBounds();
        return Math.max(0.0f, Math.min(theme().gripSize(), Math.min(b.width(), b.height()) / 3.0f));
    }

    private boolean inGrip(float rootX, float rootY) {
        RectView b = layoutBounds();
        float size = gripSize();
        return size > 0.0f && rootX >= b.x() + b.width() - size && rootX <= b.x() + b.width()
                && rootY >= b.y() + b.height() - size && rootY <= b.y() + b.height();
    }

    /** Наведён ли указатель на ручку: последнее известное положение и указатель всё ещё в окне. */
    private boolean gripHoveredNow() {
        if (!gripHovered) return false;
        UIContext context = uiContext();
        Widget hovered = context == null ? null : context.hoverManager().hoveredWidget();
        for (Widget current = hovered; current != null; current = current.parent()) {
            if (current == this) return true;
        }
        return false;
    }

    private void paintResizeGrip(DrawScope draw, DockTheme theme) {
        if (!gripEnabled()) return;
        RectView b = layoutBounds();
        float size = gripSize();
        if (size <= 0.0f) return;
        DockColor role = resizing ? DockColor.GRIP_ACTIVE : gripHoveredNow() ? DockColor.GRIP_HOVER : DockColor.GRIP;
        resizeGrip().paint(draw, b.x() + b.width(), b.y() + b.height(), size, theme.windowRadius(),
                theme.color(role));
    }
    @Override
    protected void renderSurface(RenderContext context) {
        DockTheme theme = theme();
        RectView bounds = layoutBounds();
        if (theme.windowShadowSize() > 0.0f && bounds.width() > 0.0f && bounds.height() > 0.0f) {
            DrawScope shadow = new DrawScope(context, transform(), bounds);
            DockChrome.paintShadow(shadow, bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    theme.windowRadius(), theme.windowShadowSize(), theme.color(DockColor.WINDOW_SHADOW));
        }
        super.renderSurface(context);
    }

    @Override
    protected void renderContent(RenderContext context) {
        applyTheme();
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        if (renderCustomVisual(draw)) {
            super.renderContent(context);
            return;
        }
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        if (width <= 0.0f || height <= 0.0f) {
            super.renderContent(context);
            return;
        }
        DockTheme theme = theme();
        boolean captionless = captionless();
        if (compactDrag) {
            // Компактное «призрачное» окно: только заголовок и полупрозрачное тело. Контент не рисуем и не
            // раскладываем — он всё равно не уместился бы в 128 px, а жест — это про перенос окна.
            draw.roundedRectFilled(x, y, width, height, theme.windowRadius(), theme.color(DockColor.GHOST_FILL));
            DockChrome.paintCaption(draw, theme, x, y, width, Math.min(headerHeight, height), icon, richTitle,
                    true, true, padding.left(), padding.right(), null, null, null);
            draw.roundedRectStroke(x, y, width, height, theme.windowRadius(),
                    theme.color(DockColor.WINDOW_BORDER_ACTIVE), 1.0f);
            return;
        }
        if (!captionless) {
            float caption = Math.min(headerHeight, height);
            float closeReserved = closeButtonVisible ? closeButton.layoutBounds().width() + 6.0f : 0.0f;
            float collapseReserved = collapseButtonVisible ? collapseButton.layoutBounds().width() + 6.0f : 0.0f;
            DockChrome.paintCaption(draw, theme, x, y, width, caption, icon, richTitle, active, dragging,
                    padding.left(), padding.right() + closeReserved + collapseReserved,
                    headerColor.a() > 0.0f ? headerColor : null,
                    headerSeparatorColor.a() > 0.0f ? headerSeparatorColor : null,
                    titleColor.a() > 0.0f ? titleColor : null);
        }
        super.renderContent(context);
        if (active || dragging) {
            draw.roundedRectStroke(x, y, width, height, theme.windowRadius(),
                    theme.color(DockColor.WINDOW_BORDER_ACTIVE), 1.0f);
        }
        paintResizeGrip(draw, theme);
    }

    private boolean focused() {
        UIContext context = uiContext();
        return context != null && context.focusManager().isFocused(this);
    }

    /**
     * Принимает активный drag-жест от другого владельца указателя.
     *
     * <p>Нужно tear-off handoff в докинге: таб вырвали из стрипа посреди жеста,
     * плавающее окно создано под курсором — жест продолжается им без разрыва.
     * Смещение захвата считается от свежих bounds (окно только что arrange),
     * окно не прыгает. Вызывает {@code move-started} для консистентности
     * слушателей.</p>
     *
     * @param pointerId id указателя жеста
     * @param rootX X указателя в root-координатах
     * @param rootY Y указателя в root-координатах
     */
    public void takeoverDrag(int pointerId, float rootX, float rootY) {
        dragging = true;
        dragPointerId = pointerId;
        dragOffsetX = rootX - layoutBounds().x();
        dragOffsetY = rootY - layoutBounds().y();
        lastDragRootX = rootX;
        lastDragRootY = rootY;
        invalidate(InvalidationFlags.VISUAL);
        dispatchWindowMoveStarted();
        // Немедленный preview под точкой handoff: иначе зоны/крест появятся
        // только на следующем move, а первый кадр окна будет без оффера.
        // Наш собственный окно-путь (updateDrag→WindowMovedEvent→listener)
        // подхватит превью со следующего движения мыши.
        DockingRoot.previewForeignWindowDrop(this, rootX, rootY);
    }

    /**
     * Принимает активный drag-жест и сразу сжимает окно до компактного «призрака».
     *
     * <p>Так ведёт себя вытащенная вкладка в Unity: пока вкладку несут, она маленькое окошко (заголовок
     * плюс пустое тело), которое не закрывает цель дропа. Настоящий размер окна запоминается и
     * применяется, как только жест закончился, а окно ни к чему не пристыковалось
     * ({@link #leaveCompactDrag()}). Если окно пристыковалось, запомненный размер возвращает
     * {@link #fullWidth()}/{@link #fullHeight()}.</p>
     *
     * @param pointerId id указателя жеста
     * @param rootX X указателя в root-координатах
     * @param rootY Y указателя в root-координатах
     * @param width ширина призрака
     * @param height высота призрака
     */
    public void takeoverCompactDrag(int pointerId, float rootX, float rootY, float width, float height) {
        compactFullWidth = layoutBounds().width();
        compactFullHeight = layoutBounds().height();
        if (width > 0.0f && height > 0.0f && compactFullWidth > 0.0f && compactFullHeight > 0.0f
                && (width < compactFullWidth || height < compactFullHeight)) {
            compactDrag = true;
            compactWidth = Math.max(minWindowWidth, width);
            float compactHeight = Math.max(minWindowHeight, height);
            float hostX = hostBounds.x();
            float hostY = hostBounds.y();
            float grabX = compactWidth * 0.5f;
            float grabY = Math.min(headerHeight, compactHeight) * 0.5f;
            float nextX = rootX - hostX - grabX;
            float nextY = rootY - hostY - grabY;
            if (constrainToHost && (hostBounds.width() > 0.0f || hostBounds.height() > 0.0f)) {
                nextX = clamp(nextX, 0.0f, Math.max(0.0f, hostBounds.width() - compactWidth));
                nextY = clamp(nextY, 0.0f, Math.max(0.0f, hostBounds.height() - compactHeight));
            }
            setWindowRect(nextX, nextY, compactWidth, compactHeight);
            if (hostBounds.width() > 0.0f || hostBounds.height() > 0.0f) {
                arrangeInHost(hostBounds);
            }
        }
        takeoverDrag(pointerId, rootX, rootY);
    }

    /**
     * Выходит из компактного режима: окно становится прежнего размера, а курсор остаётся на заголовке.
     *
     * <p>Вызывается после отпускания, если окно не пристыковалось. Горизонтальная точка захвата
     * сохраняется пропорционально (курсор был в середине призрака — останется в середине окна), окно
     * при необходимости сдвигается внутрь хоста. Безопасно вызывать вне компактного режима.</p>
     */
    public void leaveCompactDrag() {
        if (!compactDrag) return;
        compactDrag = false;
        if (!open) return;
        float fraction = compactWidth > 0.0f ? dragOffsetX / compactWidth : 0.5f;
        float grabX = fraction * compactFullWidth;
        float grabY = Math.min(headerHeight, compactFullHeight) * 0.5f;
        float nextX = lastDragRootX - hostBounds.x() - grabX;
        float nextY = lastDragRootY - hostBounds.y() - grabY;
        if (constrainToHost && (hostBounds.width() > 0.0f || hostBounds.height() > 0.0f)) {
            nextX = clamp(nextX, 0.0f, Math.max(0.0f, hostBounds.width() - compactFullWidth));
            nextY = clamp(nextY, 0.0f, Math.max(0.0f, hostBounds.height() - compactFullHeight));
        }
        setWindowRect(nextX, nextY, compactFullWidth, compactFullHeight);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    /**
     * Настоящая ширина окна: во время компактного перетаскивания — размер, который вернётся после
     * отпускания, иначе текущая ширина.
     *
     * @return ширина окна без учёта компактного режима
     */
    public float fullWidth() {
        return compactDrag ? compactFullWidth : layoutBounds().width();
    }

    /**
     * Настоящая высота окна, см. {@link #fullWidth()}.
     *
     * @return высота окна без учёта компактного режима
     */
    public float fullHeight() {
        return compactDrag ? compactFullHeight : layoutBounds().height();
    }

    /**
     * Находится ли окно сейчас в компактном режиме «призрака».
     *
     * @return {@code true}, пока вытащенную вкладку несут в уменьшенном окне
     */
    public boolean compactDrag() {
        return compactDrag;
    }
    private void startDragging(PointerPressedEvent pointer) {
        dragging = true;
        dragPointerId = pointer.pointerId();
        dragOffsetX = pointer.rootX() - layoutBounds().x();
        dragOffsetY = pointer.rootY() - layoutBounds().y();
        lastDragRootX = pointer.rootX();
        lastDragRootY = pointer.rootY();
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointer.pointerId(), this);
        }
        invalidate(InvalidationFlags.VISUAL);
        dispatchWindowMoveStarted();
    }

    private void updateDrag(float rootX, float rootY) {
        lastDragRootX = rootX;
        lastDragRootY = rootY;
        float hostX = hostBounds.width() > 0.0f || hostBounds.height() > 0.0f ? hostBounds.x() : layoutBounds().x() - x;
        float hostY = hostBounds.width() > 0.0f || hostBounds.height() > 0.0f ? hostBounds.y() : layoutBounds().y() - y;
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        float nextX = rootX - hostX - dragOffsetX;
        float nextY = rootY - hostY - dragOffsetY;
        if (constrainToHost) {
            nextX = clamp(nextX, 0.0f, Math.max(0.0f, hostBounds.width() - width));
            nextY = clamp(nextY, 0.0f, Math.max(0.0f, hostBounds.height() - height));
        }
        float oldX = x;
        float oldY = y;
        position(nextX, nextY);
        dispatchWindowMoved(oldX, oldY, x, y);
    }

    private void stopDragging() {
        if (!dragging) return;
        UIContext context = uiContext();
        if (context != null && dragPointerId >= 0) {
            context.releasePointer(dragPointerId, this);
        }
        dragging = false;
        dragPointerId = -1;
        DockingRoot.clearForeignWindowDropPreview(this);
        invalidate(InvalidationFlags.VISUAL);
        dispatchWindowMoveEnded();
    }

    private void startResizing(PointerPressedEvent pointer, ResizeHandle handle) {
        resizing = true;
        resizeHandle = handle == null ? ResizeHandle.NONE : handle;
        resizePointerId = pointer.pointerId();
        resizeStartRootX = pointer.rootX();
        resizeStartRootY = pointer.rootY();
        resizeStartX = x;
        resizeStartY = y;
        resizeStartWidth = Math.max(minWindowWidth, layoutBounds().width());
        resizeStartHeight = Math.max(minWindowHeight, layoutBounds().height());
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointer.pointerId(), this);
        }
        invalidate(InvalidationFlags.VISUAL);
        dispatchWindowResizeStarted();
    }

    private void updateResize(float rootX, float rootY) {
        if (!resizing || resizeHandle == ResizeHandle.NONE) return;
        float dx = rootX - resizeStartRootX;
        float dy = rootY - resizeStartRootY;
        float nextX = resizeStartX;
        float nextY = resizeStartY;
        float nextWidth = resizeStartWidth;
        float nextHeight = resizeStartHeight;

        if (resizeHandle.affectsLeft()) {
            nextX = resizeStartX + dx;
            nextWidth = resizeStartWidth - dx;
            if (nextWidth < minWindowWidth) {
                nextX -= minWindowWidth - nextWidth;
                nextWidth = minWindowWidth;
            }
        } else if (resizeHandle.affectsRight()) {
            nextWidth = resizeStartWidth + dx;
        }

        if (resizeHandle.affectsTop()) {
            nextY = resizeStartY + dy;
            nextHeight = resizeStartHeight - dy;
            if (nextHeight < minWindowHeight) {
                nextY -= minWindowHeight - nextHeight;
                nextHeight = minWindowHeight;
            }
        } else if (resizeHandle.affectsBottom()) {
            nextHeight = resizeStartHeight + dy;
        }

        nextWidth = Math.max(minWindowWidth, nextWidth);
        nextHeight = Math.max(minWindowHeight, nextHeight);

        if (constrainToHost && (hostBounds.width() > 0.0f || hostBounds.height() > 0.0f)) {
            if (resizeHandle.affectsLeft()) {
                float minX = 0.0f;
                if (nextX < minX) {
                    nextWidth += nextX - minX;
                    nextX = minX;
                }
            }
            if (resizeHandle.affectsTop()) {
                float minY = 0.0f;
                if (nextY < minY) {
                    nextHeight += nextY - minY;
                    nextY = minY;
                }
            }
            float maxWidth = Math.max(minWindowWidth, hostBounds.width() - nextX);
            float maxHeight = Math.max(minWindowHeight, hostBounds.height() - nextY);
            nextWidth = Math.min(nextWidth, maxWidth);
            nextHeight = Math.min(nextHeight, maxHeight);
        }

        float oldX = x;
        float oldY = y;
        float oldWidth = layoutBounds().width();
        float oldHeight = layoutBounds().height();
        setWindowRect(nextX, nextY, nextWidth, nextHeight);
        dispatchWindowResized(oldX, oldY, oldWidth, oldHeight, x, y, nextWidth, nextHeight);
    }

    private void stopResizing() {
        if (!resizing) return;
        UIContext context = uiContext();
        if (context != null && resizePointerId >= 0) {
            context.releasePointer(resizePointerId, this);
        }
        resizing = false;
        ResizeHandle completedHandle = resizeHandle;
        resizeHandle = ResizeHandle.NONE;
        resizePointerId = -1;
        invalidate(InvalidationFlags.VISUAL);
        dispatchWindowResizeEnded(completedHandle);
    }

    private void setWindowRect(float x, float y, float width, float height) {
        position(x, y);
        layout(style -> style.size(Math.max(minWindowWidth, width), Math.max(minWindowHeight, height)));
    }

    void setWindowManagerInternal(WindowManager windowManager) {
        if (this.windowManager == windowManager) return;
        this.windowManager = windowManager;
    }

    void setActiveInternal(boolean active) {
        if (this.active == active) return;
        this.active = active;
        invalidate(InvalidationFlags.VISUAL);
    }

    @Override
    public void dispose() {
        DockingRoot.clearForeignWindowDropPreview(this);
        super.dispose();
    }

    WindowActivatedEvent dispatchWindowActivated(WindowWidget previousWindow) {
        WindowActivatedEvent event = new WindowActivatedEvent(this, previousWindow);
        dispatchWindowEvent(event);
        return event;
    }

    WindowDeactivatedEvent dispatchWindowDeactivated(WindowWidget nextWindow) {
        WindowDeactivatedEvent event = new WindowDeactivatedEvent(this, nextWindow);
        dispatchWindowEvent(event);
        return event;
    }

    ModalOpenedEvent dispatchModalOpened(int stackDepth) {
        ModalOpenedEvent event = new ModalOpenedEvent(this, stackDepth);
        dispatchWindowEvent(event);
        return event;
    }

    ModalClosedEvent dispatchModalClosed(int stackDepth) {
        ModalClosedEvent event = new ModalClosedEvent(this, stackDepth);
        dispatchWindowEvent(event);
        return event;
    }

    private WindowOpenedEvent dispatchWindowOpened() {
        WindowOpenedEvent event = new WindowOpenedEvent(this);
        dispatchWindowEvent(event);
        return event;
    }

    private WindowClosedEvent dispatchWindowClosed() {
        WindowClosedEvent event = new WindowClosedEvent(this);
        dispatchWindowEvent(event);
        return event;
    }

    private WindowMoveStartedEvent dispatchWindowMoveStarted() {
        WindowMoveStartedEvent event = new WindowMoveStartedEvent(this, x, y);
        dispatchWindowEvent(event);
        return event;
    }

    private WindowMovedEvent dispatchWindowMoved(float oldX, float oldY, float newX, float newY) {
        if (oldX == newX && oldY == newY) return null;
        WindowMovedEvent event = new WindowMovedEvent(this, oldX, oldY, newX, newY);
        dispatchWindowEvent(event);
        return event;
    }

    private WindowMoveEndedEvent dispatchWindowMoveEnded() {
        WindowMoveEndedEvent event = new WindowMoveEndedEvent(this, x, y);
        dispatchWindowEvent(event);
        return event;
    }

    private WindowResizeStartedEvent dispatchWindowResizeStarted() {
        WindowResizeStartedEvent event = new WindowResizeStartedEvent(this,
                x, y, layoutBounds().width(), layoutBounds().height(), resizeHandle.publicName());
        dispatchWindowEvent(event);
        return event;
    }

    private WindowResizedEvent dispatchWindowResized(float oldX, float oldY, float oldWidth, float oldHeight,
                                                     float newX, float newY, float newWidth, float newHeight) {
        if (oldX == newX && oldY == newY && oldWidth == newWidth && oldHeight == newHeight) return null;
        WindowResizedEvent event = new WindowResizedEvent(this,
                oldX, oldY, oldWidth, oldHeight,
                newX, newY, newWidth, newHeight,
                resizeHandle.publicName());
        dispatchWindowEvent(event);
        return event;
    }

    private WindowResizeEndedEvent dispatchWindowResizeEnded(ResizeHandle completedHandle) {
        WindowResizeEndedEvent event = new WindowResizeEndedEvent(this,
                x, y, layoutBounds().width(), layoutBounds().height(),
                completedHandle == null ? "" : completedHandle.publicName());
        dispatchWindowEvent(event);
        return event;
    }

    private void dispatchWindowEvent(dev.sixik.unigui.api.event.WidgetEvent event) {
        UIContext context = uiContext();
        if (context == null) {
            emit(event);
        } else {
            context.routedEvents().dispatch(event);
        }
    }

    private boolean isInHeader(float rootX, float rootY) {
        RectView bounds = layoutBounds();
        return rootX >= bounds.x()
                && rootX <= bounds.x() + bounds.width()
                && rootY >= bounds.y()
                && rootY <= bounds.y() + Math.min(headerHeight, bounds.height());
    }

    private boolean canStartDraggingFrom(PointerPressedEvent pointer) {
        if (pointer.phase() != EventPhase.TARGET && pointer.phase() != EventPhase.BUBBLE) return false;
        if (!contains(layoutBounds(), pointer.rootX(), pointer.rootY())) return false;
        if (captionless()) {
            // Окно-хост таскают за пустую часть полосы вкладок (или за единственную вкладку).
            return dockingRoot.windowDragZoneAt(pointer.rootX(), pointer.rootY());
        }
        if (isInHeader(pointer.rootX(), pointer.rootY())) return true;
        return !routeContainsInputWidget(pointer.target(), pointer.rootX(), pointer.rootY());
    }

    private boolean effectiveDraggable() {
        return draggable && !(modal && fixedModal);
    }

    private boolean effectiveResizable() {
        return resizable && !(modal && fixedModal);
    }

    private boolean routeContainsInputWidget(Widget target, float rootX, float rootY) {
        Widget current = target;
        while (current != null && current != this) {
            if (isInputWidget(current, rootX, rootY)) return true;
            current = current.parent();
        }
        return false;
    }

    private boolean isInputWidget(Widget widget, float rootX, float rootY) {
        if (widget == null || widget == content) return false;
        if (widget instanceof TextWidget && !(widget instanceof TextInput)) return false;
        if (widget.focusable()) return true;
        MouseCursor cursor = widget.mouseCursorAt(rootX - widget.layoutBounds().x(), rootY - widget.layoutBounds().y());
        if (cursor != MouseCursor.DEFAULT) return true;
        return widget instanceof ScrollView
                || widget instanceof ScrollBar
                || widget instanceof Slider
                || widget instanceof ComboBox
                || widget instanceof DatePicker
                || widget instanceof ColorPicker
                || widget instanceof TreeView
                || widget instanceof VirtualTableView
                || widget instanceof ContextMenu
                || widget instanceof Popup
                || widget instanceof WindowWidget;
    }

    private static boolean contains(RectView bounds, float rootX, float rootY) {
        return bounds != null
                && rootX >= bounds.x()
                && rootX <= bounds.x() + bounds.width()
                && rootY >= bounds.y()
                && rootY <= bounds.y() + bounds.height();
    }

    private ResizeHandle resizeHandle(float rootX, float rootY) {
        if (!effectiveResizable() || !open) return ResizeHandle.NONE;
        RectView bounds = layoutBounds();
        if (bounds.width() <= 0.0f || bounds.height() <= 0.0f) return ResizeHandle.NONE;
        boolean left = rootX >= bounds.x() && rootX < bounds.x() + RESIZE_HANDLE_SIZE;
        boolean right = rootX >= bounds.x() + bounds.width() - RESIZE_HANDLE_SIZE && rootX <= bounds.x() + bounds.width();
        boolean top = rootY >= bounds.y() && rootY < bounds.y() + RESIZE_HANDLE_SIZE;
        boolean bottom = rootY >= bounds.y() + bounds.height() - RESIZE_HANDLE_SIZE && rootY <= bounds.y() + bounds.height();
        if (gripEnabled() && inGrip(rootX, rootY)) return ResizeHandle.BOTTOM_RIGHT;
        if (left && top) return ResizeHandle.TOP_LEFT;
        if (right && top) return ResizeHandle.TOP_RIGHT;
        if (left && bottom) return ResizeHandle.BOTTOM_LEFT;
        if (right && bottom) return ResizeHandle.BOTTOM_RIGHT;
        if (left) return ResizeHandle.LEFT;
        if (right) return ResizeHandle.RIGHT;
        if (top) return ResizeHandle.TOP;
        if (bottom) return ResizeHandle.BOTTOM;
        return ResizeHandle.NONE;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum ResizeHandle {
        NONE,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        private boolean affectsLeft() {
            return this == LEFT || this == TOP_LEFT || this == BOTTOM_LEFT;
        }

        private boolean affectsRight() {
            return this == RIGHT || this == TOP_RIGHT || this == BOTTOM_RIGHT;
        }

        private boolean affectsTop() {
            return this == TOP || this == TOP_LEFT || this == TOP_RIGHT;
        }

        private boolean affectsBottom() {
            return this == BOTTOM || this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
        }

        private String publicName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}

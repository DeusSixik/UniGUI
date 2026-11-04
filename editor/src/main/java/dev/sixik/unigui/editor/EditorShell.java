package dev.sixik.unigui.editor;

import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.docking.DockArea;
import dev.sixik.unigui.widgets.docking.DockLayoutSnapshot;
import dev.sixik.unigui.widgets.docking.DockPane;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextArea;
import dev.sixik.unigui.widgets.interaction.TextInput;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Каркас редактора: строка меню, панель инструментов и область докинга, собранные из панелей, команд и раскладок.
 *
 * <p>Каркас не знает, что именно редактируется. UI-редактор, редактор графа и редактор стилей отличаются набором
 * панелей, документов и команд, а окно вокруг них одно и то же. Меню строятся из {@link EditorCommands}, меню Window —
 * из зарегистрированных панелей, вкладки — из {@link EditorPanel}.</p>
 *
 * <pre>{@code
 * EditorShell shell = EditorShell.builder()
 *         .title("My editor")
 *         .panel(hierarchyPanel).panel(inspectorPanel).panel(viewportPanel)
 *         .layout(EditorLayout.named("Default").left(0.2f, "hierarchy").right(0.25f, "inspector")
 *                 .center("viewport").build())
 *         .standardCommands()
 *         .build();
 * Widget root = shell.root();
 * }</pre>
 */
public final class EditorShell {
    private static final String WINDOW_MENU = "Window";

    private final EditorContext context;
    private final EditorTheme theme;
    private final Map<String, EditorPanel> panels;
    private final Map<String, DockPane> panes = new LinkedHashMap<>();
    private final List<EditorLayout> layouts;
    private final DockingRoot dock = new DockingRoot();
    private final OverlayLayer overlay = new OverlayLayer();
    private final Widget root;
    private String currentLayout = "";

    private EditorShell(Builder builder) {
        this.context = builder.context;
        this.theme = builder.theme;
        this.panels = new LinkedHashMap<>(builder.panels);
        this.layouts = List.copyOf(builder.layouts);

        // Порядок меню в строке: объявленные явно, затем меню команд в порядке регистрации, Window всегда последним.
        for (String menu : builder.menus) context.commands().declareMenu(menu);
        if (builder.standardCommands) registerStandardCommands();
        for (EditorCommand command : builder.commands) context.commands().register(command);
        registerWindowCommands();

        // Панелям нужен слой оверлея для меню и диалогов; они берут его из контекста при создании.
        if (context.service(OverlayLayer.class).isEmpty()) context.provide(OverlayLayer.class, overlay);

        for (EditorPanel panel : panels.values()) {
            Widget content = panel.create(context);
            DockPane pane = panel.slot() == PanelSlot.CENTER
                    ? DockPane.document(panel.id(), panel.title(), content)
                    : DockPane.tool(panel.id(), panel.title(), content);
            pane.icon(panel.icon());
            pane.closable(panel.closable());
            panes.put(panel.id(), pane);
        }

        dock.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));
        theme.styleDock(dock);
        if (!layouts.isEmpty()) applyLayout(layouts.get(0).name());

        this.root = assemble(builder);
    }

    /** @return новый билдер */
    public static Builder builder() {
        return new Builder();
    }

    // ------------------------------------------------------------------ доступ

    /** @return корневой виджет редактора: вставляется в экран */
    public Widget root() {
        return root;
    }

    /** @return общие службы редактора */
    public EditorContext context() {
        return context;
    }

    /** @return тема каркаса */
    public EditorTheme theme() {
        return theme;
    }

    /** @return область докинга */
    public DockingRoot dock() {
        return dock;
    }

    /** @return слой для выпадающих меню и плавающих окон */
    public OverlayLayer overlay() {
        return overlay;
    }

    /** @return зарегистрированные раскладки */
    public List<EditorLayout> layouts() {
        return layouts;
    }

    /** @return имя применённой раскладки или пустая строка */
    public String currentLayout() {
        return currentLayout;
    }

    /**
     * @param id идентификатор панели
     * @return панель или пусто
     */
    public Optional<EditorPanel> panel(String id) {
        return Optional.ofNullable(panels.get(id));
    }

    /** @return все панели в порядке регистрации */
    public List<EditorPanel> panels() {
        return List.copyOf(panels.values());
    }

    // ------------------------------------------------------------------ раскладки и панели

    /**
     * Применяет раскладку. Панели, которых в ней нет, закрываются; открыть их можно через меню Window.
     *
     * @param name имя раскладки
     * @throws IllegalArgumentException если такой раскладки нет
     */
    public void applyLayout(String name) {
        EditorLayout layout = layouts.stream().filter(candidate -> candidate.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown layout '" + name + "'. Known: "
                        + layouts.stream().map(EditorLayout::name).toList()));
        DockLayoutSnapshot snapshot = layout.snapshot();
        for (String id : referencedPanes(snapshot.root())) {
            if (!panes.containsKey(id)) {
                throw new IllegalArgumentException("Layout '" + name + "' refers to unknown panel '" + id + "'");
            }
        }
        dock.restoreLayout(snapshot, panes);
        currentLayout = layout.name();
    }

    /**
     * @param id идентификатор панели
     * @return {@code true}, если панель сейчас открыта
     */
    public boolean isOpen(String id) {
        return dock.findPane(id) != null;
    }

    /**
     * Открывает панель или выбирает её вкладку, если она уже открыта. Панель попадает в свою область:
     * к панелям той же области — вкладкой, иначе рядом с центральной.
     *
     * @param id идентификатор панели
     * @throws IllegalArgumentException если такой панели нет
     */
    public void openPanel(String id) {
        EditorPanel panel = panels.get(id);
        DockPane pane = panes.get(id);
        if (panel == null) throw new IllegalArgumentException("Unknown panel '" + id + "'");
        if (isOpen(id)) {
            dock.selectPane(id);
            return;
        }
        String sibling = firstOpen(panel.slot());
        if (sibling != null) {
            dock.tabPane(sibling, pane);
        } else {
            String anchor = panel.slot() == PanelSlot.CENTER ? firstOpenAny() : firstOpen(PanelSlot.CENTER);
            if (anchor == null) anchor = firstOpenAny();
            if (anchor == null) {
                dock.addPane(pane);
            } else {
                float share = Math.max(0.05f, Math.min(0.95f, panel.share()));
                switch (panel.slot()) {
                    case LEFT -> dock.splitPane(anchor, DockArea.LEFT, pane, share);
                    case RIGHT -> dock.splitPane(anchor, DockArea.RIGHT, pane, complement(share));
                    case BOTTOM -> dock.splitPane(anchor, DockArea.BOTTOM, pane, complement(share));
                    default -> dock.tabPane(anchor, pane);
                }
            }
        }
        dock.selectPane(id);
    }

    /**
     * Закрывает панель, если её можно закрыть.
     *
     * @param id идентификатор панели
     */
    public void closePanel(String id) {
        EditorPanel panel = panels.get(id);
        if (panel == null || !panel.closable() || !isOpen(id)) return;
        dock.manager().closePane(id);
    }

    private String firstOpen(PanelSlot slot) {
        for (EditorPanel panel : panels.values()) {
            if (panel.slot() == slot && isOpen(panel.id())) return panel.id();
        }
        return null;
    }

    private String firstOpenAny() {
        for (EditorPanel panel : panels.values()) if (isOpen(panel.id())) return panel.id();
        return null;
    }

    private static float complement(float share) {
        return java.math.BigDecimal.ONE.subtract(new java.math.BigDecimal(Float.toString(share))).floatValue();
    }

    private static List<String> referencedPanes(DockLayoutSnapshot.Node node) {
        List<String> ids = new ArrayList<>();
        if (node == null) return ids;
        ids.addAll(node.paneIds());
        ids.addAll(referencedPanes(node.first()));
        ids.addAll(referencedPanes(node.second()));
        return ids;
    }

    // ------------------------------------------------------------------ клавиши

    /**
     * Выполняет команду, привязанную к нажатому сочетанию. Когда фокус в поле ввода, срабатывают только
     * сочетания, которые поле не использует само (см. {@link EditorShortcut#worksInTextFields()}).
     *
     * @param keyCode   код клавиши
     * @param modifiers модификаторы
     * @param target    виджет, получивший нажатие
     * @return {@code true}, если команда нашлась, была доступна и выполнена
     */
    public boolean handleKey(int keyCode, int modifiers, Widget target) {
        boolean typing = target instanceof TextInput || target instanceof TextArea;
        for (EditorCommand command : context.commands().all()) {
            EditorShortcut binding = command.binding();
            if (binding == null || !binding.matches(keyCode, modifiers)) continue;
            if (typing && !binding.worksInTextFields()) continue;
            return command.run(context);
        }
        return false;
    }

    // ------------------------------------------------------------------ меню

    /**
     * Строит выпадающее меню из команд. Доступность пунктов вычисляется в момент построения.
     *
     * @param name имя меню верхнего уровня
     * @return меню, не добавленное ни в какой слой
     */
    public ContextMenu buildMenu(String name) {
        ContextMenu menu = new ContextMenu();
        theme.styleMenu(menu);
        float width = theme.menuWidth();
        for (EditorCommand command : context.commands().menu(name)) {
            theme.addMenuItem(menu, command.title(), command.shortcut(), command.enabled(context), width,
                    () -> command.run(context));
        }
        return menu;
    }

    /**
     * Открывает меню под якорем и добавляет его в слой оверлея.
     *
     * @param name   имя меню
     * @param anchor границы кнопки, под которой оно открывается
     */
    public void openMenu(String name, RectView anchor) {
        ContextMenu menu = buildMenu(name);
        overlay.addOverlay(menu);
        overlay.applyQueuedMutations();
        menu.openAt(anchor.x(), anchor.y() + anchor.height() + 2.0f);
    }

    private void registerWindowCommands() {
        context.commands().declareMenu(WINDOW_MENU);
        int order = 0;
        for (EditorPanel panel : panels.values()) {
            String id = panel.id();
            context.commands().register(EditorCommand.builder("window.open." + id)
                    .title(panel.title()).menu(WINDOW_MENU).order(order++)
                    .action(ignored -> openPanel(id)).build());
        }
    }

    private void registerStandardCommands() {
        EditorCommands commands = context.commands();
        commands.register(EditorCommand.builder("file.save").title("Save").menu("File").order(100)
                .shortcut("Ctrl+S")
                .enabledWhen(ctx -> ctx.document().map(doc -> doc.canSave() && doc.dirty()).orElse(false))
                .action(ctx -> ctx.document().ifPresent(EditorDocument::save)).build());
        commands.register(EditorCommand.builder("edit.undo").title("Undo").menu("Edit").order(0)
                .shortcut("Ctrl+Z")
                .enabledWhen(ctx -> ctx.document().map(EditorDocument::canUndo).orElse(false))
                .action(ctx -> ctx.document().ifPresent(EditorDocument::undo)).build());
        commands.register(EditorCommand.builder("edit.redo").title("Redo").menu("Edit").order(1)
                .shortcut("Ctrl+Y")
                .enabledWhen(ctx -> ctx.document().map(EditorDocument::canRedo).orElse(false))
                .action(ctx -> ctx.document().ifPresent(EditorDocument::redo)).build());
    }

    // ------------------------------------------------------------------ сборка виджетов

    private Widget assemble(Builder builder) {
        StackPanel viewport = new StackPanel();
        viewport.layout(style -> style.sizePercent(100.0f, 100.0f).overflow(Overflow.HIDDEN));
        // Нажатия клавиш всплывают от сфокусированного виджета к корню окна редактора.
        viewport.on(KeyPressedEvent.TYPE, event -> {
            if (handleKey(event.keyCode(), event.modifiers(), event.target())) event.cancel();
        });

        Box background = new Box();
        background.themeEnabled(false);
        background.backgroundVisible(true);
        background.background().set(theme.workspaceBackground());
        background.borderVisible(false);
        background.layout(style -> style.sizePercent(100.0f, 100.0f));
        viewport.addChild(background);

        overlay.layout(style -> style.sizePercent(100.0f, 100.0f));

        VBox column = new VBox();
        column.spacing(0.0f);
        column.layout(style -> style.sizePercent(100.0f, 100.0f));
        column.addChild(menuBar(builder));
        column.addChild(theme.separator());
        if (builder.hasToolbar()) {
            column.addChild(toolbar(builder));
            column.addChild(theme.separator());
        }
        column.addChild(dock);
        column.applyQueuedMutations();

        overlay.content(column);
        overlay.applyQueuedMutations();
        viewport.addChild(overlay);
        viewport.applyQueuedMutations();
        return viewport;
    }

    private HBox menuBar(Builder builder) {
        HBox bar = new HBox();
        bar.spacing(8.0f);
        bar.layout(style -> style.widthPercent(100.0f).height(theme.menuBarHeight())
                .padding(8.0f, 0.0f, 8.0f, 0.0f).alignItems(Align.CENTER).flexGrow(0.0f).flexShrink(0.0f));
        for (Function<EditorShell, Widget> leading : builder.leading) bar.addChild(leading.apply(this));
        for (String name : context.commands().menus()) {
            Button button = theme.menuButton(name);
            button.onClick(event -> openMenu(name, button.layoutBounds()));
            bar.addChild(button);
        }
        if (!builder.title.isEmpty()) bar.addChild(theme.title(builder.title));
        bar.addChild(spacer());
        for (Function<EditorShell, Widget> trailing : builder.trailing) bar.addChild(trailing.apply(this));
        bar.applyQueuedMutations();
        return bar;
    }

    private HBox toolbar(Builder builder) {
        HBox bar = new HBox();
        bar.spacing(8.0f);
        bar.layout(style -> style.widthPercent(100.0f).height(theme.toolbarHeight())
                .padding(8.0f, 0.0f, 8.0f, 0.0f).alignItems(Align.CENTER).flexGrow(0.0f).flexShrink(0.0f));
        for (Function<EditorShell, Widget> factory : builder.toolbar.get(ToolbarSection.LEFT)) {
            bar.addChild(factory.apply(this));
        }
        bar.addChild(spacer());
        for (Function<EditorShell, Widget> factory : builder.toolbar.get(ToolbarSection.CENTER)) {
            bar.addChild(factory.apply(this));
        }
        bar.addChild(spacer());
        for (Function<EditorShell, Widget> factory : builder.toolbar.get(ToolbarSection.RIGHT)) {
            bar.addChild(factory.apply(this));
        }
        bar.applyQueuedMutations();
        return bar;
    }

    private static Box spacer() {
        Box spacer = new Box();
        spacer.themeEnabled(false);
        spacer.layout(style -> style.flexGrow(1.0f).height(1.0f));
        return spacer;
    }

    // ------------------------------------------------------------------ билдер

    /** Билдер {@link EditorShell}. */
    public static final class Builder {
        private EditorContext context = new EditorContext();
        private EditorTheme theme = EditorTheme.dark();
        private String title = "";
        private boolean standardCommands;
        private final Map<String, EditorPanel> panels = new LinkedHashMap<>();
        private final List<EditorLayout> layouts = new ArrayList<>();
        private final List<String> menus = new ArrayList<>();
        private final List<EditorCommand> commands = new ArrayList<>();
        private final List<Function<EditorShell, Widget>> leading = new ArrayList<>();
        private final List<Function<EditorShell, Widget>> trailing = new ArrayList<>();
        private final Map<ToolbarSection, List<Function<EditorShell, Widget>>> toolbar =
                new EnumMap<>(ToolbarSection.class);

        private Builder() {
            for (ToolbarSection section : ToolbarSection.values()) toolbar.put(section, new ArrayList<>());
        }

        /**
         * @param value контекст; по умолчанию создаётся новый
         * @return этот билдер
         */
        public Builder context(EditorContext value) {
            this.context = java.util.Objects.requireNonNull(value, "context");
            return this;
        }

        /**
         * @param value тема каркаса
         * @return этот билдер
         */
        public Builder theme(EditorTheme value) {
            this.theme = java.util.Objects.requireNonNull(value, "theme");
            return this;
        }

        /**
         * @param value заголовок в строке меню
         * @return этот билдер
         */
        public Builder title(String value) {
            this.title = value == null ? "" : value;
            return this;
        }

        /**
         * Добавляет панель.
         *
         * @param panel панель
         * @return этот билдер
         * @throws IllegalArgumentException если идентификатор уже занят
         */
        public Builder panel(EditorPanel panel) {
            if (panels.putIfAbsent(panel.id(), panel) != null) {
                throw new IllegalArgumentException("Duplicate panel id: " + panel.id());
            }
            return this;
        }

        /**
         * Добавляет раскладку; первая добавленная применяется при сборке.
         *
         * @param layout раскладка
         * @return этот билдер
         */
        public Builder layout(EditorLayout layout) {
            for (EditorLayout existing : layouts) {
                if (existing.name().equals(layout.name())) {
                    throw new IllegalArgumentException("Duplicate layout: " + layout.name());
                }
            }
            layouts.add(layout);
            return this;
        }

        /**
         * Добавляет команду; она регистрируется в контексте при сборке.
         *
         * @param command команда
         * @return этот билдер
         */
        public Builder command(EditorCommand command) {
            commands.add(java.util.Objects.requireNonNull(command, "command"));
            return this;
        }

        /**
         * Объявляет меню верхнего уровня; порядок вызовов — порядок в строке меню. Меню без команд тоже показывается.
         *
         * @param names имена меню
         * @return этот билдер
         */
        public Builder menus(String... names) {
            menus.addAll(List.of(names));
            return this;
        }

        /**
         * Добавляет стандартные команды Save, Undo, Redo, работающие с текущим документом контекста.
         *
         * @return этот билдер
         */
        public Builder standardCommands() {
            this.standardCommands = true;
            return this;
        }

        /**
         * Добавляет виджет в начало строки меню (логотип).
         *
         * @param factory создаёт виджет
         * @return этот билдер
         */
        public Builder menuBarLeading(Function<EditorShell, Widget> factory) {
            leading.add(factory);
            return this;
        }

        /**
         * Добавляет виджет в конец строки меню (кнопка закрытия).
         *
         * @param factory создаёт виджет
         * @return этот билдер
         */
        public Builder menuBarTrailing(Function<EditorShell, Widget> factory) {
            trailing.add(factory);
            return this;
        }

        /**
         * Добавляет виджет на панель инструментов. Панель показывается, если в неё добавлен хотя бы один виджет.
         *
         * @param section часть панели
         * @param factory создаёт виджет
         * @return этот билдер
         */
        public Builder toolbar(ToolbarSection section, Function<EditorShell, Widget> factory) {
            toolbar.get(section).add(factory);
            return this;
        }

        /** @return готовый каркас */
        public EditorShell build() {
            return new EditorShell(this);
        }

        private boolean hasToolbar() {
            return toolbar.values().stream().anyMatch(list -> !list.isEmpty());
        }
    }
}

package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.dnd.DropController;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.SessionChange;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.editor.style.ToolButton;
import dev.sixik.unigui.xaml.preview.ComponentPreview;
import dev.sixik.unigui.xaml.preview.Preview;

import java.util.List;
import java.util.Optional;

/**
 * Живой предпросмотр документа. Загрузку, «последнее удачное дерево» и соответствие узел↔виджет ведёт
 * {@link Preview} из ядра (для документа компонента — {@link ComponentPreview}); панель только показывает его,
 * рисует рамку вокруг выбранного узла и превращает щелчок в выбор узла.
 */
final class ViewportPanel extends SessionPanel {
    private static final MutableColor OUTLINE = EditorStyles.TAB_ACCENT;
    private static final MutableColor HOVER_OUTLINE = MutableColor.rgba255(120, 170, 255, 170);
    private static final MutableColor ERROR = EditorStyles.ERROR_RED;
    private static final float GRID_STEP = 18.0f;
    private static final float HANDLE = 5.0f;
    private static final float STAGE_MARGIN = 18.0f;
    private static final MutableColor SHEET_SHADOW = MutableColor.rgba255(0, 0, 0, 46);

    /** Что именно показывает панель: экран или компонент. Различие скрыто за одним набором операций. */
    private interface Shown extends AutoCloseable {
        Preview.Result show(UiDocument document);

        Widget root();

        Optional<Widget> widgetOf(UiNode node);

        Optional<UiNode> pick(UiDocument document, float x, float y);

        boolean component();

        @Override
        void close();
    }

    private final LoadOptions previewOptions;
    private final ComponentView componentView;
    private final StackPanel holder = new StackPanel();
    private final Label status = new Label("");
    private Shown preview;
    private Widget shown;

    ViewportPanel(LoadOptions previewOptions, ComponentView componentView) {
        this.previewOptions = previewOptions;
        this.componentView = componentView;
    }

    @Override
    public String id() {
        return UiEditor.VIEWPORT;
    }

    @Override
    public String title() {
        return "Viewport";
    }

    @Override
    public dev.sixik.unigui.api.render.UiIcon icon() {
        return dev.sixik.unigui.editor.style.EditorIcons.VIEWPORT;
    }

    @Override
    public PanelSlot slot() {
        return PanelSlot.CENTER;
    }

    /** Форматы экрана: размер в «пикселях интерфейса», на котором вёрстка проверяется. */
    private static final List<String> SCREENS = List.of("Free", "640x360", "854x480", "1280x720", "1920x1080", "2560x1440");
    private static final List<String> ZOOMS = List.of("Fit", "50%", "100%", "200%");
    private static final MutableColor FRAME_BORDER = EditorStyles.BORDER_BUTTON;
    private static final MutableColor STAGE = EditorStyles.BG_STAGE;

    private final Box stage = new Box();
    private final Box frame = new Box();
    private final StackPanel frameContent = new StackPanel();
    private final VBox frameColumn = new VBox();
    private final Label info = new Label("");
    private final ToolButton play = new ToolButton(EditorIcons.PLAY, "Play", ToolButton.Variant.RUN);
    private final ComboBox screenChoice = new ComboBox();
    private final ComboBox zoomChoice = new ComboBox();
    private CanvasWidget outlineLayer;
    private final StackPanel playHolder = new StackPanel();
    private Object playCopy;
    private Widget playRootWidget;
    private float screenWidth;
    private float screenHeight;
    /** 0 — вписать в панель; иначе фиксированный масштаб. */
    private float zoom;
    private boolean playing;

    @Override
    protected Widget build(EditorContext context) {
        VBox root = new VBox();
        root.layout(style -> style.sizePercent(100.0f, 100.0f));
        holder.layout(style -> style.sizePercent(100.0f, 100.0f));

        CanvasWidget outline = new CanvasWidget();
        outlineLayer = outline;
        outline.layout(style -> style.sizePercent(100.0f, 100.0f));
        // Слой выбора лежит над предпросмотром и принимает все щелчки: предпросмотр остаётся «чертежом», а выбор
        // узла идёт по месту щелчка. В режиме Play слой скрыт, и щелчки доходят до живого интерфейса.
        outline.focusable(true); // щелчок по предпросмотру даёт окну редактора фокус, и горячие клавиши работают
        outline.on(PointerPressedEvent.TYPE, event -> {
            pick(event.rootX(), event.rootY());
            beginMove(event.rootX(), event.rootY());
        });
        outline.on(dev.sixik.unigui.api.event.PointerReleasedEvent.TYPE, event -> endMove());
        outline.on(dev.sixik.unigui.api.event.PointerMovedEvent.TYPE, event -> {
            if (moving) moveTo(event.rootX(), event.rootY());
            else hoverAt(event.rootX(), event.rootY());
        });
        outline.on(dev.sixik.unigui.api.event.PointerExitedEvent.TYPE, event -> hover().set(null));
        hover().subscribe(outline::redraw);
        outline.onDraw(draw -> {
            applyView();
            RectView border = onScreen(frame.layoutBounds());
            draw.addRect(border.x(), border.y(), border.width(), border.height(), FRAME_BORDER, 1.0f);
            Widget hovered = hoveredWidget();
            if (hovered != null && hovered != selectedWidget()) {
                RectView box = onScreen(hovered.layoutBounds());
                draw.addRect(box.x(), box.y(), box.width(), box.height(), HOVER_OUTLINE, 1.0f);
            }
            Widget target = selectedWidget();
            if (target == null) return;
            drawSelection(EditorStyles.scope(draw, outline), onScreen(target.layoutBounds()));
        });

        // Сюда бросают типы из палитры и узлы из иерархии: цель — самый глубокий узел под указателем.
        DropController.attach(outline).accepts(DropActions.ACCEPTED).onDrop(event -> dropAt(event.payload(), event.rootX(), event.rootY()));

        status.color(ERROR);
        status.layout(style -> style.flexGrow(0.0f));
        status.visibility(Visibility.COLLAPSED);

        // Рамка экрана фиксированного размера; предпросмотр лежит внутри неё и раскладывается по её размеру.
        frame.themeEnabled(false);
        frame.borderVisible(false);
        frame.backgroundVisible(true);
        frame.background().set(EditorStyles.BG_WINDOW);
        frameContent.layout(style -> style.sizePercent(100.0f, 100.0f));
        playHolder.layout(style -> style.sizePercent(100.0f, 100.0f));
        playHolder.visibility(Visibility.COLLAPSED);
        frameContent.addChild(holder);
        frameContent.addChild(playHolder);
        frameContent.addChild(status);
        frameContent.applyQueuedMutations();
        frame.addChild(frameContent);
        frame.applyQueuedMutations();
        stage.themeEnabled(false);
        stage.borderVisible(false);
        stage.backgroundVisible(true);
        stage.background().set(STAGE);
        stage.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f)
                .overflow(dev.sixik.unigui.api.layout.Overflow.HIDDEN));
        StackPanel stageContent = new StackPanel();
        stageContent.layout(style -> style.sizePercent(100.0f, 100.0f));
        // Рамка лежит в строке flex: стек растягивает детей по своему размеру, а строка уважает фиксированную ширину.
        HBox frameRow = new HBox();
        frameRow.layout(style -> style.sizePercent(100.0f, 100.0f).alignItems(dev.sixik.unigui.api.layout.Align.START)
                .padding(STAGE_MARGIN, STAGE_MARGIN + 4.0f, STAGE_MARGIN, STAGE_MARGIN));
        // Фиксированная высота держится в колонке (главная ось), фиксированная ширина — в строке: поперёк оси flex
        // ребёнка ужимает до контейнера, вдоль оси размер сохраняется.
        frameColumn.layout(style -> style.sizePercent(100.0f, 100.0f).flexShrink(0.0f));
        frameColumn.addChild(frame);
        frameColumn.applyQueuedMutations();
        frameRow.addChild(frameColumn);
        frameRow.applyQueuedMutations();
        // Точечная сетка и тень лежат под рамкой экрана: рамка читается как лист на рабочем столе.
        CanvasWidget backdrop = new CanvasWidget();
        backdrop.layout(style -> style.sizePercent(100.0f, 100.0f));
        backdrop.onDraw(draw -> drawBackdrop(EditorStyles.scope(draw, backdrop)));
        stageContent.addChild(backdrop);
        stageContent.addChild(frameRow);
        // Порядок важен для щелчков: самым верхним должен быть слой выбора.
        stageContent.addChild(outline);
        stageContent.applyQueuedMutations();
        stage.addChild(stageContent);
        stage.applyQueuedMutations();
        applyScreen();

        root.addChild(toolbar());
        root.addChild(stage);
        root.applyQueuedMutations();
        return root;
    }

    /** Панель над предпросмотром: запуск, формат экрана, масштаб и состояние. */
    private Widget toolbar() {
        HBox bar = new HBox();
        bar.spacing(6.0f);
        bar.layout(style -> style.widthPercent(100.0f).height(26.0f).flexShrink(0.0f).padding(4.0f, 3.0f, 6.0f, 3.0f)
                .alignItems(dev.sixik.unigui.api.layout.Align.CENTER));
        play.id("viewport.play");
        play.layout(style -> style.width(60.0f));
        play.onClick(click -> playing(!playing));

        screenChoice.id("viewport.screen");
        screenChoice.items(SCREENS);
        screenChoice.silentSelectedIndex(0);
        EditorStyles.restyle(screenChoice);
        screenChoice.layout(style -> style.width(84.0f).flexShrink(0.0f));
        screenChoice.onSelectionChanged(event -> {
            if (screenChoice.selectedIndex() >= 0) screen(SCREENS.get(screenChoice.selectedIndex()));
        });
        zoomChoice.id("viewport.zoom");
        zoomChoice.items(ZOOMS);
        zoomChoice.silentSelectedIndex(0);
        EditorStyles.restyle(zoomChoice);
        zoomChoice.layout(style -> style.width(62.0f).flexShrink(0.0f));
        zoomChoice.onSelectionChanged(event -> {
            int index = zoomChoice.selectedIndex();
            if (index >= 0) zoom(index == 0 ? 0.0f : Float.parseFloat(ZOOMS.get(index).replace("%", "")) / 100.0f);
        });

        info.color(EditorStyles.TEXT_MUTED);
        info.textAlignment(dev.sixik.unigui.api.layout.Alignment.END);
        info.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).height(EditorStyles.ROW_HEIGHT));
        bar.addChild(play);
        bar.addChild(EditorChrome.divider());
        bar.addChild(screenChoice);
        bar.addChild(zoomChoice);
        bar.addChild(info);
        bar.applyQueuedMutations();
        return bar;
    }

    /** Точечная сетка рабочего стола и мягкая тень под рамкой экрана. */
    private void drawBackdrop(dev.sixik.unigui.api.render.DrawScope draw) {
        RectView area = stage.layoutBounds();
        for (float y = area.y() + GRID_STEP * 0.5f; y < area.y() + area.height(); y += GRID_STEP) {
            for (float x = area.x() + GRID_STEP * 0.5f; x < area.x() + area.width(); x += GRID_STEP) {
                draw.addRectFilled(x, y, 1.0f, 1.0f, EditorStyles.GRID_DOT);
            }
        }
        RectView sheet = onScreen(frame.layoutBounds());
        if (sheet.width() <= 0.0f) return;
        for (float spread = 6.0f; spread >= 2.0f; spread -= 2.0f) {
            draw.addRectFilled(sheet.x() - spread, sheet.y() - spread + 2.0f, sheet.width() + spread * 2.0f,
                    sheet.height() + spread * 2.0f, spread + 2.0f, SHEET_SHADOW);
        }
    }

    /** Рамка выбора: контур, восемь маркеров и ярлык с типом узла над виджетом. */
    private void drawSelection(dev.sixik.unigui.api.render.DrawScope draw, RectView bounds) {
        float x = bounds.x();
        float y = bounds.y();
        float w = bounds.width();
        float h = bounds.height();
        draw.addRect(x, y, w, h, OUTLINE, 1.5f);
        float[] stops = {0.0f, 0.5f, 1.0f};
        for (float fy : stops) {
            for (float fx : stops) {
                if (fx == 0.5f && fy == 0.5f) continue;
                if (fy == 0.5f && h < HANDLE * 4.0f) continue; // у низкого виджета боковые маркеры слиплись бы с угловыми
                float cx = x + w * fx - HANDLE * 0.5f;
                float cy = y + h * fy - HANDLE * 0.5f;
                draw.addRectFilled(cx, cy, HANDLE, HANDLE, EditorStyles.TEXT_BOLD);
                draw.addRect(cx, cy, HANDLE, HANDLE, OUTLINE, 1.0f);
            }
        }
        String tag = session().flatMap(EditorSession::selected).map(ViewportPanel::tagOf).orElse("");
        if (tag.isEmpty()) return;
        dev.sixik.unigui.api.text.RichText text = dev.sixik.unigui.api.text.RichText.plain(tag);
        float width = dev.sixik.unigui.impl.text.TextEngine.measureLineWidth(draw.context(), text) + 10.0f;
        float height = 14.0f;
        float textHeight = dev.sixik.unigui.impl.text.TextEngine.measureTextHeight(draw.context(), text);
        float tagY = y - height - 3.0f;
        if (tagY < stage.layoutBounds().y()) tagY = y + 4.0f;
        draw.addRectFilled(x, tagY, width, height, 2.0f, OUTLINE);
        draw.text(text, x + 5.0f, tagY + (height - textHeight) * 0.5f, width, textHeight,
                dev.sixik.unigui.api.render.Paint.fill(EditorStyles.TEXT_BOLD));
    }

    private static String tagOf(UiNode node) {
        String id = node.attributeText("id").orElse("");
        return id.isBlank() ? node.type().local() : node.type().local() + " #" + id;
    }

    /**
     * Выбирает формат экрана.
     *
     * @param name {@code Free} или {@code ШxВ}
     */
    void screen(String name) {
        if (name.equals("Free")) {
            screenWidth = 0.0f;
            screenHeight = 0.0f;
        } else {
            String[] parts = name.split("x");
            screenWidth = Float.parseFloat(parts[0]);
            screenHeight = Float.parseFloat(parts[1]);
        }
        applyScreen();
    }

    /**
     * Задаёт масштаб предпросмотра.
     *
     * @param value {@code 0} — вписать в панель, иначе множитель
     */
    void zoom(float value) {
        zoom = value;
        applyView();
    }

    /**
     * Включает режим запуска: интерфейс живёт (кнопки нажимаются, граф исполняется), выбор и рамки скрыты.
     *
     * @param value {@code true} — Play
     */
    void playing(boolean value) {
        if (value == playing) return;
        playing = value;
        play.text(value ? "Stop" : "Play");
        play.icon(value ? EditorIcons.STOP : EditorIcons.PLAY);
        play.variant(value ? ToolButton.Variant.SOLID : ToolButton.Variant.RUN);
        if (outlineLayer != null) outlineLayer.visibility(value ? Visibility.COLLAPSED : Visibility.VISIBLE);
        hover().set(null);
        if (value) startPlayCopy();
        else stopPlayCopy();
        holder.visibility(value ? Visibility.COLLAPSED : Visibility.VISIBLE);
        playHolder.visibility(value ? Visibility.VISIBLE : Visibility.COLLAPSED);
        info.text(value ? "Playing (a copy: your document is not affected)" : "");
        // Отправка событий и журнал живут на панели запуска: при старте она сразу на виду.
        if (value && context() != null) {
            for (var command : context().commands().all()) {
                if (command.id().equals("window.open." + RunPanel.ID)) command.run(context());
            }
        }
    }

    /**
     * Создаёт отдельный экземпляр интерфейса из текущего текста документа. Копия живёт сама по себе: клики, события и
     * граф работают в ней, редактируемый предпросмотр и документ не меняются, а правки документа в копию не попадают,
     * пока Play не перезапустят.
     */
    private void startPlayCopy() {
        stopPlayCopy();
        EditorSession session = session().orElse(null);
        if (session == null) return;
        if (isComponent(session.document())) {
            // Копия делит с предпросмотром точки останова и примерные значения, но живёт своим экземпляром.
            ComponentPreview design = componentView.preview().orElse(null);
            ComponentPreview copy = design == null ? new ComponentPreview(session.schema(), previewOptions)
                    : new ComponentPreview(session.schema(), previewOptions, design.debugger());
            if (design != null) design.samples().forEach(copy::sample);
            copy.onEvent(line -> componentView.changed());
            copy.trace().onChange(componentView::changed);
            Preview.Result result = copy.show(session.document());
            playCopy = copy;
            playRootWidget = copy.root().orElse(null);
            if (!result.shown()) status.text("Play: " + result.message());
            componentView.running(copy);
        } else {
            Preview copy = new Preview(session.schema(), previewOptions);
            Preview.Result result = copy.show(session.document());
            playCopy = copy;
            playRootWidget = copy.root().orElse(null);
            if (!result.shown()) status.text("Play: " + result.message());
        }
        if (playRootWidget != null) {
            playHolder.addChild(playRootWidget);
            playHolder.applyQueuedMutations();
        }
    }

    private void stopPlayCopy() {
        if (playCopy instanceof Preview screen) {
            screen.close();
        } else if (playCopy instanceof ComponentPreview component) {
            // Остановленный на точке запуск не должен ждать продолжения от закрытой копии.
            if (component.debugger().paused().isPresent()) component.debugger().resume();
            component.close();
            componentView.running(null);
        }
        playCopy = null;
        playRootWidget = null;
        playHolder.clearChildren();
        playHolder.applyQueuedMutations();
    }

    /** @return корень интерфейса копии, запущенной по Play, или {@code null}; нужно тестам */
    Widget playRoot() {
        return playRootWidget;
    }

    /** @return {@code true} в режиме Play */
    boolean playing() {
        return playing;
    }

    private void applyScreen() {
        if (screenWidth <= 0.0f) {
            frame.layout(style -> style.sizePercent(100.0f, 100.0f).minSize(0.0f, 0.0f).flexShrink(1.0f));
            frameContent.layout(style -> style.sizePercent(100.0f, 100.0f));
            holder.layout(style -> style.sizePercent(100.0f, 100.0f));
            frameColumn.layout(style -> style.sizePercent(100.0f, 100.0f).minWidth(0.0f).flexShrink(1.0f));
        } else {
            float w = screenWidth;
            float h = screenHeight;
            // Проценты внутри рамки считаются от размера содержимого, поэтому внутренним слоям размер задан явно.
            frameColumn.layout(style -> style.width(w).minWidth(w).heightPercent(100.0f).flexShrink(0.0f)
                    .flexGrow(0.0f));
            frameContent.layout(style -> style.width(w).height(h));
            holder.layout(style -> style.width(w).height(h));
            frame.layout(style -> style.width(w).height(h).minSize(w, h).flexShrink(0.0f).flexGrow(0.0f)
                    .alignSelf(dev.sixik.unigui.api.layout.Align.START));
        }
        applyView();
    }

    /** Ставит рамку экрана по центру панели с нужным масштабом; в режиме Free масштаб единичный. */
    void applyView() {
        if (screenWidth <= 0.0f) {
            frame.transform().scale().set(1.0f, 1.0f);
            frame.transform().position().set(0.0f, 0.0f);
            if (!playing) info.text("");
            return;
        }
        RectView area = stage.layoutBounds();
        RectView at = frame.layoutBounds();
        float z = zoom > 0.0f ? zoom
                : Math.max(0.1f, Math.min(4.0f, Math.min((area.width() - 24.0f) / screenWidth,
                (area.height() - 24.0f) / screenHeight)));
        if (area.width() <= 0.0f) z = zoom > 0.0f ? zoom : 1.0f;
        frame.transform().scale().set(z, z);
        frame.transform().position().set(area.x() + (area.width() - screenWidth * z) / 2.0f - at.x(),
                area.y() + (area.height() - screenHeight * z) / 2.0f - at.y());
        if (!playing) info.text((int) screenWidth + "x" + (int) screenHeight + " @ " + Math.round(z * 100.0f) + "%");
    }

    /** Точка окна → точка в раскладке предпросмотра (с учётом масштаба и сдвига рамки). */
    private dev.sixik.unigui.api.math.TransformGeometry.Point toLayout(float x, float y) {
        return dev.sixik.unigui.api.math.TransformGeometry.inverseTransformPoint(x, y, frame.layoutBounds(),
                frame.transform());
    }

    /** Прямоугольник раскладки → прямоугольник на экране окна. */
    RectView onScreen(RectView bounds) {
        return dev.sixik.unigui.api.math.TransformGeometry.transformedBounds(bounds, frame.layoutBounds(),
                frame.transform());
    }

    @Override
    protected void bound(EditorSession next) {
        closePreview();
        shown = null;
        holder.clearChildren();
        holder.applyQueuedMutations();
        reload();
    }

    @Override
    protected void changed(EditorSession current, SessionChange change) {
        switch (change.kind()) {
            case DOCUMENT_CHANGED, REVERTED -> reload();
            case SELECTION_CHANGED -> {
                // Рамка выбора рисуется колбэком холста; без явного запроса холст перерисуется только от чужого события.
                if (outlineLayer != null) outlineLayer.redraw();
            }
            default -> { }
        }
    }

    /** Бросок на предпросмотр: в узел под указателем, а если там пусто, в корень документа. */
    void dropAt(dev.sixik.unigui.widgets.dnd.DragPayload payload, float x, float y) {
        EditorSession session = session().orElse(null);
        if (session == null || preview == null) return;
        var at = toLayout(x, y);
        UiNode target = preview.pick(session.document(), at.x(), at.y()).orElse(session.document().root());
        DropActions.drop(session, payload, target);
    }

    // ------------------------------------------------------------------ свободное перемещение

    private boolean moving;
    private float moveX;
    private float moveY;
    private float carryX;
    private float carryY;

    /** Нажатие на выбранный виджет в StackPanel начинает перемещение: меняются только отступы, якорь остаётся. */
    void beginMove(float x, float y) {
        EditorSession session = session().orElse(null);
        UiNode node = session == null ? null : session.selected().orElse(null);
        moving = node != null && Anchors.supported(session, node);
        if (!moving) return;
        var at = toLayout(x, y);
        moveX = at.x();
        moveY = at.y();
        carryX = 0.0f;
        carryY = 0.0f;
    }

    /** Двигает выбранный виджет вслед за указателем; дробные остатки копятся, чтобы мелкие сдвиги не терялись. */
    void moveTo(float x, float y) {
        EditorSession session = session().orElse(null);
        UiNode node = session == null ? null : session.selected().orElse(null);
        if (node == null) {
            moving = false;
            return;
        }
        var at = toLayout(x, y);
        carryX += at.x() - moveX;
        carryY += at.y() - moveY;
        moveX = at.x();
        moveY = at.y();
        float dx = Math.round(carryX);
        float dy = Math.round(carryY);
        if (dx == 0.0f && dy == 0.0f) return;
        carryX -= dx;
        carryY -= dy;
        Anchors.move(session, node, dx, dy);
    }

    void endMove() {
        moving = false;
    }

    private void hoverAt(float x, float y) {
        EditorSession session = session().orElse(null);
        if (session == null || preview == null) return;
        var at = toLayout(x, y);
        hover().set(preview.pick(session.document(), at.x(), at.y()).orElse(null));
    }

    /** @return виджет предпросмотра под наведённым узлом (из любой панели) или {@code null} */
    Widget hoveredWidget() {
        UiNode node = hover().node();
        return preview == null || node == null ? null : preview.widgetOf(node).orElse(null);
    }

    /** Выбирает узел под точкой щелчка. */
    void pick(float x, float y) {
        EditorSession session = session().orElse(null);
        if (session == null || preview == null) return;
        var at = toLayout(x, y);
        preview.pick(session.document(), at.x(), at.y()).ifPresent(session::select);
    }

    /** @return виджет предпросмотра, созданный из выбранного узла, или {@code null} */
    Widget selectedWidget() {
        if (preview == null) return null;
        return session().flatMap(EditorSession::selected).flatMap(preview::widgetOf).orElse(null);
    }

    /** @return текущий корень предпросмотра или {@code null} */
    Widget previewRoot() {
        return preview == null ? null : preview.root();
    }

    private void closePreview() {
        if (preview != null) preview.close();
        preview = null;
        componentView.preview(null);
    }

    /** Предпросмотр нужного вида для документа; прежний закрывается, если вид сменился. */
    private void ensurePreview(EditorSession session) {
        boolean wantsComponent = isComponent(session.document());
        if (preview != null && preview.component() == wantsComponent) return;
        closePreview();
        shown = null;
        holder.clearChildren();
        holder.applyQueuedMutations();
        if (wantsComponent) {
            ComponentPreview component = new ComponentPreview(session.schema(), previewOptions);
            component.onEvent(line -> componentView.changed());
            component.trace().onChange(componentView::changed);
            component.debugger().onChange(componentView::changed);
            preview = new Shown() {
                @Override
                public Preview.Result show(UiDocument document) {
                    return component.show(document);
                }

                @Override
                public Widget root() {
                    return component.root().orElse(null);
                }

                @Override
                public Optional<Widget> widgetOf(UiNode node) {
                    return component.widgetOf(node);
                }

                @Override
                public Optional<UiNode> pick(UiDocument document, float x, float y) {
                    return component.pick(document, x, y);
                }

                @Override
                public boolean component() {
                    return true;
                }

                @Override
                public void close() {
                    component.close();
                }
            };
            componentView.preview(component);
        } else {
            Preview screen = new Preview(session.schema(), previewOptions);
            preview = new Shown() {
                @Override
                public Preview.Result show(UiDocument document) {
                    return screen.show(document);
                }

                @Override
                public Widget root() {
                    return screen.root().orElse(null);
                }

                @Override
                public Optional<Widget> widgetOf(UiNode node) {
                    return screen.widgetOf(node);
                }

                @Override
                public Optional<UiNode> pick(UiDocument document, float x, float y) {
                    return screen.pick(document, x, y);
                }

                @Override
                public boolean component() {
                    return false;
                }

                @Override
                public void close() {
                    screen.close();
                }
            };
        }
    }

    private static boolean isComponent(UiDocument document) {
        return document.root().type().local().equals("Component");
    }

    private void reload() {
        EditorSession session = session().orElse(null);
        if (session == null) {
            status.text("");
            return;
        }
        ensurePreview(session);
        Preview.Result result = preview.show(session.document());
        status.text(result.shown() ? "" : "Preview: " + result.message());
        // Пустая подпись лежала бы поверх предпросмотра и перехватывала щелчки в режиме Play.
        status.visibility(result.shown() ? Visibility.COLLAPSED : Visibility.VISIBLE);
        Widget root = preview.root();
        if (root != shown) {
            holder.clearChildren();
            if (root != null) holder.addChild(root);
            holder.applyQueuedMutations();
            shown = root;
        }
        componentView.changed();
    }
}

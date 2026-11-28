package dev.sixik.unigui.demo.ui.scene;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.scene.ScenePlaybackController;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.impl.text.DefaultFontRegistry;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.rendering.RenderToTexture;
import dev.sixik.unigui.widgets.rendering.Scene3D;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

/**
 * Показывает 3D-туториал с общей шкалой, пояснениями и перемоткой по абсолютному времени.
 */
public final class SceneTutorialPlayer extends LinearBox {
    /**
     * Акцент элементов управления и разметки текущего этапа.
     */
    private static final MutableColor ACCENT = MutableColor.fromArgb(0xFFFFC879);
    /**
     * Цвет заголовков и основных элементов управления.
     */
    private static final MutableColor TEXT = MutableColor.fromArgb(0xFFF1F5FB);
    /**
     * Цвет пояснений и счётчика времени.
     */
    private static final MutableColor MUTED = MutableColor.fromArgb(0xFFAEBDC9);
    /**
     * Цвет поверхности карточки и панели воспроизведения.
     */
    private static final MutableColor PANEL = MutableColor.fromArgb(0xFF141E2A);
    /**
     * Цвет границы панелей и обычных кнопок.
     */
    private static final MutableColor BORDER = MutableColor.fromArgb(0xFF334558);
    /**
     * Базовая заливка кнопок.
     */
    private static final MutableColor BUTTON = MutableColor.fromArgb(0xFF202F3E);
    /**
     * Заливка кнопки под указателем.
     */
    private static final MutableColor HOVER = MutableColor.fromArgb(0xFF304658);
    /**
     * Заливка текущей точки на шкале.
     */
    private static final MutableColor ACTIVE = MutableColor.fromArgb(0xFF493421);
    /**
     * Шрифт пояснений и элементов управления из ресурсов UniGUI.
     */
    static final FontFace UI_FONT = loadFont("play_regular.ttf");
    /**
     * Шрифт заголовков из ресурсов UniGUI.
     */
    private static final FontFace TITLE_FONT = loadFont("Cuprum-Bold.ttf");

    /**
     * Сцена, длительность и этапы текущего туториала.
     */
    private final Sequence sequence;
    /**
     * Playback time and transport state for the current sequence.
     */
    private final ScenePlaybackController playback;
    /**
     * Заголовок карточки текущего этапа.
     */
    private final Label caption = new Label();
    /**
     * Дополнительное пояснение к выбранной точке.
     */
    private final Label details = new Label();
    /**
     * Номер текущего этапа и общее количество этапов.
     */
    private final Label stepLabel = new Label();
    /**
     * Иконка предмета или блока, о котором идёт речь.
     */
    private final StackPanel icon = new StackPanel();
    /**
     * Прокручиваемая область карточки для коротких окон.
     */
    private final ScrollView info = new ScrollView();
    /**
     * Ползунок времени в секундах от нуля до конца сценария.
     */
    private final Slider timeSlider;
    /**
     * Текущее и полное время в секундах.
     */
    private final Label timeLabel = new Label();
    /**
     * Управление воспроизведением и паузой.
     */
    private final Button playPause = button("Pause");
    /**
     * Кнопки перехода, индекс совпадает с номером этапа.
     */
    private final Button[] markers;
    /**
     * Индекс текущего этапа или -1 до первого кадра.
     */
    private int checkpoint = -1;
    /**
     * Index of the material effect selected for the final checkpoint, with zero as the default.
     */
    private int selectedEffect;
    /**
     * Округлённое время, уже показанное в счётчике, в десятых долях секунды.
     */
    private int displayedTenths = -1;

    /**
     * Привязывает элементы управления к одному времени воспроизведения.
     *
     * @param sequence подготовленный сценарий с собственным виджетом сцены.
     */
    public SceneTutorialPlayer(Sequence sequence) {
        super(Orientation.VERTICAL);
        this.sequence = sequence;
        playback = new ScenePlaybackController(sequence.duration(), this::sampleFrame);
        timeSlider = new Slider().range(0, sequence.duration());
        markers = new Button[sequence.checkpoints().size()];
        spacing(9);
        background(MutableColor.fromArgb(0xFF0B111A));
        layout(style -> style.padding(16, 12).alignItems(Align.STRETCH));

        addChild(titleBar());

        Scene3D viewport = sequence.viewport();
        viewport.id("tutorial-scene");
        viewport.updateMode(RenderToTexture.UpdateMode.ON_CHANGE).resolutionScale(1.5f);
        viewport.clearColor().set(0, 0, 0, 0);
        addChild(workspace(viewport));
        addChild(controls());
        playback.replay();
    }

    /**
     * Places the optional effect selector at the right of the tutorial title.
     *
     * @return the fixed-height header row
     */
    private HBox titleBar() {
        HBox bar = new HBox();
        bar.spacing(8);
        bar.layout(style -> style.height(26).flexShrink(0).alignItems(Align.CENTER));
        Label title = new Label(RichText.of(sequence.title(), TITLE_FONT, 23, TEXT));
        title.id("tutorial-title");
        title.textAlignment(Alignment.START);
        title.layout(style -> style.width(0).height(26).flexGrow(1).overflow(Overflow.HIDDEN));
        bar.addChild(title);
        if (sequence.effects() != null) bar.addChild(effectSelector(sequence.effects()));
        return bar;
    }

    /**
     * Builds the effect dropdown using the same palette and typography as the playback controls.
     *
     * @param picker options for the final checkpoint
     * @return a selector with the default preset selected
     */
    private ComboBox effectSelector(EffectPicker picker) {
        ComboBox selector = new ComboBox().dropDownSameWidth().optionRowHeight(21)
                .optionHoverBackground(HOVER);
        selector.id("tutorial-effect");
        selector.layout(style -> style.size(104, 24).flexShrink(0));
        selector.richItems(picker.options().stream().map(option -> RichText.of(option.label(), UI_FONT, 11, TEXT)).toList());
        selector.silentSelectedIndex(0);
        Button header = selector.headerButton();
        header.themeEnabled(false);
        header.layout(style -> style.height(24));
        header.background(BUTTON).border(BORDER).radius(4).textPaddingX(8)
                .hoverBackground(HOVER).pressedBackground(ACTIVE).textColor(TEXT);
        var popup = selector.optionsHost();
        popup.themeEnabled(false);
        popup.background(PANEL).border(BORDER).radius(6);
        selector.onSelectionChanged(event -> selectEffect(picker, selector.selectedIndex()));
        return selector;
    }

    /**
     * Applies the selected effect and previews it at the current time or the configured preview frame.
     *
     * @param picker options for the final checkpoint
     * @param index  index of the new option
     */
    private void selectEffect(EffectPicker picker, int index) {
        selectedEffect = index;
        picker.options().get(index).select().run();
        float effectStart = sequence.checkpoints().get(markers.length - 1).seconds();
        float time = playback.timeSeconds();
        if (time <= effectStart || time >= sequence.duration()) pauseAt(picker.previewSeconds());
        else playback.seek(time);
        updateCheckpointText(sequence.checkpoints().get(checkpoint));
    }

    /**
     * Размещает сцену рядом с карточкой текущего этапа.
     *
     * @param viewport интерактивный виджет сцены.
     * @return рабочая область между заголовком и шкалой времени.
     */
    private HBox workspace(Scene3D viewport) {
        HBox workspace = new HBox();
        workspace.spacing(9);
        workspace.layout(style -> style.height(0).flexGrow(1).alignItems(Align.STRETCH));
        var stage = SceneTutorialStage.wrap(viewport, sequence.backdrop());
        stage.layout(style -> style.width(0).flexGrow(1));
        workspace.addChild(stage);

        VBox card = new VBox();
        card.background(PANEL).border(BORDER).radius(8);
        card.layout(style -> style.widthPercent(34).minWidth(122).maxWidth(220).padding(11, 9));
        info.layout(style -> style.height(0).flexGrow(1).overflowX(Overflow.HIDDEN).overflowY(Overflow.AUTO));
        info.scrollbarSize(3);
        card.addChild(info);

        VBox content = new VBox();
        content.spacing(7);
        HBox header = new HBox();
        header.spacing(8);
        header.layout(style -> style.height(28).flexShrink(0).alignItems(Align.CENTER));
        icon.layout(style -> style.size(28, 28).flexShrink(0));
        stepLabel.id("tutorial-step");
        header.addChild(icon);
        header.addChild(stepLabel);
        content.addChild(header);

        caption.id("tutorial-caption");
        caption.textAlignment(Alignment.START).wrap(true);
        details.id("tutorial-details");
        details.textAlignment(Alignment.START).wrap(true);
        content.addChild(caption);
        content.addChild(details);
        info.content(content);
        workspace.addChild(card);
        return workspace;
    }

    /**
     * Собирает шкалу с чекпоинтами и строку воспроизведения.
     *
     * @return панель, закреплённая под рабочей областью.
     */
    private VBox controls() {
        VBox controls = new VBox();
        controls.background(PANEL).border(BORDER).radius(8);
        controls.layout(style -> style.height(62).flexShrink(0).padding(10, 6));

        playPause.id("tutorial-play");
        playPause.layout(style -> style.size(66, 19).flexShrink(0));
        playPause.onClick(event -> {
            if (playback.playing()) playback.pause();
            else if (playback.timeSeconds() == sequence.duration()) playback.replay();
            else playback.play();
            updatePlaybackButton();
        });
        Button replay = button("Replay");
        replay.id("tutorial-replay");
        replay.layout(style -> style.size(57, 19).flexShrink(0));
        replay.onClick(event -> playback.replay());

        timeSlider.id("tutorial-time");
        timeSlider.themeEnabled(false);
        timeSlider.trackColor().set(BORDER);
        timeSlider.fillColor().set(ACCENT);
        timeSlider.knobColor().set(TEXT);
        timeSlider.style(style -> style.put(StyleKeys.TRACK_THICKNESS, 3f).put(StyleKeys.THUMB_THICKNESS, 5f));
        timeSlider.on(PointerPressedEvent.TYPE, event -> {
            if (event.phase() == EventPhase.TARGET && event.button() == PointerButton.PRIMARY) {
                playback.pause();
                updatePlaybackButton();
            }
        });
        timeSlider.onValueChanged(event -> {
            if (event.newValue() != playback.timeSeconds()) {
                pauseAt(event.newValue());
            }
        });

        CheckpointTimeline timeline = new CheckpointTimeline();
        timeline.layout(style -> style.height(31).flexShrink(0));
        controls.addChild(timeline);

        HBox transport = new HBox();
        transport.spacing(6);
        transport.layout(style -> style.height(19).flexShrink(0).alignItems(Align.CENTER));
        timeLabel.textAlignment(Alignment.END);
        timeLabel.layout(style -> style.width(0).flexGrow(1));
        transport.addChild(playPause);
        transport.addChild(replay);
        transport.addChild(timeLabel);
        controls.addChild(transport);
        return controls;
    }

    /**
     * Создаёт кнопку с общим оформлением управления туториалом.
     *
     * @param text видимая подпись кнопки.
     * @return настроенная кнопка без обработчика.
     */
    private static Button button(String text) {
        Button button = new Button(RichText.of(text, UI_FONT, 11));
        button.themeEnabled(false);
        return button.background(BUTTON).border(BORDER).radius(4).textPadding(3, 1)
                .hoverBackground(HOVER).pressedBackground(ACTIVE).textColor(TEXT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void tick(FrameContext frame) {
        if (visible() && frame != null) playback.update(frame.deltaSeconds());
        super.tick(frame);
    }

    /**
     * Updates the button label to match the controller's playback state.
     */
    private void updatePlaybackButton() {
        String label = playback.playing() ? "Pause" : "Play";
        if (!label.equals(playPause.text())) playPause.richText(RichText.of(label, UI_FONT, 11));
    }

    /**
     * Переходит к кадру и оставляет его на паузе для чтения пояснения.
     *
     * @param seconds время в пределах сценария.
     */
    private void pauseAt(float seconds) {
        playback.pause();
        playback.seek(seconds);
    }

    /**
     * Вычисляет кадр и обновляет отметку времени и карточку этапа.
     *
     * @param seconds время в диапазоне от нуля до длительности сценария.
     */
    private void sampleFrame(float seconds) {
        updatePlaybackButton();
        sequence.frame().sample(seconds);
        int next = markers.length - 1;
        while (next > 0 && seconds < sequence.checkpoints().get(next).seconds()) next--;
        showCheckpoint(next);

        timeSlider.value(seconds);
        int tenths = Math.round(seconds * 10);
        if (displayedTenths != tenths) {
            displayedTenths = tenths;
            timeLabel.richText(RichText.of(tenths / 10 + "." + tenths % 10 + " / " + sequence.duration() + " s",
                    UI_FONT, 11, MUTED));
        }
    }

    /**
     * Синхронизирует карточку, иконку и выделение маркера при смене этапа.
     *
     * @param next индекс этапа, соответствующего текущему времени.
     */
    private void showCheckpoint(int next) {
        if (checkpoint == next) return;
        if (checkpoint >= 0) styleMarker(markers[checkpoint], false);
        checkpoint = next;
        styleMarker(markers[next], true);
        Checkpoint point = sequence.checkpoints().get(next);
        stepLabel.richText(RichText.of("STEP " + (next + 1) + " / " + markers.length, UI_FONT, 10, ACCENT));
        updateCheckpointText(point);
        Box preview = point.icon().get();
        preview.background(BUTTON).border(BORDER).radius(6);
        preview.layout(style -> style.sizePercent(100, 100));
        icon.clearChildren();
        icon.addChild(preview);
        info.scrollTo(0, 0);
    }

    /**
     * Shows the current checkpoint text, or the selected effect explanation at the final checkpoint.
     *
     * @param point active checkpoint
     */
    private void updateCheckpointText(Checkpoint point) {
        EffectOption option = sequence.effects() != null && checkpoint == markers.length - 1
                ? sequence.effects().options().get(selectedEffect) : null;
        caption.richText(RichText.of(option == null ? point.title() : option.title(), TITLE_FONT, 16, TEXT));
        details.richText(RichText.of(option == null ? point.details() : option.details(), UI_FONT, 11, MUTED));
    }

    /**
     * Настраивает цвета кнопки этапа.
     *
     * @param marker   кнопка этапа на шкале.
     * @param selected true для текущего этапа.
     */
    private static void styleMarker(Button marker, boolean selected) {
        marker.background(selected ? ACTIVE : BUTTON).borderColor(selected ? ACCENT : BORDER)
                .textColor(selected ? ACCENT : TEXT).hoverBackground(selected ? ACTIVE : HOVER);
    }

    /**
     * Загружает шрифт из ресурсов мода для этого экрана.
     *
     * @param name имя файла в каталоге шрифтов UniGUI.
     * @return загруженный шрифт или null для стандартного шрифта бэкенда.
     */
    private static FontFace loadFont(String name) {
        try (InputStream source = SceneTutorialPlayer.class.getResourceAsStream("/assets/unigui/fonts/" + name)) {
            return source == null ? null : new DefaultFontRegistry().register(name, source);
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Сценарий для общего проигрывателя туториалов.
     *
     * @param title       заголовок экрана.
     * @param viewport    виджет с уже созданными узлами.
     * @param duration    положительная целая длительность в секундах.
     * @param checkpoints этапы по возрастанию времени. Первый начинается в нуле.
     * @param frame       восстанавливает состояние сцены для любого момента времени.
     * @param backdrop    подложка вьюпорта или null для стандартной сетки.
     * @param effects     optional material effect picker for the final checkpoint, or null
     */
    public record Sequence(String title, Scene3D viewport, int duration, List<Checkpoint> checkpoints,
                           FrameSampler frame, WidgetBase backdrop, EffectPicker effects) {
        /**
         * Creates a scenario with a custom backdrop and no material effect picker.
         *
         * @param title       screen title
         * @param viewport    scene widget with its nodes
         * @param duration    positive duration in seconds
         * @param checkpoints checkpoints in time order, starting at zero
         * @param frame       sampler for an absolute time
         * @param backdrop    viewport background, or null for the default grid
         */
        public Sequence(String title, Scene3D viewport, int duration, List<Checkpoint> checkpoints,
                        FrameSampler frame, WidgetBase backdrop) {
            this(title, viewport, duration, checkpoints, frame, backdrop, null);
        }

        /**
         * Создаёт сценарий со стандартной подложкой.
         *
         * @param title       заголовок экрана.
         * @param viewport    виджет с уже созданными узлами.
         * @param duration    положительная длительность в секундах.
         * @param checkpoints этапы по возрастанию времени, начиная с нуля.
         * @param frame       восстанавливает состояние сцены для выбранного момента.
         */
        public Sequence(String title, Scene3D viewport, int duration, List<Checkpoint> checkpoints, FrameSampler frame) {
            this(title, viewport, duration, checkpoints, frame, null);
        }
    }

    /**
     * Lists selectable effects for the final checkpoint of a tutorial.
     *
     * @param previewSeconds time in seconds within the final checkpoint for an immediate preview
     * @param options        choices in display order, with the default first
     */
    public record EffectPicker(float previewSeconds, List<EffectOption> options) {
    }

    /**
     * Describes one entry in the tutorial's material effect selector.
     *
     * @param label   dropdown label
     * @param title   final checkpoint heading
     * @param details final checkpoint description
     * @param select  changes the effect used by the scenario's frame sampler
     */
    public record EffectOption(String label, String title, String details, Runnable select) {
    }

    /**
     * Информация одной точки перехода.
     *
     * @param seconds время от начала урока в секундах.
     * @param label   короткая подпись кнопки.
     * @param title   заголовок карточки.
     * @param details пояснение к действию.
     * @param icon    фабрика виджета для иконки, который принадлежит карточке этапа.
     */
    public record Checkpoint(float seconds, String label, String title, String details,
                             Supplier<? extends Box> icon) {
    }

    /**
     * Применяет кадр к содержимому сценария.
     */
    @FunctionalInterface
    public interface FrameSampler {
        /**
         * Позиционирует узлы по абсолютному времени независимо от предыдущего кадра.
         *
         * @param seconds время в секундах от начала сценария.
         */
        void sample(float seconds);
    }

    /**
     * Размещает кнопки этапов над теми же отметками времени, которые использует ползунок.
     */
    private final class CheckpointTimeline extends PanelWidget {
        /**
         * Прямоугольник для размещения дочернего виджета в единицах UI.
         */
        private final MutableRect childBounds = new MutableRect();
        /**
         * Используются ли числовые кнопки для узкого окна.
         */
        private boolean compact;
        /**
         * Доля шкалы между ближайшими по времени этапами.
         */
        private final float minimumGap;

        /**
         * Добавляет ползунок и настоящие кнопки, участвующие в фокусе и обработке ввода.
         */
        private CheckpointTimeline() {
            addChild(timeSlider);
            float gap = sequence.duration();
            for (int index = 0; index < markers.length; index++) {
                Checkpoint point = sequence.checkpoints().get(index);
                if (index > 0) gap = Math.min(gap, point.seconds() - sequence.checkpoints().get(index - 1).seconds());
                Button marker = button(point.label());
                marker.richText(RichText.of(point.label(), UI_FONT, 10));
                marker.id("tutorial-checkpoint-" + (index + 1));
                marker.onClick(event -> pauseAt(point.seconds()));
                markers[index] = marker;
                addChild(marker);
            }
            minimumGap = gap / sequence.duration();
        }

        /**
         * Размещает центр кнопки в {@code trackX + trackWidth * seconds / duration}.
         * Здесь trackX и trackWidth задают положение и ширину ползунка в UI, seconds — время точки.
         * Чтобы кнопки не пересекались, их ширина ограничена {@code width * gap / (1 + gap)},
         * где width — ширина панели в UI, gap — минимальная доля между этапами.
         *
         * @param bounds прямоугольник шкалы в единицах UI.
         */
        @Override
        public void arrange(RectView bounds) {
            mutableLayoutBounds().set(bounds);
            applyQueuedMutations();
            float available = bounds.width();
            float markerWidth = Math.min(Math.min(59, available / 8), available * minimumGap / (1 + minimumGap) * 0.96f);
            float trackX = bounds.x() + markerWidth / 2;
            float trackWidth = bounds.width() - markerWidth;
            float trackY = bounds.y() + 18;
            timeSlider.arrange(childBounds.set(trackX, trackY, trackWidth, 13));
            boolean showNumbers = markerWidth < 41;
            for (int index = 0; index < markers.length; index++) {
                Checkpoint point = sequence.checkpoints().get(index);
                Button marker = markers[index];
                if (showNumbers != compact) {
                    marker.richText(RichText.of(showNumbers ? Integer.toString(index + 1) : point.label(), UI_FONT, 10));
                }
                float x = trackX + trackWidth * point.seconds() / sequence.duration() - markerWidth / 2;
                marker.arrange(childBounds.set(x, bounds.y(), markerWidth, 16));
            }
            compact = showNumbers;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void render(RenderContext context) {
            if (!visible()) return;
            float trackCenter = timeSlider.layoutBounds().y() + timeSlider.layoutBounds().height() / 2;
            for (int i = 0; i < markers.length; i++) {
                RectView button = markers[i].layoutBounds();
                float x = button.x() + button.width() / 2;
                context.addLine(x, button.bottom(), x, trackCenter, i == checkpoint ? ACCENT : BORDER, 1);
            }
            super.render(context);
        }
    }
}

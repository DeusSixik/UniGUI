package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.animation.AnimationClock;
import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.FloatInterpolator;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.backend.minecraft_impl.BlockNode;
import dev.sixik.unigui.backend.minecraft_impl.ItemNode;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClientCompat;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.backend.minecraft_impl.StructureNode;
import dev.sixik.unigui.impl.text.DefaultFontRegistry;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemPreviewWidget;
import dev.sixik.unigui.widgets.rendering.RenderToTexture;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Показывает 3D-туториал с общей шкалой, пояснениями и перемоткой по абсолютному времени. */
public final class SceneTutorialDemo extends LinearBox {
    /** Начало появления воронки в секундах. */
    private static final float HOPPER_IN = 1.5f;
    /** Момент установки воронки в секундах. */
    private static final float HOPPER_SEATED = 2.75f;
    /** Момент появления предмета над воронкой в секундах. */
    private static final float ITEM_IN = 3.75f;
    /** Момент начала падения предмета в секундах. */
    private static final float ITEM_FALL = 4.5f;
    /** Момент передачи предмета в сундук в секундах. */
    private static final float ITEM_STORED = 6.25f;
    /** Акцент элементов управления и разметки текущего этапа. */
    private static final MutableColor ACCENT = MutableColor.fromArgb(0xFFFFC879);
    /** Цвет заголовков и основных элементов управления. */
    private static final MutableColor TEXT = MutableColor.fromArgb(0xFFF1F5FB);
    /** Цвет пояснений и счётчика времени. */
    private static final MutableColor MUTED = MutableColor.fromArgb(0xFFAEBDC9);
    /** Цвет поверхности карточки и панели воспроизведения. */
    private static final MutableColor PANEL = MutableColor.fromArgb(0xFF141E2A);
    /** Цвет границы панелей и обычных кнопок. */
    private static final MutableColor BORDER = MutableColor.fromArgb(0xFF334558);
    /** Базовая заливка кнопок. */
    private static final MutableColor BUTTON = MutableColor.fromArgb(0xFF202F3E);
    /** Заливка кнопки под указателем. */
    private static final MutableColor HOVER = MutableColor.fromArgb(0xFF304658);
    /** Заливка текущей точки на шкале. */
    private static final MutableColor ACTIVE = MutableColor.fromArgb(0xFF493421);
    /** Шрифт пояснений и элементов управления из ресурсов UniGUI. */
    private static final FontFace UI_FONT = loadFont("play_regular.ttf");
    /** Шрифт заголовков из ресурсов UniGUI. */
    private static final FontFace TITLE_FONT = loadFont("Cuprum-Bold.ttf");

    /** Сцена, длительность и этапы текущего туториала. */
    private final Sequence sequence;
    /** Заголовок карточки текущего этапа. */
    private final Label caption = new Label();
    /** Дополнительное пояснение к выбранной точке. */
    private final Label details = new Label();
    /** Номер текущего этапа и общее количество этапов. */
    private final Label stepLabel = new Label();
    /** Иконка предмета или блока, о котором идёт речь. */
    private final MinecraftItemPreviewWidget icon = new MinecraftItemPreviewWidget("", ItemStack.EMPTY);
    /** Прокручиваемая область карточки для коротких окон. */
    private final ScrollView info = new ScrollView();
    /** Ползунок времени в секундах от нуля до конца сценария. */
    private final Slider timeSlider;
    /** Текущее и полное время в секундах. */
    private final Label timeLabel = new Label();
    /** Управление воспроизведением и паузой. */
    private final Button playPause = button("Пауза");
    /** Кнопки перехода, индекс совпадает с номером этапа. */
    private final Button[] markers;
    /** Индекс текущего этапа или -1 до первого кадра. */
    private int checkpoint = -1;
    /** Текущее время в секундах, включая кадр с конечным состоянием. */
    private float time;
    /** Округлённое время, уже показанное в счётчике, в десятых долях секунды. */
    private int displayedTenths = -1;
    /** Продвигается ли время вместе с UI-кадрами. */
    private boolean playing = true;

    /** Открывает туториал на клиентском потоке. */
    public static void open() {
        Minecraft.getInstance().execute(() -> MinecraftClientCompat.setScreen(
                new MinecraftWidgetScreen(Component.literal("Воронка и сундук"), new SceneTutorialDemo())));
    }

    /** Открывает сцену с печкой на клиентском потоке. */
    public static void openFurnace() {
        Minecraft.getInstance().execute(() -> {
            var sequence = SceneFurnaceTutorial.create();
            MinecraftClientCompat.setScreen(new MinecraftWidgetScreen(Component.literal(sequence.title()),
                    new SceneTutorialDemo(sequence)));
        });
    }

    /** Собирает туториал о воронке. */
    SceneTutorialDemo() {
        this(hopperSequence());
    }

    /**
     * Привязывает элементы управления к одному времени воспроизведения.
     *
     * @param sequence подготовленный сценарий с собственным виджетом сцены.
     */
    SceneTutorialDemo(Sequence sequence) {
        super(Orientation.VERTICAL);
        this.sequence = sequence;
        timeSlider = new Slider().range(0, sequence.duration());
        markers = new Button[sequence.checkpoints().size()];
        spacing(9);
        background(MutableColor.fromArgb(0xFF0B111A));
        layout(style -> style.padding(16, 12).alignItems(Align.STRETCH));

        Label title = new Label(RichText.of(sequence.title(), TITLE_FONT, 23, TEXT));
        title.textAlignment(Alignment.START);
        title.layout(style -> style.height(26).flexShrink(0));
        addChild(title);

        Scene3D viewport = sequence.viewport();
        viewport.id("tutorial-scene");
        viewport.updateMode(RenderToTexture.UpdateMode.ON_CHANGE).resolutionScale(1.5f);
        viewport.clearColor().set(0, 0, 0, 0);
        addChild(workspace(viewport));
        addChild(controls());
        sampleAt(0);
    }

    /**
     * Создаёт исходный сценарий о передаче предмета в сундук.
     *
     * @return сцена и функция, которая восстанавливает любую позу по времени.
     */
    private static Sequence hopperSequence() {
        Scene3D viewport = new Scene3D();
        viewport.camera().setOrthographic(5.4f, 0.1f, 32);
        viewport.orbit().setOrbit(new MutableVec3(0.5f, 1.76f, 0.5f), 8, 0.65, 0.47);
        BlockNode hopper = new BlockNode(Blocks.HOPPER.defaultBlockState());
        ItemNode item = new ItemNode(SceneDemoItems.ironIngot());
        item.setScale(0.58f, 0.58f, 0.58f).setRotation(new MutableQuat().setYawPitch(0.3, -0.15));
        BlockNode chest = new BlockNode(Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
        chest.setPosition(0, 1, 0);
        viewport.scene().addRoot(platform()).addRoot(chest).addRoot(hopper).addRoot(item);
        var pointer = SceneTutorialStage.focus(viewport);

        return new Sequence("Воронка и сундук", viewport, 9, List.of(
                new Checkpoint(0, "Сундук", "Поставьте сундук",
                        "Он будет принимать предметы. Оставьте сверху место для воронки.", Items.CHEST, "chest"),
                new Checkpoint(HOPPER_IN, "Воронка", "Направьте воронку",
                        "Присядьте и поставьте воронку на сундук. Её носик должен смотреть вниз.", Items.HOPPER, "hopper"),
                new Checkpoint(ITEM_IN, "Загрузка", "Загрузите воронку",
                        "Бросьте слиток сверху. Воронка подберёт его у входа.", Items.IRON_INGOT, "iron_ingot"),
                new Checkpoint(ITEM_STORED, "Готово", "Слиток в сундуке",
                        "Проверьте содержимое сундука. Сигнал редстоуна блокирует передачу.", Items.CHEST, "chest")
        ), seconds -> {
            // Для обеих высот: y = from + (to - from) * easing((seconds - start) / (end - start)).
            float placement = AnimationEasing.CUBIC_OUT.apply((seconds - HOPPER_IN) / (HOPPER_SEATED - HOPPER_IN));
            float hopperY = FloatInterpolator.LINEAR.interpolate(3.05f, 2, placement);
            hopper.setVisible(seconds >= HOPPER_IN).setPosition(0, hopperY, 0);

            float fall = AnimationEasing.QUAD_IN.apply((seconds - ITEM_FALL) / (ITEM_STORED - ITEM_FALL));
            float itemY = FloatInterpolator.LINEAR.interpolate(3.75f, 2.8f, fall);
            item.setVisible(seconds >= ITEM_IN && seconds < ITEM_STORED).setPosition(0.5f, itemY, 0.5f);

            if (seconds >= ITEM_IN && seconds < ITEM_STORED) pointer.position(1.04f, itemY + 0.06f, 0.52f);
            else if (seconds >= HOPPER_IN && seconds < ITEM_IN) pointer.position(0.57f, hopperY + 0.78f, 1.05f);
            else pointer.position(0.5f, 1.52f, 1.015f);
        });
    }

    /**
     * Строит основание под туториалом.
     *
     * @return структура с платформой размером три на три блока.
     */
    private static StructureNode platform() {
        StructureNode base = new StructureNode();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                var block = Blocks.DEEPSLATE_TILES;
                if (x == 0 && z == 0) block = Blocks.GILDED_BLACKSTONE;
                else if (x != 0 && z != 0) block = Blocks.POLISHED_DEEPSLATE;
                base.setBlock(new BlockPos(x, 0, z), block.defaultBlockState());
            }
        }
        return base;
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
        var stage = SceneTutorialStage.wrap(viewport);
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
        icon.previewSize(16).labelVisible(false);
        icon.decorations(false).background(BUTTON).border(BORDER).radius(6);
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
            if (time == sequence.duration()) sampleAt(0);
            setPlaying(!playing);
        });
        Button replay = button("Повтор");
        replay.id("tutorial-replay");
        replay.layout(style -> style.size(57, 19).flexShrink(0));
        replay.onClick(event -> {
            sampleAt(0);
            setPlaying(true);
        });

        timeSlider.id("tutorial-time");
        timeSlider.themeEnabled(false);
        timeSlider.trackColor().set(BORDER);
        timeSlider.fillColor().set(ACCENT);
        timeSlider.knobColor().set(TEXT);
        timeSlider.style(style -> style.put(StyleKeys.TRACK_THICKNESS, 3f).put(StyleKeys.THUMB_THICKNESS, 5f));
        timeSlider.on(PointerPressedEvent.TYPE, event -> {
            if (event.phase() == EventPhase.TARGET && event.button() == PointerButton.PRIMARY) setPlaying(false);
        });
        timeSlider.onValueChanged(event -> {
            if (event.newValue() != time) {
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

    /** {@inheritDoc} */
    @Override
    public void tick(FrameContext frame) {
        if (visible() && playing && frame != null) {
            float delta = AnimationClock.sanitizeDelta(frame.deltaSeconds());
            if (delta > 0) sampleAt(Math.min(sequence.duration(), time + delta));
        }
        super.tick(frame);
    }

    /**
     * Устанавливает воспроизведение и синхронизирует подпись кнопки.
     *
     * @param playing true для воспроизведения, false для паузы.
     */
    private void setPlaying(boolean playing) {
        if (this.playing == playing) return;
        this.playing = playing;
        playPause.richText(RichText.of(playing ? "Пауза" : "Играть", UI_FONT, 11));
    }

    /**
     * Переходит к кадру и оставляет его на паузе для чтения пояснения.
     *
     * @param seconds время в пределах сценария.
     */
    private void pauseAt(float seconds) {
        setPlaying(false);
        sampleAt(seconds);
    }

    /**
     * Вычисляет кадр и обновляет отметку времени и карточку этапа.
     *
     * @param seconds время в диапазоне от нуля до длительности сценария.
     */
    private void sampleAt(float seconds) {
        time = seconds;
        if (seconds == sequence.duration()) setPlaying(false);
        sequence.frame().sample(seconds);
        int next = markers.length - 1;
        while (next > 0 && seconds < sequence.checkpoints().get(next).seconds()) next--;
        showCheckpoint(next);

        timeSlider.value(seconds);
        int tenths = Math.round(seconds * 10);
        if (displayedTenths != tenths) {
            displayedTenths = tenths;
            timeLabel.richText(RichText.of(tenths / 10 + "." + tenths % 10 + " / " + sequence.duration() + " с",
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
        stepLabel.richText(RichText.of("ЭТАП " + (next + 1) + " / " + markers.length, UI_FONT, 10, ACCENT));
        caption.richText(RichText.of(point.title(), TITLE_FONT, 16, TEXT));
        details.richText(RichText.of(point.details(), UI_FONT, 11, MUTED));
        icon.stack(SceneDemoItems.stack(point.item(), point.model()));
        info.scrollTo(0, 0);
    }

    /**
     * Настраивает цвета кнопки этапа.
     *
     * @param marker кнопка этапа на шкале.
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
        try (InputStream source = SceneTutorialDemo.class.getResourceAsStream("/assets/unigui/fonts/" + name)) {
            return source == null ? null : new DefaultFontRegistry().register(name, source);
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Сценарий для общего проигрывателя туториалов.
     * @param title заголовок экрана.
     * @param viewport виджет с уже созданными узлами.
     * @param duration положительная целая длительность в секундах.
     * @param checkpoints этапы по возрастанию времени. Первый начинается в нуле.
     * @param frame восстанавливает состояние сцены для любого момента времени.
     */
    record Sequence(String title, Scene3D viewport, int duration, List<Checkpoint> checkpoints, FrameSampler frame) {}

    /**
     * Информация одной точки перехода.
     * @param seconds время от начала урока в секундах.
     * @param label короткая подпись кнопки.
     * @param title заголовок карточки.
     * @param details пояснение к действию.
     * @param item ванильный предмет для иконки.
     * @param model путь ванильной модели в 26.x.
     */
    record Checkpoint(float seconds, String label, String title, String details, Item item, String model) {}

    /** Применяет кадр к содержимому сценария. */
    @FunctionalInterface
    interface FrameSampler {
        /**
         * Позиционирует узлы по абсолютному времени независимо от предыдущего кадра.
         * @param seconds время в секундах от начала сценария.
         */
        void sample(float seconds);
    }

    /** Размещает кнопки этапов над теми же отметками времени, которые использует ползунок. */
    private final class CheckpointTimeline extends PanelWidget {
        /** Прямоугольник для размещения дочернего виджета в единицах UI. */
        private final MutableRect childBounds = new MutableRect();
        /** Используются ли числовые кнопки для узкого окна. */
        private boolean compact;
        /** Доля шкалы между ближайшими по времени этапами. */
        private final float minimumGap;

        /** Добавляет ползунок и настоящие кнопки, участвующие в фокусе и обработке ввода. */
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

        /** {@inheritDoc} */
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

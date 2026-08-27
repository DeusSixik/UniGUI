package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TestMod-окно обучения по основным игровым UI: квесты, терминал, навигация и карта.
 */
public final class TutorialScreen {
    private static final MutableColor BG = MutableColor.rgba(0.030f, 0.038f, 0.052f, 1.0f);
    private static final MutableColor PANEL = MutableColor.rgba(0.055f, 0.070f, 0.095f, 0.96f);
    private static final MutableColor PANEL_ALT = MutableColor.rgba(0.075f, 0.095f, 0.130f, 0.96f);
    private static final MutableColor PANEL_HOT = MutableColor.rgba(0.105f, 0.160f, 0.220f, 0.98f);
    private static final MutableColor BORDER = MutableColor.rgba(0.245f, 0.360f, 0.520f, 0.88f);
    private static final MutableColor BORDER_DIM = MutableColor.rgba(0.145f, 0.205f, 0.300f, 0.84f);
    private static final MutableColor ACCENT = MutableColor.rgba(0.340f, 0.790f, 1.000f, 1.0f);
    private static final MutableColor ACCENT_2 = MutableColor.rgba(1.000f, 0.620f, 0.250f, 1.0f);
    private static final MutableColor GREEN = MutableColor.rgba(0.390f, 0.910f, 0.520f, 1.0f);
    private static final MutableColor TITLE = MutableColor.rgba(0.850f, 0.940f, 1.000f, 1.0f);
    private static final MutableColor TEXT = MutableColor.rgba(0.700f, 0.780f, 0.880f, 1.0f);
    private static final MutableColor MUTED = MutableColor.rgba(0.465f, 0.550f, 0.660f, 1.0f);
    private static final MutableColor DANGER = MutableColor.rgba(1.000f, 0.360f, 0.320f, 1.0f);

    private TutorialScreen() {
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen previous = minecraft.screen;

        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        UnityLikeUIScaleProvider scale = new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 6.0f)
                .userScale(2.2f);
        context.scaleProvider(scale);

        Runnable[] close = new Runnable[1];
        Widget root = root(() -> close[0].run());
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(Component.literal("UniGUI Tutorial"), root, context) {
            @Override
            public void onClose() {
                Minecraft.getInstance().setScreen(previous);
            }
        };
        close[0] = screen::onClose;
        screen.useContextScale().scaleWithMinecraftGui(false);
        screen.useSdfDefaultFont();
        screen.renderPolicy(UiRenderPolicy.vsync());
        minecraft.setScreen(screen);
    }

    private static Widget root(Runnable closeAction) {
        StackPanel viewport = new StackPanel();
        viewport.layout(style -> style.sizePercent(100.0f, 100.0f).overflow(Overflow.HIDDEN));
        viewport.addChild(background());

        VBox shell = new VBox();
        shell.layout(style -> style.sizePercent(90.0f, 86.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .gap(12.0f)
                .padding(14.0f));

        Box headerPanel = panelBox(PANEL);
        headerPanel.layout(style -> style.height(52.0f).padding(10.0f).flexGrow(0.0f).flexShrink(0.0f));
        HBox header = new HBox();
        header.layout(style -> style.sizePercent(100.0f, 100.0f).gap(12.0f));
        Label title = label("ОБУЧЕНИЕ / БОРТОВОЙ СПРАВОЧНИК", TITLE, false);
        title.layout(style -> style.width(LayoutConstraints.AUTO).height(32.0f).flexGrow(1.0f));
        Label subtitle = label("Квесты, терминал, навигация, карта", MUTED, false);
        subtitle.layout(style -> style.width(360.0f).height(28.0f).flexGrow(0.0f).flexShrink(0.0f));
        Button close = actionButton("Закрыть", closeAction);
        close.layout(style -> style.width(112.0f).height(32.0f).flexGrow(0.0f).flexShrink(0.0f));
        header.addChild(title);
        header.addChild(subtitle);
        header.addChild(close);
        headerPanel.addChild(header);

        HBox body = new HBox();
        body.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f).gap(12.0f).flexGrow(1.0f).flexShrink(1.0f).overflow(Overflow.HIDDEN));

        Box navPanel = panelBox(PANEL);
        navPanel.layout(style -> style.width(245.0f).heightPercent(100.0f).padding(10.0f).flexGrow(0.0f).flexShrink(0.0f));
        VBox nav = new VBox();
        nav.layout(style -> style.sizePercent(100.0f, 100.0f).gap(8.0f));
        Label navTitle = label("Разделы", TITLE, false);
        navTitle.layout(style -> style.height(26.0f).flexGrow(0.0f).flexShrink(0.0f));
        nav.addChild(navTitle);
        navPanel.addChild(nav);

        VBox content = new VBox();
        content.layout(style -> style.widthPercent(100.0f).gap(14.0f).padding(14.0f, 12.0f, 42.0f, 30.0f).flexGrow(1.0f).flexShrink(1.0f).overflow(Overflow.HIDDEN));
        ScrollView contentScroll = new ScrollView(content);
        contentScroll.scrollStep(38.0f);
        contentScroll.scrollbarGap(8.0f);
        contentScroll.scrollbarTrackColor().set(0.020f, 0.027f, 0.040f, 0.90f);
        contentScroll.scrollbarThumbColor().set(0.300f, 0.620f, 0.860f, 0.95f);
        contentScroll.layout(style -> style.width(LayoutConstraints.AUTO).heightPercent(100.0f)
                .overflowX(Overflow.HIDDEN)
                .overflowY(Overflow.SCROLL)
                .flexGrow(1.0f)
                .flexShrink(1.0f));

        TutorialController controller = new TutorialController(nav, content, contentScroll, pages());
        controller.build();

        body.addChild(navPanel);
        body.addChild(contentScroll);
        shell.addChild(headerPanel);
        shell.addChild(body);
        viewport.addChild(shell);
        return new OverlayLayer(viewport);
    }

    private static Box background() {
        Box box = new Box();
        box.themeEnabled(false);
        box.backgroundVisible(true);
        box.borderVisible(false);
        box.background(BG);
        box.layout(style -> style.sizePercent(100.0f, 100.0f).align(Alignment.STRETCH, Alignment.STRETCH));
        return box;
    }

    private static Box panelBox(MutableColor color) {
        Box box = new Box();
        box.themeEnabled(false);
        box.backgroundVisible(true);
        box.borderVisible(true);
        box.radius(6.0f);
        box.borderWidth(1.0f);
        box.background(color);
        box.border(BORDER_DIM);
        return box;
    }

    private static Label label(String text, MutableColor color, boolean wrap) {
        Label label = new Label(text);
        label.color(color);
        if (wrap) {
            label.wrapText();
            label.overflowMode(TextOverflowMode.VISIBLE);
        } else {
            label.noWrap();
            label.overflowMode(TextOverflowMode.CLIP);
        }
        return label;
    }

    private static Button actionButton(String text, Runnable action) {
        Button button = new Button(text);
        button.themeEnabled(false);
        button.backgroundVisible(true);
        button.borderVisible(true);
        button.radius(4.0f);
        button.borderWidth(1.0f);
        button.background().set(PANEL_ALT);
        button.borderColor().set(BORDER);
        button.textColor().set(TITLE);
        button.textPadding(10.0f, 4.0f);
        button.interactionTransitions(true);
        button.onClick(event -> {
            if (action != null) action.run();
        });
        return button;
    }

    private static Button navButton(String text, Runnable action) {
        Button button = actionButton(text, action);
        button.layout(style -> style.height(34.0f).widthPercent(100.0f).flexGrow(0.0f).flexShrink(0.0f));
        return button;
    }

    private static Box cardBox() {
        Box card = panelBox(PANEL_ALT);
        card.layout(style -> style.widthPercent(100.0f).padding(12.0f).flexGrow(0.0f).flexShrink(0.0f));
        return card;
    }

    private static List<TutorialPage> pages() {
        return List.of(
                new TutorialPage(
                        "start",
                        "Быстрый старт",
                        "Быстрый старт",
                        "Эта страница должна встречать игрока перед первым использованием систем корабля. Здесь коротко объясняется основной цикл: взять задание, найти цель на карте, воспользоваться терминалом или навигацией и выполнить действие в мире.",
                        PreviewKind.OVERVIEW,
                        List.of(
                                new TutorialSection("Цикл игрока", List.of(
                                        "Открой журнал заданий и выбери активную цель.",
                                        "Посмотри этаж и зону на карте станции.",
                                        "Используй RetroTerminal для команд, диагностики и быстрых подсказок.",
                                        "Если цель далеко в космосе, переходи в SolarNavigation и лети к метке.")),
                                new TutorialSection("Подсказки UI", List.of(
                                        "Синие элементы обычно интерактивные: их можно нажимать или выбирать.",
                                        "Оранжевые маркеры показывают важную цель или активную задачу.",
                                        "Зелёные зоны используются для переходов между этажами и безопасных систем."))
                        ),
                        List.of()
                ),
                new TutorialPage(
                        "quests",
                        "Квесты",
                        "Квесты и зоны задания",
                        "Квесты лучше объяснять через цель, зону и условие завершения. Игроку не нужно знать внутренний id комнаты, ему нужно понимать, куда идти и что сделать.",
                        PreviewKind.QUESTS,
                        List.of(
                                new TutorialSection("Типы квестов", List.of(
                                        "Исследование: дойти до отмеченной зоны и подтвердить обнаружение.",
                                        "Ремонт: найти комнату с системой и пройти мини-игру обслуживания.",
                                        "Доставка: перенести предмет или данные из одной зоны в другую.",
                                        "Бой/опасность: зачистить комнату или отключить угрозу.")),
                                new TutorialSection("Как выполнять", List.of(
                                        "Активный квест подсвечивает связанные комнаты на карте одним цветом.",
                                        "Если цель на другом этаже, карта покажет переходы и текущий выбранный уровень.",
                                        "После выполнения условия терминал или HUD может вывести подтверждение."))
                        ),
                        List.of()
                ),
                new TutorialPage(
                        "terminal",
                        "RetroTerminal",
                        "RetroTerminal / командная строка",
                        "Терминал нужен для команд корабля, диагностики и административных действий. Он поддерживает динамический реестр команд, аргументы и автодополнение.",
                        PreviewKind.TERMINAL,
                        List.of(
                                new TutorialSection("Ввод команд", List.of(
                                        "Начни ввод с '/' чтобы увидеть список доступных команд.",
                                        "TAB выбирает текущую подсказку, мышь выбирает строку из popup-списка.",
                                        "Пробел переводит автодополнение к следующему аргументу, если предыдущая часть команды верная.")),
                                new TutorialSection("Пример", List.of(
                                        "/teleport <player> <x> <y> <z> — после выбора игрока терминал предлагает координаты.",
                                        "/scan — выводит ближайшие станции, поля астероидов и quest marker'ы.",
                                        "/status — показывает состояние корабля и систем."))
                        ),
                        List.of(new TutorialAction("Открыть терминал", RetroTerminalScreen::open))
                ),
                new TutorialPage(
                        "solar",
                        "SolarNavigation",
                        "SolarNavigation / полёт по системе",
                        "Навигация отвечает за двумерное перемещение корабля по солнечной системе внутри UI. Объекты рендерятся только в интерфейсе, без обязательного рендера в мире.",
                        PreviewKind.SOLAR,
                        List.of(
                                new TutorialSection("Управление", List.of(
                                        "W/S — тяга вперёд и назад.",
                                        "A/D — поворот корабля против/по часовой стрелке.",
                                        "Пробел — стыковка со станцией, если корабль находится поверх её спрайта.")),
                                new TutorialSection("Объекты", List.of(
                                        "Астероиды и станции могут иметь сидированный спавн: одинаковый сид даёт одинаковую карту.",
                                        "Квестовые станции могут отображаться стрелкой за краем карты.",
                                        "Поверх станции можно выводить сидированное имя из пула названий."))
                        ),
                        List.of()
                ),
                new TutorialPage(
                        "map",
                        "Карта станции",
                        "Map / многоуровневая карта",
                        "Карта станции показывает прямоугольные сегменты комнат из снапшота генератора. У комнаты есть bounds, этажи и connections, а зоны заданий задаются через маркеры.",
                        PreviewKind.MAP,
                        List.of(
                                new TutorialSection("Что видно", List.of(
                                        "Текущий корабль/стартовая комната отмечается подписью 'Ты здесь'.",
                                        "Переходы между этажами подсвечиваются зелёным цветом.",
                                        "Зоны задания получают цвет и описание из API маркеров.")),
                                new TutorialSection("Как читать", List.of(
                                        "Выбери уровень через DropBox справа.",
                                        "Комнаты на других этажах не смешиваются с текущим этажом.",
                                        "Legend строится из зарегистрированных маркеров, а не из захардкоженных типов комнат."))
                        ),
                        List.of(new TutorialAction("Открыть карту", LevelMapScreen::open))
                ),
                new TutorialPage(
                        "minigames",
                        "Мини-игры",
                        "Мини-игры обслуживания",
                        "Мини-игры используются как короткие интерактивные проверки: вставить вилку, затянуть гайку, включить питание или выполнить ремонт.",
                        PreviewKind.MINIGAMES,
                        List.of(
                                new TutorialSection("Розетка", List.of(
                                        "Зажми вилку мышью, подведи контакты к розетке и выровняй угол.",
                                        "Когда контакт совпадает, цепь замыкается и задача завершается.")),
                                new TutorialSection("Гайка", List.of(
                                        "Зажми гаечный ключ и крути вокруг гайки по часовой стрелке.",
                                        "Обратное движение снижает прогресс и показывает предупреждение."))
                        ),
                        List.of(
                                new TutorialAction("Вилка", PlugSocketMinigameScreen::open),
                                new TutorialAction("Гайка", WrenchNutMinigameScreen::open)
                        )
                )
        );
    }

    private record TutorialPage(String id, String navTitle, String title, String lead,
                                PreviewKind previewKind, List<TutorialSection> sections,
                                List<TutorialAction> actions) {
    }

    private record TutorialSection(String title, List<String> bullets) {
    }

    private record TutorialAction(String title, Runnable action) {
    }

    private enum PreviewKind {
        OVERVIEW,
        QUESTS,
        TERMINAL,
        SOLAR,
        MAP,
        MINIGAMES
    }

    private static final class TutorialController {
        private final VBox nav;
        private final VBox content;
        private final ScrollView scroll;
        private final List<TutorialPage> pages;
        private final List<Button> navButtons = new ArrayList<>();
        private int selectedIndex;

        private TutorialController(VBox nav, VBox content, ScrollView scroll, List<TutorialPage> pages) {
            this.nav = nav;
            this.content = content;
            this.scroll = scroll;
            this.pages = pages;
        }

        private void build() {
            for (int i = 0; i < pages.size(); i++) {
                int index = i;
                Button button = navButton(pages.get(i).navTitle(), () -> select(index));
                navButtons.add(button);
                nav.addChild(button);
            }
            select(0);
        }

        private void select(int index) {
            if (index < 0 || index >= pages.size()) return;
            selectedIndex = index;
            updateNavButtons();
            rebuildContent(pages.get(index));
            scroll.scrollTo(0.0f, 0.0f);
        }

        private void updateNavButtons() {
            for (int i = 0; i < navButtons.size(); i++) {
                Button button = navButtons.get(i);
                boolean selected = i == selectedIndex;
                button.background().set(selected ? PANEL_HOT : PANEL_ALT);
                button.borderColor().set(selected ? ACCENT : BORDER_DIM);
                button.textColor().set(selected ? TITLE : TEXT);
            }
        }

        private void rebuildContent(TutorialPage page) {
            content.clearChildren();

            HBox top = new HBox();
            top.layout(style -> style.widthPercent(100.0f).height(232.0f).gap(14.0f).flexGrow(0.0f).flexShrink(1.0f).overflow(Overflow.HIDDEN));

            VBox intro = new VBox();
            intro.layout(style -> style.width(LayoutConstraints.AUTO).heightPercent(100.0f).gap(10.0f).flexGrow(1.0f).flexShrink(1.0f).flexBasis(0.0f).padding(0.0f).overflow(Overflow.HIDDEN));
            Label title = label(page.title(), TITLE, false);
            title.layout(style -> style.height(34.0f).flexGrow(0.0f).flexShrink(0.0f));
            Label lead = label(page.lead(), TEXT, true);
            lead.layout(style -> style.widthPercent(100.0f).height(112.0f).flexGrow(1.0f).flexShrink(1.0f));
            intro.addChild(title);
            intro.addChild(lead);

            if (!page.actions().isEmpty()) {
                HBox actions = new HBox();
                actions.layout(style -> style.height(38.0f).gap(8.0f).flexGrow(0.0f).flexShrink(0.0f));
                for (TutorialAction action : page.actions()) {
                    Button actionButton = actionButton(action.title(), action.action());
                    actionButton.layout(style -> style.width(150.0f).height(32.0f).flexGrow(0.0f).flexShrink(0.0f));
                    actions.addChild(actionButton);
                }
                intro.addChild(actions);
            }

            TutorialPreview preview = new TutorialPreview(page.previewKind());
            preview.layout(style -> style.width(300.0f).heightPercent(100.0f).flexGrow(0.0f).flexShrink(0.0f));
            top.addChild(intro);
            top.addChild(preview);
            content.addChild(top);

            for (TutorialSection section : page.sections()) {
                content.addChild(sectionCard(section));
            }
            content.addChild(bottomSpacer());
        }

        private Widget bottomSpacer() {
            Box spacer = new Box();
            spacer.themeEnabled(false);
            spacer.backgroundVisible(false);
            spacer.borderVisible(false);
            spacer.layout(style -> style.widthPercent(100.0f).height(42.0f).flexGrow(0.0f).flexShrink(0.0f));
            return spacer;
        }

        private Widget sectionCard(TutorialSection section) {
            Box outer = cardBox();
            VBox inner = new VBox();
            inner.layout(style -> style.widthPercent(100.0f).height(LayoutConstraints.AUTO).gap(8.0f));
            Label title = label(section.title(), TITLE, false);
            title.layout(style -> style.height(26.0f).flexGrow(0.0f).flexShrink(0.0f));
            inner.addChild(title);
            for (String bullet : section.bullets()) {
                Label line = label("• " + bullet, TEXT, true);
                line.layout(style -> style.widthPercent(100.0f).height(LayoutConstraints.AUTO).flexGrow(0.0f).flexShrink(0.0f));
                inner.addChild(line);
            }
            outer.addChild(inner);
            return outer;
        }
    }

    private static final class TutorialPreview extends WidgetBase {
        private final PreviewKind kind;

        private TutorialPreview(PreviewKind kind) {
            this.kind = kind;
        }

        @Override
        public void measure(LayoutContext context) {
            if (visibility() == Visibility.COLLAPSED) {
                setDesiredSize(0.0f, 0.0f);
                return;
            }
            setDesiredSize(resolveDesiredSize(context, 300.0f, 220.0f));
        }

        @Override
        public void render(RenderContext context) {
            pushOpacity(context);
            try {
                DrawScope draw = new DrawScope(context, transform(), layoutBounds());
                float x = layoutBounds().x();
                float y = layoutBounds().y();
                float w = layoutBounds().width();
                float h = layoutBounds().height();
                draw.addRectFilled(x, y, w, h, 8.0f, MutableColor.rgba(0.018f, 0.024f, 0.035f, 1.0f));
                draw.addRect(x, y, w, h, 8.0f, BORDER, 1.0f);
                draw.addText("PREVIEW", x + 14.0f, y + 12.0f, 90.0f, 22.0f, MUTED);
                switch (kind) {
                    case OVERVIEW -> renderOverview(draw, x, y, w, h);
                    case QUESTS -> renderQuests(draw, x, y, w, h);
                    case TERMINAL -> renderTerminal(draw, x, y, w, h);
                    case SOLAR -> renderSolar(draw, x, y, w, h);
                    case MAP -> renderMap(draw, x, y, w, h);
                    case MINIGAMES -> renderMinigames(draw, x, y, w, h);
                }
            } finally {
                popOpacity(context);
            }
        }

        private void renderOverview(DrawScope draw, float x, float y, float w, float h) {
            float cy = y + h * 0.55f;
            float start = x + 54.0f;
            float step = (w - 108.0f) / 3.0f;
            for (int i = 0; i < 4; i++) {
                float cx = start + step * i;
                draw.addCircleFilled(cx, cy, 18.0f, i == 0 ? ACCENT : i == 3 ? GREEN : ACCENT_2, 32);
                draw.addCircle(cx, cy, 23.0f, BORDER, 32, 1.0f);
                if (i < 3) draw.addLine(cx + 25.0f, cy, cx + step - 25.0f, cy, MUTED, 2.0f);
            }
            draw.addText("QUEST -> MAP -> TERMINAL -> ACTION", x + 28.0f, y + h - 44.0f, w - 56.0f, 24.0f, TEXT);
        }

        private void renderQuests(DrawScope draw, float x, float y, float w, float h) {
            float left = x + 32.0f;
            float top = y + 54.0f;
            for (int i = 0; i < 4; i++) {
                MutableColor color = i == 0 ? ACCENT_2 : i == 1 ? GREEN : i == 2 ? DANGER : ACCENT;
                draw.addRectFilled(left, top + i * 34.0f, w - 64.0f, 22.0f, 4.0f, MutableColor.rgba(color.r() * 0.25f, color.g() * 0.25f, color.b() * 0.25f, 0.95f));
                draw.addRect(left, top + i * 34.0f, w - 64.0f, 22.0f, 4.0f, color, 1.0f);
            }
            draw.addText("ACTIVE QUEST ZONES", x + 42.0f, y + h - 40.0f, w - 84.0f, 22.0f, TEXT);
        }

        private void renderTerminal(DrawScope draw, float x, float y, float w, float h) {
            float left = x + 28.0f;
            float top = y + 50.0f;
            draw.addRectFilled(left, top, w - 56.0f, h - 76.0f, 4.0f, MutableColor.rgba(0.000f, 0.035f, 0.016f, 1.0f));
            draw.addRect(left, top, w - 56.0f, h - 76.0f, 4.0f, GREEN, 1.0f);
            draw.addText("/teleport Sixik 0 64 0", left + 14.0f, top + 18.0f, w - 84.0f, 20.0f, GREEN);
            draw.addRectFilled(left + 14.0f, top + 50.0f, w - 84.0f, 24.0f, 2.0f, MutableColor.rgba(0.035f, 0.160f, 0.070f, 0.96f));
            draw.addText("player  x  y  z", left + 22.0f, top + 54.0f, w - 100.0f, 18.0f, MutableColor.rgba(0.76f, 1.0f, 0.72f, 1.0f));
            draw.addText("TAB COMPLETION", left + 14.0f, top + 90.0f, w - 84.0f, 20.0f, MUTED);
        }

        private void renderSolar(DrawScope draw, float x, float y, float w, float h) {
            float cx = x + w * 0.50f;
            float cy = y + h * 0.54f;
            draw.addCircle(cx, cy, 74.0f, MutableColor.rgba(0.22f, 0.32f, 0.45f, 0.80f), 48, 1.0f);
            draw.addCircle(cx, cy, 116.0f, MutableColor.rgba(0.16f, 0.22f, 0.32f, 0.70f), 64, 1.0f);
            draw.addCircleFilled(cx + 84.0f, cy - 38.0f, 12.0f, ACCENT_2, 24);
            draw.addCircleFilled(cx - 92.0f, cy + 44.0f, 16.0f, MutableColor.rgba(0.46f, 0.52f, 0.60f, 1.0f), 24);
            draw.addLine(cx - 18.0f, cy + 18.0f, cx + 28.0f, cy, ACCENT, 4.0f);
            draw.addLine(cx - 18.0f, cy - 18.0f, cx + 28.0f, cy, ACCENT, 4.0f);
            draw.addText("WASD + DOCK", x + 34.0f, y + h - 40.0f, w - 68.0f, 22.0f, TEXT);
        }

        private void renderMap(DrawScope draw, float x, float y, float w, float h) {
            float ox = x + 50.0f;
            float oy = y + 68.0f;
            room(draw, ox, oy, 68.0f, 34.0f, ACCENT, "Ты здесь");
            room(draw, ox + 80.0f, oy + 6.0f, 46.0f, 22.0f, MUTED, "");
            room(draw, ox + 138.0f, oy - 18.0f, 88.0f, 58.0f, ACCENT_2, "");
            room(draw, ox + 238.0f, oy - 4.0f, 54.0f, 30.0f, GREEN, "");
            draw.addCircleFilled(ox + 262.0f, oy + 11.0f, 7.0f, GREEN, 20);
            draw.addText("FLOOR 0 / MARKERS", x + 34.0f, y + h - 40.0f, w - 68.0f, 22.0f, TEXT);
        }

        private void renderMinigames(DrawScope draw, float x, float y, float w, float h) {
            float left = x + 46.0f;
            float cy = y + h * 0.54f;
            draw.addRectFilled(left, cy - 24.0f, 74.0f, 48.0f, 8.0f, MutableColor.rgba(0.18f, 0.22f, 0.30f, 1.0f));
            draw.addCircleFilled(left + 56.0f, cy - 10.0f, 5.0f, ACCENT, 14);
            draw.addCircleFilled(left + 56.0f, cy + 10.0f, 5.0f, ACCENT, 14);
            draw.addLine(left + 110.0f, cy, left + 178.0f, cy, ACCENT_2, 8.0f);
            draw.addCircle(left + 238.0f, cy, 26.0f, MUTED, 6, 7.0f);
            draw.addLine(left + 238.0f, cy, left + 300.0f, cy - 32.0f, GREEN, 10.0f);
            draw.addText("REPAIR CHECKS", x + 42.0f, y + h - 40.0f, w - 84.0f, 22.0f, TEXT);
        }

        private void room(DrawScope draw, float x, float y, float w, float h, MutableColor color, String text) {
            draw.addRectFilled(x, y, w, h, 3.0f, MutableColor.rgba(color.r() * 0.24f, color.g() * 0.24f, color.b() * 0.24f, 0.96f));
            draw.addRect(x, y, w, h, 3.0f, color, 1.0f);
            if (!text.isBlank()) draw.addText(text, x + 6.0f, y + 8.0f, w - 12.0f, 18.0f, TITLE);
        }
    }
}
package dev.sixik.unigui.tests;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.debug.DebugOverlayAnchor;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.selection.SelectionMode;
import dev.sixik.unigui.api.sort.SortDirection;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft.MinecraftBlockPreviewWidget;
import dev.sixik.unigui.backend.minecraft.MinecraftEntityPreviewWidget;
import dev.sixik.unigui.backend.minecraft.MinecraftItemPreviewWidget;
import dev.sixik.unigui.backend.minecraft.MinecraftFonts;
import dev.sixik.unigui.backend.minecraft.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.*;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class TestCommands {
    private static final int SAMPLE_OVERVIEW = 0;
    private static final int SAMPLE_CONTROLS_TEXT = 1;
    private static final int SAMPLE_VIRTUAL_DATA = 2;
    private static final int SAMPLE_EDITABLE_TABLE = 3;
    private static final int SAMPLE_OVERLAYS = 4;
    private static final int SAMPLE_ANIMATIONS = 5;
    private static final int SAMPLE_MINECRAFT = 6;
    private static final int SAMPLE_ENTITY_STRESS = 7;
    private static final int SAMPLE_LAYOUT_V2 = 8;
    private static final int SAMPLE_FONTS = 9;

    private static Runnable changeMode;
    private static Supplier<String> renderMode = () -> "none";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unigui").executes(ctx -> {
            RenderSystem.recordRenderCall(() -> {
                final DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
                final Widget widget = demoInterface(context);
                final MinecraftWidgetScreen screen = new MinecraftWidgetScreen(Component.empty(), widget, context);
                screen.renderPolicy(UiRenderPolicy.onDirty());

                renderMode = () -> screen.renderPolicy().mode().name();
                changeMode = () -> {
                    final UiRenderPolicy.Mode mode = screen.renderPolicy().mode();
                    switch (mode) {
                        case CONTINUOUS -> {
                            screen.renderPolicy(UiRenderPolicy.vsync());
                        }
                        case VSYNC -> {
                            screen.renderPolicy(UiRenderPolicy.onDirty());
                        }
                        case ON_DIRTY -> {
                            screen.renderPolicy(UiRenderPolicy.fixedFps(60));
                        }
                        case FIXED_FPS -> {
                            screen.renderPolicy(UiRenderPolicy.continuous());
                        }
                    }
                };

                Minecraft.getInstance().setScreen(screen);
            });

            return 0;
        }));
    }

    private static Widget demoInterface(DefaultUIContext context) {
        StackPanel viewport = new StackPanel();

        Box frame = panelBox(0.025f, 0.028f, 0.035f, 0.96f);
        frame.align(Alignment.STRETCH, Alignment.STRETCH)
                .margin(6.0f)
                .grow(1.0f);

        StackPanel frameContent = new StackPanel();
        DockPanel root = new DockPanel();
        root.margin(8.0f);
        frameContent.addChild(root);
        frame.addChild(frameContent);
        viewport.addChild(frame);

        root.addChild(header(), DockSide.TOP);

        StackPanel sampleHost = new StackPanel();

        WidgetBase[] samples = new WidgetBase[]{
                overviewSample(),
                controlsAndTextSample(),
                virtualDataSample(),
                editableTableSample(),
                overlaysSample(),
                animationsSample(),
                minecraftSample(),
                minecraftEntityStressSample(),
                layoutV2Sample(),
                fontsSample()
        };
        for (WidgetBase sample : samples) {
            sampleHost.addChild(sample);
        }

        ToggleButton[] navButtons = new ToggleButton[]{
                navButton("Overview"),
                navButton("Controls + Text"),
                navButton("Virtual Data"),
                navButton("Editable Table"),
                navButton("Overlays"),
                navButton("Animations"),
                navButton("Minecraft"),
                navButton("Entity stress"),
                navButton("Layout v2"),
                navButton("Fonts")
        };

        Box navBox = navigation(context, navButtons, samples);
        ScrollView sampleScroll = new ScrollView(sampleHost);
        sampleScroll.preferredSize(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .align(Alignment.STRETCH, Alignment.STRETCH)
                .margin(8.0f)
                .grow(1.0f);
        sampleScroll.scrollStep(16.0f);

        root.addChild(navBox, DockSide.LEFT);
        root.addChild(footer(), DockSide.BOTTOM);
        root.addChild(sampleScroll);
        selectSample(SAMPLE_OVERVIEW, navButtons, samples);

        return viewport;
    }

    private static Box header() {
        Box header = panelBox(0.06f, 0.07f, 0.10f, 0.96f);
        header.preferredSize(LayoutConstraints.AUTO, 30.0f).grow(0.0f);

        HBox headerRow = new HBox();
        headerRow.spacing(8.0f);
        headerRow.margin(8.0f, 4.0f).grow(0.0f);

        Label title = new Label("UniGUI samples");
        title.preferredSize(130.0f, 18.0f).align(Alignment.START, Alignment.CENTER).grow(0.0f);

        Label subtitle = new Label("Left tabs switch sample panels. Keep Debug tools enabled for profiler overlay.");
        subtitle.preferredSize(LayoutConstraints.AUTO, 18.0f).align(Alignment.START, Alignment.CENTER).grow(1.0f);

        headerRow.addChild(title);
        headerRow.addChild(subtitle);
        header.addChild(padded(headerRow, 8.0f, 4.0f));
        return header;
    }

    private static Box navigation(DefaultUIContext context, ToggleButton[] navButtons, WidgetBase[] samples) {
        VBox nav = new VBox();
        nav.spacing(6.0f);
        nav.preferredSize(146.0f, LayoutConstraints.AUTO).margin(6.0f).grow(0.0f);

        Label navTitle = new Label("Samples");
        navTitle.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        nav.addChild(navTitle);

        for (int index = 0; index < navButtons.length; index++) {
            final int sampleIndex = index;
            ToggleButton button = navButtons[index];
            button.onClick(event -> selectSample(sampleIndex, navButtons, samples));
            nav.addChild(button);
        }

        Separator navSeparator = new Separator();
        navSeparator.preferredSize(LayoutConstraints.AUTO, 1.0f).margin(0.0f, 2.0f).grow(0.0f);
        nav.addChild(navSeparator);

        Checkbox debugTools = new Checkbox("Debug tools");
        debugTools.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        debugTools.onCheckedChanged(event -> {
            context.debugFlags(event.newValue() ? DebugFlags.ALL : DebugFlags.NONE);
        });
        nav.addChild(debugTools);

        DebugOverlayAnchor[] overlayAnchors = DebugOverlayAnchor.values();
        int[] overlayAnchorIndex = {0};
        Button overlayAnchor = new Button("Anchor: top left");
        overlayAnchor.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        overlayAnchor.onClick(event -> {
            overlayAnchorIndex[0] = (overlayAnchorIndex[0] + 1) % overlayAnchors.length;
            DebugOverlayAnchor anchor = overlayAnchors[overlayAnchorIndex[0]];
            context.debugOverlaySettings().anchor(anchor);
            overlayAnchor.text("Anchor: " + anchor.name().toLowerCase(Locale.ROOT).replace('_', ' '));
        });
        nav.addChild(overlayAnchor);

        float[] overlayScales = {0.75f, 0.5f, 1.0f};
        int[] overlayScaleIndex = {0};
        Button overlayScale = new Button("Overlay scale: 75%");
        overlayScale.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        overlayScale.onClick(event -> {
            overlayScaleIndex[0] = (overlayScaleIndex[0] + 1) % overlayScales.length;
            float scale = overlayScales[overlayScaleIndex[0]];
            context.debugOverlaySettings().scale(scale);
            overlayScale.text("Overlay scale: " + Math.round(scale * 100.0f) + "%");
        });
        nav.addChild(overlayScale);

        int[] sampleWindows = {30, 60, 100, 300};
        int[] sampleWindowIndex = {2};
        Button sampleWindow = new Button("Frame range: 100");
        sampleWindow.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        sampleWindow.onClick(event -> {
            sampleWindowIndex[0] = (sampleWindowIndex[0] + 1) % sampleWindows.length;
            int frames = sampleWindows[sampleWindowIndex[0]];
            context.debugOverlaySettings().sampleWindow(frames);
            sampleWindow.text("Frame range: " + frames);
        });
        nav.addChild(sampleWindow);

        Button changeRenderMode = new Button("Render Mod: NONE");
        changeRenderMode.onClick((event -> {
            changeMode.run();
            changeRenderMode.text("Render Mod: " + renderMode.get());
        }));
        nav.addChild(changeRenderMode);

        TextBlock navHint = new TextBlock("Tip: add each new framework feature as a new sample panel here.");
        navHint.overflowMode(TextOverflowMode.MARQUEE_ON_HOVER);
        navHint.preferredSize(LayoutConstraints.AUTO, 42.0f).grow(0.0f);
        nav.addChild(navHint);

        Box navBox = panelBox(0.035f, 0.04f, 0.055f, 0.90f);
        navBox.preferredSize(160.0f, LayoutConstraints.AUTO).grow(0.0f);

        ScrollView navScroll = new ScrollView(nav);
        navScroll.preferredSize(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .align(Alignment.STRETCH, Alignment.STRETCH)
                .grow(1.0f);
        navScroll.scrollStep(12.0f);
        navBox.addChild(padded(navScroll, 6.0f));
        return navBox;
    }

    private static Box footer() {
        Box footer = panelBox(0.04f, 0.045f, 0.06f, 0.92f);
        footer.preferredSize(LayoutConstraints.AUTO, 22.0f).grow(0.0f);

        Label footerText = new Label("/unigui  |  arrows/page navigate lists/tables, Enter/F2 edits table cells");
        footerText.preferredSize(LayoutConstraints.AUTO, 14.0f)
                .margin(8.0f, 4.0f)
                .align(Alignment.START, Alignment.CENTER)
                .grow(0.0f);
        footer.addChild(padded(footerText, 8.0f, 4.0f));
        return footer;
    }

    private static VBox overviewSample() {
        VBox sample = samplePanel("Overview", "Small smoke-test dashboard for the currently implemented MVP pieces.");

        TextBlock text = new TextBlock("Use the sample buttons on the left to test independent feature groups. "
                + "The Debug tools checkbox stays outside samples so profiling can be enabled everywhere.");
        text.overflowMode(TextOverflowMode.CLIP);
        text.preferredSize(LayoutConstraints.AUTO, 40.0f).grow(0.0f);
        sample.addChild(text);

        WrapPanel cards = new WrapPanel();
        cards.spacing(8.0f);
        cards.lineSpacing(8.0f);
        cards.grow(1.0f);
        cards.addChild(infoCard("Layout", "Dock / Stack / Wrap / VBox / HBox"));
        cards.addChild(infoCard("Input", "Focus / pointer capture / text editing"));
        cards.addChild(infoCard("Virtual", "List + table + sorted navigation"));
        sample.addChild(cards);

        return sample;
    }

    private static OverlayLayer overlaysSample() {
        VBox content = samplePanel("Overlays", "Hover controls for tooltips; click Inspect for popup or Window for draggable dialog.");

        WrapPanel row = new WrapPanel();
        row.spacing(8.0f);
        row.lineSpacing(6.0f);
        row.grow(0.0f);

        Button craft = new Button("Craft");
        craft.preferredSize(72.0f, 22.0f).grow(0.0f);

        Button inspect = new Button("Inspect");
        inspect.preferredSize(82.0f, 22.0f).grow(0.0f);

        Button openWindow = new Button("Window");
        openWindow.preferredSize(82.0f, 22.0f).grow(0.0f);

        ToggleButton freeDrag = new ToggleButton("Free drag");
        freeDrag.preferredSize(86.0f, 22.0f).grow(0.0f);

        TextBlock hint = new TextBlock("Tooltip overlays render above content and do not capture pointer input.");
        hint.overflowMode(TextOverflowMode.MARQUEE_ON_HOVER);
        hint.preferredSize(LayoutConstraints.AUTO, 24.0f).grow(0.0f);

        row.addChild(craft);
        row.addChild(inspect);
        row.addChild(openWindow);
        row.addChild(freeDrag);
        row.addChild(hint);
        content.addChild(row);

        TextBlock body = new TextBlock("Popup flips/clamps at host edges. Window drag is host-constrained unless Free drag is enabled.");
        body.overflowMode(TextOverflowMode.CLIP);
        body.preferredSize(LayoutConstraints.AUTO, 42.0f).grow(0.0f);
        content.addChild(body);

        OverlayLayer layer = new OverlayLayer(content);
        Popup popup = new Popup(inspect, popupContent());
        WindowWidget window = new WindowWidget("Recipe dialog", windowContent())
                .position(248.0f, 62.0f)
                .closeOnOutsideClick(false);
        window.preferredSize(210.0f, 124.0f).grow(0.0f);
        inspect.onClick(event -> popup.toggle());
        openWindow.onClick(event -> window.toggle());
        freeDrag.onCheckedChanged(event -> window.constrainToHost(!event.newValue()));
        layer.addOverlay(new Tooltip(craft, "Runs the selected recipe once."));
        layer.addOverlay(new Tooltip(inspect, "Opens a retained popup anchored to this button."));
        layer.addOverlay(new Tooltip(openWindow, "Opens a draggable overlay window/dialog shell."));
        layer.addOverlay(new Tooltip(freeDrag, "Allows the window to move outside the OverlayLayer host."));
        layer.addOverlay(new Tooltip(hint, "Long hints can use this instead of stealing layout space."));
        layer.addOverlay(popup);
        layer.addOverlay(window);
        return layer;
    }

    private static VBox popupContent() {
        VBox popup = new VBox();
        popup.spacing(5.0f);
        popup.preferredSize(150.0f, 82.0f).grow(0.0f);

        Label title = new Label("Popup inspector");
        title.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);

        TextBlock body = new TextBlock("Anchored overlay with outside-click close.");
        body.overflowMode(TextOverflowMode.CLIP);
        body.preferredSize(LayoutConstraints.AUTO, 32.0f).grow(0.0f);

        Button action = new Button("Action");
        action.preferredSize(64.0f, 20.0f).grow(0.0f);

        popup.addChild(title);
        popup.addChild(body);
        popup.addChild(action);
        return popup;
    }

    private static VBox windowContent() {
        VBox content = new VBox();
        content.spacing(6.0f);
        content.preferredSize(170.0f, 70.0f).grow(0.0f);

        TextBlock description = new TextBlock("Drag the title bar. The x button closes this retained overlay.");
        description.overflowMode(TextOverflowMode.CLIP);
        description.preferredSize(LayoutConstraints.AUTO, 34.0f).grow(0.0f);

        HBox actions = new HBox();
        actions.spacing(6.0f);
        actions.grow(0.0f);

        Button apply = new Button("Apply");
        apply.preferredSize(62.0f, 20.0f).grow(0.0f);

        Button reset = new Button("Reset");
        reset.preferredSize(58.0f, 20.0f).grow(0.0f);

        actions.addChild(apply);
        actions.addChild(reset);
        content.addChild(description);
        content.addChild(actions);
        return content;
    }

    private static VBox animationsSample() {
        VBox sample = samplePanel("Animations", "Property transitions for opacity, position and scale. Hover button uses interaction transitions.");

        TransitionSpec smooth = TransitionSpec.of(0.28f, AnimationEasing.EASE_IN_OUT);
        TextBlock status = new TextBlock("Click controls to animate the preview card.");
        status.overflowMode(TextOverflowMode.CLIP);
        status.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        sample.addChild(status);

        WrapPanel controls = new WrapPanel();
        controls.spacing(6.0f);
        controls.lineSpacing(5.0f);
        controls.grow(0.0f);

        Button fade = new Button("Fade");
        fade.preferredSize(58.0f, 22.0f).grow(0.0f);

        Button move = new Button("Move");
        move.preferredSize(58.0f, 22.0f).grow(0.0f);

        Button scale = new Button("Scale");
        scale.preferredSize(58.0f, 22.0f).grow(0.0f);

        Button reset = new Button("Reset");
        reset.preferredSize(58.0f, 22.0f).grow(0.0f);

        Button hover = new Button("Hover transition");
        hover.preferredSize(118.0f, 22.0f).grow(0.0f);
        hover.interactionTransitions(true)
                .interactionTransition(TransitionSpec.of(0.12f, AnimationEasing.EASE_OUT))
                .interactionScales(1.0f, 1.045f, 0.965f)
                .interactionOpacities(1.0f, 0.86f, 0.55f);

        controls.addChild(fade);
        controls.addChild(move);
        controls.addChild(scale);
        controls.addChild(reset);
        controls.addChild(hover);
        sample.addChild(controls);

        WrapPanel preview = new WrapPanel();
        preview.spacing(10.0f);
        preview.lineSpacing(8.0f);
        preview.grow(1.0f);

        Box card = panelBox(0.055f, 0.065f, 0.095f, 0.96f);
        card.preferredSize(150.0f, 82.0f).align(Alignment.START, Alignment.START).grow(0.0f);
        card.transform().pivot().set(75.0f, 41.0f);

        VBox cardContent = new VBox();
        cardContent.spacing(5.0f);
        Label cardTitle = new Label("Animated card");
        cardTitle.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        TextBlock cardBody = new TextBlock("Opacity, position and scale are driven by WidgetBase transitions.");
        cardBody.overflowMode(TextOverflowMode.CLIP);
        cardBody.preferredSize(LayoutConstraints.AUTO, 42.0f).grow(0.0f);
        cardContent.addChild(cardTitle);
        cardContent.addChild(cardBody);
        card.addChild(padded(cardContent, 8.0f));

        TextBlock notes = new TextBlock("The hover button is opt-in so existing controls do not move unless the sample enables transitions.");
        notes.overflowMode(TextOverflowMode.MARQUEE_ON_HOVER);
        notes.preferredSize(220.0f, 72.0f).grow(0.0f);

        preview.addChild(card);
        preview.addChild(notes);
        sample.addChild(preview);

        fade.onClick(event -> {
            float target = card.opacity() > 0.6f ? 0.38f : 1.0f;
            card.animateOpacity(target, smooth);
            status.text("Animating opacity -> " + Math.round(target * 100.0f) + "%");
        });
        move.onClick(event -> {
            float targetX = card.transform().position().x() == 0.0f ? 28.0f : 0.0f;
            card.animatePosition(targetX, 0.0f, smooth);
            status.text("Animating position x -> " + Math.round(targetX));
        });
        scale.onClick(event -> {
            float targetScale = card.transform().scale().x() < 1.1f ? 1.18f : 1.0f;
            card.animateScale(targetScale, targetScale, smooth);
            status.text("Animating scale -> " + Math.round(targetScale * 100.0f) + "%");
        });
        reset.onClick(event -> {
            card.animateOpacity(1.0f, smooth);
            card.animatePosition(0.0f, 0.0f, smooth);
            card.animateScale(1.0f, 1.0f, smooth);
            status.text("Resetting animation properties.");
        });

        return sample;
    }

    private static VBox minecraftSample() {
        VBox sample = samplePanel("Minecraft previews", "Backend-specific custom draw widgets for item, block and entity previews.");

        TextBlock status = new TextBlock("These widgets fall back to text in non-Minecraft render backends.");
        status.overflowMode(TextOverflowMode.CLIP);
        status.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        sample.addChild(status);

        Label vanillaFont = new Label();
        vanillaFont.richText(RichText.of(
                "Vanilla Minecraft font", MinecraftFonts.defaultFace(), 10.0f));
        vanillaFont.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        sample.addChild(vanillaFont);

        WrapPanel previews = new WrapPanel();
        previews.spacing(8.0f);
        previews.lineSpacing(8.0f);
        previews.grow(0.0f);

        MinecraftItemPreviewWidget item = new MinecraftItemPreviewWidget("Diamond", new ItemStack(Items.DIAMOND, 7));
        item.preferredSize(92.0f, 80.0f).grow(0.0f);

        MinecraftItemPreviewWidget tool = new MinecraftItemPreviewWidget("Pickaxe", Items.DIAMOND_PICKAXE);
        tool.decorations(false);
        tool.preferredSize(92.0f, 80.0f).grow(0.0f);

        MinecraftBlockPreviewWidget block = new MinecraftBlockPreviewWidget("Crafting", Blocks.CRAFTING_TABLE);
        block.preferredSize(92.0f, 80.0f).grow(0.0f);

        MinecraftEntityPreviewWidget entity = new MinecraftEntityPreviewWidget("Zombie", EntityType.ZOMBIE);
        entity.previewSize(58.0f);
        entity.preferredSize(98.0f, 84.0f).grow(0.0f);

        previews.addChild(item);
        previews.addChild(tool);
        previews.addChild(block);
        previews.addChild(entity);
        sample.addChild(previews);

        WrapPanel controls = new WrapPanel();
        controls.spacing(6.0f);
        controls.lineSpacing(5.0f);
        controls.grow(0.0f);

        Button swapItem = new Button("Swap item");
        swapItem.preferredSize(82.0f, 22.0f).grow(0.0f);

        Button swapBlock = new Button("Swap block");
        swapBlock.preferredSize(86.0f, 22.0f).grow(0.0f);

        Button swapEntity = new Button("Swap entity");
        swapEntity.preferredSize(88.0f, 22.0f).grow(0.0f);

        Button textured = new Button("Textured");
        textured.backgroundTexture(new SimpleTextureHandle(
                "minecraft:textures/block/warped_planks.png", 16, 16));
        textured.backgroundTextureFit(ImageFit.COVER);
        textured.backgroundTextureTint().set(0.72f, 0.72f, 0.72f, 1.0f);
        textured.preferredSize(82.0f, 22.0f).grow(0.0f);

        swapItem.onClick(event -> {
            if (item.stack().is(Items.DIAMOND)) {
                item.stack(new ItemStack(Items.EMERALD, 11)).label("Emerald");
                status.text("Item preview -> Emerald x11");
            } else {
                item.stack(new ItemStack(Items.DIAMOND, 7)).label("Diamond");
                status.text("Item preview -> Diamond x7");
            }
        });
        swapBlock.onClick(event -> {
            if (block.block() == Blocks.CRAFTING_TABLE) {
                block.block(Blocks.FURNACE).label("Furnace");
                status.text("Block preview -> Furnace");
            } else {
                block.block(Blocks.CRAFTING_TABLE).label("Crafting");
                status.text("Block preview -> Crafting Table");
            }
        });
        swapEntity.onClick(event -> {
            if (entity.entityType() == EntityType.ZOMBIE) {
                entity.entityType(EntityType.CREEPER).look(14.0f, 20.0f);
                entity.label("Creeper");
                status.text("Entity preview -> Creeper");
            } else {
                entity.entityType(EntityType.ZOMBIE).look(22.0f, 12.0f);
                entity.label("Zombie");
                status.text("Entity preview -> Zombie");
            }
        });

        controls.addChild(swapItem);
        controls.addChild(swapBlock);
        controls.addChild(swapEntity);
        controls.addChild(textured);
        sample.addChild(controls);

        return sample;
    }

    private static VBox minecraftEntityStressSample() {
        VBox sample = samplePanel(
                "Minecraft entity stress",
                "192 compact entity previews rendered together to expose batching, depth and render-target artifacts.");

        List<EntityType<? extends LivingEntity>> entityTypes = List.of(
                EntityType.ZOMBIE,
                EntityType.SKELETON,
                EntityType.CREEPER,
                EntityType.SPIDER,
                EntityType.CAVE_SPIDER,
                EntityType.SLIME,
                EntityType.PIG,
                EntityType.COW,
                EntityType.SHEEP,
                EntityType.CHICKEN,
                EntityType.VILLAGER,
                EntityType.WITCH,
                EntityType.PILLAGER,
                EntityType.DROWNED,
                EntityType.HUSK,
                EntityType.STRAY);

        WrapPanel entities = new WrapPanel();
        entities.spacing(1.0f);
        entities.lineSpacing(1.0f);
        entities.grow(0.0f);

        for (int index = 0; index < 192; index++) {
            EntityType<? extends LivingEntity> entityType = entityTypes.get(index % entityTypes.size());
            MinecraftEntityPreviewWidget entity = new MinecraftEntityPreviewWidget("", entityType);
            entity.labelVisible(false);
            entity.backgroundVisible(false);
            entity.borderVisible(false);
            entity.previewSize(16.0f);
            entity.look(7.0f, 4.0f);
            entity.preferredSize(26.0f, 26.0f).grow(0.0f);
            entities.addChild(entity);
        }

        sample.addChild(entities);
        return sample;
    }

    private static VBox controlsAndTextSample() {
        VBox sample = samplePanel("Controls + Text", "SearchField clipping, Slider drag, ProgressBar fill and TextBlock overflow modes.");

        WrapPanel toolbar = new WrapPanel().spacing(6.0f).lineSpacing(4.0f);
        toolbar.grow(0.0f);

        SearchField search = new SearchField("very long search query that should clip inside the widget");
        search.preferredSize(160.0f, 20.0f).grow(0.0f);

        Slider slider = new Slider().range(0.0f, 100.0f).step(5.0f).value(35.0f);
        slider.preferredSize(130.0f, 20.0f).grow(0.0f);

        ProgressBar progress = new ProgressBar().range(0.0f, 100.0f).value(42.0f);
        progress.preferredSize(100.0f, 12.0f).align(Alignment.START, Alignment.CENTER).grow(0.0f);

        NumberField number = new NumberField().range(0.0d, 100d).value(42.0d);
        number.preferredSize(70.0f, 20.0f).grow(0.0f);
        number.onValueChanged(event -> progress.value((float) event.newValue()));

        toolbar.addChild(search);
        toolbar.addChild(slider);
        toolbar.addChild(progress);
        toolbar.addChild(number);
        sample.addChild(toolbar);

        WrapPanel textModes = new WrapPanel();
        textModes.spacing(8.0f);
        textModes.lineSpacing(8.0f);
        textModes.grow(1.0f);
        textModes.addChild(textModeCard("CLIP", TextOverflowMode.CLIP));
        textModes.addChild(textModeCard("SHRINK", TextOverflowMode.SHRINK_TO_FIT));
        textModes.addChild(textModeCard("MARQUEE", TextOverflowMode.MARQUEE_ON_HOVER));
        sample.addChild(textModes);

        return sample;
    }

    private static VBox fontsSample() {
        VBox sample = samplePanel("Fonts", "SDF, vanilla Minecraft and colored RichText runs with mixed faces and sizes.");

        Label sdfTitle = new Label("External SDF font");
        sdfTitle.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        sample.addChild(sdfTitle);

        TextBlock sdfText = new TextBlock();
        sdfText.richText(RichText.of("The quick brown fox 0123456789", Fonts.defaultFace(), 16.0f));
        sdfText.preferredSize(LayoutConstraints.AUTO, 24.0f).grow(0.0f);
        sample.addChild(sdfText);

        Label vanillaTitle = new Label("Vanilla Minecraft faces");
        vanillaTitle.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        sample.addChild(vanillaTitle);

        TextBlock vanillaText = new TextBlock();
        vanillaText.richText(RichText.builder()
                .font(MinecraftFonts.defaultFace()).size(14.0f).append("minecraft:default  ")
                .font(MinecraftFonts.uniformFace()).size(14.0f).append("minecraft:uniform")
                .build());
        vanillaText.preferredSize(LayoutConstraints.AUTO, 24.0f).grow(0.0f);
        sample.addChild(vanillaText);

        Label mixedTitle = new Label("Mixed colors, faces and sizes");
        mixedTitle.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        sample.addChild(mixedTitle);

        TextBlock mixedText = new TextBlock();
        mixedText.richText(RichText.builder()
                .font(Fonts.defaultFace()).size(18.0f)
                .color(MutableColor.rgba(0.25f, 0.85f, 1.0f, 1.0f)).append("SDF 18px  ")
                .font(MinecraftFonts.defaultFace()).size(12.0f)
                .color(MutableColor.rgba(1.0f, 0.75f, 0.2f, 1.0f)).append("Default 12px  ")
                .font(MinecraftFonts.uniformFace()).size(14.0f)
                .color(MutableColor.rgba(0.35f, 1.0f, 0.45f, 1.0f)).append("Uniform 14px  ")
                .font(MinecraftFonts.altFace()).size(16.0f)
                .color(MutableColor.rgba(1.0f, 0.4f, 0.8f, 1.0f)).append("Alt 16px")
                .build());
        mixedText.preferredSize(LayoutConstraints.AUTO, 30.0f).grow(0.0f);
        sample.addChild(mixedText);

        return sample;
    }

    private static VBox layoutV2Sample() {
        VBox sample = samplePanel(
                "Layout v2",
                "Flex resolver and overflow smoke test: shrink, basis, grow, percent, clipping and two-axis scrolling.");

        TextBlock status = new TextBlock("Resize the Minecraft window or change GUI Scale to see the rows resolve again.");
        status.overflowMode(TextOverflowMode.CLIP);
        status.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        sample.addChild(status);

        TextBlock apiContract = new TextBlock(
                "Public API: preferredSize/grow stay as compatibility helpers; layout(style -> ...) is the advanced Layout v2 entry point.");
        apiContract.overflowMode(TextOverflowMode.MARQUEE_ON_HOVER);
        apiContract.preferredSize(LayoutConstraints.AUTO, 22.0f).grow(0.0f);
        sample.addChild(apiContract);

        Label shrinkTitle = new Label("Shrink: fixed control + flexible text");
        shrinkTitle.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        sample.addChild(shrinkTitle);

        HBox shrinkRow = new HBox();
        shrinkRow.spacing(6.0f);
        shrinkRow.preferredSize(LayoutConstraints.AUTO, 24.0f).grow(0.0f);

        Button fixed = new Button("Fixed 80");
        fixed.layout(style -> style.size(80.0f, 22.0f).flexShrink(0.0f));

        Button flexible = new Button("Flexible / shrink");
        flexible.layout(style -> style
                .size(180.0f, 22.0f)
                .minWidth(42.0f)
                .flexGrow(1.0f)
                .flexShrink(1.0f));

        shrinkRow.addChild(fixed);
        shrinkRow.addChild(flexible);
        sample.addChild(shrinkRow);

        Label basisTitle = new Label("Basis + grow: 1:2 free-space split");
        basisTitle.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        sample.addChild(basisTitle);

        HBox basisRow = new HBox();
        basisRow.spacing(6.0f);
        basisRow.preferredSize(LayoutConstraints.AUTO, 24.0f).grow(0.0f);

        Button basisOne = new Button("Basis 40 / grow 1");
        basisOne.layout(style -> style
                .flexBasis(40.0f)
                .height(22.0f)
                .flexGrow(1.0f)
                .flexShrink(1.0f));

        Button basisTwo = new Button("Basis 40 / grow 2");
        basisTwo.layout(style -> style
                .flexBasis(40.0f)
                .height(22.0f)
                .flexGrow(2.0f)
                .flexShrink(1.0f));

        basisRow.addChild(basisOne);
        basisRow.addChild(basisTwo);
        sample.addChild(basisRow);

        Label percentTitle = new Label("Percent + padding");
        percentTitle.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        sample.addChild(percentTitle);

        HBox percentRow = new HBox();
        percentRow.layout(style -> style.padding(4.0f).gap(6.0f));
        percentRow.preferredSize(LayoutConstraints.AUTO, 30.0f).grow(0.0f);

        Button half = new Button("50%");
        half.layout(style -> style.widthPercent(50.0f).height(22.0f).flexShrink(0.0f));

        Button remainder = new Button("Remaining");
        remainder.layout(style -> style.height(22.0f).flexGrow(1.0f).flexShrink(1.0f));

        percentRow.addChild(half);
        percentRow.addChild(remainder);
        sample.addChild(percentRow);

        WrapPanel justifyControls = new WrapPanel().spacing(6.0f).lineSpacing(4.0f);
        justifyControls.preferredSize(LayoutConstraints.AUTO, 22.0f).grow(0.0f);

        Button start = new Button("Justify start");
        Button center = new Button("Justify center");
        Button between = new Button("Space between");
        start.preferredSize(92.0f, 20.0f).grow(0.0f);
        center.preferredSize(100.0f, 20.0f).grow(0.0f);
        between.preferredSize(104.0f, 20.0f).grow(0.0f);
        justifyControls.addChild(start);
        justifyControls.addChild(center);
        justifyControls.addChild(between);
        sample.addChild(justifyControls);

        HBox justifyRow = new HBox();
        justifyRow.preferredSize(LayoutConstraints.AUTO, 24.0f).grow(0.0f);
        justifyRow.layout(style -> style
                .alignItems(Align.CENTER)
                .justifyContent(Justify.START));

        Button left = new Button("Left");
        Button right = new Button("Right");
        left.preferredSize(50.0f, 22.0f).grow(0.0f);
        right.preferredSize(58.0f, 22.0f).grow(0.0f);
        justifyRow.addChild(left);
        justifyRow.addChild(right);
        sample.addChild(justifyRow);

        start.onClick(event -> {
            justifyRow.layout(style -> style.justifyContent(Justify.START));
            status.text("justifyContent = START");
        });
        center.onClick(event -> {
            justifyRow.layout(style -> style.justifyContent(Justify.CENTER));
            status.text("justifyContent = CENTER");
        });
        between.onClick(event -> {
            justifyRow.layout(style -> style.justifyContent(Justify.SPACE_BETWEEN));
            status.text("justifyContent = SPACE_BETWEEN");
        });

        Label overflowTitle = new Label("Overflow + ScrollView");
        overflowTitle.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        sample.addChild(overflowTitle);

        TextBlock overflowStatus = new TextBlock("overflow-x/y = AUTO; use Shift + wheel for horizontal scrolling.");
        overflowStatus.overflowMode(TextOverflowMode.CLIP);
        overflowStatus.preferredSize(LayoutConstraints.AUTO, 18.0f).grow(0.0f);
        sample.addChild(overflowStatus);

        Box overflowContent = panelBox(0.055f, 0.065f, 0.085f, 0.98f);
        VBox overflowRows = new VBox();
        overflowRows.spacing(5.0f);
        for (int row = 1; row <= 7; row++) {
            Label line = new Label("Scrollable content row " + row
                    + "  |  this intentionally wide line demonstrates horizontal overflow and clipping");
            line.preferredSize(500.0f, 16.0f).grow(0.0f);
            overflowRows.addChild(line);
        }
        overflowContent.addChild(padded(overflowRows, 8.0f));

        ScrollView overflowView = new ScrollView(overflowContent).contentSize(520.0f, 156.0f);
        overflowView.preferredSize(LayoutConstraints.AUTO, 92.0f).grow(0.0f);
        overflowView.layout(style -> style.overflowX(Overflow.AUTO).overflowY(Overflow.AUTO));
        overflowView.scrollStep(14.0f);

        WrapPanel overflowControls = new WrapPanel().spacing(6.0f).lineSpacing(4.0f);
        overflowControls.preferredSize(LayoutConstraints.AUTO, 22.0f).grow(0.0f);

        Button automatic = new Button("AUTO");
        Button hidden = new Button("HIDDEN");
        Button forced = new Button("SCROLL");
        automatic.preferredSize(58.0f, 20.0f).grow(0.0f);
        hidden.preferredSize(66.0f, 20.0f).grow(0.0f);
        forced.preferredSize(66.0f, 20.0f).grow(0.0f);
        automatic.onClick(event -> {
            overflowView.layout(style -> style.overflowX(Overflow.AUTO).overflowY(Overflow.AUTO));
            overflowStatus.text("overflow-x/y = AUTO; Shift + wheel scrolls horizontally.");
        });
        hidden.onClick(event -> {
            overflowView.layout(style -> style.overflow(Overflow.HIDDEN));
            overflowView.scrollTo(0.0f, 0.0f);
            overflowStatus.text("overflow-x/y = HIDDEN; content is clipped and scrolling is disabled.");
        });
        forced.onClick(event -> {
            overflowView.layout(style -> style.overflow(Overflow.SCROLL));
            overflowStatus.text("overflow-x/y = SCROLL; both scrollbars remain visible.");
        });
        overflowControls.addChild(automatic);
        overflowControls.addChild(hidden);
        overflowControls.addChild(forced);
        sample.addChild(overflowControls);
        sample.addChild(overflowView);

        return sample;
    }

    private static VBox virtualDataSample() {
        VBox sample = samplePanel("Virtual Data", "VirtualListView and VirtualTableView with keyboard navigation and sorting.");

        HBox main = new HBox();
        main.spacing(8.0f);
        main.grow(1.0f);

        VirtualListView list = new VirtualListView()
                .itemCount(500)
                .itemHeight(18.0f)
                .overscan(2)
                .selectionMode(SelectionMode.MULTIPLE)
                .itemFactory(index -> new Label("Recipe #" + (index + 1)));
        list.preferredSize(150.0f, LayoutConstraints.AUTO).grow(0.0f);
        list.selectIndex(2);
        main.addChild(list);

        VirtualTableView table = demoTable(false);
        table.grow(1.0f);
        main.addChild(table);

        sample.addChild(main);
        return sample;
    }

    private static VBox editableTableSample() {
        VBox sample = samplePanel("Editable Table", "Select a cell and press Enter/F2. Enter commits, Escape cancels.");

        String[][] rows = {
                {"Copper Gear", "Parts", "12"},
                {"Iron Plate", "Parts", "8"},
                {"Steam Motor", "Machines", "64"},
                {"Glass Tube", "Parts", "6"},
                {"Basic Circuit", "Electronics", "24"},
                {"Recipe Machine", "Machines", "128"}
        };

        TextBlock status = new TextBlock("No edits yet.");
        status.overflowMode(TextOverflowMode.CLIP);
        status.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        sample.addChild(status);

        final VirtualTableView[] tableRef = new VirtualTableView[1];
        VirtualTableView table = new VirtualTableView()
                .addColumn("Name", 130.0f)
                .addColumn("Category", 90.0f)
                .addColumn("Price", 58.0f)
                .rowCount(rows.length)
                .rowHeight(18.0f)
                .headerHeight(20.0f)
                .overscan(2)
                .selectionMode(SelectionMode.MULTIPLE)
                .editable(true)
                .cellTextProvider((row, column) -> editableCellText(rows, tableRef[0], row, column))
                .sortKeyProvider((row, column) -> editableSortKey(rows, tableRef[0], row, column));
        tableRef[0] = table;
        table.grow(1.0f);
        table.activeCell(0, 0);
        table.selectRow(0);
        table.onCellEditStarted(event -> status.text("Editing row " + event.row() + ", col " + event.column() + ": " + event.text()));
        table.onCellEditCommitted(event -> {
            rows[event.row()][editableModelColumn(table, event.column())] = event.newText();
            status.text("Committed row " + event.row() + ", col " + event.column() + ": " + event.oldText() + " -> " + event.newText());
        });
        table.onCellEditCancelled(event -> status.text("Cancelled row " + event.row() + ", col " + event.column()));
        table.onColumnResized(event -> status.text("Resized column " + event.column() + ": " + Math.round(event.oldWidth()) + " -> " + Math.round(event.newWidth())));
        table.onColumnMoved(event -> status.text("Moved column " + event.oldIndex() + " -> " + event.newIndex()));

        WrapPanel columnTools = new WrapPanel();
        columnTools.spacing(6.0f);
        columnTools.lineSpacing(5.0f);
        columnTools.grow(0.0f);

        Button widenName = new Button("Widen first");
        widenName.preferredSize(82.0f, 20.0f).grow(0.0f);
        widenName.onClick(event -> table.resizeColumn(0, table.columnWidth(0) + 16.0f));

        Button resetWidths = new Button("Reset widths");
        resetWidths.preferredSize(86.0f, 20.0f).grow(0.0f);
        resetWidths.onClick(event -> resetEditableColumnWidths(table));

        Button cycleColumns = new Button("Cycle columns");
        cycleColumns.preferredSize(94.0f, 20.0f).grow(0.0f);
        cycleColumns.onClick(event -> table.moveColumn(0, table.columns().size() - 1));

        columnTools.addChild(widenName);
        columnTools.addChild(resetWidths);
        columnTools.addChild(cycleColumns);
        sample.addChild(columnTools);
        sample.addChild(table);

        return sample;
    }

    private static VirtualTableView demoTable(boolean editable) {
        String[] names = {
                "Copper Gear", "Iron Plate", "Steam Motor", "Glass Tube", "Basic Circuit",
                "Recipe Machine", "Carbon Filter", "Pressure Valve", "Steel Frame", "Energy Cell",
                "Fluid Pump", "Mixer Rotor", "Heat Exchanger", "Logic Core", "Assembler Arm"
        };
        String[] categories = {
                "Parts", "Parts", "Machines", "Parts", "Electronics",
                "Machines", "Consumables", "Parts", "Structures", "Power",
                "Fluid", "Machines", "Thermal", "Electronics", "Machines"
        };
        int[] prices = {12, 8, 64, 6, 24, 128, 18, 15, 48, 96, 42, 35, 76, 88, 55};

        VirtualTableView table = new VirtualTableView()
                .addColumn("Name", 120.0f)
                .addColumn("Category", 90.0f)
                .addColumn("Price", 58.0f)
                .rowCount(names.length)
                .rowHeight(18.0f)
                .headerHeight(20.0f)
                .overscan(2)
                .selectionMode(SelectionMode.MULTIPLE)
                .editable(editable)
                .cellTextProvider((row, column) -> demoCellText(names, categories, prices, row, column))
                .sortKeyProvider((row, column) -> demoSortKey(names, categories, prices, row, column))
                .sortBy(2, SortDirection.DESCENDING);
        table.selectRow(5);
        return table;
    }

    private static VBox samplePanel(String title, String hint) {
        VBox sample = new VBox();
        sample.spacing(8.0f);
        sample.margin(0.0f);

        Label titleLabel = new Label(title);
        titleLabel.preferredSize(LayoutConstraints.AUTO, 18.0f).grow(0.0f);
        sample.addChild(titleLabel);

        TextBlock hintBlock = new TextBlock(hint);
        hintBlock.overflowMode(TextOverflowMode.CLIP);
        hintBlock.preferredSize(LayoutConstraints.AUTO, 24.0f).grow(0.0f);
        sample.addChild(hintBlock);
        return sample;
    }

    private static Box infoCard(String title, String body) {
        Box card = panelBox(0.045f, 0.050f, 0.070f, 0.92f);
        card.grow(1.0f);

        VBox content = new VBox();
        content.spacing(4.0f);
        content.margin(8.0f);

        Label titleLabel = new Label(title);
        titleLabel.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);

        TextBlock bodyText = new TextBlock(body);
        bodyText.overflowMode(TextOverflowMode.MARQUEE_ON_HOVER);
        bodyText.preferredSize(LayoutConstraints.AUTO, 42.0f).grow(0.0f);

        content.addChild(titleLabel);
        content.addChild(bodyText);
        card.addChild(padded(content, 8.0f));
        return card;
    }

    private static Box textModeCard(String title, TextOverflowMode mode) {
        Box card = panelBox(0.045f, 0.050f, 0.070f, 0.92f);
        card.grow(1.0f);
        card.maxSize(40, 40);

        VBox content = new VBox();
        content.spacing(4.0f);

        Label label = new Label(title);
        label.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);

        TextBlock text = new TextBlock("This is intentionally long text to test overflow behavior in a narrow card.");
        text.overflowMode(mode);
        text.preferredSize(LayoutConstraints.AUTO, 38.0f).grow(0.0f);

        content.addChild(label);
        content.addChild(text);
        card.addChild(padded(content, 8.0f));
        return card;
    }

    private static ToggleButton navButton(String text) {
        ToggleButton button = new ToggleButton(text);
        button.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        return button;
    }

    private static void selectSample(int selectedIndex, ToggleButton[] navButtons, WidgetBase[] samples) {
        for (int index = 0; index < samples.length; index++) {
            boolean selected = index == selectedIndex;
            samples[index].visible(selected);
            navButtons[index].silentChecked(selected);
        }
    }

    private static String demoCellText(String[] names, String[] categories, int[] prices, int row, int column) {
        return switch (column) {
            case 0 -> names[row];
            case 1 -> categories[row];
            case 2 -> Integer.toString(prices[row]);
            default -> "";
        };
    }

    private static Comparable<?> demoSortKey(String[] names, String[] categories, int[] prices, int row, int column) {
        return switch (column) {
            case 0 -> names[row];
            case 1 -> categories[row];
            case 2 -> prices[row];
            default -> row;
        };
    }

    private static String editableCellText(String[][] rows, VirtualTableView table, int row, int column) {
        return rows[row][editableModelColumn(table, column)];
    }

    private static Comparable<?> editableSortKey(String[][] rows, VirtualTableView table, int row, int column) {
        int modelColumn = editableModelColumn(table, column);
        return modelColumn == 2 ? Integer.parseInt(rows[row][modelColumn]) : rows[row][modelColumn];
    }

    private static int editableModelColumn(VirtualTableView table, int visualColumn) {
        if (table == null || visualColumn < 0 || visualColumn >= table.columns().size()) {
            return Math.max(0, Math.min(2, visualColumn));
        }
        return switch (table.columns().get(visualColumn).header()) {
            case "Category" -> 1;
            case "Price" -> 2;
            default -> 0;
        };
    }

    private static void resetEditableColumnWidths(VirtualTableView table) {
        for (int column = 0; column < table.columns().size(); column++) {
            switch (table.columns().get(column).header()) {
                case "Name" -> table.resizeColumn(column, 130.0f);
                case "Category" -> table.resizeColumn(column, 90.0f);
                case "Price" -> table.resizeColumn(column, 58.0f);
                default -> {
                }
            }
        }
    }

    private static Box panelBox(float r, float g, float b, float a) {
        Box box = new Box();
        box.backgroundVisible(true);
        box.borderVisible(true);
        box.radius(3.0f);
        box.background().set(r, g, b, a);
        box.borderColor().set(0.22f, 0.24f, 0.30f, 0.95f);
        return box;
    }

    private static StackPanel padded(WidgetBase child, float margin) {
        return padded(child, margin, margin);
    }

    private static StackPanel padded(WidgetBase child, float horizontal, float vertical) {
        StackPanel panel = new StackPanel();
        child.margin(horizontal, vertical);
        panel.addChild(child);
        return panel;
    }
}

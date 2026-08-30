package dev.sixik.unigui.testmod.mixin;

import dev.sixik.unigui.testmod.client.TestModClient;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Р вЂќР С•Р В±Р В°Р Р†Р В»РЎРЏР ВµРЎвЂљ Р В±РЎвЂ№РЎРѓРЎвЂљРЎР‚РЎвЂ№Р в„– Р Р†РЎвЂ¦Р С•Р Т‘ Р Р† UniGUI demo Р Р…Р В° Р С–Р В»Р В°Р Р†Р Р…РЎвЂ№Р в„– РЎРЊР С”РЎР‚Р В°Р Р… Minecraft.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void uniguiTestMod$addDemoButton(CallbackInfo ci) {
        addRenderableWidget(Button.builder(Component.literal("UniGUI Demo"), button -> TestModClient.openDemo())
                .bounds(width - 124, 8, 116, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Retro Terminal"), button -> TestModClient.openRetroTerminal())
                .bounds(width - 124, 32, 116, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Level Map"), button -> TestModClient.openLevelMap())
                .bounds(width - 124, 56, 116, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Plug Minigame"), button -> TestModClient.openPlugSocketMinigame())
                .bounds(width - 124, 80, 116, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Wrench Minigame"), button -> TestModClient.openWrenchNutMinigame())
                .bounds(width - 124, 104, 116, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Wire Minigame"), button -> TestModClient.openWireConnectionMinigame())
                .bounds(width - 124, 128, 116, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Sync Minigame"), button -> TestModClient.openSyncBatteryMinigame())
                .bounds(width - 124, 152, 116, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Spanner Minigame"), button -> TestModClient.openSpannerRhythmMinigame())
                .bounds(width - 124, 176, 116, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Fork Minigame"), button -> TestModClient.openForkInSocketMinigame())
                .bounds(width - 124, 200, 116, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Tutorial"), button -> TestModClient.openTutorial())
                .bounds(width - 124, 224, 116, 20)
                .build());
    }
}

package dev.sixik.isf;

import dev.sixik.isf.command.IsfCommands;
import dev.sixik.isf.definition.IsfDefinitionReloadListener;
import dev.sixik.isf.network.IsfNetwork;
import dev.sixik.isf.importer.VanillaRecipeTypeSupports;
import dev.sixik.isf.runtime.IsfRuntime;
import dev.sixik.isf.trigger.IsfTriggerContext;
import dev.sixik.isf.trigger.IsfTriggerRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;

/** Forge entrypoint ISF для Minecraft 1.20.1. */
@Mod(IsfMod.MOD_ID)
public final class IsfMod {
    public static final String MOD_ID = "isf";
    public static final Logger LOGGER = LoggerFactory.getLogger("ISF");
    private static final IsfRuntime RUNTIME = new IsfRuntime();

    public IsfMod() {
        VanillaRecipeTypeSupports.registerAll(RUNTIME.recipeTypes());
        IsfNetwork.init();
        MinecraftForge.EVENT_BUS.addListener(this::addReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onItemCrafted);
        MinecraftForge.EVENT_BUS.addListener(this::onUseItem);
        MinecraftForge.EVENT_BUS.addListener(this::onUseBlock);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLogin);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                dev.sixik.isf.client.IsfClient.init());
    }

    public static IsfRuntime runtime() {
        return RUNTIME;
    }

    private void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new IsfDefinitionReloadListener(RUNTIME.definitions()));
    }

    private void registerCommands(RegisterCommandsEvent event) {
        IsfCommands.register(event.getDispatcher());
    }

    private void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ServerPlayer player = serverPlayer(event.getEntity());
        if (player == null || event.getCrafting().isEmpty()) return;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem());
        fireAndSync(IsfTriggerRegistry.CRAFT,
                new IsfTriggerContext(player, itemId, null, Map.of()));
    }

    private void onUseItem(PlayerInteractEvent.RightClickItem event) {
        ServerPlayer player = serverPlayer(event.getEntity());
        if (player == null || event.getItemStack().isEmpty()) return;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        fireAndSync(IsfTriggerRegistry.USE,
                new IsfTriggerContext(player, itemId, null, Map.of()));
    }

    private void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        ServerPlayer player = serverPlayer(event.getEntity());
        if (player == null) return;
        ResourceLocation stationId = BuiltInRegistries.BLOCK.getKey(
                event.getLevel().getBlockState(event.getPos()).getBlock());
        fireAndSync(IsfTriggerRegistry.STATION,
                new IsfTriggerContext(player, stationId, stationId, Map.of()));
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = serverPlayer(event.getEntity());
        if (player != null) IsfNetwork.sendLibrary(player);
    }

    private static void fireAndSync(ResourceLocation trigger, IsfTriggerContext context) {
        List<ResourceLocation> unlocked = RUNTIME.fire(trigger, context);
        if (!unlocked.isEmpty()) IsfNetwork.sendLibrary(context.player());
    }

    private static ServerPlayer serverPlayer(Player player) {
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }
}

package dev.sixik.isf.network;

import dev.sixik.isf.client.IsfClientState;
import dev.sixik.isf.definition.IsfCatalystDefinition;
import dev.sixik.isf.persistence.IsfWorldData;
import dev.sixik.isf.runtime.IsfResolvedRecipe;
import dev.sixik.isf.runtime.IsfRecipeQueryMatcher;
import dev.sixik.isf.definition.IsfDefinitionJson;
import dev.sixik.isf.definition.IsfRecipeDefinition;
import com.google.gson.Gson;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Синхронизация world-scoped прогресса и закладок ISF. */
public final class IsfNetwork {
    private static final String VERSION = "5";
    private static final int MAX_COLLECTION_SIZE = 100_000;
    private static final int MAX_RECIPE_JSON_LENGTH = 1_048_576;
    private static final Gson GSON = new Gson();
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.tryBuild("isf", "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);
    private static int packetId;

    private IsfNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(packetId++, ToggleBookmark.class,
                ToggleBookmark::encode, ToggleBookmark::decode, ToggleBookmark::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(packetId++, RequestRecipes.class,
                RequestRecipes::encode, RequestRecipes::decode, RequestRecipes::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(packetId++, LibrarySnapshot.class,
                LibrarySnapshot::encode, LibrarySnapshot::decode, LibrarySnapshot::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void toggleBookmark(ResourceLocation itemId, boolean bookmarked) {
        CHANNEL.sendToServer(new ToggleBookmark(itemId, bookmarked));
    }

    public static void requestRecipes(ResourceLocation itemId, boolean usages) {
        if (itemId != null) CHANNEL.sendToServer(new RequestRecipes(itemId, usages));
    }

    public static void sendLibrary(ServerPlayer player) {
        IsfWorldData data = IsfWorldData.get(player.server);
        Set<ResourceLocation> unlocked = data.unlocked(player.getUUID());
        Map<ResourceLocation, ResourceLocation> resultByRecipe = new LinkedHashMap<>();
        List<IsfRecipeDefinition> documents = new ArrayList<>();
        for (IsfResolvedRecipe recipe : dev.sixik.isf.IsfMod.runtime().definitions().recipes()) {
            if (!unlocked.contains(recipe.id())) continue;
            ResourceLocation result = resultItem(recipe);
            if (result != null) resultByRecipe.put(recipe.id(), result);
            documents.add(new IsfRecipeDefinition(recipe.id(), recipe.recipeType(), null,
                    recipe.parameters(), recipe.triggers(), recipe.visual(), recipe.source()));
        }
        Map<ResourceLocation, List<IsfCatalystDefinition>> catalysts =
                dev.sixik.isf.IsfMod.runtime().definitions().typeCatalysts();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new LibrarySnapshot(unlocked, data.bookmarks(player.getUUID()), resultByRecipe, documents, catalysts));
    }

    private static ResourceLocation resultItem(IsfResolvedRecipe recipe) {
        if (!recipe.parameters().containsKey("result")) return null;
        try {
            return ResourceLocation.tryParse(recipe.parameters().get("result").getAsString());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public record ToggleBookmark(ResourceLocation itemId, boolean bookmarked) {
        private static void encode(ToggleBookmark packet, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(packet.itemId);
            buffer.writeBoolean(packet.bookmarked);
        }

        private static ToggleBookmark decode(FriendlyByteBuf buffer) {
            return new ToggleBookmark(buffer.readResourceLocation(), buffer.readBoolean());
        }

        private static void handle(ToggleBookmark packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                IsfWorldData.get(player.server).setBookmarked(player.getUUID(), packet.itemId, packet.bookmarked);
                sendLibrary(player);
            });
            context.setPacketHandled(true);
        }
    }

    public record RequestRecipes(ResourceLocation itemId, boolean usages) {
        private static void encode(RequestRecipes packet, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(packet.itemId);
            buffer.writeBoolean(packet.usages);
        }

        private static RequestRecipes decode(FriendlyByteBuf buffer) {
            return new RequestRecipes(buffer.readResourceLocation(), buffer.readBoolean());
        }

        private static void handle(RequestRecipes packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                IsfWorldData data = IsfWorldData.get(player.server);
                for (IsfResolvedRecipe recipe : dev.sixik.isf.IsfMod.runtime().definitions().recipes()) {
                    if (IsfRecipeQueryMatcher.matches(
                            recipe.parameters(), recipe.triggers(), packet.itemId, packet.usages)) {
                        data.unlock(player.getUUID(), recipe.id());
                    }
                }
                sendLibrary(player);
            });
            context.setPacketHandled(true);
        }
    }

    public record LibrarySnapshot(Set<ResourceLocation> unlocked,
                                  Set<ResourceLocation> bookmarks,
                                  Map<ResourceLocation, ResourceLocation> recipeResults,
                                  List<IsfRecipeDefinition> recipes,
                                  Map<ResourceLocation, List<IsfCatalystDefinition>> typeCatalysts) {
        public LibrarySnapshot(Set<ResourceLocation> unlocked, Set<ResourceLocation> bookmarks) {
            this(unlocked, bookmarks, Map.of(), List.of(), Map.of());
        }

        public LibrarySnapshot {
            unlocked = Set.copyOf(unlocked == null ? Set.of() : unlocked);
            bookmarks = Set.copyOf(bookmarks == null ? Set.of() : bookmarks);
            recipeResults = Map.copyOf(recipeResults == null ? Map.of() : recipeResults);
            recipes = List.copyOf(recipes == null ? List.of() : recipes);
            Map<ResourceLocation, List<IsfCatalystDefinition>> safeCatalysts = new LinkedHashMap<>();
            if (typeCatalysts != null) {
                typeCatalysts.forEach((id, values) -> {
                    if (id != null && values != null && !values.isEmpty()) {
                        safeCatalysts.put(id, List.copyOf(values));
                    }
                });
            }
            typeCatalysts = Map.copyOf(safeCatalysts);
        }

        private static void encode(LibrarySnapshot packet, FriendlyByteBuf buffer) {
            writeIds(buffer, packet.unlocked);
            writeIds(buffer, packet.bookmarks);
            requireValidSize(packet.recipeResults.size(), "recipe results");
            buffer.writeVarInt(packet.recipeResults.size());
            packet.recipeResults.forEach((recipe, result) -> {
                buffer.writeResourceLocation(recipe);
                buffer.writeResourceLocation(result);
            });
            requireValidSize(packet.recipes.size(), "recipes");
            buffer.writeVarInt(packet.recipes.size());
            for (IsfRecipeDefinition recipe : packet.recipes) {
                buffer.writeResourceLocation(recipe.id());
                buffer.writeUtf(GSON.toJson(IsfDefinitionJson.writeRecipe(recipe)), MAX_RECIPE_JSON_LENGTH);
            }
            requireValidSize(packet.typeCatalysts.size(), "type catalysts");
            buffer.writeVarInt(packet.typeCatalysts.size());
            packet.typeCatalysts.forEach((typeId, values) -> {
                buffer.writeResourceLocation(typeId);
                buffer.writeVarInt(values.size());
                for (IsfCatalystDefinition catalyst : values) {
                    buffer.writeResourceLocation(catalyst.item());
                    buffer.writeVarInt(catalyst.count());
                }
            });
        }

        private static LibrarySnapshot decode(FriendlyByteBuf buffer) {
            Set<ResourceLocation> unlocked = readIds(buffer);
            Set<ResourceLocation> bookmarks = readIds(buffer);
            int size = readSize(buffer, "recipe results");
            Map<ResourceLocation, ResourceLocation> resultByRecipe = new LinkedHashMap<>();
            for (int index = 0; index < size; index++) {
                resultByRecipe.put(buffer.readResourceLocation(), buffer.readResourceLocation());
            }
            int recipeCount = readSize(buffer, "recipes");
            List<IsfRecipeDefinition> recipes = new ArrayList<>(recipeCount);
            for (int index = 0; index < recipeCount; index++) {
                ResourceLocation id = buffer.readResourceLocation();
                recipes.add(IsfDefinitionJson.parseRecipe(id,
                        GSON.fromJson(buffer.readUtf(MAX_RECIPE_JSON_LENGTH), com.google.gson.JsonObject.class)));
            }
            int catalystTypeCount = readSize(buffer, "type catalysts");
            Map<ResourceLocation, List<IsfCatalystDefinition>> typeCatalysts = new LinkedHashMap<>();
            for (int index = 0; index < catalystTypeCount; index++) {
                ResourceLocation typeId = buffer.readResourceLocation();
                int catalystCount = readSize(buffer, "catalysts");
                List<IsfCatalystDefinition> catalysts = new ArrayList<>(catalystCount);
                for (int catalystIndex = 0; catalystIndex < catalystCount; catalystIndex++) {
                    catalysts.add(new IsfCatalystDefinition(buffer.readResourceLocation(), buffer.readVarInt()));
                }
                typeCatalysts.put(typeId, catalysts);
            }
            return new LibrarySnapshot(unlocked, bookmarks, resultByRecipe, recipes, typeCatalysts);
        }

        private static void handle(LibrarySnapshot packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> IsfClientState.replace(
                    packet.unlocked, packet.bookmarks, packet.recipeResults, packet.recipes,
                    packet.typeCatalysts));
            context.setPacketHandled(true);
        }
    }

    private static void writeIds(FriendlyByteBuf buffer, Set<ResourceLocation> ids) {
        requireValidSize(ids.size(), "resource locations");
        buffer.writeVarInt(ids.size());
        ids.forEach(buffer::writeResourceLocation);
    }

    private static Set<ResourceLocation> readIds(FriendlyByteBuf buffer) {
        int size = readSize(buffer, "resource locations");
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (int index = 0; index < size; index++) ids.add(buffer.readResourceLocation());
        return ids;
    }

    private static int readSize(FriendlyByteBuf buffer, String collectionName) {
        int size = buffer.readVarInt();
        requireValidSize(size, collectionName);
        return size;
    }

    private static void requireValidSize(int size, String collectionName) {
        if (size < 0 || size > MAX_COLLECTION_SIZE) {
            throw new IllegalArgumentException("Invalid ISF " + collectionName + " count: " + size);
        }
    }
}

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
    private static final String VERSION = "7";
    private static final int MAX_COLLECTION_SIZE = 100_000;
    private static final int MAX_RECIPE_JSON_LENGTH = 1_048_576;
    /** Бюджет символов JSON на один chunk: лимит payload'а — 1 МБ. */
    private static final int CHUNK_CHAR_BUDGET = 256_000;
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
        CHANNEL.registerMessage(packetId++, LibraryChunk.class,
                LibraryChunk::encode, LibraryChunk::decode, LibraryChunk::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
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
        // Рецепты идут чанками: полный снапшот превышает 1 МБ лимит payload'а.
        long transferId = System.nanoTime();
        List<List<IsfRecipeDefinition>> batches = batchRecipes(documents);
        for (int index = 0; index < batches.size(); index++) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new LibraryChunk(transferId, index, batches.size(), batches.get(index)));
        }
        Map<ResourceLocation, List<IsfCatalystDefinition>> catalysts =
                dev.sixik.isf.IsfMod.runtime().definitions().typeCatalysts();
        Map<ResourceLocation, ResourceLocation> icons =
                dev.sixik.isf.IsfMod.runtime().definitions().typeIcons();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new LibrarySnapshot(transferId, unlocked, data.bookmarks(player.getUUID()),
                        resultByRecipe, catalysts, icons));
    }

    private static List<List<IsfRecipeDefinition>> batchRecipes(List<IsfRecipeDefinition> documents) {
        List<List<IsfRecipeDefinition>> batches = new ArrayList<>();
        List<IsfRecipeDefinition> current = new ArrayList<>();
        int budget = 0;
        for (IsfRecipeDefinition recipe : documents) {
            current.add(recipe);
            budget += recipe.id().toString().length()
                    + IsfDefinitionJson.writeRecipe(recipe).toString().length();
            if (budget >= CHUNK_CHAR_BUDGET) {
                batches.add(current);
                current = new ArrayList<>();
                budget = 0;
            }
        }
        batches.add(current);
        return batches;
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

    /** Часть библиотеки: рецепты уходят пакетами, финальный снапшот применяет накопленное. */
    public record LibraryChunk(long transferId, int index, int total,
                               List<IsfRecipeDefinition> recipes) {
        public LibraryChunk {
            recipes = recipes == null ? List.of() : List.copyOf(recipes);
        }

        private static void encode(LibraryChunk packet, FriendlyByteBuf buffer) {
            buffer.writeLong(packet.transferId);
            buffer.writeVarInt(packet.index);
            buffer.writeVarInt(packet.total);
            requireValidSize(packet.recipes.size(), "recipes");
            buffer.writeVarInt(packet.recipes.size());
            for (IsfRecipeDefinition recipe : packet.recipes) {
                buffer.writeResourceLocation(recipe.id());
                buffer.writeUtf(GSON.toJson(IsfDefinitionJson.writeRecipe(recipe)), MAX_RECIPE_JSON_LENGTH);
            }
        }

        private static LibraryChunk decode(FriendlyByteBuf buffer) {
            long transferId = buffer.readLong();
            int index = readSize(buffer, "chunk index");
            int total = readSize(buffer, "chunk total");
            int recipeCount = readSize(buffer, "recipes");
            List<IsfRecipeDefinition> recipes = new ArrayList<>(recipeCount);
            for (int i = 0; i < recipeCount; i++) {
                ResourceLocation id = buffer.readResourceLocation();
                recipes.add(IsfDefinitionJson.parseRecipe(id,
                        GSON.fromJson(buffer.readUtf(MAX_RECIPE_JSON_LENGTH), com.google.gson.JsonObject.class)));
            }
            return new LibraryChunk(transferId, index, total, recipes);
        }

        private static void handle(LibraryChunk packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> IsfClientState.acceptRecipeChunk(
                    packet.transferId, packet.index, packet.total, packet.recipes));
            context.setPacketHandled(true);
        }
    }

    public record LibrarySnapshot(long transferId,
                                  Set<ResourceLocation> unlocked,
                                  Set<ResourceLocation> bookmarks,
                                  Map<ResourceLocation, ResourceLocation> recipeResults,
                                  Map<ResourceLocation, List<IsfCatalystDefinition>> typeCatalysts,
                                  Map<ResourceLocation, ResourceLocation> typeIcons) {
        public LibrarySnapshot(Set<ResourceLocation> unlocked, Set<ResourceLocation> bookmarks) {
            this(0L, unlocked, bookmarks, Map.of(), Map.of(), Map.of());
        }

        public LibrarySnapshot {
            unlocked = Set.copyOf(unlocked == null ? Set.of() : unlocked);
            bookmarks = Set.copyOf(bookmarks == null ? Set.of() : bookmarks);
            recipeResults = Map.copyOf(recipeResults == null ? Map.of() : recipeResults);
            Map<ResourceLocation, List<IsfCatalystDefinition>> safeCatalysts = new LinkedHashMap<>();
            if (typeCatalysts != null) {
                typeCatalysts.forEach((id, values) -> {
                    if (id != null && values != null && !values.isEmpty()) {
                        safeCatalysts.put(id, List.copyOf(values));
                    }
                });
            }
            typeCatalysts = Map.copyOf(safeCatalysts);
            Map<ResourceLocation, ResourceLocation> safeIcons = new LinkedHashMap<>();
            if (typeIcons != null) {
                typeIcons.forEach((id, icon) -> {
                    if (id != null && icon != null) safeIcons.put(id, icon);
                });
            }
            typeIcons = Map.copyOf(safeIcons);
        }

        private static void encode(LibrarySnapshot packet, FriendlyByteBuf buffer) {
            buffer.writeLong(packet.transferId);
            writeIds(buffer, packet.unlocked);
            writeIds(buffer, packet.bookmarks);
            requireValidSize(packet.recipeResults.size(), "recipe results");
            buffer.writeVarInt(packet.recipeResults.size());
            packet.recipeResults.forEach((recipe, result) -> {
                buffer.writeResourceLocation(recipe);
                buffer.writeResourceLocation(result);
            });
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
            requireValidSize(packet.typeIcons.size(), "type icons");
            buffer.writeVarInt(packet.typeIcons.size());
            packet.typeIcons.forEach((typeId, icon) -> {
                buffer.writeResourceLocation(typeId);
                buffer.writeResourceLocation(icon);
            });
        }

        private static LibrarySnapshot decode(FriendlyByteBuf buffer) {
            long transferId = buffer.readLong();
            Set<ResourceLocation> unlocked = readIds(buffer);
            Set<ResourceLocation> bookmarks = readIds(buffer);
            int size = readSize(buffer, "recipe results");
            Map<ResourceLocation, ResourceLocation> resultByRecipe = new LinkedHashMap<>();
            for (int index = 0; index < size; index++) {
                resultByRecipe.put(buffer.readResourceLocation(), buffer.readResourceLocation());
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
            int iconTypeCount = readSize(buffer, "type icons");
            Map<ResourceLocation, ResourceLocation> typeIcons = new LinkedHashMap<>();
            for (int index = 0; index < iconTypeCount; index++) {
                typeIcons.put(buffer.readResourceLocation(), buffer.readResourceLocation());
            }
            return new LibrarySnapshot(transferId, unlocked, bookmarks, resultByRecipe, typeCatalysts, typeIcons);
        }

        private static void handle(LibrarySnapshot packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> IsfClientState.applyLibrarySnapshot(
                    packet.transferId, packet.unlocked, packet.bookmarks, packet.recipeResults,
                    packet.typeCatalysts, packet.typeIcons));
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

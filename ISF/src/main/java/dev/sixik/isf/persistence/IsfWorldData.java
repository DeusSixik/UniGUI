package dev.sixik.isf.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** World-scoped SavedData для прогресса и закладок игроков. */
public final class IsfWorldData extends SavedData implements IsfPlayerLibrary {
    private static final String DATA_NAME = "isf_player_library";
    private final Map<UUID, PlayerState> players = new LinkedHashMap<>();

    public static IsfWorldData get(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("Server cannot be null");
        ServerLevel level = server.overworld();
        return level.getDataStorage().computeIfAbsent(IsfWorldData::load, IsfWorldData::new, DATA_NAME);
    }

    public static IsfWorldData load(CompoundTag root) {
        IsfWorldData data = new IsfWorldData();
        CompoundTag playerTags = root.getCompound("players");
        for (String uuidText : playerTags.getAllKeys()) {
            try {
                UUID playerId = UUID.fromString(uuidText);
                CompoundTag stateTag = playerTags.getCompound(uuidText);
                PlayerState state = new PlayerState();
                readIds(stateTag.getList("unlocked", Tag.TAG_STRING), state.unlocked);
                readIds(stateTag.getList("bookmarks", Tag.TAG_STRING), state.bookmarks);
                data.players.put(playerId, state);
            } catch (IllegalArgumentException ignored) {
                // Повреждённая запись одного игрока не должна блокировать загрузку мира.
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        CompoundTag playerTags = new CompoundTag();
        players.forEach((uuid, state) -> {
            CompoundTag stateTag = new CompoundTag();
            stateTag.put("unlocked", writeIds(state.unlocked));
            stateTag.put("bookmarks", writeIds(state.bookmarks));
            playerTags.put(uuid.toString(), stateTag);
        });
        root.put("players", playerTags);
        return root;
    }

    @Override
    public boolean unlock(UUID playerId, ResourceLocation recipeId) {
        if (playerId == null || recipeId == null) return false;
        boolean changed = state(playerId).unlocked.add(recipeId);
        if (changed) setDirty();
        return changed;
    }

    @Override
    public boolean isUnlocked(UUID playerId, ResourceLocation recipeId) {
        PlayerState state = players.get(playerId);
        return state != null && state.unlocked.contains(recipeId);
    }

    @Override
    public Set<ResourceLocation> unlocked(UUID playerId) {
        PlayerState state = players.get(playerId);
        return state == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(state.unlocked));
    }

    @Override
    public boolean setBookmarked(UUID playerId, ResourceLocation itemId, boolean bookmarked) {
        if (playerId == null || itemId == null) return false;
        PlayerState state = state(playerId);
        boolean changed = bookmarked ? state.bookmarks.add(itemId) : state.bookmarks.remove(itemId);
        if (changed) setDirty();
        return changed;
    }

    @Override
    public Set<ResourceLocation> bookmarks(UUID playerId) {
        PlayerState state = players.get(playerId);
        return state == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(state.bookmarks));
    }

    private PlayerState state(UUID playerId) {
        return players.computeIfAbsent(playerId, ignored -> new PlayerState());
    }

    private static void readIds(ListTag list, Set<ResourceLocation> output) {
        for (int index = 0; index < list.size(); index++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(index));
            if (id != null) output.add(id);
        }
    }

    private static ListTag writeIds(Set<ResourceLocation> ids) {
        ListTag list = new ListTag();
        ids.stream().map(ResourceLocation::toString).sorted().forEach(id -> list.add(StringTag.valueOf(id)));
        return list;
    }

    private static final class PlayerState {
        private final Set<ResourceLocation> unlocked = new LinkedHashSet<>();
        private final Set<ResourceLocation> bookmarks = new LinkedHashSet<>();
    }
}

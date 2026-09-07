package dev.sixik.isf.persistence;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;

/** Хранилище открытых рецептов и закладок, привязанное к миру. */
public interface IsfPlayerLibrary {
    boolean unlock(UUID playerId, ResourceLocation recipeId);

    boolean isUnlocked(UUID playerId, ResourceLocation recipeId);

    Set<ResourceLocation> unlocked(UUID playerId);

    boolean setBookmarked(UUID playerId, ResourceLocation itemId, boolean bookmarked);

    Set<ResourceLocation> bookmarks(UUID playerId);
}

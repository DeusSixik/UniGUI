package dev.sixik.isf.api;

import com.google.gson.JsonElement;
import dev.sixik.isf.definition.IsfRecipeTypeDefinition;
import dev.sixik.isf.definition.IsfTriggerBinding;
import dev.sixik.isf.trigger.IsfTriggerRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;
import java.util.Map;

/**
 * Адаптер игрового {@link Recipe} в независимый документ ISF.
 *
 * <p>Интеграционный мод объявляет схему параметров и базовый visual в
 * {@link #definition()}, определяет поддерживаемые игровые рецепты через
 * {@link #matches(Recipe)} и копирует их значения в JSON-параметры. ISF вызывает
 * адаптер только при явном запуске команды генерации.</p>
 */
public interface IsfRecipeTypeSupport {
    /** Категория, используемая селекторами команды генерации. */
    String category();

    /** Recipe type со схемой параметров и базовым визуальным деревом. */
    IsfRecipeTypeDefinition definition();

    /** Возвращает {@code true}, если адаптер умеет импортировать этот игровой рецепт. */
    boolean matches(Recipe<?> recipe);

    /** Создаёт независимую копию параметров игрового рецепта. */
    Map<String, JsonElement> extractParameters(Recipe<?> recipe, RegistryAccess registries);

    /**
     * Создаёт триггеры сгенерированного документа.
     *
     * <p>По умолчанию результат рецепта открывается триггером {@code isf:craft}.</p>
     */
    default List<IsfTriggerBinding> createTriggers(Recipe<?> recipe,
                                                   RegistryAccess registries,
                                                   Map<String, JsonElement> parameters) {
        JsonElement result = parameters == null ? null : parameters.get("result");
        if (result == null || !result.isJsonPrimitive()) return List.of();
        ResourceLocation item = ResourceLocation.tryParse(result.getAsString());
        return item == null ? List.of() : List.of(
                new IsfTriggerBinding(IsfTriggerRegistry.CRAFT, item, null, Map.of()));
    }
}

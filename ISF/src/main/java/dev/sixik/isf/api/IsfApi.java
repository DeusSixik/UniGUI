package dev.sixik.isf.api;

import com.google.gson.JsonElement;
import dev.sixik.isf.IsfMod;
import dev.sixik.isf.network.IsfNetwork;
import dev.sixik.isf.persistence.IsfWorldData;
import dev.sixik.isf.trigger.IsfTriggerContext;
import dev.sixik.isf.trigger.IsfTriggerEngine;
import dev.sixik.isf.trigger.IsfTriggerType;
import dev.sixik.isf.runtime.IsfFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Публичная точка интеграции ISF для других модов.
 *
 * <h2>Триггеры</h2>
 * <p>Триггер решает, какой рецепт показать игроку и по какому действию.
 * Встроенные триггеры:</p>
 * <ul>
 *   <li>{@code isf:craft} (R) — крафт предмета: рецепты, где предмет — результат;</li>
 *   <li>{@code isf:use} (U) — применение предмета: рецепты, где предмет — ингредиент;</li>
 *   <li>{@code isf:station} (U по блоку) — рецепты станции: для каких крафтов
 *   применяется крафтовая станция (блок).</li>
 * </ul>
 *
 * <h2>Привязка триггеров к рецептам (JSON)</h2>
 * <p>Способ 1 — биндинг внутри рецепта ({@code data/<ns>/isf/recipes/...}):</p>
 * <pre>{@code
 * "triggers": [
 *   {"type": "mymod:my_trigger", "subject": "minecraft:iron_sword"}
 * ]
 * }</pre>
 * <p>Способ 2 — отдельный документ {@code data/<ns>/isf/trigger_bindings/<name>.json},
 * привязывающий триггер к конкретным рецептам <b>по их id</b> (сам рецепт трогать
 * не нужно — удобно для чужих/generated рецептов):</p>
 * <pre>{@code
 * {
 *   "trigger": "mymod:my_trigger",
 *   "recipes": ["isf:generated/minecraft/iron_sword"],
 *   "subject": "minecraft:iron_sword",
 *   "station": null,
 *   "conditions": {"key": "value"},
 *   "open": true
 * }
 * }</pre>
 * <p>Поля {@code subject}/{@code station}/{@code conditions} опциональны
 * ({@code null} — любое значение). Совпадение строгое: subject/station равны,
 * conditions — подмножество атрибутов вызова. Документы с неизвестными рецептами
 * отбрасываются с варном в лог. Флаг {@code open: true} — открыть окно
 * с совпавшими рецептами у игрока; без него триггер только разблокирует.</p>
 *
 * <h2>Вызов из кода мода</h2>
 * <p>Свой тип триггера (необязательно — хватает встроенных и JSON-привязок):</p>
 * <pre>{@code
 * IsfApi.registerTrigger(new ResourceLocation("mymod", "my_trigger"),
 *         (binding, context) -> true); // своя политика совпадения
 * }</pre>
 * <p>Вызов по действию игрока (например, клик по блоку, событие HighQuality):</p>
 * <pre>{@code
 * IsfApi.fireTrigger(player, new ResourceLocation("mymod", "my_trigger"),
 *         clickedItemId, stationBlockId, Map.of());
 * }</pre>
 * <p>Открыть конкретные рецепты напрямую, без триггера:</p>
 * <pre>{@code
 * IsfApi.openRecipes(player, List.of(new ResourceLocation("isf", "generated/minecraft/iron_sword")));
 * }</pre>
 *
 * <h2>Поток сервер → клиент</h2>
 * <p>{@code fireTrigger} разблокирует совпавшие рецепты, при прогрессе досылает
 * библиотеку ({@code sendLibrary}) и — если сработал документ с {@code open: true} —
 * шлёт пакет открытия окна. Порядок гарантирован: сначала документы, потом открытие.
 * Окно рисуется только поверх контейнерных экранов. Клиент и сервер должны быть
 * на одной версии мода (версия сетевого канала проверяется).</p>
 */
public final class IsfApi {
    private IsfApi() {
    }

    /**
     * Регистрирует новый тип триггера с собственной политикой совпадения.
     * Политика получает биндинг рецепта и контекст вызова; встроенные триггеры
     * сверяют subject/station на равенство, а conditions — как подмножество
     * атрибутов. Триггер-документы ({@code trigger_bindings/}) всегда используют
     * строгую семантику и политику не вызывают.
     *
     * @param id id триггера, например {@code mymod:my_trigger}
     * @param trigger политика совпадения биндинга с вызовом
     */
    public static void registerTrigger(ResourceLocation id, IsfTriggerType trigger) {
        IsfMod.runtime().triggers().register(id, trigger);
    }

    /**
     * Регистрирует поддержку игрового recipe type для команды генерации ISF.
     */
    public static void registerRecipeType(IsfRecipeTypeSupport support) {
        IsfMod.runtime().recipeTypes().register(support);
    }

    /**
     * Регистрирует безопасную функцию для выражений visual-документов.
     */
    public static void registerFunction(ResourceLocation id, IsfFunction function) {
        IsfMod.runtime().functions().register(id, function);
    }

    /**
     * Вызывает триггер для игрока (короткая форма: без станции и атрибутов).
     *
     * @param player игрок (только серверный)
     * @param trigger id триггера
     * @param subject предмет/блок-инициатор, {@code null} — любой
     * @return вновь разблокированные рецепты
     * @see #fireTrigger(ServerPlayer, ResourceLocation, ResourceLocation,
     * ResourceLocation, Map)
     */
    public static List<ResourceLocation> fireTrigger(ServerPlayer player,
                                                     ResourceLocation trigger,
                                                     ResourceLocation subject) {
        return fireTrigger(player, trigger, subject, null, Map.of());
    }

    /**
     * Вызывает триггер: разблокирует совпавшие рецепты (биндинги рецептов +
     * триггер-документы), при прогрессе досылает библиотеку и — если сработал
     * документ с {@code open: true} — открывает окно с совпавшими рецептами.
     *
     * <p>Без {@code open}-документов вызов тихий: только прогресс разблокировок,
     * окно не открывается (его открывает клиент по R/U-запросу).</p>
     *
     * @param player игрок (только серверный)
     * @param trigger id триггера (должен быть зарегистрирован)
     * @param subject предмет/блок-инициатор, {@code null} — любой
     * @param station станция крафта, {@code null} — любая
     * @param attributes дополнительные атрибуты для сверки conditions
     * @return вновь разблокированные рецепты
     * @throws IllegalArgumentException при пустом игроке/триггере или
     * неизвестном триггере
     */
    public static List<ResourceLocation> fireTrigger(ServerPlayer player,
                                                     ResourceLocation trigger,
                                                     ResourceLocation subject,
                                                     ResourceLocation station,
                                                     Map<String, JsonElement> attributes) {
        if (player == null || trigger == null) throw new IllegalArgumentException("Player and trigger are required");
        IsfTriggerEngine.FireResult result = IsfMod.runtime().fire(trigger,
                new IsfTriggerContext(player, subject, station, attributes));
        if (!result.unlocked().isEmpty()) {
            IsfNetwork.sendLibrary(player);
        }
        if (result.open()) {
            IsfNetwork.openRecipes(player, result.matched());
        }
        return result.unlocked();
    }

    /**
     * Открывает игроку конкретные рецепты по id: неизвестные пропускаются,
     * известные разблокируются, библиотека досылается при прогрессе.
     * Окно появляется поверх открытого контейнерного экрана.
     *
     * @param player игрок (только серверный)
     * @param recipeIds id рецептов из библиотеки (порядок сохраняется во вкладках)
     * @return открытые рецепты (известные клиенту после синка)
     */
    public static List<ResourceLocation> openRecipes(ServerPlayer player,
                                                     Collection<ResourceLocation> recipeIds) {
        if (player == null || recipeIds == null || recipeIds.isEmpty()) return List.of();
        List<ResourceLocation> known = new ArrayList<>();
        for (ResourceLocation recipeId : recipeIds) {
            if (recipeId != null && IsfMod.runtime().definitions().recipe(recipeId).isPresent()
                    && !known.contains(recipeId)) {
                known.add(recipeId);
            }
        }
        if (known.isEmpty()) return List.of();
        IsfWorldData data = IsfWorldData.get(player.server);
        boolean progressed = false;
        for (ResourceLocation recipeId : known) {
            progressed |= data.unlock(player.getUUID(), recipeId);
        }
        if (progressed) IsfNetwork.sendLibrary(player);
        IsfNetwork.openRecipes(player, known);
        return List.copyOf(known);
    }
}

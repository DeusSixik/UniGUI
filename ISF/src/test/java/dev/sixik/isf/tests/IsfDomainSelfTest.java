package dev.sixik.isf.tests;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.sixik.isf.definition.IsfExpression;
import dev.sixik.isf.definition.IsfParameterDefinition;
import dev.sixik.isf.definition.IsfParameterType;
import dev.sixik.isf.definition.IsfRecipeDefinition;
import dev.sixik.isf.definition.IsfRecipeTypeDefinition;
import dev.sixik.isf.definition.IsfVisualNode;
import dev.sixik.isf.definition.IsfDefinitionJson;
import dev.sixik.isf.importer.IsfRecipeTypeSupportRegistry;
import dev.sixik.isf.runtime.IsfDefinitionRegistry;
import dev.sixik.isf.runtime.IsfEvaluationContext;
import dev.sixik.isf.runtime.IsfExpressionEvaluator;
import dev.sixik.isf.runtime.IsfFunctionRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/** Быстрый тест доменной модели ISF без запуска Minecraft-клиента. */
public final class IsfDomainSelfTest {
    public static void main(String[] args) {
        new IsfDomainSelfTest().run();
        System.out.println("IsfDomainSelfTest passed");
    }

    private void run() {
        ResourceLocation baseType = id("base");
        ResourceLocation childType = id("child");
        ResourceLocation parentRecipe = id("parent");
        ResourceLocation childRecipe = id("child_recipe");

        IsfVisualNode baseVisual = node("root", "box", Map.of("width",
                new IsfExpression.Literal(new JsonPrimitive(176))), List.of());
        IsfVisualNode childVisual = node("root", "box", Map.of("height",
                new IsfExpression.Literal(new JsonPrimitive(92)), "width",
                new IsfExpression.Literal(new JsonPrimitive(180))), List.of());

        IsfRecipeTypeDefinition base = new IsfRecipeTypeDefinition(baseType, null,
                Map.of("energy", new IsfParameterDefinition("energy", IsfParameterType.NUMBER,
                        new JsonPrimitive(0), "")), baseVisual);
        IsfRecipeTypeDefinition child = new IsfRecipeTypeDefinition(childType, baseType,
                Map.of("maxEnergy", new IsfParameterDefinition("maxEnergy", IsfParameterType.NUMBER,
                        new JsonPrimitive(100), "")), childVisual);

        IsfRecipeDefinition parent = new IsfRecipeDefinition(parentRecipe, childType, null,
                Map.of("energy", new JsonPrimitive(25)), List.of(), null, null);
        IsfRecipeDefinition recipe = new IsfRecipeDefinition(childRecipe, childType, parentRecipe,
                Map.of("maxEnergy", new JsonPrimitive(50)), List.of(), null, null);

        IsfDefinitionRegistry registry = new IsfDefinitionRegistry();
        registry.replace(List.of(base, child), List.of(parent, recipe));
        var resolved = registry.recipe(childRecipe).orElseThrow();
        check(resolved.parameters().get("energy").getAsInt() == 25,
                "recipe inheritance should retain parent parameter values");
        check(resolved.parameters().get("maxEnergy").getAsInt() == 50,
                "recipe inheritance should apply child parameter overrides");
        check(resolved.visual().properties().containsKey("width")
                        && resolved.visual().properties().containsKey("height"),
                "visual inheritance should merge node properties by stable id");
        check(((IsfExpression.Literal) resolved.visual().properties().get("width")).value().getAsInt() == 180,
                "child visual properties should override parent values");

        IsfExpression percent = new IsfExpression.Call("isf:percent", List.of(
                new IsfExpression.Parameter("energy"),
                new IsfExpression.Parameter("maxEnergy")));
        var value = new IsfExpressionEvaluator(new IsfFunctionRegistry()).evaluate(percent,
                new IsfEvaluationContext(resolved.parameters()));
        check(Math.abs(value.getAsDouble() - 50.0) < 0.0001,
                "percent expression should use inherited parameters");

        IsfRecipeDefinition cycle = new IsfRecipeDefinition(id("cycle"), childType, id("cycle"), Map.of(), List.of(), null, null);
        boolean rejected = false;
        try {
            registry.replace(List.of(base, child), List.of(cycle));
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        check(rejected, "cyclic recipe inheritance should be rejected");
        check(registry.recipe(childRecipe).isPresent(),
                "failed replacement should preserve the previous working registry");

        IsfRecipeTypeDefinition serializedType = new IsfRecipeTypeDefinition(
                id("serialized_type"), null,
                Map.of("energy", new IsfParameterDefinition("energy", IsfParameterType.NUMBER,
                        new JsonPrimitive(10), "Energy value")), baseVisual);
        JsonObject serializedJson = IsfDefinitionJson.writeType(serializedType);
        check(serializedJson.getAsJsonObject("parameters").getAsJsonObject("energy")
                        .get("default").getAsInt() == 10,
                "recipe type writer should preserve parameter defaults");

        IsfRecipeTypeSupportRegistry supports = new IsfRecipeTypeSupportRegistry();
        supports.register(new dev.sixik.isf.api.IsfRecipeTypeSupport() {
            @Override
            public String category() {
                return "test";
            }

            @Override
            public IsfRecipeTypeDefinition definition() {
                return serializedType;
            }

            @Override
            public boolean matches(net.minecraft.world.item.crafting.Recipe<?> recipe) {
                return false;
            }

            @Override
            public Map<String, com.google.gson.JsonElement> extractParameters(
                    net.minecraft.world.item.crafting.Recipe<?> recipe,
                    net.minecraft.core.RegistryAccess registries) {
                return Map.of();
            }
        });
        check(supports.values().size() == 1,
                "recipe type support registry should expose registered integrations");

    }

    private static IsfVisualNode node(String nodeId, String widget,
                                      Map<String, IsfExpression> properties,
                                      List<IsfVisualNode> children) {
        return new IsfVisualNode(nodeId, id(widget), properties, children);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.tryBuild("isf", path);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

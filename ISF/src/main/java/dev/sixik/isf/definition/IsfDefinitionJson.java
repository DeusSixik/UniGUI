package dev.sixik.isf.definition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** JSON-кодек определений ISF. */
public final class IsfDefinitionJson {
    private IsfDefinitionJson() {
    }

    public static IsfRecipeTypeDefinition parseType(ResourceLocation id, JsonObject json) {
        ResourceLocation parent = optionalId(json, "parent");
        Map<String, IsfParameterDefinition> parameters = new LinkedHashMap<>();
        if (json.has("parameters")) {
            JsonObject values = object(json.get("parameters"), "parameters");
            values.entrySet().forEach(entry -> {
                JsonObject definition = object(entry.getValue(), "parameter " + entry.getKey());
                IsfParameterType type = parseParameterType(string(definition, "type", "string"));
                JsonElement defaultValue = definition.has("default") ? definition.get("default") : null;
                parameters.put(entry.getKey(), new IsfParameterDefinition(entry.getKey(), type,
                        defaultValue, string(definition, "description", "")));
            });
        }
        IsfVisualNode visual = json.has("visual") ? parseVisual(object(json.get("visual"), "visual")) : null;
        return new IsfRecipeTypeDefinition(id, parent, parameters, visual);
    }

    public static IsfRecipeDefinition parseRecipe(ResourceLocation id, JsonObject json) {
        ResourceLocation type = requiredId(json, "type");
        ResourceLocation parent = optionalId(json, "parent");
        Map<String, JsonElement> parameters = new LinkedHashMap<>();
        if (json.has("parameters")) {
            object(json.get("parameters"), "parameters").entrySet()
                    .forEach(entry -> parameters.put(entry.getKey(), entry.getValue().deepCopy()));
        }

        List<IsfTriggerBinding> triggers = new ArrayList<>();
        if (json.has("triggers")) {
            for (JsonElement element : array(json.get("triggers"), "triggers")) {
                JsonObject trigger = object(element, "trigger");
                Map<String, JsonElement> conditions = new LinkedHashMap<>();
                if (trigger.has("conditions")) {
                    object(trigger.get("conditions"), "trigger conditions").entrySet()
                            .forEach(entry -> conditions.put(entry.getKey(), entry.getValue().deepCopy()));
                }
                triggers.add(new IsfTriggerBinding(requiredId(trigger, "type"),
                        optionalId(trigger, "subject"), optionalId(trigger, "station"), conditions));
            }
        }

        IsfVisualNode visual = json.has("visual") ? parseVisual(object(json.get("visual"), "visual")) : null;
        IsfSourceReference source = null;
        if (json.has("source")) {
            JsonObject sourceJson = object(json.get("source"), "source");
            source = new IsfSourceReference(string(sourceJson, "category", "custom"),
                    optionalId(sourceJson, "id"));
        }
        return new IsfRecipeDefinition(id, type, parent, parameters, triggers, visual, source);
    }

    public static IsfVisualNode parseVisual(JsonObject json) {
        String id = string(json, "id", "");
        ResourceLocation widget = requiredId(json, "widget");
        Map<String, IsfExpression> properties = new LinkedHashMap<>();
        if (json.has("properties")) {
            object(json.get("properties"), "visual properties").entrySet()
                    .forEach(entry -> properties.put(entry.getKey(), parseExpression(entry.getValue())));
        }
        List<IsfVisualNode> children = new ArrayList<>();
        if (json.has("children")) {
            for (JsonElement child : array(json.get("children"), "visual children")) {
                children.add(parseVisual(object(child, "visual child")));
            }
        }
        return new IsfVisualNode(id, widget, properties, children);
    }

    public static IsfExpression parseExpression(JsonElement json) {
        if (json != null && json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            if (object.has("$parameter")) {
                return new IsfExpression.Parameter(object.get("$parameter").getAsString());
            }
            if (object.has("$function")) {
                List<IsfExpression> arguments = new ArrayList<>();
                if (object.has("arguments")) {
                    for (JsonElement argument : array(object.get("arguments"), "function arguments")) {
                        arguments.add(parseExpression(argument));
                    }
                }
                return new IsfExpression.Call(object.get("$function").getAsString(), arguments);
            }
        }
        return new IsfExpression.Literal(json);
    }

    public static JsonObject writeRecipe(IsfRecipeDefinition recipe) {
        JsonObject json = new JsonObject();
        json.addProperty("type", recipe.recipeType().toString());
        if (recipe.parentRecipe() != null) json.addProperty("parent", recipe.parentRecipe().toString());
        JsonObject parameters = new JsonObject();
        recipe.parameters().forEach(parameters::add);
        json.add("parameters", parameters);
        JsonArray triggers = new JsonArray();
        for (IsfTriggerBinding binding : recipe.triggers()) {
            JsonObject trigger = new JsonObject();
            trigger.addProperty("type", binding.trigger().toString());
            if (binding.subject() != null) trigger.addProperty("subject", binding.subject().toString());
            if (binding.station() != null) trigger.addProperty("station", binding.station().toString());
            if (!binding.conditions().isEmpty()) {
                JsonObject conditions = new JsonObject();
                binding.conditions().forEach(conditions::add);
                trigger.add("conditions", conditions);
            }
            triggers.add(trigger);
        }
        json.add("triggers", triggers);
        if (recipe.visual() != null) json.add("visual", writeVisual(recipe.visual()));
        if (recipe.source() != null) {
            JsonObject source = new JsonObject();
            source.addProperty("category", recipe.source().category());
            if (recipe.source().sourceId() != null) source.addProperty("id", recipe.source().sourceId().toString());
            json.add("source", source);
        }
        return json;
    }

    public static JsonObject writeType(IsfRecipeTypeDefinition type) {
        JsonObject json = new JsonObject();
        if (type.parent() != null) json.addProperty("parent", type.parent().toString());
        JsonObject parameters = new JsonObject();
        type.parameters().forEach((name, definition) -> {
            JsonObject value = new JsonObject();
            value.addProperty("type", definition.type().name().toLowerCase(Locale.ROOT));
            value.add("default", definition.defaultValue().deepCopy());
            if (!definition.description().isBlank()) {
                value.addProperty("description", definition.description());
            }
            parameters.add(name, value);
        });
        json.add("parameters", parameters);
        if (type.visual() != null) json.add("visual", writeVisual(type.visual()));
        return json;
    }

    public static JsonObject writeVisual(IsfVisualNode node) {
        JsonObject json = new JsonObject();
        json.addProperty("id", node.id());
        json.addProperty("widget", node.widget().toString());
        JsonObject properties = new JsonObject();
        node.properties().forEach((name, value) -> properties.add(name, writeExpression(value)));
        json.add("properties", properties);
        JsonArray children = new JsonArray();
        node.children().forEach(child -> children.add(writeVisual(child)));
        json.add("children", children);
        return json;
    }

    public static JsonElement writeExpression(IsfExpression expression) {
        if (expression instanceof IsfExpression.Literal literal) return literal.value().deepCopy();
        JsonObject json = new JsonObject();
        if (expression instanceof IsfExpression.Parameter parameter) {
            json.addProperty("$parameter", parameter.name());
        } else if (expression instanceof IsfExpression.Call call) {
            json.addProperty("$function", call.function());
            JsonArray arguments = new JsonArray();
            call.arguments().forEach(argument -> arguments.add(writeExpression(argument)));
            json.add("arguments", arguments);
        }
        return json;
    }

    private static IsfParameterType parseParameterType(String value) {
        try {
            return IsfParameterType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException("Unknown ISF parameter type: " + value, exception);
        }
    }

    private static ResourceLocation requiredId(JsonObject json, String name) {
        ResourceLocation id = optionalId(json, name);
        if (id == null) throw new JsonParseException("Missing or invalid ResourceLocation '" + name + "'");
        return id;
    }

    private static ResourceLocation optionalId(JsonObject json, String name) {
        if (json == null || !json.has(name) || json.get(name).isJsonNull()) return null;
        return ResourceLocation.tryParse(json.get(name).getAsString());
    }

    private static String string(JsonObject json, String name, String fallback) {
        return json != null && json.has(name) && !json.get(name).isJsonNull()
                ? json.get(name).getAsString()
                : fallback;
    }

    private static JsonObject object(JsonElement element, String name) {
        if (element == null || !element.isJsonObject()) throw new JsonParseException(name + " must be an object");
        return element.getAsJsonObject();
    }

    private static JsonArray array(JsonElement element, String name) {
        if (element == null || !element.isJsonArray()) throw new JsonParseException(name + " must be an array");
        return element.getAsJsonArray();
    }
}

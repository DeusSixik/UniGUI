package dev.sixik.isf.runtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import dev.sixik.isf.definition.IsfExpression;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Вычисляет сериализованные выражения без выполнения произвольного кода. */
public final class IsfExpressionEvaluator {
    private final IsfFunctionRegistry functions;

    public IsfExpressionEvaluator(IsfFunctionRegistry functions) {
        this.functions = functions == null ? new IsfFunctionRegistry() : functions;
    }

    public JsonElement evaluate(IsfExpression expression, IsfEvaluationContext context) {
        if (expression == null) return JsonNull.INSTANCE;
        IsfEvaluationContext safeContext = context == null ? new IsfEvaluationContext(null) : context;
        if (expression instanceof IsfExpression.Literal literal) return literal.value().deepCopy();
        if (expression instanceof IsfExpression.Parameter parameter) return safeContext.parameter(parameter.name());
        IsfExpression.Call call = (IsfExpression.Call) expression;
        ResourceLocation functionId = ResourceLocation.tryParse(call.function());
        if (functionId == null || functionId.getNamespace().equals("minecraft")) {
            functionId = ResourceLocation.tryParse("isf:" + call.function());
        }
        if (functionId == null) throw new IllegalArgumentException("Invalid ISF function id: " + call.function());
        ResourceLocation resolvedFunctionId = functionId;

        List<JsonElement> arguments = new ArrayList<>(call.arguments().size());
        for (IsfExpression argument : call.arguments()) arguments.add(evaluate(argument, safeContext));
        return functions.find(resolvedFunctionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ISF function: " + resolvedFunctionId))
                .apply(List.copyOf(arguments));
    }
}

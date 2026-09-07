package dev.sixik.isf.definition;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import java.util.List;

/** Безопасное сериализуемое выражение для свойства визуального узла. */
public sealed interface IsfExpression permits IsfExpression.Literal, IsfExpression.Parameter, IsfExpression.Call {
    record Literal(JsonElement value) implements IsfExpression {
        public Literal {
            value = value == null ? JsonNull.INSTANCE : value.deepCopy();
        }
    }

    record Parameter(String name) implements IsfExpression {
        public Parameter {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Parameter name cannot be empty");
        }
    }

    record Call(String function, List<IsfExpression> arguments) implements IsfExpression {
        public Call {
            if (function == null || function.isBlank()) throw new IllegalArgumentException("Function id cannot be empty");
            arguments = arguments == null ? List.of() : List.copyOf(arguments);
        }
    }
}

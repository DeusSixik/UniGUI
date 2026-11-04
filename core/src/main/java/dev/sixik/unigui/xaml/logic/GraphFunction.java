package dev.sixik.unigui.xaml.logic;

import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.ParamType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Функция Java, которую можно вызвать из графа: имя, типизированные параметры и результат.
 *
 * <p>Граф ссылается на функцию только по имени из реестра {@link GraphFunctions}. Произвольная рефлексия по методам
 * не используется: так набор того, что граф может сделать, задаёт автор кода, а не автор документа, и безопасно
 * загружать графы из чужих паков.</p>
 */
public final class GraphFunction {
    /**
     * Параметр функции.
     *
     * @param name имя; совпадает с именем входного пина ноды
     * @param type тип
     */
    public record Param(String name, ParamType type) {
    }

    private final String name;
    private final List<Param> params;
    private final ParamType returns;
    private final boolean pure;
    private final String description;
    private final Function<Args, Object> body;

    private GraphFunction(Builder builder) {
        this.name = builder.name;
        this.params = List.copyOf(builder.params);
        this.returns = builder.returns;
        this.pure = builder.pure;
        this.description = builder.description;
        this.body = builder.body;
    }

    /**
     * Начинает описание функции.
     *
     * @param name имя в реестре, например {@code shop.buy}
     * @return билдер
     */
    public static Builder define(String name) {
        return new Builder(name);
    }

    /** @return имя в реестре */
    public String name() {
        return name;
    }

    /** @return параметры по порядку */
    public List<Param> params() {
        return params;
    }

    /** @return тип результата или {@code null}, если функция ничего не возвращает */
    public ParamType returns() {
        return returns;
    }

    /**
     * Чистая функция вычисляется по требованию потребителя и не вызывается из потока выполнения. Чистой стоит
     * объявлять только функцию без побочных эффектов.
     *
     * @return {@code true}, если функция чистая
     */
    public boolean pure() {
        return pure;
    }

    /** @return описание для редактора */
    public String description() {
        return description;
    }

    /**
     * Вызывает функцию.
     *
     * @param args значения параметров по именам
     * @return результат или {@code null}
     */
    public Object call(Args args) {
        return body.apply(args);
    }

    /** Билдер {@link GraphFunction}. */
    public static final class Builder {
        private final String name;
        private final List<Param> params = new ArrayList<>();
        private ParamType returns;
        private boolean pure;
        private String description = "";
        private Function<Args, Object> body = args -> null;

        private Builder(String name) {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("function name must not be blank");
            this.name = name.trim();
        }

        /**
         * @param paramName имя параметра
         * @param type      тип
         * @return этот билдер
         */
        public Builder param(String paramName, ParamType type) {
            for (Param existing : params) {
                if (existing.name().equals(paramName)) {
                    throw new IllegalArgumentException("Duplicate parameter '" + paramName + "' in " + name);
                }
            }
            params.add(new Param(paramName, Objects.requireNonNull(type, "type")));
            return this;
        }

        /**
         * @param type тип результата
         * @return этот билдер
         */
        public Builder returns(ParamType type) {
            this.returns = type;
            return this;
        }

        /**
         * Объявляет функцию чистой (без побочных эффектов).
         *
         * @return этот билдер
         */
        public Builder pure() {
            this.pure = true;
            return this;
        }

        /**
         * @param text описание для редактора
         * @return этот билдер
         */
        public Builder description(String text) {
            this.description = text == null ? "" : text;
            return this;
        }

        /**
         * Задаёт тело функции.
         *
         * @param implementation получает значения параметров по именам, возвращает результат
         * @return готовая функция
         */
        public GraphFunction body(Function<Args, Object> implementation) {
            this.body = Objects.requireNonNull(implementation, "body");
            if (pure && returns == null) {
                throw new IllegalStateException("A pure function must return a value: " + name);
            }
            return new GraphFunction(this);
        }
    }
}

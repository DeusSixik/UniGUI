package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.input.TextEditorModel;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Динамический реестр команд для {@link AdminConsole}.
 *
 * <p>Реестр хранит команды в порядке регистрации, нормализует имена команд, выполняет executor'ы
 * и строит подсказки для имени команды или текущего аргумента. Значения аргументов могут быть
 * фиксированными или динамическими, поэтому виджет можно подключать к любому runtime-контексту.</p>
 *
 * <p>Пример команды с динамическими значениями:</p>
 *
 * <pre>{@code
 * registry.register(AdminCommandRegistry.command("teleport", "Teleport player")
 *         .argument("player", "Online player", () -> players)
 *         .customArgument("x", "X coordinate", List.of("0", "100"))
 *         .executor((console, call) -> console.appendInfo(call.arguments().toString()))
 *         .build());
 * }</pre>
 *
 * @see AdminConsole
 * @see CommandDefinition
 * @see ArgumentDefinition
 */
public final class AdminCommandRegistry {
    private final Map<String, CommandDefinition> commands = new Object2ObjectLinkedOpenHashMap<>();
    private Runnable changeListener = () -> {
    };
    private int version;

    /**
     * Задаёт listener изменений реестра.
     *
     * <p>Консоль использует его, чтобы обновлять подсказки при регистрации, удалении или очистке команд.</p>
     *
     * @param changeListener listener изменений или {@code null}
     * @return этот реестр для fluent-настройки
     */
    public AdminCommandRegistry onChanged(Runnable changeListener) {
        this.changeListener = changeListener == null ? () -> {
        } : changeListener;
        return this;
    }

    /**
     * Возвращает монотонно растущую версию реестра.
     *
     * @return версия, увеличиваемая при каждом изменении набора команд
     */
    public int version() {
        return version;
    }

    /**
     * Регистрирует простую команду без описания аргументов.
     *
     * @param name имя команды, начальный {@code /} допускается
     * @param description описание для help и автодополнения
     * @param executor обработчик команды
     * @return этот реестр для fluent-настройки
     */
    public AdminCommandRegistry register(String name, String description, AdminConsole.CommandExecutor executor) {
        return register(command(name, description).executor(executor).build());
    }

    /**
     * Регистрирует готовое описание команды.
     *
     * <p>Если команда с таким именем уже есть, она заменяется новой.</p>
     *
     * @param definition описание команды
     * @return этот реестр для fluent-настройки
     */
    public AdminCommandRegistry register(CommandDefinition definition) {
        if (definition == null || definition.name().isEmpty()) return this;
        commands.put(definition.name(), definition);
        notifyChanged();
        return this;
    }

    /**
     * Удаляет команду по имени.
     *
     * @param name имя команды, начальный {@code /} допускается
     * @return этот реестр для fluent-настройки
     */
    public AdminCommandRegistry unregister(String name) {
        if (commands.remove(normalizeCommandName(name)) != null) {
            notifyChanged();
        }
        return this;
    }

    /**
     * Удаляет все команды из реестра.
     *
     * @return этот реестр для fluent-настройки
     */
    public AdminCommandRegistry clear() {
        if (!commands.isEmpty()) {
            commands.clear();
            notifyChanged();
        }
        return this;
    }

    /**
     * Ищет команду по имени.
     *
     * @param name имя команды, начальный {@code /} допускается
     * @return найденная команда или {@code null}
     */
    public CommandDefinition find(String name) {
        return commands.get(normalizeCommandName(name));
    }

    /**
     * Возвращает команды в порядке регистрации.
     *
     * @return неизменяемый список команд
     */
    public List<CommandDefinition> commands() {
        return List.copyOf(commands.values());
    }

    /**
     * Возвращает короткий снимок зарегистрированных команд.
     *
     * @return неизменяемая карта {@code commandName -> description}
     */
    public Map<String, String> registeredCommands() {
        Map<String, String> result = new LinkedHashMap<>();
        for (CommandDefinition command : commands.values()) {
            result.put(command.name(), command.description());
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Выполняет команду или fallback executor.
     *
     * <p>Ошибки executor'а перехватываются и выводятся в консоль как строка ошибки, чтобы UI не падал
     * от исключения пользовательской команды.</p>
     *
     * @param console консоль, из которой пришёл вызов
     * @param invocation разобранная команда
     * @param fallbackExecutor обработчик неизвестных команд или {@code null}
     * @return {@code true}, если команда была обработана реестром или fallback'ом
     */
    public boolean execute(AdminConsole console, AdminConsole.CommandInvocation invocation,
                           AdminConsole.CommandExecutor fallbackExecutor) {
        CommandDefinition command = find(invocation.commandName());
        try {
            if (command != null && command.executor() != null) {
                command.executor().execute(console, invocation);
                return true;
            }
            if (fallbackExecutor != null) {
                fallbackExecutor.execute(console, invocation);
                return true;
            }
            return false;
        } catch (Throwable error) {
            console.appendError(error.getClass().getSimpleName() + ": "
                    + normalize(error.getMessage(), "command failed"));
            return true;
        }
    }

    /**
     * Строит подсказки для текущего ввода.
     *
     * <p>Если курсор стоит на имени команды, возвращаются подходящие команды. Если команда уже выбрана,
     * возвращаются подсказки только для текущего аргумента и только когда предыдущие аргументы валидны.</p>
     *
     * @param inputText полный текст input-поля
     * @param cursorIndex позиция курсора в UTF-16 индексах, будет приведена к code point boundary
     * @return список элементов автодополнения
     */
    public List<AdminConsole.CompletionItem> complete(String inputText, int cursorIndex) {
        CompletionRequest request = CompletionRequest.parse(inputText, cursorIndex);
        if (request.commandMode()) {
            return completeCommand(request);
        }

        CommandDefinition command = find(request.commandName());
        if (command == null) return List.of();
        if (request.argumentIndex() < 0 || request.argumentIndex() >= command.arguments().size()) return List.of();
        if (!previousArgumentsValid(command, request)) return List.of();

        ArgumentDefinition argument = command.arguments().get(request.argumentIndex());
        if (argument.suggestions() == null) return List.of();

        CompletionContext context = new CompletionContext(
                command,
                request.argumentIndex(),
                argument,
                request.prefix(),
                request.inputText(),
                request.cursorIndex(),
                request.arguments());
        List<Suggestion> suggestions = argument.suggestions().suggest(context);
        if (suggestions == null || suggestions.isEmpty()) return List.of();

        List<AdminConsole.CompletionItem> result = new ArrayList<>();
        for (Suggestion suggestion : suggestions) {
            if (suggestion == null || suggestion.insertText().isEmpty()) continue;
            if (!matchesPrefix(request.prefix(), suggestion.insertText(), suggestion.displayText())) continue;
            String replacement = suggestion.insertText();
            if (suggestion.appendSpace() && !replacement.endsWith(" ")) {
                replacement += " ";
            }
            result.add(AdminConsole.CompletionItem.replace(
                    replacement,
                    suggestion.displayText(),
                    suggestion.description().isEmpty() ? argument.description() : suggestion.description(),
                    request.replaceStart(),
                    request.replaceEnd()));
        }
        return result;
    }

    private boolean previousArgumentsValid(CommandDefinition command, CompletionRequest request) {
        int count = Math.min(request.argumentIndex(), Math.min(command.arguments().size(), request.arguments().size()));
        for (int i = 0; i < count; i++) {
            ArgumentDefinition argument = command.arguments().get(i);
            String value = request.arguments().get(i);
            if (value == null || value.isBlank()) return false;
            if (argument.allowCustomValue() || argument.suggestions() == null) continue;
            CompletionContext context = new CompletionContext(
                    command,
                    i,
                    argument,
                    value,
                    request.inputText(),
                    request.cursorIndex(),
                    request.arguments());
            if (!matchesSuggestion(argument.suggestions().suggest(context), value)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesSuggestion(List<Suggestion> suggestions, String value) {
        if (suggestions == null || suggestions.isEmpty()) return false;
        String normalized = normalize(value, "").trim().toLowerCase(Locale.ROOT);
        for (Suggestion suggestion : suggestions) {
            if (suggestion == null) continue;
            if (normalize(suggestion.insertText(), "").trim().equalsIgnoreCase(normalized)) return true;
            if (normalize(suggestion.displayText(), "").trim().equalsIgnoreCase(normalized)) return true;
        }
        return false;
    }

    private List<AdminConsole.CompletionItem> completeCommand(CompletionRequest request) {
        String prefix = stripCommandPrefix(request.prefix()).toLowerCase(Locale.ROOT);
        List<AdminConsole.CompletionItem> result = new ArrayList<>();
        for (CommandDefinition command : commands.values()) {
            if (!prefix.isEmpty() && !command.name().startsWith(prefix)) continue;
            result.add(AdminConsole.CompletionItem.replace(
                    command.name() + " ",
                    command.name(),
                    command.signatureWithDescription(),
                    request.replaceStart(),
                    request.replaceEnd()));
        }
        return result;
    }

    private void notifyChanged() {
        version++;
        changeListener.run();
    }

    /**
     * Создаёт builder описания команды.
     *
     * @param name имя команды
     * @param description описание команды
     * @return новый builder команды
     */
    public static Builder command(String name, String description) {
        return new Builder(name, description);
    }

    /**
     * Создаёт provider подсказок из фиксированного массива значений.
     *
     * @param values возможные значения аргумента
     * @return provider подсказок
     */
    public static ArgumentSuggestionProvider fixedValues(String... values) {
        List<String> copy = values == null ? List.of() : List.of(values);
        return fixedValues(copy, "");
    }

    /**
     * Создаёт provider подсказок из фиксированной коллекции значений.
     *
     * @param values возможные значения аргумента
     * @param description описание, которое будет показано у каждой подсказки
     * @return provider подсказок
     */
    public static ArgumentSuggestionProvider fixedValues(Collection<String> values, String description) {
        List<String> copy = values == null ? List.of() : List.copyOf(values);
        return context -> suggestionsFrom(copy, description, true);
    }

    /**
     * Создаёт provider подсказок, который каждый раз читает актуальные значения из supplier'а.
     *
     * @param values supplier динамического списка значений
     * @param description описание, которое будет показано у каждой подсказки
     * @return provider подсказок
     */
    public static ArgumentSuggestionProvider dynamicValues(Supplier<? extends Collection<String>> values, String description) {
        return context -> suggestionsFrom(values == null ? List.of() : values.get(), description, true);
    }

    private static List<Suggestion> suggestionsFrom(Collection<String> values, String description, boolean appendSpace) {
        if (values == null || values.isEmpty()) return List.of();
        List<Suggestion> result = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            result.add(new Suggestion(value, value, description, appendSpace));
        }
        return result;
    }

    private static String stripCommandPrefix(String value) {
        String normalized = normalize(value, "").trim();
        return normalized.startsWith("/") ? normalized.substring(1) : normalized;
    }

    private static boolean matchesPrefix(String prefix, String insertText, String displayText) {
        String value = normalize(prefix, "").toLowerCase(Locale.ROOT);
        if (value.isEmpty()) return true;
        return normalize(insertText, "").toLowerCase(Locale.ROOT).startsWith(value)
                || normalize(displayText, "").toLowerCase(Locale.ROOT).startsWith(value);
    }

    private static String normalizeCommandName(String value) {
        String normalized = normalize(value, "").trim();
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalize(String value, String fallback) {
        return value == null ? fallback : value;
    }

    /**
     * Fluent-builder для описания команды, её аргументов и executor'а.
     */
    public static final class Builder {
        private final String name;
        private final String description;
        private final List<ArgumentDefinition> arguments = new ArrayList<>();
        private AdminConsole.CommandExecutor executor;

        private Builder(String name, String description) {
            this.name = name;
            this.description = description;
        }

        /**
         * Добавляет обязательный аргумент, значение которого должно совпадать с одной из подсказок.
         *
         * @param name имя аргумента для signature
         * @param description описание аргумента
         * @param suggestions provider значений
         * @return этот builder
         */
        public Builder argument(String name, String description, ArgumentSuggestionProvider suggestions) {
            arguments.add(new ArgumentDefinition(name, description, suggestions));
            return this;
        }

        /**
         * Добавляет обязательный аргумент с фиксированными значениями.
         *
         * @param name имя аргумента
         * @param description описание аргумента
         * @param values допустимые значения
         * @return этот builder
         */
        public Builder argument(String name, String description, Collection<String> values) {
            return argument(name, description, fixedValues(values, description));
        }

        /**
         * Добавляет обязательный аргумент с динамическими значениями.
         *
         * @param name имя аргумента
         * @param description описание аргумента
         * @param values supplier актуальных значений
         * @return этот builder
         */
        public Builder argument(String name, String description, Supplier<? extends Collection<String>> values) {
            return argument(name, description, dynamicValues(values, description));
        }

        /**
         * Добавляет аргумент с подсказками, но разрешает ввод произвольного значения.
         *
         * @param name имя аргумента
         * @param description описание аргумента
         * @param suggestions provider рекомендуемых значений
         * @return этот builder
         */
        public Builder customArgument(String name, String description, ArgumentSuggestionProvider suggestions) {
            arguments.add(new ArgumentDefinition(name, description, suggestions, true));
            return this;
        }

        /**
         * Добавляет аргумент с фиксированными подсказками и разрешённым произвольным вводом.
         *
         * @param name имя аргумента
         * @param description описание аргумента
         * @param values рекомендуемые значения
         * @return этот builder
         */
        public Builder customArgument(String name, String description, Collection<String> values) {
            return customArgument(name, description, fixedValues(values, description));
        }

        /**
         * Добавляет аргумент с динамическими подсказками и разрешённым произвольным вводом.
         *
         * @param name имя аргумента
         * @param description описание аргумента
         * @param values supplier рекомендуемых значений
         * @return этот builder
         */
        public Builder customArgument(String name, String description, Supplier<? extends Collection<String>> values) {
            return customArgument(name, description, dynamicValues(values, description));
        }

        /**
         * Задаёт обработчик выполнения команды.
         *
         * @param executor executor команды
         * @return этот builder
         */
        public Builder executor(AdminConsole.CommandExecutor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Собирает неизменяемое описание команды.
         *
         * @return описание команды
         */
        public CommandDefinition build() {
            return new CommandDefinition(name, description, arguments, executor);
        }
    }

    /**
     * Описание команды в реестре.
     *
     * @param name нормализованное имя команды без начального {@code /}
     * @param description описание команды
     * @param arguments список аргументов в порядке ввода
     * @param executor обработчик команды
     */
    public record CommandDefinition(String name, String description, List<ArgumentDefinition> arguments,
                                    AdminConsole.CommandExecutor executor) {
        public CommandDefinition {
            name = normalizeCommandName(name);
            description = normalize(description, "");
            arguments = arguments == null ? List.of() : List.copyOf(arguments);
        }

        /**
         * Возвращает краткую сигнатуру команды.
         *
         * @return строка вида {@code teleport <player> <x> <y> <z>}
         */
        public String signature() {
            if (arguments.isEmpty()) return name;
            StringBuilder builder = new StringBuilder(name);
            for (ArgumentDefinition argument : arguments) {
                builder.append(' ').append('<').append(argument.name()).append('>');
            }
            return builder.toString();
        }

        /**
         * Возвращает сигнатуру вместе с описанием.
         *
         * @return сигнатура и описание для help/autocomplete
         */
        public String signatureWithDescription() {
            String signature = signature();
            return description.isEmpty() ? signature : signature + " - " + description;
        }
    }

    /**
     * Описание одного аргумента команды.
     *
     * @param name имя аргумента для signature
     * @param description описание аргумента
     * @param suggestions provider подсказок или {@code null}
     * @param allowCustomValue {@code true}, если пользователь может ввести значение вне списка подсказок
     */
    public record ArgumentDefinition(String name, String description, ArgumentSuggestionProvider suggestions,
                                     boolean allowCustomValue) {
        /**
         * Создаёт аргумент, который требует значение из списка подсказок.
         *
         * @param name имя аргумента
         * @param description описание аргумента
         * @param suggestions provider допустимых значений
         */
        public ArgumentDefinition(String name, String description, ArgumentSuggestionProvider suggestions) {
            this(name, description, suggestions, false);
        }

        public ArgumentDefinition {
            name = normalize(name, "arg").trim();
            if (name.isEmpty()) name = "arg";
            description = normalize(description, "");
        }
    }

    /**
     * Контекст запроса подсказок для аргумента.
     *
     * @param command команда, для которой строятся подсказки
     * @param argumentIndex индекс текущего аргумента
     * @param argument описание текущего аргумента
     * @param prefix уже введённый префикс аргумента
     * @param inputText полный текст input-поля
     * @param cursorIndex позиция курсора
     * @param arguments уже разобранные аргументы команды
     */
    public record CompletionContext(CommandDefinition command, int argumentIndex, ArgumentDefinition argument,
                                    String prefix, String inputText, int cursorIndex, List<String> arguments) {
        public CompletionContext {
            prefix = normalize(prefix, "");
            inputText = normalize(inputText, "");
            cursorIndex = TextEditorModel.clampToCodePointBoundary(inputText, cursorIndex);
            arguments = arguments == null ? List.of() : List.copyOf(arguments);
        }
    }

    /**
     * Одна подсказка значения аргумента.
     *
     * @param insertText текст, который будет вставлен
     * @param displayText текст, который увидит пользователь
     * @param description описание подсказки
     * @param appendSpace {@code true}, если после вставки нужно добавить пробел для перехода к следующему аргументу
     */
    public record Suggestion(String insertText, String displayText, String description, boolean appendSpace) {
        public Suggestion {
            insertText = normalize(insertText, "");
            displayText = normalize(displayText, insertText);
            description = normalize(description, "");
        }

        /**
         * Создаёт подсказку, где вставка и отображаемый текст равны одному значению.
         *
         * @param value значение аргумента
         * @return подсказка с автоматическим пробелом после вставки
         */
        public static Suggestion value(String value) {
            return new Suggestion(value, value, "", true);
        }

        /**
         * Создаёт подсказку со значением и описанием.
         *
         * @param value значение аргумента
         * @param description описание значения
         * @return подсказка с автоматическим пробелом после вставки
         */
        public static Suggestion value(String value, String description) {
            return new Suggestion(value, value, description, true);
        }

        /**
         * Создаёт raw-подсказку без автоматического пробела после вставки.
         *
         * @param insertText текст вставки
         * @param displayText отображаемый текст
         * @param description описание подсказки
         * @return подсказка без автоматического пробела
         */
        public static Suggestion raw(String insertText, String displayText, String description) {
            return new Suggestion(insertText, displayText, description, false);
        }
    }

    @FunctionalInterface
    /**
     * Provider значений для одного аргумента команды.
     */
    public interface ArgumentSuggestionProvider {
        /**
         * Возвращает подсказки для текущего аргумента.
         *
         * @param context контекст команды, аргумента и текущего ввода
         * @return список подсказок или пустой список
         */
        List<Suggestion> suggest(CompletionContext context);
    }

    private record CompletionRequest(String inputText, int cursorIndex, boolean commandMode, String commandName,
                                     int argumentIndex, String prefix, int replaceStart, int replaceEnd,
                                     List<String> arguments) {
        private CompletionRequest {
            inputText = normalize(inputText, "");
            cursorIndex = TextEditorModel.clampToCodePointBoundary(inputText, cursorIndex);
            commandName = normalizeCommandName(commandName);
            prefix = normalize(prefix, "");
            replaceStart = TextEditorModel.clampToCodePointBoundary(inputText, replaceStart);
            replaceEnd = TextEditorModel.clampToCodePointBoundary(inputText, Math.max(replaceStart, replaceEnd));
            arguments = arguments == null ? List.of() : List.copyOf(arguments);
        }

        static CompletionRequest parse(String inputText, int cursorIndex) {
            String input = normalize(inputText, "");
            int cursor = TextEditorModel.clampToCodePointBoundary(input, cursorIndex);
            List<Token> tokens = tokenize(input);
            if (tokens.isEmpty()) {
                return new CompletionRequest(input, cursor, true, "", -1,
                        input.substring(0, cursor).trim(), 0, input.length(), List.of());
            }

            Token command = tokens.get(0);
            if (cursor <= command.end()) {
                String prefix = input.substring(command.start(), Math.max(command.start(), cursor)).trim();
                return new CompletionRequest(input, cursor, true, "", -1,
                        prefix, command.start(), command.end(), List.of());
            }

            List<String> arguments = new ArrayList<>();
            for (int i = 1; i < tokens.size(); i++) {
                arguments.add(tokens.get(i).value());
            }

            int argumentIndex = 0;
            int replaceStart = cursor;
            int replaceEnd = cursor;
            String prefix = "";
            for (int i = 1; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                if (cursor >= token.start() && cursor <= token.end()) {
                    argumentIndex = i - 1;
                    replaceStart = token.start();
                    replaceEnd = token.end();
                    prefix = input.substring(token.start(), Math.max(token.start(), cursor));
                    return new CompletionRequest(input, cursor, false, command.value(), argumentIndex,
                            prefix, replaceStart, replaceEnd, arguments);
                }
                if (token.end() <= cursor) {
                    argumentIndex = i;
                }
            }
            return new CompletionRequest(input, cursor, false, command.value(), argumentIndex,
                    prefix, replaceStart, replaceEnd, arguments);
        }
    }

    private record Token(String value, int start, int end) {
    }

    private static List<Token> tokenize(String input) {
        if (input == null || input.isEmpty()) return List.of();
        List<Token> result = new ArrayList<>();
        int index = 0;
        int length = input.length();
        while (index < length) {
            while (index < length && Character.isWhitespace(input.charAt(index))) index++;
            if (index >= length) break;
            int start = index;
            StringBuilder value = new StringBuilder();
            boolean quoted = false;
            boolean escaped = false;
            while (index < length) {
                char ch = input.charAt(index);
                if (escaped) {
                    value.append(ch);
                    escaped = false;
                    index++;
                    continue;
                }
                if (ch == '\\') {
                    escaped = true;
                    index++;
                    continue;
                }
                if (ch == '"') {
                    quoted = !quoted;
                    index++;
                    continue;
                }
                if (Character.isWhitespace(ch) && !quoted) break;
                value.append(ch);
                index++;
            }
            result.add(new Token(value.toString(), start, index));
        }
        return result;
    }
}
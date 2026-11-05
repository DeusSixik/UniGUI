package dev.sixik.unigui.xaml.schema.builtin;

import dev.sixik.unigui.api.render.VectorPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Разбор упрощённого SVG-подобного синтаксиса {@code M/L/Q/C/Z} (только абсолютные команды).
 *
 * <p>Перенесено без изменения грамматики из прежнего {@code BuiltInDisplayXml}.</p>
 */
final class PathData {
    private PathData() {
    }

    static VectorPath parse(String value) {
        VectorPath path = new VectorPath();
        List<String> tokens = tokens(value);
        if (tokens.isEmpty()) return path;

        int[] index = {0};
        char command = 0;
        while (index[0] < tokens.size()) {
            String token = tokens.get(index[0]);
            if (isCommand(token)) {
                command = parseCommand(token);
                index[0]++;
                if (command == 'Z') {
                    path.close();
                    command = 0;
                    continue;
                }
            } else if (command == 0) {
                throw new IllegalArgumentException("Path data must start with a command.");
            }

            switch (command) {
                case 'M' -> {
                    path.moveTo(number(tokens, index, command), number(tokens, index, command));
                    command = 'L';
                }
                case 'L' -> path.lineTo(number(tokens, index, command), number(tokens, index, command));
                case 'Q' -> path.quadraticTo(
                        number(tokens, index, command), number(tokens, index, command),
                        number(tokens, index, command), number(tokens, index, command));
                case 'C' -> path.cubicTo(
                        number(tokens, index, command), number(tokens, index, command),
                        number(tokens, index, command), number(tokens, index, command),
                        number(tokens, index, command), number(tokens, index, command));
                default -> throw new IllegalArgumentException("Unsupported path command: " + command);
            }
        }
        return path;
    }

    private static List<String> tokens(String value) {
        if (value == null || value.isBlank()) return List.of();
        List<String> tokens = new ArrayList<>();
        int index = 0;
        while (index < value.length()) {
            char current = value.charAt(index);
            if (Character.isWhitespace(current) || current == ',' || current == ';' || current == '|') {
                index++;
                continue;
            }
            if (Character.isLetter(current)) {
                tokens.add(String.valueOf(current));
                index++;
                continue;
            }

            int start = index;
            if (current == '+' || current == '-') index++;
            boolean hasDigit = false;
            boolean hasDot = false;
            boolean hasExponent = false;
            while (index < value.length()) {
                char next = value.charAt(index);
                if (Character.isDigit(next)) {
                    hasDigit = true;
                    index++;
                } else if (next == '.' && !hasDot && !hasExponent) {
                    hasDot = true;
                    index++;
                } else if ((next == 'e' || next == 'E') && !hasExponent && hasDigit) {
                    hasExponent = true;
                    index++;
                    if (index < value.length()) {
                        char sign = value.charAt(index);
                        if (sign == '+' || sign == '-') index++;
                    }
                } else {
                    break;
                }
            }
            if (!hasDigit) {
                throw new IllegalArgumentException("Expected path number near: " + value.substring(start));
            }
            tokens.add(value.substring(start, index));
        }
        return tokens;
    }

    private static boolean isCommand(String token) {
        return token.length() == 1 && Character.isLetter(token.charAt(0));
    }

    private static char parseCommand(String token) {
        char command = Character.toUpperCase(token.charAt(0));
        return switch (command) {
            case 'M', 'L', 'Q', 'C', 'Z' -> command;
            default -> throw new IllegalArgumentException(
                    "Unsupported path command '" + token + "'. Supported commands: M, L, Q, C, Z.");
        };
    }

    private static float number(List<String> tokens, int[] index, char command) {
        if (index[0] >= tokens.size() || isCommand(tokens.get(index[0]))) {
            throw new IllegalArgumentException("Missing numeric coordinate for path command " + command + ".");
        }
        String token = tokens.get(index[0]++);
        try {
            return Float.parseFloat(token);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Expected numeric coordinate for path command " + command + ": " + token, failure);
        }
    }
}

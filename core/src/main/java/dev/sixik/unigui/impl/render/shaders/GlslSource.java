package dev.sixik.unigui.impl.render.shaders;

/**
 * Разбор текста GLSL, общий для преобразований исходников ({@link GlslUniformBlocks}, {@link GlslInterfaceLocations}).
 */
final class GlslSource {
    private GlslSource() {
    }

    /** Заменяет комментарии пробелами (переводы строк сохраняются), чтобы позиции совпадали с исходником. */
    static String maskComments(String source) {
        char[] chars = source.toCharArray();
        int i = 0;
        while (i < chars.length) {
            if (chars[i] == '/' && i + 1 < chars.length && chars[i + 1] == '/') {
                while (i < chars.length && chars[i] != '\n') chars[i++] = ' ';
            } else if (chars[i] == '/' && i + 1 < chars.length && chars[i + 1] == '*') {
                chars[i++] = ' ';
                chars[i++] = ' ';
                while (i < chars.length && !(chars[i] == '*' && i + 1 < chars.length && chars[i + 1] == '/')) {
                    if (chars[i] != '\n') chars[i] = ' ';
                    i++;
                }
                if (i < chars.length) {
                    chars[i++] = ' ';
                    chars[i++] = ' ';
                }
            } else {
                i++;
            }
        }
        return new String(chars);
    }

    /** Глубина фигурных скобок в каждой позиции (объявления внутри функций и блоков не трогаются). */
    static int[] braceDepth(String masked) {
        int[] depth = new int[masked.length() + 1];
        int current = 0;
        for (int i = 0; i < masked.length(); i++) {
            depth[i] = current;
            char c = masked.charAt(i);
            if (c == '{') current++;
            else if (c == '}') current = Math.max(0, current - 1);
        }
        depth[masked.length()] = current;
        return depth;
    }
}

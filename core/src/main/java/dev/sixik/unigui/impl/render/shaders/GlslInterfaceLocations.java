package dev.sixik.unigui.impl.render.shaders;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Расставляет явные {@code layout(location = N)} у входов и выходов шейдеров вершин и фрагментов.
 *
 * <p>Minecraft 26.3 компилирует GLSL через SPIR-V на любом бэкенде, а SPIR-V требует location у каждого
 * пользовательского {@code in}/{@code out}. Ванильные шейдеры объявляют их сами; шейдеры UniGUI и пользовательские
 * написаны без них, и этот класс добавляет их по правилам Minecraft:</p>
 * <ul>
 *     <li>входы вершин получают location по порядку (с форматом вершин Minecraft связывает их по имени);</li>
 *     <li>выходы вершин — по порядку, а входы фрагментов — location одноимённого выхода вершин (стадии
 *     Minecraft сопоставляет по location);</li>
 *     <li>выходы фрагментов — по порядку, начиная с 0.</li>
 * </ul>
 * <p>Уже заданные location сохраняются и не выдаются повторно. После {@code #version} добавляется
 * {@code #extension GL_ARB_separate_shader_objects : require}, как в ванильных шейдерах (location у varyings
 * в GLSL 330 требует этого расширения). Обрабатываются только объявления верхнего уровня вида
 * {@code [qualifiers] in|out type name[N];}; блоки интерфейса не поддерживаются.</p>
 */
public final class GlslInterfaceLocations {
    /** Расширение, без которого GLSL 330 не принимает location у varyings. */
    public static final String EXTENSION = "#extension GL_ARB_separate_shader_objects : require";

    private static final Pattern VERSION = Pattern.compile("(?m)^[ \\t]*#[ \\t]*version[^\\n]*$");
    private static final Pattern DECLARATION = Pattern.compile(
            "(?m)^[ \\t]*(layout\\s*\\(([^)]*)\\)\\s*)?"
                    + "((?:(?:flat|smooth|noperspective|centroid|invariant)\\s+)*)"
                    + "(in|out)\\s+(\\w+)\\s+(\\w+)\\s*(?:\\[\\s*(\\d+)\\s*])?\\s*;");
    private static final Pattern LOCATION = Pattern.compile("\\blocation\\s*=\\s*(\\d+)");

    private GlslInterfaceLocations() {
    }

    /**
     * Исходники обеих стадий с расставленными location.
     */
    public record Result(String vertex, String fragment) {
    }

    /**
     * @param vertex   GLSL шейдера вершин
     * @param fragment GLSL шейдера фрагментов
     * @return исходники с явными location у всех входов и выходов
     */
    public static Result assign(String vertex, String fragment) {
        List<Declaration> vertexDeclarations = parse(vertex);
        List<Declaration> fragmentDeclarations = parse(fragment);

        assignSequential(vertexDeclarations, "in");
        assignSequential(vertexDeclarations, "out");
        Map<String, Integer> varyings = new HashMap<>();
        for (Declaration declaration : vertexDeclarations) {
            if (declaration.direction.equals("out")) varyings.put(declaration.name, declaration.location);
        }

        // Входы фрагментов: location одноимённого выхода вершин, остальным — свободные после занятых.
        BitSet used = usedLocations(fragmentDeclarations, "in");
        for (Declaration declaration : fragmentDeclarations) {
            if (!declaration.direction.equals("in") || declaration.location >= 0) continue;
            Integer location = varyings.get(declaration.name);
            if (location != null) {
                declaration.location = location;
                used.set(location, location + declaration.slots);
            }
        }
        assignSequential(fragmentDeclarations, "in", used);
        assignSequential(fragmentDeclarations, "out");

        return new Result(apply(vertex, vertexDeclarations), apply(fragment, fragmentDeclarations));
    }

    private static List<Declaration> parse(String source) {
        String masked = GlslSource.maskComments(source);
        int[] depth = GlslSource.braceDepth(masked);
        List<Declaration> declarations = new ArrayList<>();
        Matcher matcher = DECLARATION.matcher(masked);
        while (matcher.find()) {
            if (depth[matcher.start()] != 0) continue;
            String name = matcher.group(6);
            if (name.startsWith("gl_")) continue;
            int location = -1;
            boolean hasLayout = matcher.group(1) != null;
            if (hasLayout) {
                Matcher explicit = LOCATION.matcher(matcher.group(2));
                if (explicit.find()) location = Integer.parseInt(explicit.group(1));
            }
            int arrayLength = matcher.group(7) == null ? 1 : Integer.parseInt(matcher.group(7));
            int start = hasLayout ? matcher.start(1) : (matcher.group(3).isEmpty() ? matcher.start(4) : matcher.start(3));
            declarations.add(new Declaration(matcher.group(4), name, slots(matcher.group(5)) * arrayLength,
                    location, location >= 0 ? -1 : start,
                    hasLayout && location < 0 ? matcher.start(2) : -1));
        }
        return declarations;
    }

    /** Число location, которое занимает значение типа (матрица — по столбцу на location). */
    private static int slots(String type) {
        if (type.startsWith("mat") || type.startsWith("dmat")) {
            String size = type.substring(type.indexOf("mat") + 3);
            return Math.max(1, Character.getNumericValue(size.charAt(0)));
        }
        return 1;
    }

    private static BitSet usedLocations(List<Declaration> declarations, String direction) {
        BitSet used = new BitSet();
        for (Declaration declaration : declarations) {
            if (declaration.direction.equals(direction) && declaration.location >= 0) {
                used.set(declaration.location, declaration.location + declaration.slots);
            }
        }
        return used;
    }

    private static void assignSequential(List<Declaration> declarations, String direction) {
        assignSequential(declarations, direction, usedLocations(declarations, direction));
    }

    private static void assignSequential(List<Declaration> declarations, String direction, BitSet used) {
        int next = 0;
        for (Declaration declaration : declarations) {
            if (!declaration.direction.equals(direction) || declaration.location >= 0) continue;
            while (!free(used, next, declaration.slots)) next++;
            declaration.location = next;
            used.set(next, next + declaration.slots);
            next += declaration.slots;
        }
    }

    private static boolean free(BitSet used, int from, int count) {
        int set = used.nextSetBit(from);
        return set < 0 || set >= from + count;
    }

    /** Вставляет location (с конца, чтобы позиции не сдвигались) и расширение после {@code #version}. */
    private static String apply(String source, List<Declaration> declarations) {
        StringBuilder out = new StringBuilder(source);
        for (int i = declarations.size() - 1; i >= 0; i--) {
            Declaration declaration = declarations.get(i);
            if (declaration.layoutArgumentsAt >= 0) {
                out.insert(declaration.layoutArgumentsAt, "location = " + declaration.location + ", ");
            } else if (declaration.insertAt >= 0) {
                out.insert(declaration.insertAt, "layout(location = " + declaration.location + ") ");
            }
        }
        String result = out.toString();
        if (GlslSource.maskComments(result).contains("GL_ARB_separate_shader_objects")) return result;
        Matcher version = VERSION.matcher(GlslSource.maskComments(result));
        if (!version.find()) return "#version 330\n" + EXTENSION + "\n" + result;
        return result.substring(0, version.end()) + "\n" + EXTENSION + result.substring(version.end());
    }

    private static final class Declaration {
        final String direction;
        final String name;
        final int slots;
        int location;
        /** Позиция для {@code layout(location = N) } или -1, если location уже задан. */
        final int insertAt;
        /** Позиция аргументов существующего {@code layout(...)} без location или -1. */
        final int layoutArgumentsAt;

        Declaration(String direction, String name, int slots, int location, int insertAt, int layoutArgumentsAt) {
            this.direction = direction;
            this.name = name;
            this.slots = slots;
            this.location = location;
            this.insertAt = insertAt;
            this.layoutArgumentsAt = layoutArgumentsAt;
        }
    }
}

package dev.sixik.unigui.impl.render.shaders;

import dev.sixik.unigui.api.render.shaders.ShaderUniform;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Переводит пользовательский GLSL с обычными {@code uniform}-переменными в вид, который принимают современные
 * графические API: все такие переменные обеих стадий собираются в один uniform-блок {@code layout(std140)}.
 *
 * <p>Зачем: Vulkan (и пайплайны Minecraft 26.2 поверх него) не принимает непрозрачные {@code uniform} вне блоков,
 * а пользовательские шейдеры UniGUI написаны под GLSL 150 с обычными {@code uniform float X;}. Блок объявляется
 * без имени экземпляра, поэтому его члены видны в коде под прежними именами и тело шейдера не меняется.</p>
 *
 * <p>Что делается с исходником каждой стадии:</p>
 * <ul>
 *     <li>убирается BOM, {@code #version} ниже 330 поднимается до {@code 330} (отсутствующий добавляется);</li>
 *     <li>объявления {@code uniform} верхнего уровня с не-сэмплерными типами удаляются, а на место первого из них
 *     вставляется общий блок (объявление всегда идёт раньше использования, поэтому так безопасно);</li>
 *     <li>сэмплеры остаются как есть, их имена собираются в {@link Result#samplers()}.</li>
 * </ul>
 *
 * <p>Блок одинаков в обеих стадиях: в нём объединение переменных вершинной и фрагментной стадий (как требует
 * компоновка GLSL). Смещения считаются по правилам std140 ({@link Kind}), значения пишет
 * {@link Result#write(Map, ByteBuffer)}.</p>
 *
 * <p>Не поддерживаются (конвертация отклоняется с {@link IllegalArgumentException}): инициализаторы у uniform,
 * структуры и неизвестные типы, одно имя с разными типами в двух стадиях, GLSL ES.</p>
 */
public final class GlslUniformBlocks {
    /** Имя uniform-блока по умолчанию. */
    public static final String DEFAULT_BLOCK = "UniGuiUniforms";

    /** Минимальная версия GLSL после конвертации. */
    private static final int MIN_VERSION = 330;

    private static final Pattern VERSION = Pattern.compile("(?m)^[ \\t]*#[ \\t]*version[ \\t]+(\\d+)([ \\t]+\\w+)?[ \\t]*$");
    private static final Pattern UNIFORM = Pattern.compile(
            "\\buniform\\s+(?:(?:lowp|mediump|highp)\\s+)?(\\w+)\\s+([^;{}]+);");
    private static final Pattern DECLARATOR = Pattern.compile("\\s*(\\w+)\\s*(?:\\[\\s*(\\d+)\\s*])?\\s*");

    private GlslUniformBlocks() {
    }

    /**
     * Тип члена uniform-блока и его размещение по std140.
     *
     * <p>std140: скаляр выравнивается на 4 байта, {@code vec2} — на 8, {@code vec3}/{@code vec4} — на 16;
     * матрица — массив столбцов {@code vecN}, каждый столбец выровнен на 16; элемент массива выровнен на 16 и
     * занимает кратное 16 число байт.</p>
     */
    public enum Kind {
        FLOAT(1, 1, false, false), INT(1, 1, true, false), UINT(1, 1, true, false), BOOL(1, 1, true, true),
        VEC2(2, 1, false, false), VEC3(3, 1, false, false), VEC4(4, 1, false, false),
        IVEC2(2, 1, true, false), IVEC3(3, 1, true, false), IVEC4(4, 1, true, false),
        UVEC2(2, 1, true, false), UVEC3(3, 1, true, false), UVEC4(4, 1, true, false),
        BVEC2(2, 1, true, true), BVEC3(3, 1, true, true), BVEC4(4, 1, true, true),
        MAT2(2, 2, false, false), MAT3(3, 3, false, false), MAT4(4, 4, false, false);

        /** Компонент в столбце (для скаляров и векторов — в значении). */
        public final int components;
        /** Число столбцов (у не-матриц {@code 1}). */
        public final int columns;
        /** Хранится как 32-битное целое. */
        public final boolean integer;
        /** Булев тип (пишется как {@code 0}/{@code 1}). */
        public final boolean bool;

        Kind(int components, int columns, boolean integer, boolean bool) {
            this.components = components;
            this.columns = columns;
            this.integer = integer;
            this.bool = bool;
        }

        /**
         * @return базовое выравнивание одиночного значения в байтах
         */
        int alignment() {
            if (columns > 1) return 16;
            return components == 1 ? 4 : components == 2 ? 8 : 16;
        }

        /**
         * @return размер одиночного значения в байтах (матрица: столбцы с шагом 16)
         */
        int size() {
            return columns > 1 ? columns * 16 : components * 4;
        }

        /**
         * @return шаг между столбцами (матрица) или {@code 0}
         */
        int columnStride() {
            return columns > 1 ? 16 : 0;
        }

        static Kind parse(String glslType) {
            return switch (glslType) {
                case "float" -> FLOAT;
                case "int" -> INT;
                case "uint" -> UINT;
                case "bool" -> BOOL;
                case "vec2" -> VEC2;
                case "vec3" -> VEC3;
                case "vec4" -> VEC4;
                case "ivec2" -> IVEC2;
                case "ivec3" -> IVEC3;
                case "ivec4" -> IVEC4;
                case "uvec2" -> UVEC2;
                case "uvec3" -> UVEC3;
                case "uvec4" -> UVEC4;
                case "bvec2" -> BVEC2;
                case "bvec3" -> BVEC3;
                case "bvec4" -> BVEC4;
                case "mat2", "mat2x2" -> MAT2;
                case "mat3", "mat3x3" -> MAT3;
                case "mat4", "mat4x4" -> MAT4;
                default -> null;
            };
        }

        String glsl() {
            return name().toLowerCase();
        }
    }

    /**
     * Член uniform-блока.
     *
     * @param name        имя в шейдере
     * @param kind        тип
     * @param arrayLength длина массива или {@code 0}, если это не массив
     * @param offset      смещение в блоке по std140, байт
     */
    public record Member(String name, Kind kind, int arrayLength, int offset) {
        /**
         * @return шаг элемента массива (кратен 16) или размер одиночного значения
         */
        int elementStride() {
            return arrayLength > 0 ? roundUp(kind.size(), 16) : kind.size();
        }

        /**
         * @return занимаемый размер в байтах
         */
        int byteSize() {
            return arrayLength > 0 ? elementStride() * arrayLength : kind.size();
        }

        String declaration() {
            return kind.glsl() + " " + name + (arrayLength > 0 ? "[" + arrayLength + "]" : "") + ";";
        }
    }

    /**
     * Результат конвертации.
     *
     * @param vertex    вершинный шейдер ({@code null}, если не передавался)
     * @param fragment  фрагментный шейдер
     * @param blockName имя uniform-блока
     * @param members   члены блока в порядке размещения (пусто — блока нет)
     * @param samplers  имена сэмплеров обеих стадий
     * @param blockSize размер блока в байтах (кратен 16; {@code 0}, если блока нет)
     */
    public record Result(String vertex, String fragment, String blockName, List<Member> members,
                         List<String> samplers, int blockSize) {
        /**
         * @return {@code true}, если в шейдере есть не-сэмплерные uniform-переменные
         */
        public boolean hasBlock() {
            return blockSize > 0;
        }

        /**
         * Пишет значения членов блока по std140 в {@code target} (с его текущей позиции, ровно {@link #blockSize()}
         * байт, порядок байт — native). Члены без значения обнуляются.
         *
         * <p>Значение подбирается по типу члена: float-компоненты берутся из {@link ShaderUniform#floats()}, а для
         * целочисленных членов — из {@link ShaderUniform#integer()} у uniform типа {@code INT} или усечением float.
         * Массивы и матрицы читают компоненты подряд (матрица — по столбцам, как {@code glUniformMatrix*} без
         * транспонирования).</p>
         *
         * @param values значения по имени uniform
         * @param target буфер, в котором осталось не меньше {@link #blockSize()} байт
         */
        public void write(Map<String, ShaderUniform> values, ByteBuffer target) {
            int base = target.position();
            ByteBuffer out = target.duplicate().order(ByteOrder.nativeOrder());
            for (int i = 0; i < blockSize; i++) out.put(base + i, (byte) 0);
            for (Member member : members) {
                ShaderUniform value = values == null ? null : values.get(member.name());
                if (value != null) writeMember(out, base, member, value);
            }
            target.position(base + blockSize);
        }

        private static void writeMember(ByteBuffer out, int base, Member member, ShaderUniform value) {
            Kind kind = member.kind();
            float[] floats = value.floats();
            boolean intSource = value.type() == ShaderUniform.Type.INT;
            int elements = Math.max(1, member.arrayLength());
            int available = intSource ? 1 : (floats == null ? 0 : floats.length);
            int component = 0;
            for (int element = 0; element < elements; element++) {
                int elementOffset = base + member.offset() + element * member.elementStride();
                for (int column = 0; column < kind.columns; column++) {
                    int columnOffset = elementOffset + column * kind.columnStride();
                    for (int row = 0; row < kind.components; row++, component++) {
                        if (component >= available) return;
                        int at = columnOffset + row * 4;
                        if (kind.integer) {
                            int number = intSource ? value.integer() : (int) floats[component];
                            out.putInt(at, kind.bool ? (number != 0 ? 1 : 0) : number);
                        } else {
                            out.putFloat(at, intSource ? value.integer() : floats[component]);
                        }
                    }
                }
            }
        }
    }

    /**
     * Конвертирует пару стадий с блоком {@link #DEFAULT_BLOCK}.
     *
     * @see #convert(String, String, String)
     */
    public static Result convert(String vertex, String fragment) {
        return convert(vertex, fragment, DEFAULT_BLOCK);
    }

    /**
     * Конвертирует пару стадий.
     *
     * @param vertex    вершинный шейдер или {@code null}
     * @param fragment  фрагментный шейдер
     * @param blockName имя uniform-блока
     * @return результат конвертации
     * @throws IllegalArgumentException если шейдер использует то, что конвертер не поддерживает
     */
    public static Result convert(String vertex, String fragment, String blockName) {
        if (fragment == null) throw new IllegalArgumentException("Fragment shader is missing");
        Stage vertexStage = vertex == null ? null : Stage.parse(vertex, "vertex");
        Stage fragmentStage = Stage.parse(fragment, "fragment");

        Map<String, Declared> declared = new LinkedHashMap<>();
        Set<String> samplers = new LinkedHashSet<>();
        for (Stage stage : vertexStage == null ? List.of(fragmentStage) : List.of(vertexStage, fragmentStage)) {
            for (Declared uniform : stage.uniforms) {
                Declared previous = declared.putIfAbsent(uniform.name, uniform);
                if (previous != null && (previous.kind != uniform.kind || previous.arrayLength != uniform.arrayLength)) {
                    throw new IllegalArgumentException("Uniform " + uniform.name
                            + " is declared with different types in vertex and fragment shaders");
                }
            }
            samplers.addAll(stage.samplers);
        }

        List<Member> members = new ArrayList<>();
        int offset = 0;
        for (Declared uniform : declared.values()) {
            int alignment = uniform.arrayLength > 0 ? 16 : uniform.kind.alignment();
            offset = roundUp(offset, alignment);
            Member member = new Member(uniform.name, uniform.kind, uniform.arrayLength, offset);
            members.add(member);
            offset += member.byteSize();
        }
        int blockSize = members.isEmpty() ? 0 : roundUp(offset, 16);
        String block = members.isEmpty() ? "" : blockSource(blockName, members);

        return new Result(
                vertexStage == null ? null : vertexStage.emit(block),
                fragmentStage.emit(block),
                blockName,
                Collections.unmodifiableList(members),
                List.copyOf(samplers),
                blockSize);
    }

    private static String blockSource(String blockName, List<Member> members) {
        // Одной строкой: блок встаёт на место первого объявления, и номера строк исходника не сдвигаются.
        StringBuilder builder = new StringBuilder("layout(std140) uniform ").append(blockName).append(" {");
        for (Member member : members) builder.append(' ').append(member.declaration());
        return builder.append(" };").toString();
    }

    private static int roundUp(int value, int alignment) {
        int remainder = value % alignment;
        return remainder == 0 ? value : value + alignment - remainder;
    }

    private record Declared(String name, Kind kind, int arrayLength) {
    }

    /** Разобранная стадия: исходник с вырезанными объявлениями и найденные uniform. */
    private static final class Stage {
        private final String source;
        private final List<int[]> removed = new ArrayList<>();
        private final List<Declared> uniforms = new ArrayList<>();
        private final List<String> samplers = new ArrayList<>();

        private Stage(String source) {
            this.source = source;
        }

        static Stage parse(String raw, String stageName) {
            String source = raw.startsWith("﻿") ? raw.substring(1) : raw;
            Stage stage = new Stage(source);
            String masked = maskComments(source);
            int[] depth = braceDepth(masked);
            Matcher matcher = UNIFORM.matcher(masked);
            while (matcher.find()) {
                if (depth[matcher.start()] != 0) continue;
                String type = matcher.group(1);
                String declarators = matcher.group(2);
                if (isOpaque(type)) {
                    for (String declarator : declarators.split(",")) {
                        Matcher name = DECLARATOR.matcher(declarator);
                        if (!name.matches()) {
                            throw new IllegalArgumentException("Unsupported sampler declaration in " + stageName
                                    + " shader: " + matcher.group().trim());
                        }
                        stage.samplers.add(name.group(1));
                    }
                    continue;
                }
                Kind kind = Kind.parse(type);
                if (kind == null) {
                    throw new IllegalArgumentException("Unsupported uniform type '" + type + "' in " + stageName
                            + " shader");
                }
                for (String declarator : declarators.split(",")) {
                    Matcher name = DECLARATOR.matcher(declarator);
                    if (!name.matches()) {
                        throw new IllegalArgumentException("Unsupported uniform declaration in " + stageName
                                + " shader (initializers are not supported): " + matcher.group().trim());
                    }
                    int length = name.group(2) == null ? 0 : Integer.parseInt(name.group(2));
                    if (name.group(2) != null && length <= 0) {
                        throw new IllegalArgumentException("Invalid uniform array length in " + stageName + " shader");
                    }
                    stage.uniforms.add(new Declared(name.group(1), kind, length));
                }
                stage.removed.add(new int[]{matcher.start(), matcher.end()});
            }
            return stage;
        }

        /**
         * Собирает итоговый исходник: версия не ниже 330, объявления вырезаны (переводы строк сохраняются, чтобы
         * номера строк в ошибках компилятора совпадали с исходником), блок — на месте первого объявления.
         */
        String emit(String block) {
            StringBuilder out = new StringBuilder(source.length() + block.length() + 32);
            int cursor = 0;
            for (int i = 0; i < removed.size(); i++) {
                int[] range = removed.get(i);
                out.append(source, cursor, range[0]);
                if (i == 0) out.append(block);
                String cut = source.substring(range[0], range[1]);
                for (int c = 0; c < cut.length(); c++) if (cut.charAt(c) == '\n') out.append('\n');
                cursor = range[1];
            }
            out.append(source, cursor, source.length());
            return withVersion(out.toString());
        }

        private static String withVersion(String source) {
            Matcher version = VERSION.matcher(maskComments(source));
            if (!version.find()) return "#version " + MIN_VERSION + "\n" + source;
            String profile = version.group(2) == null ? "" : version.group(2).trim();
            if ("es".equals(profile)) throw new IllegalArgumentException("GLSL ES shaders are not supported");
            int number = Integer.parseInt(version.group(1));
            if (number >= MIN_VERSION) return source;
            String suffix = profile.isEmpty() ? "" : " " + profile;
            return source.substring(0, version.start()) + "#version " + MIN_VERSION + suffix
                    + source.substring(version.end());
        }

        private static boolean isOpaque(String type) {
            return type.contains("sampler") || type.contains("image") || type.startsWith("atomic_");
        }

        /** Заменяет комментарии пробелами (переводы строк сохраняются), чтобы позиции совпадали с исходником. */
        private static String maskComments(String source) {
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
        private static int[] braceDepth(String masked) {
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
}

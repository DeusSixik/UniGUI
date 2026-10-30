package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.xml.XmlAttribute;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Generates a compact Markdown collection exclusively from {@link AiDoc}. */
public final class AiMarkdownGenerator {
    private static final String PACKAGE_PATH = "dev/sixik/unigui";

    private AiMarkdownGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path output = args.length == 0 ? Path.of("build/generated/ai-docs") : Path.of(args[0]);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = AiMarkdownGenerator.class.getClassLoader();
        List<Class<?>> types = discoverTypes(loader);
        writeCollection(output, types);
        System.out.println("Generated " + types.size() + " AI documentation pages in " + output.toAbsolutePath());
    }

    static String renderType(Class<?> type) {
        AiDoc typeDoc = type.getAnnotation(AiDoc.class);
        List<Executable> members = documentedMembers(type);
        if (typeDoc == null && members.isEmpty()) {
            throw new IllegalArgumentException("No @AiDoc annotations on " + type.getName());
        }

        StringBuilder markdown = new StringBuilder(4096)
                .append("# `").append(type.getSimpleName()).append('`');
        Class<?> parent = type.getSuperclass();
        List<String> relationships = new ArrayList<>();
        if (parent != null && parent != Object.class) {
            relationships.add("extends `" + parent.getSimpleName() + "`");
        }
        for (Class<?> implemented : type.getInterfaces()) {
            relationships.add((type.isInterface() ? "extends" : "implements")
                    + " `" + implemented.getSimpleName() + "`");
        }
        if (!relationships.isEmpty()) markdown.append(" (").append(String.join(", ", relationships)).append(')');
        markdown.append("\n\n");
        if (typeDoc != null) appendTypeDoc(markdown, typeDoc);
        appendMemberSections(markdown, members);
        if (typeDoc != null) appendExamples(markdown, typeDoc.examples());
        return markdown.toString();
    }

    private static List<Class<?>> discoverTypes(ClassLoader loader) throws IOException {
        Set<String> names = new TreeSet<>();
        Enumeration<URL> resources = loader.getResources(PACKAGE_PATH);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) scanDirectory(names, resource);
            else if ("jar".equals(resource.getProtocol())) scanJar(names, resource);
        }

        List<Class<?>> types = new ArrayList<>();
        for (String name : names) {
            try {
                Class<?> type = Class.forName(name, false, loader);
                if (type.isAnnotationPresent(AiDoc.class) || !documentedMembers(type).isEmpty()) types.add(type);
            } catch (ClassNotFoundException | LinkageError ignored) {
                // A documentation profile may omit optional platform dependencies.
            }
        }
        types.sort(Comparator.comparing(Class::getName));
        return List.copyOf(types);
    }

    private static List<Executable> documentedMembers(Class<?> type) {
        List<Executable> members = new ArrayList<>();
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (Modifier.isPublic(constructor.getModifiers()) && constructor.isAnnotationPresent(AiDoc.class)) {
                members.add(constructor);
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())
                    && !method.isBridge()
                    && !method.isSynthetic()
                    && method.isAnnotationPresent(AiDoc.class)) {
                members.add(method);
            }
        }
        members.sort(Comparator
                .comparingInt((Executable member) -> member.getAnnotation(AiDoc.class).sectionOrder())
                .thenComparing(member -> member.getAnnotation(AiDoc.class).section())
                .thenComparingInt(member -> member.getAnnotation(AiDoc.class).order())
                .thenComparing(AiMarkdownGenerator::signature));
        return members;
    }

    private static void appendTypeDoc(StringBuilder markdown, AiDoc doc) {
        if (!doc.summary().isBlank()) markdown.append(doc.summary().trim()).append("\n\n");
        if (!doc.purpose().isBlank()) {
            markdown.append("- **Purpose:** ").append(doc.purpose().trim()).append("\n\n");
        }
        appendList(markdown, "USE WHEN", doc.useWhen());

        if (doc.criticalRules().length > 0 || !doc.constraints().isBlank()) {
            markdown.append("### CRITICAL RULES & GOTCHAS:\n\n");
            appendBullets(markdown, doc.criticalRules());
            if (!doc.constraints().isBlank()) {
                markdown.append("- ").append(doc.constraints().trim()).append('\n');
            }
            markdown.append('\n');
        }

        List<String> related = normalized(doc.related());
        if (!related.isEmpty()) {
            markdown.append("- **Related:** ")
                    .append(related.stream()
                            .map(value -> "`" + value + "`")
                            .collect(java.util.stream.Collectors.joining(", ")))
                    .append("\n\n");
        }
    }

    private static void appendMemberSections(StringBuilder markdown, List<Executable> members) {
        Map<String, List<Executable>> sections = new LinkedHashMap<>();
        for (Executable member : members) {
            AiDoc doc = member.getAnnotation(AiDoc.class);
            String section = doc.section().isBlank() ? "API" : doc.section().trim();
            sections.computeIfAbsent(section, ignored -> new ArrayList<>()).add(member);
        }

        for (Map.Entry<String, List<Executable>> section : sections.entrySet()) {
            markdown.append("### ").append(section.getKey().toUpperCase(Locale.ROOT)).append("\n\n");
            for (Executable member : section.getValue()) appendMember(markdown, member);
            markdown.append('\n');
        }
    }

    private static void appendMember(StringBuilder markdown, Executable member) {
        AiDoc doc = member.getAnnotation(AiDoc.class);
        XmlAttribute xmlAttribute = member instanceof Method method
                ? method.getAnnotation(XmlAttribute.class) : null;
        String description = doc.summary().isBlank() && xmlAttribute != null
                ? xmlAttribute.description() : doc.summary();

        markdown.append("- `").append(signature(member)).append('`');
        if (member instanceof Method method) {
            markdown.append(" → ").append(typeName(method.getGenericReturnType()));
        }
        if (xmlAttribute != null && !xmlAttribute.defaultValue().isBlank()) {
            markdown.append(" (default: `").append(xmlAttribute.defaultValue()).append('`')
                    .append(')');
        }
        if (!description.isBlank()) markdown.append(" — ").append(description.trim());
        markdown.append('\n');

        if (!doc.purpose().isBlank()) markdown.append("  - Purpose: ").append(doc.purpose().trim()).append('\n');
        appendNestedBullets(markdown, "Constraints", doc.constraints().isBlank()
                ? new String[0] : new String[]{doc.constraints()});
        appendNestedBullets(markdown, "Rules", doc.criticalRules());
        appendNestedBullets(markdown, "Use when", doc.useWhen());
    }

    private static void appendNestedBullets(StringBuilder markdown, String heading, String[] values) {
        List<String> items = normalized(values);
        if (items.isEmpty()) return;
        markdown.append("  - ").append(heading).append(":\n");
        items.forEach(value -> markdown.append("    - ").append(value).append('\n'));
    }

    private static void appendExamples(StringBuilder markdown, AiDoc.Example[] examples) {
        if (examples.length == 0) return;
        markdown.append("### EXAMPLES\n\n");
        List<AiDoc.Example> ordered = java.util.Arrays.stream(examples)
                .sorted(Comparator.comparingInt(AiDoc.Example::order)
                        .thenComparing(AiDoc.Example::title))
                .toList();
        for (AiDoc.Example example : ordered) {
            markdown.append("#### ").append(example.title().trim()).append("\n\n")
                    .append(fence(example.code())).append(example.language().trim()).append('\n')
                    .append(example.code().stripTrailing()).append('\n')
                    .append(fence(example.code())).append("\n\n");
        }
    }

    private static void appendList(StringBuilder markdown, String title, String[] values) {
        List<String> items = normalized(values);
        if (items.isEmpty()) return;
        markdown.append("### ").append(title).append("\n\n");
        appendBullets(markdown, values);
        markdown.append('\n');
    }

    private static void appendBullets(StringBuilder markdown, String[] values) {
        normalized(values).forEach(value -> markdown.append("- ").append(value).append('\n'));
    }

    private static List<String> normalized(String[] values) {
        return java.util.Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank()).map(String::trim).toList();
    }

    private static String signature(Executable member) {
        String name = member instanceof Constructor<?> ? member.getDeclaringClass().getSimpleName() : member.getName();
        StringBuilder result = new StringBuilder(name).append('(');
        Type[] parameters = member.getGenericParameterTypes();
        for (int index = 0; index < parameters.length; index++) {
            if (index > 0) result.append(", ");
            result.append(typeName(parameters[index]));
        }
        return result.append(')').toString();
    }

    private static String typeName(Type type) {
        if (type instanceof Class<?> clazz) {
            if (clazz.isArray()) return typeName(clazz.getComponentType()) + "[]";
            return clazz.getSimpleName();
        }
        if (type instanceof ParameterizedType parameterized) {
            StringBuilder name = new StringBuilder(typeName(parameterized.getRawType())).append('<');
            Type[] arguments = parameterized.getActualTypeArguments();
            for (int index = 0; index < arguments.length; index++) {
                if (index > 0) name.append(", ");
                name.append(typeName(arguments[index]));
            }
            return name.append('>').toString();
        }
        if (type instanceof GenericArrayType array) return typeName(array.getGenericComponentType()) + "[]";
        if (type instanceof WildcardType wildcard) {
            Type[] lowerBounds = wildcard.getLowerBounds();
            if (lowerBounds.length > 0) return "? super " + typeName(lowerBounds[0]);
            Type[] upperBounds = wildcard.getUpperBounds();
            if (upperBounds.length == 0 || upperBounds[0] == Object.class) return "?";
            return "? extends " + typeName(upperBounds[0]);
        }
        if (type instanceof TypeVariable<?> variable) return variable.getName();
        return type.getTypeName();
    }

    private static String fence(String code) {
        int longest = 2;
        int current = 0;
        for (int index = 0; index < code.length(); index++) {
            if (code.charAt(index) == '`') {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }
        return "`".repeat(longest + 1);
    }

    private static void writeCollection(Path output, List<Class<?>> types) throws IOException {
        Path pages = output.resolve("pages");
        Files.createDirectories(pages);
        StringBuilder index = new StringBuilder("# UniGUI AI Documentation\n\n")
                .append("Generated exclusively from `@AiDoc`.\n\n");
        for (Class<?> type : types) {
            String fileName = type.getName().replace('.', '-').replace('$', '-') + ".md";
            Files.writeString(pages.resolve(fileName), renderType(type), StandardCharsets.UTF_8);
            index.append("- [`").append(type.getName()).append("`](<pages/").append(fileName).append(">)\n");
        }
        Files.writeString(output.resolve("index.md"), index.toString(), StandardCharsets.UTF_8);
    }

    private static void scanDirectory(Set<String> names, URL resource) throws IOException {
        try {
            Path root = Path.of(resource.toURI());
            if (!Files.isDirectory(root)) return;
            try (var stream = Files.walk(root)) {
                stream.filter(path -> path.toString().endsWith(".class"))
                        .filter(path -> !path.getFileName().toString().contains("$"))
                        .forEach(path -> names.add(toClassName(root, path)));
            }
        } catch (URISyntaxException ignored) {
        }
    }

    private static void scanJar(Set<String> names, URL resource) throws IOException {
        JarURLConnection connection = (JarURLConnection) resource.openConnection();
        try (JarFile jar = connection.getJarFile()) {
            String prefix = PACKAGE_PATH + "/";
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(prefix) && name.endsWith(".class") && !name.contains("$")) {
                    names.add(name.substring(0, name.length() - 6).replace('/', '.'));
                }
            }
        }
    }

    private static String toClassName(Path root, Path classFile) {
        String relative = root.relativize(classFile).toString().replace('\\', '/');
        return "dev.sixik.unigui." + relative.substring(0, relative.length() - 6).replace('/', '.');
    }
}

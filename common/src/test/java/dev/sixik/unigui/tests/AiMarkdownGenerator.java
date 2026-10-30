package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.docs.AiDoc;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
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

        StringBuilder markdown = new StringBuilder(2048)
                .append("# `").append(type.getName()).append("`\n\n");
        if (typeDoc != null) appendDoc(markdown, typeDoc);
        for (Executable member : members) {
            markdown.append("## `").append(signature(member)).append("`\n\n");
            appendDoc(markdown, member.getAnnotation(AiDoc.class));
        }
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
            if (Modifier.isPublic(method.getModifiers()) && method.isAnnotationPresent(AiDoc.class)) members.add(method);
        }
        members.sort(Comparator.comparing(AiMarkdownGenerator::signature));
        return members;
    }

    private static void appendDoc(StringBuilder markdown, AiDoc doc) {
        if (!doc.summary().isBlank()) markdown.append(doc.summary().trim()).append("\n\n");
        if (!doc.purpose().isBlank()) markdown.append("Purpose: ").append(doc.purpose().trim()).append("\n\n");
        appendList(markdown, "Use when", doc.useWhen());
        if (!doc.constraints().isBlank()) markdown.append("Constraints: ").append(doc.constraints().trim()).append("\n\n");
        appendList(markdown, "Related", doc.related());
    }

    private static void appendList(StringBuilder markdown, String title, String[] values) {
        List<String> normalized = java.util.Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank()).map(String::trim).toList();
        if (normalized.isEmpty()) return;
        markdown.append(title).append(":\n\n");
        normalized.forEach(value -> markdown.append("- ").append(value).append('\n'));
        markdown.append('\n');
    }

    private static String signature(Executable member) {
        String name = member instanceof Constructor<?> ? member.getDeclaringClass().getSimpleName() : member.getName();
        StringBuilder result = new StringBuilder(name).append('(');
        Class<?>[] parameters = member.getParameterTypes();
        for (int index = 0; index < parameters.length; index++) {
            if (index > 0) result.append(", ");
            result.append(parameters[index].isArray()
                    ? parameters[index].getComponentType().getSimpleName() + "[]"
                    : parameters[index].getSimpleName());
        }
        return result.append(')').toString();
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

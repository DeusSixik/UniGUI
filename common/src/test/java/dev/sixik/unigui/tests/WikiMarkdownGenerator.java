package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiGif;
import dev.sixik.unigui.api.docs.WikiIcon;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/** Generates site-ready Markdown pages from the UniGUI wiki annotations. */
public final class WikiMarkdownGenerator {
    private static final String PACKAGE_PATH = "dev/sixik/unigui";

    private WikiMarkdownGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path output = args.length == 0 ? Path.of("build/generated/wiki") : Path.of(args[0]);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = WikiMarkdownGenerator.class.getClassLoader();
        List<Class<?>> pages = discoverPages(loader);
        writePages(output, pages);
        System.out.println("Generated " + pages.size() + " wiki pages in " + output.toAbsolutePath());
    }

    static String renderPage(Class<?> type) {
        WikiPage page = type.getAnnotation(WikiPage.class);
        if (page == null) throw new IllegalArgumentException("Missing @WikiPage on " + type.getName());
        validatePage(type, page);

        StringBuilder markdown = new StringBuilder(4096);
        markdown.append("---\n")
                .append("title: ").append(yaml(page.title())).append('\n')
                .append("slug: ").append(yaml(page.slug())).append('\n')
                .append("category: ").append(yaml(page.category())).append('\n')
                .append("order: ").append(page.order()).append("\n---\n\n")
                .append("# ").append(page.title()).append("\n\n");

        if (!page.summary().isBlank()) markdown.append(page.summary().trim()).append("\n\n");
        WikiIcon icon = type.getAnnotation(WikiIcon.class);
        if (icon != null) {
            markdown.append("![").append(markdownText(icon.alt())).append("](<")
                    .append(markdownPath(icon.src())).append(">)\n\n");
        }
        for (WikiGif gif : type.getAnnotationsByType(WikiGif.class)) {
            markdown.append("![").append(markdownText(gif.alt())).append("](<")
                    .append(markdownPath(gif.src())).append(">)\n\n");
            if (!gif.caption().isBlank()) markdown.append('*').append(gif.caption().trim()).append("*\n\n");
        }

        List<WikiSection> sections = new ArrayList<>(List.of(type.getAnnotationsByType(WikiSection.class)));
        sections.sort(Comparator.comparingInt(WikiSection::order).thenComparing(WikiSection::title));
        for (WikiSection section : sections) {
            markdown.append("## ").append(section.title().trim()).append("\n\n")
                    .append(section.markdown().strip()).append("\n\n");
        }

        List<WikiExample> examples = new ArrayList<>(List.of(type.getAnnotationsByType(WikiExample.class)));
        examples.sort(Comparator.comparingInt(WikiExample::order).thenComparing(WikiExample::title));
        for (WikiExample example : examples) {
            markdown.append("## ").append(example.title().trim()).append("\n\n")
                    .append(fence(example.code())).append(example.language().trim()).append('\n')
                    .append(example.code().stripTrailing()).append('\n')
                    .append(fence(example.code())).append("\n\n");
        }

        appendDocumentedMethods(markdown, type);
        return markdown.toString();
    }

    private static List<Class<?>> discoverPages(ClassLoader loader) throws IOException {
        Set<String> names = new java.util.TreeSet<>();
        Enumeration<URL> resources = loader.getResources(PACKAGE_PATH);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                scanDirectory(names, resource);
            } else if ("jar".equals(resource.getProtocol())) {
                scanJar(names, resource);
            }
        }

        List<Class<?>> pages = new ArrayList<>();
        for (String name : names) {
            try {
                Class<?> type = Class.forName(name, false, loader);
                if (type.isAnnotationPresent(WikiPage.class)) pages.add(type);
            } catch (ClassNotFoundException | LinkageError ignored) {
                // Optional Minecraft or loader classes may not be present in every documentation runtime.
            }
        }
        pages.sort(Comparator
                .comparing((Class<?> type) -> type.getAnnotation(WikiPage.class).category().toLowerCase(Locale.ROOT))
                .thenComparingInt(type -> type.getAnnotation(WikiPage.class).order())
                .thenComparing(type -> type.getAnnotation(WikiPage.class).title().toLowerCase(Locale.ROOT)));
        validateUniqueSlugs(pages);
        return List.copyOf(pages);
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
            // Ignore classpath entries that cannot be represented as local paths.
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

    private static void writePages(Path output, List<Class<?>> pages) throws IOException {
        Path pageDirectory = output.resolve("pages");
        Files.createDirectories(pageDirectory);
        for (Class<?> type : pages) {
            WikiPage page = type.getAnnotation(WikiPage.class);
            Files.writeString(pageDirectory.resolve(page.slug() + ".md"), renderPage(type), StandardCharsets.UTF_8);
        }

        StringBuilder index = new StringBuilder("# UniGUI Wiki\n\n");
        index.append("This index and its pages are generated from runtime wiki annotations.\n\n");
        String category = null;
        for (Class<?> type : pages) {
            WikiPage page = type.getAnnotation(WikiPage.class);
            if (!page.category().equals(category)) {
                category = page.category();
                index.append("## ").append(category).append("\n\n");
            }
            index.append("- [").append(markdownText(page.title())).append("](<pages/")
                    .append(page.slug()).append(".md>)");
            if (!page.summary().isBlank()) index.append(": ").append(markdownText(page.summary()));
            index.append('\n');
        }
        Files.writeString(output.resolve("index.md"), index.toString(), StandardCharsets.UTF_8);
    }

    private static void appendDocumentedMethods(StringBuilder markdown, Class<?> type) {
        List<Executable> members = new ArrayList<>();
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.isAnnotationPresent(WikiMethod.class)) members.add(constructor);
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(WikiMethod.class)) members.add(method);
        }
        members.sort(Comparator
                .comparingInt((Executable member) -> member.getAnnotation(WikiMethod.class).order())
                .thenComparing(WikiMarkdownGenerator::memberSignature));
        if (members.isEmpty()) return;

        markdown.append("## API\n\n");
        for (Executable member : members) appendDocumentedMember(markdown, member);
    }

    private static void appendDocumentedMember(StringBuilder markdown, Executable member) {
        WikiMethod doc = member.getAnnotation(WikiMethod.class);
        markdown.append("### `").append(memberSignature(member)).append("`\n\n");
        if (!doc.title().isBlank()) markdown.append("**").append(doc.title().trim()).append("**\n\n");
        if (!doc.summary().isBlank()) markdown.append(doc.summary().trim()).append("\n\n");
        if (!doc.markdown().isBlank()) markdown.append(doc.markdown().strip()).append("\n\n");

        Parameter[] parameters = member.getParameters();
        if (parameters.length > 0) {
            markdown.append("| Parameter | Type | Description |\n| --- | --- | --- |\n");
            for (int index = 0; index < parameters.length; index++) {
                Parameter parameter = parameters[index];
                WikiParameter parameterDoc = parameter.getAnnotation(WikiParameter.class);
                String name = parameterDoc != null && !parameterDoc.name().isBlank()
                        ? parameterDoc.name().trim()
                        : (parameter.isNamePresent() ? parameter.getName() : "arg" + index);
                String description = parameterDoc == null ? "" : parameterDoc.description();
                markdown.append("| `").append(escapeCell(name)).append("` | `")
                        .append(escapeCell(typeName(parameter.getType()))).append("` | ")
                        .append(cell(description)).append(" |\n");
            }
            markdown.append('\n');
        }

        if (member instanceof Method method && method.getReturnType() != void.class) {
            WikiReturn returnDoc = method.getAnnotation(WikiReturn.class);
            markdown.append("Returns `").append(typeName(method.getReturnType())).append("`");
            if (returnDoc != null && !returnDoc.value().isBlank()) {
                markdown.append(": ").append(returnDoc.value().trim());
            }
            markdown.append(".\n\n");
        }
        WikiThrows[] throwsDocs = member.getAnnotationsByType(WikiThrows.class);
        if (throwsDocs.length > 0) {
            markdown.append("Throws:\n\n");
            for (WikiThrows throwsDoc : throwsDocs) {
                markdown.append("- `").append(throwsDoc.type().getSimpleName()).append("`: ")
                        .append(throwsDoc.description().trim()).append('\n');
            }
            markdown.append('\n');
        }
        appendExamples(markdown, member, 4);
    }

    private static void appendExamples(StringBuilder markdown, AnnotatedElement element, int headingLevel) {
        List<WikiExample> examples = new ArrayList<>(List.of(element.getAnnotationsByType(WikiExample.class)));
        examples.sort(Comparator.comparingInt(WikiExample::order).thenComparing(WikiExample::title));
        for (WikiExample example : examples) {
            markdown.append("#".repeat(headingLevel)).append(' ').append(example.title().trim()).append("\n\n")
                    .append(fence(example.code())).append(example.language().trim()).append('\n')
                    .append(example.code().stripTrailing()).append('\n')
                    .append(fence(example.code())).append("\n\n");
        }
    }

    private static String memberSignature(Executable member) {
        String name = member instanceof Constructor<?> ? member.getDeclaringClass().getSimpleName() : member.getName();
        StringBuilder signature = new StringBuilder(name).append('(');
        Class<?>[] parameters = member.getParameterTypes();
        for (int index = 0; index < parameters.length; index++) {
            if (index > 0) signature.append(", ");
            signature.append(typeName(parameters[index]));
        }
        return signature.append(')').toString();
    }

    private static String typeName(Class<?> type) {
        return type.isArray() ? typeName(type.getComponentType()) + "[]" : type.getSimpleName();
    }

    private static void validatePage(Class<?> type, WikiPage page) {
        if (page.title().isBlank()) throw new IllegalArgumentException("Blank wiki page title on " + type.getName());
        if (!page.slug().matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("Invalid wiki page slug '" + page.slug() + "' on " + type.getName());
        }
        if (page.category().isBlank()) throw new IllegalArgumentException("Blank wiki page category on " + type.getName());
        WikiIcon icon = type.getAnnotation(WikiIcon.class);
        if (icon != null && (icon.src().isBlank() || icon.alt().isBlank())) {
            throw new IllegalArgumentException("Wiki icon requires src and alt on " + type.getName());
        }
        for (WikiGif gif : type.getAnnotationsByType(WikiGif.class)) {
            if (gif.src().isBlank() || gif.alt().isBlank()) {
                throw new IllegalArgumentException("Wiki GIF requires src and alt on " + type.getName());
            }
        }
    }

    private static void validateUniqueSlugs(List<Class<?>> pages) {
        Set<String> slugs = new HashSet<>();
        for (Class<?> type : pages) {
            String slug = type.getAnnotation(WikiPage.class).slug();
            if (!slugs.add(slug)) throw new IllegalArgumentException("Duplicate wiki slug: " + slug);
        }
    }

    private static String yaml(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", " ").replace("\n", " ") + "\"";
    }

    private static String markdownPath(String value) {
        return value.replace("\\", "/").replace(">", "%3E");
    }

    private static String markdownText(String value) {
        return value.replace("[", "\\[").replace("]", "\\]")
                .replace("\r", " ").replace("\n", " ");
    }

    private static String cell(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? "—" : escapeCell(normalized);
    }

    private static String escapeCell(String value) {
        return value.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
    }

    private static String fence(String code) {
        int longest = 2;
        int current = 0;
        for (int i = 0; i < code.length(); i++) {
            if (code.charAt(i) == '`') {
                longest = Math.max(longest, ++current);
            } else {
                current = 0;
            }
        }
        return "`".repeat(longest + 1);
    }
}

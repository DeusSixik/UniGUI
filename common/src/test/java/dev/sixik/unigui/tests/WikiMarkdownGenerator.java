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

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Locale;

import java.util.Map;

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

        Path languages = args.length < 2 ? Path.of("docs/lang") : Path.of(args[1]);

        Path assets = args.length < 3 ? Path.of("docs/assets") : Path.of(args[2]);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        if (loader == null) loader = WikiMarkdownGenerator.class.getClassLoader();

        List<Class<?>> pages = discoverPages(loader);

        WikiLocalization localization = WikiLocalization.load(languages, warning -> System.err.println("[wiki] WARN: " + warning));

        writePages(output, pages, localization, assets);

        System.out.println("Generated " + pages.size() + " wiki pages for " + localization.locales().size()

                + " locales in " + output.toAbsolutePath());

    }



    static String renderPage(Class<?> type) {

        return renderPage(type, WikiLocalization.literalOnly(), WikiLocalization.DEFAULT_LOCALE, "");

    }



    static String renderPage(Class<?> type, WikiLocalization localization, String locale, String assetPrefix) {

        WikiPage page = type.getAnnotation(WikiPage.class);

        if (page == null) throw new IllegalArgumentException("Missing @WikiPage on " + type.getName());

        String context = type.getName();

        String title = localization.resolve(page.title(), locale, context + ".title");

        String category = localization.resolve(page.category(), locale, context + ".category");

        String summary = localization.resolve(page.summary(), locale, context + ".summary");

        validatePage(type, page, title, category);



        StringBuilder markdown = new StringBuilder(4096);

        markdown.append("---\n")

                .append("title: ").append(yaml(title)).append('\n')

                .append("slug: ").append(yaml(page.slug())).append('\n')

                .append("category: ").append(yaml(category)).append('\n')

                .append("locale: ").append(yaml(locale)).append('\n')

                .append("order: ").append(page.order()).append("\n---\n\n")

                .append("# ").append(title).append("\n\n");



        if (!summary.isBlank()) markdown.append(summary.trim()).append("\n\n");

        WikiIcon icon = type.getAnnotation(WikiIcon.class);

        if (icon != null) {

            String src = localizedAsset(localization.resolve(icon.src(), locale, context + ".icon.src"), assetPrefix);

            String alt = localization.resolve(icon.alt(), locale, context + ".icon.alt");

            validateMedia(type, "icon", src, alt);

            markdown.append("![").append(markdownText(alt)).append("](<")

                    .append(markdownPath(src)).append(">)\n\n");

        }

        WikiGif[] gifs = type.getAnnotationsByType(WikiGif.class);

        for (int index = 0; index < gifs.length; index++) {

            WikiGif gif = gifs[index];

            String src = localizedAsset(localization.resolve(gif.src(), locale, context + ".gif[" + index + "].src"), assetPrefix);

            String alt = localization.resolve(gif.alt(), locale, context + ".gif[" + index + "].alt");

            String caption = localization.resolve(gif.caption(), locale, context + ".gif[" + index + "].caption");

            validateMedia(type, "GIF", src, alt);

            markdown.append("![").append(markdownText(alt)).append("](<")

                    .append(markdownPath(src)).append(">)\n\n");

            if (!caption.isBlank()) markdown.append('*').append(caption.trim()).append("*\n\n");

        }



        List<WikiSection> sections = new ArrayList<>(List.of(type.getAnnotationsByType(WikiSection.class)));

        sections.sort(Comparator.comparingInt(WikiSection::order).thenComparing(WikiSection::title));

        for (int index = 0; index < sections.size(); index++) {

            WikiSection section = sections.get(index);

            markdown.append("## ").append(localization.resolve(section.title(), locale,

                            context + ".section[" + index + "].title").trim()).append("\n\n")

                    .append(localization.resolve(section.markdown(), locale,

                            context + ".section[" + index + "].markdown").strip()).append("\n\n");

        }



        List<WikiExample> examples = new ArrayList<>(List.of(type.getAnnotationsByType(WikiExample.class)));

        examples.sort(Comparator.comparingInt(WikiExample::order).thenComparing(WikiExample::title));

        for (int index = 0; index < examples.size(); index++) {

            WikiExample example = examples.get(index);

            String code = localization.resolve(example.code(), locale,

                    context + ".example[" + index + "].code");

            markdown.append("## ").append(localization.resolve(example.title(), locale,

                            context + ".example[" + index + "].title").trim()).append("\n\n")

                    .append(fence(code)).append(example.language().trim()).append('\n')

                    .append(code.stripTrailing()).append('\n')

                    .append(fence(code)).append("\n\n");

        }



        appendDocumentedMethods(markdown, type, localization, locale);

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



    static void writePages(Path output, List<Class<?>> pages,

                           WikiLocalization localization, Path sourceAssets) throws IOException {

        Files.createDirectories(output);

        copyAssets(sourceAssets, output.resolve("assets"));

        for (String locale : localization.locales()) {

            Path localeDirectory = output.resolve(locale);

            Files.createDirectories(localeDirectory);

            StringBuilder index = new StringBuilder("# ")

                    .append(localization.resolve("<key:wiki.index.title>", locale, "wiki.index.title"))

                    .append("\n\n");

            String currentCategory = null;

            for (Class<?> type : pages) {

                WikiPage page = type.getAnnotation(WikiPage.class);

                String context = type.getName();

                String title = localization.resolve(page.title(), locale, context + ".title");

                String category = localization.resolve(page.category(), locale, context + ".category");

                String englishCategory = localization.resolve(page.category(), WikiLocalization.DEFAULT_LOCALE,

                        context + ".category");

                String categoryDirectory = categoryPath(englishCategory);

                Path pageDirectory = localeDirectory.resolve(categoryDirectory);

                Files.createDirectories(pageDirectory);

                int categoryDepth = categoryDirectory.split("/").length;

                Files.writeString(pageDirectory.resolve(page.slug() + ".md"),

                        renderPage(type, localization, locale, "../".repeat(categoryDepth + 1)), StandardCharsets.UTF_8);



                if (!category.equals(currentCategory)) {

                    currentCategory = category;

                    index.append("## ").append(category.replace("/", " / ")).append("\n\n");

                }

                index.append("- [").append(markdownText(title)).append("](<")

                        .append(categoryDirectory).append('/').append(page.slug()).append(".md>)");

                String summary = localization.resolve(page.summary(), locale, context + ".summary");

                if (!summary.isBlank()) index.append(": ").append(markdownText(summary));

                index.append('\n');

            }

            Files.writeString(localeDirectory.resolve("index.md"), index.toString(), StandardCharsets.UTF_8);

        }

    }



    private static void appendDocumentedMethods(StringBuilder markdown, Class<?> type,

                                                WikiLocalization localization, String locale) {

        List<Executable> members = new ArrayList<>();

        for (Constructor<?> constructor : type.getDeclaredConstructors()) {

            if (!constructor.isSynthetic() && constructor.isAnnotationPresent(WikiMethod.class)) members.add(constructor);

        }

        for (Method method : type.getDeclaredMethods()) {

            if (!method.isBridge() && !method.isSynthetic() && method.isAnnotationPresent(WikiMethod.class)) members.add(method);

        }

        if (members.isEmpty()) return;



        markdown.append("## ").append(localization.resolve("<key:wiki.api.title>", locale, "wiki.api.title"))

                .append("\n\n");



        boolean hasSections = members.stream()

                .anyMatch(member -> !member.getAnnotation(WikiMethod.class).section().isBlank());



        if (!hasSections) {

            members.sort(Comparator

                    .comparingInt((Executable member) -> member.getAnnotation(WikiMethod.class).order())

                    .thenComparing(WikiMarkdownGenerator::memberSignature));

            for (Executable member : members) appendDocumentedMember(markdown, member, 3, localization, locale);

            return;

        }



        members.sort(Comparator

                .comparingInt((Executable member) -> member.getAnnotation(WikiMethod.class).sectionOrder())

                .thenComparingInt(member -> member.getAnnotation(WikiMethod.class).order())

                .thenComparing(WikiMarkdownGenerator::memberSignature));



        Map<String, List<Executable>> sections = new LinkedHashMap<>();

        for (Executable member : members) {

            WikiMethod doc = member.getAnnotation(WikiMethod.class);

            String sectionKey = doc.section().isBlank() ? "<key:wiki.api.title>" : doc.section();

            sections.computeIfAbsent(sectionKey, ignored -> new ArrayList<>()).add(member);

        }



        for (Map.Entry<String, List<Executable>> entry : sections.entrySet()) {

            String sectionTitle = localization.resolve(entry.getKey(), locale, type.getName() + ".api.section");

            markdown.append("### ").append(sectionTitle.trim()).append("\n\n");



            markdown.append("| ")

                    .append(localization.resolve("<key:wiki.api.method>", locale, "wiki.api.method"))

                    .append(" | ")

                    .append(localization.resolve("<key:wiki.api.description>", locale, "wiki.api.description"))

                    .append(" |\n| --- | --- |\n");



            for (Executable member : entry.getValue()) {

                WikiMethod doc = member.getAnnotation(WikiMethod.class);

                String sig = memberSignature(member);

                String anchor = slugifyAnchor(sig);

                String summary = localization.resolve(doc.summary(), locale,

                        member.getDeclaringClass().getName() + "#" + sig + ".summary");

                if (summary.isBlank() && !doc.title().isBlank()) {

                    summary = localization.resolve(doc.title(), locale,

                            member.getDeclaringClass().getName() + "#" + sig + ".title");

                }

                markdown.append("| [`").append(escapeCell(sig)).append("`](#").append(anchor).append(") | ")

                        .append(cell(summary)).append(" |\n");

            }

            markdown.append('\n');



            for (Executable member : entry.getValue()) {

                appendDocumentedMember(markdown, member, 4, localization, locale);

            }

        }

    }



    private static String slugifyAnchor(String value) {

        String cleaned = value.replaceAll("[^a-zA-Z0-9\\s_-]", "").trim();

        return cleaned.replaceAll("\\s+", "-").toLowerCase(Locale.ROOT);

    }



    private static void appendDocumentedMember(StringBuilder markdown, Executable member, int headingLevel,

                                               WikiLocalization localization, String locale) {

        markdown.append("<div class=\"api-member-card\" markdown=\"1\">\n\n");

        WikiMethod doc = member.getAnnotation(WikiMethod.class);

        String context = member.getDeclaringClass().getName() + "#" + memberSignature(member);

        markdown.append("#".repeat(headingLevel)).append(" `").append(memberSignature(member)).append("`\n\n");

        String title = localization.resolve(doc.title(), locale, context + ".title");

        String summary = localization.resolve(doc.summary(), locale, context + ".summary");

        String details = localization.resolve(doc.markdown(), locale, context + ".markdown");

        if (!title.isBlank()) markdown.append("**").append(title.trim()).append("**\n\n");

        if (!summary.isBlank()) markdown.append(summary.trim()).append("\n\n");

        if (!details.isBlank()) markdown.append(details.strip()).append("\n\n");



        Parameter[] parameters = member.getParameters();

        if (parameters.length > 0) {

            markdown.append("| ").append(localization.resolve("<key:wiki.api.parameter>", locale, "wiki.api.parameter"))

                    .append(" | ").append(localization.resolve("<key:wiki.api.type>", locale, "wiki.api.type"))

                    .append(" | ").append(localization.resolve("<key:wiki.api.description>", locale, "wiki.api.description"))

                    .append(" |\n| --- | --- | --- |\n");

            for (int index = 0; index < parameters.length; index++) {

                Parameter parameter = parameters[index];

                WikiParameter parameterDoc = parameter.getAnnotation(WikiParameter.class);

                String name = parameterDoc != null && !parameterDoc.name().isBlank()

                        ? localization.resolve(parameterDoc.name(), locale, context + ".parameter[" + index + "].name").trim()

                        : (parameter.isNamePresent() ? parameter.getName() : "arg" + index);

                String description = parameterDoc == null ? "" : localization.resolve(parameterDoc.description(), locale,

                        context + ".parameter[" + index + "].description");

                markdown.append("| `").append(escapeCell(name)).append("` | `")

                        .append(escapeCell(typeName(parameter.getType()))).append("` | ")

                        .append(cell(description)).append(" |\n");

            }

            markdown.append('\n');

        }



        if (member instanceof Method method && method.getReturnType() != void.class) {

            WikiReturn returnDoc = method.getAnnotation(WikiReturn.class);

            markdown.append(localization.resolve("<key:wiki.api.returns>", locale, "wiki.api.returns"))

                    .append(" `").append(typeName(method.getReturnType())).append("`");

            if (returnDoc != null && !returnDoc.value().isBlank()) {

                markdown.append(": ").append(localization.resolve(returnDoc.value(), locale,

                        context + ".return").trim());

            }

            markdown.append(".\n\n");

        }

        WikiThrows[] throwsDocs = member.getAnnotationsByType(WikiThrows.class);

        if (throwsDocs.length > 0) {

            markdown.append(localization.resolve("<key:wiki.api.throws>", locale, "wiki.api.throws"))

                    .append(":\n\n");

            for (WikiThrows throwsDoc : throwsDocs) {

                markdown.append("- `").append(throwsDoc.type().getSimpleName()).append("`: ")

                        .append(localization.resolve(throwsDoc.description(), locale,

                                context + ".throws." + throwsDoc.type().getName()).trim()).append('\n');

            }

            markdown.append('\n');

        }

        appendExamples(markdown, member, headingLevel + 1, localization, locale, context);

        markdown.append("</div>\n\n");

    }



    private static void appendExamples(StringBuilder markdown, AnnotatedElement element, int headingLevel,

                                       WikiLocalization localization, String locale, String context) {

        List<WikiExample> examples = new ArrayList<>(List.of(element.getAnnotationsByType(WikiExample.class)));

        examples.sort(Comparator.comparingInt(WikiExample::order).thenComparing(WikiExample::title));

        for (int index = 0; index < examples.size(); index++) {

            WikiExample example = examples.get(index);

            String title = localization.resolve(example.title(), locale, context + ".example[" + index + "].title");

            String code = localization.resolve(example.code(), locale, context + ".example[" + index + "].code");

            markdown.append("#".repeat(headingLevel)).append(' ').append(title.trim()).append("\n\n")

                    .append(fence(code)).append(example.language().trim()).append('\n')

                    .append(code.stripTrailing()).append('\n')

                    .append(fence(code)).append("\n\n");

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



    private static void validatePage(Class<?> type, WikiPage page, String title, String category) {

        if (title.isBlank()) throw new IllegalArgumentException("Blank wiki page title on " + type.getName());

        if (!page.slug().matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {

            throw new IllegalArgumentException("Invalid wiki page slug '" + page.slug() + "' on " + type.getName());

        }

        if (category.isBlank()) throw new IllegalArgumentException("Blank wiki page category on " + type.getName());

    }



    private static void validateMedia(Class<?> type, String kind, String src, String alt) {

        if (src.isBlank() || alt.isBlank()) {

            throw new IllegalArgumentException("Wiki " + kind + " requires resolved src and alt on " + type.getName());

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



    private static String localizedAsset(String value, String assetPrefix) {

        String normalized = value == null ? "" : value.replace('\\', '/');

        if (normalized.startsWith("/assets/")) normalized = normalized.substring(1);

        if (normalized.startsWith("assets/")) return assetPrefix + normalized;

        return normalized;

    }



    private static String slugify(String value) {

        String normalized = value == null ? "" : java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)

                .replaceAll("\\p{M}+", "")

                .toLowerCase(Locale.ROOT)

                .replaceAll("[^a-z0-9]+", "-")

                .replaceAll("^-+|-+$", "");

        return normalized;

    }



    private static String categoryPath(String englishCategory) {

        if (englishCategory == null || englishCategory.isBlank()) return "api";

        String[] sourceSegments = englishCategory.split("/", -1);

        List<String> pathSegments = new ArrayList<>(sourceSegments.length);

        for (String sourceSegment : sourceSegments) {

            String segment = slugify(sourceSegment);

            if (segment.isEmpty()) {

                throw new IllegalArgumentException("Invalid empty wiki category segment in '" + englishCategory + "'");

            }

            pathSegments.add(segment);

        }

        return String.join("/", pathSegments);

    }



    private static void copyAssets(Path source, Path target) throws IOException {

        Files.createDirectories(target);

        if (!Files.isDirectory(source)) return;

        try (var stream = Files.walk(source)) {

            for (Path path : stream.sorted().toList()) {

                Path relative = source.relativize(path);

                if (relative.getFileName() != null && relative.getFileName().toString().equals(".gitkeep")) continue;

                Path destination = target.resolve(relative.toString()).normalize();

                if (!destination.startsWith(target.normalize())) {

                    throw new IllegalStateException("Asset path escapes output directory: " + relative);

                }

                if (Files.isDirectory(path)) Files.createDirectories(destination);

                else Files.copy(path, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            }

        }

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


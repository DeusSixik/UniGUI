package dev.sixik.unigui.xaml.pack;

import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiParseException;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.XamlNamespaces;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.XamlLoadException;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Паспорт пака ресурсов ({@code pack.xaml} в корне архива).
 *
 * <pre>{@code
 * <Pack Id="mymod:shop" Version="1.2.0" Name="Shop UI" Description="Catalog window and cards"/>
 * }</pre>
 *
 * @param id          идентификатор пака: строчные буквы, цифры и {@code _ . : -}
 * @param version     версия пака (любой непустой текст; сравнение версий — дело вызывающего)
 * @param name        читаемое имя
 * @param description описание
 */
public record PackManifest(String id, String version, String name, String description) {
    /** Имя файла паспорта в корне архива. */
    public static final String FILE = "pack.xaml";
    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9_.:\\-]*");
    private static final Set<String> ATTRIBUTES = Set.of("Id", "Version", "Name", "Description");

    public PackManifest {
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Pack id '" + id + "' must be lowercase letters, digits and _ . : -");
        }
        if (version == null || version.isBlank()) throw new IllegalArgumentException("Pack version must not be blank");
        name = name == null || name.isBlank() ? id : name;
        description = description == null ? "" : description;
    }

    /**
     * Читает паспорт.
     *
     * @param text содержимое {@code pack.xaml}
     * @return паспорт
     * @throws XamlLoadException если документ неверен, с позицией
     */
    public static PackManifest parse(String text) {
        UiNode root;
        try {
            root = XamlFormat.INSTANCE.read(text).root();
        } catch (UiParseException failure) {
            throw new XamlLoadException("Cannot parse " + FILE + ": " + failure.reason(), failure.location(), failure);
        }
        if (!root.type().local().equals("Pack")) {
            throw new XamlLoadException(FILE + " must start with <Pack>, got <" + root.type().qualified() + ">.",
                    root.location(), null);
        }
        String id = null;
        String version = null;
        String name = null;
        String description = null;
        for (UiProperty property : root.properties()) {
            boolean plain = property.name().namespace().isEmpty() || XamlNamespaces.isLanguage(property.name().namespace());
            if (property.owner() != null || !plain || !ATTRIBUTES.contains(property.name().local())
                    || !(property.value() instanceof UiValue.Literal literal)) {
                throw new XamlLoadException("Unknown attribute '" + property.displayName() + "' on <Pack>. Allowed: "
                        + ATTRIBUTES + ".", property.location(), null);
            }
            switch (property.name().local()) {
                case "Id" -> id = literal.text().trim();
                case "Version" -> version = literal.text().trim();
                case "Name" -> name = literal.text().trim();
                default -> description = literal.text().trim();
            }
        }
        try {
            if (id == null) throw new IllegalArgumentException("<Pack> requires Id");
            if (version == null) throw new IllegalArgumentException("<Pack> requires Version");
            return new PackManifest(id, version, name, description);
        } catch (IllegalArgumentException failure) {
            throw new XamlLoadException(failure.getMessage(), root.location(), failure);
        }
    }

    /** @return паспорт как текст {@code pack.xaml} */
    public String toXaml() {
        return "<Pack Id=\"" + escape(id) + "\" Version=\"" + escape(version) + "\" Name=\"" + escape(name)
                + "\" Description=\"" + escape(description) + "\"/>\n";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

package dev.sixik.isf.importer;

import java.util.LinkedHashSet;
import java.util.Set;

/** Селекторы категорий для команды генерации. */
public record IsfImportRequest(Set<String> include, Set<String> exclude) {
    public IsfImportRequest {
        include = Set.copyOf(include == null ? Set.of() : new LinkedHashSet<>(include));
        exclude = Set.copyOf(exclude == null ? Set.of() : new LinkedHashSet<>(exclude));
    }

    public static IsfImportRequest parse(String selectors) {
        Set<String> include = new LinkedHashSet<>();
        Set<String> exclude = new LinkedHashSet<>();
        if (selectors != null && !selectors.isBlank()) {
            for (String token : selectors.split(",")) {
                String value = token.trim().toLowerCase(java.util.Locale.ROOT);
                if (value.isEmpty()) continue;
                boolean negative = value.charAt(0) == '-';
                if (negative) value = value.substring(1).trim();
                if (value.isEmpty()) throw new IllegalArgumentException("Empty ISF category selector");
                (negative ? exclude : include).add(value);
            }
        }
        return new IsfImportRequest(include, exclude);
    }

    public boolean accepts(String category) {
        String normalized = category == null ? "unknown" : category.toLowerCase(java.util.Locale.ROOT);
        return !exclude.contains(normalized) && (include.isEmpty() || include.contains(normalized));
    }
}

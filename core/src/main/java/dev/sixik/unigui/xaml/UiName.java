package dev.sixik.unigui.xaml;

import java.util.Objects;

/**
 * Имя типа или члена с разрешённым namespace.
 *
 * <p>Идентичность имени определяют только {@code namespace} и {@code local}. Префикс хранится лишь затем,
 * чтобы документ можно было записать обратно в том же виде ({@code x:Name}, {@code mc:ItemIcon}), поэтому
 * {@link #equals(Object)} и {@link #hashCode()} его игнорируют.</p>
 *
 * @param namespace URI пространства имён; пустая строка означает «без namespace»
 * @param local     локальная часть имени
 * @param prefix    префикс из исходного текста; пустая строка, если он не использовался
 */
public record UiName(String namespace, String local, String prefix) {
    public UiName {
        namespace = namespace == null ? "" : namespace;
        prefix = prefix == null ? "" : prefix;
        if (local == null || local.isEmpty()) {
            throw new IllegalArgumentException("UiName local part must not be empty");
        }
    }

    /**
     * Создаёт имя без namespace и префикса.
     *
     * @param local локальная часть
     * @return имя
     */
    public static UiName of(String local) {
        return new UiName("", local, "");
    }

    /**
     * Создаёт имя в указанном namespace без префикса.
     *
     * @param namespace URI пространства имён
     * @param local     локальная часть
     * @return имя
     */
    public static UiName of(String namespace, String local) {
        return new UiName(namespace, local, "");
    }

    /**
     * Возвращает имя в том виде, в котором оно пишется в документе.
     *
     * @return {@code prefix:local} либо просто {@code local}
     */
    public String qualified() {
        return prefix.isEmpty() ? local : prefix + ":" + local;
    }

    /**
     * Сравнивает имена по смыслу, игнорируя префикс.
     *
     * @param other другое имя
     * @return {@code true}, если совпадают namespace и локальная часть
     */
    public boolean sameAs(UiName other) {
        return other != null && namespace.equals(other.namespace) && local.equals(other.local);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof UiName other && sameAs(other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, local);
    }

    @Override
    public String toString() {
        return namespace.isEmpty() ? qualified() : "{" + namespace + "}" + local;
    }
}

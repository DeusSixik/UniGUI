package dev.sixik.unigui.widgets.navigation;

import dev.sixik.unigui.api.text.RichText;

import java.util.Objects;

public final class BreadcrumbItem {
    private String text;
    private RichText richText;
    private String value;
    private boolean enabled = true;

    public BreadcrumbItem() {
        this("");
    }

    public BreadcrumbItem(String text) {
        this.text = normalize(text);
        this.richText = RichText.plain(this.text);
        this.value = this.text;
    }

    public BreadcrumbItem(RichText text) {
        this.richText = text == null ? RichText.plain("") : text;
        this.text = this.richText.plainText();
        this.value = this.text;
    }

    public String text() {
        return text;
    }

    public BreadcrumbItem text(String text) {
        this.text = normalize(text);
        this.richText = RichText.plain(this.text);
        if (value.isEmpty()) {
            value = this.text;
        }
        return this;
    }

    public RichText richText() {
        return richText;
    }

    public BreadcrumbItem richText(RichText text) {
        this.richText = text == null ? RichText.plain("") : text;
        this.text = this.richText.plainText();
        if (value.isEmpty()) {
            value = this.text;
        }
        return this;
    }

    public String value() {
        return value;
    }

    public BreadcrumbItem value(String value) {
        this.value = normalize(value);
        return this;
    }

    public boolean enabled() {
        return enabled;
    }

    public BreadcrumbItem enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof BreadcrumbItem that)) return false;
        return enabled == that.enabled
                && Objects.equals(text, that.text)
                && Objects.equals(richText, that.richText)
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, richText, value, enabled);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}

package dev.sixik.unigui.widgets.navigation;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.text.RichText;

import java.util.Objects;

@AiDoc(summary = "Navigation item descriptor representing a step or crumb in a breadcrumb trail.")
@WikiPage(
        title = "BreadcrumbItem",
        slug = "breadcrumb-item",
        category = "<key:navigation.category>",
        summary = "<key:breadcrumb.item.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:breadcrumb.item.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:breadcrumb.item.example.basic.title>",
        code = "BreadcrumbItem item = new BreadcrumbItem(\"Dashboard\");\nitem.enabled(true);"
)
@WikiLink(
        label = "Breadcrumb",
        target = "page:breadcrumb",
        description = "<key:breadcrumb.summary>"
)
public final class BreadcrumbItem {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.BREADCRUMB_ITEM;

    private String text;
    private RichText richText;
    private String value;
    private boolean enabled = true;

        @AiDoc(summary = "Constructs an empty BreadcrumbItem.")
    @WikiMethod(summary = "<key:breadcrumb.item.method.ctorDefault>")
    public BreadcrumbItem() {
        this("");
    }

        @AiDoc(summary = "Constructs a BreadcrumbItem with a plain text label.")
    @WikiMethod(summary = "<key:breadcrumb.item.method.ctorText>")
    public BreadcrumbItem(
            @WikiParameter(description = "<key:breadcrumb.item.param.text>")
            String text
    ) {
        this.text = normalize(text);
        this.richText = RichText.resolve(this.text);
        this.value = this.text;
    }

        @AiDoc(summary = "Constructs a BreadcrumbItem with a RichText formatted label.")
    @WikiMethod(summary = "<key:breadcrumb.item.method.ctorRich>")
    public BreadcrumbItem(
            @WikiParameter(description = "<key:breadcrumb.item.param.text>")
            RichText text
    ) {
        this.richText = text == null ? RichText.plain("") : text;
        this.text = this.richText.plainText();
        this.value = this.text;
    }

        @AiDoc(summary = "Returns the plain text label.")
    @WikiMethod(summary = "<key:breadcrumb.item.method.textGet>")
    @WikiReturn("<key:breadcrumb.item.return.text>")
    public String text() {
        return text;
    }

        @AiDoc(summary = "Sets the plain text label.")
    @WikiMethod(summary = "<key:breadcrumb.item.method.textSet>")
    @WikiReturn("<key:breadcrumb.item.return.this>")
    public BreadcrumbItem text(
            @WikiParameter(description = "<key:breadcrumb.item.param.text>")
            String text
    ) {
        this.text = normalize(text);
        this.richText = RichText.resolve(this.text);
        if (value.isEmpty()) {
            value = this.text;
        }
        return this;
    }

        @AiDoc(summary = "Returns the RichText formatted label.")
    @WikiMethod(summary = "<key:breadcrumb.item.method.richTextGet>")
    @WikiReturn("<key:breadcrumb.item.return.richText>")
    public RichText richText() {
        return richText;
    }

        @AiDoc(summary = "Sets the RichText formatted label.")
    @WikiMethod(summary = "<key:breadcrumb.item.method.richTextSet>")
    @WikiReturn("<key:breadcrumb.item.return.this>")
    public BreadcrumbItem richText(
            @WikiParameter(description = "<key:breadcrumb.item.param.text>")
            RichText text
    ) {
        this.richText = text == null ? RichText.plain("") : text;
        this.text = this.richText.plainText();
        if (value.isEmpty()) {
            value = this.text;
        }
        return this;
    }

        @AiDoc(summary = "Returns the associated data payload or route string.")
    @WikiMethod(summary = "<key:breadcrumb.item.method.valueGet>")
    @WikiReturn("<key:breadcrumb.item.return.value>")
    public String value() {
        return value;
    }

        @AiDoc(summary = "Sets the associated data payload or route string.")
    @WikiMethod(summary = "<key:breadcrumb.item.method.valueSet>")
    @WikiReturn("<key:breadcrumb.item.return.this>")
    public BreadcrumbItem value(
            @WikiParameter(description = "<key:breadcrumb.item.param.value>")
            String value
    ) {
        this.value = normalize(value);
        return this;
    }

        @AiDoc(summary = "Returns true if this item is interactive.")
    @WikiMethod(summary = "<key:breadcrumb.item.method.enabledGet>")
    @WikiReturn("<key:breadcrumb.item.return.enabled>")
    public boolean enabled() {
        return enabled;
    }

        @AiDoc(summary = "Sets whether this item is interactive.")
    @WikiMethod(summary = "<key:breadcrumb.item.method.enabledSet>")
    @WikiReturn("<key:breadcrumb.item.return.this>")
    public BreadcrumbItem enabled(
            @WikiParameter(description = "<key:breadcrumb.item.param.enabled>")
            boolean enabled
    ) {
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

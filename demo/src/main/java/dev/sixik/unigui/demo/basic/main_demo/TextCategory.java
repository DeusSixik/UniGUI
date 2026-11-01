package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.PasswordField;
import dev.sixik.unigui.widgets.interaction.SearchField;
import dev.sixik.unigui.widgets.interaction.TextField;

import java.util.List;

final class TextCategory extends DemoCategory {
    private static final List<Control> CONTROLS = List.of(
            new Control("typography", "Text rendering", "Explore alignment, wrapping, overflow, and rich text runs."),
            new Control("text", "TextBox", "Edit a single-line text value."),
            new Control("password", "PasswordBox", "Enter a private value with masked characters."),
            new Control("search", "SearchField", "Filter a collection as the query changes."));

    @Override
    String title() {
        return "Text";
    }

    @Override
    boolean expandedByDefault() {
        return true;
    }

    @Override
    List<Control> controls() {
        return CONTROLS;
    }

    @Override
    void addDemo(String controlId, VBox root) {
        switch (controlId) {
            case "typography" -> addTypography(root);
            case "text" -> addText(root, false);
            case "password" -> addText(root, true);
            case "search" -> addSearch(root);
            default -> root.addChild(samplePanel("Preview", "Interactive sample", new Label(controlId), ""));
        }
    }

    private void addTypography(VBox root) {
        addAlignmentExamples(root);
        addOverflowExamples(root);
        addRichTextExamples(root);
    }

    private void addAlignmentExamples(VBox root) {
        HBox samples = new HBox();
        samples.spacing(10.0f);
        samples.layout(style -> style.widthPercent(100.0f).alignItems(Align.STRETCH));
        samples.addChild(alignmentSample("START", Alignment.START));
        samples.addChild(alignmentSample("CENTER", Alignment.CENTER));
        samples.addChild(alignmentSample("END", Alignment.END));
        root.addChild(samplePanel("Alignment", "Align text inside a fixed-width area without changing the text itself.", samples,
                "Label label = new Label(\"Aligned text\");\nlabel.textAlignment(Alignment.CENTER);\nlabel.layout(style -> style.widthPercent(100.0f));"));
    }

    private Box alignmentSample(String name, Alignment alignment) {
        Box box = textSampleBox();
        box.layout(style -> style.flexGrow(1.0f).height(84.0f).padding(10.0f, 8.0f));
        Label caption = label(name, UniGuiDemo.GalleryTemplate.color(94, 115, 132));
        caption.font(null, 9.0f);
        Label text = label("A line of text", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        text.textAlignment(alignment);
        text.layout(style -> style.widthPercent(100.0f).height(30.0f));
        VBox content = new VBox();
        content.spacing(8.0f);
        content.layout(style -> style.widthPercent(100.0f));
        content.addChild(caption);
        content.addChild(text);
        box.addChild(content);
        return box;
    }

    private void addOverflowExamples(VBox root) {
        VBox samples = new VBox();
        samples.spacing(12.0f);
        samples.layout(style -> style.widthPercent(100.0f));

        Label wrapped = label("A longer paragraph wraps inside its available width, keeping the full sentence readable without forcing the surrounding layout to grow sideways.",
                UniGuiDemo.GalleryTemplate.TEXT_DARK);
        wrapped.wrap(true);
        wrapped.layout(style -> style.widthPercent(100.0f).height(54.0f));
        samples.addChild(wrapped);

        HBox singleLines = new HBox();
        singleLines.spacing(12.0f);
        singleLines.layout(style -> style.widthPercent(100.0f).alignItems(Align.CENTER));
        Label ellipsis = label("Build output: compiling shared client and rendering modules", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        ellipsis.overflowMode(TextOverflowMode.ELLIPSIS);
        ellipsis.layout(style -> style.flexGrow(1.0f).height(34.0f));
        Label marquee = label("Hover to inspect this long status message without widening the panel", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        marquee.marqueeOnHover();
        marquee.layout(style -> style.flexGrow(1.0f).height(34.0f));
        singleLines.addChild(ellipsis);
        singleLines.addChild(marquee);
        samples.addChild(singleLines);

        root.addChild(samplePanel("Wrapping and overflow", "Wrap paragraphs, trim a single line with an ellipsis, or reveal long text on hover.", samples,
                "Label paragraph = new Label(description).wrap(true);\nLabel path = new Label(longPath).overflowMode(TextOverflowMode.ELLIPSIS);\nLabel hint = new Label(longHint).marqueeOnHover();"));
    }

    private void addRichTextExamples(VBox root) {
        RichText runs = RichText.builder()
                .size(12.0f).color(UniGuiDemo.GalleryTemplate.TEXT_DARK).append("Deploy ")
                .size(15.0f).color(UniGuiDemo.GalleryTemplate.color(20, 119, 178)).append("succeeded ")
                .size(10.0f).color(UniGuiDemo.GalleryTemplate.color(94, 115, 132)).uppercase().append("just now")
                .build();
        Label formatted = new Label(runs);
        formatted.layout(style -> style.widthPercent(100.0f).height(34.0f));

        Label gradient = label("A little color helps signal emphasis", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        gradient.textGradient(UniGuiDemo.GalleryTemplate.color(20, 119, 178),
                UniGuiDemo.GalleryTemplate.color(30, 166, 222), 0.0f);
        gradient.layout(style -> style.widthPercent(100.0f).height(34.0f));

        VBox samples = new VBox();
        samples.spacing(8.0f);
        samples.layout(style -> style.widthPercent(100.0f));
        samples.addChild(formatted);
        samples.addChild(gradient);
        RichText statusText = RichText.builder()
                .size(12.0f).color(UniGuiDemo.GalleryTemplate.TEXT_DARK).append("Deployment ")
                .inline("demo:status-dot", "[dot]", 12.0f, 12.0f, (draw, context) -> {
                    draw.roundedRect(context.x(), context.y(), context.width(), context.height(), 3.0f,
                            Paint.fill(MutableColor.rgba(0.10f, 0.44f, 0.30f, 1.0f)));
                    draw.circle(context.x() + 3.0f, context.y() + 3.0f, 6.0f, 6.0f,
                            Paint.fill(MutableColor.rgba(0.46f, 1.0f, 0.66f, 1.0f)));
                })
                .append(" Ready · 2 seconds ago")
                .build();
        Label customInline = new Label(statusText);
        customInline.layout(style -> style.widthPercent(100.0f).height(34.0f));
        samples.addChild(customInline);
        root.addChild(samplePanel("Rich text and color", "Build a line from styled runs or apply a gradient brush to a whole label.", samples,
                "RichText text = RichText.builder()\n        .size(12).color(dark).append(\"Deploy \")\n        .size(15).color(blue).append(\"succeeded\")\n        .build();\nnew Label(text);\n\nRichText status = RichText.builder()\n        .append(\"Deployment \")\n        .inline(\"demo:status-dot\", \"[dot]\", 12, 12, (draw, context) -> {\n            draw.roundedRect(context.x(), context.y(), context.width(), context.height(), 3,\n                    Paint.fill(background));\n            draw.circle(context.x() + 3, context.y() + 3, 6, 6, Paint.fill(foreground));\n        })\n        .append(\" Ready\")\n        .build();"));
    }

    private Box textSampleBox() {
        Box box = new Box();
        box.themeEnabled(false);
        box.backgroundVisible(true);
        box.background().set(UniGuiDemo.GalleryTemplate.color(255, 255, 255));
        box.borderVisible(true);
        box.borderColor().set(UniGuiDemo.GalleryTemplate.CARD_BORDER);
        box.borderWidth(1.0f);
        box.radius(4.0f);
        return box;
    }

    private void addText(VBox root, boolean password) {
        VBox fields = new VBox();
        fields.spacing(12.0f);
        TextField text = new TextField();
        text.placeholder("Type a value");
        text.layout(style -> style.widthPercent(100.0f).height(36.0f));
        fields.addChild(text);
        if (password) {
            PasswordField secret = new PasswordField();
            secret.placeholder("Password");
            secret.layout(style -> style.widthPercent(100.0f).height(36.0f));
            fields.addChild(secret);
        }
        root.addChild(samplePanel(password ? "Masked input" : "Single-line input",
                "Inputs preserve the user's text and expose it through change events.", fields,
                password ? "PasswordField field = new PasswordField().placeholder(\"Password\");"
                        : "TextField field = new TextField().placeholder(\"Type a value\");"));
    }

    private void addSearch(VBox root) {
        SearchField search = new SearchField();
        search.placeholder("Search controls...");
        search.layout(style -> style.widthPercent(100.0f).height(38.0f));
        Label result = label("Results will appear here", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        search.onSearchChanged(event -> result.text(event.newQuery().isBlank()
                ? "Results will appear here" : "Filtering for: " + event.newQuery()));
        VBox sample = new VBox();
        sample.spacing(14.0f);
        sample.layout(style -> style.widthPercent(100.0f));
        sample.addChild(search);
        sample.addChild(result);
        root.addChild(samplePanel("Search interaction", "A search field can debounce query changes before filtering a large list.", sample,
                "search.onSearchChanged(event -> filter(event.query()));"));
    }
}

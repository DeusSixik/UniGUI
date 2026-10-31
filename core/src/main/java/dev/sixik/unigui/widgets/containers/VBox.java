package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.api.xml.XmlWidgetName;

/**
 * Вертикальный shorthand для {@link LinearBox}.
 *
 * <p>Дети раскладываются сверху вниз через flex-адаптер без переноса строк.
 * Расстояние между соседними детьми задаётся через {@link #spacing(float)} и
 * попадает в {@code rowGap} текущего {@code LayoutStyle}.</p>
 *
 * <pre>{@code
 * VBox panel = new VBox()
 *         .spacing(4.0f);
 * panel.addChild(title);
 * panel.addChild(body);
 * }</pre>
 *
 * @see LinearBox
 * @see HBox
 */
@AiDoc(
        summary = "Vertical linear container stacking children top-to-bottom with uniform spacing.",
        purpose = "Convenient shorthand for LinearBox(Orientation.VERTICAL). Widely used for forms, sidebars, and vertical stacks.",
        useWhen = {
                "Stack items vertically: headers, list items, menus, and form rows.",
                "Use spacing(pixels) to separate items without configuring margins on each child."
        },
        criticalRules = {
                "FlexDirection is fixed to COLUMN.",
                "Use child.layout(s -> s.expand()) on a child to consume remaining vertical free space."
        },
        related = {"HBox", "LinearBox", "FlexBox", "ScrollView"},
        examples = @AiDoc.Example(
                title = "Vertical stack",
                code = "VBox panel = new VBox().spacing(8.0f);\npanel.addChild(title);\npanel.addChild(content);"))
@WikiPage(
        title = "<key:vbox.title>",
        slug = "vbox",
        category = "<key:vbox.category>",
        summary = "<key:vbox.summary>",
        order = 50)
@WikiSection(title = "<key:vbox.section.overview.title>",
        markdown = "<key:vbox.section.overview.body>", order = 10)
@WikiExample(title = "<key:vbox.example.form.title>",
        code = "<key:vbox.example.form.code>", order = 10)
@WikiLink(label = "HBox", target = "page:hbox")
@WikiLink(label = "LinearBox", target = "page:linear-box")
@WikiLink(label = "ScrollView", target = "page:scroll-view")
@XmlWidgetName("VBox")
public final class VBox extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.VBOX;

    /**
     * Создаёт вертикальный контейнер с направлением {@link Orientation#VERTICAL}.
     */
    @WikiMethod(order = 10, title = "<key:vbox.method.ctor.title>", summary = "<key:vbox.method.ctor.summary>")
    @AiDoc(section = "Lifecycle", sectionOrder = 10, order = 10, summary = "Creates a vertical linear container.")
    public VBox() {
        super(Orientation.VERTICAL);
    }
}

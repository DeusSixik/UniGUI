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
 * Горизонтальный shorthand для {@link LinearBox}.
 *
 * <p>Дети раскладываются слева направо через flex-адаптер без переноса строк.
 * Расстояние между соседними детьми задаётся через {@link #spacing(float)} и
 * попадает в {@code columnGap} текущего {@code LayoutStyle}.</p>
 *
 * <pre>{@code
 * HBox toolbar = new HBox()
 *         .spacing(6.0f);
 * toolbar.addChild(saveButton);
 * toolbar.addChild(cancelButton);
 * }</pre>
 *
 * @see LinearBox
 * @see VBox
 */
@AiDoc(
        summary = "Horizontal linear container placing children left-to-right with uniform spacing.",
        purpose = "Convenient shorthand for LinearBox(Orientation.HORIZONTAL). Used for toolbars, headers, and action rows.",
        useWhen = {
                "Place elements side by side horizontally in a single row.",
                "Build toolbars, action buttons, search bars, and breadcrumbs."
        },
        criticalRules = {
                "FlexDirection is fixed to ROW.",
                "Use child.layout(s -> s.expand()) to consume remaining horizontal free space."
        },
        related = {"VBox", "LinearBox", "FlexBox", "PanelRowWidget"},
        examples = @AiDoc.Example(
                title = "Toolbar row",
                code = "HBox toolbar = new HBox().spacing(4.0f);\ntoolbar.addChild(saveBtn);\ntoolbar.addChild(cancelBtn);"))
@WikiPage(
        title = "<key:hbox.title>",
        slug = "hbox",
        category = "<key:hbox.category>",
        summary = "<key:hbox.summary>",
        order = 60)
@WikiSection(title = "<key:hbox.section.overview.title>",
        markdown = "<key:hbox.section.overview.body>", order = 10)
@WikiExample(title = "<key:hbox.example.toolbar.title>",
        code = "<key:hbox.example.toolbar.code>", order = 10)
@WikiLink(label = "VBox", target = "page:vbox")
@WikiLink(label = "LinearBox", target = "page:linear-box")
@WikiLink(label = "PanelRowWidget", target = "page:panel-row-widget")
@XmlWidgetName("HBox")
public final class HBox extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.HBOX;

    /**
     * Создаёт горизонтальный контейнер с направлением {@link Orientation#HORIZONTAL}.
     */
    @WikiMethod(order = 10, title = "<key:hbox.method.ctor.title>", summary = "<key:hbox.method.ctor.summary>")
    @AiDoc(section = "Lifecycle", sectionOrder = 10, order = 10, summary = "Creates a horizontal linear container.")
    public HBox() {
        super(Orientation.HORIZONTAL);
    }
}

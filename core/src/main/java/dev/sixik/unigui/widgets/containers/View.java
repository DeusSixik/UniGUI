package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.WidgetStyles;
import dev.sixik.unigui.widgets.display.Label;

/**
 * Готовая card-like оболочка для небольших панелей интерфейса.
 *
 * <p>{@code View} наследует визуальные возможности {@link Box}, но сразу
 * создаёт внутренний {@link VBox} с заголовком и content-зоной. Это удобный
 * строительный блок для demo/debug экранов и простых панелей, где нужен
 * единый фон, рамка, padding и title.</p>
 *
 * <p>Детей следует добавлять через {@link #addContent(Widget)} или напрямую в
 * {@link #content()}, чтобы они попадали во внутренний вертикальный layout под
 * заголовком.</p>
 */
@AiDoc(
        summary = "Card-like visual container embedding a header Label and a vertical VBox content area.",
        purpose = "Provides a ready-to-use titled card or panel with default background, border, padding, and vertical layout.",
        useWhen = {
                "Building inspector panels, debug tool windows, or self-contained card UI components.",
                "Need a quick card panel with a title header and vertical content stack without manual Box/VBox wiring."
        },
        criticalRules = {
                "Widgets added via addContent(Widget) or content().addChild(Widget) are placed in the vertical VBox below the title.",
                "Inherits visual styling from Box, including background, border, corner radius, and shadow.",
                "Uses WidgetStyles.View.DEFAULT for default surface styling."
        },
        related = {"Box", "VBox", "PanelWidget"},
        examples = @AiDoc.Example(
                title = "Card View with content",
                code = "View card = new View(\"Properties\");\ncard.addContent(new Label(\"Status: Active\"));\ncard.addContent(new Button(\"Save\"));"))
@WikiPage(
        title = "<key:view.title>",
        slug = "view",
        category = "<key:view.category>",
        summary = "<key:view.summary>",
        order = 160)
@WikiSection(title = "<key:view.section.overview.title>",
        markdown = "<key:view.section.overview.body>", order = 10)
@WikiSection(title = "<key:view.section.card.title>",
        markdown = "<key:view.section.card.body>", order = 20)
@WikiExample(title = "<key:view.example.basic.title>",
        code = "<key:view.example.basic.code>", order = 10)
@WikiLink(label = "Box", target = "page:box")
@WikiLink(label = "VBox", target = "page:vbox")
@WikiLink(label = "PanelWidget", target = "page:panel-widget")
@XmlWidgetName("View")
public class View extends Box {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.VIEW;

    private final VBox content = new VBox();
    private final Label title = new Label("");

    /**
     * Создаёт view с пустым заголовком и стандартной визуальной оболочкой.
     */
    public View() {
        applyComponentStyleDefaults(WidgetStyles.View.DEFAULT);
        content.spacing(6.0f);
        content.layout(style -> style.margin(8.0f));
        title.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        content.addChild(title);
        super.addChild(content);
    }

    /**
     * Возвращает стандартный стиль поверхности карточки, уступающий стилям темы и экземпляра.
     *
     * @return стандартные значения оформления компонента {@code View}
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.View.DEFAULT;
    }

    /**
     * Создаёт view с заданным заголовком.
     *
     * @param title текст заголовка; {@code null} превращается в пустую строку
     */
    public View(String title) {
        this();
        title(title);
    }

    /**
     * Задаёт текст заголовка.
     *
     * @param title новый текст заголовка; {@code null} превращается в пустую строку
     * @return этот view для fluent-настройки
     */
    @XmlAttribute(value = "title", category = "Content", defaultValue = "", description = "View title text.")
    @WikiMethod(order = 10, title = "<key:view.method.title.title>", summary = "<key:view.method.title>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    @AiDoc(section = "Content", sectionOrder = 10, order = 10, summary = "Sets text of header label.")
    public View title(@WikiParameter(description = "<key:view.param.title>") String title) {
        this.title.text(title == null ? "" : title);
        return this;
    }

    /**
     * Возвращает внутренний вертикальный контейнер содержимого.
     *
     * @return live {@link VBox}, содержащий title label и пользовательский контент
     */
    @WikiMethod(order = 20, title = "<key:view.method.content.title>", summary = "<key:view.method.content>")
    @WikiReturn("<key:view.method.content.return>")
    @AiDoc(section = "Content", sectionOrder = 10, order = 20, summary = "Returns internal VBox container hosting content widgets.")
    public VBox content() {
        return content;
    }

    /**
     * Добавляет виджет во внутренний content-контейнер под заголовком.
     *
     * @param widget виджет для добавления; {@code null} игнорируется через {@link VBox#addChild(Widget)}
     * @return этот view для fluent-настройки
     */
    @WikiMethod(order = 30, title = "<key:view.method.add_content.title>", summary = "<key:view.method.add_content>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    @AiDoc(section = "Content", sectionOrder = 10, order = 30, summary = "Appends widget to internal VBox below title.")
    public View addContent(@WikiParameter(description = "<key:view.param.widget>") Widget widget) {
        content.addChild(widget);
        return this;
    }
}

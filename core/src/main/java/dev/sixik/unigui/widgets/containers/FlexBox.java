package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.layout.v3.LayoutV3FlexAdapter;

/**
 * Flex-контейнер, настраиваемый через {@link dev.sixik.unigui.api.layout.LayoutStyle},
 * аналог CSS {@code display: flex}.
 *
 * <p>Направление по умолчанию — {@link FlexDirection#ROW}. В отличие от
 * {@link LinearBox} и {@link WrapPanel}, контейнер не хранит отдельные состояния
 * ориентации и spacing. Настраивайте {@code flexDirection}, {@code flexWrap},
 * {@code gap} и свойства выравнивания прямо через {@code layoutStyle()}.</p>
 */
@AiDoc(
        summary = "Flex container configured directly via LayoutStyle, equivalent to CSS display: flex.",
        purpose = "Provides full unconstrained flexbox layout without helper abstractions. Direction defaults to ROW.",
        useWhen = {
                "Require advanced flexbox properties (justifyContent, alignItems, alignContent, wrap) together.",
                "Build responsive adaptive layouts mimicking CSS flexbox specifications."
        },
        criticalRules = {
                "Direction defaults to FlexDirection.ROW.",
                "Configure spacing and flow directly through layoutStyle()."
        },
        related = {"LinearBox", "WrapPanel", "LayoutStyle"},
        examples = @AiDoc.Example(
                title = "Flex container setup",
                code = "FlexBox flex = new FlexBox();\nflex.layout(s -> s.justifyContent(Justify.SPACE_BETWEEN));"))
@WikiPage(
        title = "<key:flex.box.title>",
        slug = "flex-box",
        category = "<key:flex.box.category>",
        summary = "<key:flex.box.summary>",
        order = 70)
@WikiSection(title = "<key:flex.box.section.overview.title>",
        markdown = "<key:flex.box.section.overview.body>", order = 10)
@WikiExample(title = "<key:flex.box.example.advanced.title>",
        code = "<key:flex.box.example.advanced.code>", order = 10)
@WikiLink(label = "LayoutStyle", target = "page:layout-style")
@WikiLink(label = "LinearBox", target = "page:linear-box")
@XmlWidgetName("FlexBox")
public final class FlexBox extends PanelWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.FLEX_BOX;

    @WikiMethod(order = 10, title = "<key:flex.box.method.ctor.title>", summary = "<key:flex.box.method.ctor.summary>")
    @AiDoc(section = "Lifecycle", sectionOrder = 10, order = 10, summary = "Creates an unconstrained FlexBox with ROW direction.")
    public FlexBox() {
        layoutStyle().flexDirection(FlexDirection.ROW);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        var style = layoutStyle();
        LayoutSize measured = LayoutV3FlexAdapter.measure(children(), context,
                style.flexDirection(), style.flexWrap(), style.rowGap(), style.columnGap(), style);
        setDesiredSize(resolveDesiredSize(context, measured.width(), measured.height()));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        var style = layoutStyle();
        LayoutV3FlexAdapter.arrange(children(), bounds,
                style.flexDirection(), style.flexWrap(), style.rowGap(), style.columnGap(), style);
    }
}

package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.layout.v3.LayoutV3FlexAdapter;
import dev.sixik.unigui.widgets.core.Orientation;

/**
 * Flow-контейнер, который переносит детей на следующую строку или колонку.
 *
 * <p>{@code WrapPanel} использует тот же flex-адаптер, что и {@link LinearBox},
 * но включает {@code FlexWrap.WRAP}. При горизонтальной ориентации дети идут
 * слева направо и переносятся на новую строку; при вертикальной — сверху вниз
 * и переносятся в новую колонку.</p>
 *
 * <p>{@link #spacing(float)} управляет расстоянием между элементами внутри
 * одной линии, а {@link #lineSpacing(float)} — расстоянием между линиями.</p>
 *
 * <pre>{@code
 * WrapPanel tags = new WrapPanel()
 *         .spacing(4.0f)
 *         .lineSpacing(4.0f);
 * tags.addChild(new Label("alpha"));
 * tags.addChild(new Label("beta"));
 * }</pre>
 *
 * @see LinearBox
 */
@AiDoc(
        summary = "Flow container wrapping children to a new row or column when space is exceeded.",
        purpose = "Implements multi-line flex wrapping. Distinguishes item spacing from line spacing.",
        useWhen = {
                "Layout tag clouds, chips, media galleries, or dynamic tool palettes that must wrap."
        },
        criticalRules = {
                "FlexWrap is set to WRAP.",
                "spacing() governs item-to-item gap; lineSpacing() governs line-to-line gap."
        },
        related = {"LinearBox", "FlexBox", "PanelWidget"},
        examples = @AiDoc.Example(
                title = "Tag cloud",
                code = "WrapPanel tags = new WrapPanel().spacing(4.0f).lineSpacing(4.0f);"))
@WikiPage(
        title = "<key:wrap.panel.title>",
        slug = "wrap-panel",
        category = "<key:wrap.panel.category>",
        summary = "<key:wrap.panel.summary>",
        order = 100)
@WikiSection(title = "<key:wrap.panel.section.overview.title>",
        markdown = "<key:wrap.panel.section.overview.body>", order = 10)
@WikiExample(title = "<key:wrap.panel.example.chips.title>",
        code = "<key:wrap.panel.example.chips.code>", order = 10)
@WikiLink(label = "LinearBox", target = "page:linear-box")
@WikiLink(label = "FlexBox", target = "page:flex-box")
@XmlWidgetName("WrapPanel")
public final class WrapPanel extends PanelWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.WRAP_PANEL;

    private Orientation orientation = Orientation.HORIZONTAL;
    private float spacing;
    private float lineSpacing;

    /**
     * Создаёт горизонтальный wrap-контейнер.
     */
    @WikiMethod(order = 10, title = "<key:wrap.panel.method.ctor.title>", summary = "<key:wrap.panel.method.ctor.summary>")
    @AiDoc(section = "Lifecycle", sectionOrder = 10, order = 10, summary = "Creates a WrapPanel with HORIZONTAL orientation.")
    public WrapPanel() {
        syncLayoutStyle();
    }

    /**
     * Возвращает главную ось раскладки.
     *
     * @return текущая ориентация wrap-layout'а
     */
    public Orientation orientation() {
        return orientation;
    }

    /**
     * Задаёт главную ось раскладки.
     *
     * @param orientation новая ориентация; {@code null} трактуется как {@link Orientation#HORIZONTAL}
     * @return этот контейнер для fluent-настройки
     */
    @XmlAttribute(value = "orientation", category = "Layout", defaultValue = "horizontal", description = "Main axis used before wrapping children.")
    @WikiMethod(order = 20, title = "<key:wrap.panel.method.orientation.title>", summary = "<key:wrap.panel.method.orientation.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    @AiDoc(section = "Layout", sectionOrder = 20, order = 20, summary = "Sets flow orientation (HORIZONTAL rows or VERTICAL columns).")
    public WrapPanel orientation(@WikiParameter(description = "<key:linear.box.method.orientation.param>") Orientation orientation) {
        Orientation normalized = orientation == null ? Orientation.HORIZONTAL : orientation;
        if (this.orientation == normalized) return this;
        this.orientation = normalized;
        syncLayoutStyle();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает расстояние между соседними элементами внутри одной линии.
     *
     * @return spacing в пикселях UI-пространства
     */
    public float spacing() {
        return spacing;
    }

    /**
     * Задаёт расстояние между соседними элементами внутри одной линии.
     *
     * @param spacing spacing в пикселях UI-пространства; невалидные значения заменяются на {@code 0}
     * @return этот контейнер для fluent-настройки
     */
    @XmlAttribute(value = "spacing", category = "Layout", defaultValue = "0", description = "Gap between children within one wrap line.")
    @WikiMethod(order = 30, title = "<key:wrap.panel.method.spacing.title>", summary = "<key:wrap.panel.method.spacing.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    @AiDoc(section = "Layout", sectionOrder = 20, order = 30, summary = "Sets spacing between items along flow line.")
    public WrapPanel spacing(@WikiParameter(description = "<key:wrap.panel.method.spacing.param>") float spacing) {
        float normalized = Float.isFinite(spacing) ? Math.max(0.0f, spacing) : 0.0f;
        if (this.spacing == normalized) return this;
        this.spacing = normalized;
        syncLayoutStyle();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает расстояние между wrap-линиями.
     *
     * @return line spacing в пикселях UI-пространства
     */
    public float lineSpacing() {
        return lineSpacing;
    }

    /**
     * Задаёт расстояние между wrap-линиями.
     *
     * @param lineSpacing line spacing в пикселях UI-пространства; невалидные значения заменяются на {@code 0}
     * @return этот контейнер для fluent-настройки
     */
    @XmlAttribute(value = "lineSpacing", category = "Layout", defaultValue = "0", description = "Gap between wrapped lines.")
    @WikiMethod(order = 40, title = "<key:wrap.panel.method.line_spacing.title>", summary = "<key:wrap.panel.method.line_spacing.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    @AiDoc(section = "Layout", sectionOrder = 20, order = 40, summary = "Sets spacing between wrapped lines/columns.")
    public WrapPanel lineSpacing(@WikiParameter(description = "<key:wrap.panel.method.line_spacing.param>") float lineSpacing) {
        float normalized = Float.isFinite(lineSpacing) ? Math.max(0.0f, lineSpacing) : 0.0f;
        if (this.lineSpacing == normalized) return this;
        this.lineSpacing = normalized;
        syncLayoutStyle();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        LayoutStyle style = layoutStyle();
        LayoutSize measured = LayoutV3FlexAdapter.measure(
                children(), context, style.flexDirection(), style.flexWrap(),
                style.rowGap(), style.columnGap(), style);
        setDesiredSize(resolveDesiredSize(context, measured.width(), measured.height()));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        LayoutStyle style = layoutStyle();
        LayoutV3FlexAdapter.arrange(
                children(), bounds, style.flexDirection(), style.flexWrap(),
                style.rowGap(), style.columnGap(), style);
    }

    private void syncLayoutStyle() {
        layoutStyle().update(style -> {
            style.flexDirection(orientation == Orientation.HORIZONTAL
                    ? FlexDirection.ROW
                    : FlexDirection.COLUMN);
            style.flexWrap(FlexWrap.WRAP);
            style.rowGap(orientation == Orientation.HORIZONTAL ? lineSpacing : spacing);
            style.columnGap(orientation == Orientation.HORIZONTAL ? spacing : lineSpacing);
        });
    }
}

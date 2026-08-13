package dev.sixik.unigui.widgets.map;

import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextTransform;
import dev.sixik.unigui.widgets.AnchorWidget;
import dev.sixik.unigui.widgets.Button;

/**
 * Default retained marker widget for {@link MapCanvas}.
 */
public class MapMarker extends Button {
    private MarkerStyle style = MarkerStyle.DEFAULT;
    private AnchorWidget anchor;
    private boolean selected;

    public MapMarker() {
        this("");
    }

    public MapMarker(String text) {
        super();
        text(text);
        textPaddingX(6.0f);
        interactionTransitions(true);
        applyStyle();
    }

    @Override
    public MapMarker text(String text) {
        richText(markerText(text));
        return this;
    }

    public MarkerStyle markerStyle() {
        return style;
    }

    public MapMarker markerStyle(MarkerStyle style) {
        MarkerStyle next = style == null ? MarkerStyle.DEFAULT : style;
        if (this.style == next) return this;
        this.style = next;
        applyStyle();
        return this;
    }

    public boolean selected() {
        return selected;
    }

    public MapMarker selected(boolean selected) {
        if (this.selected == selected) return this;
        this.selected = selected;
        applyStyle();
        return this;
    }

    public AnchorWidget anchor() {
        return anchor;
    }

    void anchor(AnchorWidget anchor) {
        this.anchor = anchor;
    }

    private void applyStyle() {
        backgroundVisible(true);
        borderVisible(true);
        radius(4.0f);
        borderWidth(selected ? 2.0f : 1.0f);
        background().set(style.backgroundColor());
        borderColor().set(style.ringColor());
        textColor().set(style.textColor());
    }

    private static RichText markerText(String text) {
        return RichText.builder()
                .transform(TextTransform.UPPERCASE)
                .tracking(0.12f)
                .append(text)
                .build();
    }
}
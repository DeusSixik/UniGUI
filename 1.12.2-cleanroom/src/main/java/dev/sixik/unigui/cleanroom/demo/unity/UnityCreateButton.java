package dev.sixik.unigui.cleanroom.demo.unity;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.widgets.interaction.Button;

/**
 * Кнопка создания (+) с маленькой стрелочкой dropdown в стиле редактора Unity.
 */
public class UnityCreateButton extends Button {
    public UnityCreateButton() {
        super("");
        UnityStyles.styleButton(this);
        focusRingVisible(false);
        focusable(false);
        layout(style -> style.size(28.0f, 20.0f).padding(0.0f, 0.0f));
    }

    @Override
    protected void renderButtonText(DrawScope draw) {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float w = layoutBounds().width();
        float h = layoutBounds().height();
        UnityIcons.drawCreateDropdownIcon(draw, x, y, w, h, textColor());
    }
}

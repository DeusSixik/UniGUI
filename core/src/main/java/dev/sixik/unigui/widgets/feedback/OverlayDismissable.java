package dev.sixik.unigui.widgets.feedback;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Оверлей, который {@link OverlayLayer} закрывает по нажатию вне его.
 *
 * <p>{@code OverlayLayer} знает про {@link Popup}, {@link ContextMenu} и окна. Этот интерфейс даёт то же поведение
 * любым другим всплывающим виджетам (например, палитре нод), не привязывая слой к их типам.</p>
 */
@AiDoc(
        summary = "Contract for overlay widgets that an OverlayLayer dismisses when the pointer is pressed outside them.",
        purpose = "Lets custom floating widgets get the same click-away behavior as popups and context menus.",
        useWhen = {"Implement it on a custom overlay added through `OverlayLayer.addOverlay`."},
        criticalRules = {
                "`isInteractionTarget` must return true for the overlay itself and all its descendants.",
                "The layer only calls `dismiss` while `isOpenOverlay` and `dismissOnOutsideClick` are both true."
        },
        related = {"OverlayLayer", "Popup", "ContextMenu"})
public interface OverlayDismissable {
    /** @return {@code true}, если оверлей сейчас показан */
    boolean isOpenOverlay();

    /** @return {@code true}, если нажатие вне оверлея должно его закрывать */
    boolean dismissOnOutsideClick();

    /**
     * @param target виджет, по которому пришло нажатие
     * @return {@code true}, если нажатие относится к оверлею и закрывать его не нужно
     */
    boolean isInteractionTarget(Widget target);

    /** Закрывает оверлей. */
    void dismiss();
}

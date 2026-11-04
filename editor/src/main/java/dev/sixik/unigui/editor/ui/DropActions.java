package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.widgets.dnd.DragPayload;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.schema.TypeInfo;

/**
 * Что значит «бросить» перетаскиваемое на узел документа: новый тип из палитры добавляется, существующий узел
 * переносится. Одно правило для предпросмотра и иерархии.
 */
final class DropActions {
    /** Тип данных перетаскивания узла иерархии: в {@link DragPayload#data} лежит {@link UiNode}. */
    static final String NODE_TYPE = "unigui.node";
    /** Типы, которые принимают зоны сброса редактора. */
    static final String ACCEPTED = PalettePanel.PAYLOAD_TYPE + " " + NODE_TYPE;

    private DropActions() {
    }

    /**
     * Применяет перетаскивание к узлу.
     *
     * @param session сессия
     * @param payload то, что бросили
     * @param target  узел под указателем
     * @return {@code true}, если документ изменился
     */
    static boolean drop(EditorSession session, DragPayload payload, UiNode target) {
        if (session == null || payload == null || target == null) return false;
        if (payload.matchesType(PalettePanel.PAYLOAD_TYPE) && payload.data() instanceof TypeInfo<?> type) {
            return UiEdits.insertInto(session, type, target).isPresent();
        }
        if (payload.matchesType(NODE_TYPE) && payload.data() instanceof UiNode node) {
            return UiEdits.moveOnto(session, node, target);
        }
        return false;
    }
}

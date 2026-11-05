package dev.sixik.unigui.editor;

import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;
import java.util.function.Function;

/**
 * Панель редактора: иерархия, инспектор, вьюпорт, консоль.
 *
 * <p>Панель описывает, как называется, где по умолчанию лежит и как создаётся её содержимое. Каркас сам делает из неё
 * вкладку, пункт меню Window и участника раскладок; панель ничего не знает о других панелях и общается с ними через
 * {@link EditorContext}.</p>
 */
public interface EditorPanel {
    /** @return стабильный идентификатор; на него ссылаются раскладки */
    String id();

    /** @return заголовок вкладки и пункта меню Window */
    String title();

    /**
     * Создаёт содержимое. Вызывается один раз при сборке редактора; панель подписывается на события контекста здесь.
     *
     * @param context общие службы редактора
     * @return корневой виджет панели
     */
    Widget create(EditorContext context);

    /** @return иконка вкладки */
    default UiIcon icon() {
        return UiIcon.NONE;
    }

    /** @return область, в которую панель попадает при открытии через меню Window */
    default PanelSlot slot() {
        return PanelSlot.CENTER;
    }

    /**
     * Доля окна, которую панель занимает, когда открывается в боковой или нижней области отдельно от других.
     *
     * @return число от 0 до 1
     */
    default float share() {
        return 0.25f;
    }

    /** @return {@code true}, если панель можно закрыть */
    default boolean closable() {
        return true;
    }

    /**
     * Короткая запись панели из лямбды.
     *
     * @param id      идентификатор
     * @param title   заголовок
     * @param slot    область по умолчанию
     * @param factory создаёт содержимое
     * @return панель
     */
    static EditorPanel of(String id, String title, PanelSlot slot, Function<EditorContext, Widget> factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(factory, "factory");
        return new EditorPanel() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String title() {
                return title;
            }

            @Override
            public Widget create(EditorContext context) {
                return factory.apply(context);
            }

            @Override
            public PanelSlot slot() {
                return slot;
            }
        };
    }
}

package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.core.UIScaleProvider;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Данные XML-экрана: созданный корневой виджет, настройки масштаба и lifecycle
 * binding-подписок, которые нельзя хранить в отдельном виджете.
 *
 * <p>Основная настройка уровня экрана — {@link UIScaleProvider}. Runtime может
 * передать этот provider в {@code UIContext}, а при удалении экрана вызвать
 * {@link #close()} для отписки всех XML binding-ов.</p>
 *
 * @param root                  корневой виджет, созданный из XML
 * @param scaleProvider         провайдер масштаба, заданный XML-обёрткой экрана
 * @param scaleWithMinecraftGui умножать ли масштаб на масштаб Minecraft GUI
 * @param bindingSession        владелец подписок XML binding-ов
 * @param <T>                   тип корневого виджета
 */
public record XmlWidgetScreen<T extends Widget>(
        T root,
        UIScaleProvider scaleProvider,
        boolean scaleWithMinecraftGui,
        XmlBindingSession bindingSession) implements AutoCloseable {
    /**
     * Создаёт экран без observable binding-ов.
     *
     * @param root                  корневой виджет
     * @param scaleProvider         провайдер масштаба
     * @param scaleWithMinecraftGui умножать ли масштаб на масштаб Minecraft GUI
     */
    public XmlWidgetScreen(T root, UIScaleProvider scaleProvider, boolean scaleWithMinecraftGui) {
        this(root, scaleProvider, scaleWithMinecraftGui, new XmlBindingSession());
    }

    public XmlWidgetScreen {
        if (root == null) throw new IllegalArgumentException("XML widget screen root must not be null");
        scaleProvider = scaleProvider == null ? UIScaleProvider.IDENTITY : scaleProvider;
        bindingSession = bindingSession == null ? new XmlBindingSession() : bindingSession;
    }

    /**
     * Закрывает все binding-и, созданные при загрузке этого экрана.
     */
    @Override
    public void close() {
        bindingSession.close();
    }
}

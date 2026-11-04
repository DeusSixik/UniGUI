package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.api.core.UIScaleProvider;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlBindingSession;

/**
 * Результат загрузки: корневой виджет, настройки масштаба экрана и сессия привязок.
 *
 * <p>Если в документе есть {@code {Binding}}, экран нужно закрыть ({@link #close()}), когда он больше не
 * показывается: это отписывает виджеты от источников данных.</p>
 *
 * @param root                  корневой виджет
 * @param scaleProvider         провайдер масштаба; {@link UIScaleProvider#IDENTITY}, если не задан
 * @param scaleWithMinecraftGui умножать ли масштаб на GUI-scale Minecraft
 * @param bindingSession        привязки, созданные при загрузке
 * @param <T>                   тип корня
 */
public record LoadedScreen<T extends Widget>(
        T root,
        UIScaleProvider scaleProvider,
        boolean scaleWithMinecraftGui,
        XmlBindingSession bindingSession) implements AutoCloseable {

    public LoadedScreen {
        if (root == null) throw new IllegalArgumentException("screen root must not be null");
        scaleProvider = scaleProvider == null ? UIScaleProvider.IDENTITY : scaleProvider;
        bindingSession = bindingSession == null ? new XmlBindingSession() : bindingSession;
    }

    @Override
    public void close() {
        bindingSession.close();
    }
}

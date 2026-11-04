package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.xaml.bind.XamlBindingContext;
import dev.sixik.unigui.xaml.bind.XamlBindingSession;

/**
 * Что расширению доступно на момент применения: ресурсы, источники данных и сессия привязок.
 *
 * @param resources область ресурсов в точке использования
 * @param bindings  источники данных для {@code {Binding}}
 * @param session   сессия, которой принадлежат созданные привязки (закрывается вместе с экраном)
 */
public record ExtensionContext(ResourceScope resources, XamlBindingContext bindings, XamlBindingSession session) {
}

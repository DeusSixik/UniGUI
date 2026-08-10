/**
 * Публичные контракты компоновки v2.
 *
 * <p>Новый код должен настраивать виджет через
 * {@code widget.layout(style -> ...)}. Устаревшие методы {@code preferredSize},
 * {@code minSize}, {@code maxSize}, {@code margin}, {@code align} и
 * {@code grow} остаются поддерживаемыми узконаправленными вспомогательными методами и
 * не заменяют остальные свойства компоновки v2.</p>
 *
 * <pre>{@code
 * VBox content = new VBox();
 * content.layout(style -> style
 *         .padding(8.0f)
 *         .gap(6.0f)
 *         .overflowY(Overflow.AUTO));
 *
 * Button action = new Button("Action");
 * action.layout(style -> style
 *         .widthPercent(100.0f)
 *         .minHeight(22.0f)
 *         .flex(1.0f, 1.0f, SizeValue.auto()));
 * }</pre>
 */
package dev.sixik.unigui.api.layout;

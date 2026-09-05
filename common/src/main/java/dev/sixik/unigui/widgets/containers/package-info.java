/**
 * Базовые retained-контейнеры UniGUI: панели, строки, flex-layout'ы, scroll,
 * split-панели и визуальные оболочки.
 *
 * <p>Классы из этого пакета отвечают за владение дочерними виджетами,
 * measurement/arrange-проходы и простую композицию интерфейса. Они не являются
 * Minecraft-специфичными и не содержат render-темы напрямую: визуальные детали
 * обычно передаются через renderer'ы, {@link dev.sixik.unigui.api.style.Theme}
 * и {@link dev.sixik.unigui.api.layout.LayoutStyle}.</p>
 *
 * <p>Типичный выбор контейнера:</p>
 *
 * <ul>
 *     <li>{@link dev.sixik.unigui.widgets.containers.Box} — фон, рамка и вложенные дети;</li>
 *     <li>{@link dev.sixik.unigui.widgets.containers.HBox}/{@link dev.sixik.unigui.widgets.containers.VBox} — линейный flex без переноса;</li>
 *     <li>{@link dev.sixik.unigui.widgets.containers.FlexBox} — CSS-подобный flex-контейнер с настройкой направления, переноса, gap и выравнивания через {@link dev.sixik.unigui.api.layout.LayoutStyle};</li>
 *     <li>{@link dev.sixik.unigui.widgets.containers.WrapPanel} — flow-layout с переносом строк;</li>
 *     <li>{@link dev.sixik.unigui.widgets.containers.GridBox} — равномерная сетка по колонкам;</li>
 *     <li>{@link dev.sixik.unigui.widgets.containers.ScrollView} — область просмотра для большого контента;</li>
 *     <li>{@link dev.sixik.unigui.widgets.containers.SplitPanel} — две области с перетаскиваемым разделителем.</li>
 * </ul>
 */
package dev.sixik.unigui.widgets.containers;

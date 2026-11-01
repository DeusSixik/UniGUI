
- [ ] Auto test framework

# Widgets Style
- [ ] Переработать базовый визуальный стиль библиотека чтобы она смотрелась более современно 

# Widgets API
- [ ] add Signal support for widgets
- [ ] in world widget render
- [ ] 3D widget render (Like [Noesis GUI](https://www.noesisengine.com/xamltoy/8ab948fb3b5a3edaaee5bb6cd7017a8b))
- [ ] Better style pack & style API. Он должен позволять создавать большую часть вещей без написание кода рендера у виджета
- [ ] Вывести XAML API из Legacy состояния. Оптимизировать, добавить примеры и доработать action bindings

# Text & Layout
- [ ] Замер текста не совпадает с отрисовкой. `TextWidget.measure()` (и ещё ~16 виджетов в `core/.../widgets`) вызывает `TextEngine.measureTextHeight/measureLineWidth/wrapLines` без шрифта бэкенда и берёт запасную метрику (`fallbackLineHeight`, 10px на строку при размере 10), а рендер использует `backend.defaultTextFace()` (в тесте 12.6px на строку, +26%). Из-за этого у многострочных `Label` и автовысоты высота занижена: в `ScrollView` низ контента нельзя прокрутить, нижняя строка обрезается. `LayoutContext` знает только `availableWidth/Height`, шрифта в нём нет.
  - Решение: передавать шрифт бэкенда в замер (через `LayoutContext` или `UIContext`) вместо запасной метрики. Глобальный default face для замера проще, но это глобальное состояние.
  - Риск: изменятся размеры всего, что измеряется по тексту (кнопки без заданной высоты, строки `TreeView`, `Toast`, поля ввода), и могут поехать раскладки в демо и `TestMod`. Вероятно, упадут тесты с точными числами (`BasicControlsSelfTest`). Сначала прогнать полный набор тестов с пробной правкой и посмотреть, сколько упадёт.
  - Обход сейчас: задавать многострочному тексту явный шрифт `label.font(Fonts.defaultFace(), size)` (так сделано для кода в `DemoCategory.samplePanel`). Добавить предупреждение об этом в вики/документацию `TextWidget`.

# Render API
- [ ] Метод для виджетов `renderToTexture()` как в Unigine который позволяет зарендерить виджет или дерево виджетов в текстуру для работы с ней в шейдера и т.п
- [ ] Screen overwrite. Механизм который позволяет полностью заменить экран Minecraft на экран UniGUI

# Wiki
- [ ] Сделать больше примеров
- [ ] Добавить раздел `Get started`
# Inline Style API

Визуальные свойства виджета можно задавать строкой или типизированной лямбдой. Оба вызова дополняют один inline-набор и имеют приоритет над темой:

```java
button.style("background: #7A1F1FFF; radius: 8;");
button.style(style -> style
        .put(StyleKeys.TEXT_COLOR, MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f))
        .put(StyleKeys.BORDER_WIDTH, 2.0f));
```

Поддерживаются ключи из `StyleKeyRegistry.builtIns()`. Для короткой записи доступны aliases `background`, `border` и `color`; они соответствуют `background.color`, `border.color` и `text.color`. Неизвестные или неверные декларации не прерывают загрузку: их можно получить через `widget.styleDiagnostics()`.

Каскад `StylePack` применяет подходящие правила в порядке добавления. Каждое следующее правило перекрывает значения предыдущего; специфичность selector'а на порядок не влияет.

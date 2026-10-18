# XAML data binding

Документ описывает следующий слой XML UI. В проекте используется XML-диалект
UniGUI, совместимый по идее с XAML: дерево виджетов и их свойства находятся в
разметке, а состояние и поведение приходят из Java. Это не попытка добавить
reflection-вызовы методов из XML.

## Текущее состояние

В API уже есть базовые примитивы:

- `XmlObservableValue<T>` — типизированный источник значения;
- `XmlMutableObservableValue<T>` — in-memory источник для code-behind и preview;
- `XmlBindingContext` — реестр значений по путям;
- `XmlBinding<T>` — подписка источника на callback виджета с lifecycle `close()`;
- `XmlBindingStatus` и `XmlBindingDiagnosticsModel` — ошибки, которые можно показать
  в редакторе.

XML уже умеет ссылаться на именованные команды через `onClick="screen.save"`.
MVP property binding подключён для зарегистрированных bindable-атрибутов:
загрузчик разбирает `{Binding path}`, создаёт binding-сессию экрана и применяет
начальное значение с последующими изменениями источника.

## Контракт реализованного MVP

Добавить однонаправленную связь `source -> view`:

```xml
<VBox visible="{Binding screen.showSettings}">
    <Label text="{Binding user.displayName}" />
    <Button text="Сохранить" onClick="screen.save" />
</VBox>
```

После загрузки XML виджет получает начальное значение источника. При изменении
`XmlObservableValue` значение повторно передаётся setter-у свойства. Если путь,
тип или преобразование не подходят, загрузка runtime-документа не падает без
необходимости: binding получает ошибочный `XmlBindingStatus`, а редактор может
показать диагностику.

## Предлагаемый синтаксис

### Значения

```xml
<Label text="{Binding user.name}" />
<Slider value="{Binding settings.gamma}" />
<Panel visible="{Binding ui.showAdvanced}" />
```

В первой версии разрешается только форма `{Binding path}`. Пробелы по краям
пути игнорируются. Фильтры, выражения, вызовы методов и вложенный reflection
синтаксис в MVP не добавляются.

### Команды

Существующий безопасный синтаксис остаётся основным:

```xml
<Button onClick="screen.save" />
```

Позже можно добавить явную запись `{Command screen.save}`, но она должна
разрешаться через тот же `XmlCommandRegistry`, а не вызывать Java-методы по
имени. Это сохранит совместимость и единый контроль разрешённых действий.

## Контракт загрузчика

`XmlWidgetLoader` должен различать три случая:

1. обычное значение (`text="Save"`);
2. binding expression (`text="{Binding user.name}"`);
3. action/command attribute (`onClick="screen.save"`).

Для второго случая descriptor свойства должен отдавать не только парсер
литерала, но и binding target: ожидаемый тип, setter и правила преобразования.
Binding создаётся после создания виджета и применения обычных атрибутов, чтобы
его можно было корректно закрыть вместе с загруженным UI.

Предлагаемый внутренний lifecycle:

```text
parse XML -> create widgets -> apply literals -> create binding session
         -> initial push -> subscribe -> return root + session ownership
```

`XmlBindingSession` должен хранить все созданные `XmlBinding` и закрывать их
одним вызовом. Сессия принадлежит загруженному экрану, а не глобальному
`XmlBindingContext`; это предотвращает утечки подписок при hot reload.

## Контекст и code-behind

Java-код регистрирует источники явно:

```java
XmlBindingContext context = XmlBindingContext.empty();
context.mutable("user.displayName", String.class, "Alex");
context.mutable("screen.showSettings", Boolean.class, true);

XmlCommandRegistry commands = XmlCommandRegistry.empty()
        .register("screen.save", (source, event) -> controller.save());
```

Загрузчик получает context и command registry через отдельные настройки. XML не
получает произвольный Java-объект и не сканирует его методы reflection-ом.

## Типы и преобразования

Сначала binding применяется только при совместимом runtime-типе. Для строковых
XML-атрибутов, которые должны получать число, цвет или enum, используется
отдельный registry конвертеров. Не следует молча преобразовывать произвольные
значения через `toString()`.

Ошибки должны различать:

- отсутствующий path;
- несовместимый тип source и target;
- неизвестный конвертер;
- повторную регистрацию одного path/target.

Каждая ошибка хранит path, имя свойства и положение в XML, чтобы её можно было
показать в `XmlWidgetDiagnosticsPanel`.

## Two-way и коллекции

Двусторонняя связь не входит в первый MVP. Она потребует явного writable target,
защиты от циклов и определения поведения при конфликте изменений. Предлагаемая
форма для следующего этапа:

```xml
<TextField text="{Binding user.name, mode=TwoWay}" />
```

`ItemsSource`, шаблоны строк, сортировка и наблюдение за коллекциями должны быть
отдельным этапом после стабилизации scalar binding. Не следует смешивать их с
первым контрактом свойств.

## Обязательный рабочий пример

Команда `/unigui xaml` открывает живой
экран, где:

1. разметка создаёт `Label`, `Slider` и `Button`;
2. изменение Java observable сразу меняет значение виджета;
3. кнопка вызывает зарегистрированную Java-команду;
4. отдельный API-сценарий показывает type mismatch в diagnostics.

Команда демонстрирует scalar binding MVP. Полноценным XAML API слой станет после
добавления конвертеров, explicit TwoWay и наблюдаемых коллекций.


# Ordered Primitive Stream SPEC

## Статус

Документ описывает следующий этап оптимизации рендера UniGUI после внедрения
instanced SDF-фигур и базового объединения соседних `MESH`-команд.

Документ является проектной спецификацией. Он не требует немедленного изменения
публичного API и не должен ломать текущий fallback-рендер.

## Контекст

Сейчас draw list сохраняет правильный порядок команд, после чего
`SimpleDrawBatcher` разбивает его на batch-и по типу и состоянию. Это безопасно,
но команды разных renderer-ов разрывают поток.

Типичная картина после текущих оптимизаций:

```text
SDF commands:       2300+
SDF draws:          около 120
MESH commands:      около 100
MESH batches:       около 100
barriers:           мало
singleton batches:  много
```

SDF уже рисуется instanced-проходом. Соседние mesh-команды объединяются в один
buffer, но `MESH` и SDF всё ещё не могут попасть в один GPU submit, потому что
используют разные vertex format, shader и способ интерпретации данных.

Цель следующего этапа - создать общий ordered primitive stream, который:

- сохраняет исходный порядок команд;
- не сортирует прозрачные элементы;
- группирует совместимые фрагменты потока;
- уменьшает переключения между небольшими mesh/SDF-проходами;
- оставляет backend-neutral draw list и публичный API неизменными;
- позволяет использовать разные shader-программы без лишнего `flush` для каждой
  команды.

## Цели

- Сократить количество фактических GPU submit-ов для смешанного потока
  `MESH + SDF + обычные цветные primitive-команды`.
- Уменьшить количество вызовов `graphics.flush()` и временных
  `BufferBuilder`/`MeshData`.
- Сохранить визуальный результат при прозрачности, additive blend и перекрытии
  элементов.
- Сохранить индивидуальные transform-ы команд.
- Сохранить clipping и порядок применения clip state.
- Сделать возможность измерить пользу механизма в DebugOverlay.
- Дать надёжный fallback на существующие renderer-ы при ошибке или отсутствии
  поддержки.

## Не входит в задачу

- Глобальная сортировка draw list по shader, texture или blend mode.
- Изменение z-order виджетов.
- Автоматическое перемещение прозрачных команд между соседними командами.
- Полный material system или shader graph.
- Перевод vanilla text renderer на новый поток.
- Перевод произвольных `CUSTOM` и `SHADER` команд без отдельного контракта.
- Обязательное использование OpenGL 4.x, compute shader или persistent mapped
  buffers.
- Удаление старого per-command renderer.

## Основные инварианты

### Порядок

Команды должны исполняться в том же логическом порядке, в котором они записаны в
`DrawList`.

Разрешается объединять команды только внутри непрерывного диапазона, если
объединённый renderer выдаёт тот же результат, что и последовательное выполнение
исходных команд.

Нельзя объединять команды через:

- `PUSH_CLIP` или `POP_CLIP`;
- изменение render target;
- изменение blend mode;
- изменение texture binding, если shader использует одну texture slot;
- `CUSTOM` или неизвестный backend side effect;
- shader-команду, которая может читать уже нарисованный framebuffer;
- явный barrier marker.

### Прозрачность

Для обычного alpha blending порядок перекрытия является частью результата. Даже
если две команды используют один shader, их нельзя собирать в общий буфер после
того, как между ними есть команда, которая визуально должна быть нарисована
между ними.

Допускается общий buffer для последовательных команд: vertex data добавляется в
него в исходном порядке, а triangles идут в том же порядке.

### Transform

Transform нельзя хранить только один раз на batch, если команды могут иметь
разные transform-ы. Каждый vertex или instance должен получить transform своей
команды.

Рекомендуемый вариант для первой реализации - применить command matrix на CPU
при записи stream-а. Это соответствует текущим `MinecraftTransform.commandMatrix`
и не требует добавления matrix attributes на каждую вершину.

### Clipping

Scissor clipping остаётся stateful операцией backend-а. Ordered stream не должен
пересекать границы clip scope.

Минимальная единица stream-а:

```text
clip state
blend state
texture/material state
ordered primitive commands
```

При изменении clip state текущий stream завершается, затем применяется новый
scissor и начинается следующий stream.

## Термины

### Logical command

Исходный `DrawCommand` из `DrawList`. Он остаётся владельцем семантики UI и не
знает о конкретном GPU buffer.

### Primitive packet

Лёгкое backend-внутреннее представление одной команды после классификации.
Пакет содержит только данные, нужные выбранному renderer-у:

- тип primitive;
- transform;
- цвет и alpha;
- геометрию или ссылку на mesh vertices;
- texture/material key;
- blend key;
- clip key;
- исходную позицию в ordered stream.

### Stream segment

Непрерывный диапазон primitive packets, который можно отрисовать одним shader
pass без нарушения инвариантов.

### Backend submit

Один вызов GPU draw или один эквивалентный submit для конкретного renderer-а.
`EST submit` должен показывать именно эту оценку, а runtime counters должны
показывать фактически выполненные submits.

## Предлагаемая архитектура

```text
DrawList
  -> DrawBatcher
  -> OrderedPrimitiveStreamBuilder
  -> StreamSegment[]
  -> SegmentRenderer
  -> GPU submits
```

Новый механизм должен быть backend-внутренним. На первом этапе не требуется
добавлять `OrderedPrimitiveStream` в `api.render`.

Рекомендуемые классы:

```text
dev.sixik.unigui.impl.render.OrderedPrimitiveStream
dev.sixik.unigui.impl.render.OrderedPrimitiveStreamBuilder
dev.sixik.unigui.backend.minecraft_impl.MinecraftOrderedPrimitiveRenderer
dev.sixik.unigui.backend.minecraft_impl.MinecraftPrimitiveSegment
```

Названия не являются обязательными, но ответственность должна быть разделена:

- builder классифицирует и формирует сегменты;
- renderer владеет OpenGL buffer/program state;
- существующие SDF, mesh и texture renderer-ы остаются fallback и могут быть
  вызваны для неподдержанных сегментов.

## Классификация команд

### Поддерживаемые команды первой версии

- `RECT`;
- `ROUNDED_RECT`;
- `LINE`;
- `CIRCLE`;
- `MESH` без framebuffer-dependent shader;
- `MESH` с одной общей texture binding.

### Команды, создающие barrier

- `PUSH_CLIP`;
- `POP_CLIP`;
- `TEXT`;
- `TEXTURE` и `TEXTURED_QUAD`, если они используют отдельный texture renderer;
- `SHADER`;
- `CUSTOM`;
- `DRAW_CMD`;
- mesh с неподдержанным vertex payload;
- mesh с texture/material state, который нельзя представить в текущем сегменте.

На первом этапе текст и произвольные shader-команды не нужно насильно включать в
общий stream. Они должны корректно завершать текущий segment.

## Segment key

Сегмент должен иметь компактный ключ совместимости:

```java
final class PrimitiveSegmentKey {
    int rendererKind;
    int blendMode;
    int textureId;
    int textureOptionsId;
    int clipScope;
    int targetId;
}
```

Возможные значения `rendererKind`:

```text
SDF_SHAPE
COLOR_MESH
TEXTURED_MESH
```

`rendererKind` является частью ключа, поэтому нельзя случайно объединить SDF
instance data с обычными mesh vertices.

`clipScope` должен быть идентификатором текущего clip scope, а не только
координатами прямоугольника. Если backend допускает переиспользование одинакового
scissor, это можно оптимизировать позже.

## Важное ограничение объединения SDF и MESH

Первый вариант ordered stream не обязан физически отрисовывать SDF и MESH одним
`glDraw*` вызовом. Они используют разные shader-программы, поэтому минимумом
является один segment на renderer kind.

Например:

```text
SDF, SDF, SDF, MESH, MESH, SDF
```

превращается в:

```text
SDF segment  -> 1 instanced draw
MESH segment -> 1 mesh draw
SDF segment  -> 1 instanced draw
```

Преимущество появляется за счёт того, что:

- соседние команды разных logical type внутри одного renderer-а собираются
  заранее;
- mesh не создаёт отдельный `flush` на каждую команду;
- state capture/restore выполняется на segment, а не на каждой команде;
- buffer allocation и shader setup происходят реже.

Полное физическое объединение SDF и MESH в один GPU draw является отдельной,
более сложной фазой и не должно смешиваться с первой реализацией.

## Форматы segment buffer

### SDF segment

Использует текущий instance format `MinecraftSdfShapeRenderer`:

- четыре transformed corner positions;
- local bounds;
- color;
- size;
- line endpoints;
- radius и stroke width;
- shape type и stroke flag.

Новый stream должен переиспользовать существующий grow-only CPU buffer и не
создавать новый `FloatBuffer` на каждый segment.

### Color mesh segment

```text
position: vec3
color:    rgba8 или четыре float
```

Все вершины добавляются в исходном порядке команд. Для каждой команды
применяется её собственная command matrix.

### Textured mesh segment

```text
position: vec3
uv:       vec2
color:    rgba8 или четыре float
```

В segment допускается только одна совместимая texture binding. Если texture
меняется, segment закрывается.

## Жизненный цикл кадра

```text
beginFrame
  clear/reuse stream buffers

record draw list
  classify command
  update current segment key
  append packet

finish stream
  close current segment
  seal packet ranges

render stream
  graphics.flush once before raw GL pass
  for each segment:
      apply clip/blend/texture state
      upload segment buffer
      issue one renderer draw
  restore previous GL state

endFrame
  publish runtime counters
```

Построение stream-а не должно выполнять GPU-вызовы. Все GL-операции остаются в
renderer phase.

## Управление памятью

### CPU

- Использовать переиспользуемые `ObjectArrayList`/primitive arrays.
- Не создавать `SegmentKey` через `record` на каждый command.
- Не создавать `Matrix4f` на каждый vertex.
- Переиспользовать временную matrix и числовые buffers.
- Хранить диапазоны `start/count`, а не копировать vertex arrays между слоями.

### GPU

- Использовать stream VBO с capacity growth.
- При достаточной поддержке можно добавить orphaning через `glBufferData(...,
  null, GL_STREAM_DRAW)` перед новым upload.
- Persistent mapped buffer не является обязательным для первой версии.
- Размер VBO должен ограничиваться разумным максимумом; при переполнении segment
  нужно отправить текущую часть и продолжить, не теряя порядок.

## Fallback

Fallback должен срабатывать отдельно для каждого segment, а не отключать весь
новый renderer навсегда после единичной неподдержанной команды.

Сценарии fallback:

- OpenGL instancing недоступен;
- shader не скомпилировался;
- texture binding не разрешился;
- segment buffer переполнен и не смог расшириться;
- runtime GL error в debug режиме.

Поведение:

1. завершить текущий raw pass;
2. отрисовать неподдержанный диапазон существующим renderer-ом;
3. продолжить stream со следующего безопасного сегмента;
4. записать причину fallback в debug counters.

Нельзя использовать частично заполненный segment после ошибки shader/VBO.

## Runtime counters

Debug-информация должна разделять логические команды и фактический GPU workload.

Минимальные новые счётчики:

```text
stream segments
stream segment splits
stream SDF segments
stream color mesh segments
stream textured mesh segments
stream uploaded vertices
stream uploaded instances
stream GPU submits
stream fallback segments
stream fallback reason
```

Текущие значения должны остаться доступными:

- `SDF ACTUAL commands`;
- `SDF ACTUAL draws`;
- `SDF ACTUAL uniforms`;
- `SDF ACTUAL flushes`;
- mesh vertices;
- command count;
- barriers.

Рекомендуемые строки overlay:

```text
STREAM segments 12 | splits 11 | submits 12 | fallback 0
STREAM sdf 8 | mesh 4 | texturedMesh 0 | vertices 11946
```

Текущую строку `EST submit` следует оставить оценочной и явно отличать от
`ACTUAL submits`.

## Предлагаемые этапы реализации

### Этап 1: инструментирование

- Добавить внутренние типы segment и segment key.
- На основании текущего draw list построить stream без изменения renderer-а.
- Сравнить segment boundaries с текущими batch boundaries.
- Добавить runtime counters и fallback reason.

Критерий готовности: stream можно включать диагностическим флагом и проверять,
что число команд и порядок сегментов совпадают с исходным draw list.

### Этап 2: общий color mesh segment

- Перенести текущий `renderMeshBatch` в segment renderer.
- Объединять все совместимые соседние MESH-команды.
- Переиспользовать CPU/GPU buffers.
- Убрать `flush` и state capture между командами одного segment.

Критерий готовности: визуальное сравнение с fallback на SolarNavigation,
LevelMap и mini-games без отличий в alpha, transform и clipping.

### Этап 3: SDF segment

- Подключить текущий instanced SDF renderer как renderer segment-а.
- Перевести SDF CPU instance preparation на переиспользуемый stream buffer.
- Оставить старый SDF path для fallback.

Критерий готовности: число SDF draws не увеличилось, а runtime CPU не вырос.

### Этап 4: смешанный ordered stream

- Разрешить последовательные SDF и MESH segment-ы в одном stream.
- Устранить лишние общие `graphics.flush()` между segment-ами.
- Корректно завершать stream на text, texture, shader, custom и clip commands.

Критерий готовности: итоговое изображение пиксельно совпадает с legacy path в
тестовых сценах с прозрачностью и перекрытием.

### Этап 5: дальнейшая оптимизация

Только после измерений рассмотреть:

- общий shader для простого mesh и SDF-подобных quad;
- multi-draw indirect или multi-draw elements;
- persistent mapped buffers;
- packed colors и packed shape parameters;
- частичное объединение segment-ов с одинаковым shader/material state.

Эти оптимизации не входят в минимальную реализацию и могут усложнить fallback.

## Тестовая матрица

### Сцены

- `SolarNavigationScreen` с большим числом линий, кругов и mesh-треугольников;
- `LevelMapScreen` с комнатами, лестницами, легендой и clipping;
- `WireConnectionMinigameScreen`;
- `SpannerRhythmMinigameScreen`;
- UI с additive blend;
- UI с несколькими nested clip scopes;
- UI с transform rotation/scale;
- UI в render target и post-effect layer.

### Сравнение

Для каждой сцены сравнивать:

- screenshot legacy и ordered stream;
- command count;
- segment count;
- actual submits;
- actual flushes;
- CPU render time;
- GPU render time;
- allocations/GC на кадр;
- поведение при resize и изменении GuiScale.

### Обязательные проверки порядка

- полупрозрачный mesh поверх SDF-фона;
- SDF-линия между двумя mesh-треугольниками;
- additive элемент между двумя normal-alpha элементами;
- clip push/pop между двумя одинаковыми mesh-командами;
- transform только у второй команды в одном segment.

## Критерии принятия

Первая production-ready версия должна соответствовать всем условиям:

- визуальный результат совпадает с fallback;
- исходный порядок команд не меняется;
- `CUSTOM`, `SHADER`, text и clip semantics не ломаются;
- на OpenGL без instancing работает legacy fallback;
- в обычном UI нет обязательных allocation-ов на каждый command;
- количество mesh submits уменьшается в сценах с соседними mesh-командами;
- общий CPU render time не становится выше текущего более чем на 5%;
- debug counters показывают фактический workload отдельно от estimate;
- обе версии Minecraft (`1.20.1` и `1.21.1`) проходят compile и self-test.

## Риски

### Нарушение порядка

Самый опасный риск - попытка глобально сортировать команды ради меньшего числа
shader switches. Решение: только линейное объединение непрерывных совместимых
диапазонов.

### Несовместимые transform-ы

Если transform применяется один раз на весь segment, элементы будут смещены или
повёрнуты неверно. Решение: transform применяется к данным каждой команды.

### Разный alpha model

Цветной mesh, текстурированный mesh и SDF могут использовать разные alpha/blend
правила. Решение: blend и alpha mode входят в segment key; смешивать их нельзя.

### Ошибки state restore

Raw OpenGL renderer может оставить VAO, VBO, shader, texture или scissor state.
Решение: один state guard на segment stream и отдельный debug режим с проверкой
GL error.

### Ложные метрики

Оценочный submit, количество logical batches и actual GPU draw calls нельзя
показывать как одно число. Решение: использовать отдельные поля и явные названия
`EST`/`ACTUAL`.

## Итоговое решение

Общий ordered primitive stream должен быть линейным backend-внутренним слоем между
`DrawList` и Minecraft renderer-ами. Он не сортирует команды и не меняет
публичную модель UI. Он выделяет непрерывные совместимые segment-ы, собирает
данные в переиспользуемые buffers и выполняет один submit на segment.

Первая реализация должна сосредоточиться на `COLOR_MESH`, `TEXTURED_MESH` и
текущем instanced `SDF_SHAPE`. Полное объединение разных shader-программ в один
GPU draw следует оставить отдельной будущей задачей. Такой порядок внедрения
даёт измеримую оптимизацию, сохраняет fallback и минимизирует риск изменения
визуального результата.

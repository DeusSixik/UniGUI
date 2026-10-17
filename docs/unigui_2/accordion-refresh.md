# ExpandablePanel / Accordion refresh (unigui_2)

Заголовок секции больше не вставляет inline-треугольник (`DropDownBox.createHeaderArrow`)
в текст. Шеврон — stroke-слот справа, тот же что у `Button`/`DropDownBox`.

## Почему два класса

Не слиты. `ExpandablePanel` — одна секция: заголовок, контент, expanded.
`Accordion` — контейнер секций с режимом `singleOpen`. Слить их значит
либо потерять одиночную секцию, либо тащить режим «одна открыта» в каждую
панель. XML-имена и фабрики `Widgets` остаются раздельными.

## Заголовок

- Высота 32, радиус 4.
- Текст заголовка — только заголовок, без маркера.
- `headerButton.chevronVisible(true)`: слот 24px справа, два stroke-отрезка.
- Раскрыто — шеврон вверх (`chevronUp`), свёрнуто — вниз.
- Контент по-прежнему `visibility(COLLAPSED)`, не рисуется и не меряется.

## Тест

`testExpandablePanelAndAccordionContracts` ждёт чистый текст `Advanced` и
минимум два `LINE` (шеврон) и в раскрытом, и в свёрнутом кадре. `MESH`
больше не требуется.

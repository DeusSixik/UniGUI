# Подсветка строк выпадающего списка

`ComboBox` автоматически подсвечивает строку при наведении. По умолчанию используется полупрозрачный цвет, который отделяет наведённую строку от остальных и не перекрывает выбранное состояние.

Цвет можно менять непосредственно или через fluent API. Настройка применяется к уже созданным строкам и к тем, которые будут добавлены позднее:

```java
ComboBox mode = new ComboBox()
        .items(List.of("Обычный", "Текстура", "Шейдер"))
        .optionHoverBackground(MutableColor.rgba(0.18f, 0.36f, 0.30f, 0.96f));

mode.optionHoverBackground().set(0.22f, 0.42f, 0.34f, 1.0f);
```

Для текстурного слоя передайте `TextureHandle`. Он рисуется поверх цвета и использует обычное размещение слоя кнопки:

```java
mode.optionHoverTexture(textureHandle);
```

Для процедурной подсветки используйте `ShaderHandle`. Шейдер рисуется поверх цвета и текстуры; uniforms и draw options настраиваются на обычной кнопке:

```java
mode.optionHoverShader(ShaderHandle.resource("example:shaders/dropdown_hover"));

mode.optionHoverShaderUniforms(ShaderUniforms.create().setFloat("intensity", 0.35f));
mode.optionHoverShaderOptions(ShaderDrawOptions.defaults().blend(true));
```

Для XAML доступны атрибуты `optionHoverBackground`, `optionHoverTexture` и `optionHoverShader`:

```xml
<ComboBox items="Обычный|Текстура|Шейдер"
          optionHoverBackground="#2E5C4DFF"
          optionHoverTexture="example:textures/ui/option_hover.png"
          optionHoverShader="example:shaders/dropdown_hover" />
```
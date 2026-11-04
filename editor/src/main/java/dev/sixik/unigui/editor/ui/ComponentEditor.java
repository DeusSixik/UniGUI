package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.editor.EditorLayout;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.EditorTheme;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.xaml.load.LoadOptions;

import java.util.function.Function;

/**
 * Редактор компонента: те же панели, что у {@link UiEditor} (иерархия тела, палитра, свойства, предпросмотр, текст,
 * проблемы), плюс панель контракта, где объявляются параметры, переменные и события и задаются примерные значения для
 * предпросмотра.
 *
 * <pre>{@code
 * XamlEditorDocument document = new XamlEditorDocument("card", "ProductCard",
 *         EditorSession.open(DocumentSource.file(path)));
 * EditorShell editor = ComponentEditor.shell(document);
 * }</pre>
 *
 * <p>Документ — {@code <Component>}; сессия, история отмены и сохранение общие с редактором экрана. Тело компонента
 * правится теми же панелями, что и экран: корень компонента в иерархии принимает палитру как тело.</p>
 */
public final class ComponentEditor {
    /** Имя раскладки «дизайн»: контракт слева, предпросмотр в центре. */
    public static final String DESIGN_LAYOUT = UiEditor.DESIGN_LAYOUT;
    /** Имя раскладки «текст»: редактор XAML в центре. */
    public static final String CODE_LAYOUT = UiEditor.CODE_LAYOUT;
    /** Идентификатор панели контракта. */
    public static final String CONTRACT = ContractPanel.ID;

    private ComponentEditor() {
    }

    /**
     * Собирает редактор компонента.
     *
     * @param document документ компонента
     * @return каркас
     */
    public static EditorShell shell(XamlEditorDocument document) {
        return shell(document, LoadOptions.DEFAULT, EditorTheme.dark(), builder -> builder);
    }

    /**
     * Собирает редактор компонента с настройками предпросмотра и темой.
     *
     * @param document       документ компонента
     * @param previewOptions настройки загрузки предпросмотра: резолвер текстур и команды
     * @param theme          тема каркаса
     * @param customize      позволяет добавить панели, команды и раскладки до сборки
     * @return каркас
     */
    public static EditorShell shell(XamlEditorDocument document, LoadOptions previewOptions, EditorTheme theme,
                                    Function<EditorShell.Builder, EditorShell.Builder> customize) {
        ComponentView view = new ComponentView();
        EditorShell.Builder builder = UiEditor.common(document, previewOptions, theme, view)
                .panel(new ContractPanel(view))
                .layout(EditorLayout.named(DESIGN_LAYOUT)
                        .left(0.24f, CONTRACT, UiEditor.HIERARCHY, UiEditor.PALETTE)
                        .right(0.28f, UiEditor.INSPECTOR)
                        .bottom(0.25f, UiEditor.DIAGNOSTICS)
                        .center(UiEditor.VIEWPORT, UiEditor.CODE)
                        .active(UiEditor.VIEWPORT)
                        .build())
                .layout(EditorLayout.named(CODE_LAYOUT)
                        .left(0.24f, CONTRACT)
                        .right(0.28f, UiEditor.INSPECTOR)
                        .bottom(0.25f, UiEditor.DIAGNOSTICS)
                        .center(UiEditor.CODE, UiEditor.VIEWPORT)
                        .active(UiEditor.CODE)
                        .build());
        return UiEditor.finish(customize.apply(builder).build(), document);
    }
}

package dev.sixik.unigui.xaml.preview;

import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.bind.XamlBindingStatus;
import dev.sixik.unigui.xaml.edit.NodePath;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.load.LoadedScreen;
import dev.sixik.unigui.xaml.load.XamlLoader;
import dev.sixik.unigui.xaml.schema.Schema;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Живой предпросмотр документа: держит загруженное дерево виджетов и пересобирает его при каждом изменении.
 *
 * <p>Главное свойство — устойчивость к ошибкам. Пока документ некорректен (автор ещё печатает), прежнее дерево
 * остаётся на месте, а причина доступна через {@link #error()}; когда документ снова корректен, дерево заменяется.
 * Для каждого виджета известен породивший его узел и наоборот, поэтому предпросмотр умеет подсвечивать выбранный
 * узел и выбирать узел по точке на экране. Класс не знает ни о редакторе, ни об экране: тем же предпросмотром
 * пользуется горячая перезагрузка в игре.</p>
 */
public final class Preview implements AutoCloseable {
    /**
     * Итог одной попытки показать документ.
     *
     * @param shown   {@code true}, если дерево заменено новым
     * @param message причина неудачи или пустая строка
     */
    public record Result(boolean shown, String message) {
        public Result {
            message = message == null ? "" : message;
        }
    }

    private final Schema schema;
    private final LoadOptions options;
    private LoadedScreen<Widget> screen;
    private Map<UiNode, Widget> widgets = new IdentityHashMap<>();
    private List<Map.Entry<UiNode, Widget>> order = List.of();
    private String error = "";
    private boolean closed;

    /**
     * Создаёт предпросмотр.
     *
     * @param schema  схема типов
     * @param options настройки загрузки: резолвер текстур, команды, источники привязок. Неизвестные атрибуты
     *                всегда допускаются, чтобы недописанный документ не гасил картинку
     */
    public Preview(Schema schema, LoadOptions options) {
        if (schema == null) throw new IllegalArgumentException("schema must not be null");
        this.schema = schema;
        this.options = (options == null ? LoadOptions.DEFAULT : options).strictAttributes(false);
    }

    /**
     * Пытается показать документ.
     *
     * @param document документ
     * @return успех или причина, по которой осталось прежнее дерево
     */
    public Result show(UiDocument document) {
        if (closed) throw new IllegalStateException("The preview is closed");
        if (document == null) {
            clear();
            return new Result(true, "");
        }
        Map<UiNode, Widget> created = new IdentityHashMap<>();
        List<Map.Entry<UiNode, Widget>> createdInOrder = new ArrayList<>();
        try {
            LoadOptions observed = options.nodeObserver((node, widget) -> {
                created.put(node, widget);
                createdInOrder.add(Map.entry(node, widget));
            });
            LoadedScreen<Widget> next = new XamlLoader(schema, observed).loadScreen(document);
            if (screen != null) screen.close();
            screen = next;
            widgets = created;
            order = createdInOrder;
            error = "";
            return new Result(true, "");
        } catch (RuntimeException failure) {
            error = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            return new Result(false, error);
        }
    }

    /** Убирает показанное дерево. */
    public void clear() {
        if (screen != null) screen.close();
        screen = null;
        widgets = new IdentityHashMap<>();
        order = List.of();
        error = "";
    }

    /** @return корень показанного дерева или пусто, если ничего не показано */
    public Optional<Widget> root() {
        return screen == null ? Optional.empty() : Optional.of(screen.root());
    }

    /** @return причина последней неудачи или пустая строка, если последняя попытка удалась */
    public String error() {
        return error;
    }

    /**
     * @param node узел документа
     * @return виджет, созданный из узла в показанном дереве
     */
    public Optional<Widget> widgetOf(UiNode node) {
        return Optional.ofNullable(widgets.get(node));
    }

    /**
     * Выбирает самый глубокий узел документа, чей виджет накрывает точку. Строка списка выбирает узел шаблона,
     * который её породил: правится шаблон, и изменение видно во всех строках. Тела компонентов в документе
     * отсутствуют (это отдельные документы), поэтому узлы внутри них пропускаются.
     *
     * @param document документ, узлы которого ищутся
     * @param x        координата точки в системе окна
     * @param y        координата точки в системе окна
     * @return узел или пусто, если под точкой нет виджетов документа
     */
    public Optional<UiNode> pick(UiDocument document, float x, float y) {
        UiNode hit = null;
        for (Map.Entry<UiNode, Widget> entry : order) {
            RectView bounds = entry.getValue().layoutBounds();
            boolean inside = x >= bounds.x() && x < bounds.x() + bounds.width()
                    && y >= bounds.y() && y < bounds.y() + bounds.height();
            // Родитель создаётся раньше ребёнка, поэтому последний накрывающий узел и есть самый глубокий.
            if (inside && NodePath.find(document, entry.getKey()).isPresent()) hit = entry.getKey();
        }
        return Optional.ofNullable(hit);
    }

    /** @return состояние привязок показанного дерева: какие источники найдены, какие нет и почему */
    public List<XamlBindingStatus> bindings() {
        return screen == null ? List.of() : screen.bindingSession().statuses();
    }

    /** @return {@code true} после {@link #close()} */
    public boolean closed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) return;
        clear();
        closed = true;
    }
}

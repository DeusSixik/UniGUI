package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.event.EventSubscription;

import java.util.function.Consumer;

/**
 * Активная связь типизированного observable-значения с code-behind или состоянием редактора.
 *
 * <p>Binding читает текущее значение источника сразу при создании, передаёт его в target callback,
 * а затем подписывается на последующие изменения. Если источник отсутствует или его тип не подходит,
 * объект всё равно создаётся, но остаётся неактивным и хранит {@link XmlBindingStatus} с диагностикой.
 * Это удобно для preview/editor режима: UI может показать проблему без падения загрузки всего XML.</p>
 *
 * <p>Binding владеет подпиской на observable source. Когда связь больше не нужна, вызовите
 * {@link #close()}, чтобы отписаться от изменений и перевести статус в {@link XmlBindingStatus.State#CLOSED}.</p>
 *
 * @param <T> ожидаемый runtime-тип значения, которое принимает target callback
 */
public final class XmlBinding<T> implements AutoCloseable {
    private final String targetName;
    private final Class<T> targetType;
    private final EventSubscription subscription;
    private XmlBindingStatus status;
    private boolean closed;

    private XmlBinding(String targetName, Class<T> targetType, EventSubscription subscription, XmlBindingStatus status) {
        this.targetName = XmlBindingStatus.normalizePath(targetName);
        this.targetType = targetType;
        this.subscription = subscription;
        this.status = status;
        this.closed = status == null || !status.active();
    }

    /**
     * Создаёт binding и использует имя observable source как имя target-а.
     *
     * <p>Метод удобен для прямого связывания: {@code context.bind("user.name", String.class, label::text)}.
     * Если {@code source == null}, имя будет пустым, а результат получит статус missing.</p>
     *
     * @param source observable source или {@code null}, если path не найден
     * @param targetType ожидаемый тип значения
     * @param target callback, который применяет значение к code-behind или editor state
     * @param <T> ожидаемый тип значения
     * @return активный binding либо неактивный binding с диагностикой
     */
    public static <T> XmlBinding<T> bind(XmlObservableValue<?> source, Class<T> targetType, Consumer<? super T> target) {
        String targetName = source == null ? "" : source.name();
        return bind(source, targetType, target, targetName);
    }

    /**
     * Создаёт binding с явным отображаемым именем target-а.
     *
     * <p>Перед подпиской метод проверяет наличие источника и совместимость runtime-типа.
     * При успешной проверке target получает текущее значение синхронно, а затем все будущие изменения.
     * При ошибке callback не вызывается, а возвращённый binding содержит статус для диагностики.</p>
     *
     * @param source observable source или {@code null}
     * @param targetType ожидаемый тип значения; не может быть {@code null}
     * @param target callback применения значения; не может быть {@code null}
     * @param targetName имя target-а для диагностик; пустое значение заменяется именем source
     * @param <T> ожидаемый тип значения
     * @return binding, который нужно закрыть после использования
     */
    public static <T> XmlBinding<T> bind(XmlObservableValue<?> source, Class<T> targetType,
                                         Consumer<? super T> target, String targetName) {
        if (targetType == null) throw new IllegalArgumentException("XML binding target type must not be null");
        if (target == null) throw new IllegalArgumentException("XML binding target must not be null");

        String path = targetName == null || targetName.trim().isEmpty()
                ? source == null ? "" : source.name()
                : targetName.trim();
        if (source == null) return inactive(path, targetType, XmlBindingStatus.missing(path, targetType));
        if (!targetType.isAssignableFrom(source.valueType())) {
            return inactive(path, targetType, XmlBindingStatus.typeMismatch(path, targetType, source.valueType()));
        }

        @SuppressWarnings("unchecked")
        XmlObservableValue<T> typedSource = (XmlObservableValue<T>) source;
        target.accept(typedSource.get());
        EventSubscription subscription = typedSource.onChanged(change -> target.accept(change.newValue()));
        return new XmlBinding<>(path, targetType, subscription, XmlBindingStatus.active(path, targetType, source.valueType()));
    }

    static <T> XmlBinding<T> inactive(String targetName, Class<T> targetType, XmlBindingStatus status) {
        return new XmlBinding<>(targetName, targetType, null, status);
    }

    /**
     * Возвращает текущий статус связи.
     *
     * <p>Если binding уже закрыт, но его последний сохранённый статус был активным,
     * метод возвращает synthetic closed status. Это защищает вызывающий код от устаревшего
     * отображения активной связи после {@link #close()}.</p>
     *
     * @return статус binding-а: active, closed или error
     */
    public XmlBindingStatus status() {
        if (closed && status.active()) return XmlBindingStatus.closed(targetName, targetType);
        return status;
    }

    /**
     * Проверяет, что binding сейчас активен и будет получать изменения source.
     *
     * @return {@code true}, если связь успешно создана и не закрыта
     */
    public boolean active() {
        return status().active();
    }

    /**
     * Закрывает binding и отписывает target callback от observable source.
     *
     * <p>Метод идемпотентен: повторные вызовы безопасны и не пытаются повторно отписывать
     * уже закрытую подписку.</p>
     */
    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (subscription != null) subscription.unsubscribe();
        status = XmlBindingStatus.closed(targetName, targetType);
    }
}

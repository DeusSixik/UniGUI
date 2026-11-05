package dev.sixik.unigui.api.animation;

import dev.sixik.unigui.api.core.UiDispatcher;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Компактный реестр активных анимаций одного владельца.
 *
 * <p>Реестр принадлежит UI-потоку. Если контроллер создан с {@link UiDispatcher}, операции,
 * изменяющие реестр, вызванные из другого потока, автоматически передаются в dispatcher. На UI-потоке
 * дополнительная задача не создаётся: операция выполняется сразу.</p>
 *
 * <p>Это поведение распространяется на {@link #play(Object, PlayableAnimation)}, остановку,
 * очистку и обновление анимаций. Методы чтения предназначены для вызова из UI-потока, чтобы не
 * читать внутреннюю FastUtil-коллекцию одновременно с её изменением.</p>
 */
public final class AnimationController {
    private final Object2ObjectOpenHashMap<Object, PlayableAnimation> animations = new Object2ObjectOpenHashMap<>();
    private volatile UiDispatcher dispatcher;

    /** Создаёт контроллер, работающий непосредственно в потоке вызывающего кода. */
    public AnimationController() {
    }

    /**
     * Создаёт контроллер, привязанный к dispatcher UI-потока.
     *
     * @param dispatcher dispatcher; {@code null} оставляет непосредственное выполнение
     */
    public AnimationController(UiDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Привязывает контроллер к dispatcher UI-потока.
     *
     * <p>Метод следует вызывать при создании или привязке владельца к UI-контексту. Само изменение
     * dispatcher должно выполняться из UI-потока, до начала конкурентного использования контроллера.</p>
     *
     * @param dispatcher dispatcher; {@code null} отключает автоматическую передачу
     * @return этот контроллер
     */
    public AnimationController dispatcher(UiDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        return this;
    }

    /** @return текущий dispatcher или {@code null}, если используется непосредственное выполнение */
    public UiDispatcher dispatcher() {
        return dispatcher;
    }

    /** @return {@code true}, если контроллер выполняет код в UI-потоке */
    public boolean isUiThread() {
        UiDispatcher current = dispatcher;
        return current == null || current.isUiThread();
    }

    /** Добавляет или заменяет анимацию по ключу. */
    public void play(Object key, PlayableAnimation animation) {
        if (key == null || animation == null) return;
        UiDispatcher current = dispatcher;
        if (current != null && !current.isUiThread()) {
            current.execute(() -> playNow(key, animation));
            return;
        }
        playNow(key, animation);
    }

    /** Добавляет анимацию в отдельное пространство ключей. */
    public void playScoped(int scope, Object key, boolean identity, PlayableAnimation animation) {
        if (key == null || animation == null) return;
        play(new ScopedKey(scope, key, identity), animation);
    }

    /** Останавливает анимацию по ключу. */
    public void stop(Object key) {
        if (key == null) return;
        UiDispatcher current = dispatcher;
        if (current != null && !current.isUiThread()) {
            current.execute(() -> stopNow(key));
            return;
        }
        stopNow(key);
    }

    /** Останавливает анимацию из отдельного пространства ключей. */
    public void stopScoped(int scope, Object key, boolean identity) {
        if (key == null) return;
        stop(new ScopedKey(scope, key, identity));
    }

    /** Останавливает все анимации заданного пространства ключей. */
    public void stopScope(int scope) {
        UiDispatcher current = dispatcher;
        if (current != null && !current.isUiThread()) {
            current.execute(() -> stopScopeNow(scope));
            return;
        }
        stopScopeNow(scope);
    }

    /** Останавливает все анимации. */
    public void clear() {
        UiDispatcher current = dispatcher;
        if (current != null && !current.isUiThread()) {
            current.execute(this::clearNow);
            return;
        }
        clearNow();
    }

    /** Возвращает активную анимацию по ключу без её удаления. Вызывается из UI-потока. */
    public PlayableAnimation get(Object key) {
        return key == null ? null : animations.get(key);
    }

    /** Возвращает анимацию из отдельного пространства ключей. Вызывается из UI-потока. */
    public PlayableAnimation getScoped(int scope, Object key, boolean identity) {
        return key == null ? null : get(new ScopedKey(scope, key, identity));
    }

    /** Возвращает view активных анимаций без копирования. Используется из UI-потока. */
    public ObjectCollection<PlayableAnimation> values() {
        return animations.values();
    }

    /** Останавливает все анимации указанного типа. */
    public void stopAllOf(Class<?> type) {
        if (type == null) return;
        UiDispatcher current = dispatcher;
        if (current != null && !current.isUiThread()) {
            current.execute(() -> stopAllOfNow(type));
            return;
        }
        stopAllOfNow(type);
    }

    /** Обновляет все анимации одним delta. */
    public void update(float deltaSeconds) {
        float delta = AnimationClock.sanitizeDelta(deltaSeconds);
        UiDispatcher current = dispatcher;
        if (current != null && !current.isUiThread()) {
            current.execute(() -> updateNow(delta));
            return;
        }
        updateNow(delta);
    }

    /** Обновляет все анимации из источника времени. */
    public void update(AnimationClock clock) {
        update(clock == null ? 0.0f : clock.deltaSeconds());
    }

    /** @return есть ли активные анимации; вызывается из UI-потока */
    public boolean hasActiveAnimations() { return !animations.isEmpty(); }

    /** @return число активных анимаций; вызывается из UI-потока */
    public int size() { return animations.size(); }

    private void playNow(Object key, PlayableAnimation animation) {
        PlayableAnimation previous = animations.put(key, animation);
        if (previous != null && previous != animation) previous.cancel();
    }

    private void stopNow(Object key) {
        PlayableAnimation animation = animations.remove(key);
        if (animation != null) animation.cancel();
    }

    private void stopScopeNow(int scope) {
        ObjectIterator<Object2ObjectMap.Entry<Object, PlayableAnimation>> iterator = animations.object2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            Object2ObjectMap.Entry<Object, PlayableAnimation> entry = iterator.next();
            if (entry.getKey() instanceof ScopedKey scoped && scoped.scope == scope) {
                entry.getValue().cancel();
                iterator.remove();
            }
        }
    }

    private void clearNow() {
        for (PlayableAnimation animation : animations.values()) animation.cancel();
        animations.clear();
    }

    private void stopAllOfNow(Class<?> type) {
        ObjectIterator<Object2ObjectMap.Entry<Object, PlayableAnimation>> iterator = animations.object2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            PlayableAnimation animation = iterator.next().getValue();
            if (type.isInstance(animation)) {
                animation.cancel();
                iterator.remove();
            }
        }
    }

    private void updateNow(float delta) {
        ObjectIterator<Object2ObjectMap.Entry<Object, PlayableAnimation>> iterator = animations.object2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            PlayableAnimation animation = iterator.next().getValue();
            animation.update(delta);
            if (animation.isFinished()) iterator.remove();
        }
    }

    private static final class ScopedKey {
        private final int scope;
        private final Object key;
        private final boolean identity;

        private ScopedKey(int scope, Object key, boolean identity) {
            this.scope = scope;
            this.key = key;
            this.identity = identity;
        }

        @Override
        public int hashCode() {
            int valueHash = identity ? System.identityHashCode(key) : key.hashCode();
            return 31 * scope + valueHash;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof ScopedKey other) || scope != other.scope || identity != other.identity) return false;
            return identity ? key == other.key : key.equals(other.key);
        }
    }
}
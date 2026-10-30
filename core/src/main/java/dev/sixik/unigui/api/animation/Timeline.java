package dev.sixik.unigui.api.animation;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Objects;
import java.util.function.Function;

/**
 * Композиция нескольких анимаций.
 *
 * <p>Timeline не создаёт задачи или callback-объекты во время обновления. Последовательность
 * обновляет только текущий элемент, параллельная композиция проходит по заранее подготовленному
 * массиву. Все элементы используют общий {@link PlayableAnimation} контракт.</p>
 */
public final class Timeline implements PlayableAnimation {
    private enum Mode { SEQUENCE, PARALLEL, STAGGER }

    private final Mode mode;
    private final ObjectArrayList<PlayableAnimation> children;
    private final FloatArrayList staggerStartTimes;
    private int sequenceIndex;
    private float staggerElapsedSeconds;
    private boolean cancelled;

    private Timeline(Mode mode, PlayableAnimation[] animations) {
        this.mode = mode;
        this.children = new ObjectArrayList<>(animations.length);
        this.staggerStartTimes = null;
        for (PlayableAnimation animation : animations) {
            if (animation != null) children.add(animation);
        }
    }

    private Timeline(ObjectArrayList<PlayableAnimation> animations, float offsetSeconds) {
        this.mode = Mode.STAGGER;
        this.children = animations;
        this.staggerStartTimes = new FloatArrayList(animations.size());
        for (int i = 0; i < animations.size(); i++) {
            staggerStartTimes.add(offsetSeconds * i);
        }
    }

    /** Создаёт последовательное проигрывание анимаций. */
    public static Timeline sequence(PlayableAnimation... animations) {
        return new Timeline(Mode.SEQUENCE, animations == null ? new PlayableAnimation[0] : animations);
    }

    /** Создаёт параллельное проигрывание анимаций. */
    public static Timeline parallel(PlayableAnimation... animations) {
        return new Timeline(Mode.PARALLEL, animations == null ? new PlayableAnimation[0] : animations);
    }

    /**
     * Запускает анимации элементов с одинаковым временным сдвигом между стартами.
     *
     * <p>Factory вызывается один раз при создании timeline, поэтому пользовательские анимации и их
     * writer'ы создаются вне кадрового hot path. Все элементы известны заранее, но до своего
     * стартового времени обновляться не будут.</p>
     *
     * @param items элементы, для которых создаются анимации
     * @param factory фабрика анимации одного элемента
     * @param offsetMilliseconds задержка между соседними стартами в миллисекундах
     * @param <T> тип элемента
     * @return timeline со stagger-запуском
     * @throws NullPointerException если factory равна {@code null}
     * @throws IllegalArgumentException если factory вернула {@code null} или offset некорректен
     */
    public static <T> Timeline stagger(Iterable<? extends T> items,
                                       Function<? super T, ? extends PlayableAnimation> factory,
                                       float offsetMilliseconds) {
        Objects.requireNonNull(factory, "factory");
        if (!Float.isFinite(offsetMilliseconds) || offsetMilliseconds < 0.0f) {
            throw new IllegalArgumentException("offsetMilliseconds must be finite and non-negative");
        }

        ObjectArrayList<PlayableAnimation> animations = new ObjectArrayList<>();
        if (items != null) {
            for (T item : items) {
                PlayableAnimation animation = factory.apply(item);
                if (animation == null) {
                    throw new IllegalArgumentException("stagger factory returned null animation");
                }
                animations.add(animation);
            }
        }
        return new Timeline(animations, offsetMilliseconds * 0.001f);
    }

    /** Создаёт задержку, которую можно включить в sequence. */
    public static PlayableAnimation delay(float seconds) {
        return new DelayAnimation(seconds);
    }

    @Override
    public void update(float deltaSeconds) {
        if (cancelled || isFinished()) return;
        float delta = AnimationClock.sanitizeDelta(deltaSeconds);
        if (mode == Mode.SEQUENCE) {
            while (sequenceIndex < children.size()) {
                PlayableAnimation animation = children.get(sequenceIndex);
                animation.update(delta);
                if (!animation.isFinished()) break;
                sequenceIndex++;
            }
        } else if (mode == Mode.PARALLEL) {
            for (int i = 0; i < children.size(); i++) {
                PlayableAnimation animation = children.get(i);
                if (!animation.isFinished()) animation.update(delta);
            }
        } else {
            updateStagger(delta);
        }
    }

    @Override
    public boolean isFinished() {
        if (cancelled) return true;
        if (mode == Mode.SEQUENCE) return sequenceIndex >= children.size();
        if (mode == Mode.STAGGER) {
            for (int i = 0; i < children.size(); i++) {
                if (staggerElapsedSeconds < staggerStartTimes.getFloat(i)
                        || !children.get(i).isFinished()) return false;
            }
            return true;
        }
        for (int i = 0; i < children.size(); i++) {
            if (!children.get(i).isFinished()) return false;
        }
        return true;
    }

    @Override
    public void cancel() {
        if (cancelled) return;
        cancelled = true;
        for (int i = 0; i < children.size(); i++) children.get(i).cancel();
    }

    private void updateStagger(float delta) {
        float previousElapsed = staggerElapsedSeconds;
        staggerElapsedSeconds += delta;
        for (int i = 0; i < children.size(); i++) {
            float start = staggerStartTimes.getFloat(i);
            if (staggerElapsedSeconds < start) break;

            PlayableAnimation animation = children.get(i);
            if (animation.isFinished()) continue;
            float animationDelta = previousElapsed < start
                    ? staggerElapsedSeconds - start
                    : delta;
            if (animationDelta > 0.0f) animation.update(animationDelta);
        }
    }

    /** Простая задержка без аллокаций на кадре. */
    private static final class DelayAnimation implements PlayableAnimation {
        private final float duration;
        private float elapsed;
        private boolean cancelled;

        private DelayAnimation(float seconds) {
            duration = Float.isFinite(seconds) ? Math.max(0.0f, seconds) : 0.0f;
        }

        @Override
        public void update(float deltaSeconds) {
            if (cancelled) return;
            elapsed = Math.min(duration, elapsed + AnimationClock.sanitizeDelta(deltaSeconds));
        }

        @Override
        public boolean isFinished() { return cancelled || elapsed >= duration; }

        @Override
        public void cancel() { cancelled = true; }
    }
}
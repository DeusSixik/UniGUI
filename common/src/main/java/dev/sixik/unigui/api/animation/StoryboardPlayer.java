package dev.sixik.unigui.api.animation;

import dev.sixik.unigui.api.widget.Widget;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.Arrays;

/**
 * Скомпилированный проигрыватель {@link Storyboard}.
 *
 * <p>При создании player разрешает target id и property path, проверяет типы значений и переносит
 * float-треки в primitive-массивы. {@link #update(float)} не разбирает строки, не обходит дерево
 * виджетов и не создаёт временные коллекции.</p>
 */
public final class StoryboardPlayer implements PlayableAnimation {
    private final Storyboard storyboard;
    private final ObjectList<CompiledTrack> tracks = new ObjectArrayList<>();
    private final float durationSeconds;
    private float elapsedSeconds;
    private float playbackRate = 1.0f;
    private boolean playing;
    private boolean finished;
    private boolean cancelled;

    public StoryboardPlayer(Storyboard storyboard,
                            NamedWidgetRegistry widgets,
                            PropertyPathResolver paths) {
        if (storyboard == null) throw new IllegalArgumentException("Storyboard не должен быть null.");
        if (widgets == null) throw new IllegalArgumentException("NamedWidgetRegistry не должен быть null.");
        PropertyPathResolver resolver = paths == null ? PropertyPathResolver.builtIns() : paths;
        this.storyboard = storyboard;
        this.durationSeconds = storyboard.durationSeconds();
        for (PropertyTrack<?> track : storyboard.tracks()) {
            Widget target = widgets.require(track.targetName());
            tracks.add(compile(track, target, resolver));
        }
        applyAt(0.0f);
        finished = durationSeconds <= 0.0f;
        playing = !finished;
    }

    /** Создаёт player для дерева виджетов со стандартными property path. */
    public static StoryboardPlayer bind(Storyboard storyboard, Widget root) {
        return new StoryboardPlayer(storyboard, NamedWidgetRegistry.from(root), PropertyPathResolver.builtIns());
    }

    public Storyboard storyboard() { return storyboard; }

    public float durationSeconds() { return durationSeconds; }

    public float elapsedSeconds() { return elapsedSeconds; }

    public float progress() {
        return durationSeconds <= 0.0f ? 1.0f : Easing.clamp01(elapsedSeconds / durationSeconds);
    }

    public float playbackRate() { return playbackRate; }

    /** Задаёт неотрицательный множитель скорости воспроизведения. */
    public StoryboardPlayer playbackRate(float playbackRate) {
        this.playbackRate = Float.isFinite(playbackRate) ? Math.max(0.0f, playbackRate) : 1.0f;
        return this;
    }

    public boolean playing() { return playing; }

    /** Продолжает приостановленное воспроизведение. Завершённый player нужно перезапустить через {@link #restart()}. */
    public StoryboardPlayer play() {
        if (!cancelled && !finished) playing = true;
        return this;
    }

    public StoryboardPlayer pause() {
        playing = false;
        return this;
    }

    /** Возвращает player в начало и запускает заново. */
    public StoryboardPlayer restart() {
        cancelled = false;
        finished = durationSeconds <= 0.0f;
        elapsedSeconds = 0.0f;
        applyAt(0.0f);
        playing = !finished;
        return this;
    }

    /**
     * Переходит к указанному времени и сразу применяет все треки.
     *
     * <p>Метод пригоден для editor scrubbing и не зависит от текущего состояния play/pause.</p>
     */
    public StoryboardPlayer seek(float seconds) {
        float normalized = Float.isFinite(seconds) ? Math.max(0.0f, seconds) : 0.0f;
        elapsedSeconds = Math.min(durationSeconds, normalized);
        applyAt(elapsedSeconds);
        cancelled = false;
        finished = elapsedSeconds >= durationSeconds;
        if (finished) playing = false;
        return this;
    }

    @Override
    public void update(float deltaSeconds) {
        if (!playing || finished || cancelled) return;
        float delta = AnimationClock.sanitizeDelta(deltaSeconds) * playbackRate;
        if (delta <= 0.0f) return;
        elapsedSeconds = Math.min(durationSeconds, elapsedSeconds + delta);
        applyAt(elapsedSeconds);
        if (elapsedSeconds >= durationSeconds) {
            finished = true;
            playing = false;
        }
    }

    @Override
    public boolean isFinished() { return finished || cancelled; }

    @Override
    public void cancel() {
        cancelled = true;
        playing = false;
    }

    private void applyAt(float seconds) {
        for (int i = 0; i < tracks.size(); i++) tracks.get(i).apply(seconds);
    }

    private static CompiledTrack compile(PropertyTrack<?> track,
                                         Widget target,
                                         PropertyPathResolver resolver) {
        if (track.primitiveFloat()) {
            return compileFloat(track, target, resolver.requireFloat(track.propertyPath()));
        }
        PropertyAccessor<?> accessor = resolver.resolve(track.propertyPath());
        if (accessor == null) {
            throw new IllegalArgumentException("Неизвестный storyboard property path: " + track.propertyPath());
        }
        return compileGeneric(track, target, accessor);
    }

    private static CompiledTrack compileFloat(PropertyTrack<?> track,
                                              Widget target,
                                              FloatPropertyAccessor accessor) {
        int size = track.keyframes().size();
        float[] times = new float[size];
        float[] values = new float[size];
        boolean[] discrete = new boolean[size];
        Easing[] easings = new Easing[size];
        for (int i = 0; i < size; i++) {
            Keyframe<?> keyframe = track.keyframes().get(i);
            if (!(keyframe.value() instanceof Number number)) {
                throw new IllegalArgumentException("Float track содержит нечисловое значение: " + keyframe.value());
            }
            float value = number.floatValue();
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Float track содержит невалидное значение: " + value);
            }
            times[i] = keyframe.timeSeconds();
            values[i] = value;
            discrete[i] = keyframe.discrete();
            easings[i] = easingOf(keyframe);
        }
        return new FloatCompiledTrack(target, accessor, track.floatInterpolator(), times, values, discrete, easings);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CompiledTrack compileGeneric(PropertyTrack<?> track,
                                                Widget target,
                                                PropertyAccessor<?> accessor) {
        int size = track.keyframes().size();
        float[] times = new float[size];
        Object[] values = new Object[size];
        boolean[] discrete = new boolean[size];
        Easing[] easings = new Easing[size];
        for (int i = 0; i < size; i++) {
            Keyframe<?> keyframe = track.keyframes().get(i);
            Object value = keyframe.value();
            if (value != null && !accessor.valueType().isInstance(value)) {
                throw new IllegalArgumentException("Track '" + track.propertyPath() + "' содержит "
                        + value.getClass().getName() + ", ожидался " + accessor.valueType().getName());
            }
            times[i] = keyframe.timeSeconds();
            values[i] = value;
            discrete[i] = keyframe.discrete();
            easings[i] = easingOf(keyframe);
        }
        return new GenericCompiledTrack(target, (PropertyAccessor) accessor, (Interpolator) track.interpolator(),
                times, values, discrete, easings);
    }

    private static Easing easingOf(Keyframe<?> keyframe) {
        return keyframe instanceof SplineKeyframe<?> spline ? spline.easing() : AnimationEasing.LINEAR;
    }

    private interface CompiledTrack {
        void apply(float seconds);
    }

    private abstract static class AbstractCompiledTrack implements CompiledTrack {
        protected final float[] times;
        protected final boolean[] discrete;
        protected final Easing[] easings;

        private AbstractCompiledTrack(float[] times, boolean[] discrete, Easing[] easings) {
            this.times = times;
            this.discrete = discrete;
            this.easings = easings;
        }

        protected final int exactOrInsertion(float seconds) {
            return Arrays.binarySearch(times, seconds);
        }

        protected final float segmentProgress(float seconds, int from, int to) {
            float duration = times[to] - times[from];
            if (duration <= 0.0f) return 1.0f;
            return Easing.clamp01((seconds - times[from]) / duration);
        }
    }

    private static final class FloatCompiledTrack extends AbstractCompiledTrack {
        private final Widget target;
        private final FloatPropertyAccessor accessor;
        private final FloatInterpolator interpolator;
        private final float[] values;

        private FloatCompiledTrack(Widget target,
                                   FloatPropertyAccessor accessor,
                                   FloatInterpolator interpolator,
                                   float[] times,
                                   float[] values,
                                   boolean[] discrete,
                                   Easing[] easings) {
            super(times, discrete, easings);
            this.target = target;
            this.accessor = accessor;
            this.interpolator = interpolator;
            this.values = values;
        }

        @Override
        public void apply(float seconds) {
            int result = exactOrInsertion(seconds);
            if (result >= 0) {
                accessor.setFloat(target, values[result]);
                return;
            }
            int to = -result - 1;
            if (to <= 0) {
                accessor.setFloat(target, values[0]);
                return;
            }
            if (to >= times.length) {
                accessor.setFloat(target, values[values.length - 1]);
                return;
            }
            int from = to - 1;
            if (discrete[to]) {
                accessor.setFloat(target, values[from]);
                return;
            }
            float progress = easings[to].apply(segmentProgress(seconds, from, to));
            accessor.setFloat(target, interpolator.interpolate(values[from], values[to], progress));
        }
    }

    private static final class GenericCompiledTrack extends AbstractCompiledTrack {
        private final Widget target;
        private final PropertyAccessor<Object> accessor;
        private final Interpolator<Object> interpolator;
        private final Object[] values;

        private GenericCompiledTrack(Widget target,
                                     PropertyAccessor<Object> accessor,
                                     Interpolator<Object> interpolator,
                                     float[] times,
                                     Object[] values,
                                     boolean[] discrete,
                                     Easing[] easings) {
            super(times, discrete, easings);
            this.target = target;
            this.accessor = accessor;
            this.interpolator = interpolator;
            this.values = values;
        }

        @Override
        public void apply(float seconds) {
            int result = exactOrInsertion(seconds);
            if (result >= 0) {
                accessor.set(target, values[result]);
                return;
            }
            int to = -result - 1;
            if (to <= 0) {
                accessor.set(target, values[0]);
                return;
            }
            if (to >= times.length) {
                accessor.set(target, values[values.length - 1]);
                return;
            }
            int from = to - 1;
            if (discrete[to]) {
                accessor.set(target, values[from]);
                return;
            }
            float progress = easings[to].apply(segmentProgress(seconds, from, to));
            accessor.set(target, interpolator.interpolate(values[from], values[to], progress));
        }
    }
}

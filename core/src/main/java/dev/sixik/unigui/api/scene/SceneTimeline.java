package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.Easing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.widgets.rendering.Scene3D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Декларативная анимация узлов сцены по ключевым кадрам, которую можно показать в любой момент времени.
 *
 * <p>Кадр считается только из времени: {@link #sample(float)} можно вызывать вперёд, назад и с любым шагом,
 * результат один и тот же. Поэтому одна и та же анимация годится и для проигрывания
 * ({@link #play(Scene3D)}), и для перемотки ползунком.</p>
 *
 * <pre>{@code
 * SceneTimeline timeline = new SceneTimeline();
 * timeline.track(furnace)
 *         .visible(0, false).visible(2, true)
 *         .position(2, 0, 4, 0)
 *         .position(2.8f, 0, 1, 0, AnimationEasing.QUAD_IN)   // падение с ускорением
 *         .state(4, "minecraft:furnace[facing=south,lit=true]");
 * timeline.assemble(house, 0, 3);                            // сборка структуры по слоям
 * timeline.play(viewport);                                   // или timeline.sample(seconds)
 * }</pre>
 *
 * <p>Ключи одного свойства задаются в любом порядке. Между ключами значение интерполируется с функцией
 * плавности следующего ключа. До первого ключа держится значение первого, после последнего — последнего.
 * Дискретные свойства (видимость, состояние блока) переключаются в момент ключа включительно. Свойство без
 * ключей таймлайн не трогает.</p>
 */
public final class SceneTimeline {
    /**
     * Дорожки узлов в порядке создания.
     */
    private final Map<SceneNode, Track> tracks = new IdentityHashMap<>();
    /**
     * Порядок применения дорожек.
     */
    private final List<Track> order = new ArrayList<>();
    /**
     * Пользовательские шаги, вызываемые на каждом кадре.
     */
    private final List<Sampler> samplers = new ArrayList<>();
    /**
     * Длительность, заданная явно, или отрицательное число.
     */
    private float explicitDuration = -1;
    /**
     * Наибольшее время ключа или конца интервала.
     */
    private float lastKey;

    /**
     * Принимает время или прогресс анимации.
     */
    @FunctionalInterface
    public interface Sampler {
        /**
         * @param value время в секундах или прогресс от 0 до 1.
         */
        void sample(float value);
    }

    /**
     * Возвращает дорожку узла, создавая её при первом обращении.
     *
     * @param node анимируемый узел.
     * @return дорожка для ключей этого узла.
     */
    public Track track(SceneNode node) {
        Objects.requireNonNull(node, "node");
        return tracks.computeIfAbsent(node, key -> {
            Track track = new Track(key);
            order.add(track);
            return track;
        });
    }

    /**
     * Анимирует произвольное значение на отрезке времени. Прогресс до начала равен 0, после конца — 1.
     *
     * @param start    начало в секундах.
     * @param end      конец в секундах, не раньше начала.
     * @param easing   функция плавности или null для линейной.
     * @param progress получает прогресс с учётом плавности на каждом кадре.
     * @return этот таймлайн.
     */
    public SceneTimeline during(float start, float end, Easing easing, Sampler progress) {
        requireTime(start);
        requireTime(end);
        if (end < start) throw new IllegalArgumentException("Interval ends before it starts: " + start + " > " + end);
        Objects.requireNonNull(progress, "progress");
        Easing curve = easing == null ? AnimationEasing.LINEAR : easing;
        float length = end - start;
        samplers.add(seconds -> progress.sample(curve.apply(length == 0
                ? (seconds >= end ? 1 : 0) : Easing.clamp01((seconds - start) / length))));
        lastKey = Math.max(lastKey, end);
        return this;
    }

    /**
     * Вызывает шаг на каждом кадре с текущим временем, например для видимости виджетов-подписей.
     *
     * @param sampler получает время в секундах.
     * @return этот таймлайн.
     */
    public SceneTimeline onSample(Sampler sampler) {
        samplers.add(Objects.requireNonNull(sampler, "sampler"));
        return this;
    }

    /**
     * Собирает структуру снизу вверх: верхний видимый слой растёт от «ничего» до последнего непустого слоя.
     *
     * @param structure структура.
     * @param start     начало сборки в секундах.
     * @param end       конец сборки в секундах.
     * @return этот таймлайн.
     */
    public SceneTimeline assemble(StructureNode structure, float start, float end) {
        Objects.requireNonNull(structure, "structure");
        return during(start, end, AnimationEasing.LINEAR, progress -> {
            int[] range = structure.layerRange(Integer.MAX_VALUE);
            if (range == null) structure.setTopVisibleY(Integer.MAX_VALUE);
            else if (progress >= 1) structure.setTopVisibleY(Integer.MAX_VALUE);
            else structure.setTopVisibleY(range[0] - 1 + (int) (progress * (range[1] - range[0] + 1)));
        });
    }

    /**
     * Задаёт длительность явно, например чтобы последний кадр держался дольше последнего ключа.
     *
     * @param seconds длительность в секундах.
     * @return этот таймлайн.
     */
    public SceneTimeline duration(float seconds) {
        requireTime(seconds);
        explicitDuration = seconds;
        return this;
    }

    /**
     * @return явная длительность или время последнего ключа.
     */
    public float duration() {
        return explicitDuration >= 0 ? explicitDuration : lastKey;
    }

    /**
     * Ставит все узлы в положение момента времени.
     *
     * @param seconds время от начала в секундах.
     */
    public void sample(float seconds) {
        float time = Float.isFinite(seconds) ? seconds : 0;
        for (int i = 0; i < order.size(); i++) order.get(i).sample(time);
        for (int i = 0; i < samplers.size(); i++) samplers.get(i).sample(time);
    }

    /**
     * Проигрывает таймлайн один раз от начала во вьюпорте. Повторный вызов начинает заново.
     *
     * @param viewport вьюпорт, тикающий анимацию.
     * @return этот таймлайн.
     */
    public SceneTimeline play(Scene3D viewport) {
        viewport.stopParameterAnimation(this);
        sample(0);
        float length = duration();
        if (length > 0) {
            viewport.animateParameterFrom(this, 0, this::sample, length,
                    TransitionSpec.of(length, AnimationEasing.LINEAR));
        }
        return this;
    }

    /**
     * Останавливает проигрывание, оставляя текущий кадр.
     *
     * @param viewport вьюпорт из {@link #play(Scene3D)}.
     */
    public void stop(Scene3D viewport) {
        viewport.stopParameterAnimation(this);
    }

    /**
     * @param seconds проверяемое время.
     * @throws IllegalArgumentException если время отрицательно или не конечно.
     */
    private static void requireTime(float seconds) {
        if (!Float.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Time must be finite and non-negative: " + seconds);
        }
    }

    /**
     * Ключи одного узла: позиция, поворот, масштаб, видимость и состояние блока.
     */
    public final class Track {
        /**
         * Анимируемый узел.
         */
        private final SceneNode node;
        /**
         * Позиция X, Y, Z.
         */
        private final Channel position = new Channel(3);
        /**
         * Рыскание и тангаж в радианах.
         */
        private final Channel rotation = new Channel(2);
        /**
         * Масштаб X, Y, Z.
         */
        private final Channel scale = new Channel(3);
        /**
         * Видимость: 1 или 0.
         */
        private final Channel visible = new Channel(1);
        /**
         * Поворот головы сущности: рыскание и тангаж.
         */
        private final Channel head = new Channel(2);
        /**
         * Время ключей состояния блока.
         */
        private float[] stateTimes = new float[0];
        /**
         * Состояния блока в порядке {@link #stateTimes}.
         */
        private String[] states = new String[0];
        /**
         * Последнее применённое состояние, чтобы не сбрасывать кэш блока на каждом кадре.
         */
        private String appliedState;
        /**
         * Рабочий кватернион поворота.
         */
        private final MutableQuat quaternion = new MutableQuat();
        /**
         * Рабочий буфер значений.
         */
        private final float[] values = new float[3];

        /**
         * @param node анимируемый узел.
         */
        private Track(SceneNode node) {
            this.node = node;
        }

        /**
         * Ключ позиции с линейным переходом от предыдущего ключа.
         *
         * @param seconds время ключа.
         * @param x       X.
         * @param y       Y.
         * @param z       Z.
         * @return эта дорожка.
         */
        public Track position(float seconds, float x, float y, float z) {
            return position(seconds, x, y, z, null);
        }

        /**
         * Ключ позиции.
         *
         * @param seconds время ключа.
         * @param x       X.
         * @param y       Y.
         * @param z       Z.
         * @param easing  плавность перехода от предыдущего ключа или null для линейной.
         * @return эта дорожка.
         */
        public Track position(float seconds, float x, float y, float z, Easing easing) {
            position.add(seconds, easing, x, y, z);
            return this;
        }

        /**
         * Ключ поворота вокруг вертикали с линейным переходом.
         *
         * @param seconds время ключа.
         * @param yaw     рыскание в радианах.
         * @return эта дорожка.
         */
        public Track yaw(float seconds, float yaw) {
            return rotation(seconds, yaw, 0, null);
        }

        /**
         * Ключ поворота. Углы интерполируются как числа, поэтому оборот больше чем на π задаётся явно.
         *
         * @param seconds время ключа.
         * @param yaw     рыскание в радианах.
         * @param pitch   тангаж в радианах.
         * @param easing  плавность перехода от предыдущего ключа или null для линейной.
         * @return эта дорожка.
         */
        public Track rotation(float seconds, float yaw, float pitch, Easing easing) {
            rotation.add(seconds, easing, yaw, pitch, 0);
            return this;
        }

        /**
         * Ключ равномерного масштаба с линейным переходом.
         *
         * @param seconds время ключа.
         * @param scale   масштаб по всем осям.
         * @return эта дорожка.
         */
        public Track scale(float seconds, float scale) {
            return scale(seconds, scale, scale, scale, null);
        }

        /**
         * Ключ масштаба.
         *
         * @param seconds время ключа.
         * @param x       масштаб X.
         * @param y       масштаб Y.
         * @param z       масштаб Z.
         * @param easing  плавность перехода от предыдущего ключа или null для линейной.
         * @return эта дорожка.
         */
        public Track scale(float seconds, float x, float y, float z, Easing easing) {
            scale.add(seconds, easing, x, y, z);
            return this;
        }

        /**
         * Ключ поворота головы {@link EntityNode} относительно тела.
         *
         * @param seconds время ключа.
         * @param yaw     рыскание в радианах.
         * @param pitch   тангаж в радианах.
         * @param easing  плавность перехода от предыдущего ключа или null для линейной.
         * @return эта дорожка.
         * @throws IllegalArgumentException если узел не {@link EntityNode}.
         */
        public Track head(float seconds, float yaw, float pitch, Easing easing) {
            if (!(node instanceof EntityNode)) throw new IllegalArgumentException("Only an EntityNode has a head");
            head.add(seconds, easing, yaw, pitch, 0);
            return this;
        }

        /**
         * Ключ видимости, который действует с указанного момента включительно.
         *
         * @param seconds время ключа.
         * @param visible видимость.
         * @return эта дорожка.
         */
        public Track visible(float seconds, boolean visible) {
            this.visible.add(seconds, STEP, visible ? 1 : 0, 0, 0);
            return this;
        }

        /**
         * Показывает узел только на отрезке {@code [from, to)}.
         *
         * @param from начало показа.
         * @param to   момент скрытия.
         * @return эта дорожка.
         */
        public Track visibleBetween(float from, float to) {
            return visible(0, from == 0).visible(from, true).visible(to, false);
        }

        /**
         * Ключ состояния блока, который действует с указанного момента включительно.
         *
         * @param seconds время ключа.
         * @param state   строка блока, например {@code minecraft:furnace[lit=true]}.
         * @return эта дорожка.
         * @throws IllegalArgumentException если узел не {@link BlockNode}.
         */
        public Track state(float seconds, String state) {
            if (!(node instanceof BlockNode)) throw new IllegalArgumentException("Only a BlockNode has a block state");
            requireTime(seconds);
            BlockSpec.parse(state);
            int at = insertionPoint(stateTimes, stateTimes.length, seconds);
            stateTimes = insert(stateTimes, at, seconds);
            String[] grown = Arrays.copyOf(states, states.length + 1);
            System.arraycopy(grown, at, grown, at + 1, states.length - at);
            grown[at] = state;
            states = grown;
            lastKey = Math.max(lastKey, seconds);
            return this;
        }

        /**
         * Возвращает таймлайн для следующей дорожки.
         *
         * @param other следующий узел.
         * @return его дорожка.
         */
        public Track track(SceneNode other) {
            return SceneTimeline.this.track(other);
        }

        /**
         * @return таймлайн этой дорожки.
         */
        public SceneTimeline timeline() {
            return SceneTimeline.this;
        }

        /**
         * Применяет кадр к узлу.
         *
         * @param seconds время в секундах.
         */
        private void sample(float seconds) {
            if (position.sample(seconds, values)) node.setPosition(values[0], values[1], values[2]);
            if (rotation.sample(seconds, values)) node.setRotation(quaternion.setYawPitch(values[0], values[1]));
            if (scale.sample(seconds, values)) node.setScale(values[0], values[1], values[2]);
            if (visible.sample(seconds, values)) node.setVisible(values[0] > 0.5f);
            if (head.sample(seconds, values)) ((EntityNode) node).setHead(values[0], values[1]);
            if (states.length != 0) {
                int index = Math.max(0, insertionPoint(stateTimes, stateTimes.length, seconds) - 1);
                String state = states[index];
                if (!state.equals(appliedState)) {
                    appliedState = state;
                    ((BlockNode) node).setState(state);
                }
            }
        }
    }

    /**
     * Плавность дискретного ключа: значение не меняется до самого ключа.
     */
    private static final Easing STEP = progress -> progress >= 1 ? 1 : 0;

    /**
     * Ключи одного свойства с одним, двумя или тремя числами, отсортированные по времени.
     */
    private final class Channel {
        /**
         * Чисел в одном ключе.
         */
        private final int width;
        /**
         * Время ключей.
         */
        private float[] times = new float[0];
        /**
         * Значения ключей подряд по {@link #width}.
         */
        private float[] values = new float[0];
        /**
         * Плавность перехода к каждому ключу.
         */
        private Easing[] easings = new Easing[0];

        /**
         * @param width чисел в ключе.
         */
        private Channel(int width) {
            this.width = width;
        }

        /**
         * Вставляет ключ после ключей с тем же временем.
         */
        private void add(float seconds, Easing easing, float a, float b, float c) {
            requireTime(seconds);
            if (!Float.isFinite(a) || !Float.isFinite(b) || !Float.isFinite(c)) {
                throw new IllegalArgumentException("Key values must be finite");
            }
            int count = times.length;
            int at = insertionPoint(times, count, seconds);
            times = insert(times, at, seconds);
            float[] grown = new float[(count + 1) * width];
            System.arraycopy(values, 0, grown, 0, at * width);
            System.arraycopy(values, at * width, grown, (at + 1) * width, (count - at) * width);
            float[] key = {a, b, c};
            System.arraycopy(key, 0, grown, at * width, width);
            values = grown;
            Easing[] curves = Arrays.copyOf(easings, count + 1);
            System.arraycopy(curves, at, curves, at + 1, count - at);
            curves[at] = easing == null ? AnimationEasing.LINEAR : easing;
            easings = curves;
            lastKey = Math.max(lastKey, seconds);
        }

        /**
         * Интерполирует {@code v = a + (b - a) * easing((t - ta) / (tb - ta))} между соседними ключами.
         *
         * @param seconds время.
         * @param out     буфер для значений.
         * @return false, если у свойства нет ключей.
         */
        private boolean sample(float seconds, float[] out) {
            int count = times.length;
            if (count == 0) return false;
            int next = insertionPoint(times, count, seconds);
            if (next == 0 || next == count) {
                int key = next == 0 ? 0 : count - 1;
                System.arraycopy(values, key * width, out, 0, width);
                return true;
            }
            int previous = next - 1;
            float span = times[next] - times[previous];
            float progress = easings[next].apply(span <= 0 ? 1 : (seconds - times[previous]) / span);
            for (int i = 0; i < width; i++) {
                float from = values[previous * width + i];
                out[i] = from + (values[next * width + i] - from) * progress;
            }
            return true;
        }
    }

    /**
     * Находит первый ключ строго позже момента времени.
     *
     * @param times отсортированное время ключей.
     * @param count число ключей.
     * @param value время.
     * @return индекс от 0 до count.
     */
    private static int insertionPoint(float[] times, int count, float value) {
        int low = 0, high = count;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (times[middle] <= value) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    /**
     * @param array массив. @param at позиция. @param value новое значение. @return новый массив.
     */
    private static float[] insert(float[] array, int at, float value) {
        float[] grown = new float[array.length + 1];
        System.arraycopy(array, 0, grown, 0, at);
        grown[at] = value;
        System.arraycopy(array, at, grown, at + 1, array.length - at);
        return grown;
    }
}

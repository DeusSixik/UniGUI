package dev.sixik.unigui.api.animation;

/** Общий контракт управляемой анимации. */
public interface PlayableAnimation {
    /** Продвигает анимацию на один шаг. */
    void update(float deltaSeconds);
    /** @return завершена ли анимация или отменена */
    boolean isFinished();
    /** Отменяет анимацию. */
    void cancel();
}

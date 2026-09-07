package dev.sixik.unigui.backend.minecraft_impl;

/**
 * Внутренний мост, который позволяет screen-overlay отложить ванильные tooltip до конца кадра.
 */
public interface DeferredMinecraftTooltips {
    /** Начинает накапливать vanilla tooltip вместо немедленного рендера. */
    void unigui$beginTooltipDeferral();

    /** Завершает накопление, не отрисовывая сохранённые tooltip. */
    void unigui$endTooltipDeferral();

    /** Отрисовывает накопленные vanilla tooltip в исходном порядке. */
    void unigui$flushDeferredTooltips();
}

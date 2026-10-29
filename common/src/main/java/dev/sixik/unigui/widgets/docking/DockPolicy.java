package dev.sixik.unigui.widgets.docking;

/**
 * Политика участия окна в докинге.
 *
 * <p>Докинг — свойство окна по умолчанию: любое открытое немодальное окно можно стыковать с другими окнами
 * и с {@link DockingRoot}, а само оно принимает дропы других окон (становясь окном-хостом на лету).
 * Политика нужна только чтобы это отключить там, где стыковка неуместна — для диалогов, тултипов, HUD.</p>
 */
public enum DockPolicy {
    /** Окно можно стыковать куда угодно и на него можно стыковать другие окна. Значение по умолчанию. */
    FULL(true, true),
    /** Окно можно перетащить в док или на другое окно, но дропы других окон оно не принимает. */
    GUEST_ONLY(true, false),
    /** Окно никогда не участвует в докинге. */
    DISABLED(false, false);

    private final boolean canBeDocked;
    private final boolean acceptsDrops;

    DockPolicy(boolean canBeDocked, boolean acceptsDrops) {
        this.canBeDocked = canBeDocked;
        this.acceptsDrops = acceptsDrops;
    }

    /**
     * @return {@code true}, если окно можно перетащить в док или на другое окно
     */
    public boolean canBeDocked() {
        return canBeDocked;
    }

    /**
     * @return {@code true}, если окно принимает дропы других окон и панелей
     */
    public boolean acceptsDrops() {
        return acceptsDrops;
    }
}

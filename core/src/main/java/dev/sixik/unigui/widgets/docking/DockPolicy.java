package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Политика участия окна в докинге.
 *
 * <p>Докинг — свойство окна по умолчанию: любое открытое немодальное окно можно стыковать с другими окнами
 * и с {@link DockingRoot}, а само оно принимает дропы других окон (становясь окном-хостом на лету).
 * Политика нужна только чтобы это отключить там, где стыковка неуместна — для диалогов, тултипов, HUD.</p>
 */
@AiDoc(
        description = "Docking integration policy for windows: FULL (bidirectional docking and hosting), GUEST_ONLY (can dock into hosts but accepts no drops), or DISABLED."
)
@WikiPage(
        title = "DockPolicy",
        slug = "dock-policy",
        category = "<key:docking.category>",
        summary = "<key:dock.policy.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:dock.policy.section.overview.body>",
        order = 10
)
@WikiLink(
        label = "DockingRoot",
        target = "page:docking-root",
        description = "<key:docking.root.summary>"
)
public enum DockPolicy {
    /** Окно можно стыковать куда угодно и на него можно стыковать другие окна. Значение по умолчанию. */
    @AiDoc(description = "Window can be docked anywhere and can accept drops of other windows.")
    FULL(true, true),
    /** Окно можно перетащить в док или на другое окно, но дропы других окон оно не принимает. */
    @AiDoc(description = "Window can be docked into other hosts, but cannot host other docked windows.")
    GUEST_ONLY(true, false),
    /** Окно никогда не участвует в докинге. */
    @AiDoc(description = "Window does not participate in the docking system.")
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
    @AiDoc(description = "Returns true if the window can be dragged and docked into a host.")
    @WikiMethod(description = "<key:dock.policy.method.canBeDocked>")
    @WikiReturn("<key:dock.policy.return.canBeDocked>")
    public boolean canBeDocked() {
        return canBeDocked;
    }

    /**
     * @return {@code true}, если окно принимает дропы других окон и панелей
     */
    @AiDoc(description = "Returns true if the window can accept dropped panels or foreign windows.")
    @WikiMethod(description = "<key:dock.policy.method.acceptsDrops>")
    @WikiReturn("<key:dock.policy.return.acceptsDrops>")
    public boolean acceptsDrops() {
        return acceptsDrops;
    }
}

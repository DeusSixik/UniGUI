package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.widgets.docking.DockArea;
import dev.sixik.unigui.widgets.docking.DockingManager;
import dev.sixik.unigui.widgets.docking.DockingRoot;

/**
 * Сторона, к которой {@link DockPanel} приклеивает дочерний виджет.
 *
 * <p>{@code DockSide} описывает только четыре edge-слота простого
 * layout-контейнера. Это не enum зон drop'а из IDE-style docking-системы:
 * для {@link DockingRoot}/{@link DockingManager} используется {@link DockArea}.</p>
 */
@AiDoc(
        summary = "Specifies which outer edge (LEFT, RIGHT, TOP, BOTTOM) a child widget docks against inside a DockPanel container.",
        purpose = "Used exclusively with DockPanel to attach child widgets to boundaries, shrinking the remaining central layout area with each docked element.",
        useWhen = {
                "Arranging peripheral elements like headers, toolbars, sidebars, or footers in a DockPanel."
        },
        criticalRules = {
                "DockSide is only used by DockPanel; do not confuse with DockArea, which is used for full docking systems (DockingRoot/DockingManager).",
                "The order in which widgets are docked matters: earlier children take precedence along their respective edges."
        },
        related = {"DockPanel"})
@WikiPage(
        title = "<key:dock.side.title>",
        slug = "dock-side",
        category = "<key:dock.side.category>",
        summary = "<key:dock.side.summary>",
        order = 125)
@WikiSection(
        title = "<key:dock.side.section.overview.title>",
        markdown = "<key:dock.side.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:dock.side.example.basic.title>",
        code = "<key:dock.side.example.basic.code>",
        order = 10)
@WikiLink(label = "DockPanel", target = "page:dock-panel")
public enum DockSide {
    /** Прижать виджет к левой стороне оставшейся области. */
    LEFT,
    /** Прижать виджет к правой стороне оставшейся области. */
    RIGHT,
    /** Прижать виджет к верхней стороне оставшейся области. */
    TOP,
    /** Прижать виджет к нижней стороне оставшейся области. */
    BOTTOM
}

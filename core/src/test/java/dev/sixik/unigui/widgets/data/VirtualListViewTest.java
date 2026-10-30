package dev.sixik.unigui.widgets.data;

import dev.sixik.unigui.api.style.StyleKeyRegistry;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.StylePack;
import dev.sixik.unigui.api.style.VirtualListViewMode;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.widgets.display.Label;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualListViewTest {
    @Test
    void gridMode_largeItemSetRealizesVisibleGridItems() {
        AtomicInteger factoryCalls = new AtomicInteger();
        VirtualListView list = new VirtualListView()
                .viewMode(VirtualListView.ViewMode.GRID)
                .gridCellWidth(40.0f)
                .gridCellHeight(30.0f)
                .overscan(0)
                .itemCount(1_000)
                .itemFactory(index -> {
                    factoryCalls.incrementAndGet();
                    return new Label("item " + index);
                });
        list.layout(style -> style.columnGap(4.0f).rowGap(2.0f));

        list.arrange(new MutableRect(0.0f, 0.0f, 204.0f, 64.0f));

        assertEquals(4, list.gridColumns());
        assertEquals(1_000, list.itemCount());
        assertEquals(12, list.realizedCount());
        assertEquals(12, factoryCalls.get());
        assertEquals(0, list.realizedRange().firstIndex());
        assertEquals(12, list.realizedRange().lastIndexExclusive());
        assertEquals(7_998.0f, list.contentHeight(), 0.001f);
        assertTrue(list.children().size() <= 13, "Only visible cells and the scrollbar should be children");
    }

    @Test
    void switchingFromGridToList_keepsLogicalItemCountAndUsesListIndices() {
        VirtualListView list = new VirtualListView()
                .viewMode(VirtualListView.ViewMode.GRID)
                .gridColumns(3)
                .gridCellHeight(24.0f)
                .itemCount(30)
                .overscan(0)
                .itemFactory(index -> new Label("item " + index));
        list.arrange(new MutableRect(0.0f, 0.0f, 180.0f, 48.0f));

        assertEquals(30, list.itemCount());
        assertEquals(9, list.realizedCount());

        list.viewMode(VirtualListView.ViewMode.LIST).itemHeight(16.0f);
        list.arrange(new MutableRect(0.0f, 0.0f, 180.0f, 48.0f));

        assertEquals(30, list.itemCount());
        assertEquals(4, list.realizedCount());
        assertEquals(0, list.firstVisibleIndex());
        assertEquals(4, list.lastVisibleIndexExclusive());
        assertEquals(480.0f, list.contentHeight(), 0.001f);
    }

    @Test
    void stylePack_virtualListPropertiesConfigureLayoutAndBehavior() {
        StylePack pack = StylePack.from("""
                <StylePack id="virtual-list-test">
                  <Style id="VirtualGrid" target="VirtualListView">
                    <Setter property="virtual-list.view-mode" value="grid" />
                    <Setter property="virtual-list.item-height" value="21" />
                    <Setter property="virtual-list.grid.cell-width" value="42" />
                    <Setter property="virtual-list.grid.cell-height" value="30" />
                    <Setter property="virtual-list.grid.columns" value="3" />
                    <Setter property="virtual-list.row-gap" value="2" />
                    <Setter property="virtual-list.column-gap" value="4" />
                    <Setter property="virtual-list.padding" value="5 8 7 10" />
                    <Setter property="virtual-list.overscan" value="0" />
                    <Setter property="virtual-list.offscreen-cache-size" value="12" />
                    <Setter property="virtual-list.scroll-step" value="24" />
                    <Setter property="virtual-list.consume-wheel-at-bounds" value="false" />
                  </Style>
                </StylePack>
                """, StyleKeyRegistry.builtIns());
        VirtualListView list = new VirtualListView()
                .itemCount(30)
                .itemFactory(index -> new Label("item " + index));
        list.setUiContextInternal(new DefaultUIContext().theme(pack));

        list.arrange(new MutableRect(0.0f, 0.0f, 210.0f, 76.0f));

        assertEquals(VirtualListView.ViewMode.GRID, list.viewMode());
        assertEquals(42.0f, list.gridCellWidth(), 0.001f);
        assertEquals(30.0f, list.gridCellHeight(), 0.001f);
        assertEquals(3, list.gridColumns());
        assertEquals(0, list.overscan());
        assertEquals(12, list.offscreenCacheSize());
        assertEquals(24.0f, list.scrollStep(), 0.001f);
        assertFalse(list.consumeWheelAtScrollBounds());
        assertEquals(10.0f, list.children().get(0).layoutBounds().x(), 0.001f);
        assertEquals(5.0f, list.children().get(0).layoutBounds().y(), 0.001f);
        assertTrue(StyleKeyRegistry.builtIns().descriptor(StyleKeys.VIRTUAL_LIST_VIEW_MODE).isPresent());
        assertEquals(VirtualListViewMode.GRID, pack.styleFor(VirtualListView.STYLE_TYPE)
                .get(StyleKeys.VIRTUAL_LIST_VIEW_MODE, null, VirtualListViewMode.LIST));
    }

    @Test
    void fluentConfiguration_stylePackPresent_overridesThemeForOneInstance() {
        StylePack pack = StylePack.create("grid-theme")
                .putStyle(VirtualListView.STYLE_TYPE, new dev.sixik.unigui.api.style.MutableStyle()
                        .put(StyleKeys.VIRTUAL_LIST_VIEW_MODE, VirtualListViewMode.GRID)
                        .put(StyleKeys.VIRTUAL_LIST_GRID_COLUMNS, 5));
        VirtualListView list = new VirtualListView();
        list.setUiContextInternal(new DefaultUIContext().theme(pack));

        list.viewMode(VirtualListView.ViewMode.LIST).gridColumns(2);

        assertEquals(VirtualListView.ViewMode.LIST, list.viewMode());
        assertEquals(2, list.configuredGridColumns());
    }
}

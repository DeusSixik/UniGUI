package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;

/**
 * Регрессии layout-инвалидации: скролл и arrange не должны требовать layout,
 * повторный measure на чистом поддереве пропускается.
 *
 * <p>Покрывает шаги 1–3 из docs/LAYOUT_PERF_CHECKLIST.md.</p>
 */
public final class LayoutInvalidationSelfTest {
    public static void main(String[] args) {
        LayoutInvalidationSelfTest test = new LayoutInvalidationSelfTest();
        test.arrangeDoesNotRequireLayout();
        test.scrollDoesNotRequireLayout();
        test.measureCacheSkipsCleanSubtree();
        test.measureCacheMissesOnNewContextOrDirtySubtree();
        test.arrangeInvalidatesVisualForRenderCache();
        System.out.println("LayoutInvalidationSelfTest passed");
    }

    private void arrangeDoesNotRequireLayout() {
        VBox root = new VBox();
        Label child = new Label("child");
        child.layout(style -> style.size(60.0f, 18.0f).flexGrow(0).flexShrink(0.0f));
        root.addChild(child);
        root.applyQueuedMutations();
        root.measure(new LayoutContext(200.0f, 100.0f));
        root.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        clearDirect(root);

        // Повторный arrange с теми же bounds не должен ставить LAYOUT:
        // смена bounds — результат layout, а не повод для нового.
        root.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        expect(!hasLayout(root), "re-arrange with identical bounds must not set LAYOUT");

        root.arrange(new MutableRect(10.0f, 5.0f, 200.0f, 100.0f));
        expect(!hasLayout(root) && !hasLayout(child),
                "arrange with moved bounds must not set LAYOUT on the subtree");
    }

    private void arrangeInvalidatesVisualForRenderCache() {
        Label child = new Label("child");
        child.layout(style -> style.size(60.0f, 18.0f).flexGrow(0).flexShrink(0.0f));
        child.measure(new LayoutContext(200.0f, 100.0f));
        child.arrange(new MutableRect(0.0f, 0.0f, 60.0f, 18.0f));
        child.renderCached(new NoopRenderContext());
        long rebuildsBefore = child.renderCacheRebuilds();
        clearDirect(child);

        // Сдвиг bounds обязан взвести VISUAL (через него renderCacheDirty),
        // иначе сдвинувшийся виджет отдаст протухший retained-фрагмент.
        child.arrange(new MutableRect(10.0f, 5.0f, 60.0f, 18.0f));
        expect(InvalidationFlags.has(child.invalidationFlags(), InvalidationFlags.VISUAL),
                "arrange with moved bounds must set VISUAL for repaint");
        child.renderCached(new NoopRenderContext());
        expect(child.renderCacheRebuilds() == rebuildsBefore + 1,
                "render cache must rebuild after bounds move");
    }

    private void scrollDoesNotRequireLayout() {
        VBox content = new VBox();
        for (int i = 0; i < 20; i++) {
            Label row = new Label("row " + i);
            row.layout(style -> style.size(120.0f, 18.0f).flexGrow(0).flexShrink(0.0f));
            content.addChild(row);
        }
        content.applyQueuedMutations();
        ScrollView scroll = new ScrollView();
        scroll.content(content);
        scroll.measure(new LayoutContext(200.0f, 100.0f));
        scroll.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        clearDirect(scroll);

        scroll.scrollBy(0.0f, 24.0f);
        expect(!hasLayout(scroll) && !hasLayout(content),
                "scrolling must not set LAYOUT on the scroll subtree");
        expect(InvalidationFlags.has(scroll.invalidationFlags(), InvalidationFlags.VISUAL),
                "scrolling must still invalidate VISUAL for repaint");
        expect(!content.layoutBounds().equals(new MutableRect(0.0f, 0.0f, 0.0f, 0.0f)),
                "scrolling must move content bounds");
    }

    private void measureCacheSkipsCleanSubtree() {
        CountingWidget child = new CountingWidget(40.0f, 12.0f);
        VBox root = new VBox();
        root.addChild(child);
        root.applyQueuedMutations();
        LayoutContext context = new LayoutContext(200.0f, 100.0f);
        root.measure(context);
        root.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        clearDirect(root);
        child.measures = 0;
        child.resetMeasureCacheStats();

        // Второй measure с тем же контекстом на чистом поддереве —
        // arrange-путь flex не должен перемерять ребёнка.
        root.measure(context);
        root.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        expect(child.measures == 0,
                "clean subtree with identical context must skip child measure, got " + child.measures);
        expect(child.measureCacheHits() >= 1,
                "measure cache should record hits on repeated flex arrange");
    }

    private void measureCacheMissesOnNewContextOrDirtySubtree() {
        CountingWidget child = new CountingWidget(40.0f, 12.0f);
        VBox root = new VBox();
        root.addChild(child);
        root.applyQueuedMutations();
        root.measure(new LayoutContext(200.0f, 100.0f));
        root.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        clearDirect(root);
        child.measures = 0;

        // Новый контекст — wrap/percent-зависимости требуют пересчёта.
        root.measure(new LayoutContext(120.0f, 100.0f));
        expect(child.measures >= 1,
                "changed available size must re-measure children, got " + child.measures);

        clearDirect(root);
        child.measures = 0;
        child.invalidate(InvalidationFlags.LAYOUT);
        root.measure(new LayoutContext(120.0f, 100.0f));
        expect(child.measures >= 1,
                "dirty subtree must re-measure children, got " + child.measures);
    }

    private static boolean hasLayout(WidgetBase widget) {
        return InvalidationFlags.has(widget.invalidationFlags(), InvalidationFlags.LAYOUT)
                || InvalidationFlags.has(widget.subtreeInvalidationFlags(), InvalidationFlags.LAYOUT);
    }

    private static void clearDirect(WidgetBase widget) {
        widget.clearInvalidationDirect(InvalidationFlags.ALL);
        for (dev.sixik.unigui.api.widget.Widget child : widget.children()) {
            if (child instanceof WidgetBase base) {
                clearDirect(base);
            }
        }
    }

    private static final class CountingWidget extends WidgetBase {
        private final float preferredWidth;
        private final float preferredHeight;
        private int measures;

        private CountingWidget(float preferredWidth, float preferredHeight) {
            this.preferredWidth = preferredWidth;
            this.preferredHeight = preferredHeight;
        }

        @Override
        public void measure(LayoutContext context) {
            measures++;
            if (visibility() == Visibility.COLLAPSED) {
                setDesiredSize(LayoutSize.ZERO);
                return;
            }
            setDesiredSize(preferredWidth, preferredHeight);
        }
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

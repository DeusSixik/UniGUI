# Node Graph Design

## Status

Phase 1, Phase 2, and Phase 3 are implemented for the core canvas/item/connection layer:
NodeGraph, NodeGraphItem, viewport pan, item selection/dragging, ports, connections,
connection validation policy, connection drag, renderer snapshots, typed events,
zoom-at-cursor, lasso selection, item resize handles, keyboard editing,
factory/skin registration, and basic self-tests.

This document remains implementation-oriented: it defines the public API shape, interaction model, renderer contract, event contract, testing requirements, and rollout phases for the remaining NodeGraph work.

This design must follow WIDGETS_CONTRACT.md.

## Why now

Current GraphView is a lightweight visual graph widget. It renders normalized points/edges and supports node click events. It is useful for analytics and preview graphs, but it is not a node editor.

The new NodeGraph should be a full interactive canvas:

- place any UniGUI Widget at world-space coordinates;
- optionally give placed widgets ports and connections;
- support pan, zoom, selection, dragging, lasso, and connection editing;
- keep child widgets fully interactive;
- expose all user actions as UniGUI events;
- keep rendering themeable through renderer snapshots.

## Non-goals

- Do not replace GraphView; keep it as the simple display-only graph.
- Do not introduce a separate immediate-mode UI layer.
- Do not force every item to be a node; arbitrary widgets must be placeable without ports.
- Do not store user action callbacks such as Consumer<Node> in public widget API.
- Do not make the graph renderer directly inspect widget internals.
- Do not implement graph algorithms/layout engines in the first pass.

## Naming

Recommended classes:

- NodeGraph - main widget/container/canvas.
- NodeGraphItem - a world-space placed widget. It may be a node, comment, preview, button group, mini-inspector, etc.
- NodeGraphPort - optional input/output/neutral port attached to an item.
- NodeGraphConnection - edge between two ports.
- NodeGraphViewport - pan/zoom state.
- NodeGraphRenderer - graph-level renderer.
- NodeGraphState, NodeGraphItemState, NodeGraphPortState, NodeGraphConnectionState - immutable render snapshots.

The word node remains in the widget name because users expect Node Graph, but internally the core abstraction should be item so arbitrary widgets fit naturally.

## Core model

### Coordinate spaces

NodeGraph uses three coordinate spaces:

- Root/screen coordinates: incoming pointer events and rendering coordinates.
- Viewport coordinates: graph bounds after widget layout.
- World coordinates: stable graph-space positions independent of pan/zoom.

Conversion helpers:

    float worldX(float rootX);
    float worldY(float rootY);
    float rootX(float worldX);
    float rootY(float worldY);
    MutableRect worldToRoot(RectView worldRect);
    MutableRect rootToWorld(RectView rootRect);

Viewport state:

    public record NodeGraphViewport(
            float panX,
            float panY,
            float zoom,
            float minZoom,
            float maxZoom
    ) {}

Recommended transform:

    rootX = graphBounds.x + (worldX + panX) * zoom
    rootY = graphBounds.y + (worldY + panY) * zoom

panX/panY are world-space offsets. This keeps zoom math predictable.

### Items

An item is any widget placed on the graph canvas.

    public final class NodeGraphItem {
        String id;
        Widget content;
        float x;
        float y;
        float width;
        float height;
        int zIndex;
        boolean selected;
        boolean locked;
        boolean visible;
        boolean draggable;
        boolean resizable;
        boolean portsVisible;
        NodeGraphItemChrome chrome;
        List<NodeGraphPort> ports;
    }

Important rule: content is a normal UniGUI child widget. It must receive measure, arrange, render, tick, focus, and input like any other child.

NodeGraph should support:

    NodeGraph addItem(String id, Widget content, float x, float y);
    NodeGraph addItem(NodeGraphItem item);
    NodeGraph removeItem(String id);
    NodeGraph clearItems();

    NodeGraph moveItem(String id, float x, float y);
    NodeGraph resizeItem(String id, float width, float height);
    NodeGraph selectItem(String id);
    NodeGraph toggleItemSelection(String id);
    NodeGraph clearSelection();

All mutation methods are model/edit helpers. User-triggered actions still emit typed events.

### Ports

Ports are optional. A placed widget can have no ports and still be valid.

    public record NodeGraphPort(
            String id,
            String itemId,
            NodeGraphPortKind kind,
            NodeGraphPortSide side,
            float offset,
            String type,
            boolean enabled,
            boolean visible
    ) {}

Suggested enums:

    public enum NodeGraphPortKind {
        INPUT,
        OUTPUT,
        BIDIRECTIONAL
    }

    public enum NodeGraphPortSide {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

Port position is derived from item bounds + side + offset. Offset can be normalized 0..1 along the side in phase 1. Absolute offsets can be added later if needed.

### Connections

    public record NodeGraphConnection(
            String id,
            String fromItemId,
            String fromPortId,
            String toItemId,
            String toPortId,
            boolean selected,
            boolean enabled,
            String type
    ) {}

Validation should be pluggable without direct UI callbacks for actions.

Allowed safe API:

    NodeGraph connectionPolicy(NodeGraphConnectionPolicy policy);
    NodeGraphConnectionPolicy connectionPolicy();

    @FunctionalInterface
    public interface NodeGraphConnectionPolicy {
        NodeGraphConnectionValidation validate(NodeGraph graph, NodeGraphPortRef from, NodeGraphPortRef to);
    }

This policy is configuration/model validation, not a UI action callback. Actual connection creation/completion is still reported through events.

## Widget/container behavior

NodeGraph should extend WidgetBase directly or a container base with explicit child handling. It should not use normal linear/layout containers for item placement.

### Children

children() must return visible item content widgets in z-order, plus any overlay editor widgets if introduced later.

The graph-level connection lines/grid/background are not children. They are rendered by NodeGraphRenderer.

### Measure

Default desired size should be reasonable for demos:

    default desired size: 320x200

If layout(style -> style.size(...)) is set, respect the normal layout contract.

Child content measure:

- If item has explicit width/height, measure child with that available size.
- If item size is auto, ask child for desired size and cache it into arranged item bounds for this pass.
- Avoid forcing all node content into a single fixed node size.

### Arrange

For every item:

1. Convert item world rect to root rect using viewport.
2. Arrange content inside that rect, minus optional chrome padding.
3. If zoomed, there are two possible phases:
   - Phase 1: do not scale child widgets; zoom affects positions and connection rendering only.
   - Phase 2: add render/input transform support for scaled child widgets.

Recommendation: Phase 1 should implement pan first. Add full zoom only after transform-safe child input is proven. If zoom is implemented immediately, test pointer mapping deeply.

### Render order

Recommended order:

1. background/grid
2. connections behind items
3. item chrome backgrounds / selection rectangles
4. child widgets
5. ports
6. active connection preview
7. lasso/selection overlay
8. tooltip/debug overlay if any

This lets arbitrary widget content stay interactive and visually readable while still allowing ports/connections to appear around it.

### Clipping

NodeGraph should clip graph contents to its layout bounds by default.

API:

    boolean clipsContent();
    NodeGraph clipsContent(boolean clipsContent);

Default: true.

When clipped:

- grid, connections, item chrome, child render, port render, and drag previews must be inside pushClip/popClip.
- external popups owned by child widgets should still be routed through OverlayLayer/portal mechanisms, not clipped by graph if they are true overlays.

## Input model

### General priority

Pointer/key handling priority:

1. active capture operations: item drag, resize, pan, connection drag, lasso
2. child widget input if pointer is inside item content
3. ports and connection handles
4. item chrome/title/body drag zones
5. canvas pan/lasso/selection
6. background actions

### Arbitrary widget content

Because any widget can be inside an item, content input must not be broken.

Rules:

- If pointer is over interactive child content, route normally to the child.
- Text labels and non-input content should not block item dragging if the drag mode says body-drag is allowed.
- A disabled or non-input child should not prevent item move.
- Use existing hit-test / event route semantics where possible instead of inventing a separate input tree.
- When a graph operation starts, capture pointer through UIContext.capturePointer.

Potential helper:

    private boolean shouldStartItemDrag(PointerPressedEvent event, NodeGraphItem item) {
        if (!item.draggable() || item.locked()) return false;
        if (isPortHit(event)) return false;
        if (isResizeHandleHit(event)) return false;
        if (dragMode == NodeGraphDragMode.HEADER_ONLY) return isHeaderHit(event, item);
        if (dragMode == NodeGraphDragMode.BODY_WHEN_NOT_INPUT) return !isInputWidgetHit(event);
        return true;
    }

### Drag modes

    public enum NodeGraphItemDragMode {
        HEADER_ONLY,
        BODY_WHEN_NOT_INPUT,
        ANYWHERE_ON_ITEM,
        DISABLED
    }

Default: BODY_WHEN_NOT_INPUT.

This matches the recent window dragging direction: text/non-input should not count as blocking widget content.

### Pan

Recommended controls:

- Middle mouse drag pans.
- Alt + primary drag pans.
- Optional right mouse drag pans if enabled.

API:

    NodeGraph panEnabled(boolean enabled);
    NodeGraph panButton(NodeGraphPanButton mode);

### Zoom

Recommended controls:

- Ctrl + wheel zooms at cursor.
- Wheel without Ctrl should scroll/pan only if configured.
- Clamp to minZoom/maxZoom.

API:

    NodeGraph zoomEnabled(boolean enabled);
    NodeGraph zoom(float zoom);
    NodeGraph zoomRange(float minZoom, float maxZoom);

### Wheel behavior

Default should avoid nested scroll surprises:

- If mouse is over a child scrollable widget, child owns wheel first.
- If graph handles wheel for zoom/pan, it should consume the wheel while hovered.
- Provide opt-out similar to consumeWheelAtScrollBounds(false).

API:

    NodeGraph consumeWheelWhileHovered(boolean consume);
    boolean consumeWheelWhileHovered();

Default: true.

### Selection

Supported selection modes:

    public enum NodeGraphSelectionMode {
        SINGLE,
        MULTIPLE
    }

Selection controls:

- primary click on item selects it;
- Ctrl click toggles selection;
- Shift can extend only if an ordered selection model exists; otherwise reserve for future;
- background click clears selection;
- drag on background starts lasso if enabled.

API:

    NodeGraph selectionMode(NodeGraphSelectionMode mode);
    List<String> selectedItemIds();
    List<String> selectedConnectionIds();

### Connections

Connection flow:

1. Press/drag from output/bidirectional port.
2. Emit NodeGraphConnectionDragStartedEvent.
3. While dragging, update preview target and emit NodeGraphConnectionDragMovedEvent when target changes or cursor moves enough.
4. On release:
   - if valid target: create connection, emit NodeGraphConnectionCreatedEvent;
   - if invalid/missing: emit NodeGraphConnectionDragEndedEvent with failed result.
5. All events are routed and cancellable where appropriate.

Do not call user code directly from drag handlers.

## Events

All user-facing actions must use typed events under common/src/main/java/dev/sixik/unigui/api/event/.

Every routed event must:

- extend BaseEvent;
- implement RoutableWidgetEvent;
- store target, currentTarget, phase;
- expose public static final EventType<XxxEvent> TYPE;
- copy cancellation state in routeTo(...).

Recommended event set:

### Item events

- NodeGraphItemClickedEvent
- NodeGraphItemMovedEvent
- NodeGraphItemMoveStartedEvent
- NodeGraphItemMoveEndedEvent
- NodeGraphItemResizedEvent
- NodeGraphItemSelectionChangedEvent

Event payload should include relevant fields:

    String itemId;
    float oldX;
    float oldY;
    float newX;
    float newY;
    float oldWidth;
    float oldHeight;
    float newWidth;
    float newHeight;
    List<String> oldSelection;
    List<String> newSelection;

### Viewport events

- NodeGraphViewportChangedEvent
- NodeGraphPanStartedEvent
- NodeGraphPanEndedEvent

Payload:

    float oldPanX;
    float oldPanY;
    float newPanX;
    float newPanY;
    float oldZoom;
    float newZoom;

### Connection events

- NodeGraphConnectionDragStartedEvent
- NodeGraphConnectionDragMovedEvent
- NodeGraphConnectionDragEndedEvent
- NodeGraphConnectionCreatedEvent
- NodeGraphConnectionRemovedEvent
- NodeGraphConnectionSelectionChangedEvent

Payload:

    String connectionId;
    NodeGraphPortRef from;
    NodeGraphPortRef to;
    NodeGraphConnectionValidation validation;
    float rootX;
    float rootY;
    float worldX;
    float worldY;

### Subscription API

    public EventSubscription onItemMoved(EventListener<? super NodeGraphItemMovedEvent> listener);
    public EventSubscription onItemSelectionChanged(EventListener<? super NodeGraphItemSelectionChangedEvent> listener);
    public EventSubscription onViewportChanged(EventListener<? super NodeGraphViewportChangedEvent> listener);
    public EventSubscription onConnectionCreated(EventListener<? super NodeGraphConnectionCreatedEvent> listener);

No onXxx(Runnable), no onXxx(Consumer<...>) for UI actions.

## Renderer contract

NodeGraph is visual and must implement renderer customization.

API:

    public NodeGraphRenderer renderer();
    public NodeGraph renderer(NodeGraphRenderer renderer);
    public NodeGraph useDefaultRenderer();

Renderer interface:

    @FunctionalInterface
    public interface NodeGraphRenderer {
        void render(DrawScope draw, NodeGraphState state);
    }

Default renderer should live in widgets/render/NodeGraphRenderers.java and be exposed through WidgetsRender.nodeGraph().

### Snapshots

    public record NodeGraphState(
            float x,
            float y,
            float width,
            float height,
            NodeGraphViewport viewport,
            List<NodeGraphItemState> items,
            List<NodeGraphPortState> ports,
            List<NodeGraphConnectionState> connections,
            NodeGraphInteractionState interaction,
            MutableColor gridColor,
            MutableColor connectionColor,
            MutableColor selectedConnectionColor,
            MutableColor portColor,
            MutableColor selectedItemColor
    ) {}

    public record NodeGraphItemState(
            String id,
            float worldX,
            float worldY,
            float worldWidth,
            float worldHeight,
            float x,
            float y,
            float width,
            float height,
            int zIndex,
            boolean hovered,
            boolean selected,
            boolean dragging,
            boolean locked,
            boolean visible,
            NodeGraphItemChrome chrome
    ) {}

Renderer must not directly mutate graph state or call item widgets.

### Render hooks

Fine-grained hooks are allowed but do not replace the main renderer:

    NodeGraph itemChromeRenderer(NodeGraphItemChromeRenderer renderer);
    NodeGraph portRenderer(NodeGraphPortRenderer renderer);
    NodeGraph connectionRenderer(NodeGraphConnectionRenderer renderer);
    NodeGraph gridRenderer(NodeGraphGridRenderer renderer);

These hooks are render-only. They receive state records and DrawScope.

## Public API sketch

    NodeGraph graph = Widgets.nodeGraph()
            .gridVisible(true)
            .panEnabled(true)
            .zoomEnabled(true)
            .selectionMode(NodeGraphSelectionMode.MULTIPLE)
            .itemDragMode(NodeGraphItemDragMode.BODY_WHEN_NOT_INPUT);

    graph.addItem("recipe", recipeWidget, 40.0f, 40.0f)
         .addItem("preview", previewWidget, 360.0f, 80.0f)
         .addPort("recipe", "out", NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.5f, "item")
         .addPort("preview", "in", NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.5f, "item")
         .connect(NodeGraphPortRef.of("recipe", "out"), NodeGraphPortRef.of("preview", "in"));

    graph.onItemMoved(event -> {
        // EventListener receives typed event; no direct action callback stored in widget API.
    });

## Factory API

Add to Widgets:

    public static NodeGraph nodeGraph() {
        return new NodeGraph();
    }

Add to WidgetsRender:

    public static NodeGraphRenderer nodeGraph() {
        return NodeGraphRenderers.DEFAULT;
    }

## Relationship to existing GraphView

Keep both:

- GraphView: simple static/interactive graph visualization with normalized points.
- NodeGraph: editable canvas containing widgets and optional ports/connections.

Do not retrofit GraphView into NodeGraph. The responsibilities are different enough that combining them would make both APIs worse.

## Data ownership

NodeGraph may own its internal item/port/connection model, but should expose immutable snapshots/lists where possible.

Recommended methods:

    List<NodeGraphItem> items();
    Optional<NodeGraphItem> item(String id);
    List<NodeGraphConnection> connections();

For mutable model classes, avoid leaking direct writable lists.

## Hit testing

Hit-test order:

1. active drag/capture target
2. ports, resize handles, item chrome overlays, in reverse z-order
3. child widgets, in reverse z-order
4. connections, with configurable hit slop
5. background

Connection hit slop should be larger than visual stroke:

    visual stroke: 1-2 px
    hit slop: 5-7 px

Item content hit-testing must account for clipping and viewport transform.

## Keyboard/gamepad

Phase 1 keyboard support:

- focused graph receives arrow keys to move selected items by 1 px world-space;
- Shift + arrow moves by 10 px;
- Delete removes selected connections/items if deleteEnabled;
- Ctrl+A selects all items if selectionMode == MULTIPLE;
- Escape cancels active connection/drag/lasso.

Every key action should dispatch the same typed events as pointer actions where applicable.

## Accessibility / focus

- NodeGraph should be focusable.
- Item content keeps normal focus behavior.
- Clicking item chrome focuses graph; clicking child input focuses child.
- Selection highlight must be visible in renderer state.
- Disabled graph should not allow editing, dragging, or connection creation.

## Interaction state

Internal state should be explicit, not inferred from scattered booleans.

    private enum InteractionMode {
        IDLE,
        PANNING,
        DRAGGING_ITEMS,
        RESIZING_ITEM,
        CONNECTING_PORTS,
        LASSO_SELECTING
    }

Store pointer id and start/current positions:

    int activePointerId;
    float dragStartRootX;
    float dragStartRootY;
    float dragStartWorldX;
    float dragStartWorldY;
    Map<String, MutableRect> itemDragStartRects;

Pointer capture is mandatory for drag-like modes.

## Clipping and overlays

The graph itself clips its canvas, but child widgets can still use OverlayLayer for popups/menus.

Rules:

- NodeGraph should push clip around graph canvas render.
- Do not clip global overlays rendered by OverlayLayer.
- If an item content opens a popup, it should behave like it does inside ScrollView or DockingRoot.
- If graph-level context menus are added later, implement via Popup/OverlayLayer.

## Serialization

Phase 1 should not require persistence, but the model should be serializable.

Snapshot shape:

    public record NodeGraphSnapshot(
            NodeGraphViewport viewport,
            List<NodeGraphItemSnapshot> items,
            List<NodeGraphPortSnapshot> ports,
            List<NodeGraphConnectionSnapshot> connections,
            List<String> selectedItemIds,
            List<String> selectedConnectionIds
    ) {}

Do not serialize widget instances. Store item ids and layout/port/connection state only. The caller rehydrates widgets by id.

## Performance

Initial target: hundreds of items/connections, not tens of thousands.

Important choices:

- Cache world-to-root item bounds per arrange pass.
- Avoid recomputing port positions repeatedly during render/hit-test.
- Render only visible connections/items when clipping is enabled.
- Keep renderer snapshots immutable for safety, but avoid excessive allocations inside tight loops where possible.
- Consider spatial index later for large graphs.

Phase 1 can use linear scans in reverse z-order.

## Theming

Default style should match current UniGUI dark/blue visual language:

- background: dark panel
- grid: subtle blue-gray
- selected item: cyan/blue outline
- hovered port: amber/cyan accent
- valid connection preview: cyan
- invalid connection preview: red
- disabled/locked item: dimmed chrome

Expose mutable colors on NodeGraph, but route rendering through state snapshots.

## Tests

Minimum self-test coverage:

### Model/API

- add/remove item preserves ids and child parent/context attachment;
- duplicate ids are rejected or update existing item consistently;
- arbitrary widget content is measured/arranged at requested world rect;
- Widgets.nodeGraph() factory returns NodeGraph.

### Renderer contract

- default renderer emits background/grid/connection/item chrome commands;
- custom renderer receives complete NodeGraphState;
- render hooks receive hover/selected/dragging state.

### Events

- onItemMoved receives NodeGraphItemMovedEvent;
- event routes through capture/target/bubble;
- routeTo(...) copies cancellation;
- cancelling move/connection event prevents model mutation where appropriate.

### Interaction

- click item selects it;
- Ctrl click toggles selection;
- drag selected item moves all selected items;
- child Button inside item can still be clicked without moving item;
- text/non-input content does not block body drag;
- port drag creates connection when policy validates it;
- invalid connection does not mutate model;
- pan captures pointer and updates viewport;
- wheel zoom/pan consumes event while hovered unless opt-out is disabled;
- clipping prevents outside graph content rendering.

### Regression cases

- nested scrollable child inside graph item keeps wheel ownership;
- item near graph edge clips correctly;
- drag does not continue after pointer release/cancel;
- deleting selected item removes attached connections;
- zoom/pan does not desync connection endpoint hit-tests.

## Implementation phases

### Phase 1 - Canvas items and renderer contract

- [x] Add NodeGraph, NodeGraphItem, viewport state.
- [x] Place arbitrary widgets in world space.
- [x] Pan support.
- [x] Item selection and dragging.
- [x] Renderer snapshot/default renderer.
- [x] Typed item/viewport events.
- [x] Widgets.nodeGraph() and WidgetsRender.nodeGraph().
- [x] Basic self-tests.

Exit criteria:

- arbitrary Button, Label, VBox, ScrollView can be placed and interacted with;
- item drag works without breaking child button clicks;
- common tests pass.

### Phase 2 - Ports and connections

- [x] Add ports and connection model.
- [x] Add connection rendering.
- [x] Add connection drag interaction.
- [x] Add validation policy.
- [x] Add typed connection events.
- [x] Add connection hit-test/selection/removal.

Exit criteria:

- user can connect output to input;
- invalid connections are rejected visually and in model;
- selected connection can be removed;
- renderer state exposes valid/invalid/hovered connection preview.

### Phase 3 - Zoom, lasso, resize

- [x] Add zoom-at-cursor with min/max.
- [x] Add lasso selection.
- [x] Add optional item resize handles.
- [x] Add keyboard movement/delete/select-all.

Exit criteria:

- zoom keeps pointer/world mapping stable;
- lasso and keyboard selection work;
- resize emits typed events and is cancellable.

### Phase 4 - Snapshot/persistence and examples

- [x] Add NodeGraphSnapshot.
- [x] Add restore from snapshot with widget resolver.
- [x] Add /unigui nodegraph example screen or integrate into /unigui docking editor tab.
- [x] Demo: recipe-machine style nodes with actual widgets inside nodes.

Exit criteria:

- snapshot round-trips layout/ports/connections/viewport;
- example demonstrates arbitrary widgets, ports, connections, pan, zoom, selection.

## Recommended example screen

Add a separate command later:

    /unigui nodegraph

Example contents:

- left inspector panel;
- central NodeGraph;
- nodes:
  - Recipe Input node with TextField, ComboBox, Button;
  - Machine node with ProgressBar, ToggleButton;
  - Output Preview node with MinecraftPreviewWidget or placeholder;
  - Comment card using TextBlock;
- ports between recipe -> machine -> output;
- toolbar toggles:
  - Lock selected item
  - Show grid
  - Allow invalid preview
  - Consume wheel while hovered
  - Reset viewport

This will prove that NodeGraph is a widget canvas, not just circles and lines.

## Open questions

- Should phase 1 include zoom, or should zoom wait until child transform/input scaling is fully safe?
- Should connection endpoints support multiple visual lanes per port from the start?
- Should item content be clipped to item bounds by default?
- Should ports be graph-owned decorations only, or can a port itself be a child widget later?
- Should locked items still allow child widget interaction? Recommended: yes.
- Should graph support multi-root groups/subgraphs? Recommended: not in phase 1.

## Definition of done

A first production-ready NodeGraph implementation is done when:

- all public user actions are typed events per WIDGETS_CONTRACT.md;
- visual rendering goes through NodeGraphRenderer + immutable state snapshots;
- arbitrary widget content works inside items;
- item drag does not break child input;
- ports/connections can be created and validated;
- selection, pan, clipping, and wheel ownership are tested;
- Widgets.nodeGraph() and demo/example exist;
- common compile and tests pass.

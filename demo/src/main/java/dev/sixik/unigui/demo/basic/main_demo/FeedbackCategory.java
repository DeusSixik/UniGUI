package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.Popup;
import dev.sixik.unigui.widgets.feedback.Toast;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import dev.sixik.unigui.widgets.interaction.Button;

import java.util.List;

final class FeedbackCategory extends DemoCategory {
    private static final List<Control> CONTROLS = List.of(
            new Control("context-menu", "ContextMenu", "Present actions beside a control and report the chosen command."),
            new Control("tooltip", "Tooltip", "Show a short hint while the pointer rests on its anchor."),
            new Control("popup", "Popup", "Open anchored content without changing the surrounding layout."),
            new Control("toast", "Toast", "Send a temporary notification to the gallery notification stack."));

    @Override
    String title() {
        return "Feedback";
    }

    @Override
    boolean expandedByDefault() {
        return false;
    }

    @Override
    List<Control> controls() {
        return CONTROLS;
    }

    @Override
    void addDemo(String controlId, VBox root) {
        switch (controlId) {
            case "context-menu" -> addContextMenu(root);
            case "tooltip" -> addTooltip(root);
            case "popup" -> addPopup(root);
            case "toast" -> addToast(root);
            default -> root.addChild(samplePanel("Preview", "Interactive sample", new Label(controlId), ""));
        }
    }

    private void addContextMenu(VBox root) {
        Label result = label("Choose an action from the menu.", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        Button anchor = new Button("Open actions");
        anchor.layout(style -> style.size(132.0f, 34.0f).flexGrow(0.0f).flexShrink(0.0f));
        VBox content = new VBox();
        content.spacing(10.0f);
        content.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f).alignItems(Align.START));
        content.addChild(anchor);
        content.addChild(result);

        OverlayLayer overlay = localOverlay(content, 140.0f);
        ContextMenu menu = new ContextMenu()
                .item("Copy", () -> result.text("Copied to clipboard"))
                .separator()
                .item("Rename", () -> result.text("Rename selected"))
                .item("Delete", () -> result.text("Delete selected"));
        overlay.addOverlay(menu);
        anchor.onClick(event -> {
            var bounds = anchor.layoutBounds();
            menu.openAt(bounds.x(), bounds.y() + bounds.height());
        });

        root.addChild(samplePanel("Action menu", "Open the menu to run a command; clicking outside dismisses it.", overlay,
                "ContextMenu menu = new ContextMenu()\n        .item(\"Copy\", this::copy)\n        .separator()\n        .item(\"Delete\", this::delete);\noverlay.addOverlay(menu);\nmenu.openAt(x, y);"));
    }

    private void addTooltip(VBox root) {
        Button anchor = new Button("Hover for a hint");
        anchor.layout(style -> style.size(150.0f, 34.0f).flexGrow(0.0f).flexShrink(0.0f));
        VBox content = new VBox();
        content.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f).alignItems(Align.START));
        content.addChild(anchor);
        OverlayLayer overlay = localOverlay(content, 64.0f);
        Tooltip tooltip = new Tooltip(anchor, "A tooltip is anchored to this control.")
                .maxWidth(210.0f)
                .offset(0.0f, 10.0f);
        overlay.addOverlay(tooltip);

        root.addChild(samplePanel("Hover hint", "Move the pointer over the button to reveal its anchored tooltip.", overlay,
                "Tooltip tooltip = new Tooltip(button, \"Helpful context\")\n        .maxWidth(210.0f);\noverlay.addOverlay(tooltip);"));
    }

    private void addPopup(VBox root) {
        Button anchor = new Button("Toggle details");
        anchor.layout(style -> style.size(150.0f, 34.0f).flexGrow(0.0f).flexShrink(0.0f));

        Popup[] popupRef = new Popup[1];
        VBox popupContent = new VBox();
        popupContent.spacing(8.0f);
        popupContent.layout(style -> style.size(190.0f, 88.0f).padding(8.0f, 6.0f));
        popupContent.addChild(label("Quick details", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        popupContent.addChild(label("This panel floats above the layout.", UniGuiDemo.GalleryTemplate.color(86, 105, 121)));
        Button close = new Button("Close");
        close.onClick(event -> {
            if (popupRef[0] != null) popupRef[0].close();
        });
        popupContent.addChild(close);

        VBox content = new VBox();
        content.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f).alignItems(Align.START));
        content.addChild(anchor);
        OverlayLayer overlay = localOverlay(content, 144.0f);
        Popup popup = new Popup(anchor, popupContent).placement(Popup.Placement.BELOW);
        popupRef[0] = popup;
        overlay.addOverlay(popup);
        anchor.onClick(event -> popup.open());

        root.addChild(samplePanel("Anchored popup", "Open a floating content panel; close it from the panel or with an outside click.", overlay,
                "Popup popup = new Popup(button, popupContent)\n        .placement(Popup.Placement.BELOW);\noverlay.addOverlay(popup);\nbutton.onClick(event -> popup.open());"));
    }

    private void addToast(VBox root) {
        Button show = new Button("Show notification");
        show.layout(style -> style.size(160.0f, 34.0f).flexGrow(0.0f).flexShrink(0.0f));
        Label result = label("No notification sent yet.", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        show.onClick(event -> {
            gallery.notifications.addToast(new Toast("Workspace saved successfully")
                    .duration(3.0f)
                    .preferredSize(230.0f, 40.0f));
            result.text("Notification sent to the bottom-right stack.");
        });

        VBox sample = new VBox();
        sample.spacing(12.0f);
        sample.layout(style -> style.widthPercent(100.0f));
        sample.addChild(show);
        sample.addChild(result);
        root.addChild(samplePanel("Temporary notification", "Toast messages are queued by the gallery notification stack and dismiss automatically.", sample,
                "notifications.addToast(new Toast(\"Workspace saved successfully\")\n        .duration(3.0f)\n        .preferredSize(230.0f, 40.0f));"));
    }

    private OverlayLayer localOverlay(Widget content, float height) {
        OverlayLayer overlay = new OverlayLayer(content);
        overlay.layout(style -> style.widthPercent(100.0f).height(height));
        return overlay;
    }
}

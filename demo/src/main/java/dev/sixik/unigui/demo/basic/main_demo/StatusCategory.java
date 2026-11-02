package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.LoadingIndicator;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import dev.sixik.unigui.widgets.feedback.Spinner;

import java.util.List;

final class StatusCategory extends DemoCategory {
    private static final List<Control> CONTROLS = List.of(
            new Control("progress", "ProgressBar", "Show completion for a bounded task."),
            new Control("spinner", "Spinner", "Indicate ongoing work when completion is unknown."));

    @Override
    String title() {
        return "Status and Info";
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
            case "progress" -> addProgress(root);
            case "spinner" -> addSpinner(root);
            default -> root.addChild(samplePanel("Preview", "Interactive sample", new Label(controlId), ""));
        }
    }

    private void addProgress(VBox root) {
        ProgressBar progress = new ProgressBar().range(0.0f, 100.0f).value(64.0f);
        progress.layout(style -> style.widthPercent(100.0f).height(18.0f));
        Label value = label("64% complete", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        var driver = slider(0.0f, 100.0f, 1.0f, 64.0f);
        driver.onValueChanged(event -> {
            progress.value(event.newValue());
            value.text(UniGuiDemo.GalleryTemplate.format(event.newValue()) + "% complete");
        });
        VBox sample = new VBox();
        sample.spacing(14.0f);
        sample.layout(style -> style.widthPercent(100.0f));
        sample.addChild(progress);
        sample.addChild(value);
        sample.addChild(driver);
        root.addChild(samplePanel("Determinate progress", "Pair the bar with a numeric value when completion is measurable.", sample,
                "progress.value(completed).range(0, total);"));

        var verticalDriver = verticalSlider(0.0f, 100.0f, 1.0f, 64.0f);
        ProgressBar filled = verticalBar(64.0f);
        ProgressBar remaining = verticalBar(36.0f);
        ProgressBar busy = verticalBar(0.0f).indeterminate(true);
        Label filledLabel = label("64%", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        Label remainingLabel = label("36%", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        verticalDriver.onValueChanged(event -> {
            filled.value(event.newValue());
            remaining.value(100.0f - event.newValue());
            filledLabel.text(UniGuiDemo.GalleryTemplate.format(event.newValue()) + "%");
            remainingLabel.text(UniGuiDemo.GalleryTemplate.format(100.0f - event.newValue()) + "%");
        });
        HBox verticalSample = new HBox();
        verticalSample.spacing(34.0f);
        verticalSample.layout(style -> style.widthPercent(100.0f).alignItems(Align.CENTER));
        verticalSample.addChild(captionedColumn(verticalDriver, label("Driver", UniGuiDemo.GalleryTemplate.TEXT_DARK)));
        verticalSample.addChild(captionedColumn(filled, filledLabel));
        verticalSample.addChild(captionedColumn(remaining, remainingLabel));
        verticalSample.addChild(captionedColumn(busy, label("Busy", UniGuiDemo.GalleryTemplate.TEXT_DARK)));
        root.addChild(samplePanel("Vertical progress",
                "A vertical bar fills from the bottom. Drag the slider to drive the first two bars; the last one is indeterminate.",
                verticalSample,
                "ProgressBar bar = new ProgressBar().orientation(Orientation.VERTICAL).range(0, 100);"));
    }

    private void addSpinner(VBox root) {
        HBox sample = new HBox();
        sample.spacing(14.0f);
        sample.layout(style -> style.widthPercent(100.0f).height(70.0f).alignItems(Align.CENTER));
        sample.addChild(new Spinner());
        sample.addChild(label("Loading project data...", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        root.addChild(samplePanel("Spinner", "Use a spinner while a task is running but its completion is unknown.", sample,
                "Spinner spinner = new Spinner();"));

        LoadingIndicator dots = new LoadingIndicator().mode(LoadingIndicator.Mode.DOTS);
        dots.layout(style -> style.size(72.0f, 24.0f).flexGrow(0.0f).flexShrink(0.0f));
        HBox dotsSample = new HBox();
        dotsSample.spacing(14.0f);
        dotsSample.layout(style -> style.widthPercent(100.0f).height(54.0f).alignItems(Align.CENTER));
        dotsSample.addChild(dots);
        dotsSample.addChild(label("Loading project data...", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        root.addChild(samplePanel("LoadingIndicator dots",
                "The dot mode is useful for compact status areas and inline loading states.", dotsSample,
                "LoadingIndicator indicator = new LoadingIndicator()\n        .mode(LoadingIndicator.Mode.DOTS);"));

        LoadingIndicator bar = new LoadingIndicator().mode(LoadingIndicator.Mode.BAR);
        bar.layout(style -> style.size(220.0f, 10.0f).flexGrow(0.0f).flexShrink(0.0f));
        VBox barSample = new VBox();
        barSample.spacing(12.0f);
        barSample.layout(style -> style.widthPercent(100.0f));
        barSample.addChild(bar);
        barSample.addChild(label("Synchronizing workspace...", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        root.addChild(samplePanel("LoadingIndicator bar",
                "The bar mode fits wider loading areas while the operation has no measurable completion percentage.",
                barSample,
                "LoadingIndicator indicator = new LoadingIndicator()\n        .mode(LoadingIndicator.Mode.BAR);"));
    }

    private static ProgressBar verticalBar(float value) {
        ProgressBar bar = new ProgressBar().orientation(dev.sixik.unigui.widgets.core.Orientation.VERTICAL)
                .range(0.0f, 100.0f).value(value);
        bar.layout(style -> style.size(18.0f, 130.0f).flexGrow(0.0f).flexShrink(0.0f));
        return bar;
    }

    private static Widget captionedColumn(Widget content, Label caption) {
        VBox column = new VBox();
        column.spacing(8.0f);
        column.layout(style -> style.alignItems(Align.CENTER));
        column.addChild(content);
        column.addChild(caption);
        return column;
    }
}

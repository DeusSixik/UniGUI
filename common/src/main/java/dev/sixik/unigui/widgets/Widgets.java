package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.Widget;

public final class Widgets {
    private Widgets() {
    }

    public static Box box() {
        return new Box();
    }

    public static Button button(String text) {
        return new Button(text);
    }

    public static ToggleButton toggleButton(String text) {
        return new ToggleButton(text);
    }

    public static Checkbox checkbox(String text) {
        return new Checkbox(text);
    }

    public static Text text(String text) {
        return new Text(text);
    }

    public static Label label(String text) {
        return new Label(text);
    }

    public static TextBlock textBlock(String text) {
        return new TextBlock(text);
    }

    public static TextureWidget texture(TextureHandle texture) {
        return new TextureWidget(texture);
    }

    public static ImageView image(TextureHandle texture) {
        return new ImageView(texture);
    }

    public static Shape shape() {
        return new Shape();
    }

    public static Border border() {
        return new Border();
    }

    public static Separator separator() {
        return new Separator();
    }

    public static CanvasWidget canvas() {
        return new CanvasWidget();
    }

    public static Path path() {
        return new Path();
    }

    public static PanelWidget panel() {
        return new PanelWidget();
    }

    public static CachedSubtreeWidget cached(Widget widget) {
        return new CachedSubtreeWidget(widget);
    }

    public static TextField textField() {
        return new TextField();
    }

    public static TextField textField(String text) {
        return new TextField(text);
    }

    public static PasswordField passwordField() {
        return new PasswordField();
    }

    public static PasswordField passwordField(String text) {
        return new PasswordField(text);
    }

    public static SearchField searchField() {
        return new SearchField();
    }

    public static SearchField searchField(String query) {
        return new SearchField(query);
    }

    public static Slider slider() {
        return new Slider();
    }

    public static ProgressBar progressBar() {
        return new ProgressBar();
    }

    public static NumberField numberField() {
        return new NumberField();
    }

    public static ScrollBar scrollBar() {
        return new ScrollBar();
    }

    public static ScrollView scrollView(Widget content) {
        return new ScrollView(content);
    }

    public static VBox vbox() {
        return new VBox();
    }

    public static HBox hbox() {
        return new HBox();
    }

    public static GridBox grid() {
        return new GridBox();
    }
}

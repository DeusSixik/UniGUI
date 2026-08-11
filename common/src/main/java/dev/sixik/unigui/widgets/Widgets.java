package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Widget;

/** Concise factory entry points for the built-in retained widgets. */
public final class Widgets {
    private Widgets() {
    }

    public static Box box() {
        return new Box();
    }

    public static Button button(String text) {
        return new Button(text);
    }

    public static Button button(RichText text) {
        return new Button(text);
    }

    public static ToggleButton toggleButton(String text) {
        return new ToggleButton(text);
    }

    public static ToggleButton toggleButton(RichText text) {
        return new ToggleButton(text);
    }

    public static Checkbox checkbox(String text) {
        return new Checkbox(text);
    }

    public static Checkbox checkbox(RichText text) {
        return new Checkbox(text);
    }

    public static RadioButton radioButton(String text) {
        return new RadioButton(text);
    }

    public static RadioButton radioButton(RichText text) {
        return new RadioButton(text);
    }

    public static RadioButton radioButton(String text, String value) {
        return new RadioButton(text, value);
    }

    public static RadioButton radioButton(RichText text, String value) {
        return new RadioButton(text, value);
    }

    public static RadioGroup radioGroup() {
        return new RadioGroup();
    }

    public static ComboBox comboBox() {
        return new ComboBox();
    }

    public static DropDownBox dropDownBox() {
        return new DropDownBox();
    }

    public static TreeView treeView() {
        return new TreeView();
    }

    public static TreeList treeList() {
        return new TreeList();
    }

    public static <T> TreeListPicker<T> treeListPicker() {
        return new TreeListPicker<>();
    }

    public static TreeViewNode treeNode(String text) {
        return new TreeViewNode(text);
    }

    public static TreeViewNode treeNode(RichText text) {
        return new TreeViewNode(text);
    }

    public static Text text(String text) {
        return new Text(text);
    }

    /** Generic display text; use label(...) for a focus-target caption. */
    public static Text text(RichText text) {
        return new Text(text);
    }

    /** Caption text that can be associated with a focus target via Label.focusTarget(...). */
    public static Label label(String text) {
        return new Label(text);
    }

    public static Label label(RichText text) {
        return new Label(text);
    }

    public static TextBlock textBlock(String text) {
        return new TextBlock(text);
    }

    public static TextBlock textBlock(RichText text) {
        return new TextBlock(text);
    }

    public static RichTextView richTextView(RichText text) {
        return new RichTextView(text);
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

    public static OverlayLayer overlayLayer(Widget content) {
        return new OverlayLayer(content);
    }

    public static OverlayLayer overlayLayer() {
        return new OverlayLayer();
    }

    public static Tooltip tooltip(Widget anchor, String text) {
        return new Tooltip(anchor, text);
    }

    public static Popup popup(Widget anchor, Widget content) {
        return new Popup(anchor, content);
    }

    public static ContextMenu contextMenu() {
        return new ContextMenu();
    }

    public static Toast toast(String text) {
        return new Toast(text);
    }

    public static NotificationView notification(String text) {
        return new NotificationView(text);
    }

    public static WindowWidget window(String title, Widget content) {
        return new WindowWidget(title, content);
    }

    public static StackPanel stack() {
        return new StackPanel();
    }

    public static DockPanel dock() {
        return new DockPanel();
    }

    public static WrapPanel wrap() {
        return new WrapPanel();
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

    public static TextInput textInput() {
        return new TextInput();
    }

    public static TextInput textInput(String text) {
        return new TextInput(text);
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

    public static LoadingIndicator loadingIndicator() {
        return new LoadingIndicator();
    }

    public static Spinner spinner() {
        return new Spinner();
    }

    public static Spinner spinner(Spinner.Style style) {
        return new Spinner(style);
    }

    public static SplitPanel splitPanel() {
        return new SplitPanel();
    }

    public static SplitPanel splitPanel(Widget first, Widget second) {
        return new SplitPanel(first, second);
    }

    public static Breadcrumb breadcrumb() {
        return new Breadcrumb();
    }

    public static BreadcrumbItem breadcrumbItem(String text) {
        return new BreadcrumbItem(text);
    }

    public static NumberField numberField() {
        return new NumberField();
    }

    public static TimeSpanField timeSpanField() {
        return new TimeSpanField();
    }

    public static DatePicker datePicker() {
        return new DatePicker();
    }

    public static ColorPicker colorPicker() {
        return new ColorPicker();
    }

    public static Chart chart() {
        return new Chart();
    }

    public static Sparkline sparkline() {
        return new Sparkline();
    }

    public static GraphView graphView() {
        return new GraphView();
    }

    public static NodeGraph nodeGraph() {
        return new NodeGraph();
    }

    public static Carousel carousel() {
        return new Carousel();
    }

    public static PageView pageView() {
        return new PageView();
    }

    public static View view(String title) {
        return new View(title);
    }

    public static ScrollBar scrollBar() {
        return new ScrollBar();
    }

    public static ScrollView scrollView(Widget content) {
        return new ScrollView(content);
    }

    public static ScrollView scrollView() {
        return new ScrollView();
    }

    public static ExpandablePanel expandablePanel(String title) {
        return new ExpandablePanel(title);
    }

    public static Accordion accordion() {
        return new Accordion();
    }

    public static TabControl tabControl() {
        return new TabControl();
    }

    public static VirtualListView virtualList() {
        return new VirtualListView();
    }

    public static VirtualTableView virtualTable() {
        return new VirtualTableView();
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

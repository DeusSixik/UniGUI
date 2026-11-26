package dev.sixik.unigui.widgets.rendering;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.render.WidgetTextureRenderer;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.xaml.XamlWidget;

import java.util.Collections;
import java.util.List;

/**
 * Renders widget content offscreen and exposes the result as a {@link TextureHandle}.
 *
 * <p>Set a child via {@link #content(Widget)}, draw commands via {@link #painter(Painter)}, or both.
 * The painter draws beneath the child. Display the texture with {@link #displayed(boolean)},
 * {@link #fit(ImageFit)} and {@link #filter(TextureFilter)}, or sample it elsewhere via {@link #texture()}.</p>
 *
 * <p>Content uses logical canvas units. The canvas defaults to the widget size and can be set via
 * {@link #canvasSize(float, float)}. Automatic texture dimensions scale the canvas dimensions by
 * {@link RenderContext#pixelScale()} and {@link #resolutionScale()}. A resolution scale above one
 * supersamples. {@link #textureSize(int, int)} sets explicit physical dimensions and fits the canvas
 * uniformly from the top left.</p>
 *
 * <p>{@link UpdateMode#EVERY_FRAME} refreshes continuously. {@link UpdateMode#ON_CHANGE} also tracks size,
 * settings and child invalidations. Call {@link #requestRender()} when an external painter dependency changes.
 * {@link UpdateMode#MANUAL} requires a request after the initial render and automatic target changes.</p>
 *
 * <p>Child input uses the widget's layout coordinates. To align input with displayed pixels, match the logical
 * canvas to the widget size and use {@link ImageFit#STRETCH}.</p>
 *
 * <p>The texture updates during rendering while the widget is visible in the tree.
 * Use {@link #displayed(boolean) displayed(false)} to continue updating without drawing the texture here.</p>
 */
@WikiPage(
        title = "RenderToTexture", slug = "render-to-texture",
        category = "API/3D Scene", order = 200,
        summary = "Offscreen widget that exposes its rendered content as a texture.")
@WikiSection(title = "Canvas and texture", order = 10, markdown = """
        The canvas defaults to the widget's layout size. `canvasSize` changes logical coordinates and `textureSize`
        sets physical pixels. `resolutionScale(2)` supersamples when physical dimensions are automatic. Match the
        canvas and widget sizes with `ImageFit.STRETCH` for child input to align with the texture.
        """)
@WikiSection(title = "Updating and sampling", order = 20, markdown = """
        Choose `EVERY_FRAME` for continuous redraws, `ON_CHANGE` to respond to child changes and explicit
        invalidation, or `MANUAL` for explicit requests after target setup. `texture()` exposes the current result.
        Re-read it after target size or backend changes.
        """)
@WikiExample(title = "Render a widget into a texture", code = """
        RenderToTexture preview = new RenderToTexture(new Label("Preview"));
        preview.layout(style -> style.width(160).height(48));
        preview.updateMode(RenderToTexture.UpdateMode.ON_CHANGE);
        preview.resolutionScale(2);
        """)
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@AiDoc(
        summary = "Offscreen widget that exposes its rendered content as a texture.",
        purpose = "Composite painter commands and child widgets into a texture for display or sampling.",
        criticalRules = {
                "The painter draws before the child. Scene3D runs its 3D pass before both.",
                "Canvas dimensions are logical. Automatic physical texture dimensions use pixel density and resolution scale.",
                "With ON_CHANGE, use `requestRender()` when external painter data changes.",
                "`displayed(false)` hides texture output while attached and visible. `texture()` returns null until the first successful offscreen render."
        },
        related = {"Scene3D"},
        examples = @AiDoc.Example(title = "Render a widget into a texture", code = """
                RenderToTexture preview = new RenderToTexture(new Label("Preview"));
                preview.layout(style -> style.width(160).height(48));
                preview.updateMode(RenderToTexture.UpdateMode.ON_CHANGE);
                preview.resolutionScale(2);
                """))
@XamlWidget("RenderToTexture")
public class RenderToTexture extends WidgetBase {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.RENDER_TO_TEXTURE;

    /**
     * Maximum physical width or height of a texture created by this widget.
     */
    public static final int MAX_TEXTURE_SIZE = 8192;

    /**
     * When to refresh the texture.
     */
    public enum UpdateMode {
        /**
         * Refreshes every rendered frame.
         */
        EVERY_FRAME,
        /**
         * Refreshes after size, settings or child changes and explicit requests.
         */
        ON_CHANGE,
        /**
         * Uses explicit requests after initialization and automatic target changes.
         */
        MANUAL
    }

    /**
     * Why the texture was redrawn. {@link #NONE} indicates reuse.
     */
    public enum RenderReason {
        NONE,
        NO_TEXTURE,
        REQUESTED,
        EVERY_FRAME,
        RESIZED,
        TARGET_OPTIONS_CHANGED,
        BACKEND_CHANGED,
        OWN_LAYOUT_DIRTY,
        OWN_TEXTURE_DIRTY,
        CHILD_SUBTREE_DIRTY
    }

    /**
     * Draws content into the offscreen texture.
     */
    @FunctionalInterface
    public interface Painter {
        /**
         * @param context offscreen context, with logical coordinates starting at (0, 0).
         * @param width logical canvas width.
         * @param height logical canvas height.
         */
        void paint(RenderContext context, float width, float height);
    }

    private final MutableColor tint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final MutableColor clearColor = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);
    private final Transform contentShift = new Transform();
    /**
     * Reusable target size and scale calculation.
     */
    private final TargetSize measuredTarget = new TargetSize();
    private Widget content;
    private List<Widget> childrenView = Collections.emptyList();
    private Painter painter;
    private UpdateMode updateMode = UpdateMode.EVERY_FRAME;
    private RenderTargetOptions targetOptions = RenderTargetOptions.COLOR;
    private float canvasWidth = Float.NaN;
    private float canvasHeight = Float.NaN;
    private int fixedTextureWidth;
    private int fixedTextureHeight;
    private float resolutionScale = 1.0f;
    private boolean displayed = true;
    private ImageFit fit = ImageFit.STRETCH;
    private TextureFilter filter = TextureFilter.LINEAR;

    private WidgetTextureRenderer textureRenderer;
    private RenderBackend rendererBackend;
    private SampledTexture texture;
    private int textureWidth;
    private int textureHeight;
    private float renderedCanvasWidth;
    private float renderedCanvasHeight;
    /**
     * 2D pass scale used in the most recently rendered texture.
     */
    private float renderedCoordinateScale;
    private boolean renderRequested = true;
    private boolean targetOptionsChanged;
    private boolean backendChanged;
    private long renderCount;
    private RenderReason lastRenderReason = RenderReason.NONE;

    /**
     * Creates an empty widget with automatic texture dimensions and per-frame updates.
     */
    @WikiMethod(section = "Content and updates", sectionOrder = 10,
            summary = "Creates an empty offscreen widget.")
    @AiDoc(section = "Content and updates", sectionOrder = 10,
            summary = "Creates an empty widget in EVERY_FRAME mode.")
    public RenderToTexture() {
        renderCachingEnabled(false);
        tint.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        clearColor.onChanged(this::requestRender);
    }

    /**
     * Creates a widget that draws the supplied child into its texture.
     *
     * @param content initial child widget, or null.
     */
    @WikiMethod(section = "Content and updates", sectionOrder = 10,
            summary = "Creates an offscreen widget with a child.")
    @AiDoc(section = "Content and updates", sectionOrder = 10,
            summary = "Creates an offscreen widget with a child.")
    public RenderToTexture(
            @WikiParameter(description = "initial child widget, or null.") Widget content) {
        this();
        content(content);
    }

    // ------------------------------------------------------------------ content

    public Widget content() {
        return content;
    }

    /**
     * Sets the child drawn into the texture.
     *
     * @param content child widget, or null.
     * @return this widget.
     */
    @WikiMethod(section = "Content and updates", sectionOrder = 10, order = 10,
            summary = "Sets the child drawn into the texture.")
    @WikiReturn("this widget")
    @AiDoc(section = "Content and updates", sectionOrder = 10, order = 10,
            summary = "Sets the child drawn into the texture.")
    public RenderToTexture content(
            @WikiParameter(description = "child widget, or null.") Widget content) {
        if (this.content == content) return this;
        if (content == this) throw new IllegalArgumentException("RenderToTexture cannot contain itself");

        detachContent();
        this.content = content;
        attachContent(content);
        childrenView = content == null ? Collections.emptyList() : Collections.singletonList(content);
        requestRender();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.TEXTURE);
        return this;
    }

    public Painter painter() {
        return painter;
    }

    /**
     * Sets the painter drawn beneath any child widget.
     *
     * @param painter painter callback, or null.
     * @return this widget.
     */
    @WikiMethod(section = "Content and updates", sectionOrder = 10, order = 20,
            summary = "Sets the painter drawn beneath any child widget.")
    @WikiReturn("this widget")
    @AiDoc(section = "Content and updates", sectionOrder = 10, order = 20,
            summary = "Sets the painter drawn beneath any child widget.")
    public RenderToTexture painter(
            @WikiParameter(description = "painter callback, or null.") Painter painter) {
        if (this.painter == painter) return this;
        this.painter = painter;
        requestRender();
        return this;
    }

    // ------------------------------------------------------------------ update

    public UpdateMode updateMode() {
        return updateMode;
    }

    /**
     * Sets how the offscreen texture is refreshed.
     *
     * @param updateMode refresh policy, or null for {@link UpdateMode#EVERY_FRAME}.
     * @return this widget.
     */
    @WikiMethod(section = "Content and updates", sectionOrder = 10, order = 25,
            summary = "Sets how the offscreen texture is refreshed.")
    @WikiReturn("this widget")
    @AiDoc(section = "Content and updates", sectionOrder = 10, order = 25,
            summary = "Selects EVERY_FRAME, ON_CHANGE or MANUAL refresh.")
    public RenderToTexture updateMode(
            @WikiParameter(description = "refresh policy, or null for `UpdateMode#EVERY_FRAME`.") UpdateMode updateMode) {
        this.updateMode = updateMode == null ? UpdateMode.EVERY_FRAME : updateMode;
        return this;
    }

    /**
     * Requests a texture refresh on the next render in any update mode.
     */
    @WikiMethod(section = "Content and updates", sectionOrder = 10, order = 30,
            summary = "Requests a texture refresh on the next render in any update mode.")
    @AiDoc(section = "Content and updates", sectionOrder = 10, order = 30,
            summary = "Requests a texture refresh on the next render in any update mode.")
    public void requestRender() {
        renderRequested = true;
        invalidate(InvalidationFlags.TEXTURE | InvalidationFlags.VISUAL);
    }

    // ------------------------------------------------------------------ size

    /**
     * Sets the logical canvas size, which defaults to the widget size.
     *
     * @param width logical width.
     * @param height logical height.
     * @return this widget.
     */
    @WikiMethod(section = "Canvas and target", sectionOrder = 20, order = 10,
            summary = "Sets the logical canvas size, which defaults to the widget size.")
    @WikiReturn("this widget")
    @AiDoc(section = "Canvas and target", sectionOrder = 20, order = 10,
            summary = "Sets the logical canvas size, which defaults to the widget size.")
    public RenderToTexture canvasSize(
            @WikiParameter(description = "logical width.") float width,
            @WikiParameter(description = "logical height.") float height) {
        if (!(width > 0.0f) || !(height > 0.0f) || !Float.isFinite(width) || !Float.isFinite(height)) {
            throw new IllegalArgumentException("Canvas size must be positive, got " + width + "x" + height);
        }
        canvasWidth = width;
        canvasHeight = height;
        invalidate(InvalidationFlags.LAYOUT);
        requestRender();
        return this;
    }

    /**
     * Restores the widget's layout size as the canvas size.
     *
     * @return this widget.
     */
    @WikiMethod(section = "Canvas and target", sectionOrder = 20, order = 20,
            summary = "Restores the widget's layout size as the canvas size.")
    @WikiReturn("this widget")
    @AiDoc(section = "Canvas and target", sectionOrder = 20, order = 20,
            summary = "Restores the widget's layout size as the canvas size.")
    public RenderToTexture clearCanvasSize() {
        canvasWidth = Float.NaN;
        canvasHeight = Float.NaN;
        invalidate(InvalidationFlags.LAYOUT);
        requestRender();
        return this;
    }

    /**
     * Checks for an explicit logical canvas size.
     *
     * @return true if an explicit size is configured.
     */
    @WikiMethod(section = "Canvas and target", sectionOrder = 20, order = 30,
            summary = "Checks for an explicit logical canvas size.")
    @WikiReturn("true if an explicit size is configured")
    @AiDoc(section = "Canvas and target", sectionOrder = 20, order = 30,
            summary = "Checks for an explicit logical canvas size.")
    public boolean hasCanvasSize() {
        return Float.isFinite(canvasWidth);
    }

    /**
     * Sets physical texture dimensions. Zero on either axis restores automatic sizing.
     *
     * @param width physical width in pixels.
     * @param height physical height in pixels.
     * @return this widget.
     */
    @WikiMethod(section = "Canvas and target", sectionOrder = 20, order = 40,
            summary = "Sets physical texture dimensions. Zero on either axis restores automatic sizing.")
    @WikiReturn("this widget")
    @AiDoc(section = "Canvas and target", sectionOrder = 20, order = 40,
            summary = "Sets physical texture dimensions. Zero on either axis restores automatic sizing.")
    public RenderToTexture textureSize(
            @WikiParameter(description = "physical width in pixels.") int width,
            @WikiParameter(description = "physical height in pixels.") int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Texture size cannot be negative, got " + width + "x" + height);
        }
        fixedTextureWidth = Math.min(width, MAX_TEXTURE_SIZE);
        fixedTextureHeight = Math.min(height, MAX_TEXTURE_SIZE);
        requestRender();
        return this;
    }

    public float resolutionScale() {
        return resolutionScale;
    }

    /**
     * Scales the automatic texture dimensions. Two supersamples and 0.5 halves the resolution.
     *
     * @param resolutionScale positive multiplier.
     * @return this widget.
     */
    @WikiMethod(section = "Canvas and target", sectionOrder = 20, order = 50,
            summary = "Scales the automatic texture dimensions. Two supersamples and 0.5 halves the resolution.")
    @WikiReturn("this widget")
    @AiDoc(section = "Canvas and target", sectionOrder = 20, order = 50,
            summary = "Scales the automatic texture dimensions. Two supersamples and 0.5 halves the resolution.")
    public RenderToTexture resolutionScale(
            @WikiParameter(description = "positive multiplier.") float resolutionScale) {
        if (!(resolutionScale > 0.0f) || !Float.isFinite(resolutionScale)) {
            throw new IllegalArgumentException("Resolution scale must be positive, got " + resolutionScale);
        }
        if (this.resolutionScale == resolutionScale) return this;
        this.resolutionScale = resolutionScale;
        requestRender();
        return this;
    }

    // ------------------------------------------------------------------ target

    public RenderTargetOptions targetOptions() {
        return targetOptions;
    }

    /**
     * Sets target options such as {@link RenderTargetOptions#COLOR_DEPTH}.
     *
     * @param targetOptions offscreen target options.
     * @return this widget.
     */
    @WikiMethod(section = "Canvas and target", sectionOrder = 20, order = 60,
            summary = "Sets target options such as `RenderTargetOptions#COLOR_DEPTH`.")
    @WikiReturn("this widget")
    @AiDoc(section = "Canvas and target", sectionOrder = 20, order = 60,
            summary = "Sets target options such as `RenderTargetOptions#COLOR_DEPTH`.")
    public RenderToTexture targetOptions(
            @WikiParameter(description = "offscreen target options.") RenderTargetOptions targetOptions) {
        RenderTargetOptions safeOptions = targetOptions == null ? RenderTargetOptions.COLOR : targetOptions;
        if (safeOptions.equals(this.targetOptions)) return this;
        this.targetOptions = safeOptions;
        resetRenderer();
        targetOptionsChanged = true;
        requestRender();
        return this;
    }

    /**
     * Enables or disables the target's depth attachment.
     *
     * @param depth true to use a depth buffer.
     * @return this widget.
     */
    @WikiMethod(section = "Canvas and target", sectionOrder = 20, order = 70,
            summary = "Enables or disables the target's depth attachment.")
    @WikiReturn("this widget")
    @AiDoc(section = "Canvas and target", sectionOrder = 20, order = 70,
            summary = "Enables or disables the target's depth attachment.")
    public RenderToTexture depth(
            @WikiParameter(description = "true to use a depth buffer.") boolean depth) {
        return targetOptions(depth ? RenderTargetOptions.COLOR_DEPTH : RenderTargetOptions.COLOR);
    }

    /**
     * Reads the mutable clear color.
     *
     * @return the color used before drawing content, initially transparent.
     */
    @WikiMethod(section = "Canvas and target", sectionOrder = 20, order = 80,
            summary = "Reads the mutable clear color.")
    @WikiReturn("the color used before drawing content, initially transparent")
    @AiDoc(section = "Canvas and target", sectionOrder = 20, order = 80,
            summary = "Reads the mutable clear color.")
    public MutableColor clearColor() {
        return clearColor;
    }

    // ------------------------------------------------------------------ display

    public boolean displayed() {
        return displayed;
    }

    /**
     * Controls whether the texture is drawn at this widget. A visible widget can still update the texture when false.
     *
     * @param displayed true to draw the texture here.
     * @return this widget.
     */
    @WikiMethod(section = "Displaying the result", sectionOrder = 30, order = 10,
            summary = "Controls whether the texture is drawn at this widget. A visible widget can still update the texture when false.")
    @WikiReturn("this widget")
    @AiDoc(section = "Displaying the result", sectionOrder = 30, order = 10,
            summary = "Controls whether the texture is drawn at this widget. A visible widget can still update the texture when false.")
    public RenderToTexture displayed(
            @WikiParameter(description = "true to draw the texture here.") boolean displayed) {
        if (this.displayed == displayed) return this;
        this.displayed = displayed;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public ImageFit fit() {
        return fit;
    }

    /**
     * Sets how the texture fits the widget. {@link ImageFit#NINE_SLICE} falls back to {@link ImageFit#STRETCH}.
     *
     * @param fit texture fit mode.
     * @return this widget.
     */
    @WikiMethod(section = "Displaying the result", sectionOrder = 30, order = 20,
            summary = "Sets how the texture fits the widget. `ImageFit#NINE_SLICE` falls back to `ImageFit#STRETCH`.")
    @WikiReturn("this widget")
    @AiDoc(section = "Displaying the result", sectionOrder = 30, order = 20,
            summary = "Sets how the texture fits the widget. `ImageFit#NINE_SLICE` falls back to `ImageFit#STRETCH`.")
    public RenderToTexture fit(
            @WikiParameter(description = "texture fit mode.") ImageFit fit) {
        this.fit = fit == null ? ImageFit.STRETCH : fit;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextureFilter filter() {
        return filter;
    }

    /**
     * Sets sampling for display and for {@link #texture()}. Use {@link TextureFilter#NEAREST} for pixel art.
     *
     * @param filter texture sampling filter.
     * @return this widget.
     */
    @WikiMethod(section = "Displaying the result", sectionOrder = 30, order = 30,
            summary = "Sets sampling for display and for `texture()`. Use `TextureFilter#NEAREST` for pixel art.")
    @WikiReturn("this widget")
    @AiDoc(section = "Displaying the result", sectionOrder = 30, order = 30,
            summary = "Sets sampling for display and for `texture()`. Use `TextureFilter#NEAREST` for pixel art.")
    public RenderToTexture filter(
            @WikiParameter(description = "texture sampling filter.") TextureFilter filter) {
        TextureFilter safe = filter == null ? TextureFilter.LINEAR : filter;
        if (this.filter == safe) return this;
        this.filter = safe;
        if (texture != null) texture = new SampledTexture(texture.source(), safe);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor tint() {
        return tint;
    }

    // ------------------------------------------------------------------ result

    /**
     * Reads the result texture.
     *
     * @return the texture, or null before the first render.
     */
    @WikiMethod(section = "Displaying the result", sectionOrder = 30, order = 40,
            summary = "Reads the result texture.")
    @WikiReturn("the texture, or null before the first render")
    @AiDoc(section = "Displaying the result", sectionOrder = 30, order = 40,
            summary = "Reads the result texture.")
    public TextureHandle texture() {
        return texture;
    }

    /**
     * Reads the physical result width.
     *
     * @return width in pixels, or zero before the first render.
     */
    @WikiMethod(section = "Displaying the result", sectionOrder = 30, order = 50,
            summary = "Reads the physical result width.")
    @WikiReturn("width in pixels, or zero before the first render")
    @AiDoc(section = "Displaying the result", sectionOrder = 30, order = 50,
            summary = "Reads the physical result width.")
    public int textureWidth() {
        return textureWidth;
    }

    /**
     * Reads the physical result height.
     *
     * @return height in pixels, or zero before the first render.
     */
    @WikiMethod(section = "Displaying the result", sectionOrder = 30, order = 60,
            summary = "Reads the physical result height.")
    @WikiReturn("height in pixels, or zero before the first render")
    @AiDoc(section = "Displaying the result", sectionOrder = 30, order = 60,
            summary = "Reads the physical result height.")
    public int textureHeight() {
        return textureHeight;
    }

    /**
     * Counts texture refreshes.
     *
     * @return number of times the texture was rendered.
     */
    @WikiMethod(section = "Displaying the result", sectionOrder = 30, order = 70,
            summary = "Counts texture refreshes.")
    @WikiReturn("number of times the texture was rendered")
    @AiDoc(section = "Displaying the result", sectionOrder = 30, order = 70,
            summary = "Counts texture refreshes.")
    public long renderCount() {
        return renderCount;
    }

    /**
     * Reads the reason for the last render decision.
     *
     * @return redraw reason or {@link RenderReason#NONE}.
     */
    @WikiMethod(section = "Displaying the result", sectionOrder = 30, order = 80,
            summary = "Reads the reason for the last render decision.")
    @WikiReturn("redraw reason or `RenderReason#NONE`")
    @AiDoc(section = "Displaying the result", sectionOrder = 30, order = 80,
            summary = "Reads the reason for the last render decision.")
    public RenderReason lastRenderReason() {
        return lastRenderReason;
    }

    // ------------------------------------------------------------------ widget

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        attachContent(content);
    }

    @Override
    public List<Widget> children() {
        return childrenView;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        float preferredWidth = hasCanvasSize() ? canvasWidth : 0.0f;
        float preferredHeight = hasCanvasSize() ? canvasHeight : 0.0f;
        if (content != null) {
            content.measure(context);
            if (!hasCanvasSize()) {
                preferredWidth = StackPanel.preferredWidth(content, 0.0f);
                preferredHeight = StackPanel.preferredHeight(content, 0.0f);
            }
        }
        setDesiredSize(resolveDesiredSize(context, preferredWidth, preferredHeight));
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (content != null && content.visibility() != Visibility.COLLAPSED) {
            StackPanel.arrangeChild(content, bounds.x(), bounds.y(),
                    resolvedCanvasWidth(bounds), resolvedCanvasHeight(bounds));
        }
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (content != null) content.tick(frame);
        if (updateMode == UpdateMode.EVERY_FRAME) {
            // Cached screens rebuild the frame only after invalidation.
            invalidate(InvalidationFlags.TEXTURE | InvalidationFlags.VISUAL);
        }
    }

    @Override
    public void render(RenderContext context) {
        boolean rawPass = hasRawPass();
        if (content == null && painter == null && !rawPass) return;
        pushOpacity(context);
        try {
            RenderBackend backend = context.backend();
            if (backend == null) {
                // Without a backend, draw the 2D content directly.
                renderDirect(context);
                return;
            }
            RenderReason reason = updateTexture(context, backend, rawPass);
            afterTextureUpdate(reason);
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (!renderCustomVisual(draw)) {
                if (displayed) drawTexture(draw);
                renderOverlay(draw, reason);
            }
        } finally {
            popOpacity(context);
        }
    }

    @Override
    public void dispose() {
        resetRenderer();
        if (content != null) content.dispose();
        detachContent();
        content = null;
        painter = null;
        childrenView = Collections.emptyList();
        super.dispose();
    }

    // ------------------------------------------------------------------ hooks for subclasses

    /**
     * Runs after each texture refresh decision.
     *
     * @param reason redraw reason or {@link RenderReason#NONE} when reusing the texture.
     */
    protected void afterTextureUpdate(RenderReason reason) {
    }

    /**
     * Draws above the texture, for example to show debug information.
     *
     * @param draw widget draw scope in layout coordinates.
     * @param reason redraw reason or {@link RenderReason#NONE} when the texture was reused.
     */
    protected void renderOverlay(DrawScope draw, RenderReason reason) {
    }

    /**
     * Reports whether the subclass needs a raw pass even without 2D content.
     *
     * @return true to invoke {@link #renderRawPass} when refreshing the texture.
     */
    protected boolean hasRawPass() {
        return false;
    }

    /**
     * Draws into the target before the 2D commands. A raw pass clears with {@link #clearColor()}
     * and requires target options with automatic 2D clearing disabled.
     *
     * @param backend active renderer backend.
     * @param target allocated offscreen target.
     * @param logicalWidth logical canvas width.
     * @param logicalHeight logical canvas height.
     * @param coordinateScale physical pixels per canvas unit.
     */
    protected void renderRawPass(RenderBackend backend, RenderTarget target, float logicalWidth,
                                 float logicalHeight, float coordinateScale) {
    }

    /**
     * Reusable measurement of the logical canvas and physical target.
     */
    protected static final class TargetSize {
        /**
         * Canvas width in logical UI units.
         */
        private float logicalWidth;
        /**
         * Canvas height in logical UI units.
         */
        private float logicalHeight;
        /**
         * Target width in physical pixels.
         */
        private int width;
        /**
         * Target height in physical pixels.
         */
        private int height;
        /**
         * Physical pixels per logical UI unit.
         */
        private float coordinateScale;

        /**
         * Creates the workspace for {@link RenderToTexture#targetSize(float)}.
         */
        private TargetSize() {
        }

        /**
         * Reads the canvas width.
         *
         * @return width in logical UI units.
         */
        public float logicalWidth() {
            return logicalWidth;
        }

        /**
         * Reads the canvas height.
         *
         * @return height in logical UI units.
         */
        public float logicalHeight() {
            return logicalHeight;
        }

        /**
         * Reads the target width.
         *
         * @return width in physical pixels.
         */
        public int width() {
            return width;
        }

        /**
         * Reads the target height.
         *
         * @return height in physical pixels.
         */
        public int height() {
            return height;
        }

        /**
         * Reads the target drawing scale.
         *
         * @return physical pixels per logical UI unit.
         */
        public float coordinateScale() {
            return coordinateScale;
        }
    }

    /**
     * Calculates the dimensions used by the offscreen pass.
     *
     * @param pixelScale density of the outer render context.
     * @return reused result, valid until the next calculation.
     */
    protected final TargetSize targetSize(float pixelScale) {
        RectView bounds = layoutBounds();
        float logicalWidth = resolvedCanvasWidth(bounds);
        float logicalHeight = resolvedCanvasHeight(bounds);

        float coordinateScale;
        int width;
        int height;
        if (fixedTextureWidth > 0 && fixedTextureHeight > 0) {
            width = fixedTextureWidth;
            height = fixedTextureHeight;
            coordinateScale = Math.min(width / logicalWidth, height / logicalHeight);
        } else {
            coordinateScale = safeScale(pixelScale) * resolutionScale;
            width = textureSide(logicalWidth * coordinateScale);
            height = textureSide(logicalHeight * coordinateScale);
            // Keep the target within MAX_TEXTURE_SIZE by reducing the scale with the dimensions.
            float limit = Math.min(width / (logicalWidth * coordinateScale), height / (logicalHeight * coordinateScale));
            if (limit < 1.0f) coordinateScale *= limit;
        }

        measuredTarget.width = width;
        measuredTarget.height = height;
        measuredTarget.logicalWidth = logicalWidth;
        measuredTarget.logicalHeight = logicalHeight;
        measuredTarget.coordinateScale = coordinateScale;
        return measuredTarget;
    }

    // ------------------------------------------------------------------ internals

    private RenderReason updateTexture(RenderContext context, RenderBackend backend, boolean rawPass) {
        WidgetTextureRenderer renderer = rendererFor(backend);
        RectView bounds = layoutBounds();
        TargetSize size = targetSize(context.pixelScale());
        float logicalWidth = size.logicalWidth;
        float logicalHeight = size.logicalHeight;
        float coordinateScale = size.coordinateScale;
        int width = size.width;
        int height = size.height;
        RenderReason reason = renderReason(width, height, logicalWidth, logicalHeight, coordinateScale);
        if (reason == RenderReason.NONE) return reason;

        RenderTarget target = renderer.renderToTarget(width, height, targetOptions, coordinateScale,
                offscreen -> paintContent(offscreen, bounds, logicalWidth, logicalHeight, !rawPass),
                rawPass ? destination -> renderRawPass(backend, destination, logicalWidth, logicalHeight,
                        coordinateScale) : null);
        TextureHandle source = target.colorTexture();
        if (texture == null || texture.source() != source) texture = new SampledTexture(source, filter);
        textureWidth = width;
        textureHeight = height;
        renderedCanvasWidth = logicalWidth;
        renderedCanvasHeight = logicalHeight;
        renderedCoordinateScale = coordinateScale;
        renderRequested = false;
        targetOptionsChanged = false;
        backendChanged = false;
        renderCount++;
        lastRenderReason = reason;
        if (content != null) clearSubtreeInvalidation(content);
        clearInvalidation(InvalidationFlags.LAYOUT | InvalidationFlags.TEXTURE);
        return reason;
    }

    private void paintContent(RenderContext offscreen, RectView bounds, float width, float height, boolean clear) {
        if (clear && clearColor.a() > 0.0f) {
            offscreen.rect(0.0f, 0.0f, width, height, Paint.fill(clearColor));
        }
        if (painter != null) {
            painter.paint(offscreen, width, height);
        }
        if (content != null && content.visibility() != Visibility.COLLAPSED) {
            // The child keeps screen-space layout coordinates for input routing.
            // Translate its offscreen commands, including clips, meshes and nested transforms, to the canvas origin.
            contentShift.position().set(-bounds.x(), -bounds.y());
            offscreen.pushTransform(bounds, contentShift);
            try {
                content.renderCached(offscreen);
            } finally {
                offscreen.popTransform();
            }
        }
    }

    private RenderReason renderReason(int width, int height, float logicalWidth, float logicalHeight,
                                      float coordinateScale) {
        if (backendChanged) return RenderReason.BACKEND_CHANGED;
        if (targetOptionsChanged) return RenderReason.TARGET_OPTIONS_CHANGED;
        if (texture == null) return RenderReason.NO_TEXTURE;
        if (renderRequested) return RenderReason.REQUESTED;
        if (textureWidth != width || textureHeight != height
                || renderedCanvasWidth != logicalWidth || renderedCanvasHeight != logicalHeight
                || renderedCoordinateScale != coordinateScale) {
            return RenderReason.RESIZED;
        }
        if (updateMode == UpdateMode.EVERY_FRAME) return RenderReason.EVERY_FRAME;
        if (updateMode == UpdateMode.MANUAL) return RenderReason.NONE;
        if (InvalidationFlags.has(invalidationFlags(), InvalidationFlags.LAYOUT)) return RenderReason.OWN_LAYOUT_DIRTY;
        if (InvalidationFlags.has(invalidationFlags(), InvalidationFlags.TEXTURE))
            return RenderReason.OWN_TEXTURE_DIRTY;
        if (content != null && content.subtreeInvalidationFlags() != InvalidationFlags.NONE) {
            return RenderReason.CHILD_SUBTREE_DIRTY;
        }
        return RenderReason.NONE;
    }

    private void drawTexture(DrawScope draw) {
        if (texture == null) return;
        RectView bounds = layoutBounds();
        ImageFit placementFit = fit == ImageFit.NINE_SLICE ? ImageFit.STRETCH : fit;
        if (placementFit == ImageFit.STRETCH) {
            draw.texture(texture, bounds.x(), bounds.y(), bounds.width(), bounds.height(), Paint.fill(tint));
            return;
        }
        TexturePlacement placement = TexturePlacement.fit(texture, bounds, placementFit);
        draw.texture(texture, placement, 0.0f, Paint.fill(tint));
    }

    private void renderDirect(RenderContext context) {
        RectView bounds = layoutBounds();
        if (painter != null) {
            contentShift.position().set(bounds.x(), bounds.y());
            context.pushTransform(bounds, contentShift);
            try {
                painter.paint(context, resolvedCanvasWidth(bounds), resolvedCanvasHeight(bounds));
            } finally {
                context.popTransform();
            }
        }
        if (content != null) renderChildWithInheritedTransform(context, content);
    }

    private float resolvedCanvasWidth(RectView bounds) {
        return hasCanvasSize() ? canvasWidth : Math.max(1.0f, Math.abs(bounds.width()));
    }

    private float resolvedCanvasHeight(RectView bounds) {
        return hasCanvasSize() ? canvasHeight : Math.max(1.0f, Math.abs(bounds.height()));
    }

    private WidgetTextureRenderer rendererFor(RenderBackend backend) {
        if (textureRenderer == null || rendererBackend != backend) {
            boolean hadRenderer = textureRenderer != null;
            resetRenderer();
            textureRenderer = new WidgetTextureRenderer(backend);
            rendererBackend = backend;
            backendChanged = hadRenderer;
        }
        return textureRenderer;
    }

    private void resetRenderer() {
        if (textureRenderer != null) {
            textureRenderer.close();
            textureRenderer = null;
        }
        rendererBackend = null;
        texture = null;
        textureWidth = 0;
        textureHeight = 0;
        backendChanged = false;
    }

    private void attachContent(Widget widget) {
        if (widget instanceof WidgetBase base) {
            base.setParentInternal(this);
            base.setUiContextInternal(uiContext());
        }
    }

    private void detachContent() {
        Widget previous = content;
        content = null;
        if (previous instanceof WidgetBase base) {
            base.setParentInternal(null);
            base.setUiContextInternal(null);
        }
        childrenView = Collections.emptyList();
    }

    private static void clearSubtreeInvalidation(Widget widget) {
        if (widget == null) return;
        for (Widget child : widget.children()) {
            clearSubtreeInvalidation(child);
        }
        widget.clearInvalidation(InvalidationFlags.ALL);
    }

    private static int textureSide(float pixels) {
        return Math.max(1, Math.min(MAX_TEXTURE_SIZE, (int) Math.ceil(pixels - 1.0e-3f)));
    }

    private static float safeScale(float scale) {
        return Float.isFinite(scale) && scale > 0.0f ? scale : 1.0f;
    }
}

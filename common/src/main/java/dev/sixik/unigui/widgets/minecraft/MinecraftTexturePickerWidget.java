package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.backend.minecraft.MinecraftFonts;
import dev.sixik.unigui.backend.minecraft.MinecraftTextureHandle;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.SearchableGridPickerWidget;
import dev.sixik.unigui.widgets.display.TextureWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Compact Minecraft texture selector backed by the generic searchable grid picker.
 */
@XmlWidgetName("MinecraftTexturePickerWidget")
public class MinecraftTexturePickerWidget extends SearchableGridPickerWidget<MinecraftTexturePickerWidget.TextureEntry> {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.MINECRAFT_TEXTURE_PICKER_WIDGET;

    private static final MutableColor WHITE = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);

    public MinecraftTexturePickerWidget() {
        this(true);
    }

    public MinecraftTexturePickerWidget(boolean reloadFromMinecraftResources) {
        super("Select Minecraft Texture",
                new TextureWidget(),
                "No textures",
                "Select texture...",
                240.0f,
                26.0f);
        selectedPreview().fit(ImageFit.CONTAIN);
        searchField().placeholder("Search texture...");
        dialog().position(36.0f, 36.0f);

        if (reloadFromMinecraftResources) {
            reloadFromMinecraftResources();
        } else {
            textures(List.of());
        }
    }

    @Override
    public TextureWidget selectedPreview() {
        return (TextureWidget) super.selectedPreview();
    }

    public List<TextureEntry> textures() {
        return entries();
    }

    public int textureCount() {
        return entryCount();
    }

    public List<TextureEntry> filteredTextures() {
        return filteredEntries();
    }

    public int filteredTextureCount() {
        return filteredEntryCount();
    }

    public ResourceLocation selectedId() {
        return selectedEntryOrNull() == null ? null : selectedEntryOrNull().id();
    }

    public MinecraftTextureHandle selectedTexture() {
        return selectedEntryOrNull() == null ? null : selectedEntryOrNull().texture();
    }

    public MinecraftTexturePickerWidget reloadFromMinecraftResources() {
        List<TextureEntry> resourceTextures = new ObjectArrayList<>();
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                ResourceManager resourceManager = minecraft.getResourceManager();
                Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                        "textures",
                        id -> id.getPath().endsWith(".png"));
                for (ResourceLocation id : resources.keySet()) {
                    resourceTextures.add(new TextureEntry(id));
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            resourceTextures.clear();
        }
        return textures(resourceTextures);
    }

    public MinecraftTexturePickerWidget textures(Collection<TextureEntry> entries) {
        super.entries(entries);
        return this;
    }

    public MinecraftTexturePickerWidget textureIds(Collection<ResourceLocation> ids) {
        List<TextureEntry> entries = new ObjectArrayList<>();
        if (ids != null) {
            for (ResourceLocation id : ids) {
                if (id != null) {
                    entries.add(new TextureEntry(id));
                }
            }
        }
        return textures(entries);
    }

    public MinecraftTexturePickerWidget selectId(ResourceLocation id) {
        selectEntry(findById(id), true, false);
        return this;
    }

    @Override
    public MinecraftTexturePickerWidget open() {
        super.open();
        return this;
    }

    @Override
    public MinecraftTexturePickerWidget close() {
        super.close();
        return this;
    }

    @Override
    public MinecraftTexturePickerWidget overlayLayer(OverlayLayer overlayLayer) {
        super.overlayLayer(overlayLayer);
        return this;
    }

    @Override
    @XmlAttribute(value = "query", category = "Behavior", defaultValue = "", description = "Initial search query used to filter textures.")
    public MinecraftTexturePickerWidget query(String query) {
        super.query(query);
        return this;
    }

    @Override
    public MinecraftTexturePickerWidget clearQuery() {
        super.clearQuery();
        return this;
    }

    @Override
    @XmlAttribute(value = "selectedIndex", category = "Behavior", defaultValue = "-1", description = "Initial selected texture index.")
    public MinecraftTexturePickerWidget selectIndex(int index) {
        super.selectIndex(index);
        return this;
    }

    @Override
    @XmlAttribute(value = "selectedFilteredIndex", category = "Behavior", defaultValue = "-1", description = "Initial selected index in the filtered texture list.")
    public MinecraftTexturePickerWidget selectFilteredIndex(int filteredIndex) {
        super.selectFilteredIndex(filteredIndex);
        return this;
    }

    @Override
    public MinecraftTexturePickerWidget clearSelection() {
        super.clearSelection();
        return this;
    }

    @Override
    public Optional<TextureEntry> selectedEntry() {
        return super.selectedEntry();
    }

    @Override
    public EventSubscription onSelectionChanged(EventListener<? super SelectionChangedEvent> listener) {
        return super.onSelectionChanged(listener);
    }

    @Override
    protected String entryIdText(TextureEntry entry) {
        return entry.id().toString();
    }

    @Override
    protected String searchText(TextureEntry entry) {
        return entry.id()
                + "\n"
                + entry.id().getPath()
                + "\n"
                + shortTexturePath(entry.id());
    }

    @Override
    protected void clearSelectedPreview() {
        selectedPreview().texture(null);
    }

    @Override
    protected void updateSelectedPreview(TextureEntry entry) {
        selectedPreview().texture(entry.texture());
    }

    @Override
    protected void renderEntryIcon(RenderContext context,
                                   TextureEntry entry,
                                   float x,
                                   float y,
                                   float size,
                                   float opacity,
                                   Runnable repaint) {
        MutableRect bounds = new MutableRect(x, y, size, size);
        TexturePlacement placement = TexturePlacement.fit(entry.texture(), bounds, ImageFit.CONTAIN);
        context.texture(entry.texture(), placement, 0.0f, Paint.fill(WHITE));
    }

    @Override
    protected RichText tooltipText(TextureEntry entry) {
        return RichText.of(entry.id().toString(), MinecraftFonts.defaultFace(), TextRun.DEFAULT_PIXEL_SIZE);
    }

    private TextureEntry findById(ResourceLocation id) {
        if (id == null) return null;
        for (TextureEntry entry : entries()) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    private static String shortTexturePath(ResourceLocation id) {
        String path = id == null ? "" : id.getPath();
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - ".png".length());
        }
        return path;
    }

    public record TextureEntry(ResourceLocation id, MinecraftTextureHandle texture) {
        public TextureEntry(ResourceLocation id) {
            this(id, new MinecraftTextureHandle(id, 16, 16));
        }

        public TextureEntry {
            id = Objects.requireNonNull(id, "id");
            texture = texture == null ? new MinecraftTextureHandle(id, 16, 16) : texture;
        }
    }
}


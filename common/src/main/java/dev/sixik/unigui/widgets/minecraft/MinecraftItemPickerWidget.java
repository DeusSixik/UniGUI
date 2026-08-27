package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftFonts;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftGuiRenderBackend;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.SearchableGridPickerWidget;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Compact Minecraft item selector backed by the generic searchable grid picker.
 */
@XmlWidgetName("MinecraftItemPickerWidget")
public class MinecraftItemPickerWidget extends SearchableGridPickerWidget<MinecraftItemPickerWidget.ItemEntry> {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.MINECRAFT_ITEM_PICKER_WIDGET;

    private final Map<ItemEntry, ItemStack> stackCache = new HashMap<>();

    public MinecraftItemPickerWidget() {
        this(true);
    }

    public MinecraftItemPickerWidget(boolean reloadFromRegistry) {
        super("Select Minecraft Item",
                new MinecraftItemPreviewWidget("", ItemStack.EMPTY),
                "No items",
                "Select item...",
                220.0f,
                28.0f);
        selectedPreview().labelVisible(false);
        selectedPreview().previewSize(24.0f);
        searchField().placeholder("Search item...");

        if (reloadFromRegistry) {
            reloadFromRegistry();
        } else {
            items(List.of());
        }
    }

    @Override
    public MinecraftItemPreviewWidget selectedPreview() {
        return (MinecraftItemPreviewWidget) super.selectedPreview();
    }

    public List<ItemEntry> items() {
        return entries();
    }

    public int itemCount() {
        return entryCount();
    }

    public List<ItemEntry> filteredItems() {
        return filteredEntries();
    }

    public int filteredItemCount() {
        return filteredEntryCount();
    }

    public ResourceLocation selectedId() {
        return selectedEntryOrNull() == null ? null : selectedEntryOrNull().id();
    }

    public Item selectedItem() {
        return selectedEntryOrNull() == null ? null : selectedEntryOrNull().item();
    }

    public ItemStack selectedStack() {
        return selectedEntryOrNull() == null ? ItemStack.EMPTY : stackFor(selectedEntryOrNull());
    }

    public MinecraftItemPickerWidget reloadFromRegistry() {
        List<ItemEntry> registryItems = new ObjectArrayList<>();
        try {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == null || item == Items.AIR) continue;
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id != null) {
                    registryItems.add(new ItemEntry(id, item));
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            registryItems.clear();
        }
        return items(registryItems);
    }

    public MinecraftItemPickerWidget items(Collection<ItemEntry> entries) {
        stackCache.clear();
        super.entries(entries);
        return this;
    }

    public MinecraftItemPickerWidget selectId(ResourceLocation id) {
        selectEntry(findById(id), true, false);
        return this;
    }

    public MinecraftItemPickerWidget selectItem(Item item) {
        selectEntry(findByItem(item), true, false);
        return this;
    }

    @Override
    public MinecraftItemPickerWidget open() {
        super.open();
        return this;
    }

    @Override
    public MinecraftItemPickerWidget close() {
        super.close();
        return this;
    }

    @Override
    public MinecraftItemPickerWidget overlayLayer(OverlayLayer overlayLayer) {
        super.overlayLayer(overlayLayer);
        return this;
    }

    @Override
    @XmlAttribute(value = "query", category = "Behavior", defaultValue = "", description = "Initial search query used to filter items.")
    public MinecraftItemPickerWidget query(String query) {
        super.query(query);
        return this;
    }

    @Override
    public MinecraftItemPickerWidget clearQuery() {
        super.clearQuery();
        return this;
    }

    @Override
    @XmlAttribute(value = "selectedIndex", category = "Behavior", defaultValue = "-1", description = "Initial selected item index.")
    public MinecraftItemPickerWidget selectIndex(int index) {
        super.selectIndex(index);
        return this;
    }

    @Override
    @XmlAttribute(value = "selectedFilteredIndex", category = "Behavior", defaultValue = "-1", description = "Initial selected index in the filtered item list.")
    public MinecraftItemPickerWidget selectFilteredIndex(int filteredIndex) {
        super.selectFilteredIndex(filteredIndex);
        return this;
    }

    @Override
    public MinecraftItemPickerWidget clearSelection() {
        super.clearSelection();
        return this;
    }

    @Override
    public Optional<ItemEntry> selectedEntry() {
        return super.selectedEntry();
    }

    @Override
    public EventSubscription onSelectionChanged(EventListener<? super SelectionChangedEvent> listener) {
        return super.onSelectionChanged(listener);
    }

    @Override
    protected String entryIdText(ItemEntry entry) {
        return entry.id().toString();
    }

    @Override
    protected String searchText(ItemEntry entry) {
        return entry.id()
                + "\n"
                + entry.id().getPath()
                + "\n"
                + safeDescriptionId(entry.item());
    }

    @Override
    protected void clearSelectedPreview() {
        selectedPreview().stack(ItemStack.EMPTY);
    }

    @Override
    protected void updateSelectedPreview(ItemEntry entry) {
        selectedPreview().stack(stackFor(entry));
    }

    @Override
    protected void renderEntryIcon(RenderContext context,
                                   ItemEntry entry,
                                   float x,
                                   float y,
                                   float size,
                                   float opacity,
                                   Runnable repaint) {
        context.custom(backend -> {
            if (backend instanceof MinecraftGuiRenderBackend minecraftBackend) {
                boolean rendered = minecraftBackend.renderItemPreviewLazy(stackFor(entry), x, y, size, opacity);
                if (!rendered) {
                    repaint.run();
                }
            }
        });
    }

    @Override
    protected RichText tooltipText(ItemEntry entry) {
        return RichText.of(entry.id().toString(), MinecraftFonts.defaultFace(), TextRun.DEFAULT_PIXEL_SIZE);
    }

    private ItemEntry findById(ResourceLocation id) {
        if (id == null) return null;
        for (ItemEntry entry : entries()) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    private ItemEntry findByItem(Item item) {
        if (item == null) return null;
        for (ItemEntry entry : entries()) {
            if (entry.item() == item) {
                return entry;
            }
        }
        return null;
    }

    private ItemStack stackFor(ItemEntry entry) {
        if (entry == null) return ItemStack.EMPTY;
        return stackCache.computeIfAbsent(entry, value -> new ItemStack(value.item()));
    }

    private static String safeDescriptionId(Item item) {
        try {
            return item.getDescriptionId();
        } catch (RuntimeException | LinkageError ignored) {
            return "";
        }
    }

    public record ItemEntry(ResourceLocation id, Item item) {
        public ItemEntry {
            id = Objects.requireNonNull(id, "id");
            item = Objects.requireNonNull(item, "item");
        }

        public ItemStack stack() {
            return new ItemStack(item);
        }
    }
}


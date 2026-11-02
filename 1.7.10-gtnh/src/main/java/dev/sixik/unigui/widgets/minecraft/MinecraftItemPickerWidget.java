package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.gtnh.backend.GtnhGuiRenderBackend;
import dev.sixik.unigui.gtnh.backend.MinecraftFonts;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.SearchableGridPickerWidget;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import dev.sixik.unigui.gtnh.backend.GtnhItemStacks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Compact Minecraft item selector backed by the generic searchable grid picker.
 *
 * <p>One item can look different per metadata value (wool colors, potions, enchanted books), so the list
 * holds one entry per creative-search variant. Search matches the registry id, the path and the localized name.</p>
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
                new MinecraftItemPreviewWidget("", GtnhItemStacks.EMPTY),
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
        return selectedEntryOrNull() == null ? GtnhItemStacks.EMPTY : stackFor(selectedEntryOrNull());
    }

    /**
     * Fills the list from the item registry, one entry per creative-search variant.
     */
    public MinecraftItemPickerWidget reloadFromRegistry() {
        List<ItemEntry> registryItems = new ObjectArrayList<>();
        try {
            for (Object entry : Item.itemRegistry) {
                Item item = (Item) entry;
                String name = item == null ? null : Item.itemRegistry.getNameForObject(item);
                if (name == null) continue;
                addVariants(registryItems, item, new ResourceLocation(name));
            }
        } catch (RuntimeException | LinkageError ignored) {
            registryItems.clear();
        }
        return items(registryItems);
    }

    private static void addVariants(List<ItemEntry> entries, Item item, ResourceLocation id) {
        List<ItemStack> variants = new ArrayList<>();
        try {
            item.getSubItems(item, item.getCreativeTab(), variants);
        } catch (RuntimeException | LinkageError ignored) {
            variants.clear();
        }
        if (variants.isEmpty()) {
            entries.add(new ItemEntry(id, item));
            return;
        }
        for (ItemStack variant : variants) {
            if (GtnhItemStacks.isEmpty(variant) || variant.getItem() != item) continue;
            ItemEntry entry = new ItemEntry(id, item, variant.getItemDamage(), variant.getTagCompound());
            if (!entries.contains(entry)) entries.add(entry);
        }
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
        return entry.idText();
    }

    @Override
    protected String searchText(ItemEntry entry) {
        return entry.id()
                + "\n"
                + entry.id().getResourcePath()
                + "\n"
                + safeDisplayName(stackFor(entry));
    }

    @Override
    protected void clearSelectedPreview() {
        selectedPreview().stack(GtnhItemStacks.EMPTY);
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
            if (backend instanceof GtnhGuiRenderBackend minecraftBackend) {
                boolean rendered = minecraftBackend.renderItemPreviewLazy(stackFor(entry), x, y, size, opacity);
                if (!rendered) {
                    repaint.run();
                }
            }
        });
    }

    @Override
    protected RichText tooltipText(ItemEntry entry) {
        return RichText.of(entry.idText(), MinecraftFonts.defaultFace(), TextRun.DEFAULT_PIXEL_SIZE);
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
        if (entry == null) return GtnhItemStacks.EMPTY;
        return stackCache.computeIfAbsent(entry, ItemEntry::stack);
    }

    private static String safeDisplayName(ItemStack stack) {
        try {
            return stack.getDisplayName();
        } catch (RuntimeException | LinkageError ignored) {
            return "";
        }
    }

    /**
     * One selectable item variant.
     *
     * @param id       registry id
     * @param item     the item
     * @param metadata 1.7.10 metadata (damage) value of the variant
     * @param tag      NBT of the variant, or {@code null}
     */
    public record ItemEntry(ResourceLocation id, Item item, int metadata, NBTTagCompound tag) {
        public ItemEntry {
            id = Objects.requireNonNull(id, "id");
            item = Objects.requireNonNull(item, "item");
            tag = tag == null || tag.hasNoTags() ? null : (NBTTagCompound) tag.copy();
        }

        public ItemEntry(ResourceLocation id, Item item) {
            this(id, item, 0, null);
        }

        public ItemEntry(ResourceLocation id, Item item, int metadata) {
            this(id, item, metadata, null);
        }

        public ItemStack stack() {
            ItemStack stack = new ItemStack(item, 1, metadata);
            if (tag != null) stack.setTagCompound((NBTTagCompound) tag.copy());
            return stack;
        }

        /**
         * @return the id, with {@code @metadata} appended for variants (for example {@code minecraft:wool@5})
         */
        public String idText() {
            return metadata == 0 ? id.toString() : id + "@" + metadata;
        }
    }
}
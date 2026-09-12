package dev.sixik.isf.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

/** Клиентские hooks и небольшой безопасный recipe browser overlay. */
public final class IsfClient {
    private static final IsfBrowserOverlay OVERLAY = new IsfBrowserOverlay();

    private IsfClient() {
    }

    public static void init() {
        OVERLAY.register();
        MinecraftForge.EVENT_BUS.addListener(IsfClient::keyPressed);
        MinecraftForge.EVENT_BUS.addListener(IsfClient::screenClosed);
        MinecraftForge.EVENT_BUS.addListener(IsfClient::render);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, IsfClient::mouseScrolled);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, IsfClient::mousePressed);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, IsfClient::mouseDragged);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, IsfClient::mouseReleased);
    }

    private static void render(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        OVERLAY.updatePointerPosition(event.getMouseX(), event.getMouseY());
    }

    private static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> container)) return;
        if (event.getScreen().getFocused() instanceof EditBox) return;
        if (event.getKeyCode() == org.lwjgl.glfw.GLFW.GLFW_KEY_A) {
            // A над предметом в сетке крафта — закладка на него; иначе — на клетку списка.
            ResourceLocation recipeItem = OVERLAY.recipeItemAt();
            if (recipeItem != null) {
                boolean bookmarked = !dev.sixik.isf.client.IsfClientState.bookmarks().contains(recipeItem);
                dev.sixik.isf.network.IsfNetwork.toggleBookmark(recipeItem, bookmarked);
                event.setCanceled(true);
                return;
            }
            if (OVERLAY.toggleHoveredBookmark()) event.setCanceled(true);
            return;
        }
        if (event.getKeyCode() != org.lwjgl.glfw.GLFW.GLFW_KEY_R
                && event.getKeyCode() != org.lwjgl.glfw.GLFW.GLFW_KEY_U) {
            return;
        }

        // Координаты курсора уже трекаются в render-событии — они в тех же
        // GUI-координатах, что и работающие клики, независимо от guiScale.
        ResourceLocation itemId = OVERLAY.recipeItemAt();
        if (itemId == null) itemId = OVERLAY.hoveredItemId();
        if (itemId == null) {
            Slot slot = container.getSlotUnderMouse();
            ItemStack stack = slot == null ? ItemStack.EMPTY : slot.getItem();
            if (!stack.isEmpty()) itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        }
        if (itemId == null) return;

        if (event.getKeyCode() == org.lwjgl.glfw.GLFW.GLFW_KEY_R) {
            OVERLAY.showRecipes(itemId, false);
        } else {
            OVERLAY.showRecipes(itemId, true);
        }
        event.setCanceled(true);
    }

    private static void screenClosed(ScreenEvent.Closing event) {
        OVERLAY.resetPage();
    }

    private static void mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (event.isCanceled() || !(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        OVERLAY.updatePointerPosition(event.getMouseX(), event.getMouseY());
        if (OVERLAY.scrollItemsAt(event.getMouseX(), event.getMouseY(), event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    private static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.isCanceled() || !(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        OVERLAY.updatePointerPosition(event.getMouseX(), event.getMouseY());
        if (OVERLAY.clickDetailControls(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
            return;
        }
        if (OVERLAY.clickRecipeNavigation(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
            return;
        }
        if (OVERLAY.beginDetailDrag(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
            return;
        }
        if (OVERLAY.beginItemScrollBarDrag(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    private static void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (event.isCanceled() || !(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (OVERLAY.dragDetail(event.getMouseX(), event.getMouseY(), event.getMouseButton())) {
            event.setCanceled(true);
            return;
        }
        if (OVERLAY.dragItemScrollBar(event.getMouseY(), event.getMouseButton())) {
            event.setCanceled(true);
        }
    }

    private static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.isCanceled() || !(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (OVERLAY.endDetailDrag(event.getButton())) {
            event.setCanceled(true);
            return;
        }
        if (OVERLAY.endItemScrollBarDrag(event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }
}

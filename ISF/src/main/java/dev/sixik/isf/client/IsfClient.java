package dev.sixik.isf.client;

import dev.sixik.unigui.backend.minecraft_impl.ScreenOverlayRender;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

/** Клиентские hooks и небольшой безопасный recipe browser overlay. */
public final class IsfClient {
    private static final IsfBrowserOverlay OVERLAY = new IsfBrowserOverlay();

    private IsfClient() {
    }

    public static void init(net.minecraftforge.eventbus.api.IEventBus modEventBus) {
        if (modEventBus != null) {
            // Клиентская фабрика tooltip-компонентов — IModBusEvent, регистрируется
            // до первого тултипа с нашей сеткой моделей.
            modEventBus.addListener(IsfClient::onRegisterTooltipComponents);
        }
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

    /**
     * Регистрирует клиентскую фабрику tooltip-компонентов: маркер
     * {@link IsfAcceptsTooltipData} конвертируется в рисующий
     * {@code ClientTooltipComponent} (сетка моделей вариантов тега).
     */
    private static void onRegisterTooltipComponents(
            net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(IsfAcceptsTooltipData.class, data ->
                new IsfAcceptsTooltipData.GridComponent(data,
                        net.minecraft.client.Minecraft.getInstance().font));
    }    private static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.isCanceled()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> container)) return;
        if (event.getScreen().getFocused() instanceof EditBox) return;
        if (ScreenOverlayRender.isTextInputActive(container)) return;

        // Единое разрешение предмета под курсором через IsfItemTriggerUtils:
        // визуалы рецептов, катализаторы окна рецептов, клетки каталога/закладок
        // и, как фолбэк, слот инвентаря. KeyPressed не несёт координаты мыши —
        // берём трекаемые в render-событии (те же GUI-координаты, что у кликов).
        ResourceLocation itemId = IsfItemTriggerUtils.resolveItemAt(
                OVERLAY, container, OVERLAY.pointerX(), OVERLAY.pointerY());
        if (itemId == null) return;

        int keyCode = event.getKeyCode();
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_A) {
            if (IsfItemTriggerUtils.trigger(OVERLAY,
                    IsfItemTriggerUtils.TriggerKind.TOGGLE_BOOKMARK, itemId)) {
                event.setCanceled(true);
            }
            return;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_R) {
            IsfItemTriggerUtils.trigger(OVERLAY, IsfItemTriggerUtils.TriggerKind.CRAFTING, itemId);
            event.setCanceled(true);
        } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_U) {
            IsfItemTriggerUtils.trigger(OVERLAY, IsfItemTriggerUtils.TriggerKind.USAGES, itemId);
            event.setCanceled(true);
        }
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
        if (OVERLAY.clickItemList(event.getMouseX(), event.getMouseY(), event.getButton())) {
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

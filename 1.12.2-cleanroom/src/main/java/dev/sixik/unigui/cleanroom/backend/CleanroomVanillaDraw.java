package dev.sixik.unigui.cleanroom.backend;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Draws Minecraft's own item, entity and tooltip renderers inside the UniGUI frame.
 *
 * <p>The vanilla code expects the GUI state of a container screen: depth test on for 3D models, lighting on, its own
 * blend function, and it leaves all of that behind. Every method here sets up what vanilla needs, draws at the
 * current modelview matrix, clears the depth it wrote, and puts GL back to the UI defaults
 * ({@link CleanroomGlState#afterVanilla()}), so the next UniGUI command is unaffected. A renderer that throws (a
 * broken modded item) is logged once, the matrix stack is unwound, and the item is skipped from then on.</p>
 */
final class CleanroomVanillaDraw {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    /**
     * The size of an item sprite in vanilla GUI units.
     */
    static final float ITEM_SIZE = 16.0f;
    private static final Set<Item> BROKEN_ITEMS = Collections.newSetFromMap(new IdentityHashMap<>());

    private CleanroomVanillaDraw() {
    }

    /**
     * Draws an item the way a container slot does, scaled so the sprite covers {@code size} units.
     *
     * @param stack       stack to draw, not empty
     * @param x           left edge in the current coordinate space
     * @param y           top edge
     * @param size        edge length of the square the item fills
     * @param decorations whether to add the count and durability bar
     * @return {@code false} if the item could not be drawn
     */
    static boolean item(ItemStack stack, float x, float y, float size, boolean decorations) {
        if (stack == null || stack.isEmpty() || !(size > 0.0f) || BROKEN_ITEMS.contains(stack.getItem())) return false;
        Minecraft minecraft = Minecraft.getMinecraft();
        RenderItem renderItem = minecraft.getRenderItem();
        int matrixDepth = matrixDepth();
        GlStateManager.pushMatrix();
        try {
            float scale = size / ITEM_SIZE;
            GlStateManager.translate(x, y, 0.0f);
            // Uniform scale: GL_RESCALE_NORMAL only corrects uniform scaling, and 3D blocks are lit by their normals.
            GlStateManager.scale(scale, scale, scale);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            CleanroomGlState.clearDepth();
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            enableBackFaceCulling();
            GlStateManager.enableRescaleNormal();
            RenderHelper.enableGUIStandardItemLighting();
            renderItem.renderItemAndEffectIntoGUI(stack, 0, 0);
            if (decorations) renderItem.renderItemOverlayIntoGUI(minecraft.fontRenderer, stack, 0, 0, null);
            return true;
        } catch (RuntimeException | LinkageError e) {
            // Our own push is still on the stack and is popped in the finally block.
            failed(stack, e, matrixDepth + 1);
            return false;
        } finally {
            GlStateManager.popMatrix();
            CleanroomGlState.afterVanilla();
            CleanroomGlState.clearDepth();
        }
    }

    /**
     * Draws only the count and the durability bar of an item.
     *
     * @param stack stack to decorate
     * @param x     left edge of the item square
     * @param y     top edge of the item square
     * @param size  edge length of the square
     */
    static void overlay(ItemStack stack, float x, float y, float size) {
        if (stack == null || stack.isEmpty() || !(size > 0.0f) || BROKEN_ITEMS.contains(stack.getItem())) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        int matrixDepth = matrixDepth();
        GlStateManager.pushMatrix();
        try {
            float scale = size / ITEM_SIZE;
            GlStateManager.translate(x, y, 0.0f);
            GlStateManager.scale(scale, scale, scale);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            minecraft.getRenderItem().renderItemOverlayIntoGUI(minecraft.fontRenderer, stack, 0, 0, null);
        } catch (RuntimeException | LinkageError e) {
            failed(stack, e, matrixDepth + 1);
        } finally {
            GlStateManager.popMatrix();
            CleanroomGlState.afterVanilla();
        }
    }

    /**
     * Draws a living entity like the player model of the inventory screen.
     *
     * @param entity entity to draw
     * @param x      left edge of the square the entity is fitted into
     * @param y      top edge
     * @param size   edge length of the square
     * @param lookX  x of the point the entity looks at, relative to the square
     * @param lookY  y of that point
     * @return {@code false} if the entity could not be drawn
     */
    static boolean entity(EntityLivingBase entity, float x, float y, float size, float lookX, float lookY) {
        if (entity == null || !(size > 0.0f)) return false;
        float scale = entityScale(entity.width, entity.height, size);
        int centerX = Math.round(x + size * 0.5f);
        int feetY = Math.round(y + size * 0.95f);
        float eyeY = feetY - entity.getEyeHeight() * scale;
        int matrixDepth = matrixDepth();
        try {
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            CleanroomGlState.clearDepth();
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            enableBackFaceCulling();
            // drawEntityOnScreen turns the head toward (anchor - mouse), so pass the offset to the look-at point.
            GuiInventory.drawEntityOnScreen(centerX, feetY, Math.round(scale), centerX - (x + lookX),
                    eyeY - (y + lookY), entity);
            return true;
        } catch (RuntimeException | LinkageError e) {
            LOGGER.error("UniGUI could not draw entity {}", entity.getClass().getName(), e);
            unwind(matrixDepth);
            return false;
        } finally {
            CleanroomGlState.afterVanilla();
            CleanroomGlState.clearDepth();
        }
    }

    /**
     * Scale (pixels per block) that fits an entity of the given dimensions into a square.
     *
     * @param width  entity width in blocks
     * @param height entity height in blocks
     * @param size   edge of the square
     * @return the scale, at least 1
     */
    static float entityScale(float width, float height, float size) {
        float byHeight = size * 0.78f / Math.max(0.4f, height);
        float byWidth = size * 0.9f / Math.max(0.3f, width);
        return Math.max(1.0f, Math.min(byHeight, byWidth));
    }

    /**
     * Draws a vanilla tooltip box.
     *
     * @param lines        text lines with formatting codes
     * @param stack        item the tooltip belongs to, or empty
     * @param mouseX       anchor x; the box is placed 12 units to the right of it
     * @param mouseY       anchor y; the box is placed 12 units above it
     * @param screenWidth  width of the space the box must stay inside
     * @param screenHeight height of that space
     */
    static void tooltip(List<String> lines, ItemStack stack, int mouseX, int mouseY, int screenWidth,
                        int screenHeight) {
        if (lines == null || lines.isEmpty()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        FontRenderer font = stack != null && !stack.isEmpty() ? stack.getItem().getFontRenderer(stack) : null;
        int matrixDepth = matrixDepth();
        boolean forItem = stack != null && !stack.isEmpty();
        try {
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            if (forItem) GuiUtils.preItemToolTip(stack);
            // Forge listeners of the tooltip events may edit the list; the caller's copy stays untouched.
            GuiUtils.drawHoveringText(forItem ? stack : ItemStack.EMPTY, new ArrayList<>(lines), mouseX, mouseY,
                    Math.max(1, screenWidth), Math.max(1, screenHeight), -1,
                    font == null ? minecraft.fontRenderer : font);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.error("UniGUI could not draw a vanilla tooltip", e);
            unwind(matrixDepth);
        } finally {
            if (forItem) GuiUtils.postItemToolTip();
            CleanroomGlState.afterVanilla();
        }
    }

    /**
     * The tooltip lines vanilla shows for a stack: name in the rarity color, the rest gray.
     *
     * @param stack stack, not empty
     * @return lines, possibly empty if the item threw
     */
    static List<String> itemTooltipLines(ItemStack stack) {
        Minecraft minecraft = Minecraft.getMinecraft();
        try {
            List<String> lines = stack.getTooltip(minecraft.player, minecraft.gameSettings.advancedItemTooltips
                    ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
            for (int i = 0; i < lines.size(); i++) {
                lines.set(i, (i == 0 ? stack.getRarity().getColor() : TextFormatting.GRAY) + lines.get(i));
            }
            return lines;
        } catch (RuntimeException | LinkageError e) {
            LOGGER.error("UniGUI could not build the tooltip of {}", stack.getItem().getRegistryName(), e);
            return List.of();
        }
    }

    private static void failed(ItemStack stack, Throwable error, int matrixDepth) {
        if (BROKEN_ITEMS.add(stack.getItem())) {
            LOGGER.error("UniGUI could not draw item {}; it is skipped from now on", stack.getItem().getRegistryName(),
                    error);
        }
        unwind(matrixDepth);
    }

    /**
     * Vanilla draws GUI items with back-face culling on (the world pass leaves it enabled); the UI defaults turn it
     * off. Without it the far faces of a translucent block such as glass show through the near ones.
     */
    private static void enableBackFaceCulling() {
        GlStateManager.enableCull();
        GlStateManager.cullFace(GlStateManager.CullFace.BACK);
    }

    private static int matrixDepth() {
        return GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
    }

    /**
     * Pops matrices a failed vanilla renderer left on the modelview stack, and returns to the modelview matrix mode
     * (the enchantment glint switches to the texture matrix).
     */
    private static void unwind(int depth) {
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        while (GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) > depth) GlStateManager.popMatrix();
    }
}
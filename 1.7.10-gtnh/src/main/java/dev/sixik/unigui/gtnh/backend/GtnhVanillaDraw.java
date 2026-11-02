package dev.sixik.unigui.gtnh.backend;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

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
 * ({@link GtnhGlState#afterVanilla()}), so the next UniGUI command is unaffected. A renderer that throws (a broken
 * modded item) is logged once, the matrix stack is unwound, and the item is skipped from then on.</p>
 *
 * <p>1.7.10 differences from 1.12.2: there is no shared {@code RenderItem} on {@code Minecraft} (this class owns
 * one), items are drawn from their icon and {@code RenderBlocks} instead of baked models, and the tooltip box is
 * drawn by the protected {@code GuiScreen.drawHoveringText}, reached through a private screen object.</p>
 */
final class GtnhVanillaDraw {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    /**
     * The size of an item sprite in vanilla GUI units.
     */
    static final float ITEM_SIZE = 16.0f;
    private static final Set<Item> BROKEN_ITEMS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static RenderItem renderItem;
    private static TooltipScreen tooltipScreen;

    private GtnhVanillaDraw() {
    }

    private static RenderItem renderItem() {
        if (renderItem == null) renderItem = new RenderItem();
        return renderItem;
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
        if (GtnhItemStacks.isEmpty(stack) || !(size > 0.0f) || BROKEN_ITEMS.contains(stack.getItem())) return false;
        Minecraft minecraft = Minecraft.getMinecraft();
        RenderItem items = renderItem();
        FontRenderer font = fontOf(stack, minecraft);
        int matrixDepth = matrixDepth();
        GL11.glPushMatrix();
        try {
            float scale = size / ITEM_SIZE;
            GL11.glTranslatef(x, y, 0.0f);
            // Uniform scale: GL_RESCALE_NORMAL only corrects uniform scaling, and 3D blocks are lit by their normals.
            GL11.glScalef(scale, scale, scale);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            GtnhGlState.clearDepth();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            enableBackFaceCulling();
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.enableGUIStandardItemLighting();
            items.zLevel = 0.0f;
            items.renderItemAndEffectIntoGUI(font, minecraft.getTextureManager(), stack, 0, 0);
            if (decorations) items.renderItemOverlayIntoGUI(font, minecraft.getTextureManager(), stack, 0, 0, null);
            return true;
        } catch (RuntimeException | LinkageError e) {
            // Our own push is still on the stack and is popped in the finally block.
            failed(stack, e, matrixDepth + 1);
            return false;
        } finally {
            GL11.glPopMatrix();
            GtnhGlState.afterVanilla();
            GtnhGlState.clearDepth();
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
        if (GtnhItemStacks.isEmpty(stack) || !(size > 0.0f) || BROKEN_ITEMS.contains(stack.getItem())) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        int matrixDepth = matrixDepth();
        GL11.glPushMatrix();
        try {
            float scale = size / ITEM_SIZE;
            GL11.glTranslatef(x, y, 0.0f);
            GL11.glScalef(scale, scale, scale);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            renderItem().renderItemOverlayIntoGUI(fontOf(stack, minecraft), minecraft.getTextureManager(), stack, 0, 0,
                null);
        } catch (RuntimeException | LinkageError e) {
            failed(stack, e, matrixDepth + 1);
        } finally {
            GL11.glPopMatrix();
            GtnhGlState.afterVanilla();
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
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            GtnhGlState.clearDepth();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            enableBackFaceCulling();
            // The inventory screen turns the head toward (anchor - mouse), so pass the offset to the look-at point.
            GuiInventory.func_147046_a(centerX, feetY, Math.round(scale), centerX - (x + lookX),
                eyeY - (y + lookY), entity);
            return true;
        } catch (RuntimeException | LinkageError e) {
            LOGGER.error("UniGUI could not draw entity {}", entity.getClass().getName(), e);
            unwind(matrixDepth);
            return false;
        } finally {
            GtnhGlState.afterVanilla();
            GtnhGlState.clearDepth();
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
     * @param stack        item the tooltip belongs to, or {@code null}
     * @param mouseX       anchor x; the box is placed 12 units to the right of it
     * @param mouseY       anchor y; the box is placed 12 units above it
     * @param screenWidth  width of the space the box must stay inside
     * @param screenHeight height of that space
     */
    static void tooltip(List<String> lines, ItemStack stack, int mouseX, int mouseY, int screenWidth,
                        int screenHeight) {
        if (lines == null || lines.isEmpty()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        FontRenderer font = fontOf(stack, minecraft);
        int matrixDepth = matrixDepth();
        try {
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            if (tooltipScreen == null) tooltipScreen = new TooltipScreen();
            // The box code of the screen edits nothing in the list, but mods may: the caller's copy stays untouched.
            tooltipScreen.draw(minecraft, new ArrayList<>(lines), mouseX, mouseY, Math.max(1, screenWidth),
                Math.max(1, screenHeight), font);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.error("UniGUI could not draw a vanilla tooltip", e);
            unwind(matrixDepth);
        } finally {
            GtnhGlState.afterVanilla();
        }
    }

    /**
     * The tooltip lines vanilla shows for a stack: name in the rarity color, the rest gray.
     *
     * @param stack stack, not empty
     * @return lines, possibly empty if the item threw
     */
    @SuppressWarnings("unchecked")
    static List<String> itemTooltipLines(ItemStack stack) {
        Minecraft minecraft = Minecraft.getMinecraft();
        try {
            List<String> lines = new ArrayList<>(stack.getTooltip(minecraft.thePlayer,
                minecraft.gameSettings.advancedItemTooltips));
            for (int i = 0; i < lines.size(); i++) {
                lines.set(i, (i == 0 ? stack.getRarity().rarityColor : EnumChatFormatting.GRAY) + lines.get(i));
            }
            return lines;
        } catch (RuntimeException | LinkageError e) {
            LOGGER.error("UniGUI could not build the tooltip of {}", Item.itemRegistry.getNameForObject(stack.getItem()),
                e);
            return List.of();
        }
    }

    private static FontRenderer fontOf(ItemStack stack, Minecraft minecraft) {
        FontRenderer font = GtnhItemStacks.isEmpty(stack) ? null : stack.getItem().getFontRenderer(stack);
        return font == null ? minecraft.fontRenderer : font;
    }

    private static void failed(ItemStack stack, Throwable error, int matrixDepth) {
        if (BROKEN_ITEMS.add(stack.getItem())) {
            LOGGER.error("UniGUI could not draw item {}; it is skipped from now on",
                Item.itemRegistry.getNameForObject(stack.getItem()), error);
        }
        unwind(matrixDepth);
    }

    /**
     * Vanilla draws GUI items with back-face culling on (the world pass leaves it enabled); the UI defaults turn it
     * off. Without it the far faces of a translucent block such as glass show through the near ones.
     */
    private static void enableBackFaceCulling() {
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
    }

    private static int matrixDepth() {
        return GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
    }

    /**
     * Pops matrices a failed vanilla renderer left on the modelview stack, and returns to the modelview matrix mode
     * (the enchantment glint switches to the texture matrix).
     */
    private static void unwind(int depth) {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        while (GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) > depth) GL11.glPopMatrix();
    }

    /**
     * A screen that is never shown: 1.7.10 only has the tooltip box as the protected
     * {@code GuiScreen.drawHoveringText(List, int, int, FontRenderer)}, so a subclass reaches it.
     */
    private static final class TooltipScreen extends GuiScreen {
        void draw(Minecraft minecraft, List<String> lines, int x, int y, int screenWidth, int screenHeight,
                  FontRenderer font) {
            this.mc = minecraft;
            this.width = screenWidth;
            this.height = screenHeight;
            this.fontRendererObj = minecraft.fontRenderer;
            drawHoveringText(lines, x, y, font);
        }
    }
}

package se.mickelus.tetra.items.toolbelt;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import se.mickelus.tetra.TetraMod;

import java.io.IOException;

@SideOnly(Side.CLIENT)
public class GuiToolbelt extends GuiContainer {

    private static GuiToolbelt instance;

    public static final String textureLocation = "textures/gui/toolbelt-inventory.png";
    private static final ResourceLocation INVENTORY_TEXTURE = new ResourceLocation(TetraMod.MOD_ID, textureLocation);


    public GuiToolbelt(ContainerToolbelt container) {
        super(container);
        this.allowUserInput = false;
        this.xSize = 175;
        this.ySize = 176;
    }

    /**
     * Draw the foreground layer for the GuiContainer (everything in front of the items)
     *
     * @param mouseX Mouse x coordinate
     * @param mouseY Mouse y coordinate
     */
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
//        this.fontRendererObj.drawString(this.lowerChestInventory.getDisplayName().getUnformattedText(), 8, 6, 4210752);
//        this.fontRendererObj.drawString(this.upperChestInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 96 + 2, 4210752);
    }

    /**
     * Draws the background layer of this container (behind the items).
     *
     * @param partialTicks How far into the current tick the game is, with 0.0 being the start of the tick and 1.0 being
     * the end.
     * @param mouseX Mouse x coordinate
     * @param mouseY Mouse y coordinate
     */
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(INVENTORY_TEXTURE);
        int i = (width - xSize) / 2;
        int j = (height - ySize) / 2;
        this.drawTexturedModalRect(i, j, 0, 0, xSize, ySize);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // todo: clear shadow slot if rightclick and "normal" slot is empty
    }
}

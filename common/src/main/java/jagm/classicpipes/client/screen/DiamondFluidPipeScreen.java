package jagm.classicpipes.client.screen;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.menu.DiamondFluidPipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class DiamondFluidPipeScreen extends FluidFilterScreen<DiamondFluidPipeMenu> {

    private static final Identifier BACKGROUND = MiscUtil.identifier("textures/gui/container/diamond_pipe.png");

    public DiamondFluidPipeScreen(DiamondFluidPipeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 236;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - 176) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        for (int i = 0; i < 6; i++) {
            Component text = Component.translatable("direction." + ClassicPipes.MOD_ID + "." + Direction.from3DDataValue(i).name().toLowerCase()).withStyle(ChatFormatting.BLACK);
            graphics.drawString(this.font, text, this.leftPos - 30 + (35 - this.font.width(text)) / 2, this.topPos + 22 + 18 * i, -12566464, false);
        }
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int i = (this.width - 176) / 2 - 32;
        int j = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, i, j, 0.0F, 0.0F, this.imageWidth + 32, this.imageHeight, 256, 256);
    }

}

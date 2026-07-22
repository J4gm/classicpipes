package jagm.classicpipes.client.screen;

import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.menu.FluidFilterMenu;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.FluidWithData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public abstract class FluidFilterScreen<T extends FluidFilterMenu> extends FilterScreen<T> {

    public FluidFilterScreen(T menu, Inventory playerInventory, Component title, int imageWidth, int imageHeight) {
        super(menu, playerInventory, title, imageWidth, imageHeight);
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        if (slot.container instanceof Filter && slot.hasItem() && Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            FluidWithData fluid = Services.LOADER_SERVICE.getFluidFromStack(slot.getItem());
            if (!fluid.isBlank()) {
                FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.getFluid().defaultFluidState());
                int tint = fluidModel.tintSource() == null ? -1 : fluidModel.tintSource().colorInWorld(Blocks.AIR.defaultBlockState(), Minecraft.getInstance().level, Minecraft.getInstance().player.blockPosition());
                TextureAtlasSprite sprite = fluidModel.stillMaterial().sprite();
                graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, ARGB.opaque(tint));
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, slot.x, slot.y, 16, 16, tint);
                return;
            }
        }
        super.extractSlot(graphics, slot, mouseX, mouseY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            if (this.menu.getCarried().isEmpty() || stack.getTooltipImage().map(ClientTooltipComponent::create).map(ClientTooltipComponent::showTooltipWithItemInHand).orElse(false)) {
                if (this.hoveredSlot.container instanceof Filter) {
                    FluidWithData fluid = Services.LOADER_SERVICE.getFluidFromStack(stack);
                    if (!fluid.isBlank()) {
                        graphics.setTooltipForNextFrame(this.font, Services.LOADER_SERVICE.getFluidName(fluid), mouseX, mouseY);
                        return;
                    }
                }
                graphics.setTooltipForNextFrame(this.font, this.getTooltipFromContainerItem(stack), stack.getTooltipImage(), mouseX, mouseY, stack.get(DataComponents.TOOLTIP_STYLE));
            }
        }
    }

}

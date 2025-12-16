package jagm.classicpipes.client.screen;

import jagm.classicpipes.client.renderer.FluidRenderInfo;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.menu.FluidFilterMenu;
import jagm.classicpipes.services.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public abstract class FluidFilterScreen<T extends FluidFilterMenu> extends FilterScreen<T> {

    public FluidFilterScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot, int x, int y) {
        if (slot.container instanceof Filter && slot.hasItem()) {
            Fluid fluid = Services.LOADER_SERVICE.getFluidFromStack(slot.getItem());
            if (fluid != null) {
                FluidRenderInfo info = Services.LOADER_SERVICE.getFluidRenderInfo(fluid.defaultFluidState());
                graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, ARGB.opaque(info.tint()));
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, info.sprite(), slot.x, slot.y, 16, 16, info.tint());
                return;
            }
        }
        super.renderSlot(graphics, slot, x, y);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            if (this.menu.getCarried().isEmpty() || stack.getTooltipImage().map(ClientTooltipComponent::create).map(ClientTooltipComponent::showTooltipWithItemInHand).orElse(false)) {
                if (this.hoveredSlot.container instanceof Filter) {
                    Fluid fluid = Services.LOADER_SERVICE.getFluidFromStack(stack);
                    if (fluid != null) {
                        graphics.setTooltipForNextFrame(this.font, Services.LOADER_SERVICE.getFluidName(fluid), mouseX, mouseY);
                        return;
                    }
                }
                graphics.setTooltipForNextFrame(this.font, this.getTooltipFromContainerItem(stack), stack.getTooltipImage(), mouseX, mouseY, stack.get(DataComponents.TOOLTIP_STYLE));
            }
        }
    }

}

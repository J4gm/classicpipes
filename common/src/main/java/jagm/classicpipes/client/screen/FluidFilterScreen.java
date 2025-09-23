package jagm.classicpipes.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import jagm.classicpipes.client.renderer.FluidRenderInfo;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.menu.FluidFilterMenu;
import jagm.classicpipes.services.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.joml.Matrix4f;

public abstract class FluidFilterScreen<T extends FluidFilterMenu> extends FilterScreen<T> {

    public FluidFilterScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (slot.container instanceof Filter && slot.hasItem()) {
            Fluid fluid = Services.LOADER_SERVICE.getFluidFromStack(slot.getItem());
            if (fluid != null) {
                FluidRenderInfo info = Services.LOADER_SERVICE.getFluidRenderInfo(fluid.defaultFluidState());
                graphics.fill(RenderType.gui(), slot.x, slot.y, slot.x + 16, slot.y + 16, info.tint() | 0xFF000000);
                RenderSystem.setShaderTexture(0, info.sprite().atlasLocation());
                RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                RenderSystem.enableBlend();
                Matrix4f matrix4f = graphics.pose().last().pose();
                BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                float x1 = slot.x;
                float y1 = slot.y;
                float x2 = slot.x + 16;
                float y2 = slot.y + 16;
                float minU = info.sprite().getU0();
                float maxU = info.sprite().getU1();
                float minV = info.sprite().getV0();
                float maxV = info.sprite().getV1();
                bufferbuilder.addVertex(matrix4f, x1, y1, 0).setUv(minU, minV).setColor(info.tint());
                bufferbuilder.addVertex(matrix4f, x1, y2, 0).setUv(minU, maxV).setColor(info.tint());
                bufferbuilder.addVertex(matrix4f, x2, y2, 0).setUv(maxU, maxV).setColor(info.tint());
                bufferbuilder.addVertex(matrix4f, x2, y1, 0).setUv(maxU, minV).setColor(info.tint());
                BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
                RenderSystem.disableBlend();
                //graphics.blitSprite(info.sprite().atlasLocation(), slot.x, slot.y, 16, 16);
                return;
            }
        }
        super.renderSlot(graphics, slot);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            if (this.menu.getCarried().isEmpty()) {
                if (this.hoveredSlot.container instanceof Filter) {
                    Fluid fluid = Services.LOADER_SERVICE.getFluidFromStack(stack);
                    if (fluid != null) {
                        graphics.renderTooltip(this.font, Services.LOADER_SERVICE.getFluidName(fluid), mouseX, mouseY);
                        return;
                    }
                }
                graphics.renderTooltip(this.font, this.getTooltipFromContainerItem(stack), stack.getTooltipImage(), mouseX, mouseY);
            }
        }
    }

}

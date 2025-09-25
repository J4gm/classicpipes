package jagm.classicpipes.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
import jagm.classicpipes.client.renderer.FluidRenderInfo;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.menu.FluidFilterMenu;
import jagm.classicpipes.services.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBg(graphics, partialTicks, mouseX, mouseY);
        RenderSystem.disableDepthTest();
        graphics.pose().pushPose();
        graphics.pose().translate(this.leftPos, this.topPos, 0.0F);
        this.hoveredSlot = null;
        for(int k = 0; k < this.menu.slots.size(); ++k) {
            Slot slot = this.menu.slots.get(k);
            if (slot.isActive()) {
                if (slot.container instanceof Filter) {
                    this.renderFluidSlot(graphics, slot);
                } else {
                    this.renderItemSlot(graphics, slot);
                }
            }
            if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && slot.isActive()) {
                this.hoveredSlot = slot;
                if (this.hoveredSlot.isHighlightable()) {
                    renderSlotHighlight(graphics, slot.x, slot.y, 0);
                }
            }
        }
        this.renderLabels(graphics, mouseX, mouseY);
        ItemStack stack = this.menu.getCarried();
        if (!stack.isEmpty()) {
            int x = mouseX - this.leftPos - 8;
            int y = mouseY - this.topPos - 8;
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 232.0F);
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(this.font, stack, x, y, null);
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
        RenderSystem.enableDepthTest();
    }

    private void renderFluidSlot(GuiGraphics graphics, Slot slot) {
        ItemStack stack = slot.getItem();
        boolean renderFluid = true;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);
        if (stack.isEmpty()) {
            Pair<ResourceLocation, ResourceLocation> pair = slot.getNoItemIcon();
            if (pair != null && this.minecraft != null) {
                TextureAtlasSprite sprite = this.minecraft.getTextureAtlas(pair.getFirst()).apply(pair.getSecond());
                graphics.blit(slot.x, slot.y, 0, 16, 16, sprite);
                renderFluid = false;
            }
        }
        if (renderFluid) {
            Fluid fluid = Services.LOADER_SERVICE.getFluidFromStack(slot.getItem());
            if (fluid != null) {
                FluidRenderInfo info = Services.LOADER_SERVICE.getFluidRenderInfo(fluid.defaultFluidState());
                graphics.fill(RenderType.gui(), slot.x, slot.y, slot.x + 16, slot.y + 16, info.tint() | 0xFF000000);
                RenderSystem.setShaderTexture(0, info.sprite().atlasLocation());
                RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                RenderSystem.enableBlend();
                Matrix4f matrix4f = graphics.pose().last().pose();
                BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
                float x1 = slot.x;
                float y1 = slot.y;
                float x2 = slot.x + 16;
                float y2 = slot.y + 16;
                float minU = info.sprite().getU0();
                float maxU = info.sprite().getU1();
                float minV = info.sprite().getV0();
                float maxV = info.sprite().getV1();
                bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                bufferbuilder.vertex(matrix4f, x1, y1, 0).uv(minU, minV).color(info.tint()).endVertex();
                bufferbuilder.vertex(matrix4f, x1, y2, 0).uv(minU, maxV).color(info.tint()).endVertex();
                bufferbuilder.vertex(matrix4f, x2, y2, 0).uv(maxU, maxV).color(info.tint()).endVertex();
                bufferbuilder.vertex(matrix4f, x2, y1, 0).uv(maxU, minV).color(info.tint()).endVertex();
                BufferUploader.drawWithShader(bufferbuilder.end());
                RenderSystem.disableBlend();
            }
        }
        graphics.pose().popPose();
    }

    private void renderItemSlot(GuiGraphics graphics, Slot slot) {
        ItemStack stack = slot.getItem();
        boolean renderItem = true;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);
        if (stack.isEmpty()) {
            Pair<ResourceLocation, ResourceLocation> pair = slot.getNoItemIcon();
            if (pair != null && this.minecraft != null) {
                TextureAtlasSprite sprite = this.minecraft.getTextureAtlas(pair.getFirst()).apply(pair.getSecond());
                graphics.blit(slot.x, slot.y, 0, 16, 16, sprite);
                renderItem = false;
            }
        }
        if (renderItem) {
            graphics.renderItem(stack, slot.x, slot.y, slot.x + slot.y * this.imageWidth);
            graphics.renderItemDecorations(this.font, stack, slot.x, slot.y, null);
        }
        graphics.pose().popPose();
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

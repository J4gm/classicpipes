package jagm.classicpipes.client.screen;

import jagm.classicpipes.inventory.menu.ProviderPipeMenu;
import jagm.classicpipes.network.ServerBoundLeaveOnePayload;
import jagm.classicpipes.network.ServerBoundMatchComponentsPayload;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ProviderPipeScreen extends AbstractContainerScreen<ProviderPipeMenu> {

    private static final ResourceLocation BACKGROUND = MiscUtil.resourceLocation("textures/gui/container/netherite_pipe.png");

    public ProviderPipeScreen(ProviderPipeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(SmallerCheckbox.builder()
                .pos(this.leftPos + 8, this.topPos + 38)
                .onValueChange(this::matchComponentsCheckboxChanged)
                .tooltip(Tooltip.create(Component.translatable("tooltip.classicpipes.match_components")))
                .selected(this.getMenu().getFilter().shouldMatchComponents())
                .label(Component.translatable("widget.classicpipes.match_components"), this.font)
                .build()
        );
        this.addRenderableWidget(SmallerCheckbox.builder()
                .pos(this.leftPos + 8, this.topPos + 54)
                .onValueChange(this::leaveOneCheckboxChanged)
                .tooltip(Tooltip.create(Component.translatable("tooltip.classicpipes.leave_one")))
                .selected(this.getMenu().shouldLeaveOne())
                .label(Component.translatable("widget.classicpipes.leave_one"), this.font)
                .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, float f) {
        super.render(graphics, x, y, f);
        this.renderTooltip(graphics, x, y);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float f, int x, int y) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

    private void matchComponentsCheckboxChanged(SmallerCheckbox checkbox, boolean checked) {
        Services.LOADER_SERVICE.sendToServer(new ServerBoundMatchComponentsPayload(checked));
    }

    private void leaveOneCheckboxChanged(SmallerCheckbox checkbox, boolean checked) {
        Services.LOADER_SERVICE.sendToServer(new ServerBoundLeaveOnePayload(checked));
    }

}

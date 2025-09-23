package jagm.classicpipes.client.screen;

import jagm.classicpipes.client.screen.widget.SmallerCheckbox;
import jagm.classicpipes.inventory.menu.StockingPipeMenu;
import jagm.classicpipes.network.ServerBoundActiveStockingPayload;
import jagm.classicpipes.network.ServerBoundMatchComponentsPayload;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class StockingPipeScreen extends FilterScreen<StockingPipeMenu> {

    private static final ResourceLocation BACKGROUND = MiscUtil.resourceLocation("textures/gui/container/networked_pipe.png");

    public StockingPipeScreen(StockingPipeMenu menu, Inventory playerInventory, Component title) {
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
                .onValueChange(this::activeStockingCheckboxChanged)
                .tooltip(Tooltip.create(Component.translatable("tooltip.classicpipes.active_stocking")))
                .selected(this.getMenu().isActiveStocking())
                .label(Component.translatable("widget.classicpipes.active_stocking"), this.font)
                .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        graphics.blit(BACKGROUND, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

    private void matchComponentsCheckboxChanged(SmallerCheckbox checkbox, boolean checked) {
        Services.LOADER_SERVICE.sendToServer(new ServerBoundMatchComponentsPayload(checked));
    }

    private void activeStockingCheckboxChanged(SmallerCheckbox checkbox, boolean checked) {
        Services.LOADER_SERVICE.sendToServer(new ServerBoundActiveStockingPayload(checked));
    }

}

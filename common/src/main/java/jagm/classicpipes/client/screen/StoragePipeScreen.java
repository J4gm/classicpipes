package jagm.classicpipes.client.screen;

import jagm.classicpipes.client.screen.widget.SmallerCheckbox;
import jagm.classicpipes.inventory.menu.StoragePipeMenu;
import jagm.classicpipes.network.ServerBoundDefaultRoutePayload;
import jagm.classicpipes.network.ServerBoundLeaveOnePayload;
import jagm.classicpipes.network.ServerBoundMatchComponentsPayload;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class StoragePipeScreen extends AbstractContainerScreen<StoragePipeMenu> {

    private static final Identifier BACKGROUND = MiscUtil.identifier("textures/gui/container/storage_pipe.png");

    public StoragePipeScreen(StoragePipeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(SmallerCheckbox.builder()
                .pos(this.leftPos + 8, this.topPos + 20)
                .onValueChange(this::defaultRouteCheckboxChanged)
                .tooltip(Tooltip.create(Component.translatable("tooltip.classicpipes.default_route")))
                .selected(this.getMenu().isDefaultRoute())
                .label(Component.translatable("widget.classicpipes.default_route"), this.font)
                .build()
        );
        this.addRenderableWidget(SmallerCheckbox.builder()
                .pos(this.leftPos + 8, this.topPos + 38)
                .onValueChange(this::matchComponentsCheckboxChanged)
                .tooltip(Tooltip.create(Component.translatable("tooltip.classicpipes.match_components_alt")))
                .selected(this.getMenu().shouldMatchComponents())
                .label(Component.translatable("widget.classicpipes.match_components"), this.font)
                .build()
        );
        this.addRenderableWidget(SmallerCheckbox.builder()
                .pos(this.leftPos + 8, this.topPos + 56)
                .onValueChange(this::leaveOneCheckboxChanged)
                .tooltip(Tooltip.create(Component.translatable("tooltip.classicpipes.leave_one")))
                .selected(this.getMenu().shouldLeaveOne())
                .label(Component.translatable("widget.classicpipes.leave_one"), this.font)
                .build()
        );
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

    private void matchComponentsCheckboxChanged(SmallerCheckbox checkbox, boolean checked) {
        Services.LOADER_SERVICE.sendToServer(new ServerBoundMatchComponentsPayload(checked));
    }

    private void defaultRouteCheckboxChanged(SmallerCheckbox checkbox, boolean checked) {
        Services.LOADER_SERVICE.sendToServer(new ServerBoundDefaultRoutePayload(checked));
    }

    private void leaveOneCheckboxChanged(SmallerCheckbox checkbox, boolean checked) {
        Services.LOADER_SERVICE.sendToServer(new ServerBoundLeaveOnePayload(checked));
    }

}

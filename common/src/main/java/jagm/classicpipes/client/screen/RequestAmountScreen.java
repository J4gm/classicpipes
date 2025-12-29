package jagm.classicpipes.client.screen;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.client.screen.widget.IncreaseButton;
import jagm.classicpipes.network.ServerBoundRequestPayload;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

import static jagm.classicpipes.block.NetworkedPipeBlock.ENABLED;

public class RequestAmountScreen extends Screen {

    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 96;
    private static final int ITEM_X = 62;
    private static final int ITEM_Y = 36;
    private static final int MAX_REQUEST = 999;
    private static final Identifier BACKGROUND = MiscUtil.identifier("textures/gui/container/request_amount.png");

    private final RequestScreen previousScreen;
    private final ItemStack stack;
    private final boolean craftable;
    private int leftPos;
    private int topPos;
    private int count;
    private IncreaseButton increase;
    private IncreaseButton decrease;

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected RequestAmountScreen(ItemStack stack, RequestScreen previousScreen, boolean craftable) {
        super(Component.translatable("container." + ClassicPipes.MOD_ID + ".request.amount"));
        this.stack = stack;
        this.previousScreen = previousScreen;
        this.craftable = craftable;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;
        this.count = 1;
        this.increase = new IncreaseButton(this.leftPos + ITEM_X + 38, this.topPos + 26, false, this.stack.getCount() > 1 || this.craftable, button -> this.changeCount(this.minecraft.hasShiftDown() ? 10 : 1));
        this.decrease = new IncreaseButton(this.leftPos + ITEM_X + 38, this.topPos + 54, true, false, button -> this.changeCount(this.minecraft.hasShiftDown() ? -10 : -1));
        this.addRenderableWidget(Button.builder(Component.translatable("widget." + ClassicPipes.MOD_ID + ".cancel"), button -> this.onClose()).bounds(this.leftPos + 16, this.topPos + 72, 70, 16).build());
        this.addRenderableWidget(Button.builder(Component.translatable("widget." + ClassicPipes.MOD_ID + ".request"), button -> {
            Services.LOADER_SERVICE.sendToServer(new ServerBoundRequestPayload(this.stack.copyWithCount(this.count), this.previousScreen.getMenu().getRequestPos()));
            this.previousScreen.onClose();
        }).bounds(this.leftPos + 90, this.topPos + 72, 70, 16).build());
        this.addRenderableWidget(this.increase);
        this.addRenderableWidget(this.decrease);
    }

    private void changeCount(int increment) {
        this.count += increment;
        if (this.count < 1) {
            this.count = 1;
        }
        if (this.count > this.stack.getCount() && !this.craftable) {
            this.count = this.stack.getCount();
        }
        if (this.count > MAX_REQUEST) {
            this.count = MAX_REQUEST;
        }
        this.decrease.active = this.count > 1;
        this.increase.active = this.count < this.stack.getCount() || (this.craftable && this.count < MAX_REQUEST);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (this.previousScreen.getMenu().requestPipe != null && !this.previousScreen.getMenu().requestPipe.getBlockState().getValue(ENABLED)) {
            this.onClose();
        }
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.leftPos, this.topPos);
        graphics.renderItem(this.stack, ITEM_X, ITEM_Y);
        if (this.stack.isBarVisible()) {
            int i = ITEM_X + 2;
            int j = ITEM_Y + 13;
            graphics.fill(RenderPipelines.GUI, i, j, i + 13, j + 2, -16777216);
            graphics.fill(RenderPipelines.GUI, i, j, i + stack.getBarWidth(), j + 1, ARGB.opaque(stack.getBarColor()));
        }
        graphics.drawString(this.font, this.title, (IMAGE_WIDTH - this.font.width(this.title)) / 2, 6, -12566464, false);
        Component countComponent = Component.literal(String.valueOf(this.count));
        graphics.drawString(this.font, countComponent, ITEM_X + 45 - this.font.width(countComponent) / 2, ITEM_Y + 4, -12566464, false);
        if (this.isHovering(ITEM_X, ITEM_Y, 16, 16, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, getTooltipFromItem(this.minecraft, this.stack), this.stack.getTooltipImage(), mouseX, mouseY, this.stack.get(DataComponents.TOOLTIP_STYLE));
        }
        graphics.pose().popMatrix();
    }

    private boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        mouseX -= this.leftPos;
        mouseY -= this.topPos;
        return mouseX >= (double)(x - 1) && mouseX < (double)(x + width + 1) && mouseY >= (double)(y - 1) && mouseY < (double)(y + height + 1);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderTransparentBackground(graphics);
        int i = (this.width - IMAGE_WIDTH) / 2;
        int j = (this.height - IMAGE_HEIGHT) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, i, j, 0.0F, 0.0F, IMAGE_WIDTH, IMAGE_HEIGHT, 256, 256);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.previousScreen);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        } else if (this.minecraft.options.keyInventory.matches(event)) {
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.changeCount((int) (this.minecraft.hasShiftDown() ? 10 * scrollY : scrollY));
        return true;
    }

}

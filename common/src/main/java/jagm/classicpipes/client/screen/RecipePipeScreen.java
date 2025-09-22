package jagm.classicpipes.client.screen;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.menu.RecipePipeMenu;
import jagm.classicpipes.network.ServerBoundSlotDirectionPayload;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RecipePipeScreen extends FilterScreen<RecipePipeMenu> {

    private static final ResourceLocation BACKGROUND = MiscUtil.resourceLocation("textures/gui/container/recipe_pipe.png");
    private static final ChatFormatting[] DIRECTION_COLOURS = new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE, ChatFormatting.GREEN, ChatFormatting.YELLOW, ChatFormatting.BLUE, ChatFormatting.GRAY, ChatFormatting.RED};

    private final Button[] slotDirectionButtons;
    private boolean buttonsNeedUpdate;

    public RecipePipeScreen(RecipePipeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.slotDirectionButtons = new Button[10];
        this.imageHeight = 171;
        this.inventoryLabelY = this.imageHeight - 94;
        this.buttonsNeedUpdate = true;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int slot = j + i * 3;
                Direction slotDirection = this.menu.getSlotDirection(slot);
                this.slotDirectionButtons[slot] = Button.builder(
                        Component.translatable("direction." + ClassicPipes.MOD_ID + ".short." + slotDirection.name().toLowerCase()).withStyle(DIRECTION_COLOURS[slotDirection.get3DDataValue()]),
                        button -> this.cycleSlotDirection(slot)
                )
                        .bounds(this.leftPos + 9 + j * 12, this.topPos + 25 + i * 12, 12, 12)
                        .build();
            }
        }
        Direction slotDirection = this.menu.getSlotDirection(9);
        this.slotDirectionButtons[9] = Button.builder(
                Component.translatable("direction." + ClassicPipes.MOD_ID + ".short." + slotDirection.name().toLowerCase()).withStyle(DIRECTION_COLOURS[slotDirection.get3DDataValue()]),
                button -> this.cycleSlotDirection(9)
        )
                .bounds(this.leftPos + 149, this.topPos + 37, 12, 12)
                .build();
        for (Button button : this.slotDirectionButtons) {
            this.addRenderableWidget(button);
        }
        this.buttonsNeedUpdate = true;
    }

    private void cycleSlotDirection(int slot) {
        Direction newDirection = hasShiftDown() ? this.menu.prevDirection(this.menu.getSlotDirection(slot)) : this.menu.nextDirection(this.menu.getSlotDirection(slot));
        this.menu.setSlotDirection(slot, newDirection);
        this.buttonsNeedUpdate = true;
        Services.LOADER_SERVICE.sendToServer(new ServerBoundSlotDirectionPayload(this.menu.getPos(), slot, newDirection));
    }

    private Tooltip createDirectionTooltip(Direction direction, boolean result) {
        return Tooltip.create(Component.translatable(
                "tooltip." + ClassicPipes.MOD_ID + (result ? ".recipe_pipe_result" : ".recipe_pipe_grid"),
                Component.translatable("direction." + ClassicPipes.MOD_ID + "." + direction.name().toLowerCase())
                        .withStyle(DIRECTION_COLOURS[direction.get3DDataValue()])
        ));
    }

    private void updateButtons() {
        for (int slot = 0; slot < 10; slot++) {
            boolean active = this.menu.slotHasItem(slot) && this.menu.hasAvailableDirections();
            Direction direction = this.menu.getSlotDirection(slot);
            this.slotDirectionButtons[slot].active = active;
            this.slotDirectionButtons[slot].setMessage(active ? Component.translatable("direction." + ClassicPipes.MOD_ID + ".short." + direction.name().toLowerCase()).withStyle(DIRECTION_COLOURS[direction.get3DDataValue()]) : Component.empty());
            this.slotDirectionButtons[slot].setTooltip(active ? createDirectionTooltip(direction, slot == 9) : null);
        }
        this.setFocused(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (this.buttonsNeedUpdate) {
            this.updateButtons();
            this.buttonsNeedUpdate = false;
        }
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void containerTick() {
        this.buttonsNeedUpdate = true;
        super.containerTick();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2 - 9, this.titleLabelY, -12566464, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderType::guiTextured, BACKGROUND, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.buttonsNeedUpdate = true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.buttonsNeedUpdate = true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

}

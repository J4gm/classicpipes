package jagm.classicpipes.client.screen;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.client.screen.widget.PageButton;
import jagm.classicpipes.inventory.menu.RequestMenu;
import jagm.classicpipes.network.ServerBoundRequestPayload;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.MiscUtil;
import jagm.classicpipes.util.SortingMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;

public class RequestScreen extends AbstractContainerScreen<RequestMenu> {

    private static final ResourceLocation BACKGROUND = MiscUtil.resourceLocation("textures/gui/container/request.png");
    private static final WidgetSprites X_BUTTON = new WidgetSprites(MiscUtil.resourceLocation("widget/x"), MiscUtil.resourceLocation("widget/x_hovered"));
    private static final ResourceLocation SLOT_HIGHLIGHT_BACK_SPRITE = ResourceLocation.withDefaultNamespace("container/slot_highlight_back");
    private static final ResourceLocation SLOT_HIGHLIGHT_FRONT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot_highlight_front");

    private EditBox searchBar;
    private PageButton prev_page;
    private PageButton next_page;
    private Button sort_type;
    private Button sort_direction;
    private boolean refocus;

    public RequestScreen(RequestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 222;
        this.refocus = false;
    }

    @Override
    protected void init() {

        super.init();

        this.searchBar = new EditBox(this.font, this.leftPos + 21, this.topPos + 22, 146, 12, Component.translatable("container.classicpipes.search"));
        this.searchBar.setCanLoseFocus(true);
        this.searchBar.setMaxLength(32);
        this.searchBar.setValue(this.menu.getSearch());
        this.searchBar.setTextColor(-1);
        this.searchBar.setTextColorUneditable(-1);
        this.searchBar.setBordered(false);
        this.searchBar.setResponder(this.menu::setSearch);
        this.searchBar.setEditable(true);
        this.prev_page = new PageButton(this.width / 2 - 32 - 8, this.topPos + 180, true, false, button -> this.changePage(-1));
        this.next_page = new PageButton(this.width / 2 + 32, this.topPos + 180, false, this.menu.getMaxPage() > 0, button -> this.changePage(1));
        this.sort_type = Button.builder(this.menu.getSortingMode().getType(), this::changeSortType).bounds(this.width / 2 - 25, this.topPos + 198, 50, 16).build();
        this.sort_direction = Button.builder(this.menu.getSortingMode().getDirection(), this::changeSortDirection).bounds(this.width / 2 + 27, this.topPos + 198, 50, 16).build();

        this.addRenderableWidget(this.sort_type);
        this.addRenderableWidget(this.sort_direction);
        this.addRenderableWidget(this.searchBar);
        this.addRenderableWidget(this.prev_page);
        this.addRenderableWidget(this.next_page);
        this.addRenderableWidget(new ImageButton(this.leftPos + this.imageWidth - 12, this.topPos + 5, 7, 7, X_BUTTON, button -> this.onClose()));

    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.updatePageButtons();
        this.renderContents(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderContents(graphics, mouseX, mouseY, partialTicks);
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) this.leftPos, (float) this.topPos);
        this.hoveredSlot = this.getHoveredSlot(mouseX, mouseY);
        if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
        }
        this.renderSlots(graphics);
        if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
        }
        graphics.pose().popMatrix();
    }

    private Slot getHoveredSlot(double mouseX, double mouseY) {
        Iterator<Slot> var5 = this.menu.displaySlots.iterator();
        Slot slot;
        do {
            if (!var5.hasNext()) {
                return null;
            }
            slot = var5.next();
        } while(!slot.isActive() || !this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY));
        return slot;
    }

    @Override
    protected void renderSlots(GuiGraphics graphics) {
        for (Slot slot : this.menu.displaySlots) {
            if (slot.isActive()) {
                this.renderSlot(graphics, slot);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float f, int x, int y) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        Component pageIndicator = Component.translatable("widget." + ClassicPipes.MOD_ID + ".page", this.menu.getPage() + 1, this.menu.getMaxPage() + 1);
        graphics.drawString(this.font, pageIndicator, (this.imageWidth - this.font.width(pageIndicator)) / 2, 182, -12566464, false);
        Component sortBy = Component.translatable("widget." + ClassicPipes.MOD_ID + ".sort_by");
        graphics.drawString(this.font, sortBy, this.imageWidth / 2 - 29 - this.font.width(sortBy), 202, -12566464, false);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        ItemStack stack = slot.getItem();
        int seed = slot.x + slot.y * this.imageWidth;
        graphics.renderItem(stack, slot.x, slot.y, seed);
        if (!stack.isEmpty()) {
            graphics.pose().pushMatrix();
            this.renderItemBar(graphics, stack, slot.x, slot.y);
            this.renderItemCount(graphics, this.font, stack, slot.x, slot.y, this.menu.itemCraftable(stack));
            graphics.pose().popMatrix();
        }
    }

    private void renderItemBar(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack.isBarVisible()) {
            int i = x + 2;
            int j = y + 13;
            graphics.fill(RenderPipelines.GUI, i, j, i + 13, j + 2, -16777216);
            graphics.fill(RenderPipelines.GUI, i, j, i + stack.getBarWidth(), j + 1, ARGB.opaque(stack.getBarColor()));
        }
    }

    private void renderItemCount(GuiGraphics graphics, Font font, ItemStack stack, int x, int y, boolean craftable) {
        int count = stack.getCount() - (craftable ? 1 : 0);
        String s = stringForCount(count);
        int colour = colourForCount(count);
        float countScale = 1.0F;
        if (this.minecraft != null) {
            final int guiScale = this.minecraft.getWindow().getGuiScale();
            int numerator = guiScale;
            while (font.width(s) * countScale > 16 && numerator > 1) {
                numerator--;
                countScale = (float) numerator / guiScale;
            }
        }
        int slotOffset = countScale == 1.0F ? 17 : 16;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + slotOffset, y + slotOffset);
        graphics.pose().scale(countScale);
        graphics.drawString(font, s, -font.width(s), -8, colour, true);
        graphics.pose().popMatrix();
    }

    private static int colourForCount(int count) {
        return count == 0 ? -256 : -1;
    }

    private static String stringForCount(int count) {
        if (count == 0) {
            return "+";
        } else if (count < 1000) {
            return String.valueOf(count);
        } else if (count < 10000) {
            return String.format("%.1f", (float) count / 1000 - 0.049F) + "K";
        } else if (count < 1000000) {
            return count / 1000 + "K";
        } else if (count < 10000000) {
            return String.format("%.1f", (float) count / 1000000 - 0.049F) + "M";
        } else if (count < 1000000000) {
            return count / 1000000 + "M";
        } else if (count < Integer.MAX_VALUE) {
            return String.format("%.1f", (float) count / 1000000000 - 0.049F) + "B";
        } else {
            return "MAX";
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Slot slot = this.getHoveredSlot(event.x(), event.y());
        if (slot != null) {
            ItemStack toRequest = slot.getItem();
            if (!toRequest.isEmpty()) {
                boolean craftable = this.menu.itemCraftable(toRequest);
                if (event.hasShiftDown() || event.button() == 1) {
                    int amount = event.hasShiftDown() ? Math.min(toRequest.getCount() - (craftable ? 1 : 0), toRequest.getMaxStackSize()) : 1;
                    if (amount > 0) {
                        Services.LOADER_SERVICE.sendToServer(new ServerBoundRequestPayload(toRequest.copyWithCount(amount), this.menu.getRequestPos()));
                        toRequest.shrink(amount);
                        this.menu.update();
                    }
                } else if (this.minecraft != null) {
                    this.minecraft.setScreen(new RequestAmountScreen(toRequest, this, craftable));
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.searchBar);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.input() == 256 && this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
        return this.searchBar.keyPressed(event) || this.searchBar.canConsumeInput() || super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.menu.changePage((int) -scrollY);
        return true;
    }

    private void changePage(int increment) {
        this.menu.changePage(increment);
        this.refocus = true;
    }

    private void updatePageButtons() {
        this.prev_page.active = this.menu.getPage() > 0;
        this.next_page.active = this.menu.getPage() < this.menu.getMaxPage();
        if (this.refocus) {
            this.setFocused(this.searchBar);
            this.refocus = false;
        }
    }

    private void changeSortType(Button button) {
        SortingMode nextMode = this.minecraft != null && this.minecraft.hasShiftDown() ? this.menu.getSortingMode().prevType() : this.menu.getSortingMode().nextType();
        this.sort_type.setMessage(nextMode.getType());
        this.sort_direction.setMessage(nextMode.getDirection());
        this.menu.setSortingMode(nextMode);
        this.refocus = true;
    }

    private void changeSortDirection(Button button) {
        SortingMode nextMode = this.menu.getSortingMode().otherDirection();
        this.sort_type.setMessage(nextMode.getType());
        this.sort_direction.setMessage(nextMode.getDirection());
        this.menu.setSortingMode(nextMode);
        this.refocus = true;
    }

}

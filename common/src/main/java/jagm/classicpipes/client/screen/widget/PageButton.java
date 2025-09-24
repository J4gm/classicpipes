package jagm.classicpipes.client.screen.widget;

import jagm.classicpipes.util.MiscUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;

public class PageButton extends Button {

    private static final ResourceLocation PREV_GREYED = MiscUtil.resourceLocation("textures/gui/sprites/widget/prev_page_greyed.png");
    private static final ResourceLocation PREV_NORMAL = MiscUtil.resourceLocation("textures/gui/sprites/widget/prev_page.png");
    private static final ResourceLocation PREV_SELECT = MiscUtil.resourceLocation("textures/gui/sprites/widget/prev_page_hovered.png");
    private static final ResourceLocation NEXT_GREYED = MiscUtil.resourceLocation("textures/gui/sprites/widget/next_page_greyed.png");
    private static final ResourceLocation NEXT_NORMAL = MiscUtil.resourceLocation("textures/gui/sprites/widget/next_page.png");
    private static final ResourceLocation NEXT_SELECT = MiscUtil.resourceLocation("textures/gui/sprites/widget/next_page_hovered.png");

    private final boolean prev;

    public PageButton(int x, int y, boolean prev, boolean active, OnPress onPress) {
        super(x, y, 8, 12, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.prev = prev;
        this.active = active;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation sprite = this.prev ?
                (!this.active ? PREV_GREYED : (this.isHovered() ? PREV_SELECT : PREV_NORMAL)) :
                (!this.active ? NEXT_GREYED : (this.isHovered() ? NEXT_SELECT : NEXT_NORMAL));
        this.renderTexture(graphics, sprite, this.getX(), this.getY(), 0, 0, this.height, this.width, this.height, this.width, this.height);
    }

}

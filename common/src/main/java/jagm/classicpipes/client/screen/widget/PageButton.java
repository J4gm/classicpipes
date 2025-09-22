package jagm.classicpipes.client.screen.widget;

import jagm.classicpipes.util.MiscUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;

public class PageButton extends Button {

    private static final ResourceLocation PREV_GREYED = MiscUtil.resourceLocation("widget/prev_page_greyed");
    private static final ResourceLocation PREV_NORMAL = MiscUtil.resourceLocation("widget/prev_page");
    private static final ResourceLocation PREV_SELECT = MiscUtil.resourceLocation("widget/prev_page_hovered");
    private static final ResourceLocation NEXT_GREYED = MiscUtil.resourceLocation("widget/next_page_greyed");
    private static final ResourceLocation NEXT_NORMAL = MiscUtil.resourceLocation("widget/next_page");
    private static final ResourceLocation NEXT_SELECT = MiscUtil.resourceLocation("widget/next_page_hovered");

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
        graphics.blitSprite(RenderType::guiTextured, sprite, this.getX(), this.getY(), this.width, this.height);
    }

}

package jagm.classicpipes.client.screen.widget;

import jagm.classicpipes.util.MiscUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;

public class PageButton extends Button {

    private static final Identifier PREV_GREYED = MiscUtil.identifier("widget/prev_page_greyed");
    private static final Identifier PREV_NORMAL = MiscUtil.identifier("widget/prev_page");
    private static final Identifier PREV_SELECT = MiscUtil.identifier("widget/prev_page_hovered");
    private static final Identifier NEXT_GREYED = MiscUtil.identifier("widget/next_page_greyed");
    private static final Identifier NEXT_NORMAL = MiscUtil.identifier("widget/next_page");
    private static final Identifier NEXT_SELECT = MiscUtil.identifier("widget/next_page_hovered");

    private final boolean prev;

    public PageButton(int x, int y, boolean prev, boolean active, OnPress onPress) {
        super(x, y, 8, 12, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.prev = prev;
        this.active = active;
    }

    @Override
    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        Identifier sprite = this.prev ?
                (!this.active ? PREV_GREYED : (this.isHovered() ? PREV_SELECT : PREV_NORMAL)) :
                (!this.active ? NEXT_GREYED : (this.isHovered() ? NEXT_SELECT : NEXT_NORMAL));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);
    }

}

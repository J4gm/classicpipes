package jagm.classicpipes.client.screen.widget;

import jagm.classicpipes.util.MiscUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;

public class IncreaseButton extends Button {

    private static final ResourceLocation INCREASE_GREYED = MiscUtil.resourceLocation("widget/increase_greyed");
    private static final ResourceLocation INCREASE_NORMAL = MiscUtil.resourceLocation("widget/increase");
    private static final ResourceLocation INCREASE_SELECT = MiscUtil.resourceLocation("widget/increase_hovered");
    private static final ResourceLocation DECREASE_GREYED = MiscUtil.resourceLocation("widget/decrease_greyed");
    private static final ResourceLocation DECREASE_NORMAL = MiscUtil.resourceLocation("widget/decrease");
    private static final ResourceLocation DECREASE_SELECT = MiscUtil.resourceLocation("widget/decrease_hovered");

    private final boolean decrease;

    public IncreaseButton(int x, int y, boolean decrease, boolean active, Button.OnPress onPress) {
        super(x, y, 12, 8, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.decrease = decrease;
        this.active = active;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation sprite = this.decrease ?
                (!this.active ? DECREASE_GREYED : (this.isHovered() ? DECREASE_SELECT : DECREASE_NORMAL)) :
                (!this.active ? INCREASE_GREYED : (this.isHovered() ? INCREASE_SELECT : INCREASE_NORMAL));
        graphics.blitSprite(sprite, this.getX(), this.getY(), this.width, this.height);
    }

}

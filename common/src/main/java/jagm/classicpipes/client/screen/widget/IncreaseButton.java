package jagm.classicpipes.client.screen.widget;

import jagm.classicpipes.util.MiscUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;

public class IncreaseButton extends Button {

    private static final Identifier INCREASE_GREYED = MiscUtil.identifier("widget/increase_greyed");
    private static final Identifier INCREASE_NORMAL = MiscUtil.identifier("widget/increase");
    private static final Identifier INCREASE_SELECT = MiscUtil.identifier("widget/increase_hovered");
    private static final Identifier DECREASE_GREYED = MiscUtil.identifier("widget/decrease_greyed");
    private static final Identifier DECREASE_NORMAL = MiscUtil.identifier("widget/decrease");
    private static final Identifier DECREASE_SELECT = MiscUtil.identifier("widget/decrease_hovered");

    private final boolean decrease;

    public IncreaseButton(int x, int y, boolean decrease, boolean active, Button.OnPress onPress) {
        super(x, y, 12, 8, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.decrease = decrease;
        this.active = active;
    }

    @Override
    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        Identifier sprite = this.decrease ?
                (!this.active ? DECREASE_GREYED : (this.isHovered() ? DECREASE_SELECT : DECREASE_NORMAL)) :
                (!this.active ? INCREASE_GREYED : (this.isHovered() ? INCREASE_SELECT : INCREASE_NORMAL));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);
    }

}

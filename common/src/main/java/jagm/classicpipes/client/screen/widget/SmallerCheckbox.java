package jagm.classicpipes.client.screen.widget;

import jagm.classicpipes.util.MiscUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;

public class SmallerCheckbox extends AbstractButton {

    private static final ResourceLocation CHECKBOX_SELECTED_SPRITE = MiscUtil.resourceLocation("widget/checkbox_selected");
    private static final ResourceLocation CHECKBOX_SPRITE = MiscUtil.resourceLocation("widget/checkbox");
    public static final int SIZE = 12;

    private boolean selected;
    private final SmallerCheckbox.OnValueChange onValueChange;
    private final Component label;
    private final Font font;

    public SmallerCheckbox(int x, int y, SmallerCheckbox.OnValueChange onValueChange, boolean selected, Component label, Font font) {
        super(x, y, 0, 0, CommonComponents.EMPTY);
        this.width = SIZE + (font != null ? 2 + font.width(label) : 0);
        this.height = SIZE;
        this.selected = selected;
        this.onValueChange = onValueChange;
        this.label = label;
        this.font = font;
    }

    public static SmallerCheckbox.Builder builder() {
        return new SmallerCheckbox.Builder();
    }

    @Override
    public void onPress() {
        this.selected = !this.selected;
        this.onValueChange.onValueChange(this, this.selected);
    }

    public boolean selected() {
        return this.selected;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
        if (this.active) {
            if (this.isFocused()) {
                narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.checkbox.usage.focused"));
            } else {
                narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.checkbox.usage.hovered"));
            }
        }

    }

    @Override
    public void renderWidget(GuiGraphics graphics, int x, int y, float f) {
        ResourceLocation spriteLocation = this.selected() ? CHECKBOX_SELECTED_SPRITE : CHECKBOX_SPRITE;
        graphics.blitSprite(RenderType::guiTextured, spriteLocation, this.getX(), this.getY(), SIZE, SIZE, ARGB.white(this.alpha));
        graphics.drawString(this.font, this.label, this.getX() + SIZE + 2, this.getY() + 3, -12566464, false);
    }

    public interface OnValueChange {
        SmallerCheckbox.OnValueChange NOP = (checkbox, value) -> {};
        void onValueChange(SmallerCheckbox checkbox, boolean value);
    }

    public static class Builder {

        private int x = 0;
        private int y = 0;
        private SmallerCheckbox.OnValueChange onValueChange;
        private Tooltip tooltip;
        private boolean selected = false;
        private Component label;
        private Font font;

        Builder() {
            this.onValueChange = SmallerCheckbox.OnValueChange.NOP;
            this.tooltip = null;
            this.label = null;
            this.font = null;
        }

        public SmallerCheckbox.Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public SmallerCheckbox.Builder onValueChange(SmallerCheckbox.OnValueChange onValueChange) {
            this.onValueChange = onValueChange;
            return this;
        }

        public SmallerCheckbox.Builder tooltip(Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public SmallerCheckbox.Builder selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        public SmallerCheckbox.Builder label(Component label, Font font) {
            this.label = label;
            this.font = font;
            return this;
        }

        public SmallerCheckbox build() {
            SmallerCheckbox checkbox = new SmallerCheckbox(this.x, this.y, this.onValueChange, this.selected, this.label, this.font);
            checkbox.setTooltip(this.tooltip);
            return checkbox;
        }
    }

}

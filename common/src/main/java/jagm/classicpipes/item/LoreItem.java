package jagm.classicpipes.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;

public class LoreItem extends Item {

    private final Component[] lore;

    public LoreItem(Properties properties, Component... lore) {
        super(properties);
        this.lore = lore;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.addAll(Arrays.asList(this.lore));
    }

}

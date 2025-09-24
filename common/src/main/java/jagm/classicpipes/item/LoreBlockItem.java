package jagm.classicpipes.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.List;

public class LoreBlockItem extends BlockItem {

    private final Component[] lore;

    public LoreBlockItem(Block block, Properties properties, Component... lore) {
        super(block, properties);
        this.lore = lore;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.addAll(Arrays.asList(this.lore));
    }

}

package jagm.classicpipes.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ticks.ContainerSingleItem;

public class DummyContainer implements ContainerSingleItem {

    @Override
    public ItemStack getTheItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void setTheItem(ItemStack stack) {}

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

}

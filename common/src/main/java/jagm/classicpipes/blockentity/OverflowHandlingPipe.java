package jagm.classicpipes.blockentity;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public interface OverflowHandlingPipe {

    void markCannotFit(ItemStack stack, Direction direction);

    boolean itemCanFit(ItemStack stack);

}

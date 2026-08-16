package jagm.classicpipes.blockentity;

import net.minecraft.world.item.ItemStack;

public interface MatchingPipe extends OverflowHandlingPipe {

    boolean matches(ItemStack stack);

    NetworkedPipeEntity getAsPipe();

    void updateCache();

}

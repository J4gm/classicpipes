package jagm.classicpipes.blockentity;

import net.minecraft.world.item.ItemStack;

public interface MatchingPipe {

    boolean matches(ItemStack stack);

    NetworkedPipeEntity getAsPipe();

    void markCannotFit(ItemStack stack);

    void updateCache();

}

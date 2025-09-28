package jagm.classicpipes.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface ProviderPipe {

    List<ItemStack> getCache();

    boolean extractItem(ServerLevel level, ItemStack stack);

    BlockPos getProviderPipePos();

    void updateCache();

    Direction getFacing();

}

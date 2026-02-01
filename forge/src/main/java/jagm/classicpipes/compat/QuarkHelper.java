package jagm.classicpipes.compat;

import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.util.ItemInPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.violetmoon.quark.content.automation.block.CrafterBlock;
import org.violetmoon.quark.content.automation.block.be.CrafterBlockEntity;

public class QuarkHelper {

    public static boolean isCrafter(BlockEntity container) {
        return container instanceof CrafterBlockEntity;
    }

    public static void setCrafterSlotState(BlockEntity container, int slot, boolean state) {
        if (container instanceof CrafterBlockEntity crafter) {
            crafter.blocked[slot] = !state;
        }
    }

    public static void crafterCraft(ServerLevel level, BlockEntity container) {
        if (container instanceof CrafterBlockEntity crafter) {
            crafter.update();
            ItemStack stack = crafter.result.getItem(0);
            if (!stack.isEmpty()) {
                Direction direction = crafter.getBlockState().getValue(CrafterBlock.FACING);
                BlockPos pipePos = crafter.getBlockPos().relative(direction);
                BlockEntity blockEntity = level.getBlockEntity(pipePos);
                if (blockEntity instanceof ItemPipeEntity pipe) {
                    pipe.insertPipeItem(level, new ItemInPipe(stack, direction.getOpposite(), direction));
                    level.sendBlockUpdated(pipePos, pipe.getBlockState(), pipe.getBlockState(), 2);
                }
                crafter.takeItems();
                crafter.update();
            }
        }
    }

}

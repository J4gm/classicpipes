package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.ItemInPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FlintPipeEntity extends RoundRobinPipeEntity {

    public FlintPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.FLINT_PIPE_ENTITY, pos, state);
    }

    @Override
    public void eject(ServerLevel level, BlockPos pos, ItemInPipe item) {
        DispenseItemBehavior dispenseBehavior = getDispenser(level, item.getStack());
        if (dispenseBehavior == DispenseItemBehavior.NOOP) {
            super.eject(level, pos, item);
        } else {
            BlockState state = Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, item.getTargetDirection());
            DispenserBlockEntity fakeDispenser = new DispenserBlockEntity(pos, state);
            BlockSource blockSource = new BlockSource() {

                @Override
                public double x() {
                    return pos.getX();
                }

                @Override
                public double y() {
                    return pos.getY();
                }

                @Override
                public double z() {
                    return pos.getZ();
                }

                @Override
                public BlockPos getPos() {
                    return pos;
                }

                @Override
                public BlockState getBlockState() {
                    return state;
                }

                @Override
                public DispenserBlockEntity getEntity() {
                    return fakeDispenser;
                }

                @Override
                public ServerLevel getLevel() {
                    return level;
                }

            };
            ItemStack leftover = dispenseBehavior.dispense(blockSource, item.getStack());
            if (!leftover.isEmpty()) {
                item.setStack(leftover);
                item.setTargetDirection(item.getFromDirection());
                item.setEjecting(false);
                this.queued.add(item);
            }
        }
    }

    private static DispenseItemBehavior getDispenser(Level level, ItemStack stack) {
        DispenseItemBehavior defaultBehavior = new DefaultDispenseItemBehavior();
        return !stack.isItemEnabled(level.enabledFeatures()) ? defaultBehavior : Services.LOADER_SERVICE.getDispenserBehaviour(stack);
    }

}

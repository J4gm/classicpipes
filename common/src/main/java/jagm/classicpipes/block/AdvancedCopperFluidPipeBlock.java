package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.AdvancedCopperFluidPipeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AdvancedCopperFluidPipeBlock extends CopperFluidPipeBlock {

    public AdvancedCopperFluidPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedCopperFluidPipeEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_ENTITY ? AdvancedCopperFluidPipeEntity::tick : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pipePos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isCrouching()) {
            if (super.use(state, level, pipePos, player, hand, hitResult).equals(InteractionResult.SUCCESS)) {
                return InteractionResult.SUCCESS;
            }
        }
        if (level instanceof ServerLevel && level.getBlockEntity(pipePos) instanceof AdvancedCopperFluidPipeEntity pipe) {
            player.openMenu(pipe);
        }
        return InteractionResult.SUCCESS;
    }

}

package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.MatchingPipeEntity;
import jagm.classicpipes.network.ClientBoundBoolPayload;
import jagm.classicpipes.services.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MatchingPipeBlock extends ContainerAdjacentNetworkedPipeBlock {

    public MatchingPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatchingPipeEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ClassicPipes.MATCHING_PIPE_ENTITY ? MatchingPipeEntity::tick : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pipePos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (super.use(state, level, pipePos, player, hand, hitResult).equals(InteractionResult.SUCCESS)) {
            return InteractionResult.SUCCESS;
        } else if (level instanceof ServerLevel && level.getBlockEntity(pipePos) instanceof MatchingPipeEntity matchingPipe) {
            Services.LOADER_SERVICE.openMenu(
                    (ServerPlayer) player,
                    matchingPipe,
                    new ClientBoundBoolPayload(matchingPipe.shouldMatchComponents()),
                    ClientBoundBoolPayload.HANDLER
            );
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        Direction facing = state.getValue(FACING).getDirection();
        if (level instanceof ServerLevel serverLevel && facing != null && level.getBlockEntity(pos) instanceof MatchingPipeEntity matchingPipe && neighbor.equals(pos.relative(facing))) {
            matchingPipe.updateCache(serverLevel, pos, facing);
        }
    }

}

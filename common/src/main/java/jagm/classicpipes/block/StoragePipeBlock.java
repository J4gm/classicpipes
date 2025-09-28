package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.StoragePipeEntity;
import jagm.classicpipes.network.ClientBoundThreeBoolsPayload;
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

public class StoragePipeBlock extends ContainerAdjacentNetworkedPipeBlock {

    public StoragePipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StoragePipeEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ClassicPipes.STORAGE_PIPE_ENTITY ? StoragePipeEntity::tick : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pipePos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (super.use(state, level, pipePos, player, hand, hitResult).equals(InteractionResult.SUCCESS)) {
            return InteractionResult.SUCCESS;
        } else if (level instanceof ServerLevel && level.getBlockEntity(pipePos) instanceof StoragePipeEntity storagePipe) {
            Services.LOADER_SERVICE.openMenu(
                    (ServerPlayer) player,
                    storagePipe,
                    new ClientBoundThreeBoolsPayload(storagePipe.isDefaultRoute(), storagePipe.shouldMatchComponents(), storagePipe.shouldLeaveOne()),
                    ClientBoundThreeBoolsPayload.HANDLER
            );
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        Direction facing = state.getValue(FACING).getDirection();
        if (level instanceof ServerLevel && facing != null && level.getBlockEntity(pos) instanceof StoragePipeEntity storagePipe && neighbor.equals(pos.relative(facing))) {
            storagePipe.updateCache();
        }
    }

}

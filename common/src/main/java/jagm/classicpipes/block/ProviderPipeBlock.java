package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.ProviderPipe;
import jagm.classicpipes.blockentity.ProviderPipeEntity;
import jagm.classicpipes.network.ClientBoundTwoBoolsPayload;
import jagm.classicpipes.services.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ProviderPipeBlock extends ContainerAdjacentNetworkedPipeBlock {

    public ProviderPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProviderPipeEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ClassicPipes.PROVIDER_PIPE_ENTITY ? ProviderPipeEntity::tick : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (super.useWithoutItem(state, level, pos, player, hitResult).equals(InteractionResult.SUCCESS)) {
            return InteractionResult.SUCCESS;
        } else if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof ProviderPipeEntity providerPipe) {
            Services.LOADER_SERVICE.openMenu(
                    (ServerPlayer) player,
                    providerPipe,
                    new ClientBoundTwoBoolsPayload(providerPipe.getFilter().getItemStacksForPayload(), providerPipe.shouldMatchComponents(), providerPipe.shouldLeaveOne()),
                    ClientBoundTwoBoolsPayload.STREAM_CODEC
            );
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        Direction facing = state.getValue(FACING).getDirection();
        if (level instanceof ServerLevel && facing != null && level.getBlockEntity(pos) instanceof ProviderPipeEntity providerPipe && neighbor.equals(pos.relative(facing))) {
            providerPipe.updateCache();
        }
    }

    @Override
    protected void checkPoweredState(Level level, BlockPos pos, BlockState state) {
        boolean powered = level.hasNeighborSignal(pos);
        level.setBlock(pos, state.setValue(ENABLED, !powered), 2);

        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof ProviderPipe providerPipe) {
            providerPipe.updateCache();
        }
    }

}

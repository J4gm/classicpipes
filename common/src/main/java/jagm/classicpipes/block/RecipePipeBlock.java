package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.RecipePipeEntity;
import jagm.classicpipes.network.ClientBoundRecipePipePayload;
import jagm.classicpipes.services.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RecipePipeBlock extends NetworkedPipeBlock {

    public RecipePipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RecipePipeEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ClassicPipes.RECIPE_PIPE_ENTITY ? RecipePipeEntity::tick : null;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
            if (blockEntity instanceof RecipePipeEntity craftingPipe) {
                craftingPipe.dropHeldItems(serverLevel, pos);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof RecipePipeEntity recipePipe) {
            Services.LOADER_SERVICE.openMenu(
                    (ServerPlayer) player,
                    recipePipe,
                    new ClientBoundRecipePipePayload(recipePipe.getSlotDirections(), recipePipe.getDirectionsForButtons(state), pos, recipePipe.isBlockingMode()),
                    ClientBoundRecipePipePayload.STREAM_CODEC
            );
        }
        return InteractionResult.SUCCESS;
    }

}

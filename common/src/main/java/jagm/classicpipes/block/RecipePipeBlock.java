package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.RecipePipeEntity;
import jagm.classicpipes.network.ClientBoundRecipePipePayload;
import jagm.classicpipes.services.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
            if (blockEntity instanceof RecipePipeEntity craftingPipe) {
                craftingPipe.dropHeldItems(serverLevel, pos);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pipePos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level instanceof ServerLevel && level.getBlockEntity(pipePos) instanceof RecipePipeEntity recipePipe) {
            Services.LOADER_SERVICE.openMenu(
                    (ServerPlayer) player,
                    recipePipe,
                    new ClientBoundRecipePipePayload(recipePipe.getSlotDirections(), recipePipe.getDirectionsForButtons(state), pipePos, recipePipe.isBlockingMode()),
                    ClientBoundRecipePipePayload.HANDLER
            );
        }
        return InteractionResult.SUCCESS;
    }

}

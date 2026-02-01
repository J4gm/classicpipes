package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.AdvancedCopperPipeEntity;
import jagm.classicpipes.network.ClientBoundBoolPayload;
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

public class AdvancedCopperPipeBlock extends CopperPipeBlock {

    public AdvancedCopperPipeBlock(Properties properties, boolean inverted) {
        super(properties, inverted);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedCopperPipeEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ClassicPipes.ADVANCED_COPPER_PIPE_ENTITY ? AdvancedCopperPipeEntity::tick : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pipePos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isCrouching()) {
            if (super.use(state, level, pipePos, player, hand, hitResult).equals(InteractionResult.SUCCESS)) {
                return InteractionResult.SUCCESS;
            }
        }
        if (level instanceof ServerLevel && level.getBlockEntity(pipePos) instanceof AdvancedCopperPipeEntity pipe) {
            Services.LOADER_SERVICE.openMenu(
                    (ServerPlayer) player,
                    pipe,
                    new ClientBoundBoolPayload(pipe.shouldMatchComponents()),
                    ClientBoundBoolPayload.HANDLER
            );
        }
        return InteractionResult.SUCCESS;
    }

}

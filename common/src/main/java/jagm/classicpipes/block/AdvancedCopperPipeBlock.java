package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.AdvancedCopperPipeEntity;
import jagm.classicpipes.network.ClientBoundBoolPayload;
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isCrouching()) {
            if (super.useWithoutItem(state, level, pos, player, hitResult).equals(InteractionResult.SUCCESS)) {
                return InteractionResult.SUCCESS;
            }
        }
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof AdvancedCopperPipeEntity pipe) {
            Services.LOADER_SERVICE.openMenu(
                    (ServerPlayer) player,
                    pipe,
                    new ClientBoundBoolPayload(pipe.getFilter().getItemStacksForPayload(), pipe.shouldMatchComponents()),
                    ClientBoundBoolPayload.STREAM_CODEC
            );
        }
        return InteractionResult.SUCCESS;
    }

}

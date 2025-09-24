package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.DiamondPipeEntity;
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

public class DiamondPipeBlock extends BooleanDirectionsPipeBlock {

    public DiamondPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DiamondPipeEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ClassicPipes.DIAMOND_PIPE_ENTITY ? DiamondPipeEntity::tick : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pipePos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level instanceof ServerLevel && level.getBlockEntity(pipePos) instanceof DiamondPipeEntity diamondPipe) {
            Services.LOADER_SERVICE.openMenu((ServerPlayer) player, diamondPipe, new ClientBoundBoolPayload(diamondPipe.shouldMatchComponents()), ClientBoundBoolPayload.HANDLER);
        }
        return InteractionResult.SUCCESS;
    }

}

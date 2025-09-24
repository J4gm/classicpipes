package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.blockentity.LapisPipeEntity;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.FacingOrNone;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class LapisPipeBlock extends BooleanDirectionsPipeBlock {

    public static final EnumProperty<FacingOrNone> FACING = FacingOrNone.BLOCK_PROPERTY;

    public LapisPipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, FacingOrNone.NONE));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LapisPipeEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ClassicPipes.LAPIS_PIPE_ENTITY ? LapisPipeEntity::tick : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState superState = super.getStateForPlacement(context);
        if (superState != null) {
            for (Direction direction : Direction.values()) {
                if (this.isPipeConnected(superState, direction) && Services.LOADER_SERVICE.canAccessContainer(context.getLevel(), context.getClickedPos().relative(direction), direction.getOpposite())) {
                    return superState.trySetValue(FACING, FacingOrNone.with(direction));
                }
            }
            return superState.trySetValue(FACING, FacingOrNone.NONE);
        }
        return this.defaultBlockState();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction initialDirection, BlockState neighborState, LevelAccessor level, BlockPos pipePos, BlockPos neighborPos) {
        BlockState superState = super.updateShape(state, initialDirection, neighborState, level, pipePos, neighborPos);
        Direction direction = superState.getValue(FACING) == FacingOrNone.NONE ? Direction.DOWN : superState.getValue(FACING).getDirection();
        for (int i = 0; i < 6; i++) {
            if (this.isPipeConnected(superState, direction) && Services.LOADER_SERVICE.canAccessContainer((Level) level, pipePos.relative(direction), direction.getOpposite())) {
                return superState.setValue(FACING, FacingOrNone.with(direction));
            }
            direction = MiscUtil.nextDirection(direction);
        }
        return superState.setValue(FACING, FacingOrNone.NONE);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pipePos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.getAbilities().mayBuild && !MiscUtil.itemIsPipe(player.getMainHandItem()) && state.getValue(FACING) != FacingOrNone.NONE) {
            Direction direction = MiscUtil.nextDirection(state.getValue(FACING).getDirection());
            for (int i = 0; i < 5; i++) {
                if (this.isPipeConnected(state, direction) && Services.LOADER_SERVICE.canAccessContainer(level, pipePos.relative(direction), direction.getOpposite())) {
                    level.setBlock(pipePos, state.setValue(FACING, FacingOrNone.with(direction)), 3);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.playSound(null, pipePos, ClassicPipes.PIPE_ADJUST_SOUND, SoundSource.BLOCKS);
                    }
                    if (level.getBlockEntity(pipePos) instanceof ItemPipeEntity pipe) {
                        for (ItemInPipe item : pipe.getContents()) {
                            if (item.getProgress() < ItemInPipe.HALFWAY) {
                                pipe.routeItem(state, item);
                            }
                        }
                        pipe.addQueuedItems(level, false);
                        pipe.setChanged();
                    }
                    return InteractionResult.SUCCESS;
                }
                direction = MiscUtil.nextDirection(direction);
            }
        }
        return InteractionResult.PASS;
    }

}

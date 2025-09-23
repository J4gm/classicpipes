package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.IronPipeEntity;
import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class IronPipeBlock extends BooleanDirectionsPipeBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    public IronPipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, Direction.DOWN)
                .setValue(ENABLED, false)
        );
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IronPipeEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ClassicPipes.IRON_PIPE_ENTITY ? IronPipeEntity::tick : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, ENABLED);
    }

    private Direction getSecondaryDirection(Direction primaryDirection, BlockState state) {
        if (this.isPipeConnected(state, primaryDirection.getOpposite())) {
            return primaryDirection.getOpposite();
        }
        return primaryDirection;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState superState = super.getStateForPlacement(context);
        if (superState != null) {
            for (Direction direction : Direction.values()) {
                if (this.isPipeConnected(superState, direction)) {
                    return superState.trySetValue(FACING, direction);
                }
            }
            return superState;
        }
        return this.defaultBlockState();
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction initialDirection, BlockState neighborState, LevelAccessor level, BlockPos pipePos, BlockPos neighborPos) {
        BlockState superState = super.updateShape(state, initialDirection, neighborState, level, pipePos, neighborPos);
        Direction direction = superState.getValue(FACING);
        for (int i = 0; i < 6; i++) {
            if (this.isPipeConnected(superState, direction)) {
                return superState.setValue(FACING, direction);
            }
            direction = MiscUtil.nextDirection(direction);
        }
        return superState;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.getAbilities().mayBuild && !MiscUtil.itemIsPipe(player.getMainHandItem())) {
            Direction direction = MiscUtil.nextDirection(state.getValue(FACING));
            for (int i = 0; i < 5; i++) {
                if (this.isPipeConnected(state, direction)) {
                    level.setBlock(pos, state.setValue(FACING, direction), 3);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.playSound(null, pos, ClassicPipes.PIPE_ADJUST_SOUND, SoundSource.BLOCKS);
                    }
                    if (level.getBlockEntity(pos) instanceof ItemPipeEntity pipe) {
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

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            this.checkPoweredState(level, pos, state);
        }
    }

    private void checkPoweredState(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(ENABLED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.setValue(ENABLED, false).setValue(FACING, this.getSecondaryDirection(state.getValue(FACING), state)), 2);
        } else if (!state.getValue(ENABLED) && level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.setValue(ENABLED, true).setValue(FACING, this.getSecondaryDirection(state.getValue(FACING), state)), 2);
        }
        if (level.getBlockEntity(pos) instanceof ItemPipeEntity pipe) {
            for (ItemInPipe item : pipe.getContents()) {
                if (item.getProgress() < ItemInPipe.HALFWAY) {
                    pipe.routeItem(state, item);
                }
            }
            pipe.addQueuedItems(level, false);
            pipe.setChanged();
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean b) {
        this.checkPoweredState(level, pos, state);
    }

}

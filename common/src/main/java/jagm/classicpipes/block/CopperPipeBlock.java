package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.CopperPipeEntity;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.FacingOrNone;
import jagm.classicpipes.util.MiscUtil;
import jagm.classicpipes.blockentity.PipeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

public class CopperPipeBlock extends BooleanDirectionsPipeBlock {

    public static final EnumProperty<FacingOrNone> FACING = FacingOrNone.BLOCK_PROPERTY;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    private final boolean inverted;

    public CopperPipeBlock(Properties properties, boolean inverted) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, FacingOrNone.NONE)
                .setValue(ENABLED, false)
        );
        this.inverted = inverted;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperPipeEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ClassicPipes.COPPER_PIPE_ENTITY ? CopperPipeEntity::tick : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, ENABLED);
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
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pipePos, Direction initialDirection, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        // Queue a tick update after the neighbor has been updated
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.scheduleTick(pipePos, this, 1);
        }
        BlockState superState = super.updateShape(state, level, scheduledTickAccess, pipePos, initialDirection, neighborPos, neighborState, random);
        Direction direction = state.getValue(FACING) == FacingOrNone.NONE ? Direction.DOWN : state.getValue(FACING).getDirection();
        for (int i = 0; i < 6; i++) {
            if (this.isPipeConnected(superState, direction) && Services.LOADER_SERVICE.canAccessContainer((Level) level, pipePos.relative(direction), direction.getOpposite())) {
                return superState.setValue(FACING, FacingOrNone.with(direction));
            }
            direction = MiscUtil.nextDirection(direction);
        }
        return superState.setValue(FACING, FacingOrNone.NONE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pipePos, Player player, BlockHitResult hitResult) {
        if (player.getAbilities().mayBuild && !MiscUtil.itemIsPipe(player.getMainHandItem()) && state.getValue(FACING) != FacingOrNone.NONE) {
            Direction direction = MiscUtil.nextDirection(state.getValue(FACING).getDirection());
            for (int i = 0; i < 5; i++) {
                if (this.isPipeConnected(state, direction) && Services.LOADER_SERVICE.canAccessContainer(level, pipePos.relative(direction), direction.getOpposite())) {
                    level.setBlock(pipePos, state.setValue(FACING, FacingOrNone.with(direction)), 3);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.playSound(null, pipePos, ClassicPipes.PIPE_ADJUST_SOUND, SoundSource.BLOCKS);
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
        if (state.getValue(ENABLED) && level.hasNeighborSignal(pos) == this.inverted) {
            level.setBlock(pos, state.setValue(ENABLED, false), 2);
        } else if (!state.getValue(ENABLED) && level.hasNeighborSignal(pos) != this.inverted) {
            level.setBlock(pos, state.setValue(ENABLED, true), 2);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean b) {
        this.checkPoweredState(level, pos, state);
        // Queue a tick update after the adjacent block has changed
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected boolean canConnectToPipe(PipeBlock pipeBlock){
        return !(pipeBlock instanceof CopperPipeBlock);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState refreshed = state;

        // Recheck after neighboring blocks have finished initialization
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);

            // Avoid checking neighbor in unloaded chunks
            if (level.hasChunk(SectionPos.blockToSectionCoord(neighborPos.getX()), SectionPos.blockToSectionCoord(neighborPos.getZ()))) {
                refreshed = this.setPipeConnected(refreshed, direction, this.canConnect(level, pos, direction));
            }
        }

        // Sets the extraction face, start from DOWN if none is selected
        Direction direction = state.getValue(FACING) == FacingOrNone.NONE ? Direction.DOWN : state.getValue(FACING).getDirection();
        refreshed = refreshed.setValue(FACING, FacingOrNone.NONE);

        // Keep the current extraction face if it is still valid or find another valid inventory.
        for (int i = 0; i < 6; i++) {
            BlockPos neighborPos = pos.relative(direction);
            if (level.hasChunk(SectionPos.blockToSectionCoord(neighborPos.getX()), SectionPos.blockToSectionCoord(neighborPos.getZ()))
                    && this.isPipeConnected(refreshed, direction)
                    && Services.LOADER_SERVICE.canAccessContainer(level, neighborPos, direction.getOpposite())) {
                refreshed = refreshed.setValue(FACING, FacingOrNone.with(direction));
                break;
            }
            direction = MiscUtil.nextDirection(direction);
        }

        // Update pipe neighbors if the connection state has changed
        if (refreshed != state) {
            level.setBlock(pos, refreshed, 3);
            if (level.getBlockEntity(pos) instanceof PipeEntity pipe) {
                for (Direction face : Direction.values()) {
                    boolean wasConnected = this.isPipeConnected(state, face);
                    if (wasConnected != this.isPipeConnected(refreshed, face)) {
                        pipe.scheduleUpdate(level, refreshed, pos, face, wasConnected);
                    }
                }
            }
        }
    }
}
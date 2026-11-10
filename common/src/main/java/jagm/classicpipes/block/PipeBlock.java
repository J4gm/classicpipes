package jagm.classicpipes.block;

import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.blockentity.PipeEntity;
import jagm.classicpipes.services.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.function.Function;

public abstract class PipeBlock extends TransparentBlock implements SimpleWaterloggedBlock, EntityBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private final Function<BlockState, VoxelShape> shapes;

    public PipeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
        Map<Direction, VoxelShape> map = Shapes.rotateAll(Block.boxZ(8.0F, 0.0F, 8.0F));
        this.shapes = this.getShapeForEachState(state -> {
            VoxelShape shape = Block.cube(8.0F);
            for (Direction direction : Direction.values()) {
                if (this.isPipeConnected(state, direction)) {
                    shape = Shapes.or(map.get(direction), shape);
                }
            }
            return shape;
        });
    }

    public abstract boolean isPipeConnected(BlockState state, Direction direction);

    public abstract BlockState setPipeConnected(BlockState state, Direction direction, boolean connected);

    protected final boolean isSplit(LevelReader level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof PipeEntity pipe && pipe.isSplit(direction);
    }

    protected final boolean canConnectUnlessSplit(Level level, BlockPos pos, Direction direction) {
        return !this.isSplit(level, pos, direction) && this.canConnect(level, pos, direction);
    }

    protected boolean canConnect(Level level, BlockPos pipePos, Direction direction) {
        BlockPos neighbourPos = pipePos.relative(direction);
        if (level.getBlockState(neighbourPos).getBlock() instanceof PipeBlock pipeBlock) {
            return canConnectToPipeBothWays(this, pipeBlock);
        }
        return Services.LOADER_SERVICE.canAccessContainer(level, neighbourPos, direction.getOpposite());
    }

    protected static boolean canConnectToPipeBothWays(PipeBlock pipe1, PipeBlock pipe2) {
        return pipe1.canConnectToPipe(pipe2) && pipe2.canConnectToPipe(pipe1);
    }

    protected boolean canConnectToPipe(PipeBlock pipeBlock){
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().trySetValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pipePos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            scheduledTickAccess.scheduleTick(pipePos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        boolean wasConnected = this.isPipeConnected(state, direction);
        boolean canConnect = this.canConnectUnlessSplit((Level) level, pipePos, direction);
        BlockState newState = this.setPipeConnected(state, direction, canConnect);
        boolean isConnected = this.isPipeConnected(newState, direction);
        if (wasConnected != isConnected && level.getBlockEntity(pipePos) instanceof PipeEntity pipe && level instanceof ServerLevel serverLevel) {
            pipe.scheduleUpdate(serverLevel, newState, pipePos, direction, wasConnected);
        }
        return newState;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(jagm.classicpipes.ClassicPipes.SPLITTER)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel serverLevel) {
            Direction splitDirection = null;
            PipeBlock splitNeighbour = null;
            double closestArm = Double.NEGATIVE_INFINITY;
            net.minecraft.world.phys.Vec3 hitOffset = hitResult.getLocation().subtract(net.minecraft.world.phys.Vec3.atCenterOf(pos));
            for (Direction direction : Direction.values()) {
                BlockPos neighbourPos = pos.relative(direction);
                BlockState neighbourState = level.getBlockState(neighbourPos);
                if (!this.isPipeConnected(state, direction) || this.isSplit(level, pos, direction)) {
                    continue;
                }
                PipeBlock neighbourBlock = neighbourState.getBlock() instanceof PipeBlock pipeBlock ? pipeBlock : null;
                if (neighbourBlock != null) {
                    if (!neighbourBlock.isPipeConnected(neighbourState, direction.getOpposite())
                            || neighbourBlock.isSplit(level, neighbourPos, direction.getOpposite())
                            || !canConnectToPipeBothWays(this, neighbourBlock)) {
                        continue;
                    }
                } else if (!this.canConnect(level, pos, direction)) {
                    continue;
                }
                double armDistance = hitOffset.x * direction.getStepX() + hitOffset.y * direction.getStepY() + hitOffset.z * direction.getStepZ();
                if (armDistance > closestArm) {
                    closestArm = armDistance;
                    splitDirection = direction;
                    splitNeighbour = neighbourBlock;
                }
            }
            if (splitDirection != null) {
                Direction direction = splitDirection;
                BlockPos neighbourPos = pos.relative(direction);
                BlockState neighbourState = level.getBlockState(neighbourPos);
                BlockState splitState = this.setPipeConnected(state, direction, false);
                if (level.getBlockEntity(pos) instanceof PipeEntity pipe) {
                    pipe.splitConnection(direction);
                }
                level.setBlock(pos, splitState, 2);
                if (level.getBlockEntity(pos) instanceof PipeEntity pipe) {
                    pipe.scheduleUpdate(serverLevel, splitState, pos, direction, true);
                }
                if (splitNeighbour != null) {
                    BlockState neighbourSplitState = splitNeighbour.setPipeConnected(neighbourState, direction.getOpposite(), false);
                    if (level.getBlockEntity(neighbourPos) instanceof PipeEntity neighbourPipe) {
                        neighbourPipe.splitConnection(direction.getOpposite());
                    }
                    level.setBlock(neighbourPos, neighbourSplitState, 2);
                    if (level.getBlockEntity(neighbourPos) instanceof PipeEntity neighbourPipe) {
                        neighbourPipe.scheduleUpdate(serverLevel, neighbourSplitState, neighbourPos, direction.getOpposite(), true);
                    }
                }
                serverLevel.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS);
                player.awardStat(Stats.ITEM_USED.get(jagm.classicpipes.ClassicPipes.SPLITTER));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.getBlockEntity(pos) instanceof PipeEntity pipe) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbourPos = pos.relative(direction);
                if (pipe.isSplit(direction)
                        && level.getBlockState(neighbourPos).getBlock() instanceof PipeBlock
                        && level.getBlockEntity(neighbourPos) instanceof PipeEntity neighbourPipe) {
                    neighbourPipe.clearSplitConnection(direction.getOpposite());
                }
            }
        }
        if (!(this instanceof FluidPipeBlock) && level instanceof ServerLevel serverLevel) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
            if (blockEntity instanceof ItemPipeEntity pipe) {
                pipe.dropItems(serverLevel, pos);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PipeEntity pipe) {
            return pipe.getComparatorOutput();
        }
        return 0;
    }

}

package jagm.classicpipes.block;

import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.blockentity.PipeEntity;
import jagm.classicpipes.services.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
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
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false)
        );
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
        boolean willConnect = this.canConnect((Level) level, pipePos, direction);
        BlockState newState = this.setPipeConnected(state, direction, willConnect);
        if (wasConnected != willConnect && level.getBlockEntity(pipePos) instanceof PipeEntity pipe && level instanceof ServerLevel serverLevel) {
            pipe.scheduleUpdate(serverLevel, newState, pipePos, direction, wasConnected);
        }
        return newState;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
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
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PipeEntity pipe) {
            return pipe.getComparatorOutput();
        }
        return 0;
    }

}

package jagm.classicpipes.block;

import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.blockentity.PipeEntity;
import jagm.classicpipes.services.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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

public abstract class PipeBlock extends TransparentBlock implements SimpleWaterloggedBlock, EntityBlock, Equipable {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected final VoxelShape[] shapeByIndex;

    public PipeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false)
        );
        this.shapeByIndex = this.makeShapes();
    }

    private VoxelShape[] makeShapes() {
        float a = 0.25F;
        float f = 0.5F - a;
        float f1 = 0.5F + a;
        VoxelShape voxelshape = Block.box(
                f * 16.0F, f * 16.0F, f * 16.0F, f1 * 16.0F, f1 * 16.0F, f1 * 16.0F
        );
        VoxelShape[] avoxelshape = new VoxelShape[6];
        for (int i = 0; i < 6; i++) {
            Direction direction = Direction.from3DDataValue(i);
            avoxelshape[i] = Shapes.box(
                    0.5 + Math.min(-a, (double)direction.getStepX() * 0.5),
                    0.5 + Math.min(-a, (double)direction.getStepY() * 0.5),
                    0.5 + Math.min(-a, (double)direction.getStepZ() * 0.5),
                    0.5 + Math.max(a, (double)direction.getStepX() * 0.5),
                    0.5 + Math.max(a, (double)direction.getStepY() * 0.5),
                    0.5 + Math.max(a, (double)direction.getStepZ() * 0.5)
            );
        }
        VoxelShape[] avoxelshape1 = new VoxelShape[64];
        for (int k = 0; k < 64; k++) {
            VoxelShape voxelshape1 = voxelshape;

            for (int j = 0; j < 6; j++) {
                if ((k & 1 << j) != 0) {
                    voxelshape1 = Shapes.or(voxelshape1, avoxelshape[j]);
                }
            }

            avoxelshape1[k] = voxelshape1;
        }
        return avoxelshape1;
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
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pipePos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pipePos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
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
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapeByIndex[this.getAABBIndex(state)];
    }

    protected int getAABBIndex(BlockState state) {
        int i = 0;
        for (int j = 0; j < 6; j++) {
            if (this.isPipeConnected(state, Direction.from3DDataValue(j))) {
                i |= 1 << j;
            }
        }
        return i;
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
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PipeEntity pipe) {
            return pipe.getComparatorOutput();
        }
        return 0;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public InteractionResultHolder<ItemStack> swapWithEquipmentSlot(Item item, Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }

}

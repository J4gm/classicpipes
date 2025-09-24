package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.CopperFluidPipeEntity;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.FacingOrNone;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class CopperFluidPipeBlock extends FluidPipeBlock {

    public static final EnumProperty<FacingOrNone> FACING = FacingOrNone.BLOCK_PROPERTY;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    public CopperFluidPipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, FacingOrNone.NONE)
                .setValue(ENABLED, false)
        );
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperFluidPipeEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ClassicPipes.COPPER_FLUID_PIPE_ENTITY ? CopperFluidPipeEntity::tick : null;
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
                if (this.isPipeConnected(superState, direction) && Services.LOADER_SERVICE.canAccessFluidContainer(context.getLevel(), context.getClickedPos().relative(direction), direction.getOpposite())) {
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
        Direction direction = state.getValue(FACING) == FacingOrNone.NONE ? Direction.DOWN : state.getValue(FACING).getDirection();
        for (int i = 0; i < 6; i++) {
            if (this.isPipeConnected(superState, direction) && Services.LOADER_SERVICE.canAccessFluidContainer((Level) level, pipePos.relative(direction), direction.getOpposite())) {
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
                if (this.isPipeConnected(state, direction) && Services.LOADER_SERVICE.canAccessFluidContainer(level, pipePos.relative(direction), direction.getOpposite())) {
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
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            this.checkPoweredState(level, pos, state);
        }
    }

    private void checkPoweredState(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(ENABLED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.setValue(ENABLED, false), 2);
        } else if (!state.getValue(ENABLED) && level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.setValue(ENABLED, true), 2);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean b) {
        this.checkPoweredState(level, pos, state);
    }

    @Override
    protected boolean canConnectToPipe(PipeBlock pipeBlock){
        return super.canConnectToPipe(pipeBlock) && !(pipeBlock instanceof CopperFluidPipeBlock);
    }

}

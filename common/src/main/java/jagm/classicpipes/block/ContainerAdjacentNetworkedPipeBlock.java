package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.PipeEntity;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

public abstract class ContainerAdjacentNetworkedPipeBlock extends NetworkedPipeBlock {

    public static final EnumProperty<FacingOrNone> FACING = FacingOrNone.BLOCK_PROPERTY;

    public ContainerAdjacentNetworkedPipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, FacingOrNone.NONE));
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
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pipePos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        boolean wasConnected = this.isPipeConnected(state, initialDirection);
        boolean willConnect = this.canConnect((Level) level, pipePos, initialDirection);
        state = this.setPipeConnected(state, initialDirection, willConnect);
        Direction direction = state.getValue(FACING) == FacingOrNone.NONE ? Direction.DOWN : state.getValue(FACING).getDirection();
        state = state.setValue(FACING, FacingOrNone.NONE);
        for (int i = 0; i < 6; i++) {
            if (this.isPipeConnected(state, direction) && Services.LOADER_SERVICE.canAccessContainer((Level) level, pipePos.relative(direction), direction.getOpposite())) {
                state = state.setValue(FACING, FacingOrNone.with(direction));
                break;
            }
            direction = MiscUtil.nextDirection(direction);
        }
        if (wasConnected != willConnect && level.getBlockEntity(pipePos) instanceof PipeEntity pipe && level instanceof ServerLevel serverLevel) {
            pipe.scheduleUpdate(serverLevel, state, pipePos, initialDirection, wasConnected);
        }
        return state;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pipePos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        Direction facing = state.getValue(FACING).getDirection();
        if (player.getAbilities().mayBuild && player.isCrouching() && !MiscUtil.itemIsPipe(player.getMainHandItem()) && facing != null) {
            Direction direction = MiscUtil.nextDirection(facing);
            for (int i = 0; i < 5; i++) {
                BlockPos nextPos = pipePos.relative(direction);
                if (this.isPipeConnected(state, direction) && Services.LOADER_SERVICE.canAccessContainer(level, nextPos, direction.getOpposite())) {
                    BlockState newState = state.setValue(FACING, FacingOrNone.with(direction));
                    level.setBlock(pipePos, newState, 3);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.playSound(null, pipePos, ClassicPipes.PIPE_ADJUST_SOUND, SoundSource.BLOCKS);
                        this.onNeighborChange(newState, serverLevel, pipePos, nextPos);
                    }
                    return InteractionResult.SUCCESS;
                }
                direction = MiscUtil.nextDirection(direction);
            }
        }
        return InteractionResult.PASS;
    }

    // Overrides method present in Forge (IForgeBlock) and NeoForge (IBlockExtension).
    // Called from ContainerAdjacentPipeUpdaterMixin in Fabric.
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {}

}

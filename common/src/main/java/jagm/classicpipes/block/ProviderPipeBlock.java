package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.ProviderPipeEntity;
import jagm.classicpipes.network.ClientBoundTwoBoolsPayload;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.FacingOrNone;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ProviderPipeBlock extends RoutingPipeBlock {

    public static final EnumProperty<FacingOrNone> FACING = FacingOrNone.BLOCK_PROPERTY;

    public ProviderPipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, FacingOrNone.NONE));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProviderPipeEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ClassicPipes.PROVIDER_PIPE_ENTITY ? ProviderPipeEntity::tick : null;
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
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pipePos, Direction initialDirection, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        Direction facing = state.getValue(FACING).getDirection();
        if (player.getAbilities().mayBuild && player.isCrouching() && !MiscUtil.itemIsPipe(player.getMainHandItem()) && facing != null) {
            Direction direction = MiscUtil.nextDirection(facing);
            for (int i = 0; i < 5; i++) {
                BlockPos nextPos = pos.relative(direction);
                if (this.isPipeConnected(state, direction) && Services.LOADER_SERVICE.canAccessContainer(level, nextPos, direction.getOpposite())) {
                    BlockState newState = state.setValue(FACING, FacingOrNone.with(direction));
                    level.setBlock(pos, newState, 3);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.playSound(null, pos, ClassicPipes.PIPE_ADJUST_SOUND, SoundSource.BLOCKS);
                        this.onNeighborChange(newState, serverLevel, pos, nextPos);
                    }
                    return InteractionResult.SUCCESS;
                }
                direction = MiscUtil.nextDirection(direction);
            }
        }
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof ProviderPipeEntity providerPipe) {
            Services.LOADER_SERVICE.openMenu(
                    (ServerPlayer) player,
                    providerPipe,
                    new ClientBoundTwoBoolsPayload(providerPipe.shouldMatchComponents(), providerPipe.shouldLeaveOne()),
                    ClientBoundTwoBoolsPayload.STREAM_CODEC
            );
        }
        return InteractionResult.SUCCESS;
    }

    // Overrides method present in Forge (IForgeBlock) and NeoForge (IBlockExtension).
    // Called from ProviderPipeUpdaterMixin in Fabric.
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        Direction facing = state.getValue(FACING).getDirection();
        if (level instanceof ServerLevel serverLevel && facing != null && level.getBlockEntity(pos) instanceof ProviderPipeEntity providerPipe && neighbor.equals(pos.relative(facing))) {
            providerPipe.updateCache(serverLevel, pos, facing);
        }
    }

}

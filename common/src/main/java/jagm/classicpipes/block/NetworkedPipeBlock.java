package jagm.classicpipes.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.NetworkedPipeEntity;
import jagm.classicpipes.blockentity.RequestPipeEntity;
import jagm.classicpipes.blockentity.RoutingPipeEntity;
import jagm.classicpipes.inventory.menu.RequestMenu;
import jagm.classicpipes.network.ClientBoundItemListPayload;
import jagm.classicpipes.network.ClientBoundTwoBoolsPayload;
import jagm.classicpipes.services.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Map;

public class NetworkedPipeBlock extends PipeBlock {

    public static final EnumProperty<ConnectionState> NORTH = EnumProperty.create("north", ConnectionState.class, ConnectionState.values());
    public static final EnumProperty<ConnectionState> EAST = EnumProperty.create("east", ConnectionState.class, ConnectionState.values());
    public static final EnumProperty<ConnectionState> SOUTH = EnumProperty.create("south", ConnectionState.class, ConnectionState.values());
    public static final EnumProperty<ConnectionState> WEST = EnumProperty.create("west", ConnectionState.class, ConnectionState.values());
    public static final EnumProperty<ConnectionState> UP = EnumProperty.create("up", ConnectionState.class, ConnectionState.values());
    public static final EnumProperty<ConnectionState> DOWN = EnumProperty.create("down", ConnectionState.class, ConnectionState.values());
    public static final Map<Direction, EnumProperty<ConnectionState>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST, Direction.UP, UP, Direction.DOWN, DOWN)));

    public NetworkedPipeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(NORTH, ConnectionState.NONE)
                .setValue(EAST, ConnectionState.NONE)
                .setValue(SOUTH, ConnectionState.NONE)
                .setValue(WEST, ConnectionState.NONE)
                .setValue(UP, ConnectionState.NONE)
                .setValue(DOWN, ConnectionState.NONE)
        );
    }

    @Override
    public boolean isPipeConnected(BlockState state, Direction direction) {
        return !state.getValue(PROPERTY_BY_DIRECTION.get(direction)).equals(ConnectionState.NONE);
    }

    @Override
    public BlockState setPipeConnected(BlockState state, Direction direction, boolean connected) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connected ? (this.isLinked(state, direction) ? ConnectionState.LINKED : ConnectionState.UNLINKED) : ConnectionState.NONE);
    }

    public boolean isLinked(BlockState state, Direction direction) {
        return state.getValue(PROPERTY_BY_DIRECTION.get(direction)).equals(ConnectionState.LINKED);
    }

    public BlockState setLinked(BlockState state, Direction direction, boolean linked) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), linked ? ConnectionState.LINKED : ConnectionState.UNLINKED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RoutingPipeEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ClassicPipes.ROUTING_PIPE_ENTITY ? RoutingPipeEntity::tick : null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState superState = super.getStateForPlacement(context);
        if (superState != null) {
            return superState
                    .trySetValue(NORTH, this.canConnect(context.getLevel(), context.getClickedPos(), Direction.NORTH) ? ConnectionState.UNLINKED : ConnectionState.NONE)
                    .trySetValue(EAST, this.canConnect(context.getLevel(), context.getClickedPos(), Direction.EAST) ? ConnectionState.UNLINKED : ConnectionState.NONE)
                    .trySetValue(SOUTH, this.canConnect(context.getLevel(), context.getClickedPos(), Direction.SOUTH) ? ConnectionState.UNLINKED : ConnectionState.NONE)
                    .trySetValue(WEST, this.canConnect(context.getLevel(), context.getClickedPos(), Direction.WEST) ? ConnectionState.UNLINKED : ConnectionState.NONE)
                    .trySetValue(UP, this.canConnect(context.getLevel(), context.getClickedPos(), Direction.UP) ? ConnectionState.UNLINKED : ConnectionState.NONE)
                    .trySetValue(DOWN, this.canConnect(context.getLevel(), context.getClickedPos(), Direction.DOWN) ? ConnectionState.UNLINKED : ConnectionState.NONE);
        }
        return this.defaultBlockState();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof RoutingPipeEntity routingPipe) {
            Services.LOADER_SERVICE.openMenu(
                    (ServerPlayer) player,
                    routingPipe,
                    new ClientBoundTwoBoolsPayload(routingPipe.getFilter().getItemStacksForPayload(), routingPipe.shouldMatchComponents(), routingPipe.isDefaultRoute()),
                    ClientBoundTwoBoolsPayload.STREAM_CODEC
            );
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem().equals(ClassicPipes.PIPE_SLICER)) {
            if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof NetworkedPipeEntity networkedPipe && networkedPipe.hasNetwork()) {
                ClientBoundItemListPayload payload = networkedPipe.getNetwork().requestItemList(pos);
                Services.LOADER_SERVICE.openMenu(
                        serverPlayer,
                        new MenuProvider() {

                            @Override
                            public Component getDisplayName() {
                                return RequestPipeEntity.TITLE;
                            }

                            @Override
                            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                                return new RequestMenu(id, inventory, payload);
                            }

                        },
                        payload,
                        ClientBoundItemListPayload.STREAM_CODEC
                );
                player.awardStat(Stats.ITEM_USED.get(ClassicPipes.PIPE_SLICER));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    public enum ConnectionState implements StringRepresentable {

        NONE, UNLINKED, LINKED;

        @Override
        public String getSerializedName() {
            return switch(this) {
                case NONE -> "none";
                case UNLINKED -> "unlinked";
                case LINKED -> "linked";
            };
        }

    }

}

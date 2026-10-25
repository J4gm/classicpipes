package jagm.classicpipes.blockentity;

import jagm.classicpipes.block.PipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public abstract class PipeEntity extends BlockEntity {

    private final List<ScheduledPipeUpdate> updates;
    private final Set<Direction> splitDirections;

    public PipeEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.updates = new ArrayList<>();
        this.splitDirections = EnumSet.noneOf(Direction.class);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
        if (blockEntity instanceof PipeEntity pipe) {
            if (level instanceof ServerLevel serverLevel) {
                pipe.tickServer(serverLevel, pos, state);
                for (ScheduledPipeUpdate update : pipe.updates) {
                    pipe.update(update.level(), update.state(), update.pos(), update.direction(), update.wasConnected());
                }
                pipe.updates.clear();
            } else {
                pipe.tickClient(level, pos);
            }
        }
    }

    public abstract void tickServer(ServerLevel level, BlockPos pos, BlockState state);

    public abstract void tickClient(Level level, BlockPos pos);

    public final void scheduleUpdate(ServerLevel level, BlockState state, BlockPos pos, Direction direction, boolean wasConnected) {
        this.updates.add(new ScheduledPipeUpdate(level, state, pos, direction, wasConnected));
    }

    protected abstract void update(ServerLevel level, BlockState state, BlockPos pos, Direction direction, boolean wasConnected);

    public abstract int getComparatorOutput();

    public abstract short getTargetSpeed();

    public abstract short getAcceleration();

    public final boolean isSplit(Direction direction) {
        return this.splitDirections.contains(direction);
    }

    public final void splitConnection(Direction direction) {
        this.splitDirections.add(direction);
        this.setChanged();
    }

    public final void clearSplitConnection(Direction direction) {
        if (this.splitDirections.remove(direction)) {
            this.setChanged();
        }
    }

    protected final boolean isPipeConnected(BlockState state, Direction direction) {
        if (state.getBlock() instanceof PipeBlock pipeBlock) {
            return pipeBlock.isPipeConnected(state, direction);
        }
        return false;
    }

    protected final int countConnections(BlockState state) {
        int count = 0;
        for (Direction direction : Direction.values()) {
            if (isPipeConnected(state, direction)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private record ScheduledPipeUpdate(ServerLevel level, BlockState state, BlockPos pos, Direction direction, boolean wasConnected) {
    }

}

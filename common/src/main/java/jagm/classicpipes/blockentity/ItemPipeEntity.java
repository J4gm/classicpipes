package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.NetworkedPipeBlock;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import jagm.classicpipes.util.Tuple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public abstract class ItemPipeEntity extends PipeEntity {

    protected final List<ItemInPipe> contents;
    protected final List<ItemInPipe> queued;
    public final Map<Direction, Tuple<BlockPos, Integer>> networkDistances;
    private boolean networkingInitialised = false;
    private final Map<ItemInPipe, Long> tickAdded;

    public ItemPipeEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.contents = new ArrayList<>();
        this.queued = new ArrayList<>();
        this.networkDistances = new HashMap<>();
        this.tickAdded = new HashMap<>();
    }

    @Override
    public void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        boolean sendBlockUpdate = false;
        if (!this.networkingInitialised) {
            this.initialiseNetworking(level, state, pos); // sets changed
            this.networkingInitialised = true;
            sendBlockUpdate = true;
        }
        if (!this.isEmpty()) {
            ListIterator<ItemInPipe> iterator = this.contents.listIterator();
            while (iterator.hasNext()) {
                ItemInPipe item = iterator.next();
                if (this.tickAdded.containsKey(item)) {
                    if (this.tickAdded.get(item) == level.getGameTime()) {
                        continue;
                    }
                    this.tickAdded.remove(item);
                }
                item.move(this.getTargetSpeed(), this.getAcceleration());
                if (item.getAge() > ItemInPipe.DESPAWN_AGE) {
                    iterator.remove();
                    sendBlockUpdate = true;
                    continue;
                }
                if (item.getProgress() >= ItemInPipe.HALFWAY) {
                    if (item.isEjecting()) {
                        iterator.remove();
                        this.eject(level, pos, item);
                        sendBlockUpdate = true;
                        continue;
                    }
                }
                if (item.getProgress() >= ItemInPipe.PIPE_LENGTH) {
                    if (Services.LOADER_SERVICE.handleItemInsertion(this, level, pos, state, item)) {
                        iterator.remove();
                    } else if (this instanceof MatchingPipe matchingPipe) {
                        matchingPipe.markCannotFit(item.getStack());
                    }
                    sendBlockUpdate = true;
                }
            }
            this.addQueuedItems(level, false); // sets changed
        }
        if (sendBlockUpdate) {
            level.sendBlockUpdated(pos, state, state, 2);
        }
    }

    @Override
    public void tickClient(Level level, BlockPos pos) {
        if (!this.isEmpty()) {
            ListIterator<ItemInPipe> iterator = this.contents.listIterator();
            while (iterator.hasNext()) {
                ItemInPipe item = iterator.next();
                if (this.tickAdded.containsKey(item)) {
                    if (this.tickAdded.get(item) == level.getGameTime()) {
                        continue;
                    }
                    this.tickAdded.remove(item);
                }
                item.move(this.getTargetSpeed(), this.getAcceleration());
                if (item.getProgress() >= ItemInPipe.PIPE_LENGTH) {
                    BlockPos nextPos = pos.relative(item.getTargetDirection());
                    if (level.getBlockEntity(nextPos) instanceof ItemPipeEntity nextPipe) {
                        item.resetProgress(item.getTargetDirection().getOpposite());
                        nextPipe.insertPipeItem(level, item);
                        iterator.remove();
                    }
                }
            }
        }
    }

    public void insertPipeItem(Level level, ItemInPipe item) {
        this.queued.add(item);
        this.routeItem(item);
        this.addQueuedItems(level, true);
    }

    public void addQueuedItems(Level level, boolean waitForNextTick) {
        for (ItemInPipe item : this.queued) {
            this.contents.add(item);
            if (waitForNextTick) {
                this.tickAdded.put(item, level.getGameTime());
            }
        }
        this.setChanged();
        this.queued.clear();
    }

    public abstract void routeItem(BlockState state, ItemInPipe item);

    public final void routeItem(ItemInPipe item) {
        routeItem(this.getBlockState(), item);
    }

    protected boolean canJoinNetwork() {
        return true;
    }

    @Override
    public void update(ServerLevel level, BlockState state, BlockPos pos, Direction direction, boolean wasConnected) {
        if (!this.isEmpty()) {
            ListIterator<ItemInPipe> iterator = this.contents.listIterator();
            while (iterator.hasNext()) {
                ItemInPipe item = iterator.next();
                if (!wasConnected || (item.getTargetDirection() == direction && item.getFromDirection() != direction && item.getProgress() < ItemInPipe.HALFWAY)) {
                    this.routeItem(state, item);
                } else if ((item.getFromDirection() == direction && item.getProgress() < ItemInPipe.HALFWAY) || (item.getTargetDirection() == direction && item.getProgress() >= ItemInPipe.HALFWAY)) {
                    iterator.remove();
                    item.drop(level, pos);
                }
            }
            this.addQueuedItems(level, false);
        }
        if (wasConnected) {
            this.networkDistances.remove(direction);
        }
        boolean flaggedChange = false;
        for (Direction otherDirection : Direction.values()) {
            if (!this.isPipeConnected(state, otherDirection)) {
                this.networkDistances.remove(otherDirection);
                if (this instanceof NetworkedPipeEntity networkedPipe && !flaggedChange) {
                    networkedPipe.networkChanged(level, pos, false);
                    flaggedChange = true;
                }
            }
        }
        for (Direction otherDirection : Direction.values()) {
            BlockPos nextPos = pos.relative(otherDirection);
            if (this.isPipeConnected(state, otherDirection) && level.getBlockEntity(nextPos) instanceof ItemPipeEntity nextPipe) {
                this.updateNetworking(level, state, pos, nextPipe, nextPos, otherDirection, new HashSet<>(), true);
            }
        }
        this.setChanged();
        level.sendBlockUpdated(pos, state, state, 2);
    }

    protected void initialiseNetworking(ServerLevel level, BlockState state, BlockPos pos) {
        Set<Tuple<BlockPos, Direction>> visited = new HashSet<>();
        for (Direction direction : Direction.values()) {
            BlockPos nextPos = pos.relative(direction);
            if (this.isPipeConnected(state, direction) && level.getBlockEntity(nextPos) instanceof ItemPipeEntity nextPipe) {
                visited = this.updateNetworking(level, state, pos, nextPipe, nextPos, direction, visited, false);
            } else {
                this.networkDistances.remove(direction);
            }
        }
        this.setChanged();
    }

    private Set<Tuple<BlockPos, Direction>> updateNetworking(ServerLevel level, BlockState state, BlockPos pos, ItemPipeEntity nextPipe, BlockPos nextPos, Direction nextDirection, Set<Tuple<BlockPos, Direction>> visited, boolean triggerNetworkChanges) {

        for (Tuple<BlockPos, Direction> tuple : visited) {
            if (tuple.a().equals(pos) && tuple.b().equals(nextDirection)) {
                return visited;
            }
        }
        visited.add(new Tuple<>(pos, nextDirection));

        if (this instanceof NetworkedPipeEntity && nextPipe.canJoinNetwork()) {
            nextPipe.networkDistances.put(nextDirection.getOpposite(), new Tuple<>(pos, 1));
        } else {
            boolean hasNetworkConnection = false;
            if (this.countConnections(state) < 3) {
                for (Direction direction : this.networkDistances.keySet()) {
                    if (!direction.equals(nextDirection) && nextPipe.canJoinNetwork()) {
                        Tuple<BlockPos, Integer> tuple = this.networkDistances.get(direction);
                        nextPipe.networkDistances.put(nextDirection.getOpposite(), new Tuple<>(tuple.a(), tuple.b() + 1));
                        hasNetworkConnection = true;
                    }
                }
            }
            if (!hasNetworkConnection) {
                nextPipe.networkDistances.remove(nextDirection.getOpposite());
            }
        }

        if (nextPipe instanceof NetworkedPipeEntity && this.canJoinNetwork()) {
            this.networkDistances.put(nextDirection, new Tuple<>(nextPos, 1));
        } else {
            boolean hasNetworkConnection = false;
            if (nextPipe.countConnections(nextPipe.getBlockState()) < 3) {
                for (Direction direction : nextPipe.networkDistances.keySet()) {
                    if (!direction.equals(nextDirection.getOpposite()) && this.canJoinNetwork()) {
                        Tuple<BlockPos, Integer> tuple = nextPipe.networkDistances.get(direction);
                        this.networkDistances.put(nextDirection, new Tuple<>(tuple.a(), tuple.b() + 1));
                        hasNetworkConnection = true;
                    }
                }
            }
            if (!hasNetworkConnection) {
                this.networkDistances.remove(nextDirection);
            }
            for (Direction direction : Direction.values()) {
                if (!direction.equals(nextDirection.getOpposite())) {
                    BlockPos anotherPos = nextPos.relative(direction);
                    if (nextPipe.isPipeConnected(nextPipe.getBlockState(), direction) && level.getBlockEntity(anotherPos) instanceof ItemPipeEntity anotherPipe) {
                        visited = nextPipe.updateNetworking(level, nextPipe.getBlockState(), nextPos, anotherPipe, anotherPos, direction, visited, triggerNetworkChanges);
                    } else {
                        nextPipe.networkDistances.remove(direction);
                    }
                }
            }
        }

        if (this instanceof NetworkedPipeEntity networkedPipe && state.getBlock() instanceof NetworkedPipeBlock networkedBlock) {
            boolean wasLinked = networkedBlock.isLinked(state, nextDirection);
            boolean isLinked = this.networkDistances.containsKey(nextDirection);
            if (wasLinked != isLinked && triggerNetworkChanges) {
                networkedPipe.networkChanged(level, pos, isLinked);
            }
            level.setBlock(pos, networkedBlock.setLinked(state, nextDirection, isLinked), 3);
        }

        if (nextPipe instanceof NetworkedPipeEntity networkedPipe && networkedPipe.getBlockState().getBlock() instanceof NetworkedPipeBlock networkedBlock) {
            boolean wasLinked = networkedBlock.isLinked(networkedPipe.getBlockState(), nextDirection.getOpposite());
            boolean isLinked = networkedPipe.networkDistances.containsKey(nextDirection.getOpposite());
            if (wasLinked != isLinked && triggerNetworkChanges) {
                networkedPipe.networkChanged(level, nextPos, isLinked);
            }
            level.setBlock(nextPos, networkedBlock.setLinked(networkedPipe.getBlockState(), nextDirection.getOpposite(), isLinked), 3);
        }

        this.setChanged();
        level.sendBlockUpdated(pos, state, state, 2);
        nextPipe.setChanged();
        level.sendBlockUpdated(nextPos, nextPipe.getBlockState(), nextPipe.getBlockState(), 2);
        return visited;

    }

    public void eject(ServerLevel level, BlockPos pos, ItemInPipe item) {
        if (!item.getStack().isEmpty()) {
            Vec3 offset = new Vec3(
                    item.getTargetDirection() == Direction.WEST ? 0.125F : (item.getTargetDirection() == Direction.EAST ? 0.875F : 0.5F),
                    item.getTargetDirection() == Direction.DOWN ? 0.0F : (item.getTargetDirection() == Direction.UP ? 0.75F : 0.375F),
                    item.getTargetDirection() == Direction.NORTH ? 0.125F : (item.getTargetDirection() == Direction.SOUTH ? 0.875F : 0.5F)
            );
            ItemEntity ejectedItem = new ItemEntity(level, pos.getX() + offset.x, pos.getY() + offset.y, pos.getZ() + offset.z, item.getStack());
            float v = (float) item.getSpeed() / ItemInPipe.PIPE_LENGTH;
            ejectedItem.setDeltaMovement(
                    item.getTargetDirection() == Direction.WEST ? -v : (item.getTargetDirection() == Direction.EAST ? v : 0.0F),
                    item.getTargetDirection() == Direction.DOWN ? -v : (item.getTargetDirection() == Direction.UP ? v : 0.0F),
                    item.getTargetDirection() == Direction.NORTH ? -v : (item.getTargetDirection() == Direction.SOUTH ? v : 0.0F)
            );
            ejectedItem.setDefaultPickUpDelay();
            level.addFreshEntity(ejectedItem);
            level.playSound(ejectedItem, pos, ClassicPipes.PIPE_EJECT_SOUND, SoundSource.BLOCKS);
        }
    }

    public void dropItems(ServerLevel serverLevel, BlockPos pos) {
        for (ItemInPipe item : this.contents) {
            item.drop(serverLevel, pos);
        }
    }

    public List<ItemInPipe> getContents() {
        return this.contents;
    }

    public void setItem(Direction side, ItemStack stack) {
        if (this.getLevel() instanceof ServerLevel serverLevel && !stack.isEmpty()) {
            ItemInPipe item = new ItemInPipe(stack, side, side.getOpposite());
            this.insertPipeItem(serverLevel, item);
            serverLevel.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 2);
        }
        this.setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag valueInput, HolderLookup.Provider registries) {
        this.clearContent();
        this.networkDistances.clear();
        this.tickAdded.clear();
        super.loadAdditional(valueInput, registries);
        ListTag itemsList = valueInput.getListOrEmpty("items");
        itemsList.forEach(tag -> MiscUtil.loadFromTag(tag, ItemInPipe.CODEC, registries, this.contents::add));
        for (Direction direction : Direction.values()) {
            BlockPos pos = valueInput.read(direction.getName() + "_pos", BlockPos.CODEC).orElse(BlockPos.ZERO);
            valueInput.getInt(direction.getName() + "_distance").ifPresent(distance -> this.networkDistances.put(direction, new Tuple<>(pos, distance)));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag valueOutput, HolderLookup.Provider registries) {
        super.saveAdditional(valueOutput, registries);
        ListTag itemsList = new ListTag();
        for (ItemInPipe item : this.contents) {
            if (!item.getStack().isEmpty()) {
                MiscUtil.saveToTag(new CompoundTag(), item, ItemInPipe.CODEC, registries, itemsList::add);
            }
        }
        valueOutput.put("items", itemsList);
        for (Direction direction : this.networkDistances.keySet()) {
            Tuple<BlockPos, Integer> tuple = this.networkDistances.get(direction);
            valueOutput.store(direction.getName() + "_pos", BlockPos.CODEC, tuple.a());
            valueOutput.putInt(direction.getName() + "_distance", tuple.b());
        }
    }

    public void clearContent() {
        this.contents.clear();
    }

    public boolean isEmpty() {
        return this.contents.isEmpty();
    }

    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public int getComparatorOutput() {
        return Math.min(15, this.getContents().size());
    }

}

package jagm.classicpipes.blockentity;

import jagm.classicpipes.block.ContainerAdjacentNetworkedPipeBlock;
import jagm.classicpipes.block.NetworkedPipeBlock;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.*;

public abstract class NetworkedPipeEntity extends RoundRobinPipeEntity {

    private final Map<ItemStack, ScheduledRoute> routingSchedule;
    private PipeNetwork network;
    private boolean controller;
    public BlockPos syncedNetworkPos;

    public NetworkedPipeEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
        super(blockEntityType, pos, state);
        this.routingSchedule = new HashMap<>();
        this.network = null;
        this.controller = false;
        this.syncedNetworkPos = this.getBlockPos();
    }

    @Override
    public void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);
        if (!this.routingSchedule.isEmpty()) {
            Iterator<ItemStack> iterator = this.routingSchedule.keySet().iterator();
            while (iterator.hasNext()) {
                ScheduledRoute route = this.routingSchedule.get(iterator.next());
                route.tick();
                if (route.timedOut()) {
                    iterator.remove();
                    this.setChanged();
                }
            }
        }
        if (this.isController()) {
            this.getNetwork().tick(level);
        }
    }

    @Override
    protected void initialiseNetworking(ServerLevel level, BlockState state, BlockPos pos) {
        if (this.isController() && this.hasNetwork()) {
            this.distributeNetwork(level, this.getBlockPos(), new HashSet<>(), this.getNetwork());
        }
        super.initialiseNetworking(level, state, pos);
    }

    protected Set<NetworkedPipeEntity> distributeNetwork(ServerLevel level, BlockPos pos, Set<NetworkedPipeEntity> visited, PipeNetwork network) {
        if (visited.contains(this)) {
            return visited;
        }
        visited.add(this);
        for (Direction direction : this.networkDistances.keySet()) {
            BlockPos nextPos = this.networkDistances.get(direction).a();
            if (level.getBlockEntity(nextPos) instanceof NetworkedPipeEntity nextPipe) {
                visited = nextPipe.distributeNetwork(level, nextPos, visited, network);
            }
        }
        this.setController(pos.equals(network.getPos()));
        this.setNetwork(network, level);
        return visited;
    }

    @Override
    protected List<Direction> getValidDirections(BlockState state, ItemInPipe item) {
        // When reverting to default routing behaviour, networked pipes prefer selecting directions that keep an item within the network.
        // This should reduce the amount of chaos caused by a player forgetting to add a default route.
        List<Direction> validDirections = new ArrayList<>();
        Direction direction = MiscUtil.nextDirection(item.getFromDirection());
        for (int i = 0; i < 5; i++) {
            if (state.getValue(NetworkedPipeBlock.PROPERTY_BY_DIRECTION.get(direction)).equals(NetworkedPipeBlock.ConnectionState.LINKED)) {
                validDirections.add(direction);
            }
            direction = MiscUtil.nextDirection(direction);
        }
        if (validDirections.isEmpty()) {
            return super.getValidDirections(state, item);
        } else {
            return validDirections;
        }
    }

    @Override
    public void routeItem(BlockState state, ItemInPipe item) {
        if (!this.hasNetwork()) {
            super.routeItem(state, item);
            return;
        }
        if (!this.checkRoutingSchedule(item) && this.getLevel() instanceof ServerLevel serverLevel) {
            List<NetworkedPipeEntity> validTargets = new ArrayList<>();
            RequestedItem thisRequestedItem = null;
            List<ItemInPipe> spareItems = new ArrayList<>();
            for (RequestedItem requestedItem : this.getNetwork().getRequestedItems()) {
                if (requestedItem.matches(item) && this.getLevel() != null) {
                    NetworkedPipeEntity target = requestedItem.getTarget(this.getLevel());
                    if (target != null) {
                        if (item.getStack().getCount() > requestedItem.getAmountRemaining()) {
                            spareItems.add(item.copyWithCount(item.getStack().getCount() - requestedItem.getAmountRemaining()));
                            item.getStack().setCount(requestedItem.getAmountRemaining());
                        }
                        thisRequestedItem = requestedItem;
                        validTargets.add(target);
                        break; // First requested item prioritised.
                    }
                }
            }
            if (validTargets.isEmpty()) {
                for (StockingPipeEntity stockingPipe : this.network.getStockingPipes()) {
                    for (ItemStack stack : stockingPipe.getMissingItemsCache()) {
                        if (stack.is(item.getStack().getItem()) && (!stockingPipe.shouldMatchComponents() || ItemStack.isSameItemSameComponents(stack, item.getStack()))) {
                            int surplus = item.getStack().getCount() - stack.getCount();
                            if (surplus > 0) {
                                spareItems.add(item.copyWithCount(surplus));
                                item.getStack().setCount(stack.getCount());
                            }
                            validTargets.add(stockingPipe);
                            break;
                        }
                    }
                }
            }
            if (validTargets.isEmpty()) {
                for (MatchingPipe matchingPipe : this.network.getMatchingPipes()) {
                    if (matchingPipe.matches(item.getStack())) {
                        validTargets.add(matchingPipe.getAsPipe());
                    }
                }
            }
            if (validTargets.isEmpty()) {
                Map<Filter.MatchingResult, List<RoutingPipeEntity>> matchPriority = new HashMap<>();
                matchPriority.put(Filter.MatchingResult.ITEM, new ArrayList<>());
                matchPriority.put(Filter.MatchingResult.TAG, new ArrayList<>());
                matchPriority.put(Filter.MatchingResult.MOD, new ArrayList<>());
                for (RoutingPipeEntity routingPipe : this.network.getRoutingPipes()) {
                    Filter.MatchingResult result = routingPipe.canRouteItemHere(item.getStack());
                    if (result.matches) {
                        matchPriority.get(result).add(routingPipe);
                    }
                }
                if (!matchPriority.get(Filter.MatchingResult.ITEM).isEmpty()) {
                    validTargets.addAll(matchPriority.get(Filter.MatchingResult.ITEM));
                } else if (!matchPriority.get(Filter.MatchingResult.TAG).isEmpty()) {
                    validTargets.addAll(matchPriority.get(Filter.MatchingResult.TAG));
                } else if (!matchPriority.get(Filter.MatchingResult.MOD).isEmpty()) {
                    validTargets.addAll(matchPriority.get(Filter.MatchingResult.MOD));
                }
            }
            if (validTargets.isEmpty()) {
                validTargets.addAll(this.network.getDefaultRoutes());
            }
            if (validTargets.contains(this) && state.getBlock() instanceof NetworkedPipeBlock networkedBlock) {
                List<Direction> validDirections = new ArrayList<>();
                if (networkedBlock instanceof ContainerAdjacentNetworkedPipeBlock && state.getValue(ContainerAdjacentNetworkedPipeBlock.FACING) != FacingOrNone.NONE) {
                    validDirections.add(state.getValue(ContainerAdjacentNetworkedPipeBlock.FACING).getDirection());
                } else {
                    for (Direction direction : Direction.values()) {
                        if (this.isPipeConnected(state, direction) && !networkedBlock.isLinked(state, direction)) {
                            validDirections.add(direction);
                        }
                    }
                }
                if (validDirections.isEmpty() || this instanceof RecipePipeEntity) {
                    item.setEjecting(true);
                    item.setTargetDirection(item.getFromDirection().getOpposite());
                } else {
                    item.setEjecting(false);
                    item.setTargetDirection(validDirections.get(serverLevel.getRandom().nextInt(validDirections.size())));
                }
                if (thisRequestedItem != null) {
                    int remaining = thisRequestedItem.getAmountRemaining();
                    int leftover = item.getStack().getCount() - remaining;
                    thisRequestedItem.arrived(item.getStack().getCount());
                    if (thisRequestedItem.isDelivered()) {
                        this.getNetwork().removeRequestedItem(thisRequestedItem);
                    }
                    if (leftover > 0) {
                        item.setStack(item.getStack().copyWithCount(remaining));
                        spareItems.add(item.copyWithCount(leftover));
                    }
                }
            } else if (!validTargets.isEmpty()) {
                this.schedulePath(serverLevel, item, validTargets.get(serverLevel.getRandom().nextInt(validTargets.size())));
                this.checkRoutingSchedule(item);
            } else {
                super.routeItem(state, item);
            }
            for (ItemInPipe spareItem : spareItems) {
                this.queued.add(spareItem);
                this.routeItem(state, spareItem);
            }
        }
    }

    private boolean checkRoutingSchedule(ItemInPipe item) {
        Iterator<ItemStack> iterator = this.routingSchedule.keySet().iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            if (ItemStack.isSameItemSameComponents(stack, item.getStack()) && stack.getCount() == item.getStack().getCount()) {
                item.setEjecting(false);
                item.setTargetDirection(this.routingSchedule.get(stack).getDirection());
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    public void schedule(ItemStack stack, Direction direction) {
        this.routingSchedule.put(stack, new ScheduledRoute(direction));
    }

    public void schedulePath(ServerLevel level, ItemInPipe item, NetworkedPipeEntity target) {
        Map<NetworkedPipeEntity, Tuple<NetworkedPipeEntity, Direction>> cameFrom = new HashMap<>();
        Map<NetworkedPipeEntity, Integer> gScore = new HashMap<>();
        gScore.put(this, 0);
        Map<NetworkedPipeEntity, Integer> fScore = new HashMap<>();
        fScore.put(this, 0);
        PriorityQueue<NetworkedPipeEntity> openSet = new PriorityQueue<>(Comparator.comparingInt(fScore::get));
        openSet.add(this);
        while (!openSet.isEmpty()) {
            NetworkedPipeEntity current = openSet.poll();
            if (current == target) {
                while (cameFrom.containsKey(current)) {
                    Tuple<NetworkedPipeEntity, Direction> tuple = cameFrom.get(current);
                    current = tuple.a();
                    current.schedule(item.getStack(), tuple.b());
                    current.setChanged();
                    level.sendBlockUpdated(current.getBlockPos(), current.getBlockState(), current.getBlockState(), 2);
                }
            }
            for (Direction side : current.networkDistances.keySet()) {
                Tuple<BlockPos, Integer> tuple = current.networkDistances.get(side);
                if (level.getBlockEntity(tuple.a()) instanceof NetworkedPipeEntity neighbour) {
                    int newScore = gScore.get(current) + tuple.b();
                    if (!gScore.containsKey(neighbour) || newScore < gScore.get(neighbour)) {
                        cameFrom.put(neighbour, new Tuple<>(current, side));
                        gScore.put(neighbour, newScore);
                        fScore.put(neighbour, newScore + this.worldPosition.distChessboard(neighbour.worldPosition));
                        if (!openSet.contains(neighbour)) {
                            openSet.add(neighbour);
                        }
                    }
                }
            }
        }
    }

    public void setNetwork(PipeNetwork network, ServerLevel level) {
        if (network != null) {
            network.addPipe(this);
        }
        this.network = network;
        this.setChanged();
        if (level != null) {
            level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 2);
        }
    }

    public PipeNetwork getNetwork() {
        return this.network;
    }

    public boolean hasNetwork() {
        return this.network != null;
    }

    public void setController(boolean controller) {
        this.controller = controller;
    }

    public boolean isController() {
        return this.controller;
    }

    public void disconnect(ServerLevel level) {
        this.setController(false);
        this.routingSchedule.clear();
        if (this.hasNetwork()) {
            this.getNetwork().removePipe(level, this);
        }
        this.setNetwork(null, level);
    }

    public Tuple<Boolean, Set<NetworkedPipeEntity>> stillLinkedToNetwork(PipeNetwork network, ServerLevel level, BlockPos thisPos, Set<NetworkedPipeEntity> visited) {
        if (visited.contains(this)) {
            return new Tuple<>(false, visited);
        }
        visited.add(this);
        if (network.getPos().equals(thisPos)) {
            return new Tuple<>(true, visited);
        }
        for (Direction direction : this.networkDistances.keySet()) {
            BlockPos nextPos = this.networkDistances.get(direction).a();
            if (level.getBlockEntity(nextPos) instanceof NetworkedPipeEntity nextPipe) {
                Tuple<Boolean, Set<NetworkedPipeEntity>> tuple = nextPipe.stillLinkedToNetwork(network, level, nextPos, visited);
                visited = tuple.b();
                if (tuple.a()) {
                    return tuple;
                }
            }
        }
        return new Tuple<>(false, visited);
    }

    public Tuple<PipeNetwork, Set<NetworkedPipeEntity>> findLinkedNetwork(ServerLevel level, BlockPos thisPos, Set<NetworkedPipeEntity> visited) {
        if (visited.contains(this)) {
            return new Tuple<>(null, visited);
        }
        visited.add(this);
        if (this.hasNetwork() && this.getNetwork().getPos().equals(thisPos)) {
            return new Tuple<>(this.getNetwork(), visited);
        }
        for (Direction direction : this.networkDistances.keySet()) {
            BlockPos nextPos = this.networkDistances.get(direction).a();
            if (level.getBlockEntity(nextPos) instanceof NetworkedPipeEntity nextPipe) {
                Tuple<PipeNetwork, Set<NetworkedPipeEntity>> tuple = nextPipe.findLinkedNetwork(level, nextPos, visited);
                visited = tuple.b();
                if (tuple.a() != null) {
                    return tuple;
                }
            }
        }
        return new Tuple<>(null, visited);
    }

    public Set<NetworkedPipeEntity> disconnectAllLinked(ServerLevel level, Set<NetworkedPipeEntity> visited) {
        if (visited.contains(this)) {
            return visited;
        }
        visited.add(this);
        this.disconnect(level);
        for (Direction direction : this.networkDistances.keySet()) {
            BlockPos nextPos = this.networkDistances.get(direction).a();
            if (level.getBlockEntity(nextPos) instanceof NetworkedPipeEntity nextPipe) {
                visited = nextPipe.disconnectAllLinked(level, visited);
            }
        }
        return visited;
    }

    public void networkChanged(ServerLevel level, BlockPos pos, boolean isLinked) {
        if (this.hasNetwork()) {
            if (!isLinked) {
                if (!this.stillLinkedToNetwork(this.getNetwork(), level, pos, new HashSet<>()).a()) {
                    this.disconnectAllLinked(level, new HashSet<>());
                    this.distributeNetwork(level, pos, new HashSet<>(), new PipeNetwork(pos));
                }
            } else {
                this.distributeNetwork(level, pos, new HashSet<>(), this.getNetwork());
            }
        } else {
            PipeNetwork network = this.findLinkedNetwork(level, pos, new HashSet<>()).a();
            if (network != null) {
                this.setNetwork(network, level);
                this.setController(network.getPos().equals(pos));
            } else {
                this.distributeNetwork(level, pos, new HashSet<>(), new PipeNetwork(pos));
            }
        }
    }

    @Override
    public void setRemoved() {
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            this.disconnect(serverLevel);
        }
        super.setRemoved();
    }

    @Override
    public short getTargetSpeed() {
        return ItemInPipe.SPEED_LIMIT;
    }

    @Override
    public short getAcceleration() {
        return ItemInPipe.DEFAULT_ACCELERATION * 64;
    }

    public boolean isDefaultRoute() {
        return false;
    }

    @Override
    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.routingSchedule.clear();
        this.setController(valueInput.getBooleanOr("controller", false));
        if (this.isController()) {
            this.network = new PipeNetwork(this.getBlockPos(), SortingMode.fromByte(valueInput.getByteOr("sorting_mode", (byte) 1)));
            ValueInput.TypedInputList<RequestedItem> requestedItems = valueInput.listOrEmpty("requested_items", RequestedItem.CODEC);
            for (RequestedItem requestedItem : requestedItems) {
                this.network.addRequestedItem(requestedItem);
            }
            this.syncedNetworkPos = this.getBlockPos();
        } else {
            this.syncedNetworkPos = valueInput.read("synced_network_pos", BlockPos.CODEC).orElse(this.getBlockPos());
        }
        ValueInput.TypedInputList<ItemStackWithSlot> routingList = valueInput.listOrEmpty("routing_schedule", ItemStackWithSlot.CODEC);
        for (ItemStackWithSlot slotStack : routingList) {
            this.routingSchedule.put(slotStack.stack(), new ScheduledRoute(Direction.from3DDataValue(slotStack.slot())));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.putBoolean("controller", this.isController());
        if (this.hasNetwork() && this.isController()) {
            valueOutput.putByte("sorting_mode", this.getNetwork().getSortingMode().getValue());
            ValueOutput.TypedOutputList<RequestedItem> requestedItems = valueOutput.list("requested_items", RequestedItem.CODEC);
            for (RequestedItem requestedItem : this.getNetwork().getRequestedItems()) {
                if (!requestedItem.isDelivered()) {
                    requestedItems.add(requestedItem);
                }
            }
        }
        valueOutput.store("synced_network_pos", BlockPos.CODEC, this.hasNetwork() ? this.network.getPos() : this.getBlockPos());
        ValueOutput.TypedOutputList<ItemStackWithSlot> routingList = valueOutput.list("routing_schedule", ItemStackWithSlot.CODEC);
        for (ItemStack stack : this.routingSchedule.keySet()) {
            routingList.add(new ItemStackWithSlot(this.routingSchedule.get(stack).getDirection().get3DDataValue(), stack));
        }
    }

}

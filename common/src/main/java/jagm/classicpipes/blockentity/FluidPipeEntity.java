package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.FluidInPipe;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.*;

public class FluidPipeEntity extends PipeEntity {

    public static final int CAPACITY = 1000;
    public static final int MIN_PACKET_SIZE = 20;

    protected Fluid fluid;
    protected final List<FluidInPipe> contents;
    protected final List<FluidInPipe> queued;
    private final Map<FluidInPipe, Long> tickAdded;

    public FluidPipeEntity(BlockPos pos, BlockState state) {
        this(ClassicPipes.FLUID_PIPE_ENTITY, pos, state);
    }

    public FluidPipeEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.contents = new ArrayList<>();
        this.queued = new ArrayList<>();
        this.tickAdded = new HashMap<>();
        this.fluid = Fluids.WATER;
    }

    @Override
    public void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        boolean sendBlockUpdate = false;
        if (!this.contents.isEmpty()) {
            ListIterator<FluidInPipe> iterator = this.contents.listIterator();
            while (iterator.hasNext()) {
                FluidInPipe fluidPacket = iterator.next();
                if (this.tickAdded.containsKey(fluidPacket)) {
                    if (this.tickAdded.get(fluidPacket) == level.getGameTime()) {
                        continue;
                    }
                    this.tickAdded.remove(fluidPacket);
                }
                fluidPacket.move(this.getTargetSpeed(), this.getAcceleration());
                if (fluidPacket.getAge() > ItemInPipe.DESPAWN_AGE) {
                    iterator.remove();
                    sendBlockUpdate = true;
                    continue;
                }
                if (fluidPacket.getProgress() >= ItemInPipe.PIPE_LENGTH) {
                    boolean remove = false;
                    BlockPos containerPos = pos.relative(fluidPacket.getTargetDirection());
                    BlockEntity blockEntity = level.getBlockEntity(containerPos);
                    if (blockEntity instanceof FluidPipeEntity nextPipe) {
                        if (nextPipe.emptyOrMatches(fluid)) {
                            nextPipe.setFluid(fluid);
                            remove = true;
                            int amountToPass = Math.min(fluidPacket.getAmount(), nextPipe.remainingCapacity());
                            if (amountToPass == fluidPacket.getAmount()) {
                                fluidPacket.resetProgress(fluidPacket.getTargetDirection().getOpposite());
                                nextPipe.insertFluidPacket(level, fluidPacket);
                            } else {
                                remove = false;
                                if (amountToPass >= FluidPipeEntity.MIN_PACKET_SIZE) {
                                    FluidInPipe newPacket = fluidPacket.copyWithAmount(amountToPass);
                                    newPacket.resetProgress(fluidPacket.getTargetDirection().getOpposite());
                                    nextPipe.insertFluidPacket(level, newPacket);
                                    fluidPacket.setAmount(fluidPacket.getAmount() - amountToPass);
                                }
                            }
                            level.sendBlockUpdated(containerPos, nextPipe.getBlockState(), nextPipe.getBlockState(), 2);
                        }
                    } else if (blockEntity != null) {
                        remove = Services.LOADER_SERVICE.handleFluidInsertion(this, level, pos, state, blockEntity, containerPos, this.fluid, fluidPacket);
                    }
                    if (remove) {
                        iterator.remove();
                    } else {
                        fluidPacket.resetProgress(fluidPacket.getTargetDirection());
                        this.routePacket(state, fluidPacket);
                    }
                    sendBlockUpdate = true;
                }
            }
            this.addQueuedPackets(level, false);
        }
        if (sendBlockUpdate) {
            level.sendBlockUpdated(pos, state, state, 2);
        }
    }

    @Override
    public void tickClient(Level level, BlockPos pos) {
        if (!this.contents.isEmpty()) {
            ListIterator<FluidInPipe> iterator = this.contents.listIterator();
            while (iterator.hasNext()) {
                FluidInPipe fluidPacket = iterator.next();
                if (this.tickAdded.containsKey(fluidPacket)) {
                    if (this.tickAdded.get(fluidPacket) == level.getGameTime()) {
                        continue;
                    }
                    this.tickAdded.remove(fluidPacket);
                }
                fluidPacket.move(this.getTargetSpeed(), this.getAcceleration());
                if (fluidPacket.getProgress() >= ItemInPipe.PIPE_LENGTH) {
                    BlockPos nextPos = pos.relative(fluidPacket.getTargetDirection());
                    if (level.getBlockEntity(nextPos) instanceof FluidPipeEntity nextPipe && nextPipe.emptyOrMatches(this.fluid)) {
                        fluidPacket.resetProgress(fluidPacket.getTargetDirection().getOpposite());
                        nextPipe.setFluid(this.fluid);
                        nextPipe.insertFluidPacket(level, fluidPacket);
                        iterator.remove();
                    }
                }
            }
        }
    }

    @Override
    public void update(ServerLevel level, BlockState state, BlockPos pos, Direction direction, boolean wasConnected) {
        if (!this.contents.isEmpty()) {
            ListIterator<FluidInPipe> iterator = this.contents.listIterator();
            while (iterator.hasNext()) {
                FluidInPipe fluidPacket = iterator.next();
                if (!wasConnected || (fluidPacket.getTargetDirection() == direction && fluidPacket.getProgress() < ItemInPipe.HALFWAY)) {
                    this.routePacket(state, fluidPacket);
                } else if ((fluidPacket.getFromDirection() == direction && fluidPacket.getProgress() < ItemInPipe.HALFWAY) || (fluidPacket.getTargetDirection() == direction && fluidPacket.getProgress() >= ItemInPipe.HALFWAY)) {
                    iterator.remove();
                    // TODO spawn particles or something?
                }
            }
            this.addQueuedPackets(level, false);
        }
        this.setChanged();
        level.sendBlockUpdated(pos, state, state, 2);
    }

    @Override
    public int getComparatorOutput() {
        if (this.contents.isEmpty()) {
            return 0;
        }
        return Math.max(1, Math.round(15.0F * (float) this.totalAmount() / CAPACITY));
    }

    @Override
    public short getTargetSpeed() {
        //TODO different speeds for thick and thin fluids.
        return ItemInPipe.DEFAULT_SPEED;
    }

    @Override
    public short getAcceleration() {
        return ItemInPipe.DEFAULT_ACCELERATION;
    }

    @Override
    protected void loadAdditional(ValueInput valueInput) {
        this.contents.clear();
        this.tickAdded.clear();
        super.loadAdditional(valueInput);
        ValueInput.TypedInputList<FluidInPipe> fluidPacketList = valueInput.listOrEmpty("fluid_packets", FluidInPipe.CODEC);
        fluidPacketList.forEach(this.contents::add);
        this.setFluid(valueInput.read("fluid", BuiltInRegistries.FLUID.byNameCodec()).orElse(Fluids.WATER));
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        ValueOutput.TypedOutputList<FluidInPipe> fluidPacketList = valueOutput.list("fluid_packets", FluidInPipe.CODEC);
        for (FluidInPipe fluidPacket : this.contents) {
            if (fluidPacket.getAmount() > 0) {
                fluidPacketList.add(fluidPacket);
            }
        }
        valueOutput.store("fluid", BuiltInRegistries.FLUID.byNameCodec(), this.fluid);
    }

    public void addQueuedPackets(Level level, boolean waitForNextTick) {
        for (FluidInPipe fluidPacket : this.queued) {
            this.contents.add(fluidPacket);
            if (waitForNextTick) {
                this.tickAdded.put(fluidPacket, level.getGameTime());
            }
        }
        this.setChanged();
        this.queued.clear();
    }

    public boolean emptyOrMatches(Fluid fluid) {
        return this.contents.isEmpty() || this.fluid == fluid;
    }

    public int totalAmount() {
        int total = 0;
        for (FluidInPipe fluidPacket : this.contents) {
            total += fluidPacket.getAmount();
        }
        return total;
    }

    public int remainingCapacity() {
        return CAPACITY - this.totalAmount();
    }

    public void setFluid(Fluid fluid) {
        this.fluid = fluid;
    }

    public void insertFluidPacket(Level level, FluidInPipe fluidPacket) {
        this.queued.add(fluidPacket);
        this.routePacket(fluidPacket);
        this.addQueuedPackets(level, true);
    }

    protected List<Direction> getValidDirections(BlockState state, FluidInPipe fluidPacket) {
        List<Direction> validDirections = new ArrayList<>();
        Direction direction = MiscUtil.nextDirection(fluidPacket.getFromDirection());
        for (int i = 0; i < 5; i++) {
            if (this.isPipeConnected(state, direction)) {
                validDirections.add(direction);
            }
            direction = MiscUtil.nextDirection(direction);
        }
        return validDirections;
    }

    public void routePacket(BlockState state, FluidInPipe fluidPacket) {
        List<Direction> validDirections = this.getValidDirections(state, fluidPacket);
        int numDirections = validDirections.size();
        if (numDirections == 0) {
            fluidPacket.setTargetDirection(fluidPacket.getFromDirection());
        } else if (numDirections == 1) {
            fluidPacket.setTargetDirection(validDirections.getFirst());
        } else {
            if (fluidPacket.getAmount() > MIN_PACKET_SIZE) {
                int splitAmount = fluidPacket.getAmount() / numDirections;
                int leftoverAmount = fluidPacket.getAmount() % numDirections;
                Collections.shuffle(validDirections);
                for (int i = 0; i < numDirections; i++) {
                    if (i == 0) {
                        fluidPacket.setAmount(splitAmount + leftoverAmount);
                        fluidPacket.setTargetDirection(validDirections.get(i));
                    } else {
                        FluidInPipe newPacket = fluidPacket.copyWithAmount(splitAmount);
                        newPacket.setTargetDirection(validDirections.get(i));
                        this.queued.add(newPacket);
                    }
                }
            } else if (this.getLevel() != null) {
                fluidPacket.setTargetDirection(validDirections.get(this.getLevel().getRandom().nextInt(validDirections.size())));
            }
        }
    }

    public void routePacket(FluidInPipe fluidPacket) {
        this.routePacket(this.getBlockState(), fluidPacket);
    }

    public Fluid getFluid() {
        return this.fluid;
    }

    public boolean isEmpty() {
        return this.contents.isEmpty();
    }

    public List<FluidInPipe> getContents() {
        return this.contents;
    }

}

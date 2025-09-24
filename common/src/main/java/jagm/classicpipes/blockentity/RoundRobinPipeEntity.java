package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoundRobinPipeEntity extends ItemPipeEntity {

    protected Direction nextDirection;

    public RoundRobinPipeEntity(BlockPos pos, BlockState state) {
        this(ClassicPipes.BASIC_PIPE_ENTITY, pos, state);
    }

    public RoundRobinPipeEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
        super(blockEntityType, pos, state);
        this.nextDirection = Direction.DOWN;
    }

    protected void updateRoundRobin(List<Direction> validDirections) {
        if (!validDirections.isEmpty()) {
            do {
                this.nextDirection = MiscUtil.nextDirection(this.nextDirection);
            } while (!validDirections.contains(this.nextDirection));
        }
    }

    protected List<Direction> getValidDirections(BlockState state, ItemInPipe item) {
        List<Direction> validDirections = new ArrayList<>();
        Direction direction = MiscUtil.nextDirection(item.getFromDirection());
        for (int i = 0; i < 5; i++) {
            if (this.isPipeConnected(state, direction)) {
                validDirections.add(direction);
            }
            direction = MiscUtil.nextDirection(direction);
        }
        return validDirections;
    }

    @Override
    public void routeItem(BlockState state, ItemInPipe item) {
        List<Direction> validDirections = this.getValidDirections(state, item);
        if (validDirections.size() == 1) {
            item.setTargetDirection(validDirections.get(0));
            item.setEjecting(false);
        } else if (validDirections.isEmpty() || this.nextDirection == null) {
            item.setTargetDirection(item.getFromDirection().getOpposite());
            item.setEjecting(true);
        } else {
            if (!validDirections.contains(this.nextDirection)) {
                this.updateRoundRobin(validDirections);
            }
            int count = item.getStack().getCount();
            if (count == 1) {
                item.setTargetDirection(this.nextDirection);
                item.setEjecting(false);
                this.updateRoundRobin(validDirections);
            } else {
                Map<Direction, Integer> routeMap = new HashMap<>();
                validDirections.forEach(direction -> routeMap.put(direction, 0));
                for (int i = 0; i < count; i++) {
                    routeMap.put(this.nextDirection, routeMap.get(this.nextDirection) + 1);
                    this.updateRoundRobin(validDirections);
                }
                boolean inputRouted = false;
                for (Direction direction : validDirections) {
                    int routeCount = routeMap.get(direction);
                    if (routeCount > 0) {
                        if (!inputRouted) {
                            inputRouted = true;
                            item.setStack(item.getStack().copyWithCount(routeCount));
                            item.setTargetDirection(direction);
                            item.setEjecting(false);
                        } else {
                            this.queued.add(new ItemInPipe(item.getStack().copyWithCount(routeCount), item.getSpeed(), item.getProgress(), item.getFromDirection(), direction, false, item.getAge()));
                        }
                    }
                }
            }
        }
    }

    @Override
    public short getTargetSpeed() {
        return ItemInPipe.DEFAULT_SPEED;
    }

    @Override
    public short getAcceleration() {
        return ItemInPipe.DEFAULT_ACCELERATION;
    }

    @Override
    public void load(CompoundTag valueInput) {
        super.load(valueInput);
        this.nextDirection = Direction.from3DDataValue(valueInput.getByte("next_direction"));
    }

    @Override
    protected void saveAdditional(CompoundTag valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.putByte("next_direction", (byte) this.nextDirection.get3DDataValue());
    }

}

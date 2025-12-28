package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BonePipeEntity extends ItemPipeEntity {

    public BonePipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.BONE_PIPE_ENTITY, pos, state);
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
        if (!validDirections.isEmpty() && this.getLevel() != null) {
            int count = item.getStack().getCount();
            if (count == 1) {
                item.setTargetDirection(validDirections.get(this.getLevel().getRandom().nextInt(validDirections.size())));
                item.setEjecting(false);
            } else {
                Map<Direction, Integer> routeMap = new HashMap<>();
                validDirections.forEach(direction -> routeMap.put(direction, 0));
                for (int i = 0; i < count; i++) {
                    Direction direction = validDirections.get(this.getLevel().getRandom().nextInt(validDirections.size()));
                    routeMap.put(direction, routeMap.get(direction) + 1);
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
        } else {
            item.setTargetDirection(item.getFromDirection().getOpposite());
            item.setEjecting(true);
        }
    }

    @Override
    public short getTargetSpeed(BlockState state, Direction fromDirection, Direction targetDirection) {
        return ItemInPipe.DEFAULT_SPEED;
    }

    @Override
    public short getAcceleration(BlockState state, Direction fromDirection, Direction targetDirection) {
        return ItemInPipe.DEFAULT_ACCELERATION;
    }

}

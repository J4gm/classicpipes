package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.LapisFluidPipeBlock;
import jagm.classicpipes.util.FacingOrNone;
import jagm.classicpipes.util.FluidInPipe;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class LapisFluidPipeEntity extends FluidPipeEntity {

    private Direction entryDirection;

    public LapisFluidPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.LAPIS_FLUID_PIPE_ENTITY, pos, state);
        this.entryDirection = Direction.DOWN;
    }

    @Override
    public void insertFluidPacket(Level level, FluidInPipe fluidPacket) {
        this.entryDirection = fluidPacket.getFromDirection();
        super.insertFluidPacket(level, fluidPacket);
    }

    @Override
    protected List<Direction> getValidDirections(BlockState state, FluidInPipe fluidPacket) {
        List<Direction> validDirections = new ArrayList<>();
        Direction facing = state.getValue(LapisFluidPipeBlock.FACING).getDirection();
        boolean attached = state.getValue(LapisFluidPipeBlock.FACING) != FacingOrNone.NONE;
        if (attached && !fluidPacket.getFromDirection().equals(facing) && this.isPipeConnected(state, facing)) {
            validDirections.add(facing);
        } else {
            Direction direction = MiscUtil.nextDirection(fluidPacket.getFromDirection());
            for (int i = 0; i < 5; i++) {
                if (this.isPipeConnected(state, direction) && !direction.equals(this.entryDirection)) {
                    validDirections.add(direction);
                }
                direction = MiscUtil.nextDirection(direction);
            }
            if (validDirections.isEmpty() && this.isPipeConnected(state, this.entryDirection) && attached) {
                validDirections.add(this.entryDirection);
            }
        }
        return validDirections;
    }

    @Override
    public void load(CompoundTag valueInput) {
        super.load(valueInput);
        this.entryDirection = Direction.from3DDataValue(valueInput.getByte("entry_direction"));
    }

    @Override
    protected void saveAdditional(CompoundTag valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.putByte("entry_direction", (byte) this.entryDirection.get3DDataValue());
    }

}

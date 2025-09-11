package jagm.classicpipes.blockentity;

import jagm.classicpipes.util.FluidInPipe;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class NeoForgeFluidPipeWrapper implements IFluidHandler {

    private final FluidPipeEntity pipe;
    private final Direction side;

    public NeoForgeFluidPipeWrapper(FluidPipeEntity pipe, Direction side) {
        this.pipe = pipe;
        this.side = side;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int i) {
        return new FluidStack(this.pipe.getFluid(), this.pipe.totalAmount());
    }

    @Override
    public int getTankCapacity(int i) {
        return FluidPipeEntity.CAPACITY;
    }

    @Override
    public boolean isFluidValid(int i, FluidStack fluidStack) {
        return true;
    }

    @Override
    public int fill(FluidStack fluidStack, FluidAction fluidAction) {
        if (fluidStack.isEmpty() || !this.pipe.emptyOrMatches(fluidStack.getFluid())) {
            return 0;
        } else {
            int amount = Math.min(this.pipe.remainingCapacity(), fluidStack.getAmount());
            if (fluidAction.execute()) {
                if (this.pipe.getLevel() instanceof ServerLevel serverLevel) {
                    this.pipe.setFluid(fluidStack.getFluid());
                    FluidInPipe fluidPacket = new FluidInPipe(amount, this.pipe.getTargetSpeed(), (short) 0, this.side, this.side, (short) 0);
                    this.pipe.insertFluidPacket(serverLevel, fluidPacket);
                    serverLevel.sendBlockUpdated(this.pipe.getBlockPos(), this.pipe.getBlockState(), this.pipe.getBlockState(), 2);
                }
                this.pipe.setChanged();
            }
            return amount;
        }
    }

    @Override
    public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int i, FluidAction fluidAction) {
        return FluidStack.EMPTY;
    }

}

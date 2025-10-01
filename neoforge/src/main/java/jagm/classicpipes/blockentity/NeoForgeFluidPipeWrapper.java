package jagm.classicpipes.blockentity;

import jagm.classicpipes.util.FluidInPipe;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class NeoForgeFluidPipeWrapper implements ResourceHandler<FluidResource> {

    private final FluidPipeEntity pipe;
    private final Direction side;

    public NeoForgeFluidPipeWrapper(FluidPipeEntity pipe, Direction side) {
        this.pipe = pipe;
        this.side = side;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int tank) {
        return FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int tank) {
        return 0;
    }

    @Override
    public long getCapacityAsLong(int tank, FluidResource fluidResource) {
        return 1000;
    }

    @Override
    public boolean isValid(int tank, FluidResource fluidResource) {
        return this.pipe.isPipeConnected(this.pipe.getBlockState(), this.side);
    }

    @Override
    public int insert(int tank, FluidResource fluidResource, int maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0 || !this.pipe.emptyOrMatches(fluidResource.getFluid()) || !this.isValid(tank, fluidResource)) {
            return 0;
        } else {
            int amount = Math.min(this.pipe.remainingCapacity(), maxAmount);
            if (this.pipe.getLevel() instanceof ServerLevel serverLevel) {
                this.pipe.setFluid(fluidResource.getFluid());
                FluidInPipe fluidPacket = new FluidInPipe(amount, this.pipe.getTargetSpeed(), (short) 0, this.side, this.side, (short) 0);
                this.pipe.insertFluidPacket(serverLevel, fluidPacket);
                serverLevel.sendBlockUpdated(this.pipe.getBlockPos(), this.pipe.getBlockState(), this.pipe.getBlockState(), 2);
            }
            this.pipe.setChanged();
            return amount;
        }
    }

    @Override
    public int extract(int tank, FluidResource fluidResource, int amount, TransactionContext transaction) {
        return 0;
    }
}

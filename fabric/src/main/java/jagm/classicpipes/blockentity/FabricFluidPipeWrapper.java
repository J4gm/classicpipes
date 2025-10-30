package jagm.classicpipes.blockentity;

import jagm.classicpipes.FabricEntrypoint;
import jagm.classicpipes.util.FluidInPipe;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class FabricFluidPipeWrapper implements Storage<FluidVariant>, StorageView<FluidVariant> {

    private final FluidPipeEntity pipe;
    private final Direction side;

    public FabricFluidPipeWrapper(FluidPipeEntity pipe, Direction side) {
        this.pipe = pipe;
        this.side = side;
    }

    @Override
    public boolean supportsInsertion() {
        return this.side != null && this.pipe.isPipeConnected(this.pipe.getBlockState(), this.side);
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }

    @Override
    public long insert(FluidVariant fluidVariant, long maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0 || !this.pipe.emptyOrMatches(fluidVariant.getFluid()) || !this.supportsInsertion()) {
            return 0L;
        } else {
            long amount = Math.min(this.pipe.remainingCapacity() * FabricEntrypoint.FLUID_CONVERSION_RATE, maxAmount);
            transaction.addCloseCallback((closingTransaction, result) -> {
                if (result.wasCommitted()) {
                    if (this.pipe.getLevel() instanceof ServerLevel serverLevel) {
                        this.pipe.setFluid(fluidVariant.getFluid());
                        FluidInPipe fluidPacket = new FluidInPipe((int) (amount / FabricEntrypoint.FLUID_CONVERSION_RATE), this.pipe.getTargetSpeed(), (short) 0, this.side, this.side, (short) 0);
                        this.pipe.insertFluidPacket(serverLevel, fluidPacket);
                        serverLevel.sendBlockUpdated(this.pipe.getBlockPos(), this.pipe.getBlockState(), this.pipe.getBlockState(), 2);
                    }
                    this.pipe.setChanged();
                }
            });
            return amount;
        }
    }

    @Override
    public long extract(FluidVariant fluidVariant, long maxAmount, TransactionContext transaction) {
        return 0L;
    }

    @Override
    public boolean isResourceBlank() {
        return this.pipe.isEmpty();
    }

    @Override
    public FluidVariant getResource() {
        return this.pipe.isEmpty() ? FluidVariant.blank() : FluidVariant.of(this.pipe.getFluid());
    }

    @Override
    public long getAmount() {
        return this.pipe.totalAmount() * FabricEntrypoint.FLUID_CONVERSION_RATE;
    }

    @Override
    public long getCapacity() {
        return FluidPipeEntity.CAPACITY * FabricEntrypoint.FLUID_CONVERSION_RATE;
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        return new Iterator<>() {

            boolean hasNext = true;

            @Override
            public boolean hasNext() {
                return this.hasNext;
            }

            @Override
            public StorageView<FluidVariant> next() {
                if (!this.hasNext) {
                    throw new NoSuchElementException();
                } else {
                    this.hasNext = false;
                    return FabricFluidPipeWrapper.this;
                }
            }

        };
    }

}

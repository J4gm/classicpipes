package jagm.classicpipes.blockentity;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class NeoForgeItemPipeWrapper implements ResourceHandler<ItemResource> {

    private final ItemPipeEntity pipe;
    private final Direction side;

    public NeoForgeItemPipeWrapper(ItemPipeEntity pipe, Direction side) {
        this.pipe = pipe;
        this.side = side;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public ItemResource getResource(int slot) {
        return ItemResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int slot) {
        return 0;
    }

    @Override
    public long getCapacityAsLong(int slot, ItemResource itemResource) {
        return 64;
    }

    @Override
    public boolean isValid(int slot, ItemResource itemResource) {
        return this.pipe.isPipeConnected(this.pipe.getBlockState(), this.side);
    }

    @Override
    public int insert(int slot, ItemResource itemResource, int amount, TransactionContext transaction) {
        if (this.isValid(slot, itemResource)) {
            int amountToInsert = Math.min(amount, 64);
            this.pipe.setItem(this.side, itemResource.toStack(amountToInsert));
            return amountToInsert;
        }
        return 0;
    }

    @Override
    public int extract(int slot, ItemResource itemResource, int amount, TransactionContext transaction) {
        return 0;
    }
}

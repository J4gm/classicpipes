package jagm.classicpipes.blockentity;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class FabricItemPipeWrapper implements Storage<ItemVariant>, StorageView<ItemVariant> {

    private final ItemPipeEntity pipe;
    private final Direction side;

    public FabricItemPipeWrapper(ItemPipeEntity pipe, Direction side) {
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
    public long insert(ItemVariant itemVariant, long amount, TransactionContext transaction) {
        if (this.supportsInsertion()) {
            int amountToInsert = (int) Math.min(amount, 64);
            transaction.addCloseCallback((closingTransaction, result) -> {
                if (result.wasCommitted()) {
                    this.pipe.setItem(this.side, itemVariant.toStack(amountToInsert));
                }
            });
            return amountToInsert;
        }
        return 0;
    }

    @Override
    public long extract(ItemVariant itemVariant, long amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public boolean isResourceBlank() {
        return true;
    }

    @Override
    public ItemVariant getResource() {
        return ItemVariant.blank();
    }

    @Override
    public long getAmount() {
        return 0;
    }

    @Override
    public long getCapacity() {
        return 64;
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        return new Iterator<>() {

            boolean hasNext = true;

            @Override
            public boolean hasNext() {
                return this.hasNext;
            }

            @Override
            public StorageView<ItemVariant> next() {
                if (!this.hasNext) {
                    throw new NoSuchElementException();
                } else {
                    this.hasNext = false;
                    return FabricItemPipeWrapper.this;
                }
            }

        };
    }
}

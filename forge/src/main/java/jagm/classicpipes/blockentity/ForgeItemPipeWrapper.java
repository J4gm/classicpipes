package jagm.classicpipes.blockentity;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class ForgeItemPipeWrapper implements IItemHandler {

    private final ItemPipeEntity pipe;
    private final Direction side;

    public ForgeItemPipeWrapper(ItemPipeEntity pipe, Direction side) {
        this.pipe = pipe;
        this.side = side;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!simulate && this.isItemValid(slot, stack)) {
            this.pipe.setItem(this.side, stack);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return this.side != null && this.pipe.isPipeConnected(this.pipe.getBlockState(), this.side);
    }

}

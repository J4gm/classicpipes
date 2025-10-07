package jagm.classicpipes.inventory.menu;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.StockingPipeEntity;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.container.FilterContainer;
import jagm.classicpipes.network.ClientBoundTwoBoolsPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class StockingPipeMenu extends FilterMenu {

    private boolean activeStocking;

    public StockingPipeMenu(int id, Inventory playerInventory, ClientBoundTwoBoolsPayload payload) {
        this(id, playerInventory, new FilterContainer(null, 9, payload.first()), payload.second());
        payload.items().forEach(stackWithSlot -> this.getFilter().setItem(stackWithSlot.slot(), stackWithSlot.stack()));
    }

    public StockingPipeMenu(int id, Inventory playerInventory, Filter filter, boolean activeStocking) {
        super(ClassicPipes.STOCKING_PIPE_MENU, id, filter);
        this.activeStocking = activeStocking;
        for (int j = 0; j < 9; j++) {
            this.addSlot(new FilterSlot(filter, j, 8 + j * 18, 18));
        }
        this.addStandardInventorySlots(playerInventory, 8, 84);
    }

    @Override
    public void clicked(int index, int button, ClickType clickType, Player player) {
        if (index >= this.getFilter().getContainerSize() || index < 0 || clickType.equals(ClickType.CLONE)) {
            super.clicked(index, button, clickType, player);
        } else {
            Slot slot = this.slots.get(index);
            ItemStack stack = slot.getItem();
            if (clickType == ClickType.QUICK_MOVE) {
                slot.set(ItemStack.EMPTY);
            } else if (clickType == ClickType.PICKUP) {
                if (stack.isEmpty()) {
                    slot.set(this.getCarried().copyWithCount(button == 0 ? this.getCarried().getCount() : 1));
                } else {
                    if (this.getCarried().isEmpty()) {
                        stack.grow(button == 0 ? -1 : 1);
                        if (stack.isEmpty()) {
                            slot.set(ItemStack.EMPTY);
                        }
                    } else if (ItemStack.isSameItemSameComponents(stack, this.getCarried())) {
                        stack.grow(1);
                    } else {
                        slot.set(this.getCarried().copyWithCount(1));
                    }
                    if (stack.getCount() > 999) {
                        stack.setCount(999);
                    }
                }
            }
            slot.setChanged();
        }
    }

    public boolean isActiveStocking() {
        return this.activeStocking;
    }

    public void setActiveStocking(boolean activeStocking) {
        this.activeStocking = activeStocking;
        if (this.getFilter().getPipe() instanceof StockingPipeEntity stockingPipe) {
            stockingPipe.setActiveStocking(activeStocking);
        }
    }

}

package jagm.classicpipes.inventory.menu;

import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.FluidWithData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public abstract class FluidFilterMenu extends FilterMenu {

    protected FluidFilterMenu(MenuType<?> menuType, int id, Filter filter) {
        super(menuType, id, filter);
    }

    @Override
    public void clicked(int index, int button, ClickType clickType, Player player) {
        if (index >= this.getFilter().getContainerSize() || index < 0 || clickType.equals(ClickType.CLONE)) {
            super.clicked(index, button, clickType, player);
        } else {
            Slot slot = this.slots.get(index);
            if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && (button == 0 || button == 1)) {
                if (this.getCarried().isEmpty() || clickType == ClickType.QUICK_MOVE) {
                    slot.remove(1);
                    slot.setChanged();
                } else {
                    FluidWithData fluid = Services.LOADER_SERVICE.getFluidFromStack(this.getCarried());
                    if (!fluid.isBlank()) {
                        slot.set(fluid.getBucketStack());
                        slot.setChanged();
                    }
                }
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            if (index < this.getFilter().getContainerSize()) {
                slot.remove(1);
                slot.setChanged();
            } else {
                FluidWithData fluid = Services.LOADER_SERVICE.getFluidFromStack(slot.getItem());
                if (!fluid.isBlank()) {
                    for (int i = 0; i < this.getFilter().getContainerSize(); i++) {
                        if (!this.slots.get(i).hasItem()) {
                            Slot toSlot = this.slots.get(i);
                            toSlot.set(fluid.getBucketStack());
                            toSlot.setChanged();
                            break;
                        }
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

}

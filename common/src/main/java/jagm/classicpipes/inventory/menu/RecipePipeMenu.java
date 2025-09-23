package jagm.classicpipes.inventory.menu;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.container.FilterContainer;
import jagm.classicpipes.network.ClientBoundRecipePipePayload;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class RecipePipeMenu extends FilterMenu {

    private final Direction[] ioDirections;
    private final List<Direction> availableDirections;
    private final BlockPos pos;

    public RecipePipeMenu(int id, Inventory playerInventory, ClientBoundRecipePipePayload payload) {
        this(id, playerInventory, new FilterContainer(null, 10, true), payload.slotDirections(), payload.availableDirections(), payload.pos());
    }

    public RecipePipeMenu(int id, Inventory playerInventory, FilterContainer filter, Direction[] ioDirections, List<Direction> availableDirections, BlockPos pos) {
        super(ClassicPipes.RECIPE_PIPE_MENU, id, filter);
        this.ioDirections = ioDirections;
        this.availableDirections = availableDirections;
        this.pos = pos;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new FilterSlot(filter, j + i * 3, 53 + j * 18, 17 + i * 18));
            }
        }
        this.addSlot(new FilterSlot(filter, 9, 125, 35));
        int x = 8;
        int y = 89;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, x + j * 18, y + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, x + i * 18, y + 58));
        }
    }

    public void setSlotDirection(int slot, Direction direction) {
        this.ioDirections[slot] = direction;
    }

    public Direction getSlotDirection(int slot) {
        return this.ioDirections[slot];
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public boolean slotHasItem(int slot) {
        return this.getSlot(slot).hasItem();
    }

    public Direction nextDirection(Direction direction) {
        if (hasAvailableDirections()) {
            for (int i = 0; i < 6; i++) {
                direction = MiscUtil.nextDirection(direction);
                if (this.availableDirections.contains(direction)) {
                    return direction;
                }
            }
        }
        return direction;
    }

    public Direction prevDirection(Direction direction) {
        if (hasAvailableDirections()) {
            for (int i = 0; i < 6; i++) {
                direction = MiscUtil.prevDirection(direction);
                if (this.availableDirections.contains(direction)) {
                    return direction;
                }
            }
        }
        return direction;
    }

    public boolean hasAvailableDirections() {
        return !this.availableDirections.isEmpty();
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

}

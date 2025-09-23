package jagm.classicpipes.inventory.menu;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.StoragePipeEntity;
import jagm.classicpipes.network.ClientBoundThreeBoolsPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class StoragePipeMenu extends AbstractContainerMenu {

    private boolean defaultRoute;
    private boolean matchComponents;
    private boolean leaveOne;
    private final StoragePipeEntity pipe;

    public StoragePipeMenu(int id, Inventory playerInventory, ClientBoundThreeBoolsPayload payload) {
        this(id, playerInventory, payload.first(), payload.second(), payload.third(), null);
    }

    public StoragePipeMenu(int id, Inventory playerInventory, boolean defaultRoute, boolean matchComponents, boolean leaveOne, StoragePipeEntity pipe) {
        super(ClassicPipes.STORAGE_PIPE_MENU, id);
        int x = 8;
        int y = 84;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, x + j * 18, y + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, x + i * 18, y + 58));
        }
        this.defaultRoute = defaultRoute;
        this.matchComponents = matchComponents;
        this.leaveOne = leaveOne;
        this.pipe = pipe;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int id) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.pipe != null) {
            return this.pipe.stillValid(player);
        }
        return true;
    }

    public boolean shouldMatchComponents() {
        return this.matchComponents;
    }

    public void setMatchComponents(boolean matchComponents) {
        this.matchComponents = matchComponents;
        if (this.pipe != null) {
            this.pipe.setMatchComponents(matchComponents);
        }
    }

    public boolean shouldLeaveOne() {
        return this.leaveOne;
    }

    public void setLeaveOne(boolean leaveOne) {
        this.leaveOne = leaveOne;
        if (this.pipe != null) {
            this.pipe.setLeaveOne(leaveOne);
        }
    }

    public boolean isDefaultRoute() {
        return this.defaultRoute;
    }

    public void setDefaultRoute(boolean defaultRoute) {
        this.defaultRoute = defaultRoute;
        if (this.pipe != null) {
            this.pipe.setDefaultRoute(defaultRoute);
        }
    }

}

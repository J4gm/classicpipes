package jagm.classicpipes.inventory.menu;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.MatchingPipeEntity;
import jagm.classicpipes.network.ClientBoundBoolPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MatchingPipeMenu extends AbstractContainerMenu {

    private boolean matchComponents;
    private final MatchingPipeEntity pipe;

    public MatchingPipeMenu(int id, Inventory playerInventory, ClientBoundBoolPayload payload) {
        this(id, playerInventory, payload.value(), null);
    }

    public MatchingPipeMenu(int id, Inventory playerInventory, boolean matchComponents, MatchingPipeEntity pipe) {
        super(ClassicPipes.MATCHING_PIPE_MENU, id);
        int x = 8;
        int y = 66;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, x + j * 18, y + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, x + i * 18, y + 58));
        }
        this.matchComponents = matchComponents;
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

}

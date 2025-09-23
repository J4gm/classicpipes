package jagm.classicpipes.inventory.menu;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.container.SingleItemFilterContainer;
import jagm.classicpipes.network.ClientBoundBoolPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class AdvancedCopperPipeMenu extends FilterMenu {

    public AdvancedCopperPipeMenu(int id, Inventory playerInventory, ClientBoundBoolPayload payload) {
        this(id, playerInventory, new SingleItemFilterContainer(null, 9, payload.value()));
    }

    public AdvancedCopperPipeMenu(int id, Inventory playerInventory, Filter filter) {
        super(ClassicPipes.ADVANCED_COPPER_PIPE_MENU, id, filter);
        for (int j = 0; j < 9; j++) {
            this.addSlot(new FilterSlot(filter, j, 8 + j * 18, 18));
        }
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
    }

}

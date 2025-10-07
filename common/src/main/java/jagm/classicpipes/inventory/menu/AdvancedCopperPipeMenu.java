package jagm.classicpipes.inventory.menu;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.container.SingleItemFilterContainer;
import jagm.classicpipes.network.ClientBoundBoolPayload;
import net.minecraft.world.entity.player.Inventory;

public class AdvancedCopperPipeMenu extends FilterMenu {

    public AdvancedCopperPipeMenu(int id, Inventory playerInventory, ClientBoundBoolPayload payload) {
        this(id, playerInventory, new SingleItemFilterContainer(null, 9, payload.value()));
        payload.items().forEach(stackWithSlot -> this.getFilter().setItem(stackWithSlot.slot(), stackWithSlot.stack()));
    }

    public AdvancedCopperPipeMenu(int id, Inventory playerInventory, Filter filter) {
        super(ClassicPipes.ADVANCED_COPPER_PIPE_MENU, id, filter);
        for (int j = 0; j < 9; j++) {
            this.addSlot(new FilterSlot(filter, j, 8 + j * 18, 18));
        }
        this.addStandardInventorySlots(playerInventory, 8, 84);
    }

}

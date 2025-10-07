package jagm.classicpipes.inventory.menu;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.container.DirectionalFilterContainer;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.network.ClientBoundBoolPayload;
import net.minecraft.world.entity.player.Inventory;

public class DiamondPipeMenu extends FilterMenu {

    public DiamondPipeMenu(int id, Inventory playerInventory, ClientBoundBoolPayload payload) {
        this(id, playerInventory, new DirectionalFilterContainer(null, payload.value()));
        payload.items().forEach(stackWithSlot -> this.getFilter().setItem(stackWithSlot.slot(), stackWithSlot.stack()));
    }

    public DiamondPipeMenu(int id, Inventory playerInventory, Filter filter) {
        super(ClassicPipes.DIAMOND_PIPE_MENU, id, filter);
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new FilterSlot(filter, j + i * 9, 8 + j * 18, 18 + i * 18));
            }
        }
        this.addStandardInventorySlots(playerInventory, 8, 154);
    }
}

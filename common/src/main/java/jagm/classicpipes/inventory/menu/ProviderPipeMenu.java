package jagm.classicpipes.inventory.menu;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.ProviderPipeEntity;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.container.SingleItemFilterContainer;
import jagm.classicpipes.network.ClientBoundTwoBoolsPayload;
import net.minecraft.world.entity.player.Inventory;

public class ProviderPipeMenu extends FilterMenu {

    private boolean leaveOne;

    public ProviderPipeMenu(int id, Inventory playerInventory, ClientBoundTwoBoolsPayload payload) {
        this(id, playerInventory, new SingleItemFilterContainer(null, 9, payload.first()), payload.second());
        payload.items().forEach(stackWithSlot -> this.getFilter().setItem(stackWithSlot.slot(), stackWithSlot.stack()));
    }

    public ProviderPipeMenu(int id, Inventory playerInventory, Filter filter, boolean leaveOne) {
        super(ClassicPipes.PROVIDER_PIPE_MENU, id, filter);
        this.leaveOne = leaveOne;
        for (int j = 0; j < 9; j++) {
            this.addSlot(new FilterSlot(filter, j, 8 + j * 18, 18));
        }
        this.addStandardInventorySlots(playerInventory, 8, 84);
    }

    public boolean shouldLeaveOne() {
        return this.leaveOne;
    }

    public void setLeaveOne(boolean leaveOne) {
        this.leaveOne = leaveOne;
        if (this.getFilter().getPipe() instanceof ProviderPipeEntity providerPipe) {
            providerPipe.setLeaveOne(leaveOne);
        }
    }

}

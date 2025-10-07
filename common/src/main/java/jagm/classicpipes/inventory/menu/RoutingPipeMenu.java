package jagm.classicpipes.inventory.menu;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.RoutingPipeEntity;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.container.SingleItemFilterContainer;
import jagm.classicpipes.network.ClientBoundTwoBoolsPayload;
import net.minecraft.world.entity.player.Inventory;

public class RoutingPipeMenu extends FilterMenu {

    private boolean defaultRoute;

    public RoutingPipeMenu(int id, Inventory playerInventory, ClientBoundTwoBoolsPayload payload) {
        this(id, playerInventory, new SingleItemFilterContainer(null, 9, payload.first()), payload.second());
        payload.items().forEach(stackWithSlot -> this.getFilter().setItem(stackWithSlot.slot(), stackWithSlot.stack()));
    }

    public RoutingPipeMenu(int id, Inventory playerInventory, Filter filter, boolean defaultRoute) {
        super(ClassicPipes.ROUTING_PIPE_MENU, id, filter);
        this.defaultRoute = defaultRoute;
        for (int j = 0; j < 9; j++) {
            this.addSlot(new FilterSlot(filter, j, 8 + j * 18, 18));
        }
        this.addStandardInventorySlots(playerInventory, 8, 84);
    }

    public boolean isDefaultRoute() {
        return this.defaultRoute;
    }

    public void setDefaultRoute(boolean defaultRoute) {
        this.defaultRoute = defaultRoute;
        if (this.getFilter().getPipe() instanceof RoutingPipeEntity routingPipe) {
            routingPipe.setDefaultRoute(defaultRoute);
        }
    }

}

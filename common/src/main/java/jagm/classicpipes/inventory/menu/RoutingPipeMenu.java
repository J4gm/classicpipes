package jagm.classicpipes.inventory.menu;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.RoutingPipeEntity;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.container.SingleItemFilterContainer;
import jagm.classicpipes.network.ClientBoundTwoBoolsPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class RoutingPipeMenu extends FilterMenu {

    private boolean defaultRoute;

    public RoutingPipeMenu(int id, Inventory playerInventory, ClientBoundTwoBoolsPayload payload) {
        this(id, playerInventory, new SingleItemFilterContainer(null, 9, payload.first()), payload.second());
    }

    public RoutingPipeMenu(int id, Inventory playerInventory, Filter filter, boolean defaultRoute) {
        super(ClassicPipes.ROUTING_PIPE_MENU, id, filter);
        this.defaultRoute = defaultRoute;
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

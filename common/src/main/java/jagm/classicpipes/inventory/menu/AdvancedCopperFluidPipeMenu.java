package jagm.classicpipes.inventory.menu;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.container.SingleItemFilterContainer;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class AdvancedCopperFluidPipeMenu extends FluidFilterMenu {

    private static final Identifier EMPTY_SLOT = MiscUtil.identifier("container/slot/fluid");

    public AdvancedCopperFluidPipeMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SingleItemFilterContainer(null, 9, false));
    }

    public AdvancedCopperFluidPipeMenu(int id, Inventory playerInventory, Filter filter) {
        super(ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_MENU, id, filter);
        for (int j = 0; j < 9; j++) {
            int column = j;
            this.addSlot(new FilterSlot(filter, j, 8 + j * 18, 18) {

                @Override
                public Identifier getNoItemIcon() {
                    return column == 0 ? EMPTY_SLOT : null;
                }

            });
        }
        this.addStandardInventorySlots(playerInventory, 8, 84);
    }

}

package jagm.classicpipes.inventory.menu;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.container.DirectionalFilterContainer;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class DiamondFluidPipeMenu extends FluidFilterMenu {

    private static final Identifier EMPTY_SLOT = MiscUtil.identifier("container/slot/fluid");

    public DiamondFluidPipeMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new DirectionalFilterContainer(null, false));
    }

    public DiamondFluidPipeMenu(int id, Inventory playerInventory, DirectionalFilterContainer filter) {
        super(ClassicPipes.DIAMOND_FLUID_PIPE_MENU, id, filter);
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 9; j++) {
                int column = j;
                this.addSlot(new FilterSlot(filter, j + i * 9, 8 + j * 18, 18 + i * 18) {

                    @Override
                    public Identifier getNoItemIcon() {
                        return column == 0 ? EMPTY_SLOT : null;
                    }

                });
            }
        }
        this.addStandardInventorySlots(playerInventory, 8, 154);
    }

}

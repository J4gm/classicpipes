package jagm.classicpipes.inventory.menu;

import com.mojang.datafixers.util.Pair;
import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.container.DirectionalFilterContainer;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

public class DiamondFluidPipeMenu extends FluidFilterMenu {

    private static final ResourceLocation EMPTY_SLOT = MiscUtil.resourceLocation("item/empty_bucket");

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
                    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                        return column == 0 ? Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_SLOT) : null;
                    }

                });
            }
        }
        int x = 8;
        int y = 154;
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

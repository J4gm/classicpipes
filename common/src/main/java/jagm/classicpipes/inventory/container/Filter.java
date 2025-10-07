package jagm.classicpipes.inventory.container;

import jagm.classicpipes.blockentity.PipeEntity;
import net.minecraft.world.Container;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public interface Filter extends Container {

    void setMatchComponents(boolean matchComponents);

    boolean shouldMatchComponents();

    PipeEntity getPipe();

    default List<ItemStackWithSlot> getItemStacksForPayload() {
        List<ItemStackWithSlot> stacks = new ArrayList<>();
        for (int slot = 0; slot < this.getContainerSize(); slot++) {
            ItemStack stack = this.getItem(slot);
            if (!stack.isEmpty()) {
                stacks.add(new ItemStackWithSlot(slot, stack));
            }
        }
        return stacks;
    }

    enum MatchingResult {

        ITEM(true),
        TAG(true),
        MOD(true),
        FALSE(false);

        public final boolean matches;

        MatchingResult(boolean matches) {
            this.matches = matches;
        }

    }

}

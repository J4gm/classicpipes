package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.container.SingleItemFilterContainer;
import jagm.classicpipes.inventory.menu.AdvancedCopperFluidPipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.function.Predicate;

public class AdvancedCopperFluidPipeEntity extends CopperFluidPipeEntity implements MenuProvider {

    private final SingleItemFilterContainer filter;

    public AdvancedCopperFluidPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_ENTITY, pos, state);
        this.filter = new SingleItemFilterContainer(this, 9, false);
    }

    @Override
    protected int extractAmount() {
        return 500;
    }

    @Override
    protected Predicate<Fluid> filterPredicate() {
        return fluid -> this.filter.isEmpty() || this.filter.matches(new ItemStack(fluid.getBucket()));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + ClassicPipes.MOD_ID + ".advanced_copper_fluid_pipe");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AdvancedCopperFluidPipeMenu(id, inventory, this.filter);
    }

    @Override
    public void load(CompoundTag valueInput) {
        this.filter.clearContent();
        super.load(valueInput);
        ListTag filterList = valueInput.getList("filter", ListTag.TAG_COMPOUND);
        filterList.forEach(tag -> {
            if (tag instanceof CompoundTag compoundTag) {
                int slot = compoundTag.getInt("slot");
                MiscUtil.loadFromTag(tag, ItemStack.CODEC, stack -> this.filter.setItem(slot, stack));
            }
        });
    }

    @Override
    protected void saveAdditional(CompoundTag valueOutput) {
        super.saveAdditional(valueOutput);
        ListTag filterList = new ListTag();
        for (int slot = 0; slot < this.filter.getContainerSize(); slot++) {
            ItemStack stack = this.filter.getItem(slot);
            if (!stack.isEmpty()) {
                CompoundTag tag = new CompoundTag();
                tag.putInt("slot", slot);
                MiscUtil.saveToTag(tag, stack, ItemStack.CODEC, filterList::add);
            }
        }
        valueOutput.put("filter", filterList);
    }

}

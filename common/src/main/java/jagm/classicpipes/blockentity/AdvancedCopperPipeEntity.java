package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.container.SingleItemFilterContainer;
import jagm.classicpipes.inventory.menu.AdvancedCopperPipeMenu;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public class AdvancedCopperPipeEntity extends CopperPipeEntity implements MenuProvider {

    private final SingleItemFilterContainer filter;

    public AdvancedCopperPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.ADVANCED_COPPER_PIPE_ENTITY, pos, state);
        this.filter = new SingleItemFilterContainer(this, 9, false);
    }

    @Override
    protected int extractAmount() {
        return 64;
    }

    @Override
    protected Predicate<ItemStack> filterPredicate() {
        return stack -> this.filter.isEmpty() || this.filter.matches(stack);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + ClassicPipes.MOD_ID + ".advanced_copper_pipe");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AdvancedCopperPipeMenu(id, inventory, this.filter);
    }

    @Override
    public short getTargetSpeed() {
        return ItemInPipe.DEFAULT_SPEED * 8;
    }

    @Override
    public short getAcceleration() {
        return ItemInPipe.DEFAULT_ACCELERATION * 16;
    }

    public boolean shouldMatchComponents() {
        return this.filter.shouldMatchComponents();
    }

    @Override
    protected void loadAdditional(CompoundTag valueInput, HolderLookup.Provider registries) {
        this.filter.clearContent();
        super.loadAdditional(valueInput, registries);
        ListTag filterList = valueInput.getList("filter", ListTag.TAG_COMPOUND);
        filterList.forEach(tag -> {
            if (tag instanceof CompoundTag compoundTag) {
                int slot = compoundTag.getInt("slot");
                MiscUtil.loadFromTag(tag, ItemStack.CODEC, registries, stack -> this.filter.setItem(slot, stack));
            }
        });
        this.filter.setMatchComponents(valueInput.getBoolean("match_components"));
    }

    @Override
    protected void saveAdditional(CompoundTag valueOutput, HolderLookup.Provider registries) {
        super.saveAdditional(valueOutput, registries);
        ListTag filterList = new ListTag();
        for (int slot = 0; slot < this.filter.getContainerSize(); slot++) {
            ItemStack stack = this.filter.getItem(slot);
            if (!stack.isEmpty()) {
                CompoundTag tag = new CompoundTag();
                tag.putInt("slot", slot);
                MiscUtil.saveToTag(tag, stack, ItemStack.CODEC, registries, filterList::add);
            }
        }
        valueOutput.put("filter", filterList);
        valueOutput.putBoolean("match_components", this.filter.shouldMatchComponents());
    }

}

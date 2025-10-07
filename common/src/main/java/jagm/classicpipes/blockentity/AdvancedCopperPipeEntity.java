package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.container.SingleItemFilterContainer;
import jagm.classicpipes.inventory.menu.AdvancedCopperPipeMenu;
import jagm.classicpipes.util.ItemInPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
        return stack -> this.filter.isEmpty() || this.filter.matches(stack).matches;
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
    protected void loadAdditional(ValueInput valueInput) {
        this.filter.clearContent();
        super.loadAdditional(valueInput);
        ValueInput.TypedInputList<ItemStackWithSlot> filterList = valueInput.listOrEmpty("filter", ItemStackWithSlot.CODEC);
        for (ItemStackWithSlot slotStack : filterList) {
            this.filter.setItem(slotStack.slot(), slotStack.stack());
        }
        this.filter.setMatchComponents(valueInput.getBooleanOr("match_components", false));
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        ValueOutput.TypedOutputList<ItemStackWithSlot> filterList = valueOutput.list("filter", ItemStackWithSlot.CODEC);
        for (int slot = 0; slot < this.filter.getContainerSize(); slot++) {
            ItemStack stack = this.filter.getItem(slot);
            if (!stack.isEmpty()) {
                filterList.add(new ItemStackWithSlot(slot, stack));
            }
        }
        valueOutput.putBoolean("match_components", this.filter.shouldMatchComponents());
    }

}

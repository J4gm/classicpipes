package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.container.FilterContainer;
import jagm.classicpipes.inventory.container.SingleItemFilterContainer;
import jagm.classicpipes.inventory.menu.RoutingPipeMenu;
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

public class RoutingPipeEntity extends NetworkedPipeEntity implements MenuProvider {

    private final SingleItemFilterContainer filter;
    private boolean defaultRoute;

    public RoutingPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.ROUTING_PIPE_ENTITY, pos, state);
        this.filter = new SingleItemFilterContainer(this, 9, false);
        this.defaultRoute = false;
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
        this.defaultRoute = valueInput.getBooleanOr("default_route", false);
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
        valueOutput.putBoolean("default_route", this.defaultRoute);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + ClassicPipes.MOD_ID + ".routing_pipe");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new RoutingPipeMenu(id, playerInventory, this.filter, this.defaultRoute);
    }

    public boolean shouldMatchComponents() {
        return this.filter.shouldMatchComponents();
    }

    @Override
    public boolean isDefaultRoute() {
        return this.defaultRoute;
    }

    public void setDefaultRoute(boolean defaultRoute) {
        this.defaultRoute = defaultRoute;
        if (defaultRoute) {
            this.getNetwork().addPipe(this);
        } else {
            this.getNetwork().getDefaultRoutes().remove(this);
        }
    }

    public FilterContainer.MatchingResult canRouteItemHere(ItemStack stack) {
        return this.filter.matches(stack);
    }

    public Filter getFilter() {
        return this.filter;
    }

}
